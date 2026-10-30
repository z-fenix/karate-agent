# Karate CLI — Skill (Non-Java Teams)

> **Status:** The standalone-CLI skill — for teams that don't use Maven/Gradle and don't want JVM build tooling in their workflow.
> **Stack:** karate-cli (Rust launcher `karate` + bundled JVM runtime). No Maven, no `pom.xml`, no `Runner.java`. Tests live in `features/` + `karate-pom.json`.
> **Prerequisites:** None. This is the on-ramp for non-Java teams.
> **Companion docs:** karate-docs `/running-tests/command-line`, `/getting-started/standalone-execution`.

## Who this is for

Use this skill — **not** `Skill.help('getting-started')` — when:

- The team's stack is **Python / Node / Go / Ruby / .NET** — Karate runs as a tool, not a library.
- A **QA team** owns the tests independently of any service codebase.
- You want **zero Java tooling on the developer machine** — no JDK install, no Maven, no IDE plugin.
- You're running Karate in a **CI image** where Maven would be overhead (e.g. a minimal Alpine + the karate fatjar).
- You're driving Karate **from a non-JVM orchestrator** — a Node CLI, a Python script, a Make target, a shell pipeline.

Java teams writing Java services + Karate tests in the same repo should use `Skill.help('getting-started')`.

> **Have a `karate-agent-<v>.jar` or `karate-async-<v>.jar` (the governance product)? Run it with `java -jar`.**
> You need **Java 21+** and nothing else to install — skip the standalone-`karate`-launcher install below. The
> subcommands are **`run` · `mock` · `serve` · `launch`**, invoked directly on the jar:
> `java -jar karate-agent-<v>.jar run features/` runs your suite with full coverage / requirements / rules
> (browser/UI needs `serve`/`launch`, not `run`). The `karate` launcher and its management commands
> (`setup`/`update`/`config`/`init`/`doctor`) in the rest of this page are a **separate track** — for those jars,
> prefer the `java -jar` form. Everything else here (the `run` flags, `karate-config.js`, `KARATE_OPTIONS`, the
> CI recipes) applies to `java -jar … run` verbatim.

## Mental model

```
   karate (Rust launcher binary)
     │
     ├─ Management:  setup / update / config / init / doctor / version
     │  (no JVM needed — pure Rust)
     │
     └─ Runtime:     run / mock / mcp / clean
        │
        ▼ delegates
     Java fatjar (bundled, lives in ~/.karate/dist/)
        │
        ▼ reads
     karate-pom.json   ← project file (paths, tags, env, threads, output)
     karate-config.js  ← runtime config (baseUrl, auth, env switching)
     features/*.feature ← the tests
```

The `karate` binary is the only thing on the developer's `$PATH`. It auto-installs / updates a bundled JVM + the karate runtime fatjar to `~/.karate/dist/`. When you run `karate run …`, the Rust launcher invokes `java -jar` under the hood — but the developer never types `java`.

## Quick start (zero to green)

### 1. Install

```bash
# macOS / Linux (Homebrew)
brew install karatelabs/tap/karate

# macOS / Linux (curl)
curl -fsSL https://karatelabs.io/install.sh | bash

# Windows
# Download installer from https://karatelabs.io/download
```

First run triggers `karate setup` which downloads the runtime (~50 MB).

### 2. Scaffold a project

```bash
mkdir my-tests && cd my-tests
karate init                       # interactive scaffold
```

`karate init` prompts for project name + base URL and writes:

```
my-tests/
├── karate-pom.json
├── karate-config.js
└── features/
    └── hello.feature
```

### 3. Write a test

`features/hello.feature`:

```feature
Feature: hello world

Scenario: status check
  Given url baseUrl
  And path 'get'
  When method get
  Then status 200
```

`karate-config.js`:

```javascript
function fn() {
  var env = karate.env || 'dev';
  var profile = {
    dev:  { baseUrl: 'https://httpbin.org' },
    prod: { baseUrl: 'https://httpbin.org' }
  }[env];
  profile.apiKey    = karate.sysenv('API_KEY');
  profile.timeoutMs = parseInt(karate.sysprop('http.timeout.ms', '5000'));
  return profile;
}
```

