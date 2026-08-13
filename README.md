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

- `index.html`: estructura y contenido principal.
- `styles.css`: identidad base y layout general.
- `components.css`: órbita, juego, tarjetas y componentes específicos.
- `script.js`: navegación, carga de componentes y revelado progresivo.
- `game.js`: lógica del mini crucigrama y progreso local.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El mejor resultado del reto inicial se conserva únicamente en el navegador mediante `localStorage`.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
