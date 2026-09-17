package br.com.seuprojeto.servico;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import java.math.BigDecimal;

/**
 * Uma perna do bilhete depois da tentativa de vinculo com o banco.
 *
 * <p>Campos nulos sao informacao, nao falha: dizem exatamente onde a cadeia parou,
 * e {@link #status()} explica o motivo.
 */
public record SelecaoAnalisada(String textoOriginal,
                               String mandanteLido,
                               String visitanteLido,
                               Long mandanteId,
                               Long visitanteId,
                               Mercado mercado,
                               Selecao selecao,
                               BigDecimal odd,
                               Long partidaId,
                               Integer rodada,
                               StatusDeVinculo status) {
}
