// Testes ponta a ponta do servidor de demonstração: mesmos fluxos e códigos HTTP da API real.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { criarDemonstracao, SENHA_DEMO } from '../src/services/mock/dados.js'

const pdf = { name: 'receita.pdf', size: 1200, bytes: new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31]) }
const entrarComo = async (s, email) => { await s.api.auth.sair(); return s.api.auth.login(email, SENHA_DEMO) }
const status = (codigo) => (e) => e.status === codigo

test('carga inicial coerente: saldos nunca negativos e reservas batem com pedidos em preparação', async () => {
  const s = await criarDemonstracao()
  const lotes = s.estado.medicamentos.flatMap((m) => m.lotes)
  assert.ok(lotes.every((l) => l.quantidadeFisica >= 0 && l.quantidadeReservada >= 0 && l.quantidadeReservada <= l.quantidadeFisica))
  const reservadoPedidos = s.estado.pedidos.filter((p) => ['Pendente', 'EmSeparacao'].includes(p.status))
    .flatMap((p) => p.itens.flatMap((i) => i.alocacoes)).reduce((a, x) => a + x.quantidade, 0)
  assert.equal(lotes.reduce((a, l) => a + l.quantidadeReservada, 0), reservadoPedidos)
  const contagem = Object.groupBy(s.estado.pedidos, (p) => p.status)
  assert.equal(contagem.Pendente.length, 2)
  assert.equal(contagem.EmSeparacao.length, 1)
  assert.equal(contagem.AguardandoEntregador.length, 1)
  assert.equal(contagem.EmRota.length, 1)
  assert.ok(contagem.Entregue.length >= 6)
})

test('CT05 — senhas da demonstração também ficam em hash com salt', async () => {
  const s = await criarDemonstracao()
  const [a, b] = s.estado.usuarios
  assert.ok(a.senhaHash.startsWith('PBKDF2-SHA256$'))
  assert.notEqual(a.senhaHash, b.senhaHash)
  assert.ok(!JSON.stringify(s.estado.usuarios).includes(SENHA_DEMO))
})

test('CT07/CT08/CT09/CT10 — login, bloqueio e permissões', async () => {
  const s = await criarDemonstracao()
  await assert.rejects(() => s.api.pedidos.meus(), status(401))
  for (let i = 0; i < 4; i++) await assert.rejects(() => s.api.auth.login('carlos@email.com', 'errada'), status(401))
  await assert.rejects(() => s.api.auth.login('carlos@email.com', 'errada'), status(423))
  await assert.rejects(() => s.api.auth.login('carlos@email.com', SENHA_DEMO), status(423))
  await entrarComo(s, 'maria.lopes@email.com')
  await assert.rejects(() => s.api.painel.obter(), status(403))
  await assert.rejects(() => s.api.receitas.pendentes(), status(403))
})

test('CT17/CT22/CT25/CT28/CT29 — renovação do Carlos: reserva, separação direta, baixa e entrega', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  const [receita] = await s.api.receitas.minhas()
  assert.equal(receita.status, 'Aprovada')
  const losartanaAntes = (await s.api.medicamentos.listar()).find((m) => m.nome.startsWith('Losartana'))
  const pedido = await s.api.pedidos.confirmar({
    receitaId: receita.id, enderecoEntrega: 'Rua das Acácias, 120 — São Paulo/SP',
    itens: [{ medicamentoId: losartanaAntes.id, horarios: ['08:00', '20:00'], dosePorHorario: 1 }],
  })
  assert.equal(pedido.status, 'EmSeparacao')
  const losartanaDepois = (await s.api.medicamentos.listar()).find((m) => m.id === losartanaAntes.id)
  assert.equal(losartanaDepois.quantidadeReservada - losartanaAntes.quantidadeReservada, 60)
  assert.equal(losartanaDepois.quantidadeFisica, losartanaAntes.quantidadeFisica)

  await entrarComo(s, 'bruno@medflow.com.br')
  const pronto = await s.api.pedidos.finalizarBox(pedido.id)
  assert.equal(pronto.status, 'AguardandoEntregador')
  assert.ok(pronto.itens[0].lotes.every((l) => l.lote.startsWith('LOS')))

  await entrarComo(s, 'rafael@medflow.com.br')
  await s.api.entregas.iniciar(pedido.id)
  await assert.rejects(() => s.api.entregas.confirmar(pedido.id, ''), status(422))
  const entregue = await s.api.entregas.confirmar(pedido.id, 'Carlos Mendes')
  assert.equal(entregue.status, 'Entregue')
  assert.equal(entregue.codigo, `BOX-${String(pedido.id).padStart(6, '0')}`)
  await entrarComo(s, 'bruno@medflow.com.br')
  const completo = await s.api.pedidos.obter(pedido.id)
  assert.deepEqual(completo.historico.map((h) => h.para), ['Pendente', 'EmSeparacao', 'AguardandoEntregador', 'EmRota', 'Entregue'])
})

