# MedFlow Solutions — PIM IV (UNIP, ADS)

Plataforma web e mobile para gestão e entrega rastreável de medicamentos de uso contínuo (Box de 30 dias com sachês por horário).
Projeto acadêmico, organização fictícia, **dados fictícios**. Evolução do PIM III; o código do PIM III continua intacto no repositório original.

| Parte | Tecnologia | Pasta |
|---|---|---|
| API REST | ASP.NET Core 8, EF Core 8 (Pomelo), MySQL 8.4 | `backend/` (Api, Core, Infrastructure, Tests) |
| Web | Vue 3, Vite, Pinia, Vue Router | `frontend/` |
| Mobile | Kotlin, Jetpack Compose, Retrofit, Room, WorkManager | `mobile/` |
| Banco | migrações EF + script SQL único | `database/` |
| Contêineres e CI | Docker, Compose, Caddy, GitHub Actions | `docker-compose*.yml`, `deploy/`, `.github/` |

Contrato da API: `docs/CONTRATO_API.md`. Roadmap e registro do que foi feito: `ROADMAP.md`. Evidências: `docs/evidencias/` e `database/resultado_testes_banco.txt`.

## O que o sistema faz (resumo)
- Quatro perfis: Paciente, Administrador (farmacêutico RT), Funcionário (operador) e Entregador. Perfil vem do token JWT, nunca do navegador.
- Senha com PBKDF2-SHA256 (600 mil iterações) e bloqueio após 5 falhas.
- Receita analisada por humano (prescritor, CRM, checklist e conferência item a item). Só medicamento de categoria Comum e não termolábil entra na Box.
- Estoque por lote com FEFO: **reserva na confirmação** do pedido (tudo ou nada, em transação, com controle de concorrência) e **baixa na finalização** da Box.
- Máquina de estados do pedido com histórico; entregador vê só os dados mínimos para entregar.
- LGPD: consentimento com versão e data, exportar, corrigir, revogar e excluir (anonimização).
- Banco com 14 tabelas, 4 procedimentos, 5 gatilhos, 4 visões e restrições CHECK.
- Previsão de reposição por média móvel de 3 meses e relatórios CSV.

## Como rodar (Windows, PowerShell ou Git Bash)
Pré-requisitos: .NET 8 SDK, Node 22+, Docker Desktop. Para o app: Android Studio (veja `mobile/README.md`).

```bash
# 1) Banco de dados (MySQL 8.4 em contêiner, porta 3307)
docker compose up -d mysql

# 2) API (em Development aplica as migrações e cria os dados de demonstração)
cd backend
export ASPNETCORE_ENVIRONMENT=Development
export ConnectionStrings__MedFlow="server=localhost;port=3307;database=medflow;user=root;password=troque-esta-senha"
export Jwt__Chave="uma-chave-local-com-pelo-menos-32-caracteres"
dotnet run --project src/MedFlow.Api --urls http://localhost:5080
# Swagger: http://localhost:5080/swagger   ·   saúde: /health/live e /health/ready

# 3) Web
cd frontend && npm ci && npm run dev        # http://localhost:5173
npm run dev:demo                            # modo demonstração sem API e sem banco (plano B)
```
No PowerShell use `$env:NOME = "valor"` no lugar de `export`. As contas de demonstração (e a senha delas) estão em
`backend/src/MedFlow.Infrastructure/Persistencia/DadosIniciais.cs` e só existem em Development ou com `Demo__Semear=true`.

## Testes
```bash
cd backend && dotnet test                   # 47 testes sem MySQL
# com MySQL real (inclui a prova de concorrência: 20 pedidos simultâneos sobre o mesmo lote):
export MEDFLOW_TESTE_MYSQL="server=localhost;port=3307;user=root;password=troque-esta-senha"
dotnet test                                 # 48 testes
cd ../frontend && npm test                  # 26 testes
python scripts/e2e_api.py                   # roteiro ponta a ponta contra a API real (banco recém-criado)
```

## Banco de dados
- Migrações: `dotnet ef migrations list --project backend/src/MedFlow.Infrastructure` (ferramenta local: `dotnet tool restore`).
- Script único (DDL + programabilidade): `database/medflow_schema.sql` (`mysql ... medflow < database/medflow_schema.sql` em banco vazio).
- Verificação de procedimentos, gatilhos, restrições e visões: `database/testes_banco.sql` → `database/resultado_testes_banco.txt`.

## Produção / homologação
```bash
cp deploy/.env.example .env      # preencha (nunca versione)
docker compose -f docker-compose.prod.yml up -d --build
```
Caddy emite o HTTPS automaticamente para o `DOMINIO` do `.env`. O banco não publica porta; dados e receitas ficam em volumes.
`Demo__Semear=true` cria contas e dados fictícios (use só em demonstração). Pipeline de CI: `.github/workflows/ci.yml`.

## Limites conhecidos
- O app Android compila (`mobile/gradlew assembleDebug`) e tem 15 testes de unidade; ainda não foi testado em emulador ou celular.
- O suporte do .NET 8 termina em novembro de 2026 (migração para versão LTS atual é trabalho futuro).
- As regras sanitárias foram levantadas em fontes documentais e não substituem parecer de farmacêutico responsável técnico.
