(() => {
  const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];
  const board=$('#crossword'),input=$('#answerInput'),check=$('#checkAnswer'),reset=$('#resetGame'),msg=$('#gameMessage');
  const solvedCount=$('#solvedCount'),bestScore=$('#bestScore'),state=$('#progressState'),clues=$$('.clue');
  if(!board||!input||!check||!reset)return;
  const KEY='crucilux-progress-v1';
  const words={SOL:[1,2,3],MAR:[15,16,17],LUZ:[3,8,13]};
  const blocks=new Set([0,4,5,6,9,10,11,14,18,19,20,21,22,23,24]);
  let active='SOL',solved=new Set(),best=0;
  try{best=Number(JSON.parse(localStorage.getItem(KEY)||'{}').best)||0;}catch(_){ }
  function build(){
    board.innerHTML='';
    for(let i=0;i<25;i++){
      const cell=document.createElement('span');
      cell.className=blocks.has(i)?'cell block':'cell';cell.dataset.index=i;cell.setAttribute('role','gridcell');
      if(blocks.has(i))cell.setAttribute('aria-hidden','true');board.appendChild(cell);
    }
    render();
  }
  function render(){
    $$('.cell').forEach(c=>{if(!c.classList.contains('block')){c.textContent='';c.classList.remove('solved');}});
    solved.forEach(word=>words[word].forEach((index,n)=>{const c=board.querySelector(`[data-index="${index}"]`);if(c){c.textContent=word[n];c.classList.add('solved');}}));
    clues.forEach(c=>c.classList.toggle('done',solved.has(c.dataset.word)));
    if(solved.size>best){best=solved.size;try{localStorage.setItem(KEY,JSON.stringify({best}));}catch(_){ }}
    solvedCount.textContent=String(solved.size);bestScore.textContent=`${best}/3`;state.textContent=solved.size===3?'Completado':solved.size?'En progreso':'Comenzar';
  }
  function select(word){active=word;clues.forEach(c=>c.classList.toggle('active',c.dataset.word===word));input.value='';input.focus();msg.className='game-message';msg.textContent=solved.has(word)?'Esta palabra ya está resuelta.':'Escribe tu respuesta y compruébala.';}
  function verify(){
    const answer=input.value.trim().toUpperCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');
    if(!answer){msg.className='game-message error';msg.textContent='Escribe una respuesta antes de comprobar.';return;}
    if(answer===active){solved.add(active);render();msg.className='game-message ok';msg.textContent=solved.size===3?'¡Reto completado! Has resuelto las tres palabras.':'¡Correcto! Selecciona otra pista.';input.value='';}
    else{msg.className='game-message error';msg.textContent='Aún no coincide. Revisa la pista e inténtalo otra vez.';}
  }
  clues.forEach(c=>c.addEventListener('click',()=>select(c.dataset.word)));
  check.addEventListener('click',verify);input.addEventListener('keydown',e=>{if(e.key==='Enter')verify();});
  reset.addEventListener('click',()=>{solved=new Set();render();select('SOL');msg.textContent='Reto reiniciado. Selecciona una pista para comenzar.';});
  build();render();
})();