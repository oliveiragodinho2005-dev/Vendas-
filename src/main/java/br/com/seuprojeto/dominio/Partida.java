package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Jogo entre dois times em uma rodada. O placar so existe depois do encerramento,
 * por isso os gols sao nulos ate a partida terminar.
 *
 * <p>As consultas de resultado devolvem {@link Selecao}, a mesma linguagem usada
 * pelas odds, o que permite comparar previsao e realidade sem conversao extra.
 */
@Entity
@Table(name = "partidas")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @Column(name = "rodada", nullable = false)
    private int rodada;

    /** Sempre em UTC. A conversao para America/Sao_Paulo acontece na borda da API. */
    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandante_id", nullable = false)
    private Time mandante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitante_id", nullable = false)
    private Time visitante;

    @Column(name = "estadio", length = 160)
    private String estadio;

    @Column(name = "gols_mandante")
    private Integer golsMandante;

    @Column(name = "gols_visitante")
    private Integer golsVisitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPartida status = StatusPartida.AGENDADA;

    /** Identificador na fonte de dados externa, usado para ingestao idempotente. */
    @Column(name = "id_externo", length = 60)
    private String idExterno;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Partida() {
        // exigido pelo JPA
    }

    public Partida(Temporada temporada, int rodada, OffsetDateTime dataHora, Time mandante, Time visitante) {
        this.temporada = Objects.requireNonNull(temporada, "temporada");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora");
        this.mandante = Objects.requireNonNull(mandante, "mandante");
        this.visitante = Objects.requireNonNull(visitante, "visitante");
        if (rodada < 1) {
            throw new IllegalArgumentException("rodada deve ser positiva, recebido: " + rodada);
        }
        if (mandante == visitante) {
            throw new IllegalArgumentException("mandante e visitante nao podem ser o mesmo time");
        }
        this.rodada = rodada;
    }

    /** Registra o placar final e encerra a partida. */
    public void registrarPlacar(int golsMandante, int golsVisitante) {
        if (golsMandante < 0 || golsVisitante < 0) {
            throw new IllegalArgumentException("gols nao podem ser negativos");
        }
        this.golsMandante = golsMandante;
        this.golsVisitante = golsVisitante;
        this.status = StatusPartida.ENCERRADA;
    }

    public void alterarStatus(StatusPartida status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void definirEstadio(String estadio) {
        this.estadio = estadio;
    }

    public void definirIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public boolean encerrada() {
        return status.temPlacarDefinitivo() && golsMandante != null && golsVisitante != null;
    }

    public OptionalInt totalGols() {
        return encerrada() ? OptionalInt.of(golsMandante + golsVisitante) : OptionalInt.empty();
    }

    /**
     * Selecao vencedora do mercado informado, ou vazio enquanto a partida nao encerrar.
     * E a ponte entre o placar real e a avaliacao de acerto do modelo.
     */
    public Optional<Selecao> resultadoDe(Mercado mercado) {
        Objects.requireNonNull(mercado, "mercado");
        if (!encerrada()) {
            return Optional.empty();
        }
        return Optional.of(switch (mercado) {
            case RESULTADO_1X2 -> {
                int diferenca = golsMandante - golsVisitante;
                yield diferenca > 0 ? Selecao.CASA : diferenca < 0 ? Selecao.FORA : Selecao.EMPATE;
            }
            case OVER_UNDER_2_5 -> golsMandante + golsVisitante > 2 ? Selecao.OVER : Selecao.UNDER;
            case AMBAS_MARCAM -> golsMandante > 0 && golsVisitante > 0 ? Selecao.SIM : Selecao.NAO;
        });
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Temporada getTemporada() {
        return temporada;
    }

    public int getRodada() {
        return rodada;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public Time getMandante() {
        return mandante;
    }

    public Time getVisitante() {
        return visitante;
    }

    public String getEstadio() {
        return estadio;
    }

    public Integer getGolsMandante() {
        return golsMandante;
    }

    public Integer getGolsVisitante() {
        return golsVisitante;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Partida partida && id != null && id.equals(partida.id);
    }

    @Override
    public int hashCode() {
        return Partida.class.hashCode();
    }

    @Override
    public String toString() {
        return "rodada " + rodada + ": " + mandante + " x " + visitante;
    }
}
