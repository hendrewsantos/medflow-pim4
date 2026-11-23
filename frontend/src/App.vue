<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from './components/AppShell.vue'
import ToastHost from './components/ToastHost.vue'
import Abertura from './components/Abertura.vue'
import { useAuthStore } from './stores/auth.js'
import { useUiStore } from './stores/ui.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

// Abertura animada: só na primeira visita da sessão do navegador.
const CHAVE_ABERTURA = 'medflow.abertura'
const jaViu = () => { try { return sessionStorage.getItem(CHAVE_ABERTURA) === '1' } catch { return true } }
const abertura = ref(!jaViu())
function fimDaAbertura() {
  abertura.value = false
  try { sessionStorage.setItem(CHAVE_ABERTURA, '1') } catch { /* sem armazenamento: aparece de novo na próxima visita */ }
}

// Token expirado ou inválido (HTTP 401): volta para o login com um aviso claro.
function sessaoExpirada() {
  auth.usuario = null
  ui.avisar('Sua sessão expirou. Entre novamente.', 'ruim')
  router.push({ name: 'login' })
}
onMounted(() => { ui.aplicar(); window.addEventListener('medflow:sessao-expirada', sessaoExpirada) })
onBeforeUnmount(() => window.removeEventListener('medflow:sessao-expirada', sessaoExpirada))
</script>

<template>
  <RouterView v-if="route.meta.publica" />
  <AppShell v-else-if="route.name"><RouterView /></AppShell>
  <Abertura v-if="abertura" @fim="fimDaAbertura" />
  <ToastHost />
</template>
