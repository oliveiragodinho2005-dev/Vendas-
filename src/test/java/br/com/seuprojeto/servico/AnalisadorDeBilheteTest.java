package br.com.seuprojeto.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.Selecao;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.ia.BilheteLido;
import br.com.seuprojeto.ia.ImagemDeBilhete;
import br.com.seuprojeto.ia.LeitorDeBilhete;
import br.com.seuprojeto.ia.SelecaoLida;
import br.com.seuprojeto.repositorio.PartidaRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Exercita o vinculo entre o que o modelo leu e o que existe no banco, com um duble
 * no lugar do leitor: nenhuma chamada de API, nenhuma chave, nenhum custo.
 */
class AnalisadorDeBilheteTest {

    private static final OffsetDateTime QUANDO = OffsetDateTime.of(2025, 5, 10, 19, 0, 0, 0, ZoneOffset.UTC);
    private static final ImagemDeBilhete IMAGEM =
            ImagemDeBilhete.de(new byte[]{1, 2, 3}, "image/png", 0);

    private final ResolvedorDeTimes resolvedor = mock(ResolvedorDeTimes.class);
    private final PartidaRepository partidas = mock(PartidaRepository.class);

    private Time flamengo;
    private Time palmeiras;
    private Partida confronto;

    @BeforeEach
    void preparar() {
        flamengo = time(1L, "Flamengo");
        palmeiras = time(2L, "Palmeiras");
        confronto = partida(10L, flamengo, palmeiras);
        when(resolvedor.nomesCadastrados()).thenReturn(List.of("Flamengo", "Palmeiras"));
        when(resolvedor.resolver(any())).thenReturn(Map.of(
                "Flamengo", ResolucaoDeTime.resolvido("Flamengo", flamengo),
                "Palmeiras", ResolucaoDeTime.resolvido("Palmeiras", palmeiras)));
        when(partidas.findByMandanteAndVisitanteOrderByDataHoraAsc(flamengo, palmeiras))
                .thenReturn(List.of(confronto));
    }

    @Test
    void vinculaSelecaoCompletaAPartidaDoBanco() {
        AnaliseDeBilhete analise = analisar(selecao(Mercado.RESULTADO_1X2, Selecao.CASA, "2.10"));

        assertThat(analise.selecoes()).hasSize(1);
        SelecaoAnalisada unica = analise.selecoes().get(0);
        assertThat(unica.status()).isEqualTo(StatusDeVinculo.VINCULADA);
        assertThat(unica.partidaId()).isEqualTo(10L);
        assertThat(unica.rodada()).isEqualTo(7);
        assertThat(unica.mandanteId()).isEqualTo(1L);
        assertThat(unica.visitanteId()).isEqualTo(2L);
        assertThat(analise.selecoesVinculadas()).isEqualTo(1);
    }

    @Test
    void oddCombinadaEOProdutoDasPernas() {
        AnaliseDeBilhete analise = analisar(
                selecao(Mercado.RESULTADO_1X2, Selecao.CASA, "2.00"),
                selecao(Mercado.AMBAS_MARCAM, Selecao.SIM, "1.50"));

        assertThat(analise.oddCombinada()).isEqualByComparingTo("3.00");
        assertThat(analise.totalDeSelecoes()).isEqualTo(2);
    }

    @Test
    void pernaSemOddFicaForaDaCombinadaEGeraAviso() {
        AnaliseDeBilhete analise = analisar(
                selecao(Mercado.RESULTADO_1X2, Selecao.CASA, "2.00"),
                selecao(Mercado.AMBAS_MARCAM, Selecao.SIM, null));

        assertThat(analise.oddCombinada()).isEqualByComparingTo("2.00");
        assertThat(analise.avisos()).anyMatch(aviso -> aviso.contains("sem odd legivel"));
    }

    @Test
    void semNenhumaOddACombinadaEhNula() {
        AnaliseDeBilhete analise = analisar(selecao(Mercado.RESULTADO_1X2, Selecao.CASA, null));

        assertThat(analise.oddCombinada()).isNull();
    }

    @Test
    void mercadoForaDosTresSuportadosEhReportadoSemVinculo() {
        // Bilhete real mistura escanteios e handicap; o modelo devolve mercado nulo.
        AnaliseDeBilhete analise = analisar(new SelecaoLida(
                "Flamengo x Palmeiras - Escanteios +9.5", "Flamengo", "Palmeiras",
                null, null, new BigDecimal("1.80")));

        SelecaoAnalisada unica = analise.selecoes().get(0);
        assertThat(unica.status()).isEqualTo(StatusDeVinculo.MERCADO_NAO_SUPORTADO);
        assertThat(unica.partidaId()).isNull();
        // Ainda assim os times foram reconhecidos: isso e informacao util na tela.
        assertThat(unica.mandanteId()).isEqualTo(1L);
        assertThat(analise.avisos()).anyMatch(aviso -> aviso.contains("ainda nao precifica"));
    }

