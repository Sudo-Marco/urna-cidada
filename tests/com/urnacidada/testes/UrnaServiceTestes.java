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

import static com.urnacidada.testes.MiniTeste.grupo;
import static com.urnacidada.testes.MiniTeste.igual;
import static com.urnacidada.testes.MiniTeste.lanca;
import static com.urnacidada.testes.MiniTeste.teste;
import static com.urnacidada.testes.MiniTeste.verdade;

/** Testes das regras de votação (RF02 a RF08). */
final class UrnaServiceTestes {

    private static VotoRepositoryEmMemoria repositorio;
    private static UrnaService urna;

    private UrnaServiceTestes() { }

    private static void preparar() {
        repositorio = new VotoRepositoryEmMemoria();
        urna = new UrnaService(Eleicao.exemplo(), repositorio);
    }

    private static void digitar(UrnaService alvo, String numero) {
        for (char c : numero.toCharArray()) {
            alvo.digitar(c);
        }
    }

    private static void caso(String nome, MiniTeste.Corpo corpo) {
        teste(nome, () -> {
            preparar();
            corpo.rodar();
        });
    }

    static void executar() {
        grupo("Regras da votação (UrnaService)");

        caso("fluxo começa pelo primeiro cargo", () -> {
            igual("Vereador", urna.getCargoAtual().nome());
            igual(1, urna.getNumeroCargoAtual());
            igual(2, urna.getTotalCargos());
        });

        caso("número válido mostra o candidato", () -> {
            digitar(urna, "12345");
            igual("Ana Exemplo", urna.getCandidatoAtual().orElseThrow().nome());
        });

        caso("número incompleto não mostra candidato", () -> {
            digitar(urna, "123");
            verdade(urna.getCandidatoAtual().isEmpty(), "sem candidato");
        });

        caso("não aceita letras", () -> {
            lanca(IllegalArgumentException.class, () -> urna.digitar('a'));
            igual("", urna.getDigitado());
        });

        caso("não aceita mais dígitos que o cargo permite", () -> {
            digitar(urna, "12345");
            lanca(IllegalStateException.class, () -> urna.digitar('6'));
        });

        caso("não confirma com número incompleto", () -> {
            digitar(urna, "123");
            lanca(IllegalStateException.class, () -> urna.confirmar());
            igual("Vereador", urna.getCargoAtual().nome());
        });

        caso("corrigir apaga o que foi digitado", () -> {
            digitar(urna, "123");
            urna.corrigir();
            igual("", urna.getDigitado());
        });

        caso("confirmar avança para o segundo cargo", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            igual("Prefeito", urna.getCargoAtual().nome());
            igual(2, urna.getNumeroCargoAtual());
            igual("", urna.getDigitado());
        });

        caso("número inexistente vira voto nulo", () -> {
            digitar(urna, "99999");
            urna.confirmar();
            igual(TipoVoto.NULO, urna.getVotosRegistrados().get(0).tipo());
        });

        caso("voto em branco funciona e não guarda número", () -> {
            urna.votarEmBranco();
            urna.confirmar();
            Voto voto = urna.getVotosRegistrados().get(0);
            igual(TipoVoto.BRANCO, voto.tipo());
            igual(null, voto.numero());
        });

        caso("não pode votar em branco depois de digitar", () -> {
            digitar(urna, "1");
            lanca(IllegalStateException.class, () -> urna.votarEmBranco());
        });

        caso("não pode digitar depois de marcar branco", () -> {
            urna.votarEmBranco();
            lanca(IllegalStateException.class, () -> urna.digitar('1'));
        });

        caso("corrigir desfaz o branco", () -> {
            urna.votarEmBranco();
            urna.corrigir();
            verdade(!urna.isBranco(), "branco desfeito");
            digitar(urna, "1");
            igual("1", urna.getDigitado());
        });

        caso("fluxo completo dos dois cargos grava os votos", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            digitar(urna, "22");
            urna.confirmar();
            verdade(urna.isFinalizada(), "finalizada");
            List<Voto> salvos = repositorio.getVotos();
            igual(2, salvos.size());
            igual("Vereador", salvos.get(0).cargo());
            igual("Prefeito", salvos.get(1).cargo());
            igual(TipoVoto.VALIDO, salvos.get(1).tipo());
        });

        caso("votos não são gravados antes de terminar", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            igual(0, repositorio.getVotos().size());
        });

        caso("depois de encerrar não aceita mais ações", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            digitar(urna, "11");
            urna.confirmar();
            lanca(IllegalStateException.class, () -> urna.digitar('1'));
            lanca(IllegalStateException.class, () -> urna.confirmar());
        });

        caso("cada votação tem um identificador de sessão próprio", () -> {
            UrnaService outra = new UrnaService(Eleicao.exemplo(), repositorio);
            verdade(!urna.getSessaoId().isBlank(), "id preenchido");
            verdade(!urna.getSessaoId().equals(outra.getSessaoId()), "ids diferentes");
        });

        caso("não aceita eleição ou repositório nulos", () -> {
            lanca(NullPointerException.class, () -> new UrnaService(null, repositorio));
            lanca(NullPointerException.class, () -> new UrnaService(Eleicao.exemplo(), null));
        });

        teste("falha ao salvar não encerra a votação e permite tentar de novo", () -> {
            AtomicInteger tentativas = new AtomicInteger();
            VotoRepositoryEmMemoria destino = new VotoRepositoryEmMemoria();
            VotoRepository instavel = (sessaoId, votos) -> {
                if (tentativas.incrementAndGet() == 1) {
                    throw new RepositorioException("sem internet", null);
                }
                destino.salvarSessao(sessaoId, votos);
            };
            UrnaService semRede = new UrnaService(Eleicao.exemplo(), instavel);
            digitar(semRede, "12345");
            semRede.confirmar();
            digitar(semRede, "11");

            lanca(RepositorioException.class, () -> semRede.confirmar());
            verdade(!semRede.isFinalizada(), "não finalizada após falha");
            igual(0, destino.getVotos().size());

            semRede.confirmar(); // segunda tentativa funciona
            verdade(semRede.isFinalizada(), "finalizada após nova tentativa");
            igual(2, destino.getVotos().size());
        });
    }
}
