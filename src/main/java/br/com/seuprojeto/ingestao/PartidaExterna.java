package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.StatusPartida;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Partida como a fonte externa a descreve.
 *
 * <p>Os times vem por {@code idExterno}, nao por nome: casar por nome entre chamadas
 * da mesma fonte seria fragil sem necessidade, ja que ela propria da um id estavel.
 *
 * <p>O adaptador e quem traduz o status da fonte para {@link StatusPartida} - cada API
 * tem o seu proprio vocabulario de estados.
 */
public record PartidaExterna(String idExterno,
                             int rodada,
                             OffsetDateTime dataHora,
                             String estadio,
                             String idExternoMandante,
                             String idExternoVisitante,
                             Integer golsMandante,
                             Integer golsVisitante,
                             StatusPartida status) {

    public PartidaExterna {
        Objects.requireNonNull(idExterno, "idExterno");
        Objects.requireNonNull(dataHora, "dataHora");
        Objects.requireNonNull(idExternoMandante, "idExternoMandante");
        Objects.requireNonNull(idExternoVisitante, "idExternoVisitante");
        Objects.requireNonNull(status, "status");
        if (idExternoMandante.equals(idExternoVisitante)) {
            throw new IllegalArgumentException("partida " + idExterno + " com o mesmo time dos dois lados");
        }
        if (rodada < 1) {
            throw new IllegalArgumentException("partida " + idExterno + " com rodada invalida: " + rodada);
        }
    }

    /** Verdadeiro quando a fonte ja entregou um placar completo. */
    public boolean temPlacar() {
        return golsMandante != null && golsVisitante != null;
    }
}
