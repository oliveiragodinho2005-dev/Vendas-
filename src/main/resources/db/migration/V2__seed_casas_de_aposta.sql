-- bet365 e o alvo das apostas; a Pinnacle entra como referencia de mercado
-- por operar com margem baixa. Ambas sao lidas via API agregadora.
INSERT INTO casas_de_aposta (nome, chave, ativa, referencia_de_mercado) VALUES
    ('Bet365',   'bet365',   TRUE, FALSE),
    ('Pinnacle', 'pinnacle', TRUE, TRUE)
ON CONFLICT (chave) DO NOTHING;
