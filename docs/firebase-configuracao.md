# Firebase Authentication e contagem de votos

Esta configuração habilita o cadastro por CPF, a prevenção de uma segunda votação pela mesma conta e a apuração no Cloud Firestore.

## Como os dados são separados

O aplicativo pede onze algarismos no formato visual `000.000.000-00` (por exemplo, `123.456.789-00`) e deriva no computador credenciais técnicas para usar com o provedor Email/Password do Firebase Authentication. Não calcula dígitos verificadores: pode ser qualquer sequência inventada de onze algarismos. O participante não cria nem digita uma senha. O valor em texto não é enviado nem gravado. O identificador serve somente à simulação educativa e não confirma titularidade.

| Local | Documento | Conteúdo |
|---|---|---|
| Firebase Authentication | Conta técnica | UID, identificador derivado da sequência digitada e credencial protegida pelo Firebase |
| Firestore | `participantes/{uid}` | Apenas `votou: true`; criado uma vez por conta |
| Firestore | `sessoes/{uuid-aleatorio}` | Impressão digital e três votos; ID sem identificador ou UID |
| Firestore | `resultados/{cargo}_{numero}` | Contagem agregada e ID aleatório da última sessão que alterou o total |
| Firestore | `administradores/{uid}` | `ativo: true` para uma conta administrativa separada |
| Computador | `votos-pendentes-{uid}.tsv` | Sessões que ainda aguardam sincronização |

O documento de participação e a sessão são gravados juntos. As regras impedem a criação de outra marca para o mesmo UID e verificam que cada incremento de contador corresponde ao voto na sessão do mesmo lote. Participantes não podem listar nem consultar sessões ou resultados. A equipe responsável consulta os totais no Console Firebase.

## 1. Preparar o Firebase

1. Crie um projeto em [Firebase Console](https://console.firebase.google.com/).
2. Em **Authentication → Sign-in method**, ative **Email/Password**. Não é necessário configurar envio de email: o endereço usado pela aplicação é técnico e não recebe mensagens.
3. Crie o banco Cloud Firestore `(default)`.
4. Em **Firestore Database → Regras**, publique o conteúdo atualizado de [`firestore.rules`](../firestore.rules). Esta versão aceita sessões com três votos e a lista atual de candidatos.

## 2. Configurar a aplicação

Para executar pelo código-fonte, copie `urna.properties.example` para `urna.properties` na raiz do projeto e informe os valores do Firebase. O comando `iniciar.bat` usa essa pasta para encontrar a configuração. No ZIP portátil, o arquivo fica na pasta `UrnaCidada\app`.

```properties
firebase.projectId=ID_DO_PROJETO
firebase.apiKey=CHAVE_WEB_DA_API
```

O app também lê `URNA_FIRESTORE_PROJECT_ID` e `URNA_FIRESTORE_API_KEY`. A chave de API é usada pelo cliente Firebase, não concede acesso administrativo; restrinja-a às APIs necessárias para Firestore, Identity Toolkit e Secure Token. Não adicione `urna.properties` ao Git.

Para montar o pacote de Release com Firebase, configure `FIREBASE_PROJECT_ID` em **Settings → Secrets and variables → Actions → Variables** e `FIREBASE_API_KEY` em **Secrets**. O workflow inclui essa configuração no ZIP somente quando os dois valores existem.

## 3. Criar uma conta administrativa

1. Em **Authentication → Users**, crie uma conta administrativa separada com e-mail e senha. Não use o cadastro CPF dos participantes como conta de administrador.
2. Copie o UID dessa conta.
3. Em **Firestore Database → Data**, crie o documento `administradores/{UID}` com o campo booleano `ativo` igual a `true`.
4. Publique as regras de [`firestore.rules`](../firestore.rules), que impedem os clientes da urna de criar ou alterar documentos de administrador.

No código-fonte, execute `scripts\administrar.bat`. No ZIP do Windows, abra `UrnaCidadaAdmin.exe`. Entre com o e-mail e a senha da conta administrativa. A ferramenta confere o UID no Firestore e só então mostra **Apagar dados salvos**.

A limpeza remove os documentos das coleções `participantes`, `sessoes` e `resultados`, além dos arquivos de voto no computador que executa a ferramenta. Ela mantém `administradores` e logs. Antes de excluir, o administrador confirma a ação e digita `APAGAR`. Os arquivos locais de outros computadores não são removidos; votos que ainda aguardam sincronização podem voltar ao Firestore quando essas máquinas se conectarem.

## 4. Cadastrar e votar

1. Abra a aplicação e escolha **Primeiro acesso**.
2. Informe onze algarismos no formato `000.000.000-00`. Podem ser números inventados; os dígitos verificadores não são calculados. O cadastro já abre a votação e não pede senha.
3. Nos próximos acessos, escolha **Entrar** e informe a mesma sequência de onze algarismos.
4. Depois de confirmar o último cargo, a sessão é gravada localmente e sincronizada. A urna toca o sinal, fala a mensagem de encerramento e retorna à tela de identificação após cinco segundos.

Se a mesma conta já tiver votado, o aplicativo informa isso antes de abrir uma nova sessão. A proteção central depende do Firebase. Os modos `--local` e `--memoria` não identificam participantes entre computadores.

## 5. Conferir a apuração

No Console Firebase, abra o banco Firestore e a coleção `resultados`. Cada documento usa o formato `cargo_numero`, por exemplo `pres_13` ou `df_1736`, e apresenta `contagem`. Um contador é criado com zero quando a primeira votação válida daquele candidato é registrada. Voto de legenda, branco ou nulo não aumenta a contagem de um candidato. As regras bloqueiam a consulta dessa coleção pelos participantes.

IDs de resultado correspondentes às candidaturas atuais:

```text
df_1736  df_5730  df_9462
gov_14  gov_28  gov_37
pres_13  pres_27  pres_39
```

Depois de atualizar o app, publique novamente as regras deste repositório em **Firestore Database → Regras**. A lista acima mostra os IDs desta versão. Documentos antigos no Firestore não são removidos automaticamente.

O envio usa `documents:commit`: a sessão e os incrementos de contagem entram no mesmo lote atômico. Se faltar conexão depois da identificação, a votação permanece no arquivo local e é reenviada quando a conexão voltar. O Firestore aceita somente a primeira participação de cada conta.

## Observação sobre CPF

O número no formato CPF funciona como identificador único do cadastro. O aplicativo não valida dígitos verificadores, não consulta a Receita Federal e não verifica titularidade. A mesma sequência deriva a mesma conta Firebase, que não pode ser cadastrada duas vezes. Este projeto é uma simulação, não uma eleição oficial.
