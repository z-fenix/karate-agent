# Binding a twin to the wire

Bind a twin when the project wants a contract, a stateful mock or a playground off the same model.
One `t.resource`, four fields per command, and `Rule.project` writes the OpenAPI document. Concepts:
`Twin.help('binding')`.

## The declaration

```js
t.resource({
    path: '/claims',            // the collection; the item path is path + '/{' + identity + '}'
    identity: 'claimId',        // the world field the item path addresses
    view: w => ({ claimId: w.claimId, status: w.status, reserve: w.reserve }),
    schema: {                   // the view's JSON Schema, copied into the contract verbatim
        type: 'object',
        required: ['claimId', 'status', 'reserve'],
        properties: {
            claimId: { type: 'string' },
            status: { enum: ['OPEN', 'RESERVED', 'PAID', 'CLOSED'] },
            reserve: { type: 'integer', minimum: 0 }
        }
    },
    errorCode: 'error',         // the body field a refusal's code travels in (default 'error')
    list: true,                 // also project GET on the collection
    name: 'Claim'               // component name + read operationIds (default: PascalCase of path)
});

t.command('reserve', {
    when: w => w.status === 'OPEN',
    args: a => { a.int('amount', 1, 60000, [1, 60000]); },
    apply: (w, a) => { if (a.amount > w.limit) t.reject('OVER_LIMIT'); w.reserve = a.amount; w.status = 'RESERVED'; },
    req: 'CLM-004/2',
    operation: 'reserveClaim',                  // default: the command id
    route: 'POST /claims/{claimId}/reserve',    // method + template
    wire: {
        applied: 200,                           // default 200, or 201 on the creating command
        refused: {
            NOT_OPEN: { status: 409, code: 'not_open' },
            OVER_LIMIT: { status: 409, code: 'over_limit' }
        }
    }
});
```

## What the author owes

- **One command carries `creates: true`** and its route is the collection path. Every other route is
  the item path or a sub-path of it. `apply` never writes the identity — the mock allocates it.
- **Name every reason the model can raise**: the guard's reason comes from
  `refusals: { guard: '<REASON>' }`, an in-`apply` `t.reject('<REASON>')` names its own. Each needs a
  `wire.refused` entry, or `Twin.check` reports `wireUndeclared`. A `scopeGuard: true` command is the
  exception: its guard fences exploration and owes no entry.
- **Declare arguments with the `a.*` primitives**, never a literal row list: the primitives transcribe
  to the request-body schema, a literal list carries no domain and the command reads `ARGS_LITERAL`.
- **Keep the view schema inside the validator's subset**, anywhere in the tree, and self-contained — a
  `$ref`, `$defs`, `$id` or anchor is `SCHEMA_UNSUPPORTED`. Declare the identity in its `properties`,
  or the projection refuses `IDENTITY_UNPROJECTED` and writes nothing. Path parameters project as
  `{type: 'string'}`: a path segment is text on the wire.
- **An authored `request`/`observe` pair is live-only** and is refused beside `route`. Use it for a
  backend the declaration cannot express, and accept that the command reads `AUTHORED_TRANSPORT`.

## The loop

```js
Rule.project('claims')                  // -> {ok, spec, digest, notprojected, refusals, out}
Openapi.lint(Rule.project('claims').out)
Twin.check('claims')                    // findings.binding, and the verdict
Http.mock({twin: 'claims'})             // the same binding mounted: that spec routes, the twin answers
Rule.project('claims', {against: 'api/claims.yaml', failOn: 'warning'})   // a spec you already hold
```

**Starting from a spec you already hold**, `{against}` diffs it (baseline) against the projection and
writes nothing. A projection is not a release, so nothing is breaking. `OPERATION_UNMODELLED` is a
spec operation the twin lacks, `OPERATION_UNCLAIMED` a projected one the spec lacks, `*_DIFFERS` and
`*_DEPARTS` a mismatch the facet names, `ENGINE_RESPONSE` the engine's own 400/404, `CONSTRUCT_FENCED`
a query, header, auth or servers entry on the fence. Bind the twin until `failOn: 'warning'` passes,
and waive what stays different by name, with a reason and an owner.

Read `notprojected` every time: it names each command left out and the standing fence — query
parameters, headers, auth, pagination, content negotiation, multipart, streaming. The projected file
is reviewed as a diff; editing it by hand reads `projectionDrift` in `Twin.check`. Auth is on the
fence, so the bundled rulepack's `security-scheme-declared` is waived by name with a reason and an
owner (`Openapi.lint(spec, {waivers: {...}})`), never answered with a guessed scheme.
