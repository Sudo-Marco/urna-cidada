<div align="center">

# 🗳️ Urna Cidadã

**Simulador educativo de votação em Java 17 e Swing.**

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Interface](https://img.shields.io/badge/Interface-Swing-blue)
![Armazenamento](https://img.shields.io/badge/Armazenamento-local%20primeiro%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black)

</div>

> **SIMULAÇÃO EDUCATIVA · NÃO É UMA URNA OFICIAL.** Candidatos, partidos, números, propostas e vices deste exemplo são fictícios. O programa não coleta dados pessoais.

## O que é

Aplicativo desktop de uma única votação sequencial em cinco cargos. Cada votação tem um voto por cargo e só é persistida após a confirmação do último. Por padrão, grava primeiro em arquivo local e envia ao Cloud Firestore em segundo plano quando configurado e conectado. Não exige Maven, bibliotecas externas nem login do Google.

## Começar no Windows

Requisito: **JDK 17 ou superior**, instalado e disponível nos comandos `java`, `javac` e `jar`. Para confirmar, abra um terminal e execute `java -version`.

Baixe ou clone o repositório, abra a pasta e dê duplo clique em `demo.bat` para testar em memória. Para usar no modo padrão local-primeiro, dê duplo clique em `iniciar.bat`. Em terminal:

```bat
scripts\demo.bat
scripts\iniciar.bat
scripts\iniciar.bat --local
scripts\iniciar.bat --memoria
scripts\iniciar.bat --ajuda
```

O primeiro uso compila o programa com `javac --release 17` e cria `dist\urna-cidada.jar`. O modo padrão funciona sem configuração Firebase: os votos ficam em `dados\votos-pendentes.tsv` ao lado do aplicativo. Se essa pasta não puder ser gravada, o programa tenta `%USERPROFILE%\.urna-cidada\dados`. O modo `--local` grava apenas em `votos-locais.tsv`; `--memoria` não mantém os dados ao fechar.

Para o Firestore, siga [Firebase: configuração passo a passo](docs/firebase-configuracao.md). A chave deve ficar em `urna.properties`, arquivo ignorado pelo Git. Sem configuração válida a urna abre e permanece em modo local.

## Uma votação

1. Inicie a urna; escolha o número de cada candidatura, branco ou nulo.
2. Revise o candidato e confirme. Nos cargos proporcionais, os dois dígitos iniciais de partido podem ser confirmados como voto de legenda.
3. Repita na ordem: Deputado Federal, Deputado Estadual, Senador, Governador e Presidente. Esta simulação simplifica o Senado a um voto.
4. A tela mostra **FIM**; um mediador inicia a próxima votação pelo botão.

Use as teclas `0–9`, `B` para branco, `Backspace` para corrigir e `Enter` para confirmar. O som pode ser silenciado na tela. Há um painel rolável com candidatos e propostas fictícias.

## Compilar, testar e empacotar

```bat
scripts\compilar.bat
scripts\testar.bat
scripts\empacotar.bat
```

Os testes são Java puro, não usam Maven nem internet. O resultado verificado nesta revisão foi `86 passaram, 0 falharam em cada uma de três execuções consecutivas`. Detalhes, matriz e checklist manual em [docs/testes.md](docs/testes.md). Também há lançadores `.sh` em `scripts/` para Linux e macOS; neste ciclo não foram executados em Linux/macOS.

## GitHub

O código fonte pode ser colocado no repositório GitHub já existente. Esta aplicação Swing não é um site estático: GitHub Pages não pode hospedar uma janela Java. A entrega de desktop pode ser empacotada como JAR pela equipe; para abrir em outro computador, ele também precisa do JDK 17+. Leia [docs/operacao-oficina.md](docs/operacao-oficina.md) para clonar, testar e executar.

## Estrutura

```text
src/com/urnacidada/       modelo, serviço, interface Swing e persistência
scripts/                  lançadores Windows/Linux/macOS
firestore.rules           regras create-only do Firestore
urna.properties.example   modelo sem chave real
tests/                    testes Java puros e servidor HTTP falso
docs/                     operação, rastreabilidade, regras e verificação
```

As classes de dados de exemplo ficam centralizadas em `Eleicao.exemplo()`; edite somente dados fictícios antes de uma oficina. Os números de partido 81–83 também são inventados.

## Privacidade e limites

Cada voto contém somente cargo, tipo, número e data/hora. A sessão recebe um UUID aleatório; nenhum nome, matrícula, e-mail ou campo equivalente é guardado. A chave de API do Firebase é pública por natureza: as regras impedem leitura, atualização e remoção anônimas, mas qualquer pessoa com acesso à chave pode tentar criar sessões. Restrinja a chave à API do Firestore e monitore o uso. Não use este protótipo como sistema eleitoral real.

## Situação de verificação

Verificados nesta sessão: compilação `javac --release 17`, 77 testes automáticos, fluxo REST contra servidor HTTP local falso e criação de pacote JAR. Ainda dependem de checagem manual: execução visual no Windows com escala 125%/150%, teclado físico, áudio e projetor; configuração e escrita no Firebase real; execução dos `.sh` em Linux/macOS; validação de rubrica do professor. O PDF mencionado em versões anteriores não estava no ZIP nem nos anexos disponíveis para esta revisão; os critérios que dependem dele ficam **não verificáveis**.

A oficina e os resultados de extensão permanecem pendentes. Materiais de extensão, “Desafios”/CANVAS e a tabela Equipe não fazem parte desta revisão.

## Documentação

Veja [docs/README.md](docs/README.md) para o índice, [docs/solucao-de-problemas.md](docs/solucao-de-problemas.md) para mensagens e correções, e [docs/testes.md](docs/testes.md) para resultados reais e verificações manuais.

## 👥 Equipe

| Integrante | Frentes de atuação |
|---|---|
| **Marco Antônio** | Coordenação do repositório (Git e GitHub, branches e Pull Requests); regras de votação e testes automáticos; persistência no Cloud Firestore e configuração do Firebase; integração e entrega do código |
| **Cauã Reis** | Interface gráfica em Swing (tela, teclado e mensagens ao participante); verificação manual da tela e do teclado; testes de usabilidade no projetor da oficina |
| **Vitória Alves** | Ação de extensão: material educativo sobre desinformação, questionário de impacto e alinhamento com os ODS; contato com a escola e condução da oficina; documentação pedagógica |
