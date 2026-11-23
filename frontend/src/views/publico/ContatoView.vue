<script setup>
import PublicoLayout from './PublicoLayout.vue'
import { CONTROLADOR, EMAIL_CONTATO, EMAIL_ENCARREGADO } from './dadosLegais.js'
</script>

<template>
  <PublicoLayout>
    <article class="pub-texto">
      <h1>Contato</h1>
      <div class="aviso aviso-info"><p><strong>Ambiente de demonstração com dados fictícios.</strong> {{ CONTROLADOR }} é uma organização fictícia criada para um projeto acadêmico (UNIP). Os endereços abaixo não existem de verdade.</p></div>
      <section class="stack">
        <h2>Dúvidas e suporte</h2>
        <p>E-mail: <a :href="`mailto:${EMAIL_CONTATO}`">{{ EMAIL_CONTATO }}</a></p>
        <p>Atendimento de segunda a sexta, das 8h às 18h (horário de Brasília).</p>
      </section>
      <section class="stack">
        <h2>Privacidade e direitos do titular</h2>
        <p>Para exercer os seus direitos da LGPD ou falar com o encarregado de dados: <a :href="`mailto:${EMAIL_ENCARREGADO}`">{{ EMAIL_ENCARREGADO }}</a>.</p>
        <p>Você também consegue consultar, corrigir, baixar e excluir os seus dados direto na tela <strong>Meus dados</strong>, depois de entrar.</p>
      </section>
      <section class="stack">
        <h2>Em caso de urgência</h2>
        <p>O MedFlow não atende emergências. Se você passar mal, procure um serviço de saúde ou ligue para o SAMU (192).</p>
      </section>
    </article>
  </PublicoLayout>
</template>
