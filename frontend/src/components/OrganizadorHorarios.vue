<script setup>
// A Box vista pelo paciente como uma fita de sachês: um sachê por horário, com o dia, a hora e o que há dentro.
// O sachê da próxima dose fica destacado e se atualiza sozinho a cada minuto.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { periodo } from '../utils/formato.js'

const props = defineProps({ itens: { type: Array, required: true } })
const agora = ref(new Date())
let relogio
onMounted(() => { relogio = setInterval(() => { agora.value = new Date() }, 60_000) })
onBeforeUnmount(() => clearInterval(relogio))

const hhmm = computed(() => agora.value.toTimeString().slice(0, 5))

/** Começa pelo próximo sachê: primeiro os horários que ainda faltam hoje, depois os de amanhã. */
const compartimentos = computed(() => {
  const mapa = new Map()
  for (const i of props.itens) for (const h of i.horarios) mapa.set(h, [...(mapa.get(h) || []), { medicamento: i.medicamento, dose: i.dosePorHorario }])
  const horas = [...mapa.keys()].sort()
  const ordenadas = [...horas.filter((h) => h >= hhmm.value), ...horas.filter((h) => h < hhmm.value)]
  return ordenadas.map((h) => ({ hora: h, itens: mapa.get(h), unidades: mapa.get(h).reduce((s, x) => s + x.dose, 0) }))
})
/** Próximo horário de hoje; se todos já passaram, o primeiro de amanhã. */
const proxima = computed(() => compartimentos.value[0]?.hora)
const amanha = computed(() => !compartimentos.value.some((c) => c.hora >= hhmm.value))
const diaDe = (aoAmanha) => {
  const d = new Date(agora.value)
  if (aoAmanha) d.setDate(d.getDate() + 1)
  return d
}
const cabecalho = computed(() => diaDe(false).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }))
const curto = (d) => d.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' }).replace('.', '')
const diaDoSache = (c) => curto(diaDe(c.hora < hhmm.value))
</script>

<template>
  <div class="stack">
    <p class="subtle">Hoje é {{ cabecalho }}.<template v-if="proxima"> Próxima dose: <strong>{{ proxima }}</strong><template v-if="amanha"> (amanhã)</template>.</template></p>
    <div class="fita escalonar" role="list">
      <article v-for="c in compartimentos" :key="c.hora" class="sache" :class="{ proxima: c.hora === proxima }" role="listitem" :aria-label="`Sachê das ${c.hora}: ${c.itens.map((i) => i.medicamento).join(', ')}${c.hora === proxima ? '. Próxima dose' : ''}`">
        <div class="sache-topo"><span class="periodo">{{ periodo(c.hora) }}</span><span class="dia">{{ diaDoSache(c) }}</span></div>
        <div class="hora">{{ c.hora }}</div>
        <ul>
          <li v-for="i in c.itens" :key="i.medicamento">{{ i.medicamento }} <span class="dose">· {{ i.dose }} un.</span></li>
        </ul>
        <div class="sache-rodape"><span>{{ c.unidades }} {{ c.unidades === 1 ? 'unidade' : 'unidades' }}</span><strong v-if="c.hora === proxima" class="sache-prox">Próxima dose</strong></div>
      </article>
    </div>
  </div>
</template>
