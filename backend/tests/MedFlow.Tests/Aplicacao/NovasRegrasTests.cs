using MedFlow.Application.Dtos;
using MedFlow.Application.Excecoes;
using MedFlow.Domain.Comum;
using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Tests;

/// <summary>Regras do PIM IV: conferência da receita, categoria regulatória, dados mínimos do entregador, conta e privacidade, previsão.</summary>
public class NovasRegrasTests
{
    private const string Endereco = "Rua das Acácias, 120 — São Paulo/SP";

    private static AprovarReceitaRequest Aprovacao(Ambiente amb, IReadOnlyList<int>? itens, bool checklist = true, string crm = "123456/SP") =>
        new(amb.Relogio.Agora.AddMonths(3), "Dra. Ana Souza", crm, amb.Relogio.Agora.AddDays(-3), checklist, itens);

    private static async Task<(Ambiente amb, Usuario carlos, Usuario juliana, Medicamento losartana, ReceitaDto receita, PedidoDto pedido)> CenarioPedidoPendenteAsync()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var juliana = await amb.CriarUsuarioAsync("Juliana Prado", "juliana@medflow.com.br", "39053344705", PerfilUsuario.Administrador);
        var losartana = await amb.CriarMedicamentoAsync("Losartana", 500);
        var paciente = Ambiente.Logado(carlos);
        var receita = await amb.Receitas().EnviarAsync(paciente, Ambiente.PdfValido, "receita.pdf", true);
        var pedido = await amb.Pedidos().ConfirmarAsync(paciente,
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00", "20:00" }, 1) }, Endereco));
        return (amb, carlos, juliana, losartana, receita, pedido);
    }

    [Fact(DisplayName = "CT43 — Aprovação sem checklist completo, sem CRM ou com data futura é recusada e nada muda")]
    public async Task AprovacaoExigeChecklistECrm()
    {
        var (amb, _, juliana, losartana, receita, pedido) = await CenarioPedidoPendenteAsync();
        var far = Ambiente.Logado(juliana);
        await Assert.ThrowsAsync<DomainException>(() => amb.Receitas().AprovarAsync(far, receita.Id, Aprovacao(amb, new[] { losartana.Id }, checklist: false)));
        await Assert.ThrowsAsync<DomainException>(() => amb.Receitas().AprovarAsync(far, receita.Id, Aprovacao(amb, new[] { losartana.Id }, crm: "")));
        await Assert.ThrowsAsync<DomainException>(() => amb.Receitas().AprovarAsync(far, receita.Id,
            Aprovacao(amb, new[] { losartana.Id }) with { DataEmissao = amb.Relogio.Agora.AddDays(2) }));
        Assert.Equal(StatusReceita.Pendente, (await amb.Db.Receitas.SingleAsync()).Status);
        Assert.Equal(StatusPedido.Pendente, (await amb.Pedidos().ObterAsync(Ambiente.Logado(await amb.Db.Usuarios.FirstAsync(x => x.Perfil == PerfilUsuario.Paciente)), pedido.Id)).Status);
    }

    [Fact(DisplayName = "CT44 — Item do pedido não conferido bloqueia a aprovação; conferido libera para separação e grava o prescritor")]
    public async Task ConferenciaItemAItem()
    {
        var (amb, carlos, juliana, losartana, receita, pedido) = await CenarioPedidoPendenteAsync();
        var far = Ambiente.Logado(juliana);
        await Assert.ThrowsAsync<DomainException>(() => amb.Receitas().AprovarAsync(far, receita.Id, Aprovacao(amb, Array.Empty<int>())));

        var aprovada = await amb.Receitas().AprovarAsync(far, receita.Id, Aprovacao(amb, new[] { losartana.Id }));
        Assert.Equal("Dra. Ana Souza", aprovada.PrescritorNome);
        Assert.Equal("123456/SP", aprovada.PrescritorCrm);
        Assert.Equal(StatusPedido.EmSeparacao, (await amb.Pedidos().ObterAsync(Ambiente.Logado(carlos), pedido.Id)).Status);
    }

