package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.CasaDeAposta;
import br.com.seuprojeto.dominio.Odd;
import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.repositorio.CasaDeApostaRepository;
import br.com.seuprojeto.repositorio.OddRepository;
import br.com.seuprojeto.repositorio.PartidaRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava as cotacoes coletadas.
 *
 * <p>Fica em bean proprio para que a transacao valha de verdade: chamada de metodo
 * dentro da mesma classe nao passa pelo proxy do Spring, e a anotacao seria enfeite.
 *
 * <p>Cotacao sem partida ou sem casa correspondente e descartada com contagem, nao
 * com excecao: a fonte cobre mais jogos do que os que importamos, e isso e normal.
 */
@Service
public class RegistradorDeOdds {

    private static final Logger log = LoggerFactory.getLogger(RegistradorDeOdds.class);

    private final PartidaRepository partidas;
    private final CasaDeApostaRepository casas;
    private final OddRepository odds;

    public RegistradorDeOdds(PartidaRepository partidas, CasaDeApostaRepository casas, OddRepository odds) {
        this.partidas = partidas;
        this.casas = casas;
        this.odds = odds;
    }

    @Transactional
    public ResultadoDeColeta registrar(List<OddExterna> externas, String fonte) {
        Map<String, CasaDeAposta> casasPorChave = new HashMap<>();
        casas.findAll().forEach(casa -> casasPorChave.put(casa.getChave(), casa));
        Map<String, Partida> partidasPorIdExterno = new HashMap<>();

        int gravadas = 0;
        int ignoradas = 0;
        for (OddExterna externa : externas) {
            Partida partida = partidasPorIdExterno.computeIfAbsent(externa.idExternoPartida(),
                    id -> partidas.findByIdExterno(id).orElse(null));
            CasaDeAposta casa = casasPorChave.get(externa.chaveCasa());
            if (partida == null || casa == null) {
                ignoradas++;
                continue;
            }

            Odd odd = new Odd(partida, casa, externa.mercado(), externa.selecao(),
                    externa.valor(), externa.coletadaEm());
            odd.definirLinha(externa.linha());
            odd.definirFonte(fonte);
            odds.save(odd);
            gravadas++;
        }
        odds.flush();

        log.info("odds de {}: {} gravadas, {} ignoradas", fonte, gravadas, ignoradas);
        return ResultadoDeColeta.executada(gravadas, ignoradas);
    }
}
