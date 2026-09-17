package br.com.seuprojeto.servico;

import br.com.seuprojeto.dominio.Time;

/**
 * Desfecho da tentativa de casar um nome lido no bilhete com um time cadastrado.
 *
 * @param nomeLido  texto como veio da imagem
 * @param time      time casado, nulo quando nao houve casamento unico
 * @param situacao  por que resolveu ou por que nao
 */
public record ResolucaoDeTime(String nomeLido, Time time, Situacao situacao) {

    public enum Situacao {
        /** Casamento unico e confiavel. */
        RESOLVIDO,
        /** Nenhum time cadastrado bate com esse nome. */
        NAO_CADASTRADO,
        /** Mais de um time bate: resolver seria chute, entao nao resolvemos. */
        AMBIGUO
    }

    public static ResolucaoDeTime resolvido(String nomeLido, Time time) {
        return new ResolucaoDeTime(nomeLido, time, Situacao.RESOLVIDO);
    }

    public static ResolucaoDeTime naoCadastrado(String nomeLido) {
        return new ResolucaoDeTime(nomeLido, null, Situacao.NAO_CADASTRADO);
    }

    public static ResolucaoDeTime ambiguo(String nomeLido) {
        return new ResolucaoDeTime(nomeLido, null, Situacao.AMBIGUO);
    }

    public boolean resolvido() {
        return situacao == Situacao.RESOLVIDO;
    }
}
