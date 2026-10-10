import com.urnacidada.model.Eleicao;
import com.urnacidada.model.TipoVoto;
import com.urnacidada.model.Voto;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepository;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.service.UrnaService;

import java.util.List;

/**
 * Testes simples, sem nenhuma biblioteca. Cada teste imprime OK ou FALHA.
 * Para rodar, veja o LEIA-ME.txt.
 */
public class TesteUrna {

    static int passou = 0, falhou = 0;
    static VotoRepositoryEmMemoria repositorio;
    static UrnaService urna;

    // ---------- ajudantes ----------
    interface Teste { void rodar() throws Exception; }

    static void teste(String nome, Teste t) {
        repositorio = new VotoRepositoryEmMemoria();
        urna = new UrnaService(Eleicao.exemplo(), repositorio);
        try {
            t.rodar();
            passou++;
            System.out.println("OK     " + nome);
        } catch (Throwable e) {
            falhou++;
            System.out.println("FALHA  " + nome + "  ->  " + e);
        }
    }

    static void digitar(UrnaService u, String numero) {
        for (char c : numero.toCharArray()) u.digitar(c);
    }

    static void igual(Object esperado, Object obtido) {
        if (!java.util.Objects.equals(esperado, obtido))
            throw new AssertionError("esperava " + esperado + " mas veio " + obtido);
    }

    static void verdade(boolean condicao) {
        if (!condicao) throw new AssertionError("esperava verdadeiro");
    }

    /** Verifica que o trecho lança a exceção indicada. */
    static void lanca(Class<? extends Throwable> tipo, Teste t) {
        try {
            t.rodar();
        } catch (Throwable e) {
            if (tipo.isInstance(e)) return;
            throw new AssertionError("lançou " + e + " em vez de " + tipo.getSimpleName());
        }
        throw new AssertionError("deveria ter lançado " + tipo.getSimpleName());
    }

    // ---------- os testes ----------
    public static void main(String[] args) {

        teste("fluxo começa pelo primeiro cargo", () ->
            igual("Vereador", urna.getCargoAtual().nome()));

        teste("número válido mostra o candidato", () -> {
            digitar(urna, "12345");
            igual("Ana Exemplo", urna.getCandidatoAtual().orElseThrow().nome());
        });

        teste("número incompleto não mostra candidato", () -> {
            digitar(urna, "123");
            verdade(urna.getCandidatoAtual().isEmpty());
        });

        teste("não aceita letras", () -> {
            lanca(IllegalArgumentException.class, () -> urna.digitar('a'));
            igual("", urna.getDigitado());
        });

        teste("não aceita mais dígitos que o cargo permite", () -> {
            digitar(urna, "12345");
            lanca(IllegalStateException.class, () -> urna.digitar('6'));
        });

        teste("não confirma com número incompleto", () -> {
            digitar(urna, "123");
            lanca(IllegalStateException.class, () -> urna.confirmar());
            igual("Vereador", urna.getCargoAtual().nome());
        });

        teste("corrigir apaga o que foi digitado", () -> {
            digitar(urna, "123");
            urna.corrigir();
            igual("", urna.getDigitado());
        });

        teste("confirmar avança para o segundo cargo", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            igual("Prefeito", urna.getCargoAtual().nome());
            igual("", urna.getDigitado());
        });

        teste("número inexistente vira nulo", () -> {
            digitar(urna, "99999");
            urna.confirmar();
            igual(TipoVoto.NULO, urna.getVotosRegistrados().get(0).tipo());
        });

        teste("voto em branco funciona", () -> {
            urna.votarEmBranco();
            urna.confirmar();
            Voto voto = urna.getVotosRegistrados().get(0);
            igual(TipoVoto.BRANCO, voto.tipo());
            igual(null, voto.numero());
        });

        teste("não pode votar em branco depois de digitar", () -> {
            digitar(urna, "1");
            lanca(IllegalStateException.class, () -> urna.votarEmBranco());
        });

        teste("não pode digitar depois de marcar branco", () -> {
            urna.votarEmBranco();
            lanca(IllegalStateException.class, () -> urna.digitar('1'));
        });

        teste("fluxo completo dos dois cargos salva os votos", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            digitar(urna, "22");
            urna.confirmar();
            verdade(urna.isFinalizada());
            List<Voto> salvos = repositorio.getVotos();
            igual(2, salvos.size());
            igual("Vereador", salvos.get(0).cargo());
            igual("Prefeito", salvos.get(1).cargo());
            igual(TipoVoto.VALIDO, salvos.get(1).tipo());
        });

        teste("votos não são salvos antes de terminar", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            igual(0, repositorio.getVotos().size());
        });

        teste("depois de encerrar não aceita mais ações", () -> {
            digitar(urna, "12345");
            urna.confirmar();
            digitar(urna, "11");
            urna.confirmar();
            lanca(IllegalStateException.class, () -> urna.digitar('1'));
        });

        teste("falha ao salvar não encerra a votação", () -> {
            VotoRepository quebrado = (sessaoId, votos) -> {
                throw new RepositorioException("sem internet", null);
            };
            UrnaService semRede = new UrnaService(Eleicao.exemplo(), quebrado);
            digitar(semRede, "12345");
            semRede.confirmar();
            digitar(semRede, "11");
            lanca(RepositorioException.class, () -> semRede.confirmar());
            verdade(!semRede.isFinalizada());
        });

        System.out.println("\n" + passou + " passaram, " + falhou + " falharam");
        if (falhou > 0) System.exit(1);
    }
}
