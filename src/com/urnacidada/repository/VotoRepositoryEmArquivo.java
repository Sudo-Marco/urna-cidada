package com.urnacidada.repository;

import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guarda as sessões em um arquivo de texto local, uma sessão por linha.
 *
 * <p>Formato: {@code sessaoId <TAB> voto <TAB> voto ...}, em que cada voto é
 * {@code cargo,tipo,numero,instante} com os campos de texto codificados (URL encoding).
 * Gravar de novo a mesma sessão substitui a linha anterior, então não há duplicidade.</p>
 *
 * <p>Também serve de reserva quando a nuvem está indisponível: ver
 * {@link VotoRepositoryComReserva}.</p>
 */
public final class VotoRepositoryEmArquivo implements VotoRepository {

    /** Uma sessão lida do arquivo. */
    public record SessaoSalva(String sessaoId, List<Voto> votos) { }

    private final Path arquivo;
    private final String rotulo;

    public VotoRepositoryEmArquivo(Path arquivo, String rotulo) {
        this.arquivo = arquivo;
        this.rotulo = rotulo;
    }

    @Override
    public synchronized void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        List<String> linhas = new ArrayList<>(lerLinhas());
        linhas.removeIf(linha -> sessaoId.equals(idDaLinha(linha)));
        linhas.add(codificar(sessaoId, votos));
        escrever(linhas);
    }

    /** Sessões válidas do arquivo. Linhas ilegíveis são ignoradas (e mantidas no arquivo). */
    public synchronized List<SessaoSalva> lerSessoes() throws RepositorioException {
        List<SessaoSalva> sessoes = new ArrayList<>();
        for (String linha : lerLinhas()) {
            try {
                sessoes.add(decodificar(linha));
            } catch (RuntimeException ignorada) {
                // linha corrompida: não é apagada, só não é usada
            }
        }
        return sessoes;
    }

    /** Remove do arquivo as sessões indicadas. */
    public synchronized void removerSessoes(Set<String> ids) throws RepositorioException {
        List<String> linhas = new ArrayList<>(lerLinhas());
        linhas.removeIf(linha -> ids.contains(idDaLinha(linha)));
        escrever(linhas);
    }

    @Override
    public String descricao() {
        return rotulo;
    }

    public Path arquivo() {
        return arquivo;
    }

    // ------------------------------------------------------------------

    private List<String> lerLinhas() throws RepositorioException {
        if (!Files.exists(arquivo)) {
            return List.of();
        }
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8).stream()
                    .filter(linha -> !linha.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new RepositorioException("Não foi possível ler o arquivo local " + arquivo + ".", e);
        }
    }

    private void escrever(List<String> linhas) throws RepositorioException {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Files.write(arquivo, linhas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositorioException("Não foi possível gravar o arquivo local " + arquivo + ".", e);
        }
    }

    private static String codificar(String sessaoId, List<Voto> votos) {
        StringBuilder linha = new StringBuilder(codificarTexto(sessaoId));
        for (Voto voto : votos) {
            linha.append('\t')
                    .append(codificarTexto(voto.cargo())).append(',')
                    .append(voto.tipo().name()).append(',')
                    .append(voto.numero() == null ? "" : voto.numero()).append(',')
                    .append(voto.dataHora());
        }
        return linha.toString();
    }

    private static SessaoSalva decodificar(String linha) {
        String[] partes = linha.split("\t");
        List<Voto> votos = new ArrayList<>();
        for (int i = 1; i < partes.length; i++) {
            String[] campos = partes[i].split(",", -1);
            if (campos.length != 4) {
                throw new IllegalArgumentException("Voto malformado");
            }
            votos.add(new Voto(decodificarTexto(campos[0]), TipoVoto.valueOf(campos[1]),
                    campos[2].isEmpty() ? null : campos[2], Instant.parse(campos[3])));
        }
        return new SessaoSalva(decodificarTexto(partes[0]), List.copyOf(votos));
    }

    private static String idDaLinha(String linha) {
        int tab = linha.indexOf('\t');
        return decodificarTexto(tab < 0 ? linha : linha.substring(0, tab));
    }

    private static String codificarTexto(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static String decodificarTexto(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }
}
