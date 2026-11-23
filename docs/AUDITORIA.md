# Auditoria do sistema — 22/11/2026 (noite)

Feita por execução, sem confiar em texto. Tudo local (Docker + .NET + Vite).

## O que foi executado e o resultado
| Verificação | Resultado |
|---|---|
| Testes do back-end (com MySQL real, inclui 20 pedidos simultâneos no mesmo lote) | 48 de 48 passaram |
| Testes do front-end | 26 de 26 passaram |
| Roteiro ponta a ponta contra a API (`scripts/e2e_api.py`) em banco recém-criado | 27 de 27 verificações |
| `npm audit` (dependências de produção) | 0 vulnerabilidades |
| `dotnet list package --vulnerable` | 3 pacotes com aviso (Caching.Memory, System.Text.Json, MySqlConnector); atualizados, 0 avisos depois |
| CI no GitHub Actions | verde |
| Saúde da API (`/health/ready`) com banco | 200 |
| Menções a ferramenta de IA no repositório e no autor dos commits | nenhuma |

## Percurso real pela interface (navegador, API real, banco recém-criado)
- **Paciente:** Início, Montar Box, Meus pedidos, Receitas, Meus dados. Montou uma Box (Losartana 08:00 e 20:00), revisou e confirmou; o pedido entrou em separação com reserva por lote.
- **Funcionário:** Painel, Pedidos, Estoque. Finalizou a Box (confirmação antes da baixa). Telas de análise de receita e usuários redirecionam para o painel (perfil sem permissão, como deve ser).
- **Entregador:** viu só nome, endereço e quantidade de sachês; iniciou a rota e confirmou a entrega com o nome de quem recebeu; foi para o Histórico.
- **Administrador:** Painel, Análise de receitas, Pedidos, Estoque, Usuários (CPF mascarado).
- Em todo o percurso: nenhuma resposta de erro da API e nenhum erro no console.

## Corrigido nesta auditoria
- Pacotes .NET vulneráveis atualizados.
- Content-Security-Policy implementada (API restrita; interface com exceção para Google Fonts e VLibras).

## Limites que continuam (ditos no documento)
- App Android: compila e tem 15 testes de unidade; ainda não foi aberto em emulador ou celular.
- Procedimentos armazenados existem e são testados no MySQL, mas a API aplica a regra de estoque em C#.
- Upload de receita e análise pela interface não foram refeitos hoje no navegador (cobertos pelos testes e pelo roteiro da API).
- Sem hospedagem pública; a stack de produção foi provada localmente com HTTPS.
- .NET 8 sai de suporte em novembro de 2026.
