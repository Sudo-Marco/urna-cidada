package com.urnacidada.view;

import com.urnacidada.model.Candidato;
import com.urnacidada.model.Cargo;
import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.service.UrnaService;
import com.urnacidada.view.EstadoTela.Modo;
import com.urnacidada.view.EstadoTela.TipoMensagem;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Faz a ponte entre a tela e o {@link UrnaService}: recebe os pedidos do participante,
 * chama o serviço e publica um novo {@link EstadoTela}.
 *
 * <p>Não depende de Swing, então pode ser testado sem abrir janela. A gravação dos votos
 * (que pode demorar, por usar a rede) roda em {@code segundoPlano}, e o resultado volta
 * pela {@code threadDaTela}. Todos os métodos públicos devem ser chamados na thread da tela.</p>
 */
public final class ControladorUrna implements AcoesUrna {

    private final Eleicao eleicao;
    private final VotoRepository repositorio;
    private final Executor segundoPlano;
    private final Executor threadDaTela;
    private final Consumer<EstadoTela> aoMudar;
    private final Runnable aoPedirDicas;
    private final Supplier<String> idSessao;
    private final boolean umaSessaoPorConta;

    private UrnaService urna;
    private boolean gravando;
    private String mensagem = "";
    private TipoMensagem tipoMensagem = TipoMensagem.NENHUMA;
    private boolean sessaoConcluida;

    public ControladorUrna(Eleicao eleicao, VotoRepository repositorio, Executor segundoPlano,
                           Executor threadDaTela, Consumer<EstadoTela> aoMudar, Runnable aoPedirDicas) {
        this(eleicao, repositorio, segundoPlano, threadDaTela, aoMudar, aoPedirDicas,
                () -> java.util.UUID.randomUUID().toString(), false);
    }

    public ControladorUrna(Eleicao eleicao, VotoRepository repositorio, Executor segundoPlano,
                           Executor threadDaTela, Consumer<EstadoTela> aoMudar, Runnable aoPedirDicas,
                           Supplier<String> idSessao) {
        this(eleicao, repositorio, segundoPlano, threadDaTela, aoMudar, aoPedirDicas, idSessao, false);
    }

    public ControladorUrna(Eleicao eleicao, VotoRepository repositorio, Executor segundoPlano,
                           Executor threadDaTela, Consumer<EstadoTela> aoMudar, Runnable aoPedirDicas,
                           Supplier<String> idSessao, boolean umaSessaoPorConta) {
        this.eleicao = Objects.requireNonNull(eleicao, "eleicao");
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.segundoPlano = Objects.requireNonNull(segundoPlano, "segundoPlano");
        this.threadDaTela = Objects.requireNonNull(threadDaTela, "threadDaTela");
        this.aoMudar = Objects.requireNonNull(aoMudar, "aoMudar");
        this.aoPedirDicas = Objects.requireNonNull(aoPedirDicas, "aoPedirDicas");
        this.idSessao = Objects.requireNonNull(idSessao, "idSessao");
        this.umaSessaoPorConta = umaSessaoPorConta;
    }

    // ---------------------------------------------------------------- ações

    @Override
    public void novaVotacao() {
        if (umaSessaoPorConta && sessaoConcluida) {
            informar(TipoMensagem.AVISO, "Esta conta já concluiu a votação.");
            publicar();
            return;
        }
        try {
            urna = new UrnaService(eleicao, repositorio, idSessao.get());
        } catch (IllegalArgumentException e) {
            urna = null;
            informar(TipoMensagem.ERRO, e.getMessage());
            publicar();
            return;
        }
        gravando = false;
        informar(TipoMensagem.INFO, "Votação iniciada. Primeiro cargo: " + urna.getCargoAtual().nome() + ".");
        publicar();
    }

    @Override
    public void digitar(char digito) {
        if (!podeAgir()) {
            return;
        }
        if (!executar(() -> urna.digitar(digito))) {
            return;
        }
        switch (modo()) {
            case CANDIDATO -> informar(TipoMensagem.SUCESSO, "Confira o candidato e confirme, ou corrija.");
            case NUMERO_ERRADO -> informar(TipoMensagem.AVISO,
                    "Número não cadastrado. Confirme para registrar voto nulo ou corrija.");
            default -> informar(TipoMensagem.INFO, "Digite o número e confirme sua escolha.");
        }
        publicar();
    }

    @Override
    public void branco() {
        if (!podeAgir() || !executar(urna::votarEmBranco)) {
            return;
        }
        informar(TipoMensagem.INFO, "Voto em branco marcado. Confirme ou corrija.");
        publicar();
    }

    @Override
    public void corrigir() {
        if (!podeAgir() || !executar(urna::corrigir)) {
            return;
        }
        informar(TipoMensagem.INFO, "Escolha apagada. Digite novamente ou marque branco.");
        publicar();
    }

