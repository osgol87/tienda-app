# Anexo — Guía básica de mantenimiento

> Documentación técnica del proyecto — Tienda App

---

## 1. Objetivo y alcance

Todas las cosas que funcionan necesitan cuidado. Una planta necesita agua, una bicicleta necesita que se
revisen sus llantas de vez en cuando, y un sistema de software necesita que alguien lo revise para saber
si sigue funcionando bien. A ese cuidado se le llama **mantenimiento**. Mantener un sistema significa
revisarlo seguido, darse cuenta a tiempo si algo anda mal, y corregirlo con calma antes de que el problema
crezca y afecte a las personas que lo usan.

Este anexo reúne, paso a paso y con lenguaje sencillo, las tareas de mantenimiento básico del sistema
**Tienda App**. Está pensado para que cualquier persona con acceso al repositorio del proyecto, con Docker
instalado y con conocimientos básicos de línea de comandos, pueda seguir cada paso sin necesidad de ser
experta en el sistema. No se requiere memorizar nada: basta con seguir la guía en orden y comprender qué
hace cada comando antes de escribirlo.

Concretamente, al terminar de leer y practicar esta guía, la persona encargada del mantenimiento podrá:

- Comprobar si cada servicio está encendido y funcionando correctamente.
- Leer los registros de actividad (*logs*) de un servicio para entender qué está pasando cuando algo falla.
- Reiniciar un servicio, o el sistema completo, sin causar daños.
- Aplicar un cambio de código nuevo y confirmar que el sistema lo recibió correctamente.
- Mantener al día el frontend.
- Cuidar que la información guardada en las bases de datos y en el buscador no se pierda por accidente.

El alcance de esta guía es el **entorno local**, es decir, el sistema completo funcionando en la propia
computadora de quien lo mantiene, organizado por el archivo `docker-compose.yml`. Se eligió este entorno
porque es aquel en el que cualquier persona puede repetir cada paso desde cero y comprobar el resultado con
sus propios ojos, sin depender de accesos ni de permisos especiales.

La idea general que guía toda la guía es sencilla: primero mirar con calma cómo está el sistema, después
entender qué está pasando si algo no se ve bien, y solo hasta entonces actuar —reiniciando, actualizando o
corrigiendo lo que haga falta—. Actuar sin antes revisar y entender es lo que más fácilmente puede convertir
un problema pequeño en uno grande. Por eso las secciones siguientes están ordenadas de esa misma manera:
primero se explica cómo verificar que todo esté bien, después cómo diagnosticar cuando algo no lo está, y
solo después de eso se explican los pasos para reiniciar, actualizar y cuidar la información del sistema.

## 2. Componentes sujetos a mantenimiento

Tienda App no es una sola pieza de software, sino varias piezas más pequeñas que trabajan en equipo, como
las piezas de un juguete armable: cada una hace un trabajo distinto, y para que el juguete completo funcione
bien, cada pieza debe estar en buen estado. A cada una de esas piezas se le llama **componente** o
**servicio**. Antes de aprender a revisarlos, reiniciarlos o actualizarlos, conviene saber cuáles son y qué
hace cada uno.

Casi todos estos componentes viven dentro de una especie de caja llamada **contenedor**. Un contenedor es
parecido a una lonchera: adentro lleva todo lo que ese programa necesita para funcionar, sin mezclarse con
lo que hay en el resto de la computadora. La herramienta que arma, enciende y organiza todas esas loncheras
al mismo tiempo se llama **Docker**, y el archivo que le indica qué loncheras crear y cómo conectarlas entre
sí es `docker-compose.yml`.

A continuación se explica, en palabras simples, qué hace cada componente:

- **Frontend (React).** Es la parte que las personas ven y usan: los botones, las imágenes de los tenis, el
  carrito de compras. No vive dentro de una lonchera de Docker; se enciende de forma directa con un
  programa llamado npm.
- **Gateway Service.** Es como el portero de una fiesta: todas las peticiones que llegan de afuera pasan
  primero por él, y él revisa que cada una tenga una sesión válida antes de dejarla continuar hacia el
  servicio que corresponde. Es la única puerta de entrada de todo el sistema.
- **Eureka Server.** Es como una libreta de direcciones: ahí se anota cada servicio para que los demás
  sepan dónde encontrarlo. Por eso debe encenderse primero: si la libreta todavía no existe, ningún
  servicio puede anotarse en ella.
