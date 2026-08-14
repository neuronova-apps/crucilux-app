# Crucilux

Crucilux es una aplicación web de crucigramas y retos de palabras perteneciente al ecosistema Neuronova Apps.

## Propósito

La propuesta busca ejercitar vocabulario, atención y razonamiento verbal mediante pistas breves, asociación de conceptos, resolución de palabras y recursos educativos sobre estrategias de crucigramas.

## Estado actual

Crucilux se encuentra como **MVP web funcional en desarrollo activo**. La versión disponible incorpora una progresión inicial de **tres retos seleccionables y nueve palabras en total**, escritura de letras directamente dentro de la cuadrícula, ayudas educativas graduales y **cinco guías públicas e indexables**.

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
- contexto ARIA dinámico para retos, pistas y tablero;
- retorno de foco al botón del menú móvil al cerrarlo con `Escape`;
- foco visible reforzado para enlaces y botones;
- progreso independiente por reto;
- persistencia local de letras parciales, palabras resueltas, pista activa, casilla seleccionada, mejor resultado y reto seleccionado mediante `localStorage`;
- restauración del avance después de recargar en el mismo navegador;
- migración compatible desde los formatos anteriores;
- funcionamiento en memoria si `localStorage` no está disponible;
- cinco guías educativas HTML con canonical, `index, follow`, navegación cruzada y acceso a la práctica;
- tarjeta social dedicada 1200 × 630 en `assets/social/crucilux-social.png`;
- Open Graph y Twitter Card normalizados con PNG 1200 × 630 y `summary_large_image` en las siete páginas públicas;
- diseño responsive;
- skip link, controles HTML nativos, regiones de estado mediante `aria-live` y soporte para `prefers-reduced-motion`;
- política de privacidad y sitemap con siete URLs públicas.

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

- `Tab` recorre los retos, pistas y controles externos del juego;
- seleccionar un reto o una pista lleva el foco a una casilla útil del tablero;
- las flechas desplazan la selección por casillas ocupadas adyacentes;
- `Backspace` borra la casilla actual o retrocede dentro de la palabra activa si está vacía;
- `Delete` borra la casilla actual;
- `Enter` comprueba la palabra activa sin sacar el foco del tablero;
- `Escape` cierra el menú móvil cuando está abierto y devuelve el foco al botón que lo controla.

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

## Guías educativas indexables

Crucilux incorpora cinco páginas estáticas independientes del motor del juego:

- `como-resolver-crucigramas.html`: método paso a paso para interpretar pistas, utilizar patrones y validar mediante cruces;
- `tipos-de-pistas-crucigramas.html`: definiciones directas, descripciones por función, asociaciones conceptuales y uso del patrón;
- `crucigramas-para-principiantes.html`: rutina inicial para resolver respuestas seguras, dejar huecos útiles y aprovechar letras confirmadas;
- `estrategias-crucigramas.html`: priorización, candidatos, alternancia horizontal/vertical y revisión de posiciones débiles;
- `vocabulario-y-crucigramas.html`: relación entre significado, categorías, recuperación de palabras, ortografía y cruces.

Cada guía dispone de:

- título y descripción propios;
- canonical absoluto;
- `robots="index, follow"`;
- Open Graph/Twitter con la tarjeta social dedicada de Crucilux;
- `og:image:type="image/png"`, dimensiones 1200 × 630 y texto alternativo;
- `twitter:card="summary_large_image"`;
- un único `h1`;
- skip link;
- navegación cruzada entre las cinco guías;
- enlace directo a la demo de Crucilux;
- estilos compartidos mediante `resources.css`.

La portada presenta estas páginas en la sección **Guías de Crucilux**, cuyo diseño se mantiene en `guide-cards.css`.

Estas páginas son contenido educativo estático. No modifican la partida, no analizan automáticamente el tablero y no convierten las ayudas graduales del juego en un sistema adaptativo.

## Tarjeta social

El activo social compartido es `assets/social/crucilux-social.png`, un PNG de **1200 × 630** diseñado para representar Crucilux como aplicación de crucigramas y retos de palabras.

