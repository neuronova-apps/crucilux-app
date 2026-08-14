# Crucilux

Crucilux es una aplicación web de crucigramas y retos de palabras perteneciente al ecosistema Neuronova Apps.

## Propósito

La propuesta busca ejercitar vocabulario, memoria, atención y razonamiento verbal mediante pistas breves, asociación de conceptos y resolución de palabras.

## Estado actual

Crucilux se encuentra como **MVP web funcional en desarrollo activo**. La versión disponible incorpora una progresión inicial de **tres retos seleccionables y nueve palabras en total**, con escritura de letras directamente dentro de la cuadrícula y ayudas educativas graduales.

La versión actual incluye:

- identidad visual oscura coherente con Neuronova Apps;
- hero orbital temático de Crucilux;
- tres mini crucigramas seleccionables;
- nueve respuestas distribuidas en tres categorías;
- selección de pistas horizontales y verticales;
- entrada y edición de letras directamente en las casillas del tablero;
- cruces compartidos entre palabras;
- comprobación de la palabra activa;
- feedback distinto para palabras incompletas e intentos incorrectos;
- conteo de letras colocadas en la posición correcta tras un intento completo fallido;
- marcado visual y accesible de casillas que necesitan revisión;
- tres niveles de pista gradual por palabra, sin escribir la respuesta automáticamente;
- explicación breve del concepto después de resolver una palabra;
- fijación de letras cuando una palabra se resuelve correctamente;
- navegación por casillas con flechas y borrado mediante `Backspace` o `Delete`;
- progreso independiente por reto;
- persistencia local de letras parciales, palabras resueltas, pista activa, casilla seleccionada, mejor resultado y reto seleccionado mediante `localStorage`;
- restauración del avance después de recargar en el mismo navegador;
- migración compatible desde los formatos anteriores;
- funcionamiento en memoria si `localStorage` no está disponible;
- diseño responsive;
- skip link, controles HTML nativos, mensajes mediante `aria-live` y soporte para `prefers-reduced-motion` como base de accesibilidad;
- política de privacidad y sitemap.

No existen todavía niveles adaptativos, generación dinámica de crucigramas, un sistema de pistas basado en análisis avanzado del tablero ni validación formal de accesibilidad. Estas capacidades no deben considerarse disponibles hasta estar implementadas y verificadas.

## Retos disponibles

### 1. Palabras cotidianas

- `SOL` — horizontal;
- `MAR` — horizontal;
- `LUZ` — vertical.

### 2. Naturaleza

- `LUNA` — horizontal;
- `ROCA` — horizontal;
- `AGUA` — vertical.

`LUNA` y `ROCA` comparten cruces válidos con `AGUA` dentro de la cuadrícula 5 × 5.

### 3. Lenguaje e ideas

- `LIBRO` — horizontal;
- `IDEA` — horizontal;
- `RISA` — vertical.

`LIBRO` e `IDEA` quedan conectados mediante `RISA`.

Los identificadores internos estables de los retos son `cotidianas`, `naturaleza` y `lenguaje`.

## Modelo de interacción actual

Cada reto sigue este flujo:

1. elegir un reto;
2. seleccionar una pista horizontal o vertical;
3. escribir una letra directamente en cada casilla de la palabra activa;
4. aprovechar las letras compartidas en los cruces;
5. comprobar la palabra activa;
6. revisar el feedback o solicitar una pista gradual si todavía no encaja;
7. fijar sus letras cuando la respuesta es correcta;
8. leer una explicación breve y continuar con las demás pistas.

Las casillas útiles son campos de una sola letra. La cuadrícula marca visualmente la palabra activa y la casilla seleccionada. Las casillas pertenecientes a una palabra ya resuelta pasan a ser de solo lectura para conservar los cruces correctos.

En teclado:

- las flechas desplazan la selección por casillas ocupadas adyacentes;
- `Backspace` borra la casilla actual o retrocede dentro de la palabra activa si está vacía;
- `Delete` borra la casilla actual;
- `Enter` comprueba la palabra activa.

El botón **Borrar palabra** elimina las letras editables de la palabra activa, pero conserva cualquier casilla compartida que ya haya quedado fijada por otra palabra resuelta.

## Feedback y pistas graduales

`feedback.js` complementa la lógica principal sin modificar la solución ni el almacenamiento del juego.

Cuando se comprueba una palabra:

