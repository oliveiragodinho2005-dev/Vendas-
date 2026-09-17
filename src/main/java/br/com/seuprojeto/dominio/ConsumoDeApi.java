package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Quantas requisicoes uma fonte externa ja consumiu em um dia. */
@Entity
@Table(name = "consumo_de_api")
public class ConsumoDeApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fonte", nullable = false, length = 60)
    private String fonte;

    @Column(name = "dia", nullable = false)
    private LocalDate dia;

    @Column(name = "requisicoes", nullable = false)
    private int requisicoes;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected ConsumoDeApi() {
        // exigido pelo JPA
    }

    public Long getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
    }

    public LocalDate getDia() {
        return dia;
    }

    public int getRequisicoes() {
        return requisicoes;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
