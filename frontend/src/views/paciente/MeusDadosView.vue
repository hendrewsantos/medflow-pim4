<script setup>
// UC12 — direitos do titular (LGPD art. 18): acesso, correção, portabilidade e eliminação.
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icone from '../../components/Icone.vue'
import ModalDialogo from '../../components/ModalDialogo.vue'
import { api, MODO_DEMO } from '../../services/api.js'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { data, dataHora, formatarCpf } from '../../utils/formato.js'
import { senhaForte } from '../../domain/regras.js'

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()
const dados = ref(null)
const form = ref({ nome: '', email: '', telefone: '', endereco: '' })
const json = ref('')
const excluindo = ref(false)
const confirmacao = ref('')
const revogando = ref(false)
const trocandoSenha = ref(false)
const senhas = ref({ atual: '', nova: '', confirmar: '' })
const errosSenha = ref([])
const saudeAtiva = computed(() => !!dados.value?.consentimentos.some((c) => c.tipo === 'DadosDeSaude' && !c.revogadoEm))

async function carregar() {
  try {
    dados.value = await api.privacidade.meusDados()
    form.value = { nome: dados.value.nome, email: dados.value.email, telefone: dados.value.telefone || '', endereco: dados.value.endereco || '' }
  } catch (e) {
    ui.erro(e)
  }
}
onMounted(carregar)

async function salvar() {
  try {
    await api.privacidade.atualizar(form.value)
    ui.avisar('Dados atualizados.', 'ok')
    await carregar()
  } catch (e) {
    ui.erro(e)
  }
}

async function exportar() {
  try {
    if (MODO_DEMO) json.value = JSON.stringify(await api.privacidade.meusDados(), null, 2)
    else await api.privacidade.exportarArquivo()
  } catch (e) {
    ui.erro(e)
  }
}

async function copiar() {
  try { await navigator.clipboard.writeText(json.value); ui.avisar('Copiado.', 'ok') } catch { ui.avisar('Selecione o texto e use Ctrl+C para copiar.') }
}

async function revogarSaude() {
  try {
    dados.value = await api.privacidade.revogarSaude()
    revogando.value = false
    ui.avisar('Consentimento de dados de saúde revogado. Não é possível enviar novas receitas nem fazer novos pedidos.', 'ok')
    await carregar()
  } catch (e) {
    ui.erro(e)
  }
}

function abrirSenha() {
  senhas.value = { atual: '', nova: '', confirmar: '' }
  errosSenha.value = []
  trocandoSenha.value = true
}

async function alterarSenha() {
  const e = []
  if (!senhas.value.atual) e.push('Informe a senha atual.')
  if (!senhaForte(senhas.value.nova)) e.push('A nova senha deve ter ao menos 8 caracteres, com letras e números.')
  if (senhas.value.nova !== senhas.value.confirmar) e.push('A confirmação não é igual à nova senha.')
  errosSenha.value = e
  if (e.length) return
  try {
    await api.auth.alterarSenha(senhas.value.atual, senhas.value.nova)
    trocandoSenha.value = false
    ui.avisar('Senha alterada.', 'ok')
  } catch (err) {
    errosSenha.value = [err?.erros ? Object.values(err.erros).flat().join(' ') : err?.detail || 'Não foi possível alterar a senha.']
  }
}

async function excluir() {
  if (confirmacao.value.trim().toUpperCase() !== 'EXCLUIR') return ui.avisar('Digite EXCLUIR para confirmar.', 'ruim')
  try {
    await api.privacidade.excluirConta()
    await auth.sair()
    ui.avisar('Conta excluída e dados anonimizados.', 'ok')
    router.push({ name: 'login' })
  } catch (e) {
    ui.erro(e)
  }
}
</script>

