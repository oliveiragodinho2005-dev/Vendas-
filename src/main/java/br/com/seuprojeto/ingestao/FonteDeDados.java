package br.com.seuprojeto.ingestao;

import java.util.List;

/**
 * Porta de resultados e calendario.
 *
 * <p>Tudo aqui e em lote por temporada, nunca por partida. No plano gratuito da
 * API-Football sao 100 requisicoes por dia: uma temporada inteira do Brasileirao cabe
 * em uma chamada, enquanto pedir jogo a jogo gastaria 380 e queimaria a franquia
 * de quase quatro dias em um unico job.
 */
public interface FonteDeDados {

    /** Nome curto da fonte, usado no controle de cota e nos logs. */
    String nome();

    /** Falso quando falta credencial; os jobs pulam em vez de falhar. */
    boolean disponivel();

    List<TimeExterno> buscarTimes(int ano);

    List<PartidaExterna> buscarPartidas(int ano);
}
