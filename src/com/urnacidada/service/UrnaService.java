package com.urnacidada.service;

import com.urnacidada.model.Cargo;
import com.urnacidada.model.Candidato;
import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Regras e fluxo da votação (SEM interface gráfica e SEM Firestore).
 * A tela Swing apenas chama estes métodos e mostra o que eles retornam.
 *
 * Regras adotadas (rascunho, validar com o grupo/professor):
 *  - o eleitor digita o número do candidato, dígito a dígito;
 *  - BRANCO só é permitido se nenhum dígito foi digitado;
 *  - CORRIGIR apaga o que foi digitado/o branco do cargo atual;
 *  - CONFIRMAR exige número completo ou branco;
 *  - número completo que não existe vira voto NULO;
 *  - os votos só são salvos ao terminar o último cargo.
 */
public class UrnaService {

    private final Eleicao eleicao;
    private final VotoRepository repositorio;
    private final String sessaoId = UUID.randomUUID().toString();
    private final List<Voto> votos = new ArrayList<>();

    private int indiceCargo = 0;
    private StringBuilder digitado = new StringBuilder();
    private boolean branco = false;
    private boolean finalizada = false;

    public UrnaService(Eleicao eleicao, VotoRepository repositorio) {
        this.eleicao = eleicao;
        this.repositorio = repositorio;
    }

    // ---------- consulta de estado (a tela usa para se desenhar) ----------

    public Cargo getCargoAtual() {
        verificarEmAndamento();
        return eleicao.cargos().get(indiceCargo);
    }

    public String getDigitado() {
        return digitado.toString();
    }

    public boolean isBranco() {
        return branco;
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    /** Candidato correspondente ao número digitado (só quando o número está completo). */
    public Optional<Candidato> getCandidatoAtual() {
        if (finalizada || !numeroCompleto()) {
            return Optional.empty();
        }
        return getCargoAtual().buscarCandidato(digitado.toString());
    }

    public List<Voto> getVotosRegistrados() {
        return List.copyOf(votos);
    }

    // ---------- ações do eleitor ----------

    public void digitar(char digito) {
        verificarEmAndamento();
        if (!Character.isDigit(digito)) {
            throw new IllegalArgumentException("Digite apenas números.");
        }
        if (branco) {
            throw new IllegalStateException("Voto em branco marcado. Use CORRIGE para digitar um número.");
        }
        if (numeroCompleto()) {
            throw new IllegalStateException("Número completo. Confirme ou use CORRIGE.");
        }
        digitado.append(digito);
    }

    public void votarEmBranco() {
        verificarEmAndamento();
        if (digitado.length() > 0) {
            throw new IllegalStateException("Use CORRIGE antes de votar em branco.");
        }
        branco = true;
    }

    public void corrigir() {
        verificarEmAndamento();
        digitado = new StringBuilder();
        branco = false;
    }

    /**
     * Confirma o voto do cargo atual e avança.
     * @throws RepositorioException se, ao terminar, não for possível salvar os votos.
     *         Nesse caso a urna NÃO é finalizada e o eleitor pode tentar confirmar de novo.
     */
    public void confirmar() throws RepositorioException {
        verificarEmAndamento();
        if (!branco && !numeroCompleto()) {
            throw new IllegalStateException("Digite o número completo ou vote em branco.");
        }

        Voto voto = criarVoto();
        boolean ultimoCargo = indiceCargo == eleicao.cargos().size() - 1;

        if (ultimoCargo) {
            List<Voto> todos = new ArrayList<>(votos);
            todos.add(voto);
            repositorio.salvarSessao(sessaoId, todos); // se falhar, nada muda aqui
            votos.add(voto);
            finalizada = true;
        } else {
            votos.add(voto);
            indiceCargo++;
            corrigir();
        }
    }

    // ---------- internos ----------

    private Voto criarVoto() {
        Cargo cargo = getCargoAtual();
        if (branco) {
            return new Voto(cargo.nome(), TipoVoto.BRANCO, null, Instant.now());
        }
        String numero = digitado.toString();
        TipoVoto tipo = cargo.buscarCandidato(numero).isPresent() ? TipoVoto.VALIDO : TipoVoto.NULO;
        return new Voto(cargo.nome(), tipo, numero, Instant.now());
    }

    private boolean numeroCompleto() {
        return digitado.length() == eleicao.cargos().get(indiceCargo).digitos();
    }

    private void verificarEmAndamento() {
        if (finalizada) {
            throw new IllegalStateException("A votação já foi encerrada.");
        }
    }
}
