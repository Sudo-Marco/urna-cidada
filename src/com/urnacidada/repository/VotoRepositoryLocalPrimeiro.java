package com.urnacidada.repository;

import com.urnacidada.model.Voto;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Persiste cada sessão localmente antes de pedir sincronização assíncrona. */
public final class VotoRepositoryLocalPrimeiro implements VotoRepository, AutoCloseable {
    private static final long INTERVALO_INICIAL_SEGUNDOS = 30;
    private static final long INTERVALO_MAXIMO_SEGUNDOS = 300;

    private final VotoRepositoryEmArquivo pendentes;
    private final FirestoreRestVotoClient cliente;
    private final ScheduledExecutorService sincronizador;
    private final AtomicBoolean sincronizando = new AtomicBoolean();
    private final AtomicReference<String> estado = new AtomicReference<>("Online · tudo enviado");
    private final AtomicReference<String> aviso = new AtomicReference<>();
    private final boolean configuracaoValida;
    private volatile boolean configuracaoRejeitada;
    private volatile long proximoIntervalo = INTERVALO_INICIAL_SEGUNDOS;
    private volatile ScheduledFuture<?> tarefaAgendada;

    public VotoRepositoryLocalPrimeiro(VotoRepositoryEmArquivo pendentes,
                                       FirestoreRestVotoClient cliente,
                                       boolean configuracaoValida) {
        this(pendentes, cliente, configuracaoValida, "");
    }

    public VotoRepositoryLocalPrimeiro(VotoRepositoryEmArquivo pendentes,
                                       FirestoreRestVotoClient cliente,
                                       boolean configuracaoValida,
                                       String avisoConfiguracao) {
        this(pendentes, cliente, configuracaoValida, avisoConfiguracao, true);
    }

