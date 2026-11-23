<script setup>
// Contagem regressiva do prazo de pagamento (mm:ss). Avisa uma única vez quando chega a zero.
// Não é região "ao vivo": leitores de tela não são interrompidos a cada segundo.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({ prazo: { type: String, required: true } })
const emit = defineEmits(['fim'])
const agora = ref(Date.now())
let relogio
let avisou = false

const restanteMs = computed(() => Math.max(0, new Date(props.prazo).getTime() - agora.value))
const texto = computed(() => {
  const total = Math.ceil(restanteMs.value / 1000)
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
})
const urgente = computed(() => restanteMs.value <= 30_000)

function tic() {
  agora.value = Date.now()
  if (restanteMs.value === 0 && !avisou) { avisou = true; emit('fim') }
}
onMounted(() => { tic(); relogio = setInterval(tic, 1000) })
onBeforeUnmount(() => clearInterval(relogio))
</script>

<template>
  <span class="contagem" :class="{ urgente }" role="timer" :aria-label="`Tempo restante para pagar: ${texto}`">{{ texto }}</span>
</template>

<style>
.contagem { font-family: var(--f-mono); font-weight: 600; font-size: 1.6rem; font-variant-numeric: tabular-nums; color: var(--ink); }
.contagem.urgente { color: var(--bad, #b3261e); }
</style>
