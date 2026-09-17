package br.com.seuprojeto.ingestao;

import br.com.seuprojeto.dominio.NomeDeTime;
import br.com.seuprojeto.dominio.Time;
import br.com.seuprojeto.repositorio.TimeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava os times da fonte externa sem duplicar.
 *
 * <p>Tres caminhos, nesta ordem: casar pelo {@code idExterno}, adotar um time que ja
 * existe no banco sem id externo, ou criar. A adocao e o que evita duplicata quando o
 * time entrou antes por outro caminho - cadastrado a mao, ou nascido da leitura de um
 * bilhete. Sem ela, a primeira ingestao criaria um "Flamengo" ao lado do outro.
 *
 * <p>Quando o nome bate com mais de um time, a sincronizacao ignora e avisa. O mesmo
 * principio da resolucao de bilhete vale aqui: fundir os times errados e irreversivel.
 */
@Service
public class SincronizadorDeTimes {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorDeTimes.class);

    private final TimeRepository times;

    public SincronizadorDeTimes(TimeRepository times) {
        this.times = times;
    }

    @Transactional
    public ResultadoDeSincronizacao sincronizar(List<TimeExterno> externos) {
        ResultadoDeSincronizacao.Acumulador acumulador = ResultadoDeSincronizacao.acumulador();

        List<Time> cadastrados = new ArrayList<>(times.findAll());
        Map<String, Time> porIdExterno = new HashMap<>();
        Map<String, List<Time>> porChave = new HashMap<>();
        cadastrados.forEach(time -> indexar(time, porIdExterno, porChave));

        for (TimeExterno externo : externos) {
            Time existente = porIdExterno.get(externo.idExterno());
            if (existente != null) {
                atualizar(existente, externo);
                acumulador.atualizado();
                continue;
            }

            List<Time> candidatos = porChave.getOrDefault(NomeDeTime.chave(externo.nome()), List.of());
            if (candidatos.size() > 1) {
                acumulador.ignorado("nome \"%s\" bate com %d times cadastrados; nao foi adotado"
                        .formatted(externo.nome(), candidatos.size()));
                continue;
            }
            if (candidatos.size() == 1) {
                Time candidato = candidatos.get(0);
                if (candidato.getIdExterno() != null) {
                    acumulador.ignorado("\"%s\" ja esta vinculado ao id externo %s; %s foi descartado"
                            .formatted(candidato.getNome(), candidato.getIdExterno(), externo.idExterno()));
                    continue;
                }
                candidato.definirIdExterno(externo.idExterno());
                atualizar(candidato, externo);
                porIdExterno.put(externo.idExterno(), candidato);
                acumulador.atualizado();
                log.info("time \"{}\" adotado pelo id externo {}", candidato.getNome(), externo.idExterno());
                continue;
            }

            Time novo = new Time(externo.nome());
            novo.definirIdExterno(externo.idExterno());
            atualizar(novo, externo);
            times.save(novo);
            indexar(novo, porIdExterno, porChave);
            acumulador.criado();
        }

        times.flush();
        ResultadoDeSincronizacao resultado = acumulador.concluir();
        log.info("times sincronizados: {}", resultado);
        return resultado;
    }

    private static void atualizar(Time time, TimeExterno externo) {
        if (externo.sigla() != null && !externo.sigla().isBlank()) {
            time.definirSigla(externo.sigla());
        }
        if (externo.escudoUrl() != null && !externo.escudoUrl().isBlank()) {
            time.definirEscudoUrl(externo.escudoUrl());
        }
    }

    private static void indexar(Time time, Map<String, Time> porIdExterno, Map<String, List<Time>> porChave) {
        if (time.getIdExterno() != null) {
            porIdExterno.put(time.getIdExterno(), time);
        }
        String chave = NomeDeTime.chave(time.getNome());
        if (!chave.isEmpty()) {
            porChave.computeIfAbsent(chave, ignorada -> new ArrayList<>()).add(time);
        }
    }
}
