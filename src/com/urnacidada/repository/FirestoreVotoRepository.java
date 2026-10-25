package com.urnacidada.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.urnacidada.model.Voto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Persistência anônima no Cloud Firestore.
 *
 * <p>Cada sessão vira um documento em {@code sessoes/{id}}, então repetir a gravação da mesma
 * sessão apenas atualiza o mesmo documento. A espera tem limite de tempo para a tela não
 * ficar travada quando não há internet.</p>
 */
public final class FirestoreVotoRepository implements VotoRepository, AutoCloseable {

    private static final long LIMITE_SEGUNDOS = 15;

    private final String projectId;
    private final Firestore firestore;

    public FirestoreVotoRepository(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Defina FIRESTORE_PROJECT_ID antes de iniciar.");
        }
        this.projectId = projectId;
        this.firestore = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    @Override
    public void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        List<Map<String, Object>> dadosVotos = votos.stream().map(voto -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cargo", voto.cargo());
            item.put("tipo", voto.tipo().name());
            item.put("numero", voto.numero());
            item.put("dataHora", voto.dataHora().toString());
            return item;
        }).toList();
        try {
            firestore.collection("sessoes").document(sessaoId)
                    .set(Map.of("votos", dadosVotos))
                    .get(LIMITE_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositorioException("Gravação interrompida. Tente confirmar novamente.", e);
        } catch (TimeoutException e) {
            throw new RepositorioException("O Firestore não respondeu a tempo. Verifique a conexão.", e);
        } catch (ExecutionException | RuntimeException e) {
            throw new RepositorioException(
                    "Não foi possível gravar no Firestore. Verifique a conexão e a configuração.", e);
        }
    }

    @Override
    public String descricao() {
        return "Cloud Firestore (projeto " + projectId + ")";
    }

    @Override
    public void close() {
        try {
            firestore.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignorada) {
            // encerrando o programa: não há o que fazer se o fechamento falhar
        }
    }
}
