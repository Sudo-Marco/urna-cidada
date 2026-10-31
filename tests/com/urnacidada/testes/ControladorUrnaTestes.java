package com.urnacidada.testes;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.view.ControladorUrna;
import com.urnacidada.view.EstadoTela;
import com.urnacidada.view.EstadoTela.Modo;
import com.urnacidada.view.EstadoTela.TipoMensagem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static com.urnacidada.testes.MiniTeste.*;

/** Testes do estado apresentado pela interface e da gravação em segundo plano. */
final class ControladorUrnaTestes {
    private static VotoRepositoryEmMemoria memoria;
    private static ControladorUrna controlador;
    private static List<EstadoTela> publicados;
    private static List<Runnable> fila;
    private ControladorUrnaTestes() { }
    private static void preparar(VotoRepository repo) {
        publicados = new ArrayList<>(); controlador = new ControladorUrna(Eleicao.exemplo(), repo, Runnable::run, Runnable::run, publicados::add, () -> { }); controlador.novaVotacao();
    }
    private static void prepararAssincrono() {
        memoria = new VotoRepositoryEmMemoria(); publicados = new ArrayList<>(); fila = new ArrayList<>();
        controlador = new ControladorUrna(Eleicao.exemplo(), memoria, fila::add, Runnable::run, publicados::add, () -> { }); controlador.novaVotacao();
    }
    private static void digitar(String s) { for (char c : s.toCharArray()) controlador.digitar(c); }
    private static EstadoTela ultimo() { return publicados.get(publicados.size()-1); }
    private static void rodarFila() { List<Runnable> tarefas = new ArrayList<>(fila); fila.clear(); tarefas.forEach(Runnable::run); }
    private static void votarTudo(String... numeros) { for (String n : numeros) { digitar(n); controlador.confirmar(); } }
    private static void caso(String nome, MiniTeste.Corpo corpo) { teste(nome, () -> { memoria = new VotoRepositoryEmMemoria(); preparar(memoria); corpo.rodar(); }); }
    static void executar() {
        grupo("Tela: estado apresentado pelo ControladorUrna");
        caso("inicia no primeiro dos cinco cargos", () -> { igual("DEPUTADO FEDERAL", ultimo().cargo()); igual("CARGO 1 DE 5", ultimo().progresso()); igual(4, ultimo().posicoes()); });
        caso("candidato válido mostra nome, partido e explicação", () -> { digitar("8101"); igual(Modo.CANDIDATO, ultimo().modo()); igual("Ari Lume", ultimo().titulo()); igual("Partido Horizonte Comunitário (fictício)", ultimo().detalhe()); verdade(ultimo().explicacao().contains("proporcional")); });
        caso("número completo desconhecido avisa que será nulo", () -> { digitar("9999"); igual(Modo.NUMERO_ERRADO, ultimo().modo()); igual("VOTO NULO", ultimo().detalhe()); igual(TipoMensagem.AVISO, ultimo().tipoMensagem()); });
        caso("dois primeiros dígitos de partido mostram legenda", () -> { digitar("81"); igual("VOTO DE LEGENDA", ultimo().titulo()); igual("Partido Horizonte Comunitário (fictício)", ultimo().detalhe()); });
        caso("branco mostra confirmação e não exige caixas", () -> { controlador.branco(); igual(Modo.BRANCO, ultimo().modo()); igual(0, ultimo().posicoes()); });
        caso("corrigir limpa número e branco", () -> { digitar("81"); controlador.corrigir(); igual("", ultimo().digitado()); controlador.branco(); controlador.corrigir(); igual(Modo.DIGITANDO, ultimo().modo()); });
        caso("número incompleto não avança", () -> { digitar("8"); controlador.confirmar(); igual("DEPUTADO FEDERAL", ultimo().cargo()); igual(TipoMensagem.ERRO, ultimo().tipoMensagem()); });
        caso("confirmação avança sem gravar os votos ainda", () -> { digitar("8101"); controlador.confirmar(); igual("DEPUTADO ESTADUAL", ultimo().cargo()); igual("CARGO 2 DE 5", ultimo().progresso()); igual(0, memoria.getVotos().size()); });
        caso("governador mostra vice e explicação majoritária", () -> {
            votarTudo("8101", "81001", "820"); digitar("84");
            verdade(ultimo().explicacao().contains("majoritário"));
        });
        caso("sequência completa termina em FIM e grava cinco votos", () -> { votarTudo("8101", "81001", "820", "84", "85"); verdade(ultimo().finalizada()); igual(5, memoria.getVotos().size()); });
        teste("falha temporária do repositório é mostrada e pode ser repetida", () -> {
            AtomicInteger tentativas = new AtomicInteger(); VotoRepositoryEmMemoria destino = new VotoRepositoryEmMemoria();
            preparar((id, votos) -> { if (tentativas.getAndIncrement() == 0) throw new RepositorioException("sem rede", null); destino.salvarSessao(id, votos); });
            votarTudo("8101", "81001", "820", "84"); digitar("85"); controlador.confirmar();
            igual(TipoMensagem.ERRO, ultimo().tipoMensagem()); verdade(!ultimo().finalizada()); controlador.confirmar(); verdade(ultimo().finalizada());
        });
        grupo("Tela: gravação em segundo plano");
        teste("interface desabilita controles até a gravação terminar", () -> {
            prepararAssincrono(); digitar("8101"); controlador.confirmar(); verdade(!ultimo().controlesHabilitados()); int antes=publicados.size(); controlador.digitar('9'); igual(antes, publicados.size());
            fila.remove(0).run(); igual("DEPUTADO ESTADUAL", ultimo().cargo()); verdade(ultimo().controlesHabilitados());
        });
        teste("resultado de uma confirmação antiga não altera nova votação", () -> {
            prepararAssincrono(); digitar("8101"); controlador.confirmar(); controlador.novaVotacao(); rodarFila(); igual("DEPUTADO FEDERAL", ultimo().cargo()); igual("", ultimo().digitado());
        });
    }
}
