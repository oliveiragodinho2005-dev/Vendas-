# Calculadora de Valor Esperado — Brasileirão Série A

Backend que estima probabilidades de partidas do Campeonato Brasileiro Série A com
um modelo de Poisson, compara com as odds de mercado sem a margem da casa e calcula
o valor esperado (EV) de cada seleção.

> **Origem dos dados:** resultados e odds vêm exclusivamente de APIs de dados
> agregadoras. O projeto não faz scraping da bet365 nem de qualquer site de apostas.

## Status

| Fase | Escopo | Situação |
|---|---|---|
| 1 | Fundação: schema, entidades, Flyway, Docker Compose | ✅ concluída |
| 2 | Ingestão: API-Football, fonte de odds, jobs agendados | pendente |
| 3 | Motor de probabilidades: Poisson + Dixon-Coles | pendente |
| 4 | Valor esperado, Kelly fracionário e API REST | pendente |
| 5 | Backtest: Brier, log loss, ROI, calibração, CLV | pendente |
| 6 | Bilhete por print (extração de seleções via Claude API) | 🟡 leitura pronta; EV depende da Fase 4 |

## Stack

Java 21 · Spring Boot 3.5.16 · Spring Web · Spring Data JPA · Bean Validation ·
PostgreSQL 16 · Flyway · Maven · JUnit 5 · AssertJ · Testcontainers ·
Claude API (`com.anthropic:anthropic-java`, modelo `claude-opus-5`)

## Como subir o ambiente

**Pré-requisitos:** JDK 21, Maven 3.9+, Docker e Docker Compose.

```bash
# 1. Configuração local (o .env nunca é versionado)
cp .env.example .env
$EDITOR .env

# 2. Banco de dados
docker compose up -d
docker compose ps          # aguarde o healthcheck ficar "healthy"

# 3. Aplicação (o Flyway aplica as migrations no start)
mvn spring-boot:run
```

Conferindo que subiu:

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
```

### Leitura de bilhete por print

```bash
curl -s -X POST http://localhost:8080/api/bilhetes/analise \
  -F "imagem=@/caminho/do/bilhete.png"
```

Resposta:

```json
{
  "selecoes": [
    {
      "textoOriginal": "Flamengo x Palmeiras - Resultado Final: Casa",
      "mandanteLido": "Flamengo",
      "visitanteLido": "Palmeiras",
      "mandanteId": 1,
      "visitanteId": 2,
      "mercado": "RESULTADO_1X2",
      "selecao": "CASA",
      "odd": 2.10,
      "partidaId": 10,
      "rodada": 7,
      "status": "VINCULADA"
    }
  ],
  "oddCombinada": 2.10,
  "totalDeSelecoes": 1,
  "selecoesVinculadas": 1,
  "observacao": null,
  "avisos": []
}
```

O campo `status` diz até onde cada seleção chegou: `VINCULADA`,
`MERCADO_NAO_SUPORTADO`, `TIME_NAO_IDENTIFICADO`, `TIME_NAO_CADASTRADO`,
`TIME_AMBIGUO` ou `PARTIDA_NAO_ENCONTRADA`. Enquanto a ingestão da Fase 2 não
popular `times` e `partidas`, o normal é receber `TIME_NAO_CADASTRADO` — a
extração funciona, falta com o que vincular.

**Não há EV aqui ainda.** `oddCombinada` é aritmética sobre o bilhete; o valor
esperado de cada seleção exige as probabilidades do motor (Fase 3) e as odds sem
margem (Fase 4).

Códigos de erro: `400` imagem inválida, `413` arquivo acima do limite,
`502` a API do Claude falhou, `503` sem `ANTHROPIC_API_KEY` no servidor.

Os endpoints de análise (`/api/rodadas/{numero}/partidas`,
`/api/partidas/{id}/analise`, `/api/partidas/ev-positivo`) entram na Fase 4.

Para derrubar o ambiente:

```bash
docker compose down        # mantém os dados
docker compose down -v     # descarta o volume também
```

## Variáveis de ambiente

Todas são lidas do ambiente; nenhuma credencial fica no repositório. Veja
`.env.example` para o modelo completo.

| Variável | Usada por | Padrão | Descrição |
|---|---|---|---|
| `POSTGRES_DB` | docker compose | `apostas` | Nome do banco criado no container |
| `POSTGRES_USER` | docker compose | `apostas` | Usuário do banco |
| `POSTGRES_PASSWORD` | docker compose | `apostas` | Senha do banco |
| `POSTGRES_PORT` | docker compose | `5432` | Porta publicada no host |
| `DB_URL` | aplicação | `jdbc:postgresql://localhost:5432/apostas` | JDBC URL |
| `DB_USER` | aplicação | `apostas` | Usuário JDBC |
| `DB_PASSWORD` | aplicação | `apostas` | Senha JDBC |
| `SERVER_PORT` | aplicação | `8080` | Porta HTTP |
| `API_FOOTBALL_KEY` | ingestão (Fase 2) | vazio | Chave da API-Football |
| `ODDS_API_KEY` | ingestão (Fase 2) | vazio | Chave da The Odds API |
| `ANTHROPIC_API_KEY` | leitura de bilhete | vazio | Chave da Claude API |

