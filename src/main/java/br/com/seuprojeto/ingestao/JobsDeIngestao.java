package br.com.seuprojeto.ingestao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparos automaticos da ingestao.
 *
 * <p>Desligados por padrao ({@code apostas.ingestao.habilitada=false}). Um job que
 * comeca a rodar sozinho na primeira subida da aplicacao e a forma mais rapida de
 * torrar a franquia diaria antes de o sistema estar configurado.
 *
 * <p>Os jobs nao decidem nada: perguntam ao {@link PlanejadorDeIngestao} e executam o
 * que ele apontou. Quando nao ha o que fazer, nenhuma requisicao externa sai.
 */
@Component
@ConditionalOnProperty(prefix = "apostas.ingestao", name = "habilitada", havingValue = "true")
public class JobsDeIngestao {

    private static final Logger log = LoggerFactory.getLogger(JobsDeIngestao.class);

    private final PlanejadorDeIngestao planejador;
    private final ImportadorDeTemporadas importador;
    private final ColetorDeOdds coletor;

    public JobsDeIngestao(PlanejadorDeIngestao planejador,
                          ImportadorDeTemporadas importador,
                          ColetorDeOdds coletor) {
        this.planejador = planejador;
        this.importador = importador;
        this.coletor = coletor;
    }

    @Scheduled(cron = "${apostas.ingestao.cron-resultados:0 20 */6 * * *}")
    public void sincronizarResultados() {
        PlanoDeIngestao plano = planejador.planejar();
        if (plano.temporadasParaImportar().isEmpty() && plano.temporadasParaAtualizar().isEmpty()) {
            log.debug("nada a sincronizar: {}", plano.motivos());
            return;
        }
        log.info("plano de resultados: importar {}, atualizar {}, ~{} requisicao(oes)",
                plano.temporadasParaImportar(), plano.temporadasParaAtualizar(), plano.requisicoesEstimadas());

        for (int ano : plano.temporadasParaImportar()) {
            if (interrompido(importador.importar(ano))) {
                return;
            }
        }
        for (int ano : plano.temporadasParaAtualizar()) {
            if (interrompido(importador.atualizarPartidas(ano))) {
                return;
            }
        }
    }

    @Scheduled(cron = "${apostas.ingestao.cron-odds:0 40 */3 * * *}")
    public void coletarOdds() {
        PlanoDeIngestao plano = planejador.planejar();
        if (!plano.coletarOdds()) {
            log.debug("coleta de odds dispensada: {}", plano.motivos());
            return;
        }
        ResultadoDeColeta resultado = coletor.coletar();
        if (!resultado.executada()) {
            log.info("coleta de odds nao executada: {}", resultado.motivo());
        }
    }

    /** Cota estourada interrompe o resto do plano: insistir so gera chamadas negadas. */
    private boolean interrompido(ResultadoDeImportacao resultado) {
        if (!resultado.executado()) {
            log.info("importacao da temporada {} nao executada: {}", resultado.ano(), resultado.motivo());
            return true;
        }
        return false;
    }
}
