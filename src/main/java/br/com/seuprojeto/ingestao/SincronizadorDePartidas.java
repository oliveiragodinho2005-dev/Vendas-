package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.repositorio.PartidaRepository;
import br.com.seuprojeto.repositorio.TimeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o calendario e os resultados de uma temporada sem duplicar.
 *
 * <p>Roda depois de {@link SincronizadorDeTimes}: as partidas referenciam os times
 * pelo id externo, e um time que ainda nao existe faz a partida ser ignorada com
 * aviso, em vez de criar um time meia-boca a partir de um id solto.
 *
 * <p>E idempotente por construcao: rodar a mesma temporada duas vezes nao cria nada
 * na segunda vez. Isso importa porque a atualizacao de resultados vai reimportar a
 * temporada corrente varias vezes por semana.
 */
@Service
public class SincronizadorDePartidas {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorDePartidas.class);

    private final PartidaRepository partidas;
    private final TimeRepository times;

    public SincronizadorDePartidas(PartidaRepository partidas, TimeRepository times) {
        this.partidas = partidas;
        this.times = times;
    }

    @Transactional
    public ResultadoDeSincronizacao sincronizar(Temporada temporada, List<PartidaExterna> externas) {
        ResultadoDeSincronizacao.Acumulador acumulador = ResultadoDeSincronizacao.acumulador();

        Map<String, Time> timesPorIdExterno = new HashMap<>();
        times.findAll().stream()
                .filter(time -> time.getIdExterno() != null)
                .forEach(time -> timesPorIdExterno.put(time.getIdExterno(), time));

        Map<String, Partida> porIdExterno = new HashMap<>();
        Map<String, Partida> porConfronto = new HashMap<>();
        for (Partida partida : partidas.findByTemporadaOrderByDataHoraAsc(temporada)) {
            if (partida.getIdExterno() != null) {
                porIdExterno.put(partida.getIdExterno(), partida);
            }
            porConfronto.put(chaveDeConfronto(partida.getMandante(), partida.getVisitante()), partida);
        }

        for (PartidaExterna externa : externas) {
            Time mandante = timesPorIdExterno.get(externa.idExternoMandante());
            Time visitante = timesPorIdExterno.get(externa.idExternoVisitante());
            if (mandante == null || visitante == null) {
                acumulador.ignorado("partida %s ignorada: time externo %s nao esta cadastrado".formatted(
                        externa.idExterno(),
                        mandante == null ? externa.idExternoMandante() : externa.idExternoVisitante()));
                continue;
            }

            Partida existente = porIdExterno.get(externa.idExterno());
            if (existente == null) {
                existente = porConfronto.get(chaveDeConfronto(mandante, visitante));
            }

            if (existente != null) {
                existente.definirIdExterno(externa.idExterno());
                aplicar(existente, externa, acumulador);
                acumulador.atualizado();
                continue;
            }

            Partida nova = new Partida(temporada, externa.rodada(), externa.dataHora(), mandante, visitante);
            nova.definirIdExterno(externa.idExterno());
            aplicar(nova, externa, acumulador);
            partidas.save(nova);
            porIdExterno.put(externa.idExterno(), nova);
            porConfronto.put(chaveDeConfronto(mandante, visitante), nova);
            acumulador.criado();
        }

        partidas.flush();
        ResultadoDeSincronizacao resultado = acumulador.concluir();
        log.info("partidas da temporada {} sincronizadas: {}", temporada.getAno(), resultado);
        return resultado;
    }

    private static void aplicar(Partida partida,
                                PartidaExterna externa,
                                ResultadoDeSincronizacao.Acumulador acumulador) {
        partida.reagendar(externa.dataHora(), externa.rodada());
        if (externa.estadio() != null && !externa.estadio().isBlank()) {
            partida.definirEstadio(externa.estadio());
        }

        if (externa.temPlacar()) {
            // registrarPlacar encerra a partida, entao so vale quando a fonte diz que acabou.
            if (externa.status() == StatusPartida.ENCERRADA) {
                partida.registrarPlacar(externa.golsMandante(), externa.golsVisitante());
                return;
            }
            partida.alterarStatus(externa.status());
            return;
        }

        if (externa.status() == StatusPartida.ENCERRADA) {
            // O banco proibe encerrada sem placar, e com razao: partida encerrada sem
            // gols corromperia a base historica que alimenta o modelo.
            acumulador.avisoSemContar("partida %s veio encerrada e sem placar; status mantido em %s"
                    .formatted(externa.idExterno(), partida.getStatus()));
            return;
        }
        partida.alterarStatus(externa.status());
    }

    private static String chaveDeConfronto(Time mandante, Time visitante) {
        return mandante.getId() + "x" + visitante.getId();
    }
}
