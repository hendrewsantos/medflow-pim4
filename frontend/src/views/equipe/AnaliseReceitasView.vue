<script setup>
// UC06 / RF07 — análise humana da receita: dados do prescritor, checklist e conferência item a item.
// Nenhuma Box é separada sem aprovação farmacêutica; item "Divergente" obriga recusar com motivo.
import { computed, onMounted, reactive, ref } from 'vue'
import Icone from '../../components/Icone.vue'
import Receituario from '../../components/Receituario.vue'
import { api } from '../../services/api.js'
import { useUiStore } from '../../stores/ui.js'
import { dataHora, isoData, relativo } from '../../utils/formato.js'
import { somarMeses } from '../../domain/regras.js'

const ui = useUiStore()
const pendentes = ref([])
const sel = ref(null)
const pedido = ref(null)
const motivo = ref('')
const MOTIVOS = ['Receita ilegível', 'Sem assinatura ou carimbo do prescritor', 'Receita fora da validade', 'Medicamento pedido não consta na receita', 'Dados do paciente divergentes']
const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO']
const CHECKLIST = [
  ['paciente', 'Nome do paciente confere com o cadastro'],
  ['prescritor', 'Prescritor identificado (nome e CRM)'],
  ['data', 'Data de emissão presente e dentro da validade'],
  ['posologia', 'Posologia clara (dose, frequência e duração)'],
  ['legibilidade', 'Receita legível'],
  ['assinatura', 'Assinatura e carimbo do prescritor'],
]
const hoje = () => isoData(new Date())

const form = reactive({ prescritorNome: '', crmNumero: '', crmUf: 'SP', dataEmissao: '', validaAte: '', checklist: {}, itens: {} })

function reiniciar() {
  const r = sel.value
  const [num, uf] = String(r?.prescritorCrm ?? '').split('/')
  Object.assign(form, {
    prescritorNome: r?.prescritorNome ?? '', crmNumero: num ?? '', crmUf: UFS.includes(uf) ? uf : 'SP',
    dataEmissao: r?.dataEmissao ? String(r.dataEmissao).slice(0, 10) : '', validaAte: isoData(somarMeses(new Date(), 6)),
    checklist: Object.fromEntries(CHECKLIST.map(([k]) => [k, false])), itens: {},
  })
  motivo.value = ''
}

async function carregar(id) {
  try {
    pendentes.value = await api.receitas.pendentes()
    sel.value = pendentes.value.find((r) => r.id === id) || pendentes.value[0] || null
    pedido.value = null
    reiniciar()
    if (sel.value) {
      const candidatos = await Promise.all((await api.pedidos.operacional('Pendente')).map((p) => api.pedidos.obter(p.id)))
      pedido.value = candidatos.find((p) => p.receitaId === sel.value.id) || null
    }
  } catch (e) {
    ui.erro(e)
  }
}
onMounted(() => carregar())

const itensPedido = computed(() => pedido.value?.itens ?? [])
const checklistCompleto = computed(() => CHECKLIST.every(([k]) => form.checklist[k]))
const divergentes = computed(() => itensPedido.value.filter((i) => form.itens[i.medicamentoId] === 'divergente'))
const todosConferem = computed(() => itensPedido.value.every((i) => form.itens[i.medicamentoId] === 'confere'))
const dadosOk = computed(() => form.prescritorNome.trim().length >= 3 && form.crmNumero.trim().length >= 3 && !!form.dataEmissao && form.dataEmissao <= hoje() && form.validaAte >= hoje())
const podeAprovar = computed(() => checklistCompleto.value && todosConferem.value && dadosOk.value)
const motivoOk = computed(() => motivo.value.trim().length >= 5)

function marcar(item, valor) {
  form.itens[item.medicamentoId] = valor
  if (valor === 'divergente' && !motivo.value.trim()) motivo.value = `Item divergente da receita: ${item.medicamento}.`
}

