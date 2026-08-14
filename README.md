# Crucilux

Crucilux es una aplicación web de crucigramas y retos de palabras perteneciente al ecosistema Neuronova Apps.

## Propósito

La propuesta busca ejercitar vocabulario, memoria, atención y razonamiento verbal mediante pistas breves, asociación de conceptos y resolución de palabras.

## Estado actual

Crucilux se encuentra como **MVP web funcional en desarrollo activo**. La versión disponible incorpora una progresión inicial de **tres retos seleccionables y nueve palabras en total**, organizada por categorías simples.

La versión actual incluye:

- identidad visual oscura coherente con Neuronova Apps;
- hero orbital temático de Crucilux;
- tres mini crucigramas seleccionables;
- nueve respuestas distribuidas en tres categorías;
- selección de pistas y comprobación de respuestas completas;
- progreso independiente por reto;
- persistencia local de palabras resueltas, pista activa, mejor resultado y reto seleccionado mediante `localStorage`;
- restauración del avance después de recargar en el mismo navegador;
- migración compatible desde los formatos anteriores del reto inicial;
- funcionamiento en memoria si `localStorage` no está disponible;
- diseño responsive;
- skip link, controles HTML nativos, mensajes mediante `aria-live` y soporte para `prefers-reduced-motion` como base de accesibilidad;
- política de privacidad y sitemap.

No existen todavía escritura letra por letra dentro del tablero, pistas graduales automáticas, niveles adaptativos ni validación formal de accesibilidad. Estas capacidades no deben considerarse disponibles hasta estar implementadas y verificadas.

## Retos disponibles

### 1. Palabras cotidianas

Reto inicial conservado desde la primera versión:

- `SOL` — horizontal;
- `MAR` — horizontal;
- `LUZ` — vertical.

### 2. Naturaleza

Reto conectado alrededor de conceptos del cielo, el agua y la tierra:

- `LUNA` — horizontal;
- `ROCA` — horizontal;
- `AGUA` — vertical.

`LUNA` y `ROCA` comparten cruces válidos con `AGUA` dentro de la cuadrícula 5 × 5.

### 3. Lenguaje e ideas

Reto centrado en lectura, expresión y pensamiento:

- `LIBRO` — horizontal;
- `IDEA` — horizontal;
- `RISA` — vertical.

`LIBRO` e `IDEA` quedan conectados mediante `RISA`.

Los identificadores internos estables de los retos son `cotidianas`, `naturaleza` y `lenguaje`.

## Progreso local

Crucilux utiliza la clave `crucilux-progress-v1` para conservar localmente el estado de los retos.

El formato actual utiliza `version: 3` y guarda:

- `selectedChallenge`: identificador del reto seleccionado;
- `challenges`: estado independiente de cada reto;
- `best`: mejor cantidad de palabras resueltas alcanzada dentro de cada reto;
- `solved`: lista validada de palabras ya resueltas en ese reto;
- `active`: pista seleccionada dentro de ese reto.

Al cargar la página, Crucilux valida los identificadores de reto y las palabras guardadas contra el catálogo incorporado en `game.js`. Después reconstruye la cuadrícula correspondiente, las respuestas ya resueltas, la pista activa y los indicadores del reto seleccionado.

El formato mantiene compatibilidad con las versiones 1 y 2, que solo conocían el reto original. Los campos antiguos `best`, `solved` y `active` se migran al reto `cotidianas`; los dos retos nuevos comienzan sin progreso previo.

Cambiar de reto no borra el progreso de los demás. **Reiniciar reto** elimina únicamente las palabras resueltas del desafío visible y vuelve a su primera pista, pero conserva su mejor marca histórica.

Si `localStorage` no está disponible o el contenido almacenado no puede interpretarse, los tres retos continúan funcionando durante la sesión sin bloquear la experiencia.

## Modelo de interacción actual

Cada reto mantiene el mismo flujo sencillo:

1. elegir un reto;
2. seleccionar una pista horizontal o vertical;
3. escribir la respuesta completa en el campo de texto;
4. comprobar la palabra;
5. revelar la respuesta correcta dentro de la cuadrícula cuando coincide.

La cuadrícula se reconstruye según las posiciones definidas para cada reto. Las casillas no utilizadas se muestran como bloques.

La edición directa de letras dentro del tablero permanece fuera del alcance de esta etapa y corresponde a una mejora posterior.

## Accesibilidad

La versión actual incorpora una base de accesibilidad formada por estructura semántica, enlace de salto al contenido, botones y campo de respuesta nativos, mensajes de estado con `aria-live` y reducción de movimiento mediante `prefers-reduced-motion`.

El selector de retos y las pistas utilizan botones reales y exponen el elemento activo mediante `aria-pressed`. Esta base no se presenta como certificación WCAG ni como sustituto de una auditoría formal. El tablero todavía requiere mejoras específicas de semántica, foco y navegación para una experiencia de crucigrama más completa.

## Arquitectura

- `index.html`: estructura semántica, selector de retos, contenido principal y declaración estática de estilos y scripts.
- `styles.css`: identidad base, layout general, navegación, tipografía y comportamiento responsive.
- `components.css`: órbita, selector de retos, juego, tarjetas, progreso y componentes específicos.
- `script.js`: datos estructurados, navegación, año dinámico y revelado progresivo.
- `game.js`: catálogo de retos, construcción de cuadrículas, comprobación de respuestas y persistencia local por reto.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz (`components.css` y `game.js`) se declaran directamente en `index.html`; `script.js` no crea recursos adicionales durante la carga salvo los datos estructurados JSON-LD.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El reto seleccionado, las palabras resueltas, la pista activa y la mejor marca de cada desafío se conservan únicamente en el navegador mediante `localStorage` para restaurar la experiencia local.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
