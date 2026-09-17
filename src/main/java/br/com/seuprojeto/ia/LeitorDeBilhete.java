package br.com.seuprojeto.ia;

import java.util.List;

/**
 * Porta de leitura de bilhete por imagem.
 *
 * <p>Interface em Java puro de proposito: o servico depende dela, nao do SDK, entao
 * os testes rodam com um dublê, sem rede e sem chave de API.
 */
public interface LeitorDeBilhete {

    /**
     * Extrai as selecoes visiveis na imagem.
     *
     * @param imagem         print do bilhete
     * @param timesConhecidos nomes ja cadastrados, usados como vocabulario para o modelo
     *                        grafar os times do jeito que o banco espera; pode ser vazia
     * @throws IaNaoConfiguradaException  se nao houver credencial configurada
     * @throws LeituraDeBilheteException  se a API falhar ou recusar a leitura
     */
    BilheteLido ler(ImagemDeBilhete imagem, List<String> timesConhecidos);
}