test('RN13 — entregador recebe só EntregaResumoDto (sem medicamentos, lotes nem receita) e 403 em /pedidos/{id}', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'rafael@medflow.com.br')
  const lista = [...(await s.api.entregas.disponiveis()), ...(await s.api.entregas.minhas())]
  assert.ok(lista.length >= 2)
  const chaves = ['codigo', 'endereco', 'paciente', 'pedidoId', 'status', 'telefone', 'tentativa', 'totalSaches']
  for (const e of lista) {
    assert.deepEqual(Object.keys(e).sort(), chaves)
    assert.ok(e.totalSaches > 0 && e.telefone)
  }
  await assert.rejects(() => s.api.pedidos.obter(lista[0].pedidoId), status(403))
})

test('RF07 — aprovar exige checklist, CRM e todos os itens do pedido conferidos', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'juliana@medflow.com.br')
  const daMaria = (await s.api.receitas.pendentes()).find((r) => r.paciente.startsWith('Maria'))
  const itens = s.estado.pedidos.find((p) => p.receitaId === daMaria.id).itens.map((i) => i.medicamentoId)
  const base = { validaAte: '2099-01-01', prescritorNome: 'Dr. Paulo Arantes', prescritorCrm: '96115/SP', dataEmissao: '2026-01-10', checklistCompleto: true, itensConferidos: itens }
  await assert.rejects(() => s.api.receitas.aprovar(daMaria.id, { ...base, checklistCompleto: false }), status(422))
  await assert.rejects(() => s.api.receitas.aprovar(daMaria.id, { ...base, prescritorCrm: ' ' }), status(422))
  await assert.rejects(() => s.api.receitas.aprovar(daMaria.id, { ...base, itensConferidos: itens.slice(1) }), status(422))
  const r = await s.api.receitas.aprovar(daMaria.id, base)
  assert.equal(r.status, 'Aprovada')
  assert.equal(r.prescritorCrm, '96115/SP')
  assert.equal(s.estado.pedidos.find((p) => p.receitaId === daMaria.id).status, 'EmSeparacao')
})

test('Catálogo — categoria e termolábil; paciente só vê Comum não termolábil', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'bruno@medflow.com.br')
  const equipe = await s.api.medicamentos.listar()
  assert.equal(equipe.find((m) => m.nome === 'Amoxicilina').categoria, 'Antimicrobiano')
  assert.equal(equipe.find((m) => m.nome === 'Insulina NPH').termolabil, true)
  assert.equal(equipe.find((m) => m.nome === 'Clonazepam').categoria, 'ControleEspecial')
  await entrarComo(s, 'carlos@email.com')
  const paciente = await s.api.medicamentos.listar()
  assert.ok(paciente.length > 0 && paciente.every((m) => m.categoria === 'Comum' && !m.termolabil))
})

test('Conta — alterar senha, revogar saúde (bloqueia receita e pedido) e próxima renovação', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  await assert.rejects(() => s.api.auth.alterarSenha('errada', 'NovaSenha1'), status(422))
  await assert.rejects(() => s.api.auth.alterarSenha(SENHA_DEMO, 'fraca'), status(400))
  await s.api.auth.alterarSenha(SENHA_DEMO, 'NovaSenha1')
  await s.api.auth.login('carlos@email.com', 'NovaSenha1')
  const renov = await s.api.pedidos.proximaRenovacao()
  assert.equal(renov.temBox, true)
  assert.ok(renov.diasRestantes >= 0 && renov.diasRestantes <= 5, `dias restantes: ${renov.diasRestantes}`)
  assert.equal(renov.receitaVenceAntes, false)
  const dados = await s.api.privacidade.revogarSaude()
  assert.ok(dados.consentimentos.some((c) => c.tipo === 'DadosDeSaude' && c.revogadoEm))
  await assert.rejects(() => s.api.receitas.enviar(pdf, true), status(422))
})

