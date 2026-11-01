# Urna Cidadã

Simulador de votação feito em **Java 17 e Swing** para um projeto de extensão sobre cidadania digital e desinformação eleitoral. A aplicação funciona como programa de computador e não usa página HTML, CSS ou JavaScript.

O projeto passa por cinco cargos — Deputado Federal, Deputado Estadual, Senador, Governador e Presidente — e permite voto válido, de legenda, branco ou nulo. Os dados de candidaturas fazem parte da simulação e não representam uma eleição oficial.

## Executar

### Windows

Para usar a versão pronta, baixe `UrnaCidada-Windows-x64.zip` em [Releases](https://github.com/Sudo-Marco/urna-cidada/releases), extraia a pasta inteira e abra `UrnaCidada.exe`. O pacote inclui o runtime Java.

Para executar pelo código-fonte, instale o JDK 17 ou superior e abra `iniciar.bat`. Para uma demonstração sem gravar votos, abra `demo.bat`.

```bat
scripts\iniciar.bat
scripts\iniciar.bat --local
scripts\iniciar.bat --memoria
scripts\testar.bat
```

### Linux e macOS

Com JDK 17 ou superior:

```sh
sh scripts/iniciar.sh
sh scripts/iniciar.sh --local
sh scripts/demo.sh
sh scripts/testar.sh
```

## Identificação por CPF

Com Firebase configurado, a tela inicial identifica o participante pelo CPF e uma senha. No primeiro acesso, escolha **Primeiro acesso**; nas próximas votações, escolha **Entrar**. O aplicativo valida os dígitos verificadores e usa um identificador técnico derivado do CPF para criar a conta no Firebase Authentication. O CPF em texto não é enviado ao Firebase nem gravado nos arquivos de voto.

O CPF organiza o cadastro, mas a urna não consulta uma base oficial e não confirma quem é seu titular. Esse fluxo serve para a simulação do projeto.

Depois do último voto, a urna toca o sinal de encerramento e fala “Você já votou. Obrigado por participar.” Em cinco segundos, retorna à identificação para o próximo participante. No modo local, sem Firebase, não há identificação central nem bloqueio de votos repetidos em outros computadores.

No Windows, a fala usa a síntese de voz do sistema e seleciona uma voz em português do Brasil quando disponível.

## Candidatos adicionados

Os números e propostas abaixo estão cadastrados em [Eleicao.java](src/com/urnacidada/model/Eleicao.java) e também aparecem em [Regras de votação](docs/regras-de-votacao.md).

| Cargo | Número | Candidato | Partido |
|---|---:|---|---|
| Presidente | 11 | Rafael Mendes | Renovação Nacional (PRN) |
| Presidente | 22 | Eduardo Carvalho | Aliança pelo Progresso (PAP) |
| Presidente | 33 | Mariana Oliveira | Movimento Popular Democrático (MPD) |
| Governador | 44 | Gabriel Santos | Força e Desenvolvimento Estadual (FDE) |
| Governador | 55 | Camila Ferreira | União pelo Futuro (PUF) |
| Governador | 66 | Lucas Almeida | Desenvolvimento e Justiça Social (DJS) |
| Deputado Federal | 1111 | Pedro Henrique Costa | Juventude e Inovação Brasileira (JIB) |
| Deputado Federal | 2222 | Beatriz Rodrigues | Vozes da Cidadania (PVC) |
| Deputado Federal | 3333 | André Martins | Aliança de Desenvolvimento Sustentável (ADS) |

Os demais candidatos de exemplo foram mantidos nos cargos de Deputado Estadual e Senador, além dos nomes já existentes para Governador e Presidente.

## Firebase e apuração

Sem configuração, a urna grava localmente. Para habilitar autenticação e sincronização:

1. Crie um projeto Firebase com Cloud Firestore e ative o provedor **Email/Password** em Authentication. O app usa uma conta técnica derivada do CPF; não envia email de confirmação.
2. Publique o arquivo [`firestore.rules`](firestore.rules) nas regras do banco.
3. Copie `urna.properties.example` para `urna.properties` e preencha:

   ```properties
   firebase.projectId=ID_DO_PROJETO
   firebase.apiKey=CHAVE_WEB_DA_API
   ```

   Também são aceitas as variáveis `URNA_FIRESTORE_PROJECT_ID` e `URNA_FIRESTORE_API_KEY`. `urna.properties` está ignorado pelo Git.

Ao concluir os cinco cargos, o aplicativo grava uma sessão com ID aleatório e atualiza os totais no mesmo lote do Firestore. Os votos válidos incrementam `resultados/{cargo}_{numero}`; legenda, branco e nulo não alteram a contagem de candidatos. Um documento separado em `participantes/{uid}` impede que a mesma conta envie outra votação. A estrutura e a configuração estão em [docs/firebase-configuracao.md](docs/firebase-configuracao.md).

## Testes e pacote

Execute `scripts\testar.bat` no Windows ou `sh scripts/testar.sh` em Linux/macOS. A suíte não exige Maven nem conexão externa. `scripts\compilar.bat` gera um JAR Java; `scripts\empacotar.bat` monta um ZIP para Windows 64 bits com runtime incluído.

O workflow do GitHub executa os testes e publica o ZIP quando uma tag `v*` é enviada. Para incluir a configuração Firebase no pacote da Release, defina `FIREBASE_PROJECT_ID` nas variáveis de Actions e `FIREBASE_API_KEY` nos secrets do repositório.

## Estrutura

```text
src/                 aplicação Java e interface Swing
tests/                testes Java
scripts/              execução, compilação, testes e empacotamento
docs/                 regras, operação e configuração Firebase
firestore.rules       regras de acesso ao Firestore
```

## Documentação

- [Regras de votação e candidaturas](docs/regras-de-votacao.md)
- [Configuração Firebase e contagem de votos](docs/firebase-configuracao.md)
- [Casos de uso](docs/casos-de-uso.md)
- [Operação da urna](docs/operacao-oficina.md)
- [Testes](docs/testes.md)
- [Diagrama de classes](docs/diagrama-classes.md)
