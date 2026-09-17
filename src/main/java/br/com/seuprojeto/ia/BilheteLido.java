package br.com.seuprojeto.ia;

import java.util.List;
import java.util.Objects;

/**
 * Resultado bruto da leitura da imagem.
 *
 * @param selecoes   pernas encontradas, possivelmente vazia
 * @param observacao nota do modelo quando a imagem nao e um bilhete ou esta ilegivel
 */
public record BilheteLido(List<SelecaoLida> selecoes, String observacao) {

    public BilheteLido {
        selecoes = List.copyOf(Objects.requireNonNull(selecoes, "selecoes"));
    }

    public static BilheteLido vazio(String observacao) {
        return new BilheteLido(List.of(), observacao);
    }
}
