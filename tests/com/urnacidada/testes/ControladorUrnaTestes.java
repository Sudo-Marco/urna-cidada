package com.urnacidada.testes;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryComReserva;
import com.urnacidada.repository.VotoRepositoryEmArquivo;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.view.ControladorUrna;
import com.urnacidada.view.EstadoTela;
import com.urnacidada.view.EstadoTela.Modo;
import com.urnacidada.view.EstadoTela.TipoMensagem;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.urnacidada.testes.MiniTeste.grupo;
import static com.urnacidada.testes.MiniTeste.igual;
import static com.urnacidada.testes.MiniTeste.teste;
import static com.urnacidada.testes.MiniTeste.verdade;

/** Testes do que a tela mostra, sem abrir janela (RF01, RF04 a RF08 e RNF01). */
final class ControladorUrnaTestes {

    private static VotoRepositoryEmMemoria memoria;
    private static ControladorUrna controlador;
    private static List<EstadoTela> publicados;
    private static List<Runnable> filaDeGravacao;

    private ControladorUrnaTestes() { }

    /** Cria um controlador com gravação síncrona (o resultado volta na mesma chamada). */
    private static void preparar(VotoRepository repositorio) {
        publicados = new ArrayList<>();
        controlador = new ControladorUrna(Eleicao.exemplo(), repositorio, Runnable::run, Runnable::run,
                publicados::add, () -> { });
        controlador.novaVotacao();
    }

    /** Cria um controlador em que a gravação só roda quando o teste mandar. */
    private static void prepararAssincrono() {
        memoria = new VotoRepositoryEmMemoria();
        publicados = new ArrayList<>();
        filaDeGravacao = new ArrayList<>();
        controlador = new ControladorUrna(Eleicao.exemplo(), memoria, filaDeGravacao::add, Runnable::run,
                publicados::add, () -> { });
        controlador.novaVotacao();
    }

    private static void rodarFila() {
        List<Runnable> tarefas = new ArrayList<>(filaDeGravacao);
        filaDeGravacao.clear();
        tarefas.forEach(Runnable::run);
    }

    private static void digitar(String numero) {
        for (char c : numero.toCharArray()) {
            controlador.digitar(c);
        }
    }

    private static EstadoTela ultimo() {
        return publicados.get(publicados.size() - 1);
    }

    private static void caso(String nome, MiniTeste.Corpo corpo) {
        teste(nome, () -> {
            memoria = new VotoRepositoryEmMemoria();
            preparar(memoria);
            corpo.rodar();
        });
    }

