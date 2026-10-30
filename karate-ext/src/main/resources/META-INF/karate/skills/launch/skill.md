# Karate — launch a whole suite in one command — Skill

> **Status:** The DevOps/CI on-ramp: one `*.karate.js` file serves the app + runs UI + API + rules as ONE suite into ONE report — `karate launch suite.karate.js`, locally and in CI unchanged.
> **What it is:** `karate launch <file>.karate.js` runs a pure-JS **orchestrator** — the "Jenkinsfile for a karate suite". It is the layer *above* `Runner.*`: `Runner.run(...)` executes one suite; a launcher is the pipeline around it (stand up the SUT, run the matrix, one consolidated report/RTM, and — on the roadmap — sync/notify out).
> **Two ways to start one, same file:** `karate launch suite.karate.js` from a shell, or **`Runner.launch('suite.karate.js')`** from the JS surface — the console, `/api/eval`, MCP. Reach for the JSAPI form whenever you are not at a terminal; see §"Running one without a shell".
> **Stack:** karate-max (paid — the `launch` subcommand is ext-contributed, like `serve`); the `karate` CLI or the `karate-agent` Docker image.
> **Prerequisites:** a licensed karate-max (CLI or container). For the browser stage, a local Chrome/Chromium (the container bundles it).
> **Companion docs:** `Runner.help()` + the **Runner** help page (`runner`); `Http.help()` + the **Launch** help page (`launch`); the public `traceability-demo` kit.

## Who this is for

A **DevOps / CI role** who wants "run the whole demo/suite in one command" to be smooth across GitHub Actions, Azure Pipelines, Jenkins, or GitLab — no separate web server to start, no per-CI bespoke script, one report to publish. If you instead want to *author and iterate on tests inside a project* (deep-dive, generate checks, connect requirements, re-run to close a coverage gap), that's the `Runner.*` / `Harness.*` / coverage surface — start with the **coverage** and **getting-started** skills. This skill is the outer wrapper that runs a finished suite.

## Mental model

```
  karate launch suite.karate.js
        │
        ▼
  suite.karate.js  ── a pure-JS orchestrator (the pipeline) ──────────────┐
    1. Http.serve('sut', 9100)          ← stand up the app (setup)         │
    2. Runner.run([ oracle.feature,     ← run UI + API + rules as ONE      │  same file
                    checks/api.feature,     suite                          │  runs locally
                    ui.feature ])                                          │  AND in CI
    3. if (result.failed) throw …       ← non-zero exit → CI goes red      │
    4. finally { sut.stop() }           ← teardown                        │
        │                                                                  │
        ▼                                                                  │
  target/karate-reports/   ← ONE report: HTML + JUnit (Tests tab) + ──────┘
                             JSONL/RTM + screenshots
```

- **`launch` wraps `Runner`.** `Runner.run(...)` runs one suite and feeds the one coverage graph; the launcher is the setup→run→report→(notify) pipeline around it.
- **Setup/teardown are plain JS.** `Http.serve(...)` at the top, `sut.stop()` in a `finally`, real `if`/loops in between — because it's just a JS file.
- **One report, dual-mode output.** `launch` writes the CLI-default `target/karate-reports` (a drop-in for `karate run`); the served console/agent writes a per-run `runs/<id>/` instead. Both are build artifacts — gitignore them.

## What this skill is for

Get a suite that spans a **web app UI + a REST API + business rules** running with a single command — for a demo, or as the CI job — and producing **one** consolidated traceability report. The skill teaches:

1. Write a `suite.karate.js` that serves the SUT and runs the matrix as one suite.
2. When to reach for `Http.serve(dir[, port])` (host a static SUT in-process — no external server).
3. The one-report/RTM outcome and the dual-mode output dir (`target/karate-reports` vs `runs/<id>/`).
4. Screenshots (always) + opt-in video (`{video:true}` / `KARATE_VIDEO=true`).
5. The CI one-liner (`docker run … launch suite.karate.js`) — the same file the developer runs.

## Quick start — a `suite.karate.js`

```js
// suite.karate.js — serve the app, run UI + API + rules as ONE suite, one report.
//   karate launch suite.karate.js                     (locally: a visible Chrome)
//   docker run … karate-agent launch suite.karate.js  (in CI: headless Chromium)
var sut = Http.serve('sut', 9100);              // host ./sut on the port karate-config.js expects
try {
  var result = Runner.run(['oracle.feature', 'checks/loan-api.feature', 'loan-rate-ui.feature']);
  if (result.failed > 0) {
    throw 'launch: ' + result.failed + ' of ' + result.total + ' scenarios failed — see ' + result.reportUrl;
  }
} finally {
  sut.stop();                                   // teardown
}
```

Run it:

```bash
karate launch suite.karate.js
```

The report lands in `target/karate-reports/` — open `karate-summary.html`. `result` is the uniform Runner shape (same as `Runner.help()` / the **runner** help page): `{ runId, kind, ok, passed, failed, total, reportUrl }` — under `launch`, `reportUrl` points at the report just written.

The three legs are different kinds of thing: `oracle.feature` is a rulebook **oracle** feature (author it with the **rule-authoring** skill); `checks/loan-api.feature` is a REST API check (against a live service, or an in-process `Http.mock` — see the **mock** skill); `loan-rate-ui.feature` drives the browser against the served SUT. Here they're just three `.feature` paths in one `Runner.run([...])`.

## Running one without a shell — `Runner.launch(path)`

If you are driving a served project (the console, `/api/eval`, MCP), you have no terminal, and the CLI form is not available to you. Use the JSAPI twin:

```js
Runner.launch('suite.karate.js')     // → {script, ok}
```

