using MedFlow.Application.Servicos;

namespace MedFlow.Api.Pagamento;

/// <summary>Renova, a cada 30 segundos, as assinaturas ativas cuja data de renovação chegou (assinatura simulada).</summary>
public sealed class RenovacaoAutomatica : BackgroundService
{
    private readonly IServiceScopeFactory _escopos;
    private readonly ILogger<RenovacaoAutomatica> _log;

    public RenovacaoAutomatica(IServiceScopeFactory escopos, ILogger<RenovacaoAutomatica> log)
    {
        _escopos = escopos;
        _log = log;
    }

    protected override async Task ExecuteAsync(CancellationToken parada)
    {
        using var temporizador = new PeriodicTimer(TimeSpan.FromSeconds(30));
        while (await temporizador.WaitForNextTickAsync(parada))
        {
            try
            {
                using var escopo = _escopos.CreateScope();
                var renovadas = await escopo.ServiceProvider.GetRequiredService<AssinaturaService>().RenovarVencidasAsync(parada);
                if (renovadas > 0) _log.LogInformation("{Quantidade} assinatura(s) renovada(s) automaticamente.", renovadas);
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex) { _log.LogWarning(ex, "Falha ao renovar assinaturas."); }
        }
    }
}
