<script setup>
// Fundo decorativo: comprimidos, cápsulas e sachês atravessando a tela bem devagar, por trás do texto.
// Só enfeite (aria-hidden). Com "reduzir movimentos" ficam parados.
const ITENS = [
  { tipo: 'capsula', y: 9, x: 12, s: 1.0, r: -18, d: 110, f: 0.10, volta: false },
  { tipo: 'comprimido', y: 22, x: 70, s: 0.9, r: 10, d: 95, f: 0.55, volta: true },
  { tipo: 'sache', y: 36, x: 34, s: 1.1, r: 8, d: 120, f: 0.80, volta: false },
  { tipo: 'capsula', y: 52, x: 84, s: 1.2, r: 34, d: 105, f: 0.30, volta: true },
  { tipo: 'comprimido', y: 66, x: 20, s: 0.8, r: -8, d: 90, f: 0.65, volta: false },
  { tipo: 'sache', y: 80, x: 62, s: 0.9, r: -12, d: 115, f: 0.05, volta: true },
  { tipo: 'comprimido', y: 92, x: 46, s: 1.0, r: 16, d: 100, f: 0.45, volta: false },
]
</script>

<template>
  <div class="fundo-remedios" aria-hidden="true">
    <span v-for="(i, n) in ITENS" :key="n" class="rem" :class="{ volta: i.volta }" :style="{ top: `${i.y}%`, '--x': i.x, '--s': i.s, '--r': `${i.r}deg`, '--d': `${i.d}s`, '--a': `-${(i.d * i.f).toFixed(1)}s` }">
      <svg v-if="i.tipo === 'capsula'" viewBox="0 0 72 28" width="72" height="28">
        <rect x="1" y="1" width="70" height="26" rx="13" fill="#ffffff" stroke="#02756e" stroke-width="2" />
        <path d="M36 1 H58 a13 13 0 0 1 0 26 H36 Z" fill="#02756e" />
      </svg>
      <svg v-else-if="i.tipo === 'comprimido'" viewBox="0 0 44 44" width="44" height="44">
        <circle cx="22" cy="22" r="20" fill="#ffffff" stroke="#0b6a8f" stroke-width="2" />
        <path d="M8 22 H36" stroke="#0b6a8f" stroke-width="2.5" stroke-linecap="round" />
      </svg>
      <svg v-else viewBox="0 0 64 42" width="64" height="42">
        <path d="M2 6 l4 -4 l4 4 l4 -4 l4 4 l4 -4 l4 4 l4 -4 l4 4 l4 -4 l4 4 l4 -4 l4 4 V40 H2 Z" fill="#ffffff" stroke="#02756e" stroke-width="2" stroke-linejoin="round" />
        <path d="M10 18 H38 M10 26 H30" stroke="#0b6a8f" stroke-width="2.5" stroke-linecap="round" />
        <circle cx="50" cy="22" r="5" fill="#12a865" />
      </svg>
    </span>
  </div>
</template>

<style>
.fundo-remedios { position: absolute; inset: 0; overflow: hidden; pointer-events: none; z-index: 0; container-type: inline-size; }
.fundo-remedios .rem { position: absolute; left: 0; opacity: .42; animation: rem-cruza var(--d) linear var(--a) infinite; will-change: transform; }
.fundo-remedios .rem.volta { animation-direction: reverse; }
.fundo-remedios svg { display: block; animation: rem-bob 12s ease-in-out infinite alternate; }
@keyframes rem-cruza { from { transform: translate3d(-14cqw, 0, 0); } to { transform: translate3d(114cqw, 0, 0); } }
@keyframes rem-bob {
  from { transform: translateY(-9px) scale(var(--s)) rotate(var(--r)); }
  to { transform: translateY(9px) scale(var(--s)) rotate(calc(var(--r) + 16deg)); }
}
/* Parados, cada um no seu lugar */
@media (prefers-reduced-motion: reduce) {
  .fundo-remedios .rem { animation: none; transform: translate3d(calc(var(--x) * 1cqw), 0, 0); }
  .fundo-remedios svg { animation: none; transform: scale(var(--s)) rotate(var(--r)); }
}
:root[data-movimento="reduzido"] .fundo-remedios .rem { animation: none; transform: translate3d(calc(var(--x) * 1cqw), 0, 0); }
:root[data-movimento="reduzido"] .fundo-remedios svg { animation: none; transform: scale(var(--s)) rotate(var(--r)); }
:root[data-theme="dark"] .fundo-remedios .rem { opacity: .3; }
</style>
