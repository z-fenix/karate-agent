You drive **karate** over one HTTP endpoint: send JavaScript, get JSON back. **Call a verb by name and
end with the value you want** — e.g. `Coverage.gaps()`, `Requirement.readiness()`. (Over MCP, that JS is the
argument to the single `karate_eval` tool.)

This console works **live and in-run** — inspect and author tests, requirements, coverage, mocks, and
API specs; stand up and drive live handles; read the report. Whatever the task — troubleshoot a failure,
improve coverage, author or scaffold tests — you **discover the relevant namespaces and compose them**. (A
full file-based `karate run` suite is a separate path; this console drives and inspects.)

## Single-user: one run, one browser, one container

This container is **single-user by design** — **one** persistent JS engine and **one** observable browser
(one display / video / noVNC), so **one run at a time**. Starting a second live or autonomous run
while one is in flight is **refused** with a clear error — finish or stop the current one (`Harness.stop()`
/ `bot.destroy()`) first, or run parallel work in a **second container**. The container is the tenant
boundary; there is no multi-user mode. `Harness.status()` tells you the one run in flight (loop or live).

## The engine is your memory

The server holds **one persistent JS engine** (the session). Every top-level `var`, `function`, and
assignment you make **survives into your next call** — same scope, same heap. So:

- **Capture live things in a var and reuse them** — `var mock = Http.mock(...)`, then `mock.url`,
  `mock.reset()` (clear the call log + the store) or `mock.restart()` (back to the state it booted in, same
  port) later. Don't recreate; keep the handle alive and tweak it across the task.
- **Build a helper once, call it many times** — define a function in one call, use it in the next.
- `Engine.vars()` lists what you're holding (handles, helpers, data) — use it to recall your state. (To
  read one back, just evaluate its name — end a call with `mock`.) A *run* is an execution
  (`Harness.runs()` / the `/runs` page), not this memory.

**The one exception, and it bites hard: a LIVE RUN settling clears every var you hold — including the ones
you defined before it started.** Bindings are working memory scoped to a live run, so when one ends —
`Harness.stop()`, a bot's `destroy()`, a loop finishing — *all* of them go, not just what the run created
(namespaces and `help()` stay).
`Harness.stop()` says so in its result (`varsCleared`); without that you would meet it as a `ReferenceError`
two calls later. So **read out anything you want to keep BEFORE you stop**, and re-assign it after.

## Every call returns the same envelope

`{ "command": "result" | "error", "payload": ... }` — and **success needs both halves checked**. `command`
says only whether the JavaScript ran (`"error"` = it threw). A verb that **refused** still ran: it answers
`command: "result"` with `payload: {error: "<what to fix>", code: "<CODE>", status, hint?}` — data, never an exception.
So return a verb's whole result and read `payload.error` before any field: a field read off a refusal
(`Noun.verb(…).counts`) is `null`, and the refusal is lost.

When a result is a **live handle**, the envelope adds `"ref": { "kind": "...", "verbs": [...] }`. That means
`payload` is a *snapshot* of an object that persists in the run — keep its var, call those `verbs` on it,
re-read it to see current state.

## Discover, then compose (pull what you need)

You are **not** given the full API up front — you pull it on demand:

- `help()` → the **menu**: each namespace's name, one-line purpose, and verbs.
- `Noun.help()` → one namespace's verbs + detail (e.g. `Openapi.help()`, `Http.help()`).
- `Skill.list()` / `Skill.help(name)` → **composite workflows** (multi-step recipes) — match your task to
  one when it fits, rather than improvising the steps.
- `Help.get('<help-id>')` → the product **Help/datasheet** for a concept; `Help.topics()` lists the ids.
  If the user pastes a dotted **help-id** slug (e.g. `rule.execute`, `model.coverage.oracleonly` — these are
  copied from a "?" on a page or an offline report), it's a Help lookup: call `Help.get(id)` and answer in
  context from its markdown. To point the user's own screen at it, call `Help.surface(id)` (opens `/help#id`).
- `Engine.vars()` → your working memory.
- `Work.current()` → **which project this server is pointed at** (`{name, root, …}`) — confirm it's the one you were launched for; if you can see your own working directory, compare its folder name to `Work.current().name`, and on a mismatch `Work.list()` then `Work.use('<name>')` to re-anchor before you start.
- `Runner.target()` → **the app under test**: `{url, reachable, …}` from the project's `appUrl` config for the active env. Use its `url` rather than guessing a host.
- `File.ls()` → **what is actually in the project** — one directory level (`File.ls('requirements')` to descend, `File.list('**/*.md')` to glob, `File.read(path)` to open). Look before you ask: the brief, spec or policy document you need is usually already on disk.

Then **compose** these namespaces + skills yourself to do the task. The task comes from the user; your job
is to discover the pieces and assemble them.

## Shell mechanics

Send the JavaScript to **`POST /api/eval`** on this same server — the host:port you fetched this guide from
(shown below as `HOST`). Over MCP it's the `code` argument of the single `karate_eval` tool instead.

- Inline: `curl -s HOST/api/eval -d 'Coverage.gaps()'`
- Multi-line: `curl -s HOST/api/eval --data-binary @- <<'EOF' … EOF`
- A call returns the value of its **last expression** — end a block with the thing you want back
  (`var x = f(); x`), not just an assignment. For several results, make several calls.
