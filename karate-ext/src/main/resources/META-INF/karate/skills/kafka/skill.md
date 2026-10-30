# Karate Kafka — Skill

> **Status:** the companion event-streaming skill to `Skill.help('grpc')`. Shares the channel-SPI mental model.
> **Stack:** karate-core + the **karate-kafka** leaf ext (`io.karatelabs:karate-kafka`, project-versioned). Confluent Avro + Protobuf serializers, JSON registry-free; JDK 21+.
> **Prerequisites:** `Skill.help('getting-started')`, `Skill.help('assertions')`. Familiarity with Kafka producer/consumer semantics helps.
> **Companion docs:** karate-kafka README (`../karate-kafka/README.md`), the `kafka` demo kit (`agent-seed/projects/kafka/`).

## What this skill is for

Author **tests against Kafka-based systems** with the same `.feature` ergonomics as REST/gRPC. The skill teaches an LLM to:

1. Configure a Kafka channel via `karate.channel('kafka')`.
2. Produce + consume messages on a topic.
3. Choose the serialization: plain JSON, Avro (via schema registry), or Protobuf.
4. Register schemas (`.avsc` / `.proto`) at runtime.
5. Set connection config (broker, schema registry, SASL/SSL) **once in `karate-boot.js`** via `boot.ext('kafka')` (the old `configure kafka` global is removed).
6. Use headers, keys, partitions where supported.
7. Pair Kafka events with REST/gRPC calls in a single end-to-end scenario (produce a REST request → assert downstream Kafka event landed).

## Two surfaces — probe live, then crystallize

1. **`Kafka.*` — the live namespace (explore over MCP / `/api/eval`).** When the engine runs off the
   **async** jar (served console), `Kafka.connect(opts)` returns a live handle you keep in a `var`.
   `consumer()`/`producer()` return their own handles whose **verb-args** `start({…})`/`send({…})` take
   their inputs as args. Start the consumer first so it doesn't miss the message.
   ```
   var k = Kafka.connect({ bootstrap:'127.0.0.1:29092', schemaRegistry:'http://localhost:8081' })
   k.register({ name:'hello', path:'hello.avsc' })            // optional — Avro/Protobuf

   var c = k.consumer(); c.start({ topic:'test', count:1 })   // non-blocking subscribe
   var p = k.producer(); p.send({ topic:'test', key:'k1', value:{ message:'hi' } })
   c.collect()            // blocking drain w/ timeout -> [{ offset, key, headers, value }]
   k.close()
   ```
   `Kafka.help()` is the on-ramp. `k` is `kind: kafka`, `c` is `kafka-consumer`, `p` is `kafka-producer`.

2. **`karate.channel('kafka')` — the durable feature DSL (lock it in).** Crystallize what you learned into a
   `.feature`/`.js` suite that runs in CI. This is the rest of the skill.

## Mental model (the feature DSL)

Kafka in Karate is **pub/sub, not RPC**. A channel exposes two roles — `producer()` and `consumer()` — each with its own properties + methods. The consumer **starts polling asynchronously**; the producer sends synchronously.

```
   karate.channel('kafka')                       ← get channel
     ├─ channel.register({avsc/proto schema})    ← (optional) schema setup
     ↓
   consumer = channel.consumer()                 ← async consumer
     ├─ consumer.topic = 'orders'
     ├─ consumer.count = 1
     ├─ consumer.timeout = 5000
     └─ consumer.start()                          ← begin polling

   producer = channel.producer()                 ← sync producer
     ├─ producer.topic = 'orders'
     ├─ producer.key = 'order-42'
     ├─ producer.value = { id: 42, total: 100 }
     └─ producer.send()                          ← fires

   consumer.pop()                                 ← blocks for next message
     ↓
   match result.value == { id: 42, total: 100 }
```

`consumer.start()` must be called **before** `producer.send()` if the topic has nothing buffered. Kafka consumers default to the `latest` offset for fresh consumer groups; messages produced before `start()` are missed.

## Connection config — central, in `karate-boot.js`

The broker + registry are **not** set in the feature (there is no `configure kafka` global, and `bootstrap`
is not a per-session property). They ride the booted ext, set **once** in `karate-boot.js`; `karate.channel('kafka')`
reads them lazily at create time. The same central config drives both the live `Kafka.*` namespace and `Runner`.

> `boot.ext('kafka')` is a **load-time** (`karate-boot.js`) construct only — it is **not** a runtime global, so calling `boot.ext('kafka')` over `/api/eval` fails with `boot is not defined`. To inspect or override config at runtime, set per-session properties on the channel instead.

