package br.com.seuprojeto.repositorio;

import br.com.seuprojeto.dominio.CasaDeAposta;
import br.com.seuprojeto.dominio.Mercado;
import br.com.seuprojeto.dominio.Odd;
import br.com.seuprojeto.dominio.Partida;
import br.com.seuprojeto.dominio.Selecao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OddRepository extends JpaRepository<Odd, Long> {

    /** Historico completo de uma selecao, do mais antigo ao mais recente. */
    List<Odd> findByPartidaAndCasaAndMercadoAndSelecaoOrderByColetadaEmAsc(
            Partida partida, CasaDeAposta casa, Mercado mercado, Selecao selecao);

    /** Ultima cotacao conhecida de uma selecao. */
    Optional<Odd> findTopByPartidaAndCasaAndMercadoAndSelecaoOrderByColetadaEmDesc(
            Partida partida, CasaDeAposta casa, Mercado mercado, Selecao selecao);

    List<Odd> findByPartidaOrderByColetadaEmDesc(Partida partida);
}
