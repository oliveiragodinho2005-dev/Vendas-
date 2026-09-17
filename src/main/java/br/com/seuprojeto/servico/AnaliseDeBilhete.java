package br.com.seuprojeto.servico;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da leitura de um bilhete.
 *
 * <p>Nao traz valor esperado: EV depende das probabilidades do motor de Poisson, que
 * chega na Fase 3, e das odds sem margem, que chegam na Fase 4. A odd combinada e
 * aritmetica pura sobre o que esta no bilhete, entao ja vale agora.
 *
 * @param oddCombinada produto das odds lidas, ou nulo se nenhuma perna teve odd legivel
 */
public record AnaliseDeBilhete(List<SelecaoAnalisada> selecoes,
                               BigDecimal oddCombinada,
                               int totalDeSelecoes,
                               int selecoesVinculadas,
                               String observacao,
                               List<String> avisos) {

    public AnaliseDeBilhete {
        selecoes = List.copyOf(selecoes);
        avisos = List.copyOf(avisos);
    }
}
