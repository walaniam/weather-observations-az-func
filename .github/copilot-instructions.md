# Copilot instructions for weather-observations-az-func

Azure Functions app (Java 21, Maven) exposing HTTP endpoints to store and query weather
observations backed by MongoDB / Azure Cosmos DB (Mongo API).

## Build, test, run

- Build & package functions: `mvn clean package`
- Run functions locally: `mvn azure-functions:run` (serves on `http://localhost:7071`)
- Run all tests: `mvn test`
- Run a single test class: `mvn test -Dtest=WeatherObservationsFunctionsHandlerTest`
- Run a single test method: `mvn test -Dtest=WeatherObservationsFunctionsHandlerTest#shouldPostObservation`
- CI (`.github/workflows/maven.yml`) runs `mvn -B package` on JDK 21 (temurin) for pushes/PRs to `main`.

Tests use Testcontainers with a real MongoDB container, so a running Docker daemon is required.
For manual local runs, `docker-compose.yml` starts MongoDB (`27017`) and mongo-express UI (`8081`).

`maven-surefire-plugin` (`3.5.2`) and `junit-jupiter` (`5.11.4`) must be upgraded together —
surefire 2.x does not discover JUnit 5 tests on JDK 21 (reports "Tests run: 0").

To provision the Azure environment, follow the Terraform walkthrough in `DEMO.md` (fill in
`src/main/tf/myenv.auto.tfvars` first, per `README.md`).

## Architecture

- `WeatherObservationsFunctionsHandler` is the single entry point holding all `@FunctionName`
  HTTP triggers: `post-observations-v1` (auth `FUNCTION`), `get-latest-observations-v1`,
  `get-latest-observation-v1`, `get-chart-v1`, `get-chart-image-v1`, `get-extremes-v1`
  (all auth `ANONYMOUS`).
- Persistence is abstracted behind `WeatherDataRepository`, implemented by
  `WeatherDataMongoRepository` (DB `weather`, collection `observations`). The repository
  constructor ensures required indexes exist on startup.
- `MongoClientExecutor<T>` opens a fresh `MongoClient` per call inside try-with-resources and
  applies a POJO codec registry — do not cache clients; each `execute`/`executeWithResult` is
  self-contained.
- MongoDB connection string comes from the `CosmosDBConnectionString` env var; when absent the
  no-arg constructor falls back to a hardcoded local Mongo URL.
- Chart flow: `get-chart-v1` -> `getRange` -> `HtmlGenerator`/`ChartGenerator` (JFreeChart)
  returns an HTML page with an embedded image (`Content-Type: text/html`); `get-chart-image-v1`
  serves the raw chart image.
- Infrastructure lives in `src/main/tf` (Terraform, azurerm provider). See `README.md` and
  `IaC_AZ_CLI.md` for provisioning and Azure CLI operations.

## Conventions

- Handlers use dependency injection via a `Function<ExecutionContext, WeatherDataRepository>`
  provider so tests can inject a repository backed by a Testcontainers connection string.
- Observation input is CSV: `timestamp,outsideTemp,insideTemp,pressureHpa` parsed by
  `WeatherData.of(...)`. The incoming timestamp (`data[0]`) is ignored; the server stamps
  `dateTime` as current UTC.
- Lombok is used throughout (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.); MapStruct
  (`WeatherDataMapper.INSTANCE`) maps `WeatherData` -> `WeatherDataView`. Both are wired as
  annotation processors in `pom.xml` (order: lombok, lombok-mapstruct-binding, mapstruct).
- Log via `LoggingUtils.logInfo`/`logWarn` static helpers (they prefix the function name), not
  raw loggers.
- Errors: catch `MongoException` -> `500`, `NoSuchElementException` -> `404`; JSON responses set
  `Content-Type: application/json` explicitly.
- Date-range endpoints accept either `fromDate`/`toDate` (UTC strings) or `fromDays`/`toDays`
  query params via `DateRangeRequestParamsResolver` (defaults to last 7 days).
