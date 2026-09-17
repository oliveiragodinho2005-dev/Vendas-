package br.com.seuprojeto.ingestao;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ConfiguracaoDeIngestao {

    /**
     * Relogio injetado em vez de {@code now()} espalhado pelo codigo: o planejador
     * decide por comparacao de datas, e sem controlar o tempo nao da para testar isso.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock relogio() {
        return Clock.systemUTC();
    }

    /**
     * Assim que a Fase 2B trouxer o adaptador real anotado como bean, ele assume o
     * lugar destes sem precisar mexer aqui.
     */
    @Bean
    @ConditionalOnMissingBean(FonteDeDados.class)
    public FonteDeDados fonteDeDadosAusente() {
        return new FonteDeDadosAusente();
    }

    @Bean
    @ConditionalOnMissingBean(FonteDeOdds.class)
    public FonteDeOdds fonteDeOddsAusente() {
        return new FonteDeOddsAusente();
    }
}
