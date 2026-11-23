import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const P = ['Paciente']
const FARMA = ['Administrador']
const EQUIPE = ['Administrador', 'Funcionario']

const routes = [
  // Páginas públicas com layout próprio (aberta: também acessíveis a quem já entrou).
  { path: '/', name: 'home', component: () => import('../views/publico/InicioPublicoView.vue'), meta: { publica: true, aberta: true, titulo: 'Início' } },
  { path: '/site', name: 'site', component: () => import('../views/publico/SiteView.vue'), meta: { publica: true, aberta: true, titulo: 'Página institucional' } },
  { path: '/termos', name: 'termos', component: () => import('../views/publico/TermosView.vue'), meta: { publica: true, aberta: true, titulo: 'Termos de Uso' } },
  { path: '/privacidade', name: 'privacidade', component: () => import('../views/publico/PrivacidadeView.vue'), meta: { publica: true, aberta: true, titulo: 'Política de Privacidade' } },
  { path: '/contato', name: 'contato', component: () => import('../views/publico/ContatoView.vue'), meta: { publica: true, aberta: true, titulo: 'Contato' } },
  { path: '/entrar', name: 'login', component: () => import('../views/publico/LoginView.vue'), meta: { publica: true, titulo: 'Entrar' } },
  { path: '/cadastro', name: 'cadastro', component: () => import('../views/publico/CadastroView.vue'), meta: { publica: true, titulo: 'Criar conta' } },

  { path: '/inicio', name: 'inicio', component: () => import('../views/paciente/InicioView.vue'), meta: { perfis: P, titulo: 'Início' } },
  { path: '/medicamentos', name: 'medicamentos', component: () => import('../views/paciente/MedicamentosView.vue'), meta: { perfis: P, titulo: 'Medicamentos' } },
  { path: '/montar-box', name: 'box', component: () => import('../views/paciente/MontarBoxView.vue'), meta: { perfis: P, titulo: 'Montar Box' } },
  { path: '/meus-pedidos/:id?', name: 'pedidos', component: () => import('../views/paciente/MeusPedidosView.vue'), meta: { perfis: P, titulo: 'Meus pedidos' } },
  { path: '/receitas', name: 'receitas', component: () => import('../views/paciente/ReceitasView.vue'), meta: { perfis: P, titulo: 'Receitas' } },
  { path: '/meus-dados', name: 'dados', component: () => import('../views/paciente/MeusDadosView.vue'), meta: { titulo: 'Meus dados' } },

  { path: '/painel', name: 'painel', component: () => import('../views/equipe/PainelView.vue'), meta: { perfis: EQUIPE, titulo: 'Painel da operação' } },
  { path: '/analise-receitas', name: 'fila', component: () => import('../views/equipe/AnaliseReceitasView.vue'), meta: { perfis: FARMA, titulo: 'Análise de receitas' } },
  { path: '/pedidos', name: 'operacao', component: () => import('../views/equipe/PedidosView.vue'), meta: { perfis: EQUIPE, titulo: 'Pedidos' } },
  { path: '/estoque', name: 'estoque', component: () => import('../views/equipe/EstoqueView.vue'), meta: { perfis: EQUIPE, titulo: 'Estoque' } },
  { path: '/usuarios', name: 'usuarios', component: () => import('../views/equipe/UsuariosView.vue'), meta: { perfis: FARMA, titulo: 'Usuários' } },

  { path: '/entregas', name: 'entregas', component: () => import('../views/entregador/EntregasView.vue'), meta: { perfis: ['Entregador'], titulo: 'Minhas entregas' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

// Guarda de rotas por perfil. É conveniência de interface: quem garante o acesso é a API (JWT + [Authorize]).
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.aberta) return true
  if (to.meta.publica) return auth.autenticado ? { name: auth.rotaInicial } : true
  if (!auth.autenticado) return { name: 'login', query: { voltar: to.fullPath } }
  if (to.meta.perfis && !to.meta.perfis.includes(auth.perfil)) return { name: auth.rotaInicial }
  return true
})

router.afterEach((to) => { document.title = `${to.meta.titulo ?? 'MedFlow'} · MedFlow Solutions` })

export default router
