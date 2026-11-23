<script setup>
import { computed } from 'vue'
import { ETAPAS, ETAPAS_COM_PAGAMENTO, ETAPA_ROTULO, dataHora } from '../utils/formato.js'

const props = defineProps({ pedido: { type: Object, required: true } })
const quando = computed(() => Object.fromEntries(props.pedido.historico.map((h) => [h.para, h.em])))
const etapas = computed(() => (props.pedido.historico.some((h) => h.para === 'AguardandoPagamento') ? ETAPAS_COM_PAGAMENTO : ETAPAS))
const atual = computed(() => etapas.value.indexOf(props.pedido.status))
const classe = (i) => (i < atual.value || props.pedido.status === 'Entregue' ? 'feito' : i === atual.value ? 'atual' : '')
</script>

<template>
  <ol v-if="atual >= 0" class="timeline" aria-label="Etapas do pedido">
    <li v-for="(etapa, i) in etapas" :key="etapa" :class="classe(i)" :aria-current="i === atual ? 'step' : undefined">
      <span class="etapa">{{ ETAPA_ROTULO[etapa] }}</span>
      <span class="quando">{{ quando[etapa] ? dataHora(quando[etapa]) : '' }}</span>
    </li>
  </ol>
</template>
