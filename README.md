# java-kb-indexer

Spring Boot-сервис индексации Java-кода. По коммиту клонирует репозиторий, разбирает `src/main/java` через JavaParser (AST-чанки + метаданные), пишет эмбеддинги в Qdrant и отдаёт поиск агенту через REST и встроенный MCP.

Запускается из GitLab CI при push в default branch (шаблон в `docs/gitlab-ci-template.yml`) или вручную через API.

GroupId: `ru.zdadco`. Пакеты: `ru.zdadco.indexer.*`.

## Архитектура модулей

Один процесс, один JAR (`indexer-api`).

| Модуль | Назначение |
|--------|------------|
| `indexer-api` | Точка входа, REST, Spring Security (Bearer на `/api/v1/**`), Liquibase, Actuator, Docker-сборка |
| `indexer-core` | JGit checkout, JavaParser, chunking, embeddings/Qdrant (`VectorStore`), JPA (`index_jobs`, `indexed_repos`), `SearchService` |
| `indexer-mcp` | MCP-инструменты (`@McpTool`), делегируют в тот же `SearchService` |

REST и MCP ходят в общий `SearchService` из `indexer-core`. Векторы — Qdrant, коллекция `java_kb`. Состояние джоб — PostgreSQL.

## Требования

- Java 21 (локальный Maven / `mvnw.cmd`)
- Docker (для `docker compose`)
- OpenAI-compatible API key для эмбеддингов (`OPENAI_API_KEY`)
- Для индексации приватного GitLab: `GITLAB_TOKEN` с `read_repository`

На Windows Maven Wrapper: `mvnw.cmd`. Unix-скрипта `mvnw` в репозитории нет; сборка образа идёт через `maven:3.9.9-eclipse-temurin-21` в `Dockerfile`.

## Переменные окружения

Значения по умолчанию — из `indexer-api/src/main/resources/application.yml` и `docker-compose.yml`.

| Переменная | Назначение | По умолчанию |
|------------|------------|--------------|
| `DATABASE_URL` | JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/java_kb` (в compose: `jdbc:postgresql://postgres:5432/java_kb`) |
| `DATABASE_USER` | Пользователь БД | `java_kb` |
| `DATABASE_PASSWORD` | Пароль БД | `java_kb` |
| `QDRANT_HOST` | Хост Qdrant (gRPC) | `localhost` (в compose: `qdrant`) |
| `QDRANT_PORT` | gRPC-порт Qdrant | `6334` |
| `OPENAI_API_KEY` | Ключ эмбеддингов | пусто |
| `OPENAI_BASE_URL` | OpenAI-compatible endpoint | `https://api.openai.com` |
| `EMBEDDING_MODEL` | Модель эмбеддингов | `text-embedding-3-small` |
| `EMBEDDING_DIMS` | Размерность | `1536` |
| `EMBEDDING_BATCH_SIZE` | Размер батча upsert | `50` |
| `INDEXER_TOKEN` | Bearer-токен REST `/api/v1/**` | `changeme` |
| `GITLAB_TOKEN` | Токен clone/checkout (JGit, user `oauth2`) | пусто |
| `SERVER_PORT` | HTTP-порт приложения | `8080` |

В `docker-compose.yml` в контейнер индексера также пробрасывается `GITLAB_URL` — приложение её не читает (используется `GITLAB_TOKEN` + `repo_url` из запроса).

При смене `EMBEDDING_DIMS` коллекцию `java_kb` нужно пересоздать. При смене модели — полный reindex.

## Запуск

### Docker Compose (рекомендуется)

Поднимает PostgreSQL (`5432`), Qdrant (`6333` HTTP, `6334` gRPC) и индексер (`8080`).

```powershell
$env:OPENAI_API_KEY = "sk-..."
$env:INDEXER_TOKEN = "changeme"
# опционально:
# $env:DATABASE_PASSWORD = "java_kb"
# $env:OPENAI_BASE_URL = "https://api.openai.com"
# $env:GITLAB_TOKEN = "..."
docker compose up -d --build
```

Проверка: `GET http://localhost:8080/actuator/health` (без токена).

Остановка: `docker compose down`.

### Maven без Docker-образа индексера

Нужны живые PostgreSQL и Qdrant на localhost (можно только их из compose):

```powershell
docker compose up -d postgres qdrant
```

