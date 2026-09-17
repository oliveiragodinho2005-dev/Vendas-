package br.com.seuprojeto.servico;

import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.ia.BilheteLido;
import br.com.seuprojeto.ia.ImagemDeBilhete;
import br.com.seuprojeto.ia.LeitorDeBilhete;
import br.com.seuprojeto.ia.SelecaoLida;
import br.com.seuprojeto.repositorio.PartidaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Le um print de bilhete e liga cada selecao ao dominio.
 *
 * <p>A chamada ao modelo acontece fora de transacao de proposito: ela pode levar
 * dezenas de segundos e seguraria uma conexao do pool sem necessidade.
 */
@Service
public class AnalisadorDeBilhete {

    private final LeitorDeBilhete leitor;
    private final ResolvedorDeTimes resolvedor;
    private final PartidaRepository partidas;

    public AnalisadorDeBilhete(LeitorDeBilhete leitor,
                               ResolvedorDeTimes resolvedor,
                               PartidaRepository partidas) {
        this.leitor = leitor;
        this.resolvedor = resolvedor;
        this.partidas = partidas;
    }

    public AnaliseDeBilhete analisar(ImagemDeBilhete imagem) {
        BilheteLido lido = leitor.ler(imagem, resolvedor.nomesCadastrados());
        Map<String, ResolucaoDeTime> resolucoes = resolvedor.resolver(nomesCitados(lido));

        List<SelecaoAnalisada> analisadas = new ArrayList<>();
        for (SelecaoLida selecaoLida : lido.selecoes()) {
            analisadas.add(analisarSelecao(selecaoLida, resolucoes));
        }

        return new AnaliseDeBilhete(
                analisadas,
                combinarOdds(analisadas),
                analisadas.size(),
                (int) analisadas.stream().filter(s -> s.status() == StatusDeVinculo.VINCULADA).count(),
                lido.observacao(),
                montarAvisos(analisadas));
    }

    private static Set<String> nomesCitados(BilheteLido lido) {
        Set<String> nomes = new LinkedHashSet<>();
        for (SelecaoLida selecao : lido.selecoes()) {
            if (selecao.nomeMandante() != null) {
                nomes.add(selecao.nomeMandante());
            }
            if (selecao.nomeVisitante() != null) {
                nomes.add(selecao.nomeVisitante());
            }
        }
        return nomes;
    }

    private SelecaoAnalisada analisarSelecao(SelecaoLida lida, Map<String, ResolucaoDeTime> resolucoes) {
        if (!lida.temConfronto()) {
            return montar(lida, null, null, null, null, StatusDeVinculo.TIME_NAO_IDENTIFICADO);
        }

        ResolucaoDeTime mandante = resolucoes.get(lida.nomeMandante());
        ResolucaoDeTime visitante = resolucoes.get(lida.nomeVisitante());
        StatusDeVinculo falhaDeTime = avaliarTimes(mandante, visitante);
        if (falhaDeTime != null) {
            return montar(lida, idDe(mandante), idDe(visitante), null, null, falhaDeTime);
        }

        // Mercado nao suportado so importa depois de identificar os times: assim o
        // usuario ve que o jogo foi reconhecido e o que faltou foi o mercado.
        if (!lida.mercadoSuportado()) {
            return montar(lida, idDe(mandante), idDe(visitante), null, null,
                    StatusDeVinculo.MERCADO_NAO_SUPORTADO);
        }

        Optional<Partida> partida = localizarConfronto(mandante.time(), visitante.time());
        return partida
                .map(p -> montar(lida, idDe(mandante), idDe(visitante), p.getId(), p.getRodada(),
                        StatusDeVinculo.VINCULADA))
                .orElseGet(() -> montar(lida, idDe(mandante), idDe(visitante), null, null,
                        StatusDeVinculo.PARTIDA_NAO_ENCONTRADA));
    }

