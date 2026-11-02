# Solução de problemas

| Mensagem ou situação | Causa | Solução |
|---|---|---|
| JDK não encontrado | Java de desenvolvimento não está instalado ou configurado | Instale JDK 17 ou superior e confira `java -version` e `javac -version`. O ZIP de Release já inclui runtime Java. |
| Não há ambiente gráfico | Programa iniciado em servidor ou terminal sem desktop | Abra a urna em uma sessão Windows, macOS ou Linux com interface gráfica. |
| Outra urna já está aberta | Uma instância já usa o mesmo perfil | Feche a janela aberta antes de iniciar outra. |
| Identificador fora do formato | Quantidade diferente de onze algarismos ou caracteres não numéricos | Digite onze algarismos no formato `000.000.000-00`. A simulação não verifica dígitos verificadores. |
| CPF ainda não cadastrado | Não existe uma conta para esse CPF | Escolha **Primeiro acesso** para criar o cadastro. |
| CPF cadastrado em versão anterior | A conta existe no Firebase Authentication | Escolha **Entrar**. Se escolher **Primeiro acesso**, a versão atual tenta reutilizar essa conta. |
| CPF já cadastrado | A conta já existe no Firebase Authentication e o Firebase não aceita outra conta com o mesmo identificador | A urna entra na conta existente quando **Primeiro acesso** é escolhido; também é possível selecionar **Entrar** diretamente. |
| CPF já concluiu uma votação | A conta já possui a marca de participação no Firestore | Uma conta participa uma vez. Cada pessoa deve usar seu próprio cadastro. |
| Votação aguardando sincronização | A sessão está no arquivo local e ainda não chegou ao Firestore | Conecte à internet e abra a urna novamente com o mesmo CPF. |
| Configuração Firebase incompleta | Um dos campos ou variáveis de configuração não foi preenchido | Confira `firebase.projectId` e `firebase.apiKey` em `urna.properties`. |
| Configuração Firebase inválida | Chave, projeto, provedor Authentication ou regras não correspondem | Ative Email/Password, confira os dados do projeto e publique `firestore.rules`. A sessão fica no arquivo local quando a rede falha. |
| Acesso administrativo negado | A conta não tem `administradores/{uid}` com `ativo: true`, ou as regras ainda não foram publicadas | Use a conta administrativa criada no Firebase e publique as regras atualizadas. |
| Console Firestore mostra somente `administradores` | A versão atual ainda não restaurou os documentos estruturais ou as regras novas não foram publicadas | Publique `firestore.rules`, abra `scripts\administrar.bat` e escolha **Restaurar estrutura do Firestore (preservar votos)**. Os contadores atuais serão criados com zero; votos que já foram apagados não podem ser recuperados. |
| Sem conexão com a nuvem | Internet indisponível ou Firebase temporariamente sem resposta | Os votos ficam locais e o sincronizador tenta novamente com intervalos crescentes. |
| Arquivo local não pode ser gravado | Pasta do aplicativo sem permissão de escrita | Defina `URNA_HOME` para uma pasta gravável ou use `%USERPROFILE%\.urna-cidada\`. |
| Som ou fala não toca | Saída de áudio ausente, desativada ou silenciada | Ative o som no botão da tela e confira o dispositivo padrão do Windows. A votação continua disponível sem áudio. |

O log fica em `logs/urna.log` ou na pasta `%USERPROFILE%\.urna-cidada\logs`. Ele registra códigos HTTP e classes de erro, sem a chave de API nem o corpo das requisições.
