<script setup>
// Links legais e de contato, usados no rodapé das páginas públicas e nas telas de entrar e criar conta.
</script>

<template>
  <nav class="rodape-links" aria-label="Informações legais e contato">
    <RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink>
    <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink>
    <RouterLink :to="{ name: 'contato' }">Contato</RouterLink>
    <RouterLink :to="{ name: 'home' }">Página inicial</RouterLink>
  </nav>
</template>
