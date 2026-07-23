# Tienda App — Aplicación de Comercio Electrónico basada en Microservicios

> Documentación técnica del proyecto

---

## Resumen

**Tienda App** es una aplicación de comercio electrónico desarrollada bajo una **arquitectura de
microservicios**. El sistema implementa la separación de dominios de negocio (catálogo de productos,
gestión de pedidos y gestión de usuarios), un motor de búsqueda de texto completo basado en
Elasticsearch, **autenticación stateless mediante JSON Web Tokens (JWT)** validada de forma
centralizada en el Gateway, mecanismos de descubrimiento dinámico de servicios y enrutamiento
centralizado, así como una infraestructura de despliegue contenedorizada y orientada a la nube.

El presente documento describe de forma explícita las funcionalidades, los módulos que componen el
sistema y las características técnicas avanzadas que lo sustentan.

---

## Índice

1. [Introducción y objetivos](#1-introducción-y-objetivos)
2. [Arquitectura general del sistema](#2-arquitectura-general-del-sistema)
3. [Separación de dominios](#3-separación-de-dominios)
4. [Descripción de módulos](#4-descripción-de-módulos)
5. [Motor de búsqueda avanzado](#5-motor-de-búsqueda-avanzado)
6. [Comunicación entre servicios](#6-comunicación-entre-servicios)
7. [Manejo de excepciones](#7-manejo-de-excepciones)
8. [Bases de datos y modelo de datos](#8-bases-de-datos-y-modelo-de-datos)
9. [Infraestructura de despliegue](#9-infraestructura-de-despliegue)
10. [Stack tecnológico](#10-stack-tecnológico)
11. [Catálogo de endpoints (API REST)](#11-catálogo-de-endpoints-api-rest)
12. [Pruebas automatizadas y calidad](#12-pruebas-automatizadas-y-calidad)
13. [Ejecución del proyecto](#13-ejecución-del-proyecto)

---

## 1. Introducción y objetivos

El proyecto persigue la implementación de una tienda en línea funcional aplicando los principios de
diseño de sistemas distribuidos. Sus objetivos técnicos son:

- **Desacoplamiento de dominios** mediante microservicios independientes, cada uno con su propia
  base de datos (*patrón database-per-service*).
- **Escalabilidad horizontal** apoyada en descubrimiento dinámico de servicios.
- **Búsqueda avanzada de productos** con tolerancia a errores tipográficos (*fuzzy search*).
- **Seguridad de acceso** mediante registro de usuarios, autenticación basada en JWT y propagación
  de la identidad del usuario a los servicios de negocio.
- **Despliegue reproducible** mediante contenedores y automatización en la nube.

---

## 2. Arquitectura general del sistema

El sistema sigue un patrón de microservicios con una **puerta de entrada única** (*API Gateway*) y un
**registro de servicios** (*Service Registry*). El frontend en React consume la API exclusivamente a
través del Gateway, que además **valida el token JWT** y propaga la identidad del usuario autenticado
a los servicios de negocio.

La validación del JWT ya no es responsabilidad exclusiva del Gateway: `OrderService` y
`ProductServiceElastic` incorporan su propio filtro de Spring Security (`JwtAuthenticationFilter`) que
revalida la firma y vigencia del token de forma independiente (ver §6, *defensa en profundidad*), de
modo que un `X-User-Id` falsificado no pueda alcanzar un servicio interno si este quedara expuesto sin
pasar por el Gateway.

```
                         ┌─────────────────────────┐
                         │   Frontend (React)      │
                         │   tienda-react          │
                         └────────────┬────────────┘
                                      │  HTTP / CORS + JWT (cookie HttpOnly)
                                      ▼
                         ┌─────────────────────────────┐
                         │  Gateway Service  :8762     │
                         │  (Spring Cloud Gateway)     │
                         │  Validación JWT + X-User-Id │
                         └────────────┬────────────────┘
                                      │  Enrutamiento dinámico (Eureka)
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                          ▼
  ┌───────────────────┐    ┌───────────────────┐     ┌───────────────────┐
  │  User Service     │    │ Product Service   │     │  Order Service    │
  │      :8083        │    │  Elastic  :8081   │◄─ Feign ─│      :8082   │
  │ (PostgreSQL,      │    │ (Elasticsearch)   │     │  (PostgreSQL)     │
  │  JWT + BCrypt)    │    │                   │     └───────────────────┘
  └───────────────────┘    └───────────────────┘
            ▲                         ▲                          ▲
            │            Registro / descubrimiento               │
            └─────────────────────────┬──────────────────────────┘
                                      ▼
                         ┌─────────────────────────┐
                         │ Discovery Service :8761 │
                         │   (Netflix Eureka)      │
                         └─────────────────────────┘
```

| Servicio | Puerto | Rol arquitectónico |
|----------|:------:|--------------------|
| `DiscoveryService` | 8761 | Registro y descubrimiento de servicios |
| `GatewayService` | 8762 | Puerta de entrada, enrutamiento y validación de JWT |
| `UserService` | 8083 | Dominio de usuarios y autenticación |
| `ProductServiceElastic` | 8081 | Dominio de catálogo |
| `OrderService` | 8082 | Dominio de pedidos |

---

## 3. Separación de dominios

La aplicación descompone la lógica de negocio en **dominios independientes**, cada uno encapsulado en
un microservicio con su propio modelo de datos y almacenamiento. Esta separación garantiza que un
dominio pueda evolucionar, escalar y desplegarse sin afectar a los demás.

- **Aislamiento de datos:** el dominio de pedidos (`orders_db`), el de usuarios (`users_db`) y el de
  productos (índice `products` en Elasticsearch) no comparten esquema entre sí; la única vía de acceso
  a datos ajenos es a través de la API del servicio propietario.
- **Independencia de despliegue:** cada microservicio dispone de su propio `Dockerfile` y se despliega
  como contenedor autónomo.
- **Responsabilidad única:** cada servicio implementa un único contexto delimitado (*bounded context*).

---

## 4. Descripción de módulos

### 4.1. Discovery Service (Registro de servicios)

Servidor de registro y descubrimiento basado en **Netflix Eureka** (`@EnableEurekaServer`).

- Permite que los servicios se **auto-registren** y se localicen entre sí por nombre lógico,
  eliminando la dependencia de direcciones IP o URLs fijas.
- Constituye la base para el **escalado horizontal** y el balanceo de carga.
- Puerto: `8761` (parametrizable mediante `EUREKA_PORT`).

### 4.2. Gateway Service (Puerta de entrada)

Puerta de entrada única implementada con **Spring Cloud Gateway** sobre un stack reactivo (WebFlux).

- **Enrutamiento dinámico** basado en descubrimiento: las rutas (`/productservice/**`,
  `/orderservice/**`) se derivan automáticamente de los servicios registrados en Eureka.
- **Configuración CORS** global que habilita el consumo desde el frontend (métodos
  `GET`, `POST`, `PUT`, `DELETE`, `PATCH`).
- **Observabilidad** mediante Spring Boot Actuator (`GET /actuator/gateway/routes`).
- Puerto: `8762` (parametrizable mediante `GATEWAY_PORT`).

### 4.3. Product Service Elastic (Catálogo)

Microservicio de gestión del catálogo de productos, con búsqueda de texto completo como requisito de
diseño.

- **CRUD completo** de productos a través de una API REST.
- Modelo de producto: identificador, nombre, marca, categoría, descripción corta, descripción larga,
  precio (`BigDecimal`) e imagen.
- **Búsqueda con tolerancia a errores** (*fuzzy search*) sobre nombre, marca y categoría (ver §5 para
  el detalle del motor).
- Persistencia documental en **Elasticsearch**, gestionado como servicio externo en **Bonsai Cloud**.
- **Validación independiente del JWT** mediante Spring Security (`SecurityConfig` +
  `JwtAuthenticationFilter`): revalida la cookie `token` en cada petición y solo exime de esta
  validación las rutas `/products/health` y `/actuator/**`.
- **Manejo centralizado de excepciones** (`GlobalExceptionHandler`): un producto inexistente, una
  petición de alta/edición inválida o una falla de comunicación con Elasticsearch responden con un
  cuerpo de error consistente en lugar de un `500` genérico (ver §7).
- Puerto: `8081`.

> El proyecto contó en una etapa anterior con una versión relacional del catálogo (`ProductService`,
> sobre PostgreSQL, con búsqueda mediante `LIKE`). Esa versión fue retirada por completo del
> repositorio; `ProductServiceElastic` es hoy la única implementación del dominio de catálogo.

### 4.4. Order Service (Pedidos)

Microservicio de gestión de pedidos del cliente.

- Creación y consulta de órdenes a través de una API REST.
- Modelo de orden compuesto por **líneas de pedido** (`OrderItem`): cantidad, precio unitario,
  subtotal y *snapshot* del nombre e imagen del producto en el momento de la compra.
- **Asociación al usuario autenticado:** cada orden registra el identificador del usuario propietario,
  obtenido de la cabecera `X-User-Id` que el Gateway propaga tras validar el JWT (ver §6). Esto
  habilita la consulta de los pedidos propios del cliente ("mis pedidos").
- Estados del pedido modelados mediante enumeración (`OrderStatus`).
- **Integración con el catálogo** mediante cliente OpenFeign (ver §6), cuyo `FeignConfig` reenvía la
  cookie `token` de la petición original hacia `ProductServiceElastic`, que ahora exige un JWT válido
  propio.
- **Validación independiente del JWT** mediante Spring Security (`SecurityConfig` +
  `JwtAuthenticationFilter`): revalida la cookie `token` en cada petición, igual que
  `ProductServiceElastic` (ver §2 y §6).
- **Manejo centralizado de excepciones** (`GlobalExceptionHandler`): una orden inexistente, una
  solicitud inválida o una falla al consultar `ProductServiceElastic` vía Feign responden con un
  cuerpo de error consistente en lugar de un `500` genérico (ver §7).
- Persistencia en **PostgreSQL** independiente (`orders_db`).
- Puerto: `8082`.

### 4.5. User Service (Usuarios y autenticación)

Microservicio responsable de la gestión de usuarios y de la **autenticación stateless** del sistema.

- **Registro de usuarios** (`POST /auth/register`) e **inicio de sesión** (`POST /auth/login`) a
  través de una API REST.
- Modelo de usuario: identificador, nombre de usuario (único), correo electrónico (único), contraseña,
  rol y fecha de creación.
- **Almacenamiento seguro de credenciales:** las contraseñas se cifran con **BCrypt**; nunca se
  persisten ni se devuelven en texto plano.
- **Emisión de JSON Web Tokens (JWT):** tras un inicio de sesión o registro correcto, el servicio
  genera un token firmado (con un secreto `JWT_SECRET` compartido con el Gateway) que se entrega al
  cliente como **cookie `HttpOnly`** (`token`, vigencia de 24 horas), no en el cuerpo de la respuesta.
  El cliente no necesita adjuntar manualmente ningún encabezado: el navegador reenvía la cookie en
  cada petición posterior.
- **Seguridad con Spring Security:** configuración *stateless* (sin sesión de servidor), con los
  endpoints bajo `/auth/**` expuestos públicamente a nivel de este servicio (la protección real de
  las rutas de negocio ocurre en el Gateway, ver §6).
- Consulta del perfil del usuario autenticado mediante `GET /auth/me`, y cierre de sesión mediante
  `POST /auth/logout` (invalida la cookie).
- Persistencia en **PostgreSQL** independiente (`users_db`).
- Puerto: `8083`.

### 4.6. Frontend (tienda-react)

Interfaz de usuario de la tienda construida en **React**, que consume la API exclusivamente a través
del Gateway.

---

## 5. Motor de búsqueda avanzado

El módulo `ProductServiceElastic` implementa un motor de búsqueda de texto completo de carácter
distribuido y escalable:

- **Persistencia y búsqueda sobre Elasticsearch 7.10.2**, desplegado como servicio gestionado en
  **Bonsai Cloud** (conexión vía SSL con autenticación básica).
- **Búsqueda con tolerancia a errores (*fuzzy search*):** consulta `multi_match` de tipo `bool_prefix`
  con parámetro `fuzziness: 1`, lo que permite coincidencias parciales y aproximadas —habilitando
  autocompletado y corrección de errores tipográficos— sobre los campos nombre, marca y categoría.
- **Modelo indexado como documento** (`@Document`, índice `products`) con mapeo explícito de tipos de
  campo Elasticsearch.
- **Configuración personalizada del cliente** (`ElasticsearchConfig`) con interceptores para la
  gestión de cabeceras `Content-Type`.
- **Endpoint de verificación de estado** dedicado: `GET /products/health`.

---

## 6. Comunicación entre servicios

La comunicación inter-servicio se realiza de forma **declarativa y balanceada**:

- El `OrderService` integra el catálogo mediante un **cliente OpenFeign** (`ProductClient`,
  `@FeignClient(name = "productservice")`).
- Al construir un pedido, el servicio invoca `GET /products/{id}` sobre `productservice` para
  enriquecer las líneas de pedido con la información del producto.
- El destino se **resuelve dinámicamente a través de Eureka** (por nombre lógico, no por URL fija) y
  las peticiones se distribuyen con **balanceo de carga**.

### Flujo de autenticación y autorización

La seguridad se basa en **JWT** y se aplica de forma centralizada en el Gateway:

1. El cliente se autentica contra el `UserService` (`POST /auth/login` o `POST /auth/register`) y
   recibe el token JWT como **cookie `HttpOnly`** (`token`), no en el cuerpo de la respuesta.
2. En cada petición posterior, el navegador adjunta automáticamente esa cookie (el cliente React la
   envía con `credentials: 'include'` en cada `fetch`); no hay un encabezado `Authorization` manual.
3. El **Gateway valida la firma y la vigencia del token** mediante un filtro global
   (`AuthenticationFilter`) que se ejecuta antes del enrutamiento. Las únicas rutas exentas de esta
   validación son `/userservice/auth/login`, `/userservice/auth/register` y
   `/userservice/auth/logout`; la consulta de productos **no** es pública y también exige una sesión
   válida.
4. Tras validar el token, el Gateway **extrae la identidad del usuario y la propaga** a los servicios
   internos a través de la cabecera `X-User-Id`.
5. **Defensa en profundidad:** `OrderService` y `ProductServiceElastic` no confían ciegamente en la
   cabecera `X-User-Id` inyectada por el Gateway. Cada uno incorpora su propio
   `JwtAuthenticationFilter` (Spring Security) que vuelve a extraer la cookie `token` de la petición,
   revalida su firma y vigencia, y **sobrescribe** `X-User-Id` con el valor real extraído del token
   antes de que llegue al controlador. Así, si alguno de estos servicios quedara alcanzable sin pasar
   por el Gateway (p. ej. por un puerto expuesto en `docker-compose.yml`), una cabecera `X-User-Id`
   falsificada no podría suplantar a otro usuario.
6. Cuando `OrderService` invoca a `ProductServiceElastic` mediante el cliente Feign, el
   `FeignConfig` reenvía la cookie `token` de la petición original, de modo que la llamada interna
   también supera la validación de JWT propia de `ProductServiceElastic`.

---

## 7. Manejo de excepciones

Cada microservicio de negocio centraliza el tratamiento de errores en un `GlobalExceptionHandler`
(`@RestControllerAdvice`), de modo que ninguna falla se traduce en un `500` genérico sin contexto para
el cliente:

| Servicio | Excepciones controladas | Respuesta |
|----------|--------------------------|-----------|
| `UserService` | Usuario ya existente, credenciales inválidas, errores de validación de campos (`@Valid`) | `409`, `401`, `400` respectivamente, con cuerpo `{"message": ...}` |
| `ProductServiceElastic` | Producto no encontrado, ID o petición inválida, falla de comunicación con Elasticsearch (`DataAccessException`) | `404`, `400`, `503`, con cuerpo `{"message": ...}` |
| `OrderService` | Orden no encontrada, errores de validación, ID no numérico, producto no encontrado o catálogo no disponible vía Feign (`FeignException`) | `404`, `400`, `503`, con cuerpo `{"message": ...}` |

En todos los casos existe además un manejador de última instancia para `RuntimeException` que evita
que una excepción no anticipada exponga una traza de pila al cliente, registrando el detalle en el log
del servicio. En `OrderService`, una falla al invocar `ProductServiceElastic` vía Feign (tiempo de
espera, `401` por un token vencido entre servicios, `5xx`, conexión rechazada) se traduce en un `503`
con un mensaje de catálogo no disponible, en lugar de propagar el error crudo del cliente Feign.

---

## 8. Bases de datos y modelo de datos

El sistema aplica el patrón **database-per-service**: cada microservicio es el único propietario de su
almacén de datos y nadie accede directamente al esquema de otro servicio (el acceso siempre es a
través de su API). Conviven dos tecnologías de persistencia: **PostgreSQL** (relacional) para los
dominios de usuarios y pedidos, y **Elasticsearch** (documental) para el catálogo.

| Servicio | Tecnología | Almacén | Esquema / Índice |
|----------|------------|---------|------------------|
| `UserService` | PostgreSQL | `users_db` | tabla `users` |
| `OrderService` | PostgreSQL | `orders_db` | tablas `orders`, `order_items` |
| `ProductServiceElastic` | Elasticsearch | índice `products` | documento `Product` |

> **Nota:** en el entorno local con Docker Compose, `users_db` y `orders_db` corren en contenedores
> PostgreSQL independientes (`users-db`, `orders-db`), cada uno con su propio volumen. El esquema se
> genera automáticamente a partir de las entidades JPA mediante la propiedad `hibernate.ddl-auto`
> (parametrizable por servicio, p. ej. `ORDERSERVICE_DDL_AUTO`).

### 8.1. Dominio de usuarios (`users_db`)

Tabla **`users`** — gestionada por `UserService`.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `BIGSERIAL` (PK) | Identificador del usuario |
| `username` | `VARCHAR` (único) | Nombre de usuario |
| `email` | `VARCHAR` (único) | Correo electrónico, usado como credencial de acceso |
| `password` | `VARCHAR` | Contraseña cifrada con **BCrypt** (nunca en texto plano) |
| `role` | `VARCHAR` | Rol del usuario para autorización |
| `created_at` | `TIMESTAMP` | Fecha de creación de la cuenta |

### 8.2. Dominio de pedidos (`orders_db`)

Tabla **`orders`** — cabecera del pedido (entidad `Order`).

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `BIGSERIAL` (PK) | Identificador de la orden |
| `user_id` | `VARCHAR` | Identificador del usuario propietario (propagado por el Gateway vía `X-User-Id`) |
| `order_date` | `TIMESTAMP` | Fecha y hora del pedido |
| `status` | `VARCHAR` | Estado del pedido (enumeración `OrderStatus`, p. ej. `PENDING`) |
| `total_amount` | `NUMERIC(10,2)` | Importe total del pedido |

Tabla **`order_items`** — líneas del pedido (entidad `OrderItem`).

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `BIGSERIAL` (PK) | Identificador de la línea |
| `order_id` | `BIGINT` (FK → `orders.id`) | Orden a la que pertenece la línea |
| `product_id` | `VARCHAR` (indexado) | Identificador del producto |
| `name` | `VARCHAR` | *Snapshot* del nombre del producto al momento de la compra |
| `image_url` | `VARCHAR` | *Snapshot* de la imagen del producto |
| `quantity` | `INTEGER` | Cantidad solicitada |
| `price_per_unit` | `NUMERIC(10,2)` | Precio unitario en el momento de la compra |
| `subtotal` | `NUMERIC(10,2)` | Subtotal de la línea |

> La relación `orders` 1—N `order_items` se gestiona con `cascade = ALL` y `orphanRemoval = true`,
> de modo que las líneas se persisten y eliminan junto con su orden. Los campos `name` e `image_url`
> son *snapshots* tomados del catálogo al crear el pedido (vía OpenFeign, ver §6), lo que preserva la
> información histórica aunque el producto cambie posteriormente.

### 8.3. Dominio de catálogo (índice `products`)

Índice **`products`** de Elasticsearch — gestionado por `ProductServiceElastic` (documento `Product`,
`@Document(indexName = "products")`), con un modelo optimizado para búsqueda de texto completo.

| Campo | Tipo Elasticsearch | Indexado | Descripción |
|-------|--------------------|:--------:|-------------|
| `id` | `keyword` | sí | Identificador del documento |
| `name` | `text` | sí | Nombre — analizado para *full-text* y *fuzzy search* |
| `brand` | `text` | sí | Marca — analizada para búsqueda |
| `category` | `text` | sí | Categoría — analizada para búsqueda |
| `short_description` | `object` | no (`enabled = false`) | Descripción corta (solo almacenada) |
| `long_description` | `object` | no (`enabled = false`) | Descripción larga (solo almacenada) |
| `price` | `double` | sí | Precio |
| `image_url` | `object` | no (`enabled = false`) | URL de la imagen (solo almacenada) |

> Solo los campos `name`, `brand` y `category` se indexan como `text` para participar en la búsqueda
> `multi_match` con tolerancia a errores (ver §5); el resto se almacena sin analizar para reducir el
> tamaño del índice.

---

## 9. Infraestructura de despliegue

### 9.1. Contenedorización

- Cada microservicio incluye su propio **`Dockerfile`**.
- El fichero **`docker-compose.yml`** orquesta el entorno de desarrollo local completo (Eureka,
  Gateway, User Service, Product Service Elastic, Order Service y las bases PostgreSQL de usuarios y
  pedidos) sobre una red bridge dedicada (`microservice-net`). `ProductServiceElastic` se conecta
  desde ahí al clúster de Elasticsearch gestionado externamente en Bonsai.

### 9.2. Despliegue en la nube (Azure Container Apps)

El script **`deploy-azure-tienda-app.sh`** automatiza el despliegue sobre **Azure Container Apps**:

- Creación del *resource group* (`rg-unir-tienda-app`), el entorno de Container Apps y un
  **Log Analytics Workspace** para la centralización de logs y la monitorización.
- Asignación de recursos por contenedor (0.75 vCPU, 1.5 GiB de memoria).
- **Topología de red diferenciada:** *ingress* externo únicamente para el Gateway; *ingress* interno
  para Eureka, User Service, Product Service y Order Service (no expuestos públicamente de forma
  directa).
- Imágenes publicadas en **Docker Hub** (`osgol/*`).

### 9.3. Servicios gestionados externos

- **PostgreSQL** alojado en **Railway**.
- **Elasticsearch** alojado en **Bonsai**.
- La configuración se externaliza mediante **variables de entorno** (puertos, credenciales de base de
  datos, *hosts* de Elasticsearch y el secreto de firma JWT `JWT_SECRET`, compartido entre el Gateway
  y el User Service), separando configuración y código conforme a la metodología *Twelve-Factor App*.

---

## 10. Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Lenguaje y plataforma** | Java 21 |
| **Framework base** | Spring Boot 3.5.4 |
| **Ecosistema distribuido** | Spring Cloud 2025.0.0 — Gateway, Netflix Eureka, OpenFeign |
| **Persistencia relacional** | Spring Data JPA, Hibernate, PostgreSQL |
| **Seguridad y autenticación** | Spring Security, JWT (JSON Web Tokens), BCrypt |
| **Motor de búsqueda** | Spring Data Elasticsearch, Elasticsearch 7.10.2 (Bonsai) |
| **Frontend** | React 19, React Router 7 |
| **Utilidades** | Lombok, Jakarta Validation, Spring Boot Actuator |
| **Pruebas backend** | JUnit 5, Mockito, Spring Security Test, JaCoCo (cobertura) |
| **Pruebas frontend** | Vitest, Testing Library (React), jsdom |
| **Infraestructura** | Docker, Docker Compose, Azure Container Apps, Railway, Log Analytics |

---

## 11. Catálogo de endpoints (API REST)

Todas las peticiones se realizan a través del Gateway (`:8762`), que antepone el nombre del servicio a
la ruta (p. ej. `/productservice/products`). Los endpoints marcados con 🔒 requieren la cookie de
sesión `token` (emitida por `UserService` y enviada automáticamente por el navegador); solo los tres
endpoints de `/auth` listados a continuación quedan exentos de esa validación en el Gateway.

### Usuarios y autenticación

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `POST` | `/auth/register` | Registro de un nuevo usuario; entrega la cookie `token` |
| `POST` | `/auth/login` | Inicio de sesión; entrega la cookie `token` |
| `POST` | `/auth/logout` | Cierre de sesión; invalida la cookie `token` |
| `GET` | `/auth/me` 🔒 | Perfil del usuario autenticado |

### Productos

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `GET` | `/products` 🔒 | Listado de productos (parámetro opcional `?search`) |
| `GET` | `/products/{id}` 🔒 | Consulta de un producto por identificador |
| `POST` | `/products` 🔒 | Alta de un nuevo producto |
| `PUT` | `/products/{id}` 🔒 | Actualización de un producto |
| `DELETE` | `/products/{id}` 🔒 | Baja de un producto |
| `GET` | `/products/health` 🔒 | Verificación de estado del servicio |

### Pedidos

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `POST` | `/orders` 🔒 | Creación de un pedido para el usuario autenticado |
| `GET` | `/orders` 🔒 | Listado de los pedidos del usuario autenticado |
| `GET` | `/orders/{id}` 🔒 | Consulta de un pedido por identificador |

### Infraestructura

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `GET` | `/actuator/gateway/routes` | Inspección de las rutas activas del Gateway |

---

## 12. Pruebas automatizadas y calidad

### 12.1. Backend

Cada microservicio de negocio cuenta con **pruebas unitarias** (JUnit 5 + Mockito) que cubren sus
capas principales, con **JaCoCo** integrado en el ciclo de vida de Maven para el reporte de cobertura:

| Servicio | Cobertura de pruebas |
|----------|-----------------------|
| `GatewayService` | Filtro de autenticación (`AuthenticationFilterTest`): rutas públicas, token ausente/ inválido/expirado, propagación de `X-User-Id`. |
| `UserService` | Controlador de autenticación, servicio de autenticación (registro, login, credenciales inválidas), emisión/validación de JWT (`JwtServiceTest`) y manejador global de excepciones. |
| `ProductServiceElastic` | Controlador de productos, servicio de productos, filtro de validación de JWT propio y manejador global de excepciones (incluida la caída de Elasticsearch). |
| `OrderService` | Controlador de órdenes, servicio de órdenes (incluida la integración vía Feign con el catálogo), filtro de validación de JWT propio y manejador global de excepciones. |

### 12.2. Frontend

El frontend (`tienda-react`) incorpora pruebas unitarias y de integración de componentes con
**Vitest** y **Testing Library**, ejecutables con `npm test` (`npm run test:coverage` para el reporte
de cobertura). Cubren, entre otros:

- **Componentes de UI:** `Header`, `Footer`, `NavLinks`, `UserMenu`, `CartLink`, `SearchForm`,
  `ProductCard`, `Notification`, `ErrorMessage`, `Copyright`, `ProtectedRoute`.
- **Páginas:** login, registro, catálogo, detalle de producto, carrito, listado y detalle de pedidos,
  contacto y política de devoluciones.
- **Lógica de datos:** hooks de acceso a la API (`useProduct`, `useProducts`, `useOrder`, `useOrders`),
  el contexto de autenticación (`AuthContext`) y la configuración del cliente HTTP (`config/api`).

### 12.3. Verificación manual de la API

- **Colecciones Postman** para la verificación de la API:
  - `gateway-collection.postman_collection.json` — pruebas *end-to-end* a través del Gateway.
  - `Elasticsearch.postman_collection.json` — operaciones directas sobre Elasticsearch.
- **Health checks** y **Spring Boot Actuator** para la monitorización operativa.
- **Lombok** para la reducción de código repetitivo y **Jakarta Validation** para la validación de
  datos de entrada.

---

## 13. Ejecución del proyecto

### Entorno local con Docker Compose

```bash
docker-compose up --build
```

Esto levanta el registro de servicios, el gateway, los servicios de usuario, producto (conectado a
Elasticsearch en Bonsai) y pedido, y las bases de datos PostgreSQL de usuarios y pedidos, sobre la red
`microservice-net`.

### Despliegue en Azure

```bash
./deploy-azure-tienda-app.sh
```

El script provisiona y despliega la totalidad del backend sobre Azure Container Apps.

---

> **Repositorio:** aplicación de tienda online desarrollada con React, microservicios Spring Boot,
> base de datos PostgreSQL, motor de búsqueda Elasticsearch y autenticación basada en JWT.
