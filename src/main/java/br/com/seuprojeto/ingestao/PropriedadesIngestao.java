package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.CasaDeAposta;
import br.com.seuprojeto.dominio.Temporada;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ajustes da ingestao.
 *
 * <p>Os tetos sao baixos por padrao e {@code habilitada} vem falsa: estourar a
 * franquia de um plano gratuito e facil, e um job que dispara sozinho na primeira
 * subida da aplicacao seria a forma mais rapida de conseguir isso.
 */
@ConfigurationProperties(prefix = "apostas.ingestao")
public record PropriedadesIngestao(@DefaultValue("false") boolean habilitada,
                                   @DefaultValue(Temporada.BRASILEIRAO_SERIE_A) String competicao,
                                   @DefaultValue("4") int temporadas,
                                   @DefaultValue("100") int requisicoesPorDiaResultados,
                                   @DefaultValue("100") int requisicoesPorDiaOdds,
                                   @DefaultValue("72") int janelaDeOddsEmHoras,
                                   @DefaultValue("PT3H") Duration intervaloEntreColetasDeOdds,
                                   @DefaultValue({CasaDeAposta.BET365, CasaDeAposta.PINNACLE})
                                   List<String> casasDeInteresse) {

    public PropriedadesIngestao {
        if (temporadas < 1) {
            throw new IllegalArgumentException("temporadas deve ser ao menos 1, recebido: " + temporadas);
        }
        if (janelaDeOddsEmHoras < 1) {
            throw new IllegalArgumentException("janelaDeOddsEmHoras deve ser positiva");
        }
        casasDeInteresse = List.copyOf(casasDeInteresse);
    }

    /** Anos do historico: a temporada corrente e as anteriores, conforme configurado. */
    public List<Integer> anosDoHistorico(int anoCorrente) {
        return java.util.stream.IntStream
                .rangeClosed(anoCorrente - temporadas + 1, anoCorrente)
                .boxed()
                .toList();
    }
}
