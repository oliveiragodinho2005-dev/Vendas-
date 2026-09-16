package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.Time;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<Time, Long> {

    Optional<Time> findByIdExterno(String idExterno);

    Optional<Time> findByNome(String nome);
}
