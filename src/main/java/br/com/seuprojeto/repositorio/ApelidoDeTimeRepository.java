package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.ApelidoDeTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApelidoDeTimeRepository extends JpaRepository<ApelidoDeTime, Long> {

    Optional<ApelidoDeTime> findByApelidoNormalizado(String apelidoNormalizado);
}
