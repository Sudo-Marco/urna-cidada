# Firebase Authentication e contagem de votos

Esta configuração habilita o cadastro por CPF, a prevenção de uma segunda votação pela mesma conta e a apuração no Cloud Firestore.

## Como os dados são separados

O aplicativo valida os dígitos do CPF e deriva no computador um identificador técnico para usar com o provedor Email/Password do Firebase Authentication. O CPF em texto não é enviado nem gravado. Esse cadastro não consulta uma base oficial e não confirma a titularidade do documento; serve à simulação educativa.

| Local | Documento | Conteúdo |
|---|---|---|
| Firebase Authentication | Conta técnica | UID, identificador derivado do CPF e credencial protegida pelo Firebase |
| Firestore | `participantes/{uid}` | Apenas `votou: true`; criado uma vez por conta |
| Firestore | `sessoes/{uuid-aleatorio}` | Impressão digital e cinco votos; ID sem CPF ou UID |
| Firestore | `resultados/{cargo}_{numero}` | Contagem agregada e ID aleatório da última sessão que alterou o total |
| Computador | `votos-pendentes-{uid}.tsv` | Sessões que ainda aguardam sincronização |

O documento de participação e a sessão são gravados juntos. As regras impedem a criação de outra marca para o mesmo UID e verificam que cada incremento de contador corresponde ao voto na sessão do mesmo lote. Participantes não podem listar nem consultar sessões ou resultados. A equipe responsável consulta os totais no Console Firebase.

## 1. Preparar o Firebase

1. Crie um projeto em [Firebase Console](https://console.firebase.google.com/).
2. Em **Authentication → Sign-in method**, ative **Email/Password**. Não é necessário configurar envio de email: o endereço usado pela aplicação é técnico e não recebe mensagens.
3. Crie o banco Cloud Firestore `(default)`.
4. Em **Firestore Database → Regras**, publique o conteúdo de [`firestore.rules`](../firestore.rules).

## 2. Configurar a aplicação

Copie `urna.properties.example` para `urna.properties`, na pasta do aplicativo, e informe os valores do projeto:

```properties
firebase.projectId=ID_DO_PROJETO
firebase.apiKey=CHAVE_WEB_DA_API
```

O app também lê `URNA_FIRESTORE_PROJECT_ID` e `URNA_FIRESTORE_API_KEY`. A chave de API é usada pelo cliente Firebase, não concede acesso administrativo; restrinja-a às APIs necessárias para Firestore, Identity Toolkit e Secure Token. Não adicione `urna.properties` ao Git.

Para montar o pacote de Release com Firebase, configure `FIREBASE_PROJECT_ID` em **Settings → Secrets and variables → Actions → Variables** e `FIREBASE_API_KEY` em **Secrets**. O workflow inclui essa configuração no ZIP somente quando os dois valores existem.

## 3. Cadastrar e votar

1. Abra a aplicação e escolha **Primeiro acesso**.
2. Informe o CPF e crie uma senha. O cadastro já abre a votação.
3. Nos próximos acessos, escolha **Entrar** e use o mesmo CPF e senha.
4. Depois de confirmar o último cargo, a sessão é gravada localmente e sincronizada. A urna toca o sinal, fala a mensagem de encerramento e retorna à tela de identificação após cinco segundos.

Se a mesma conta já tiver votado, o aplicativo informa isso antes de abrir uma nova sessão. A proteção central depende do Firebase. Os modos `--local` e `--memoria` não identificam participantes entre computadores.

## 4. Conferir a apuração

No Console Firebase, abra o banco Firestore e a coleção `resultados`. Cada documento usa o formato `cargo_numero`, por exemplo `pres_11` ou `df_1111`, e apresenta `contagem`. Um contador é criado com zero quando a primeira votação válida daquele candidato é registrada. Voto de legenda, branco ou nulo não aumenta a contagem de um candidato. As regras bloqueiam a consulta dessa coleção pelos participantes.

IDs de resultado correspondentes às candidaturas atuais:

```text
df_8101  df_8202  df_8303  df_1111  df_2222  df_3333
de_81001
sen_820  sen_830  sen_833
gov_84  gov_82  gov_83  gov_44  gov_55  gov_66
pres_81  pres_85  pres_83  pres_11  pres_22  pres_33
```

O envio usa `documents:commit`: a sessão e os incrementos de contagem entram no mesmo lote atômico. Se faltar conexão depois da identificação, a votação permanece no arquivo local e é reenviada quando a conexão voltar. O Firestore aceita somente a primeira participação de cada conta.

## Observação sobre CPF

O CPF funciona como identificador único do cadastro, mas o aplicativo não consulta a Receita Federal, não verifica titularidade e não substitui credenciamento presencial. Os dados de autenticação derivam do CPF no cliente; este projeto é destinado a uma simulação, não a uma eleição oficial.