`karate.sysenv(name, [default])` for OS env vars; `karate.sysprop(name, [default])` for `-D` JVM system properties. Optional second arg = fallback when unset or empty.

### 4. Run

```bash
karate run                              # auto-detects karate-pom.json in cwd
karate run features/                    # explicit path
karate run features/hello.feature       # one file
karate run features/hello.feature:5     # one scenario (by line)
karate run -n 'status check'            # one scenario (by name)
karate run -t @smoke                    # filter by tag
karate run -T 4                         # 4 parallel threads
karate run -e prod                      # set karate.env
```

Reports land in `target/karate-reports/karate-summary.html` (override via `-o` or `output.dir` in pom).

> ⚠️ **Already handed a `karate-agent` JAR? Just run it — do not download any separate karate jar.** The
> `karate-agent-<v>.jar` *is* this same 2.x engine (a superset — same `run`/`mock` plus `serve`/`launch`),
> so `java -jar karate-agent-<v>.jar run features/` runs your tests as a standalone process with full coverage /
> requirements / rules. **Never** pull `karate-<version>.jar` from GitHub releases (e.g. `karate-1.4.1.jar`) — that
> is the **old `com.intuit.karate` 1.x** engine: unsupported, no governance features, and it hangs on Java 25.

### 5. Wire karate-agent dashboard

```bash
KARATE_AGENT_URL=http://localhost:4444 karate run
```

Same env var as Maven workflows — the dashboard receives JSONL events from the standalone CLI exactly as it does from `mvn test`.

## `karate-pom.json` — the project file

