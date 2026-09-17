package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.ConsumoDeApi;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumoDeApiRepository extends JpaRepository<ConsumoDeApi, Long> {

    Optional<ConsumoDeApi> findByFonteAndDia(String fonte, LocalDate dia);

    /** Cria a linha do dia se ainda nao existir. Idempotente e livre de corrida. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO consumo_de_api (fonte, dia, requisicoes, atualizado_em)
            VALUES (:fonte, :dia, 0, :agora)
            ON CONFLICT (fonte, dia) DO NOTHING
            """, nativeQuery = true)
    void garantirLinhaDoDia(@Param("fonte") String fonte,
                            @Param("dia") LocalDate dia,
                            @Param("agora") OffsetDateTime agora);

    /**
     * Debita a franquia em uma unica instrucao condicional.
     *
     * <p>O teto entra no WHERE de proposito: ler o contador e depois gravar abriria
     * janela para dois jobs concorrentes passarem do limite. Aqui o banco decide,
     * sob lock da linha, e devolve 0 quando a reserva nao cabe.
     *
     * @return 1 se a reserva foi concedida, 0 se estouraria o teto
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE consumo_de_api
               SET requisicoes = requisicoes + :quantidade,
                   atualizado_em = :agora
             WHERE fonte = :fonte
               AND dia = :dia
               AND requisicoes + :quantidade <= :teto
            """, nativeQuery = true)
    int reservar(@Param("fonte") String fonte,
                 @Param("dia") LocalDate dia,
                 @Param("quantidade") int quantidade,
                 @Param("teto") int teto,
                 @Param("agora") OffsetDateTime agora);
}
