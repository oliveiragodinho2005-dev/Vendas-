package br.com.seuprojeto.ingestao;

import java.util.List;

/**
 * O que vale a pena buscar agora - e, por consequencia, o que nao vale.
 *
 * <p>Esta e a peca que cumpre o "sem chamadas duplicadas": em vez de o job sair
 * buscando tudo a cada disparo, ele pergunta ao planejador, que olha o estado do
 * banco e o relogio e responde so o que mudou.
 *
 * @param requisicoesEstimadas custo do plano, para conferir contra a franquia antes de gastar
 */
public record PlanoDeIngestao(List<Integer> temporadasParaImportar,
                              List<Integer> temporadasParaAtualizar,
                              boolean coletarOdds,
                              int partidasNaJanelaDeOdds,
                              int requisicoesEstimadas,
                              List<String> motivos) {

    public PlanoDeIngestao {
        temporadasParaImportar = List.copyOf(temporadasParaImportar);
        temporadasParaAtualizar = List.copyOf(temporadasParaAtualizar);
        motivos = List.copyOf(motivos);
    }

    public boolean vazio() {
        return temporadasParaImportar.isEmpty() && temporadasParaAtualizar.isEmpty() && !coletarOdds;
    }
}
