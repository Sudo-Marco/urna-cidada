package com.urnacidada.repository;

import com.urnacidada.model.Voto;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência. A lógica da urna só conhece esta interface; as implementações
 * podem gravar em memória, em arquivo local ou no Cloud Firestore.
 */
public interface VotoRepository {

    /**
     * Grava os votos de uma sessão. Deve ser idempotente: repetir a mesma sessão não pode
     * duplicar votos.
     */
    void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException;

    /** Texto curto que a tela mostra para informar onde os votos são guardados. */
    default String descricao() {
        return getClass().getSimpleName();
    }

    /** Aviso opcional sobre a última gravação (por exemplo, "ficou na reserva local"). */
    default Optional<String> avisoDaUltimaGravacao() {
        return Optional.empty();
    }
}
