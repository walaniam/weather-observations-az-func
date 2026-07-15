# Sensor Health / Staleness Endpoint — Design

## Summary

Add a read-only HTTP endpoint that reports whether the weather sensor is still
alive and producing data. It exposes the timestamp of the most recent
observation, how many minutes ago that was, and a `stale` flag derived from a
configurable threshold. The endpoint is intended for lightweight liveness
monitoring (dashboards, uptime probes, or a future alerting function).

Scope is deliberately limited to **liveness only** — no gap detection or
flatline/stuck-sensor detection in this iteration.

## Endpoint

- Function name: `get-sensor-health-v1`
- Method: `GET`
- Auth: `AuthorizationLevel.ANONYMOUS` (consistent with the other read endpoints)
- Response `Content-Type`: `application/json`
- HTTP status:
  - `200 OK` on success (health is conveyed entirely in the JSON body, including
    when the sensor is stale or when there is no data at all)
  - `500 INTERNAL_SERVER_ERROR` on `MongoException`, matching every other read
    handler

### Query parameters

| Param         | Type | Default | Notes                                             |
|---------------|------|---------|---------------------------------------------------|
| `staleMinutes`| int  | `60`    | Minimum `1`. Invalid/negative values fall back to the default. |

The sensor posts roughly every 30 minutes, so a 60-minute default tolerates a
single missed post before flagging the sensor as stale.

## Response body — `SensorHealthView`

New DTO in the `walaniam.weather.function` package, following the existing
`WeatherDataView` conventions (Lombok `@Data`, `@Builder`, `@NoArgsConstructor`,
`@AllArgsConstructor`).

| Field              | Type      | Description                                                        |
|--------------------|-----------|-------------------------------------------------------------------|
| `stale`            | `boolean` | `true` when the latest observation is older than `staleMinutes`, or when there is no data at all. |
| `lastSeen`         | `String`  | UTC timestamp of the latest observation, formatted `yyyyMMdd_HHmmss`. `null` when no data. |
| `minutesSinceLast` | `Long`    | Whole minutes between `lastSeen` and now (UTC). Boxed so it can be `null` when no data. |
| `staleMinutes`     | `int`     | The threshold actually applied (echoes the effective query param). |

`lastSeen` uses the same `yyyyMMdd_HHmmss` format produced by
`WeatherDataMapper` / `DateTimeUtils`, keeping timestamp formatting consistent
across the API.

## Logic — `SensorHealthCalculator`

The health computation is extracted into a dedicated calculator class, mirroring
how `WeatherStatsCalculator` is split out from the handler and unit-tested
independently. This keeps the logic deterministic and testable without a
database or wall-clock dependency.

```
SensorHealthView calculate(List<WeatherData> latest, int staleMinutes, LocalDateTime nowUtc)
```

Behaviour:

- **Empty list** (no observations at all):
  `stale = true`, `lastSeen = null`, `minutesSinceLast = null`,
  `staleMinutes = <threshold>`.
- **Non-empty** (first element is the most recent observation):
  - `minutesSinceLast = max(0, Duration.between(lastSeen, nowUtc).toMinutes())`
    — the `max(0, ...)` guards against minor clock skew where a freshly stamped
    observation could appear slightly in the future.
  - `stale = minutesSinceLast >= staleMinutes` (boundary is stale).
  - `lastSeen` = latest observation's `dateTime` formatted `yyyyMMdd_HHmmss`.

## Handler wiring

The new handler method in `WeatherObservationsFunctionsHandler`:

1. Resolves `staleMinutes` from the query params via a private helper that
   mirrors the existing `smoothingWindowHours(...)` (default `60`, min `1`,
   `NumberFormatException` → default).
2. Calls `repository.getLatest(1)` — **no repository or interface change is
   required**; this method already exists.
3. Calls
   `SensorHealthCalculator.calculate(latest, staleMinutes, LocalDateTime.now(ZoneOffset.UTC))`.
4. Serializes the result as JSON with `Content-Type: application/json`.
5. Catches `MongoException` → `500`.

## Testing

- **`SensorHealthCalculatorTest`** — pure unit tests with a fixed `nowUtc`, no
  container required (mirrors `WeatherStatsCalculatorTest`):
  - empty list → `stale=true`, `lastSeen=null`, `minutesSinceLast=null`
  - fresh observation (0 minutes old) → `stale=false`
  - observation older than threshold → `stale=true`
  - boundary: exactly `staleMinutes` old → `stale=true`
  - custom `staleMinutes` respected
- **`WeatherObservationsFunctionsHandlerTest`** — one Testcontainers case:
  post an observation, call `getSensorHealth`, assert `200`,
  `Content-Type: application/json`, `stale=false`, `lastSeen` present.

The stale and empty branches are covered deterministically by the calculator
unit tests (the shared static Mongo container in the handler test accumulates
data across methods and stamps observations at "now", so those branches cannot
be reproduced reliably there).

## Out of scope

- Gap detection over a date range.
- Flatline / stuck-sensor detection.
- Push notifications / timer-triggered alerting (a future feature could consume
  this endpoint).
- Any change to the persistence layer or `WeatherData` model.
