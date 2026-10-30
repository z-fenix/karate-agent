// The page -> JSAPI bridge (D83 / D63 / S14).
//
// A served page drives the SAME `/api/eval` surface the LLM and curl do (P3): post a line of
// JavaScript, get back the canonical `{command, payload}` envelope (plus an optional `ref` when
// the result is a live handle like `mock`/`bot`, and `name` on errors). The live channel is the
// SSE stream at `/api/events`. Nothing here is feature-specific — every served UI in the D82 push
// (console, rule-sim, report viewer) sits on these two calls.
window.karate = {
  // Evaluate one expression/statement against the shared serve engine. Resolves to the envelope.
  async eval(code) {
    const res = await fetch('/api/eval', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: code
    });
    return res.json(); // { command:'result'|'error', payload, ref?, name? }
  },

  // Convenience: eval + throw on the `error` envelope, returning just the payload on success.
  async value(code) {
    const env = await this.eval(code);
    if (env.command === 'error') {
      throw new Error(env.name ? env.name + ': ' + env.payload : env.payload);
    }
    return env.payload;
  },

  // Subscribe to the live SSE channel. `onEvent(name, data)` fires for every event; the raw
  // EventSource is returned so callers can `.addEventListener(name, ...)` for a specific type
  // or `.close()`. Pass `{run}` to scope to one harness run's topic (the console stream),
  // and `{since}` to backfill only events after that seq on (re)connect — the page dedups by the
  // `seq` field the host splices into each run-topic payload. Named events arrive via the
  // per-type listeners below (EventSource only routes UNNAMED events to onmessage).
  events(onEvent, { run, since, onOpen, onError } = {}) {
    let url = '/api/events';
    if (run) {
      url += '?run=' + encodeURIComponent(run);
      if (since != null) url += '&since=' + encodeURIComponent(since);
    }
    const src = new EventSource(url);
    if (onOpen) src.onopen = onOpen;
    if (onError) src.onerror = onError;
    if (onEvent) {
      src.onmessage = (e) => onEvent('message', e.data);
      // the host's generic signals + every typed HarnessEvent name (the thin faithful projection,
      // D42) — a new event type is one line here, nothing else on the wire.
      const named = [
        'run_start', 'log', 'eval', 'eval_start', 'reload', 'reanchor', 'transcript', 'status', 'artifact',
        // a settled run was folded into the project graph — a page reloads its numbers from its /ui/ handler
        'coverage',
        // a side-chat turn's events on its chat topic (api-workspace §5): the tester's message, call and run cards, the reply
        'chat',
        'iteration_start', 'context_trim', 'llm_request', 'llm_response_start', 'text_chunk',
        'llm_response', 'thinking', 'tool_request', 'tool_result', 'console', 'hint', 'step', 'screenshot',
        'assertion', 'healed', 'paused', 'resumed', 'complete', 'error',
        // the human UI-recorder live feed (D150) — without these the recorder events publish to
        // SSE but EventSource drops them (no per-type listener), so captured actions only appear on a
        // refresh (the transcript.jsonl replay), never live while recording.
        'human_action', 'focus', 'annotation', 'browser_reset', 'control_handoff',
        // the suite (Runner.run a .feature) live feed — core's RunEvent stream bridged to SSE (D132)
        'feature_enter', 'feature_exit', 'scenario_enter', 'scenario_exit',
        // the View.* presentation control plane (D115) — the LLM steers the human's view; the app-wide
        // control-channel client in layout.html interprets these. `tab-limit` rejects an over-cap tab.
        'navigate', 'highlight', 'callout', 'overlay', 'open', 'tab-limit', 'view-clear'
      ];
      named.forEach((name) => src.addEventListener(name, (e) => onEvent(name, e.data)));
    }
    return src;
  }
};

// Copy text to the clipboard — the async Clipboard API needs a secure context (https/localhost),
// so a remote-Docker http:// console falls back to the hidden-textarea path.
async function kaCopy(text) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  document.execCommand('copy');
  document.body.removeChild(ta);
}

