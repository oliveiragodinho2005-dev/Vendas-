package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.repositorio.TemporadaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Traz uma temporada inteira da fonte externa para o banco.
 *
 * <p>Custo: duas requisicoes por temporada - uma de times, uma de partidas. As quatro
 * temporadas do historico saem em oito requisicoes, o que cabe folgado em um plano
 * gratuito de cem por dia. Esse numero so se mantem porque as portas trabalham em
 * lote; qualquer busca por partida individual estouraria a franquia.
 *
 * <p>A reserva de cota acontece aqui, antes de chamar a porta, porque e aqui que se
 * sabe quantas chamadas logicas serao feitas. Um adaptador que pagine internamente
 * deve reservar as unidades extras por conta propria.
 */
@Service
public class ImportadorDeTemporadas {

    private static final Logger log = LoggerFactory.getLogger(ImportadorDeTemporadas.class);

    /** Uma chamada de times mais uma de partidas. */
    private static final int CUSTO_DE_IMPORTACAO_COMPLETA = 2;
    private static final int CUSTO_SO_DE_PARTIDAS = 1;

    private final FonteDeDados fonte;
    private final TemporadaRepository temporadas;
    private final SincronizadorDeTimes sincronizadorDeTimes;
    private final SincronizadorDePartidas sincronizadorDePartidas;
    private final ControleDeCota cota;
    private final PropriedadesIngestao propriedades;

    public ImportadorDeTemporadas(FonteDeDados fonte,
                                  TemporadaRepository temporadas,
                                  SincronizadorDeTimes sincronizadorDeTimes,
                                  SincronizadorDePartidas sincronizadorDePartidas,
                                  ControleDeCota cota,
                                  PropriedadesIngestao propriedades) {
        this.fonte = fonte;
        this.temporadas = temporadas;
        this.sincronizadorDeTimes = sincronizadorDeTimes;
        this.sincronizadorDePartidas = sincronizadorDePartidas;
        this.cota = cota;
        this.propriedades = propriedades;
    }

    /** Importacao completa: times e partidas. Usada quando a temporada ainda nao existe. */
    public ResultadoDeImportacao importar(int ano) {
        return executar(ano, true);
    }

    /** Reimportacao so do calendario e dos resultados, poupando a chamada de times. */
    public ResultadoDeImportacao atualizarPartidas(int ano) {
        return executar(ano, false);
    }

    private ResultadoDeImportacao executar(int ano, boolean incluirTimes) {
        if (!fonte.disponivel()) {
            return ResultadoDeImportacao.naoExecutado(ano, "fonte " + fonte.nome() + " sem credencial");
        }

        int custo = incluirTimes ? CUSTO_DE_IMPORTACAO_COMPLETA : CUSTO_SO_DE_PARTIDAS;
        if (!cota.tentarReservar(fonte.nome(), custo, propriedades.requisicoesPorDiaResultados())) {
            return ResultadoDeImportacao.naoExecutado(ano, "cota diaria de " + fonte.nome() + " esgotada");
        }

        ResultadoDeSincronizacao resultadoDeTimes = null;
        if (incluirTimes) {
            resultadoDeTimes = sincronizadorDeTimes.sincronizar(fonte.buscarTimes(ano));
        }

        Temporada temporada = obterOuCriar(ano);
        ResultadoDeSincronizacao resultadoDePartidas =
                sincronizadorDePartidas.sincronizar(temporada, fonte.buscarPartidas(ano));

        log.info("temporada {} importada de {}: partidas {}", ano, fonte.nome(), resultadoDePartidas);
        return ResultadoDeImportacao.executado(ano, resultadoDeTimes, resultadoDePartidas);
    }

    public Temporada obterOuCriar(int ano) {
        return temporadas.findByCompeticaoAndAno(propriedades.competicao(), ano)
                .orElseGet(() -> temporadas.save(new Temporada(propriedades.competicao(), ano)));
    }

}
