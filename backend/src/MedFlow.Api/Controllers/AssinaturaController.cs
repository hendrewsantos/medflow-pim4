using MedFlow.Api.Seguranca;
using MedFlow.Application.Dtos;
using MedFlow.Application.Servicos;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MedFlow.Api.Controllers;

/// <summary>Assinatura simulada: renovação automática da Box. Sem cobrança real.</summary>
[Route("api/assinatura")]
[Authorize(Policy = Politicas.Paciente)]
public sealed class AssinaturaController : BaseController
{
    private readonly AssinaturaService _assinatura;
    private readonly bool _botoesDeDemonstracao;

    public AssinaturaController(AssinaturaService assinatura, IConfiguration configuracao)
    {
        _assinatura = assinatura;
        _botoesDeDemonstracao = configuracao.GetValue<bool>("Demo:BotoesDePagamento");
    }

    [HttpGet]
    public async Task<ActionResult<AssinaturaDto>> Obter(CancellationToken ct) => Ok(await _assinatura.ObterAsync(Logado, ct));

    [HttpPost("ativar")]
    public async Task<ActionResult<AssinaturaDto>> Ativar(CancellationToken ct) => Ok(await _assinatura.AtivarAsync(Logado, ct));

    [HttpPost("cancelar")]
    public async Task<ActionResult<AssinaturaDto>> Cancelar(CancellationToken ct) => Ok(await _assinatura.CancelarAsync(Logado, ct));

    /// <summary>Antecipa a renovação para mostrar o efeito. Só na demonstração; fora dela responde 404.</summary>
    [HttpPost("renovar-agora")]
    public async Task<ActionResult<RenovacaoDto>> RenovarAgora(CancellationToken ct)
    {
        if (!_botoesDeDemonstracao) return NotFound();
        await _assinatura.ObterAsync(Logado, ct);
        return Ok(await _assinatura.RenovarAsync(Logado.Id, forcar: true, ct));
    }
}
