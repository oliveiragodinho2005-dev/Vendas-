package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.StatusPartida;
import br.com.seuprojeto.dominio.Temporada;
import br.com.seuprojeto.dominio.Time;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    Optional<Partida> findByIdExterno(String idExterno);

    List<Partida> findByTemporadaAndRodadaOrderByDataHoraAsc(Temporada temporada, int rodada);

    List<Partida> findByTemporadaAndStatusOrderByDataHoraAsc(Temporada temporada, StatusPartida status);

    List<Partida> findByTemporadaOrderByDataHoraAsc(Temporada temporada);

    long countByTemporada(Temporada temporada);

    /** Jogos que a fonte ja deveria ter encerrado: o alvo da atualizacao de resultados. */
    List<Partida> findByTemporadaAndStatusInAndDataHoraBeforeOrderByDataHoraAsc(
            Temporada temporada, Collection<StatusPartida> status, OffsetDateTime limite);

    /** Jogos que comecam dentro de uma janela: o alvo da coleta de odds. */
    List<Partida> findByStatusAndDataHoraBetweenOrderByDataHoraAsc(
            StatusPartida status, OffsetDateTime inicio, OffsetDateTime fim);

    /** Confrontos entre dois times, do mais antigo ao mais recente. */
    List<Partida> findByMandanteAndVisitanteOrderByDataHoraAsc(Time mandante, Time visitante);

    /** Base historica do modelo: somente jogos ja encerrados antes de um instante. */
    List<Partida> findByStatusAndDataHoraBeforeOrderByDataHoraAsc(StatusPartida status, OffsetDateTime limite);
}
