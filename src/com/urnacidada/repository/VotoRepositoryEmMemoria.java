package com.urnacidada.repository;

import com.urnacidada.model.Voto;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Guarda os votos apenas na memória do processo. Serve para demonstração e para testes. */
public class VotoRepositoryEmMemoria implements VotoRepository {

    private final List<Voto> votos = new CopyOnWriteArrayList<>();

    @Override
    public void salvarSessao(String sessaoId, List<Voto> novos) {
        votos.addAll(novos);
    }

    public List<Voto> getVotos() {
        return List.copyOf(votos);
    }

    @Override
    public String descricao() {
        return "Somente memória (nada é salvo ao fechar)";
    }
}
