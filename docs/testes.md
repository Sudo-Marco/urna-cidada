# Testes e checklist

## Como executar

Windows PowerShell ou CMD, com JDK 17+:

```bat
scripts\testar.bat
```

Linux/macOS com JDK 17+:

```sh
sh scripts/testar.sh
```

São testes Java puros sem JUnit, Maven ou internet. O cliente HTTP usa `com.sun.net.httpserver.HttpServer` ligado somente a `127.0.0.1`. Na revisão de 29/10/2026, em Windows com JDK 17+ disponível, o resultado foi **86 passaram, 0 falharam em cada uma de três execuções consecutivas**. A compilação usa `javac --release 17`.

## Cobertura atual

| Bateria | Casos cobertos |
|---|---|
| `ModeloTestes` | Herança/polimorfismo, números, cargos em ordem, propostas e vices consistentes |
| `UrnaServiceTestes` | Cinco cargos, válido, nulo, branco, correção, legenda, persistência só no fim, falha e nova tentativa |
| `RepositorioTestes` | Memória, gravação/leitura atômica, TSV anterior, duplicidade, arquivo corrompido, reserva |
| `ControladorUrnaTestes` | Mensagens, candidato/legenda, progressão, fim, falhas e retorno assíncrono |
| `PainelUrnaTestes` | Desenho em thread Swing, botões, estados e texto em 1366×768 e mínimo 1120×680 |
| `ConfiguracaoTestes` | Configuração ausente, incompleta, exemplo e sintaticamente válida; segredo mascarado |
| `FirestoreTestes` | HTTP 200/409, 400/401/403/404, 503, timeout e servidor indisponível |
| `LocalPrimeiroTestes` | Modo sem nuvem, offline→online, remoção da pendência e recuo até 300 s |

**Não coberto automaticamente:** janela nativa real e DPI 125%/150%, hardware de áudio e teclado físico, queda real de internet/Firebase, publicação das regras, permissão física/AV, colisões entre sistemas de arquivo e uso prolongado. Nenhum experimento de “falha proposital” foi executado nesta rodada.

## Matriz rápida requisito → implementação → evidência

| Requisito | Código | Evidência |
|---|---|---|
| Cinco cargos, sequência e regras de voto | `model/Eleicao.java`, `service/UrnaService.java` | `ModeloTestes`, `UrnaServiceTestes` |
| Legenda, branco, nulo e correção | `TipoVoto.java`, `UrnaService.java` | `UrnaServiceTestes`, `ControladorUrnaTestes` |
| Layout principal sem painel lateral | `PainelUrna.java` | `PainelUrnaTestes` (headless); aparência real pendente |
| Arquivo atômico e compatibilidade TSV | `VotoRepositoryEmArquivo.java` | `RepositorioTestes` |
| REST, sincronização e idempotência | `FirestoreRestVotoClient.java`, `VotoRepositoryLocalPrimeiro.java` | Servidor HTTP falso verificado; configuração real mostrou `Online · tudo enviado` e o usuário informou que a sessão de teste foi salva; documento não inspecionado diretamente no console |
| Sem credenciais para modo local/demonstração | `Main.java`, `ConfiguracaoUrna.java` | Configuração e repositório local sob teste |
| Rubrica do professor | não localizado | **Não verificável** sem PDF/enunciado |

## Verificação manual relatada em 30/10/2026

O usuário configurou o projeto Firebase e publicou as regras. A captura da urna mostrou `Firebase configurado` e `Online · tudo enviado`; o usuário informou que a votação de teste foi salva. Não houve inspeção independente do documento no console Firestore. A sincronização automática depois de uma interrupção real da internet continua sem verificação manual.

## Checklist manual pendente

Os itens abaixo permanecem pendentes de conferência independente. As verificações relatadas pelo usuário estão registradas na seção anterior.

### Interface, teclado e áudio

- [ ] Abrir `scripts\demo.bat` no Windows e conferir o aviso fixo “SIMULAÇÃO EDUCATIVA · NÃO É UMA URNA OFICIAL”.
- [ ] Realizar cinco cargos com botões gráficos e depois com teclado físico: dígitos, `B`, `Backspace`, `Enter`.
- [ ] Confirmar válido, legenda em Deputado, nulo, branco e correção.
- [ ] Conferir a identificação do candidato selecionado e os comandos da tela principal.
- [ ] Silenciar/reativar som; testar máquina sem dispositivo de áudio e confirmar que continua funcionando.
- [ ] Conferir texto e botões no tamanho normal e maximizado a 1366×768.
- [ ] Conferir escala Windows 125% e 150%, mínimo da janela e legibilidade no projetor.
- [ ] Encerrar, iniciar nova votação somente pelo botão e fechar pela janela.

### Sistema de arquivos

- [ ] Executar com permissão normal; confirmar `dados\votos-pendentes.tsv` ao lado do JAR.
- [ ] Testar pasta de aplicação sem permissão e conferir caminho de fallback `%USERPROFILE%\.urna-cidada\dados`.
- [ ] Abrir uma segunda instância e confirmar mensagem clara de bloqueio.
- [ ] Fazer cópia de segurança dos dados de teste antes de limpar a pasta.

### Firebase real: pendências (usar somente projeto descartável de teste)

- [ ] Conferir diretamente no console o documento de cinco votos anônimos na coleção `sessoes`.
- [ ] Desconectar a internet antes do fim, completar a votação, reabrir rede e aguardar sincronização automática.
- [ ] Confirmar no Firestore que o documento da sessão tem UUID e cinco votos e que o TSV pendente foi removido após a sincronização.
- [ ] Limpar os documentos de teste no console Firebase e descartar a chave de teste se apropriado.

### Outras plataformas e rubrica

- [ ] Executar `sh scripts/testar.sh`, `sh scripts/compilar.sh`, `sh scripts/empacotar.sh` em Linux/macOS com JDK 17+.
- [ ] Conferir a janela e a renderização em cada sistema operacional pretendido.
- [ ] Obter a rubrica/PDF do professor e comparar com `docs/requisitos.md`.
- [ ] Oficina, autorização escolar e avaliação de impacto: pendentes de realização.
