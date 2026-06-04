# Tienda App — Aplicación de Comercio Electrónico basada en Microservicios

> Documentación técnica del proyecto

---

## Resumen

**Tienda App** es una aplicación de comercio electrónico desarrollada bajo una **arquitectura de
microservicios**. El sistema implementa la separación de dominios de negocio (catálogo de productos
y gestión de pedidos), un motor de búsqueda de texto completo basado en Elasticsearch, mecanismos de
descubrimiento dinámico de servicios y enrutamiento centralizado, así como una infraestructura de
despliegue contenedorizada y orientada a la nube.

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
7. [Infraestructura de despliegue](#7-infraestructura-de-despliegue)
8. [Stack tecnológico](#8-stack-tecnológico)
9. [Catálogo de endpoints (API REST)](#9-catálogo-de-endpoints-api-rest)
10. [Herramientas de soporte y calidad](#10-herramientas-de-soporte-y-calidad)
11. [Ejecución del proyecto](#11-ejecución-del-proyecto)

---

## 1. Introducción y objetivos

El proyecto persigue la implementación de una tienda en línea funcional aplicando los principios de
diseño de sistemas distribuidos. Sus objetivos técnicos son:

- **Desacoplamiento de dominios** mediante microservicios independientes, cada uno con su propia
  base de datos (*patrón database-per-service*).
- **Escalabilidad horizontal** apoyada en descubrimiento dinámico de servicios.
- **Búsqueda avanzada de productos** con tolerancia a errores tipográficos (*fuzzy search*).
- **Despliegue reproducible** mediante contenedores y automatización en la nube.

---

## 2. Arquitectura general del sistema

El sistema sigue un patrón de microservicios con una **puerta de entrada única** (*API Gateway*) y un
**registro de servicios** (*Service Registry*). El frontend en React consume la API exclusivamente a
través del Gateway.

```
                         ┌─────────────────────────┐
                         │   Frontend (React)       │
                         │   tienda-react           │
                         └────────────┬────────────┘
                                      │  HTTP / CORS
                                      ▼
                         ┌─────────────────────────┐
                         │  Gateway Service  :8762  │
                         │  (Spring Cloud Gateway)  │
                         └────────────┬────────────┘
                                      │  Enrutamiento dinámico (Eureka)
                  ┌───────────────────┼───────────────────┐
                  ▼                                        ▼
        ┌───────────────────┐                   ┌───────────────────┐
        │ Product Service   │                   │  Order Service    │
        │      :8081        │◄──── OpenFeign ────│      :8082        │
        │ (PostgreSQL /     │                   │  (PostgreSQL)     │
        │  Elasticsearch)   │                   └───────────────────┘
        └───────────────────┘
                  ▲                                        ▲
                  │       Registro / descubrimiento        │
                  └──────────────────┬─────────────────────┘
                                     ▼
                         ┌─────────────────────────┐
                         │ Discovery Service :8761  │
                         │   (Netflix Eureka)       │
                         └─────────────────────────┘
```

| Servicio | Puerto | Rol arquitectónico |
|----------|:------:|--------------------|
| `DiscoveryService` | 8761 | Registro y descubrimiento de servicios |
| `GatewayService` | 8762 | Puerta de entrada y enrutamiento |
| `ProductService` / `ProductServiceElastic` | 8081 | Dominio de catálogo |
| `OrderService` | 8082 | Dominio de pedidos |

---

## 3. Separación de dominios

La aplicación descompone la lógica de negocio en **dominios independientes**, cada uno encapsulado en
un microservicio con su propio modelo de datos y almacenamiento. Esta separación garantiza que un
dominio pueda evolucionar, escalar y desplegarse sin afectar a los demás.

- **Aislamiento de datos:** el dominio de pedidos (`orders_db`) no comparte esquema con el dominio de
  productos (`products_db`); la única vía de acceso a datos ajenos es a través de la API del servicio
  propietario.
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
- Estados del pedido modelados mediante enumeración (`OrderStatus`).
- **Integración con el catálogo** mediante cliente OpenFeign (ver §6).
- Persistencia en **PostgreSQL** independiente (`orders_db`).
- Puerto: `8082`.

### 4.5. Product Service Elastic (Catálogo — versión con motor de búsqueda)

Implementación alternativa del catálogo que sustituye la persistencia relacional por **Elasticsearch**
(ver §5 para el detalle del motor de búsqueda). Es **intercambiable** con `ProductService`: ambos se
registran en Eureka con el mismo nombre lógico (`productservice`) y exponen la misma API, por lo que el
resto del sistema es agnóstico a la implementación de búsqueda subyacente.

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

---

## 7. Infraestructura de despliegue

### 7.1. Contenedorización

- Cada microservicio incluye su propio **`Dockerfile`**.
- El fichero **`docker-compose.yml`** orquesta el entorno de desarrollo local completo (Eureka,
  Gateway, Product Service, Order Service y PostgreSQL) sobre una red bridge dedicada
  (`microservice-net`).

### 7.2. Despliegue en la nube (Azure Container Apps)

El script **`deploy-azure-tienda-app.sh`** automatiza el despliegue sobre **Azure Container Apps**:

- Creación del *resource group* (`rg-unir-tienda-app`), el entorno de Container Apps y un
  **Log Analytics Workspace** para la centralización de logs y la monitorización.
- Asignación de recursos por contenedor (0.75 vCPU, 1.5 GiB de memoria).
- **Topología de red diferenciada:** *ingress* externo únicamente para el Gateway; *ingress* interno
  para Eureka, Product Service y Order Service (no expuestos públicamente de forma directa).
- Imágenes publicadas en **Docker Hub** (`osgol/*`).

### 7.3. Servicios gestionados externos

- **PostgreSQL** alojado en **Railway**.
- **Elasticsearch** alojado en **Bonsai**.
- La configuración se externaliza mediante **variables de entorno** (puertos, credenciales de base de
  datos, *hosts* de Elasticsearch), separando configuración y código conforme a la metodología
  *Twelve-Factor App*.

---

## 8. Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Lenguaje y plataforma** | Java 21 |
| **Framework base** | Spring Boot 3.5.4 |
| **Ecosistema distribuido** | Spring Cloud 2025.0.0 — Gateway, Netflix Eureka, OpenFeign |
| **Persistencia relacional** | Spring Data JPA, Hibernate, PostgreSQL |
| **Motor de búsqueda** | Spring Data Elasticsearch, Elasticsearch 7.10.2 (Bonsai) |
| **Frontend** | React |
| **Utilidades** | Lombok, Jakarta Validation, Spring Boot Actuator |
| **Infraestructura** | Docker, Docker Compose, Azure Container Apps, Railway, Log Analytics |

---

## 9. Catálogo de endpoints (API REST)

Todas las peticiones se realizan a través del Gateway (`:8762`), que antepone el nombre del servicio a
la ruta (p. ej. `/productservice/products`).

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
| `POST` | `/orders` | Creación de un pedido |
| `GET` | `/orders` | Listado de pedidos |
| `GET` | `/orders/{id}` | Consulta de un pedido por identificador |

### Infraestructura

| Método | Ruta | Descripción |
|:------:|------|-------------|
| `GET` | `/actuator/gateway/routes` | Inspección de las rutas activas del Gateway |

---

## 10. Herramientas de soporte y calidad

- **Colecciones Postman** para la verificación de la API:
  - `gateway-collection.postman_collection.json` — pruebas *end-to-end* a través del Gateway.
  - `Elasticsearch.postman_collection.json` — operaciones directas sobre Elasticsearch.
- **Health checks** y **Spring Boot Actuator** para la monitorización operativa.
- **Lombok** para la reducción de código repetitivo y **Jakarta Validation** para la validación de
  datos de entrada.

---

## 11. Ejecución del proyecto

### Entorno local con Docker Compose

```bash
docker-compose up --build
```

Esto levanta el registro de servicios, el gateway, los servicios de producto y pedido, y la base de
datos PostgreSQL sobre la red `microservice-net`.

### Despliegue en Azure

```bash
./deploy-azure-tienda-app.sh
```

El script provisiona y despliega la totalidad del backend sobre Azure Container Apps.

---

> **Repositorio:** aplicación de tienda online desarrollada con React, microservicios Spring Boot,
> base de datos PostgreSQL y motor de búsqueda Elasticsearch.
