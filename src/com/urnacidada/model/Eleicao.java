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
                proporcional("8303", "Cael Brisa", caminhos, "83", List.of("Incentivar transporte público de baixa emissão.", "Ampliar dados abertos sobre serviços."))));

        Cargo estadual = new Cargo("Deputado Estadual", 5, List.of(
                proporcional("81001", "Meia Bomba", horizonte, "81", List.of("Apoiar bibliotecas comunitárias e leitura.", "Publicar metas anuais de serviços estaduais."))));

        Cargo senador = new Cargo("Senador", 3, List.of(
                majoritario("820", "Curupira", aurora, "82", propostasEducacao(), null),
                majoritario("830", "Peter Pardo", caminhos, "83", propostasMobilidade(), null),
                majoritario("833", "Ivo Pedra-Mansa", caminhos, "83", propostasMobilidade(), null)));

        Cargo governador = new Cargo("Governador", 2, List.of(
                majoritario("84", "Zé Gotinha", cuidado, "84", List.of("Ampliar ações educativas de prevenção em saúde.", "Publicar informações de serviços em linguagem simples."), "Kiro Horizonte"),
                majoritario("82", "Luma Cedro-Doce", aurora, "82", propostasMobilidade(), "Miro Brisa-Firme"),
                majoritario("83", "Nilo Serra-Clara", caminhos, "83", propostasAmbiente(), "Ona Vale-Verde")));

        Cargo presidente = new Cargo("Presidente", 2, List.of(
                majoritario("81", "Pia Sol-Nascente", horizonte, "81", propostasAmbiente(), "Quim Rio-Calmo"),
                majoritario("85", "Beto", futuro, "85", List.of("Apoiar espaços de convivência, esporte e cultura.", "Divulgar metas e resultados dos serviços públicos."), "Suri Campo-Livre"),
                majoritario("83", "Tali Nuvem-Mansa", caminhos, "83", propostasEducacao(), "Uli Trilha-Larga")));

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
