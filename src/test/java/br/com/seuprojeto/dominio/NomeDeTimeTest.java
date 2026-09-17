package br.com.seuprojeto.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NomeDeTimeTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "Flamengo, flamengo",
            "Atlético-MG, atletico mg",
            "São Paulo, sao paulo",
            "Grêmio, gremio",
            "  Vasco da Gama  , vasco da gama",
            "RB BRAGANTINO, rb bragantino",
    })
    void normalizarTiraAcentoPontuacaoECaixa(String entrada, String esperado) {
        assertThat(NomeDeTime.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "Clube de Regatas do Flamengo, flamengo",
            "Flamengo, flamengo",
            "Sociedade Esportiva Palmeiras, palmeiras",
            "Palmeiras, palmeiras",
            "Associação Atlética Ponte Preta, ponte preta",
            "EC Bahia, bahia",
            "Bahia, bahia",
    })
    void chaveIgnoraRuidoSocietario(String entrada, String esperado) {
        assertThat(NomeDeTime.chave(entrada)).isEqualTo(esperado);
    }

    @Test
    void nomeFormalECurtoCaemNaMesmaChave() {
        assertThat(NomeDeTime.chave("Clube de Regatas do Flamengo"))
                .isEqualTo(NomeDeTime.chave("Flamengo"));
    }

    @Test
    void sufixoDeEstadoEPreservado() {
        // Tirar o "-MG" faria America-MG e America-RN colidirem, e um vinculo errado
        // e pior que nenhum: ele contamina o EV sem dar sinal.
        assertThat(NomeDeTime.chave("América-MG")).isEqualTo("america mg");
        assertThat(NomeDeTime.chave("América-RN")).isEqualTo("america rn");
        assertThat(NomeDeTime.chave("América-MG")).isNotEqualTo(NomeDeTime.chave("América-RN"));
    }

    @Test
    void nomeFeitoSoDeTokenDescartavelNaoViraVazio() {
        // Se a limpeza zerasse o nome, qualquer time casaria com qualquer outro.
        assertThat(NomeDeTime.chave("Esporte Clube")).isEqualTo("esporte clube");
        assertThat(NomeDeTime.chave("FC")).isEqualTo("fc");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "---"})
    void entradaSemConteudoViraVazio(String entrada) {
        assertThat(NomeDeTime.chave(entrada)).isEmpty();
    }

    @Test
    void nuloNaoQuebra() {
        assertThat(NomeDeTime.normalizar(null)).isEmpty();
        assertThat(NomeDeTime.chave(null)).isEmpty();
    }
}
