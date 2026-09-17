package br.com.seuprojeto.ingestao;

import java.util.List;

/**
 * Fica no lugar do adaptador real enquanto ele nao existe.
 *
 * <p>Declara-se indisponivel, entao os jobs pulam em silencio em vez de quebrar. Se
 * alguem chamar mesmo assim, a excecao diz exatamente o que falta - bem melhor do que
 * um {@code NullPointerException} vindo de uma dependencia opcional.
 */
public class FonteDeDadosAusente implements FonteDeDados {

    @Override
    public String nome() {
        return "nenhuma";
    }

    @Override
    public boolean disponivel() {
        return false;
    }

    @Override
    public List<TimeExterno> buscarTimes(int ano) {
        throw new IngestaoException("nenhuma FonteDeDados configurada");
    }

    @Override
    public List<PartidaExterna> buscarPartidas(int ano) {
        throw new IngestaoException("nenhuma FonteDeDados configurada");
    }
}
