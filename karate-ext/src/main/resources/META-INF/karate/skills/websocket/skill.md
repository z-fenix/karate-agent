# Karate WebSocket — Skill

> **Status:** the long-lived-connection companion to `Skill.help('kafka')` and
> `Skill.help('grpc')`; same channel-SPI mental model, same `WireCodec` serde seam.
> **Stack:** karate-core + the **in-max** `io.karatelabs.ext.websocket` package — it rides core's
> `io.karatelabs.http.WsClient` (Netty transport), so there is **no separate jar**. JDK 21+.
> **License:** WebSocket folds into the **`openapi` web-API bundle** (OpenAPI + AsyncAPI + WebSocket +
> Webhook); a license carrying `openapi` (or the `agent` umbrella) unlocks it. Boot it with
> `boot.ext('websocket')`.
> **Prerequisites:** `Skill.help('getting-started')`,
> `Skill.help('assertions')`.

## What this skill is for

Author **end-to-end WebSocket tests** with the same `.feature` ergonomics as REST/gRPC/Kafka. The skill
teaches an LLM to:

1. Open a session via `karate.channel('websocket')`, connect to a `ws://` / `wss://` endpoint.
2. Send + receive frames, collecting an expected count with a timeout.
3. Pick the wire format with a **`WireCodec`** — raw text/bytes (default), object⇄JSON, or a **custom**
   codec for a bespoke protocol (a STOMP frame, a length-prefixed envelope, …).
4. Add **protocol handshake / teardown / routing** behaviour with the `WebsocketLifecycle` mix-in
   (e.g. send a STOMP `CONNECT` frame on open).
5. Filter inbound frames, send headers, pick a subprotocol, and wire TLS.

> **No live `Websocket.*` namespace yet.** Unlike gRPC/Kafka (which add a `Grpc.*`/`Kafka.*` explore
> surface over MCP), WebSocket today is the **`karate.channel('websocket')` feature DSL only** (the live
> namespace + a `ws-match` coverage embed are roadmap). Author against the channel directly.

## Mental model (the feature DSL)

WebSocket is a **single long-lived, bidirectional session** (not pub/sub like Kafka, not request/reply like
gRPC). You open it, send frames whenever, and the session collects inbound frames asynchronously into a
buffer that `collect()` / `pop()` drain.

```
   karate.channel('websocket')             ← get a session
     ↓
   session.url = 'ws://localhost:8080/ws'  ← connection (ws:// or wss://)
   session.codec = new JsonTextCodec()     ← wire format (optional; default = raw passthrough)
   session.count = 1                        ← how many inbound frames collect() waits for
   session.timeout = 5000                   ← ms ceiling on collect() (optional)
     ↓
   session.start()                          ← open the connection (send() auto-starts too)
   session.send({ type: 'ping' })           ← encode via codec → frame out
     ↓
   session.pop()                            ← block for the next decoded frame (= collect()[0])
     ↓
   match result == { type: 'pong' }
```

The matcher is the same as REST — once a frame **decodes to a map**, `match`/`#string`/`contains` all apply.

## Quick start

### Raw text echo (default codec)

```feature
@websocket
Feature: echo (WebSocket)

Background:
  * def session = karate.channel('websocket')
  * session.url = 'ws://localhost:8080/echo'

@req=WS-001
Scenario: round-trip a text frame
  * session.start()
  * session.send('hello')
  * match session.pop() == 'hello'
  * session.stop()
```

### Object ⇄ JSON (`JsonTextCodec`)

```feature
@websocket @json
Scenario: JSON round-trip
  * def session = karate.channel('websocket')
  * session.url = 'ws://localhost:8080/ws'
  * session.codec = new (Java.type('io.karatelabs.ext.websocket.JsonTextCodec'))()
  * session.start()
  * session.send({ type: 'ping', seq: 1 })
  * match session.pop() == { type: 'pong', seq: 1 }
  * session.stop()
```

### Collect several frames