```javascript
// karate-boot.js
var kafka = boot.ext('kafka');
kafka.bootstrap = boot.sysprop('kafka.bootstrap', '127.0.0.1:29092');
kafka.schemaRegistry = boot.sysprop('kafka.schemaRegistry', 'http://localhost:8081');   // for Avro/Protobuf
// kafka.ssl = { protocol: 'SSL', truststore: '...', truststorePassword: '...', keystore: '...' }  // TLS/mTLS
// kafka.props = { 'security.protocol': 'SASL_SSL', 'sasl.mechanism': 'PLAIN', 'sasl.jaas.config': '...' }  // SASL
```

Per-session values (`topic`, `key`, `value`, `schema`, `count`, `filter`, `timeout`, `headers`) are still set in the feature.

## Quick start

> Each feature below assumes `karate-boot.js` set the broker/registry as above — the `Background` just gets the channel.

### Plain JSON (no schema registry)

```feature
@kafka
Feature: order events (JSON)

Background:
  # broker comes from karate-boot.js (boot.ext('kafka')) — see "Connection config" above
  * def channel = karate.channel('kafka')

@req=ORD-001
Scenario: produce and consume an order event
  * def consumer = channel.consumer()
  * consumer.topic = 'orders'
  * consumer.count = 1
  * consumer.timeout = 5000
  * consumer.start()

  * def producer = channel.producer()
  * producer.topic = 'orders'
  * producer.key = 'order-42'
  * producer.value = { id: 42, total: 100, status: 'NEW' }
  * producer.send()

  * def msg = consumer.pop()
  * match msg.key == 'order-42'
  * match msg.value == { id: 42, total: 100, status: 'NEW' }
```

### Avro with schema registry

```feature
@kafka @avro
Feature: order events (Avro)

Background:
  # broker + schema registry come from karate-boot.js (boot.ext('kafka'))
  * def channel = karate.channel('kafka')
  * channel.register({ name: 'order', path: '/karate/order.avsc' })

Scenario: produce + consume Avro
  * def consumer = channel.consumer()
  * consumer.topic = 'orders-avro'
  * consumer.count = 1
  * consumer.schema = 'order'
  * consumer.start()

  * def producer = channel.producer()
  * producer.topic = 'orders-avro'
  * producer.schema = 'order'
  * producer.key = 'order-42'
  * producer.value = { id: 42, total: 100, status: 'NEW' }
  * producer.send()

  * match consumer.pop().value == { id: 42, total: 100, status: 'NEW' }
```

### Protobuf

```feature
@kafka @proto
Feature: order events (Protobuf)

Background:
  # broker comes from karate-boot.js (boot.ext('kafka'))
  * def channel = karate.channel('kafka')
  * channel.register({
      name: 'order-proto',
      path: '/karate/order.proto',
      message: 'Order',
      roots: ['/karate']
    })

Scenario: produce + consume Protobuf
  * def consumer = channel.consumer()
  * consumer.topic = 'orders-proto'
  * consumer.count = 1
  * consumer.schema = 'order-proto'
  * consumer.start()

  * def producer = channel.producer()
  * producer.topic = 'orders-proto'
  * producer.schema = 'order-proto'
  * producer.value = { id: 42, total: 100 }
  * producer.send()

  * match consumer.pop().value == { id: 42, total: 100 }
```

## Channel + producer + consumer reference

### Channel methods

| Method | Returns | Purpose |
|---|---|---|
| `register(opts)` | void | Register a named wire codec. `{ name, path }` for Avro (`.avsc`); `{ name, path, message, roots }` for Protobuf (`.proto`); `{ name, codec }` to plug a **custom `WireCodec`** (see *Custom wire formats* below). The default (no `schema`) is JSON. |
| `producer()` | Producer | Get a fresh producer handle. |
| `consumer()` | Consumer | Get a fresh consumer handle. |

### Producer properties

| Property | Type | Purpose |
|---|---|---|
| `topic` | String | Kafka topic. |
| `key` | String | Message key (null allowed). |
| `value` | Object | Message body (Map or primitive). Serialized per `schema`. |
| `schema` | String | Registered schema name. Omit for JSON. |
| `headers` | Map<String, String> | Optional headers (null values dropped). |

| Method | Returns | Purpose |
|---|---|---|
| `send()` | void | Synchronous send. |

### Consumer properties

| Property | Type | Purpose |
|---|---|---|
| `topic` | String | Kafka topic. |
| `count` | int | Expected message count. |
| `timeout` | int | Poll timeout in ms (default 5000). |
| `schema` | String | Registered schema name (for deserialization). Omit for JSON. |
| `filter` | function | Drop messages where the predicate returns false. |

| Method | Returns | Purpose |
|---|---|---|
| `start()` | void | Begin async polling. **Must be called before producer sends.** |
| `collect()` | List<{offset, key, value, headers}> | Block until `count` reached or `timeout`. |
| `pop()` | Object | Shorthand for `collect()[0]`. |

