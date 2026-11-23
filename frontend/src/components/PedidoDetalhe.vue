<script setup>
import StatusPill from './StatusPill.vue'
import LinhaDoTempo from './LinhaDoTempo.vue'
import Icone from './Icone.vue'
import ContagemRegressiva from './ContagemRegressiva.vue'
import { MODO_DEMO } from '../services/api.js'
import { data, dataSo, dataHora, numero, reais, mensagemStatus } from '../utils/formato.js'

defineProps({
  pedido: { type: Object, required: true },
  podeCancelar: { type: Boolean, default: false },
  mostrarLotes: { type: Boolean, default: true },
  podePagar: { type: Boolean, default: false },
})
defineEmits(['cancelar', 'aprovar-pagamento', 'expirar-pagamento', 'atualizar'])
// Os botões que imitam o gateway existem só na demonstração (a API responde 404 fora dela).
const botoesDeDemonstracao = MODO_DEMO || import.meta.env.VITE_BOTOES_PAGAMENTO === 'true'
</script>

<template>
  <div class="stack-lg">
    <div class="row-between">
      <div><h3>Pedido #{{ pedido.id }} · Box do ciclo de {{ data(pedido.criadoEm) }}</h3></div>
      <StatusPill :status="pedido.status" />
    </div>
    <LinhaDoTempo :pedido="pedido" />
    <div class="aviso" :class="pedido.status === 'Recusado' ? 'aviso-ruim' : pedido.status === 'Entregue' ? 'aviso-ok' : 'aviso-info'">
      <Icone :nome="pedido.status === 'Recusado' ? 'erro' : 'info'" />
      <p>{{ mensagemStatus(pedido) }}</p>
    </div>
    <section v-if="podePagar && pedido.status === 'AguardandoPagamento'" class="pagamento stack" aria-labelledby="pg-titulo">
      <div class="row-between">
        <div><h4 id="pg-titulo">Pagamento</h4><p class="muted">Pague dentro do prazo para manter a reserva.</p></div>
        <div class="pg-prazo"><span class="subtle">Tempo restante</span><ContagemRegressiva :prazo="pedido.pagamentoPrazoEm" @fim="$emit('atualizar')" /></div>
      </div>
      <dl class="valores">
        <div><dt>Medicamentos</dt><dd>{{ reais(pedido.valorMedicamentos) }}</dd></div>
        <div><dt>Entrega</dt><dd>{{ reais(pedido.valorEntrega) }}</dd></div>
        <div class="total"><dt>Total</dt><dd>{{ reais(pedido.valorMedicamentos + pedido.valorEntrega) }}</dd></div>
      </dl>
      <p class="subtle">Pagamento simulado: nenhum valor real é cobrado e nenhum dado de pagamento é pedido.</p>
      <div v-if="botoesDeDemonstracao" class="row">
        <button class="btn btn-primario" type="button" @click="$emit('aprovar-pagamento', pedido)">Aprovar pagamento</button>
        <button class="btn" type="button" @click="$emit('expirar-pagamento', pedido)">Simular expiração</button>
        <span class="subtle">Botões da demonstração: representam o retorno do meio de pagamento.</span>
      </div>
    </section>
    <dl v-else-if="pedido.valorMedicamentos + pedido.valorEntrega > 0" class="valores" aria-label="Valores do pedido">
      <div><dt>Medicamentos</dt><dd>{{ reais(pedido.valorMedicamentos) }}</dd></div>
      <div><dt>Entrega</dt><dd>{{ reais(pedido.valorEntrega) }}</dd></div>
      <div class="total"><dt>Total{{ pedido.pagoEm ? ' pago' : '' }}</dt><dd>{{ reais(pedido.valorMedicamentos + pedido.valorEntrega) }}</dd></div>
    </dl>
    <div class="stack">
      <h4>Itens da Box · {{ pedido.diasCiclo }} dias</h4>
      <div class="tabela-wrap">
        <table class="tabela">
          <thead><tr><th>Medicamento</th><th>Horários</th><th class="n">Por dose</th><th class="n">Total</th><th v-if="mostrarLotes">Lotes</th></tr></thead>
          <tbody>
            <tr v-for="i in pedido.itens" :key="i.medicamentoId">
              <td>{{ i.medicamento }}</td>
              <td class="mono">{{ i.horarios.join(' · ') }}</td>
              <td class="n">{{ i.dosePorHorario }}</td>
              <td class="n">{{ numero(i.quantidade) }}</td>
              <td v-if="mostrarLotes" class="mono subtle">{{ i.lotes.map((l) => `${l.lote} (${l.quantidade}, val. ${dataSo(l.validade)})`).join(' · ') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="stack">
      <h4>Histórico</h4>
      <ul class="historico">
        <li v-for="(h, n) in [...pedido.historico].reverse()" :key="n">
          <span class="quando">{{ dataHora(h.em) }}</span>
          <StatusPill :status="h.para" tipo="historico" /> {{ h.observacao }}
        </li>
      </ul>
    </div>
    <p class="subtle">Entrega em: {{ pedido.enderecoEntrega }}</p>
    <div v-if="podeCancelar && (pedido.status === 'Pendente' || pedido.status === 'AguardandoPagamento')">
      <button class="btn btn-perigo" type="button" @click="$emit('cancelar', pedido)">Cancelar pedido</button>
    </div>
  </div>
</template>
