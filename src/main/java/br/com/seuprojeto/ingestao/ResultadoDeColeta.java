package br.com.seuprojeto.ingestao;

/**
 * Desfecho de uma coleta de odds.
 *
 * @param ignoradas cotacoes descartadas por nao haver partida ou casa correspondente
 *                  no banco; numero alto aqui aponta ingestao de calendario atrasada
 */
public record ResultadoDeColeta(boolean executada, String motivo, int gravadas, int ignoradas) {

    public static ResultadoDeColeta naoExecutada(String motivo) {
        return new ResultadoDeColeta(false, motivo, 0, 0);
    }

    public static ResultadoDeColeta executada(int gravadas, int ignoradas) {
        return new ResultadoDeColeta(true, null, gravadas, ignoradas);
    }
}
