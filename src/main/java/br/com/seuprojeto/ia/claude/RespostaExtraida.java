package br.com.seuprojeto.ia.claude;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Forma exata que o modelo deve devolver. O SDK gera o JSON Schema a partir desta
 * classe e a API fica obrigada a respeita-lo, entao nao ha parsing de texto livre.
 *
 * <p>Tudo e String: a normalizacao para {@code Mercado}, {@code Selecao} e
 * {@code BigDecimal} acontece em Java, onde da para validar e rejeitar.
 */
public class RespostaExtraida {

    @JsonPropertyDescription("Uma entrada por selecao/perna visivel no bilhete. Vazia se a imagem nao for um bilhete.")
    public List<SelecaoExtraida> selecoes;

    @JsonPropertyDescription("Nota curta quando a imagem nao e um bilhete de apostas ou esta ilegivel. Vazio caso contrario.")
    public String observacao;

    public static class SelecaoExtraida {

        @JsonPropertyDescription("A linha do bilhete transcrita como aparece na imagem.")
        public String textoOriginal;

        @JsonPropertyDescription("Time mandante (o da casa, a esquerda em 'A x B'). Vazio se nao der para ler.")
        public String mandante;

        @JsonPropertyDescription("Time visitante (a direita em 'A x B'). Vazio se nao der para ler.")
        public String visitante;

        @JsonPropertyDescription("Um de: RESULTADO_1X2, OVER_UNDER_2_5, AMBAS_MARCAM, OUTRO.")
        public String mercado;

        @JsonPropertyDescription("RESULTADO_1X2: CASA, EMPATE ou FORA. OVER_UNDER_2_5: OVER ou UNDER. "
                + "AMBAS_MARCAM: SIM ou NAO. OUTRO: vazio.")
        public String selecao;

        @JsonPropertyDescription("Odd decimal como aparece no bilhete, por exemplo 2.10 ou 2,10. Vazio se nao visivel.")
        public String odd;
    }
}
