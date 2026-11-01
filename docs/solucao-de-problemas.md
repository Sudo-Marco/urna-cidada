# Solução de problemas

| Mensagem ou situação | Causa | Solução |
|---|---|---|
| JDK não encontrado | Java de desenvolvimento não está instalado ou configurado | Instale JDK 17 ou superior e confira `java -version` e `javac -version`. O ZIP de Release já inclui runtime Java. |
| Não há ambiente gráfico | Programa iniciado em servidor ou terminal sem desktop | Abra a urna em uma sessão Windows, macOS ou Linux com interface gráfica. |
| Outra urna já está aberta | Uma instância já usa o mesmo perfil | Feche a janela aberta antes de iniciar outra. |
| CPF inválido | Quantidade de dígitos ou dígitos verificadores incorretos | Digite os 11 algarismos do CPF, com ou sem pontos e traço. |
| CPF ou senha incorretos | O cadastro não corresponde aos dados informados | Confira o CPF e use a senha definida no primeiro acesso. |
| CPF já cadastrado | O cadastro já existe no Firebase Authentication | Escolha **Entrar** em vez de **Primeiro acesso**. |
| CPF já concluiu uma votação | A conta já possui a marca de participação no Firestore | Uma conta participa uma vez. Cada pessoa deve usar seu próprio cadastro. |
| Votação aguardando sincronização | A sessão está no arquivo local e ainda não chegou ao Firestore | Conecte à internet e abra a urna novamente com o mesmo CPF. |
| Configuração Firebase incompleta | Um dos campos ou variáveis de configuração não foi preenchido | Confira `firebase.projectId` e `firebase.apiKey` em `urna.properties`. |
| Configuração Firebase inválida | Chave, projeto, provedor Authentication ou regras não correspondem | Ative Email/Password, confira os dados do projeto e publique `firestore.rules`. A sessão fica no arquivo local quando a rede falha. |
| Sem conexão com a nuvem | Internet indisponível ou Firebase temporariamente sem resposta | Os votos ficam locais e o sincronizador tenta novamente com intervalos crescentes. |
| Arquivo local não pode ser gravado | Pasta do aplicativo sem permissão de escrita | Defina `URNA_HOME` para uma pasta gravável ou use `%USERPROFILE%\.urna-cidada\`. |
| Som ou fala não toca | Saída de áudio ausente, desativada ou silenciada | Ative o som no botão da tela e confira o dispositivo padrão do Windows. A votação continua disponível sem áudio. |

O log fica em `logs/urna.log` ou na pasta `%USERPROFILE%\.urna-cidada\logs`. Ele registra códigos HTTP e classes de erro, sem a chave de API nem o corpo das requisições.