    @Test
    void selecaoSemTimesLegiveisParaEmTimeNaoIdentificado() {
        AnaliseDeBilhete analise = analisar(new SelecaoLida(
                "linha borrada", null, null, Mercado.RESULTADO_1X2, Selecao.CASA, new BigDecimal("2.00")));

        assertThat(analise.selecoes().get(0).status()).isEqualTo(StatusDeVinculo.TIME_NAO_IDENTIFICADO);
    }

    @Test
    void timeForaDoCadastroNaoVincula() {
        when(resolvedor.resolver(any())).thenReturn(Map.of(
                "Flamengo", ResolucaoDeTime.resolvido("Flamengo", flamengo),
                "Bangu", ResolucaoDeTime.naoCadastrado("Bangu")));

        AnaliseDeBilhete analise = analisar(new SelecaoLida(
                "Flamengo x Bangu", "Flamengo", "Bangu",
                Mercado.RESULTADO_1X2, Selecao.CASA, new BigDecimal("1.40")));

        assertThat(analise.selecoes().get(0).status()).isEqualTo(StatusDeVinculo.TIME_NAO_CADASTRADO);
        assertThat(analise.selecoesVinculadas()).isZero();
    }

    @Test
    void nomeAmbiguoNaoViraChute() {
        when(resolvedor.resolver(any())).thenReturn(Map.of(
                "América", ResolucaoDeTime.ambiguo("América"),
                "Palmeiras", ResolucaoDeTime.resolvido("Palmeiras", palmeiras)));

        AnaliseDeBilhete analise = analisar(new SelecaoLida(
                "América x Palmeiras", "América", "Palmeiras",
                Mercado.RESULTADO_1X2, Selecao.FORA, new BigDecimal("1.90")));

        SelecaoAnalisada unica = analise.selecoes().get(0);
        assertThat(unica.status()).isEqualTo(StatusDeVinculo.TIME_AMBIGUO);
        assertThat(unica.mandanteId()).isNull();
        assertThat(analise.avisos()).anyMatch(aviso -> aviso.contains("ambiguo"));
    }

    @Test
    void confrontoInexistenteNoBancoEhReportado() {
        when(partidas.findByMandanteAndVisitanteOrderByDataHoraAsc(flamengo, palmeiras))
                .thenReturn(List.of());

        AnaliseDeBilhete analise = analisar(selecao(Mercado.RESULTADO_1X2, Selecao.CASA, "2.10"));

        assertThat(analise.selecoes().get(0).status()).isEqualTo(StatusDeVinculo.PARTIDA_NAO_ENCONTRADA);
    }

    @Test
    void imagemQueNaoEBilheteDevolveObservacaoDoModelo() {
        AnalisadorDeBilhete analisador = new AnalisadorDeBilhete(
                (imagem, times) -> BilheteLido.vazio("a imagem e a foto de um cachorro"),
                resolvedor, partidas);

        AnaliseDeBilhete analise = analisador.analisar(IMAGEM);

        assertThat(analise.selecoes()).isEmpty();
        assertThat(analise.totalDeSelecoes()).isZero();
        assertThat(analise.observacao()).contains("cachorro");
    }

    @Test
    void osNomesCadastradosSaoPassadosAoLeitorComoVocabulario() {
        List<String> recebidos = new java.util.ArrayList<>();
        LeitorDeBilhete espiao = (imagem, times) -> {
            recebidos.addAll(times);
            return BilheteLido.vazio(null);
        };

        new AnalisadorDeBilhete(espiao, resolvedor, partidas).analisar(IMAGEM);

        assertThat(recebidos).containsExactly("Flamengo", "Palmeiras");
    }

    private AnaliseDeBilhete analisar(SelecaoLida... selecoes) {
        LeitorDeBilhete duble = (imagem, times) -> new BilheteLido(List.of(selecoes), null);
        return new AnalisadorDeBilhete(duble, resolvedor, partidas).analisar(IMAGEM);
    }

    private static SelecaoLida selecao(Mercado mercado, Selecao selecao, String odd) {
        return new SelecaoLida("Flamengo x Palmeiras", "Flamengo", "Palmeiras",
                mercado, selecao, odd == null ? null : new BigDecimal(odd));
    }

    private static Time time(long id, String nome) {
        Time time = new Time(nome);
        ReflectionTestUtils.setField(time, "id", id);
        return time;
    }

    private static Partida partida(long id, Time mandante, Time visitante) {
        Partida partida = new Partida(Temporada.brasileiraoSerieA(2025), 7, QUANDO, mandante, visitante);
        ReflectionTestUtils.setField(partida, "id", id);
        return partida;
    }
}
