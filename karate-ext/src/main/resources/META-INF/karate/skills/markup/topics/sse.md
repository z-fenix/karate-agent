# SSE & Real-Time Updates

> Hybrid pattern: HTMX sse for low-frequency server-rendered updates, Alpine EventSource for high-frequency client-rendered updates.

## Karate-Core SSE Support

Karate has built-in SSE via `SseHandler` and `SseConnection`. SSE is triggered when a client sends `Accept: text/event-stream`.

### Server Setup

Register an `SseHandler` alongside your `ServerRequestHandler`:

```java
SseHandler sseHandler = (request, connection) -> {
    // Offload to background thread — this runs on Netty worker
    Thread.startVirtualThread(() -> {
        while (connection.isOpen()) {
            connection.send("state", renderBadge(session));  // named event
            connection.send("iteration", toJson(progress));  // named event
            Thread.sleep(1000);
        }
    });
};
connection.onDisconnect(() -> cleanup());

HttpServer.start(port, requestHandler, sseHandler);
```

### SseConnection API

| Method | Purpose |
|--------|---------|
| `send(event, data)` | Send named event (maps to HTMX `sse-swap` or `addEventListener`) |
| `send(data)` | Send unnamed event (default `message` type) |
| `sendComment(text)` | Send SSE comment (keep-alive) |
| `close()` | Close the connection |
| `isOpen()` | Check if client is still connected |
| `onDisconnect(callback)` | Register cleanup callback |

Multiline data is handled automatically — newlines are split into separate `data:` lines per the SSE spec.

## Pattern 1: HTMX SSE — Low Frequency

Use when the server should render HTML fragments and push them to the client. Best for state changes, notifications, and UI updates that happen every few seconds or slower.

```html
<div hx-ext="sse" sse-connect="/api/sessions/abc123/sse">
  <!-- State badge — replaced when server sends "state" event -->
  <div id="state-badge" sse-swap="state">
    <span class="<your-neutral-pill-class>">connecting...</span>
  </div>

  <!-- Error panel — replaced when server sends "error" event -->
  <div id="error-panel" sse-swap="error"></div>
</div>
```

(Concrete pill / alert classes are CSS-framework-specific. See CSS_BOOTSTRAP.md / `Skill.help('markup', 'tailwind')`.)

**Server sends:**
```
event: state
data: <span class="<your-success-pill-class>">ready</span>

event: error
data: <div class="<your-danger-alert-class>">Connection lost</div>
```

Each `sse-swap` target is replaced with the HTML in the event's `data` field. The server renders the fragment — the client just swaps it in.

**HTMX SSE extension must be loaded:**
```html
<script src="https://unpkg.com/htmx-ext-sse@2.2.2/sse.js"></script>
```

## Pattern 2: Alpine EventSource — High Frequency

> **Prefer `$live` for new code.** The hand-rolled `new EventSource` + `addEventListener` + `JSON.parse` + `push` shape below is preserved here as the reference. For any new SSE-driven view — and especially anything that needs to reconcile a REST history endpoint with the live stream — use the `$live` Alpine magic instead. See `Skill.help('markup')` §Live SSE+REST Reconciliation. One declarative call, opt-in `initial:` for history backfill, opt-in `dedup:` to drop duplicates across the REST/SSE seam.

Use when events are frequent (multiple per second) and server-side rendering would be wasteful. The server sends JSON, the client renders.

```html
<div x-data="{ iterations: [], connected: false }" x-init="
  var es = new EventSource('/api/sessions/abc123/sse');
  es.addEventListener('iteration', function(e) {
    iterations.push(JSON.parse(e.data));
  });
  es.addEventListener('open', function() { connected = true; });
  es.addEventListener('error', function() { connected = false; });
">
  <span x-show="!connected" class="text-warning">Reconnecting...</span>

  <template x-for="iter in iterations" :key="iter.index">
    <div class="flex items-center gap-2">
      <span x-text="'Step ' + iter.index"></span>
      <span x-text="iter.status"
            x-bind:class="iter.status == 'pass' ? '<your-success-text-class>' : '<your-danger-text-class>'"></span>
    </div>
  </template>
</div>
```

**Equivalent with `$live`** (drop-in for the common case — single named-event channel, no history, no dedup):

```html
<div x-data="{ iterations: $live('/api/sessions/abc123/sse', { event: 'iteration' }) }">
  <template x-for="iter in iterations" :key="iter.index">
    <div x-text="iter.status"></div>
  </template>
</div>
```

**Server sends:**
```
event: iteration
data: {"index":1,"status":"pass","duration":42}

event: iteration
data: {"index":2,"status":"fail","duration":108}
```

## When to Use Which Pattern

| Factor | HTMX SSE | Alpine EventSource |
|--------|-----------------|--------------------------|
| Event frequency | Low (seconds between events) | High (sub-second) |
| Rendering | Server renders HTML fragment | Client renders from JSON |
| Complexity | Simple — just swap HTML | More code — parse JSON, build DOM |
| Server load | Higher — renders template per event | Lower — sends raw JSON |
| Use cases | State badges, notifications, alerts | Live logs, progress bars, charts |

## Mixed Pattern

A single SSE connection can serve both patterns. Use HTMX `sse-swap` for some events and Alpine `addEventListener` for others:

```html
<div hx-ext="sse" sse-connect="/api/sessions/abc123/sse"
     x-data="{ iterations: [] }" x-init="
       var es = document.querySelector('[sse-connect]');
       // Alpine handles high-frequency events
       new EventSource('/api/sessions/abc123/sse')
         .addEventListener('iteration', function(e) {
           iterations.push(JSON.parse(e.data));
         });
     ">

  <!-- HTMX handles low-frequency state swaps -->
  <div sse-swap="state">
    <span class="badge bg-secondary">loading...</span>
  </div>

  <!-- Alpine handles high-frequency iteration list -->
  <template x-for="iter in iterations">
    <div x-text="iter.status"></div>
  </template>
</div>
```

Note: In the mixed pattern, use a separate `EventSource` instance for Alpine rather than trying to share the HTMX one. HTMX manages its own connection lifecycle.

## Keep-Alive

For long-lived connections, send periodic comments to prevent proxy/load-balancer timeouts:

```java
// Server-side keep-alive
while (connection.isOpen()) {
    connection.sendComment("keepalive");
    Thread.sleep(15000); // every 15 seconds
}
```

`EventSource` auto-reconnects on disconnect. HTMX sse extension also handles reconnection automatically.
