<script setup>
import PublicoLayout from './PublicoLayout.vue'
import { CONTROLADOR, EMAIL_ENCARREGADO, VERSAO_TERMO, VIGENTE_DESDE } from './dadosLegais.js'
</script>

<template>
  <PublicoLayout>
    <article class="pub-texto">
      <span class="eyebrow">Versão {{ VERSAO_TERMO }} · em vigor desde {{ VIGENTE_DESDE }}</span>
      <h1>Política de Privacidade</h1>
      <div class="aviso aviso-espera"><p><strong>Ambiente de demonstração com dados fictícios.</strong> Este texto descreve como o sistema trata dados dentro do projeto acadêmico MedFlow (UNIP). A organização e os contatos são fictícios.</p></div>

      <section class="stack"><h2>1. Quem é o controlador</h2>
        <p>O controlador dos seus dados é {{ CONTROLADOR }}, organização fictícia criada para este projeto acadêmico. Esta política segue a Lei Geral de Proteção de Dados (Lei 13.709/2018, a LGPD).</p></section>

      <section class="stack"><h2>2. Quais dados coletamos</h2>
        <ul>
          <li><strong>Cadastro:</strong> nome, e-mail, CPF, endereço de entrega, telefone (opcional) e senha (guardada só como hash com salt).</li>
          <li><strong>Dados de saúde:</strong> a receita enviada (PDF ou imagem) e os medicamentos e horários da sua Box. São dados pessoais sensíveis.</li>
          <li><strong>Operação:</strong> pedidos, histórico de status, entregas, quem recebeu a Box e registros de aceite (data, hora e versão do termo).</li>
        </ul></section>

      <section class="stack"><h2>3. Para que usamos (finalidades) e em qual base legal</h2>
        <div class="tabela-wrap"><table class="tabela">
          <thead><tr><th>Finalidade</th><th>Dados</th><th>Base legal</th></tr></thead>
          <tbody>
            <tr><td>Criar e manter a sua conta, montar o pedido e entregar a Box</td><td>Cadastro e endereço</td><td>Art. 7º, I (consentimento)</td></tr>
            <tr><td>Conferir a receita e separar os medicamentos</td><td>Dados de saúde</td><td>Art. 11, I (consentimento específico e destacado)</td></tr>
          </tbody>
        </table></div>
        <p>Não usamos os seus dados para publicidade nem os vendemos.</p></section>

      <section class="stack"><h2>4. Com quem compartilhamos</h2>
        <ul>
          <li><strong>Equipe da farmácia:</strong> a farmacêutica vê a receita; o operador vê o pedido; o entregador vê só nome, endereço, telefone e quantidade de sachês.</li>
          <li><strong>VLibras:</strong> serviço do Governo Federal que traduz o conteúdo da página para Libras. Ele carrega no seu navegador e pode receber o texto exibido na tela.</li>
          <li>Não compartilhamos dados com outras empresas para fins comerciais.</li>
        </ul></section>

      <section class="stack"><h2>5. Por quanto tempo guardamos</h2>
        <p>Guardamos os dados enquanto a sua conta existir. Ao excluir a conta, apagamos os dados pessoais e as receitas. Pedidos e movimentações de estoque ficam guardados sem identificar você, porque a rastreabilidade sanitária é uma obrigação legal (art. 16, I). Os registros de aceite são mantidos para provarmos o consentimento (art. 8º, §2º).</p></section>

      <section class="stack"><h2>6. Seus direitos (art. 18)</h2>
        <p>Depois de entrar, em <strong>Meus dados</strong>, você pode:</p>
        <ul>
          <li>ver e corrigir os seus dados;</li>
          <li>baixar uma cópia em formato JSON (portabilidade);</li>
          <li><strong>revogar o consentimento de dados de saúde</strong>: a partir daí, não é possível enviar novas receitas nem fazer novos pedidos;</li>
          <li>excluir a conta e anonimizar os dados;</li>
          <li>alterar a senha.</li>
        </ul>
        <p>Para qualquer outro pedido, fale com o encarregado.</p></section>

      <section class="stack"><h2>7. Segurança</h2>
        <p>A comunicação usa token de acesso com prazo de validade, senhas ficam em hash com salt, cada perfil acessa só o que precisa e o envio de arquivos é validado. Nenhum sistema é 100% seguro; em caso de incidente relevante, avisaremos você e a autoridade competente.</p></section>

      <section class="stack"><h2>8. Encarregado (DPO)</h2>
        <p>Canal do encarregado pelo tratamento de dados: <a :href="`mailto:${EMAIL_ENCARREGADO}`">{{ EMAIL_ENCARREGADO }}</a> (endereço fictício).</p></section>

      <section class="stack"><h2>9. Mudanças nesta política</h2>
        <p>Esta é a versão {{ VERSAO_TERMO }}. Se algo mudar, a versão muda e pedimos um novo aceite. Veja também os <RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink>.</p></section>
    </article>
  </PublicoLayout>
</template>
