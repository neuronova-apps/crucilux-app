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
- visualización de palabras acertadas y estado de la partida actual;
- persistencia local de palabras resueltas, pista activa y mejor resultado mediante `localStorage`;
- restauración del avance después de recargar en el mismo navegador;
- compatibilidad con datos locales creados antes de incorporar la persistencia de la partida;
- funcionamiento en memoria si `localStorage` no está disponible;
- diseño responsive;
- skip link, controles HTML nativos, mensajes mediante `aria-live` y soporte para `prefers-reduced-motion` como base de accesibilidad;
- política de privacidad y sitemap.

No existen todavía varios niveles o retos seleccionables, escritura letra por letra dentro del tablero ni validación formal de accesibilidad. Estas capacidades no deben considerarse disponibles hasta estar implementadas y verificadas.

## Progreso local

Crucilux utiliza la clave `crucilux-progress-v1` para conservar localmente el estado relevante del reto inicial.

El formato actual guarda:

- `best`: mejor cantidad de palabras resueltas alcanzada en el reto;
- `solved`: lista validada de palabras ya resueltas en la partida actual;
- `active`: pista seleccionada al guardar;
- `version`: versión interna de la estructura persistida.

Al cargar la página, Crucilux valida los identificadores guardados contra las tres palabras conocidas (`SOL`, `MAR` y `LUZ`). Si encuentra una partida coherente, reconstruye las palabras resueltas, restaura la pista activa y actualiza los indicadores del tablero.

El formato es compatible con el almacenamiento anterior que solo contenía `best`. En ese caso, la mejor marca se mantiene y la partida comienza sin palabras restauradas.

Si `localStorage` no está disponible o el contenido almacenado no puede interpretarse, el juego continúa funcionando durante la sesión sin bloquear la experiencia.

**Reiniciar** elimina las palabras resueltas de la partida actual y vuelve a `SOL` como pista inicial, pero conserva la mejor marca histórica. Si no existe partida ni mejor resultado, no se mantiene un registro local innecesario.

## Accesibilidad

La versión actual incorpora una base de accesibilidad formada por estructura semántica, enlace de salto al contenido, botones y campo de respuesta nativos, mensajes de estado con `aria-live` y reducción de movimiento mediante `prefers-reduced-motion`.

Esta base no se presenta como certificación WCAG ni como sustituto de una auditoría formal. El tablero todavía requiere mejoras específicas de semántica, foco y navegación para una experiencia de crucigrama más completa.

## Arquitectura

- `index.html`: estructura semántica, contenido principal y declaración estática de estilos y scripts.
- `styles.css`: identidad base, layout general, navegación, tipografía y comportamiento responsive.
- `components.css`: órbita, juego, tarjetas, progreso y componentes específicos.
- `script.js`: datos estructurados, navegación, año dinámico y revelado progresivo.
- `game.js`: lógica del mini crucigrama, persistencia de la partida actual y mejor resultado local.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz (`components.css` y `game.js`) se declaran directamente en `index.html`; `script.js` no crea recursos adicionales durante la carga salvo los datos estructurados JSON-LD.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. Las palabras resueltas, la pista activa y el mejor resultado del reto inicial se conservan únicamente en el navegador mediante `localStorage` para restaurar la experiencia local.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