The standalone equivalent of `pom.xml`. Defines *how* to run tests; does **not** define runtime config (that's `karate-config.js`).

Full schema:

```json
{
  "paths": ["features/"],
  "tags": ["@smoke", "~@slow"],
  "env": "dev",
  "threads": 4,
  "scenarioName": "Login happy path",
  "configDir": ".",
  "workingDir": "/abs/path/to/project",
  "dryRun": false,
  "clean": false,
  "output": {
    "dir": "target/karate-reports",
    "html": true,
    "junitXml": true,
    "cucumberJson": false,
    "jsonLines": true
  },
  "logging": {
    "report": "debug",
    "console": "info"
  },
  "listeners": ["com.example.MyListener"],
  "listenerFactories": ["com.example.MyListenerFactory"]
}
```

**CLI args override pom values.** Precedence (highest → lowest):

```
karate.options sysprop / KARATE_OPTIONS env
  → individual sysprops / env (karate.env, KARATE_ENV, etc.)
    → karate-pom.json
      → defaults
```

`workingDir` resolves *features + config + pom location*; `output.dir` resolves relative to the *process cwd*. Use absolute paths for `output.dir` in CI where cwd may vary.

## Subcommand reference

| Subcommand | Purpose | Common args |
|---|---|---|
| `karate run` | Run tests. | `[paths]`, `-t @tag`, `-e env`, `-T N`, `-n "name"`, `-o dir`, `-f fmt`, `-D` (dry run — see below) |
| `karate mock` | Start a mock server from a `.feature`. | `--port 8080`, `mocks/` |
| `karate clean` | Wipe reports + temp dirs. | `-o dir` |
| `karate mcp` | MCP server mode for LLM integration. | `--stdio` |
| `karate setup` | First-run wizard. | (interactive) |
| `karate update` | Check for updates. | |
| `karate config` | View / edit config. | `--show`, `--export` |
| `karate init` | Scaffold a new project. | (interactive) |
| `karate doctor` | System diagnostics. | |
| `karate version` | Show version info. | |

`karate run` is the workhorse — the rest are management commands.

### `-D` — the dry run (coverage with nothing executed)

```bash
karate run -D -o target/rtm-dryrun features/
# handed a jar instead of the launcher? same command, same flags:
java -jar karate-agent-<v>.jar run -D -o target/rtm-dryrun features/
```

Parse-only: every scenario is discovered and its **tags are read**, but no step, hook or `karate-config.js`
runs — nothing is called, so no environment, SUT or mock is needed. The report and the **coverage graph are
still written**, built from the *declared* edges (`@req=` / `@cov=`) alone with zero observed hits. That is
how you answer *"does a test exist that claims this requirement/operation?"* without running the suite —
cheap enough for every PR. The JSAPI/MCP equivalent is `Runner.suite(path, {dryRun:true})`.

Read the output as an inventory: **NOTRUN** = "a test declares this, nobody ran it", **NOTCOVERED** =
"nothing claims it at all". A dry run can never report COVERED (its steps are stamped `skipped`, not
`passed`), so the headline coverage `%` is **0** by design — the signal is the NOTRUN/NOTCOVERED split.
Caveats: the console still prints `passed: N` (skips count as "did not fail"); pair `-D` with `-t` and the
filtered-out scenarios' declared edges never land, so a *complete* declared RTM needs no tag filter;
`@setup` scenarios are exempt from the skip and really execute; and always use `-o <dir>` — a run replaces
the report dir it writes, so a dry run into your normal one displaces the real graph with an all-NOTRUN one.

## `*.karate.js` — JavaScript scripts (no Gherkin)

The CLI also runs `*.karate.js` files — full `karate.*` API in a JS context, no Gherkin. Ideal for:

- Complex orchestration with `karate.fork()` / `karate.exec()`.
- Multi-step async with callbacks.
- Custom test harnesses that don't fit the Scenario shape.

```javascript
// setup.karate.js
const orders = karate.read('/fixtures/orders.json');

karate.log('seeding ' + orders.length + ' orders');

orders.forEach(o => {
  const res = karate.http(karate.env.baseUrl + '/orders').body(o).post();
  if (res.status !== 201) {
    karate.fail('seed failed for ' + o.id + ' (' + res.status + ')');
  }
});

karate.log('seed complete');
```

```bash
karate run setup.karate.js
karate run features/ setup.karate.js    # mix .feature + .karate.js
```

## CI/CD integration (non-Java)

### GitHub Actions

```yaml
name: tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install karate
        run: curl -fsSL https://karatelabs.io/install.sh | bash
      - name: Run tests
        env:
          KARATE_AGENT_URL: ${{ secrets.KARATE_AGENT_URL }}
          KARATE_ENV: ci
        run: karate run -T 4 features/
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: karate-reports
          path: target/karate-reports/
```

### GitLab CI

```yaml
test:
  image: karatelabs/karate:2.0.10                    # official image; karate is already on PATH
  variables:
    KARATE_AGENT_URL: $KARATE_AGENT_URL
    KARATE_ENV: ci
  script:
    - karate run -T 4 features/
  artifacts:
    paths:
      - target/karate-reports/
    when: always
```

### Docker (Alpine-based image, ~150 MB)

```bash
docker run --rm -v $(pwd):/work -w /work \
  -e KARATE_ENV=dev \
  karatelabs/karate:2.0.10 \
  karate run -T 4 features/
```

### Jenkins (declarative)

```groovy
pipeline {
  agent { docker { image 'karatelabs/karate:2.0.10' } }
  environment {
    KARATE_AGENT_URL = 'http://karate-agent:4444'
  }
  stages {
    stage('test') {
      steps { sh 'karate run -T 4 features/' }
      post { always {
        archiveArtifacts artifacts: 'target/karate-reports/**'
        junit 'target/karate-reports/*.xml'
      } }
    }
  }
}
```

## `karate.options` — single-string override

Maven teams pass `-Dkarate.options="..."`; standalone teams use `KARATE_OPTIONS=...`. Same grammar as the `karate run` CLI:

```bash
KARATE_OPTIONS="--tags @smoke --env qa --threads 4" karate run features/
```

When set, `karate.options` **replaces** the pom's path/tag/env values. Used in CI to override a stable pom from outside.

## Log masking (secrets)

Don't pass secrets via CLI flags. Configure masking once in `karate-config.js`:

```javascript
configure logging = {
  mask: {
    headers:    ['Authorization', 'Cookie', 'X-Api-Key'],
    jsonPaths:  ['$.password', '$..token'],
    patterns:   [{ regex: '\\bBearer [A-Za-z0-9._-]+\\b', replacement: 'Bearer ***' }],
    replacement: '***'
  }
}
```

Applies to all output (HTML reports, JSONL, console). No CLI flag — masking is declarative in code.

## Output formats

`-f / --format` accepts comma-separated names; prefix with `~` to disable:

| Format | Purpose | Default |
|---|---|---|
| `html` | Karate HTML reports. | on |
| `cucumber:json` | Per-feature Cucumber JSON (Allure / RP). | off |
| `junit:xml` | JUnit XML (CI test-result UIs). | off |
| `karate:jsonl` | JSONL event stream (karate-agent dashboard). | off |

```bash
karate run -f cucumber:json,junit:xml features/      # add Cucumber + JUnit
karate run -f ~html,karate:jsonl features/           # JSONL only
```

Or set `output.cucumberJson: true` in `karate-pom.json` to persist.

## Standing rules

1. **`karate-pom.json` is for "how"; `karate-config.js` is for "what".** Paths/tags/threads/output in pom. Base URLs / auth tokens / env switching in config.js. Don't mix. (Where files *resolve* is neither one's job: a path reference anchors at the project root — `Help.page('paths')` — and a Java project declares its resource root with `boot.classpath(dir)` in `karate-boot.js`.)
2. **Use `karate init` to scaffold.** It generates a sane pom + config + sample feature; modify rather than write from scratch.
3. **Set `karate.env` per environment.** `KARATE_ENV=qa karate run` switches config.js to the qa branch.
4. **Use absolute `output.dir` in CI.** Relative paths break when cwd varies (containers, IDE wrappers).
5. **Prefer `KARATE_OPTIONS` for CI overrides, not editing the pom.** Keep the pom stable in git; let CI inject the tag filter.
6. **For non-Java codebases, treat the karate suite as a *separate project*.** Its own repo, its own CI pipeline, its own karate-agent dashboard. Linked to the service codebase by REST contracts, not by build tooling.
7. **Use `karate clean` between runs in CI** to wipe stale reports + temp dirs.
8. **Don't install Java separately.** The Rust launcher bundles a JVM; `karate doctor` validates the install.
9. **For mock servers in non-Java integration**: `karate mock --port 8080 mocks/` — drop-in replacement for WireMock / Mountebank.

