package br.com.seuprojeto.ingestao;

/**
 * Desfecho da importacao de uma temporada.
 *
 * @param executado falso quando nada foi buscado - fonte indisponivel ou cota esgotada;
 *                  nesses casos {@code motivo} explica, e nenhum dos dois e erro
 */
public record ResultadoDeImportacao(int ano,
                                    boolean executado,
                                    String motivo,
                                    ResultadoDeSincronizacao times,
                                    ResultadoDeSincronizacao partidas) {

    public static ResultadoDeImportacao naoExecutado(int ano, String motivo) {
        return new ResultadoDeImportacao(ano, false, motivo, null, null);
    }

    public static ResultadoDeImportacao executado(int ano,
                                                  ResultadoDeSincronizacao times,
                                                  ResultadoDeSincronizacao partidas) {
        return new ResultadoDeImportacao(ano, true, null, times, partidas);
    }
}
