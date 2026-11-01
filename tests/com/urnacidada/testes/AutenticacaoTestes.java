package com.urnacidada.testes;

import com.sun.net.httpserver.HttpServer;
import com.urnacidada.repository.AutenticacaoFirebase;
import com.urnacidada.repository.CpfIdentificador;
import com.urnacidada.repository.RepositorioException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static com.urnacidada.testes.MiniTeste.*;

final class AutenticacaoTestes {
    private static final String CPF = "529.982.247-25";
    private AutenticacaoTestes() { }

    private static HttpServer servidor(String falha, AtomicInteger chamadas, StringBuilder corpos) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            chamadas.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (corpos != null) corpos.append(path).append(body);
            int status = falha == null ? 200 : 400;
            String response = falha == null
                    ? "{\"localId\":\"uid-abc\",\"idToken\":\"id-token\",\"refreshToken\":\"refresh-token\",\"expiresIn\":\"3600\"}"
                    : "{\"error\":{\"message\":\"" + falha + "\"}}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static AutenticacaoFirebase cliente(HttpServer server) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        return new AutenticacaoFirebase("chave-de-teste", base, base);
    }

    static void executar() {
        grupo("Firebase Authentication: CPF e senha");

        teste("cadastro abre a conta e prepara a sessão da votação", () -> {
            AtomicInteger chamadas = new AtomicInteger();
            StringBuilder corpos = new StringBuilder();
            HttpServer server = servidor(null, chamadas, corpos);
            char[] senha = "senha-teste-123".toCharArray();
            try {
                AutenticacaoFirebase.Sessao sessao = cliente(server).cadastrar(CPF, senha);
                igual("uid-abc", sessao.uid());
                igual("id-token", sessao.token());
                igual(1, chamadas.get());
                verdade(corpos.toString().contains("accounts:signUp"), "cadastro solicitado");
                verdade(corpos.toString().contains(CpfIdentificador.emailFirebase(CPF)), "identificador técnico derivado enviado");
                verdade(!corpos.toString().contains("52998224725"), "CPF não enviado ao Firebase Auth");
                verdade(corpos.toString().contains("senha-teste-123"), "senha enviada ao endpoint HTTPS de Auth");
            } finally { java.util.Arrays.fill(senha, '\0'); server.stop(0); }
        });

        teste("login com o mesmo CPF prepara uma sessão autenticada", () -> {
            AtomicInteger chamadas = new AtomicInteger();
            HttpServer server = servidor(null, chamadas, null);
            try {
                AutenticacaoFirebase.Sessao sessao = cliente(server).entrar(CPF, "senha-teste-123".toCharArray());
                igual("uid-abc", sessao.uid());
                igual("id-token", sessao.token());
                igual(1, chamadas.get());
            } finally { server.stop(0); }
        });

        teste("CPF que já tem conta orienta o participante a entrar", () -> {
            HttpServer server = servidor("EMAIL_EXISTS", new AtomicInteger(), null);
            try {
                RepositorioException falha = lanca(RepositorioException.class,
                        () -> cliente(server).cadastrar(CPF, "senha-teste-123".toCharArray()));
                verdade(falha.getMessage().contains("Este CPF já tem cadastro"), "erro traduzido para a tela");
            } finally { server.stop(0); }
        });
    }
}
