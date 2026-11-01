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
    }

    /**
     * Lista única de dados de demonstração. Edite aqui cargos, números, nomes, partidos,
     * vices e propostas antes de usar a oficina; todos os dados abaixo são fictícios.
     */
    public static Eleicao exemplo() {
        String horizonte = "Partido Horizonte Comunitário (fictício)";
        String aurora = "Partido Raízes do Amanhã (fictício)";
        String caminhos = "Partido Caminhos Abertos (fictício)";
        String cuidado = "Partido Cuidado Coletivo (fictício)";
        String futuro = "Partido Futuro Compartilhado (fictício)";

        Cargo federal = new Cargo("Deputado Federal", 4, List.of(
                proporcional("8101", "Ari Lume", horizonte, "81", List.of("Ampliar bibliotecas com horários acessíveis.", "Divulgar metas e gastos em linguagem simples.")),
                proporcional("8202", "Bela Nébula", aurora, "82", List.of("Apoiar formação técnica em escolas públicas.", "Publicar indicadores de acesso à saúde.")),
                proporcional("8303", "Cael Brisa", caminhos, "83", List.of("Incentivar transporte público de baixa emissão.", "Ampliar dados abertos sobre serviços.")),
                proporcional("1111", "Pedro Henrique Costa", "Juventude e Inovação Brasileira (JIB)", "11", List.of(
                        "Educação: propor mais investimentos nas escolas e universidades públicas.",
                        "Juventude: defender programas de primeiro emprego e estágio.",
                        "Tecnologia: incentivar pesquisas científicas e inovação.",
                        "Transparência: fiscalizar os gastos públicos e cobrar responsabilidade dos governantes.")),
                proporcional("2222", "Beatriz Rodrigues", "Vozes da Cidadania (PVC)", "22", List.of(
                        "Saúde: defender mais recursos para hospitais e unidades do SUS.",
                        "Proteção à mulher: fortalecer políticas de prevenção à violência.",
                        "Moradia: defender investimentos em habitação popular.",
                        "Trabalho: propor medidas de proteção aos trabalhadores e incentivo ao emprego.")),
                proporcional("3333", "André Martins", "Aliança de Desenvolvimento Sustentável (ADS)", "33", List.of(
                        "Economia: simplificar impostos e reduzir a burocracia para pequenos negócios.",
                        "Segurança: propor medidas legislativas de combate ao crime organizado.",
                        "Infraestrutura: defender investimentos federais em saneamento e transporte.",
                        "Tecnologia: incentivar o desenvolvimento tecnológico e a modernização dos serviços públicos."))));

        Cargo estadual = new Cargo("Deputado Estadual", 5, List.of(
                proporcional("81001", "Meia Bomba", horizonte, "81", List.of("Apoiar bibliotecas comunitárias e leitura.", "Publicar metas anuais de serviços estaduais."))));

        Cargo senador = new Cargo("Senador", 3, List.of(
                majoritario("820", "Curupira", aurora, "82", propostasEducacao(), null),
                majoritario("830", "Peter Pardo", caminhos, "83", propostasMobilidade(), null),
                majoritario("833", "Ivo Pedra-Mansa", caminhos, "83", propostasMobilidade(), null)));

        Cargo governador = new Cargo("Governador", 2, List.of(
                majoritario("84", "Zé Gotinha", cuidado, "84", List.of("Ampliar ações educativas de prevenção em saúde.", "Publicar informações de serviços em linguagem simples."), "Kiro Horizonte"),
                majoritario("82", "Luma Cedro-Doce", aurora, "82", propostasMobilidade(), "Miro Brisa-Firme"),
                majoritario("83", "Nilo Serra-Clara", caminhos, "83", propostasAmbiente(), "Ona Vale-Verde"),
                majoritario("44", "Gabriel Santos", "Força e Desenvolvimento Estadual (FDE)", "44", List.of(
                        "Saúde: construir e reformar hospitais regionais.",
                        "Educação: modernizar escolas estaduais e ampliar cursos técnicos.",
                        "Segurança: melhorar os equipamentos policiais e ampliar o policiamento.",
                        "Infraestrutura: recuperar estradas estaduais e melhorar o transporte intermunicipal."), null),
                majoritario("55", "Camila Ferreira", "União pelo Futuro (PUF)", "55", List.of(
                        "Economia: atrair empresas e incentivar o empreendedorismo.",
                        "Educação: ampliar cursos profissionalizantes para jovens e adultos.",
                        "Saúde: reduzir as filas de consultas e exames.",
                        "Turismo: valorizar as atrações naturais e culturais do estado."), null),
                majoritario("66", "Lucas Almeida", "Desenvolvimento e Justiça Social (DJS)", "66", List.of(
                        "Agricultura: apoiar pequenos produtores rurais e a agricultura familiar.",
                        "Saneamento: ampliar o acesso à água tratada e à coleta de esgoto.",
                        "Segurança: fortalecer o policiamento nas cidades.",
                        "Esporte: construir e reformar espaços esportivos públicos."), null)));

        Cargo presidente = new Cargo("Presidente", 2, List.of(
                majoritario("81", "Pia Sol-Nascente", horizonte, "81", propostasAmbiente(), "Quim Rio-Calmo"),
                majoritario("85", "Beto", futuro, "85", List.of("Apoiar espaços de convivência, esporte e cultura.", "Divulgar metas e resultados dos serviços públicos."), "Suri Campo-Livre"),
                majoritario("83", "Tali Nuvem-Mansa", caminhos, "83", propostasEducacao(), "Uli Trilha-Larga"),
                majoritario("11", "Rafael Mendes", "Renovação Nacional (PRN)", "11", List.of(
                        "Educação: ampliar o acesso às universidades e melhorar a infraestrutura das escolas públicas.",
                        "Saúde: fortalecer o atendimento do SUS e reduzir as filas de consultas e exames.",
                        "Emprego: incentivar pequenas empresas e criar programas de qualificação profissional.",
                        "Meio ambiente: combater o desmatamento e investir em energia solar e eólica."), null),
                majoritario("22", "Eduardo Carvalho", "Aliança pelo Progresso (PAP)", "22", List.of(
                        "Economia: simplificar impostos e incentivar o crescimento das empresas.",
                        "Segurança: ampliar os investimentos em equipamentos e tecnologia policial.",
                        "Infraestrutura: modernizar rodovias, ferrovias e aeroportos.",
                        "Agricultura: oferecer apoio financeiro e tecnológico aos produtores rurais."), null),
                majoritario("33", "Mariana Oliveira", "Movimento Popular Democrático (MPD)", "33", List.of(
                        "Moradia: ampliar programas habitacionais para famílias de baixa renda.",
                        "Educação: investir no ensino integral e na valorização dos professores.",
                        "Direitos sociais: combater a desigualdade e ampliar políticas de inclusão.",
                        "Meio ambiente: incentivar energias renováveis e proteger os recursos naturais."), null)));

        return new Eleicao(List.of(federal, estadual, senador, governador, presidente));
    }

    private static CandidatoProporcional proporcional(String numero, String nome, String partido,
                                                       String numeroPartido, List<String> propostas) {
        return new CandidatoProporcional(numero, nome, partido, numeroPartido, propostas);
    }

    private static CandidatoMajoritario majoritario(String numero, String nome, String partido,
                                                     String numeroPartido, List<String> propostas, String vice) {
        return new CandidatoMajoritario(numero, nome, partido, numeroPartido, propostas, vice);
    }

    private static List<String> propostasEducacao() {
        return List.of("Ampliar bibliotecas e espaços de leitura.", "Apoiar formação técnica acessível.");
    }

    private static List<String> propostasMobilidade() {
        return List.of("Planejar trajetos seguros para pedestres.", "Melhorar a integração do transporte público.");
    }

    private static List<String> propostasAmbiente() {
        return List.of("Incentivar eficiência energética em serviços públicos.", "Ampliar áreas verdes com manutenção planejada.");
    }

    private static List<String> propostasTransparencia() {
        return List.of("Publicar metas e gastos em linguagem simples.", "Divulgar indicadores de serviços regularmente.");
    }
}