test('RF20/RF23 — previsão de reposição e CSV com BOM e separador ponto e vírgula, sem CPF', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'bruno@medflow.com.br')
  const prev = await s.api.previsaoReposicao.listar()
  assert.ok(prev.length > 5 && prev.every((p) => 'consumoMedioMensal' in p && 'diasAteMinimo' in p))
  assert.ok(prev.some((p) => p.consumoMedioMensal > 0) && prev.some((p) => p.diasAteMinimo === null))
  for (const nome of ['estoque', 'lotes-a-vencer', 'entregas']) {
    const csv = await s.api.relatorios.baixar(nome)
    assert.ok(csv.startsWith('\uFEFF') && csv.includes(';'))
    assert.ok(!/\d{3}\.\d{3}\.\d{3}-\d{2}|Rua |Av\. /.test(csv))
  }
  await entrarComo(s, 'carlos@email.com')
  await assert.rejects(() => s.api.relatorios.baixar('estoque'), status(403))
})

test('CT18 — Atenolol 2x ao dia (60 un.) com 40 em estoque: 409 e nada reservado', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  const [receita] = await s.api.receitas.minhas()
  const meds = await s.api.medicamentos.listar()
  const atenolol = meds.find((m) => m.nome === 'Atenolol')
  const losartana = meds.find((m) => m.nome.startsWith('Losartana'))
  const itens = [{ medicamentoId: losartana.id, horarios: ['08:00'], dosePorHorario: 1 }, { medicamentoId: atenolol.id, horarios: ['08:00', '20:00'], dosePorHorario: 1 }]
  const sim = await s.api.pedidos.simular(itens)
  assert.equal(sim.podeConfirmar, false)
  const antes = JSON.stringify(s.estado.medicamentos)
  await assert.rejects(() => s.api.pedidos.confirmar({ receitaId: receita.id, itens, enderecoEntrega: 'Rua das Acácias, 120 — São Paulo/SP' }),
    (e) => e.status === 409 && e.faltas[0].medicamento === 'Atenolol')
  assert.equal(JSON.stringify(s.estado.medicamentos), antes)
  assert.ok(!meds.some((m) => m.controleEspecial), 'paciente não vê controlados no catálogo')
})

test('CT23/CT24 — recusa libera reserva; funcionário não analisa receita', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'bruno@medflow.com.br')
  await assert.rejects(() => s.api.receitas.pendentes(), status(403))
  await entrarComo(s, 'juliana@medflow.com.br')
  const pendentes = await s.api.receitas.pendentes()
  const daMaria = pendentes.find((r) => r.paciente.startsWith('Maria'))
  const reservadoAntes = s.estado.medicamentos.flatMap((m) => m.lotes).reduce((a, l) => a + l.quantidadeReservada, 0)
  await assert.rejects(() => s.api.receitas.recusar(daMaria.id, ''), status(422))
  await s.api.receitas.recusar(daMaria.id, 'Receita sem carimbo do prescritor')
  const reservadoDepois = s.estado.medicamentos.flatMap((m) => m.lotes).reduce((a, l) => a + l.quantidadeReservada, 0)
  assert.equal(reservadoAntes - reservadoDepois, 90)
  assert.equal(s.estado.pedidos.find((p) => p.receitaId === daMaria.id).status, 'Recusado')
})

test('CT12/CT13 — upload rejeita arquivo disfarçado e falta de consentimento', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  await assert.rejects(() => s.api.receitas.enviar({ name: 'virus.pdf', size: 10, bytes: new Uint8Array([0x4d, 0x5a, 0, 0]) }, true), status(400))
  await assert.rejects(() => s.api.receitas.enviar(pdf, false), status(400))
  const r = await s.api.receitas.enviar(pdf, true)
  assert.equal(r.status, 'Pendente')
})

test('CT19 — pico de renovação: o segundo pedido sobre o mesmo saldo é recusado', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'juliana@medflow.com.br')
  const antes = JSON.stringify(s.estado.medicamentos)
  const r = await s.api.demo.simularPico()
  assert.equal(r.passos[0].ok, true)
  assert.equal(r.passos[1].ok, false)
  assert.match(r.passos[1].mensagem, /409/)
  assert.equal(JSON.stringify(s.estado.medicamentos), antes, 'a simulação desfaz a reserva de teste')
})

