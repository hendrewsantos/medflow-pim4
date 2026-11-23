<script setup>
// Placeholder de carregamento: mostra a forma da tela enquanto os dados chegam.
defineProps({ linhas: { type: Number, default: 4 } })
</script>

<template>
  <div class="esqueleto" role="status" aria-label="Carregando">
    <span v-for="n in linhas + 1" :key="n" aria-hidden="true"></span>
    <span class="sr-only">Carregando…</span>
  </div>
</template>
