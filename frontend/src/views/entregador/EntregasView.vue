<script setup>
// UC10 e UC11 — rotas do entregador (mobile-first). RN13: a API entrega só o resumo (EntregaResumoDto):
// código, paciente, endereço, telefone, total de sachês, status e tentativa. Sem medicamentos, lotes nem receita.
import { computed, onMounted, reactive, ref } from 'vue'
import Icone from '../../components/Icone.vue'
import ModalDialogo from '../../components/ModalDialogo.vue'
import { api } from '../../services/api.js'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { primeiroNome } from '../../utils/formato.js'

const auth = useAuthStore()
const ui = useUiStore()
const ABAS = [['disponiveis', 'Prontas para sair'], ['rota', 'Em rota'], ['concluidas', 'Histórico']]
const OCORRENCIAS = ['Destinatário ausente', 'Endereço não localizado', 'Recusado no local', 'Acesso ao condomínio negado']
const aba = ref('disponiveis')
const grupos = reactive({ disponiveis: [], rota: [], concluidas: [] })
const recebedor = reactive({})
const ocorrencia = ref(null)
const motivo = ref('')
const lista = computed(() => grupos[aba.value])

async function carregar() {
  try {
    const [disp, minhas] = await Promise.all([api.entregas.disponiveis(), api.entregas.minhas()])
    grupos.disponiveis = disp
    grupos.rota = minhas.filter((e) => e.status === 'EmRota')
    grupos.concluidas = minhas.filter((e) => e.status !== 'EmRota')
  } catch (e) { ui.erro(e) }
}
onMounted(carregar)

const linkTelefone = (e) => `tel:${String(e.telefone).replace(/[^\d+]/g, '')}`
const linkMapa = (e) => `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(e.endereco)}`

async function executar(fn, msg, proximaAba) {
  try {
    await fn()
    ui.avisar(msg, 'ok')
    aba.value = proximaAba
    await carregar()
  } catch (e) { ui.erro(e) }
}
const iniciar = (e) => executar(() => api.entregas.iniciar(e.pedidoId), 'Rota iniciada. O paciente já vê "Saiu para entrega".', 'rota')
const confirmar = (e) => executar(() => api.entregas.confirmar(e.pedidoId, recebedor[e.pedidoId] || ''), 'Entrega confirmada.', 'concluidas')
async function salvarOcorrencia() {
  await executar(() => api.entregas.ocorrencia(ocorrencia.value.pedidoId, motivo.value), 'Ocorrência registrada. A Box volta para "Prontas para sair".', 'disponiveis')
  ocorrencia.value = null
  motivo.value = ''
}
</script>

<template>
  <div class="cabecalho-pagina"><div><h1>Olá, {{ primeiroNome(auth.usuario?.nome) }}</h1><p>Confirme cada entrega com o nome de quem recebeu: é o comprovante que o paciente e a farmácia veem.</p></div></div>
  <div class="abas" role="tablist"><button v-for="[k, r] in ABAS" :key="k" role="tab" type="button" :aria-selected="aba === k" @click="aba = k">{{ r }}<span class="cont">{{ grupos[k].length }}</span></button></div>
  <div v-if="lista.length" class="entregas">
    <article v-for="e in lista" :key="e.pedidoId" class="entrega">
      <div class="row-between"><span><strong>{{ e.codigo }}</strong> · {{ e.totalSaches }} sachês</span>
        <span v-if="aba === 'concluidas'" class="pill" :class="e.status === 'Entregue' ? 'pill-ok' : 'pill-espera'">{{ e.status === 'Entregue' ? 'Entregue' : 'Ocorrência' }}</span></div>
      <div><strong>{{ e.paciente }}</strong><p class="endereco">{{ e.endereco }}</p></div>
      <div v-if="aba !== 'concluidas'" class="acoes-entrega">
        <a v-if="e.telefone" class="btn" :href="linkTelefone(e)">Ligar</a>
        <a class="btn" :href="linkMapa(e)" target="_blank" rel="noopener noreferrer">Abrir no mapa</a>
      </div>
      <template v-if="aba === 'disponiveis'">
        <p v-if="e.tentativa" class="subtle">{{ e.tentativa }}ª tentativa não foi concluída. Esta será a tentativa {{ e.tentativa + 1 }}.</p>
        <button class="btn btn-primario" type="button" @click="iniciar(e)"><Icone nome="entrega" />Iniciar rota</button>
      </template>
      <template v-else-if="aba === 'rota'">
        <p class="subtle">Tentativa {{ e.tentativa }}</p>
        <form class="stack" novalidate @submit.prevent="confirmar(e)">
          <div class="campo"><label :for="`rec-${e.pedidoId}`">Quem recebeu?</label><input :id="`rec-${e.pedidoId}`" v-model="recebedor[e.pedidoId]" class="input" placeholder="Nome de quem recebeu" autocomplete="off" /></div>
          <button class="btn btn-primario" type="submit"><Icone nome="ok" />Confirmar entrega</button>
        </form>
        <button class="btn" type="button" @click="ocorrencia = e">Não consegui entregar</button>
      </template>
      <p v-else class="subtle">{{ e.status === 'Entregue' ? `Entregue na tentativa ${e.tentativa}.` : `Ocorrência na tentativa ${e.tentativa}: a Box voltou para a fila de saída.` }}</p>
    </article>
  </div>
  <div v-else class="vazio"><Icone nome="entrega" /><p>{{ aba === 'rota' ? 'Nenhuma entrega em andamento.' : aba === 'disponiveis' ? 'Nenhuma Box pronta para sair agora.' : 'Sem entregas concluídas.' }}</p></div>
  <ModalDialogo :aberto="!!ocorrencia" titulo="O que aconteceu?" @fechar="ocorrencia = null">
    <div class="horarios"><button v-for="o in OCORRENCIAS" :key="o" type="button" class="chip" style="font-family: var(--f-body)" @click="motivo = o">{{ o }}</button></div>
    <div class="campo"><label for="motivo-ocorrencia">Descrição</label><textarea id="motivo-ocorrencia" v-model="motivo" class="input" /></div>
    <p class="subtle">A Box volta para "Prontas para sair" e uma nova tentativa é registrada na próxima rota.</p>
    <template #rodape><button class="btn" type="button" @click="ocorrencia = null">Voltar</button><button class="btn btn-primario" type="button" @click="salvarOcorrencia">Registrar ocorrência</button></template>
  </ModalDialogo>
</template>
