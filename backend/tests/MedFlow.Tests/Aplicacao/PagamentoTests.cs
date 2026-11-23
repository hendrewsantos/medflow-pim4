using MedFlow.Application.Dtos;
using MedFlow.Application.Excecoes;
using MedFlow.Application.Servicos;
using MedFlow.Domain.Comum;
using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Tests;

/// <summary>Pagamento simulado: estado AguardandoPagamento, prazo, aprovação e expiração com devolução da reserva.</summary>
public class PagamentoTests
{
    private const string Endereco = "Rua das Acácias, 120 — São Paulo/SP";
    private static readonly PagamentoOpcoes Opcoes = new() { Habilitado = true, PrazoMinutos = 3, TaxaEntrega = 9.90m };

    private static PedidoService Servico(Ambiente amb) => new(amb.Db, amb.Relogio, Opcoes);

    private static async Task<(Ambiente amb, Usuario carlos, Usuario juliana, Medicamento losartana, ReceitaDto receita, PedidoDto pedido)> CenarioAsync(bool receitaAprovada = false)
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var juliana = await amb.CriarUsuarioAsync("Juliana Prado", "juliana@medflow.com.br", "39053344705", PerfilUsuario.Administrador);
        var losartana = await amb.CriarMedicamentoAsync("Losartana", 500);
        losartana.DefinirPreco(0.50m);
        await amb.Db.SaveChangesAsync();
        var paciente = Ambiente.Logado(carlos);
        var receita = await amb.Receitas().EnviarAsync(paciente, Ambiente.PdfValido, "receita.pdf", true);
        if (receitaAprovada)
            receita = await amb.Receitas().AprovarAsync(Ambiente.Logado(juliana), receita.Id,
                new AprovarReceitaRequest(amb.Relogio.Agora.AddMonths(3), "Dra. Ana Souza", "123456/SP", amb.Relogio.Agora.AddDays(-3), true, null));
        var pedido = await Servico(amb).ConfirmarAsync(paciente,
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00", "20:00" }, 1) }, Endereco));
        return (amb, carlos, juliana, losartana, receita, pedido);
    }

    [Fact(DisplayName = "CT50 — Com pagamento habilitado, a confirmação reserva o estoque, calcula o valor e espera o pagamento")]
    public async Task ConfirmacaoAguardaPagamento()
    {
        var (amb, _, _, losartana, _, pedido) = await CenarioAsync(receitaAprovada: true);
        Assert.Equal(StatusPedido.AguardandoPagamento, pedido.Status);
        Assert.Equal(60 * 0.50m, pedido.ValorMedicamentos);   // 2 horários x 30 dias x R$ 0,50
        Assert.Equal(9.90m, pedido.ValorEntrega);
        Assert.Equal(amb.Relogio.Agora.AddMinutes(3), pedido.PagamentoPrazoEm);
        Assert.Equal(60, (await amb.Db.Medicamentos.Include(m => m.Lotes).SingleAsync(m => m.Id == losartana.Id)).QuantidadeReservada);
    }

    [Fact(DisplayName = "CT51 — Pagar com receita já aprovada leva direto à separação; com receita em análise, ao estado Pendente")]
    public async Task PagamentoSegueConformeReceita()
    {
        var (amb, carlos, _, _, _, pedido) = await CenarioAsync(receitaAprovada: true);
        var pago = await Servico(amb).AprovarPagamentoAsync(Ambiente.Logado(carlos), pedido.Id);
        Assert.Equal(StatusPedido.EmSeparacao, pago.Status);
        Assert.NotNull(pago.PagoEm);

        var (amb2, carlos2, _, _, _, pedido2) = await CenarioAsync(receitaAprovada: false);
        Assert.Equal(StatusPedido.Pendente, (await Servico(amb2).AprovarPagamentoAsync(Ambiente.Logado(carlos2), pedido2.Id)).Status);
    }