## Configuration knobs — on the booted ext (`karate-boot.js`)

Set on `boot.ext('kafka')`. `bootstrap`/`schemaRegistry` are first-class; everything else (SASL, raw
client overrides) rides `ssl` / `props`. The producer + consumer Karate creates inherit it all.

| On `boot.ext('kafka')` | Purpose |
|---|---|
| `bootstrap` | Broker list. Required (defaults `127.0.0.1:29092`). |
| `schemaRegistry` | Confluent registry URL. Required for Avro / Protobuf. |
| `ssl` | `{ protocol:'SSL', truststore, truststorePassword, keystore, keystorePassword }` — TLS / mTLS. |
| `props` | Any raw Kafka client key, e.g. `{ 'security.protocol':'SASL_SSL', 'sasl.mechanism':'PLAIN', 'sasl.jaas.config':'…' }`. |

Karate's producer defaults: `enable.idempotence=true`, `acks=all`, `compression.type=snappy`, `linger.ms=20`, `batch.size=32768` (override via `props`). `producer.send()` blocks on the broker ack, so a failed produce surfaces synchronously.

## Filter — selective consume

```feature
* consumer.filter = function(msg) { return msg.value.status == 'COMPLETED' }
* consumer.count = 1
* consumer.start()
# Producer sends 5 messages; consumer only collects the COMPLETED one
```

## Coverage — topic × direction

The Kafka universe is **topic × direction** (the event-side analog of an OpenAPI operation). Declare it in
`karate-boot.js`; an omitted `direction` enumerates **both** publish + subscribe (two items per topic):

```javascript
// karate-boot.js (alongside boot.ext('kafka'))
var cov = boot.ext('coverage');
cov.kafka = [
  { topic: 'orders', schema: 'order' },          // -> orders#publish + orders#subscribe
  { topic: 'shipments', direction: 'subscribe' } // -> shipments#subscribe only
];
```

**Don't know the declared topics?** Don't guess (or read `karate-boot.js`) — call `Report.aggregate()`
**before running anything**: with no runs it rebuilds the graph from the declared universe alone, so
`Coverage.gaps()` / `Coverage.items('kafka')` enumerate every `topic#direction` as an uncovered gap. That
is the pre-run "what am I expected to cover" view; then run your suite — on the console its settle fills in
what it covered.

Every `producer.send()` / `consumer.collect()` emits a pre-matched `kafka-match` embed that joins by
`topic#direction`, so a run shows which topics were exercised + the **ack/ok status histogram**, with
untested topics as honest gaps (`Coverage.gaps()`). A served run logs `coverage[kafka]: N/M items`. The
declared universe can also come from an **AsyncAPI doc** (`cov.asyncapi`) — the richer topic×schema topology
Kafka lacks natively. (`cov.kafka` is the inline form; both feed one coverage report.)

### Field-dimension coverage — Avro schema fields (the registry form)

Point `cov.kafka` at the **Confluent Schema Registry** (the `schemaRegistry` key — same URL the `kafka` ext
uses) and each topic's `<topic>-value` Avro schema additionally resolves to its **field axes** (enum symbols,
booleans), so a *produced* message grades **input-field coverage** too — the Kafka analog of gRPC's
descriptor-grade field dimensions, reverse-inferred from live traffic with **zero authoring**:

```javascript
// karate-boot.js — the registry/hybrid form (an object, not a list)
var cov = boot.ext('coverage');
cov.kafka = {
  schemaRegistry: kafka.schemaRegistry,         // reuse the kafka ext's registry URL
  topics: [                                     // optional: omit to DISCOVER all subjects from the registry
    { topic: 'json-topic' },                    // plain JSON, no subject -> covered name-only (no field axes)
    { topic: 'orders-avro', schema: 'order' }   // <orders-avro>-value resolves -> enum/bool field axes
  ]
};
```

After a run that produced to `orders-avro`, `Coverage.dimensions()` grades each field axis and lists the
**unsent values as gaps** — e.g. an enum `status` field with only `NEW` produced reads `2/3` (the other
symbols are gaps), a boolean `express` only ever `true` reads `1/2` (`false` is the gap). Avro carries no
native min/max/pattern, so numeric/string fields stay *open* (no axis) — like bare protobuf; only enum + bool
(and nested record fields, by dotted path) are closed axes. **Forms:** a **list** → topic×direction only
(name-only types); an **object with `schemaRegistry`** → topic×direction **plus** Avro field axes; with
`topics` omitted it discovers the universe from the registry's subjects.

## Custom wire formats — `WireCodec` (plug any serialization)

