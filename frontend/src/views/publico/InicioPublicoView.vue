<script setup>
import PublicoLayout from './PublicoLayout.vue'

const PASSOS = [
  ['Envie a receita', 'Você manda a foto ou o PDF da receita e dá o consentimento específico para o uso dos seus dados de saúde.'],
  ['Monte a sua Box', 'Escolhe os medicamentos de uso contínuo e os horários. O sistema confere o estoque e reserva tudo na hora de confirmar.'],
  ['A farmácia confere e separa', 'A farmacêutica responsável analisa a receita item a item. Depois a equipe separa os sachês, lote a lote.'],
  ['Receba em casa', 'O entregador leva a Box até a sua porta e registra quem recebeu. Perto do fim do ciclo, avisamos sobre a renovação.'],
]
const SEGURANCA = [
  ['Seus dados de saúde só com o seu consentimento', 'A receita só é enviada depois do seu aceite específico (LGPD, art. 11, I). Você pode revogar quando quiser.'],
  ['Cada pessoa vê só o que precisa', 'O entregador recebe nome, endereço, telefone e quantidade de sachês. Medicamentos, lotes e receita ficam fora da tela dele.'],
  ['Você manda nos seus dados', 'Em "Meus dados" você consulta, corrige, baixa uma cópia ou exclui a sua conta (art. 18).'],
  ['Senha guardada com segurança', 'Sua senha nunca é guardada como texto: usamos hash com salt. Cinco erros seguidos bloqueiam a conta por 15 minutos.'],
]
const ACESSIBILIDADE = [
  'Três temas: claro, escuro e alto contraste.',
  'Três tamanhos de texto, sem quebrar o layout.',
  'Tradução para Libras com o VLibras, do Governo Federal.',
  'Foco do teclado sempre visível e navegação completa só com o teclado.',
  'Fontes pensadas para leitura fácil (Lexend e Atkinson Hyperlegible).',
]
</script>

<template>
  <PublicoLayout remedios>
    <section class="pub-hero">
      <h1>A dose certa, no horário certo, na sua porta.</h1>
      <p>O MedFlow organiza os seus medicamentos de uso contínuo em sachês separados por horário, confere a receita com uma farmacêutica e entrega em casa. Você não precisa lembrar de renovar: avisamos antes.</p>
      <div class="row">
        <RouterLink class="btn btn-primario" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
        <RouterLink class="btn" :to="{ name: 'login' }">Entrar</RouterLink>
      </div>
      <p class="subtle">Projeto acadêmico (UNIP). Ambiente de demonstração com dados fictícios. Existem outras empresas chamadas Medflow, em nichos diferentes: o nosso é entrega rastreável de medicamento de uso contínuo, em sachês por horário.</p>
    </section>

    <section class="pub-secao pub-site" aria-labelledby="h-site">
      <div class="card row-between">
        <div class="stack"><h2 id="h-site">Conheça a página institucional do MedFlow</h2>
          <p class="muted">Menu, telas do sistema, números do projeto e o passo a passo, como seria o site da empresa.</p></div>
        <RouterLink class="btn btn-primario" :to="{ name: 'site' }">Ver a página institucional</RouterLink>
      </div>
    </section>

    <section class="pub-secao" aria-labelledby="h-como">
      <h2 id="h-como">Como funciona</h2>
      <ol class="pub-passos">
        <li v-for="([titulo, texto], i) in PASSOS" :key="titulo" class="card">
          <h3>{{ i + 1 }}. {{ titulo }}</h3>
          <p class="muted">{{ texto }}</p>
        </li>
      </ol>
    </section>

    <section class="pub-secao" aria-labelledby="h-seg">
      <h2 id="h-seg">Segurança e LGPD</h2>
      <div class="grid-2">
        <article v-for="[titulo, texto] in SEGURANCA" :key="titulo" class="card stack">
          <h3>{{ titulo }}</h3><p class="muted">{{ texto }}</p>
        </article>
      </div>
      <p class="subtle">Leia os <RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink> e a <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink>.</p>
    </section>

    <section class="pub-secao" aria-labelledby="h-a11y">
      <h2 id="h-a11y">Acessibilidade</h2>
      <p class="muted">Feito para ser usado por quem toma remédio todo dia, inclusive por pessoas idosas e com deficiência.</p>
      <ul class="pub-lista"><li v-for="a in ACESSIBILIDADE" :key="a">{{ a }}</li></ul>
    </section>

    <section class="pub-secao pub-chamada">
      <h2>Pronto para começar?</h2>
      <p class="muted">Crie a conta em poucos minutos e envie a sua primeira receita.</p>
      <div class="row">
        <RouterLink class="btn btn-primario" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
        <RouterLink class="btn" :to="{ name: 'login' }">Entrar</RouterLink>
      </div>
    </section>
  </PublicoLayout>
</template>
