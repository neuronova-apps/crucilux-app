# Crucilux

Crucilux es una aplicación web de crucigramas y retos de palabras perteneciente al ecosistema Neuronova Apps.

## Propósito

La propuesta busca ejercitar vocabulario, memoria, atención y razonamiento verbal mediante pistas breves, asociación de conceptos y resolución de palabras.

## Estado actual

Crucilux se encuentra como **MVP web funcional en desarrollo activo**. La versión disponible incluye un único reto inicial compuesto por tres palabras y sirve como base para ampliar más adelante el contenido por retos, categorías y tipos de pistas.

La versión actual incluye:

- identidad visual oscura coherente con Neuronova Apps;
- hero orbital temático de Crucilux;
- un mini crucigrama funcional de tres palabras: `SOL`, `MAR` y `LUZ`;
- selección de pistas y comprobación de respuestas completas;
- visualización de palabras acertadas y estado durante la sesión actual;
- conservación únicamente del mejor resultado mediante `localStorage` entre recargas;
- diseño responsive;
- skip link, controles HTML nativos, mensajes mediante `aria-live` y soporte para `prefers-reduced-motion` como base de accesibilidad;
- política de privacidad y sitemap.

No existen todavía varios niveles o retos seleccionables, persistencia de la partida actual, escritura letra por letra dentro del tablero ni validación formal de accesibilidad. Estas capacidades no deben considerarse disponibles hasta estar implementadas y verificadas.

## Progreso local

Durante una sesión, Crucilux mantiene en memoria las palabras resueltas y actualiza los indicadores **Palabras acertadas** y **Estado**.

Entre recargas, `localStorage` conserva únicamente el mejor resultado alcanzado en el reto inicial mediante la clave `crucilux-progress-v1`. Las palabras ya resueltas de la sesión no se restauran actualmente después de recargar.

## Accesibilidad

La versión actual incorpora una base de accesibilidad formada por estructura semántica, enlace de salto al contenido, botones y campo de respuesta nativos, mensajes de estado con `aria-live` y reducción de movimiento mediante `prefers-reduced-motion`.

Esta base no se presenta como certificación WCAG ni como sustituto de una auditoría formal. El tablero todavía requiere mejoras específicas de semántica, foco y navegación para una experiencia de crucigrama más completa.

## Arquitectura

- `index.html`: estructura semántica, contenido principal y declaración estática de estilos y scripts.
- `styles.css`: identidad base, layout general, navegación, tipografía y comportamiento responsive.
- `components.css`: órbita, juego, tarjetas, progreso y componentes específicos.
- `script.js`: datos estructurados, navegación, año dinámico y revelado progresivo.
- `game.js`: lógica del mini crucigrama y mejor resultado local.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz (`components.css` y `game.js`) se declaran directamente en `index.html`; `script.js` no crea recursos adicionales durante la carga salvo los datos estructurados JSON-LD.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El mejor resultado del reto inicial se conserva únicamente en el navegador mediante `localStorage`.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
