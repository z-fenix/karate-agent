# Karate gRPC — Skill

> **Status:** the synchronous-RPC companion to `Skill.help('kafka')`; same channel-SPI mental model.
> **Stack:** karate-core + the **karate-grpc** leaf ext (`io.karatelabs:karate-grpc`, project-versioned). grpc-java 1.76+, protobuf 4.33+, JDK 21+.
> **Prerequisites:** `Skill.help('getting-started')`, `Skill.help('assertions')`, and ideally `Skill.help('api')` (gRPC reuses REST scenario ergonomics).
> **Companion docs:** karate-grpc README (`../karate-grpc/README.md`), the `grpc` demo kit (`agent-seed/projects/grpc/`).

## What this skill is for

Author **end-to-end gRPC tests** with the same `.feature` ergonomics as REST. The skill teaches an LLM to:

1. Configure a gRPC session via `karate.channel('grpc')`.
2. Discover a service two ways — **a `.proto` file** (classpath path), or **server reflection** (no `.proto` at
   all, shapes fetched off the live server) — then pick service + method by name.
3. Cover all four RPC kinds: **unary**, **server-streaming**, **client-streaming**, **bidi-streaming**.
4. Send metadata (auth headers, request-id), capture response metadata.
5. Match decoded protobuf as JSON using the standard `match` operator.
6. Wire TLS (server-cert) and mTLS (client-cert + server-cert).

## Two surfaces — probe live, then crystallize

