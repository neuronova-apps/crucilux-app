# Crucilux

Crucilux es una aplicación de Neuronova Apps orientada a practicar vocabulario, atención y razonamiento verbal mediante crucigramas, pistas graduales y recursos educativos sobre estrategias de resolución.

## Estado del proyecto

- **Web:** MVP funcional en desarrollo activo.
- **Publicación:** disponible mediante GitHub Pages.
- **Android:** rama `android` separada en trabajo en progreso; no es una versión estable ni publicada.

## Alcance actual

La versión pública ofrece retos cerrados de crucigrama con categorías definidas, feedback progresivo y almacenamiento local. Su alcance actual no incluye niveles adaptativos ni generación dinámica de crucigramas.

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
- navegación por teclado y contexto ARIA dinámico;
- cinco guías educativas públicas e indexables;
- diseño responsive, política pública de privacidad y sitemap.

## Tecnología

La versión web utiliza HTML5, CSS3, JavaScript en el navegador, `localStorage`, GitHub Pages, páginas HTML estáticas para guías educativas, metadatos sociales y Schema.org, además de capas específicas de feedback y accesibilidad. No requiere proceso de compilación.

## Accesibilidad

Crucilux utiliza controles nativos, skip link, foco visible, cuadrícula accesible 5 × 5, navegación interna mediante flechas, estados ARIA sincronizados, regiones `aria-live`, teclas de borrado y comprobación desde teclado, y retorno lógico del foco al cerrar el menú con `Escape`.

La superficie pública forma parte de la auditoría automática central del ecosistema. Estas medidas no constituyen una certificación WCAG y continúan sujetas a pruebas manuales con lectores de pantalla, zoom, alto contraste y otras tecnologías de asistencia.

## Privacidad

La versión actual no requiere cuenta ni base de datos remota. El reto seleccionado, letras parciales, palabras resueltas, pista activa, casilla seleccionada y mejor marca se conservan localmente en el navegador.

Política pública: https://neuronova-apps.github.io/crucilux-app/privacy/

La antigua ruta `privacy.html` se mantiene únicamente como compatibilidad y redirige a `/privacy/`.

## Limitaciones conocidas

No existen todavía niveles adaptativos ni generación dinámica de crucigramas. El contenido disponible es limitado y el progreso permanece únicamente en el navegador. La revisión manual integral de accesibilidad continúa pendiente y la rama Android no es una aplicación publicada.

## Roadmap

Las prioridades son ampliar niveles y categorías, incorporar más tipos de pistas, evaluar generación o selección dinámica de retos, profundizar el feedback educativo y completar validaciones manuales de accesibilidad del tablero.

## Desarrollo local

```bash
git clone https://github.com/neuronova-apps/crucilux-app.git
cd crucilux-app
python3 -m http.server 8000
```

Después abre `http://localhost:8000`. La rama `main` corresponde a la versión web pública y `android` mantiene el desarrollo móvil separado.

## Estructura principal

- `index.html`: portada, selector de retos y juego;
- `game.js`: catálogo, cuadrículas, comprobación y persistencia;
- `feedback.js`: diagnóstico, pistas graduales y explicaciones;
- `game-accessibility.js`: estados ARIA y navegación accesible;
- `script.js`: navegación y comportamiento general;
- hojas CSS: sistema visual, componentes y recursos;
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

Crucilux forma parte de Neuronova Apps y comparte con el ecosistema una base común de identidad, accesibilidad, privacidad, SEO, documentación y publicación web, conservando su repositorio independiente.

## Autoría

Proyecto personal e independiente desarrollado por Gabriel Berrospi dentro del ecosistema Neuronova Apps.

## Última revisión

2026-08-15
