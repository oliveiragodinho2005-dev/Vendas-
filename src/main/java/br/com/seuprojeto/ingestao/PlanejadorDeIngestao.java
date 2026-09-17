package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.repositorio.OddRepository;
import br.com.seuprojeto.repositorio.PartidaRepository;
import br.com.seuprojeto.repositorio.TemporadaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide o que a ingestao deve buscar.
 *
 * <p>Toda a economia de requisicoes mora aqui. Uma temporada encerrada nao se busca de
 * novo; uma temporada corrente so se busca quando ha jogo cujo horario ja passou e que
 * ainda consta como agendado; odds so quando ha jogo na janela e a ultima coleta ja
 * envelheceu. Um job que ignorasse isso torraria os 100 pedidos diarios do plano
 * gratuito antes do almoco.
 */
@Service
public class PlanejadorDeIngestao {

    /**
     * Folga depois do apito inicial antes de considerar que a partida deveria ter
     * terminado. Noventa minutos de jogo mais intervalo, acrescimos e atraso da fonte.
     */
    private static final Duration MARGEM_APOS_INICIO = Duration.ofHours(3);

    private static final List<StatusPartida> PENDENTES =
            List.of(StatusPartida.AGENDADA, StatusPartida.EM_ANDAMENTO);

    private final TemporadaRepository temporadas;
    private final PartidaRepository partidas;
    private final OddRepository odds;
    private final PropriedadesIngestao propriedades;
    private final Clock relogio;

    public PlanejadorDeIngestao(TemporadaRepository temporadas,
                                PartidaRepository partidas,
                                OddRepository odds,
                                PropriedadesIngestao propriedades,
                                Clock relogio) {
        this.temporadas = temporadas;
        this.partidas = partidas;
        this.odds = odds;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public PlanoDeIngestao planejar() {
        OffsetDateTime agora = OffsetDateTime.now(relogio).withOffsetSameInstant(ZoneOffset.UTC);
        List<Integer> anos = propriedades.anosDoHistorico(LocalDate.now(relogio).getYear());

        List<Integer> paraImportar = new ArrayList<>();
        List<Integer> paraAtualizar = new ArrayList<>();
        List<String> motivos = new ArrayList<>();

        for (int ano : anos) {
            Optional<Temporada> temporada = temporadas.findByCompeticaoAndAno(propriedades.competicao(), ano);
            if (temporada.isEmpty() || partidas.countByTemporada(temporada.get()) == 0) {
                paraImportar.add(ano);
                motivos.add("temporada %d ainda nao foi importada".formatted(ano));
                continue;
            }
            long pendentes = partidas.findByTemporadaAndStatusInAndDataHoraBeforeOrderByDataHoraAsc(
                    temporada.get(), PENDENTES, agora.minus(MARGEM_APOS_INICIO)).size();
            if (pendentes > 0) {
                paraAtualizar.add(ano);
                motivos.add("temporada %d tem %d jogo(s) sem resultado".formatted(ano, pendentes));
            }
        }

        int naJanela = partidas.findByStatusAndDataHoraBetweenOrderByDataHoraAsc(
                StatusPartida.AGENDADA, agora, agora.plusHours(propriedades.janelaDeOddsEmHoras())).size();
        boolean coletarOdds = naJanela > 0 && coletaEnvelheceu(agora);
        if (naJanela > 0 && !coletarOdds) {
            motivos.add("odds coletadas ha menos de " + propriedades.intervaloEntreColetasDeOdds());
        } else if (coletarOdds) {
            motivos.add("%d jogo(s) na janela de odds".formatted(naJanela));
        }

        int estimativa = paraImportar.size() * 2 + paraAtualizar.size() + (coletarOdds ? 1 : 0);
        return new PlanoDeIngestao(paraImportar, paraAtualizar, coletarOdds, naJanela, estimativa, motivos);
    }

    private boolean coletaEnvelheceu(OffsetDateTime agora) {
        return odds.findTopByOrderByColetadaEmDesc()
                .map(ultima -> ultima.getColetadaEm()
                        .isBefore(agora.minus(propriedades.intervaloEntreColetasDeOdds())))
                .orElse(true);
    }
}
