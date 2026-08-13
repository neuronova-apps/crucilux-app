# Crucilux

Crucilux es una aplicación web de crucigramas y retos de palabras perteneciente al ecosistema Neuronova Apps.

## Propósito

La propuesta busca ejercitar vocabulario, memoria, atención y razonamiento verbal mediante pistas breves, asociación de conceptos y resolución progresiva de palabras.

## Estado actual

La primera versión incluye:

- identidad visual oscura coherente con Neuronova Apps;
- hero orbital temático de Crucilux;
- mini crucigrama funcional de tres palabras;
- selección de pistas y comprobación de respuestas;
- progreso local mediante `localStorage`;
- diseño responsive;
- soporte para `prefers-reduced-motion`;
- política de privacidad y sitemap.

## Arquitectura

- `index.html`: estructura semántica, contenido principal y declaración estática de estilos y scripts.
- `styles.css`: identidad base, layout general, navegación, tipografía y comportamiento responsive.
- `components.css`: órbita, juego, tarjetas, progreso y componentes específicos.
- `script.js`: navegación, año dinámico y revelado progresivo.
- `game.js`: lógica del mini crucigrama y progreso local.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz (`components.css` y `game.js`) se declaran directamente en `index.html`; `script.js` no crea recursos adicionales durante la carga.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El mejor resultado del reto inicial se conserva únicamente en el navegador mediante `localStorage`.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
