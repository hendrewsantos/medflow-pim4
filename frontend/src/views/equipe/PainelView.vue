<script setup>
// UC14 — indicadores de resultado (não de atividade) para a equipe da farmácia.
import { computed, onMounted, ref } from 'vue'
import Icone from '../../components/Icone.vue'
import { api, MODO_DEMO } from '../../services/api.js'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { numero, STATUS_PEDIDO } from '../../utils/formato.js'

const auth = useAuthStore()
const ui = useUiStore()
const ind = ref(null)
const alertas = ref([])
const previsao = ref([])
const baixando = ref('')
const RELATORIOS = [['estoque', 'Estoque'], ['lotes-a-vencer', 'Lotes a vencer'], ['entregas', 'Entregas']]
const pico = ref(null)
const FLUXO = ['Pendente', 'EmSeparacao', 'AguardandoEntregador', 'EmRota', 'Entregue']

onMounted(async () => {
  try { [ind.value, alertas.value, previsao.value] = await Promise.all([api.painel.obter(), api.medicamentos.alertas(), api.previsaoReposicao.listar()]) } catch (e) { ui.erro(e) }
})

const maximo = computed(() => Math.max(1, ...FLUXO.map((s) => ind.value?.pedidosPorStatus[s] ?? 0)))
const kpis = computed(() => {
  const i = ind.value
  if (!i) return []
  return [
    { rotulo: 'Tempo de ciclo (P90)', valor: i.tempoCicloP90Horas === null ? '—' : numero(i.tempoCicloP90Horas), unidade: 'h', meta: 'Da confirmação à entrega, últimos 30 dias', classe: i.tempoCicloP90Horas > 48 ? 'alerta' : '' },
    { rotulo: 'Entregues na 1ª tentativa', valor: i.percentualPrimeiraTentativa === null ? '—' : numero(i.percentualPrimeiraTentativa), unidade: '%', meta: 'Se cair, o paciente ficou sem a Box no dia previsto', classe: i.percentualPrimeiraTentativa < 90 ? 'alerta' : '' },
    { rotulo: 'Parados em separação há +24 h', valor: i.pedidosParadosEmSeparacao, meta: 'Trabalho na fila, não em curso', classe: i.pedidosParadosEmSeparacao ? 'critico' : '' },
    { rotulo: 'Receitas aguardando análise', valor: i.receitasPendentes, meta: auth.perfil === 'Administrador' ? 'Análise humana obrigatória' : 'Somente a farmacêutica analisa', classe: i.receitasPendentes ? 'alerta' : '' },
    { rotulo: 'Itens abaixo do mínimo', valor: i.medicamentosAbaixoDoMinimo, meta: 'Saldo disponível ≤ nível mínimo', classe: i.medicamentosAbaixoDoMinimo ? 'critico' : '' },
    { rotulo: 'Lotes vencidos ou a vencer', valor: i.lotesVencendo30Dias + i.lotesVencidos, meta: `${i.lotesVencidos} vencido(s) · ${i.lotesVencendo30Dias} em até 30 dias`, classe: i.lotesVencidos ? 'critico' : i.lotesVencendo30Dias ? 'alerta' : '' },
  ]
})

async function exportar(nome) {
  baixando.value = nome
  try { await api.relatorios.baixar(nome); ui.avisar('Relatório baixado.', 'ok') } catch (e) { ui.erro(e) } finally { baixando.value = '' }
}

const situacaoReposicao = (p) => (p.disponivel <= p.nivelMinimo ? ['Abaixo do mínimo', 'pill-ruim'] : p.diasAteMinimo === null ? ['Sem consumo', 'pill-neutro'] : p.diasAteMinimo <= 7 ? ['Repor já', 'pill-ruim'] : p.diasAteMinimo <= 30 ? ['Repor em breve', 'pill-espera'] : ['OK', 'pill-ok'])

async function simularPico() {
  try { pico.value = await api.demo.simularPico() } catch (e) { ui.erro(e) }
}
</script>

