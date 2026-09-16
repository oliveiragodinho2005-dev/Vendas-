package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Casa de apostas cujas odds sao coletadas via API agregadora.
 * Nenhum dado aqui vem de scraping de site de apostas.
 */
@Entity
@Table(name = "casas_de_aposta")
public class CasaDeAposta {

    public static final String BET365 = "bet365";
    public static final String PINNACLE = "pinnacle";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    /** Chave estavel usada pelas APIs agregadoras, por exemplo bet365. */
    @NotBlank
    @Column(name = "chave", nullable = false, length = 60)
    private String chave;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    /**
     * Casas de baixa margem servem de referencia de mercado para aferir o modelo.
     * A Pinnacle entra nesse papel; a bet365 e o alvo das apostas.
     */
    @Column(name = "referencia_de_mercado", nullable = false)
    private boolean referenciaDeMercado = false;

    protected CasaDeAposta() {
        // exigido pelo JPA
    }

    public CasaDeAposta(String nome, String chave) {
        this.nome = Objects.requireNonNull(nome, "nome");
        this.chave = Objects.requireNonNull(chave, "chave");
    }

    public void definirAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public void definirReferenciaDeMercado(boolean referenciaDeMercado) {
        this.referenciaDeMercado = referenciaDeMercado;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getChave() {
        return chave;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public boolean isReferenciaDeMercado() {
        return referenciaDeMercado;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof CasaDeAposta casa && id != null && id.equals(casa.id);
    }

    @Override
    public int hashCode() {
        return CasaDeAposta.class.hashCode();
    }

    @Override
    public String toString() {
        return nome;
    }
}
