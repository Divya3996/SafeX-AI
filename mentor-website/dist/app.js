'use strict';

const menuToggle = document.getElementById('menu-toggle');
const menuClose = document.getElementById('menu-close');
const sidebar = document.getElementById('sidebar');
const backdrop = document.getElementById('nav-backdrop');
const navLinks = Array.from(document.querySelectorAll('.section-nav a'));
const sections = Array.from(document.querySelectorAll('.document-section'));
const progress = document.getElementById('reading-progress');
const printButton = document.getElementById('print-plan');
let isMenuOpen = false;
let scrollQueued = false;

function toggleMenu(open, restoreFocus = false) {
  isMenuOpen = open;
  sidebar.classList.toggle('is-open', open);
  backdrop.hidden = !open;
  menuToggle.setAttribute('aria-expanded', String(open));
  document.body.classList.toggle('nav-open', open);
  if (open) menuClose.focus();
  else if (restoreFocus) menuToggle.focus();
}

menuToggle.addEventListener('click', () => toggleMenu(!isMenuOpen));
menuClose.addEventListener('click', () => toggleMenu(false, true));
backdrop.addEventListener('click', () => toggleMenu(false, true));
document.addEventListener('keydown', (event) => {
  if (!isMenuOpen) return;
  if (event.key === 'Escape') {
    toggleMenu(false, true);
    return;
  }
  if (event.key === 'Tab') {
    const controls = Array.from(sidebar.querySelectorAll('a, button')).filter(el => el.getClientRects().length);
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault(); last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault(); first.focus();
    }
  }
});

sidebar.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
  if (isMenuOpen) {
    toggleMenu(false);
    const target = document.getElementById(link.hash.slice(1));
    if (target) {
      target.setAttribute('tabindex', '-1');
      target.focus({preventScroll: true});
    }
  }
}));

function updateReadingState() {
  scrollQueued = false;
  const max = document.documentElement.scrollHeight - window.innerHeight;
  const fraction = max > 0 ? Math.max(0, Math.min(1, window.scrollY / max)) : 0;
  progress.style.transform = `scaleX(${fraction})`;
  let current = sections[0]?.id;
  const offset = window.innerWidth <= 1000 ? 140 : 130;
  for (const section of sections) {
    if (section.getBoundingClientRect().top <= offset) current = section.id;
    else break;
  }
  if (fraction > 0.99) current = sections[sections.length - 1]?.id;
  navLinks.forEach(link => {
    if (link.dataset.section === current) link.setAttribute('aria-current', 'location');
    else link.removeAttribute('aria-current');
  });
}

window.addEventListener('scroll', () => {
  if (!scrollQueued) {
    scrollQueued = true;
    requestAnimationFrame(updateReadingState);
  }
}, {passive: true});
window.addEventListener('resize', () => {
  if (window.innerWidth > 1000 && isMenuOpen) toggleMenu(false);
  updateReadingState();
});
window.addEventListener('load', updateReadingState);
printButton.hidden = false;
printButton.addEventListener('click', () => {
  if (isMenuOpen) toggleMenu(false);
  window.print();
});
updateReadingState();
