package com.urnacidada.model;

import java.util.List;

/** Define quais cargos serão votados e em que ordem. */
public record Eleicao(List<Cargo> cargos) {

    public Eleicao {
        if (cargos == null || cargos.isEmpty() || cargos.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("A eleição precisa de pelo menos um cargo.");
        }
        cargos = List.copyOf(cargos);
        if (cargos.stream().map(c -> c.nome().toLowerCase(java.util.Locale.ROOT)).distinct().count() != cargos.size()) {
            throw new IllegalArgumentException("A eleição não pode repetir o mesmo cargo.");
        }
        long quantidadeDeCandidaturas = cargos.stream().mapToLong(c -> c.candidatos().size()).sum();
        long numerosDistintos = cargos.stream().flatMap(c -> c.candidatos().stream())
                .map(Candidato::numero).distinct().count();
        if (numerosDistintos != quantidadeDeCandidaturas) {
            throw new IllegalArgumentException("O número de candidato não pode se repetir entre os cargos.");
        }
    }

    /** Lista das nove candidaturas definidas para a simulação. */
    public static Eleicao exemplo() {
        Cargo federal = new Cargo("Deputado Federal", 4, List.of(
                proporcional("1736", "Pedro Henrique Costa", "Juventude e Inovação Brasileira (JIB)", "17", List.of(
                        "Educação: propor mais investimentos nas escolas e universidades públicas.",
                        "Juventude: defender programas de primeiro emprego e estágio.",
                        "Tecnologia: incentivar pesquisas científicas e inovação.",
                        "Transparência: fiscalizar os gastos públicos e cobrar responsabilidade dos governantes.")),
                proporcional("5730", "Beatriz Rodrigues", "Vozes da Cidadania (PVC)", "57", List.of(
                        "Saúde: defender mais recursos para hospitais e unidades do SUS.",
                        "Proteção à mulher: fortalecer políticas de prevenção à violência.",
                        "Moradia: defender investimentos em habitação popular.",
                        "Trabalho: propor medidas de proteção aos trabalhadores e incentivo ao emprego.")),
                proporcional("9462", "André Martins", "Aliança de Desenvolvimento Sustentável (ADS)", "94", List.of(
                        "Economia: simplificar impostos e reduzir a burocracia para pequenos negócios.",
                        "Segurança: propor medidas legislativas de combate ao crime organizado.",
                        "Infraestrutura: defender investimentos federais em saneamento e transporte.",
                        "Tecnologia: incentivar o desenvolvimento tecnológico e a modernização dos serviços públicos."))));

        Cargo governador = new Cargo("Governador", 2, List.of(
                majoritario("14", "Gabriel Santos", "Força e Desenvolvimento Estadual (FDE)", "14", List.of(
                        "Saúde: construir e reformar hospitais regionais.",
                        "Educação: modernizar escolas estaduais e ampliar cursos técnicos.",
                        "Segurança: melhorar os equipamentos policiais e ampliar o policiamento.",
                        "Infraestrutura: recuperar estradas estaduais e melhorar o transporte intermunicipal."), null),
                majoritario("28", "Camila Ferreira", "União pelo Futuro (PUF)", "28", List.of(
                        "Economia: atrair empresas e incentivar o empreendedorismo.",
                        "Educação: ampliar cursos profissionalizantes para jovens e adultos.",
                        "Saúde: reduzir as filas de consultas e exames.",
                        "Turismo: valorizar as atrações naturais e culturais do estado."), null),
                majoritario("37", "Lucas Almeida", "Desenvolvimento e Justiça Social (DJS)", "37", List.of(
                        "Agricultura: apoiar pequenos produtores rurais e a agricultura familiar.",
                        "Saneamento: ampliar o acesso à água tratada e à coleta de esgoto.",
                        "Segurança: fortalecer o policiamento nas cidades.",
                        "Esporte: construir e reformar espaços esportivos públicos."), null)));

        Cargo presidente = new Cargo("Presidente", 2, List.of(
                majoritario("13", "Rafael Mendes", "Renovação Nacional (PRN)", "13", List.of(
                        "Educação: ampliar o acesso às universidades e melhorar a infraestrutura das escolas públicas.",
                        "Saúde: fortalecer o atendimento do SUS e reduzir as filas de consultas e exames.",
                        "Emprego: incentivar pequenas empresas e criar programas de qualificação profissional.",
                        "Meio ambiente: combater o desmatamento e investir em energia solar e eólica."), null),
                majoritario("27", "Eduardo Carvalho", "Aliança pelo Progresso (PAP)", "27", List.of(
                        "Economia: simplificar impostos e incentivar o crescimento das empresas.",
                        "Segurança: ampliar os investimentos em equipamentos e tecnologia policial.",
                        "Infraestrutura: modernizar rodovias, ferrovias e aeroportos.",
                        "Agricultura: oferecer apoio financeiro e tecnológico aos produtores rurais."), null),
                majoritario("39", "Mariana Oliveira", "Movimento Popular Democrático (MPD)", "39", List.of(
                        "Moradia: ampliar programas habitacionais para famílias de baixa renda.",
                        "Educação: investir no ensino integral e na valorização dos professores.",
                        "Direitos sociais: combater a desigualdade e ampliar políticas de inclusão.",
                        "Meio ambiente: incentivar energias renováveis e proteger os recursos naturais."), null)));

        return new Eleicao(List.of(federal, governador, presidente));
    }

    private static CandidatoProporcional proporcional(String numero, String nome, String partido,
                                                       String numeroPartido, List<String> propostas) {
        return new CandidatoProporcional(numero, nome, partido, numeroPartido, propostas);
    }

    private static CandidatoMajoritario majoritario(String numero, String nome, String partido,
                                                     String numeroPartido, List<String> propostas, String vice) {
        return new CandidatoMajoritario(numero, nome, partido, numeroPartido, propostas, vice);
    }
}
