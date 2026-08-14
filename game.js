(() => {
  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];

  const board = $('#crossword');
  const check = $('#checkAnswer');
  const clearWordButton = $('#clearWord');
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

  if (!board || !check || !clearWordButton || !reset || !acrossClues || !downClues) {
    return;
  }

  const STORAGE_KEY = 'crucilux-progress-v1';
  const STORAGE_VERSION = 4;
  const DEFAULT_CHALLENGE = 'cotidianas';
  const GRID_SIZE = 5;

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

  function normalizeLetter(value) {
    return String(value ?? '')
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^A-ZÑ]/g, '')
      .slice(-1);
  }

  function occupiedIndices(puzzle) {
    const occupied = new Set();
    puzzle.order.forEach(word => {
      puzzle.words[word].indices.forEach(index => occupied.add(index));
    });
    return occupied;
  }

  function wordsForIndex(puzzle, index) {
    return puzzle.order.filter(word => puzzle.words[word].indices.includes(index));
  }

  function firstIndexForWord(puzzle, word) {
    return puzzle.words[word].indices[0];
  }

  function createEmptyState(challengeId) {
    const puzzle = puzzles[challengeId];
    return {
      best: 0,
      solved: new Set(),
      active: puzzle.order[0],
      letters: {},
      selectedIndex: firstIndexForWord(puzzle, puzzle.order[0])
    };
  }

  function normalizeState(challengeId, storedState = {}) {
    const puzzle = puzzles[challengeId];
    const occupied = occupiedIndices(puzzle);
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
    const letters = {};

    if (storedState.letters && typeof storedState.letters === 'object') {
      Object.entries(storedState.letters).forEach(([key, value]) => {
        const index = Number(key);
        const letter = normalizeLetter(value);
        if (Number.isInteger(index) && occupied.has(index) && letter) {
          letters[index] = letter;
        }
      });
    }

    // Las versiones anteriores solo guardaban palabras resueltas: sus letras se reconstruyen aquí.
    solved.forEach(word => {
      puzzle.words[word].indices.forEach((index, letterIndex) => {
        letters[index] = word[letterIndex];
      });
    });

    const storedSelected = Number(storedState.selectedIndex);
    const selectedIndex = Number.isInteger(storedSelected) && occupied.has(storedSelected)
      ? storedSelected
      : firstIndexForWord(puzzle, active);

    return { best, solved, active, letters, selectedIndex };
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

      restored = challengeIds.some(id => {
        const state = states[id];
        return state.solved.size > 0 || Object.keys(state.letters).length > 0;
      });
    } catch (_) {
      // Los retos continúan en memoria cuando el almacenamiento no está disponible o no es válido.
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
          active: state.active,
          letters: state.letters,
          selectedIndex: state.selectedIndex
        };
      });

      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        version: STORAGE_VERSION,
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

  function isLockedIndex(index) {
    const puzzle = currentPuzzle();
    const state = currentState();
    return wordsForIndex(puzzle, index).some(word => state.solved.has(word));
  }

  function startNumberForIndex(puzzle, index) {
    const starter = puzzle.order.find(word => puzzle.words[word].indices[0] === index);
    return starter ? puzzle.words[starter].number : null;
  }

  function directionLabel(direction) {
    return direction === 'down' ? 'vertical' : 'horizontal';
  }

  function cellLabel(index) {
    const puzzle = currentPuzzle();
    const state = currentState();
    const row = Math.floor(index / GRID_SIZE) + 1;
    const column = (index % GRID_SIZE) + 1;
    const letter = state.letters[index] || '';
    const memberships = wordsForIndex(puzzle, index)
      .map(word => `${puzzle.words[word].number} ${directionLabel(puzzle.words[word].direction)}`)
      .join(' y ');
    const status = isLockedIndex(index) ? 'Casilla resuelta.' : 'Casilla editable.';
    return `Fila ${row}, columna ${column}. ${letter ? `Letra ${letter}.` : 'Vacía.'} ${status} Pistas ${memberships}.`;
  }

  function buildBoard() {
    const puzzle = currentPuzzle();
    const occupied = occupiedIndices(puzzle);
    board.innerHTML = '';
    board.setAttribute('aria-rowcount', String(GRID_SIZE));
    board.setAttribute('aria-colcount', String(GRID_SIZE));

    for (let index = 0; index < GRID_SIZE * GRID_SIZE; index += 1) {
      const cell = document.createElement('div');
      const isBlock = !occupied.has(index);
      cell.className = isBlock ? 'cell block' : 'cell';
      cell.dataset.index = String(index);
      cell.setAttribute('role', 'gridcell');
      cell.setAttribute('aria-rowindex', String(Math.floor(index / GRID_SIZE) + 1));
      cell.setAttribute('aria-colindex', String((index % GRID_SIZE) + 1));

      if (isBlock) {
        cell.setAttribute('aria-hidden', 'true');
        board.appendChild(cell);
        continue;
      }

      const clueNumber = startNumberForIndex(puzzle, index);
      if (clueNumber !== null) {
        const number = document.createElement('span');
        number.className = 'cell-number';
        number.textContent = String(clueNumber);
        number.setAttribute('aria-hidden', 'true');
        cell.appendChild(number);
      }

      const letterInput = document.createElement('input');
      letterInput.className = 'cell-input';
      letterInput.type = 'text';
      letterInput.maxLength = 1;
      letterInput.autocomplete = 'off';
      letterInput.autocapitalize = 'characters';
      letterInput.spellcheck = false;
      letterInput.inputMode = 'text';
      letterInput.dataset.index = String(index);
      letterInput.addEventListener('focus', () => selectCell(index, false));
      letterInput.addEventListener('click', () => letterInput.select());
      letterInput.addEventListener('input', event => handleCellInput(event, index));
      letterInput.addEventListener('keydown', event => handleCellKeydown(event, index));
      cell.appendChild(letterInput);
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
    const activeIndices = new Set(puzzle.words[state.active].indices);

    $$('.cell:not(.block)').forEach(cell => {
      const index = Number(cell.dataset.index);
      const input = cell.querySelector('.cell-input');
      const locked = isLockedIndex(index);
      const selected = index === state.selectedIndex;

      cell.classList.toggle('active-word', activeIndices.has(index));
      cell.classList.toggle('selected', selected);
      cell.classList.toggle('solved', locked);
      cell.classList.toggle('locked', locked);
      cell.setAttribute('aria-selected', String(selected));
      cell.setAttribute('aria-readonly', String(locked));

      if (input) {
        input.value = state.letters[index] || '';
        input.readOnly = locked;
        input.tabIndex = selected ? 0 : -1;
        input.setAttribute('aria-label', cellLabel(index));
      }
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
        : state.solved.size || Object.keys(state.letters).length
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

  function focusCell(index) {
    const input = board.querySelector(`.cell-input[data-index="${index}"]`);
    if (input) {
      input.focus();
      input.select();
    }
  }

  function chooseCellForWord(word) {
    const puzzle = currentPuzzle();
    const state = currentState();
    const indices = puzzle.words[word].indices;
    const currentIsUseful = indices.includes(state.selectedIndex) && !isLockedIndex(state.selectedIndex);
    if (currentIsUseful) {
      return state.selectedIndex;
    }

    return indices.find(index => !isLockedIndex(index) && !state.letters[index])
      ?? indices.find(index => !isLockedIndex(index))
      ?? indices[0];
  }

  function selectWord(word, focus = true) {
    const puzzle = currentPuzzle();
    const state = currentState();

    if (!puzzle.words[word]) {
      return;
    }

    state.active = word;
    state.selectedIndex = chooseCellForWord(word);
    renderBoard();
    renderClueState();
    persistProgress();

    if (focus) {
      focusCell(state.selectedIndex);
    }

    if (message) {
      message.className = 'game-message';
      message.textContent = state.solved.has(word)
        ? 'Esta palabra ya está resuelta. Puedes usar sus letras en los cruces.'
        : 'Escribe una letra directamente en cada casilla y luego comprueba la palabra.';
    }
  }

  function selectCell(index, focus = true) {
    const puzzle = currentPuzzle();
    const state = currentState();
    const memberships = wordsForIndex(puzzle, index);

    if (!memberships.length) {
      return;
    }

    state.selectedIndex = index;
    if (!memberships.includes(state.active)) {
      state.active = memberships[0];
    }

    renderBoard();
    renderClueState();
    persistProgress();

    if (focus) {
      focusCell(index);
    }
  }

  function nextEditableInActive(index, delta) {
    const puzzle = currentPuzzle();
    const state = currentState();
    const indices = puzzle.words[state.active].indices;
    const position = indices.indexOf(index);

    if (position === -1) {
      return null;
    }

    for (let next = position + delta; next >= 0 && next < indices.length; next += delta) {
      if (!isLockedIndex(indices[next])) {
        return indices[next];
      }
    }

    return null;
  }

  function handleCellInput(event, index) {
    const state = currentState();
    if (isLockedIndex(index)) {
      event.target.value = state.letters[index] || '';
      return;
    }

    const letter = normalizeLetter(event.target.value);
    if (letter) {
      state.letters[index] = letter;
      event.target.value = letter;
    } else {
      delete state.letters[index];
      event.target.value = '';
    }

    state.selectedIndex = index;
    renderStats();
    persistProgress();

    if (letter) {
      const next = nextEditableInActive(index, 1);
      if (next !== null) {
        state.selectedIndex = next;
        renderBoard();
        persistProgress();
        focusCell(next);
      } else if (message) {
        message.className = 'game-message';
        message.textContent = 'La palabra activa tiene todas sus casillas completas. Pulsa “Comprobar palabra”.';
      }
    }
  }

  function moveByGrid(index, rowDelta, columnDelta) {
    const row = Math.floor(index / GRID_SIZE);
    const column = index % GRID_SIZE;
    const targetRow = row + rowDelta;
    const targetColumn = column + columnDelta;

    if (targetRow < 0 || targetRow >= GRID_SIZE || targetColumn < 0 || targetColumn >= GRID_SIZE) {
      return;
    }

    const target = targetRow * GRID_SIZE + targetColumn;
    if (occupiedIndices(currentPuzzle()).has(target)) {
      selectCell(target);
    }
  }

  function handleCellKeydown(event, index) {
    const state = currentState();

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      moveByGrid(index, 0, -1);
      return;
    }
    if (event.key === 'ArrowRight') {
      event.preventDefault();
      moveByGrid(index, 0, 1);
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      moveByGrid(index, -1, 0);
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      moveByGrid(index, 1, 0);
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      checkActiveWord();
      return;
    }
    if (event.key === 'Delete') {
      if (!isLockedIndex(index)) {
        event.preventDefault();
        delete state.letters[index];
        renderBoard();
        renderStats();
        persistProgress();
        focusCell(index);
      }
      return;
    }
    if (event.key === 'Backspace' && !isLockedIndex(index)) {
      event.preventDefault();
      if (state.letters[index]) {
        delete state.letters[index];
        renderBoard();
        renderStats();
        persistProgress();
        focusCell(index);
      } else {
        const previous = nextEditableInActive(index, -1);
        if (previous !== null) {
          delete state.letters[previous];
          state.selectedIndex = previous;
          renderBoard();
          renderStats();
          persistProgress();
          focusCell(previous);
        }
      }
    }
  }

  function checkActiveWord() {
    const puzzle = currentPuzzle();
    const state = currentState();
    const word = state.active;
    const entry = puzzle.words[word];

    if (state.solved.has(word)) {
      if (message) {
        message.className = 'game-message ok';
        message.textContent = 'Esta palabra ya está resuelta correctamente.';
      }
      return;
    }

    const letters = entry.indices.map(index => state.letters[index] || '');
    const missing = letters.filter(letter => !letter).length;

    if (missing) {
      if (message) {
        message.className = 'game-message error';
        message.textContent = `Faltan ${missing} ${missing === 1 ? 'letra' : 'letras'} para comprobar esta palabra.`;
      }
      return;
    }

    if (letters.join('') === word) {
      state.solved.add(word);
      entry.indices.forEach((index, letterIndex) => {
        state.letters[index] = word[letterIndex];
      });
      render();

      if (message) {
        message.className = 'game-message ok';
        message.textContent = state.solved.size === puzzle.order.length
          ? `¡${puzzle.title} completado! Has resuelto las ${puzzle.order.length} palabras.`
          : '¡Correcto! La palabra quedó fijada y sus cruces pueden ayudarte con las demás.';
      }
      return;
    }

    if (message) {
      message.className = 'game-message error';
      message.textContent = 'La palabra todavía no coincide con la pista. Revisa las letras e inténtalo otra vez.';
    }
  }

  function clearActiveWord() {
    const puzzle = currentPuzzle();
    const state = currentState();
    const word = state.active;

    if (state.solved.has(word)) {
      if (message) {
        message.className = 'game-message';
        message.textContent = 'La palabra ya está resuelta y permanece fijada en el tablero.';
      }
      return;
    }

    puzzle.words[word].indices.forEach(index => {
      if (!isLockedIndex(index)) {
        delete state.letters[index];
      }
    });
    state.selectedIndex = chooseCellForWord(word);
    render();
    focusCell(state.selectedIndex);

    if (message) {
      message.className = 'game-message';
      message.textContent = 'Se borraron las letras editables de la palabra activa. Los cruces ya resueltos se conservan.';
    }
  }

  function selectChallenge(challengeId, focus = true) {
    if (!puzzles[challengeId]) {
      return;
    }

    selectedChallenge = challengeId;
    const puzzle = currentPuzzle();
    const state = currentState();
    const occupied = occupiedIndices(puzzle);

    if (!occupied.has(state.selectedIndex)) {
      state.selectedIndex = firstIndexForWord(puzzle, state.active);
    }

    buildBoard();
    buildClues();
    render();

    if (focus) {
      focusCell(state.selectedIndex);
    }

    if (message) {
      message.className = 'game-message';
      message.textContent = `Reto seleccionado: ${puzzle.title}. Progreso ${state.solved.size} de ${puzzle.order.length} palabras.`;
    }
  }

  challengeButtons.forEach(button => {
    button.addEventListener('click', () => selectChallenge(button.dataset.challenge));
  });

  check.addEventListener('click', checkActiveWord);
  clearWordButton.addEventListener('click', clearActiveWord);

  reset.addEventListener('click', () => {
    const puzzle = currentPuzzle();
    const state = currentState();
    state.solved = new Set();
    state.letters = {};
    state.active = puzzle.order[0];
    state.selectedIndex = firstIndexForWord(puzzle, state.active);
    render();
    focusCell(state.selectedIndex);

    if (message) {
      message.className = 'game-message';
      message.textContent = `Reto reiniciado: ${puzzle.title}. Se borraron las letras de la partida; la mejor marca se conserva.`;
    }
  });

  readStoredProgress();
  selectChallenge(selectedChallenge, false);

  if (restored && message) {
    const puzzle = currentPuzzle();
    const state = currentState();
    const enteredLetters = Object.keys(state.letters).length;
    message.className = 'game-message ok';
    message.textContent = state.solved.size || enteredLetters
      ? `Progreso restaurado en ${puzzle.title}: ${state.solved.size} de ${puzzle.order.length} palabras resueltas y ${enteredLetters} casillas con letra.`
      : 'Tus progresos guardados en Crucilux están disponibles en el selector de retos.';
  }
})();