Затем (Java 21, из корня репозитория):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot"
$env:OPENAI_API_KEY = "sk-..."
$env:INDEXER_TOKEN = "changeme"
$env:GITLAB_TOKEN = "..."   # если репозиторий не публичный
.\mvnw.cmd -pl indexer-api -am spring-boot:run
```

Приложение слушает `http://localhost:8080`, БД `jdbc:postgresql://localhost:5432/java_kb`, Qdrant gRPC `localhost:6334`.

## Как пользоваться

Все `/api/v1/**` требуют заголовок `Authorization: Bearer <INDEXER_TOKEN>`. MCP и `/actuator/health` — без auth.

### Создать index job

`POST /api/v1/index-jobs` → `202 Accepted`, тело с `job_id`. Обработка асинхронная: clone по `repo_url` @ `commit_sha`, парсинг `**/src/main/java/**/*.java`, удаление старых точек репо в Qdrant, upsert нового snapshot.

```bash
curl --fail-with-body -X POST "http://localhost:8080/api/v1/index-jobs" \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d "{
    \"gitlab_project_id\": \"123\",
    \"repo_path\": \"group/backend-service\",
    \"repo_url\": \"https://gitlab.example.com/group/backend-service.git\",
    \"commit_sha\": \"abc123\",
    \"branch\": \"master\"
  }"
```

Поля тела: `gitlab_project_id`, `repo_path`, `repo_url`, `commit_sha`, `branch` — все обязательные. Повтор с тем же `(repo_path, commit_sha)` не создаёт вторую джобу.

Org-wide CI: скопировать job из [`docs/gitlab-ci-template.yml`](docs/gitlab-ci-template.yml). Group variables: `JAVA_KB_INDEXER_URL`, `JAVA_KB_INDEXER_TOKEN`.

### Статус джобы

`GET /api/v1/index-jobs/{jobId}` → `200` или `404`.

Статусы: `PENDING`, `RUNNING`, `DONE`, `FAILED`. В ответе также `repo_path`, `commit_sha`, `error`, `started_at`, `finished_at`.

```bash
curl -H "Authorization: Bearer changeme" \
  "http://localhost:8080/api/v1/index-jobs/<job-uuid>"
```

### Поиск

`GET /api/v1/search?q=...` — семантический поиск по проиндексированным чанкам.

Опциональные query-параметры: `repo_path`, `symbol_type`, `stereotype`.

```bash
curl -H "Authorization: Bearer changeme" \
  "http://localhost:8080/api/v1/search?q=where%20is%20user%20created&repo_path=group/backend-service&symbol_type=method"
```

### MCP SSE (Cursor)

Эндпоинт: `http://localhost:8080/mcp/sse` (без Bearer). Индексер должен быть запущен.

Пример `mcp.json`:

```json
{
  "mcpServers": {
    "java-kb": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Инструменты:

| Tool | Назначение |
|------|------------|
| `semantic_search` | query + опционально `repoPath`, `symbolType`, `stereotype` |
| `get_symbol` | точный поиск по `qualifiedName` |
| `list_repos` | список проиндексированных репозиториев (PostgreSQL) |
| `find_by_annotation` | например `@RestController` |
| `get_callers` | обратный поиск по payload `calls` |

## Структура репозитория

```
java-kb-indexer/
├── indexer-api/          # REST, security, Liquibase, application.yml, main class
├── indexer-core/         # parser, git, qdrant, search, JPA
├── indexer-mcp/          # MCP tool handlers
├── docs/
│   └── gitlab-ci-template.yml   # job index:knowledge-base для GitLab CI
├── docker-compose.yml
├── Dockerfile            # multi-stage: Maven 21 → jre, порт 8080
├── pom.xml               # parent, Spring Boot 4.1.0, Spring AI 2.0.0
└── mvnw.cmd
```

Индексируется только `**/src/main/java/**/*.java`. Не индексируются тесты, `target/` / `build/` / `out/`, `**/generated/**`, конфиги и не-Java файлы.

## Тесты

Из корня (Java 21):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot"
.\mvnw.cmd test
```

Только ядро:

```powershell
.\mvnw.cmd -pl indexer-core test
```

Docker, PostgreSQL и OpenAI для юнит-тестов не нужны: parser/search/jobs — Mockito, API — `@WebMvcTest`, H2 только в test-профиле API.
