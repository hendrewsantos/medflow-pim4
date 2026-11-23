<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MenuAcessibilidade from './MenuAcessibilidade.vue'
import { useAuthStore } from '../stores/auth.js'
import { api, MODO_DEMO } from '../services/api.js'
import { PERFIL } from '../utils/formato.js'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const badges = ref({})

const MENUS = {
  Paciente: [['inicio', 'Início', 'inicio'], ['box', 'Montar Box', 'box'], ['pedidos', 'Meus pedidos', 'pedidos'], ['receitas', 'Receitas', 'receita'], ['dados', 'Meus dados', 'dados']],
  Administrador: [['painel', 'Painel', 'painel'], ['fila', 'Análise de receitas', 'fila'], ['operacao', 'Pedidos', 'pedidos'], ['estoque', 'Estoque', 'estoque'], ['usuarios', 'Usuários', 'usuarios']],
  Funcionario: [['painel', 'Painel', 'painel'], ['operacao', 'Pedidos', 'pedidos'], ['estoque', 'Estoque', 'estoque']],
  Entregador: [['entregas', 'Minhas entregas', 'entrega']],
}
const menu = computed(() => MENUS[auth.perfil] ?? [])

async function atualizarBadges() {
  try {
    if (auth.perfil === 'Administrador') badges.value = { fila: (await api.receitas.pendentes()).length }
    else if (auth.perfil === 'Entregador') badges.value = { entregas: (await api.entregas.disponiveis()).length }
    else badges.value = {}
  } catch { badges.value = {} }
}
onMounted(atualizarBadges)
watch(() => route.fullPath, atualizarBadges)

async function sair() {
  await auth.sair()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <header class="barra">
      <div class="marca"><img src="/simbolo-medflow.png" alt="" /><div class="nome">MedFlow<small>Solutions</small></div></div>
      <nav class="nav" aria-label="Menu principal">
        <RouterLink v-for="[nome, rotulo] in menu" :key="nome" :to="{ name: nome }" :aria-current="route.name === nome ? 'page' : undefined">
          <span>{{ rotulo }}</span><span v-if="badges[nome]" class="badge">{{ badges[nome] }}</span>
        </RouterLink>
      </nav>
      <div class="acoes">
        <span v-if="MODO_DEMO" class="subtle">Modo demonstração</span>
        <MenuAcessibilidade />
        <div class="quem"><span><strong>{{ auth.usuario?.nome }}</strong> · {{ PERFIL[auth.perfil] }}</span><button type="button" @click="sair">Sair</button></div>
      </div>
    </header>
    <main id="conteudo" class="conteudo" :class="`perfil-${(auth.perfil || '').toLowerCase()}`" tabindex="-1"><div :key="route.fullPath" class="pagina"><slot /></div></main>
  </div>
</template>