    private static StatusDeVinculo avaliarTimes(ResolucaoDeTime mandante, ResolucaoDeTime visitante) {
        if (mandante == null || visitante == null) {
            return StatusDeVinculo.TIME_NAO_CADASTRADO;
        }
        if (mandante.situacao() == ResolucaoDeTime.Situacao.AMBIGUO
                || visitante.situacao() == ResolucaoDeTime.Situacao.AMBIGUO) {
            return StatusDeVinculo.TIME_AMBIGUO;
        }
        if (!mandante.resolvido() || !visitante.resolvido()) {
            return StatusDeVinculo.TIME_NAO_CADASTRADO;
        }
        return null;
    }

    /**
     * Entre confrontos do mesmo par, prefere o que ainda vai acontecer - e nele que se
     * aposta. Se todos ja encerraram, devolve o mais recente.
     */
    private Optional<Partida> localizarConfronto(Time mandante, Time visitante) {
        List<Partida> confrontos = partidas.findByMandanteAndVisitanteOrderByDataHoraAsc(mandante, visitante);
        if (confrontos.isEmpty()) {
            return Optional.empty();
        }
        return confrontos.stream()
                .filter(partida -> partida.getStatus() != StatusPartida.ENCERRADA
                        && partida.getStatus() != StatusPartida.CANCELADA)
                .findFirst()
                .or(() -> Optional.of(confrontos.get(confrontos.size() - 1)));
    }

    private static Long idDe(ResolucaoDeTime resolucao) {
        return resolucao != null && resolucao.resolvido() ? resolucao.time().getId() : null;
    }

    private static SelecaoAnalisada montar(SelecaoLida lida,
                                           Long mandanteId,
                                           Long visitanteId,
                                           Long partidaId,
                                           Integer rodada,
                                           StatusDeVinculo status) {
        return new SelecaoAnalisada(
                lida.textoOriginal(),
                lida.nomeMandante(),
                lida.nomeVisitante(),
                mandanteId,
                visitanteId,
                lida.mercado(),
                lida.selecao(),
                lida.odd(),
                partidaId,
                rodada,
                status);
    }

    /** Odd de multipla e o produto das pernas. Pernas sem odd legivel ficam de fora. */
    private static BigDecimal combinarOdds(List<SelecaoAnalisada> selecoes) {
        BigDecimal produto = null;
        for (SelecaoAnalisada selecao : selecoes) {
            if (selecao.odd() != null) {
                produto = produto == null ? selecao.odd() : produto.multiply(selecao.odd());
            }
        }
        return produto == null ? null : produto.stripTrailingZeros();
    }

    private static List<String> montarAvisos(List<SelecaoAnalisada> selecoes) {
        List<String> avisos = new ArrayList<>();
        long semOdd = selecoes.stream().filter(selecao -> selecao.odd() == null).count();
        if (semOdd > 0) {
            avisos.add(semOdd + " selecao(oes) sem odd legivel; ficaram de fora da odd combinada");
        }
        contar(selecoes, StatusDeVinculo.TIME_NAO_CADASTRADO).ifPresent(quantidade -> avisos.add(
                quantidade + " selecao(oes) com time fora do cadastro; a ingestao da Fase 2 resolve"));
        contar(selecoes, StatusDeVinculo.TIME_AMBIGUO).ifPresent(quantidade -> avisos.add(
                quantidade + " selecao(oes) com nome de time ambiguo; cadastre um apelido para desambiguar"));
        contar(selecoes, StatusDeVinculo.MERCADO_NAO_SUPORTADO).ifPresent(quantidade -> avisos.add(
                quantidade + " selecao(oes) em mercado que o sistema ainda nao precifica"));
        return avisos;
    }

    private static Optional<Long> contar(List<SelecaoAnalisada> selecoes, StatusDeVinculo status) {
        long quantidade = selecoes.stream().filter(selecao -> selecao.status() == status).count();
        return quantidade > 0 ? Optional.of(quantidade) : Optional.empty();
    }
}
