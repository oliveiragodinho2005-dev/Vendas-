-- Schema base da calculadora de valor esperado.
-- O Flyway e a unica fonte da verdade do schema; o Hibernate roda em ddl-auto=validate.

CREATE TABLE temporadas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    competicao  VARCHAR(60) NOT NULL,
    ano         INTEGER     NOT NULL,
    data_inicio DATE,
    data_fim    DATE,
    id_externo  VARCHAR(60),
    CONSTRAINT uq_temporadas_competicao_ano UNIQUE (competicao, ano),
    CONSTRAINT uq_temporadas_id_externo     UNIQUE (id_externo),
    CONSTRAINT ck_temporadas_ano            CHECK (ano BETWEEN 1959 AND 2200),
    CONSTRAINT ck_temporadas_periodo        CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
);

CREATE TABLE times (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    sigla      VARCHAR(5),
    escudo_url VARCHAR(300),
    id_externo VARCHAR(60),
    CONSTRAINT uq_times_nome       UNIQUE (nome),
    CONSTRAINT uq_times_id_externo UNIQUE (id_externo)
);

CREATE TABLE partidas (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    temporada_id   BIGINT       NOT NULL REFERENCES temporadas (id),
    rodada         INTEGER      NOT NULL,
    data_hora      TIMESTAMPTZ  NOT NULL,
    mandante_id    BIGINT       NOT NULL REFERENCES times (id),
    visitante_id   BIGINT       NOT NULL REFERENCES times (id),
    estadio        VARCHAR(160),
    gols_mandante  INTEGER,
    gols_visitante INTEGER,
    status         VARCHAR(20)  NOT NULL,
    id_externo     VARCHAR(60),
    criado_em      TIMESTAMPTZ  NOT NULL,
    atualizado_em  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_partidas_confronto  UNIQUE (temporada_id, mandante_id, visitante_id),
    CONSTRAINT uq_partidas_id_externo UNIQUE (id_externo),
    CONSTRAINT ck_partidas_rodada     CHECK (rodada >= 1),
    CONSTRAINT ck_partidas_adversario CHECK (mandante_id <> visitante_id),
    CONSTRAINT ck_partidas_gols       CHECK (
        (gols_mandante IS NULL OR gols_mandante >= 0)
        AND (gols_visitante IS NULL OR gols_visitante >= 0)
    ),
    CONSTRAINT ck_partidas_status     CHECK (
        status IN ('AGENDADA', 'EM_ANDAMENTO', 'ENCERRADA', 'ADIADA', 'CANCELADA')
    ),
    -- Partida encerrada sem placar corromperia o historico que alimenta o modelo.
    CONSTRAINT ck_partidas_placar_encerrada CHECK (
        status <> 'ENCERRADA'
        OR (gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)
    )
);

CREATE INDEX ix_partidas_temporada_rodada ON partidas (temporada_id, rodada);
CREATE INDEX ix_partidas_data_hora        ON partidas (data_hora);
CREATE INDEX ix_partidas_status           ON partidas (status);
CREATE INDEX ix_partidas_mandante         ON partidas (mandante_id);
CREATE INDEX ix_partidas_visitante        ON partidas (visitante_id);

CREATE TABLE casas_de_aposta (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                  VARCHAR(120) NOT NULL,
    chave                 VARCHAR(60)  NOT NULL,
    ativa                 BOOLEAN      NOT NULL DEFAULT TRUE,
    referencia_de_mercado BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_casas_de_aposta_chave UNIQUE (chave)
);

-- Cada coleta e uma linha nova: o historico permite medir movimento de linha e CLV.
CREATE TABLE odds (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partida_id        BIGINT        NOT NULL REFERENCES partidas (id),
    casa_de_aposta_id BIGINT        NOT NULL REFERENCES casas_de_aposta (id),
    mercado           VARCHAR(30)   NOT NULL,
    selecao           VARCHAR(20)   NOT NULL,
    linha             NUMERIC(5, 2),
    valor             NUMERIC(7, 3) NOT NULL,
    coletada_em       TIMESTAMPTZ   NOT NULL,
    fonte             VARCHAR(60),
    -- NULLS NOT DISTINCT faz a chave funcionar tambem nos mercados sem linha,
    -- onde linha e nula; sem isso coletas repetidas de 1X2 nunca colidiriam.
    CONSTRAINT uq_odds_coleta UNIQUE NULLS NOT DISTINCT
        (partida_id, casa_de_aposta_id, mercado, selecao, linha, coletada_em),
    CONSTRAINT ck_odds_valor CHECK (valor > 1),
    -- Espelha Mercado.aceita(Selecao) no Java; OddMercadoIT cobre as duas pontas.
    CONSTRAINT ck_odds_selecao_do_mercado CHECK (
        (mercado = 'RESULTADO_1X2'  AND selecao IN ('CASA', 'EMPATE', 'FORA'))
        OR (mercado = 'OVER_UNDER_2_5' AND selecao IN ('OVER', 'UNDER'))
        OR (mercado = 'AMBAS_MARCAM'   AND selecao IN ('SIM', 'NAO'))
    )
);

CREATE INDEX ix_odds_partida_mercado_coleta ON odds (partida_id, mercado, coletada_em DESC);
CREATE INDEX ix_odds_casa                   ON odds (casa_de_aposta_id);
