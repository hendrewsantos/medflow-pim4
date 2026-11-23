using MedFlow.Application.Servicos;

namespace MedFlow.Api.Pagamento;

/// <summary>Varre os pedidos com pagamento vencido e devolve a reserva ao estoque. Só trabalha com o pagamento simulado habilitado.</summary>
public sealed class ExpiracaoDePagamentos : BackgroundService
{
    private readonly IServiceScopeFactory _escopos;
    private readonly PagamentoOpcoes _opcoes;
    private readonly ILogger<ExpiracaoDePagamentos> _log;

    public ExpiracaoDePagamentos(IServiceScopeFactory escopos, PagamentoOpcoes opcoes, ILogger<ExpiracaoDePagamentos> log)
    {
        _escopos = escopos;
        _opcoes = opcoes;
        _log = log;
    }

    protected override async Task ExecuteAsync(CancellationToken parada)
    {
        if (!_opcoes.Habilitado) return;
        using var temporizador = new PeriodicTimer(TimeSpan.FromSeconds(10));
        while (await temporizador.WaitForNextTickAsync(parada))
        {
            try
            {
                using var escopo = _escopos.CreateScope();
                var expirados = await escopo.ServiceProvider.GetRequiredService<PedidoService>().ExpirarVencidosAsync(parada);
                if (expirados > 0) _log.LogInformation("{Quantidade} pedido(s) cancelado(s) por pagamento não confirmado.", expirados);
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex) { _log.LogWarning(ex, "Falha ao expirar pagamentos vencidos."); }
        }
    }
}