    [Fact(DisplayName = "CT48 — Antimicrobiano, retenção de receita e termolábil não entram na Box nem aparecem ao paciente")]
    public async Task ItensForaDaBoxPorCategoria()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var comum = await amb.CriarMedicamentoAsync("Losartana", 100);
        var antimicrobiano = new Medicamento("Amoxicilina", "amoxicilina", "500 mg", "capsula", Tarja.Vermelha, false, 10, CategoriaRegulatoria.Antimicrobiano);
        var termolabil = new Medicamento("Insulina", "insulina", "100 UI/mL", "caneta", Tarja.Vermelha, false, 10, CategoriaRegulatoria.Comum, termolabil: true);
        foreach (var m in new[] { antimicrobiano, termolabil }) { m.RegistrarEntrada("X-1", amb.Relogio.Agora.AddMonths(-1), amb.Relogio.Agora.AddMonths(10), 100, amb.Relogio.Agora); amb.Db.Medicamentos.Add(m); }
        await amb.Db.SaveChangesAsync();

        var catalogo = await new Servicos.CatalogoService(amb.Db, amb.Relogio).ListarAsync(Ambiente.Logado(carlos));
        Assert.Equal(new[] { "Losartana" }, catalogo.Select(m => m.Nome).ToArray());

        var receita = await amb.Receitas().EnviarAsync(Ambiente.Logado(carlos), Ambiente.PdfValido, "receita.pdf", true);
        foreach (var proibido in new[] { antimicrobiano, termolabil })
            await Assert.ThrowsAsync<DomainException>(() => amb.Pedidos().ConfirmarAsync(Ambiente.Logado(carlos),
                new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(proibido.Id, new[] { "08:00" }, 1) }, Endereco)));
        Assert.True(comum.PermitidoNaBox);
    }

    [Fact(DisplayName = "CT47 — Entregador recebe só dados mínimos e não abre o detalhe do pedido")]
    public async Task EntregadorSoVeDadosMinimos()
    {
        var (amb, carlos, juliana, losartana, receita, pedido) = await CenarioPedidoPendenteAsync();
        var rafael = await amb.CriarUsuarioAsync("Rafael Souza", "rafael@medflow.com.br", "86288366757", PerfilUsuario.Entregador);
        await amb.Receitas().AprovarAsync(Ambiente.Logado(juliana), receita.Id, Aprovacao(amb, new[] { losartana.Id }));
        await amb.Pedidos().FinalizarBoxAsync(Ambiente.Logado(juliana), pedido.Id);

        var entregas = new Servicos.EntregaService(amb.Db, amb.Relogio);
        var lista = await entregas.ListarDisponiveisAsync(Ambiente.Logado(rafael));
        var item = Assert.Single(lista);
        Assert.Equal("BOX-" + pedido.Id.ToString("D6"), item.Codigo);
        Assert.Equal("Carlos Mendes", item.Paciente);
        Assert.Equal(2, item.TotalSaches);
        // O tipo EntregaResumoDto não tem propriedades de medicamento, lote ou receita:
        Assert.DoesNotContain(typeof(EntregaResumoDto).GetProperties(), p => p.Name.Contains("Medicamento") || p.Name.Contains("Lote") || p.Name.Contains("Receita") || p.Name == "Itens");
        await Assert.ThrowsAsync<AcessoNegadoException>(() => amb.Pedidos().ObterAsync(Ambiente.Logado(rafael), pedido.Id));
    }

    [Fact(DisplayName = "CT42 — Alterar senha exige a senha atual e senha forte; a antiga deixa de valer")]
    public async Task AlterarSenha()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var auth = amb.Auth();
        var u = Ambiente.Logado(carlos);
        await Assert.ThrowsAsync<ValidacaoException>(() => auth.AlterarSenhaAsync(u, new AlterarSenhaRequest("errada1234", "NovaSenha123")));
        await Assert.ThrowsAsync<ValidacaoException>(() => auth.AlterarSenhaAsync(u, new AlterarSenhaRequest("Senha1234", "curta")));
        await auth.AlterarSenhaAsync(u, new AlterarSenhaRequest("Senha1234", "NovaSenha123"));
        await Assert.ThrowsAsync<CredenciaisInvalidasException>(() => auth.LoginAsync(new LoginRequest("carlos@email.com", "Senha1234")));
        Assert.NotNull(await auth.LoginAsync(new LoginRequest("carlos@email.com", "NovaSenha123")));
    }

    [Fact(DisplayName = "CT46 — Revogar o consentimento de saúde bloqueia novos pedidos e fica registrado")]
    public async Task RevogarConsentimento()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        var losartana = await amb.CriarMedicamentoAsync("Losartana", 500);
        var p = Ambiente.Logado(carlos);
        var receita = await amb.Receitas().EnviarAsync(p, Ambiente.PdfValido, "receita.pdf", true);

        var dados = await amb.Privacidade().RevogarAsync(p, new RevogarConsentimentoRequest(TipoConsentimento.DadosDeSaude));
        Assert.All(dados.Consentimentos.Where(c => c.Tipo == TipoConsentimento.DadosDeSaude), c => Assert.NotNull(c.RevogadoEm));
        await Assert.ThrowsAsync<DomainException>(() => amb.Pedidos().ConfirmarAsync(p,
            new ConfirmarPedidoRequest(receita.Id, new[] { new ItemBoxRequest(losartana.Id, new[] { "08:00" }, 1) }, Endereco)));
    }

    [Fact(DisplayName = "RF23 — Previsão por média móvel de 3 meses e relatório CSV sem dados pessoais")]
    public async Task PrevisaoEMediaMovel()
    {
        var (amb, _, juliana, losartana, receita, pedido) = await CenarioPedidoPendenteAsync();
        await amb.Receitas().AprovarAsync(Ambiente.Logado(juliana), receita.Id, Aprovacao(amb, new[] { losartana.Id }));
        await amb.Pedidos().FinalizarBoxAsync(Ambiente.Logado(juliana), pedido.Id); // baixa 60 unidades

        var rep = new Servicos.ReposicaoService(amb.Db, amb.Relogio);
        var previsao = await rep.PrevisaoAsync(Ambiente.Logado(juliana));
        var item = previsao.Single(x => x.MedicamentoId == losartana.Id);
        Assert.Equal(20.0, item.ConsumoMedioMensal);                 // 60 unidades / 3 meses
        Assert.Equal(440, item.Disponivel);                          // 500 - 60
        Assert.Equal((440 - 60) / (20.0 / 30.0), item.DiasAteMinimo!.Value, 0); // (disponível − mínimo) ÷ consumo diário

        var csv = System.Text.Encoding.UTF8.GetString(await rep.EntregasCsvAsync(Ambiente.Logado(juliana)));
        Assert.DoesNotContain("Carlos", csv);
        Assert.DoesNotContain("Acácias", csv);
    }

    [Fact(DisplayName = "RF19 — Próxima renovação avisa quando a receita vence antes do fim da Box")]
    public async Task ProximaRenovacao()
    {
        var (amb, carlos, juliana, losartana, receita, _) = await CenarioPedidoPendenteAsync();
        await amb.Receitas().AprovarAsync(Ambiente.Logado(juliana), receita.Id,
            Aprovacao(amb, new[] { losartana.Id }) with { ValidaAte = amb.Relogio.Agora.AddDays(10) });
        var r = await amb.Pedidos().ProximaRenovacaoAsync(Ambiente.Logado(carlos));
        Assert.True(r.TemBox);
        Assert.Equal(30, r.DiasRestantes);
        Assert.True(r.ReceitaVenceAntes);
    }

    [Fact(DisplayName = "CT61 — Catálogo de consulta: o paciente vê todos os itens com o motivo de os de fora da Box, sem quantidades de estoque")]
    public async Task CatalogoDeConsultaMostraTudoSemEstoque()
    {
        var amb = new Ambiente();
        var carlos = await amb.CriarUsuarioAsync("Carlos Mendes", "carlos@email.com", "52998224725", PerfilUsuario.Paciente);
        await amb.CriarMedicamentoAsync("Losartana", 100);
        var controlado = await amb.CriarMedicamentoAsync("Clonazepam", 50, controlado: true);
        var antimicrobiano = new Medicamento("Amoxicilina", "amoxicilina", "500 mg", "capsula", Tarja.Vermelha, false, 10, CategoriaRegulatoria.Antimicrobiano);
        amb.Db.Medicamentos.Add(antimicrobiano);
        await amb.Db.SaveChangesAsync();

        var itens = await new Servicos.CatalogoService(amb.Db, amb.Relogio).CatalogoAsync(Ambiente.Logado(carlos));
        Assert.Equal(3, itens.Count);
        Assert.True(itens.Single(i => i.Nome == "Losartana").PermitidoNaBox);
        var clona = itens.Single(i => i.Nome == "Clonazepam");
        Assert.False(clona.PermitidoNaBox);
        Assert.Contains("344", clona.MotivoForaDaBox);
        Assert.Contains("Antimicrobiano", itens.Single(i => i.Nome == "Amoxicilina").MotivoForaDaBox);
        Assert.False(itens.Single(i => i.Nome == "Amoxicilina").Disponivel);   // sem lote
        Assert.DoesNotContain(typeof(MedicamentoCatalogoDto).GetProperties(), p => p.Name.Contains("Quantidade"));
    }
}
