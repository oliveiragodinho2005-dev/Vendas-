package br.com.seuprojeto.ingestao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** Contraparte de {@link FonteDeDadosAusente} para odds. */
public class FonteDeOddsAusente implements FonteDeOdds {

    @Override
    public String nome() {
        return "nenhuma";
    }

    @Override
    public boolean disponivel() {
        return false;
    }

    @Override
    public List<OddExterna> buscarOdds(OffsetDateTime inicio, OffsetDateTime fim, Set<String> chavesDeCasas) {
        throw new IngestaoException("nenhuma FonteDeOdds configurada");
    }
}
