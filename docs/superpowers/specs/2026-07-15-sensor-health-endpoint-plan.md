# Sensor Health / Staleness Endpoint — Implementation Plan

Implements the design in
`2026-07-15-sensor-health-endpoint-design.md`. Executed test-first (TDD).

## Conventions to follow

- Single handler class `WeatherObservationsFunctionsHandler` holds all
  `@FunctionName` methods.
- Read endpoints are `ANONYMOUS`, return `application/json`, catch
  `MongoException` → `500`.
- Business logic that can be computed in-memory lives in a package-private
  calculator (see `WeatherStatsCalculator`) and is unit-tested independently
  (see `WeatherStatsCalculatorTest`).
- Timestamps are formatted `yyyyMMdd_HHmmss` (`DateTimeUtils` / `WeatherDataMapper`).
- Lombok for DTOs (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## Steps

### 1. `SensorHealthView` DTO
File: `src/main/java/walaniam/weather/function/SensorHealthView.java`
Fields: `boolean stale`, `String lastSeen`, `Long minutesSinceLast`,
`int staleMinutes`. Lombok annotations as above.

### 2. `SensorHealthCalculatorTest` (write first — TDD)
File: `src/test/java/walaniam/weather/function/SensorHealthCalculatorTest.java`
Fixed `nowUtc`, no container. Cases:
- empty list → `stale=true`, `lastSeen=null`, `minutesSinceLast=null`,
  `staleMinutes` echoed.
- fresh observation at `nowUtc` (0 min) → `stale=false`, `minutesSinceLast=0`,
  `lastSeen` formatted `yyyyMMdd_HHmmss`.
- observation `nowUtc - 90min` with `staleMinutes=60` → `stale=true`,
  `minutesSinceLast=90`.
- boundary: observation exactly `staleMinutes` old → `stale=true`.
- custom `staleMinutes` (e.g. 120) with a 90-min-old observation → `stale=false`.

### 3. `SensorHealthCalculator`
File: `src/main/java/walaniam/weather/function/SensorHealthCalculator.java`
Package-private, `@NoArgsConstructor(access = PRIVATE)`.
`static SensorHealthView calculate(List<WeatherData> latest, int staleMinutes, LocalDateTime nowUtc)`:
- empty → builder with `stale=true`, null `lastSeen`/`minutesSinceLast`,
  `staleMinutes`.
- else take `latest.get(0)`; `minutesSinceLast = Math.max(0,
  Duration.between(obs.getDateTime(), nowUtc).toMinutes())`;
  `stale = minutesSinceLast >= staleMinutes`; format `lastSeen`.
Run step 2 tests → green.

### 4. `getSensorHealth` handler
File: `src/main/java/walaniam/weather/function/WeatherObservationsFunctionsHandler.java`
Add `@FunctionName("get-sensor-health-v1")` GET, `ANONYMOUS` method:
- private `staleMinutes(request)` helper: parse `staleMinutes` param, default
  `60`, `Math.max(1, ...)`, `NumberFormatException` → `60` (mirror
  `smoothingWindowHours`).
- `List<WeatherData> latest = repository.getLatest(1);`
- `SensorHealthView health = SensorHealthCalculator.calculate(latest,
  staleMinutes, LocalDateTime.now(ZoneOffset.UTC));`
- build `200` response, `Content-Type: application/json`.
- `catch (MongoException e)` → `logWarn` + `500`.

### 5. Handler Testcontainers test
File: `src/test/java/walaniam/weather/WeatherObservationsFunctionsHandlerTest.java`
Add `shouldGetSensorHealth`: post an observation, call `getSensorHealth` with
empty query params, assert `200`, `application/json`, `stale=false`,
`lastSeen` not null.

### 6. Verify
`mvn test -Dtest=SensorHealthCalculatorTest,WeatherObservationsFunctionsHandlerTest`
(Docker daemon required for Testcontainers). All green.

## Risk / notes

- No persistence-layer change: `getLatest(1)` already exists.
- The stale/empty branches are only exercised deterministically by the
  calculator unit test, because the shared static Mongo container in the
  handler test stamps observations at "now" and accumulates state across
  methods.