## Anti-patterns

1. **Anti-pattern:** mixing `pom.xml` and `karate-pom.json` in the same project. Pick one — Java teams use `pom.xml` + JUnit runner; non-Java teams use `karate-pom.json` + CLI.
2. **Anti-pattern:** invoking `java -jar karate.jar` directly. Use the `karate` launcher — it manages updates, bundled JVM, classpath, and extensions.
3. **Anti-pattern:** committing `target/` or `.karate/` directories. Both are generated.
4. **Anti-pattern:** passing secrets on the command line (visible in `ps`, in shell history, in CI logs). Set them as env vars and read via `karate.sysenv('NAME')` (env var) or `karate.sysprop('name')` (`-Dname=…` system property) in `karate-config.js`. Both take an optional second arg as the default when unset/empty.
5. **Anti-pattern:** building a custom shell wrapper around `karate run`. If you need custom orchestration, write a `*.karate.js` script — same engine, full API.
6. **Anti-pattern:** running Karate from a JDK-less CI image and then `apt install`-ing the JDK. Use the official `karatelabs/karate` image instead.

## Cross-skill links

- `Skill.help('getting-started')` — the Maven-based on-ramp; **read this if your team writes Java**.
- `Skill.help('api')` — REST authoring patterns (same `.feature` syntax regardless of CLI vs Maven).
- `Skill.help('assertions')` — the matcher (CLI runs use the same matcher; not CLI-specific).
- `Skill.help('mock')` — `karate mock` subcommand is the CLI surface for mock servers.

## References

- karate-docs `/running-tests/command-line` — published reference.
- karate-docs `/getting-started/standalone-execution` — install + first-run walkthrough.
