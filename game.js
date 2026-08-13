(() => {
  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];

  const board = $('#crossword');
  const input = $('#answerInput');
  const check = $('#checkAnswer');
  const reset = $('#resetGame');
  const message = $('#gameMessage');
  const solvedCount = $('#solvedCount');
  const bestScore = $('#bestScore');
  const progressState = $('#progressState');
  const clues = $$('.clue');

  if (!board || !input || !check || !reset) {
    return;
  }

  const STORAGE_KEY = 'crucilux-progress-v1';
  const words = {
    SOL: [1, 2, 3],
    MAR: [15, 16, 17],
    LUZ: [3, 8, 13]
  };
  const blocks = new Set([0, 4, 5, 6, 9, 10, 11, 14, 18, 19, 20, 21, 22, 23, 24]);

  let active = 'SOL';
  let solved = new Set();
  let best = 0;

  try {
    best = Number(JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}').best) || 0;
  } catch (_) {
    // El juego continúa sin persistencia cuando el almacenamiento no está disponible.
  }

  function build() {
    board.innerHTML = '';

    for (let index = 0; index < 25; index += 1) {
      const cell = document.createElement('span');
      cell.className = blocks.has(index) ? 'cell block' : 'cell';
      cell.dataset.index = index;
      cell.setAttribute('role', 'gridcell');

      if (blocks.has(index)) {
        cell.setAttribute('aria-hidden', 'true');
      }

      board.appendChild(cell);
    }

    render();
  }

  function render() {
    $$('.cell').forEach(cell => {
      if (!cell.classList.contains('block')) {
        cell.textContent = '';
        cell.classList.remove('solved');
      }
    });

    solved.forEach(word => {
      words[word].forEach((index, letterIndex) => {
        const cell = board.querySelector(`[data-index="${index}"]`);
        if (cell) {
          cell.textContent = word[letterIndex];
          cell.classList.add('solved');
        }
      });
    });

    clues.forEach(clue => {
      clue.classList.toggle('done', solved.has(clue.dataset.word));
    });

    if (solved.size > best) {
      best = solved.size;
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ best }));
      } catch (_) {
        // El progreso de la sesión sigue funcionando aunque no pueda persistirse.
      }
    }

    if (solvedCount) {
      solvedCount.textContent = String(solved.size);
    }
    if (bestScore) {
      bestScore.textContent = `${best}/3`;
    }
    if (progressState) {
      progressState.textContent = solved.size === 3
        ? 'Completado'
        : solved.size
          ? 'En progreso'
          : 'Comenzar';
    }
  }

  function select(word) {
    active = word;

    clues.forEach(clue => {
      clue.classList.toggle('active', clue.dataset.word === word);
    });

    input.value = '';
    input.focus();

    if (message) {
      message.className = 'game-message';
      message.textContent = solved.has(word)
        ? 'Esta palabra ya está resuelta.'
        : 'Escribe tu respuesta y compruébala.';
    }
  }

  function verify() {
    const answer = input.value
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    if (!answer) {
      if (message) {
        message.className = 'game-message error';
        message.textContent = 'Escribe una respuesta antes de comprobar.';
      }
      return;
    }

    if (answer === active) {
      solved.add(active);
      render();

      if (message) {
        message.className = 'game-message ok';
        message.textContent = solved.size === 3
          ? '¡Reto completado! Has resuelto las tres palabras.'
          : '¡Correcto! Selecciona otra pista.';
      }
      input.value = '';
      return;
    }

    if (message) {
      message.className = 'game-message error';
      message.textContent = 'Aún no coincide. Revisa la pista e inténtalo otra vez.';
    }
  }

  clues.forEach(clue => {
    clue.addEventListener('click', () => select(clue.dataset.word));
  });

  check.addEventListener('click', verify);
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      verify();
    }
  });

  reset.addEventListener('click', () => {
    solved = new Set();
    render();
    select('SOL');

    if (message) {
      message.textContent = 'Reto reiniciado. Selecciona una pista para comenzar.';
    }
  });

  build();
})();
