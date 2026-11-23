<script setup>
// Abertura animada (cerca de 5 s): o traço do batimento cruza a tela, vira o pulso do símbolo,
// a mão sobe, o conjunto dá uma batida e entra o nome. Aparece só na primeira visita da sessão.
// Com "reduzir movimentos" (ou preferência do sistema) a mesma abertura acontece só esmaecendo.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUiStore } from '../stores/ui.js'

const emit = defineEmits(['fim'])
const ui = useUiStore()
const saindo = ref(false)
const botao = ref(null)
let timer
let fecho

const suave = computed(() => ui.prefs.reduzirMovimento || (typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches))
const letras = (t, ini, cls) => t.split('').map((c, i) => ({ c, i: ini + i, cls }))
const nome = [...letras('MedFlow', 0, 'm'), { c: ' ', i: 7, cls: 'esp' }, ...letras('Solutions', 8, 's')]

function encerrar() {
  if (saindo.value) return
  saindo.value = true
  clearTimeout(timer)
  fecho = setTimeout(() => emit('fim'), 450)
}
function tecla(e) { if (e.key === 'Escape') encerrar() }
onMounted(() => {
  botao.value?.focus()
  window.addEventListener('keydown', tecla)
  timer = setTimeout(encerrar, suave.value ? 3800 : 5200)
})
onBeforeUnmount(() => { clearTimeout(timer); clearTimeout(fecho); window.removeEventListener('keydown', tecla) })
</script>

<template>
  <div class="abertura" :class="{ saindo, suave }" role="dialog" aria-modal="true" aria-label="Abertura do MedFlow Solutions">
    <div class="ab-palco">
      <svg class="ab-fio" viewBox="0 0 800 500" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 180 H352 L368 180 L384 150 L398 218 L412 160 L424 180 H448 L800 180" pathLength="1" />
      </svg>
      <span class="ab-anel" aria-hidden="true"></span>
      <div class="ab-conj"><div class="ab-mao"><img src="/mao-medflow.png" alt="" /></div></div>
      <p class="ab-nome" aria-hidden="true"><span v-for="l in nome" :key="l.i" :class="l.cls" :style="{ '--i': l.i }">{{ l.c }}</span></p>
      <p class="ab-lema">A dose certa, no horário certo.</p>
    </div>
    <button ref="botao" class="ab-pular" type="button" @click="encerrar">Pular</button>
  </div>
</template>

<style>
@property --ab-a { syntax: '<percentage>'; inherits: false; initial-value: 0%; }
@property --ab-b { syntax: '<percentage>'; inherits: false; initial-value: 100%; }
.abertura { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; overflow: hidden;
  background: linear-gradient(160deg, #ffffff 0%, #dff3f1 52%, #cfe3f5 100%); transition: opacity .45s ease; }
.abertura.saindo { opacity: 0; pointer-events: none; }
.ab-palco { position: relative; width: min(1000px, 100vw, 160vh); aspect-ratio: 16 / 10; }
.ab-fio { position: absolute; inset: 0; width: 100%; height: 100%; mask-image: linear-gradient(90deg, transparent var(--ab-a), #000 calc(var(--ab-a) + 5%), #000 calc(var(--ab-b) - 5%), transparent var(--ab-b)); animation: ab-afina .9s 3s ease-in forwards; }
.ab-fio path { fill: none; stroke: #12a865; stroke-width: 3.2; stroke-linecap: round; stroke-linejoin: round; stroke-dasharray: 1; stroke-dashoffset: 1; animation: ab-tracar 2.5s .3s linear forwards; }
.ab-conj { position: absolute; inset: 0; transform-origin: 50% 40%; animation: ab-bate .55s 2.45s ease-out; }
.ab-mao { position: absolute; left: 40%; width: 20%; top: 31%; opacity: 0; animation: ab-sobe .9s 1.4s cubic-bezier(.2, .8, .2, 1) forwards; }
.ab-mao img { width: 100%; display: block; }
.ab-anel { position: absolute; left: 50%; top: 36%; width: 14%; aspect-ratio: 1; margin: -7% 0 0 -7%; border-radius: 50%; border: 2px solid #12a865; opacity: 0; animation: ab-onda 1.1s 2.5s ease-out; }
.ab-nome { position: absolute; left: 0; right: 0; top: 68%; margin: 0; text-align: center; font-family: var(--f-display, "Lexend", sans-serif); font-weight: 700; font-size: clamp(1.25rem, 4vw, 1.95rem); letter-spacing: -.01em; white-space: nowrap; color: #0f2a3d; }
.ab-nome span { display: inline-block; opacity: 0; transform: translateY(10px); animation: ab-letra .5s ease-out forwards; animation-delay: calc(3.3s + var(--i) * .045s); }
.ab-nome .s { font-weight: 400; color: #02756e; }
.ab-nome .esp { width: .3em; }
.ab-lema { position: absolute; left: 0; right: 0; top: 83%; margin: 0; text-align: center; font-size: clamp(.8rem, 2vw, 1rem); color: #43596b; opacity: 0; animation: ab-surge .7s 4.1s ease-out forwards; }
.ab-pular { position: absolute; right: 16px; bottom: 16px; border: 1px solid #7f98a6; background: rgba(255, 255, 255, .9); color: #0f2a3d; border-radius: 8px; padding: 8px 16px; cursor: pointer; font: inherit; }
.ab-pular:hover { border-color: #02756e; color: #02756e; }
@keyframes ab-tracar { to { stroke-dashoffset: 0; } }
@keyframes ab-afina { to { --ab-a: 36%; --ab-b: 64%; } }
@keyframes ab-sobe { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: none; } }
@keyframes ab-bate { 0% { transform: scale(1); } 35% { transform: scale(1.07); } 100% { transform: scale(1); } }
@keyframes ab-onda { 0% { opacity: .55; transform: scale(.7); } 100% { opacity: 0; transform: scale(3); } }
@keyframes ab-letra { to { opacity: 1; transform: none; } }
@keyframes ab-surge { to { opacity: 1; } }
/* Movimento reduzido: o pulso e a mão esmaecem juntos; nada se desloca */
.abertura.suave .ab-anel { display: none; }
.abertura.suave .ab-conj, .abertura.suave .ab-mao, .abertura.suave .ab-nome span, .abertura.suave .ab-lema { animation-name: ab-surge; animation-timing-function: ease-out; transform: none; }
.abertura.suave .ab-conj { animation-duration: .01s; animation-delay: 0s; }
.abertura.suave .ab-mao { animation-duration: 1s; animation-delay: .2s; }
.abertura.suave .ab-nome span { animation-duration: 1s; animation-delay: .9s; }
.abertura.suave .ab-lema { animation-duration: 1s; animation-delay: 1.5s; }
.abertura.suave .ab-fio { animation: ab-surge 1s .2s ease-out both; opacity: 0; mask-image: linear-gradient(90deg, transparent 36%, #000 40%, #000 60%, transparent 64%); }
.abertura.suave .ab-fio path { animation: none; stroke-dashoffset: 0; }
</style>
