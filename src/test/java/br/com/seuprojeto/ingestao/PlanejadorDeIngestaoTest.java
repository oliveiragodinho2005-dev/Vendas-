package br.com.seuprojeto.ingestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seuprojeto.dominio.Odd;
import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.repositorio.OddRepository;
import br.com.seuprojeto.repositorio.PartidaRepository;
import br.com.seuprojeto.repositorio.TemporadaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * O planejador e o que impede a ingestao de gastar requisicao a toa, entao o que se
 * testa aqui e principalmente o que ele decide NAO fazer.
 */
class PlanejadorDeIngestaoTest {

    private static final Instant AGORA = Instant.parse("2026-05-10T18:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

    private final TemporadaRepository temporadas = mock(TemporadaRepository.class);
    private final PartidaRepository partidas = mock(PartidaRepository.class);
    private final OddRepository odds = mock(OddRepository.class);

    private PropriedadesIngestao propriedades;
    private PlanejadorDeIngestao planejador;

    @BeforeEach
    void preparar() {
        propriedades = new PropriedadesIngestao(true, Temporada.BRASILEIRAO_SERIE_A, 4,
                100, 100, 72, Duration.ofHours(3), List.of("bet365", "pinnacle"));
        planejador = new PlanejadorDeIngestao(temporadas, partidas, odds, propriedades, RELOGIO);
        when(temporadas.findByCompeticaoAndAno(anyString(), anyInt())).thenReturn(Optional.empty());
        when(partidas.findByTemporadaAndStatusInAndDataHoraBeforeOrderByDataHoraAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(partidas.findByStatusAndDataHoraBetweenOrderByDataHoraAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(odds.findTopByOrderByColetadaEmDesc()).thenReturn(Optional.empty());
    }

    @Test
    void bancoVazioPedeAsQuatroTemporadasDoHistorico() {
        PlanoDeIngestao plano = planejador.planejar();

        assertThat(plano.temporadasParaImportar()).containsExactly(2023, 2024, 2025, 2026);
        assertThat(plano.temporadasParaAtualizar()).isEmpty();
        // Duas requisicoes por temporada: uma de times, uma de partidas.
        assertThat(plano.requisicoesEstimadas()).isEqualTo(8);
    }

    @Test
    void temporadaCompletaESemPendenciaNaoGeraRequisicao() {
        temporadaCadastrada(2026, 380);

        PlanoDeIngestao plano = planejador.planejar();

        assertThat(plano.temporadasParaAtualizar()).doesNotContain(2026);
        assertThat(plano.temporadasParaImportar()).doesNotContain(2026);
    }

    @Test
    void temporadaSemPartidasContaComoNaoImportada() {
        temporadaCadastrada(2026, 0);

        assertThat(planejador.planejar().temporadasParaImportar()).contains(2026);
    }

    @Test
    void jogoComHorarioVencidoESemResultadoPedeAtualizacao() {
        // Historico inteiro ja importado, para isolar o custo da atualizacao.
        temporadaCadastrada(2023, 380);
        temporadaCadastrada(2024, 380);
        temporadaCadastrada(2025, 380);
        Temporada temporada = temporadaCadastrada(2026, 380);
        when(partidas.findByTemporadaAndStatusInAndDataHoraBeforeOrderByDataHoraAsc(
                eq(temporada), any(), any())).thenReturn(List.of(mock(Partida.class)));

        PlanoDeIngestao plano = planejador.planejar();

        assertThat(plano.temporadasParaAtualizar()).containsExactly(2026);
        // Atualizacao pede so as partidas, nao os times: uma requisicao, nao duas.
        assertThat(plano.requisicoesEstimadas()).isEqualTo(1);
    }

    @Test
    void jogoAindaEmAndamentoNaoContaComoAtrasado() {
        temporadaCadastrada(2026, 380);

        planejador.planejar();

        // A busca so considera jogos iniciados ha mais de 3h; um que comecou agora
        // ainda esta rolando, e pedir o resultado dele seria requisicao jogada fora.
        ArgumentCaptor<OffsetDateTime> limite = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(partidas, org.mockito.Mockito.atLeastOnce())
                .findByTemporadaAndStatusInAndDataHoraBeforeOrderByDataHoraAsc(
                        any(), any(), limite.capture());
        assertThat(limite.getValue().toInstant()).isEqualTo(AGORA.minus(Duration.ofHours(3)));
    }

    @Test
    void semJogoNaJanelaNaoColetaOdds() {
        PlanoDeIngestao plano = planejador.planejar();

        assertThat(plano.coletarOdds()).isFalse();
        assertThat(plano.partidasNaJanelaDeOdds()).isZero();
    }

    @Test
    void jogoNaJanelaSemColetaAnteriorPedeColeta() {
        comJogosNaJanela(3);

        PlanoDeIngestao plano = planejador.planejar();

        assertThat(plano.coletarOdds()).isTrue();
        assertThat(plano.partidasNaJanelaDeOdds()).isEqualTo(3);
    }

    @Test
    void coletaRecenteBloqueiaNovaColeta() {
        comJogosNaJanela(3);
        comUltimaColetaEm(AGORA.minus(Duration.ofMinutes(30)));

        assertThat(planejador.planejar().coletarOdds()).isFalse();
    }

    @Test
    void coletaVelhaLiberaNovaColeta() {
        comJogosNaJanela(3);
        comUltimaColetaEm(AGORA.minus(Duration.ofHours(4)));

        assertThat(planejador.planejar().coletarOdds()).isTrue();
    }

    @Test
    void aJanelaDeOddsSegueAConfiguracao() {
        comJogosNaJanela(1);

        planejador.planejar();

        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(partidas).findByStatusAndDataHoraBetweenOrderByDataHoraAsc(
                eq(StatusPartida.AGENDADA), any(), fim.capture());
        assertThat(fim.getValue().toInstant()).isEqualTo(AGORA.plus(Duration.ofHours(72)));
    }

    private Temporada temporadaCadastrada(int ano, long quantidadeDePartidas) {
        Temporada temporada = Temporada.brasileiraoSerieA(ano);
        when(temporadas.findByCompeticaoAndAno(Temporada.BRASILEIRAO_SERIE_A, ano))
                .thenReturn(Optional.of(temporada));
        when(partidas.countByTemporada(temporada)).thenReturn(quantidadeDePartidas);
        return temporada;
    }

    private void comJogosNaJanela(int quantidade) {
        when(partidas.findByStatusAndDataHoraBetweenOrderByDataHoraAsc(any(), any(), any()))
                .thenReturn(java.util.Collections.nCopies(quantidade, mock(Partida.class)));
    }

    private void comUltimaColetaEm(Instant instante) {
        Odd ultima = mock(Odd.class);
        when(ultima.getColetadaEm()).thenReturn(instante.atOffset(ZoneOffset.UTC));
        when(odds.findTopByOrderByColetadaEmDesc()).thenReturn(Optional.of(ultima));
    }
}
