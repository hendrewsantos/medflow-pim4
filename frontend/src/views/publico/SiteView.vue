<script setup>
// Página institucional do MedFlow Solutions: menu horizontal, tour automático pelas telas do sistema, números reais do
// projeto, passo a passo que se acende em sequência, programas de cuidado, segurança, perguntas e equipe.
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MenuAcessibilidade from '../../components/MenuAcessibilidade.vue'
import FundoRemedios from '../../components/FundoRemedios.vue'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { programasDoCatalogo } from '../../domain/catalogo.js'
import '../../styles/site.css'

const auth = useAuthStore()
const ui = useUiStore()
const menuAberto = ref(false)
const raiz = ref(null)
const aba = ref(0)
const tocando = ref(true)
const pausadoPorFoco = ref(false)
const passosAtivos = ref(-1)
const passosRef = ref(null)

const MENU = [['perfis', 'Para quem'], ['como', 'Como funciona'], ['box', 'A Box'], ['programas', 'Programas de cuidado'], ['seguranca', 'Segurança'], ['perguntas', 'Perguntas'], ['equipe', 'Equipe']]

const TOUR = [
  { rotulo: 'Início', quem: 'Paciente', img: '/site/10-paciente-inicio.jpg', alt: 'Início do paciente com os atalhos do dia', texto: 'O paciente abre o sistema e vê o que fazer hoje: renovar a Box, enviar a receita, acompanhar a entrega ou conferir os horários.' },
  { rotulo: 'Montar Box', quem: 'Paciente', img: '/site/11-paciente-montar-box.jpg', alt: 'Escolha de medicamentos filtrada por programa de cuidado', texto: 'Ele escolhe os medicamentos por programa de cuidado, define horários e dose, e o sistema confere o estoque antes de confirmar.' },
  { rotulo: 'Pagamento', quem: 'Paciente', img: '/site/12-paciente-pedidos.jpg', alt: 'Pedido aguardando pagamento com contagem regressiva', texto: 'Ao confirmar, os medicamentos ficam reservados e o paciente tem alguns minutos para pagar (pagamento simulado, sem cobrança real).' },
  { rotulo: 'Análise', quem: 'Farmacêutica', img: '/site/21-admin-analise-receita.jpg', alt: 'Análise de receita pela farmacêutica responsável', texto: 'A farmacêutica responsável confere a receita item a item, vê os pontos de atenção entre os medicamentos e aprova ou recusa com justificativa.' },
  { rotulo: 'Separação', quem: 'Operador', img: '/site/30-operador-separacao.jpg', alt: 'Etiquetas dos sachês com lote e validade', texto: 'A equipe separa os sachês horário a horário, lote a lote, sempre pelo lote que vence primeiro, e finaliza com a baixa no estoque.' },
  { rotulo: 'Entrega', quem: 'Entregador', img: '/site/40-entregador-celular.jpg', alt: 'Tela do entregador no celular', retrato: true, texto: 'O entregador vê só endereço, telefone e quantidade de sachês, inicia a rota e registra quem recebeu.' },
  { rotulo: 'Painel', quem: 'Farmacêutica', img: '/site/20-admin-painel.jpg', alt: 'Painel da operação com indicadores', texto: 'O painel mostra tempo de ciclo, entregas na primeira tentativa, estoque abaixo do mínimo e lotes a vencer.' },
]
const INTERVALO = 6000
const VERSAO_IMAGENS = 3 // muda quando os prints são refeitos, para o navegador não usar a cópia antiga

const NUMEROS = [
  { valor: 72, rotulo: 'medicamentos de uso contínuo para montar a Box' },
  { valor: 30, rotulo: 'dias de tratamento em cada Box' },
  { valor: 4, rotulo: 'perfis de acesso, cada um vendo só o que precisa' },
  { valor: 85, rotulo: 'testes automatizados do site e da API, todos aprovados' },
]

