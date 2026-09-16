package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    Optional<Partida> findByIdExterno(String idExterno);

    List<Partida> findByTemporadaAndRodadaOrderByDataHoraAsc(Temporada temporada, int rodada);

    List<Partida> findByTemporadaAndStatusOrderByDataHoraAsc(Temporada temporada, StatusPartida status);

    /** Base historica do modelo: somente jogos ja encerrados antes de um instante. */
    List<Partida> findByStatusAndDataHoraBeforeOrderByDataHoraAsc(StatusPartida status, OffsetDateTime limite);
}