    public VotoRepositoryLocalPrimeiro(VotoRepositoryEmArquivo pendentes,
                                       FirestoreRestVotoClient cliente,
                                       boolean configuracaoValida,
                                       String avisoConfiguracao,
                                       boolean sincronizarAoIniciar) {
        this.pendentes = pendentes;
        this.cliente = cliente;
        this.configuracaoValida = configuracaoValida;
        this.sincronizador = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread t = new Thread(tarefa, "sincronizacao-firestore");
            t.setDaemon(true);
            return t;
        });
        if (!configuracaoValida) {
            estado.set("Modo local");
            if (avisoConfiguracao != null && !avisoConfiguracao.isBlank()) aviso.set(avisoConfiguracao);
        } else {
            estado.set("Firebase configurado · verificando pendências");
            if (sincronizarAoIniciar) sincronizador.execute(this::sincronizarAgora);
        }
    }

    @Override
    public void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        pendentes.salvarSessao(sessaoId, votos); // fonte de verdade: sempre local primeiro
        if (configuracaoValida) {
            aviso.set("Votação guardada neste computador; envio ao Firebase em segundo plano.");
            atualizarEstadoPendente();
        }
        if (configuracaoValida && !configuracaoRejeitada) {
            sincronizador.execute(this::sincronizarAgora);
        }
    }

    public int sincronizarAgora() {
        if (!configuracaoValida || configuracaoRejeitada || !sincronizando.compareAndSet(false, true)) return 0;
        try {
            List<VotoRepositoryEmArquivo.SessaoSalva> sessoes = pendentes.lerSessoes();
            if (sessoes.isEmpty()) {
                estado.set("Firebase configurado · sem pendências");
                aviso.set(null);
                proximoIntervalo = INTERVALO_INICIAL_SEGUNDOS;
                return 0;
            }
            Set<String> enviadas = new HashSet<>();
            for (VotoRepositoryEmArquivo.SessaoSalva sessao : sessoes) {
                try {
                    cliente.criarSessao(sessao.sessaoId(), sessao.votos());
                    enviadas.add(sessao.sessaoId());
                } catch (FalhaFirestore falha) {
                    if (falha.tipo() == FalhaFirestore.Tipo.USUARIO_JA_VOTOU) {
                        configuracaoRejeitada = true;
                        estado.set("Esta conta já votou");
                        aviso.set(falha.getMessage());
                    } else if (falha.tipo() == FalhaFirestore.Tipo.CONFIGURACAO) {
                        configuracaoRejeitada = true;
                        estado.set("Configuração do Firebase inválida");
                        aviso.set("Firebase rejeitou a configuração. A votação continua guardada localmente.");
                    } else {
                        atualizarEstadoPendente();
                        agendarNovaTentativa();
                    }
                    break;
                }
            }
            if (!enviadas.isEmpty()) pendentes.removerSessoes(enviadas);
            List<VotoRepositoryEmArquivo.SessaoSalva> restantes = pendentes.lerSessoes();
            if (restantes.isEmpty()) {
                estado.set("Online · tudo enviado");
                aviso.set(null);
                proximoIntervalo = INTERVALO_INICIAL_SEGUNDOS;
                return enviadas.size();
            }
            if (!configuracaoRejeitada) {
                atualizarEstadoPendente();
                if (enviadas.size() == sessoes.size()) proximoIntervalo = INTERVALO_INICIAL_SEGUNDOS;
            }
            return enviadas.size();
        } catch (RepositorioException e) {
            estado.set("Sem conexão · dados locais preservados");
            agendarNovaTentativa();
            return 0;
        } finally {
            sincronizando.set(false);
        }
    }

    public boolean temPendentes() throws RepositorioException {
        return !pendentes.lerSessoes().isEmpty();
    }

    private void atualizarEstadoPendente() {
        try {
            int votos = pendentes.lerSessoes().stream().mapToInt(s -> s.votos().size()).sum();
            estado.set("Sem conexão · " + votos + (votos == 1
                    ? " voto guardado neste computador" : " votos guardados neste computador"));
        } catch (RepositorioException e) {
            estado.set("Sem conexão · dados locais preservados");
        }
    }

    private synchronized void agendarNovaTentativa() {
        if (configuracaoRejeitada || sincronizador.isShutdown()) return;
        if (tarefaAgendada != null && !tarefaAgendada.isDone()) tarefaAgendada.cancel(false);
        long espera = proximoIntervalo;
        proximoIntervalo = Math.min(INTERVALO_MAXIMO_SEGUNDOS, proximoIntervalo * 2);
        tarefaAgendada = sincronizador.schedule(this::sincronizarAgora, espera, TimeUnit.SECONDS);
    }

    @Override
    public String descricao() { return estado.get(); }

    @Override
    public String estadoPersistencia() { return estado.get(); }

    @Override
    public String dicaPersistencia() { return "Dados locais: " + pendentes.arquivo().toAbsolutePath(); }

    @Override
    public java.util.Optional<String> avisoDaUltimaGravacao() {
        return java.util.Optional.ofNullable(aviso.get());
    }

    public static long proximoIntervalo(long atual) {
        return Math.min(INTERVALO_MAXIMO_SEGUNDOS, atual * 2);
    }

    @Override
    public void close() {
        if (tarefaAgendada != null) tarefaAgendada.cancel(false);
        if (configuracaoValida && !configuracaoRejeitada) {
            try {
                var tarefas = Executors.newSingleThreadExecutor(t -> {
                    Thread thread = new Thread(t, "sincronizacao-final");
                    thread.setDaemon(true);
                    return thread;
                });
                try {
                    tarefas.submit(() -> sincronizarComPrazoFinal()).get(5, TimeUnit.SECONDS);
                } finally {
                    tarefas.shutdownNow();
                }
            } catch (Exception ignorada) { }
        }
        sincronizador.shutdownNow();
    }

    private void sincronizarComPrazoFinal() {
        if (!sincronizando.compareAndSet(false, true)) return;
        try {
            for (VotoRepositoryEmArquivo.SessaoSalva sessao : pendentes.lerSessoes()) {
                try {
                    cliente.criarSessao(sessao.sessaoId(), sessao.votos(), Duration.ofSeconds(3));
                    pendentes.removerSessoes(Set.of(sessao.sessaoId()));
                } catch (RepositorioException e) {
                    break;
                }
            }
        } catch (RepositorioException ignorada) { }
        finally { sincronizando.set(false); }
    }
}