- si faltan letras, indica cuántas casillas están vacías;
- si todas las casillas están completas pero la palabra es incorrecta, informa cuántas letras están en la posición correcta y marca las casillas que necesitan revisión;
- si la palabra es correcta, muestra una explicación breve de por qué el concepto encaja con la pista.

El control **Pista gradual** ofrece hasta tres niveles por palabra durante la sesión:

1. asociación semántica adicional;
2. letra inicial;
3. letra final.

Las pistas no escriben letras, no incrementan el progreso y no marcan palabras como resueltas. El nivel de pista mostrado y el feedback visual son estados temporales de interfaz y no se guardan en `localStorage`.

## Progreso local

Crucilux utiliza la clave `crucilux-progress-v1` para conservar localmente el estado de los retos.

El formato actual utiliza `version: 4` y guarda:

- `selectedChallenge`: identificador del reto seleccionado;
- `challenges`: estado independiente de cada reto;
- `best`: mejor cantidad de palabras resueltas alcanzada dentro de cada reto;
- `solved`: lista validada de palabras ya resueltas;
- `active`: pista seleccionada;
- `letters`: letras introducidas por índice de casilla, incluidas respuestas parciales;
- `selectedIndex`: casilla seleccionada al guardar.

Al cargar la página, Crucilux valida los identificadores de reto, palabras, índices y letras contra el catálogo incorporado en `game.js`. Después reconstruye la cuadrícula, las letras parciales, las palabras ya resueltas, la pista activa, la casilla seleccionada y los indicadores del reto.

El formato mantiene compatibilidad con las versiones anteriores. Los formatos v1 y v2 se asignan al reto `cotidianas`. Los estados v3 por reto también se aceptan aunque todavía no contengan `letters` ni `selectedIndex`; las letras de cualquier palabra ya resuelta se reconstruyen automáticamente.

Cambiar de reto no borra el progreso de los demás. **Reiniciar reto** elimina las palabras resueltas y todas las letras parciales del desafío visible, vuelve a su primera pista y conserva su mejor marca histórica.

Si `localStorage` no está disponible o el contenido almacenado no puede interpretarse, los tres retos continúan funcionando durante la sesión sin bloquear la experiencia.

## Accesibilidad

La versión actual incorpora una base de accesibilidad formada por estructura semántica, enlace de salto al contenido, controles nativos, mensajes de estado con `aria-live` y reducción de movimiento mediante `prefers-reduced-motion`.

El selector de retos y las pistas utilizan botones reales y exponen el elemento activo mediante `aria-pressed`. El tablero declara una cuadrícula 5 × 5; cada casilla ocupada informa fila, columna, letra actual, si está resuelta y los números/direcciones de las pistas a las que pertenece. Solo la casilla seleccionada permanece en el orden de tabulación y el desplazamiento principal dentro del tablero se realiza con flechas.

Después de un intento completo incorrecto, las casillas que necesitan revisión reciben `aria-invalid="true"` además del estado visual correspondiente. El panel educativo usa `aria-live="polite"` para comunicar pistas y explicaciones sin sustituir el mensaje principal del juego.

Esta base no se presenta como certificación WCAG ni como sustituto de una auditoría formal. El flujo de foco general, menú móvil y pruebas con tecnologías de asistencia se reforzarán en una etapa específica de accesibilidad.

## Arquitectura

- `index.html`: estructura semántica, selector de retos, instrucciones del tablero, pistas, controles de comprobación y panel educativo.
- `styles.css`: identidad base, layout general, navegación, tipografía y comportamiento responsive.
- `components.css`: órbita, selector de retos, casillas editables, feedback, tarjetas y progreso.
- `script.js`: datos estructurados, navegación, año dinámico y revelado progresivo.
- `game.js`: catálogo de retos, construcción y edición de cuadrículas, comprobación de palabras y persistencia local por reto.
- `feedback.js`: diagnóstico de intentos, pistas graduales y explicaciones educativas de sesión.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: rutas públicas principales.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz (`components.css`, `game.js` y `feedback.js`) se declaran directamente en `index.html`; `script.js` no crea recursos adicionales durante la carga salvo los datos estructurados JSON-LD.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El reto seleccionado y, para cada desafío, las letras parciales, palabras resueltas, pista activa, casilla seleccionada y mejor marca se conservan únicamente en el navegador mediante `localStorage` para restaurar la experiencia local. Las pistas graduales y el feedback educativo no añaden datos al almacenamiento persistente.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
