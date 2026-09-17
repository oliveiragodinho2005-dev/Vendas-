package br.com.seuprojeto.ingestao;

import java.util.Objects;

/**
 * Time como a fonte externa o descreve, antes de virar {@code Time} no banco.
 *
 * @param idExterno identificador na fonte; e o que torna a importacao idempotente
 */
public record TimeExterno(String idExterno, String nome, String sigla, String escudoUrl) {

    public TimeExterno {
        Objects.requireNonNull(idExterno, "idExterno");
        Objects.requireNonNull(nome, "nome");
        if (idExterno.isBlank() || nome.isBlank()) {
            throw new IllegalArgumentException("time externo sem id ou sem nome: " + idExterno);
        }
    }
}
