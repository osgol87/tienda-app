# ¿Cómo se gestionan los datos?

## Contenido

### Modelo de persistencia

El sistema aplica el patrón *database-per-service*: cada microservicio es dueño de su propio almacén y nadie consulta el esquema de otro directamente. Cuando OrderService necesita datos del catálogo, no lee la tabla de productos — llama a ProductService por HTTP a través de un cliente OpenFeign. Esta decisión aísla dominios (usuarios, pedidos, catálogo) y permite que cada uno evolucione sin arrastrar a los demás.

Vale la pena señalar que ese aislamiento es lógico, no físico. En el entorno local con Docker Compose las tres bases de PostgreSQL (`users_db`, `orders_db`, `products_db`) corren dentro del mismo contenedor de servidor (`postgres-container`). Un problema de recursos en esa instancia —disco lleno, conexiones agotadas— afectaría a los tres dominios al mismo tiempo, algo que contradice la promesa de independencia que el patrón sugiere sobre el papel.

### PostgreSQL

Dos servicios usan PostgreSQL de forma activa: UserService (`users_db`) y OrderService (`orders_db`). El esquema no se define en scripts de migración: se genera automáticamente a partir de las entidades JPA mediante la propiedad `hibernate.ddl-auto`, configurable por variable de entorno pero con el valor `create` como predeterminado en ambos servicios. Esto significa que, tal como está configurado hoy, cada reinicio de la aplicación borra y vuelve a crear el esquema. Para una etapa de desarrollo académico es tolerable; para una tienda con pedidos reales de clientes, es un defecto que hay que corregir antes de pensar en producción, y el repositorio no incluye ninguna herramienta de migración (Flyway, Liquibase) que documente cambios de esquema o permita revertirlos.

La configuración de conexión sí sigue una buena práctica: usuario, contraseña y URL se externalizan por variables de entorno, con valores locales de respaldo apuntando a un PostgreSQL en `localhost:5432`. En el despliegue en la nube, la base vive en Railway.

### Organización de la información

Los datos se organizan por dominio de negocio, no por tabla compartida. Dentro de OrderService esto se ve con claridad en `OrderItem`: en lugar de apoyarse solo en el identificador del producto, la línea de pedido guarda una copia (*snapshot*) del nombre, la imagen y el precio unitario tomados en el momento de la compra. Es una redundancia deliberada: si el producto cambia de precio o se elimina del catálogo más adelante, el historial del pedido no se altera. Es el comportamiento esperado en comercio electrónico, donde el recibo de una compra no debe cambiar retroactivamente.

La contraparte de esa separación es que las relaciones entre dominios dejan de ser relaciones de base de datos. `Order.userId` y `OrderItem.productId` se guardan como texto (`VARCHAR`), no como llaves foráneas verificables por PostgreSQL, porque el usuario y el producto viven en bases distintas. La integridad de esos vínculos depende por completo del código de aplicación —del encabezado `X-User-Id` que propaga el Gateway tras validar el JWT, y de la respuesta que Feign obtiene de ProductService al construir el pedido—, no de una restricción que la base de datos pueda hacer cumplir. Es una limitación estructural de este tipo de arquitectura, no un descuido, pero conviene nombrarla como tal en lugar de darla por sentada.

### Diagrama Entidad-Relación

*(Espacio para insertar el diagrama elaborado en draw.io.)*

Antes de leer el diagrama, conviene aclarar qué representa: no hay un esquema relacional único, porque cada servicio conserva el suyo por separado y el dominio de catálogo, además, se reparte entre dos motores distintos según la implementación activa (PostgreSQL o Elasticsearch). El diagrama muestra en realidad tres bloques desconectados entre sí, con las relaciones que cruzan esa frontera marcadas como referencias lógicas y no como llaves foráneas:

- `users` (1) — `orders` (N), vinculadas por `user_id`, sin restricción de base de datos.
- `orders` (1) — `order_items` (N), esta sí es una llave foránea real (`order_id`), reforzada con `cascade = ALL` y `orphanRemoval = true`: al borrar una orden se borran sus líneas.
- `order_items` — `products`, vínculo resuelto en el momento de escritura vía Feign, sin restricción posterior; de ahí la necesidad del *snapshot* mencionado antes.

### Tablas principales de la base de datos

*Tabla `users` (UserService).* La entidad real (`User.java`) define `id`, `username`, `email`, `password`, `role` y `created_at`. Vale la pena mencionar que el propio README del proyecto documenta esta tabla con una columna `name` que no existe en el código —el campo real se llama `username`— y omite `created_at` por completo. Es un detalle menor, pero en un informe sobre gestión de datos es justo el tipo de cosa que hay que señalar: ni la documentación interna del proyecto coincide todavía con lo que el código realmente persiste.

