package br.com.seuprojeto.ingestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.repositorio.TemporadaRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportadorDeTemporadasTest {

    private final TemporadaRepository temporadas = mock(TemporadaRepository.class);
    private final SincronizadorDeTimes sincronizadorDeTimes = mock(SincronizadorDeTimes.class);
    private final SincronizadorDePartidas sincronizadorDePartidas = mock(SincronizadorDePartidas.class);
    private final ControleDeCota cota = mock(ControleDeCota.class);

    private FonteEspia fonte;
    private ImportadorDeTemporadas importador;

    @BeforeEach
    void preparar() {
        fonte = new FonteEspia(true);
        PropriedadesIngestao propriedades = new PropriedadesIngestao(true,
                Temporada.BRASILEIRAO_SERIE_A, 4, 100, 100, 72, Duration.ofHours(3), List.of("bet365"));
        importador = new ImportadorDeTemporadas(fonte, temporadas, sincronizadorDeTimes,
                sincronizadorDePartidas, cota, propriedades);

        when(temporadas.findByCompeticaoAndAno(anyString(), anyInt()))
                .thenReturn(Optional.of(Temporada.brasileiraoSerieA(2026)));
        when(cota.tentarReservar(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(sincronizadorDeTimes.sincronizar(any()))
                .thenReturn(new ResultadoDeSincronizacao(20, 0, 0, List.of()));
        when(sincronizadorDePartidas.sincronizar(any(), any()))
                .thenReturn(new ResultadoDeSincronizacao(380, 0, 0, List.of()));
    }

    @Test
    void importacaoCompletaGravaTimesEPartidas() {
        ResultadoDeImportacao resultado = importador.importar(2026);

        assertThat(resultado.executado()).isTrue();
        assertThat(resultado.times().criados()).isEqualTo(20);
        assertThat(resultado.partidas().criados()).isEqualTo(380);
        assertThat(fonte.anosDeTimes).containsExactly(2026);
        assertThat(fonte.anosDePartidas).containsExactly(2026);
    }

    @Test
    void importacaoCompletaReservaDuasRequisicoes() {
        importador.importar(2026);

        verify(cota).tentarReservar("espia", 2, 100);
    }

    @Test
    void atualizacaoPoupaAChamadaDeTimes() {
        ResultadoDeImportacao resultado = importador.atualizarPartidas(2026);

        assertThat(resultado.executado()).isTrue();
        assertThat(resultado.times()).isNull();
        assertThat(fonte.anosDeTimes).isEmpty();
        assertThat(fonte.anosDePartidas).containsExactly(2026);
        verify(cota).tentarReservar("espia", 1, 100);
    }

    @Test
    void fonteSemCredencialNaoGastaCota() {
        importador = comFonte(new FonteEspia(false));

        ResultadoDeImportacao resultado = importador.importar(2026);

        assertThat(resultado.executado()).isFalse();
        assertThat(resultado.motivo()).contains("sem credencial");
        verify(cota, never()).tentarReservar(anyString(), anyInt(), anyInt());
    }

    @Test
    void cotaNegadaImpedeAChamadaExterna() {
        when(cota.tentarReservar(anyString(), anyInt(), anyInt())).thenReturn(false);

        ResultadoDeImportacao resultado = importador.importar(2026);

        assertThat(resultado.executado()).isFalse();
        assertThat(resultado.motivo()).contains("cota diaria");
        // O ponto do teste: a reserva negada barra a chamada antes de ela sair.
        assertThat(fonte.anosDeTimes).isEmpty();
        assertThat(fonte.anosDePartidas).isEmpty();
        verify(sincronizadorDePartidas, never()).sincronizar(any(), any());
    }

    @Test
    void temporadaInexistenteECriadaNaPrimeiraImportacao() {
        when(temporadas.findByCompeticaoAndAno(anyString(), anyInt())).thenReturn(Optional.empty());
        when(temporadas.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        importador.importar(2023);

        verify(temporadas).save(any(Temporada.class));
    }

    private ImportadorDeTemporadas comFonte(FonteEspia outra) {
        fonte = outra;
        return new ImportadorDeTemporadas(outra, temporadas, sincronizadorDeTimes, sincronizadorDePartidas,
                cota, new PropriedadesIngestao(true, Temporada.BRASILEIRAO_SERIE_A, 4, 100, 100, 72,
                Duration.ofHours(3), List.of("bet365")));
    }

    /** Fonte de mentira que registra o que foi pedido, sem rede nenhuma. */
    private static final class FonteEspia implements FonteDeDados {

        private final boolean disponivel;
        private final List<Integer> anosDeTimes = new ArrayList<>();
        private final List<Integer> anosDePartidas = new ArrayList<>();

        private FonteEspia(boolean disponivel) {
            this.disponivel = disponivel;
        }

        @Override
        public String nome() {
            return "espia";
        }

        @Override
        public boolean disponivel() {
            return disponivel;
        }

        @Override
        public List<TimeExterno> buscarTimes(int ano) {
            anosDeTimes.add(ano);
            return List.of(new TimeExterno("t1", "Flamengo", "FLA", null));
        }

        @Override
        public List<PartidaExterna> buscarPartidas(int ano) {
            anosDePartidas.add(ano);
            return List.of(new PartidaExterna("p1", 1,
                    OffsetDateTime.of(2026, 4, 12, 19, 0, 0, 0, ZoneOffset.UTC),
                    "Maracana", "t1", "t2", null, null, StatusPartida.AGENDADA));
        }
    }
}
