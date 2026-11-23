using MedFlow.Api.Seguranca;
using MedFlow.Application.Dtos;
using MedFlow.Application.Servicos;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MedFlow.Api.Controllers;

/// <summary>RF20 e RF23 — previsão de reposição (média móvel de 3 meses) e relatórios em CSV para a equipe.</summary>
[Route("api")]
[Authorize(Policy = Politicas.Equipe)]
public sealed class RelatoriosController : BaseController
{
    private readonly ReposicaoService _reposicao;
    public RelatoriosController(ReposicaoService reposicao) => _reposicao = reposicao;

    [HttpGet("previsao-reposicao")]
    public async Task<ActionResult<IReadOnlyList<PrevisaoReposicaoDto>>> Previsao(CancellationToken ct) => Ok(await _reposicao.PrevisaoAsync(Logado, ct));

    [HttpGet("relatorios/estoque.csv")]
    public async Task<IActionResult> Estoque(CancellationToken ct) => File(await _reposicao.EstoqueCsvAsync(Logado, ct), "text/csv; charset=utf-8", "estoque.csv");

    [HttpGet("relatorios/lotes-a-vencer.csv")]
    public async Task<IActionResult> LotesAVencer(CancellationToken ct) => File(await _reposicao.LotesAVencerCsvAsync(Logado, ct), "text/csv; charset=utf-8", "lotes-a-vencer.csv");

    [HttpGet("relatorios/entregas.csv")]
    public async Task<IActionResult> Entregas(CancellationToken ct) => File(await _reposicao.EntregasCsvAsync(Logado, ct), "text/csv; charset=utf-8", "entregas.csv");
}