It evaluates the orchestrator in the live session, so a var the script sets stays readable afterwards. An orchestrator reports failure by **throwing**, and that message comes straight back as the error — `launch: 3 of 12 scenarios failed — see <reportUrl>` is the verdict, not a stack trace to decode.

**When a project ships a `*.karate.js`, that file IS "run everything" — start it, don't rebuild it.** `Runner.run('checks')` or a hand-picked feature list looks equivalent and is not: the orchestrator decides which features run, in what order, with what props, and usually ends with `Report.generate()` / `Report.traceability()` to produce the one consolidated report the team publishes. Re-deriving the list skips those steps and quietly runs something CI does not — the verdict may still look right while the artifact is missing. `Runner.run` and `Runner.suite` refuse a `.karate.js` path for this reason and point here.

## Parameterise it — the config door

CI has to be able to say "run this against staging". `Settings.sysenv(name[, default])` reads an OS
environment variable and `Settings.sysprop(name[, default])` a JVM `-D` property, each falling back to the
default when the name is unset **or empty** — the same verbs `karate-boot.js` (`boot.sysenv`/`boot.sysprop`)
and a feature (`karate.sysenv`/`karate.sysprop`) already use, so it is one pattern across all three script
kinds.

```js
var provider = Settings.sysprop('provider.url', Settings.sysenv('PROVIDER_URL', 'http://localhost:8080'));
var result = Runner.run(['checks/api.feature'], { props: { 'loan.url': provider } });
```

- **`sysprop(X, sysenv(X))`** when one run should be able to override an environment default — the property
  wins, the env is the fallback.
- **Two doors, opposite directions.** `Runner.run(path, {props})` pushes properties *into* a suite (read by
  `karate.sysprop` / `karate-config.js` / `boot.sysprop`); `Settings.*` reads a parameter *into the script*
  composing the run. A launcher usually does both, in that order.
- **Secret-shaped names are withheld** (`KEY`, `SECRET`, `TOKEN`, `PASSWORD`, `PAT`, `CREDENTIAL` as a
  `_`-separated segment) — `Settings` reports a key's presence, never its value. A credential is read by the
  process the suite runs in, not printed by the orchestrator.

Keep the local default in the file. The point is that the *same* file runs on a laptop with nothing set and
in CI with one env var — not that the URL moves out of the repo.

## When to use Http.serve

`Http.serve(dir[, port])` stands up a **static-file** HTTP server over `dir` and returns `{url, port, dir, stop()}`. Use it in a launcher so a browser check drives a real `http://…` app with no external `python -m http.server` / `app.sh` — the pipeline owns the server.

- **`dir`** — a kit's static assets (e.g. `sut/`), resolved against the project root.
- **`port`** — omit (or `0`) for an ephemeral port; pass the exact port your `karate-config.js` defaults the app URL to (the demo kits do this so the UI feature finds the SUT).
- **Stop it** — call `handle.stop()` in a `finally`. (A one-shot `docker run … launch` also reaps everything on JVM exit.)

`Http.serve` hosts static files. A programmable **API mock** is a different tool — `Http.mock(...)` (see the **mock** skill); a launcher can start both.

## Output, screenshots, video

- **One report dir.** `launch` writes `target/karate-reports` (HTML + JUnit XML + JSONL/RTM). Send it elsewhere with `Runner.run([...], { output: 'some/dir' })` or `{ output: { dir, junitXml } }`.
- **Screenshots** always ride the report (native karate evidence).
- **Video is opt-in, off by default** (CI-safe): `Runner.run([...], { video: true })` or `KARATE_VIDEO=true` in the environment.

## Wire it into CI

The pipeline runs the **same** file a developer runs — one code path. Publish JUnit → the Tests tab and `target/karate-reports` → the build artifact.

```bash
# one line in the CI job
docker run --rm -v "$PWD:/work" -w /work karate-agent:<version> launch suite.karate.js
```

**Copy a worked recipe, don't re-derive.** The public examples ship both a GitHub Actions workflow and an Azure Pipelines file (with ADO work-item linking): <https://github.com/karatelabs/karate-agent-examples> (`traceability-demo`). Their pipeline `docker run`s the pinned image with `launch suite.karate.js`, `chmod 777 target` for the non-root container, then publishes JUnit + `target/karate-reports`.

## Planned, not shipped

Requirement sync and result write-back to an ALM (Azure DevOps / Jira), a retry policy that folds flaky reruns into the one report, and merging raw JSONL across runs. A launcher today serves the SUT, runs the matrix into one report, takes screenshots and opt-in video, and does JS setup/teardown.

## Standing rules

1. **The same file runs locally and in CI.** No CI-only script — if it's green on your box with `karate launch`, the pipeline runs the identical file. From a served session that is `Runner.launch('<file>')` — same file, same result, no shell.
2. **Gitignore `target/` and `runs/`.** Both are regenerated build artifacts, not source.
3. **Stop what you start.** `Http.serve(...)` → `handle.stop()` in a `finally`.
4. **A suite runs `.feature` files and `.js` checks, mixed.** Pass any mix or a folder to `Runner.run([...])`; a `*.karate.js` goes to `Runner.launch` (see the **runner** help).
5. **Let the exit code carry pass/fail.** `throw` on `result.failed > 0` so the CI job reds — don't swallow it.

## Cross-skill / help links

- `Runner.help()` + the **runner** help page — the execution front door `launch` wraps.
- `Http.help()` + the **launch** / **http** help pages — `Http.serve` and `Http.mock`.
- The **coverage** skill — reading the RTM / gaps the consolidated report produces.
- The **getting-started** skill — first feature → green run, if you're starting from zero.
