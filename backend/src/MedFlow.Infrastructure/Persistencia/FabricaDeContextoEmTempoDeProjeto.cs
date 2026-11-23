using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace MedFlow.Infrastructure.Persistencia;

/// <summary>Usada só pelo "dotnet ef" para gerar migrações sem precisar subir a API.</summary>
public sealed class FabricaDeContextoEmTempoDeProjeto : IDesignTimeDbContextFactory<AppDbContext>
{
    public AppDbContext CreateDbContext(string[] args)
    {
        var conexao = Environment.GetEnvironmentVariable("ConnectionStrings__MedFlow")
            ?? "server=localhost;port=3307;database=medflow;user=root;password=nao-usado-na-geracao";
        var opcoes = new DbContextOptionsBuilder<AppDbContext>()
            .UseMySql(conexao, new MySqlServerVersion(new Version(8, 4, 0)))
            .Options;
        return new AppDbContext(opcoes);
    }
}
