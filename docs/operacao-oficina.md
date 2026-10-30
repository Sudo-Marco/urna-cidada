# Operação da urna

## Requisitos

- JDK 17 ou superior. No Windows, `java`, `javac` e `jar` precisam estar no `PATH`.
- Modo demo/local não requer rede, Maven, Google Cloud CLI ou credenciais.
- O modo padrão começa em local e sincroniza com Firestore somente se `urna.properties` estiver configurado.

## Modos

| Comando | Resultado |
|---|---|
| `scripts\demo.bat` ou `demo.bat` | Em memória; nada persiste ao fechar |
| `scripts\iniciar.bat --local` | Arquivo local sem rede |
| `scripts\iniciar.bat` | Arquivo local primeiro; Firestore REST opcional |
| `scripts\compilar.bat` | Gera `dist\urna-cidada.jar` |
| `scripts\testar.bat` | Executa testes Java puros |
| `scripts\empacotar.bat` | JAR e arquivos de início em `dist\` |

Para Linux/macOS use `sh scripts/demo.sh`, `sh scripts/iniciar.sh --local`, `sh scripts/testar.sh` ou `sh scripts/empacotar.sh`. Scripts `.sh` ainda não foram executados nesta estação Windows.

## Início no Windows

1. Instale um **JDK 17+** (por exemplo, Temurin). Abra CMD ou PowerShell e confira `java -version`, `javac -version`.
2. Clone ou extraia o repositório e execute `demo.bat`.
3. Para uma votação local persistente, execute `iniciar.bat --local`.
4. Para sincronizar com Firebase, faça a configuração documentada em [firebase-configuracao.md](firebase-configuracao.md) e execute `iniciar.bat`.

O primeiro início compila sem Maven. O aplicativo cria `dist\urna-cidada.jar`. Dados e logs são procurados ao lado do JAR/projeto. Se a pasta não for gravável, usa `%USERPROFILE%\.urna-cidada\dados` e `.\logs`; a tela mostra o estado e o caminho nos detalhes da gravação. Uma trava evita duas instâncias concorrentes.

## Durante a atividade

- Explique que os candidatos e propostas são fictícios e que esta é uma simulação educativa.
- Uma votação passa por Federal, Estadual, Senador, Governador e Presidente; a simplificação é um voto para Senador.
- A sessão só é salva ao confirmar Presidente. Ao fim aparece FIM; a próxima sessão começa pelo botão.
- Se houver falha de internet, votos continuam no TSV. Não apague `dados/` enquanto houver pendências.
- Faça primeiro uma votação descartável de teste; confirme e remova seus documentos de teste no Firebase antes de usar a oficina.

## Caminhos, dados e logs

- Padrão: `dist\dados\votos-pendentes.tsv` quando executado pelo JAR empacotado. Em modo `--local`: `dados\votos-locais.tsv`.
- Fallback em perfil: `%USERPROFILE%\.urna-cidada\dados`.
- Log rotativo: `logs\urna.log`; pode haver fallback para `%USERPROFILE%\.urna-cidada\logs`.
- A sessão guarda UUID e cinco votos com cargo, tipo, número e data/hora; nenhum dado de identidade.
- Configure caminho personalizado definindo `URNA_HOME` antes de abrir.

## Publicação GitHub

A branch desta entrega é `feature/urna-final`, criada de `develop`. Depois que alterações forem publicadas no repositório, abrir um Pull Request com base `develop`; não enviar para `main`. O programa é Swing desktop, não uma página estática GitHub Pages. Para um site seria necessário reescrever a interface como aplicação web e manter o envio do Firestore protegido por serviço apropriado.

## Conferências pendentes

Ver o checklist em [testes.md](testes.md): máquina real da oficina, teclado e áudio, escala e projetor, fallback de permissões, Firebase real e sistemas Linux/macOS. Não se deve afirmar que esses itens já foram testados.
