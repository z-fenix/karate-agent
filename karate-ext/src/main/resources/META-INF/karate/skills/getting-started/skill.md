# Karate Getting Started — Skill

> **Status:** The on-ramp skill: first feature file → first green run → first CI integration → first karate-agent dashboard run.
> **Stack:** karate-core, JDK 21+, Maven **or** Gradle, `karate-junit6` runner (JUnit Platform).
> **Prerequisites:** None. This is the entry point.
> **Companion docs:** karate-docs `/getting-started/*`.

## What this skill is for

Get a developer (or LLM) **from zero to a green `mvn test` with karate-agent dashboard integration** in under 10 minutes. The skill teaches:

1. Scaffold a minimal Maven project with karate-core.
2. Write the first `.feature` with one passing `Scenario:`.
3. Run via `mvn test`.
4. Wire karate-runner-mode: set `KARATE_AGENT_URL` and confirm runs land in the dashboard.
5. Hand off to the next skill: `api/`, `grpc/`, `kafka/`, `review/`, etc.

> **Non-Java teams (Python / Node / Go / Ruby / .NET / standalone QA team):** use the standalone `karate` CLI (`java -jar` / the launcher) instead — see the **standalone CLI** section below. It is the right on-ramp when Maven / Java tooling isn't part of your stack. This skill otherwise **assumes Java**, and covers **both Maven and Gradle** (the Gradle equivalent follows the Maven quick start below).

## Mental model

```
   src/test/
   ├── java/example/RunTests.java        ← JUnit entry point (boilerplate)
   ├── resources/example/
   │   ├── hello.feature                  ← your test
   │   └── karate-config.js (optional)    ← per-env config (baseUrl, tokens)
   └── (target/)
       └── karate-reports/                ← HTML reports + karate-events.jsonl
```

- The `.feature` files are the tests.
- The Java entry point exists only because Maven Surefire needs a class. JUnit 5 finds the class, the `Karate.run()` method walks `src/test/resources/` for `.feature` files.
- `karate-config.js` runs once per feature; it returns a map that becomes top-level variables.
- Reports land in `target/karate-reports/` (HTML) + `target/karate-json/karate-events.jsonl` if `outputJsonLines(true)`.

## Quick start

### 1. Scaffold

```
my-tests/
├── pom.xml
├── src/test/java/example/RunTests.java
└── src/test/resources/example/hello.feature
```

