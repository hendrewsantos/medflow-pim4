<script setup>
import { computed, onMounted, ref } from 'vue'
import Icone from '../../components/Icone.vue'
import StatusPill from '../../components/StatusPill.vue'
import LinhaDoTempo from '../../components/LinhaDoTempo.vue'
import OrganizadorHorarios from '../../components/OrganizadorHorarios.vue'
import { api } from '../../services/api.js'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { data, dataSo, relativo, primeiroNome, mensagemStatus } from '../../utils/formato.js'
import { DIAS_CICLO, inicioDoDia, somarDias } from '../../domain/regras.js'

const auth = useAuthStore()
const ui = useUiStore()
const carregando = ref(true)
const receitas = ref([])
const ativo = ref(null)
const ultimo = ref(null)
const recusado = ref(null)
const renovacao = ref(null)

const EM_ANDAMENTO = ['Pendente', 'EmSeparacao', 'AguardandoEntregador', 'EmRota']
const hoje = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

onMounted(async () => {
  try {
    const [pedidos, rec] = await Promise.all([api.pedidos.meus(), api.receitas.minhas()])
    receitas.value = rec
    const a = pedidos.find((p) => EM_ANDAMENTO.includes(p.status))
    const u = pedidos.find((p) => p.status === 'Entregue')
    ;[ativo.value, ultimo.value] = await Promise.all([a && api.pedidos.obter(a.id), u && api.pedidos.obter(u.id)])
    renovacao.value = await api.pedidos.proximaRenovacao().catch(() => null)
    if (pedidos[0]?.status === 'Recusado') recusado.value = await api.pedidos.obter(pedidos[0].id)
  } catch (e) {
    ui.erro(e)
  } finally {
    carregando.value = false
  }
})

const receitaValida = computed(() => receitas.value.find((r) => r.status === 'Pendente' || (r.status === 'Aprovada' && inicioDoDia(r.validaAte) >= inicioDoDia(new Date()))))
const entregueEm = computed(() => ultimo.value?.entregas.at(-1)?.concluidaEm)
const cobreAte = computed(() => (entregueEm.value ? somarDias(entregueEm.value, DIAS_CICLO) : null))
const diasRestantes = computed(() => (cobreAte.value ? Math.ceil((inicioDoDia(cobreAte.value) - inicioDoDia(new Date())) / 86400000) : null))
const base = computed(() => ativo.value || ultimo.value)
</script>

