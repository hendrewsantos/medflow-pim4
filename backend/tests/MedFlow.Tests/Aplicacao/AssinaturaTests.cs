using MedFlow.Application.Abstracoes;
using MedFlow.Application.Dtos;
using MedFlow.Application.Excecoes;
using MedFlow.Application.Servicos;
using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Tests;

/// <summary>Assinatura simulada: renovação automática da Box alguns dias antes de acabar.</summary>
public class AssinaturaTests
{
    private const string Endereco = "Rua das Acácias, 120 — São Paulo/SP";
    private static readonly PagamentoOpcoes Pagamento = new() { Habilitado = true, PrazoMinutos = 3, TaxaEntrega = 9.90m };

    private sealed record Cenario(Ambiente Amb, Usuario Carlos, Usuario Operador, Usuario Entregador, Medicamento Losartana)
    {
        public PedidoService Pedidos() => new(Amb.Db, Amb.Relogio, Pagamento);
        public AssinaturaService Assinaturas() => new(Amb.Db, Amb.Relogio, Pedidos());
        public UsuarioLogado Paciente => Ambiente.Logado(Carlos);
    }

    /// <summary>Paciente com uma Box já entregue (receita aprovada por 3 meses) e o relógio no dia da entrega.</summary>
    private static async Task<Cenario> ComBoxEntregueAsync()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var juliana = await amb.CriarUsuarioAsync("Juliana Prado", "juliana@medflow.com.br", "39053344705", PerfilUsuario.Administrador);
        var rafael = await amb.CriarUsuarioAsync("Rafael Souza", "rafael@medflow.com.br", "11144477735", PerfilUsuario.Entregador);
        var losartana = await amb.CriarMedicamentoAsync("Losartana", 2000);
        var c = new Cenario(amb, carlos, juliana, rafael, losartana);

        var receita = await amb.Receitas().EnviarAsync(c.Paciente, Ambiente.PdfValido, "receita.pdf", true);
        await amb.Receitas().AprovarAsync(Ambiente.Logado(juliana), receita.Id,
            new AprovarReceitaRequest(amb.Relogio.Agora.AddMonths(3), "Dra. Ana Souza", "123456/SP", amb.Relogio.Agora.AddDays(-3), true, null));
        var pedido = await c.Pedidos().ConfirmarAsync(c.Paciente,
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00", "20:00" }, 1) }, Endereco));
        await c.Pedidos().AprovarPagamentoAsync(c.Paciente, pedido.Id);
        await EntregarAsync(c, pedido.Id);
        return c;
    }

    private static async Task EntregarAsync(Cenario c, int pedidoId)
    {
        var p = await c.Amb.Db.Pedidos
            .Include(x => x.Itens).ThenInclude(i => i.Alocacoes).ThenInclude(a => a.Lote)
            .Include(x => x.Historico).Include(x => x.Entregas).SingleAsync(x => x.Id == pedidoId);
        var agora = c.Amb.Relogio.Agora;
        p.FinalizarBox(c.Operador.Id, agora);
        p.IniciarRota(c.Entregador.Id, agora);
        p.ConfirmarEntrega(c.Entregador.Id, "Carlos Mendes", agora);
        await c.Amb.Db.SaveChangesAsync();
    }

    [Fact(DisplayName = "CT57 — Ativar e cancelar a assinatura; a data da próxima renovação é o fim da Box menos 5 dias")]
    public async Task AtivarECancelar()
    {
        var c = await ComBoxEntregueAsync();
        var s = c.Assinaturas();
        Assert.False((await s.ObterAsync(c.Paciente)).Ativa);

        var ativa = await s.AtivarAsync(c.Paciente);
        Assert.True(ativa.Ativa);
        Assert.Equal(c.Amb.Relogio.Agora.Date.AddDays(25), ativa.ProximaRenovacaoEm);   // 30 dias de Box, 5 de antecedência

        var cancelada = await s.CancelarAsync(c.Paciente);
        Assert.False(cancelada.Ativa);
        Assert.Null(cancelada.ProximaRenovacaoEm);
        await Assert.ThrowsAsync<AcessoNegadoException>(() => s.ObterAsync(Ambiente.Logado(c.Operador)));
    }

    [Fact(DisplayName = "CT58 — Perto do fim da Box, a assinatura pede a próxima sozinha, paga (simulado) e segue para separação")]
    public async Task RenovaSozinhaPertoDoFim()
    {
        var c = await ComBoxEntregueAsync();
        var s = c.Assinaturas();
        await s.AtivarAsync(c.Paciente);

        Assert.Equal(0, await s.RenovarVencidasAsync());                       // ainda faltam mais de 5 dias
        c.Amb.Relogio.Agora = c.Amb.Relogio.Agora.AddDays(26);
        Assert.Equal(1, await s.RenovarVencidasAsync());

        var novo = await c.Amb.Db.Pedidos.Where(p => p.Status != StatusPedido.Entregue).SingleAsync();
        Assert.Equal(StatusPedido.EmSeparacao, novo.Status);
        Assert.NotNull(novo.PagoEm);
        Assert.Single(novo.Itens);
        Assert.NotNull((await s.ObterAsync(c.Paciente)).UltimaRenovacaoEm);
        Assert.Equal(0, await s.RenovarVencidasAsync());                       // não duplica enquanto há Box em andamento
    }

    [Fact(DisplayName = "CT59 — Sem assinatura ativa nada é renovado; sem receita válida a renovação avisa o motivo")]
    public async Task NaoRenovaSemAssinaturaOuSemReceita()
    {
        var c = await ComBoxEntregueAsync();
        var s = c.Assinaturas();
        c.Amb.Relogio.Agora = c.Amb.Relogio.Agora.AddDays(26);
        Assert.Equal(0, await s.RenovarVencidasAsync());                       // assinatura nunca ativada

        await s.AtivarAsync(c.Paciente);
        c.Amb.Relogio.Agora = c.Amb.Relogio.Agora.AddMonths(4);                // a receita (3 meses) venceu
        var r = await s.RenovarAsync(c.Carlos.Id, forcar: false);
        Assert.False(r.Renovou);
        Assert.Contains("receita", r.Motivo!, StringComparison.OrdinalIgnoreCase);
    }

    [Fact(DisplayName = "CT60 — Renovar agora (demonstração) antecipa a renovação")]
    public async Task RenovarAgoraAntecipa()
    {
        var c = await ComBoxEntregueAsync();
        var s = c.Assinaturas();
        await s.AtivarAsync(c.Paciente);
        Assert.False((await s.RenovarAsync(c.Carlos.Id, forcar: false)).Renovou);
        var forcada = await s.RenovarAsync(c.Carlos.Id, forcar: true);
        Assert.True(forcada.Renovou);
        Assert.False((await s.RenovarAsync(c.Carlos.Id, forcar: true)).Renovou);   // já há Box em andamento
    }
}
