# Requisitos da Urna Cidadã

## Escopo

Aplicação de simulação eleitoral educativa, feita em Java 17 com interface Swing. A tela e as regras estão no código Java; o projeto não contém interface em HTML, CSS ou JavaScript. A urna não é um sistema eleitoral oficial.

## Requisitos funcionais

| Requisito | Implementação |
|---|---|
| Votar em cinco cargos, nesta ordem: Deputado Federal, Deputado Estadual, Senador, Governador e Presidente | `model/Eleicao.java`, `service/UrnaService.java` |
| Digitar número, conferir candidato, corrigir, votar em branco, nulo ou legenda nos cargos proporcionais | `model/`, `service/`, `view/` |
| Cadastrar e identificar participantes por CPF quando Firebase está configurado | `repository/CpfIdentificador.java`, `repository/AutenticacaoFirebase.java`, `Main.java` |
| Impedir segunda votação da mesma conta no Firestore | `repository/FirestoreRestVotoClient.java`, `firestore.rules` |
| Contabilizar votos válidos por candidato sem somar branco, nulo ou legenda | `repository/FirestoreRestVotoClient.java`, `firestore.rules` |
| Manter votos localmente quando a internet falhar e sincronizar depois | `repository/VotoRepositoryLocalPrimeiro.java` |
| Tocar sinal, falar a mensagem de encerramento e retornar à identificação após cinco segundos | `view/SomUrna.java`, `view/TelaUrna.java` |
| Distribuir uma versão Windows com runtime Java incluído | `scripts/empacotar.bat`, `.github/workflows/publicar-release.yml` |

## Identificação e privacidade

O app valida o formato e os dígitos verificadores do CPF. Em seguida, deriva um identificador técnico para o Firebase Authentication; não envia nem guarda o CPF em texto. A autenticação não consulta base oficial nem comprova titularidade. É um identificador para a simulação.

No Firestore, `participantes/{uid}` guarda apenas a marca de uso. Os cinco votos são armazenados em `sessoes/{uuid-aleatorio}`, sem o CPF e sem o UID. Os totais ficam em `resultados/{cargo}_{numero}`. No modo local, não há bloqueio central entre computadores.

## Candidaturas

As nove candidaturas cadastradas usam números exclusivos:

| Cargo | Números e nomes |
|---|---|
| Presidente | 11 Rafael Mendes; 22 Eduardo Carvalho; 33 Mariana Oliveira |
| Governador | 44 Gabriel Santos; 55 Camila Ferreira; 66 Lucas Almeida |
| Deputado Federal | 1111 Pedro Henrique Costa; 2222 Beatriz Rodrigues; 3333 André Martins |

As propostas e partidos estão em `Eleicao.exemplo()` e em [regras-de-votacao.md](regras-de-votacao.md). Deputado Estadual e Senador permanecem na sequência dos cinco cargos, sem candidaturas; nesses cargos, a pessoa pode votar em branco ou nulo.

## Execução e armazenamento

- Compilação, execução e testes usam JDK 17 ou superior; Maven não é necessário.
- O pacote Windows inclui o runtime Java e não exige instalação separada.
- O modo demo guarda dados somente em memória; `--local` usa arquivo local; o modo padrão usa Firebase quando configurado e, caso contrário, permanece local.
- A configuração Firebase fica em `urna.properties`, fora do controle de versão.