test('Painel: indicadores de resultado coerentes com a carga', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'juliana@medflow.com.br')
  const p = await s.api.painel.obter()
  assert.equal(p.receitasPendentes, 2)
  assert.equal(p.pedidosParadosEmSeparacao, 1)
  assert.ok(p.tempoCicloP90Horas > 24)
  assert.ok(p.percentualPrimeiraTentativa < 100)
  assert.equal(p.medicamentosAbaixoDoMinimo >= 1, true)
  assert.equal(p.lotesVencidos, 1)
  const alertas = await s.api.medicamentos.alertas()
  assert.ok(alertas.some((a) => a.tipo === 'ValidadeProxima' && a.medicamento === 'Metformina'))
})

test('CT34/CT35 — exportação sem hash; exclusão anonimiza e impede novo login', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  const dados = await s.api.privacidade.meusDados()
  assert.ok(!JSON.stringify(dados).includes('PBKDF2'))
  assert.ok(dados.consentimentos.some((c) => c.tipo === 'DadosDeSaude'))
  await s.api.privacidade.excluirConta()
  await assert.rejects(() => s.api.auth.login('carlos@email.com', SENHA_DEMO), status(401))
  const u = s.estado.usuarios.find((x) => x.nome === 'Titular removido')
  assert.ok(u && u.endereco === null && u.ativo === false)
})

test('CT41 — RN12: uma Box em preparação por paciente', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'carlos@email.com')
  const [receita] = await s.api.receitas.minhas()
  const losartana = (await s.api.medicamentos.listar()).find((m) => m.nome.startsWith('Losartana'))
  const pedido = { receitaId: receita.id, enderecoEntrega: 'Rua das Acácias, 120 — São Paulo/SP', itens: [{ medicamentoId: losartana.id, horarios: ['08:00'], dosePorHorario: 1 }] }
  await s.api.pedidos.confirmar(pedido)
  await assert.rejects(() => s.api.pedidos.confirmar(pedido), status(409))
})

test('CT36 — listagem de usuários: CPF mascarado e nenhum hash de senha', async () => {
  const s = await criarDemonstracao()
  await entrarComo(s, 'juliana@medflow.com.br')
  const lista = await s.api.usuarios.listar()
  assert.ok(lista.every((u) => /^\*\*\*\.\*\*\*\.\d{3}-\d{2}$/.test(u.cpfMascarado) && !('senhaHash' in u) && !('cpf' in u)))
  await entrarComo(s, 'bruno@medflow.com.br')
  await assert.rejects(() => s.api.usuarios.listar(), status(403))
})

test('Pagamento simulado — aguarda o pagamento, aprova com receita aprovada e expira devolvendo a reserva', async () => {
  const s = await criarDemonstracao(new Date(), { pagamento: true })
  const ant = await entrarComo(s, 'carlos@email.com')
  assert.ok(ant)
  const meds = await s.api.medicamentos.listar()
  const los = meds.find((m) => m.nome.startsWith('Losartana'))
  assert.ok(los.precoUnitario > 0)
  const receitas = await s.api.receitas.minhas()
  const aprovada = receitas.find((r) => r.status === 'Aprovada')
  const caixa = { receitaId: aprovada.id, enderecoEntrega: 'Rua das Acácias, 120 — Vila Mariana, São Paulo/SP', itens: [{ medicamentoId: los.id, horarios: ['08:00', '20:00'], dosePorHorario: 1 }] }
  const ped = await s.api.pedidos.confirmar(caixa)
  assert.equal(ped.status, 'AguardandoPagamento')
  assert.equal(ped.valorMedicamentos, Math.round(60 * los.precoUnitario * 100) / 100)
  assert.equal(ped.valorEntrega, 9.9)
  const pago = await s.api.pedidos.aprovarPagamento(ped.id)
  assert.equal(pago.status, 'EmSeparacao')
  assert.ok(pago.pagoEm)
  await assert.rejects(() => s.api.pedidos.aprovarPagamento(ped.id), status(409))
})

