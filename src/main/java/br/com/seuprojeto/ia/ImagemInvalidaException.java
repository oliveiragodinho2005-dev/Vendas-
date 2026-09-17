package br.com.seuprojeto.ia;

/** Imagem rejeitada antes de qualquer chamada ao modelo. Vira 400 na borda HTTP. */
public class ImagemInvalidaException extends RuntimeException {

    public ImagemInvalidaException(String mensagem) {
        super(mensagem);
    }
}
