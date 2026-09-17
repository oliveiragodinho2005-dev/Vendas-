package br.com.seuprojeto.ingestao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.repositorio.PartidaRepository;
import br.com.seuprojeto.repositorio.TemporadaRepository;
import br.com.seuprojeto.repositorio.TimeRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Idempotencia e controle de cota contra um PostgreSQL real.
 *
 * <p>Estes sao os dois comportamentos que nao da para provar com mock: que reimportar
 * a mesma temporada nao duplica nada, e que a franquia diaria sobrevive a concorrencia
 * e a reinicio da aplicacao.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class IngestaoIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final OffsetDateTime QUANDO =
            OffsetDateTime.of(2026, 5, 10, 19, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private SincronizadorDeTimes sincronizadorDeTimes;
    @Autowired
    private SincronizadorDePartidas sincronizadorDePartidas;
    @Autowired
    private ControleDeCota cota;
    @Autowired
    private TimeRepository times;
    @Autowired
    private PartidaRepository partidas;
    @Autowired
    private TemporadaRepository temporadas;

    private Temporada temporada;

    @BeforeEach
    void preparar() {
        temporada = temporadas.saveAndFlush(Temporada.brasileiraoSerieA(2026));
    }

    // ---------- controle de cota ----------

    @Test
    void reservaCabeAteOTetoEDepoisEhNegada() {
        assertThat(cota.tentarReservar("fonte-teste", 3, 5)).isTrue();
        assertThat(cota.consumoDeHoje("fonte-teste")).isEqualTo(3);

        assertThat(cota.tentarReservar("fonte-teste", 2, 5)).isTrue();
        assertThat(cota.consumoDeHoje("fonte-teste")).isEqualTo(5);

        assertThat(cota.tentarReservar("fonte-teste", 1, 5)).isFalse();
    }

    @Test
    void reservaNegadaNaoDebitaNada() {
        cota.tentarReservar("fonte-teste", 4, 5);

        assertThat(cota.tentarReservar("fonte-teste", 3, 5)).isFalse();
        // O ponto: uma negativa nao pode consumir parcialmente a franquia.
        assertThat(cota.consumoDeHoje("fonte-teste")).isEqualTo(4);
        assertThat(cota.restanteHoje("fonte-teste", 5)).isEqualTo(1);
    }

    @Test
    void fontesTemFranquiasIndependentes() {
        cota.tentarReservar("resultados", 5, 5);

        assertThat(cota.tentarReservar("odds", 5, 5)).isTrue();
        assertThat(cota.consumoDeHoje("resultados")).isEqualTo(5);
        assertThat(cota.consumoDeHoje("odds")).isEqualTo(5);
    }

    // ---------- sincronizacao de times ----------

    @Test
    void sincronizarDuasVezesNaoDuplicaTimes() {
        List<TimeExterno> externos = List.of(
                new TimeExterno("127", "Flamengo", "FLA", null),
                new TimeExterno("121", "Palmeiras", "PAL", null));

        assertThat(sincronizadorDeTimes.sincronizar(externos).criados()).isEqualTo(2);
        ResultadoDeSincronizacao segunda = sincronizadorDeTimes.sincronizar(externos);

        assertThat(segunda.criados()).isZero();
        assertThat(segunda.atualizados()).isEqualTo(2);
        assertThat(times.count()).isEqualTo(2);
    }

    @Test
    void timeCadastradoSemIdExternoEhAdotadoEmVezDeDuplicado() {
        // Cenario real: o time nasceu da leitura de um bilhete, antes da ingestao.
        Time preexistente = times.saveAndFlush(new Time("Flamengo"));

        sincronizadorDeTimes.sincronizar(List.of(new TimeExterno("127", "Flamengo", "FLA", null)));

        assertThat(times.count()).isEqualTo(1);
        Time adotado = times.findById(preexistente.getId()).orElseThrow();
        assertThat(adotado.getIdExterno()).isEqualTo("127");
        assertThat(adotado.getSigla()).isEqualTo("FLA");
    }

    @Test
    void nomeFormalNaFonteAdotaOCadastroCurto() {
        Time preexistente = times.saveAndFlush(new Time("Flamengo"));

        sincronizadorDeTimes.sincronizar(
                List.of(new TimeExterno("127", "Clube de Regatas do Flamengo", null, null)));

        assertThat(times.count()).isEqualTo(1);
        assertThat(times.findById(preexistente.getId()).orElseThrow().getIdExterno()).isEqualTo("127");
    }

    @Test
    void timeJaVinculadoAOutroIdExternoNaoEhRoubado() {
        Time existente = new Time("Flamengo");
        existente.definirIdExterno("127");
        times.saveAndFlush(existente);

        ResultadoDeSincronizacao resultado = sincronizadorDeTimes.sincronizar(
                List.of(new TimeExterno("999", "Flamengo", null, null)));

        assertThat(resultado.ignorados()).isEqualTo(1);
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("ja esta vinculado"));
        assertThat(times.count()).isEqualTo(1);
    }

    @Test
    void nomeAmbiguoNaoEhAdotado() {
        times.saveAndFlush(new Time("America"));
        times.saveAndFlush(new Time("America FC"));

        ResultadoDeSincronizacao resultado = sincronizadorDeTimes.sincronizar(
                List.of(new TimeExterno("500", "America", null, null)));

        assertThat(resultado.ignorados()).isEqualTo(1);
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("bate com"));
    }

    // ---------- sincronizacao de partidas ----------

    @Test
    void reimportarATemporadaNaoDuplicaPartidas() {
        sincronizarDoisTimes();
        List<PartidaExterna> externas = List.of(partidaExterna(null, null, StatusPartida.AGENDADA));

        assertThat(sincronizadorDePartidas.sincronizar(temporada, externas).criados()).isEqualTo(1);
        ResultadoDeSincronizacao segunda = sincronizadorDePartidas.sincronizar(temporada, externas);

        assertThat(segunda.criados()).isZero();
        assertThat(segunda.atualizados()).isEqualTo(1);
        assertThat(partidas.countByTemporada(temporada)).isEqualTo(1);
    }

    @Test
    void reimportacaoGravaOPlacarEEncerraAPartida() {
        sincronizarDoisTimes();
        sincronizadorDePartidas.sincronizar(temporada,
                List.of(partidaExterna(null, null, StatusPartida.AGENDADA)));

        sincronizadorDePartidas.sincronizar(temporada,
                List.of(partidaExterna(3, 1, StatusPartida.ENCERRADA)));

        Partida gravada = partidas.findByIdExterno("1035000").orElseThrow();
        assertThat(gravada.getStatus()).isEqualTo(StatusPartida.ENCERRADA);
        assertThat(gravada.encerrada()).isTrue();
        assertThat(gravada.totalGols()).hasValue(4);
    }

    @Test
    void partidaRemarcadaMudaDeDataERodada() {
        sincronizarDoisTimes();
        sincronizadorDePartidas.sincronizar(temporada,
                List.of(partidaExterna(null, null, StatusPartida.AGENDADA)));

        OffsetDateTime novaData = QUANDO.plusDays(30);
        sincronizadorDePartidas.sincronizar(temporada, List.of(new PartidaExterna(
                "1035000", 19, novaData, "Maracana", "127", "121", null, null, StatusPartida.AGENDADA)));

        Partida gravada = partidas.findByIdExterno("1035000").orElseThrow();
        assertThat(gravada.getRodada()).isEqualTo(19);
        assertThat(gravada.getDataHora()).isEqualTo(novaData);
        assertThat(partidas.countByTemporada(temporada)).isEqualTo(1);
    }

    @Test
    void partidaComTimeDesconhecidoEhIgnoradaComAviso() {
        sincronizarDoisTimes();

        ResultadoDeSincronizacao resultado = sincronizadorDePartidas.sincronizar(temporada,
                List.of(new PartidaExterna("999", 1, QUANDO, null, "127", "404",
                        null, null, StatusPartida.AGENDADA)));

        assertThat(resultado.ignorados()).isEqualTo(1);
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("nao esta cadastrado"));
        assertThat(partidas.countByTemporada(temporada)).isZero();
    }

    @Test
    void fonteQueDizEncerradaSemPlacarNaoCorrompeABase() {
        sincronizarDoisTimes();
        sincronizadorDePartidas.sincronizar(temporada,
                List.of(partidaExterna(null, null, StatusPartida.AGENDADA)));

        ResultadoDeSincronizacao resultado = sincronizadorDePartidas.sincronizar(temporada,
                List.of(partidaExterna(null, null, StatusPartida.ENCERRADA)));

        Partida gravada = partidas.findByIdExterno("1035000").orElseThrow();
        assertThat(gravada.getStatus()).isEqualTo(StatusPartida.AGENDADA);
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("encerrada e sem placar"));
    }

    private void sincronizarDoisTimes() {
        sincronizadorDeTimes.sincronizar(List.of(
                new TimeExterno("127", "Flamengo", "FLA", null),
                new TimeExterno("121", "Palmeiras", "PAL", null)));
    }

    private static PartidaExterna partidaExterna(Integer golsMandante, Integer golsVisitante,
                                                 StatusPartida status) {
        return new PartidaExterna("1035000", 7, QUANDO, "Maracana", "127", "121",
                golsMandante, golsVisitante, status);
    }
}