test('Pagamento simulado — expiração cancela e libera a reserva; prazo vencido cancela na leitura', async () => {
  const s = await criarDemonstracao(new Date(), { pagamento: true })
  await entrarComo(s, 'carlos@email.com')
  const meds = await s.api.medicamentos.listar()
  const los = meds.find((m) => m.nome.startsWith('Losartana'))
  const reservaAntes = s.estado.medicamentos.find((m) => m.id === los.id).lotes.reduce((a, l) => a + l.quantidadeReservada, 0)
  const aprovada = (await s.api.receitas.minhas()).find((r) => r.status === 'Aprovada')
  const ped = await s.api.pedidos.confirmar({ receitaId: aprovada.id, enderecoEntrega: 'Rua das Acácias, 120 — Vila Mariana, São Paulo/SP', itens: [{ medicamentoId: los.id, horarios: ['08:00'], dosePorHorario: 1 }] })
  const reservaDurante = s.estado.medicamentos.find((m) => m.id === los.id).lotes.reduce((a, l) => a + l.quantidadeReservada, 0)
  assert.equal(reservaDurante, reservaAntes + 30)
  const cancelado = await s.api.pedidos.simularExpiracao(ped.id)
  assert.equal(cancelado.status, 'Cancelado')
  assert.equal(s.estado.medicamentos.find((m) => m.id === los.id).lotes.reduce((a, l) => a + l.quantidadeReservada, 0), reservaAntes)
  // novo pedido; o prazo vence sem ninguém pagar
  const outro = await s.api.pedidos.confirmar({ receitaId: aprovada.id, enderecoEntrega: 'Rua das Acácias, 120 — Vila Mariana, São Paulo/SP', itens: [{ medicamentoId: los.id, horarios: ['08:00'], dosePorHorario: 1 }] })
  s.estado.pedidos.find((p) => p.id === outro.id).pagamentoPrazoEm = new Date(Date.now() - 1000).toISOString()
  assert.equal((await s.api.pedidos.obter(outro.id)).status, 'Cancelado')
})

test('Assinatura simulada — ativar renova sozinha quando a Box está perto do fim; cancelar desliga; não duplica', async () => {
  const s = await criarDemonstracao(new Date(), { pagamento: true })
  await entrarComo(s, 'carlos@email.com')                          // a última Box do Carlos foi entregue há 28 dias
  assert.equal((await s.api.assinatura.obter()).ativa, false)
  const ativa = await s.api.assinatura.ativar()
  assert.equal(ativa.ativa, true)
  const meus = await s.api.pedidos.meus()
  const novo = meus.find((p) => p.status === 'EmSeparacao')       // renovou e pagou (simulado) sozinha
  assert.ok(novo, 'deveria ter criado a próxima Box')
  assert.equal(meus.filter((p) => ['AguardandoPagamento', 'Pendente', 'EmSeparacao'].includes(p.status)).length, 1)
  await s.api.assinatura.ativar()                                  // ativar de novo não duplica
  assert.equal((await s.api.pedidos.meus()).filter((p) => p.status === 'EmSeparacao').length, 1)
  assert.equal((await s.api.assinatura.cancelar()).ativa, false)
})

test('Assinatura simulada — "renovar agora" respeita Box em andamento e só vale para paciente', async () => {
  const s = await criarDemonstracao(new Date(), { pagamento: true })
  await entrarComo(s, 'carlos@email.com')
  await s.api.assinatura.ativar()
  const r = await s.api.assinatura.renovarAgora()
  assert.equal(r.renovou, false)
  assert.match(r.motivo, /em andamento/)
  await entrarComo(s, 'bruno@medflow.com.br')
  await assert.rejects(() => s.api.assinatura.obter(), status(403))
})

test('Catálogo de consulta — o paciente vê todos os itens, com motivo dos que ficam fora da Box e sem quantidades de estoque', async () => {
  const s = await criarDemonstracao(new Date(), { pagamento: true })
  await entrarComo(s, 'carlos@email.com')
  const caixa = await s.api.medicamentos.listar()
  const todos = await s.api.medicamentos.catalogo()
  assert.ok(todos.length > caixa.length, 'o catálogo de consulta inclui os itens que ficam fora da Box')
  const clona = todos.find((m) => m.nome.startsWith('Clonazepam'))
  assert.equal(clona.permitidoNaBox, false)
  assert.match(clona.motivoForaDaBox, /344/)
  assert.ok(todos.filter((m) => m.permitidoNaBox).length === caixa.length)
  assert.ok(todos.every((m) => !('quantidadeFisica' in m) && !('lotes' in m)))
})
