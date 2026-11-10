# Operação da urna

## Preparação

Para compilar e rodar os scripts, use JDK 17 ou superior. A versão distribuída em ZIP para Windows já inclui o runtime Java. Maven, navegador e VS Code não são necessários para usar o pacote.

| Comando | Resultado |
|---|---|
| `demo.bat` ou `scripts\demo.bat` | Demonstração em memória |
| `iniciar.bat` | Compila o código atual e abre a urna a partir da pasta do projeto |
| `scripts\iniciar.bat --local` | Votos em arquivo local |
| `scripts\iniciar.bat` | Firebase configurado, ou modo local se não houver configuração |
| `scripts\administrar.bat` | Apuração local offline ou reinício de votação no Firestore pela conta administrativa |
| `scripts\testar.bat` | Suíte de testes Java |
| `scripts\empacotar.bat` | Pacote Windows x64 com runtime incluído |

Em Linux/macOS, use os scripts equivalentes em `scripts/` com `sh`.

No terminal integrado do VS Code, abra a pasta do projeto e execute `cmd /c iniciar.bat`. A janela de identificação é trazida para a frente automaticamente.

## Início rápido no Windows

1. Baixe `UrnaCidada-Windows-x64.zip` em [Releases](https://github.com/Sudo-Marco/urna-cidada/releases).
2. Extraia o ZIP inteiro para Documentos ou Área de Trabalho.
3. Abra `UrnaCidada.exe`.
4. Com Firebase, identifique cada participante pelo CPF. No primeiro acesso, escolha **Primeiro acesso**; nos demais, **Entrar**.

Sem configuração Firebase, a urna funciona no computador e não identifica participantes entre máquinas. Para configurar Authentication e Cloud Firestore, consulte [firebase-configuracao.md](firebase-configuracao.md).

Para reiniciar uma votação, execute `scripts\administrar.bat` e entre com uma conta administrativa separada. O UID precisa ter um documento `administradores/{uid}` com `ativo: true`. O reset remove sessões, zera os nove contadores e altera os marcadores `votou` para `false`; mantém contas de CPF, documentos de participantes, administradores e coleções do Firestore. Os votos locais deste computador também são removidos.

O mesmo menu oferece **Apuração local offline**. Na primeira utilização, entre uma vez com a conta administrativa enquanto há conexão para preparar o acesso local. Depois, a apuração pode ser aberta sem rede usando a mesma conta e senha. O painel organiza em abas os cargos e mostra votos por candidatura, votos de legenda, brancos, nulos, totais e os mais e menos votados. A consulta agrega os votos e não exibe dados de participantes. O ZIP do Windows inclui `UrnaCidadaAdmin.exe`, além do iniciador principal.

## Durante a votação

Explique que esta é uma simulação educativa. Os cargos são Deputado Federal, Governador e Presidente, nessa ordem. Os números e partidos seguem a lista em [regras-de-votacao.md](regras-de-votacao.md).

Ao confirmar Presidente, a urna salva a sessão, toca o sinal, diz “Você já votou. Obrigado por participar.” e, em cinco segundos, retorna à identificação do próximo participante. A tecla `Enter` confirma, `B` escolhe branco, `Backspace` corrige e as teclas numéricas inserem o voto.

Os cargos, números e nomes dos candidatos ficam visíveis em uma lista fixa no canto superior esquerdo da tela.

A fala usa a síntese de voz do Windows e seleciona uma voz em português do Brasil quando disponível.

## Arquivos e dados

- Dados principais: pasta `dados` ao lado do aplicativo.
- Logs: pasta `logs`, com rotação automática.
- Se a pasta do aplicativo não permitir gravação, o programa usa `%USERPROFILE%\.urna-cidada\`.
- A urna mantém uma trava de instância para impedir duas janelas simultâneas no mesmo perfil.
- `urna.properties` guarda a configuração Firebase e não deve ser incluído no repositório.

## Publicação

Uma tag `v*` inicia o workflow `.github/workflows/publicar-release.yml`. A ação executa os testes, cria um app-image para Windows x64 com runtime Java e publica o ZIP no GitHub Releases. Os valores Firebase podem ser fornecidos nas variáveis e secrets de Actions descritos em [firebase-configuracao.md](firebase-configuracao.md).
