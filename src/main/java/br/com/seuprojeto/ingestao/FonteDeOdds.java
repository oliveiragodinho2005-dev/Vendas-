package br.com.seuprojeto.ingestao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Porta de odds, sempre via API agregadora.
 *
 * <p>Nunca por scraping de site de apostas: o projeto inteiro depende de a bet365 vir
 * de um agregador com termos de uso compativeis.
 *
 * <p>A busca e por janela de tempo, nao por partida, pelo mesmo motivo de economia
 * de requisicoes que vale em {@link FonteDeDados}.
 */
public interface FonteDeOdds {

    String nome();

    boolean disponivel();

    /**
     * Odds das partidas que comecam na janela informada.
     *
     * @param chavesDeCasas casas de interesse, por exemplo bet365 e pinnacle; o
     *                      adaptador filtra na fonte quando ela permitir, e em memoria
     *                      quando nao permitir
     */
    List<OddExterna> buscarOdds(OffsetDateTime inicio, OffsetDateTime fim, Set<String> chavesDeCasas);
}