La portada, las cinco guías educativas y `privacy.html` utilizan el mismo activo mediante Open Graph y Twitter Cards. El favicon continúa reservado para identidad del navegador y ya no se utiliza como imagen social principal.

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

La versión actual incorpora una capa específica de accesibilidad para el juego, además de la estructura semántica general, el enlace de salto al contenido, controles nativos, reducción de movimiento y foco visible.

El selector de retos utiliza botones con `aria-pressed` y nombres accesibles que incluyen número, categoría, progreso y estado seleccionado. Cada reto expone mediante `aria-controls` los elementos principales que actualiza.

Las pistas horizontales y verticales se agrupan semánticamente. Cada botón de pista comunica número, dirección, texto, si está resuelto y si corresponde a la palabra activa. Las pistas también identifican el tablero y las regiones de feedback que controlan.

El tablero declara una cuadrícula 5 × 5 con `aria-rowcount` y `aria-colcount`. Cada casilla ocupada informa fila, columna, letra actual, estado editable/resuelto y las pistas a las que pertenece. La capa `game-accessibility.js` mantiene sincronizados `aria-selected`, `aria-readonly` y `aria-invalid`, y añade el contexto de la pista activa al nombre accesible del tablero.

Solo la casilla seleccionada permanece en el orden de tabulación; el desplazamiento interno usa flechas. Las teclas disponibles en una casilla también se exponen mediante `aria-keyshortcuts`. Las regiones `gameMessage` y `learningFeedback` funcionan como estados `aria-live="polite"` y `aria-atomic="true"`.

El menú móvil actualiza su nombre entre **Abrir menú de navegación** y **Cerrar menú de navegación**. Si se cierra con `Escape`, el foco vuelve al botón del menú. Los enlaces y botones disponen además de un indicador `:focus-visible` explícito.

Las guías educativas usan estructura semántica, skip link, foco visible y navegación directa entre recursos. Estas mejoras no se presentan como certificación WCAG ni sustituyen pruebas manuales con lectores de pantalla, ampliación, alto contraste u otras tecnologías de asistencia. Esa validación formal sigue siendo una tarea posterior.

## Arquitectura

- `index.html`: estructura semántica, selector de retos, juego, sección de guías y contenido principal.
- `styles.css`: identidad base, layout general, navegación, foco visible, tipografía y comportamiento responsive.
- `components.css`: órbita, selector de retos, casillas editables, feedback, tarjetas y progreso.
- `guide-cards.css`: tarjetas de las guías educativas en la portada.
- `resources.css`: sistema visual compartido por las cinco páginas educativas.
- `assets/social/crucilux-social.png`: tarjeta social 1200 × 630 compartida por las páginas públicas.
- `script.js`: datos estructurados, navegación, año dinámico y revelado progresivo.
- `game.js`: catálogo de retos, construcción y edición de cuadrículas, comprobación de palabras y persistencia local por reto.
- `feedback.js`: diagnóstico de intentos, pistas graduales y explicaciones educativas de sesión.
- `game-accessibility.js`: contexto ARIA dinámico, nombres accesibles, estados de casillas y retorno de foco del menú.
- `como-resolver-crucigramas.html`, `tipos-de-pistas-crucigramas.html`, `crucigramas-para-principiantes.html`, `estrategias-crucigramas.html`, `vocabulario-y-crucigramas.html`: recursos educativos indexables.
- `privacy.html`: política de privacidad.
- `sitemap.xml`: portada, cinco guías y privacidad.
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Las dependencias propias de la interfaz del juego (`components.css`, `guide-cards.css`, `game.js`, `feedback.js` y `game-accessibility.js`) se declaran directamente en `index.html`; las guías solo utilizan `resources.css` y no cargan la lógica del juego.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El reto seleccionado y, para cada desafío, las letras parciales, palabras resueltas, pista activa, casilla seleccionada y mejor marca se conservan únicamente en el navegador mediante `localStorage` para restaurar la experiencia local. Las pistas graduales, la capa de accesibilidad y las páginas educativas no añaden datos al almacenamiento persistente.

## Sitio

https://neuronova-apps.github.io/crucilux-app/

## Ecosistema

https://neuronova-apps.github.io/
