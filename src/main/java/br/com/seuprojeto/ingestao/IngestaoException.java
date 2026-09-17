package br.com.seuprojeto.ingestao;

/** Falha ao falar com uma fonte externa ou ao interpretar o que ela devolveu. */
public class IngestaoException extends RuntimeException {

    public IngestaoException(String mensagem) {
        super(mensagem);
    }

    public IngestaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
