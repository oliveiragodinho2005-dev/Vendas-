package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Cotacao como a fonte agregadora a entrega.
 *
 * @param chaveCasa chave da casa de apostas, igual a {@code casas_de_aposta.chave}
 */
public record OddExterna(String idExternoPartida,
                         String chaveCasa,
                         Mercado mercado,
                         Selecao selecao,
                         BigDecimal linha,
                         BigDecimal valor,
                         OffsetDateTime coletadaEm) {

    public OddExterna {
        Objects.requireNonNull(idExternoPartida, "idExternoPartida");
        Objects.requireNonNull(chaveCasa, "chaveCasa");
        Objects.requireNonNull(mercado, "mercado");
        Objects.requireNonNull(selecao, "selecao");
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(coletadaEm, "coletadaEm");
        if (!mercado.aceita(selecao)) {
            throw new IllegalArgumentException("selecao " + selecao + " nao pertence ao mercado " + mercado);
        }
    }
}
