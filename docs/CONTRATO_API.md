# Contrato da API (fonte única para web, Android e testes)

Base local: `http://localhost:5080`. JSON em camelCase; enums como texto (`"Paciente"`, `"EmRota"`...). Autenticação: `Authorization: Bearer <jwt>`. Erros: ProblemDetails (`status`, `title`, `detail`, `errors`).
Perfis: `Paciente`, `Administrador` (farmacêutico RT), `Funcionario`, `Entregador`.
Códigos: 400 validação · 401 sem token · 403 sem permissão · 404 · 409 conflito (estoque, transição, duplicidade) · 422 regra violada · 423 conta bloqueada · 429 limite de login.

## Existentes (como na v2; ver `backend/src/MedFlow.Core/Application/Dtos/Dtos.cs`)
- `POST /api/auth/cadastro` `{nome,email,cpf,senha,endereco,telefone?,aceiteTermos}` → `AuthResponse {token, expiraEm, usuario{id,nome,email,cpfMascarado,perfil,ativo}}`
- `POST /api/auth/login` `{email,senha}` → `AuthResponse`
- `GET /api/auth/eu`
- `POST /api/receitas` (multipart: `arquivo`, `consentimentoSaude`), `GET /api/receitas/minhas`, `GET /api/receitas/pendentes` (Administrador), `GET /api/receitas/{id}/arquivo`, `POST /api/receitas/{id}/recusar {motivo}`
- `GET /api/medicamentos/catalogo` (qualquer usuário autenticado): todos os medicamentos com especificações, sem quantidades de estoque; `permitidoNaBox` e `motivoForaDaBox` dizem o que entra na Box
- `GET /api/medicamentos`, `POST/PUT /api/medicamentos` (equipe), `POST /api/medicamentos/{id}/lotes`, `GET /api/medicamentos/movimentacoes`, `GET /api/medicamentos/alertas`
- `POST /api/pedidos/simular`, `POST /api/pedidos`, `GET /api/pedidos/meus` → `PedidoResumoDto[]`, `GET /api/pedidos/{id}` → `PedidoDto` (tem `historico[]` = linha do tempo), `POST /api/pedidos/{id}/cancelar`, `POST /api/pedidos/{id}/pagamento/aprovar` e `POST /api/pedidos/{id}/pagamento/expirar` (Paciente; só existem na demonstração com `Demo:BotoesDePagamento`, senão 404), `POST /api/pedidos/{id}/finalizar-box` (equipe), `GET /api/pedidos?status=` (equipe)
- `GET /api/painel` (equipe), `GET/POST /api/usuarios`, `PATCH /api/usuarios/{id}/ativo?valor=true|false` (Administrador)
- `GET /api/privacidade/meus-dados`, `GET /api/privacidade/meus-dados/exportar`, `PUT /api/privacidade/meus-dados`, `DELETE /api/privacidade/minha-conta`

## NOVOS / ALTERADOS nesta versão (o backend implementa exatamente isto)

### Entregador: dados mínimos (RN13) — substitui `PedidoDto` nas rotas de entrega
`GET /api/entregas/disponiveis`, `GET /api/entregas/minhas` → `EntregaResumoDto[]`
`POST /api/entregas/{pedidoId}/iniciar` · `POST /api/entregas/{pedidoId}/confirmar {recebedor}` · `POST /api/entregas/{pedidoId}/ocorrencia {motivo}` → `EntregaResumoDto`
```json
{ "pedidoId": 7, "codigo": "BOX-000007", "paciente": "Carlos Mendes", "endereco": "Rua ...", "telefone": "11999990000",
  "totalSaches": 6, "status": "AguardandoEntregador", "tentativa": 0 }
```
O perfil Entregador NÃO recebe medicamentos, lotes nem receita (nem em `GET /api/pedidos/{id}`, que responde 403 para ele).

### Receita: aprovação com conferência (RF07)
`POST /api/receitas/{id}/aprovar`
```json
{ "validaAte": "2027-02-01", "prescritorNome": "Dra. Ana Souza", "prescritorCrm": "123456/SP", "dataEmissao": "2026-11-10",
  "checklistCompleto": true, "itensConferidos": [1, 3] }
```
`itensConferidos` = ids de medicamento marcados "Confere". 422 se checklist incompleto, CRM vazio ou se algum item do pedido ligado à receita não estiver conferido. `ReceitaDto` ganha `prescritorNome`, `prescritorCrm`, `dataEmissao`.

### Conta e privacidade
- `POST /api/auth/alterar-senha` `{senhaAtual, novaSenha}` → 204
- `POST /api/privacidade/revogar` `{tipo: "DadosDeSaude"}` → `MeusDadosDto` (revogar saúde bloqueia novos envios de receita e novos pedidos)
- Páginas públicas do front (sem API): `/termos`, `/privacidade`, `/contato`, `/` (início).

### Catálogo
`MedicamentoDto` e `MedicamentoRequest` ganham `categoria` (`Comum|ControleEspecial|Antimicrobiano|RetencaoDeReceita`) e `termolabil` (bool). Paciente só vê `Comum` e não termolábil.

### Paciente: próxima renovação (RF19)
`GET /api/pedidos/proxima-renovacao` (Paciente) →
```json
{ "temBox": true, "dataEstimadaFimDaBox": "2026-12-20", "diasRestantes": 28, "receitaValidaAte": "2026-12-10", "receitaVenceAntes": true }
```

### Equipe: previsão e relatórios (RF20, RF23)
- `GET /api/previsao-reposicao` → `[{ medicamentoId, nome, consumoMedioMensal, disponivel, nivelMinimo, diasAteMinimo }]` (média móvel de 3 meses das baixas; `diasAteMinimo` nulo se sem consumo)
- `GET /api/relatorios/estoque.csv`, `/lotes-a-vencer.csv`, `/entregas.csv` → `text/csv; charset=utf-8` com BOM e separador `;` (sem CPF nem endereço)

### Saúde
`GET /health/live` (200 sempre) · `GET /health/ready` (200; 503 se o banco falhar)

## Pagamento simulado

Com `Pagamento:Habilitado=true`, a confirmação do pedido reserva o estoque e deixa o pedido em `AguardandoPagamento` por `Pagamento:PrazoMinutos`. `PedidoDto` traz `valorMedicamentos`, `valorEntrega`, `pagamentoPrazoEm` e `pagoEm`; `MedicamentoDto` traz `precoUnitario` (valores fictícios) e `SimulacaoDto` traz `valorMedicamentos`, `taxaEntrega` e `prazoPagamentoMinutos`. Pagar leva o pedido a `EmSeparacao` (receita já aprovada) ou `Pendente` (receita em análise); vencido o prazo, o pedido vai a `Cancelado` e a reserva volta ao estoque. Não há cobrança real nem dado de pagamento.
