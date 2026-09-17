package br.com.seuprojeto.ia.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracao da leitura por IA. A chave vem sempre do ambiente
 * ({@code ANTHROPIC_API_KEY}) - nunca fica em arquivo versionado.
 */
@ConfigurationProperties(prefix = "apostas.ia")
public record PropriedadesIa(@DefaultValue("") String chave,
                             @DefaultValue("claude-opus-5") String modelo,
                             @DefaultValue("high") String esforco,
                             @DefaultValue("16000") long maxTokens,
                             @DefaultValue("5") int limiteImagemMb) {

    public boolean configurada() {
        return chave != null && !chave.isBlank();
    }

    public long limiteImagemEmBytes() {
        return limiteImagemMb * 1024L * 1024L;
    }
}
