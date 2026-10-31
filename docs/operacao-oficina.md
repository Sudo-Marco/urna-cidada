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
| `scripts\empacotar.bat` | ZIP portátil Windows x64 com runtime Java incluído |

Para Linux/macOS use `sh scripts/demo.sh`, `sh scripts/iniciar.sh --local`, `sh scripts/testar.sh` ou `sh scripts/empacotar.sh`. Scripts `.sh` ainda não foram executados nesta estação Windows.

## Início no Windows

### Para quem vai usar a urna

1. Abra [Releases do GitHub](https://github.com/Sudo-Marco/urna-cidada/releases) e baixe `UrnaCidada-Windows-x64.zip`.
2. Extraia a pasta `UrnaCidada` para Documentos ou Área de Trabalho.
3. Abra `UrnaCidada.exe`. O runtime Java já vem no pacote: não precisa instalar JDK, VS Code, Git ou Maven.
4. Sem `urna.properties`, a urna salva os votos localmente naquele computador. Para sincronizar no Firestore, a equipe precisa copiar uma configuração autorizada para a pasta `app` e seguir [firebase-configuracao.md](firebase-configuracao.md).

### Para quem desenvolve/publica

1. Instale um **JDK 17+** e confira `java -version`, `javac -version` e `jpackage --version`.
2. Clone ou extraia o repositório e execute `scripts\testar.bat`.
3. Execute `scripts\empacotar.bat`. Ele cria `dist\UrnaCidada-Windows-x64.zip` com o runtime incluído.
4. Uma tag `v*` no GitHub dispara `.github/workflows/publicar-release.yml`, que testa, cria o ZIP e o anexa ao Release.

Para que o ZIP automático também envie ao Firestore, configure no repositório em **Settings → Secrets and variables → Actions** a variável `FIREBASE_PROJECT_ID` e o segredo `FIREBASE_API_KEY`. Sem os dois, o pacote continua funcional e grava localmente. O workflow nunca registra os valores no log. A chave de API é pública para aplicativos cliente; antes de distribuir, restrinja-a à API necessária no Google Cloud e monitore o uso.

O primeiro início do código-fonte compila sem Maven. No pacote portátil, a compilação já foi feita e o runtime Java está incluído. Dados e logs ficam ao lado do aplicativo. Se a pasta não for gravável, os dados e logs usam `%USERPROFILE%\.urna-cidada\`; a tela mostra o estado e o caminho. Uma trava evita duas instâncias concorrentes.

## Durante a atividade

- Explique que os candidatos e propostas são fictícios e que esta é uma simulação educativa.
- Uma votação passa por Federal, Estadual, Senador, Governador e Presidente; a simplificação é um voto para Senador. Os números de demonstração seguem o tamanho do cargo: Meia Bomba (Estadual, 81001), Curupira e Peter Pardo (Senador, 820 e 830), Zé Gotinha (Governador, 84) e Beto (Presidente, 85).
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

A branch padrão do repositório é `URNA`. Uma tag com prefixo `v` aciona o workflow para gerar um Release Windows. O programa é Swing desktop e não abre dentro do navegador do GitHub; o pacote portátil permite baixar, extrair e executar sem instalar Java.

## Conferências pendentes

Ver o checklist em [testes.md](testes.md): máquina real da oficina, teclado e áudio, escala e projetor, fallback de permissões, Firebase real e sistemas Linux/macOS. Não se deve afirmar que esses itens já foram testados.
