package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.CasaDeAposta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasaDeApostaRepository extends JpaRepository<CasaDeAposta, Long> {

    Optional<CasaDeAposta> findByChave(String chave);
}
