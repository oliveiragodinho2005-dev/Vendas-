package br.com.seuprojeto.ia;

/** Nenhuma ANTHROPIC_API_KEY no ambiente. Vira 503 na borda HTTP. */
public class IaNaoConfiguradaException extends RuntimeException {

    public IaNaoConfiguradaException(String mensagem) {
        super(mensagem);
    }
}
