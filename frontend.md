# ¿Cómo se implementó el frontend?

## Contenido

### React

El frontend de Speed Sneakers se construyó con React 19 sobre Vite como herramienta de desarrollo y empaquetado. La aplicación completa vive en el proyecto `tienda-react` y arranca desde `main.jsx`, donde se monta un único componente raíz, `SneakerStoreApp`, envuelto en `StrictMode`. No hay un framework de gestión de estado externo como Redux ni Zustand: el estado se resuelve con los propios mecanismos de React, combinando `useState`, `useEffect` y el patrón de Context API para lo que necesita estar disponible en toda la aplicación, que en este caso es la sesión del usuario.

Esa decisión tiene sentido para el tamaño actual del catálogo y del flujo de compra, pero también marca un límite: el carrito de compras se guarda como estado local dentro de `SneakerStoreApp` y se pasa hacia abajo por props a cada página que lo necesita (`HomePage`, `ProductListPage`, `ProductDetailsPage`, `CartPage`). Si el usuario recarga la página o cierra la pestaña, el carrito desaparece porque nunca se persiste en `localStorage` ni en el backend. Es una limitación real del diseño actual, no un detalle menor.

Para las llamadas a los microservicios no se usa una librería como Axios ni React Query; todo pasa por `fetch` nativo dentro de hooks personalizados, con `credentials: 'include'` para que el navegador envíe la cookie de sesión que emite `UserService`. La URL base de cada servicio se arma a partir de variables de entorno (`VITE_API_GATEWAY_URL`, `VITE_API_PRODUCTS_URL`, `VITE_API_ORDERS_URL`), con un valor de respaldo fijo a `http://localhost:8762` cuando esas variables no están definidas. Esto facilita correr el proyecto en local sin configurar nada, pero deja un rastro de valores hardcodeados que conviene revisar antes de considerar el build listo para un entorno distinto al de desarrollo.

### Organización de la interfaz

La interfaz sigue un esquema de tres franjas fijas: un `Header` con logo, buscador y navegación en la parte superior; un bloque `<main>` donde React Router intercambia el contenido según la ruta; y un `Footer` con enlaces legales y de contacto. Esa misma estructura se repite en cada pantalla, así que el usuario nunca pierde de vista el buscador ni el contador del carrito, sin importar en qué página esté.

El estilo se resuelve en un solo archivo, `styles/styles.css`, con nombres de clase que siguen una convención tipo BEM (`header__nav`, `product-card__body`, `cart__summary-checkout-button`). No hay CSS Modules ni una librería de componentes visuales (Material UI, Tailwind, etc.); todo el diseño depende de esa hoja de estilos compartida. Funciona porque el sitio es pequeño, pero un archivo único de estilos crece rápido en fricción cuando el catálogo de páginas aumenta, porque cualquier cambio de nombre de clase obliga a revisar todo el archivo para no romper otra pantalla.

### Componentes principales

Los componentes se dividen en dos carpetas con responsabilidades distintas: `components/` para piezas reutilizables y `pages/` para las pantallas completas que cuelgan de una ruta.

Entre los componentes reutilizables:

- **`Header`**: concentra el logo, el formulario de búsqueda (que redirige a `/products?search=...`), los enlaces de navegación, el contador de artículos en el carrito y el bloque de sesión (nombre de usuario y botón de salida). Es el componente con más responsabilidades del proyecto, algo que se explica porque ahí conviven navegación, búsqueda y sesión sin que se hayan separado en piezas más pequeñas.
- **`ProductCard`**: muestra imagen, nombre, descripción corta, marca, categoría y precio de un producto, con un botón para agregarlo al carrito. Se reutiliza igual en la página de inicio, en el listado completo y en los resultados de búsqueda.
- **`ProtectedRoute`**: envuelve cualquier ruta que exija sesión iniciada. Consulta el estado `user` y `loading` del `AuthContext`; mientras la sesión se está verificando muestra un mensaje de carga, y si no hay usuario autenticado redirige a `/login` con `Navigate replace`. Prácticamente toda la aplicación pasa por este guard, salvo `/login` y `/register`.
- **`Footer`** y **`Copyright`**: componentes pequeños, sin lógica, que solo aportan los enlaces a la política de devoluciones y contacto.