- **User Service.** Se encarga de las cuentas de las personas: el registro, el inicio de sesión y los datos
  de cada usuario. Guarda esa información en su propia base de datos, llamada `users-db`.
- **Product Service.** Se encarga del catálogo: qué productos existen, de qué marca y a qué precio. Guarda
  esa información en un buscador especial llamado Elasticsearch, que no vive en la computadora local sino
  en un servicio externo llamado Bonsai.
- **Order Service.** Se encarga de los pedidos: qué compró cada persona y cuándo. Guarda esa información en
  su propia base de datos, llamada `orders-db`.
- **PostgreSQL (`users-db` y `orders-db`).** Son dos bases de datos construidas con PostgreSQL, un programa
  que se especializa en guardar información de forma ordenada y segura, como un archivero muy confiable.
- **Elasticsearch.** Es el buscador donde vive el catálogo de productos. No corre como una lonchera en la
  computadora local: vive en un servidor externo, así que no aparece cuando se listan los contenedores.
- **Docker y Docker Compose.** No son un servicio del negocio, sino la herramienta que enciende, organiza y
  conecta entre sí a casi todos los componentes anteriores, siguiendo las instrucciones escritas en
  `docker-compose.yml`.

La siguiente tabla resume, de forma rápida, cada componente y qué se debe tener en cuenta al darle
mantenimiento.

| Componente | Carpeta o imagen | Qué hay que saber |
|---|---|---|
| Frontend (React) | `tienda-react` | No corre dentro de Docker; se mantiene con `npm` (sección 7) |
| Gateway Service | `GatewayService` | Es la única puerta de entrada del sistema; revisa que cada petición tenga sesión válida |
| Eureka Server | `DiscoveryService` | Debe encender primero, porque los demás servicios se registran en él |
| User Service | `UserService` | Guarda sus datos en la base `users-db` |
| Product Service | `ProductServiceElastic` | Guarda el catálogo en el índice `products` de Elasticsearch (Bonsai) |
| Order Service | `OrderService` | Guarda sus datos en la base `orders-db` |
| PostgreSQL (`users-db`, `orders-db`) | imagen `postgres:17-alpine3.22` | Su información vive en volúmenes con nombre (sección 8) |
| Elasticsearch | servidor externo (Bonsai) | No es un contenedor local; no aparece al listar los contenedores |
| Docker y Docker Compose | `docker-compose.yml` | Enciende y organiza los seis servicios anteriores, salvo el frontend y Elasticsearch |

El resto de esta guía se basa en el entorno local que arma `docker-compose.yml`, porque es el único sobre
el que se puede mostrar evidencia real y verificable.

## 3. Verificación del estado de los servicios

Verificar el estado de los servicios es el primer paso de todo mantenimiento, y también el más sencillo:
consiste en preguntarle a la computadora cuáles componentes están encendidos y cuáles no. Es parecido a
pasar lista en un salón de clases: no hace falta saber todavía qué le pasa a cada quien, solo confirmar que
todos están presentes antes de continuar. Esta verificación debe repetirse siempre que el sistema se
enciende, después de cualquier reinicio o actualización, y de vez en cuando mientras el sistema está en
uso, aunque nadie haya reportado ningún problema.

Para saber si los contenedores están encendidos, se usan estos comandos:

```bash
docker ps -a
docker compose ps
```

El primer comando, `docker ps -a`, le pregunta a Docker por **todos** los contenedores que existen en la
computadora, estén encendidos o apagados (la letra `a` significa "todos", *all* en inglés). El segundo
comando, `docker compose ps`, pregunta algo parecido, pero solo sobre los contenedores que pertenecen a
este proyecto, es decir, los que están escritos en `docker-compose.yml`.

Cualquiera de los dos comandos responde con una tabla. La columna que más importa se llama `STATUS`
(estado). Si dice `Up`, seguido del tiempo que lleva encendido, el contenedor está funcionando con
normalidad. Si en cambio dice `Exited` (se apagó) o `Restarting` (se está reiniciando una y otra vez), ese
contenedor tiene un problema y hay que investigarlo, tal como se explica en la sección 4.

