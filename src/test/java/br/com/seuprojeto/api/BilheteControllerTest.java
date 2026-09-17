package br.com.seuprojeto.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import br.com.seuprojeto.ia.IaNaoConfiguradaException;
import br.com.seuprojeto.ia.LeituraDeBilheteException;
import br.com.seuprojeto.ia.claude.PropriedadesIa;
import br.com.seuprojeto.servico.AnaliseDeBilhete;
import br.com.seuprojeto.servico.AnalisadorDeBilhete;
import br.com.seuprojeto.servico.SelecaoAnalisada;
import br.com.seuprojeto.servico.StatusDeVinculo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Borda HTTP isolada: sem banco, sem rede, sem chave. O que se prova aqui e que cada
 * tipo de falha vira o status certo - quem consome precisa distinguir "corrija sua
 * imagem" de "o servidor esta sem chave".
 */
@WebMvcTest(BilheteController.class)
@Import(BilheteControllerTest.Configuracao.class)
class BilheteControllerTest {

    @TestConfiguration
    static class Configuracao {
        @Bean
        PropriedadesIa propriedadesIa() {
            return new PropriedadesIa("", "claude-opus-5", "high", 16000, 5);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalisadorDeBilhete analisador;

    private static MockMultipartFile arquivo(String tipo, byte[] conteudo) {
        return new MockMultipartFile("imagem", "bilhete.png", tipo, conteudo);
    }

    @Test
    void devolveAsSelecoesLidas() throws Exception {
        when(analisador.analisar(any())).thenReturn(new AnaliseDeBilhete(
                List.of(new SelecaoAnalisada("Flamengo x Palmeiras", "Flamengo", "Palmeiras",
                        1L, 2L, Mercado.RESULTADO_1X2, Selecao.CASA,
                        new BigDecimal("2.10"), 10L, 7, StatusDeVinculo.VINCULADA)),
                new BigDecimal("2.10"), 1, 1, null, List.of()));

        mockMvc.perform(multipart("/api/bilhetes/analise").file(arquivo("image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeSelecoes").value(1))
                .andExpect(jsonPath("$.selecoesVinculadas").value(1))
                .andExpect(jsonPath("$.oddCombinada").value(2.10))
                .andExpect(jsonPath("$.selecoes[0].mercado").value("RESULTADO_1X2"))
                .andExpect(jsonPath("$.selecoes[0].selecao").value("CASA"))
                .andExpect(jsonPath("$.selecoes[0].status").value("VINCULADA"));
    }

    @Test
    void tipoDeArquivoNaoSuportadoVira400() throws Exception {
        mockMvc.perform(multipart("/api/bilhetes/analise").file(arquivo("application/pdf", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Imagem invalida"));
    }

    @Test
    void arquivoVazioVira400() throws Exception {
        mockMvc.perform(multipart("/api/bilhetes/analise").file(arquivo("image/png", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faltaDeChaveVira503EDizOQueFalta() throws Exception {
        when(analisador.analisar(any()))
                .thenThrow(new IaNaoConfiguradaException("ANTHROPIC_API_KEY nao configurada"));

        mockMvc.perform(multipart("/api/bilhetes/analise").file(arquivo("image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("ANTHROPIC_API_KEY nao configurada"));
    }

    @Test
    void falhaDaApiExternaVira502() throws Exception {
        when(analisador.analisar(any()))
                .thenThrow(new LeituraDeBilheteException("a API do Claude recusou a requisicao"));

        mockMvc.perform(multipart("/api/bilhetes/analise").file(arquivo("image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha ao ler o bilhete"));
    }
}
