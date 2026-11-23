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

## Complemento (23/11/2026)
- **Acessibilidade automática (axe-core 4.9.1, WCAG 2.0/2.1 A e AA, tema claro):** 15 telas (públicas, paciente, equipe, administrador e entregador). Achados: contraste insuficiente nos cabeçalhos de tabela e no texto secundário sobre a linha selecionada, e `aria-expanded` inválido na linha do estoque. Corrigidos; nova varredura: 0 violações em todas as telas.
- **Receita pela interface:** paciente enviou receita (Em análise) → farmacêutica abriu a análise, preencheu prescritor, CRM, datas e checklist → aprovou → status Aprovada com prescritor e CRM.
- **App Android em emulador (Pixel 6, Android 14):** login do entregador na API real, lista de Boxes, detalhe, iniciar rota, abrir mapa e confirmar entrega com nome de quem recebeu; o pedido chegou a Entregue.

## Complemento (23/11/2026, noite): catálogo, pagamento simulado e página institucional
- **Acessibilidade automática (axe-core 4.9.1, WCAG 2.0/2.1 A e AA):** página institucional (`/site`) nos temas claro, escuro e de alto contraste; início do paciente, montar Box (escolha de medicamentos e revisão), pedido aguardando pagamento (com contagem regressiva) e início com pagamento pendente. Resultado: 0 violações. Uma falha de contraste apareceu uma vez no tema escuro e era só a transição de cor ainda em andamento no instante da leitura; repetida após a transição, deu 0.
- **Pagamento simulado:** confirmar → Aguardando pagamento (estoque reservado, valor e prazo) → aprovar → separação; expirar → cancelado, reserva devolvida. Verificado nos testes de unidade (7 novos), no servidor da demonstração (2 novos), no navegador e no roteiro ponta a ponta contra a API e o MySQL reais (`scripts/e2e_api.py`, 32 verificações). A migração `Pagamento` atualiza também o gatilho do banco que valida as transições do pedido.
- **Catálogo:** 81 medicamentos (72 visíveis ao paciente); antimicrobianos, controle especial e termolábeis continuam fora da Box.
- **Testes automatizados agora:** 54 na API (sem MySQL), 31 no site, 15 no app.
- **Não verificado:** a expiração por vencimento do prazo (tarefa em segundo plano) foi coberta por teste de unidade e pela leitura do pedido, mas não aguardada em tempo real contra o MySQL; o app Android não foi reaberto com o pagamento (o entregador só enxerga pedidos já pagos).
