using System.Security.Claims;
using System.Text;
using System.Text.Json.Serialization;
using System.Threading.RateLimiting;
using MedFlow.Api.Seguranca;
using MedFlow.Application;
using MedFlow.Application.Abstracoes;
using MedFlow.Infrastructure;
using MedFlow.Infrastructure.Persistencia;
using MedFlow.Infrastructure.Seguranca;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);

builder.Services.AddControllers()
    .AddJsonOptions(o => o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));
builder.Services.AddProblemDetails();
builder.Services.AddExceptionHandler<TratadorDeExcecoes>();

// ── RNF03: autenticação JWT ──
var jwt = builder.Configuration.GetSection(JwtOptions.Secao).Get<JwtOptions>() ?? new JwtOptions();
if (Encoding.UTF8.GetByteCount(jwt.Chave) < 32)
    throw new InvalidOperationException("Configure Jwt:Chave com pelo menos 32 caracteres (variável de ambiente Jwt__Chave).");
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(o =>
    {
        o.MapInboundClaims = false;
        o.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwt.Emissor,
            ValidateAudience = true,
            ValidAudience = jwt.Audiencia,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt.Chave)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(1),
            RoleClaimType = ClaimTypes.Role,
            NameClaimType = ClaimTypes.Name,
        };
    });

// ── RN01: controle de acesso por perfil (RBAC) ──
builder.Services.AddAuthorization(o =>
{
    o.AddPolicy(Politicas.Paciente, p => p.RequireRole("Paciente"));
    o.AddPolicy(Politicas.Farmaceutico, p => p.RequireRole("Administrador"));
    o.AddPolicy(Politicas.Equipe, p => p.RequireRole("Administrador", "Funcionario"));
    o.AddPolicy(Politicas.Entregador, p => p.RequireRole("Entregador"));
});

// ── RNF04: CORS restrito às origens configuradas (nada de AllowAnyOrigin) ──
var origens = builder.Configuration.GetSection("Cors:Origens").Get<string[]>() ?? new[] { "http://localhost:5173" };
builder.Services.AddCors(o => o.AddPolicy("Web", p => p.WithOrigins(origens).AllowAnyHeader().AllowAnyMethod()));

// ── RN17 (complemento): limita tentativas de login por IP ──
builder.Services.AddRateLimiter(o =>
{
    o.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    o.AddPolicy("login", contexto => RateLimitPartition.GetFixedWindowLimiter(
        contexto.Connection.RemoteIpAddress?.ToString() ?? "desconhecido",
        _ => new FixedWindowRateLimiterOptions { PermitLimit = 10, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
});

builder.Services.AddHealthChecks().AddCheck<MedFlow.Api.Saude.BancoHealthCheck>("banco", tags: new[] { "ready" });
builder.Services.Configure<ForwardedHeadersOptions>(o =>
{
    o.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    o.KnownNetworks.Clear();
    o.KnownProxies.Clear();
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo { Title = "MedFlow Solutions API", Version = "v2", Description = "API REST do MedFlow — PIM III (UNIP)" });
    var esquema = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "Bearer" },
    };
    c.AddSecurityDefinition("Bearer", esquema);
    c.AddSecurityRequirement(new OpenApiSecurityRequirement { [esquema] = Array.Empty<string>() });
});

var app = builder.Build();

app.UseForwardedHeaders();
app.UseExceptionHandler();

// RNF04 — cabeçalhos de segurança e resposta sem cache para dados autenticados.
app.Use(async (contexto, proximo) =>
{
    contexto.Response.Headers["X-Content-Type-Options"] = "nosniff";
    contexto.Response.Headers["Referrer-Policy"] = "no-referrer";
    contexto.Response.Headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()";
    if (contexto.Request.Path.StartsWithSegments("/api")) contexto.Response.Headers["Cache-Control"] = "no-store";
    await proximo();
});

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}
else
{
    app.UseHsts();
}

// Banco: em Development (ou com Banco:AplicarMigrations=true) as migrações são aplicadas na inicialização;
// os dados de demonstração só entram com Demo:Semear=true ou em Development.
var aplicarMigracoes = app.Environment.IsDevelopment() || app.Configuration.GetValue<bool>("Banco:AplicarMigrations");
var semear = app.Environment.IsDevelopment() || app.Configuration.GetValue<bool>("Demo:Semear");
if (aplicarMigracoes || semear)
{
    using var escopo = app.Services.CreateScope();
    var db = escopo.ServiceProvider.GetRequiredService<AppDbContext>();
    if (semear)
        await DadosIniciais.AplicarAsync(db,
            escopo.ServiceProvider.GetRequiredService<IPasswordHasher>(),
            escopo.ServiceProvider.GetRequiredService<IArquivoStorage>(),
            escopo.ServiceProvider.GetRequiredService<IRelogio>().Agora);
    else
        await db.Database.MigrateAsync();
}

// ── RNF02: HTTPS obrigatório ──
app.UseHttpsRedirection();
app.UseCors("Web");
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.MapHealthChecks("/health/live", new HealthCheckOptions { Predicate = _ => false });
app.MapHealthChecks("/health/ready", new HealthCheckOptions { Predicate = c => c.Tags.Contains("ready") });

app.Run();

public partial class Program { }
