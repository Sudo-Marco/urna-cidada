# Requisitos e rastreabilidade — Urna Cidadã

Este documento reflete o código e a verificação desta revisão. A especificação detalhada enviada junto com a tarefa orienta o escopo técnico. O enunciado/rubrica do professor em PDF não foi localizado nos arquivos do repositório, ZIP ou anexos disponíveis; requisitos exclusivos desse arquivo são **não verificáveis**. Não se afirma que a aprovação acadêmica foi obtida.

## Decisões

- Java 17+, Swing, sem dependências externas em runtime. Compilação e testes pelos scripts não exigem Maven nem internet.
- Uma única votação contém cinco cargos e um voto por cargo: Deputado Federal (4 dígitos), Deputado Estadual (5), Senador (3; simplificado a um voto), Governador (2) e Presidente (2), nessa ordem.
- Candidatos, partidos, números 81–83, vices e propostas são fictícios. Dados de demonstração ficam em `Eleicao.exemplo()`.
- A gravação de sessão ocorre no fim do último cargo. Arquivo local é a fonte de verdade; envio Firestore REST é assíncrono e opcional.
- Nenhum dado pessoal é coletado. Um documento contém UUID da sessão e lista de votos (cargo, tipo, número e data/hora).

## Matriz de rastreabilidade

| Requisito da missão | Código | Documentação | Evidência de teste/estado |
|---|---|---|---|
| Cinco cargos em ordem e números com comprimentos corretos | `model/Eleicao.java`, `Cargo.java` | `regras-de-votacao.md` | `ModeloTestes`, `UrnaServiceTestes`: verificado automaticamente |
| Candidaturas únicas, 2–4 propostas até 90 caracteres, partidos e vices consistentes | `Candidato.java`, subclasses, `Cargo.java` | `regras-de-votacao.md` | `ModeloTestes`: verificado automaticamente |
| Entrada válida, nula, branca, correção e legenda proporcional | `UrnaService.java`, `TipoVoto.java` | `regras-de-votacao.md` | `UrnaServiceTestes`, `ControladorUrnaTestes`: verificado |
| Confirmações sequenciais e persistência só após quinto cargo | `UrnaService.java` | `casos-de-uso.md` | fluxo completo nos testes: verificado |
| Painel, teclado gráfico/físico, estado visual e fim sem reinício automático | `view/PainelUrna.java`, `TelaUrna.java` | `testes.md` | Desenho headless verificado; janela, teclado físico e áudio manual pendentes |
| Painel rolável de candidaturas e propostas | `view/PainelCandidatos.java` | `regras-de-votacao.md` | Compilado; inspeção visual real e escala 125%/150% pendentes |
| Local primeiro, escrita atômica, compatibilidade TSV e sincronização assíncrona sem duplicar | `VotoRepositoryEmArquivo.java`, `VotoRepositoryLocalPrimeiro.java` | `operacao-oficina.md`, `testes.md` | Teste local + servidor falso: verificado; indisponibilidade de energia/disco real não simulada |
| Firestore REST, timeouts, erros de configuração/transitórios e idempotência | `FirestoreRestVotoClient.java` | `firebase-configuracao.md` | HTTP falso: 200, 409, 400/401/403/404, 503, timeout e servidor ausente verificados; Firebase real pendente |
| Regras create-only e sem leitura/alteração/exclusão | `firestore.rules` | `firebase-configuracao.md` | Inspecionadas; publicação e avaliação no Firebase real pendentes |
| Configuração fora do Git, variáveis override e mensagens | `ConfiguracaoUrna.java`, `.gitignore` | `firebase-configuracao.md`, `solucao-de-problemas.md` | Ausente, incompleta, exemplo e sintaticamente válida verificados |
| Caminhos ao lado do app com fallback de perfil, log rotativo e instância única | `CaminhosAplicacao.java`, `LogUrna.java`, `TravaInstancia.java` | `solucao-de-problemas.md` | Compilação verificada; cenários reais de permissão/múltiplas contas pendentes |
| JDK 17+, scripts sem Maven e JAR executável | `scripts/*`, `pom.xml` | README, `operacao-oficina.md` | Windows: testes e empacotamento executados; Linux/macOS não executados nesta máquina |
| Sem segredo versionado ou dados reais | `.gitignore`, `urna.properties.example` | README, Firebase docs | Busca e revisão do diff; chave Firebase real não fornecida |
| Requisitos da rubrica do professor | PDF ausente | este arquivo | **Não verificável** sem o PDF/enunciado |
| Aparência em computador da oficina, projetor, escala e áudio | interface Swing | `testes.md` | **Pendente manualmente**, sem equipamento verificado |
| Oficina, autorização e avaliação de impacto | fora da aplicação | materiais existentes preservados | **Não realizado/não verificável** nesta revisão |

## Requisitos não funcionais e limites

| Tema | Atendimento | Situação |
|---|---|---|
| Privacidade | Só dados da votação e UUID aleatório | Inspecionado no modelo; sem avaliação externa de privacidade |
| Não perder voto em falha de rede | Persistência local antes da rede | Testado com HTTP falso; falhas físicas de armazenamento não cobertas |
| Compatibilidade | Fontes `--release 17`; sem dependências de aplicação | Compilado neste Windows com JDK disponível; outras versões/sistemas não testados |
| Segurança da chave | Não versionada; validação e mascaramento | Inspecionada; chave de API continua pública e escrita anônima pode ser abusada |
| Legibilidade | Layout testado sem janela a 1366×768 e 1120×680 | Automático no painel; DPI real e projetor pendentes |
| Uso eleitoral real | Não é objetivo nem seguro | Não usar como urna oficial |

## Não verificável

O PDF do professor não estava disponível no repositório ou nos anexos fornecidos para esta rodada. Portanto, não foi possível montar uma correspondência a itens que só existam nessa rubrica. Solicitar o arquivo e revisar esta matriz é o passo necessário para verificar esses critérios. Nenhum requisito de extensão foi inferido a partir de textos que não estavam disponíveis.
