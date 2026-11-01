# Regras de votação

Os cinco cargos aparecem nesta ordem: Deputado Federal, Deputado Estadual, Senador, Governador e Presidente. A escolha de Senador está simplificada a um voto. Os partidos e programas de candidatura compõem a simulação educativa.

## Escolha em cada cargo

- **Válido:** número completo de uma candidatura cadastrada.
- **Nulo:** número completo sem candidatura correspondente; a tela mostra o aviso antes da confirmação.
- **Branco:** escolher BRANCO sem digitar algarismos.
- **Corrigir:** apaga o número digitado ou desmarca branco.
- **Legenda:** nos cargos proporcionais, confirmar os dois algarismos de um partido registra voto de legenda.
- **Confirmar:** aceita número completo, legenda válida ou branco. Um número incompleto não avança.

Os votos ficam na memória enquanto a pessoa percorre os cinco cargos. Ao confirmar Presidente, o aplicativo grava os cinco votos juntos no computador e, se configurado, no Firestore. Toca o sinal de encerramento, fala “Você já votou. Obrigado por participar.” e, após cinco segundos, volta à identificação do próximo participante.

Atalhos: `0–9` para digitar, `B` para branco, `Backspace` para corrigir e `Enter` para confirmar. O botão de som silencia os bipes e a fala.

## Candidaturas

Os números, partidos e propostas abaixo estão centralizados em `src/com/urnacidada/model/Eleicao.java`.

### Deputado Federal · quatro dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 8101 | Ari Lume | Partido Horizonte Comunitário (fictício) | Ampliar bibliotecas com horários acessíveis; divulgar metas e gastos em linguagem simples. |
| 8202 | Bela Nébula | Partido Raízes do Amanhã (fictício) | Apoiar formação técnica em escolas públicas; publicar indicadores de acesso à saúde. |
| 8303 | Cael Brisa | Partido Caminhos Abertos (fictício) | Incentivar transporte público de baixa emissão; ampliar dados abertos sobre serviços. |
| 1111 | Pedro Henrique Costa | Juventude e Inovação Brasileira (JIB) | Investir em escolas e universidades; primeiro emprego e estágio; pesquisa e inovação; fiscalização dos gastos públicos. |
| 2222 | Beatriz Rodrigues | Vozes da Cidadania (PVC) | Recursos para hospitais e SUS; prevenção à violência; habitação popular; proteção ao trabalho e incentivo ao emprego. |
| 3333 | André Martins | Aliança de Desenvolvimento Sustentável (ADS) | Simplificar impostos; combater o crime organizado; investir em saneamento e transporte; modernizar serviços públicos. |

### Deputado Estadual · cinco dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 81001 | Meia Bomba | Partido Horizonte Comunitário (fictício) | Apoiar bibliotecas comunitárias e leitura; publicar metas anuais de serviços estaduais. |

### Senador · três dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 820 | Curupira | Partido Raízes do Amanhã (fictício) | Ampliar bibliotecas e espaços de leitura; apoiar formação técnica acessível. |
| 830 | Peter Pardo | Partido Caminhos Abertos (fictício) | Planejar trajetos seguros para pedestres; integrar melhor o transporte público. |
| 833 | Ivo Pedra-Mansa | Partido Caminhos Abertos (fictício) | Planejar trajetos seguros para pedestres; integrar melhor o transporte público. |

### Governador · dois dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 84 | Zé Gotinha | Partido Cuidado Coletivo (fictício) | Ampliar ações educativas de prevenção em saúde; publicar informações de serviços em linguagem simples. |
| 82 | Luma Cedro-Doce | Partido Raízes do Amanhã (fictício) | Planejar trajetos seguros para pedestres; integrar melhor o transporte público. |
| 83 | Nilo Serra-Clara | Partido Caminhos Abertos (fictício) | Incentivar eficiência energética em serviços públicos; ampliar áreas verdes com manutenção planejada. |
| 44 | Gabriel Santos | Força e Desenvolvimento Estadual (FDE) | Construir e reformar hospitais; modernizar escolas e ampliar cursos técnicos; melhorar equipamentos e policiamento; recuperar estradas e transporte intermunicipal. |
| 55 | Camila Ferreira | União pelo Futuro (PUF) | Atrair empresas; ampliar cursos profissionalizantes; reduzir filas de consultas e exames; valorizar atrações naturais e culturais. |
| 66 | Lucas Almeida | Desenvolvimento e Justiça Social (DJS) | Apoiar agricultura familiar; ampliar acesso à água tratada e coleta de esgoto; fortalecer o policiamento; reformar espaços esportivos. |

### Presidente · dois dígitos

| Número | Candidato | Partido | Propostas |
|---:|---|---|---|
| 81 | Pia Sol-Nascente | Partido Horizonte Comunitário (fictício) | Incentivar eficiência energética em serviços públicos; ampliar áreas verdes com manutenção planejada. |
| 85 | Beto | Partido Futuro Compartilhado (fictício) | Apoiar espaços de convivência, esporte e cultura; divulgar metas e resultados dos serviços públicos. |
| 83 | Tali Nuvem-Mansa | Partido Caminhos Abertos (fictício) | Ampliar bibliotecas e espaços de leitura; apoiar formação técnica acessível. |
| 11 | Rafael Mendes | Renovação Nacional (PRN) | Ampliar acesso às universidades e melhorar escolas públicas; fortalecer o SUS; qualificar profissionais; combater o desmatamento e investir em energia solar e eólica. |
| 22 | Eduardo Carvalho | Aliança pelo Progresso (PAP) | Simplificar impostos; ampliar tecnologia policial; modernizar rodovias, ferrovias e aeroportos; apoiar produtores rurais. |
| 33 | Mariana Oliveira | Movimento Popular Democrático (MPD) | Ampliar moradia popular; investir no ensino integral e nos professores; reduzir desigualdades; proteger recursos naturais. |

## Persistência e apuração

Com Firebase, a coleção `participantes/{uid}` registra somente `votou: true`. Cada sessão fica em `sessoes/{uuid-aleatorio}` e contém uma impressão digital e os cinco votos. O UID e o CPF não fazem parte do documento de votos. As regras permitem uma marca de participação por conta e não permitem listar sessões.

Votos válidos atualizam `resultados/{cargo}_{numero}`. O total aumenta no mesmo lote atômico que cria a sessão. Voto de legenda, branco e nulo não incrementam contadores de candidatura. Veja a [configuração Firebase](firebase-configuracao.md) para os campos e os IDs de resultado.
