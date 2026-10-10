package com.urnacidada.repository;

import com.urnacidada.model.Voto;
import java.util.List;

/**
 * Contrato de persistência. A lógica da urna só conhece ESTA interface,
 * nunca o Firestore. Quem fizer a frente Firebase cria a classe
 * FirestoreVotoRepository implementando-a.
 */
public interface VotoRepository {
    void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException;
}