    [Fact(DisplayName = "CT52 — Prazo vencido: o pedido é cancelado e a reserva volta ao estoque; pagar depois não é possível")]
    public async Task PrazoVencidoLiberaReserva()
    {
        var (amb, carlos, _, losartana, _, pedido) = await CenarioAsync();
        amb.Relogio.Agora = amb.Relogio.Agora.AddMinutes(4);
        var atual = await Servico(amb).ObterAsync(Ambiente.Logado(carlos), pedido.Id);   // leitura já expira o vencido
        Assert.Equal(StatusPedido.Cancelado, atual.Status);
        Assert.Equal(0, (await amb.Db.Medicamentos.Include(m => m.Lotes).SingleAsync(m => m.Id == losartana.Id)).QuantidadeReservada);
        await Assert.ThrowsAsync<TransicaoInvalidaException>(() => Servico(amb).AprovarPagamentoAsync(Ambiente.Logado(carlos), pedido.Id));
    }

    [Fact(DisplayName = "CT53 — Expiração simulada cancela na hora, só pelo titular, e registra no histórico")]
    public async Task ExpiracaoSimulada()
    {
        var (amb, carlos, juliana, losartana, _, pedido) = await CenarioAsync();
        await Assert.ThrowsAsync<AcessoNegadoException>(() => Servico(amb).SimularExpiracaoAsync(Ambiente.Logado(juliana), pedido.Id));
        var cancelado = await Servico(amb).SimularExpiracaoAsync(Ambiente.Logado(carlos), pedido.Id);
        Assert.Equal(StatusPedido.Cancelado, cancelado.Status);
        Assert.Contains(cancelado.Historico, h => h.Para == StatusPedido.Cancelado && h.Observacao!.Contains("simulada"));
        Assert.Equal(0, (await amb.Db.Medicamentos.Include(m => m.Lotes).SingleAsync(m => m.Id == losartana.Id)).QuantidadeReservada);
    }

    [Fact(DisplayName = "CT54 — Receita recusada enquanto o pedido espera pagamento recusa o pedido e libera a reserva")]
    public async Task RecusaDaReceitaDuranteEspera()
    {
        var (amb, carlos, juliana, losartana, receita, pedido) = await CenarioAsync();
        await amb.Receitas().RecusarAsync(Ambiente.Logado(juliana), receita.Id, new RecusarReceitaRequest("Receita ilegível, envie uma foto mais nítida."));
        Assert.Equal(StatusPedido.Recusado, (await Servico(amb).ObterAsync(Ambiente.Logado(carlos), pedido.Id)).Status);
        Assert.Equal(0, (await amb.Db.Medicamentos.Include(m => m.Lotes).SingleAsync(m => m.Id == losartana.Id)).QuantidadeReservada);
    }

    [Fact(DisplayName = "CT55 — Paciente com pedido aguardando pagamento não abre outra Box (RN12); outro paciente não paga o pedido")]
    public async Task UmaBoxPorVezEApenasOTitularPaga()
    {
        var (amb, carlos, _, losartana, receita, _) = await CenarioAsync();
        await Assert.ThrowsAsync<ConflitoException>(() => Servico(amb).ConfirmarAsync(Ambiente.Logado(carlos),
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00" }, 1) }, Endereco)));
        var intruso = await amb.CriarUsuarioAsync("Outra Pessoa", "outra@email.com", "11144477735", PerfilUsuario.Paciente);
        var pedido = await amb.Db.Pedidos.SingleAsync();
        await Assert.ThrowsAsync<OperacaoNaoAutorizadaException>(() => Servico(amb).AprovarPagamentoAsync(Ambiente.Logado(intruso), pedido.Id));
    }

    [Fact(DisplayName = "CT56 — Com pagamento desligado (padrão) o fluxo anterior continua igual")]
    public async Task PagamentoDesligadoMantemFluxo()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var losartana = await amb.CriarMedicamentoAsync("Losartana", 500);
        var receita = await amb.Receitas().EnviarAsync(Ambiente.Logado(carlos), Ambiente.PdfValido, "receita.pdf", true);
        var pedido = await amb.Pedidos().ConfirmarAsync(Ambiente.Logado(carlos),
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00" }, 1) }, Endereco));
        Assert.Equal(StatusPedido.Pendente, pedido.Status);
        Assert.Equal(0, pedido.ValorMedicamentos);
        Assert.Null(pedido.PagamentoPrazoEm);
    }
}
