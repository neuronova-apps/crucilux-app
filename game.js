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
  const validWords = new Set(Object.keys(words));
  const blocks = new Set([0, 4, 5, 6, 9, 10, 11, 14, 18, 19, 20, 21, 22, 23, 24]);

  let active = 'SOL';
  let solved = new Set();
  let best = 0;
  let restored = false;

  function readStoredProgress() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return;
      }

      const stored = JSON.parse(raw);
      const storedBest = Number(stored?.best);

      if (Number.isFinite(storedBest)) {
        best = Math.max(0, Math.min(validWords.size, Math.floor(storedBest)));
      }

      if (Array.isArray(stored?.solved)) {
        solved = new Set(stored.solved.filter(word => validWords.has(word)));
      }

      if (validWords.has(stored?.active)) {
        active = stored.active;
      }

      if (solved.size > best) {
        best = solved.size;
      }

      restored = solved.size > 0;
    } catch (_) {
      // El juego continúa en memoria cuando el almacenamiento no está disponible o no es válido.
    }
  }

  function persistProgress() {
    try {
      if (solved.size === 0 && best === 0) {
        localStorage.removeItem(STORAGE_KEY);
        return;
      }

      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        version: 2,
        best,
        solved: [...solved],
        active
      }));
    } catch (_) {
      // El progreso de la sesión sigue funcionando aunque no pueda persistirse.
    }
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
      const word = clue.dataset.word;
      clue.classList.toggle('done', solved.has(word));
      clue.classList.toggle('active', word === active);
    });

    if (solved.size > best) {
      best = solved.size;
    }

    persistProgress();

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
    if (!validWords.has(word)) {
      return;
    }

    active = word;

    clues.forEach(clue => {
      clue.classList.toggle('active', clue.dataset.word === word);
    });

    persistProgress();
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
          : '¡Correcto! Tu avance quedó guardado en este navegador. Selecciona otra pista.';
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
    active = 'SOL';
    render();
    select('SOL');

    if (message) {
      message.className = 'game-message';
      message.textContent = 'Reto reiniciado. La partida actual se borró; tu mejor resultado se conserva.';
    }
  });

  readStoredProgress();
  build();

  if (restored && message) {
    message.className = 'game-message ok';
    message.textContent = solved.size === 3
      ? 'Reto completado restaurado desde este navegador.'
      : `Progreso restaurado: ${solved.size} de 3 palabras resueltas. Puedes continuar.`;
  }
})();
