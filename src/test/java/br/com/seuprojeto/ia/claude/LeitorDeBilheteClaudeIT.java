package br.com.seuprojeto.ia.claude;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import br.com.seuprojeto.ia.BilheteLido;
import br.com.seuprojeto.ia.ImagemDeBilhete;
import br.com.seuprojeto.ia.SelecaoLida;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Unico teste que fala com a API de verdade, e portanto o unico que custa dinheiro.
 *
 * <p>Sem {@code ANTHROPIC_API_KEY} no ambiente ele e pulado, entao {@code mvn verify}
 * continua valido em CI e em maquina sem credencial.
 *
 * <p>O bilhete e desenhado aqui em vez de versionado como arquivo: print de bilhete
 * real carrega identificador de aposta e dado pessoal, e isso nao entra no repositorio.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class LeitorDeBilheteClaudeIT {

    private final LeitorDeBilheteClaude leitor = new LeitorDeBilheteClaude(
            new PropriedadesIa(System.getenv("ANTHROPIC_API_KEY"), "claude-opus-5", "high", 16000, 5));

    @Test
    void extraiAsDuasPernasDeUmBilheteDesenhado() {
        BilheteLido lido = leitor.ler(
                ImagemDeBilhete.de(bilheteSintetico(), "image/png", 0),
                List.of("Flamengo", "Palmeiras", "Gremio"));

        assertThat(lido.selecoes()).hasSize(2);

        SelecaoLida primeira = lido.selecoes().get(0);
        assertThat(primeira.mercado()).isEqualTo(Mercado.RESULTADO_1X2);
        assertThat(primeira.selecao()).isEqualTo(Selecao.CASA);
        assertThat(primeira.odd()).isEqualByComparingTo("2.10");
        assertThat(primeira.nomeMandante()).containsIgnoringCase("flamengo");
        assertThat(primeira.nomeVisitante()).containsIgnoringCase("palmeiras");

        SelecaoLida segunda = lido.selecoes().get(1);
        assertThat(segunda.mercado()).isEqualTo(Mercado.AMBAS_MARCAM);
        assertThat(segunda.selecao()).isEqualTo(Selecao.SIM);
        // Escrita com virgula na imagem, como bilhete brasileiro faz.
        assertThat(segunda.odd()).isEqualByComparingTo("1.75");
    }

    @Test
    void imagemSemBilheteNaoInventaSelecao() {
        BilheteLido lido = leitor.ler(
                ImagemDeBilhete.de(imagemSemBilhete(), "image/png", 0), List.of());

        assertThat(lido.selecoes()).isEmpty();
        assertThat(lido.observacao()).isNotBlank();
    }

    private static byte[] bilheteSintetico() {
        return desenhar(linhas -> {
            linhas.texto("APOSTA MULTIPLA", 22, true);
            linhas.pular();
            linhas.texto("Flamengo x Palmeiras", 18, true);
            linhas.texto("Resultado Final: Casa", 16, false);
            linhas.texto("Odd: 2.10", 16, false);
            linhas.pular();
            linhas.texto("Palmeiras x Gremio", 18, true);
            linhas.texto("Ambas as equipes marcam: Sim", 16, false);
            linhas.texto("Odd: 1,75", 16, false);
            linhas.pular();
            linhas.texto("Odd total: 3,67", 18, true);
            linhas.texto("Valor apostado: R$ 20,00", 16, false);
        });
    }

    private static byte[] imagemSemBilhete() {
        return desenhar(linhas -> {
            linhas.texto("LISTA DE COMPRAS", 22, true);
            linhas.pular();
            linhas.texto("2 kg de arroz", 16, false);
            linhas.texto("1 litro de leite", 16, false);
            linhas.texto("Cafe", 16, false);
        });
    }

    private static byte[] desenhar(java.util.function.Consumer<Pincel> conteudo) {
        BufferedImage imagem = new BufferedImage(520, 440, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = imagem.createGraphics();
        grafico.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, imagem.getWidth(), imagem.getHeight());
        grafico.setColor(Color.BLACK);

        conteudo.accept(new Pincel(grafico));
        grafico.dispose();

        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Escreve linha a linha, controlando a altura corrente. */
    private static final class Pincel {

        private final Graphics2D grafico;
        private int altura = 44;

        private Pincel(Graphics2D grafico) {
            this.grafico = grafico;
        }

        void texto(String conteudo, int tamanho, boolean negrito) {
            grafico.setFont(new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho));
            grafico.drawString(conteudo, 34, altura);
            altura += tamanho + 14;
        }

        void pular() {
            altura += 18;
        }
    }
}
