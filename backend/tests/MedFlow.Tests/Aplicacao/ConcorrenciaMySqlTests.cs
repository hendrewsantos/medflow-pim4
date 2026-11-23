using MedFlow.Application.Abstracoes;
using MedFlow.Application.Dtos;
using MedFlow.Application.Excecoes;
using MedFlow.Domain.Comum;
using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;
using MedFlow.Infrastructure.Persistencia;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Tests;

/// <summary>Só roda quando MEDFLOW_TESTE_MYSQL aponta para um MySQL 8 (usuário com permissão de criar bancos).</summary>
public sealed class FatoRequerMySqlAttribute : FactAttribute
{
    public const string Variavel = "MEDFLOW_TESTE_MYSQL";
    public FatoRequerMySqlAttribute()
    {
        if (string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable(Variavel)))
            Skip = $"Defina {Variavel} (ex.: server=localhost;port=3307;user=root;password=...) para executar este teste com MySQL real.";
    }
}

public class ConcorrenciaMySqlTests
{
    [FatoRequerMySql(DisplayName = "CT09 / IM04 — 20 confirmações simultâneas sobre o mesmo lote em MySQL real: nunca reserva além do saldo")]
    public async Task VinteConfirmacoesSimultaneas()
    {
        var baseConexao = Environment.GetEnvironmentVariable(FatoRequerMySqlAttribute.Variavel)!;
        var banco = $"medflow_teste_{Guid.NewGuid():N}";
        DbContextOptions<AppDbContext> Opcoes(string db) => new DbContextOptionsBuilder<AppDbContext>()
            .UseMySql($"{baseConexao};database={db}", new MySqlServerVersion(new Version(8, 4, 0))).Options;

        var relogio = new RelogioFixo();
        try
        {
            await using (var admin = new AppDbContext(Opcoes(banco)))
            {
                await admin.Database.MigrateAsync();
                var m = new Medicamento("Losartana", "losartana", "50 mg", "comprimido", Tarja.Vermelha, false, 10);
                m.RegistrarEntrada("LOT-1", relogio.Agora.AddMonths(-1), relogio.Agora.AddMonths(10), 100, relogio.Agora);
                admin.Medicamentos.Add(m);
                for (var i = 0; i < 20; i++)
                {
                    var u = new Usuario($"Paciente {i:D2} Teste", $"p{i}@teste.med", Cpfs[i], "hash", PerfilUsuario.Paciente, "Rua Teste 123 - Sao Paulo", relogio.Agora);
                    u.RegistrarConsentimento(TipoConsentimento.DadosDeSaude, "1", relogio.Agora);
                    admin.Usuarios.Add(u);
                }
                await admin.SaveChangesAsync();
                foreach (var u in await admin.Usuarios.ToListAsync())
                    admin.Receitas.Add(new Receita(u.Id, "x.pdf", "x.pdf", "application/pdf", 10, relogio.Agora));
                await admin.SaveChangesAsync();
            }

            int medId, total;
            List<(int pacienteId, int receitaId)> alvos;
            await using (var leitura = new AppDbContext(Opcoes(banco)))
            {
                medId = (await leitura.Medicamentos.SingleAsync()).Id;
                alvos = (await leitura.Receitas.ToListAsync()).Select(r => (r.PacienteId, r.Id)).ToList();
                total = alvos.Count;
            }

            // Cada paciente pede 30 unidades (1 comprimido por dia × 30 dias); o lote tem 100: no máximo 3 pedidos cabem.
            var resultados = await Task.WhenAll(alvos.Select(async a =>
            {
                await using var db = new AppDbContext(Opcoes(banco));
                var servico = new Servicos.PedidoService(db, relogio);
                try
                {
                    await servico.ConfirmarAsync(new UsuarioLogado(a.pacienteId, PerfilUsuario.Paciente),
                        new ConfirmarPedidoRequest(a.receitaId, new[] { new ItemBoxRequest(medId, new[] { "08:00" }, 1) }, "Rua Teste 123 - Sao Paulo"));
                    return "ok";
                }
                catch (ConflitoException) { return "conflito"; }
                catch (EstoqueInsuficienteException) { return "falta"; }
            }));

            var sucessos = resultados.Count(r => r == "ok");
            await using var verifica = new AppDbContext(Opcoes(banco));
            var lote = await verifica.Lotes.SingleAsync();
            Assert.Equal(20, total);
            Assert.InRange(sucessos, 1, 3);
            Assert.Equal(30 * sucessos, lote.QuantidadeReservada);
            Assert.True(lote.QuantidadeReservada <= lote.QuantidadeFisica);
            Assert.Equal(sucessos, await verifica.Pedidos.CountAsync());
        }
        finally
        {
            await using var limpeza = new AppDbContext(Opcoes("mysql"));
            await limpeza.Database.ExecuteSqlRawAsync($"DROP DATABASE IF EXISTS `{banco}`");
        }
    }

    private static readonly string[] Cpfs =
    {
        "52998224725", "39053344705", "86288366757", "11144477735", "24763598155", "93541134780", "71428793860", "12345678909", "98765432100", "45317828791",
        "10000000019", "20000000108", "30000000299", "40000000396", "50000000485", "60000000582", "70000000671", "80000000760", "90000000850", "11111111111",
    };
}
