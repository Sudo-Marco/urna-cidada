package com.urnacidada.testes;

import com.urnacidada.model.Candidato;
import com.urnacidada.model.CandidatoMajoritario;
import com.urnacidada.model.CandidatoProporcional;
import com.urnacidada.model.Cargo;
import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;

import java.time.Instant;
import java.util.List;

import static com.urnacidada.testes.MiniTeste.grupo;
import static com.urnacidada.testes.MiniTeste.igual;
import static com.urnacidada.testes.MiniTeste.lanca;
import static com.urnacidada.testes.MiniTeste.teste;
import static com.urnacidada.testes.MiniTeste.verdade;

/** Testes das classes de modelo: candidatos, cargos, eleição e voto. */
final class ModeloTestes {

    private ModeloTestes() { }

    static void executar() {
        grupo("Modelo: candidatos (herança e polimorfismo)");

        teste("candidato expõe número, nome e partido", () -> {
            Candidato c = new CandidatoProporcional("12345", "Ana Exemplo", "Partido A");
            igual("12345", c.numero());
            igual("Ana Exemplo", c.nome());
            igual("Partido A", c.partido());
        });

        teste("candidato rejeita número vazio, com letras ou nome em branco", () -> {
            lanca(IllegalArgumentException.class, () -> new CandidatoProporcional("", "Ana", "A"));
            lanca(IllegalArgumentException.class, () -> new CandidatoProporcional("12a45", "Ana", "A"));
            lanca(IllegalArgumentException.class, () -> new CandidatoMajoritario("11", " ", "A"));
            lanca(IllegalArgumentException.class, () -> new CandidatoMajoritario("11", "Ana", ""));
        });

        teste("candidato valida partido e propostas fictícias", () -> {
            List<String> validas = List.of("Ampliar bibliotecas comunitárias.", "Publicar indicadores simples.");
            lanca(IllegalArgumentException.class, () -> new CandidatoProporcional("8101", "Ari", "Partido", "82", validas));
            lanca(IllegalArgumentException.class, () -> new CandidatoMajoritario("81", "Ari", "Partido", "81", List.of("Uma só"), null));
            lanca(IllegalArgumentException.class, () -> new CandidatoMajoritario("81", "Ari", "Partido", "81", List.of("x".repeat(181), "Outra"), null));
        });

        teste("o mesmo tipo Candidato responde de forma diferente (polimorfismo)", () -> {
            Candidato vereador = new CandidatoProporcional("12345", "Ana", "A");
            Candidato prefeito = new CandidatoMajoritario("11", "Diego", "A");
            igual("Proporcional", vereador.tipoCargo());
            igual("Majoritário", prefeito.tipoCargo());
            verdade(vereador.explicacaoCargo().contains("proporcional"), "explicação do proporcional");
            verdade(prefeito.explicacaoCargo().contains("majoritário"), "explicação do majoritário");
        });

        teste("toString inclui o tipo do cargo", () ->
                verdade(new CandidatoMajoritario("11", "Diego", "A").toString().contains("Majoritário")));

        grupo("Modelo: cargo, eleição e voto");

        teste("cargo encontra candidato pelo número e devolve vazio se não existe", () -> {
            Cargo cargo = Eleicao.exemplo().cargos().get(0);
            igual("Ari Lume", cargo.buscarCandidato("8101").orElseThrow().nome());
            verdade(cargo.buscarCandidato("99999").isEmpty(), "número inexistente");
        });

        teste("cargo rejeita candidato com quantidade errada de dígitos", () ->
                lanca(IllegalArgumentException.class, () ->
                        new Cargo("Prefeito", 2, List.of(new CandidatoMajoritario("111", "Diego", "A")))));

        teste("cargos oficiais validam tamanho e tipo de candidatura", () -> {
            var federal = Eleicao.exemplo().cargos().get(0).candidatos().get(0);
            lanca(IllegalArgumentException.class, () -> new Cargo("Deputado Federal", 5, List.of(federal)));
            lanca(IllegalArgumentException.class, () -> new Cargo("Senador", 3, List.of(
                    new CandidatoMajoritario("811", "Galo", "Partido", "81",
                            List.of("Ampliar bibliotecas.", "Publicar metas."), "Vice Fictício"))));
        });

        teste("cargo rejeita números repetidos", () ->
                lanca(IllegalArgumentException.class, () -> new Cargo("Prefeito", 2, List.of(
                        new CandidatoMajoritario("11", "Diego", "A"),
                        new CandidatoMajoritario("11", "Elisa", "B")))));

        teste("cargo não aceita a lista de candidatos alterada depois de criado", () -> {
            Cargo cargo = Eleicao.exemplo().cargos().get(1);
            lanca(UnsupportedOperationException.class, () -> cargo.candidatos().clear());
        });

        teste("eleição exige pelo menos um cargo", () ->
                lanca(IllegalArgumentException.class, () -> new Eleicao(List.of())));

        teste("eleição não repete o mesmo cargo", () -> {
            Cargo federal = Eleicao.exemplo().cargos().get(0);
            lanca(IllegalArgumentException.class, () -> new Eleicao(List.of(federal,
                    new Cargo("deputado federal", 4, federal.candidatos()))));
        });

        teste("eleição de exemplo tem os cinco cargos em ordem e os dígitos correspondentes", () -> {
            List<Cargo> cargos = Eleicao.exemplo().cargos();
            igual(5, cargos.size());
            igual(List.of("Deputado Federal", "Deputado Estadual", "Senador", "Governador", "Presidente"),
                    cargos.stream().map(Cargo::nome).toList());
            igual(List.of(4, 5, 3, 2, 2), cargos.stream().map(Cargo::digitos).toList());
        });

        teste("candidatos pedidos usam os números correspondentes a cada cargo", () -> {
            List<Cargo> cargos = Eleicao.exemplo().cargos();
            igual("Meia Bomba", cargos.get(1).buscarCandidato("81001").orElseThrow().nome());
            igual("Curupira", cargos.get(2).buscarCandidato("820").orElseThrow().nome());
            igual("Peter Pardo", cargos.get(2).buscarCandidato("830").orElseThrow().nome());
            igual("Zé Gotinha", cargos.get(3).buscarCandidato("84").orElseThrow().nome());
            igual("Beto", cargos.get(4).buscarCandidato("85").orElseThrow().nome());
            igual("Partido Horizonte Comunitário (fictício)", cargos.get(1).buscarCandidato("81001").orElseThrow().partido());
            igual("Partido Raízes do Amanhã (fictício)", cargos.get(2).buscarCandidato("820").orElseThrow().partido());
            igual("Partido Caminhos Abertos (fictício)", cargos.get(2).buscarCandidato("830").orElseThrow().partido());
            igual("Partido Cuidado Coletivo (fictício)", cargos.get(3).buscarCandidato("84").orElseThrow().partido());
            igual("Partido Futuro Compartilhado (fictício)", cargos.get(4).buscarCandidato("85").orElseThrow().partido());
        });

        teste("candidaturas têm números, propostas e vices consistentes", () -> {
            List<Cargo> cargos = Eleicao.exemplo().cargos();
            for (Cargo cargo : cargos) {
                verdade(cargo.candidatos().size() <= 10, "limite de candidaturas");
                for (Candidato c : cargo.candidatos()) {
                    igual(cargo.digitos(), c.numero().length());
                    verdade(c.numero().startsWith(c.numeroPartido()), "prefixo do partido");
                    verdade(c.propostas().size() >= 2 && c.propostas().size() <= 4, "quantidade de propostas");
                    verdade(c.propostas().stream().allMatch(p -> p.length() <= 180), "propostas preservadas");
                    verdade(c.vice().isEmpty() || cargo.nome().equals("Governador") || cargo.nome().equals("Presidente"),
                            "vice apenas em chapa executiva");
                }
            }
        });

        teste("novas candidaturas têm nomes, números, partidos e propostas", () -> {
            List<Cargo> cargos = Eleicao.exemplo().cargos();
            igual("Rafael Mendes", cargos.get(4).buscarCandidato("11").orElseThrow().nome());
            igual("Eduardo Carvalho", cargos.get(4).buscarCandidato("22").orElseThrow().nome());
            igual("Mariana Oliveira", cargos.get(4).buscarCandidato("33").orElseThrow().nome());
            igual("Gabriel Santos", cargos.get(3).buscarCandidato("44").orElseThrow().nome());
            igual("Camila Ferreira", cargos.get(3).buscarCandidato("55").orElseThrow().nome());
            igual("Lucas Almeida", cargos.get(3).buscarCandidato("66").orElseThrow().nome());
            igual("Pedro Henrique Costa", cargos.get(0).buscarCandidato("1111").orElseThrow().nome());
            igual("Beatriz Rodrigues", cargos.get(0).buscarCandidato("2222").orElseThrow().nome());
            igual("André Martins", cargos.get(0).buscarCandidato("3333").orElseThrow().nome());
            verdade(cargos.get(4).buscarCandidato("11").orElseThrow().propostas().size() == 4,
                    "propostas presidenciais mantidas");
        });

        teste("voto branco não pode ter número; válido e nulo precisam ter", () -> {
            Instant agora = Instant.now();
            lanca(IllegalArgumentException.class, () -> new Voto("Prefeito", TipoVoto.BRANCO, "11", agora));
            lanca(IllegalArgumentException.class, () -> new Voto("Prefeito", TipoVoto.VALIDO, null, agora));
            lanca(IllegalArgumentException.class, () -> new Voto("Prefeito", TipoVoto.NULO, " ", agora));
            lanca(IllegalArgumentException.class, () -> new Voto(" ", TipoVoto.BRANCO, null, agora));
        });
    }
}
