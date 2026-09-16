package br.com.seuprojeto.dominio;

/** Ciclo de vida de uma partida, alimentado pela ingestao de resultados. */
public enum StatusPartida {

    AGENDADA,
    EM_ANDAMENTO,
    ENCERRADA,
    ADIADA,
    CANCELADA;

    /** Somente partidas encerradas tem placar definitivo e podem alimentar o modelo. */
    public boolean temPlacarDefinitivo() {
        return this == ENCERRADA;
    }
}
