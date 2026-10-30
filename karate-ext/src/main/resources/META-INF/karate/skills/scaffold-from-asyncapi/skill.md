# Scaffold from AsyncAPI — Skill

> **What it is:** turn an AsyncAPI doc **alone** — no hand-written project — into a runnable Karate
> **Kafka** project: boot glue plus a produce/consume **round-trip** feature, run against a real broker so
> `cov.asyncapi` scores which channels you exercised **and** grades the payload's field axes (enum/bool).
> The **event-side twin** of `Skill.help('scaffold-from-openapi')`: the AsyncAPI doc
> IS the channel×direction universe, the way an OpenAPI spec is the operation universe.
> **The keystone:** the message sample rides **straight from the doc** (the payload JSON-Schema, inline or
> `$ref` into `components/schemas`; or an Avro type read from a **local `.avsc`**) — **no schema registry**.

## When to use this

- You have an AsyncAPI v2/v3 doc describing **Kafka** channels and want a working Karate project fast.
- You want to **score event coverage** of your tests — which topics/directions ran, and which payload field
  values (enum members, booleans) you actually exercised vs. left as honest gaps.
- You are exploring a live topic with the `Kafka.*` namespace (see the `kafka` skill) and want to
  crystallize a starter project from the contract.

**Requires the `kafka` leaf** (the async jar) and a reachable broker — this skill is hidden on the lean
agent jar. Two payload formats today — **JSON-Schema** and **local-`.avsc` Avro** (registryless, copied in
beside the doc). Confluent **Schema-Registry** Avro is the later layer (see *Limits*).

## The primitives this composes (drill into each with `help()`)

