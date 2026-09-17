package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.repositorio.ConsumoDeApiRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Franquia diaria de requisicoes por fonte externa.
 *
 * <p>O contador vive no banco, e nao em memoria, por dois motivos: reiniciar a
 * aplicacao nao pode zerar o gasto do dia, e a reserva precisa ser atomica para que
 * dois jobs concorrentes nao passem do teto cada um achando que ha espaco.
 *
 * <p>O dia e em UTC, que e como as APIs costumam virar a contagem.
 */
@Service
public class ControleDeCota {

    private static final Logger log = LoggerFactory.getLogger(ControleDeCota.class);

    private final ConsumoDeApiRepository consumos;
    private final Clock relogio;

    public ControleDeCota(ConsumoDeApiRepository consumos, Clock relogio) {
        this.consumos = consumos;
        this.relogio = relogio;
    }

    /**
     * Debita {@code quantidade} requisicoes da franquia de hoje.
     *
     * @return true se coube no teto; false se a reserva foi negada e nada foi debitado
     */
    @Transactional
    public boolean tentarReservar(String fonte, int quantidade, int teto) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva, recebido: " + quantidade);
        }
        LocalDate hoje = hoje();
        OffsetDateTime agora = OffsetDateTime.now(relogio).withOffsetSameInstant(ZoneOffset.UTC);

        consumos.garantirLinhaDoDia(fonte, hoje, agora);
        boolean reservado = consumos.reservar(fonte, hoje, quantidade, teto, agora) == 1;

        if (!reservado) {
            log.warn("cota diaria de {} esgotada: {} de {} requisicoes ja usadas, pedido de {} negado",
                    fonte, consumoDeHoje(fonte), teto, quantidade);
        }
        return reservado;
    }

    /** Igual a {@link #tentarReservar}, mas estoura em vez de devolver false. */
    @Transactional
    public void reservar(String fonte, int quantidade, int teto) {
        if (!tentarReservar(fonte, quantidade, teto)) {
            throw new CotaEsgotadaException(
                    "cota diaria de %s esgotada; %d requisicao(oes) negada(s)".formatted(fonte, quantidade));
        }
    }

    @Transactional(readOnly = true)
    public int consumoDeHoje(String fonte) {
        return consumos.findByFonteAndDia(fonte, hoje())
                .map(consumo -> consumo.getRequisicoes())
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int restanteHoje(String fonte, int teto) {
        return Math.max(0, teto - consumoDeHoje(fonte));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio.withZone(ZoneOffset.UTC));
    }
}
