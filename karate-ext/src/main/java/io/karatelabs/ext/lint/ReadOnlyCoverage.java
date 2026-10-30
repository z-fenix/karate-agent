package io.karatelabs.ext.lint;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReadOnlyCoverage implements SimpleObject {
   public static final List<String> GRANTED = List.of("help", "summary", "sources", "items", "status", "gaps", "tests", "hits", "dimensions", "coveringArray");
   public static final List<String> EXCLUDED = List.of("path");
   private final SimpleObject delegate;

   private ReadOnlyCoverage(SimpleObject var1) {
      this.delegate = var1;
   }

   public static ReadOnlyCoverage of(SimpleObject var0) {
      return new ReadOnlyCoverage(var0);
   }

   @Override
   public Object jsGet(String var1) {
      if ("help".equals(var1)) {
         return (JavaInvokable)var1x -> this.help();
      } else {
         return GRANTED.contains(var1) ? this.delegate.jsGet(var1) : null;
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return GRANTED;
   }

   public boolean available() {
      return this.delegate.jsGet("summary") instanceof JavaCallable var1 && var1.call(null) instanceof List;
   }

   public Set<String> ungranted() {
      var var1 = new LinkedHashSet<>(this.delegate.jsKeys());
      GRANTED.forEach(var1::remove);
      return var1;
   }

   public List<String> danglingGrants() {
      ArrayList<java.lang.String> var1 = new ArrayList();
      var var2 = this.delegate.jsKeys();

      for (String var4 : GRANTED) {
         if (!"help".equals(var4) && !var2.contains(var4)) {
            var1.add(var4);
         }
      }

      return var1;
   }

   private Map<String, Object> help() {
      return JsApi.markdown(
         "# Coverage — the read-only project graph, as a lint rule sees it\n\nThe graph-reaching half of a governance rule: the document tells you what was *declared*, this\ntells you what is *claimed* and what actually *ran*. It reads the **project** graph (the\naggregate), so it answers with no run in flight.\n\n- `Coverage.status(id)` — one item: `{id, status, callCount, tests, requirements, mockOnly,\n  oracleOnly, refusalOnly, modelOnly, notasserted}`. What the graph-reaching rule needs is on\n  this one call: `requirements` (the requirements whose `@real=` claims this artifact — empty\n  means nothing declares it), and the `mockOnly` / `oracleOnly` / `refusalOnly` (covered only\n  by refusal proofs) / `modelOnly` (only a model checker vouches) / `notasserted` (covered only\n  by tests that assert nothing) disclosures.\n- `Coverage.items([source])` · `Coverage.gaps()` · `Coverage.summary()` · `Coverage.sources()`\n- `Coverage.tests([itemId])` · `Coverage.hits([itemId])`\n- `Coverage.dimensions([itemId])` · `Coverage.coveringArray([itemId], [strength])`\n\n**What you may not do here.** This binding is read-only and it is the *whole* of a rule's\nauthority over the graph — there is no write verb, and `Coverage.path()` is excluded on purpose\n(it hands back a filesystem path, which this sandbox denies). Filesystem, network, process and\nJava interop are denied outright; see the lint result's capability statement for the full text."
      );
   }
}