const PERFIS = [
  ['Paciente', 'Quem toma o remédio todo dia.', ['Envia a receita com consentimento específico', 'Monta a Box e paga (simulado)', 'Acompanha a entrega e a hora de renovar', 'Exporta, corrige ou exclui os próprios dados']],
  ['Farmacêutica responsável', 'Quem garante a segurança da Box.', ['Confere a receita item a item', 'Aprova ou recusa com justificativa', 'Vê pontos de atenção entre medicamentos', 'Gerencia estoque, usuários e relatórios']],
  ['Operador da farmácia', 'Quem monta os sachês.', ['Separa por horário, com etiqueta de lote e validade', 'Finaliza a Box e dá a baixa no estoque', 'Acompanha pedidos e estoque']],
  ['Entregador', 'Quem leva até a porta.', ['Vê só nome, endereço e quantidade de sachês', 'Abre o endereço no mapa e liga para o paciente', 'Confirma com o nome de quem recebeu ou registra ocorrência']],
]

const PASSOS = [
  ['Envie a receita', 'Foto ou PDF, com o seu consentimento específico para o uso dos dados de saúde.', ['Aceite separado, que você pode revogar', 'Arquivo conferido antes de aceitar']],
  ['Monte a sua Box', 'Medicamentos, horários e dose, conferidos contra o estoque na hora.', ['Filtro por programa de cuidado', 'Avisos de combinações a conferir', 'Valor e prazo claros antes de confirmar']],
  ['A farmácia confere', 'A farmacêutica analisa a receita e a equipe separa os sachês pelo lote que vence primeiro.', ['Nenhuma Box segue sem receita aprovada', 'Etiqueta de lote e validade em cada sachê']],
  ['Receba em casa', 'O entregador leva até a porta e registra quem recebeu. Perto do fim do ciclo, avisamos para renovar.', ['Linha do tempo sempre atualizada', 'Aviso de renovação antes de acabar']],
]

const SACHES = [
  ['06:00', 'Levotiroxina 50 mcg'],
  ['08:00', 'Losartana 50 mg', 'Metformina 850 mg'],
  ['12:00', 'AAS 100 mg'],
  ['22:00', 'Sinvastatina 20 mg'],
]
const BENEFICIOS = [
  ['Um horário, um sachê', 'Tudo o que você toma àquela hora vem junto, com o dia e a hora impressos.'],
  ['Lote e validade em cada um', 'A etiqueta permite rastrear de onde veio cada comprimido.'],
  ['Menos esquecimento', 'Em vez de abrir várias caixas, você abre o sachê da hora.'],
]

const PROGRAMAS_DESCRICAO = {
  'Hipertensão': 'Controle da pressão arterial.',
  'Diabetes': 'Controle da glicose.',
  'Colesterol': 'Redução de colesterol e triglicerídeos.',
  'Tireoide': 'Reposição e controle hormonal.',
  'Estômago': 'Proteção e tratamento gástrico.',
  'Vitaminas e minerais': 'Suplementos de uso contínuo.',
  'Coração e circulação': 'Proteção cardiovascular e circulação.',
  'Outros cuidados': 'Próstata, alergia, dor, neurologia e mais.',
}
const programas = programasDoCatalogo().map((p) => ({ ...p, descricao: PROGRAMAS_DESCRICAO[p.programa] }))

const SEGURANCA = [
  ['Consentimento antes da receita', 'O envio só acontece depois do seu aceite específico (LGPD, art. 11, I). Você pode revogar quando quiser.'],
  ['Cada pessoa vê só o que precisa', 'O entregador não vê medicamentos, lotes nem receita. O CPF aparece mascarado para a equipe.'],
  ['Seus dados, sob o seu comando', 'Consulte, corrija, baixe uma cópia ou exclua a conta em "Meus dados" (art. 18).'],
  ['Senha guardada com segurança', 'Nunca como texto: hash com salt. Cinco erros seguidos bloqueiam a conta por 15 minutos.'],
  ['Estoque que não se perde', 'A reserva acontece na confirmação, tudo ou nada. Testado com 20 pedidos simultâneos sobre o mesmo lote.'],
  ['Regras também no banco', 'O banco de dados recusa transições de status inválidas e não deixa apagar o histórico de estoque.'],
]
const ACESSIBILIDADE = ['Três temas: claro, escuro e alto contraste', 'Três tamanhos de texto, sem quebrar o layout', 'Tradução para Libras com o VLibras', 'Opção de reduzir movimentos', 'Navegação completa pelo teclado']

