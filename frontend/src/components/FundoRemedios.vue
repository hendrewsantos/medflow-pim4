<script setup>
// Fundo decorativo: comprimidos, cápsulas e sachês flutuando devagar. Só enfeite (aria-hidden), sem texto por baixo
// de contraste crítico; as formas usam a paleta da marca em baixa opacidade e ficam paradas com "reduzir movimentos".
const props = defineProps({ variante: { type: String, default: 'lateral' } })
// "lateral": só na faixa direita do painel; "faixas": só no alto e no pé da seção. Em ambos, longe do texto.
const PRESETS = {
  lateral: [
    { tipo: 'capsula', x: 70, y: 8, s: 1.0, r: -28, d: 19, a: 0 },
    { tipo: 'comprimido', x: 90, y: 22, s: 0.9, r: 12, d: 23, a: 3 },
    { tipo: 'sache', x: 78, y: 40, s: 1.1, r: 10, d: 21, a: 6 },
    { tipo: 'capsula', x: 86, y: 58, s: 1.2, r: 38, d: 25, a: 5 },
    { tipo: 'comprimido', x: 79, y: 72, s: 0.8, r: -8, d: 17, a: 2 },
    { tipo: 'sache', x: 88, y: 84, s: 0.9, r: -14, d: 22, a: 8 },
  ],
  faixas: [
    { tipo: 'capsula', x: 6, y: 2, s: 0.9, r: -20, d: 19, a: 0 },
    { tipo: 'comprimido', x: 34, y: 1, s: 0.8, r: 12, d: 23, a: 3 },
    { tipo: 'sache', x: 58, y: 1, s: 0.9, r: 8, d: 21, a: 6 },
    { tipo: 'comprimido', x: 92, y: 3, s: 0.8, r: -8, d: 17, a: 2 },
    { tipo: 'sache', x: 10, y: 92, s: 0.9, r: -10, d: 22, a: 8 },
    { tipo: 'capsula', x: 44, y: 94, s: 1.0, r: 30, d: 24, a: 4 },
    { tipo: 'comprimido', x: 80, y: 93, s: 0.9, r: -20, d: 20, a: 7 },
  ],
}
const ITENS = PRESETS[props.variante] ?? PRESETS.lateral
</script>

<template>
  <div class="fundo-remedios" aria-hidden="true">
    <span v-for="(i, n) in ITENS" :key="n" class="rem" :style="{ left: `${i.x}%`, top: `${i.y}%`, '--s': i.s, '--r': `${i.r}deg`, '--d': `${i.d}s`, '--a': `-${i.a}s` }">
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
.fundo-remedios { position: absolute; inset: 0; overflow: hidden; pointer-events: none; z-index: 0; }
.fundo-remedios .rem { position: absolute; opacity: .5; transform: scale(var(--s)) rotate(var(--r)); animation: rem-flutua var(--d) ease-in-out var(--a) infinite alternate; }
.fundo-remedios svg { display: block; }
@keyframes rem-flutua {
  from { transform: translate3d(0, 0, 0) scale(var(--s)) rotate(var(--r)); }
  to { transform: translate3d(14px, -26px, 0) scale(var(--s)) rotate(calc(var(--r) + 16deg)); }
}
@media (prefers-reduced-motion: reduce) { .fundo-remedios .rem { animation: none; } }
:root[data-movimento="reduzido"] .fundo-remedios .rem { animation: none; }
:root[data-theme="dark"] .fundo-remedios .rem { opacity: .35; }
</style>
