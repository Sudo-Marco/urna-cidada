# Casos de uso

## Atores

| Ator | Papel |
|---|---|
| Participante | Identifica-se pelo CPF quando Firebase está ativo e vota nos cinco cargos |
| Mediador | Abre a aplicação e orienta a simulação |
| Firebase | Autentica o cadastro e recebe as sessões e contagens, quando configurado |

## Identificar participante e votar

1. Na tela de identificação, escolha **Primeiro acesso** para cadastrar CPF e senha ou **Entrar** para usar um cadastro existente.
2. A urna confere se a conta já possui uma marca de participação. Se já votou, o fluxo termina com uma mensagem.
3. O participante escolhe um candidato, voto de legenda, branco ou nulo em cada cargo e confirma sua escolha.
4. Depois de Presidente, a urna grava a votação localmente e envia a sessão e os incrementos de contagem juntos ao Firestore, quando disponível.
5. A urna toca um sinal, fala a mensagem de encerramento e retorna à identificação após cinco segundos.

Sem Firebase, o programa pode rodar em modo local ou em memória. Esses modos não identificam a pessoa nem compartilham um bloqueio de votos entre computadores.

## Sincronizar votação

O aplicativo salva primeiro em arquivo TSV. Quando há conexão, o sincronizador tenta criar a marca do participante, a sessão aleatória e os incrementos dos votos válidos. Se a rede falhar, mantém a sessão local para tentar novamente. Se outra sessão já usou a conta, o Firestore rejeita a gravação.

Nos cargos proporcionais, os dois dígitos de partido podem ser confirmados como legenda. Voto branco não contém número; número completo sem candidatura é nulo. Mais detalhes estão em [regras-de-votacao.md](regras-de-votacao.md).