    static void executar() {
        grupo("Tela: o que é mostrado ao participante (ControladorUrna)");

        caso("ao iniciar mostra o primeiro cargo, progresso e orientação", () -> {
            EstadoTela e = ultimo();
            igual(Modo.DIGITANDO, e.modo());
            igual("VEREADOR", e.cargo());
            igual("CARGO 1 DE 2", e.progresso());
            igual(5, e.posicoes());
            igual(TipoMensagem.INFO, e.tipoMensagem());
            verdade(e.controlesHabilitados(), "teclas habilitadas");
        });

        caso("número válido mostra nome, partido e explicação do cargo (polimorfismo)", () -> {
            digitar("12345");
            EstadoTela e = ultimo();
            igual(Modo.CANDIDATO, e.modo());
            igual("Ana Exemplo", e.titulo());
            igual("Partido Fictício A", e.detalhe());
            verdade(e.explicacao().contains("proporcional"), "explicação do proporcional");
            igual(TipoMensagem.SUCESSO, e.tipoMensagem());
        });

        caso("número inexistente mostra NÚMERO ERRADO / VOTO NULO como aviso", () -> {
            digitar("99999");
            EstadoTela e = ultimo();
            igual(Modo.NUMERO_ERRADO, e.modo());
            igual("NÚMERO ERRADO", e.titulo());
            igual("VOTO NULO", e.detalhe());
            igual(TipoMensagem.AVISO, e.tipoMensagem());
        });

        caso("número incompleto continua em DIGITANDO", () -> {
            digitar("123");
            igual(Modo.DIGITANDO, ultimo().modo());
            igual("123", ultimo().digitado());
        });

        caso("branco mostra VOTO EM BRANCO e esconde as caixas", () -> {
            controlador.branco();
            EstadoTela e = ultimo();
            igual(Modo.BRANCO, e.modo());
            igual("VOTO EM BRANCO", e.titulo());
            igual(0, e.posicoes());
        });

        caso("corrigir volta para DIGITANDO e limpa os dígitos", () -> {
            digitar("99999");
            controlador.corrigir();
            igual(Modo.DIGITANDO, ultimo().modo());
            igual("", ultimo().digitado());
        });

        caso("letra mostra erro e não altera os dígitos", () -> {
            controlador.digitar('a');
            igual(TipoMensagem.ERRO, ultimo().tipoMensagem());
            igual("", ultimo().digitado());
        });

        caso("confirmar com número incompleto mostra erro e não avança de cargo", () -> {
            digitar("123");
            controlador.confirmar();
            igual(TipoMensagem.ERRO, ultimo().tipoMensagem());
            igual("VEREADOR", ultimo().cargo());
        });

        caso("branco depois de digitar mostra erro explicando o que fazer", () -> {
            digitar("1");
            controlador.branco();
            igual(TipoMensagem.ERRO, ultimo().tipoMensagem());
            verdade(ultimo().mensagem().contains("CORRIGE"), "orienta usar CORRIGE");
        });

        caso("mensagem de sucesso não é mostrada como erro", () -> {
            digitar("12345");
            controlador.confirmar();
            EstadoTela e = ultimo();
            igual("PREFEITO", e.cargo());
            igual("CARGO 2 DE 2", e.progresso());
            igual(TipoMensagem.SUCESSO, e.tipoMensagem());
            verdade(e.mensagem().contains("Prefeito"), "cita o próximo cargo");
            verdade(!e.explicacao().contains("proporcional"), "não sobra explicação do cargo anterior");
        });

        caso("cargo majoritário explica que vence o mais votado (polimorfismo)", () -> {
            digitar("12345");
            controlador.confirmar();
            digitar("11");
            verdade(ultimo().explicacao().contains("majoritário"), "explicação do majoritário");
        });

        caso("fluxo completo termina em FIM, grava e desabilita as teclas", () -> {
            digitar("12345");
            controlador.confirmar();
            digitar("22");
            controlador.confirmar();
            EstadoTela e = ultimo();
            igual(Modo.FIM, e.modo());
            verdade(e.finalizada(), "finalizada");
            verdade(!e.controlesHabilitados(), "teclas desabilitadas");
            igual(TipoMensagem.SUCESSO, e.tipoMensagem());
            igual(2, memoria.getVotos().size());
        });

        caso("depois do FIM as teclas são ignoradas", () -> {
            digitar("12345");
            controlador.confirmar();
            digitar("11");
            controlador.confirmar();
            int antes = publicados.size();
            controlador.digitar('5');
            controlador.branco();
            controlador.confirmar();
            igual(antes, publicados.size());
            igual(2, memoria.getVotos().size());
        });

        caso("nova votação depois do FIM volta ao começo", () -> {
            digitar("12345");
            controlador.confirmar();
            digitar("11");
            controlador.confirmar();
            controlador.novaVotacao();
            igual(Modo.DIGITANDO, ultimo().modo());
            igual("VEREADOR", ultimo().cargo());
            verdade(ultimo().controlesHabilitados(), "teclas habilitadas");
        });

        teste("falha ao gravar mostra erro, mantém as teclas e permite tentar de novo", () -> {
            AtomicInteger tentativas = new AtomicInteger();
            VotoRepositoryEmMemoria destino = new VotoRepositoryEmMemoria();
            preparar((sessaoId, votos) -> {
                if (tentativas.incrementAndGet() == 1) {
                    throw new RepositorioException("Sem internet.", null);
                }
                destino.salvarSessao(sessaoId, votos);
            });
            digitar("12345");
            controlador.confirmar();
            digitar("11");
            controlador.confirmar();

            EstadoTela falha = ultimo();
            igual(TipoMensagem.ERRO, falha.tipoMensagem());
            igual("Sem internet.", falha.mensagem());
            verdade(falha.controlesHabilitados(), "teclas voltam a funcionar");
            verdade(!falha.finalizada(), "não finalizada");

            controlador.confirmar();
            verdade(ultimo().finalizada(), "finalizada na segunda tentativa");
            igual(2, destino.getVotos().size());
        });

        teste("sem internet e com reserva: termina a votação e avisa onde os votos ficaram", () -> {
            VotoRepositoryEmArquivo reserva = new VotoRepositoryEmArquivo(
                    Files.createTempDirectory("urna-testes").resolve("pendentes.tsv"), "reserva");
            VotoRepository nuvemFora = (sessaoId, votos) -> {
                throw new RepositorioException("sem rede", null);
            };
            preparar(new VotoRepositoryComReserva(nuvemFora, reserva));
            digitar("12345");
            controlador.confirmar();
            digitar("11");
            controlador.confirmar();

            EstadoTela e = ultimo();
            verdade(e.finalizada(), "finalizada");
            igual(TipoMensagem.AVISO, e.tipoMensagem());
            verdade(e.mensagem().contains("guardados neste computador"), "explica a reserva");
            igual(1, reserva.lerSessoes().size());
        });

        grupo("Tela: gravação em segundo plano");

        teste("enquanto grava, as teclas ficam desabilitadas e novos toques são ignorados", () -> {
            prepararAssincrono();
            digitar("12345");
            controlador.confirmar();
            verdade(!ultimo().controlesHabilitados(), "teclas desabilitadas durante a gravação");
            int antes = publicados.size();
            controlador.digitar('9');
            controlador.confirmar();
            igual(antes, publicados.size());
            igual(1, filaDeGravacao.size()); // só uma gravação foi pedida

            rodarFila();
            igual("PREFEITO", ultimo().cargo());
            verdade(ultimo().controlesHabilitados(), "teclas voltam depois da gravação");
        });

        teste("resultado de uma votação antiga não afeta a nova votação", () -> {
            prepararAssincrono();
            digitar("12345");
            controlador.confirmar();
            controlador.novaVotacao();       // recomeçou antes de a gravação terminar
            rodarFila();                     // resultado antigo chega agora e deve ser ignorado
            igual("VEREADOR", ultimo().cargo());
            igual(Modo.DIGITANDO, ultimo().modo());
            igual("", ultimo().digitado());
        });
    }
}