En un sistema sano, ambos comandos deben mostrar los siete contenedores definidos en
`docker-compose.yml` (`eureka-server-container`, `gateway-service-container`, `user-service-container`,
`product-service-container`, `order-service-container`, `users-db-container`, `orders-db-container`) con
estado `Up`.

Como segunda comprobación, se puede abrir el panel de **Eureka Server** en el navegador
(`http://localhost:8761`). Recordando la comparación de la sección anterior, Eureka es como una libreta de
direcciones: cada servicio, al encender, anota ahí su nombre para que los demás lo encuentren. En ese panel
deben aparecer registradas las cuatro aplicaciones: `GATEWAYSERVICE`, `USERSERVICE`, `PRODUCTSERVICE` y
`ORDERSERVICE`. Si un contenedor aparece como `Up` pero su nombre no aparece en esta lista, es probable que
el servicio haya encendido pero no haya logrado anotarse todavía en la libreta; en ese caso conviene
esperar unos segundos y volver a mirar, y si el problema continúa, revisar sus registros (sección 4).

*(En esta sección se pueden reutilizar las capturas ya tomadas para el Anexo B de `docker ps -a`, de
Docker Compose y del panel de Eureka, en lugar de generar evidencia nueva de forma artificial.)*

## 4. Consulta de registros para diagnóstico

Cuando la verificación de la sección anterior muestra que algo no anda bien —un contenedor que se reinicia
solo, o que no logra registrarse en Eureka—, no basta con saberlo: hace falta entender por qué está
pasando. Para eso sirven los **registros**, también llamados *logs*. Un registro es como un diario que
cada servicio va escribiendo mientras trabaja: ahí anota, línea por línea, lo que va haciendo y, sobre
todo, anota con detalle el momento exacto en que algo sale mal. Leer ese diario es la forma más confiable
de entender un problema, mucho más que solo mirar si el servicio está encendido o apagado.

Para ver los registros de todos los servicios del proyecto a la vez:

```bash
docker compose logs
docker compose logs -f            # muestra los registros nuevos en tiempo real
docker compose logs -f gateway-service
```

Para ver los registros de un solo contenedor, usando el nombre indicado en `docker-compose.yml`:

```bash
docker logs user-service-container
docker logs --tail 100 -f order-service-container
```

La diferencia entre estos dos grupos de comandos es sencilla: `docker compose logs` muestra el diario de
todos los servicios del proyecto juntos, cada línea identificada con el nombre del servicio que la escribió;
`docker logs` muestra el diario de un único contenedor, por lo que hace falta escribir su nombre exacto. La
opción `-f` (de *follow*, "seguir" en inglés) deja el diario abierto en la pantalla, mostrando cada línea
nueva a medida que el servicio la va escribiendo, en lugar de mostrar solo lo que ya ocurrió antes. La
opción `--tail 100` le pide al comando que muestre únicamente las últimas 100 líneas, para no tener que leer
el diario completo desde el principio.

Cuando el diario de un servicio es muy largo, ayuda buscar dentro de él una palabra clave en lugar de leerlo
entero. Por ejemplo:

```bash
docker compose logs order-service | grep -i error
```

Este comando muestra únicamente las líneas del diario de `order-service` donde aparece la palabra "error",
sin importar si está escrita con mayúsculas o minúsculas.

Además de saber leer un diario en general, ayuda conocer de antemano qué buscar en cada servicio, porque el
sistema tiene algunas particularidades reales que conviene tener presentes:

- **User Service y Order Service** no tienen una página especial de estado (no incluyen la herramienta
  Actuator). Por eso, los registros y el panel de Eureka son, en la práctica, la mejor manera de saber si
  algo falla en ellos.
- **Product Service** sí tiene una página de estado (`/products/health`), pero solo devuelve un mensaje
  fijo; no comprueba de verdad si la conexión con Elasticsearch funciona. Un problema de conexión con
  Bonsai (por credenciales o por red) solo se ve en el registro del contenedor, no en esa página.
- **Order Service** no tiene un manejo especial de errores cuando falla la comunicación con Product
  Service. Si eso ocurre, el cliente recibe un mensaje de error genérico, pero en el registro sí queda el
  detalle completo de lo que pasó.
- **Gateway Service y User Service** comparten una misma clave secreta para firmar la sesión de cada
  usuario. Si esa clave no coincide entre ambos, o si la sesión se corrompe, el registro del Gateway
  muestra rechazos de sesión repetidos.

