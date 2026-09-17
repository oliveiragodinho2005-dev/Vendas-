package br.com.seuprojeto.ia.claude;

import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Selecao;
import br.com.seuprojeto.ia.BilheteLido;
import br.com.seuprojeto.ia.IaNaoConfiguradaException;
import br.com.seuprojeto.ia.ImagemDeBilhete;
import br.com.seuprojeto.ia.LeitorDeBilhete;
import br.com.seuprojeto.ia.LeituraDeBilheteException;
import br.com.seuprojeto.ia.SelecaoLida;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredOutputConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Le o bilhete com o modelo de visao do Claude, usando saida estruturada tipada.
 *
 * <p>Escopo deliberadamente estreito: o modelo transcreve o que esta na imagem e
 * nada mais. Probabilidade e valor esperado sao do motor de calculo - um LLM nao
 * produz probabilidade calibrada, produz um numero que parece uma.
 */
@Component
public class LeitorDeBilheteClaude implements LeitorDeBilhete {

    private static final Logger log = LoggerFactory.getLogger(LeitorDeBilheteClaude.class);

    /** Teto de nomes enviados como vocabulario, para nao inflar o prompt sem ganho. */
    private static final int MAXIMO_DE_TIMES_NO_PROMPT = 200;

    private static final String INSTRUCAO_SISTEMA = """
            Voce extrai selecoes de prints de bilhetes de apostas esportivas brasileiros.

            Regras:
            - Extraia SOMENTE o que esta visivel na imagem. Nunca invente time, mercado ou odd.
            - Campo que voce nao conseguir ler fica vazio. Vazio e resposta valida; chute nao e.
            - A odd e decimal e pode vir com virgula. Transcreva como aparece na imagem.
            - O campo mercado deve ser um destes quatro valores:
              RESULTADO_1X2    - "vencedor da partida", "resultado final", "1X2", "casa/empate/fora"
              OVER_UNDER_2_5   - "mais/menos de 2.5 gols", "over/under 2.5"; SOMENTE a linha 2.5
              AMBAS_MARCAM     - "ambas marcam", "ambos os times marcam", "BTTS"
              OUTRO            - qualquer outro mercado: handicap, escanteios, cartoes, placar
                                 exato, artilheiro, ou linha de gols diferente de 2.5
            - O campo selecao segue o mercado: RESULTADO_1X2 usa CASA, EMPATE ou FORA;
              OVER_UNDER_2_5 usa OVER ou UNDER; AMBAS_MARCAM usa SIM ou NAO; OUTRO fica vazio.
            - Mandante e o time da casa, a esquerda em "A x B". Mantenha a ordem da imagem.
            - Se a imagem nao for um bilhete de apostas, devolva selecoes vazia e diga o porque
              em observacao.

            Voce nao estima probabilidade, nao opina se a aposta e boa e nao calcula retorno.
            Seu trabalho e transcrever.
            """;

    private final PropriedadesIa propriedades;
    private final AnthropicClient cliente;

    public LeitorDeBilheteClaude(PropriedadesIa propriedades) {
        this.propriedades = propriedades;
        // Sem chave o bean existe mas nao conecta: a aplicacao sobe normalmente e o
        // erro so aparece - explicito - para quem chamar o endpoint.
        this.cliente = propriedades.configurada()
                ? AnthropicOkHttpClient.builder().apiKey(propriedades.chave()).build()
                : null;
    }

    @Override
    public BilheteLido ler(ImagemDeBilhete imagem, List<String> timesConhecidos) {
        if (cliente == null) {
            throw new IaNaoConfiguradaException(
                    "ANTHROPIC_API_KEY nao configurada; a leitura de bilhete esta indisponivel");
        }

        StructuredOutputConfig<RespostaExtraida> saida = StructuredOutputConfig.<RespostaExtraida>builder()
                .effort(esforco())
                .format(RespostaExtraida.class, JsonSchemaLocalValidation.YES)
                .build();

        // thinking fica omitido: no Opus 5 isso ja roda em modo adaptativo, e ler print
        // torto de celular e exatamente o caso em que pensar antes ajuda.
        var parametros = MessageCreateParams.builder()
                .model(propriedades.modelo())
                .maxTokens(propriedades.maxTokens())
                .system(INSTRUCAO_SISTEMA)
                .addUserMessageOfBlockParams(List.of(
                        ContentBlockParam.ofImage(bloco(imagem)),
                        ContentBlockParam.ofText(instrucaoDoUsuario(timesConhecidos))))
                .outputConfig(saida)
                .build();

        StructuredMessage<RespostaExtraida> resposta;
        try {
            resposta = cliente.messages().create(parametros);
        } catch (AnthropicServiceException e) {
            throw new LeituraDeBilheteException("a API do Claude recusou a requisicao: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new LeituraDeBilheteException("falha ao chamar a API do Claude", e);
        }

        verificarRecusa(resposta);
        log.info("bilhete lido: {} tokens de entrada, {} de saida",
                resposta.usage().inputTokens(), resposta.usage().outputTokens());
        return converter(extrairConteudo(resposta));
    }

    private ImageBlockParam bloco(ImagemDeBilhete imagem) {
        return ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .data(Base64.getEncoder().encodeToString(imagem.conteudo()))
                        .mediaType(Base64ImageSource.MediaType.of(imagem.mediaType()))
                        .build())
                .build();
    }