async function aprovar() {
  if (!podeAprovar.value) return
  try {
    await api.receitas.aprovar(sel.value.id, {
      validaAte: form.validaAte,
      prescritorNome: form.prescritorNome.trim(),
      prescritorCrm: `${form.crmNumero.trim()}/${form.crmUf}`,
      dataEmissao: form.dataEmissao,
      checklistCompleto: true,
      itensConferidos: itensPedido.value.filter((i) => form.itens[i.medicamentoId] === 'confere').map((i) => i.medicamentoId),
    })
    ui.avisar(`Receita de ${sel.value.paciente} aprovada.${pedido.value ? ` O pedido #${pedido.value.id} foi para separação.` : ''}`, 'ok')
    await carregar()
  } catch (e) { ui.erro(e) }
}
async function recusar() {
  if (!motivoOk.value) return ui.avisar('Informe o motivo da recusa (mínimo de 5 caracteres).', 'ruim')
  try {
    await api.receitas.recusar(sel.value.id, motivo.value.trim())
    ui.avisar(`Receita recusada.${pedido.value ? ' A reserva do pedido voltou ao estoque.' : ''}`, 'ok')
    await carregar()
  } catch (e) { ui.erro(e) }
}
</script>

<template>
  <div v-if="!pendentes.length" class="vazio"><Icone nome="ok" /><h3>Nenhuma receita aguardando análise</h3><p>Novas receitas aparecem aqui assim que o paciente envia.</p></div>
  <template v-else>
    <div class="cabecalho-pagina"><div><h1>Análise de receitas</h1><p>Nenhum pedido segue para separação sem receita aprovada por farmacêutico.</p></div></div>
    <div class="grid-lista-detalhe">
      <section class="tabela-wrap" aria-label="Receitas pendentes">
        <table class="tabela"><thead><tr><th>Paciente</th><th>Enviada</th></tr></thead>
          <tbody><tr v-for="r in pendentes" :key="r.id" class="clicavel" tabindex="0" :aria-selected="r.id === sel?.id" @click="carregar(r.id)" @keydown.enter="carregar(r.id)">
            <td><strong>{{ r.paciente }}</strong><div class="subtle">{{ r.nomeOriginal }}</div></td><td class="subtle">{{ relativo(r.enviadaEm) }}</td></tr></tbody>
        </table>
      </section>
      <section v-if="sel" class="stack-lg">
        <div class="grid-2">
          <div class="stack"><h3>Receita enviada em {{ dataHora(sel.enviadaEm) }}</h3><Receituario :receita="sel" /></div>
          <div class="stack"><h3>Pedido do paciente</h3>
            <template v-if="pedido">
              <div class="tabela-wrap"><table class="tabela"><thead><tr><th>Medicamento</th><th>Horários</th><th class="n">Dose</th></tr></thead>
                <tbody><tr v-for="i in pedido.itens" :key="i.medicamentoId"><td>{{ i.medicamento }}</td><td class="mono">{{ i.horarios.join(' · ') }}</td><td class="n">{{ i.dosePorHorario }}</td></tr></tbody></table></div>
              <p class="subtle">Pedido #{{ pedido.id }} · estoque já reservado. Confira se cada item e posologia constam na receita.</p>
            </template>
            <p v-else class="muted">Ainda não há pedido ligado a esta receita.</p>
          </div>
        </div>

        <form class="card stack-lg" novalidate aria-label="Conferência da receita" @submit.prevent="aprovar">
          <h3>Conferência</h3>
          <div class="form-grid">
            <div class="campo inteiro"><label for="presc-nome">Prescritor (nome)</label><input id="presc-nome" v-model="form.prescritorNome" class="input" autocomplete="off" /></div>
            <div class="campo"><label for="presc-crm">CRM (número)</label><input id="presc-crm" v-model="form.crmNumero" class="input mono" inputmode="numeric" autocomplete="off" /></div>
            <div class="campo"><label for="presc-uf">UF do CRM</label><select id="presc-uf" v-model="form.crmUf" class="input"><option v-for="u in UFS" :key="u">{{ u }}</option></select></div>
            <div class="campo"><label for="emissao">Data de emissão</label><input id="emissao" v-model="form.dataEmissao" class="input" type="date" :max="hoje()" /></div>
            <div class="campo"><label for="valida-ate">Receita válida até</label><input id="valida-ate" v-model="form.validaAte" class="input" type="date" :min="hoje()" />
              <span class="ajuda">Enquanto válida, as renovações mensais vão direto para separação.</span></div>
          </div>

          <fieldset class="checklist">
            <legend>Checklist (todos os itens são obrigatórios para aprovar)</legend>
            <label v-for="[k, texto] in CHECKLIST" :key="k" class="check"><input v-model="form.checklist[k]" type="checkbox" /><span>{{ texto }}</span></label>
          </fieldset>

          <fieldset v-if="itensPedido.length" class="checklist">
            <legend>Conferência item a item</legend>
            <div v-for="i in itensPedido" :key="i.medicamentoId" class="conferencia" role="group" :aria-label="`Conferir ${i.medicamento}`">
              <div><strong>{{ i.medicamento }}</strong><div class="subtle mono">{{ i.horarios.join(' · ') }} · {{ i.dosePorHorario }} por horário</div></div>
              <div class="segmentado">
                <button type="button" :aria-pressed="form.itens[i.medicamentoId] === 'confere'" @click="marcar(i, 'confere')">Confere</button>
                <button type="button" class="ruim" :aria-pressed="form.itens[i.medicamentoId] === 'divergente'" @click="marcar(i, 'divergente')">Divergente</button>
              </div>
            </div>
          </fieldset>

          <div v-if="divergentes.length" class="aviso aviso-ruim" role="alert"><Icone nome="alerta" />
            <p>Há item divergente ({{ divergentes.map((i) => i.medicamento).join(', ') }}). A aprovação fica bloqueada: recuse a receita informando o motivo.</p></div>
          <p v-else-if="!podeAprovar" class="subtle">Para aprovar: preencha prescritor, CRM e datas, marque todo o checklist e confirme cada item como "Confere".</p>

          <div><button class="btn btn-primario" type="submit" :disabled="!podeAprovar"><Icone nome="ok" />Aprovar receita</button></div>
        </form>

        <form class="card stack" novalidate :style="divergentes.length ? 'border-color: var(--bad)' : ''" @submit.prevent="recusar"><h3>Recusar</h3>
          <div class="horarios"><button v-for="m in MOTIVOS" :key="m" type="button" class="chip" style="font-family: var(--f-body)" @click="motivo = m">{{ m }}</button></div>
          <div class="campo"><label for="motivo">Motivo (o paciente verá este texto)</label><textarea id="motivo" v-model="motivo" class="input" /></div>
          <div><button class="btn btn-perigo" type="submit" :disabled="!motivoOk">Recusar e liberar a reserva</button></div></form>
      </section>
    </div>
  </template>
</template>
