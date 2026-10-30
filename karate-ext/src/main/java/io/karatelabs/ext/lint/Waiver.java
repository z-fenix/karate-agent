package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public record Waiver(String b, String c, String d, String e, io.karatelabs.ext.lint.Waiver.WaiverSource f, String g) {
   static final List<String> VALID_KEYS = List.of("rule", "scope", "reason", "owner", "source", "expires");

   public Waiver(String b, String c, String d, String e, io.karatelabs.ext.lint.Waiver.WaiverSource f, String g) {
      if (b != null && !b.isBlank()) {
         c = c == null ? "" : c;
         f = f == null ? io.karatelabs.ext.lint.Waiver.WaiverSource.FILE : f;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      } else {
         throw new LintException("a waiver needs a `rule` (an id, or '*' for every rule in scope)");
      }
   }

   public static Waiver parse(Object var0) {
      var var1 = validateKeysAndCopy(var0, VALID_KEYS);
      String var2 = stringValue(var1.get("source"));
      io.karatelabs.ext.lint.Waiver.WaiverSource var3 = io.karatelabs.ext.lint.Waiver.WaiverSource.FILE;
      if (var2 != null) {
         var3 = null;

         for (io.karatelabs.ext.lint.Waiver.WaiverSource var7 : io.karatelabs.ext.lint.Waiver.WaiverSource.values()) {
            if (var7.wire.equals(var2)) {
               var3 = var7;
            }
         }

         if (var3 == null) {
            throw new LintException(
               "waiver: unknown source '"
                  + var2
                  + "' (known: project — the project's waiver register, and inline — comment-anchored in the spec document"
                  + ("file".equals(var2) ? "; `file` was renamed to `project`" : "")
                  + ")"
            );
         }
      }

      return new Waiver(stringValue(var1.get("rule")), stringValue(var1.get("scope")), stringValue(var1.get("reason")), stringValue(var1.get("owner")), var3, stringValue(var1.get("expires")));
   }

   static Map<String, Object> validateKeysAndCopy(Object var0, List<String> var1) {
      if (var0 instanceof Map var2) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var6 = String.valueOf(var5.getKey());
            if (!var1.contains(var6)) {
               var var7 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var6, var1);
               throw new LintException(
                  "waiver: unknown key '"
                     + var6
                     + "'"
                     + (var7.isEmpty() ? "" : " — did you mean " + String.join(" / ", var7) + "?")
                     + " (known: "
                     + String.join(", ", var1)
                     + ")"
               );
            }

            var3.put(var6, var5.getValue());
         }

         return var3;
      } else {
         throw new LintException("a waiver must be an object of " + String.join("/", var1) + ", got " + (var0 == null ? "null" : var0.getClass().getSimpleName()));
      }
   }

   public boolean matches(String var1, String var2) {
      if (!"*".equals(this.b) && !this.b.equals(var1)) {
         return false;
      } else if (this.f == io.karatelabs.ext.lint.Waiver.WaiverSource.INLINE) {
         return !this.c.isEmpty() && var2.equals(this.c);
      } else {
         return this.c.isEmpty() ? true : var2.equals(this.c) || var2.startsWith(this.c.endsWith("/") ? this.c : this.c + "/");
      }
   }

   public List<String> missingRequiredFields(io.karatelabs.ext.lint.LintRule.WaiverPolicy var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : var1.c()) {
         String var5 = switch (var4) {
            case "reason" -> this.d;
            case "owner" -> this.e;
            case "scope" -> this.c.isEmpty() ? null : this.c;
            case "expires" -> this.g;
            default -> throw new LintException("a waiver policy cannot require the unknown field '" + var4 + "' (known: reason, owner, scope, expires)");
         };
         if (var5 == null || var5.isBlank()) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("rule", this.b);
      var1.put("scope", this.c);
      var1.put("reason", this.d);
      var1.put("owner", this.e);
      var1.put("source", this.f.wire());
      if (this.g != null) {
         var1.put("expires", this.g);
      }

      return var1;
   }

   private static String stringValue(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   public static enum WaiverSource {
      FILE("project"),
      INLINE("inline");

      private final String wire;

      private WaiverSource(String var3) {
         this.wire = var3;
      }

      public String wire() {
         return this.wire;
      }
   }
}
