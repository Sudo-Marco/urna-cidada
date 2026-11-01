# Testes

## Executar

Windows, com JDK 17 ou superior:

```bat
scripts\testar.bat
```

Linux/macOS:

```sh
sh scripts/testar.sh
```

Os testes são Java puro, sem Maven, JUnit ou acesso externo. Os testes REST usam servidores HTTP locais em `127.0.0.1`.

Na execução desta revisão, **100 testes passaram e nenhum falhou**.

## Cobertura

| Bateria | Verificações |
|---|---|
| `ModeloTestes` | Candidatos, números, partidos, cargos e ordem dos três votos |
| `UrnaServiceTestes` | Válido, legenda, branco, nulo, correção, sequência, gravação e repetição após erro |
| `RepositorioTestes` | Memória, arquivo, formato TSV, duplicidade, arquivo inválido e reserva local |
| `ControladorUrnaTestes` | Estados da tela, mensagens, fluxo de voto e gravação em segundo plano |
| `PainelUrnaTestes` | Painel Swing, botões, estados, som e dimensões de janela |
| `CpfIdentificadorTestes` | Onze algarismos no formato CPF, identificadores inventados e derivação técnica |
| `AutenticacaoTestes` | Cadastro e login por CPF, conta existente e uso do token Firebase |
| `FirestoreTestes` | Marca de participação, sessão UUID, incremento de totais, idempotência e erros HTTP |
| `LocalPrimeiroTestes` | Gravação offline, sincronização posterior e recuo das tentativas |

Os testes de autenticação e Firestore usam respostas simuladas de servidor local; não fazem requisições ao Firebase do projeto. O app-image e o ZIP Windows são gerados por `scripts/empacotar.bat`.