| Primitive | Role |
|---|---|
| `Asyncapi.scaffold(doc, {out?, feature?})` | **the one-shot** — writes the runnable project (`karate-boot.js` + `checks/<name>.feature`) from the doc alone and copies the doc in. Start here — `Asyncapi.help()` |
| `Kafka.connect(...)` / `.producer()` / `.consumer()` | **explore** a live topic before/while authoring — the live drive surface (the `kafka` skill's explore→author loop) |
| `karate.channel('kafka')` (in a feature) | the in-run producer/consumer the scaffolded feature uses — `boot.ext('kafka')` lights it |
| `cov.asyncapi` (karate-boot.js) | register the doc as the **asyncapi coverage source** (channel×direction + JSON-Schema field axes) — `Coverage.help()` |
| `Coverage.dimensions()` / `gaps()` | the field-axis grade (enum/bool reverse-inferred from produced values) + the uncovered channels — `Coverage.help()` |
| `Report.generate()` | the standalone `file://` coverage scorecard — `Report.help()` |

There is **no** `Asyncapi.summary`/`operations` inventory yet (that rides `OperationModel.from(Contract)`),
and **no** mock — the round-trip runs against a **real broker**, not a double. Orient with `Asyncapi.help()`
and the live `Kafka.*` namespace.

## The flow

**Fast path — one call writes the project:** `Asyncapi.scaffold('asyncapi.yml', {out:'.'})` emits
`karate-boot.js` + a starter `checks/<name>.feature` and copies the doc in. Boot a broker, run it, then
read coverage. The steps below are what `scaffold` produces — follow them by hand only to customize.

**Honor the project layout.** A runnable `.feature`/`.js` goes under `checks/`; `scaffold` already
writes there. Keep any files you author by hand in the same dirs so the console's runs view and `Check.list`
surface them.

```
   AsyncAPI doc
        │
        ├─(1)─ create the skeleton (karate-boot.js): boot.ext('kafka') + cov.asyncapi = '<doc>'
        │
        ├─(2)─ the round-trip feature (per channel/operation):
        │        produce a SAMPLED message (straight from the doc) → consume + match it back
        │        karate.channel('kafka'), producer.send() / consumer.pop()
        │
        ├─(3)─ run: karate run <feature> ......... real broker traffic → observed coverage
        │        (boot.ext('coverage') auto-enables the karate-events.jsonl stream it computes from)
        │
        └─(4)─ the report ........ Coverage.dimensions() + gaps(), Report.generate()
                 channels exercised (topic×direction) + payload field axes (enum/bool) graded
```

### 1 — the skeleton
`Asyncapi.scaffold` writes a `karate-boot.js` that lights the kafka ext and registers the doc as the
coverage source. JSON payloads need **no** `schemaRegistry`:

```js
// karate-boot.js
var kafka = boot.ext('kafka');
kafka.bootstrap = boot.sysprop('kafka.bootstrap', 'localhost:9092');
var cov = boot.ext('coverage');
cov.asyncapi = 'asyncapi.yml';     // the channel×direction universe + JSON-Schema payload field axes
```

### 2 — the round-trip feature
For each channel/operation, the starter feature **produces a sampled message** and **consumes + asserts** it
— a real round-trip against the broker, no mock. The sample comes from the doc's payload schema, so an
`enum`/`boolean` field is sampled with a real value. For an **Avro** channel the scaffold also copies the
`.avsc` in and adds `channel.register({name, path})` + `producer.schema`/`consumer.schema` to the feature
(registryless raw Avro — see the `kafka` skill's *Custom wire formats*):

```cucumber
@scaffold
Feature: Orders — starter (scaffolded)

  Background:
    * def channel = karate.channel('kafka')

  Scenario: orders publish
    * def consumer = channel.consumer()
    * consumer.topic = 'orders'
    * consumer.count = 1
    * consumer.start()
    * def producer = channel.producer()
    * producer.topic = 'orders'
    * producer.value = { id: '...', status: 'NEW', express: true }
    * producer.send()
    * match consumer.pop().value == { id: '#string', status: '#string', express: '#boolean' }
```

### 3 + 4 — run and report
Run the suite — **`karate run <feature>`** (`boot.ext('coverage')` auto-enables the enriched
`karate-events.jsonl` stream coverage is computed from). Each produce drops a
`kafka-match` carrying the **produced value**, so the payload's field coordinates are reverse-inferred — no
extra authoring. Then:

```js
Coverage.dimensions();   // per-channel field axes: status enum 2/3 (CANCELLED the gap), express bool 2/2
Coverage.gaps();         // channels/directions never exercised
Report.generate();       // → the file:// coverage report page
```

The field-axis grade is the headline: produce `status: 'NEW'` and `status: 'SHIPPED'` of a
`{NEW,SHIPPED,CANCELLED}` enum and the axis grades **2/3** — `CANCELLED` is a real, named gap, **straight
from the doc, no registry**.

## The scaffolded starter feature is provisional — replace it, don't grow it

The starter `.feature` is tagged **`@scaffold`** and is **bootstrap smoke, not a deliverable**. Its produce
calls count as **normal asyncapi coverage** (the channels *are* exercised). Replace it with intent-bearing
tests of your real event flows as they come online; `@scaffold` marks it disposable.

## Limits (what this deliberately does NOT do yet)

- **JSON-Schema + local-`.avsc` Avro, no registry.** A message whose `schemaFormat` is Avro resolves its
  payload from a **local `.avsc`** (the gRPC-`.proto` parallel) — `scaffold` copies the `.avsc` in and emits
  `channel.register({name, path})` + `producer.schema`/`consumer.schema` (raw Avro wire bytes ⇄ JSON,
  registryless), and the enum/bool axes light up `cov.asyncapi` just like JSON. **Confluent Schema-Registry**
  Avro (sampling from a registry subject) is the remaining layer — the scaffolded `karate-boot.js` notes
  where `kafka.schemaRegistry` would be added.
- **No derived requirements.** Unlike `scaffold-from-openapi` (which emits hollow requirement stubs),
  the AsyncAPI path emits boot + feature only — cross-protocol requirement derivation rides a separate
  piece (`OperationModel.from(Contract)`).
- **No mock.** The round-trip runs against a **real broker** (the event-side analog of a live API), not a
  generated double.
- **Forward only (this skill).** Reverse-engineering an AsyncAPI doc *from* observed live traffic is a
  separate, **shipped** capability — `Asyncapi.reverse(source)` turns a run's recorded `kafka-match` events
  into an AsyncAPI v3 doc (each payload schema inferred from the produced messages). Use it to seed a doc you
  then feed *back* into this scaffold.

## Related skills

- **`kafka`** — the live `Kafka.*` namespace + `karate.channel('kafka')`: connect, produce/consume,
  JSON/Avro/Protobuf serialization (the explore→author loop, and the runtime the scaffold targets).
- **`scaffold-from-openapi`** — the REST/HTTP twin (stateful mock + tests + derived requirements + report).
- **`coverage`** — read the scorecard: `Coverage.dimensions()`/`gaps()`, the 3-source report.
