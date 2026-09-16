package br.com.seuprojeto.dominio;

/** Resultado possivel dentro de um mercado. Cada selecao pertence a exatamente um mercado. */
public enum Selecao {

    CASA(Mercado.RESULTADO_1X2),
    EMPATE(Mercado.RESULTADO_1X2),
    FORA(Mercado.RESULTADO_1X2),

    OVER(Mercado.OVER_UNDER_2_5),
    UNDER(Mercado.OVER_UNDER_2_5),

    SIM(Mercado.AMBAS_MARCAM),
    NAO(Mercado.AMBAS_MARCAM);

    private final Mercado mercado;

    Selecao(Mercado mercado) {
        this.mercado = mercado;
    }

    public Mercado mercado() {
        return mercado;
    }
}
