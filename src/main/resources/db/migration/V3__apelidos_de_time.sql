-- Apelidos e grafias alternativas de clube. Existe porque bilhete impresso usa
-- "Galo" e "Verdao", e cada fonte externa grafa o mesmo time de um jeito.
CREATE TABLE apelidos_de_time (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    time_id             BIGINT       NOT NULL REFERENCES times (id),
    apelido             VARCHAR(120) NOT NULL,
    apelido_normalizado VARCHAR(120) NOT NULL,
    -- Um apelido nao pode apontar para dois times: a ambiguidade tem que estourar
    -- na escrita, nao virar vinculo errado na leitura do bilhete.
    CONSTRAINT uq_apelidos_normalizado UNIQUE (apelido_normalizado),
    CONSTRAINT ck_apelidos_normalizado CHECK (length(trim(apelido_normalizado)) > 0)
);

CREATE INDEX ix_apelidos_time ON apelidos_de_time (time_id);