    private String instrucaoDoUsuario(List<String> timesConhecidos) {
        StringBuilder texto = new StringBuilder("Extraia as selecoes deste bilhete.");
        if (timesConhecidos != null && !timesConhecidos.isEmpty()) {
            texto.append("""

                    Times cadastrados no sistema. Quando reconhecer um deles no bilhete, use
                    exatamente esta grafia. Se o time do bilhete nao estiver nesta lista, escreva
                    o nome como aparece na imagem - nao force um nome da lista:
                    """);
            timesConhecidos.stream()
                    .limit(MAXIMO_DE_TIMES_NO_PROMPT)
                    .forEach(nome -> texto.append("- ").append(nome).append('\n'));
        }
        return texto.toString();
    }

    private void verificarRecusa(StructuredMessage<RespostaExtraida> resposta) {
        if (resposta.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            String motivo = resposta.stopDetails()
                    .flatMap(detalhes -> detalhes.explanation().or(() -> Optional.of("sem detalhe")))
                    .orElse("sem detalhe");
            throw new LeituraDeBilheteException("o modelo recusou processar esta imagem: " + motivo);
        }
    }

    private RespostaExtraida extrairConteudo(StructuredMessage<RespostaExtraida> resposta) {
        return resposta.content().stream()
                .flatMap(bloco -> bloco.text().stream())
                .map(texto -> texto.text())
                .findFirst()
                .orElseThrow(() -> new LeituraDeBilheteException("a resposta do modelo veio sem conteudo"));
    }

    private BilheteLido converter(RespostaExtraida extraida) {
        List<SelecaoLida> selecoes = new ArrayList<>();
        if (extraida.selecoes != null) {
            for (RespostaExtraida.SelecaoExtraida bruta : extraida.selecoes) {
                if (bruta == null) {
                    continue;
                }
                Mercado mercado = converterMercado(bruta.mercado);
                Selecao selecao = converterSelecao(mercado, bruta.selecao);
                selecoes.add(new SelecaoLida(
                        bruta.textoOriginal == null ? "" : bruta.textoOriginal.trim(),
                        limpar(bruta.mandante),
                        limpar(bruta.visitante),
                        selecao == null ? null : mercado,
                        selecao,
                        converterOdd(bruta.odd)));
            }
        }
        return new BilheteLido(selecoes, limpar(extraida.observacao));
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static Mercado converterMercado(String bruto) {
        String limpo = limpar(bruto);
        if (limpo == null) {
            return null;
        }
        for (Mercado mercado : Mercado.values()) {
            if (mercado.name().equalsIgnoreCase(limpo)) {
                return mercado;
            }
        }
        // "OUTRO" e qualquer coisa fora dos tres mercados caem aqui, e isso e esperado:
        // bilhete real mistura escanteios e handicap com o que sabemos precificar.
        return null;
    }

    /** Descarta o par inteiro se a selecao nao pertencer ao mercado: meio par nao serve. */
    private static Selecao converterSelecao(Mercado mercado, String bruto) {
        String limpo = limpar(bruto);
        if (mercado == null || limpo == null) {
            return null;
        }
        return mercado.selecoes().stream()
                .filter(selecao -> selecao.name().equalsIgnoreCase(limpo))
                .findFirst()
                .orElse(null);
    }

    /** Aceita virgula decimal, que e como bilhete brasileiro escreve. */
    private static BigDecimal converterOdd(String bruto) {
        String limpo = limpar(bruto);
        if (limpo == null) {
            return null;
        }
        String numero = limpo.toLowerCase(Locale.ROOT)
                .replace(",", ".")
                .replaceAll("[^0-9.]", "");
        if (numero.isEmpty()) {
            return null;
        }
        try {
            BigDecimal valor = new BigDecimal(numero);
            // Odd decimal <= 1 nao paga nada; tratamos como leitura falhada.
            return valor.compareTo(BigDecimal.ONE) > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OutputConfig.Effort esforco() {
        return OutputConfig.Effort.of(propriedades.esforco().toLowerCase(Locale.ROOT));
    }
}
