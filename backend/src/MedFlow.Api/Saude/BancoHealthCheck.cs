using MedFlow.Infrastructure.Persistencia;
using Microsoft.Extensions.Diagnostics.HealthChecks;

namespace MedFlow.Api.Saude;

/// <summary>/health/ready — responde 503 se o banco de dados não responder.</summary>
public sealed class BancoHealthCheck : IHealthCheck
{
    private readonly AppDbContext _db;
    public BancoHealthCheck(AppDbContext db) => _db = db;

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default) =>
        await _db.Database.CanConnectAsync(cancellationToken) ? HealthCheckResult.Healthy("Banco de dados respondendo.") : HealthCheckResult.Unhealthy("Banco de dados indisponível.");
}
