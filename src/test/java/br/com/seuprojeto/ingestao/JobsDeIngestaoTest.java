package br.com.seuprojeto.ingestao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobsDeIngestaoTest {

    private final PlanejadorDeIngestao planejador = mock(PlanejadorDeIngestao.class);
    private final ImportadorDeTemporadas importador = mock(ImportadorDeTemporadas.class);
    private final ColetorDeOdds coletor = mock(ColetorDeOdds.class);
    private final JobsDeIngestao jobs = new JobsDeIngestao(planejador, importador, coletor);

    @Test
    void planoVazioNaoDisparaNenhumaChamada() {
        when(planejador.planejar()).thenReturn(
                new PlanoDeIngestao(List.of(), List.of(), false, 0, 0, List.of()));

        jobs.sincronizarResultados();
        jobs.coletarOdds();

        verifyNoInteractions(importador, coletor);
    }

    @Test
    void cotaEsgotadaNaPrimeiraTemporadaAbortaOResto() {
        when(planejador.planejar()).thenReturn(
                new PlanoDeIngestao(List.of(2023, 2024, 2025), List.of(), false, 0, 6, List.of()));
        when(importador.importar(2023))
                .thenReturn(ResultadoDeImportacao.naoExecutado(2023, "cota diaria esgotada"));

        jobs.sincronizarResultados();

        verify(importador).importar(2023);
        // Insistir depois da cota estourada so geraria chamadas negadas em serie.
        verify(importador, never()).importar(2024);
        verify(importador, never()).importar(2025);
    }

    @Test
    void temporadasPendentesSaoImportadasEDepoisAtualizadas() {
        when(planejador.planejar()).thenReturn(
                new PlanoDeIngestao(List.of(2023), List.of(2026), false, 0, 3, List.of()));
        when(importador.importar(anyInt()))
                .thenReturn(ResultadoDeImportacao.executado(2023, null, null));
        when(importador.atualizarPartidas(anyInt()))
                .thenReturn(ResultadoDeImportacao.executado(2026, null, null));

        jobs.sincronizarResultados();

        verify(importador).importar(2023);
        verify(importador).atualizarPartidas(2026);
    }

    @Test
    void coletaDeOddsSoAconteceQuandoOPlanoPede() {
        when(planejador.planejar()).thenReturn(
                new PlanoDeIngestao(List.of(), List.of(), true, 5, 1, List.of()));
        when(coletor.coletar()).thenReturn(ResultadoDeColeta.executada(30, 0));

        jobs.coletarOdds();

        verify(coletor).coletar();
    }
}
