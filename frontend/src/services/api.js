// Ponto único de acesso aos dados. Com VITE_DEMO=true (npm run dev:demo) as telas usam o servidor
// de demonstração em memória, com as mesmas regras e respostas da API real. Caso contrário, a API ASP.NET Core.
import { api as apiHttp, sessao } from './http.js'
import { salvarBlob } from '../utils/arquivo.js'

export const MODO_DEMO = import.meta.env.VITE_DEMO === 'true'

function criarApiDemo() {
  const pronto = import('./mock/dados.js').then(({ criarDemonstracao }) => criarDemonstracao(new Date(), { pagamento: true }))
  const grupos = ['auth', 'receitas', 'medicamentos', 'pedidos', 'assinatura', 'entregas', 'painel', 'usuarios', 'privacidade', 'previsaoReposicao', 'relatorios', 'demo']
  return Object.fromEntries(grupos.map((g) => [g, new Proxy({}, {
    get: (_, metodo) => async (...args) => (await pronto).api[g][metodo](...args),
  })]))
}

export const api = MODO_DEMO ? criarApiDemo() : apiHttp
export { sessao, salvarBlob }
