# CS-11208: Telemetry events ship full clicked URLs

**Goal:** Prevent open-link telemetry from sending clicked URLs with query strings, fragments, userinfo, or other sensitive URL components.

**Approach:** Keep browser behavior unchanged and sanitize only the telemetry payload in core business logic. Build the open-link telemetry event from a redacted URL representation that preserves `scheme://host/path` for valid URLs and never falls back to the original string for malformed input.

## Context

The ClickUp ticket identifies a privacy issue in `core/src/main/kotlin/com/codescene/jetbrains/core/handler/CwfActionLogic.kt`: `telemetryForOpenUrl(url: String)` currently sends the full URL in telemetry. Full clicked URLs can include query strings, fragments, userinfo, tokens, or per-user identifiers. The platform handler should still open the original clicked URL, but telemetry sent through `TelemetryService` and `ExtensionAPI.sendTelemetry` must not include sensitive URL components.

## Acceptance Criteria

- Open-link telemetry does not include URL query strings.
- Open-link telemetry does not include URL fragments.
- Open-link telemetry does not include URL userinfo such as embedded username/password data.
- Valid URL telemetry keeps only the scheme, host, and path.
- Malformed URL input does not cause telemetry to contain the original unsanitized URL.
- Existing URL-opening behavior remains unchanged.

---

### Task 1: Sanitize Open-Link Telemetry URLs

**Files:**

- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/handler/CwfActionLogic.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/handler/CwfActionLogicTest.kt`

**Intent:** Centralize open-link telemetry redaction in the existing core telemetry helper so every caller gets safe telemetry data without changing platform URL-opening behavior. The platform handler continues to pass and open the original URL, while `telemetryForOpenUrl` stores only a sanitized value in the event data.

**Key signatures/shapes:**

- `fun telemetryForOpenUrl(url: String): CwfTelemetryEvent` returns `TelemetryEvents.OPEN_LINK` with `data["url"]` set to the sanitized URL value.
- `private fun sanitizedTelemetryUrl(url: String): String` parses the URL and returns only `scheme://host/path` for valid URLs.
- For URLs that cannot be parsed into a scheme and host, `sanitizedTelemetryUrl` returns an empty string, not the original input.

**Test strategy:**

- Scenario: `telemetryForOpenUrl("https://codescene.io/docs/page?token=secret#section")` produces data `mapOf("url" to "https://codescene.io/docs/page")`.
- Scenario: `telemetryForOpenUrl("https://user:password@codescene.io/docs")` produces data `mapOf("url" to "https://codescene.io/docs")`.
- Scenario: `telemetryForOpenUrl("https://codescene.io")` keeps the origin as `"https://codescene.io"`.
- Edge case: `telemetryForOpenUrl("not a url ?token=secret")` produces data `mapOf("url" to "")`.
- Regression: existing helper event-name assertions still pass for `OPEN_LINK` and other telemetry helpers.

**Commit:** `CS-11208: sanitize open-link telemetry URLs`
