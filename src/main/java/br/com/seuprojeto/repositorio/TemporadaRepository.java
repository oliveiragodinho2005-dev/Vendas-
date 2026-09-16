package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.Temporada;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    Optional<Temporada> findByCompeticaoAndAno(String competicao, int ano);

    Optional<Temporada> findByIdExterno(String idExterno);
}