Avro, Protobuf, and JSON are the built-ins, but the serialization seam is **open**: a `WireCodec` is two
methods — `encode(value)` (JSON object → wire) and `decode(wire)` (wire → JSON object). Once a message decodes
to a Map, `match`/assert/report/coverage all work unchanged, so a **proprietary format** (a bespoke binary
envelope, a CSV line, a length-prefixed frame) is one small class away. Register it by `name`, then point a
producer/consumer at it via `schema`:

```javascript
// karate-boot.js (or a *.js helper) — a custom codec
var MyCodec = Java.extend(Java.type('io.karatelabs.ext.contract.WireCodec'));
var myCodec = new MyCodec({
  encode: function(value) { /* JSON map -> wire bytes/string */ return ... },
  decode: function(wire)  { /* wire     -> JSON map         */ return ... }
});
```

```feature
* def channel = karate.channel('kafka')
* channel.register({ name: 'envelope', codec: myCodec })   // a custom WireCodec
...
* producer.schema = 'envelope'      // produce through it
* consumer.schema = 'envelope'      // consume + decode through it
* match consumer.pop().value == { ... }   // assert on the decoded map, as always
```

The built-in `JsonCodec`/`AvroCodec`/`ProtoCodec` are themselves `WireCodec`s — `register({name, path})`
just wires the right built-in for you. The **same `WireCodec` seam backs WebSocket** (see the `websocket`
skill), so a format you teach Karate once is reusable across both transports.

## Standing rules

1. **`consumer.start()` before `producer.send()`.** Fresh consumer groups start at `latest`; produce-then-consume drops the message.
2. **Use a unique consumer group per scenario** for isolation. Don't share groups across parallel tests.
3. **Set `count` to the exact expected.** Use `'#[_ >= N]'` matcher on the collected list if "at least N" matters more than exactness.
4. **Use `timeout` deliberately.** Too short → flaky tests; too long → CI run drags. 5s is a good default for local Kafka; 15s for cross-region.
5. **Register schemas in `Background`** so they're available to every scenario. Schemas don't leak across channels (they're channel-scoped), so re-registration per scenario is wasteful but not incorrect.
6. **For end-to-end scenarios that pair REST + Kafka**: produce via REST (your API), consume via Kafka to assert the downstream event. Don't shortcut by producing the event directly — it bypasses the system under test.
7. **Tag with `@kafka`** (and `@avro` / `@proto` / `@json` where it matters). Lets the dashboard slice tests by serialization.

## Anti-patterns

1. **Anti-pattern:** `producer.send()` before `consumer.start()`. Race condition; intermittent failures.
2. **Anti-pattern:** sharing one consumer across scenarios. Async polling state leaks. New consumer per scenario.
3. **Anti-pattern:** asserting on offsets. Offsets shift with broker state; assert on key/value/headers.
4. **Anti-pattern:** test that expects a specific partition assignment. Karate doesn't expose partition control; if your test needs it, use a custom Java producer instead.
5. **Anti-pattern:** Avro `value` with field types that don't match the schema. The producer will fail with a Confluent serializer error; pin schema first, payload second.
6. **Anti-pattern:** leaving Kafka brokers running across test runs without resetting topics. Lingering messages contaminate later runs.

## Advanced surface

- **mTLS with karate-kafka**: set `kafka.ssl = { protocol:'SSL', truststore, truststorePassword, keystore, keystorePassword }` on `boot.ext('kafka')`; the in-repo `kafka-mtls` demo kit is a genuine mutual-TLS broker + cert set.
- **Multiple schemas per channel**: register many; switch via `producer.schema` / `consumer.schema` per scenario.
- **Schema-registry-less Avro**: supported via a **local `.avsc`** — `register({ name, path:'/order.avsc' })` with **no** `schemaRegistry` configured emits **raw Avro wire bytes** (the `AvroCodec` encodes/decodes from the local schema, no Confluent wire header). Configure `kafka.schemaRegistry` and the *same* registration switches to the Confluent registry serializer. (This is the `WireCodec` seam — see *Custom wire formats* above; it is also how `Asyncapi.scaffold` lights up Avro coverage from a local `.avsc`.)
- **Transactions**: not exposed. Use Java producers directly if needed.
- **Headers**: `producer.headers = { 'x-correlation-id': uuid }`. Captured on the consumer side as `msg.headers`.

## Cross-skill links

- `Skill.help('api')` — REST patterns; most Background / config / tagging discipline transfers.
- `Skill.help('grpc')` — companion synchronous streaming skill; same channel SPI.
- `Skill.help('assertions')` — `match` semantics on the decoded message value.
- `Skill.help('coverage')` — `cov.kafka` topic×direction coverage rolls into the same engine as REST + gRPC; `cov.asyncapi` adds the declared event topology.
