<script setup>
// Página institucional do MedFlow Solutions: menu horizontal, rolagem com revelação, números reais do projeto,
// mockup com abas (prints do próprio sistema) e passo a passo com linha de progresso.
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MenuAcessibilidade from '../../components/MenuAcessibilidade.vue'
import { useAuthStore } from '../../stores/auth.js'
import { useUiStore } from '../../stores/ui.js'
import { PROGRAMAS } from '../../domain/catalogo.js'
import '../../styles/site.css'

const auth = useAuthStore()
const ui = useUiStore()
const menuAberto = ref(false)
const raiz = ref(null)
const aba = ref(0)
const passoRef = ref(null)

const MENU = [['como', 'Como funciona'], ['box', 'A Box'], ['programas', 'Programas de cuidado'], ['seguranca', 'Segurança'], ['equipe', 'Equipe']]

const ABAS = [
  { rotulo: 'Montar a Box', quem: 'Paciente', img: '/site/11-paciente-montar-box.jpg', alt: 'Tela de montar a Box com medicamentos e filtro por programa', texto: 'O paciente escolhe os medicamentos e os horários. O sistema confere o estoque antes de confirmar.' },
  { rotulo: 'Análise da receita', quem: 'Farmacêutica', img: '/site/21-admin-analise-receita.jpg', alt: 'Tela de análise de receita pela farmacêutica responsável', texto: 'A farmacêutica responsável confere a receita item a item e aprova ou recusa com justificativa.' },
  { rotulo: 'Separação', quem: 'Operador', img: '/site/30-operador-separacao.jpg', alt: 'Tela de separação dos sachês por horário', texto: 'A equipe separa os sachês horário a horário, lote a lote, sempre pelo lote que vence primeiro.' },
  { rotulo: 'Entrega', quem: 'Entregador', img: '/site/40-entregador-celular.jpg', alt: 'Tela do entregador no celular', retrato: true, texto: 'O entregador vê só endereço, telefone e quantidade de sachês, e registra quem recebeu.' },
]

const NUMEROS = [
  { valor: 72, rotulo: 'medicamentos de uso contínuo para montar a Box' },
  { valor: 30, rotulo: 'dias de tratamento em cada Box' },
  { valor: 4, rotulo: 'perfis de acesso, cada um vendo só o que precisa' },
  { valor: 82, rotulo: 'testes automatizados do site e da API, todos aprovados' },
]

const PASSOS = [
  ['Envie a receita', 'Foto ou PDF, com o seu consentimento específico para o uso dos dados de saúde.'],
  ['Monte a sua Box', 'Medicamentos e horários, conferidos contra o estoque na hora.'],
  ['A farmácia confere', 'A farmacêutica analisa a receita e a equipe separa os sachês.'],
  ['Receba em casa', 'O entregador leva até a porta. Perto do fim do ciclo, avisamos para renovar.'],
]

const SACHES = [
  ['06:00', 'Levotiroxina 50 mcg'],
  ['08:00', 'Losartana 50 mg', 'Metformina 850 mg'],
  ['12:00', 'AAS 100 mg'],
  ['22:00', 'Sinvastatina 20 mg'],
]

const SEGURANCA = [
  ['Consentimento antes da receita', 'O envio só acontece depois do seu aceite específico (LGPD, art. 11, I). Você pode revogar quando quiser.'],
  ['Cada pessoa vê só o que precisa', 'O entregador não vê medicamentos, lotes nem receita.'],
  ['Seus dados, sob o seu comando', 'Consulte, corrija, baixe uma cópia ou exclua a conta em "Meus dados".'],
  ['Senha guardada com segurança', 'Hash com salt e bloqueio de 15 minutos após cinco erros seguidos.'],
]

const EQUIPE = ['Gabriel Pimenta', 'Hendrew Braz', 'Manuel Douglas', 'Matheus Renato', 'Matheus Ribeiro']

const sem = computed(() => ui.prefs.reduzirMovimento || (typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches))
const exibidos = ref(NUMEROS.map(() => 0))
let observador
let rAF

function contar() {
  if (sem.value) { exibidos.value = NUMEROS.map((n) => n.valor); return }
  const ini = performance.now()
  const dur = 1300
  const passo = (t) => {
    const p = Math.min(1, (t - ini) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    exibidos.value = NUMEROS.map((n) => Math.round(n.valor * e))
    if (p < 1) rAF = requestAnimationFrame(passo)
  }
  rAF = requestAnimationFrame(passo)
}

function progressoPassos() {
  const el = passoRef.value
  if (!el) return
  if (sem.value) { el.style.setProperty('--p', 1); return }
  const r = el.getBoundingClientRect()
  const alto = window.innerHeight
  const p = Math.min(1, Math.max(0, (alto * 0.85 - r.top) / (r.height + alto * 0.2)))
  el.style.setProperty('--p', p.toFixed(3))
}

function irPara(id) {
  menuAberto.value = false
  const alvo = document.getElementById(id)
  if (alvo) alvo.scrollIntoView({ behavior: sem.value ? 'auto' : 'smooth', block: 'start' })
}

function teclaAba(e, i) {
  const n = ABAS.length
  let novo = i
  if (e.key === 'ArrowRight') novo = (i + 1) % n
  else if (e.key === 'ArrowLeft') novo = (i - 1 + n) % n
  else return
  e.preventDefault()
  aba.value = novo
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
      observador.unobserve(i.target)
    }), { threshold: 0.18 })
    els.forEach((e) => observador.observe(e))
  }
  if (sem.value) contar()
  window.addEventListener('scroll', progressoPassos, { passive: true })
  progressoPassos()
})
onBeforeUnmount(() => { observador?.disconnect(); cancelAnimationFrame(rAF); window.removeEventListener('scroll', progressoPassos) })
</script>

