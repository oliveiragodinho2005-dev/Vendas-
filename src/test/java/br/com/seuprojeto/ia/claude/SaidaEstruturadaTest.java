package br.com.seuprojeto.ia.claude;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredOutputConfig;
import org.junit.jupiter.api.Test;

/**
 * Prova que o JSON Schema sai de {@link RespostaExtraida} sem erro.
 *
 * <p>Vale mais do que parece: a geracao do schema acontece localmente, sem rede e sem
 * chave, entao um campo mal declarado na classe de resposta quebra aqui em vez de
 * quebrar na primeira chamada real - que custa dinheiro e so acontece em producao.
 */
class SaidaEstruturadaTest {

    @Test
    void schemaESaidaTipadaSaemDaClasseDeResposta() {
        StructuredOutputConfig<RespostaExtraida> saida = StructuredOutputConfig.<RespostaExtraida>builder()
                .effort(OutputConfig.Effort.HIGH)
                .format(RespostaExtraida.class, JsonSchemaLocalValidation.YES)
                .build();

        assertThat(saida.outputType()).isEqualTo(RespostaExtraida.class);
        assertThat(saida.rawOutputConfig().effort()).hasValue(OutputConfig.Effort.HIGH);
        assertThat(saida.rawOutputConfig().format()).isPresent();
    }

    @Test
    void schemaDescreveTodosOsCamposQueOModeloPrecisaPreencher() {
        StructuredOutputConfig<RespostaExtraida> saida = StructuredOutputConfig.<RespostaExtraida>builder()
                .format(RespostaExtraida.class, JsonSchemaLocalValidation.YES)
                .build();

        String schema = saida.rawOutputConfig().format().orElseThrow().toString();

        assertThat(schema)
                .contains("selecoes")
                .contains("textoOriginal")
                .contains("mandante")
                .contains("visitante")
                .contains("mercado")
                .contains("selecao")
                .contains("odd")
                .contains("observacao");
    }
}