```feature
Scenario: gather N frames
  * def session = karate.channel('websocket')
  * session.url = 'ws://localhost:8080/echo'
  * session.count = 3
  * session.timeout = 10000
  * session.start()
  * session.send('a')
  * session.send('b')
  * session.send('c')
  * match session.collect() contains 'b'
  * session.stop()
```

> Frame **order is not guaranteed** by the transport — assert on the *set* / `contains`, not on position,
> when several sends are in flight.

## Session reference

Set as JS properties before / between `send()` calls.

| Property | Type | Purpose |
|---|---|---|
| `url` | String | Endpoint, `ws://` or `wss://`. **Required.** |
| `codec` | WireCodec | Wire format. Default `RawCodec` (text/bytes verbatim); set `JsonTextCodec` for object⇄JSON, or a custom codec (below). |
| `count` | int | How many inbound frames `collect()` waits for (default 1). |
| `timeout` | long | ms ceiling on `collect()` — a safety stall-bound, not the happy-path latency (`collect()` returns the instant `count` frames arrive). Omit to block indefinitely. |
| `filter` | function | Drop inbound frames where the predicate returns false (applied to the **decoded** message). |
| `headers` | Map | Handshake request headers (e.g. `Authorization`). Or set one at a time with `session.header(k, v)`. |
| `subProtocol` | String | WebSocket subprotocol (the `Sec-WebSocket-Protocol` offer). |
| `ssl` | boolean \| Map | TLS cert handling. **Default trusts all certs** (handy for self-signed test endpoints); set `ssl = false` (or `{ trustAll: false }`) for strict validation. |

