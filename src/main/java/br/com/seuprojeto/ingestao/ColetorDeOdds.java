package br.com.seuprojeto.ingestao;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coleta as odds das partidas proximas e grava cada leitura como linha nova.
 *
 * <p>Uma requisicao cobre a janela inteira, nao uma por partida - e o mesmo cuidado
 * de franquia que vale na importacao de temporadas.
 *
 * <p>Nada e sobrescrito: a serie temporal de {@code odds} e o que permite medir
 * movimento de linha e, na Fase 5, o CLV contra a odd de fechamento.
 */
@Service
public class ColetorDeOdds {

    private static final Logger log = LoggerFactory.getLogger(ColetorDeOdds.class);

    private final FonteDeOdds fonte;
    private final RegistradorDeOdds registrador;
    private final ControleDeCota cota;
    private final PropriedadesIngestao propriedades;
    private final Clock relogio;

    public ColetorDeOdds(FonteDeOdds fonte,
                         RegistradorDeOdds registrador,
                         ControleDeCota cota,
                         PropriedadesIngestao propriedades,
                         Clock relogio) {
        this.fonte = fonte;
        this.registrador = registrador;
        this.cota = cota;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public ResultadoDeColeta coletar() {
        if (!fonte.disponivel()) {
            return ResultadoDeColeta.naoExecutada("fonte " + fonte.nome() + " sem credencial");
        }
        if (!cota.tentarReservar(fonte.nome(), 1, propriedades.requisicoesPorDiaOdds())) {
            return ResultadoDeColeta.naoExecutada("cota diaria de " + fonte.nome() + " esgotada");
        }

        OffsetDateTime inicio = OffsetDateTime.now(relogio).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime fim = inicio.plusHours(propriedades.janelaDeOddsEmHoras());
        List<OddExterna> externas = fonte.buscarOdds(
                inicio, fim, Set.copyOf(propriedades.casasDeInteresse()));

        return registrador.registrar(externas, fonte.nome());
    }

}
