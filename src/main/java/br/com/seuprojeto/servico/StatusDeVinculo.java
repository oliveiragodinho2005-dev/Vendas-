package br.com.seuprojeto.servico;

/** Ate onde foi possivel levar uma selecao lida, da imagem ate uma partida do banco. */
public enum StatusDeVinculo {

    /** Times e partida identificados: pronta para receber EV quando a Fase 4 existir. */
    VINCULADA,
    /** Mercado fora dos tres que o sistema precifica (handicap, escanteios, etc.). */
    MERCADO_NAO_SUPORTADO,
    /** O modelo nao conseguiu ler os times dessa linha. */
    TIME_NAO_IDENTIFICADO,
    /** Times lidos, mas algum nao existe no cadastro. */
    TIME_NAO_CADASTRADO,
    /** O nome lido bate com mais de um time; vincular seria chute. */
    TIME_AMBIGUO,
    /** Times resolvidos, mas nao ha esse confronto cadastrado. */
    PARTIDA_NAO_ENCONTRADA
}
