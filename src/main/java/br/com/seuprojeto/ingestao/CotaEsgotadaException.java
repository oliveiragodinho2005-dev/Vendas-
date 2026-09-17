package br.com.seuprojeto.ingestao;

/** A franquia diaria da fonte acabou. Nao e erro: e o limite funcionando. */
public class CotaEsgotadaException extends RuntimeException {

    public CotaEsgotadaException(String mensagem) {
        super(mensagem);
    }
}
