package com.urnacidada;

import com.urnacidada.model.Eleicao;
import com.urnacidada.repository.RepositorioException;
import com.urnacidada.repository.VotoRepositoryEmMemoria;
import com.urnacidada.service.UrnaService;

import java.util.Scanner;

/**
 * Ponto de entrada. Por enquanto roda a urna no terminal, só para provar que
 * a lógica funciona. Quando a tela Swing estiver pronta, ela substitui este laço.
 */
public class Main {

    public static void main(String[] args) {
        var repositorio = new VotoRepositoryEmMemoria();
        var urna = new UrnaService(Eleicao.exemplo(), repositorio);
        var teclado = new Scanner(System.in);

        System.out.println("=== URNA CIDADÃ (simulador educativo, NÃO é urna oficial) ===");

        while (!urna.isFinalizada()) {
            var cargo = urna.getCargoAtual();
            System.out.printf("%nCargo: %s (%d dígitos)%n", cargo.nome(), cargo.digitos());
            System.out.println("Digite o número, ou: b = branco | c = corrige | ok = confirma");

            String entrada = teclado.nextLine().trim().toLowerCase();
            try {
                switch (entrada) {
                    case "b" -> urna.votarEmBranco();
                    case "c" -> urna.corrigir();
                    case "ok" -> urna.confirmar();
                    default -> entrada.chars().forEach(ch -> urna.digitar((char) ch));
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Atenção: " + e.getMessage());
            } catch (RepositorioException e) {
                System.out.println("Não foi possível salvar os votos: " + e.getMessage());
            }

            if (!urna.isFinalizada()) {
                System.out.println(urna.isBranco()
                        ? "-> BRANCO"
                        : "-> Digitado: " + urna.getDigitado()
                          + urna.getCandidatoAtual()
                                .map(c -> "  (" + c.nome() + " - " + c.partido() + ")")
                                .orElse(""));
            }
        }

        System.out.println("\nFIM. Votos registrados:");
        urna.getVotosRegistrados().forEach(System.out::println);
    }
}