const PERGUNTAS = [
  ['Isso cobra de verdade?', 'Não. O MedFlow é um projeto acadêmico e o pagamento é simulado: nenhum valor real é cobrado e nenhum dado de pagamento é pedido. Todos os dados são fictícios.'],
  ['Posso pedir medicamento controlado?', 'Não. Medicamentos de controle especial (Portaria SVS/MS 344/1998), antimicrobianos e os que exigem cadeia fria ficam fora da Box por regra de segurança.'],
  ['E se eu não pagar a tempo?', 'O pedido é cancelado e os medicamentos reservados voltam ao estoque. Você pode montar outra Box.'],
  ['E se a minha receita vencer?', 'O sistema avisa quando a receita vale menos que a Box e pede uma nova antes da próxima renovação.'],
  ['Quem vê os meus dados de saúde?', 'A farmacêutica responsável e a equipe que separa a Box. O entregador não vê medicamentos nem receita.'],
]

const TECNOLOGIAS = [['API', 'ASP.NET Core 8 e Entity Framework'], ['Banco', 'MySQL 8, com gatilhos e procedimentos'], ['Site', 'Vue 3 e Vite'], ['App do entregador', 'Kotlin e Jetpack Compose'], ['Entrega contínua', 'Docker e GitHub Actions']]
const EQUIPE = [
  { nome: 'Gabriel Pimenta', linkedin: 'https://www.linkedin.com/in/gabriel-pimenta-790276360/' },
  { nome: 'Hendrew Braz', linkedin: 'https://www.linkedin.com/in/hendrew-santos2530' },
  { nome: 'Manuel Douglas', linkedin: 'https://www.linkedin.com/in/mdouglas12/' },
  { nome: 'Matheus Renato', linkedin: 'https://www.linkedin.com/in/matheus-estevo-281582357/' },
  { nome: 'Matheus Ribeiro', linkedin: 'https://www.linkedin.com/in/matheus-santos-3733923a8/' },
]

const sem = computed(() => ui.prefs.reduzirMovimento || (typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches))
const exibidos = ref(NUMEROS.map(() => 0))
let observador
let rAF
let temporizador
let ritmoPassos

