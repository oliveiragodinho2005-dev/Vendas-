package br.com.seuprojeto.ia;

/** Falha ao ler o bilhete: erro da API, resposta recusada ou conteudo inesperado. */
public class LeituraDeBilheteException extends RuntimeException {

    public LeituraDeBilheteException(String mensagem) {
        super(mensagem);
    }

    public LeituraDeBilheteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
