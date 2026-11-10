# Regras de votação

Os três cargos aparecem nesta ordem: Deputado Federal, Governador e Presidente. Os nove números são únicos entre todas as candidaturas. A tela de votação mantém a relação de candidatos no canto superior esquerdo. Quando um número válido é reconhecido, exibe o retrato individual centralizado acima dos campos numéricos, com nome e partido abaixo da sequência digitada.

## Escolha em cada cargo

- **Válido:** número completo de uma candidatura cadastrada.
- **Nulo:** número completo sem candidatura correspondente; a tela mostra o aviso antes da confirmação.
- **Branco:** escolher BRANCO sem digitar algarismos.
- **Corrigir:** apaga o número digitado ou desmarca branco.
- **Legenda:** nos cargos proporcionais, confirmar os dois algarismos de um partido registra voto de legenda.
- **Confirmar:** aceita número completo, legenda válida ou branco. Um número incompleto não avança.

Os votos ficam na memória enquanto a pessoa percorre os três cargos. Ao confirmar Presidente, o aplicativo grava os três votos juntos no computador e, se configurado, no Firestore. Toca o sinal de encerramento, fala “Você já votou. Obrigado por participar.” e, após cinco segundos, volta à identificação do próximo participante.

Atalhos: `0–9` para digitar, `B` para branco, `Backspace` para corrigir e `Enter` para confirmar. O botão de som silencia os bipes e a fala.

## Candidaturas

Os números, partidos e propostas abaixo estão centralizados em `src/com/urnacidada/model/Eleicao.java`.

### Deputado Federal · quatro dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 1736 | Pablo Rodrigues | Juventude e Inovação Brasileira (JIB) | Educação: propor mais investimentos nas escolas e universidades públicas. Juventude: defender programas de primeiro emprego e estágio. Tecnologia: incentivar pesquisas científicas e inovação. Transparência: fiscalizar os gastos públicos e cobrar responsabilidade dos governantes. |
| 5730 | Peixonauta da Silva | Vozes da Cidadania (PVC) | Saúde: defender mais recursos para hospitais e unidades do SUS. Proteção à mulher: fortalecer políticas de prevenção à violência. Moradia: defender investimentos em habitação popular. Trabalho: propor medidas de proteção aos trabalhadores e incentivo ao emprego. |
| 9462 | Cebolinha da Mônica | Aliança de Desenvolvimento Sustentável (ADS) | Economia: simplificar impostos e reduzir a burocracia para pequenos negócios. Segurança: propor medidas legislativas de combate ao crime organizado. Infraestrutura: defender investimentos federais em saneamento e transporte. Tecnologia: incentivar o desenvolvimento tecnológico e a modernização dos serviços públicos. |

### Governador · dois dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 14 | The Rock Johnson | Força e Desenvolvimento Estadual (FDE) | Saúde: construir e reformar hospitais regionais. Educação: modernizar escolas estaduais e ampliar cursos técnicos. Segurança: melhorar os equipamentos policiais e ampliar o policiamento. Infraestrutura: recuperar estradas estaduais e melhorar o transporte intermunicipal. |
| 28 | Chico Bento do Rosário | União pelo Futuro (PUF) | Economia: atrair empresas e incentivar o empreendedorismo. Educação: ampliar cursos profissionalizantes para jovens e adultos. Saúde: reduzir as filas de consultas e exames. Turismo: valorizar as atrações naturais e culturais do estado. |
| 37 | Kevin Hart Silva | Desenvolvimento e Justiça Social (DJS) | Agricultura: apoiar pequenos produtores rurais e a agricultura familiar. Saneamento: ampliar o acesso à água tratada e à coleta de esgoto. Segurança: fortalecer o policiamento nas cidades. Esporte: construir e reformar espaços esportivos públicos. |

### Presidente · dois dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 13 | Austin Mendes | Renovação Nacional (PRN) | Educação: ampliar o acesso às universidades e melhorar a infraestrutura das escolas públicas. Saúde: fortalecer o atendimento do SUS e reduzir as filas de consultas e exames. Emprego: incentivar pequenas empresas e criar programas de qualificação profissional. Meio ambiente: combater o desmatamento e investir em energia solar e eólica. |
| 27 | Totem Carvalho | Aliança pelo Progresso (PAP) | Economia: simplificar impostos e incentivar o crescimento das empresas. Segurança: ampliar os investimentos em equipamentos e tecnologia policial. Infraestrutura: modernizar rodovias, ferrovias e aeroportos. Agricultura: oferecer apoio financeiro e tecnológico aos produtores rurais. |
| 39 | Abacatudo Oliveira | Movimento Popular Democrático (MPD) | Moradia: ampliar programas habitacionais para famílias de baixa renda. Educação: investir no ensino integral e na valorização dos professores. Direitos sociais: combater a desigualdade e ampliar políticas de inclusão. Meio ambiente: incentivar energias renováveis e proteger os recursos naturais. |

## Persistência e apuração

Com Firebase, a coleção `participantes/{uid}` registra somente se a conta já votou na rodada atual. O reset administrativo altera `votou` para `false` sem remover o cadastro. Cada sessão fica em `sessoes/{uuid-aleatorio}` e contém uma impressão digital e os três votos. O UID e o identificador informado não fazem parte do documento de votos. As regras permitem uma votação por conta em cada rodada e não permitem que participantes listem sessões.

Votos válidos atualizam `resultados/{cargo}_{numero}`. O total aumenta no mesmo lote atômico que cria a sessão. Voto de legenda, branco e nulo não incrementam contadores de candidatura. Veja a [configuração Firebase](firebase-configuracao.md) para os campos e os IDs de resultado.

Uma conta administrativa separada, marcada por `administradores/{uid}` com `ativo: true`, pode remover as sessões de voto, zerar os nove contadores e rearmar os marcadores dos participantes. As contas do Firebase Authentication e os documentos de participantes são preservados. Documentos técnicos `_controle` mantêm `sessoes` e `participantes` visíveis quando não há votos. A regra não permite que o aplicativo crie ou altere os marcadores de administrador.

Cada computador mantém um histórico local anônimo após sincronizar os votos. A opção **Apuração local offline** mostra, em abas separadas, a contagem por candidatura e voto de legenda, totais de brancos e nulos, e os destaques de mais e menos votado em cada cargo. A consulta não acessa o Firebase, não associa votos a participantes e não reúne dados de outras máquinas.
