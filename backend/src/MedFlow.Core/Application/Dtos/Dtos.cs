using MedFlow.Domain.Enums;

namespace MedFlow.Application.Dtos;

// ── Autenticação e conta ──
public sealed record CadastroPacienteRequest(string Nome, string Email, string Cpf, string Senha, string Endereco, string? Telefone, bool AceiteTermos);
public sealed record LoginRequest(string Email, string Senha);
public sealed record UsuarioResumo(int Id, string Nome, string Email, string CpfMascarado, PerfilUsuario Perfil, bool Ativo);
public sealed record AuthResponse(string Token, DateTime ExpiraEm, UsuarioResumo Usuario);
public sealed record CriarUsuarioInternoRequest(string Nome, string Email, string Cpf, string Senha, PerfilUsuario Perfil);

// ── LGPD (art. 18) ──
public sealed record AtualizarMeusDadosRequest(string Nome, string Email, string? Endereco, string? Telefone);
public sealed record ConsentimentoDto(TipoConsentimento Tipo, string VersaoTermo, DateTime ConcedidoEm, DateTime? RevogadoEm);
public sealed record MeusDadosDto(
    int Id, string Nome, string Email, string Cpf, string? Endereco, string? Telefone, DateTime CriadoEm,
    IReadOnlyList<ConsentimentoDto> Consentimentos,
    IReadOnlyList<ReceitaDto> Receitas,
    IReadOnlyList<PedidoResumoDto> Pedidos);

// ── Receitas ──
public sealed record ReceitaDto(int Id, int PacienteId, string? Paciente, string NomeOriginal, string TipoConteudo, long TamanhoBytes,
    DateTime EnviadaEm, StatusReceita Status, DateTime? ValidaAte, string? MotivoRecusa, DateTime? AnalisadaEm,
    string? PrescritorNome = null, string? PrescritorCrm = null, DateTime? DataEmissao = null);
/// <summary>RF07: ItensConferidos = ids de medicamento marcados "Confere"; todos os itens dos pedidos ligados à receita precisam estar conferidos.</summary>
public sealed record AprovarReceitaRequest(DateTime ValidaAte, string? PrescritorNome = null, string? PrescritorCrm = null, DateTime? DataEmissao = null,
    bool ChecklistCompleto = false, IReadOnlyList<int>? ItensConferidos = null);
public sealed record RecusarReceitaRequest(string Motivo);

// ── Catálogo e estoque ──
public sealed record MedicamentoRequest(string Nome, string PrincipioAtivo, string Concentracao, string FormaFarmaceutica, Tarja Tarja, bool ControleEspecial, int NivelMinimo,
    CategoriaRegulatoria Categoria = CategoriaRegulatoria.Comum, bool Termolabil = false);
public sealed record LoteDto(int Id, string Numero, DateTime Fabricacao, DateTime Validade, int QuantidadeFisica, int QuantidadeReservada, int Disponivel, bool Vencido);
public sealed record MedicamentoDto(int Id, string Nome, string PrincipioAtivo, string Concentracao, string FormaFarmaceutica, Tarja Tarja,
    bool ControleEspecial, int NivelMinimo, int QuantidadeFisica, int QuantidadeReservada, int DisponivelParaCiclo, bool AbaixoDoMinimo,
    IReadOnlyList<LoteDto> Lotes, CategoriaRegulatoria Categoria = CategoriaRegulatoria.Comum, bool Termolabil = false, decimal PrecoUnitario = 0);
public sealed record EntradaLoteRequest(string Numero, DateTime Fabricacao, DateTime Validade, int Quantidade);
public sealed record MovimentacaoDto(int Id, string Medicamento, string Lote, TipoMovimentacao Tipo, int Quantidade, int? PedidoId, DateTime Em, string Observacao);
public sealed record AlertaDto(string Tipo, string Medicamento, string Mensagem, string Severidade);

// ── Box e pedidos ──
public sealed record ItemBoxRequest(int MedicamentoId, IReadOnlyList<string> Horarios, int DosePorHorario);
public sealed record ConfirmarPedidoRequest(int ReceitaId, IReadOnlyList<ItemBoxRequest> Itens, string EnderecoEntrega);
public sealed record DisponibilidadeItemDto(int MedicamentoId, string Medicamento, int Necessario, int Disponivel, bool Atende);
public sealed record SimulacaoDto(bool PodeConfirmar, IReadOnlyList<DisponibilidadeItemDto> Itens, decimal ValorMedicamentos = 0, decimal TaxaEntrega = 0, int PrazoPagamentoMinutos = 0);
public sealed record AlocacaoDto(string Lote, DateTime Validade, int Quantidade);
public sealed record PedidoItemDto(int MedicamentoId, string Medicamento, int Quantidade, IReadOnlyList<string> Horarios, int DosePorHorario, IReadOnlyList<AlocacaoDto> Lotes);
public sealed record HistoricoDto(StatusPedido? De, StatusPedido Para, DateTime Em, string? Observacao);
public sealed record EntregaDto(int Tentativa, int EntregadorId, DateTime IniciadaEm, DateTime? ConcluidaEm, ResultadoEntrega Resultado, string? Recebedor, string? Ocorrencia);
public sealed record PedidoResumoDto(int Id, string Paciente, StatusPedido Status, DateTime CriadoEm, DateTime AtualizadoEm, int TotalItens, decimal ValorTotal = 0, DateTime? PagamentoPrazoEm = null);
public sealed record PedidoDto(int Id, int PacienteId, string Paciente, int ReceitaId, StatusPedido Status, string EnderecoEntrega, int DiasCiclo,
    DateTime CriadoEm, DateTime AtualizadoEm, IReadOnlyList<PedidoItemDto> Itens, IReadOnlyList<HistoricoDto> Historico, IReadOnlyList<EntregaDto> Entregas,
    decimal ValorMedicamentos = 0, decimal ValorEntrega = 0, DateTime? PagamentoPrazoEm = null, DateTime? PagoEm = null);
/// <summary>RN13 — o entregador recebe apenas o necessário para entregar (sem medicamentos, lotes ou receita).</summary>
public sealed record EntregaResumoDto(int PedidoId, string Codigo, string Paciente, string Endereco, string? Telefone, int TotalSaches, StatusPedido Status, int Tentativa);
public sealed record ConfirmarEntregaRequest(string Recebedor);
public sealed record OcorrenciaRequest(string Motivo);

// ── Conta, privacidade, renovação e previsão ──
public sealed record AlterarSenhaRequest(string SenhaAtual, string NovaSenha);
public sealed record RevogarConsentimentoRequest(TipoConsentimento Tipo);
public sealed record ProximaRenovacaoDto(bool TemBox, DateTime? DataEstimadaFimDaBox, int? DiasRestantes, DateTime? ReceitaValidaAte, bool ReceitaVenceAntes);
public sealed record PrevisaoReposicaoDto(int MedicamentoId, string Nome, double ConsumoMedioMensal, int Disponivel, int NivelMinimo, int? DiasAteMinimo);

// ── Painel ──
public sealed record IndicadoresDto(
    IReadOnlyDictionary<StatusPedido, int> PedidosPorStatus,
    int ReceitasPendentes,
    int MedicamentosAbaixoDoMinimo,
    int LotesVencendo30Dias,
    int LotesVencidos,
    double? TempoCicloP90Horas,
    double? PercentualPrimeiraTentativa,
    int PedidosParadosEmSeparacao);
