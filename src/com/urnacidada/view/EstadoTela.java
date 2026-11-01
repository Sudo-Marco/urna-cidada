package com.urnacidada.view;

/**
 * Tudo o que a tela precisa para se desenhar, já calculado. A tela não toma decisões:
 * só mostra este estado.
 *
 * @param modo                situação da votação
 * @param cargo               título do cargo (ou "VOTAÇÃO CONCLUÍDA")
 * @param progresso           texto como "CARGO 1 DE 2" (vazio quando não se aplica)
 * @param posicoes            quantidade de dígitos do cargo atual (0 quando não se aplica)
 * @param digitado            dígitos já digitados
 * @param titulo              texto principal da área central (nome do candidato, aviso, "FIM"...)
 * @param detalhe             texto secundário (partido)
 * @param explicacao          explicação do tipo de cargo
 * @param mensagem            orientação ou aviso ao participante
 * @param tipoMensagem        define a cor da mensagem
 * @param controlesHabilitados se as teclas respondem a cliques
 */
public record EstadoTela(
        Modo modo,
        String cargo,
        String progresso,
        int posicoes,
        String digitado,
        String titulo,
        String detalhe,
        String explicacao,
        String mensagem,
        TipoMensagem tipoMensagem,
        boolean controlesHabilitados) {

    /** Situações possíveis da votação. */
    public enum Modo { AGUARDANDO_INICIO, DIGITANDO, CANDIDATO, NUMERO_ERRADO, BRANCO, FIM }

    /** Natureza da mensagem, usada para escolher a cor. */
    public enum TipoMensagem { NENHUMA, INFO, SUCESSO, AVISO, ERRO }

    public boolean finalizada() {
        return modo == Modo.FIM;
    }
}