Sem `API_FOOTBALL_KEY` / `ODDS_API_KEY` definidas a ingestão fica desligada, em vez
de cair para alguma chave embutida. Sem `ANTHROPIC_API_KEY` a aplicação sobe
normalmente e só o endpoint de bilhete responde `503`.

## Testes

```bash
mvn verify
```

- **Unitários** (`*Test`, via Surefire) — domínio em Java puro, sem Spring e sem banco.
- **Integração** (`*IT`, via Failsafe) — sobem um PostgreSQL 16 com Testcontainers,
  aplicam as migrations e validam o mapeamento JPA contra o schema real.

Os ITs de banco são anotados com `@Testcontainers(disabledWithoutDocker = true)`:
sem um daemon Docker acessível eles são **pulados** em vez de quebrar o build.

`LeitorDeBilheteClaudeIT` é o único teste que chama a API de verdade — e portanto
o único que custa dinheiro. Ele desenha um bilhete sintético com Java2D (print de
bilhete real carrega identificador de aposta e não entra no repositório) e só roda
com `ANTHROPIC_API_KEY` no ambiente; sem ela, é pulado.

Se o seu `mvn verify` reportar testes pulados, é um desses dois casos — confira
antes de tratar o resultado como cobertura completa.

## Estrutura

```
src/main/java/br/com/seuprojeto/
  dominio/       entidades JPA, enums e regras em Java puro
  repositorio/   interfaces Spring Data JPA
  servico/       orquestração (análise de bilhete, resolução de times)
  api/           controllers REST e tratamento de erros
  ia/            porta de leitura por IA, em Java puro
    claude/      adaptador do SDK da Anthropic
src/main/resources/
  application.yml
  db/migration/  migrations Flyway (fonte única do schema)
```

Camadas previstas: `controller → service → repository`. O motor de cálculo virá em
pacote próprio (`motor/`), em Java puro e sem dependência do Spring, para ser testado
sem subir contexto.

## Decisões de modelagem

- **O Flyway é dono do schema.** O Hibernate roda com `ddl-auto=validate` e só
  verifica se o mapeamento bate; ele nunca altera tabelas.
- **Histórico de odds é imutável.** Cada coleta insere uma linha nova em `odds`;
  nada é sobrescrito. É esse histórico que permite medir movimento de linha e CLV
  contra a odd de fechamento na Fase 5.
- **Mercado e seleção são validados nas duas pontas.** `Mercado.aceita(Selecao)` no
  Java e o CHECK `ck_odds_selecao_do_mercado` no banco. O teste
  `bancoAceitaTodosOsParesDeclaradosNoJava` existe para detectar divergência entre eles.
- **Datas sempre em UTC** (`timestamptz` + `OffsetDateTime`). A conversão para
  `America/Sao_Paulo` acontece na borda da API.
- **Resultado como `Selecao`.** `Partida.resultadoDe(Mercado)` devolve a seleção
  vencedora na mesma linguagem das odds, o que evita conversão ao comparar previsão
  e realidade no backtest.
- **A IA não estima probabilidade.** Um LLM não produz probabilidade calibrada, e
  sim um número que se parece com uma. O papel dele aqui é percepção — ler a imagem
  e devolver dado estruturado. As probabilidades são do motor de Poisson/Dixon-Coles
  da Fase 3, e o EV sai delas.
- **Porta e adaptador na leitura por IA.** `LeitorDeBilhete` é uma interface em Java
  puro; o SDK vive só no adaptador. Por isso os testes do serviço rodam com um dublê,
  sem rede, sem chave e sem custo.
- **Nome ambíguo não vira chute.** Se um nome lido bate com mais de um time, a
  resolução devolve `AMBIGUO` em vez de escolher. Vínculo errado não aparece como
  erro: ele vira um EV calculado contra o jogo errado.

## Modelo de dados

| Tabela | Papel |
|---|---|
| `temporadas` | Edição anual da competição |
| `times` | Clubes, reaproveitados entre temporadas |
| `partidas` | Confrontos, com placar nulo até o encerramento |
| `casas_de_aposta` | bet365 (alvo) e Pinnacle (referência de mercado) |
| `odds` | Cotações coletadas, em série temporal |
| `apelidos_de_time` | "Galo", "Mengão" e grafias que a normalização não alcança |

Mercados suportados: `RESULTADO_1X2`, `OVER_UNDER_2_5`, `AMBAS_MARCAM`.
