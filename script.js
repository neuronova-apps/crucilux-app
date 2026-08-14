(() => {
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'WebApplication',
    '@id': 'https://neuronova-apps.github.io/crucilux-app/#app',
    name: 'Crucilux',
    url: 'https://neuronova-apps.github.io/crucilux-app/',
    description: 'MVP web de Crucilux con tres retos de crucigrama, nueve palabras, edición directa de letras, feedback educativo y pistas graduales para practicar vocabulario y razonamiento verbal.',
    applicationCategory: 'GameApplication',
    operatingSystem: 'Web',
    inLanguage: 'es-PE',
    applicationSuite: 'Neuronova Apps',
    featureList: [
      'Tres retos de crucigrama seleccionables',
      'Nueve palabras distribuidas por categorías',
      'Entrada y edición de letras directamente en las casillas',
      'Cruces compartidos y comprobación de la palabra activa',
      'Feedback de intentos y pistas graduales por palabra',
      'Progreso parcial y mejor resultado guardados por reto'
    ],
    isPartOf: {'@id': 'https://neuronova-apps.github.io/#website'}
  };

  if (!document.querySelector('script[data-neuronova-schema="true"]')) {
    const schema = document.createElement('script');
    schema.type = 'application/ld+json';
    schema.dataset.neuronovaSchema = 'true';
    schema.textContent = JSON.stringify(structuredData);
    document.head.appendChild(schema);
  }

  const menu = document.querySelector('.menu-button');
  const nav = document.querySelector('.main-nav');
  const year = document.querySelector('#year');

  if (year) {
    year.textContent = new Date().getFullYear();
  }

  if (menu && nav) {
    const close = () => {
      nav.classList.remove('open');
      menu.setAttribute('aria-expanded', 'false');
    };

    menu.addEventListener('click', () => {
      const open = nav.classList.toggle('open');
      menu.setAttribute('aria-expanded', String(open));
    });

    nav.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', close);
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        close();
      }
    });
  }

  const items = [...document.querySelectorAll('.reveal')];
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if ('IntersectionObserver' in window && !reduceMotion) {
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: .12 });

    items.forEach(item => observer.observe(item));
  } else {
    items.forEach(item => item.classList.add('visible'));
  }
})();
