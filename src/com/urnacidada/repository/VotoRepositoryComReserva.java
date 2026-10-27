package com.urnacidada.repository;

import com.urnacidada.model.Voto;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tenta gravar no repositório principal (por exemplo, o Firestore). Se ele estiver
 * indisponível, guarda a sessão num arquivo local e a envia depois, quando houver conexão.
 *
 * <p>Assim, um problema de internet durante a oficina não impede o participante de
 * terminar a votação nem faz o voto se perder.</p>
 */
public final class VotoRepositoryComReserva implements VotoRepository, AutoCloseable {

    private final VotoRepository principal;
    private final VotoRepositoryEmArquivo reserva;
    private volatile String aviso;

    public VotoRepositoryComReserva(VotoRepository principal, VotoRepositoryEmArquivo reserva) {
        this.principal = principal;
        this.reserva = reserva;
    }

    @Override
    public void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        aviso = null;
        try {
            principal.salvarSessao(sessaoId, votos);
            sincronizarPendentes(); // aproveita a conexão para enviar o que ficou guardado
        } catch (RepositorioException falhaPrincipal) {
            try {
                reserva.salvarSessao(sessaoId, votos);
                aviso = "Sem conexão com a nuvem. Os votos foram guardados neste computador "
                        + "e serão enviados quando houver conexão.";
            } catch (RepositorioException falhaReserva) {
                falhaReserva.addSuppressed(falhaPrincipal);
                throw new RepositorioException(
                        "Não foi possível gravar os votos nem na reserva local. Confirme novamente.",
                        falhaReserva);
            }
        }
    }

    /**
     * Envia ao repositório principal as sessões guardadas na reserva.
     *
     * @return quantas sessões foram enviadas agora (0 se não havia nada ou se ainda está sem conexão)
     */
    public int sincronizarPendentes() {
        try {
            Set<String> enviadas = new HashSet<>();
            for (VotoRepositoryEmArquivo.SessaoSalva sessao : reserva.lerSessoes()) {
                try {
                    principal.salvarSessao(sessao.sessaoId(), sessao.votos());
                    enviadas.add(sessao.sessaoId());
                } catch (RepositorioException aindaSemConexao) {
                    break; // se uma falhou, as próximas também falhariam
                }
            }
            if (!enviadas.isEmpty()) {
                reserva.removerSessoes(enviadas);
            }
            return enviadas.size();
        } catch (RepositorioException | RuntimeException e) {
            return 0;
        }
    }

    @Override
    public String descricao() {
        return principal.descricao() + " + reserva local";
    }

    @Override
    public Optional<String> avisoDaUltimaGravacao() {
        return Optional.ofNullable(aviso);
    }

    @Override
    public void close() {
        if (principal instanceof AutoCloseable recurso) {
            try {
                recurso.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignorada) {
                // encerrando o programa: não há o que fazer se o fechamento falhar
            }
        }
    }
}
