<script setup>
import PublicoLayout from './PublicoLayout.vue'
import { CONTROLADOR, EMAIL_CONTATO, VERSAO_TERMO, VIGENTE_DESDE } from './dadosLegais.js'
</script>

<template>
  <PublicoLayout>
    <article class="pub-texto">
      <span class="eyebrow">Versão {{ VERSAO_TERMO }} · em vigor desde {{ VIGENTE_DESDE }}</span>
      <h1>Termos de Uso</h1>
      <div class="aviso aviso-espera"><p><strong>Ambiente de demonstração com dados fictícios.</strong> O MedFlow é um projeto acadêmico (UNIP). {{ CONTROLADOR }} é uma organização fictícia. Não use dados reais de saúde neste ambiente.</p></div>

      <section class="stack"><h2>1. O que é o MedFlow</h2>
        <p>O MedFlow é um sistema que ajuda pessoas em tratamento contínuo a receber os medicamentos em casa. Você envia a receita, monta a sua Box de sachês por horário, uma farmacêutica confere a receita e a equipe separa e entrega.</p></section>

      <section class="stack"><h2>2. Quem pode usar</h2>
        <p>Pacientes maiores de 18 anos ou representados por responsável legal, com CPF válido e endereço de entrega. Equipe da farmácia e entregadores recebem acesso pela administração do sistema.</p></section>

      <section class="stack"><h2>3. Sua conta</h2>
        <p>Guarde a sua senha com cuidado e não a compartilhe. Cinco erros seguidos de senha bloqueiam a conta por 15 minutos. Se você suspeitar de uso indevido, troque a senha em "Meus dados" e fale com a gente.</p></section>

      <section class="stack"><h2>4. Receita e Box</h2>
        <p>Nenhum pedido segue para separação sem receita aprovada por farmacêutico. A farmacêutica pode recusar a receita e, nesse caso, informa o motivo. Medicamentos de controle especial, antimicrobianos, itens que exigem retenção de receita e itens que precisam de refrigeração não aparecem para escolha na Box.</p>
        <p>Ao confirmar o pedido, os medicamentos ficam reservados para você. Se faltar algum item, o sistema avisa antes da confirmação.</p></section>

      <section class="stack"><h2>5. Entrega</h2>
        <p>O entregador recebe somente o que precisa para entregar: seu nome, endereço, telefone e a quantidade de sachês. Confirme quem estará no endereço para receber. Se a entrega não for possível, a Box volta para a fila e uma nova tentativa é registrada.</p></section>

      <section class="stack"><h2>6. O que o MedFlow não faz</h2>
        <p>O MedFlow não faz diagnóstico, não prescreve e não substitui a orientação do seu médico ou da sua farmacêutica. Não interrompa nem altere o tratamento por conta própria.</p></section>

      <section class="stack"><h2>7. Seus compromissos</h2>
        <p>Informar dados verdadeiros, enviar apenas receitas suas (ou de quem você representa legalmente) e não tentar burlar as regras do sistema.</p></section>

      <section class="stack"><h2>8. Alterações e encerramento</h2>
        <p>Se estes termos mudarem, a versão muda e pediremos um novo aceite. Você pode excluir a sua conta quando quiser, em "Meus dados". Os efeitos estão descritos na <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink>.</p></section>

      <section class="stack"><h2>9. Dúvidas</h2>
        <p>Escreva para <a :href="`mailto:${EMAIL_CONTATO}`">{{ EMAIL_CONTATO }}</a> ou veja a página de <RouterLink :to="{ name: 'contato' }">contato</RouterLink>.</p></section>
    </article>
  </PublicoLayout>
</template>
