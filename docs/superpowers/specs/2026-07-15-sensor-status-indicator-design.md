# Sensor Status Indicator on Chart Page — Design

## Summary

Add a small visual status indicator to the chart dashboard (`chart-page.html`)
showing whether the weather sensor is reporting up-to-date data. It consumes
the existing `get-sensor-health-v1` API (see
`2026-07-15-sensor-health-endpoint-design.md`) and requires no backend
changes.

## Placement

A small circular dot (~10px diameter) positioned in the **top-right corner of
the page `<header>`**. The header becomes `position: relative` so the dot can
be `position: absolute; top: ...; right: ...;` without disrupting the existing
title/subtitle layout.

## States

| State                          | Color                              | Tooltip (native `title` attribute)                                  |
|---------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| Healthy (`stale: false`)        | Green (`#22c55e`)                   | `"Data up to date · Last observation: <formatWhen(lastSeen)>"`        |
| Stale (`stale: true`, has data) | Orange (`#f59e0b`)                  | `"Data stale · Last observation: <formatWhen(lastSeen)>"`             |
| No data (`lastSeen: null`)      | Orange (`#f59e0b`)                  | `"No data available"`                                                 |
| Fetch failed (network/500)      | Neutral gray (`#9ca3af`)            | `"Sensor status unavailable"`                                         |

The "fetch failed" gray state avoids showing a misleading green/orange when
the status itself couldn't be determined.

`<lastSeen>` is formatted using the page's existing `formatWhen(dateTime)`
helper, which already parses the `yyyyMMdd_HHmmss` format returned by the API
and renders it as e.g. `"15 Jul 2026 at 09:30"`.

## Data flow

- New JS function `loadSensorHealth()`:
  - `fetch("get-sensor-health-v1")` (default `staleMinutes=60`, no query
    override in this iteration).
  - On success (`r.ok`): parse JSON, set dot color class and `title` per the
    state table above.
  - On failure (non-OK response or thrown error): set the dot to the gray
    "unavailable" state.
- Called once immediately on page load, and refreshed on the same cadence as
  the existing forecast refresh: `setInterval(loadSensorHealth, 15 * 60 * 1000)`.
- Independent of `loadForecast()` — a failure in one must not affect the
  other.

## Markup / styling changes

- `header` gets `position: relative`.
- New element, e.g. `<span class="sensor-dot" id="sensor-dot" title="..."></span>`
  inside `<header>`.
- New CSS class `.sensor-dot` (fixed size circle, `position: absolute`,
  `top`/`right` offset) plus modifier classes `.healthy`, `.stale`, `.unknown`
  controlling `background-color`.

## Testing

This is static HTML/JS with no existing test harness (`chart-page.html` has
no JS unit tests in the repo; it's covered only by the existing
`shouldServeChartPage` content-assertion test). Consistent with that
convention, verification is manual:
- Confirm dot renders green when the API reports `stale: false`.
- Confirm dot renders orange with correct tooltip when `stale: true`.
- Confirm dot renders orange with "No data available" tooltip when
  `lastSeen` is null.
- Confirm dot renders gray when the endpoint call fails.
- Confirm the existing `shouldServeChartPage` handler test still passes.

## Out of scope

- Making `staleMinutes` configurable from the UI.
- Any change to the `get-sensor-health-v1` API contract.
- Automated JS tests (no JS test infra exists in this repo today).