function contar() {
  if (sem.value) { exibidos.value = NUMEROS.map((n) => n.valor); return }
  const ini = performance.now()
  const dur = 1800
  const passo = (t) => {
    const p = Math.min(1, (t - ini) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    exibidos.value = NUMEROS.map((n) => Math.round(n.valor * e))
    if (p < 1) rAF = requestAnimationFrame(passo)
  }
  rAF = requestAnimationFrame(passo)
}

/** O passo a passo se acende um a um, devagar, quando a seção aparece. */
function acenderPassos() {
  if (sem.value) { passosAtivos.value = PASSOS.length - 1; return }
  passosAtivos.value = 0
  ritmoPassos = setInterval(() => {
    if (passosAtivos.value >= PASSOS.length - 1) { clearInterval(ritmoPassos); return }
    passosAtivos.value += 1
  }, 2200)
}

const andando = computed(() => tocando.value && !pausadoPorFoco.value && !sem.value)
function agendarTour() {
  clearTimeout(temporizador)
  if (!andando.value) return
  temporizador = setTimeout(() => { aba.value = (aba.value + 1) % TOUR.length; agendarTour() }, INTERVALO)
}
function irParaAba(i) { aba.value = i; agendarTour() }
function alternarTour() { tocando.value = !tocando.value; agendarTour() }
function pausar(v) { pausadoPorFoco.value = v; agendarTour() }

function irPara(id) {
  menuAberto.value = false
  document.getElementById(id)?.scrollIntoView({ behavior: sem.value ? 'auto' : 'smooth', block: 'start' })
}

function teclaAba(e, i) {
  const n = TOUR.length
  let novo = i
  if (e.key === 'ArrowRight') novo = (i + 1) % n
  else if (e.key === 'ArrowLeft') novo = (i - 1 + n) % n
  else return
  e.preventDefault()
  irParaAba(novo)
  nextTick(() => document.getElementById(`aba-${novo}`)?.focus())
}

onMounted(() => {
  const els = raiz.value.querySelectorAll('.revela')
  if (sem.value || !('IntersectionObserver' in window)) els.forEach((e) => e.classList.add('visivel'))
  else {
    observador = new IntersectionObserver((itens) => itens.forEach((i) => {
      if (!i.isIntersecting) return
      i.target.classList.add('visivel')
      if (i.target.classList.contains('st-numeros')) contar()
      if (i.target === passosRef.value) acenderPassos()
      observador.unobserve(i.target)
    }), { threshold: 0.25 })
    els.forEach((e) => observador.observe(e))
    observador.observe(passosRef.value)
  }
  if (sem.value) { contar(); acenderPassos() }
  agendarTour()
})
onBeforeUnmount(() => { observador?.disconnect(); cancelAnimationFrame(rAF); clearTimeout(temporizador); clearInterval(ritmoPassos) })
</script>

<template>
  <div ref="raiz" class="st" :class="{ 'st-sem-movimento': sem }">
    <div class="st-fundo" aria-hidden="true"></div>
    <a class="pular" href="#st-conteudo">Pular para o conteúdo</a>
    <header class="st-topo">
      <RouterLink class="st-marca" :to="{ name: 'site' }" aria-label="MedFlow Solutions, início"><img src="/simbolo-medflow.png" alt="" /><span>MedFlow <small>Solutions</small></span></RouterLink>
      <button class="st-menu-botao btn btn-sm" type="button" :aria-expanded="menuAberto" aria-controls="st-nav" @click="menuAberto = !menuAberto">Menu</button>
      <nav id="st-nav" class="st-nav" :class="{ aberto: menuAberto }" aria-label="Seções da página">
        <a v-for="[id, rotulo] in MENU" :key="id" :href="`#${id}`" @click.prevent="irPara(id)">{{ rotulo }}</a>
      </nav>
      <div class="st-acoes">
        <MenuAcessibilidade />
        <RouterLink v-if="auth.autenticado" class="btn btn-primario btn-sm" :to="{ name: auth.rotaInicial }">Ir para o sistema</RouterLink>
        <template v-else>
          <RouterLink class="btn btn-sm" :to="{ name: 'login' }">Entrar</RouterLink>
          <RouterLink class="btn btn-primario btn-sm" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
        </template>
      </div>
    </header>

    <main id="st-conteudo" tabindex="-1">
      <section class="st-hero">
        <FundoRemedios />
        <div class="st-hero-texto st-entra">
          <h1>A dose certa, no horário certo, na sua porta.</h1>
          <p>O MedFlow Solutions organiza os seus medicamentos de uso contínuo em sachês separados por horário, confere a receita com uma farmacêutica e entrega em casa. Você não precisa lembrar de renovar: avisamos antes.</p>
          <div class="row">
            <RouterLink class="btn btn-primario" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
            <RouterLink class="btn" :to="{ name: 'login' }">Entrar na demonstração</RouterLink>
          </div>
        </div>

        <div class="st-mock st-entra st-atraso" @mouseenter="pausar(true)" @mouseleave="pausar(false)" @focusin="pausar(true)" @focusout="pausar(false)">
          <div class="st-mock-barra" role="tablist" aria-label="Telas do sistema">
            <button v-for="(a, i) in TOUR" :id="`aba-${i}`" :key="a.rotulo" class="st-aba" type="button" role="tab" :aria-selected="aba === i" :aria-controls="`painel-${i}`" :tabindex="aba === i ? 0 : -1" @click="irParaAba(i)" @keydown="teclaAba($event, i)">
              {{ a.rotulo }}
              <span v-if="aba === i && andando" :key="`p-${i}-${tocando}`" class="st-progresso" :style="{ animationDuration: `${INTERVALO}ms` }"></span>
            </button>
          </div>
          <div class="st-mock-corpo">
            <div class="st-paineis">
              <div v-for="(a, i) in TOUR" :id="`painel-${i}`" :key="a.rotulo" class="st-painel" :class="{ ativo: aba === i }" role="tabpanel" :aria-labelledby="`aba-${i}`" :aria-hidden="aba !== i">
                <div class="st-tela" :class="{ retrato: a.retrato }"><img :src="`${a.img}?v=${VERSAO_IMAGENS}`" :alt="a.alt" /></div>
                <p><strong>{{ a.quem }}.</strong> {{ a.texto }}</p>
              </div>
            </div>
            <button v-if="!sem" class="st-pausa btn btn-sm" type="button" :aria-pressed="!tocando" @click="alternarTour">{{ tocando ? 'Pausar tour' : 'Retomar tour' }}</button>
          </div>
        </div>
      </section>

      <section class="st-numeros revela" aria-label="O projeto em números">
        <dl>
          <div v-for="(n, i) in NUMEROS" :key="n.rotulo"><dt>{{ exibidos[i] }}</dt><dd>{{ n.rotulo }}</dd></div>
        </dl>
      </section>

      <section id="perfis" class="st-secao" aria-labelledby="h-perfis">
        <h2 id="h-perfis" class="revela">Um sistema, quatro perfis</h2>
        <p class="st-lead revela">Cada pessoa entra com a sua conta e vê só o que precisa para fazer o seu trabalho.</p>
        <div class="st-grade st-grade-4">
          <article v-for="([nome, resumo, itens], i) in PERFIS" :key="nome" class="revela" :style="{ '--d': `${i * 90}ms` }">
            <h3>{{ nome }}</h3><p class="st-resumo">{{ resumo }}</p>
            <ul><li v-for="x in itens" :key="x">{{ x }}</li></ul>
          </article>
        </div>
      </section>

      <section id="como" class="st-secao" aria-labelledby="h-como">
        <h2 id="h-como" class="revela">Como funciona</h2>
        <ol ref="passosRef" class="st-passos" :style="{ '--p': passosAtivos < 0 ? 0 : passosAtivos / (PASSOS.length - 1) }">
          <li v-for="([titulo, texto, itens], i) in PASSOS" :key="titulo" :class="{ ativo: i <= passosAtivos }">
            <span class="st-passo-n">{{ i + 1 }}</span>
            <h3>{{ titulo }}</h3>
            <p>{{ texto }}</p>
            <ul><li v-for="x in itens" :key="x">{{ x }}</li></ul>
          </li>
        </ol>
      </section>

      <section id="box" class="st-secao st-box" aria-labelledby="h-box">
        <div class="revela">
          <h2 id="h-box">Cada horário, um sachê</h2>
          <p class="st-lead">Em vez de uma caixa por remédio, você recebe os comprimidos de cada horário juntos, identificados com o dia e a hora. Menos dúvida na hora de tomar, menos esquecimento.</p>
        </div>
        <ul class="st-saches revela" aria-label="Exemplo de sachês de um dia">
          <li v-for="[hora, ...itens] in SACHES" :key="hora"><strong class="mono">{{ hora }}</strong><span v-for="i in itens" :key="i">{{ i }}</span><small>Lote e validade na etiqueta</small></li>
        </ul>
        <p class="st-nota revela">Exemplo ilustrativo, de um paciente fictício.</p>
        <div class="st-grade st-grade-3">
          <article v-for="([t, x], i) in BENEFICIOS" :key="t" class="revela" :style="{ '--d': `${i * 90}ms` }"><h3>{{ t }}</h3><p>{{ x }}</p></article>
        </div>
      </section>

      <section id="programas" class="st-secao" aria-labelledby="h-prog">
        <h2 id="h-prog" class="revela">Programas de cuidado</h2>
        <p class="st-lead revela">O catálogo é organizado pelo tipo de cuidado, para o paciente achar o que procura sem rolar uma lista enorme.</p>
        <div class="st-grade st-grade-4">
          <article v-for="(p, i) in programas" :key="p.programa" class="revela" :style="{ '--d': `${(i % 4) * 80}ms` }">
            <h3>{{ p.programa }}</h3>
            <p class="st-resumo">{{ p.descricao }}</p>
            <p class="st-contagem"><strong>{{ p.total }}</strong> {{ p.total === 1 ? 'medicamento' : 'medicamentos' }}</p>
            <p class="st-exemplos">{{ p.exemplos.join(' · ') }}</p>
          </article>
        </div>
        <p class="st-nota revela">Medicamentos de controle especial, antimicrobianos e os que exigem cadeia fria ficam fora da Box por regra de segurança.</p>
      </section>

      <section id="seguranca" class="st-secao" aria-labelledby="h-seg">
        <h2 id="h-seg" class="revela">Segurança, LGPD e acessibilidade</h2>
        <div class="st-grade st-grade-3">
          <article v-for="([t, x], i) in SEGURANCA" :key="t" class="revela" :style="{ '--d': `${(i % 3) * 80}ms` }"><h3>{{ t }}</h3><p>{{ x }}</p></article>
        </div>
        <div class="st-a11y revela">
          <h3>Feito para ser usado por todos</h3>
          <ul><li v-for="a in ACESSIBILIDADE" :key="a">{{ a }}</li></ul>
        </div>
        <p class="st-nota revela">Leia os <RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink> e a <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink>.</p>
      </section>

      <section id="perguntas" class="st-secao" aria-labelledby="h-perg">
        <h2 id="h-perg" class="revela">Perguntas frequentes</h2>
        <div class="st-faq revela">
          <details v-for="[q, r] in PERGUNTAS" :key="q"><summary>{{ q }}</summary><p>{{ r }}</p></details>
        </div>
      </section>

      <section id="equipe" class="st-secao" aria-labelledby="h-eq">
        <h2 id="h-eq" class="revela">Quem fez</h2>
        <p class="st-lead revela">Projeto Integrado Multidisciplinar IV do curso de Análise e Desenvolvimento de Sistemas da UNIP, desenvolvido por cinco estudantes, com orientação do professor André Gomes de Lira Muniz.</p>
        <ul class="st-equipe revela">
          <li v-for="e in EQUIPE" :key="e.nome">
            <a v-if="e.linkedin" :href="e.linkedin" target="_blank" rel="noopener noreferrer" :aria-label="`${e.nome}, perfil no LinkedIn (abre em nova aba)`"><span>{{ e.nome }}</span><small>LinkedIn ↗</small></a>
            <span v-else class="st-sem-link">{{ e.nome }}</span>
          </li>
        </ul>
        <h3 class="revela st-sub">Tecnologias</h3>
        <dl class="st-tec revela"><div v-for="[k, v] in TECNOLOGIAS" :key="k"><dt>{{ k }}</dt><dd>{{ v }}</dd></div></dl>
      </section>

      <section class="st-final revela">
        <h2>Quer ver funcionando?</h2>
        <p>A demonstração roda no seu navegador, com dados fictícios. Entre como paciente, farmacêutica, operador ou entregador.</p>
        <div class="row">
          <RouterLink class="btn btn-primario" :to="{ name: 'login' }">Entrar na demonstração</RouterLink>
          <RouterLink class="btn" :to="{ name: 'home' }">Página de demonstração</RouterLink>
        </div>
      </section>
    </main>

    <footer class="st-rodape">
      <p>MedFlow Solutions · Projeto acadêmico (UNIP) · Ambiente de demonstração com dados fictícios.</p>
      <p>Existem outras empresas chamadas Medflow, em nichos diferentes. O nosso é entrega rastreável de medicamento de uso contínuo, em sachês por horário.</p>
      <p><RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink> · <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink> · <RouterLink :to="{ name: 'contato' }">Contato</RouterLink></p>
    </footer>
  </div>
</template>
