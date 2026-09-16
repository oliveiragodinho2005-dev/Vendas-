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
| 6 | Bilhete por print (extração de seleções via Claude API) | pendente |

## Stack

Java 21 · Spring Boot 3.5.16 · Spring Web · Spring Data JPA · Bean Validation ·
PostgreSQL 16 · Flyway · Maven · JUnit 5 · AssertJ · Testcontainers

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

Os endpoints de negócio (`/api/rodadas/{numero}/partidas`,
`/api/partidas/{id}/analise`, `/api/partidas/ev-positivo`) entram na Fase 4.
Hoje o único endpoint exposto é o do Actuator acima.

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

Sem `API_FOOTBALL_KEY` / `ODDS_API_KEY` definidas a ingestão fica desligada, em vez
de cair para alguma chave embutida.

## Testes

```bash
mvn verify
```

- **Unitários** (`*Test`, via Surefire) — domínio em Java puro, sem Spring e sem banco.
- **Integração** (`*IT`, via Failsafe) — sobem um PostgreSQL 16 com Testcontainers,
  aplicam as migrations e validam o mapeamento JPA contra o schema real.

Os ITs são anotados com `@Testcontainers(disabledWithoutDocker = true)`: sem um
daemon Docker acessível eles são **pulados** em vez de quebrar o build. Se o seu
`mvn verify` reportar testes pulados, é isso — confira que o Docker está no ar
antes de tratar o resultado como cobertura completa.

## Estrutura

```
src/main/java/br/com/seuprojeto/
  dominio/       entidades JPA e enums; regras de placar em Java puro
  repositorio/   interfaces Spring Data JPA
  config/        configuração da aplicação
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

## Modelo de dados

| Tabela | Papel |
|---|---|
| `temporadas` | Edição anual da competição |
| `times` | Clubes, reaproveitados entre temporadas |
| `partidas` | Confrontos, com placar nulo até o encerramento |
| `casas_de_aposta` | bet365 (alvo) e Pinnacle (referência de mercado) |
| `odds` | Cotações coletadas, em série temporal |

Mercados suportados: `RESULTADO_1X2`, `OVER_UNDER_2_5`, `AMBAS_MARCAM`.
