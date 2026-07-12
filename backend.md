# ¿Cómo se implementó el backend?

## Contenido

### Spring Boot

El backend se construyó como cinco proyectos Maven independientes, cada uno con su propio `pom.xml` y su propio `spring-boot-starter-parent` en la versión 3.5.4, sobre Java 21: `DiscoveryService`, `GatewayService`, `UserService`, `OrderService` y `ProductServiceElastic`. No existe un POM raíz que los agrupe como módulos de un mismo *reactor* de Maven; cada carpeta se compila y se empaqueta por su cuenta, con su propio `Dockerfile`. Es una organización válida para cinco servicios que en efecto se despliegan por separado, aunque tiene un costo que ya se nota en el código: la versión de Spring Cloud (`2025.0.0`) y la de la librería JWT (`jjwt 0.12.6`) están declaradas como propiedades sueltas, repetidas de forma idéntica en `UserService` y `GatewayService` porque no hay un BOM propio del proyecto que las centralice. Hoy coinciden por casualidad de que nadie las ha tocado; nada impide que una actualización futura en un servicio deje al otro un paso atrás sin que ningún build lo señale.

Cada servicio elige únicamente los *starters* que su función exige, no un paquete genérico común: `GatewayService` depende de `spring-cloud-starter-gateway-server-webflux` (la variante reactiva, sobre Netty, no la basada en Servlet), mientras que `UserService`, `OrderService` y `ProductServiceElastic` usan `spring-boot-starter-web`, que arranca sobre Tomcat embebido con el modelo de hilo por petición tradicional. Es una decisión correcta —un *gateway* que reenvía miles de conexiones concurrentes se beneficia del modelo no bloqueante—, pero también significa que conviven dos paradigmas de concurrencia distintos dentro del mismo backend: reactivo (`Mono`, `WebExchange`) en la puerta de entrada, imperativo en todo lo demás. Un desarrollador que solo conoce Spring MVC puede leer y modificar cuatro de los cinco servicios sin dificultad, pero necesita otro vocabulario para tocar el filtro del Gateway.

### Organización de los microservicios

Los cinco proyectos siguen el mismo esqueleto de paquetes: `controller`, `service`, `model` (con subcarpetas `entity`, `dto`, `request`, `response` según el servicio), `repository` y `exception`, todo bajo `com.speedsneakers.<nombreservicio>`. Esa uniformidad de carpetas no es casualidad de estilo: hace que, una vez entendido un servicio, los otros se lean sin curva de aprendizaje adicional, aunque el contenido real de cada capa varía mucho según lo que el servicio necesita.

`DiscoveryService` es el más simple de los cinco: una sola clase, `DiscoveryserviceApplication`, anotada con `@EnableEurekaServer`, sin controladores propios ni lógica de negocio. `GatewayService` tampoco tiene capa de servicio ni repositorio; su único componente relevante es `AuthenticationFilter`, un `GlobalFilter` que intercepta cada petición antes de enrutarla. `UserService`, `OrderService` y `ProductServiceElastic` sí siguen el patrón de tres capas completo (controlador REST → servicio con la lógica → repositorio de datos), aunque cada uno habla con un motor de persistencia distinto: `UserService` y `OrderService` usan Spring Data JPA sobre PostgreSQL, y `ProductServiceElastic` usa Spring Data Elasticsearch, con una interfaz de repositorio (`ProductRepository extends ElasticsearchRepository<Product, String>`) que no tiene equivalente relacional en el resto del backend.

### Responsabilidades

Cada servicio tiene un dominio de negocio exclusivo y no invade el de los demás, ni siquiera para consultas de solo lectura:

- **`DiscoveryService`** no resuelve negocio: es el registro donde los demás anuncian su dirección y su estado de salud, para que nadie tenga que codificar una URL fija hacia otro servicio.
- **`GatewayService`** tampoco resuelve negocio: su única responsabilidad es recibir toda petición externa, decidir si trae una sesión válida y, de ser así, reenviarla al servicio correcto. No sabe qué es un producto ni qué es una orden.
- **`UserService`** posee en exclusiva el dominio de identidad: alta de cuentas, verificación de contraseña con BCrypt, emisión y validación del JWT. Ningún otro servicio tiene su propia tabla de usuarios ni su propia lógica de contraseñas.
- **`ProductServiceElastic`** posee el catálogo: alta, baja, modificación y búsqueda de productos contra un índice de Elasticsearch. Es también el único servicio de negocio que no depende de PostgreSQL.
- **`OrderService`** posee el dominio de pedidos, pero no puede resolverlo solo con su propia base de datos: para armar una orden necesita el precio y el nombre reales del producto, y en lugar de asumir un duplicado local va a buscarlo a `ProductServiceElastic` en el momento de la compra. Es la única responsabilidad del backend que exige, por diseño, coordinarse con otra.

Esta separación tiene una consecuencia que vale la pena nombrar: ningún servicio de negocio (`UserService`, `OrderService`, `ProductServiceElastic`) valida el JWT por su cuenta. `OrderController.createOrder` recibe el identificador del usuario ya resuelto en el encabezado `X-User-Id` y lo usa sin cuestionarlo; ni siquiera existe una clase `SecurityConfig` en `OrderService` ni en `ProductServiceElastic`. La responsabilidad de "saber quién es el usuario" quedó concentrada por completo en el Gateway, lo cual simplifica los tres servicios de negocio, pero también los deja sin ninguna defensa propia si algo llega hasta ellos sin pasar por esa puerta.

### Comunicación REST

El backend combina dos formas distintas de comunicación REST, y ninguna de las dos usa un *broker* de mensajería ni comunicación asíncrona: todo es síncrono, petición-respuesta.

