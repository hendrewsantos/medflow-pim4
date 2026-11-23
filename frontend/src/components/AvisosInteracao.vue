<script setup>
// Pontos de atenção entre os medicamentos da Box. Informa, não bloqueia; a validação é da farmacêutica.
import { computed } from 'vue'
import Icone from './Icone.vue'
import { interacoesDe } from '../domain/interacoes.js'

const props = defineProps({
  itens: { type: Array, required: true },   // textos: "Nome concentração" ou "Nome princípio ativo"
  perfil: { type: String, default: 'paciente' }, // paciente | equipe
})
const avisos = computed(() => interacoesDe(props.itens))
</script>

<template>
  <section v-if="avisos.length" class="interacoes aviso aviso-info" aria-label="Pontos de atenção entre medicamentos">
    <Icone nome="info" />
    <div class="stack">
      <p><strong>{{ avisos.length === 1 ? 'Um ponto de atenção' : `${avisos.length} pontos de atenção` }}</strong>
        <template v-if="perfil === 'paciente'"> nesta Box. Isso não impede o pedido: a farmacêutica vai ver e, se for o caso, orienta você.</template>
        <template v-else> entre os medicamentos deste pedido, a conferir na análise (tabela de demonstração).</template></p>
      <ul>
        <li v-for="a in avisos" :key="a.titulo"><strong>{{ a.titulo }}.</strong> {{ a.texto }}</li>
      </ul>
    </div>
  </section>
</template>

<style>
.interacoes ul { margin: 6px 0 0; padding-left: 18px; display: grid; gap: 6px; }
</style>