## 5. Reinicio de servicios

Reiniciar un servicio suele ser la primera solución que se intenta cuando algo no funciona bien, de la
misma manera en que apagar y volver a encender un foco que parpadea muchas veces lo arregla. Reiniciar
significa apagar el servicio por completo y volver a encenderlo desde cero, para que empiece a trabajar de
nuevo sin arrastrar el problema que tenía. Aun así, reiniciar no reemplaza a diagnosticar: conviene haber
revisado antes los registros de la sección 4, para no apagar y encender un servicio una y otra vez sin
entender qué le está pasando en realidad.

Para reiniciar un solo servicio, de manera controlada:

```bash
docker compose restart order-service
```

Este comando apaga y vuelve a encender únicamente el contenedor indicado, sin tocar a los demás. Es la
opción recomendada cuando el problema parece estar limitado a un solo servicio.

Para reiniciar todo el conjunto de servicios, respetando el orden en que deben encender:

```bash
docker compose down
docker compose up -d
```

El primer comando, `docker compose down`, apaga y elimina todos los contenedores del proyecto (no borra la
información guardada en las bases de datos, como se explica en la sección 8; solo elimina los contenedores
en sí). El segundo comando, `docker compose up -d`, vuelve a crear y a encender esos mismos contenedores
desde cero, en el orden que indica `docker-compose.yml`. La opción `-d` significa *detached*
("desconectado" o "en segundo plano"): hace que los servicios queden encendidos sin ocupar la ventana de la
terminal, para poder seguir escribiendo otros comandos mientras el sistema arranca.

En general, conviene reiniciar primero solo el servicio que presenta el problema, y dejar el reinicio
completo del sistema para cuando eso no resuelve la situación, o cuando varios servicios fallan al mismo
tiempo.

Algunas notas importantes:

- El orden de encendido que indica `docker-compose.yml` garantiza que un contenedor exista antes que otro,
  pero no garantiza que ya esté listo para recibir peticiones. Por eso es normal ver, durante los primeros
  segundos después de `docker compose up`, que un servicio intenta registrarse en Eureka o conectarse a su
  base de datos varias veces antes de lograrlo.
- Reiniciar `orders-db` o `users-db` no borra su información por sí solo (ver sección 8). El riesgo de
  perder datos aparece únicamente con comandos que afectan de forma directa a los volúmenes.

## 6. Actualización de los componentes de la plataforma

Actualizar un componente significa reemplazar el código que tiene por dentro con una versión más nueva,
sin cambiar lo que ese componente hace hacia afuera. Es parecido a cambiarle una pieza gastada a una
bicicleta: se reemplaza solo lo necesario, y la bicicleta sigue siendo la misma bicicleta, pero funciona
mejor.

Recordando la comparación de la sección 2, cada componente vive dentro de una lonchera (un contenedor) que
se empacó con una copia del código en un momento dado. Reiniciar esa lonchera, como se explicó en la
sección 5, solo la apaga y la vuelve a encender con la misma comida de adentro; eso no sirve para que un
código nuevo entre en ella. Para que el código nuevo quede adentro, hace falta construir una lonchera
distinta, y eso es justamente lo que hace una actualización.

Cuando hay un cambio de código en alguno de los microservicios, el procedimiento básico es el siguiente:

```bash
docker compose down
git pull
docker compose up --build -d
```

El comando `git pull` descarga, desde el repositorio del proyecto, los cambios de código más recientes que
se hayan guardado ahí, y los coloca en la copia local del proyecto; es el paso que asegura que la lonchera
nueva se construya con el código más reciente, y no con uno atrasado. La opción `--build`, agregada al
comando de encendido, le indica a Docker que, antes de encender cada servicio, vuelva a construir su
lonchera desde cero a partir del código actual, en lugar de reutilizar la que ya tenía guardada de antes.

Si solo se quiere actualizar un servicio, sin tocar los demás:

```bash
docker compose up --build -d product-service
```

Esta segunda forma es más rápida y más segura cuando el cambio de código afecta a un solo microservicio,
porque evita apagar y reconstruir componentes que no cambiaron.

Después de reconstruir, se debe repetir la verificación de la sección 3 (contenedores encendidos y
registrados en Eureka) antes de considerar terminada la actualización. Si algo no enciende correctamente,
conviene volver a la sección 4 y revisar los registros del servicio recién actualizado.

