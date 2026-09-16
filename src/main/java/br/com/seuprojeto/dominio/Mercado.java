package br.com.seuprojeto.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * Mercados de aposta suportados. Cada mercado conhece as selecoes que aceita,
 * o que impede combinacoes invalidas como OVER dentro de AMBAS_MARCAM.
 */
public enum Mercado {

    RESULTADO_1X2,
    OVER_UNDER_2_5,
    AMBAS_MARCAM;

    public boolean aceita(Selecao selecao) {
        return selecao != null && selecao.mercado() == this;
    }

    /** Selecoes possiveis deste mercado, na ordem declarada em {@link Selecao}. */
    public List<Selecao> selecoes() {
        return Arrays.stream(Selecao.values()).filter(this::aceita).toList();
    }
}
