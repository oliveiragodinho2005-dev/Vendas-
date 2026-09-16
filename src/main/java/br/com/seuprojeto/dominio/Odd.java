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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Cotacao decimal observada para uma selecao, em um instante.
 *
 * <p>Cada coleta gera uma linha nova: nada aqui e sobrescrito. O historico completo
 * e o que permite medir movimento de linha e CLV contra a odd de fechamento.
 */
@Entity
@Table(name = "odds")
public class Odd {

    /** Menor cotacao decimal com sentido: abaixo disso nao existe retorno. */
    private static final BigDecimal VALOR_MINIMO = BigDecimal.ONE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "casa_de_aposta_id", nullable = false)
    private CasaDeAposta casa;

    @Enumerated(EnumType.STRING)
    @Column(name = "mercado", nullable = false, length = 30)
    private Mercado mercado;

    @Enumerated(EnumType.STRING)
    @Column(name = "selecao", nullable = false, length = 20)
    private Selecao selecao;

    /**
     * Linha do mercado quando ele tem handicap, por exemplo 2.5 em over/under.
     * Nula nos mercados sem linha, como 1X2 e ambas marcam.
     */
    @Column(name = "linha", precision = 5, scale = 2)
    private BigDecimal linha;

    @Column(name = "valor", nullable = false, precision = 7, scale = 3)
    private BigDecimal valor;

    /** Sempre em UTC, e parte da identidade da coleta. */
    @Column(name = "coletada_em", nullable = false)
    private OffsetDateTime coletadaEm;

    /** API agregadora que forneceu a cotacao, para rastrear divergencias entre fontes. */
    @Column(name = "fonte", length = 60)
    private String fonte;

    protected Odd() {
        // exigido pelo JPA
    }

    public Odd(Partida partida,
               CasaDeAposta casa,
               Mercado mercado,
               Selecao selecao,
               BigDecimal valor,
               OffsetDateTime coletadaEm) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.casa = Objects.requireNonNull(casa, "casa");
        this.mercado = Objects.requireNonNull(mercado, "mercado");
        this.selecao = Objects.requireNonNull(selecao, "selecao");
        this.valor = Objects.requireNonNull(valor, "valor");
        this.coletadaEm = Objects.requireNonNull(coletadaEm, "coletadaEm");
        if (!mercado.aceita(selecao)) {
            throw new IllegalArgumentException(
                    "selecao " + selecao + " nao pertence ao mercado " + mercado);
        }
        if (valor.compareTo(VALOR_MINIMO) <= 0) {
            throw new IllegalArgumentException("odd decimal deve ser maior que 1, recebido: " + valor);
        }
    }

    public void definirLinha(BigDecimal linha) {
        this.linha = linha;
    }

    public void definirFonte(String fonte) {
        this.fonte = fonte;
    }

    public Long getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public CasaDeAposta getCasa() {
        return casa;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public Selecao getSelecao() {
        return selecao;
    }

    public BigDecimal getLinha() {
        return linha;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public OffsetDateTime getColetadaEm() {
        return coletadaEm;
    }

    public String getFonte() {
        return fonte;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Odd odd && id != null && id.equals(odd.id);
    }

    @Override
    public int hashCode() {
        return Odd.class.hashCode();
    }

    @Override
    public String toString() {
        return mercado + "/" + selecao + " @ " + valor;
    }
}
