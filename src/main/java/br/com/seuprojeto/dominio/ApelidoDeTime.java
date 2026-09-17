package br.com.seuprojeto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Como um time tambem e chamado: "Galo", "Fla", "Verdao", ou a grafia de uma fonte
 * externa que a normalizacao sozinha nao alcanca.
 *
 * <p>Alimentado conforme aparecem nomes nao resolvidos, em vez de uma lista chutada
 * na migration - quem esta na Serie A muda a cada temporada.
 */
@Entity
@Table(name = "apelidos_de_time")
public class ApelidoDeTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private Time time;

    @Column(name = "apelido", nullable = false, length = 120)
    private String apelido;

    /** {@link NomeDeTime#chave(String)} do apelido, materializado para busca direta. */
    @Column(name = "apelido_normalizado", nullable = false, length = 120)
    private String apelidoNormalizado;

    protected ApelidoDeTime() {
        // exigido pelo JPA
    }

    public ApelidoDeTime(Time time, String apelido) {
        this.time = Objects.requireNonNull(time, "time");
        this.apelido = Objects.requireNonNull(apelido, "apelido");
        this.apelidoNormalizado = NomeDeTime.chave(apelido);
        if (this.apelidoNormalizado.isEmpty()) {
            throw new IllegalArgumentException("apelido sem conteudo util: " + apelido);
        }
    }

    public Long getId() {
        return id;
    }

    public Time getTime() {
        return time;
    }

    public String getApelido() {
        return apelido;
    }

    public String getApelidoNormalizado() {
        return apelidoNormalizado;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof ApelidoDeTime apelidoDeTime && id != null && id.equals(apelidoDeTime.id);
    }

    @Override
    public int hashCode() {
        return ApelidoDeTime.class.hashCode();
    }

    @Override
    public String toString() {
        return apelido;
    }
}
