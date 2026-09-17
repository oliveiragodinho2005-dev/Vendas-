package br.com.seuprojeto.ia;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma perna do bilhete, como o modelo leu da imagem. Ainda nao vinculada ao banco.
 *
 * <p>Campos nulos sao normais e significam "nao deu para ler" ou "mercado fora dos
 * tres suportados". Preferimos devolver o buraco a inventar dado, porque um time
 * chutado vira EV errado la na frente.
 */
public record SelecaoLida(String textoOriginal,
                          String nomeMandante,
                          String nomeVisitante,
                          Mercado mercado,
                          Selecao selecao,
                          BigDecimal odd) {

    public SelecaoLida {
        Objects.requireNonNull(textoOriginal, "textoOriginal");
        // Reaproveita a regra do dominio: um par mercado/selecao incoerente nunca entra.
        if (mercado != null && selecao != null && !mercado.aceita(selecao)) {
            throw new LeituraDeBilheteException(
                    "modelo devolveu selecao " + selecao + " para o mercado " + mercado);
        }
    }

    /** Verdadeiro quando o mercado lido e um dos tres que o sistema precifica. */
    public boolean mercadoSuportado() {
        return mercado != null && selecao != null;
    }

    public boolean temConfronto() {
        return nomeMandante != null && !nomeMandante.isBlank()
                && nomeVisitante != null && !nomeVisitante.isBlank();
    }
}