1. **`Grpc.*` — the live namespace (explore over MCP / `/api/eval`).** When the engine runs off the
   **async** jar (served console), `Grpc.connect(opts)` returns a live handle you keep in a `var` and drive
   with **one verb**: `g.call(method, message)`. The streaming mode is read from the descriptors, so there is
   nothing to set. Use this to learn a real service's shapes *before* writing any test.

   **Ask the user how to reach their service — either is enough:** a `.proto` file path, **or** just a
   reflection-enabled `host:port` (most servers expose `grpc.reflection.v1.ServerReflection`). With a
   `proto` you read it locally; with **no `proto`** the shapes are fetched off the live server — `g.methods()`,
   `g.describe()`, `g.call()` behave identically either way. `proto:` always wins (no reflection call), so pass
   it when you have it; omit it to discover via reflection. If reflection is off/unreachable, the error says so
   and points back to `proto:`.
   ```
   // (a) from a .proto:
   var g = Grpc.connect({ host:'localhost', port:50051,
                          proto:'proto/hello.proto', protoRoots:['.'], service:'HelloService' })
   // (b) NO proto — discover via server reflection:
   var g = Grpc.connect({ host:'localhost', port:50051, service:'HelloService' })

   g.methods()                                // the catalog — call FIRST to stop guessing names/shapes
   g.describe('HelloRequest')                 // field-level shape of a request/response message
   g.call('Hello', { name:'Ada' })            // unary         -> { message:'hello Ada' }
   g.call('LotsOfReplies', { name:'Ada' })    // server-stream -> [ … ]   (blocking, timeout-bounded)
   g.call('LotsOfGreetings', [a, b])          // client-stream -> single response  (array arg ⇒ stream in)
   g.call('BidiHello', [a, b, c])             // bidi          -> [ … ]
   g.call('Hello', { name:'J' }, { metadata:{ authorization:'x' } })   // optional per-call opts
   ```
   Read-only state: `g.exchanges` (captured calls), `g.metadataResponse` (last trailers). `Grpc.help()` is
   the on-ramp. `g` is `kind: grpc`. Reflection happens once at connect (cached for the handle's life), and is
   the live **authoring** path — see the coverage note: the durable `cov.grpc` universe still needs a `.proto`.

2. **`karate.channel('grpc')` — the durable feature DSL (lock it in).** Crystallize what you learned into a
   `.feature`/`.js` suite that runs in CI and produces a coverage report. This is the rest of the skill.

## Mental model (the feature DSL)

karate-grpc registers itself as a **channel** in karate-core's pluggable channel system (the same SPI Kafka and WebSocket use). You ask karate for a gRPC channel, configure connection + proto + service + method as properties, then `send()` / `collect()` / `pop()`.

```
   karate.channel('grpc')                  ← get a session
     ↓
   session.host = 'localhost'              ← connection
   session.port = 9000
   session.proto = '/hello.proto' ← schema
   session.service = 'HelloService'        ← service + method picked from proto
   session.method = 'Hello'
   session.metadata = { ... }              ← request headers (optional)
   session.trustCert = '...'               ← TLS (optional)
     ↓
   session.send({ name: 'John' })          ← serialize JSON → protobuf
     ↓
   session.pop()                            ← deserialize protobuf → JSON
     ↓
   match result == { message: 'hello John' }
```

Same matcher as REST; protobuf is a wire-format detail handled by the proto codec.

## Quick start

`hello.proto` (under `src/test/resources/karate/`):

```protobuf
syntax = "proto3";
package karate;

service HelloService {
  rpc Hello (HelloRequest) returns (HelloReply);
  rpc LotsOfReplies (HelloRequest) returns (stream HelloReply);
  rpc LotsOfGreetings (stream HelloRequest) returns (HelloReply);
  rpc BidiHello (stream HelloRequest) returns (stream HelloReply);
}

message HelloRequest  { string name = 1; }
message HelloReply    { string message = 1; }
```

`hello.feature`:

```feature
@grpc
Feature: Hello (gRPC)

Background:
  * def session = karate.channel('grpc')
  * session.host = 'localhost'
  * session.port = 9000
  * session.proto = '/karate/hello.proto'
  * session.service = 'HelloService'

@req=HELLO-001
Scenario: unary call
  * session.method = 'Hello'
  * session.send({ name: 'John' })
  * match session.pop() == { message: 'hello John' }

@req=HELLO-002
Scenario: server-streaming — multiple replies
  * session.method = 'LotsOfReplies'
  * session.count = 3
  * session.send({ name: 'John' })
  * def replies = session.collect()
  * match each replies == { message: '#regex hello John \\d+' }

@req=HELLO-003
Scenario: client-streaming — flush after all sends
  * session.method = 'LotsOfGreetings'
  * session.stream = true
  * session.send({ name: 'John' })
  * session.send({ name: 'Smith' })
  * session.flush()
  * match session.pop() == { message: 'hello [John, Smith]' }

@req=HELLO-004
Scenario: bidi
  * session.method = 'BidiHello'
  * session.stream = true
  * session.count = 3
  * session.send({ name: 'John' })
  * session.send({ name: 'Smith' })
  * session.send({ name: 'Lee' })
  * def replies = session.collect()
  * match replies == '#[3]'
```

## Session property reference

Set as JS properties before `send()` / `collect()`.

| Property | Type | Purpose |
|---|---|---|
| `host` | String | Server hostname. |
| `port` | int | Server port. |
| `proto` | String | Path reference to the `.proto` file — project-root-anchored (`'/karate/hello.proto'`); `classpath:` also works for a proto genuinely on a Java classpath. On the live `Grpc.connect` handle this is **optional** — omit it to discover the service via server reflection. The durable feature DSL below uses a `.proto`. |
| `protoRoots` | List<String> | Optional include dirs for proto imports. |
| `service` | String | gRPC service name as declared in the proto. |
| `method` | String | RPC method name. |
| `stream` | boolean | `true` for client-streaming or bidi; `false` (default) for unary or server-streaming. |
| `count` | int | Expected message count for server-streaming / bidi. `collect()` blocks until count reached or stream closes. |
| `metadata` | Map<String, String> | Request metadata (gRPC headers). Binary metadata uses `-bin` key suffix. |
| `trustCert` | String | Classpath path to CA cert PEM for server verification. Setting *any* TLS property enables TLS. |
| `clientCert` | String | Classpath path to client cert PEM (mTLS). |
| `clientKey` | String | Classpath path to client key PEM (mTLS). |

| Method | Returns | Purpose |
|---|---|---|
| `send(obj)` | void | Send one request message; serializes JSON to protobuf via the loaded descriptor. Implicitly starts the call. |
| `collect()` | List | Block until expected messages arrive (or stream closes); returns all received as a list of JSON objects. |
| `pop()` | Object | Convenience for `collect()[0]`. |
| `flush()` | void | Client-streaming / bidi: signal end-of-send so the server can complete. |

| Captured field | Type | Purpose |
|---|---|---|
| `metadataResponse` | Map<String, List<String>> | Response metadata (headers + trailers) populated after the call. |
| `status` | String | The gRPC status code of the last call (`OK` / `INVALID_ARGUMENT` / …). Lets a feature drive an **error path** and assert it — read it instead of `pop()` (which throws on an empty error response). |
| `statusDetails` | Map | The gRPC **rich-error model** (`google.rpc.Status`: `code` + `message` + `details[]`) when the server attached typed details — `null` otherwise. Assert it with `match` like an HTTP problem body. |

## Error paths — assert the status, and the typed details

A gRPC error is the analog of an HTTP 4xx/5xx. Drive it, then read `session.status` (don't `pop()` — there is
no response to pop). If the server uses the **rich-error model** (a `google.rpc.Status` in the
`grpc-status-details-bin` trailer — `BadRequest`, `ErrorInfo`, `QuotaFailure`, …), `session.statusDetails`
decodes it to JSON so you can assert the specific reason/field, not just the coarse code:

```feature
Scenario: empty name is rejected
  * session.method = 'Hello'
  * session.send({ name: '' })
  * match session.collect() == []
  * match session.status == 'INVALID_ARGUMENT'
  # rich details (present only if the server sends them):
  * match session.statusDetails contains { code: 3, message: 'name is required' }
  * match session.statusDetails.details[1] contains { reason: 'NAME_REQUIRED', domain: '#string' }
```

Each `details[]` entry carries an `@type` (e.g. `type.googleapis.com/google.rpc.ErrorInfo`) plus that type's
fields. `statusDetails` is `null` for a code-only error — `session.status` is enough there.

## Configuration knobs — central, in `karate-boot.js`

Suite-wide defaults (host/port/protoRoots/tls) ride the booted ext, set **once** in `karate-boot.js` (there is
no `configure grpc` global). Per-session properties (`service`/`method`/per-call `host`) still override in the feature.

> `boot.ext('grpc')` is a **load-time** (`karate-boot.js`) construct only — it is **not** a runtime global, so calling `boot.ext('grpc')` over `/api/eval` fails with `boot is not defined`. To inspect or override config at runtime, set per-session properties on the channel instead.

```javascript
// karate-boot.js
var grpc = boot.ext('grpc');
grpc.host = boot.sysprop('grpc.host', 'localhost');
grpc.port = boot.sysprop('grpc.port', '50051');
grpc.protoRoots = ['.'];
// grpc.tls = { trustCert: '...', clientCert: '...', clientKey: '...' }   // TLS / mTLS defaults
```

A session created with `karate.channel('grpc')` is seeded with these defaults at creation; scenarios then set only `service`/`method` (+ proto if it varies).

## TLS

### Server-side TLS (server presents a cert; client verifies)

```feature
* def session = karate.channel('grpc')
* session.host = 'grpc.example.com'
* session.port = 443
* session.proto = '/karate/hello.proto'
* session.service = 'HelloService'
* session.method = 'Hello'
* session.trustCert = '/certs/ca.pem'               # CA that signed the server cert
* session.send({ name: 'John' })
* match session.pop() == { message: 'hello John' }
```

### mTLS (client presents a cert too)

```feature
* session.trustCert = '/certs/ca.pem'
* session.clientCert = '/certs/client.pem'
* session.clientKey = '/certs/client.key'
```

Plaintext is the default — TLS is enabled the moment *any* cert property is set.

## Metadata (gRPC headers)

```feature
* session.metadata = {
    'authorization': 'Bearer ' + token,
    'x-request-id': karate.uuid(),
    'x-tenant': 'acme'
  }
* session.send({ name: 'John' })

# After the call, response metadata is available
* match session.metadataResponse['x-server-version'] == '#string'
```

Binary metadata uses the `-bin` suffix convention (`x-custom-bin`).

## Streaming patterns — when to use which

| RPC kind | `stream` | `count` | Pattern |
|---|---|---|---|
| Unary | false (default) | not used | `send` + `pop` |
| Server-streaming | false | set to expected | `send` + `collect` |
| Client-streaming | true | not used | multiple `send` + `flush` + `pop` |
| Bidi | true | set to expected | multiple `send` + `collect` |

Server-streaming `count` doesn't mean "stop after N" — the server controls when the stream ends. `count` is the assertion: `collect()` waits until *at least* `count` messages have arrived (or the stream closes early).

## Standing rules

1. **JSON-first**: write payloads as JSON; the proto codec handles protobuf conversion. Protobuf is a wire detail.
2. **Same matcher discipline as REST**: `#number`, `#? _ > 0`, `match contains`. Don't over-pin.
3. **Set `service` + `method` per scenario, not per send.** Reduces error surface.
4. **For server-streaming, always set `count`** before `send`. Without it, `collect` won't know when to stop.
5. **For client-streaming and bidi, always `flush()`** after the last `send` — otherwise the server may hang.
6. **Use `protoRoots` for multi-file protos.** `proto = '/karate/hello.proto'`, `protoRoots = ['/karate']`.
7. **Plaintext for local dev; TLS for staging/prod.** Set `trustCert` only when calling secured endpoints.
8. **Tag with `@grpc`** alongside `@req=…` so the dashboard / Runs rail can slice gRPC scenarios.

## Anti-patterns

1. **Anti-pattern:** asserting on the binary protobuf wire format. Match on the decoded JSON.
2. **Anti-pattern:** forgetting `flush()` on client-streaming — the test hangs until the call deadline, then fails opaquely.
3. **Anti-pattern:** `count` mismatched with what the server emits. If the server emits 5 messages and `count = 3`, `collect` returns 3 — but later sends arrive on a now-dead session. Set `count` to the exact expected, or use `'#[_ >= 3]'` matcher on `collect()` output if you only care about a minimum.
4. **Anti-pattern:** wiring TLS on a plaintext local server. Set `trustCert` only when needed; misconfigured TLS is a confusing failure mode.
5. **Anti-pattern:** hard-coding ports. Use `karate-config.js` env switching.

## Maintenance — proto breaking-change diff (`Grpc.diff`)

`Grpc.diff({ old, new, protoRoots? })` compares two `.proto` versions and classifies every change — a
no-server CI/maintenance check (the gRPC analog of OpenAPI spec-diff):

```
Grpc.diff({ old:'proto/order-v1.proto', new:'proto/order-v2.proto' })
// -> { ok:false,
//      summary:{ breaking:3, warnings:1, compatible:2 },
//      breaking:[{ kind:'FIELD_REMOVED', level:'BREAKING', path:'Order.id', detail:'…' }, …],
//      warnings:[{ kind:'FIELD_RENAMED', path:'Order.1', detail:'id -> orderId …' }],
//      compatible:[{ kind:'FIELD_ADDED', path:'Order.locale', … }] }
```

Matched by **wire identity** — fields + enum values by number, methods + messages by name — so a rename is
caught (same number, new name → `WARNING`: wire-compatible, breaks JSON/codegen) and a removed/renumbered
field or a dropped/streaming-changed RPC is `BREAKING`. Additive changes are `COMPATIBLE`. `ok` is `true` iff
there are no breaking changes — gate a release on `Grpc.diff(...).ok`.

## Cold start — scaffold a runnable project from a proto (`Grpc.scaffold`)

`Grpc.scaffold({ proto, out?, feature? })` turns a `.proto` into a runnable gRPC coverage project — the gRPC
analog of `Openapi.scaffold` (no server needed to *generate*; the generated feature runs against a **live**
server, since there is no gRPC mock yet):

```
Grpc.scaffold({ proto:'proto/hello.proto', out:'work/grpc' })
// -> writes karate-boot.js (boot the grpc ext + cov.grpc) + checks/<name>.feature
//    (one scenario per RPC, sampled request, send + pop/collect by streaming type),
//    and copies the proto in (self-contained). Omit `out` to get the `files` map back.
```

Then start the server and `karate run checks/<name>.feature` (`boot.ext('coverage')` auto-enables the
event stream the grpc coverage join reads). Replace the smoke scenarios with
intent-bearing `@req=ID/criterion` checks that assert the responses; untested methods stay honest gaps.
Run `Grpc.help()` for the live verb reference.

## Advanced surface

- **`session.config = { ... }`** — bulk-set multiple properties in one assignment.
- **`waitForPort` before send** — `* karate.waitForPort(host, port, 5000)` confirms the server is up before kicking off.
- **gRPC service mock** — planned, not shipped. Test gRPC against a live server (the demo `HelloServer`).
- **Coverage for proto** (built): point `cov.grpc` at a `.proto` file, a **directory** (every `*.proto` in
  it), or a list of paths in `karate-boot.js` — the `.proto` IS the gRPC method universe (service × method),
  the analog of an OpenAPI spec. A run's gRPC traffic joins by `service/method`, so the Coverage report shows
  which methods were exercised + the OK/error status histogram, with untested methods as honest gaps
  (`Coverage.gaps()`). Real services span many `.proto` files — the dir/list form feeds them all in.
  **Error-path (outcome) coverage** rides along zero-authoring: each method is also graded happy-only vs
  error-tested (did the run exercise any non-OK status), surfaced as the source's `errorPaths` rollup +
  `Coverage.gaps().happyOnly` — so "tested, but only on the success path" is a visible gap, not a green pass.
  **Note — reflection vs coverage:** server reflection (above) powers *live authoring* only; `cov.grpc` is
  build-time and still wants a `.proto` (a directory/list of them) for the declared universe. So a fully
  proto-free flow probes + crystallizes tests via reflection, but add a `.proto` to `cov.grpc` to get the
  method-coverage report. (Sourcing the coverage universe from a captured reflected descriptor is a later option.)
- **Custom interceptors**: not currently exposed. If you need a client interceptor (e.g. dynamic header per call), capture the use case and we'll surface it.

## Cross-skill links

- `Skill.help('api')` — REST patterns; most Background / config / tagging discipline transfers.
- `Skill.help('assertions')` — `match` semantics; identical for gRPC responses.
- `Skill.help('kafka')` — companion event-streaming skill; same channel SPI.
- `Skill.help('mock')` — HTTP mock servers from feature files (a gRPC mock is planned; test gRPC against a live server).
- `Skill.help('coverage')` — coverage rolls up REST + gRPC into one engine.
