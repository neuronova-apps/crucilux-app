(() => {
  const css=document.createElement('link');
  css.rel='stylesheet';css.href='components.css';document.head.appendChild(css);
  const game=document.createElement('script');
  game.src='game.js';game.defer=true;document.head.appendChild(game);
  const menu=document.querySelector('.menu-button');
  const nav=document.querySelector('.main-nav');
  const year=document.querySelector('#year');
  if(year)year.textContent=new Date().getFullYear();
  if(menu&&nav){
    const close=()=>{nav.classList.remove('open');menu.setAttribute('aria-expanded','false');};
    menu.addEventListener('click',()=>{const open=nav.classList.toggle('open');menu.setAttribute('aria-expanded',String(open));});
    nav.querySelectorAll('a').forEach(a=>a.addEventListener('click',close));
    document.addEventListener('keydown',e=>{if(e.key==='Escape')close();});
  }
  const items=[...document.querySelectorAll('.reveal')];
  if('IntersectionObserver' in window&&!matchMedia('(prefers-reduced-motion: reduce)').matches){
    const obs=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('visible');obs.unobserve(entry.target);}}),{threshold:.12});
    items.forEach(item=>obs.observe(item));
  }else items.forEach(item=>item.classList.add('visible'));
})();