Esta sección cubre únicamente la actualización del entorno local que arma `docker-compose.yml`. Llevar
esos mismos cambios al entorno donde el sistema queda disponible para el público es un proceso distinto,
que no forma parte de esta guía básica.

## 7. Mantenimiento del frontend

El frontend es la parte de Tienda App con la que las personas interactúan de forma directa: lo que ven y lo
que tocan en la pantalla. A diferencia de los demás componentes explicados en la sección 2, el frontend no
vive dentro de una lonchera de Docker, sino que se mantiene con dos herramientas llamadas **Node** y
**npm**. Node es el programa que permite ejecutar código de JavaScript fuera de un navegador, y npm es como
una caja de herramientas que viene junto con Node: sirve para instalar, actualizar y ejecutar el código del
frontend.

Dentro de la carpeta `tienda-react` hay dos archivos importantes para el mantenimiento: `package.json`, que
es como la lista de ingredientes del frontend (anota el nombre de cada pieza de código externa que el
proyecto necesita), y `package-lock.json`, que anota la versión exacta de cada uno de esos ingredientes,
para que el frontend funcione igual sin importar en qué computadora se instale.

Los comandos básicos de mantenimiento son:

```bash
cd tienda-react
npm install          # solo hace falta cuando cambian package.json o package-lock.json
npm run dev           # levanta el frontend en modo de desarrollo
npm run build          # genera la versión lista para publicar
npm run preview       # permite revisar en local esa versión ya generada
```

En palabras simples, cada uno hace lo siguiente:

- `npm install` lee la lista de ingredientes y descarga cada uno de ellos a la computadora. Solo hace
  falta repetirlo cuando `package.json` o `package-lock.json` cambiaron, no cada vez que se enciende el
  frontend.
- `npm run dev` enciende el frontend en modo de desarrollo: lo abre en el navegador y lo actualiza solo,
  de forma automática, cada vez que se guarda un cambio en el código.
- `npm run build` construye la versión final del frontend, ya optimizada, lista para que otras personas la
  usen.
- `npm run preview` permite revisar en la propia computadora, antes de publicarla, esa versión final que
  generó `npm run build`.

Antes de dar por buena una actualización del frontend, también conviene correr las pruebas y la revisión
de estilo de código que ya tiene el proyecto:

```bash
npm run lint
npm run test
```

`npm run lint` revisa que el código esté bien escrito y ordenado, de forma parecida a un maestro revisando
la ortografía de un texto. `npm run test` ejecuta las pruebas automáticas del proyecto, que confirman que
el frontend sigue haciendo lo que debe hacer, como ensayar una obra de teatro completa antes de
presentarla frente al público.

Publicar esa versión para que el público la use es un paso aparte que no forma parte de esta guía; aquí el
mantenimiento se limita a mantener las dependencias actualizadas y a confirmar que el frontend compila y
funciona en local, conectado al Gateway.

## 8. Consideraciones sobre persistencia de datos

De todas las tareas que reúne esta guía, cuidar la información guardada es la más delicada, porque un
error aquí no se corrige con un simple reinicio: si un dato se borra por accidente, se pierde de verdad.
Por eso esta sección conviene leerla con más calma que las anteriores.

Recordando la comparación de la sección 2, cada componente vive dentro de una lonchera (un contenedor) que
se puede apagar, tirar y volver a armar sin ningún problema, tal como se vio en las secciones 5 y 6. Pero la
información que guardan las bases de datos no vive dentro de esa lonchera: vive en un lugar aparte, llamado
**volumen**, que se parece más a una mochila guardada en un armario aparte. Gracias a eso, apagar o
reconstruir un contenedor no borra, por sí solo, la información que tiene guardada; solo se pierde esa
información si alguien, además de tirar la lonchera, decide también vaciar la mochila.

**PostgreSQL.** La información de `users-db` y `orders-db` se guarda en dos volúmenes con nombre,
`users-data` y `orders-data`, definidos en `docker-compose.yml`. Los siguientes comandos "vacían la
mochila", es decir, borran esos volúmenes, y por eso deben evitarse a menos que se quiera borrar esa
información a propósito:

```bash
docker compose down -v        # también borra los volúmenes: elimina users-data y orders-data
docker volume rm users-data orders-data
```

