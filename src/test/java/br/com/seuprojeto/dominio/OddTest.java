package br.com.seuprojeto.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OddTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2025, 5, 10, 12, 0, 0, 0, ZoneOffset.UTC);

    private final Partida partida = new Partida(
            Temporada.brasileiraoSerieA(2025), 7, AGORA.plusHours(7),
            new Time("Flamengo"), new Time("Palmeiras"));
    private final CasaDeAposta casa = new CasaDeAposta("Bet365", CasaDeAposta.BET365);

    private Odd odd(Mercado mercado, Selecao selecao, String valor) {
        return new Odd(partida, casa, mercado, selecao, new BigDecimal(valor), AGORA);
    }

    @ParameterizedTest
    @EnumSource(Mercado.class)
    void aceitaQualquerSelecaoDoProprioMercado(Mercado mercado) {
        for (Selecao selecao : mercado.selecoes()) {
            Odd odd = odd(mercado, selecao, "1.850");

            assertThat(odd.getMercado()).isEqualTo(mercado);
            assertThat(odd.getSelecao()).isEqualTo(selecao);
        }
    }

    @Test
    void rejeitaSelecaoDeOutroMercado() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> odd(Mercado.AMBAS_MARCAM, Selecao.OVER, "1.850"))
                .withMessageContaining("nao pertence ao mercado");
    }

    @Test
    void rejeitaOddSemRetorno() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> odd(Mercado.RESULTADO_1X2, Selecao.CASA, "1.000"))
                .withMessageContaining("maior que 1");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> odd(Mercado.RESULTADO_1X2, Selecao.CASA, "0.900"))
                .withMessageContaining("maior que 1");
    }

    @Test
    void guardaValorEInstanteDaColeta() {
        Odd odd = odd(Mercado.RESULTADO_1X2, Selecao.CASA, "2.375");

        assertThat(odd.getValor()).isEqualByComparingTo("2.375");
        assertThat(odd.getColetadaEm()).isEqualTo(AGORA);
    }
}
