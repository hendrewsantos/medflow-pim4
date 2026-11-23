# MedFlow Mobile (Android)

Aplicativo Android do MedFlow Solutions (projeto acadêmico UNIP). Dois perfis:

- **Entregador**: listas "Boxes prontas" e "Minhas entregas", detalhe da entrega (ligar, abrir no mapa), iniciar rota, confirmar entrega (nome de quem recebeu) ou registrar ocorrência (motivo).
- **Paciente**: "Meus pedidos", linha do tempo do pedido e "Próxima renovação".

Outros perfis (Administrador, Funcionário) recebem a mensagem "use a versão web". O app só consome a API descrita em `docs/CONTRATO_API.md`.

## Tecnologias

Kotlin, Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow + repositório), Retrofit + OkHttp + kotlinx.serialization, Room (cache de entregas e pedidos + fila offline), WorkManager (reenvio da fila), `EncryptedSharedPreferences` (token JWT protegido pelo Android Keystore). minSdk 26, compileSdk/targetSdk 35, JDK 17+.

## Como abrir

1. Instale o **Android Studio** (versão recente, por exemplo Ladybug ou superior). Ele instala o Android SDK e o JDK.
2. `File > Open` e escolha a pasta `mobile/`. Aguarde o *Gradle Sync* (baixa o Gradle 8.9 e as dependências; precisa de internet).
3. Crie um emulador em `Device Manager` (por exemplo Pixel com API 34) e clique em **Run**.

Pela linha de comando (depois do primeiro sync, que gera o `gradlew`; se não existir, use o Android Studio ou `gradle wrapper --gradle-version 8.9`):

```
gradlew assembleDebug
gradlew testDebugUnitTest
```

## Como apontar para a API

A URL base vem de `BuildConfig.API_BASE_URL`.

- **Emulador** (padrão no build debug): `http://10.0.2.2:5080/` (o `10.0.2.2` é o computador onde roda o backend).
- **Celular físico na mesma rede**: use o IP do computador, sem editar código:
  `gradlew assembleDebug -Pmedflow.apiUrl=http://192.168.0.10:5080/`
  (ou coloque `medflow.apiUrl=...` em `~/.gradle/gradle.properties`). Para HTTP funcionar nesse IP, adicione-o em `app/src/debug/res/xml/network_security_config.xml`.
- **Release**: `-Pmedflow.apiUrlRelease=https://sua-api/`. Texto claro (HTTP) só é permitido no build **debug**, apenas para `10.0.2.2`, `localhost` e `127.0.0.1`.

Suba o backend antes (`docker compose` ou `dotnet run` na pasta `backend/`, porta 5080).

## Usuário de teste

Peça ao coordenador do projeto o e-mail e a senha de um entregador e de um paciente de teste. Nenhuma credencial é guardada neste repositório.

## Como funciona a fila offline

- **Confirmar entrega** e **Registrar ocorrência** feitos sem internet são gravados no aparelho (Room) e a tela mostra "Aguardando envio". O WorkManager reenvia quando houver rede, com espera crescente entre tentativas, sempre na ordem em que foram feitos.
- Se a API responder **409** (a entrega já foi alterada), a ação é **descartada** da fila e a lista é atualizada; o app mostra um aviso (enquanto estiver aberto).
- **Iniciar rota** exige internet (não entra na fila).
- Não é possível sair da conta enquanto houver ações aguardando envio (para não perdê-las).
- Limitação: depois de uma queda de rede no meio do envio, a ação pode chegar duas vezes ao servidor; a segunda recebe 409 e é descartada.

## Acessibilidade

`contentDescription`/semantics nos cartões e botões (TalkBack), alvos de toque de pelo menos 48dp, textos em `sp` (respeitam o tamanho de fonte do sistema), cores com contraste adequado (claro e escuro), mensagens anunciadas como *live region* e linguagem simples.

## Estrutura

```
app/src/main/java/br/com/medflow/app/
  data/    DTOs, API (Retrofit), Room, token criptografado, repositórios, fila offline
  ui/      ViewModels, telas Compose, navegação
  work/    SyncWorker + agendador (WorkManager)
app/src/test/   testes de unidade (fila offline, ViewModel de login, regras)
```

## Limites conhecidos

- Não há envio de receita nem criação de pedido no app (use a versão web).
- Datas sem fuso vindas da API são exibidas como recebidas.
- Ícone do app é o padrão do sistema.