<template>
  <div class="cabecalho-pagina"><div><h1>Painel da operação</h1></div></div>
  <div v-if="ind" class="stack-lg">
    <section class="kpis" aria-label="Indicadores">
      <div v-for="k in kpis" :key="k.rotulo" class="kpi" :class="k.classe"><span class="rotulo-kpi">{{ k.rotulo }}</span><span class="valor">{{ k.valor }}<small v-if="k.unidade">{{ k.unidade }}</small></span><span class="meta">{{ k.meta }}</span></div>
    </section>
    <div class="grid-2">
      <section class="card">
        <div class="card-header"><h3>Boxes por etapa</h3><RouterLink class="btn-link" :to="{ name: 'operacao' }">Abrir pedidos</RouterLink></div>
        <div class="fluxo">
          <div v-for="s in FLUXO" :key="s" class="fluxo-linha" :title="`${STATUS_PEDIDO[s].curto}: ${ind.pedidosPorStatus[s]}`">
            <span>{{ STATUS_PEDIDO[s].curto }}</span>
            <span class="fluxo-barra" role="img" :aria-label="`${STATUS_PEDIDO[s].curto}: ${ind.pedidosPorStatus[s]} pedidos`"><span :style="{ width: `${(100 * ind.pedidosPorStatus[s]) / maximo}%` }" /></span>
            <span class="n">{{ ind.pedidosPorStatus[s] }}</span>
          </div>
        </div>
        <p class="subtle" style="margin-top: 12px">{{ ind.pedidosPorStatus.Recusado }} recusado(s) e {{ ind.pedidosPorStatus.Cancelado }} cancelado(s) no total.</p>
      </section>
      <section class="card">
        <div class="card-header"><h3>Alertas de estoque</h3><RouterLink class="btn-link" :to="{ name: 'estoque' }">Abrir estoque</RouterLink></div>
        <ul v-if="alertas.length" class="alertas"><li v-for="a in alertas" :key="a.mensagem" :class="a.severidade"><span><strong>{{ a.medicamento }} · {{ a.severidade === 'alta' ? 'crítico' : 'atenção' }}</strong>{{ a.mensagem }}</span></li></ul>
        <p v-else class="muted">Nenhum alerta.</p>
      </section>
    </div>
    <section class="card stack" aria-labelledby="h-prev">
      <div class="row-between"><div><h3 id="h-prev">Previsão de reposição</h3><p class="subtle">Média móvel de 3 meses das baixas. "Dias até o mínimo" estima quando o saldo disponível chega ao nível mínimo.</p></div>
        <div class="row" role="group" aria-label="Exportar relatórios em CSV">
          <button v-for="[k, r] in RELATORIOS" :key="k" class="btn btn-sm" type="button" :disabled="baixando === k" @click="exportar(k)">{{ baixando === k ? 'Baixando…' : `CSV: ${r}` }}</button></div></div>
      <div v-if="previsao.length" class="tabela-wrap" tabindex="0"><table class="tabela">
        <thead><tr><th>Medicamento</th><th class="n">Consumo médio/mês</th><th class="n">Disponível</th><th class="n">Mínimo</th><th class="n">Dias até o mínimo</th><th>Situação</th></tr></thead>
        <tbody><tr v-for="p in previsao" :key="p.medicamentoId"><td>{{ p.nome }}</td><td class="n">{{ numero(p.consumoMedioMensal) }}</td><td class="n">{{ numero(p.disponivel) }}</td><td class="n">{{ numero(p.nivelMinimo) }}</td>
          <td class="n">{{ p.diasAteMinimo === null ? '—' : numero(p.diasAteMinimo) }}</td><td><span class="pill" :class="situacaoReposicao(p)[1]">{{ situacaoReposicao(p)[0] }}</span></td></tr></tbody>
      </table></div>
      <p v-else class="muted">Sem dados de consumo ainda.</p>
    </section>
    <section v-if="MODO_DEMO" class="card stack">
      <div class="row-between"><h3>Teste do pico de renovação</h3>
        <button class="btn btn-primario" type="button" @click="simularPico"><Icone nome="raio" />Simular dois pedidos sobre o mesmo saldo</button></div>
      <p class="muted">Reproduz o incidente do pitch: o pedido A pede todo o saldo de Losartana e, logo depois, o pedido B pede mais 60 unidades. Agora a reserva é feita na confirmação, dentro de uma transação.</p>
      <template v-if="pico">
        <ol class="stack" style="margin: 0; padding-left: 20px"><li v-for="p in pico.passos" :key="p.pedido"><strong>Pedido {{ p.pedido }}:</strong> <span :class="p.ok ? 'ok-txt' : 'ruim-txt'">{{ p.ok ? 'aceito' : 'recusado' }}</span> — {{ p.mensagem }}</li></ol>
        <p class="subtle">Saldo disponível no início: {{ pico.saldoInicial }} un. A simulação desfaz a reserva de teste ao terminar.</p>
      </template>
    </section>
  </div>
</template>