<template>
  <div class="cabecalho-pagina"><div><h1>Meus dados</h1><p>Você controla seus dados. Aqui estão os direitos previstos no art. 18 da LGPD, em um só lugar.</p></div></div>
  <div v-if="dados" class="grid-main-side">
    <div class="stack-lg">
      <form class="card stack" novalidate @submit.prevent="salvar">
        <h3>Dados cadastrais</h3>
        <div class="form-grid">
          <div class="campo inteiro"><label for="d-nome">Nome completo</label><input id="d-nome" v-model="form.nome" class="input" /></div>
          <div class="campo"><label for="d-email">E-mail</label><input id="d-email" v-model="form.email" class="input" type="email" /></div>
          <div class="campo"><label for="d-tel">Celular</label><input id="d-tel" v-model="form.telefone" class="input" /></div>
          <div class="campo inteiro"><label for="d-end">Endereço de entrega</label><input id="d-end" v-model="form.endereco" class="input" /></div>
          <div class="campo"><label for="d-cpf">CPF</label><input id="d-cpf" class="input mono" :value="formatarCpf(dados.cpf)" readonly /><span class="ajuda">Para corrigir o CPF, fale com a farmácia.</span></div>
          <div class="campo"><span class="rotulo">Conta criada em</span><span>{{ data(dados.criadoEm) }}</span></div>
        </div>
        <div><button class="btn btn-primario" type="submit">Salvar alterações</button> <span class="subtle">Correção de dados — art. 18, III</span></div>
      </form>
      <section class="stack"><h3>Consentimentos registrados</h3>
        <div class="tabela-wrap"><table class="tabela">
          <thead><tr><th>Finalidade</th><th>Base legal</th><th>Versão</th><th>Concedido em</th><th>Situação</th></tr></thead>
          <tbody><tr v-for="c in dados.consentimentos" :key="c.tipo + c.concedidoEm">
            <td>{{ c.tipo === 'TermosDeUso' ? 'Cadastro e entrega' : 'Dados de saúde da receita' }}</td><td>{{ c.tipo === 'TermosDeUso' ? 'Art. 7º, I' : 'Art. 11, I' }}</td>
            <td class="mono">{{ c.versaoTermo }}</td><td>{{ dataHora(c.concedidoEm) }}</td>
            <td><span class="pill" :class="c.revogadoEm ? 'pill-neutro' : 'pill-ok'">{{ c.revogadoEm ? 'Revogado' : 'Ativo' }}</span></td></tr></tbody>
        </table></div>
        <p class="subtle">Guardamos data, hora e versão de cada aceite porque cabe a nós provar o consentimento (art. 8º, §2º).</p></section>
    </div>
    <aside class="stack-lg">
      <section class="card stack"><h3>Levar meus dados</h3>
        <p class="muted">Receba tudo o que guardamos sobre você em formato JSON: cadastro, consentimentos, receitas e pedidos (art. 18, II e V).</p>
        <div><button class="btn" type="button" @click="exportar">Baixar meus dados</button></div></section>
      <section class="card stack"><h3>Consentimento de dados de saúde</h3>
        <p class="muted">Você deu este consentimento ao enviar a receita (art. 11, I). Se revogar, não será possível enviar novas receitas nem fazer novos pedidos até consentir de novo. O que já foi entregue continua registrado por obrigação sanitária.</p>
        <div v-if="saudeAtiva"><button class="btn" type="button" @click="revogando = true">Revogar consentimento de saúde</button></div>
        <p v-else class="subtle">Nenhum consentimento de saúde ativo no momento.</p></section>
      <section class="card stack"><h3>Senha</h3>
        <p class="muted">Troque a senha se suspeitar que alguém a conhece. Você continua conectado neste aparelho.</p>
        <div><button class="btn" type="button" @click="abrirSenha">Alterar senha</button></div></section>
      <section class="card stack" style="border-color: var(--bad)"><h3>Excluir minha conta</h3>
        <p class="muted">Seus dados pessoais são apagados e as receitas, removidas (art. 18, VI). Pedidos e movimentações de estoque ficam guardados sem identificar você, porque a rastreabilidade sanitária é obrigação legal (art. 16, I).</p>
        <div><button class="btn btn-perigo" type="button" @click="excluindo = true">Excluir minha conta</button></div></section>
    </aside>
  </div>

  <ModalDialogo :aberto="!!json" titulo="Seus dados (JSON)" largo @fechar="json = ''">
    <p class="muted">A senha não aparece, nem mesmo como hash.</p>
    <pre class="json" tabindex="0">{{ json }}</pre>
    <template #rodape><button class="btn" type="button" @click="json = ''">Fechar</button><button class="btn btn-primario" type="button" @click="copiar"><Icone nome="copiar" />Copiar</button></template>
  </ModalDialogo>
  <ModalDialogo :aberto="revogando" titulo="Revogar consentimento de saúde?" @fechar="revogando = false">
    <p>Depois de revogar, você não poderá enviar novas receitas nem confirmar novos pedidos. Pedidos em andamento seguem o fluxo normal. Você pode consentir de novo no próximo envio de receita.</p>
    <template #rodape><button class="btn" type="button" @click="revogando = false">Voltar</button><button class="btn btn-perigo" type="button" @click="revogarSaude">Revogar consentimento</button></template>
  </ModalDialogo>
  <ModalDialogo :aberto="trocandoSenha" titulo="Alterar senha" @fechar="trocandoSenha = false">
    <form id="form-senha" class="stack" novalidate @submit.prevent="alterarSenha">
      <div class="campo"><label for="senha-atual">Senha atual</label><input id="senha-atual" v-model="senhas.atual" class="input" type="password" autocomplete="current-password" /></div>
      <div class="campo"><label for="senha-nova">Nova senha</label><input id="senha-nova" v-model="senhas.nova" class="input" type="password" autocomplete="new-password" /><span class="ajuda">Mínimo de 8 caracteres, com letras e números.</span></div>
      <div class="campo"><label for="senha-conf">Repita a nova senha</label><input id="senha-conf" v-model="senhas.confirmar" class="input" type="password" autocomplete="new-password" /></div>
      <p v-for="e in errosSenha" :key="e" class="erro" role="alert" style="color: var(--bad); font-size: var(--t-sm)">{{ e }}</p>
    </form>
    <template #rodape><button class="btn" type="button" @click="trocandoSenha = false">Cancelar</button><button class="btn btn-primario" type="submit" form="form-senha">Alterar senha</button></template>
  </ModalDialogo>
  <ModalDialogo :aberto="excluindo" titulo="Excluir sua conta?" @fechar="excluindo = false">
    <p>Esta ação não pode ser desfeita. Se houver um pedido pendente, ele será cancelado e a reserva, liberada.</p>
    <div class="campo"><label for="confirma-exclusao">Digite EXCLUIR para confirmar</label><input id="confirma-exclusao" v-model="confirmacao" class="input" autocomplete="off" /></div>
    <template #rodape><button class="btn" type="button" @click="excluindo = false">Voltar</button><button class="btn btn-perigo" type="button" @click="excluir">Excluir definitivamente</button></template>
  </ModalDialogo>
</template>
