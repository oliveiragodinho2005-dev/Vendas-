package br.com.seuprojeto.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MercadoTest {

    @Test
    void mercadosIniciaisSaoOsTresDaFase1() {
        assertThat(Mercado.values())
                .containsExactly(Mercado.RESULTADO_1X2, Mercado.OVER_UNDER_2_5, Mercado.AMBAS_MARCAM);
    }

    @Test
    void cadaMercadoConheceSuasSelecoes() {
        assertThat(Mercado.RESULTADO_1X2.selecoes())
                .containsExactly(Selecao.CASA, Selecao.EMPATE, Selecao.FORA);
        assertThat(Mercado.OVER_UNDER_2_5.selecoes())
                .containsExactly(Selecao.OVER, Selecao.UNDER);
        assertThat(Mercado.AMBAS_MARCAM.selecoes())
                .containsExactly(Selecao.SIM, Selecao.NAO);
    }

    @ParameterizedTest
    @EnumSource(Selecao.class)
    void selecaoPertenceApenasAoProprioMercado(Selecao selecao) {
        List<Mercado> mercadosQueAceitam = Arrays.stream(Mercado.values())
                .filter(mercado -> mercado.aceita(selecao))
                .toList();

        assertThat(mercadosQueAceitam).containsExactly(selecao.mercado());
    }

    @Test
    void toda1SelecaoEstaCobertaPorAlgumMercado() {
        List<Selecao> cobertas = Arrays.stream(Mercado.values())
                .flatMap(mercado -> mercado.selecoes().stream())
                .toList();

        assertThat(cobertas).containsExactlyInAnyOrder(Selecao.values());
    }

    @Test
    void mercadoNaoAceitaSelecaoNula() {
        assertThat(Mercado.RESULTADO_1X2.aceita(null)).isFalse();
    }
}
