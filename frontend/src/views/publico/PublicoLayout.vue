<script setup>
// Moldura das páginas públicas (início, termos, privacidade, contato): cabeçalho, conteúdo e rodapé com links legais.
import MenuAcessibilidade from '../../components/MenuAcessibilidade.vue'
import FundoRemedios from '../../components/FundoRemedios.vue'

defineProps({ remedios: { type: Boolean, default: false } })
import RodapePublico from './RodapePublico.vue'
import { MODO_DEMO } from '../../services/api.js'
import { useAuthStore } from '../../stores/auth.js'

const auth = useAuthStore()
</script>

<template>
  <div class="pub">
    <a class="pular" href="#conteudo-publico">Pular para o conteúdo</a>
    <header class="pub-topo">
      <RouterLink class="pub-marca" :to="{ name: 'home' }" aria-label="MedFlow Solutions, página inicial">
        <img src="/simbolo-medflow.png" alt="" /><span>MedFlow<small>Solutions</small></span>
      </RouterLink>
      <nav class="pub-nav" aria-label="Principal">
        <RouterLink v-if="auth.autenticado" class="btn btn-primario btn-sm" :to="{ name: auth.rotaInicial }">Ir para o sistema</RouterLink>
        <template v-else>
          <RouterLink class="btn btn-sm" :to="{ name: 'login' }">Entrar</RouterLink>
          <RouterLink class="btn btn-primario btn-sm" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
        </template>
        <MenuAcessibilidade />
      </nav>
    </header>
    <main id="conteudo-publico" class="pub-conteudo" tabindex="-1"><div v-if="remedios" class="pub-fundo"><FundoRemedios /></div><slot /></main>
    <footer class="pub-rodape">
      <RodapePublico />
      <p v-if="MODO_DEMO" class="subtle">Modo demonstração: tudo roda no seu navegador, com dados fictícios.</p>
    </footer>
  </div>
</template>