Entre las páginas, destacan por su lógica propia `CartPage` (calcula el total, dispara el checkout contra `OrderService` y maneja el estado de "procesando" mientras espera la respuesta), `LoginPage` y `RegisterPage` (formularios controlados que llaman a `AuthContext`), y `OrderListPage` / `OrderDetailPage` (listan y detallan las compras del usuario autenticado, con fechas formateadas en español de México mediante `toLocaleDateString('es-MX', ...)`, uno de los pocos puntos donde el código reconoce explícitamente la localización mexicana).

La obtención de datos se aisló en hooks dedicados por recurso: `useProducts`, `useProduct`, `useOrders` y `useOrder`. Cada uno maneja su propio `loading` y `error`, lo cual evita duplicar lógica de fetch en cada página, aunque también significa que el manejo de errores no es uniforme: algunas pantallas muestran un `<h2>Error: ...</h2>` plano, mientras que el flujo de checkout en `SneakerStoreApp` usa `alert()` del navegador para comunicar tanto éxitos como fallos, algo que no es coherente con el resto de la interfaz ni con una experiencia de compra que se quiera ver como profesional.

### Navegación

El enrutamiento corre sobre `react-router-dom` v7, con un único `BrowserRouter` en `AppWrapper` que envuelve a `AuthProvider` y este, a su vez, a `SneakerStoreApp`. Todas las rutas están declaradas en un solo lugar, dentro de `SneakerStoreApp`, sin rutas anidadas ni `layout routes`: `/`, `/products`, `/products/:id`, `/cart`, `/returns`, `/contact`, `/orders`, `/orders/:id`, más `/login` y `/register` fuera del guard de autenticación.

La búsqueda de productos se resuelve por query string (`/products?search=texto`), leído con `useLocation` y `URLSearchParams` dentro de `ProductListPage`, en lugar de mantenerse como estado de React. La ventaja es que la búsqueda queda en la URL y se puede compartir o recargar sin perderse; la desventaja es que ese patrón conviven con el carrito, que sí se maneja como estado interno y no sobrevive a un refresco, así que la aplicación termina usando dos filosofías distintas para conservar datos entre pantallas.

## Análisis

El frontend cumple su función principal, mostrar catálogo, permitir compra y proteger las rutas privadas, con un stack minimalista que evita dependencias innecesarias. Esa sobriedad es una fortaleza cuando el objetivo es un proyecto académico o un prototipo funcional, pero también expone decisiones que un producto real tendría que resolver antes de crecer. El carrito sin persistencia es el ejemplo más claro: cualquier usuario que refresque la página en medio de una compra pierde su selección sin ningún aviso previo. El uso de `alert()` para notificar errores y éxitos del checkout rompe con el resto de la interfaz, que sí invierte en componentes y clases propias para el resto de las pantallas.

La separación entre `components/` y `pages/`, junto con los hooks por recurso, muestra una intención clara de mantener responsabilidades separadas, y el `ProtectedRoute` resuelve bien el problema de proteger rutas sin duplicar la lógica de verificación en cada página. Sin embargo, el `Header` concentra demasiadas funciones (búsqueda, navegación, sesión y carrito) sin dividirse en subcomponentes, lo que dificulta probarlo o modificarlo de forma aislada. La dependencia de URLs de respaldo hardcodeadas a `localhost:8762` en tres hooks distintos (`useProducts`, `useOrder` y el checkout de `SneakerStoreApp`) es otro punto que un despliegue fuera del entorno de desarrollo tendría que corregir, porque ese valor por defecto no tiene sentido en producción y queda repetido en lugar de centralizado en un solo archivo de configuración.

## Transición

Con la interfaz y su lógica de navegación descritas, el siguiente punto del informe debe abordar cómo se conectan estas pantallas con los microservicios de backend, en particular cómo `UserService`, `ProductService` y `OrderService` exponen sus contratos a través del API Gateway y qué implicaciones tiene esa comunicación para la seguridad y la consistencia de los datos que el usuario ve en el frontend.