La primera es la que entra desde el frontend. `GatewayService` no declara una tabla fija de rutas en su `application.yml`; activa `spring.cloud.gateway.server.webflux.discovery.locator.enabled: true`, que arma las rutas en tiempo real a partir de lo que `DiscoveryService` tenga registrado, usando el nombre del servicio en minúsculas como prefijo (`/userservice/**`, `/orderservice/**`, `/productservice/**`). Antes de reenviar nada, `AuthenticationFilter` —con `getOrder() = -1` para ejecutarse antes que el enrutamiento— revisa la cookie `token`, valida su firma con la misma clave HMAC que usa `UserService` para firmarla, y si es válida agrega el encabezado `X-User-Id` con el `subject` del token. Solo tres rutas quedan exentas de este filtro: `/userservice/auth/login`, `/register` y `/logout`.

La segunda es la comunicación servicio a servicio, y ocurre en un único punto del backend: `OrderService` llama a `ProductServiceElastic` mediante un cliente OpenFeign (`ProductClient`, anotado `@FeignClient(name = "productservice")`), que resuelve la dirección real contra `DiscoveryService` y hace la llamada HTTP directamente, sin pasar otra vez por el Gateway. Es tráfico interno entre dos servicios de negocio, y ahí no hay validación de JWT ni propagación de `X-User-Id` hacia `ProductServiceElastic`: ese servicio recibe la petición sin ningún dato sobre qué usuario la originó, porque no lo necesita para resolver un producto por su identificador. `OrderServiceImpl.createOrder` usa la respuesta de ese Feign —nombre, precio, imagen— para construir cada línea de la orden, en lugar de confiar en los valores que mandó el propio cliente al armar el carrito, que es la decisión correcta frente a la alternativa de aceptar un precio que el navegador pudiera manipular.

El manejo de errores de esta comunicación es desigual entre servicios. `UserService` sí centraliza sus respuestas de error en un `@RestControllerAdvice` (`GlobalExceptionHandler`), con manejadores específicos para credenciales inválidas, usuario duplicado y errores de validación de campos. `OrderService` y `ProductServiceElastic` no tienen ese componente: una excepción sin capturar —un tiempo de espera del cliente Feign, un producto que ya no existe en el índice— sube tal cual y el cliente recibe un `500` genérico de Spring Boot, sin un mensaje que distinga "el producto ya no está disponible" de cualquier otra falla interna.

### Tabla de Microservicios implementados y su función

| Microservicio | Puerto | Función principal | Persistencia | Framework / mecanismo clave |
|---|---|---|---|---|
| `DiscoveryService` | 8761 | Registro y descubrimiento de los demás servicios (Eureka Server) | Ninguna | `spring-cloud-starter-netflix-eureka-server` |
| `GatewayService` | 8762 | Punto único de entrada; enrutamiento dinámico y validación de JWT vía cookie | Ninguna | `spring-cloud-starter-gateway-server-webflux`, `AuthenticationFilter` |
| `UserService` | 8083 | Registro, autenticación e identidad de usuarios; emisión del JWT | PostgreSQL (`users_db`) | Spring Security + BCrypt, JJWT, JPA |
| `OrderService` | 8082 | Creación y consulta de órdenes, con precios verificados contra el catálogo | PostgreSQL (`orders_db`) | JPA, OpenFeign (`ProductClient`) |
| `ProductServiceElastic` | 8081 | Alta, baja, modificación y búsqueda de productos del catálogo | Elasticsearch (Bonsai, gestionado externamente) | Spring Data Elasticsearch |

## Análisis

La separación de responsabilidades es la parte del backend mejor resuelta: cada servicio tiene un dominio claro, ninguno accede a la base de datos de otro, y la autenticación se resolvió una sola vez en el Gateway en lugar de repetirse en cada servicio de negocio. Es una arquitectura de microservicios correctamente entendida en su intención, no solo copiada de un tutorial.

El punto más frágil no está en el diseño de cada servicio sino en lo que falta entre ellos. Que la lógica de firmar y validar el JWT —`getSigningKey`, `getClaims`, la comprobación de expiración— esté escrita de forma casi idéntica en `AuthenticationFilter` (Gateway) y en `JwtService` (UserService), sin ninguna librería compartida entre los cinco proyectos, es consecuencia directa de no tener un módulo común: cualquier corrección de seguridad a esa lógica —por ejemplo, endurecer la validación de la firma— hay que aplicarla dos veces, en dos repositorios de código que ya hoy solo coinciden por coincidencia. Que `OrderService` y `ProductServiceElastic` no tengan un manejador de excepciones centralizado, a diferencia de `UserService`, tampoco es una decisión explícita documentada en ningún lado: es simplemente lo que quedó sin escribir cuando esos dos servicios se construyeron. Y que ninguno de los tres servicios de negocio valide el JWT por su cuenta es coherente con el diseño mientras todo el tráfico pase por el Gateway, pero convierte a `X-User-Id` en un valor de confianza ciega en cuanto una petición llega a `OrderService` o `ProductServiceElastic` por cualquier otra vía, algo que la topología de red del entorno de desarrollo actual no impide.

## Transición

Con las cinco piezas del backend descritas por separado —qué construye cada una y con qué versión de Spring Boot— el informe puede pasar de la organización del código a lo que ocurre cuando esas piezas efectivamente se comunican en producción: cómo viaja una cookie entre el frontend en Vercel y el Gateway en Azure, qué expone `docker-compose.yml` más allá del puerto del Gateway, y qué tan protegido está en la práctica el tráfico que este apartado describió en teoría.