    @Override
    public void confirmar() {
        if (!podeAgir()) {
            return;
        }
        gravando = true;
        informar(TipoMensagem.INFO, "Registrando seu voto… aguarde.");
        publicar();

        UrnaService sessao = urna;
        segundoPlano.execute(() -> {
            Exception falha = null;
            try {
                sessao.confirmar();
            } catch (Exception e) {
                falha = e;
            }
            Exception resultado = falha;
            threadDaTela.execute(() -> aoTerminarConfirmacao(sessao, resultado));
        });
    }

    @Override
    public void mostrarDicas() {
        aoPedirDicas.run();
    }

    // ---------------------------------------------------------------- consulta

    /** Estado atual, útil para testes e para a primeira exibição. */
    public EstadoTela estado() {
        if (urna == null) {
            return new EstadoTela(Modo.AGUARDANDO_INICIO, "AGUARDANDO INÍCIO", "", 0, "",
                    "Preparando votação", "", "", mensagem, tipoMensagem, false);
        }
        Modo modo = modo();
        if (modo == Modo.FIM) {
            return new EstadoTela(modo, "VOTAÇÃO CONCLUÍDA", "", 0, "", "FIM",
                    "Obrigado por participar da simulação.", "", mensagem, tipoMensagem, false);
        }

        Cargo cargo = urna.getCargoAtual();
        String progresso = "CARGO " + urna.getNumeroCargoAtual() + " DE " + urna.getTotalCargos();
        boolean ativo = !gravando;
        Optional<Candidato> candidato = urna.getCandidatoAtual();

        return switch (modo) {
            case CANDIDATO -> new EstadoTela(modo, cargo.nome().toUpperCase(Locale.ROOT), progresso, cargo.digitos(),
                    urna.getDigitado(), candidato.orElseThrow().nome(), candidato.orElseThrow().partido(),
                    candidato.orElseThrow().explicacaoCargo(), mensagem, tipoMensagem, ativo);
            case NUMERO_ERRADO -> new EstadoTela(modo, cargo.nome().toUpperCase(Locale.ROOT), progresso, cargo.digitos(),
                    urna.getDigitado(), "NÚMERO ERRADO", "VOTO NULO", "", mensagem, tipoMensagem, ativo);
            case BRANCO -> new EstadoTela(modo, cargo.nome().toUpperCase(Locale.ROOT), progresso, 0, "",
                    "VOTO EM BRANCO", "", "", mensagem, tipoMensagem, ativo);
            default -> {
                Optional<String> partidoLegenda = urna.getPartidoLegendaAtual();
                yield new EstadoTela(modo, cargo.nome().toUpperCase(Locale.ROOT), progresso, cargo.digitos(),
                        urna.getDigitado(), partidoLegenda.isPresent() ? "VOTO DE LEGENDA" : "Digite o número do candidato",
                        partidoLegenda.orElse(""), partidoLegenda.map(p -> "Confirme para votar na legenda do partido.").orElse(""),
                        mensagem, tipoMensagem, ativo);
            }
        };
    }

    // ---------------------------------------------------------------- internos

    private void aoTerminarConfirmacao(UrnaService sessao, Exception falha) {
        if (sessao != urna) {
            return; // uma nova votação já começou; ignora o resultado antigo
        }
        gravando = false;
        if (falha != null) {
            String texto = falha.getMessage() == null || falha.getMessage().isBlank()
                    ? "Não foi possível confirmar o voto." : falha.getMessage();
            informar(TipoMensagem.ERRO, texto);
        } else if (sessao.isFinalizada()) {
            sessaoConcluida = true;
            Optional<String> aviso = repositorio.avisoDaUltimaGravacao();
            if (aviso.isPresent()) {
                informar(TipoMensagem.AVISO, aviso.get());
            } else {
                informar(TipoMensagem.SUCESSO,
                        "Você já votou. Obrigado por participar. A próxima identificação abrirá em cinco segundos.");
            }
        } else {
            informar(TipoMensagem.SUCESSO,
                    "Voto confirmado. Próximo cargo: " + sessao.getCargoAtual().nome() + ".");
        }
        publicar();
    }

    private boolean executar(Runnable acao) {
        try {
            acao.run();
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            informar(TipoMensagem.ERRO, e.getMessage());
            publicar();
            return false;
        }
    }

    private Modo modo() {
        if (urna == null) return Modo.AGUARDANDO_INICIO;
        if (urna.isFinalizada()) {
            return Modo.FIM;
        }
        if (urna.isBranco()) {
            return Modo.BRANCO;
        }
        return modoPorNumero();
    }

    private Modo modoPorNumero() {
        Cargo cargo = urna.getCargoAtual();
        if (urna.getDigitado().length() < cargo.digitos()) {
            return Modo.DIGITANDO;
        }
        return urna.getCandidatoAtual().isPresent() ? Modo.CANDIDATO : Modo.NUMERO_ERRADO;
    }

    private boolean podeAgir() {
        return urna != null && !gravando && !urna.isFinalizada();
    }

    private void informar(TipoMensagem tipo, String texto) {
        this.tipoMensagem = tipo;
        this.mensagem = texto;
    }

    private void publicar() {
        aoMudar.accept(estado());
    }
}
