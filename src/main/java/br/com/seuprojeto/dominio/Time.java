package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

/** Clube participante. Persistido uma unica vez e reaproveitado entre temporadas. */
@Entity
@Table(name = "times")
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    /** Abreviacao de 3 letras usada nas telas compactas, por exemplo FLA. */
    @Column(name = "sigla", length = 5)
    private String sigla;

    @Column(name = "escudo_url", length = 300)
    private String escudoUrl;

    /** Identificador na fonte de dados externa, usado para ingestao idempotente. */
    @Column(name = "id_externo", length = 60)
    private String idExterno;

    protected Time() {
        // exigido pelo JPA
    }

    public Time(String nome) {
        this.nome = Objects.requireNonNull(nome, "nome");
    }

    public void definirSigla(String sigla) {
        this.sigla = sigla;
    }

    public void definirEscudoUrl(String escudoUrl) {
        this.escudoUrl = escudoUrl;
    }

    public void definirIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }

    public String getEscudoUrl() {
        return escudoUrl;
    }

    public String getIdExterno() {
        return idExterno;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Time time && id != null && id.equals(time.id);
    }

    @Override
    public int hashCode() {
        return Time.class.hashCode();
    }

    @Override
    public String toString() {
        return nome;
    }
}
