# Solução de problemas

| Mensagem ou sintoma | Causa provável | O que fazer |
|---|---|---|
| Java 17 ou superior não encontrado | JDK ausente ou `PATH` antigo | Instale Temurin JDK 17+, feche/reabra o terminal e rode `java -version`, `javac -version` |
| Não foi encontrado um ambiente gráfico | Execução em servidor/headless, SSH sem encaminhamento gráfico | Inicie pelo desktop Windows/macOS/Linux com ambiente gráfico |
| Outra urna já está aberta | Trava exclusiva de instância ativa | Feche a outra janela normalmente e tente novamente; se travou, confirme no Gerenciador de Tarefas antes de reiniciar |
| Não há permissão para gravar os dados | Pasta ao lado do JAR e fallback do perfil indisponíveis | Escolha pasta gravável para `URNA_HOME` ou corrija permissão; faça backup antes de mover dados |
| “Modo local · configure urna.properties…” | Configuração ausente | Normal se deseja votar offline. Para sincronizar, siga [Firebase configuração](firebase-configuracao.md) |
| “Configuração do Firebase incompleta” | Só um campo foi preenchido ou arquivo não pode ser lido | Confira `firebase.projectId` e `firebase.apiKey`; não cole a chave em issue ou chat |
| “Configuração de exemplo não preenchida” | Ainda usa `SEU_PROJECT_ID`/`SUA_CHAVE_API` | Substitua ambos pelos dados de teste do seu projeto |
| “Configuração do Firebase inválida” | ID/chave inválidos, API não ativada, chave restrita incorretamente ou regras rejeitam os dados | Reveja console, restrições da API e `firestore.rules`. O voto fica no arquivo local |
| “Sem conexão… N votos guardados neste computador” | Rede indisponível, timeout, limitação ou erro 5xx | Continue a atividade; confira o caminho da dica no rodapé; aguarde conexão e retentativa |
| “Firebase temporariamente indisponível” | REST recebeu timeout, 408, 429 ou 5xx | Aguarde; as tentativas aumentam de 30 s até 5 min e os dados locais permanecem |
| Erro HTTP 400/401/403/404 | Projeto, chave, Firestore ou regras inválidos/incompatíveis | Corrija configuração no console. O cliente para de tentar automaticamente enquanto o processo está aberto |
| TSV existe mas fila não esvazia | Linha inválida/corrompida ou erro persistente no Firestore | Preserve uma cópia do arquivo; consulte `logs/urna.log`; não edite manualmente sem cópia |
| Som não toca | Sem dispositivo, bloqueio do sistema ou áudio desligado | Clique “Som: ligado”. A urna funciona sem som e falha de áudio é ignorada |
| Texto pequeno ou cortado | Resolução, DPI ou tamanho de janela | Maximize; confira escala em 100%, 125% e 150%; relate tamanho físico e escala para análise |
| `scripts\testar.bat` falha | JDK incompatível, fonte alterada ou erro de teste | Leia o primeiro `FALHA`; confirme `javac -version`; não apague `dados/` como tentativa de correção |

O log é rotativo em `logs/urna.log` (ou pasta do perfil). Ele registra classes de erro e códigos HTTP, nunca a chave API nem corpo/requisição. Antes de compartilhar o log, ainda assim confira se não há informação local inserida manualmente.
