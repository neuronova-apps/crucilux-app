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
  const gameKicker = $('#gameKicker');
  const gameTitle = $('#game-title');
  const challengeSummary = $('#challengeSummary');
  const acrossClues = $('#acrossClues');
  const downClues = $('#downClues');
  const challengeButtons = $$('.challenge-option');

  if (!board || !input || !check || !reset || !acrossClues || !downClues) {
    return;
  }

  const STORAGE_KEY = 'crucilux-progress-v1';
  const DEFAULT_CHALLENGE = 'cotidianas';

  const puzzles = {
    cotidianas: {
      title: 'Palabras cotidianas',
      kicker: 'Reto 1 · Inicio',
      summary: 'Tres conceptos breves y familiares para comenzar.',
      words: {
        SOL: { number: 1, direction: 'across', indices: [1, 2, 3], clue: 'Estrella que ilumina nuestro planeta.' },
        MAR: { number: 2, direction: 'across', indices: [15, 16, 17], clue: 'Gran extensión de agua salada.' },
        LUZ: { number: 3, direction: 'down', indices: [3, 8, 13], clue: 'Hace posible que podamos ver.' }
      },
      order: ['SOL', 'MAR', 'LUZ']
    },
    naturaleza: {
      title: 'Naturaleza',
      kicker: 'Reto 2 · Naturaleza',
      summary: 'Palabras relacionadas con el cielo, el agua y la tierra.',
      words: {
        LUNA: { number: 1, direction: 'across', indices: [0, 1, 2, 3], clue: 'Satélite natural visible en el cielo nocturno.' },
        ROCA: { number: 2, direction: 'across', indices: [15, 16, 17, 18], clue: 'Material sólido que forma parte de la corteza terrestre.' },
        AGUA: { number: 3, direction: 'down', indices: [3, 8, 13, 18], clue: 'Líquido esencial presente en ríos, lagos y mares.' }
      },
      order: ['LUNA', 'ROCA', 'AGUA']
    },
    lenguaje: {
      title: 'Lenguaje e ideas',
      kicker: 'Reto 3 · Lenguaje',
      summary: 'Conceptos vinculados con expresión, lectura y pensamiento.',
      words: {
        LIBRO: { number: 1, direction: 'across', indices: [5, 6, 7, 8, 9], clue: 'Obra escrita formada por páginas reunidas en un volumen.' },
        IDEA: { number: 2, direction: 'across', indices: [20, 21, 22, 23], clue: 'Representación mental, pensamiento o propuesta.' },
        RISA: { number: 3, direction: 'down', indices: [8, 13, 18, 23], clue: 'Expresión de alegría que suele acompañarse de sonidos.' }
      },
      order: ['LIBRO', 'IDEA', 'RISA']
    }
  };

  const challengeIds = Object.keys(puzzles);
  const states = Object.fromEntries(challengeIds.map(id => [id, createEmptyState(id)]));

  let selectedChallenge = DEFAULT_CHALLENGE;
  let restored = false;

  function createEmptyState(challengeId) {
    const puzzle = puzzles[challengeId];
    return {
      best: 0,
      solved: new Set(),
      active: puzzle.order[0]
    };
  }

  function normalizeState(challengeId, storedState = {}) {
    const puzzle = puzzles[challengeId];
    const validWords = new Set(puzzle.order);
    const solved = new Set(
      Array.isArray(storedState.solved)
        ? storedState.solved.filter(word => validWords.has(word))
        : []
    );
    const storedBest = Number(storedState.best);
    const best = Number.isFinite(storedBest)
      ? Math.max(solved.size, Math.min(puzzle.order.length, Math.max(0, Math.floor(storedBest))))
      : solved.size;
    const active = validWords.has(storedState.active)
      ? storedState.active
      : puzzle.order[0];

    return { best, solved, active };
  }

  function readStoredProgress() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return;
      }

      const stored = JSON.parse(raw);

      if (stored && typeof stored.challenges === 'object' && stored.challenges !== null) {
        challengeIds.forEach(id => {
          states[id] = normalizeState(id, stored.challenges[id]);
        });

        if (challengeIds.includes(stored.selectedChallenge)) {
          selectedChallenge = stored.selectedChallenge;
        }
      } else {
        // Compatibilidad con las versiones 1 y 2: el progreso previo pertenece al reto original.
        states[DEFAULT_CHALLENGE] = normalizeState(DEFAULT_CHALLENGE, stored || {});
      }

      restored = challengeIds.some(id => states[id].solved.size > 0);
    } catch (_) {
      // El juego continúa en memoria cuando el almacenamiento no está disponible o no es válido.
    }
  }

  function persistProgress() {
    try {
      const challenges = {};

      challengeIds.forEach(id => {
        const state = states[id];
        challenges[id] = {
          best: state.best,
          solved: [...state.solved],
          active: state.active
        };
      });

      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        version: 3,
        selectedChallenge,
        challenges
      }));
    } catch (_) {
      // Los retos siguen funcionando durante la sesión aunque no puedan persistirse.
    }
  }

  function currentPuzzle() {
    return puzzles[selectedChallenge];
  }

  function currentState() {
    return states[selectedChallenge];
  }

  function occupiedIndices(puzzle) {
    const occupied = new Set();
    puzzle.order.forEach(word => {
      puzzle.words[word].indices.forEach(index => occupied.add(index));
    });
    return occupied;
  }

  function buildBoard() {
    const puzzle = currentPuzzle();
    const occupied = occupiedIndices(puzzle);
    board.innerHTML = '';

    for (let index = 0; index < 25; index += 1) {
      const cell = document.createElement('span');
      const isBlock = !occupied.has(index);
      cell.className = isBlock ? 'cell block' : 'cell';
      cell.dataset.index = String(index);
      cell.setAttribute('role', 'gridcell');

      if (isBlock) {
        cell.setAttribute('aria-hidden', 'true');
      }

      board.appendChild(cell);
    }
  }

  function createClueButton(word) {
    const puzzle = currentPuzzle();
    const entry = puzzle.words[word];
    const button = document.createElement('button');
    button.className = 'clue';
    button.type = 'button';
    button.dataset.word = word;

    const number = document.createElement('span');
    number.textContent = String(entry.number);

    const text = document.createElement('span');
    text.textContent = entry.clue;

    button.append(number, text);
    button.addEventListener('click', () => selectWord(word));
    return button;
  }

  function buildClues() {
    const puzzle = currentPuzzle();
    acrossClues.innerHTML = '';
    downClues.innerHTML = '';

    puzzle.order.forEach(word => {
      const target = puzzle.words[word].direction === 'down' ? downClues : acrossClues;
      target.appendChild(createClueButton(word));
    });
  }

  function renderBoard() {
    const puzzle = currentPuzzle();
    const state = currentState();

    $$('.cell').forEach(cell => {
      if (!cell.classList.contains('block')) {
        cell.textContent = '';
        cell.classList.remove('solved');
      }
    });

    state.solved.forEach(word => {
      puzzle.words[word].indices.forEach((index, letterIndex) => {
        const cell = board.querySelector(`[data-index="${index}"]`);
        if (cell) {
          cell.textContent = word[letterIndex];
          cell.classList.add('solved');
        }
      });
    });
  }

  function renderClueState() {
    const state = currentState();
    $$('.clue').forEach(clue => {
      const word = clue.dataset.word;
      clue.classList.toggle('done', state.solved.has(word));
      clue.classList.toggle('active', word === state.active);
      clue.setAttribute('aria-pressed', String(word === state.active));
    });
  }

  function renderChallengeSelector() {
    challengeButtons.forEach(button => {
      const id = button.dataset.challenge;
      const puzzle = puzzles[id];
      const state = states[id];
      if (!puzzle || !state) {
        return;
      }

      const active = id === selectedChallenge;
      button.classList.toggle('active', active);
      button.setAttribute('aria-pressed', String(active));

      const status = button.querySelector('.challenge-status');
      if (status) {
        status.textContent = state.solved.size === puzzle.order.length
          ? 'Completado'
          : `${state.solved.size}/${puzzle.order.length}`;
      }
    });
  }

  function renderHeader() {
    const puzzle = currentPuzzle();
    if (gameKicker) gameKicker.textContent = puzzle.kicker;
    if (gameTitle) gameTitle.textContent = puzzle.title;
    if (challengeSummary) challengeSummary.textContent = puzzle.summary;
  }

  function renderStats() {
    const puzzle = currentPuzzle();
    const state = currentState();
    const total = puzzle.order.length;

    if (state.solved.size > state.best) {
      state.best = state.solved.size;
    }

    if (solvedCount) solvedCount.textContent = `${state.solved.size}/${total}`;
    if (bestScore) bestScore.textContent = `${state.best}/${total}`;
    if (progressState) {
      progressState.textContent = state.solved.size === total
        ? 'Completado'
        : state.solved.size
          ? 'En progreso'
          : 'Comenzar';
    }
  }

  function render() {
    renderBoard();
    renderClueState();
    renderChallengeSelector();
    renderHeader();
    renderStats();
    persistProgress();
  }

  function selectWord(word, focusInput = true) {
    const puzzle = currentPuzzle();
    const state = currentState();

    if (!puzzle.words[word]) {
      return;
    }

    state.active = word;
    renderClueState();
    persistProgress();
    input.value = '';
    if (focusInput) input.focus();

    if (message) {
      message.className = 'game-message';
      message.textContent = state.solved.has(word)
        ? 'Esta palabra ya está resuelta.'
        : 'Escribe tu respuesta y compruébala.';
    }
  }

  function selectChallenge(challengeId, focusInput = true) {
    if (!puzzles[challengeId]) {
      return;
    }

    selectedChallenge = challengeId;
    buildBoard();
    buildClues();
    render();

    if (focusInput) input.focus();

    if (message) {
      const puzzle = currentPuzzle();
      const state = currentState();
      message.className = 'game-message';
      message.textContent = `Reto seleccionado: ${puzzle.title}. Progreso ${state.solved.size} de ${puzzle.order.length}.`;
    }
  }

  function verify() {
    const puzzle = currentPuzzle();
    const state = currentState();
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

    if (answer === state.active) {
      state.solved.add(state.active);
      render();

      if (message) {
        message.className = 'game-message ok';
        message.textContent = state.solved.size === puzzle.order.length
          ? `¡${puzzle.title} completado! Has resuelto las ${puzzle.order.length} palabras.`
          : '¡Correcto! Tu avance quedó guardado. Selecciona otra pista cuando quieras.';
      }
      input.value = '';
      return;
    }

    if (message) {
      message.className = 'game-message error';
      message.textContent = 'Aún no coincide. Revisa la pista e inténtalo otra vez.';
    }
  }

  challengeButtons.forEach(button => {
    button.addEventListener('click', () => selectChallenge(button.dataset.challenge));
  });

  check.addEventListener('click', verify);
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      verify();
    }
  });

  reset.addEventListener('click', () => {
    const puzzle = currentPuzzle();
    const state = currentState();
    state.solved = new Set();
    state.active = puzzle.order[0];
    render();
    selectWord(state.active);

    if (message) {
      message.className = 'game-message';
      message.textContent = `Reto reiniciado: ${puzzle.title}. La mejor marca de este reto se conserva.`;
    }
  });

  readStoredProgress();
  selectChallenge(selectedChallenge, false);

  if (restored && message) {
    const puzzle = currentPuzzle();
    const state = currentState();
    message.className = 'game-message ok';
    message.textContent = state.solved.size
      ? `Progreso restaurado en ${puzzle.title}: ${state.solved.size} de ${puzzle.order.length} palabras resueltas.`
      : 'Tus progresos guardados en Crucilux están disponibles en el selector de retos.';
  }
})();