La letra `-v` significa "volúmenes" (*volumes*, en inglés); al agregarla, se le está pidiendo a Docker que,
además de tirar la lonchera, también vacíe la mochila con toda la información de adentro. En cambio,
`docker compose down` (sin `-v`) y `docker compose restart` son comandos seguros: la información se
mantiene intacta aunque los contenedores se apaguen y se vuelvan a crear.

**Elasticsearch.** El catálogo de productos vive en el servidor de Bonsai, fuera de `docker-compose.yml`.
No existe ningún volumen local que lo guarde ni que se pueda borrar por accidente desde este entorno. Lo
que sí debe cuidarse es la configuración de acceso a ese servidor (usuario, contraseña y dirección,
definidos en `docker-compose.yml`). Si el catálogo llegara a borrarse directamente desde Bonsai, el sistema
vuelve a crear un índice vacío al reiniciar Product Service, y ahora sí existe un proceso que lo recupera
sin intervención manual: al arrancar, `ProductServiceElastic` revisa si el índice `products` está vacío y,
de estarlo, lo llena automáticamente con el catálogo de referencia guardado en
`src/main/resources/products-seed.json` (componente `ProductCatalogSeeder`). Si el índice ya tiene
productos, esta carga automática no hace nada, precisamente para no duplicar información en cada reinicio.
Antes, este mismo catálogo se cargaba a mano con una colección de Postman que hablaba directamente con
Bonsai, saltándose por completo al microservicio.

Es importante dejar claro que esta sección no describe una política de respaldo ya implementada: el
proyecto no tiene copias de seguridad configuradas ni para PostgreSQL ni para Elasticsearch. La única
protección real, por ahora, es evitar los comandos destructivos mencionados arriba sin necesidad real de
usarlos.

Como regla general y sencilla de recordar: ningún comando que mencione la palabra "volumen" o que incluya
la opción `-v` debe ejecutarse sin estar completamente seguro de qué información se va a borrar. Ante la
duda, lo más seguro siempre es no ejecutar el comando y, en cambio, preguntar antes a alguien con más
experiencia.

### 8.1 ¿Qué es un respaldo y por qué hace falta?

Guardar la información en un volumen, como se explicó antes, protege la mochila de que alguien la tire por
accidente junto con la lonchera. Pero la mochila sigue estando en un solo lugar: la misma computadora. Si
esa computadora se dañara, o si alguien vaciara la mochila a propósito o por error, la información
desaparecería igual, porque nunca existió una segunda copia en ningún otro lugar.

Un **respaldo** (también llamado *backup*) es, en palabras simples, sacarle una fotocopia a lo que hay
dentro de la mochila y guardar esa fotocopia en un lugar distinto, separado de la mochila original. Si algo
le pasa a la mochila, la fotocopia sigue existiendo y permite reconstruir la información que se había
perdido. Un volumen protege la información de un reinicio o de una reconstrucción de contenedores; un
respaldo protege la información de perder la mochila por completo.

Como se mencionó arriba, **Tienda App no cuenta todavía con un proceso de respaldo automático**, ni para
PostgreSQL ni para Elasticsearch. Esto no es un descuido menor: significa que, hoy por hoy, la única copia
de cada dato guardado en `users-db` y en `orders-db` es la que vive en el volumen local. Por eso, mientras
esa mejora no se implemente, cuidar de no ejecutar comandos destructivos (sección 8) es la única protección
real con la que cuenta el sistema.

### 8.2 Cómo sacar un respaldo manual de PostgreSQL

Aunque el proyecto no automatiza los respaldos, sí es posible sacar uno manualmente, de forma sencilla,
usando una herramienta que ya viene incluida con PostgreSQL llamada `pg_dump`. Este comando lee todo el
contenido de una base de datos y lo copia a un solo archivo de texto, como fotocopiar cada página de un
cuaderno y guardar esas fotocopias juntas en una carpeta.

Para respaldar la base de datos de usuarios, por ejemplo:

```bash
docker exec users-db-container pg_dump -U <usuario> <nombre_base_de_datos> > respaldo_users.sql
```

Y, de la misma manera, para respaldar la base de datos de pedidos:

```bash
docker exec orders-db-container pg_dump -U <usuario> <nombre_base_de_datos> > respaldo_orders.sql
```