*Tabla `orders` (OrderService).* `id`, `user_id`, `order_date`, `status` (enumeración `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`) y `total_amount` con precisión decimal (10,2), adecuada para manejar montos en pesos sin errores de redondeo binario.

*Tabla `order_items` (OrderService).* `id`, `order_id` (llave foránea), `product_id` (indexado), `name`, `image_url`, `quantity`, `price_per_unit` y `subtotal` — estos tres últimos son los campos de *snapshot* descritos arriba.

*Catálogo activo (`ProductServiceElastic`).* No es una tabla sino un índice de Elasticsearch (`products`), con documentos que incluyen `id`, `name`, `brand`, `category`, `short_description`, `long_description`, `price` e `image_url`. Solo `name`, `brand` y `category` se indexan como texto analizado; el resto se almacena sin analizar, lo que reduce el tamaño del índice pero también significa que esos campos no participan en la búsqueda.

## Análisis

La separación por dominio está bien pensada para el propósito del proyecto y se sostiene de forma consistente en el código, no solo en el discurso arquitectónico. El uso de BCrypt para contraseñas, la externalización de credenciales por variable de entorno y el manejo del total de la orden como `NUMERIC(10,2)` en lugar de tipos de punto flotante son decisiones correctas para un sistema que maneja dinero y credenciales.

Los puntos débiles están en la operación, no en el diseño. El valor `create` en `hibernate.ddl-auto` es aceptable para una demostración, pero incompatible con cualquier uso real: cualquier despliegue nuevo pierde los datos existentes. La ausencia de una herramienta de migración agrava el problema, porque no hay forma de versionar cambios de esquema ni de auditar cómo llegó la base de datos a su estado actual. Además, el archivo `application.yml` de `ProductServiceElastic` contiene, como valores por defecto literales dentro del repositorio, las credenciales reales de la instancia de Elasticsearch alojada en Bonsai —no solo el nombre de la variable de entorno que las contendría—. Esto es una exposición de credenciales que conviene resolver antes de compartir el repositorio o de continuar el desarrollo, independientemente de este informe.

Sobre los datos personales: `users` guarda correo y usuario en claro y contraseña cifrada, sin ningún campo que evidencie consentimiento, aviso de privacidad o política de retención. Para una tienda operando en México, el correo electrónico y el nombre de usuario ya constituyen datos personales sujetos a la Ley Federal de Protección de Datos Personales en Posesión de los Particulares, que exige informar al titular sobre el uso de sus datos mediante un aviso de privacidad y justificar cuánto tiempo se conservan. El esquema actual no tiene forma de responder a una solicitud de cancelación o eliminación de datos sin borrar también el historial de pedidos asociado, porque no existe ningún mecanismo de anonimización ni de baja lógica —ni en `users` ni en `orders`—. A eso se suma que `orphanRemoval = true` en `OrderItem`, combinado con el borrado de esquema en cada arranque, apunta en la dirección contraria a la que exigiría una eventual obligación fiscal: los registros de una venta, una vez facturados ante el SAT mediante CFDI, deben conservarse de forma íntegra y no depender de que nadie reinicie el servicio con la configuración por defecto.

## Transición

El estado actual del repositorio muestra un cambio de fondo en el dominio de catálogo: `ProductService`, la versión relacional que guardaba productos en `products_db` sobre PostgreSQL y resolvía la búsqueda con consultas `LIKE`, ha sido eliminada por completo del código —no marcada como obsoleta, sino retirada—, mientras que `ProductServiceElastic` queda como la única implementación activa.

La razón del cambio es concreta: una búsqueda con `LIKE` no tolera errores de tipografía ni ofrece autocompletado, y el catálogo de una tienda de zapatillas depende de que el cliente encuentre el producto aunque escriba mal la marca. Elasticsearch resuelve eso de forma nativa con `multi_match` de tipo `bool_prefix` y un parámetro de tolerancia a errores (`fuzziness: 1`). Los dos servicios se registran bajo el mismo nombre lógico en Eureka (`productservice`) y exponen la misma API REST, así que el resto del sistema —Gateway, OrderService, el frontend— no necesitó cambios para aceptar el nuevo motor. Es una migración bien ejecutada en el límite de la API.

Lo que no es gratuito es el cambio en el perfil de riesgo del sistema. Antes, el catálogo dependía de una base de datos local, bajo control del propio despliegue. Ahora depende de un clúster de Elasticsearch gestionado por un tercero (Bonsai), alcanzado por internet con credenciales que, como se señaló antes, están además expuestas en el repositorio. Una interrupción de red hacia Bonsai deja sin catálogo a toda la tienda, algo que no ocurría con la versión relacional local. Y al haber eliminado por completo el código de `ProductService` en lugar de conservarlo como alternativa, el proyecto no tiene ya un camino de reversa si esa dependencia externa falla o cambia de condiciones comerciales.
