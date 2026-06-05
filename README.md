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
7. [Bases de datos y modelo de datos](#7-bases-de-datos-y-modelo-de-datos)
8. [Infraestructura de despliegue](#8-infraestructura-de-despliegue)
9. [Stack tecnológico](#9-stack-tecnológico)
10. [Catálogo de endpoints (API REST)](#10-catálogo-de-endpoints-api-rest)
11. [Herramientas de soporte y calidad](#11-herramientas-de-soporte-y-calidad)
12. [Ejecución del proyecto](#12-ejecución-del-proyecto)

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

```
                         ┌─────────────────────────┐
                         │   Frontend (React)      │
                         │   tienda-react          │
                         └────────────┬────────────┘
                                      │  HTTP / CORS + JWT (Bearer)
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
  │      :8083        │    │      :8081        │◄─ Feign ─│      :8082   │
  │ (PostgreSQL,      │    │ (PostgreSQL /     │     │  (PostgreSQL)     │
  │  JWT + BCrypt)    │    │  Elasticsearch)   │     └───────────────────┘
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
| `ProductService` / `ProductServiceElastic` | 8081 | Dominio de catálogo |
| `OrderService` | 8082 | Dominio de pedidos |

---

## 3. Separación de dominios

La aplicación descompone la lógica de negocio en **dominios independientes**, cada uno encapsulado en
un microservicio con su propio modelo de datos y almacenamiento. Esta separación garantiza que un
dominio pueda evolucionar, escalar y desplegarse sin afectar a los demás.

- **Aislamiento de datos:** el dominio de pedidos (`orders_db`), el de productos (`products_db`) y el
  de usuarios (`users_db`) no comparten esquema entre sí; la única vía de acceso a datos ajenos es a
  través de la API del servicio propietario.
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

### 4.3. Product Service (Catálogo — versión relacional)

Microservicio de gestión del catálogo de productos con persistencia relacional.

- **CRUD completo** de productos a través de una API REST.
- Modelo de producto: identificador, nombre, marca, categoría, descripción corta, descripción larga,
  precio (`BigDecimal`) e imagen.
- **Búsqueda relacional** mediante consulta JPA personalizada con coincidencias parciales (`LIKE`)
  sobre nombre, marca y categoría.
- Persistencia en **PostgreSQL** mediante Spring Data JPA / Hibernate.
- Puerto: `8081`.

### 4.4. Order Service (Pedidos)

Microservicio de gestión de pedidos del cliente.

- Creación y consulta de órdenes a través de una API REST.
- Modelo de orden compuesto por **líneas de pedido** (`OrderItem`): cantidad, precio unitario,
  subtotal y *snapshot* del nombre e imagen del producto en el momento de la compra.
- **Asociación al usuario autenticado:** cada orden registra el identificador del usuario propietario,
  obtenido de la cabecera `X-User-Id` que el Gateway propaga tras validar el JWT (ver §6). Esto
  habilita la consulta de los pedidos propios del cliente ("mis pedidos").
- Estados del pedido modelados mediante enumeración (`OrderStatus`).
- **Integración con el catálogo** mediante cliente OpenFeign (ver §6).
- Persistencia en **PostgreSQL** independiente (`orders_db`).
- Puerto: `8082`.

### 4.5. Product Service Elastic (Catálogo — versión con motor de búsqueda)

Implementación alternativa del catálogo que sustituye la persistencia relacional por **Elasticsearch**
(ver §5 para el detalle del motor de búsqueda). Es **intercambiable** con `ProductService`: ambos se
registran en Eureka con el mismo nombre lógico (`productservice`) y exponen la misma API, por lo que el
resto del sistema es agnóstico a la implementación de búsqueda subyacente.

### 4.6. User Service (Usuarios y autenticación)

Microservicio responsable de la gestión de usuarios y de la **autenticación stateless** del sistema.

- **Registro de usuarios** (`POST /auth/register`) e **inicio de sesión** (`POST /auth/login`) a
  través de una API REST.
- Modelo de usuario: identificador, correo electrónico (único), contraseña, nombre y rol.
- **Almacenamiento seguro de credenciales:** las contraseñas se cifran con **BCrypt**; nunca se
  persisten ni se devuelven en texto plano.
- **Emisión de JSON Web Tokens (JWT):** tras un inicio de sesión correcto, el servicio genera un token
  firmado (con un secreto `JWT_SECRET` compartido con el Gateway) que el cliente adjunta en la
  cabecera `Authorization: Bearer <token>` de las peticiones posteriores.
- **Seguridad con Spring Security:** configuración *stateless* (sin sesión de servidor), con los
  endpoints de registro e inicio de sesión expuestos públicamente y el resto protegidos.
- Consulta del perfil del usuario autenticado mediante `GET /users/me`.
- Persistencia en **PostgreSQL** independiente (`users_db`).
- Puerto: `8083`.

### 4.7. Frontend (tienda-react)

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

1. El cliente se autentica contra el `UserService` (`POST /auth/login`) y recibe un **token JWT**
   firmado.
2. En cada petición posterior, el cliente adjunta el token en la cabecera
   `Authorization: Bearer <token>`.
3. El **Gateway valida la firma y la vigencia del token** mediante un filtro global. Las rutas
   públicas (registro e inicio de sesión, y la consulta de productos) se permiten sin token.
4. Tras validar el token, el Gateway **extrae la identidad del usuario y la propaga** a los servicios
   internos a través de la cabecera `X-User-Id`, de modo que los servicios de negocio (p. ej.
   `OrderService`) confían en dicha cabecera para asociar las operaciones al usuario.

---

## 7. Bases de datos y modelo de datos

El sistema aplica el patrón **database-per-service**: cada microservicio es el único propietario de su
almacén de datos y nadie accede directamente al esquema de otro servicio (el acceso siempre es a
través de su API). Conviven dos tecnologías de persistencia: **PostgreSQL** (relacional) para los
dominios de usuarios, pedidos y la versión relacional del catálogo, y **Elasticsearch** (documental)
para la versión del catálogo orientada a búsqueda.

| Servicio | Tecnología | Almacén | Esquema / Índice |
|----------|------------|---------|------------------|
| `UserService` | PostgreSQL | `users_db` | tabla `users` |
| `OrderService` | PostgreSQL | `orders_db` | tablas `orders`, `order_items` |
| `ProductService` | PostgreSQL | `products_db` | tabla `products` |
| `ProductServiceElastic` | Elasticsearch | índice `products` | documento `Product` |

> **Nota:** en el entorno local con Docker Compose, los servicios PostgreSQL comparten una misma
> instancia de servidor (`postgres-container`); el aislamiento lógico se mantiene a nivel de dominio.
> El esquema se genera automáticamente a partir de las entidades JPA mediante la propiedad
> `hibernate.ddl-auto` (parametrizable por servicio, p. ej. `ORDERSERVICE_DDL_AUTO`).

### 7.1. Dominio de usuarios (`users_db`)

Tabla **`users`** — gestionada por `UserService`.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `BIGSERIAL` (PK) | Identificador del usuario |
| `email` | `VARCHAR` (único) | Correo electrónico, usado como credencial de acceso |
| `password` | `VARCHAR` | Contraseña cifrada con **BCrypt** (nunca en texto plano) |
| `name` | `VARCHAR` | Nombre del usuario |
| `role` | `VARCHAR` | Rol del usuario para autorización |

### 7.2. Dominio de pedidos (`orders_db`)

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

### 7.3. Dominio de catálogo — versión relacional (`products_db`)

Tabla **`products`** — gestionada por `ProductService` (entidad `Product`).

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | `BIGSERIAL` (PK) | Identificador del producto |
| `name` | `VARCHAR` | Nombre del producto |
| `brand` | `VARCHAR` | Marca |
| `category` | `VARCHAR` | Categoría |
| `short_description` | `VARCHAR` | Descripción corta |
| `long_description` | `VARCHAR` | Descripción larga |
| `price` | `NUMERIC(10,2)` | Precio |
| `image_url` | `VARCHAR` | URL de la imagen |

### 7.4. Dominio de catálogo — versión documental (índice `products`)

Índice **`products`** de Elasticsearch — gestionado por `ProductServiceElastic` (documento `Product`,
`@Document(indexName = "products")`). Es **intercambiable** con la versión relacional (misma API y
mismo nombre lógico en Eureka), pero su modelo está optimizado para búsqueda de texto completo.

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

## 8. Infraestructura de despliegue

### 8.1. Contenedorización

- Cada microservicio incluye su propio **`Dockerfile`**.
- El fichero **`docker-compose.yml`** orquesta el entorno de desarrollo local completo (Eureka,
  Gateway, User Service, Product Service, Order Service y PostgreSQL) sobre una red bridge dedicada
  (`microservice-net`).

### 8.2. Despliegue en la nube (Azure Container Apps)

El script **`deploy-azure-tienda-app.sh`** automatiza el despliegue sobre **Azure Container Apps**:

- Creación del *resource group* (`rg-unir-tienda-app`), el entorno de Container Apps y un
  **Log Analytics Workspace** para la centralización de logs y la monitorización.
- Asignación de recursos por contenedor (0.75 vCPU, 1.5 GiB de memoria).
- **Topología de red diferenciada:** *ingress* externo únicamente para el Gateway; *ingress* interno
  para Eureka, User Service, Product Service y Order Service (no expuestos públicamente de forma
  directa).
- Imágenes publicadas en **Docker Hub** (`osgol/*`).

### 8.3. Servicios gestionados externos

- **PostgreSQL** alojado en **Railway**.
- **Elasticsearch** alojado en **Bonsai**.
- La configuración se externaliza mediante **variables de entorno** (puertos, credenciales de base de
  datos, *hosts* de Elasticsearch y el secreto de firma JWT `JWT_SECRET`, compartido entre el Gateway
  y el User Service), separando configuración y código conforme a la metodología *Twelve-Factor App*.

---

## 9. Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Lenguaje y plataforma** | Java 21 |
| **Framework base** | Spring Boot 3.5.4 |
| **Ecosistema distribuido** | Spring Cloud 2025.0.0 — Gateway, Netflix Eureka, OpenFeign |
| **Persistencia relacional** | Spring Data JPA, Hibernate, PostgreSQL |
| **Seguridad y autenticación** | Spring Security, JWT (JSON Web Tokens), BCrypt |
| **Motor de búsqueda** | Spring Data Elasticsearch, Elasticsearch 7.10.2 (Bonsai) |
| **Frontend** | React |
| **Utilidades** | Lombok, Jakarta Validation, Spring Boot Actuator |
| **Infraestructura** | Docker, Docker Compose, Azure Container Apps, Railway, Log Analytics |

---

## 10. Catálogo de endpoints (API REST)

Todas las peticiones se realizan a través del Gateway (`:8762`), que antepone el nombre del servicio a
la ruta (p. ej. `/productservice/products`). Los endpoints marcados con 🔒 requieren un token JWT
válido en la cabecera `Authorization: Bearer <token>`.

### Usuarios y autenticación

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `POST` | `/auth/register` | Registro de un nuevo usuario |
| `POST` | `/auth/login` | Inicio de sesión; devuelve el token JWT |
| `GET` | `/users/me` 🔒 | Perfil del usuario autenticado |

### Productos

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `GET` | `/products` | Listado de productos (parámetro opcional `?search`) |
| `GET` | `/products/{id}` | Consulta de un producto por identificador |
| `POST` | `/products` | Alta de un nuevo producto |
| `PUT` | `/products/{id}` | Actualización de un producto |
| `DELETE` | `/products/{id}` | Baja de un producto |
| `GET` | `/products/health` | Verificación de estado (solo versión Elastic) |

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

## 11. Herramientas de soporte y calidad

- **Colecciones Postman** para la verificación de la API:
  - `gateway-collection.postman_collection.json` — pruebas *end-to-end* a través del Gateway.
  - `Elasticsearch.postman_collection.json` — operaciones directas sobre Elasticsearch.
- **Health checks** y **Spring Boot Actuator** para la monitorización operativa.
- **Lombok** para la reducción de código repetitivo y **Jakarta Validation** para la validación de
  datos de entrada.

---

## 12. Ejecución del proyecto

### Entorno local con Docker Compose

```bash
docker-compose up --build
```

Esto levanta el registro de servicios, el gateway, los servicios de usuario, producto y pedido, y la
base de datos PostgreSQL sobre la red `microservice-net`.

### Despliegue en Azure

```bash
./deploy-azure-tienda-app.sh
```

El script provisiona y despliega la totalidad del backend sobre Azure Container Apps.

---

> **Repositorio:** aplicación de tienda online desarrollada con React, microservicios Spring Boot,
> base de datos PostgreSQL, motor de búsqueda Elasticsearch y autenticación basada en JWT.
