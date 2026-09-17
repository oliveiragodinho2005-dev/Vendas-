package br.com.seuprojeto.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ImagemDeBilheteTest {

    private static final byte[] CONTEUDO = {1, 2, 3, 4};
    private static final long SEM_LIMITE = 0;

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "image/jpeg, image/jpeg",
            "image/jpg, image/jpeg",
            "IMAGE/PNG, image/png",
            "'image/png; charset=binary', image/png",
            "image/x-png, image/png",
            "image/webp, image/webp",
    })
    void normalizaOTipoDeclaradoPeloCliente(String declarado, String esperado) {
        assertThat(ImagemDeBilhete.de(CONTEUDO, declarado, SEM_LIMITE).mediaType()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "text/plain", "image/bmp", "image/heic"})
    void rejeitaTipoQueAApiNaoAceita(String tipo) {
        assertThatExceptionOfType(ImagemInvalidaException.class)
                .isThrownBy(() -> ImagemDeBilhete.de(CONTEUDO, tipo, SEM_LIMITE))
                .withMessageContaining("nao suportado");
    }

    @Test
    void rejeitaImagemVazia() {
        assertThatExceptionOfType(ImagemInvalidaException.class)
                .isThrownBy(() -> ImagemDeBilhete.de(new byte[0], "image/png", SEM_LIMITE))
                .withMessageContaining("vazia");
    }

    @Test
    void rejeitaTipoAusente() {
        assertThatExceptionOfType(ImagemInvalidaException.class)
                .isThrownBy(() -> ImagemDeBilhete.de(CONTEUDO, null, SEM_LIMITE))
                .withMessageContaining("nao informado");
    }

    @Test
    void rejeitaImagemAcimaDoLimiteAntesDeGastarChamada() {
        assertThatExceptionOfType(ImagemInvalidaException.class)
                .isThrownBy(() -> ImagemDeBilhete.de(new byte[10], "image/png", 4))
                .withMessageContaining("excede o limite");
    }

    @Test
    void aceitaImagemExatamenteNoLimite() {
        assertThat(ImagemDeBilhete.de(new byte[4], "image/png", 4).tamanhoEmBytes()).isEqualTo(4);
    }
}
