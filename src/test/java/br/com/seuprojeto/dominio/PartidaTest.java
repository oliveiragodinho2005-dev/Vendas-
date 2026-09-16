package br.com.seuprojeto.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartidaTest {

    private static final OffsetDateTime QUANDO = OffsetDateTime.of(2025, 5, 10, 19, 0, 0, 0, ZoneOffset.UTC);

    private final Temporada temporada = Temporada.brasileiraoSerieA(2025);
    private final Time mandante = new Time("Flamengo");
    private final Time visitante = new Time("Palmeiras");

    private Partida partida() {
        return new Partida(temporada, 7, QUANDO, mandante, visitante);
    }

    @Test
    void partidaNasceAgendadaESemPlacar() {
        Partida partida = partida();

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.AGENDADA);
        assertThat(partida.encerrada()).isFalse();
        assertThat(partida.getGolsMandante()).isNull();
        assertThat(partida.getGolsVisitante()).isNull();
        assertThat(partida.totalGols()).isEmpty();
    }

    @Test
    void semPlacarNenhumMercadoTemResultado() {
        Partida partida = partida();

        for (Mercado mercado : Mercado.values()) {
            assertThat(partida.resultadoDe(mercado))
                    .as("mercado %s", mercado)
                    .isEmpty();
        }
    }

    @Test
    void registrarPlacarEncerraAPartida() {
        Partida partida = partida();

        partida.registrarPlacar(2, 1);

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.ENCERRADA);
        assertThat(partida.encerrada()).isTrue();
        assertThat(partida.totalGols()).hasValue(3);
    }

    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "2, 1, CASA",
            "1, 2, FORA",
            "0, 0, EMPATE",
            "3, 3, EMPATE",
    })
    void resultado1x2SegueADiferencaDeGols(int golsMandante, int golsVisitante, Selecao esperado) {
        Partida partida = partida();
        partida.registrarPlacar(golsMandante, golsVisitante);

        assertThat(partida.resultadoDe(Mercado.RESULTADO_1X2)).hasValue(esperado);
    }

    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "0, 0, UNDER",
            "1, 1, UNDER",
            "2, 0, UNDER",
            "2, 1, OVER",
            "0, 3, OVER",
    })
    void overUnderUsaALinhaDeDoisEMeio(int golsMandante, int golsVisitante, Selecao esperado) {
        Partida partida = partida();
        partida.registrarPlacar(golsMandante, golsVisitante);

        assertThat(partida.resultadoDe(Mercado.OVER_UNDER_2_5)).hasValue(esperado);
    }

    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "1, 1, SIM",
            "3, 2, SIM",
            "2, 0, NAO",
            "0, 0, NAO",
    })
    void ambasMarcamExigeGolDosDoisLados(int golsMandante, int golsVisitante, Selecao esperado) {
        Partida partida = partida();
        partida.registrarPlacar(golsMandante, golsVisitante);

        assertThat(partida.resultadoDe(Mercado.AMBAS_MARCAM)).hasValue(esperado);
    }

    @Test
    void resultadoSempreCaiEmUmaSelecaoDoProprioMercado() {
        Partida partida = partida();
        partida.registrarPlacar(2, 1);

        for (Mercado mercado : Mercado.values()) {
            assertThat(partida.resultadoDe(mercado))
                    .get()
                    .matches(mercado::aceita, "pertence ao mercado " + mercado);
        }
    }

    @Test
    void statusEncerradoSemPlacarNaoContaComoEncerrada() {
        Partida partida = partida();

        partida.alterarStatus(StatusPartida.ENCERRADA);

        assertThat(partida.encerrada()).isFalse();
        assertThat(partida.resultadoDe(Mercado.RESULTADO_1X2)).isEmpty();
    }

    @Test
    void timeNaoPodeEnfrentarASiMesmo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Partida(temporada, 1, QUANDO, mandante, mandante))
                .withMessageContaining("mesmo time");
    }

    @Test
    void rodadaPrecisaSerPositiva() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Partida(temporada, 0, QUANDO, mandante, visitante))
                .withMessageContaining("rodada");
    }

    @Test
    void placarNaoAceitaGolsNegativos() {
        Partida partida = partida();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> partida.registrarPlacar(-1, 0))
                .withMessageContaining("negativos");
    }
}
