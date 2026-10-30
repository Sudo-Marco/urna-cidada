package com.urnacidada.repository;

import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final Map<Path, ReentrantLock> TRAVAS_DA_JVM = new ConcurrentHashMap<>();

    /** Uma sessão lida do arquivo. */
    public record SessaoSalva(String sessaoId, List<Voto> votos) { }

    private final Path arquivo;
    private final String rotulo;

    public VotoRepositoryEmArquivo(Path arquivo, String rotulo) {
        this.arquivo = arquivo;
        this.rotulo = rotulo;
    }

    @Override
    public void salvarSessao(String sessaoId, List<Voto> votos) throws RepositorioException {
        comTrava(() -> {
            List<String> linhas = new ArrayList<>(lerLinhas());
            linhas.removeIf(linha -> sessaoId.equals(idDaLinha(linha)));
            linhas.add(codificar(sessaoId, votos));
            escreverAtomico(linhas);
            return null;
        });
    }

    /** Sessões válidas do arquivo. Linhas ilegíveis são ignoradas (e mantidas no arquivo). */
    public List<SessaoSalva> lerSessoes() throws RepositorioException {
        return comTrava(() -> {
            List<SessaoSalva> sessoes = new ArrayList<>();
            for (String linha : lerLinhas()) {
                try {
                    sessoes.add(decodificar(linha));
                } catch (RuntimeException ignorada) {
                    // linha corrompida: não é apagada, só não é usada
                }
            }
            return List.copyOf(sessoes);
        });
    }

    /** Remove do arquivo as sessões indicadas. */
    public void removerSessoes(Set<String> ids) throws RepositorioException {
        comTrava(() -> {
            List<String> linhas = new ArrayList<>(lerLinhas());
            linhas.removeIf(linha -> ids.contains(idDaLinha(linha)));
            escreverAtomico(linhas);
            return null;
        });
    }

    @Override
    public String descricao() {
        return rotulo;
    }

    @Override
    public String estadoPersistencia() { return rotulo; }

    @Override
    public String dicaPersistencia() { return "Dados locais: " + arquivo.toAbsolutePath(); }

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

    private void escreverAtomico(List<String> linhas) throws RepositorioException {
        Path temporario = null;
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            temporario = Files.createTempFile(pasta, arquivo.getFileName().toString(), ".tmp");
            String conteudo = linhas.isEmpty() ? "" : String.join(System.lineSeparator(), linhas) + System.lineSeparator();
            Files.writeString(temporario, conteudo, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                canal.force(true);
            }
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RepositorioException("Não foi possível gravar o arquivo local " + arquivo + ".", e);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignorada) { }
            }
        }
    }

    private <T> T comTrava(OperacaoComTrava<T> operacao) throws RepositorioException {
        Path normalizado = arquivo.toAbsolutePath().normalize();
        ReentrantLock local = TRAVAS_DA_JVM.computeIfAbsent(normalizado, ignorada -> new ReentrantLock());
        local.lock();
        Path pasta = normalizado.getParent();
        Path arquivoTrava = normalizado.resolveSibling(normalizado.getFileName() + ".lock");
        try {
            if (pasta != null) Files.createDirectories(pasta);
            try (FileChannel canal = FileChannel.open(arquivoTrava, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock trava;
                try {
                    trava = canal.lock();
                } catch (OverlappingFileLockException ocupado) {
                    throw new RepositorioException("Outra instância da urna está usando os dados. Feche-a e tente novamente.", ocupado);
                }
                FileLock adquirida = trava;
                try (adquirida) {
                    return operacao.executar();
                }
            }
        } catch (IOException e) {
            throw new RepositorioException("Não foi possível proteger o arquivo local " + normalizado + ".", e);
        } finally {
            local.unlock();
        }
    }

    @FunctionalInterface
    private interface OperacaoComTrava<T> {
        T executar() throws RepositorioException;
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
        try {
            return decodificarTexto(tab < 0 ? linha : linha.substring(0, tab));
        } catch (IllegalArgumentException linhaCorrompida) {
            return null; // mantém a linha ilegível sem impedir que outras sessões sejam gravadas/removidas
        }
    }

    private static String codificarTexto(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static String decodificarTexto(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }
}