| Method | Returns | Purpose |
|---|---|---|
| `start()` | void | Open the connection. `send()` auto-starts if you didn't. |
| `send(obj)` | void | Encode `obj` via the codec and send one frame. |
| `collect()` | List | Block until `count` frames arrive (or `timeout`); returns the decoded frames, then resets the buffer. |
| `pop()` | Object | `collect()[0]` — the next single decoded frame. |
| `header(k, v)` | void | Set one handshake header (`v = null` removes it). |
| `stop()` | void | Close the session (also runs the codec's `onStop` lifecycle hook if any). |

## Wire formats — `WireCodec` (the serde seam, shared with Kafka)

A `WireCodec` is two methods — `encode(value)` (JSON/object → wire text/bytes) and `decode(wire)` (inbound
text/bytes → JSON/object). Built-ins:

- **`RawCodec`** (default) — passthrough: outbound used verbatim (a String or `byte[]`), inbound delivered as
  the raw text/bytes. Use for plain-text or binary protocols you assert as-is.
- **`JsonTextCodec`** — outbound values are JSON-encoded, inbound text frames parsed to maps. The common case.

For a **bespoke protocol**, implement `WireCodec` and assign it to `session.codec`. Because everything
downstream (`match`, the report, coverage) speaks maps, a custom decode is all it takes to make a proprietary
frame assertable. This is the **same seam Kafka uses** (`register({name, codec})`) — teach a format once,
reuse it on both transports (see the `kafka` skill's *Custom wire formats*).

### Protocol handshakes — the `WebsocketLifecycle` mix-in

A plain codec only does serde. A protocol that also needs **connect/teardown/inbound-routing** behaviour
(STOMP, MQTT-over-WS, a subscribe-then-receive flow) has its codec **also implement `WebsocketLifecycle`**:

| Hook | When | Default |
|---|---|---|
| `onStart(session)` | right after the socket opens | no-op (e.g. send a `CONNECT` frame here) |
| `onStop(session)` | on teardown | no-op |
| `onMessage(session, decoded)` | each inbound frame | `session.receive(decoded)` — into the collector (honours `filter`/`count`); override to route/demux |

The session invokes these only when the codec implements the interface — so it composes cleanly with serde.
A worked STOMP codec (CONNECT → SUBSCRIBE → SEND → MESSAGE, JSON bodies) lives in the test sources
(`StompCodec` / `StompFlowTest`) as the reference pattern.

```feature
# a custom STOMP-style flow (codec implements WireCodec + WebsocketLifecycle)
* def session = karate.channel('websocket')
* session.url = 'ws://localhost:61613/stomp'
* session.codec = new (Java.type('com.acme.StompCodec'))()
* session.start()
* session.send({ command: 'CONNECT', headers: { 'accept-version': '1.2' } })
* match session.pop().command == 'CONNECTED'
```

## TLS

`wss://` enables TLS. Certs are **trusted by default** (test-friendly). For strict validation against a real
CA chain:

```feature
* session.url = 'wss://stream.example.com/ws'
* session.ssl = false          # strict cert validation (or { trustAll: false })
```

## Central config — `karate-boot.js` (`boot.ext('websocket')`)

Suite-wide defaults ride the booted ext; each `karate.channel('websocket')` session is seeded with them at
create time (a per-session value overrides):

```javascript
// karate-boot.js
var ws = boot.ext('websocket');
ws.headers = { Authorization: 'Bearer ' + token };                 // applied to every session (optional)
ws.codec = new (Java.type('io.karatelabs.ext.websocket.JsonTextCodec'))();   // default codec (optional)
```

> `boot.ext('websocket')` is a **load-time** construct (`karate-boot.js`) only — not a runtime global. Set
> per-session properties on the channel to override at runtime.

## Connect diagnostics

A failed connect throws a **structured** error — host:port, TLS mode, subprotocol, the offered header keys,
and a targeted hint (HTTP 101 not returned ⇒ wrong path/subprotocol/auth; connection refused ⇒ wrong
scheme/port or server down; TLS handshake failed ⇒ `ws://` vs `wss://` mismatch). Read it before guessing.

## Coverage

There is **no native `cov.websocket`** source yet. To score WebSocket channels today, declare them in an
**AsyncAPI** doc and register it with `cov.asyncapi` — AsyncAPI is the declared event-side universe and covers
WebSocket channels as well as Kafka topics (see the `coverage` and `scaffold-from-asyncapi` skills). A
first-class `cov.websocket` + a `ws-match` traffic embed are roadmap.

## Standing rules

1. **Always `stop()`** the session (or rely on the channel's after-scenario cleanup) — a leaked connection
   keeps the Netty client open.
2. **Set `count` to the exact expected** before the sends that produce those frames; use `timeout` as a
   stall-bound so a hung server fails fast instead of blocking forever.
3. **Assert on the decoded map, not the wire** — pick `JsonTextCodec` (or a custom codec) and `match` the
   structure; don't string-parse frames in the feature.
4. **Order is not guaranteed** — assert with `contains` / on the set when multiple frames are in flight.
5. **`wss://` for anything real; `ws://` for local.** Keep `ssl` default (trust-all) only for test endpoints.
6. **Tag with `@websocket`** (+ `@json` / a protocol tag) so the dashboard can slice WS scenarios.

## Anti-patterns

1. **Anti-pattern:** `collect()` with no `timeout` against a server that may never send `count` frames — the
   test hangs. Set a deliberate `timeout`.
2. **Anti-pattern:** asserting frame order across concurrent sends. The transport interleaves; assert the set.
3. **Anti-pattern:** hand-parsing a protocol in the feature. Write a `WireCodec` (+ `WebsocketLifecycle` for
   handshakes) once and assert maps.
4. **Anti-pattern:** leaving `ssl` trust-all against production. Set `ssl = false` for strict validation.
5. **Anti-pattern:** reusing one session across scenarios. New `karate.channel('websocket')` per scenario.

## Cross-skill links

- `Skill.help('kafka')` — companion event skill; the **same `WireCodec` seam** (`register({name, codec})`).
- `Skill.help('grpc')` — companion streaming skill; same channel SPI.
- `Skill.help('assertions')` — `match` semantics on the decoded frame.
- `Skill.help('scaffold-from-asyncapi')` / `Skill.help('coverage')` — declare WebSocket channels via AsyncAPI for coverage today.
