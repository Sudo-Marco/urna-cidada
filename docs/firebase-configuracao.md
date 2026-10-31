# Configurar o Firebase para envio opcional

A urna funciona sem Firebase. No modo padrão, o arquivo local é sempre gravado primeiro; este guia só ativa uma cópia anônima no Firestore. Use um projeto descartável de teste. O aplicativo é educativo, não é um sistema eleitoral oficial.

## 1. Criar projeto e banco

1. Abra [Firebase Console](https://console.firebase.google.com/) e crie um projeto de teste.
2. No projeto, abra **Firestore Database** e crie um banco Cloud Firestore. Escolha a localização apropriada ao exercício; a região não pode ser trocada facilmente depois da criação.
3. O cliente usa o banco `(default)` e cria documentos na coleção `sessoes`.

## 2. Publicar regras

1. Abra **Firestore Database → Regras** no console.
2. Copie o conteúdo do arquivo [`firestore.rules`](../firestore.rules) para o editor do console.
3. Clique em **Publicar** e confira que o console não apontou erro.
4. Confirme que não existe outra regra ampla que permita acesso em coleções. As regras deste projeto só permitem `create` anônimo com um campo `votos` contendo cinco mapas de formato permitido; leitura, atualização e remoção são negadas.

Não altere as regras para `allow read, write: if true`. Regras no Firestore são aplicadas no servidor e condições podem validar campos. Consulte a [documentação oficial de regras](https://firebase.google.com/docs/firestore/security/rules-fields).

## 3. Criar e restringir a chave de API

1. No Google Cloud Console do mesmo projeto, abra **APIs e serviços → Credenciais** e localize/crie uma chave de API para a aplicação.
2. Em **Restrições de API**, limite a chave à **Cloud Firestore API**. Uma aplicação desktop não tem origem web fixa; restrição por referenciador web não é apropriada para esse cliente.
3. Acompanhe uso e cotas na consola do Google Cloud; restrinja/rotacione a chave se houver abuso. Veja [boas práticas oficiais para chaves de API](https://cloud.google.com/docs/authentication/api-keys-best-practices).

A chave de API é pública por natureza: o cliente a usa numa requisição HTTP e não há login de usuário. As regras negam leitura/alteração/exclusão, mas uma pessoa com o aplicativo e chave pode tentar criar sessões até os limites/cotas do projeto. Regras não provam que cada voto veio de uma pessoa autorizada, nem protegem contra automação/abuso. Para serviço público ou dados importantes, não use este desenho; seria necessário backend autenticado, controles de abuso e auditoria.

## 4. Preencher a configuração local

1. Copie `urna.properties.example` para `urna.properties` na mesma pasta do JAR (`dist/` depois de empacotar; no projeto, na raiz).
2. Preencha os dois valores sem aspas:

```properties
firebase.projectId=ID_REAL_DO_PROJETO
firebase.apiKey=CHAVE_REAL_DA_API
```

3. Não comite `urna.properties` ao repositório. O arquivo está em `.gitignore`. Para o pacote público das Releases, use a configuração de segredos descrita abaixo e restrinja a chave de API antes de distribuir.
4. Opcionalmente use as variáveis `URNA_FIRESTORE_PROJECT_ID` e `URNA_FIRESTORE_API_KEY`; elas têm precedência. As antigas `FIRESTORE_PROJECT_ID` e `FIRESTORE_API_KEY` também são aceitas.
5. Inicie `iniciar.bat` (modo padrão). Sem configuração válida a urna continua local.

## 5. Configurar o pacote automático do GitHub

Para o ZIP publicado em **Releases** sincronizar com o Firestore sem configuração manual em cada computador, o responsável pelo repositório cadastra uma vez em **Settings → Secrets and variables → Actions**:

1. Em **Variables**, crie `FIREBASE_PROJECT_ID` com o ID do projeto.
2. Em **Secrets**, crie `FIREBASE_API_KEY` com a chave Web do aplicativo.
3. Restrinja a chave à Cloud Firestore API no Google Cloud antes de distribuir. A configuração de cliente dentro do ZIP público pode ser extraída; as regras do banco continuam sendo a proteção de dados.
4. Publique uma tag `v*` para iniciar o workflow. Sem esses valores, o ZIP continua funcionando e grava os votos localmente.

O workflow grava os valores apenas durante a geração do pacote e não os adiciona ao repositório nem os imprime nos logs. O pacote contém somente configuração pública de cliente, nunca credenciais de serviço.

## 6. Testar e limpar

1. Use somente uma máquina/projeto de teste e faça uma sessão de simulação.
2. Confira **Firestore Database → Dados → sessoes**. Deve haver um documento cujo ID é o UUID da sessão, com cinco votos anônimos.
3. Para simular offline, desligue a rede antes do último CONFIRMA e complete a votação. Verifique `dados/votos-pendentes.tsv`; religue a rede e aguarde o reenvio automático. O TSV só é removido depois de resposta de sucesso (200 ou 409).
4. No console, selecione a coleção/documento de teste e exclua os registros quando terminar. Confirme que não há participantes reais; nenhum dado pessoal deve ser inserido.

A REST API e o payload tipado são baseados na [referência oficial de documentos](https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents) e no [guia REST do Firestore](https://firebase.google.com/docs/firestore/use-rest-api). Em 30/10/2026, o usuário informou que configurou o Firebase e salvou uma sessão de teste; a tela mostrou `Firebase configurado` e `Online · tudo enviado`. A conferência independente do documento no console e o teste de reconexão após uma queda real de internet continuam pendentes.
