# Crucilux

Crucilux es una aplicación de Neuronova Apps orientada a practicar vocabulario, atención y razonamiento verbal mediante crucigramas, pistas graduales y recursos educativos sobre estrategias de resolución.

## Estado del proyecto

- **Web:** MVP funcional en desarrollo activo.
- **Publicación:** disponible mediante GitHub Pages.
- **Android:** existe una rama `android` separada para el desarrollo móvil. Se considera trabajo en progreso y no una versión estable o publicada.

## Funciones disponibles

- tres retos de crucigrama seleccionables;
- nueve palabras distribuidas en tres categorías;
- escritura y edición de letras directamente en el tablero;
- cruces compartidos entre palabras;
- selección de pistas horizontales y verticales;
- comprobación de la palabra activa;
- feedback para palabras incompletas e intentos incorrectos;
- tres niveles de pista gradual por palabra;
- explicación educativa después de resolver;
- progreso independiente por reto;
- persistencia local de letras, palabras resueltas, selección y mejor resultado;
- navegación por teclado y contexto ARIA dinámico para retos, pistas y tablero;
- cinco guías educativas públicas e indexables;
- diseño responsive, política pública de privacidad y sitemap.

No existen todavía niveles adaptativos ni generación dinámica de crucigramas. Estas capacidades se consideran futuras hasta que estén implementadas y verificadas.

## Tecnología

La versión web utiliza:

- HTML5;
- CSS3;
- JavaScript en el navegador;
- `localStorage` para el progreso de los retos;
- GitHub Pages;
- páginas HTML estáticas para las guías educativas;
- metadatos sociales y Schema.org;
- capas específicas de feedback y accesibilidad.

No requiere proceso de compilación para ejecutar la versión web actual.

## Accesibilidad

Crucilux utiliza controles nativos, skip link, foco visible, cuadrícula accesible 5 × 5, navegación interna mediante flechas, estados ARIA sincronizados, regiones `aria-live`, teclas de borrado y comprobación desde teclado, y retorno del foco al botón del menú al cerrarlo con `Escape`.

Estas medidas no constituyen una certificación WCAG y continúan sujetas a pruebas manuales con lectores de pantalla, zoom, alto contraste y otras tecnologías de asistencia.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El reto seleccionado, letras parciales, palabras resueltas, pista activa, casilla seleccionada y mejor marca se conservan localmente en el navegador.

Política pública:

https://neuronova-apps.github.io/crucilux-app/privacy/

La antigua ruta `privacy.html` se mantiene únicamente como compatibilidad y redirige a `/privacy/`.

## Desarrollo local

```bash
git clone https://github.com/neuronova-apps/crucilux-app.git
cd crucilux-app
python3 -m http.server 8000
```

Después abre `http://localhost:8000`.

La rama `main` corresponde a la versión web pública. La rama `android` mantiene el desarrollo móvil separado.

## Estructura principal

- `index.html`: portada, selector de retos y juego;
- `game.js`: catálogo, cuadrículas, comprobación y persistencia;
- `feedback.js`: diagnóstico, pistas graduales y explicaciones;
- `game-accessibility.js`: estados ARIA y navegación accesible;
- `script.js`: navegación, datos estructurados y comportamiento general;
- `styles.css` y `components.css`: sistema visual y componentes;
- `guide-cards.css` y `resources.css`: recursos educativos;
- páginas HTML educativas: cinco guías indexables;
- `privacy/`: política pública;
- `privacy.html`: redirección de compatibilidad;
- `assets/social/`: tarjeta social;
- `sitemap.xml`: URLs públicas.

## Enlaces

- **Web:** https://neuronova-apps.github.io/crucilux-app/
- **Privacidad:** https://neuronova-apps.github.io/crucilux-app/privacy/
- **Repositorio:** https://github.com/neuronova-apps/crucilux-app
- **Ecosistema:** https://neuronova-apps.github.io/

## Neuronova Apps

Crucilux forma parte de **Neuronova Apps** y comparte con el ecosistema una base común de identidad, accesibilidad, privacidad, SEO, documentación y publicación web.

## Autoría

Proyecto personal e independiente desarrollado por Gabriel Berrospi dentro del ecosistema Neuronova Apps.
