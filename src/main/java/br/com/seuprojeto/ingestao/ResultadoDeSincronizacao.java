package br.com.seuprojeto.ingestao;

import java.util.ArrayList;
import java.util.List;

/**
 * O que uma sincronizacao fez. Os avisos importam tanto quanto os numeros: e neles
 * que aparece o registro que a ingestao decidiu nao tocar, e por que.
 */
public record ResultadoDeSincronizacao(int criados, int atualizados, int ignorados, List<String> avisos) {

    public ResultadoDeSincronizacao {
        avisos = List.copyOf(avisos);
    }

    public static Acumulador acumulador() {
        return new Acumulador();
    }

    public int total() {
        return criados + atualizados + ignorados;
    }

    @Override
    public String toString() {
        return "%d criado(s), %d atualizado(s), %d ignorado(s)".formatted(criados, atualizados, ignorados);
    }

    /** Coletor mutavel usado durante o laco de sincronizacao. */
    public static final class Acumulador {

        private int criados;
        private int atualizados;
        private int ignorados;
        private final List<String> avisos = new ArrayList<>();

        public void criado() {
            criados++;
        }

        public void atualizado() {
            atualizados++;
        }

        public void ignorado(String aviso) {
            ignorados++;
            avisos.add(aviso);
        }

        /** Aviso sobre um registro que ainda assim foi gravado; nao conta como ignorado. */
        public void avisoSemContar(String aviso) {
            avisos.add(aviso);
        }

        public ResultadoDeSincronizacao concluir() {
            return new ResultadoDeSincronizacao(criados, atualizados, ignorados, avisos);
        }
    }
}
