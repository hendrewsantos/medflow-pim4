<script setup>
// Assinatura simulada: a próxima Box é pedida sozinha 5 dias antes de a atual acabar, com os mesmos medicamentos e horários,
// e cobrada automaticamente (pagamento simulado). O paciente liga e desliga quando quiser.
import { computed, onMounted, ref } from 'vue'
import { api, MODO_DEMO } from '../services/api.js'
import { useUiStore } from '../stores/ui.js'
import { dataSo } from '../utils/formato.js'

const emit = defineEmits(['mudou'])
const ui = useUiStore()
const a = ref(null)
const ocupado = ref(false)
const botoesDeDemonstracao = MODO_DEMO || import.meta.env.VITE_BOTOES_PAGAMENTO === 'true'

async function carregar() {
  try { a.value = await api.assinatura.obter() } catch { a.value = null }
}
onMounted(carregar)

async function agir(fazer, aviso) {
  ocupado.value = true
  try {
    const r = await fazer()
    a.value = r.assinatura ?? r
    if (aviso) ui.avisar(typeof aviso === 'function' ? aviso(r) : aviso, 'ok')
    emit('mudou')
  } catch (e) {
    ui.erro(e)
  } finally {
    ocupado.value = false
  }
}
const ativar = () => agir(() => api.assinatura.ativar(), 'Renovação automática ativada. Você pode cancelar quando quiser.')
const cancelar = () => agir(() => api.assinatura.cancelar(), 'Renovação automática cancelada.')
const renovarAgora = () => agir(() => api.assinatura.renovarAgora(), (r) => (r.renovou ? 'Renovação feita: a nova Box já foi pedida e paga (simulado).' : `Não foi possível renovar agora: ${r.motivo}`))

const jaChegou = (iso) => iso && new Date(iso) <= new Date()
/** Já renovou depois da data prevista: a Box em andamento é a renovação. */
const renovouNesteCiclo = computed(() => !!a.value?.ultimaRenovacaoEm && !!a.value?.proximaRenovacaoEm && new Date(a.value.ultimaRenovacaoEm) >= new Date(a.value.proximaRenovacaoEm))
</script>

<template>
  <section v-if="a" class="card stack" aria-labelledby="renov-auto">
    <h3 id="renov-auto">Renovação automática</h3>
    <template v-if="a.ativa">
      <p class="muted">
        <template v-if="renovouNesteCiclo">A Box atual foi pedida automaticamente em <strong>{{ dataSo(a.ultimaRenovacaoEm) }}</strong>. A próxima será pedida sozinha depois da entrega desta.</template>
        <template v-else-if="a.proximaRenovacaoEm && !jaChegou(a.proximaRenovacaoEm)">A próxima Box será pedida sozinha em <strong>{{ dataSo(a.proximaRenovacaoEm) }}</strong>, com os mesmos medicamentos e horários.</template>
        <template v-else-if="a.proximaRenovacaoEm">A próxima Box já está na hora de ser pedida e será renovada automaticamente assim que possível.</template>
        <template v-else>Assim que você receber a primeira Box, a renovação passa a valer.</template>
      </p>
      <p v-if="a.ultimaRenovacaoEm" class="subtle">Última renovação automática: {{ dataSo(a.ultimaRenovacaoEm) }}.</p>
      <div class="row">
        <button class="btn" type="button" :disabled="ocupado" @click="cancelar">Cancelar renovação automática</button>
        <button v-if="botoesDeDemonstracao" class="btn" type="button" :disabled="ocupado" @click="renovarAgora">Simular renovação agora</button>
      </div>
    </template>
    <template v-else>
      <p class="muted">Ative para não precisar lembrar de renovar: 5 dias antes de a Box acabar, o sistema pede a próxima, com os mesmos medicamentos e horários, e faz a cobrança sozinho (simulada, sem valor real). Você cancela quando quiser.</p>
      <div><button class="btn btn-primario" type="button" :disabled="ocupado" @click="ativar">Ativar renovação automática</button></div>
    </template>
  </section>
</template>