### 2. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>example</groupId>
  <artifactId>my-tests</artifactId>
  <version>1.0.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <karate.version>2.1.0</karate.version>
    <junit.version>6.1.0</junit.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.karatelabs</groupId>
      <artifactId>karate-junit6</artifactId>
      <version>${karate.version}</version>
      <scope>test</scope>
    </dependency>
    <!-- karate 2.x (karate-junit6) declares JUnit as `provided`, so you pick the version — add it explicitly -->
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>${junit.version}</version>
      <scope>test</scope>
    </dependency>
    <!-- optional but recommended: an SLF4J backend so Karate's console + HTTP logs show up
         (without one you get a "No SLF4J providers were found" notice and silent runs) -->
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.6.3</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <testResources>
      <testResource>
        <directory>src/test/resources</directory>
      </testResource>
      <testResource>
        <directory>src/test/java</directory>
        <excludes>
          <exclude>**/*.java</exclude>
        </excludes>
      </testResource>
    </testResources>
    <plugins>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.2</version>   <!-- 3.2.5+ required for the JUnit platform karate-junit6 uses -->
      </plugin>
    </plugins>
  </build>
</project>
```

The dual `testResources` config lets `.feature` files live next to `.java` files (a common convention).

> **v1 → v2 coordinates changed.** The 2.x JUnit artifact is **`karate-junit6`** (not `karate-junit5`, which
> stops at the 1.x line), and it no longer bundles JUnit — add `org.junit.jupiter:junit-jupiter` yourself. The
> runner import is `io.karatelabs.junit6.Karate` and its method now returns `Iterable<DynamicNode>`. Full details:
> <https://github.com/karatelabs/karate/blob/master/docs/MIGRATION_GUIDE.md>.

### 3. `RunTests.java`

```java
package example;

import io.karatelabs.junit6.Karate;
import org.junit.jupiter.api.DynamicNode;

class RunTests {

    @Karate.Test
    Iterable<DynamicNode> testAll() {
        return Karate.run().relativeTo(getClass());
    }
}
```

### 4. `hello.feature`

```feature
Feature: hello world

Scenario: status check
  Given url 'https://httpbin.org'
  When path 'get'
  And method get
  Then status 200
  And match response.url == 'https://httpbin.org/get'
```

### 5. Run

```bash
mvn test
```

Reports land in `target/karate-reports/karate-summary.html`.

### Prefer Gradle?

Same `RunTests.java` + `hello.feature` (steps 3–4); swap `pom.xml` for a `build.gradle`. Verified with Gradle 8.6
on JDK 21 against karate `2.1.0`:

```gradle
plugins { id 'java' }
repositories { mavenCentral() }

dependencies {
    testImplementation platform('org.junit:junit-bom:6.1.0')
    testImplementation 'io.karatelabs:karate-junit6:2.1.0'
    testImplementation 'org.junit.jupiter:junit-jupiter'          // karate-junit6 declares JUnit `provided` — add it
    testRuntimeOnly    'org.junit.platform:junit-platform-launcher'
    testRuntimeOnly    'ch.qos.logback:logback-classic:1.6.3'    // optional: enables Karate's console/HTTP logs
}

// let .feature files live next to the Java runner (the Maven `testResources` equivalent)
sourceSets.test.resources.srcDirs += 'src/test/java'

test { useJUnitPlatform() }
```

```bash
gradle test        # or ./gradlew test
```

Reports land in `build/karate-reports/karate-summary.html` (Gradle's build dir), JUnit XML in `build/test-results/test/`.

### 6. Already have a karate project? Upgrade it in place

An existing OSS karate repo needs **no build change** to start using karate-agent — no dependency, no `pom.xml` edit, no change to how CI runs your tests. Add `karate-boot.js` at the **repo root** (beside `pom.xml`, not under the resource dir — the agent looks for it at the launch dir):

```js
boot.classpath('src/test/resources');   // or 'src/test/java' — wherever karate-config.js lives
const cov = boot.ext('coverage');
cov.openapi = 'classpath:api/openapi.yaml';
```

…then run the features you already have through the agent JAR, or serve the folder and drive it from the console:

```bash
java -jar ~/karate-agent-<version>.jar run src/test/java/app/api
java -jar ~/karate-agent-<version>.jar serve
```

Use `classpath:` (not `/`) for a project with a build — a build flattens resources, so the JVM lane and the served lane have different roots and only `classpath:` names one file in both. Full procedure: `Help.page('install')` §"Upgrading an existing karate project"; the path rule: `Help.page('paths')`.

### 7. Wire karate-agent dashboard (when ready)

```bash
KARATE_AGENT_URL=http://localhost:4444 mvn test
```

The run now also POSTs JSONL events to the dashboard. Browse `http://localhost:4444/runs` to see it.

## Reading env vars and system properties

Inside `karate-config.js`:

```javascript
function fn() {
  return {
    baseUrl:    karate.sysenv('API_BASE_URL', 'http://localhost:8080'),  // OS env var with default
    apiKey:     karate.sysprop('api.key'),                               // -Dapi.key=… (returns null if unset)
    dbPassword: karate.sysenv('DB_PASSWORD')
  };
}
```

`karate.sysenv(name, [default])` reads OS env vars; `karate.sysprop(name, [default])` reads `-D` JVM system properties. The optional second arg is the fallback when the variable is unset or empty.

Inside `karate-boot.js` (Plugin SPI startup script): same APIs, prefixed `boot.`: `boot.sysenv(...)`, `boot.sysprop(...)`.

**`karate-boot.js` activates the extensions a run needs — create it (project root) if your project doesn't have one.** karate auto-loads it from the working dir, so it lights the in-run globals before any feature/check runs:
```js
// karate-boot.js — boot the exts this project uses
const rules = boot.ext('rules');   rules.home = 'rulebooks';   // binds Check / Rule / match in features
boot.ext('robot');                                              // binds `bot` for UI checks (lazy — no browser until Robot.create)
const cov = boot.ext('coverage'); cov.openapi = 'openapi.yaml';    // openapi coverage source
cov.rules = 'rulebooks'; cov.requirements = 'requirements';     // rule + @req= coverage land in the RTM
```
Without it, a `.js` check run as a suite (`Runner.run('checks/x.js')`) fails *"Check is not defined"*, and `@req=` coverage never reaches `Requirement.matrix()`. Only add the `boot.ext(...)` lines for the surfaces you actually use.

## Standalone CLI (no Maven)

If you don't have or don't want Maven on the machine, use the `karate` standalone CLI (`karate-pom.json`, subcommands, CI integration, non-Java workflows). Brief version:

```bash
# Install
brew install karatelabs/tap/karate                  # macOS / Linux
curl -fsSL https://karatelabs.io/install.sh | bash  # cross-platform

# Run
karate run hello.feature
karate run -e prod hello.feature        # with karate.env
karate run -t @smoke hello.feature      # tag filter
```

The CLI reads `karate-pom.json` in the working directory for project config (paths, tags, threads, output dir).

> ⚠️ **Do NOT download `karate-<version>.jar` from GitHub releases to run these tests.** The well-known
> standalone `karate-1.x.jar` (e.g. `karate-1.4.1.jar`) is the **old `com.intuit.karate` 1.x engine** — a
> different, unsupported product with **no coverage / requirements / rules / traceability**, and it hangs on
> current JDKs (it predates Java 25). This platform is **karate 2.x** (`io.karatelabs`, Java 21+), and you almost
> certainly **already have the engine** — the `karate-agent` JAR *is* a full 2.x karate CLI (a superset: same
> `karate run`/`mock` plus `serve`/`launch`/`mcp`). To run tests:
> - **Have the karate-agent JAR?** Run it directly as a separate process — no separate download needed:
>   `java -jar karate-agent-<v>.jar run <feature-or-folder>` (it defaults to `run`, so `java -jar karate-agent-<v>.jar
>   <folder>` also works), or `java -jar karate-agent-<v>.jar launch suite.karate.js` for a whole suite → one report.
> - **Have the karate-agent container running?** Run *in* it — `Runner.run('<feature>')` over `/api/eval` or MCP, or
>   `docker run … karate-agent launch suite.karate.js`. If your repo is outside the container's `/work` mount, re-mount
>   the repo (or its parent) as `/work` rather than running outside the product — see `Work.help()`.
> - **No engine at all?** Install the 2.x `karate` CLI above (self-updating, bundles its own JVM). Never the OSS 1.x jar.

## IDE setup

| IDE | Plugin | Notes |
|---|---|---|
| **IntelliJ IDEA** | Karate plugin (JetBrains marketplace) | Syntax highlighting, run individual scenarios, debugger. |
| **VS Code** | Karate Labs extension | Same surface; integrates with `karate-vscode` v3 (real-time results panel). |
| **Cursor / Claude Code** | None needed | Use the LLM directly; the skill content is the IDE-equivalent. |

## CI integration

### GitHub Actions

```yaml
name: tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - name: Run Karate
        env:
          KARATE_AGENT_URL: ${{ secrets.KARATE_AGENT_URL }}
          KARATE_AGENT_TOKEN: ${{ secrets.KARATE_AGENT_TOKEN }}
        run: mvn -B test
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: karate-reports
          path: target/karate-reports/
```

> **On Gradle?** Swap `run: mvn -B test` for `run: ./gradlew test` and the artifact `path:` to
> `build/karate-reports/`. Same for the Jenkins / GitLab blocks below (`sh './gradlew test'` / `- ./gradlew test`).

### Jenkins (declarative)

```groovy
pipeline {
  agent any
  environment {
    KARATE_AGENT_URL = 'http://karate-agent:4444'
  }
  stages {
    stage('test') {
      steps {
        sh 'mvn -B test'
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/karate-reports/**', fingerprint: true
          junit 'target/surefire-reports/*.xml'
        }
      }
    }
  }
}
```

### GitLab CI

```yaml
test:
  image: maven:3-eclipse-temurin-21
  variables:
    KARATE_AGENT_URL: $KARATE_AGENT_URL
  script:
    - mvn -B test
  artifacts:
    paths:
      - target/karate-reports/
    when: always
```

## Standing rules

1. **Pick the right next skill.** REST APIs → `Skill.help('api')`. gRPC → `Skill.help('grpc')`. Kafka → `Skill.help('kafka')`. Existing-test review → `Skill.help('review')`. Assertion strategy → `Skill.help('assertions')`.
2. **Run locally before pushing to CI.** A green `mvn test` on the dev box de-risks the CI debug loop.
3. **Set `KARATE_AGENT_URL` early.** Even on day one — the dashboard becomes useful the moment more than one run exists.
4. **Use JDK 21+.** karate-core v2 requires modern JDK; older builds will surface `UnsupportedClassVersionError`.
5. **One `Feature:` per `.feature` file.** A `.feature` file is one logical TCM record. Don't bundle.
6. **Project layout**: `.feature` files under `src/test/resources/`, one Java runner. The Java runner is plumbing; the test surface is markdown-adjacent.
7. **Path references use the project-root `/`-form.** In any JS — `karate-config.js`, `read()`, `callSingle()`, `karate.start({ mock })`, `cov.*`, `rules.home` — anchor file refs at the project root with a leading `/`: `read('/data/x.json')`, `callSingle('/mock/start.js')`, `karate.start({ mock: '/mock/api.feature' })`. A leading `/` is **always** the project root (webapp context-path style) — never a host path, even one that exists — so the ref resolves **identically** from a feature run, a config-eval (`Runner.config` / the explore harness), and the coverage sources, on every machine. Resolution never probes the filesystem: one spelling, one file. A bare or `../` path is **feature-dir-relative** inside a feature — handy for an asset next to a feature, but it breaks the moment the same code is evaluated outside one (config-eval, where `../mock/x` overshoots the project). Need the feature's own dir explicitly? Use `this:x` (feature-only — it is rejected in a config/JSAPI reference). **Prefer `/`-rooted; relative is discouraged.** `file:` is a real host path. `classpath:` asks the Java classloader **first**, then falls back to the project's resource root — so it keeps working for a Java project's committed refs; a Java project declares where its resources live with one line in `karate-boot.js`: `boot.classpath('src/test/resources')`. (Full rule: `Help.page('paths')`.)

## Anti-patterns

1. **Anti-pattern:** writing tests as a giant single Scenario with 50 steps. Split into smaller Scenarios under one Feature.
2. **Anti-pattern:** `*.java` files driving Karate via the embedded API for every test. The JUnit runner exists so you don't have to. Use the embedded API only when integrating Karate as a library (perf harness, custom orchestrator).
3. **Anti-pattern:** committing generated report dirs to git. In a git-first karate-agent project (git is the source of truth), **gitignore both `target/` and `runs/`** — `target/` (incl. `target/karate-reports`) is the CLI / `karate launch` report dir; `runs/` (`runs/<id>/`) is the served-console / agent per-run evidence store (report + screenshots + video). Neither is source; both regenerate on every run.
4. **Anti-pattern:** hard-coding hostnames in features. Always via `karate-config.js`.
5. **Anti-pattern:** feature-dir-relative file refs in shared JS (`callSingle('../mock/start.js')`, `read('../../data.json')`). They silently overshoot the project root when evaluated outside a feature (config-eval). Use the `/`-rooted form (standing rule 7).
5. **Anti-pattern:** running Karate with a different JDK than you compile with. Set `<maven.compiler.release>21</maven.compiler.release>` and use `actions/setup-java@v4` with the matching version.

## Cross-skill links

- `Skill.help('api')` — next stop for REST work.
- `Skill.help('grpc')` — gRPC.
- `Skill.help('kafka')` — Kafka.
- `Skill.help('assertions')` — match operator.
- `Skill.help('review')` — improve existing feature files.