Aquí, `docker exec` le pide a Docker que ejecute un comando *dentro* del contenedor indicado, en lugar de
en la computadora local; `<usuario>` y `<nombre_base_de_datos>` deben reemplazarse por los valores reales
configurados en el proyecto. El resultado es un archivo `.sql` que contiene, en texto plano, todo lo
necesario para reconstruir esa base de datos desde cero. Ese archivo conviene guardarlo en un lugar distinto
a la propia computadora —por ejemplo, en un disco externo o en un almacenamiento en la nube—, porque un
respaldo que vive en el mismo lugar que el original no cumple su propósito: si la computadora fallara, se
perderían los dos al mismo tiempo.

Si en algún momento hiciera falta restaurar la información a partir de ese archivo, el comando es el
siguiente:

```bash
docker exec -i users-db-container psql -U <usuario> <nombre_base_de_datos> < respaldo_users.sql
```

Este comando hace el proceso al revés: toma el archivo de texto con las fotocopias y las vuelve a escribir,
página por página, dentro de la base de datos. Por tratarse de una operación que puede sobrescribir
información existente, conviene practicarla primero en un entorno de prueba, nunca directamente sobre datos
reales sin antes entender bien qué hace cada parte del comando.

### 8.3 Un volumen no es magia: sus límites

Conviene cerrar esta sección con una idea importante y fácil de olvidar: un volumen no hace que la
información sea indestructible, solo la hace más difícil de borrar por accidente al trabajar con
contenedores. Un volumen sigue viviendo dentro del disco duro de una sola computadora, así que no protege la
información frente a situaciones como estas:

- Que el disco de esa computadora se dañe o falle.
- Que alguien, sin darse cuenta de lo que hace, borre el volumen directamente con comandos de Docker.
- Que se pierda o se dañe la computadora completa (por ejemplo, robo, incendio o una falla grave de
  hardware).

Por eso, aunque el volumen es una primera capa de protección razonable para el trabajo diario, no reemplaza
a un respaldo guardado en otro lugar. Entender esta diferencia —entre "la información sobrevive un
reinicio" y "la información sobrevive aunque se pierda la computadora completa"— es, quizás, la lección más
importante de toda esta sección.

## 9. Recomendaciones básicas de mantenimiento preventivo

Todo lo explicado en las secciones anteriores sirve para reaccionar cuando algo ya salió mal: un contenedor
que se apagó, un registro que muestra un error, una información que se perdió. El **mantenimiento
preventivo** es distinto: consiste en revisar el sistema aunque nadie haya reportado ningún problema
todavía, de la misma manera en que una persona se lava los dientes todos los días para no llegar a tener
una caries, en lugar de esperar a que le duela un diente para recién entonces ir al dentista. Revisar antes
de que algo falle casi siempre cuesta menos tiempo y menos esfuerzo que arreglarlo después de que ya falló.

Las siguientes recomendaciones no son pasos obligatorios con un resultado único, como los de las secciones
anteriores, sino hábitos que conviene repetir con cierta frecuencia:

- **Revisar que todo siga encendido.** Repetir de forma periódica `docker ps -a` o `docker compose ps`
  (sección 3), para detectar a tiempo contenedores apagados o que se reinician una y otra vez, en lugar de
  enterarse del problema solo cuando alguien más lo reporta.
- **Leer los registros aunque nada parezca estar fallando.** Revisar de vez en cuando `docker compose logs`
  (sección 4) buscando errores que se repiten. Un error que aparece una sola vez puede no ser importante,
  pero un error que se repite muchas veces suele ser el aviso temprano de un problema que, si no se atiende,
  puede crecer.
- **Vigilar el espacio en disco.** Con el tiempo, Docker va guardando copias e imágenes que ya no se usan,
  como una alacena que se va llenando de envases vacíos. Revisar `docker system df` de vez en cuando permite
  ver cuánto espacio están ocupando, y `docker image prune` ayuda a limpiar con cuidado lo que ya no hace
  falta. Al limpiar, siempre debe evitarse la opción `--volumes`, salvo que se tenga total certeza de qué
  información se va a borrar (sección 8).
- **Mantener actualizadas las dependencias.** Tanto el frontend (`npm outdated`, sección 7) como cada
  microservicio dependen de piezas de código externas y de versiones específicas de Java y de Spring. Con
  el paso del tiempo, esas piezas reciben correcciones y mejoras; probar esas actualizaciones primero en el
  entorno local, antes de llevarlas a producción, reduce el riesgo de que un cambio inesperado sorprenda al
  sistema completo de una sola vez.
