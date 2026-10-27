# Testes

> Como o sistema é verificado. Relacionado a [requisitos.md](requisitos.md).

## Como executar

```text
scripts\testar.bat
```

Não precisa de Maven nem de internet: o script compila só o que os testes usam e roda tudo. O resultado esperado termina com:

```text
67 passaram, 0 falharam
```

Se algum teste falhar, o script mostra `FALHA` com o nome do teste e o motivo.

## Estratégia

Os testes são **Java puro**, sem JUnit, para o projeto não depender de bibliotecas externas. Ficam em `tests/com/urnacidada/testes/`, organizados por assunto, com um executor único (`ExecutarTestes`) e uma mini-biblioteca (`MiniTeste`).

Os testes de tela rodam **sem abrir janela** (modo *headless*). O que depende de ver a janela de verdade, do Firestore real ou do projetor está no [roteiro manual](#roteiro-de-verificação-manual) abaixo.

## O que é coberto

| Bateria | Testes | O que verifica |
|---|---:|---|
| `ModeloTestes` | 11 | Validação de candidatos, cargos, eleição e voto; herança e polimorfismo |
| `UrnaServiceTestes` | 19 | Regras de votação: digitar, branco, corrigir, confirmar, nulo, fim, repetir gravação |
| `RepositorioTestes` | 13 | Memória, arquivo local, reserva automática e sincronização |
| `ControladorUrnaTestes` | 18 | O que a tela mostra em cada situação, cores das mensagens, gravação em segundo plano |
| `PainelUrnaTestes` | 6 | A tela monta e desenha todos os estados; cada botão dispara a ação certa; nenhum texto fica cortado |
| **Total** | **67** | |

## Requisito → teste

| Requisito | Verificado por |
|---|---|
| RF01 Iniciar votação | `ControladorUrnaTestes`: "ao iniciar…", "nova votação depois do FIM…" |
| RF02, RF03 Dois cargos em sequência | `UrnaServiceTestes`: "confirmar avança para o segundo cargo"; `ModeloTestes`: "eleição de exemplo" |
| RF04 Entrada do voto | `PainelUrnaTestes`: "cada botão da tela dispara a ação certa" |
| RF05 Validar entrada | `UrnaServiceTestes`: letras, excesso de dígitos, confirmar incompleto |
| RF06 Confirmar | `UrnaServiceTestes`, `ControladorUrnaTestes` |
| RF07 Corrigir, branco, nulo | `UrnaServiceTestes`: branco, nulo, corrigir; `ControladorUrnaTestes`: modos BRANCO e NÚMERO ERRADO |
| RF08 Encerrar | `ControladorUrnaTestes`: "fluxo completo termina em FIM…" |
| RF09 Persistência | `RepositorioTestes` (memória e arquivo); **Firestore real: só no roteiro manual** |
| RF11 Reserva local | `RepositorioTestes`: grupo "reserva automática"; `ControladorUrnaTestes`: "sem internet e com reserva…" |
| RNF02 Robustez | Todos os testes de erro e de falha de gravação |
| RNF03 Privacidade | Por inspeção: a classe `Voto` só tem cargo, tipo, número e data/hora (nenhum campo identifica o participante) |

## Verificação dos testes

Para conferir que os testes realmente detectam erros, foram introduzidos defeitos de propósito no código (mensagem de sucesso mostrada como erro, reserva que não guarda o voto, gravação que duplica, número inexistente que deixa de ser nulo). Cada defeito foi detectado por pelo menos um teste.

## Roteiro de verificação manual

Execute com a janela aberta (`scripts\demo.bat`) e marque o que foi conferido. Os itens marcados foram comprovados (com a data); os demais continuam pendentes.

### Tela e teclado

- [x] A janela abre com título, aviso "SIMULAÇÃO EDUCATIVA · NÃO É UMA URNA OFICIAL" e teclado completo (24/10/2026).
- [ ] Nenhum texto aparece cortado em tela cheia e em janela pequena. *(Em 25/10 o aviso da reserva local apareceu cortado; foi corrigido e há um teste automático. Conferir de novo na janela real.)*
- [ ] Votar Vereador `12345` mostra "Ana Exemplo", partido e a explicação do cargo.
- [ ] Digitar `99999` mostra "NÚMERO ERRADO / VOTO NULO" em vermelho e o aviso em âmbar.
- [ ] BRANCO sem digitar mostra "VOTO EM BRANCO".
- [ ] CORRIGE limpa a entrada.
- [ ] CONFIRMA com número incompleto mostra erro em vermelho.
- [ ] Prefeito `22` mostra "Elisa Educativa" e a explicação do cargo majoritário.
- [ ] Ao final aparecem "FIM", a mensagem verde e o botão "Iniciar nova votação".
- [ ] "Iniciar nova votação" volta ao Vereador.
- [ ] Teclado do computador: números, `B` (branco), `Backspace` (corrige) e `Enter` (confirma).
- [ ] "Dicas contra desinformação" abre a janela de dicas.
- [ ] Fechar a janela encerra o programa.

### Gravação

- [ ] `scripts\rodar.bat --local`: depois de votar, existe `dados\votos-locais.tsv` com a sessão.
- [ ] Modo padrão **sem** `FIRESTORE_PROJECT_ID`: aparece o aviso de configuração e a urna usa o arquivo local.

### Firestore real

- [x] Projeto criado no Firebase, Firestore habilitado e autenticação configurada ([operacao-oficina.md](operacao-oficina.md)) (25/10/2026).
- [x] Depois de votar, o documento aparece em `sessoes` no console do Firebase, com dois votos e sem dados pessoais (25/10/2026).
- [x] Sem internet: a votação termina, aparece o aviso âmbar e existe `dados\votos-pendentes.tsv` (25/10/2026).
- [x] Com a internet de volta: ao votar de novo (ou reabrir a urna), a sessão pendente aparece no Firebase e o arquivo de pendentes fica vazio (25/10/2026).

### Equipamento da oficina

- [ ] A urna abre e funciona no computador que será usado na escola.
- [ ] O texto é legível no projetor ou na tela usada.