<template>
  <div ref="raiz" class="st" :class="{ 'st-sem-movimento': sem }">
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
        <div class="st-hero-texto st-entra">
          <h1>A dose certa, no horário certo, na sua porta.</h1>
          <p>O MedFlow Solutions organiza os seus medicamentos de uso contínuo em sachês separados por horário, confere a receita com uma farmacêutica e entrega em casa. Você não precisa lembrar de renovar: avisamos antes.</p>
          <div class="row">
            <RouterLink class="btn btn-primario" :to="{ name: 'cadastro' }">Criar conta</RouterLink>
            <RouterLink class="btn" :to="{ name: 'login' }">Entrar na demonstração</RouterLink>
          </div>
        </div>

        <div class="st-mock st-entra st-atraso">
          <div class="st-mock-barra" role="tablist" aria-label="Telas do sistema">
            <button v-for="(a, i) in ABAS" :id="`aba-${i}`" :key="a.rotulo" class="st-aba" type="button" role="tab" :aria-selected="aba === i" :aria-controls="`painel-${i}`" :tabindex="aba === i ? 0 : -1" @click="aba = i" @keydown="teclaAba($event, i)">{{ a.rotulo }}</button>
          </div>
          <div class="st-mock-corpo">
            <div v-for="(a, i) in ABAS" v-show="aba === i" :id="`painel-${i}`" :key="a.rotulo" class="st-painel" role="tabpanel" :aria-labelledby="`aba-${i}`">
              <div class="st-tela" :class="{ retrato: a.retrato }"><img :src="a.img" :alt="a.alt" loading="lazy" /></div>
              <p><strong>{{ a.quem }}.</strong> {{ a.texto }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="st-numeros revela" aria-label="O projeto em números">
        <dl>
          <div v-for="(n, i) in NUMEROS" :key="n.rotulo"><dt>{{ exibidos[i] }}</dt><dd>{{ n.rotulo }}</dd></div>
        </dl>
      </section>

      <section id="como" class="st-secao" aria-labelledby="h-como">
        <h2 id="h-como" class="revela">Como funciona</h2>
        <ol ref="passoRef" class="st-passos">
          <li v-for="([titulo, texto], i) in PASSOS" :key="titulo" class="revela" :style="{ '--d': `${i * 90}ms` }">
            <span class="st-passo-n">{{ i + 1 }}</span>
            <h3>{{ titulo }}</h3>
            <p>{{ texto }}</p>
          </li>
        </ol>
      </section>

      <section id="box" class="st-secao st-box" aria-labelledby="h-box">
        <div class="revela">
          <h2 id="h-box">Cada horário, um sachê</h2>
          <p class="st-lead">Em vez de uma caixa por remédio, você recebe os comprimidos de cada horário juntos, identificados com o dia e a hora. Menos dúvida na hora de tomar, menos esquecimento.</p>
        </div>
        <ul class="st-saches revela" aria-label="Exemplo de sachês de um dia">
          <li v-for="[hora, ...itens] in SACHES" :key="hora"><strong class="mono">{{ hora }}</strong><span v-for="i in itens" :key="i">{{ i }}</span></li>
        </ul>
        <p class="st-nota revela">Exemplo ilustrativo, de um paciente fictício.</p>
      </section>

      <section id="programas" class="st-secao" aria-labelledby="h-prog">
        <h2 id="h-prog" class="revela">Programas de cuidado</h2>
        <p class="st-lead revela">O catálogo é organizado pelo tipo de cuidado, para o paciente achar o que procura sem rolar uma lista enorme.</p>
        <ul class="st-programas revela"><li v-for="p in PROGRAMAS" :key="p">{{ p }}</li></ul>
        <p class="st-nota revela">Medicamentos de controle especial, antimicrobianos e os que exigem cadeia fria ficam fora da Box por regra de segurança.</p>
      </section>

      <section id="seguranca" class="st-secao" aria-labelledby="h-seg">
        <h2 id="h-seg" class="revela">Segurança e LGPD</h2>
        <div class="st-grade">
          <article v-for="([t, x], i) in SEGURANCA" :key="t" class="revela" :style="{ '--d': `${i * 80}ms` }"><h3>{{ t }}</h3><p>{{ x }}</p></article>
        </div>
        <p class="st-nota revela">Leia os <RouterLink :to="{ name: 'termos' }">Termos de Uso</RouterLink> e a <RouterLink :to="{ name: 'privacidade' }">Política de Privacidade</RouterLink>. O sistema também tem três temas, três tamanhos de texto, Libras (VLibras) e navegação completa pelo teclado.</p>
      </section>

      <section id="equipe" class="st-secao" aria-labelledby="h-eq">
        <h2 id="h-eq" class="revela">Quem fez</h2>
        <p class="st-lead revela">Projeto Integrado Multidisciplinar do curso de Análise e Desenvolvimento de Sistemas da UNIP, desenvolvido por cinco estudantes.</p>
        <ul class="st-equipe revela"><li v-for="e in EQUIPE" :key="e">{{ e }}</li></ul>
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
