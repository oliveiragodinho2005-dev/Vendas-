package br.com.seuprojeto.servico;

import br.com.seuprojeto.dominio.ApelidoDeTime;
import br.com.seuprojeto.dominio.NomeDeTime;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.repositorio.ApelidoDeTimeRepository;
import br.com.seuprojeto.repositorio.TimeRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casa nomes soltos de time com o cadastro.
 *
 * <p>Carrega o elenco inteiro em memoria e resolve em Java: uma liga tem dezenas de
 * clubes, nao milhares, entao uma consulta por nome nao se justifica.
 *
 * <p>Regra central: quando mais de um time bate com o mesmo texto, devolvemos
 * {@code AMBIGUO} em vez de escolher. Um vinculo errado nao aparece como erro - ele
 * vira um EV calculado contra o jogo errado.
 */
@Service
public class ResolvedorDeTimes {

    private final TimeRepository times;
    private final ApelidoDeTimeRepository apelidos;

    public ResolvedorDeTimes(TimeRepository times, ApelidoDeTimeRepository apelidos) {
        this.times = times;
        this.apelidos = apelidos;
    }

    /** Nomes cadastrados, oferecidos ao modelo como vocabulario de grafia. */
    @Transactional(readOnly = true)
    public List<String> nomesCadastrados() {
        return times.findAll().stream().map(Time::getNome).sorted().toList();
    }

    /**
     * Resolve varios nomes de uma vez, com uma unica leitura do cadastro.
     *
     * @return mapa do texto original para o desfecho; nomes em branco sao ignorados
     */
    @Transactional(readOnly = true)
    public Map<String, ResolucaoDeTime> resolver(Collection<String> nomesLidos) {
        Map<String, ResolucaoDeTime> resultado = new LinkedHashMap<>();
        if (nomesLidos == null || nomesLidos.isEmpty()) {
            return resultado;
        }

        Indice indice = carregarIndice();
        for (String nomeLido : nomesLidos) {
            if (nomeLido == null || nomeLido.isBlank() || resultado.containsKey(nomeLido)) {
                continue;
            }
            resultado.put(nomeLido, indice.casar(nomeLido));
        }
        return resultado;
    }

    private Indice carregarIndice() {
        Map<String, Set<Time>> porChave = new HashMap<>();
        for (Time time : times.findAll()) {
            registrar(porChave, NomeDeTime.chave(time.getNome()), time);
            registrar(porChave, NomeDeTime.normalizar(time.getNome()), time);
            registrar(porChave, NomeDeTime.chave(time.getSigla()), time);
        }
        // Apelido e mapeamento curado: tem prioridade e nao entra no bolo ambiguo.
        Map<String, Time> porApelido = new HashMap<>();
        for (ApelidoDeTime apelido : apelidos.findAll()) {
            porApelido.put(apelido.getApelidoNormalizado(), apelido.getTime());
        }
        return new Indice(porChave, porApelido);
    }

    private static void registrar(Map<String, Set<Time>> indice, String chave, Time time) {
        if (chave != null && !chave.isEmpty()) {
            indice.computeIfAbsent(chave, ignorada -> new java.util.LinkedHashSet<>()).add(time);
        }
    }

    private record Indice(Map<String, Set<Time>> porChave, Map<String, Time> porApelido) {

        ResolucaoDeTime casar(String nomeLido) {
            String chave = NomeDeTime.chave(nomeLido);
            if (chave.isEmpty()) {
                return ResolucaoDeTime.naoCadastrado(nomeLido);
            }

            Time porApelidoCurado = porApelido.get(chave);
            if (porApelidoCurado != null) {
                return ResolucaoDeTime.resolvido(nomeLido, porApelidoCurado);
            }

            Set<Time> candidatos = porChave.get(chave);
            if (candidatos == null) {
                candidatos = porChave.get(NomeDeTime.normalizar(nomeLido));
            }
            if (candidatos == null || candidatos.isEmpty()) {
                return ResolucaoDeTime.naoCadastrado(nomeLido);
            }
            if (candidatos.size() > 1) {
                return ResolucaoDeTime.ambiguo(nomeLido);
            }
            return ResolucaoDeTime.resolvido(nomeLido, candidatos.iterator().next());
        }
    }
}
