# Requisitos da Urna Cidadã

## Escopo

Aplicação de simulação eleitoral educativa, feita em Java 17 com interface Swing. A tela e as regras estão no código Java; o projeto não contém interface em HTML, CSS ou JavaScript. A urna não é um sistema eleitoral oficial.

## Requisitos funcionais

| Requisito | Implementação |
|---|---|
| Votar em três cargos, nesta ordem: Deputado Federal, Governador e Presidente | `model/Eleicao.java`, `service/UrnaService.java` |
| Digitar número, conferir candidato, corrigir, votar em branco, nulo ou legenda nos cargos proporcionais | `model/`, `service/`, `view/` |
| Cadastrar e identificar participantes por sequência de onze algarismos na máscara CPF quando Firebase está configurado | `repository/CpfIdentificador.java`, `repository/AutenticacaoFirebase.java`, `Main.java` |
| Impedir segunda votação da mesma conta no Firestore | `repository/FirestoreRestVotoClient.java`, `firestore.rules` |
| Contabilizar votos válidos por candidato sem somar branco, nulo ou legenda | `repository/FirestoreRestVotoClient.java`, `firestore.rules` |
| Manter votos localmente quando a internet falhar e sincronizar depois | `repository/VotoRepositoryLocalPrimeiro.java` |
| Consultar a apuração local por cargo, candidatura e legenda, com totais e ranking, sem acessar a rede | `service/ApuracaoLocal.java`, `view/PainelApuracaoLocal.java`, `Main.java` |
| Reiniciar votação sem remover contas CPF ou coleções do Firestore | `repository/FirestoreAdminClient.java`, `firestore.rules` |
| Tocar sinal, falar a mensagem de encerramento e retornar à identificação após cinco segundos | `view/SomUrna.java`, `view/TelaUrna.java` |
| Distribuir uma versão Windows com runtime Java incluído | `scripts/empacotar.bat`, `.github/workflows/publicar-release.yml` |

## Identificação e privacidade

O app exige onze algarismos no formato visual `000.000.000-00`, mas não verifica dígitos verificadores nem titularidade. O valor pode ser inventado para a simulação. Em seguida, deriva um identificador técnico para o Firebase Authentication; não envia nem guarda o valor em texto. O Firebase impede que o mesmo identificador crie mais de uma conta.

No Firestore, `participantes/{uid}` guarda apenas se a conta votou na rodada atual. Um reset administrativo altera essa marca para `false`, preservando o cadastro. Os três votos são armazenados em `sessoes/{uuid-aleatorio}`, sem o identificador digitado e sem o UID. Os totais ficam em `resultados/{cargo}_{numero}`. No modo local, não há bloqueio central entre computadores.

## Candidaturas

As nove candidaturas cadastradas usam números exclusivos:

| Cargo | Números e nomes |
|---|---|
| Presidente | 13 Austin Mendes; 27 Totem Carvalho; 39 Abacatudo Oliveira |
| Governador | 14 The Rock Johnson; 28 Chico Bento do Rosário; 37 Kevin Hart Silva |
| Deputado Federal | 1736 Pablo Rodrigues; 5730 Peixonauta da Silva; 9462 Cebolinha da Mônica |

As propostas e partidos estão em `Eleicao.exemplo()` e em [regras-de-votacao.md](regras-de-votacao.md). Deputado Estadual e Senador não fazem parte da votação.

## Execução e armazenamento

- Compilação, execução e testes usam JDK 17 ou superior; Maven não é necessário.
- O pacote Windows inclui o runtime Java e não exige instalação separada.
- O modo demo guarda dados somente em memória; `--local` usa arquivo local; o modo padrão usa Firebase quando configurado e, caso contrário, permanece local.
- A configuração Firebase fica em `urna.properties`, fora do controle de versão.
