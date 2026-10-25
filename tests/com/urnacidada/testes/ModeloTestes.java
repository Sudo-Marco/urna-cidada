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
            igual("Ana Exemplo", cargo.buscarCandidato("12345").orElseThrow().nome());
            verdade(cargo.buscarCandidato("99999").isEmpty(), "número inexistente");
        });

        teste("cargo rejeita candidato com quantidade errada de dígitos", () ->
                lanca(IllegalArgumentException.class, () ->
                        new Cargo("Prefeito", 2, List.of(new CandidatoMajoritario("111", "Diego", "A")))));

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

        teste("eleição de exemplo: Vereador (5 dígitos) e depois Prefeito (2 dígitos)", () -> {
            List<Cargo> cargos = Eleicao.exemplo().cargos();
            igual(2, cargos.size());
            igual("Vereador", cargos.get(0).nome());
            igual(5, cargos.get(0).digitos());
            igual("Prefeito", cargos.get(1).nome());
            igual(2, cargos.get(1).digitos());
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
