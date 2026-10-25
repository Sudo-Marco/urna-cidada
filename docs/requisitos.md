# Requisitos — Urna Cidadã

> Este documento define **o que o sistema deve fazer** e informa, com honestidade, **o que já está implementado e o que ainda depende de decisão ou de execução real**. Os códigos (RF01, RNF01…) são usados também nos testes e na documentação.

## 1. Objetivo

Desenvolver um simulador educativo de urna eletrônica para demonstrar o processo de votação e promover a conscientização sobre cidadania digital e desinformação eleitoral, em uma oficina com estudantes do Ensino Médio.

O sistema **não é** uma urna oficial e usa somente candidatos, partidos e números **fictícios**.

## 2. Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 ou superior | Linguagem principal, com programação orientada a objetos |
| Java Swing | Interface gráfica |
| Cloud Firestore | Persistência anônima dos votos na nuvem |
| Maven | Compilação e dependências |
| Git e GitHub | Controle de versão e colaboração |

## 3. Requisitos funcionais

Legenda: ✅ implementado e coberto por teste automático · 🖥️ implementado, mas só verificável com a janela aberta · ☁️ implementado, mas depende de um serviço externo real.

| Código | Requisito | Situação | Onde está / como é verificado |
|---|---|---|---|
| RF01 | Iniciar uma votação simulada | ✅ | A urna abre pronta; ao final, o botão "Iniciar nova votação" recomeça. `ControladorUrna.novaVotacao` |
| RF02 | Apresentar o primeiro cargo (proporcional) | ✅ | Vereador, 5 dígitos. `Eleicao.exemplo()` |
| RF03 | Apresentar o segundo cargo (majoritário) após o primeiro | ✅ | Prefeito, 2 dígitos |
| RF04 | Informar o número do candidato | 🖥️ | Teclado na tela (✅ testado) e teclado do computador (🖥️) |
| RF05 | Validar a entrada e orientar o participante | ✅ | Só números, limite de dígitos, mensagens por cor (ver [regras-de-votacao.md](regras-de-votacao.md)) |
| RF06 | Confirmar o voto antes de avançar | ✅ | Botão CONFIRMA exige número completo ou branco |
| RF07 | Corrigir, votar em branco e voto nulo | ✅ | Regras adotadas em [regras-de-votacao.md](regras-de-votacao.md), **a validar com o professor** |
| RF08 | Encerrar com mensagem de conclusão | ✅ | Tela "VOTAÇÃO CONCLUÍDA / FIM" |
| RF09 | Persistir os dados no Cloud Firestore | ☁️ | `FirestoreVotoRepository`, coleção `sessoes`. **Ainda não foi testado com um projeto Firestore real** |
| RF10 | Material educativo sobre desinformação e deepfakes | 🖥️ | Botão "Dicas contra desinformação" na tela e [material-oficina.md](material-oficina.md) |
| RF11 | Não perder votos se a internet cair *(extensão do grupo)* | ✅ | `VotoRepositoryComReserva` guarda em `dados/votos-pendentes.tsv` e envia depois |

## 4. Requisitos não funcionais

| Código | Requisito | Situação | Observação |
|---|---|---|---|
| RNF01 | Usabilidade: instruções curtas e legíveis para estudantes do Ensino Médio | 🖥️ | Textos quebram linha, cores por tipo de mensagem, progresso "CARGO 1 DE 2". Conferir na tela e no projetor da oficina |
| RNF02 | Robustez: entradas inesperadas não encerram o programa | ✅ | Letras, excesso de dígitos, confirmações incompletas e falhas de gravação são tratadas |
| RNF03 | Privacidade: sem dados pessoais | ✅ | O voto guarda apenas cargo, tipo, número e data/hora |
| RNF04 | Segurança de configuração | ✅ | `.gitignore` bloqueia credenciais e a pasta `dados/` |
| RNF05 | Documentação | ✅ | README e esta pasta `docs/` |
| RNF06 | Testabilidade sem depender de cliques | ✅ | 66 testes automáticos ([testes.md](testes.md)) |
| RNF07 | Escopo educativo claro | ✅ | Aviso permanente no topo da tela e no README |
| RNF08 | Portabilidade | ✅ | Java 17+; scripts `.bat` para Windows e comandos Maven para outros sistemas |

## 5. Decisões de projeto adotadas

Estas decisões **substituem** a antiga lista de pendências. Continuam sujeitas à validação do grupo e do professor.

| Tema | Decisão | Detalhes |
|---|---|---|
| Cargos | Vereador (proporcional, 5 dígitos) e Prefeito (majoritário, 2 dígitos) | `Eleicao.exemplo()` |
| Candidatos | Três fictícios por cargo | [regras-de-votacao.md](regras-de-votacao.md) |
| Fluxo | Sequencial: primeiro Vereador, depois Prefeito | [casos-de-uso.md](casos-de-uso.md) |
| Branco e nulo | Branco é escolha explícita; nulo é número completo inexistente | [regras-de-votacao.md](regras-de-votacao.md) |
| Dados na nuvem | Documento `sessoes/{id}` com lista de votos anônimos | `FirestoreVotoRepository` |
| Modelagem | Casos de uso e diagrama de classes | [casos-de-uso.md](casos-de-uso.md), [diagrama-classes.md](diagrama-classes.md) |
| Critérios de teste | Testes automáticos + roteiro manual | [testes.md](testes.md) |

## 6. Pendências reais

| Pendência | Quem resolve |
|---|---|
| Validar as regras de votação com o grupo e o professor | Grupo |
| Criar o projeto no Firebase, habilitar o Firestore, autenticar e testar a gravação real | Quem fizer a frente de persistência |
| Conferir a tela, o teclado do computador e o projetor no equipamento da oficina | Grupo |
| Confirmar escola, data e autorizações da oficina | Grupo e professor |
| Revisar o material educativo com a escola | Grupo |
| Realizar a oficina e registrar dados reais | Grupo |

## 7. Observação

Os requisitos do professor e o escopo acadêmico vieram da proposta em PDF. Itens que exigem credenciais, autorização, oficina presencial ou dados de participantes **não podem ser declarados executados** apenas porque o código está pronto.
