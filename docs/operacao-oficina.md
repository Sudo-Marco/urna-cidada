# Operação técnica da oficina

> Guia para o mediador preparar e conduzir a urna. O roteiro pedagógico está em [material-oficina.md](material-oficina.md). Verificações em [testes.md](testes.md).

## Modos de gravação

| Comando | Para que serve | Onde ficam os votos |
|---|---|---|
| `scripts\demo.bat` | Demonstração e treino | Só na memória (somem ao fechar) |
| `scripts\rodar.bat --local` | Oficina **sem** internet | `dados\votos-locais.tsv` |
| `scripts\rodar.bat` | Oficina com Firestore | Nuvem, com reserva em `dados\votos-pendentes.tsv` se a rede falhar |

Se `FIRESTORE_PROJECT_ID` não estiver definido, ou o Firestore não puder ser iniciado, a urna **avisa** e usa o arquivo local. Ela não deixa de abrir.

## Preparar o Firestore (uma vez)

1. Crie um projeto no [console do Firebase](https://console.firebase.google.com) e habilite o **Cloud Firestore**.
2. Instale a [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) e autentique o computador:
   ```text
   gcloud auth application-default login
   ```
3. Informe o projeto antes de abrir a urna:
   ```text
   set FIRESTORE_PROJECT_ID=meu-projeto          (CMD)
   $env:FIRESTORE_PROJECT_ID="meu-projeto"       (PowerShell)
   ```
4. Abra a urna com `scripts\rodar.bat`, vote e confira no console do Firebase se o documento apareceu em `sessoes`.

A conta usada precisa ter permissão de escrita no Firestore do projeto. Esse tipo de acesso funciona por permissões da conta (IAM), e não pelas regras de segurança do Firestore.

## Segurança

- **Nunca** envie ao GitHub chaves, `serviceAccountKey.json`, arquivos `.env` ou a pasta `dados/` (o `.gitignore` já bloqueia).
- Não distribua credenciais pessoais para os computadores da escola. Prefira levar o **seu** computador, já autenticado.
- A pasta `dados/` guarda apenas votos anônimos, mas ainda assim não deve ser publicada.

## Antes do dia da oficina

- [ ] Testar a urna **no equipamento que será usado**, com o projetor ligado.
- [ ] Decidir o modo: Firestore (precisa de internet) ou `--local`.
- [ ] Se for usar Firestore, testar a gravação real e o plano B sem internet ([testes.md](testes.md)).
- [ ] Confirmar que o JDK 17 ou superior e o Maven funcionam nesse computador (`java -version`, `mvn -version`).
- [ ] Levar cabo/adaptador do projetor e um computador reserva.
- [ ] Combinar com a escola autorização, local e horário.

## No dia

- [ ] Abrir a urna **antes** de os participantes chegarem e fazer uma votação de teste.
- [ ] Depois do teste, apague a pasta `dados\` se não quiser misturar os votos de teste com os reais.
- [ ] Explicar que a urna é uma simulação e que os candidatos são fictícios.
- [ ] Ao final de cada votação, a tela mostra "FIM"; o botão "Iniciar nova votação" prepara o próximo participante.

## Se algo der errado

| Problema | O que fazer |
|---|---|
| A urna não abre | Conferir `java -version`; tentar `scripts\demo.bat` |
| Aviso de configuração do Firestore | Seguir com o arquivo local, ou conferir `FIRESTORE_PROJECT_ID` e o login do `gcloud` |
| Aviso âmbar ao final ("guardados neste computador") | Sem problema: os votos ficaram na reserva e serão enviados quando houver internet |
| Texto pequeno demais no projetor | Maximizar a janela |
| Erro vermelho na hora de gravar | O participante pode apertar CONFIRMA de novo |
| Nada funciona | Abrir `scripts\demo.bat` para continuar a atividade sem gravar e resolver depois |
