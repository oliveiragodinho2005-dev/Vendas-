package br.com.seuprojeto.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.seuprojeto.dominio.ApelidoDeTime;
import br.com.seuprojeto.dominio.CasaDeAposta;
import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Odd;
import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.Selecao;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.servico.ResolucaoDeTime;
import br.com.seuprojeto.servico.ResolvedorDeTimes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe um PostgreSQL real, aplica as migrations e confere que o mapeamento JPA
 * bate com o schema (ddl-auto=validate falha o contexto se divergir).
 *
 * <p>As regras de integridade sao exercitadas por SQL cru de proposito: pelo
 * construtor das entidades o Java barraria antes, e o que se quer provar aqui e
 * que o banco tambem barra.
 *
 * <p>Sem Docker disponivel a classe inteira e pulada, entao mvn verify continua
 * valido em maquina sem daemon.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2025, 5, 10, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private TemporadaRepository temporadas;
    @Autowired
    private TimeRepository times;
    @Autowired
    private PartidaRepository partidas;
    @Autowired
    private CasaDeApostaRepository casas;
    @Autowired
    private OddRepository odds;
    @Autowired
    private ApelidoDeTimeRepository apelidos;
    @Autowired
    private ResolvedorDeTimes resolvedor;
    @Autowired
    private JdbcTemplate jdbc;

    private Temporada temporada;
    private Time flamengo;
    private Time palmeiras;
    private Time gremio;
    private Partida partida;
    private CasaDeAposta bet365;

    @BeforeEach
    void preparar() {
        temporada = temporadas.saveAndFlush(Temporada.brasileiraoSerieA(2025));
        flamengo = times.saveAndFlush(new Time("Flamengo"));
        palmeiras = times.saveAndFlush(new Time("Palmeiras"));
        gremio = times.saveAndFlush(new Time("Gremio"));
        partida = partidas.saveAndFlush(new Partida(temporada, 7, AGORA.plusHours(7), flamengo, palmeiras));
        bet365 = casas.findByChave(CasaDeAposta.BET365).orElseThrow();
    }

    @Test
    void migrationsForamAplicadas() {
        List<String> versoes = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versoes).contains("1", "2", "3");
    }

    @Test
    void seedCadastraBet365ComoAlvoEPinnacleComoReferencia() {
        assertThat(bet365.isAtiva()).isTrue();
        assertThat(bet365.isReferenciaDeMercado()).isFalse();

        CasaDeAposta pinnacle = casas.findByChave(CasaDeAposta.PINNACLE).orElseThrow();
        assertThat(pinnacle.isReferenciaDeMercado()).isTrue();
    }

    @Test
    void partidaPersistidaPreservaPlacarEStatus() {
        partida.registrarPlacar(3, 1);
        partidas.saveAndFlush(partida);

        Partida recuperada = partidas.findById(partida.getId()).orElseThrow();

        assertThat(recuperada.getStatus()).isEqualTo(StatusPartida.ENCERRADA);
        assertThat(recuperada.resultadoDe(Mercado.RESULTADO_1X2)).hasValue(Selecao.CASA);
        assertThat(recuperada.resultadoDe(Mercado.OVER_UNDER_2_5)).hasValue(Selecao.OVER);
        assertThat(recuperada.resultadoDe(Mercado.AMBAS_MARCAM)).hasValue(Selecao.SIM);
        assertThat(recuperada.getCriadoEm()).isNotNull();
    }

    @Test
    void coletasSucessivasDaMesmaSelecaoConvivemSemSobrescrever() {
        odds.saveAndFlush(new Odd(partida, bet365, Mercado.RESULTADO_1X2, Selecao.CASA,
                new BigDecimal("2.100"), AGORA));
        odds.saveAndFlush(new Odd(partida, bet365, Mercado.RESULTADO_1X2, Selecao.CASA,
                new BigDecimal("1.950"), AGORA.plusHours(3)));

        List<Odd> historico = odds.findByPartidaAndCasaAndMercadoAndSelecaoOrderByColetadaEmAsc(
                partida, bet365, Mercado.RESULTADO_1X2, Selecao.CASA);

        assertThat(historico).hasSize(2);
        assertThat(historico).extracting(Odd::getValor)
                .containsExactly(new BigDecimal("2.100"), new BigDecimal("1.950"));
        assertThat(odds.findTopByPartidaAndCasaAndMercadoAndSelecaoOrderByColetadaEmDesc(
                partida, bet365, Mercado.RESULTADO_1X2, Selecao.CASA))
                .get()
                .extracting(Odd::getValor)
                .isEqualTo(new BigDecimal("1.950"));
    }

    @Test
    void bancoAceitaTodosOsParesDeclaradosNoJava() {
        int gravadas = 0;
        for (Mercado mercado : Mercado.values()) {
            for (Selecao selecao : mercado.selecoes()) {
                Odd odd = new Odd(partida, bet365, mercado, selecao, new BigDecimal("1.800"), AGORA);
                if (mercado == Mercado.OVER_UNDER_2_5) {
                    odd.definirLinha(new BigDecimal("2.50"));
                }
                odds.saveAndFlush(odd);
                gravadas++;
            }
        }

        assertThat(gravadas).isEqualTo(Selecao.values().length);
        assertThat(odds.findByPartidaOrderByColetadaEmDesc(partida)).hasSize(gravadas);
    }

    @Test
    void resolvedorCasaNomeFormalComCadastroCurto() {
        Map<String, ResolucaoDeTime> resolvido =
                resolvedor.resolver(List.of("Clube de Regatas do Flamengo", "PALMEIRAS"));

        assertThat(resolvido.get("Clube de Regatas do Flamengo").time()).isEqualTo(flamengo);
        assertThat(resolvido.get("PALMEIRAS").time()).isEqualTo(palmeiras);
    }

    @Test
    void resolvedorRecusaNomeQueBateComDoisTimes() {
        // Dois clubes cujo nome util colide: vincular qualquer um seria chute.
        Time americaMineiro = times.saveAndFlush(new Time("America"));
        times.saveAndFlush(new Time("America FC"));

        Map<String, ResolucaoDeTime> resolvido = resolvedor.resolver(List.of("America"));

        assertThat(resolvido.get("America").situacao())
                .isEqualTo(ResolucaoDeTime.Situacao.AMBIGUO);
        assertThat(resolvido.get("America").time()).isNull();
        assertThat(americaMineiro.getId()).isNotNull();
    }

    @Test
    void apelidoCuradoResolveOQueANormalizacaoNaoAlcanca() {
        apelidos.saveAndFlush(new ApelidoDeTime(flamengo, "Mengao"));

        Map<String, ResolucaoDeTime> resolvido = resolvedor.resolver(List.of("Mengao"));

        assertThat(resolvido.get("Mengao").resolvido()).isTrue();
        assertThat(resolvido.get("Mengao").time()).isEqualTo(flamengo);
    }

    @Test
    void bancoRejeitaApelidoApontandoParaDoisTimes() {
        apelidos.saveAndFlush(new ApelidoDeTime(flamengo, "Tricolor"));

        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                apelidos.saveAndFlush(new ApelidoDeTime(palmeiras, "Tricolor")));
    }

    @Test
    void bancoRejeitaSelecaoForaDoMercado() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                jdbc.update("""
                        INSERT INTO odds (partida_id, casa_de_aposta_id, mercado, selecao, valor, coletada_em)
                        VALUES (?, ?, 'AMBAS_MARCAM', 'OVER', 1.900, ?)
                        """, partida.getId(), bet365.getId(), AGORA));
    }

    @Test
    void bancoRejeitaOddSemRetorno() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                jdbc.update("""
                        INSERT INTO odds (partida_id, casa_de_aposta_id, mercado, selecao, valor, coletada_em)
                        VALUES (?, ?, 'RESULTADO_1X2', 'CASA', 1.000, ?)
                        """, partida.getId(), bet365.getId(), AGORA));
    }

    @Test
    void bancoRejeitaTimeEnfrentandoASiMesmo() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                jdbc.update("""
                        INSERT INTO partidas (temporada_id, rodada, data_hora, mandante_id, visitante_id,
                                              status, criado_em, atualizado_em)
                        VALUES (?, 8, ?, ?, ?, 'AGENDADA', ?, ?)
                        """, temporada.getId(), AGORA, gremio.getId(), gremio.getId(), AGORA, AGORA));
    }

    @Test
    void bancoRejeitaPartidaEncerradaSemPlacar() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                jdbc.update("""
                        INSERT INTO partidas (temporada_id, rodada, data_hora, mandante_id, visitante_id,
                                              status, criado_em, atualizado_em)
                        VALUES (?, 9, ?, ?, ?, 'ENCERRADA', ?, ?)
                        """, temporada.getId(), AGORA, palmeiras.getId(), gremio.getId(), AGORA, AGORA));
    }
}
