package com.urnacidada.testes;

import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.service.UrnaService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.urnacidada.testes.MiniTeste.*;

/** Regras de votação dos cinco cargos e persistência somente ao concluir. */
final class UrnaServiceTestes {
    private static VotoRepositoryEmMemoria repositorio;
    private static UrnaService urna;
    private UrnaServiceTestes() { }
    private static void preparar() { repositorio = new VotoRepositoryEmMemoria(); urna = new UrnaService(Eleicao.exemplo(), repositorio); }
    private static void digitar(UrnaService u, String s) { for (char c : s.toCharArray()) u.digitar(c); }
    private static void caso(String nome, MiniTeste.Corpo corpo) { teste(nome, () -> { preparar(); corpo.rodar(); }); }
    static void executar() {
        grupo("Regras da votação (UrnaService)");
        caso("inicia na ordem de cinco cargos", () -> {
            igual("Deputado Federal", urna.getCargoAtual().nome()); igual(1, urna.getNumeroCargoAtual()); igual(5, urna.getTotalCargos());
        });
        caso("número válido encontra candidato federal", () -> { digitar(urna, "8101"); igual("Ari Lume", urna.getCandidatoAtual().orElseThrow().nome()); });
        caso("número incompleto não encontra candidato", () -> { digitar(urna, "810"); verdade(urna.getCandidatoAtual().isEmpty()); });
        caso("voto de legenda é permitido com dois dígitos de partido nos cargos proporcionais", () -> {
            digitar(urna, "81"); igual("Partido Horizonte Comunitário (fictício)", urna.getPartidoLegendaAtual().orElseThrow());
            verdade(urna.podeConfirmar()); urna.confirmar(); igual(TipoVoto.LEGENDA, urna.getVotosRegistrados().get(0).tipo());
            igual("81", urna.getVotosRegistrados().get(0).numero());
        });
        caso("legenda não pode ser confirmada em cargo majoritário", () -> {
            digitar(urna, "8101"); urna.confirmar();
            digitar(urna, "81001"); urna.confirmar();
            digitar(urna, "81"); verdade(!urna.podeConfirmar());
            lanca(IllegalStateException.class, urna::confirmar);
        });
        caso("não aceita letra nem excesso de dígitos", () -> {
            lanca(IllegalArgumentException.class, () -> urna.digitar('x'));
            digitar(urna, "8101"); lanca(IllegalStateException.class, () -> urna.digitar('1'));
        });
        caso("número incompleto não confirma", () -> { digitar(urna, "8"); lanca(IllegalStateException.class, urna::confirmar); igual("Deputado Federal", urna.getCargoAtual().nome()); });
        caso("corrigir apaga o número digitado", () -> { digitar(urna, "810"); urna.corrigir(); igual("", urna.getDigitado()); });
        caso("número federal inexistente produz nulo", () -> { digitar(urna, "9999"); urna.confirmar(); igual(TipoVoto.NULO, urna.getVotosRegistrados().get(0).tipo()); });
        caso("branco não guarda número e exige campo vazio", () -> {
            urna.votarEmBranco(); urna.confirmar(); igual(TipoVoto.BRANCO, urna.getVotosRegistrados().get(0).tipo()); igual(null, urna.getVotosRegistrados().get(0).numero());
        });
        caso("branco depois de digitar exige correção", () -> { digitar(urna, "8"); lanca(IllegalStateException.class, urna::votarEmBranco); });
        caso("corrigir desfaz o branco", () -> { urna.votarEmBranco(); urna.corrigir(); verdade(!urna.isBranco()); digitar(urna, "8101"); });
        teste("branco, nulo e correção são aplicáveis em cada tipo de cargo", () -> {
            List<String> validos = List.of("8101", "81001", "820", "84", "85");
            for (int indice = 0; indice < 5; indice++) {
                urna = new UrnaService(Eleicao.exemplo(), new VotoRepositoryEmMemoria());
                for (int anterior = 0; anterior < indice; anterior++) { digitar(urna, validos.get(anterior)); urna.confirmar(); }
                urna.votarEmBranco(); urna.confirmar();
                igual(TipoVoto.BRANCO, urna.getVotosRegistrados().get(indice).tipo());

                urna = new UrnaService(Eleicao.exemplo(), new VotoRepositoryEmMemoria());
                for (int anterior = 0; anterior < indice; anterior++) { digitar(urna, validos.get(anterior)); urna.confirmar(); }
                int digitos = urna.getCargoAtual().digitos(); digitar(urna, "9".repeat(digitos)); urna.confirmar();
                igual(TipoVoto.NULO, urna.getVotosRegistrados().get(indice).tipo());

                urna = new UrnaService(Eleicao.exemplo(), new VotoRepositoryEmMemoria());
                for (int anterior = 0; anterior < indice; anterior++) { digitar(urna, validos.get(anterior)); urna.confirmar(); }
                urna.digitar('9'); urna.corrigir(); digitar(urna, validos.get(indice)); urna.confirmar();
                igual(TipoVoto.VALIDO, urna.getVotosRegistrados().get(indice).tipo());
            }
        });
        caso("uma confirmação válida avança ao próximo cargo", () -> { digitar(urna, "8101"); urna.confirmar(); igual("Deputado Estadual", urna.getCargoAtual().nome()); igual(2, urna.getNumeroCargoAtual()); });
        caso("votos não são persistidos até a confirmação do último cargo", () -> { digitar(urna, "8101"); urna.confirmar(); igual(0, repositorio.getVotos().size()); });
        caso("sequência completa contém os cinco votos em ordem e encerra", () -> {
            String[] numeros = {"8101", "81001", "820", "84", "85"};
            for (String numero : numeros) { digitar(urna, numero); urna.confirmar(); }
            verdade(urna.isFinalizada()); igual(5, repositorio.getVotos().size());
            igual(List.of("Deputado Federal", "Deputado Estadual", "Senador", "Governador", "Presidente"), repositorio.getVotos().stream().map(Voto::cargo).toList());
            lanca(IllegalStateException.class, () -> urna.confirmar());
        });
        teste("falha ao salvar no último cargo não perde votos e permite tentar de novo", () -> {
            AtomicInteger tentativas = new AtomicInteger(); VotoRepositoryEmMemoria destino = new VotoRepositoryEmMemoria();
            VotoRepository repo = (id, votos) -> { if (tentativas.incrementAndGet() == 1) throw new RepositorioException("sem rede", null); destino.salvarSessao(id, votos); };
            UrnaService u = new UrnaService(Eleicao.exemplo(), repo);
            for (String n : List.of("8101", "81001", "820", "84")) { digitar(u, n); u.confirmar(); }
            digitar(u, "85"); lanca(RepositorioException.class, u::confirmar); verdade(!u.isFinalizada());
            u.confirmar(); verdade(u.isFinalizada()); igual(5, destino.getVotos().size());
        });
    }
}
