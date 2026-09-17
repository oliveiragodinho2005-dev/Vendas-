-- Franquia diaria de requisicoes por fonte externa.
-- Persistido, e nao em memoria, para que reiniciar a aplicacao nao zere o contador
-- e estoure a cota do dia - no plano gratuito sao 100 requisicoes e acabou.
CREATE TABLE consumo_de_api (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fonte         VARCHAR(60)  NOT NULL,
    dia           DATE         NOT NULL,
    requisicoes   INTEGER      NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_consumo_fonte_dia UNIQUE (fonte, dia),
    CONSTRAINT ck_consumo_requisicoes CHECK (requisicoes >= 0)
);