<template>
  <div class="cabecalho-pagina"><div><h1>Início</h1></div></div>
  <p v-if="carregando" class="muted">Carregando…</p>
  <div v-else class="grid-main-side">
    <div class="stack-lg">
      <section class="card" aria-label="Sua Box">
        <div v-if="ativo" class="stack-lg">
          <div class="row-between"><div><h3>Box em andamento · pedido #{{ ativo.id }}</h3><p class="subtle">Ciclo de {{ data(ativo.criadoEm) }}</p></div><StatusPill :status="ativo.status" /></div>
          <LinhaDoTempo :pedido="ativo" />
          <div class="aviso aviso-info"><Icone nome="info" /><p>{{ mensagemStatus(ativo) }}</p></div>
          <div><RouterLink class="btn" :to="{ name: 'pedidos', params: { id: ativo.id } }">Ver detalhes do pedido</RouterLink></div>
        </div>
        <div v-else-if="recusado" class="stack">
          <div class="aviso aviso-ruim"><Icone nome="erro" /><p>{{ mensagemStatus(recusado) }}</p></div>
          <div><RouterLink class="btn btn-primario" :to="{ name: 'receitas' }">Enviar nova receita</RouterLink></div>
        </div>
        <div v-else-if="ultimo" class="stack">
          <div class="row-between"><div><h3>Última Box entregue {{ relativo(entregueEm) }}</h3><p class="subtle">Nenhuma Box em preparação</p></div><StatusPill status="Entregue" /></div>
          <div class="aviso" :class="diasRestantes <= 5 ? 'aviso-espera' : 'aviso-info'"><Icone nome="relogio" />
            <p>Seus sachês cobrem até <strong>{{ data(cobreAte) }}</strong> ({{ diasRestantes > 0 ? `${diasRestantes} dias` : 'acabam hoje' }}).
              {{ diasRestantes <= 5 ? 'Renove agora para não interromper o tratamento.' : 'Renove alguns dias antes do fim.' }}</p></div>
          <div class="row"><RouterLink class="btn btn-primario" :to="{ name: 'box', query: { repetir: 1 } }"><Icone nome="box" />Renovar minha Box</RouterLink>
            <span class="subtle">Repete os mesmos medicamentos e horários; você pode ajustar antes de confirmar.</span></div>
        </div>
        <div v-else class="stack"><h3>Comece pela sua receita</h3><p class="muted">Envie a receita, escolha medicamentos e horários e receba os sachês prontos em casa.</p>
          <div><RouterLink class="btn btn-primario" :to="{ name: 'box' }">Montar minha primeira Box</RouterLink></div></div>
      </section>
      <section v-if="base" class="stack">
        <div class="secao-titulo"><h2>Seus horários</h2><span class="subtle">Cada compartimento é um sachê do dia</span></div>
        <OrganizadorHorarios :itens="base.itens" />
      </section>
    </div>
    <aside class="stack-lg">
      <section v-if="renovacao" class="card stack" aria-label="Próxima renovação"><span class="eyebrow">Próxima renovação</span>
        <template v-if="renovacao.temBox">
          <p class="renov-dias"><strong>{{ renovacao.diasRestantes > 0 ? renovacao.diasRestantes : 0 }}</strong> {{ renovacao.diasRestantes === 1 ? 'dia' : 'dias' }} até o fim da sua Box</p>
          <p class="muted">Sua Box atual dura até <strong>{{ dataSo(renovacao.dataEstimadaFimDaBox) }}</strong>. {{ renovacao.diasRestantes <= 5 ? 'Renove agora para não ficar sem os medicamentos.' : 'Renove alguns dias antes do fim.' }}</p>
          <div v-if="renovacao.receitaVenceAntes" class="aviso aviso-espera"><Icone nome="alerta" />
            <p>Sua receita vale só até <strong>{{ dataSo(renovacao.receitaValidaAte) }}</strong>, antes do fim da Box. Envie uma receita nova para a próxima renovação.</p></div>
          <div class="row"><RouterLink class="btn" :to="{ name: renovacao.receitaVenceAntes ? 'receitas' : 'box' }">{{ renovacao.receitaVenceAntes ? 'Enviar nova receita' : 'Renovar minha Box' }}</RouterLink></div>
        </template>
        <p v-else class="muted">Assim que você receber a primeira Box, mostramos aqui quando ela acaba e quando renovar.</p>
      </section>
      <section class="card stack"><span class="eyebrow">Receita</span>
        <template v-if="receitaValida">
          <div class="row-between"><strong>{{ receitaValida.nomeOriginal }}</strong><StatusPill :status="receitaValida.status" tipo="receita" /></div>
          <p class="muted" v-if="receitaValida.status === 'Aprovada'">Válida até <strong>{{ dataSo(receitaValida.validaAte) }}</strong>. Enquanto estiver válida, cada renovação vai direto para separação.</p>
          <p class="muted" v-else>Aguardando a análise da farmacêutica responsável.</p>
        </template>
        <template v-else><p class="muted">Você não tem receita válida. Envie uma para montar a Box.</p><div><RouterLink class="btn" :to="{ name: 'receitas' }">Enviar receita</RouterLink></div></template>
      </section>
      <section class="card stack"><span class="eyebrow">Como sua Box fica garantida</span>
        <p class="muted">Ao confirmar o pedido, os medicamentos já ficam reservados para você naquele instante, lote a lote. Se faltar algum item, o sistema avisa antes de confirmar — e nada fica pela metade.</p></section>
    </aside>
  </div>
</template>
