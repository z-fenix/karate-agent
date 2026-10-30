package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.CoverageEmit;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class RuleResult extends JsDispatch {
   private static final AtomicLong VERDICTS = new AtomicLong();
   private final String rulebook;
   private final String scenarioId;
   private final List<String> reqs;
   private final List<String> dataKeys;
   private final io.karatelabs.ext.rules.Rule.StepListener sink;

   RuleResult(String var1, String var2, Map<String, Object> var3, io.karatelabs.ext.rules.Rule.StepListener var4) {
      this.rulebook = var1;
      this.scenarioId = var2;
      this.sink = var4;
      Object var5 = var3.get("reqs");
      this.reqs = var5 instanceof List ? (List)var5 : List.of();
      this.dataKeys = List.copyOf(var3.keySet());
      var3.forEach((var1x, var2x) -> this.val(var1x, var2x));
      this.fn("verify", this::verify);
   }

   @Override
   public Collection<String> jsKeys() {
      return this.dataKeys;
   }

   @Override
   public String jsKind() {
      return "rule-result";
   }

   Object verify(Object... var1) {
      Object var2 = var1.length > 0 ? var1[0] : null;
      if (!(var2 instanceof Boolean) && !isVerdictWord(var2)) {
         throw new RuntimeException(
            "verify(ok, note?) expects a boolean (or 'pass'/'fail'), got "
               + (var2 == null ? "nothing" : "'" + var2 + "'")
               + " — pass the COMPARISON itself, e.g. verify(shown === r.output.total, '…')."
         );
      } else {
         boolean var3 = truthy(var2);
         String var4 = var1.length > 1 && var1[1] != null ? String.valueOf(var1[1]) : null;
         String var5 = var3 ? "pass" : "fail";
         StringBuilder var6 = new StringBuilder(var4 != null && !var4.isBlank() ? var4.trim() : this.rulebook + " matches the rules");

         for (String var8 : this.reqs) {
            var6.append(" @req=").append(var8);
         }

         if (this.rulebook != null && !this.rulebook.isBlank()) {
            var6.append(" @cov=rules:").append(this.rulebook);
         }

         if (this.sink != null) {
            this.sink.step(var6.toString(), var5, null);
         }

         if (this.rulebook != null && !this.rulebook.isBlank() && this.scenarioId != null && !this.scenarioId.isBlank()) {
            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("status", var5);
            if (var4 != null && !var4.isBlank()) {
               var12.put("note", var4);
            }

            CoverageEmit.emit("rules", this.rulebook + "/" + this.scenarioId, var12);
         }

         long var13 = VERDICTS.incrementAndGet();

         for (String var10 : this.reqs) {
            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("status", var5);
            var11.put("verified", true);
            var11.put("verdict", var13);
            if (this.rulebook != null && !this.rulebook.isBlank()) {
               var11.put("rule", this.rulebook);
            }

            CoverageEmit.emit("req", var10, var11);
         }

         if (!var3) {
            StringBuilder var15 = new StringBuilder();
            if (var4 != null && !var4.isBlank()) {
               var15.append(var4.trim()).append(" — ");
            }

            var15.append("verify failed: the observed behaviour disagrees with ")
               .append(this.rulebook != null && !this.rulebook.isBlank() ? "the '" + this.rulebook + "' rulebook" : "the rules oracle");
            if (!this.reqs.isEmpty()) {
               var15.append(" (").append(String.join(", ", this.reqs)).append(")");
            }

            var15.append(". The verdict is on the graph as FAILING evidence; fix the system or the rules,").append(" never the assertion.");
            throw new RuntimeException(var15.toString());
         } else {
            LinkedHashMap var14 = new LinkedHashMap();
            var14.put("ok", var3);
            var14.put("status", var5);
            var14.put("reqs", this.reqs);
            if (this.rulebook != null) {
               var14.put("rule", "rules:" + this.rulebook);
            }

            return var14;
         }
      }
   }

   private static boolean isVerdictWord(Object var0) {
      if (!(var0 instanceof String var1)) {
         return false;
      } else {
         String var2 = var1.trim().toLowerCase();
         return var2.equals("pass")
            || var2.equals("passed")
            || var2.equals("true")
            || var2.equals("ok")
            || var2.equals("yes")
            || var2.equals("fail")
            || var2.equals("failed")
            || var2.equals("false")
            || var2.equals("no");
      }
   }

   private static boolean truthy(Object var0) {
      if (var0 instanceof Boolean var2) {
         return var2;
      } else if (var0 == null) {
         return false;
      } else {
         String var1 = String.valueOf(var0).trim().toLowerCase();
         return var1.equals("pass") || var1.equals("passed") || var1.equals("true") || var1.equals("ok") || var1.equals("yes");
      }
   }
}
