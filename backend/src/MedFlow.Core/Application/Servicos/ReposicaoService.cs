using System.Globalization;
using System.Text;
using MedFlow.Application.Abstracoes;
using MedFlow.Application.Dtos;
using MedFlow.Application.Excecoes;
using MedFlow.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Servicos;

/// <summary>
/// RF20 e RF23 — previsão de reposição por média móvel de três meses sobre as baixas de estoque
/// (técnica simples, escolhida pelo princípio "comece de onde você está") e relatórios em CSV.
/// </summary>
public sealed class ReposicaoService
{
    public const int JanelaDias = 90;
    private readonly IAppDbContext _db;
    private readonly IRelogio _relogio;

    public ReposicaoService(IAppDbContext db, IRelogio relogio)
    {
        _db = db;
        _relogio = relogio;
    }

    /// <summary>Consumo médio mensal = baixas dos últimos 90 dias ÷ 3. Dias até o mínimo = (disponível − mínimo) ÷ consumo diário.</summary>
    public async Task<IReadOnlyList<PrevisaoReposicaoDto>> PrevisaoAsync(UsuarioLogado u, CancellationToken ct = default)
    {
        if (!u.EhEquipe) throw new AcessoNegadoException();
        var agora = _relogio.Agora;
        var desde = agora.AddDays(-JanelaDias);
        var medicamentos = await _db.Medicamentos.Include(m => m.Lotes).Where(m => m.Ativo).OrderBy(m => m.Nome).ToListAsync(ct);
        var baixas = await _db.Movimentacoes.Where(m => m.Tipo == TipoMovimentacao.Baixa && m.Em >= desde)
            .Select(m => new { m.Lote!.MedicamentoId, m.Quantidade }).ToListAsync(ct);

        return medicamentos.Select(m =>
        {
            var mensal = baixas.Where(b => b.MedicamentoId == m.Id).Sum(b => b.Quantidade) / 3.0;
            var disponivel = m.DisponivelAte(agora.Date);
            int? dias = mensal > 0 ? Math.Max(0, (int)Math.Floor((disponivel - m.NivelMinimo) / (mensal / 30.0))) : null;
            return new PrevisaoReposicaoDto(m.Id, m.Nome, Math.Round(mensal, 1), disponivel, m.NivelMinimo, dias);
        }).ToList();
    }

    public async Task<byte[]> EstoqueCsvAsync(UsuarioLogado u, CancellationToken ct = default)
    {
        if (!u.EhEquipe) throw new AcessoNegadoException();
        var hoje = _relogio.Agora.Date;
        var itens = await _db.Medicamentos.Include(m => m.Lotes).Where(m => m.Ativo).OrderBy(m => m.Nome).ToListAsync(ct);
        return Csv(new[] { "Medicamento", "Concentracao", "Categoria", "Fisico", "Reservado", "Disponivel", "Minimo", "AbaixoDoMinimo" },
            itens.Select(m => new[] { m.Nome, m.Concentracao, m.Categoria.ToString(), m.QuantidadeFisica.ToString(), m.QuantidadeReservada.ToString(),
                m.DisponivelAte(hoje).ToString(), m.NivelMinimo.ToString(), m.AbaixoDoMinimo(hoje) ? "Sim" : "Nao" }));
    }

    public async Task<byte[]> LotesAVencerCsvAsync(UsuarioLogado u, CancellationToken ct = default)
    {
        if (!u.EhEquipe) throw new AcessoNegadoException();
        var hoje = _relogio.Agora.Date;
        var lotes = await _db.Lotes.Include(l => l.Medicamento).Where(l => l.QuantidadeFisica > 0).ToListAsync(ct);
        var a_vencer = lotes.Where(l => l.VenceEmAte(hoje, CatalogoService.DiasAlertaValidade) || l.Vencido(hoje)).OrderBy(l => l.Validade);
        return Csv(new[] { "Medicamento", "Lote", "Validade", "DiasParaVencer", "QuantidadeFisica" },
            a_vencer.Select(l => new[] { l.Medicamento?.Nome ?? "", l.Numero, l.Validade.ToString("yyyy-MM-dd"), ((int)(l.Validade.Date - hoje).TotalDays).ToString(), l.QuantidadeFisica.ToString() }));
    }

    /// <summary>Sem CPF, nome ou endereço do paciente (minimização).</summary>
    public async Task<byte[]> EntregasCsvAsync(UsuarioLogado u, CancellationToken ct = default)
    {
        if (!u.EhEquipe) throw new AcessoNegadoException();
        var pedidos = await _db.Pedidos.Include(p => p.Entregas).OrderBy(p => p.Id).ToListAsync(ct);
        return Csv(new[] { "Pedido", "Status", "Tentativas", "CriadoEm", "AtualizadoEm" },
            pedidos.Select(p => new[] { $"BOX-{p.Id:D6}", p.Status.ToString(), p.Entregas.Count.ToString(),
                p.CriadoEm.ToString("yyyy-MM-dd HH:mm", CultureInfo.InvariantCulture), p.AtualizadoEm.ToString("yyyy-MM-dd HH:mm", CultureInfo.InvariantCulture) }));
    }

    /// <summary>UTF-8 com BOM e separador ';' — abre direto no Excel em português.</summary>
    private static byte[] Csv(string[] cabecalho, IEnumerable<string[]> linhas)
    {
        static string Esc(string v) => v.Contains(';') || v.Contains('"') || v.Contains('\n') ? "\"" + v.Replace("\"", "\"\"") + "\"" : v;
        var sb = new StringBuilder();
        sb.AppendLine(string.Join(';', cabecalho.Select(Esc)));
        foreach (var l in linhas) sb.AppendLine(string.Join(';', l.Select(Esc)));
        return Encoding.UTF8.GetPreamble().Concat(Encoding.UTF8.GetBytes(sb.ToString())).ToArray();
    }
}
