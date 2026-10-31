<div align="center">

# 🗳️ Urna Cidadã

**Simulador educativo de votação em Java 17, Swing e interface web local.**

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Interfaces](https://img.shields.io/badge/Interfaces-Swing%20%7C%20Web-blue)
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

Use as teclas `0–9`, `B` para branco, `Backspace` para corrigir e `Enter` para confirmar. O som pode ser silenciado na tela. A votação fica concentrada na tela principal, sem painel lateral.

## Compilar, testar e empacotar

### VS Code e Maven (Windows)

O projeto já tem `pom.xml` e usa Java 17. Com o Apache Maven disponível, abra esta pasta no VS Code e use **Terminal > Run Task**: `Maven: compilar` ou `Maven: executar urna (desktop)`. Também é possível executar no terminal:

```bat
.tools\apache-maven-3.10.0\bin\mvn.cmd compile
.tools\apache-maven-3.10.0\bin\mvn.cmd exec:java -Dexec.args=--memoria
```

O segundo comando abre a interface Swing como aplicativo desktop. Ele não abre a urna no navegador; veja a observação em [GitHub](#github) sobre a diferença entre aplicativo desktop e site.

```bat
scripts\compilar.bat
scripts\testar.bat
scripts\empacotar.bat
```

Os testes são Java puro, não usam Maven nem internet. O resultado verificado nesta revisão foi `86 passaram, 0 falharam em cada uma de três execuções consecutivas`. Detalhes, matriz e checklist manual em [docs/testes.md](docs/testes.md). Também há lançadores `.sh` em `scripts/` para Linux e macOS; neste ciclo não foram executados em Linux/macOS.

## Usar no navegador sem Maven

O projeto inclui uma interface web servida por Java. Ela reutiliza as regras de votação e os repositórios existentes; o navegador mostra a tela e o servidor local continua responsável por validar e gravar os votos. É necessário apenas o JDK 17+.

No Windows, dê duplo clique em `web.bat` ou execute no terminal:

```bat
scripts\servir.bat
scripts\servir.bat --local
scripts\servir.bat --memoria
```

Abra `http://127.0.0.1:8080`. A primeira opção usa o armazenamento local-primeiro já documentado; `--local` grava só no arquivo local e `--memoria` não mantém votos ao encerrar. Deixe o terminal aberto enquanto usa a página; `Ctrl+C` encerra o servidor. Para mudar a porta, defina a variável `URNA_WEB_PORT` antes de iniciar.

O servidor escuta apenas em `127.0.0.1`, neste computador. Para outras pessoas acessarem pela rede ou pela internet, seria necessário hospedar o servidor e rever autenticação, segurança e persistência.

## GitHub

O código fonte pode ser colocado no repositório GitHub já existente. GitHub Pages pode hospedar arquivos estáticos, mas não executa o servidor Java necessário para esta urna web. Para abrir em outro computador, inicie `scripts/servir.bat` nele e acesse a URL local; o computador precisa do JDK 17+. Leia [docs/operacao-oficina.md](docs/operacao-oficina.md) para clonar, testar e executar.

## Estrutura

```text
src/com/urnacidada/       modelo, serviço, interfaces desktop/web e persistência
web/                      interface HTML/CSS/JavaScript servida localmente
scripts/                  compilação e lançadores Windows/Linux/macOS
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
