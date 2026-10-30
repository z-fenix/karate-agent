package io.karatelabs.ext.mcp;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.Map;

public class Mcp extends JsDispatch {
   public Mcp() {
      this.fn("connect", var1 -> this.connect(JsApi.str(var1, 0), JsApi.arg(var1, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public Object connect(String var1, Object var2) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("mcp", "openapi");
      if (var1 == null) {
         return JsApi.missing("Mcp.connect(url) needs the server endpoint, e.g. 'http://localhost:8080/api/mcp'");
      } else {
         McpSession var3 = new McpSession();
         var3.lenient = true;
         var3.putMember("url", var1);
         if (var2 instanceof Map var4) {
            var4.forEach((var1x, var2x) -> var3.putMember(String.valueOf(var1x), var2x));
         }

         var var5 = var3.connect();
         return "connect".equals(var5.get("kind")) ? var5 : var3;
      }
   }

   private static Map<String, Object> help() {
      return helpMarkdown();
   }

   static Map<String, Object> helpMarkdown() {
      return JsApi.markdown(
         "# Mcp — explore and test an MCP server over the wire\n\n```js\nvar s = Mcp.connect('http://localhost:8080/api/mcp');  // s.server = {lane, revision, capabilities}\nvar r = s.invoke('karate_eval', { code: '1+1' });      // tools/call\ns.call('tools/list', {}, { 'MCP-Protocol-Version': '2027-01-01' });  // {status, headers, body}\n```\n\n- `Mcp.connect(url[, {headers, timeout, revision, capabilities}])` negotiates the era;\n  `revision` pins one. `s.server` is the assertion target.\n- `s.tools()` / `s.resources()` / `s.prompts()` return the JSON-RPC result verbatim, one\n  page. The envelope is era-dependent, so `match response.tools` (not the whole map) is the\n  portable assertion. Paginate:\n  `var page = s.tools(); while (page.nextCursor) page = s.call('tools/list', {cursor: page.nextCursor}).body.result;`\n- `s.invoke(name, args[, {inputs, onInputRequired, maxRounds}])` — a tool's own failure is a\n  RESULT: `{isError:true, content}` returns verbatim and never throws. Assert it:\n  `match r.isError != true` (or `== true` for a deliberate bad call). Content is a list of\n  blocks; a text block's `r.content[0].text` is often JSON-in-a-string — parse it.\n- A protocol failure is a value on this face: `{error, kind, …}` with `kind` one of\n  `connect|jsonrpc|malformed|refused` — branch on `r.error`. Transport/timeout/decode throw.\n- `s.call(method[, params[, headers]])` is one raw request, never negotiated —\n  `{status, headers, body}`, the conformance primitive; `s.stop()` tears down.\n- In a feature, `karate.channel('mcp')` has the same verbs and `help`, but they throw so the\n  step fails."
      );
   }
}
