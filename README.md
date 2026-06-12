
# Netty Load Tester

Async HTTP load tester built with Netty. Runs persona-based API flows as virtual users with optional total-request mode, dynamic scaling, and CSV/terminal reporting.

## Requirements

- **JDK 26** (see `pom.xml` compiler target)
- **Maven 3.9+**

## Build

```bash
mvn compile
```

## Run

Default persona file: `personas/persona.json`

```bash
mvn exec:java
```

Pass CLI flags via Maven:

```bash
mvn exec:java -Dexec.args="--no-request-log --sticky-connections"
```

### Interactive prompts

After startup, the tool asks (stdin):

1. **Load mode** — virtual users per persona, or total requests per persona
2. **Per-persona load** — count for each persona in the test plan
3. **Duration** — test length in seconds
4. **Ramp-up** — seconds to spread user spawn (user mode) or initial scaler ramp (request mode)
5. **Target TPS** — global cap; `0` = unlimited

### CLI flags

| Flag | Default | Description |
|------|---------|-------------|
| `--no-request-log` | off | Skip `request-log.csv` (faster, less disk I/O) |
| `--track-sent-rps` | off | Track send-time RPS separately from completed-response throughput |
| `--sticky-connections` | off | One serialized HTTP channel per virtual user per host (session-like) |

Default connection mode is **pooled** (acquire/release per request).

## Output

| File | Description |
|------|-------------|
| Terminal summary | Global, per-persona, and per-step stats (P90/P95/P99, error rate, throughput) |
| `csvreport.csv` | Append-only ledger — one summary row per test run |
| `step-report.csv` | Per-step breakdown for the latest run |
| `request-log.csv` | Per-request detail (skipped with `--no-request-log`) |

During the run, a live line prints every 2s: `[LIVE] TPS | Total | Errors`.

## Load modes

- **User mode** — fixed virtual users per persona loop steps until duration ends
- **Request mode** — fixed total requests per persona over duration; `DynamicRequestModeScaler` spawns VUs to hit the budget

## Project layout

```
src/main/java/org/tester/
  Main.java              Entry point
  config/                CLI flags, shared constants
  control/               TPS limiter, request budgets, pacing
  executor/              Netty HTTP, WebSocket, step dispatch
  metrics/               Lock-free metrics and histograms
  model/                 Persona / ApiStep JSON models
  parser/                Persona file parsing
  report/                Terminal + CSV reporting
  runner/                Virtual users, scaler, orchestration
  runtime/               Variables and response extraction
  selector/              Interactive load/duration prompts
personas/                Persona JSON test plans
```

## Notes

- HTTP request timeout is **60s** (`HttpExecutor`). Timeouts appear as ~60000ms in latency percentiles under heavy load.
- `mvn exec:java` defaults to `--no-request-log` and 2GB heap (see `pom.xml`).
>>>>>>> netty
