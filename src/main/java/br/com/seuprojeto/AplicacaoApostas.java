package br.com.seuprojeto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AplicacaoApostas {

    public static void main(String[] args) {
        SpringApplication.run(AplicacaoApostas.class, args);
    }
}