- **Revisar los servicios externos.** De vez en cuando conviene confirmar que el buscador en Bonsai y, en el
  entorno de producción, la base de datos en Railway, sigan disponibles. Una falla en cualquiera de los dos
  no se nota ni en `docker ps` ni en el panel de Eureka, porque ambos viven fuera del sistema local; por
  eso, si nadie los revisa a propósito, un problema ahí puede pasar inadvertido durante mucho tiempo.
- **Comprobar que los respaldos manuales realmente funcionan.** No basta con saber cómo se saca un respaldo
  (sección 8.2); conviene, de vez en cuando, practicar también el proceso de restaurarlo en un entorno de
  prueba. Una fotocopia que nunca se revisó puede estar incompleta o dañada sin que nadie lo sepa, y eso
  solo se descubre justo en el peor momento: cuando de verdad hace falta usarla.
- **Anotar lo que se revisa y lo que se encuentra.** Llevar un registro sencillo —puede ser tan simple como
  una nota con la fecha, qué se revisó y qué se encontró— ayuda a notar patrones con el tiempo, como un
  servicio que falla siempre el mismo día de la semana, algo que sería muy difícil de notar si cada
  revisión se olvida apenas termina.

En conjunto, estas recomendaciones vuelven a la misma idea con la que abre esta guía (sección 1): mirar con
calma antes de que algo se vea mal, para poder actuar temprano y con tranquilidad, en lugar de reaccionar
apurado cuando el problema ya es grande. El mantenimiento preventivo no elimina por completo la posibilidad
de que algo falle —ningún sistema puede prometer eso—, pero sí hace que las fallas sean más pequeñas, más
fáciles de entender y menos sorpresivas para quien las tiene que resolver.

## 10. Cierre del anexo

Este anexo empezó con una idea muy simple: todo lo que funciona necesita cuidado, y ese cuidado se llama
mantenimiento. A lo largo de las nueve secciones anteriores, esa idea se fue llenando de pasos concretos,
como cuando a un dibujo hecho solo con el contorno se le va agregando color, poco a poco, hasta completarlo.

Vale la pena recordar, de forma breve, el camino recorrido. Primero se conocieron las piezas del juguete
armable que es Tienda App: sus loncheras (contenedores), la libreta de direcciones que las conecta (Eureka)
y las mochilas donde guardan lo importante (los volúmenes). Después se aprendió a **mirar**: a preguntarle
al sistema si todo sigue encendido. Luego se aprendió a **entender**: a leer el diario que cada servicio
escribe para descubrir qué le pasa cuando algo no anda bien. Solo después de saber mirar y entender, se
explicó cómo **actuar**: reiniciando un servicio como quien apaga y prende de nuevo un foco que parpadea,
cambiando una pieza gastada de la bicicleta al actualizar el código, y manteniendo al día la parte del
sistema que las personas ven y tocan. Y, por tratarse de lo más delicado de todo, se dedicó un cuidado
especial a la información guardada: a entender que una mochila no es lo mismo que una fotocopia guardada
en otro lugar, y a saber, con toda claridad, qué comandos hay que evitar para no perder por accidente algo
que no se puede recuperar.

Ese mismo orden —mirar, entender y solo después actuar— es, en el fondo, la única receta que esta guía
intenta dejar. No hace falta memorizar cada comando de memoria para dar un buen mantenimiento; alcanza con
recordar esa receta y volver a esta guía cada vez que haga falta repasar un paso concreto, de la misma
manera en que se vuelve a consultar una receta de cocina aunque ya se haya preparado ese platillo antes.

Por último, conviene decirlo con toda claridad: esta guía cubre el mantenimiento básico del **entorno
local**, y no agota todo lo que puede hacerse para cuidar un sistema como Tienda App. Cosas como respaldos
automáticos, monitoreo permanente o alertas que avisen solas cuando algo falla quedan fuera de este anexo,
señaladas a lo largo del texto como mejoras pendientes y no como tareas ya resueltas. Reconocer con
honestidad qué le falta a un sistema es también, a su manera, una forma de cuidarlo: es el primer paso para
que, en el futuro, alguien pueda completarlo.
