package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class JsEngine extends JsDispatch {
   private final Engine engine;

   public JsEngine(Engine var1) {
      this.engine = var1;
      this.fn("vars", var1x -> this.vars());
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   Object vars() {
      ArrayList<java.util.LinkedHashMap> var1 = new ArrayList();

      for (Entry var3 : this.engine.getBindings().entrySet()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("name", var3.getKey());
         Object var5 = var3.getValue();
         if (var5 instanceof SimpleObject var6) {
            String var7 = var6 instanceof JsDispatch var8 ? var8.jsKind() : null;
            var4.put("kind", var7 != null ? var7 : "object");
            if (var6 instanceof JsDispatch var11) {
               var var9 = var11.jsVerbs();
               if (var9 != null && !var9.isEmpty()) {
                  var4.put("verbs", new ArrayList(var9));
               }
            }
         } else if (isFunction(var5)) {
            var4.put("kind", "function");
         } else {
            var4.put("kind", "data");
         }

         var1.add(var4);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
      var10.put("vars", var1);
      var10.put("count", var1.size());
      return JsApi.hint(
         var10,
         var1.isEmpty()
            ? "no vars yet — capture handles in a var (e.g. `var mock = Http.mock(...)`); they persist across calls as your working memory"
            : "these persist across calls; read one by just evaluating its name (e.g. end a call with `mock`)"
      );
   }

   private static boolean isFunction(Object var0) {
      return var0 instanceof JsFunction || var0 instanceof JavaInvokable || var0 instanceof JavaCallable;
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Engine — your working memory (the persistent JS engine)\n\nThe serve console is **one persistent JS engine**: every top-level `var`, `function`, and\nassignment survives into your next `/api/eval` call — same scope, same heap. Capture live\nhandles and reuse them across calls (the `Robot.create() → bot` pattern):\n\n```js\nvar mock = Http.mock({ js: 'response.body = {ok:true}', port: 0 });  // kept alive\nmock.url            // use it\nmock.reset()        // tweak it later, same handle\n```\n\nA result that is a live handle comes back with a `ref` envelope field (`{kind, verbs}`) —\nthat's your cue it persists and what you can call on it.\n\n- `Engine.vars()` — your variables: `[{name, kind, verbs?}]`. `kind` is the handle\n  kind (`mock`, with its `verbs`) or `object`/`function`/`data`. Use to recall what you\n  hold (especially after a long session or a context compaction). This is the only way to\n  enumerate them — there is no `globalThis` here and vars are not on `this`.\n- **To read one var, just evaluate its name** — end a call with `mock` and you get its wire\n  snapshot. (No `get(name)` verb: it would only duplicate plain evaluation.)\n\nBound namespaces (`Coverage`, `Http`, `Engine`, …) are not listed — only what you created.\n(A *run* is an execution — see `Harness.runs()` / the `/runs` page — not this memory.)"
      );
   }
}
