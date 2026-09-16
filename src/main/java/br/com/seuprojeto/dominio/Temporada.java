package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Objects;

/** Edicao anual de uma competicao. Cada temporada agrupa as partidas daquele ano. */
@Entity
@Table(name = "temporadas")
public class Temporada {

    public static final String BRASILEIRAO_SERIE_A = "BRASILEIRAO_SERIE_A";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "competicao", nullable = false, length = 60)
    private String competicao;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    /** Identificador na fonte de dados externa, usado para ingestao idempotente. */
    @Column(name = "id_externo", length = 60)
    private String idExterno;

    protected Temporada() {
        // exigido pelo JPA
    }

    public Temporada(String competicao, int ano) {
        this.competicao = Objects.requireNonNull(competicao, "competicao");
        this.ano = ano;
    }

    public static Temporada brasileiraoSerieA(int ano) {
        return new Temporada(BRASILEIRAO_SERIE_A, ano);
    }

    public void definirPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim anterior a dataInicio");
        }
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public void definirIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public Long getId() {
        return id;
    }

    public String getCompeticao() {
        return competicao;
    }

    public int getAno() {
        return ano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public String getIdExterno() {
        return idExterno;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Temporada temporada && id != null && id.equals(temporada.id);
    }

    @Override
    public int hashCode() {
        return Temporada.class.hashCode();
    }

    @Override
    public String toString() {
        return competicao + " " + ano;
    }
}