// ---- "Point an LLM at this page" — the reusable copy-prompt pattern (D120) ------------------------
//
// The product bet (P1/P2): a business user does NOT need to learn the console. They click "Copy LLM
// prompt" on whatever page they're on, paste it into any LLM (an MCP client like Claude, or any agent
// that can curl), and the LLM is bootstrapped onto the right *skill* + THIS server — then it mediates
// the UI for them in plain language, driving the same `/api/eval` surface the page itself uses (P3).
//
// This is the generic engine behind that button. Any page wires it in two lines (see the markup skill
// "copy-prompt button" recipe):
//
//   <div x-data="kaPromptButton('rule-authoring', {context: 'Work on the rulebook \"order-pricing\".'})">
//     <button @click="copy()" x-text="label" class="...your button classes..."></button>
//   </div>
//
// `skill` is a served skill name (Skill.list()); the LLM loads it itself via Skill.help(skill), so the
// skill markdown stays the single source of truth — the prompt never duplicates the workflow.

// Build the canonical skill-pointer bootstrap prompt for this server. Pure string — no network.
function kaLlmPrompt(skill, opts) {
  opts = opts || {};
  const base = location.origin;
  const lines = [
    'You are connected to a Karate server at ' + base + '. You drive it by sending JavaScript to it',
    'and reading back a JSON { command, payload } envelope:',
    '  - MCP client: call the `karate_eval` tool (endpoint ' + base + '/api/mcp).',
    '  - Otherwise: POST the JavaScript to ' + base + '/api/eval (e.g. `curl -s ' + base + '/api/eval -d "Rule.list()"`).',
    '',
    'First, load the skill for this task so you know the verbs and the workflow — then follow it:',
    '  Skill.help(\'' + skill + '\')'
  ];
  if (opts.context) { lines.push('', opts.context); }
  lines.push('', opts.task
    || 'Then ask me, in plain language, what I want to do — and drive the server to do it, showing me the'
       + ' results. I should not have to learn the console; you operate it for me.');
  return lines.join('\n');
}

// Build + copy in one call (the button click handler).
async function kaCopyLlmPrompt(skill, opts) {
  await kaCopy(kaLlmPrompt(skill, opts));
}

// The Alpine component factory — `x-data="kaPromptButton('<skill>', {context, task})"`. Exposes
// `copy()` and a reactive `label` ("Copy LLM prompt" -> "copied ✓" for ~1.8s).
function kaPromptButton(skill, opts) {
  return {
    copied: false,
    get label() { return this.copied ? 'copied ✓' : 'Copy LLM prompt'; },
    async copy() {
      await kaCopyLlmPrompt(skill, opts);
      this.copied = true;
      setTimeout(() => { this.copied = false; }, 1800);
    }
  };
}

// ---- provider preflight — the Test control beside any selectable provider·model ------------------
//
// One Alpine factory behind every surface that offers a provider·model to pick (the Settings rows,
// the run-kickoff modal's LLM select, the Convert-to-check model select): a key that is set says
// nothing about whether the provider ANSWERS, so wherever the key/model shows, so does a way to
// test it. Rides the /ui page-data channel (D98 — a fresh engine, never /api/eval, so the round
// trip never queues behind a bot op); Harness.ping returns a failure as a RESULT (S14), so this is
// always a 200 to branch on.
//
//   x-data="kaProviderTest(() => combos[combo])"   // a live getter — read at click time
//
// `pick()` returns anything carrying {provider, model}; a combo object passes through as-is.
function kaProviderTest(pick) {
  return {
    busy: false, res: null,
    async test() {
      this.busy = true;
      this.res = null;
      const p = pick() || {};
      const body = { provider: p.provider };
      if (p.model) { body.model = p.model; }
      try {
        const r = await fetch('/ui/settings/ping', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body)
        });
        this.res = await r.json();
      } catch (e) {
        this.res = { ok: false, error: String(e) };
      }
      this.busy = false;
    },
    // ok → what answered and how fast; failure → the status + the provider's own words, capped (the
    // full body belongs on a title attribute, the hint on its own line beneath)
    verdict() {
      const r = this.res;
      if (!r) return '';
      // `model` is absent when the run would take the provider's own default — say nothing rather than 'null'
      if (r.ok) return 'ok · ' + (r.model ? r.model + ' · ' : '') + r.latencyMs + 'ms';
      const body = (r.error || 'failed').replace(/\s+/g, ' ').trim();
      return (r.status ? r.status + ' · ' : '') + (body.length > 140 ? body.slice(0, 140) + '…' : body);
    }
  };
}
