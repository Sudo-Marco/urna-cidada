package com.urnacidada.repository;

import com.urnacidada.model.Voto;
import java.util.ArrayList;
import java.util.List;

/** Implementação provisória (guarda na memória). Útil para testes e para começar sem Firebase. */
public class VotoRepositoryEmMemoria implements VotoRepository {

    private final List<Voto> votos = new ArrayList<>();

    @Override
    public void salvarSessao(String sessaoId, List<Voto> novos) {
        votos.addAll(novos);
    }

    public List<Voto> getVotos() {
        return List.copyOf(votos);
    }
}
