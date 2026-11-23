<script setup>
// Consulta de medicamentos: o paciente vê tudo o que o MedFlow tem, com as especificações, o preço e se cabe na Box,
// sem precisar de receita nem de pedido. Itens fora da Box aparecem só como informação, com o motivo.
import { computed, onMounted, ref, watch } from 'vue'
import Icone from '../../components/Icone.vue'
import ModalDialogo from '../../components/ModalDialogo.vue'
import Esqueleto from '../../components/Esqueleto.vue'
import { api } from '../../services/api.js'
import { useUiStore } from '../../stores/ui.js'
import { PROGRAMAS, programaDe } from '../../domain/catalogo.js'
import { reais, tarjaTexto, CATEGORIA } from '../../utils/formato.js'

const ui = useUiStore()
const carregando = ref(true)
const itens = ref([])
const busca = ref('')
const programa = ref('')
const soBox = ref(false)
const LIMITE = 24
const limite = ref(LIMITE)
const aberto = ref(null)

onMounted(async () => {
  try { itens.value = await api.medicamentos.catalogo() } catch (e) { ui.erro(e) } finally { carregando.value = false }
})

const filtrados = computed(() => {
  const t = busca.value.trim().toLowerCase()
  return itens.value.filter((m) =>
    (!programa.value || programaDe(m.principioAtivo) === programa.value) &&
    (!soBox.value || m.permitidoNaBox) &&
    (!t || `${m.nome} ${m.principioAtivo} ${m.formaFarmaceutica}`.toLowerCase().includes(t)))
})
const visiveis = computed(() => filtrados.value.slice(0, limite.value))
const programasComItens = computed(() => PROGRAMAS.filter((p) => itens.value.some((m) => programaDe(m.principioAtivo) === p)))
const totalNaBox = computed(() => itens.value.filter((m) => m.permitidoNaBox).length)
watch([busca, programa, soBox], () => { limite.value = LIMITE })

const situacao = (m) => (!m.permitidoNaBox ? { texto: 'Fora da Box', classe: 'subtle' } : m.disponivel ? { texto: 'Disponível', classe: 'ok-txt' } : { texto: 'Indisponível', classe: 'ruim-txt' })
</script>

<template>
  <div class="cabecalho-pagina"><div><h1>Medicamentos</h1>
    <p>Tudo o que o MedFlow oferece, com as especificações. Para pedir é preciso uma receita aprovada; a consulta é livre.</p></div>
    <RouterLink class="btn btn-primario" :to="{ name: 'box' }"><Icone nome="box" />Montar minha Box</RouterLink></div>

  <Esqueleto v-if="carregando" :linhas="5" />
  <template v-else>
    <div class="row-between filtros-catalogo">
      <div class="campo" style="width: min(360px, 100%)"><label class="sr-only" for="busca-med">Buscar medicamento</label>
        <input id="busca-med" v-model="busca" class="input" type="search" placeholder="Buscar por nome, princípio ativo ou forma" /></div>
      <label class="check"><input v-model="soBox" type="checkbox" /> Só os que entram na Box ({{ totalNaBox }})</label>
    </div>
    <div class="programas" role="group" aria-label="Programa de cuidado">
      <button class="chip" type="button" :aria-pressed="!programa" @click="programa = ''">Todos</button>
      <button v-for="p in programasComItens" :key="p" class="chip" type="button" :aria-pressed="programa === p" @click="programa = p">{{ p }}</button>
    </div>
    <p class="subtle" aria-live="polite">{{ filtrados.length }} {{ filtrados.length === 1 ? 'medicamento' : 'medicamentos' }}<template v-if="programa"> em {{ programa }}</template></p>

    <div class="catalogo">
      <button v-for="m in visiveis" :key="m.id" class="med" type="button" @click="aberto = m">
        <span class="row-between"><span class="nome">{{ m.nome }} {{ m.concentracao }}</span><span class="tarja" :class="`tarja-${m.tarja}`">{{ tarjaTexto(m.tarja) }}</span></span>
        <span class="principio">{{ m.principioAtivo }} · {{ m.formaFarmaceutica }}</span>
        <span class="rodape"><span :class="situacao(m).classe">{{ situacao(m).texto }}</span><span>{{ m.permitidoNaBox ? `${reais(m.precoUnitario)} por unidade` : 'Ver ficha' }}</span></span>
      </button>
    </div>
    <div v-if="!filtrados.length" class="vazio"><Icone nome="vazio" /><p>Nenhum medicamento encontrado. Tente outro nome ou outro programa.</p></div>
    <div v-else-if="filtrados.length > visiveis.length"><button class="btn" type="button" @click="limite += LIMITE">Mostrar mais ({{ filtrados.length - visiveis.length }})</button></div>
    <p class="subtle">Os preços são fictícios, só para a demonstração do pagamento simulado. A farmacêutica responsável confere cada receita antes da separação.</p>
  </template>

  <ModalDialogo :aberto="!!aberto" :titulo="aberto ? `${aberto.nome} ${aberto.concentracao}` : ''" @fechar="aberto = null">
    <template v-if="aberto">
      <dl class="ficha">
        <div><dt>Princípio ativo</dt><dd>{{ aberto.principioAtivo }}</dd></div>
        <div><dt>Concentração</dt><dd>{{ aberto.concentracao }}</dd></div>
        <div><dt>Forma</dt><dd>{{ aberto.formaFarmaceutica }}</dd></div>
        <div><dt>Venda</dt><dd>{{ tarjaTexto(aberto.tarja) }}</dd></div>
        <div><dt>Programa de cuidado</dt><dd>{{ programaDe(aberto.principioAtivo) }}</dd></div>
        <div><dt>Categoria</dt><dd>{{ CATEGORIA[aberto.categoria] ?? aberto.categoria }}<template v-if="aberto.termolabil"> · exige cadeia fria</template></dd></div>
        <div v-if="aberto.permitidoNaBox"><dt>Preço (fictício)</dt><dd>{{ reais(aberto.precoUnitario) }} por unidade</dd></div>
        <div><dt>Na Box</dt><dd>{{ aberto.permitidoNaBox ? (aberto.disponivel ? 'Pode entrar e há estoque para um ciclo de 30 dias.' : 'Pode entrar, mas está sem estoque para um ciclo de 30 dias.') : aberto.motivoForaDaBox }}</dd></div>
      </dl>
    </template>
    <template #rodape>
      <button class="btn" type="button" @click="aberto = null">Fechar</button>
      <RouterLink v-if="aberto?.permitidoNaBox" class="btn btn-primario" :to="{ name: 'box' }" @click="aberto = null">Montar Box</RouterLink>
    </template>
  </ModalDialogo>
</template>

<style>
.filtros-catalogo { gap: 12px 20px; flex-wrap: wrap; margin-bottom: 20px; }
.filtros-catalogo + .programas { margin-bottom: 18px; }
.programas + .subtle { margin: 0 0 18px; }
.ficha { margin: 0; display: grid; gap: 10px; }
.ficha > div { display: grid; grid-template-columns: 11rem 1fr; gap: 12px; }
.ficha dt { color: var(--ink-2); font-weight: 700; }
.ficha dd { margin: 0; }
@media (max-width: 520px) { .ficha > div { grid-template-columns: 1fr; gap: 2px; } }
</style>
