package io.karatelabs.ext.openapi.changes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ApiChangelog {
   public static final String a = "karate-changelog/v1";
   public static final int b = 65536;
   private static final int c = 120;
   private static final List<String> d = List.of("BREAKING", "WARNING", "COMPATIBLE");

   private ApiChangelog() {
   }

   public static Map<String, Object> a(Map<String, Object> var0) {
      var var1 = b(var0);
      var1.put("markdown", a(var1, 65536));
      return var1;
   }

   public static String a(Map<String, Object> var0, int var1) {
      Map var2 = "karate-changelog/v1".equals(var0.get("schema")) ? var0 : b(var0);
      return e(var2).a(var1);
   }

   private static Map<String, Object> b(Map<String, Object> var0) {
      LinkedHashMap<String, List<Map<String, Object>>> var1 = new LinkedHashMap<>();
      ArrayList<Map<String, Object>> var2 = new ArrayList<>();

      for (String var4 : List.of("breaking", "warnings", "compatible")) {
         for (Map var6 : (Iterable<Map>) (Iterable<?>) ((List)var0.get(var4))) {
            if (var6.get("subject") instanceof Map var7) {
               var1.computeIfAbsent(String.valueOf(var7.get("key")), var0x -> new ArrayList<>()).add(var6);
            } else {
               var2.add(var6);
            }
         }
      }

      ArrayList<Map<String, Object>> var9 = new ArrayList<>();
      var1.forEach((var1x, var2x) -> var9.add(a(var1x, (List<Map<String, Object>>)var2x)));
      var9.sort(
         Comparator.comparingInt(io.karatelabs.ext.openapi.changes.ApiChangelog::d)
            .thenComparing(var0x -> (String)var0x.get("path"), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(var0x -> (String)var0x.get("webhook"), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(var0x -> (String)var0x.get("method"))
            .thenComparing(var0x -> (String)var0x.get("key"))
      );
      TreeMap<java.lang.String, java.lang.Integer> var10 = new TreeMap();

      for (Map var13 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("notEvaluated"))) {
         var10.merge(String.valueOf(var13.get("reason")), 1, Integer::sum);
      }

      ArrayList<java.util.LinkedHashMap> var12 = new ArrayList();
      var10.forEach((var1x, var2x) -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("reason", var1x);
         var3.put("count", var2x);
         var12.add(var3);
      });
      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("schema", "karate-changelog/v1");
      var14.put("taxonomy", var0.get("taxonomy"));
      var14.put("baseline", a(var0.get("baseline")));
      var14.put("current", a(var0.get("current")));
      var14.put("ok", var0.get("ok"));
      var14.put("complete", var0.get("complete"));
      var14.put("summary", var0.get("summary"));
      if (var0.get("asOf") != null) {
         var14.put("asOf", var0.get("asOf"));
      }

      if (var0.get("gate") != null) {
         var14.put("gate", var0.get("gate"));
      }

      var14.put("operations", var9);
      var14.put("document", a(var2, null));
      var14.put("notEvaluated", var12);
      var14.put("waivers", var0.get("waivers"));
      return var14;
   }

   private static Map<String, Object> a(Object var0) {
      Map<java.lang.String, Object> var1 = (Map)var0;
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("source", var1.get("source"));
      var2.put("digest", var1.get("digest"));
      return var2;
   }

   private static Map<String, Object> a(String var0, List<Map<String, Object>> var1) {
      Map<java.lang.String, Object> var2 = (Map)((Map)var1.get(0)).get("operation");
      Map<String, Object> var3 = var2;

      for (Map var5 : var1) {
         if (var5.get("moved") instanceof Map var6) {
            var3 = var6;
            break;
         }
      }

      boolean var14 = var3 != var2;
      String var15 = (String)var3.get("method");
      String var16 = (String)var3.get("path");
      String var17 = (String)var3.get("webhook");
      String var8 = (String)var3.get("operationId");
      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("key", var14 ? a(var15, var16, var17) : var0);
      var9.put("method", var15);
      if (var16 != null) {
         var9.put("path", var16);
      }

      if (var17 != null) {
         var9.put("webhook", var17);
      }

      if (var8 != null) {
         var9.put("operationId", var8);
      }

      if (var14) {
         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("key", var0);
         var10.put("method", var2.get("method"));
         if (var2.get("path") != null) {
            var10.put("path", var2.get("path"));
         }

         if (var2.get("webhook") != null) {
            var10.put("webhook", var2.get("webhook"));
         }

         if (var2.get("operationId") != null) {
            var10.put("operationId", var2.get("operationId"));
         }

         var9.put("was", var10);
      }

      for (Map var11 : var1) {
         if (var11.get("join") instanceof Map var12) {
            if (!"notjoined".equals(var12.get("state"))) {
               var9.put("join", var12);
            }
            break;
         }
      }

      var9.put("changes", a(var1, var0));
      return var9;
   }

   private static String a(String var0, String var1, String var2) {
      return var2 != null ? "webhook " + var2 + " " + var0 : var0 + " " + var1.replaceAll("\\{[^}]*}", "{}");
   }

   private static List<Map<String, Object>> a(List<Map<String, Object>> var0, String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (Map var4 : var0) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("id", var4.get("id"));
         var5.put("level", var4.get("level"));
         var5.put("kind", var4.get("kind"));
         var5.put("text", a(var4, var1));
         if (var4.get("oasdiff") != null) {
            var5.put("oasdiff", var4.get("oasdiff"));
         }

         var5.put("waived", var4.get("waived"));
         if (var4.get("waiver") instanceof Map var6) {
            LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();

            for (String var9 : List.of("rule", "scope", "owner", "reason", "expires")) {
               if (var6.get(var9) != null) {
                  var11.put(var9, var6.get(var9));
               }
            }

            var5.put("waiver", var11);
         }

         if (var4.get("witness") instanceof Map var10) {
            LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
            var13.put("instance", var10.get("instance"));
            var13.put("validUnder", var10.get("validUnder"));
            var13.put("direction", var4.get("direction"));
            var13.put("class", var10.get("class"));
            var13.put("truncated", var10.get("truncated"));
            var13.put("sha256", var10.get("sha256"));
            var5.put("counterexample", var13);
         }

         var2.add(var5);
      }

      var2.sort(
         Comparator.<Map<String, Object>>comparingInt(io.karatelabs.ext.openapi.changes.ApiChangelog::c)
            .thenComparing(var0x -> (String)var0x.get("text"))
            .thenComparing(var0x -> (String)var0x.get("id"))
      );
      return var2;
   }

   private static String a(Map<String, Object> var0, String var1) {
      String var2 = var0.get("path") == null ? "" : String.valueOf(var0.get("path"));
      if (var1 != null && var2.startsWith(var1)) {
         var2 = var2.substring(var1.length()).strip();
      }

      Object var3 = var0.get("detail");
      return var3 == null ? var2 : (var2.isEmpty() ? String.valueOf(var3) : var2 + " — " + var3);
   }

   private static int c(Map<String, Object> var0) {
      int var1 = d.indexOf(String.valueOf(var0.get("level")));
      return Boolean.TRUE.equals(var0.get("waived")) ? 3 + var1 : var1;
   }

   private static int d(Map<String, Object> var0) {
      int var1 = 3;

      for (Map var3 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("changes"))) {
         var1 = Math.min(var1, Math.min(c(var3), 3));
      }

      return var1;
   }

   private static String a(int[] var0, boolean var1) {
      return "\n… [truncated: "
         + var0[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.B.ordinal()]
         + " breaking, "
         + var0[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.W.ordinal()]
         + " warnings, "
         + var0[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.C.ordinal()]
         + " compatible, "
         + var0[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.V.ordinal()]
         + " waived, "
         + var0[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.R.ordinal()]
         + " waiver rows"
         + (var1 ? ", the not-evaluated table" : "")
         + " not shown — the JSON has them]\n";
   }

   private static String a(String var0, int var1) {
      String var2 = b(var0, Math.max(0, var1 - a(var0.length()).length()));
      return var2 + a(var0.length() - var2.length());
   }

   private static String b(String var0, int var1) {
      if (var0.length() <= var1) {
         return var0;
      } else {
         int var2 = Math.max(0, var1);
         return var0.substring(0, var2 > 0 && Character.isHighSurrogate(var0.charAt(var2 - 1)) ? var2 - 1 : var2);
      }
   }

   private static String a(int var0) {
      return "… [cut, " + var0 + " characters]\n";
   }

   private static io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogDocument e(Map<String, Object> var0) {
      Map<java.lang.String, Object> var1 = (Map)var0.get("summary");
      StringBuilder var2 = new StringBuilder("## API changes — ");
      boolean var3 = Boolean.TRUE.equals(var0.get("ok"));
      var2.append(!var3 ? "BREAKING" : (Boolean.TRUE.equals(var0.get("complete")) ? "OK" : "OK, INCOMPLETE")).append("\n\n");
      if (var0.get("gate") instanceof Map var4) {
         var2.append("gate: failOn ")
            .append(var4.get("failOn"))
            .append(" — ")
            .append(Boolean.TRUE.equals(var4.get("pass")) ? "pass" : "fail")
            .append(" (exit ")
            .append(var4.get("exitCode"))
            .append(")\n");
      }

      var2.append('`')
         .append(((Map)var0.get("baseline")).get("source"))
         .append("` → `")
         .append(((Map)var0.get("current")).get("source"))
         .append("` · ")
         .append(var1.get("breaking"))
         .append(" breaking · ")
         .append(var1.get("warnings"))
         .append(" warnings · ")
         .append(var1.get("compatible"))
         .append(" compatible · ")
         .append(var1.get("notEvaluated"))
         .append(" not evaluated · ")
         .append(var1.get("waived"))
         .append(" waived\n");
      ArrayList<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection> var11 = new ArrayList();

      for (Map var6 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("operations"))) {
         StringBuilder var7 = new StringBuilder("\n### ").append(f(var6));
         if (var6.get("was") instanceof Map var8) {
            String var20 = f(var8);
            Object var10 = var8.get("operationId") == null ? "no operationId" : var8.get("operationId");
            var7.append(" (was ").append(var20.equals(f(var6)) ? var10 : var20).append(')');
         }

         var7.append("\n\n");
         if (var6.get("join") instanceof Map var17) {
            var7.append(g(var17)).append("\n\n");
         }

         var11.add(new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection(var7.toString(), a((List<Map<String, Object>>)var6.get("changes")), true));
      }

      List var13 = (List)var0.get("document");
      if (!var13.isEmpty()) {
         var11.add(new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection("\n### Document\n\n", a(var13), true));
      }

      List var14 = (List)var0.get("notEvaluated");
      if (!var14.isEmpty()) {
         StringBuilder var15 = new StringBuilder("\n### Not evaluated — ").append(var1.get("notEvaluated")).append("\n\n| reason | count |\n|---|---|\n");

         for (Map var22 : (Iterable<Map>) (Iterable<?>) (var14)) {
            var15.append("| ").append(var22.get("reason")).append(" | ").append(var22.get("count")).append(" |\n");
         }

         var11.add(new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection(var15.toString(), List.of(), false));
      }

      List var16 = (List)var0.get("waivers");
      if (var16 != null && !var16.isEmpty()) {
         ArrayList<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> var19 = new ArrayList();

         for (Map var24 : (Iterable<Map>) (Iterable<?>) (var16)) {
            var19.add(
               new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem(
                  io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.R,
                  1,
                  "| "
                     + var24.get("rule")
                     + " | "
                     + c(var24.get("scope"))
                     + " | "
                     + var24.get("status")
                     + " | "
                     + c(var24.get("owner"))
                     + " | "
                     + c(var24.get("reason"))
                     + " |\n",
                  ""
               )
            );
         }

         var11.add(new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection("\n### Waivers\n\n| rule | scope | status | owner | reason |\n|---|---|---|---|---|\n", var19, true));
      }

      return new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogDocument(var2.toString(), var11);
   }

   private static String f(Map<String, Object> var0) {
      return var0.get("webhook") != null ? "webhook " + var0.get("webhook") + " " + var0.get("method") : var0.get("method") + " " + var0.get("path");
   }

   private static String g(Map<?, ?> var0) {
      String var1 = String.valueOf(var0.get("state"));
      if (!var1.equals("mapped") && !var1.equals("partmapped")) {
         return "join: " + var1;
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();
         if (var0.get("requirements") instanceof List var3) {
            var3.forEach(var1x -> var2.add(String.valueOf(((Map)var1x).get("id"))));
         }

         int var6 = var0.get("tests") instanceof List var7 ? var7.size() : 0;
         return "requirements: " + (var2.isEmpty() ? "none" : String.join(", ", var2)) + " · tests: " + var6;
      }
   }

   private static List<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> a(List<Map<String, Object>> var0) {
      ArrayList<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> var1 = new ArrayList();
      StringBuilder var2 = new StringBuilder();
      int var3 = 0;
      ArrayList<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> var4 = new ArrayList();

      for (Map var6 : var0) {
         String var7 = String.valueOf(var6.get("level"));
         String var8 = "`" + var6.get("kind") + "` " + b(var6.get("text"));
         if (Boolean.TRUE.equals(var6.get("waived"))) {
            Map<java.lang.String, Object> var9 = var6.get("waiver") instanceof Map var10 ? var10 : Map.of();
            var4.add(
               new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem(
                  io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.V,
                  1,
                  "- ~~" + var7 + "~~ waived " + var8 + " — " + b(var9.get("owner")) + ": " + b(var9.get("reason")) + "\n",
                  ""
               )
            );
         } else if (var7.equals("COMPATIBLE")) {
            var2.append("- ").append(var8).append('\n');
            var3++;
         } else {
            StringBuilder var12 = new StringBuilder("- **").append(var7).append("** ").append(var8).append('\n');
            if (var7.equals("BREAKING") && var6.get("counterexample") instanceof Map var13) {
               var12.append("  - counterexample (")
                  .append(var13.get("direction"))
                  .append(", ")
                  .append(var13.get("class"))
                  .append(", valid under ")
                  .append(var13.get("validUnder"))
                  .append("): ")
                  .append(h(var13))
                  .append('\n');
            }

            var1.add(
               new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem(
                  var7.equals("BREAKING") ? io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.B : io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.W, 1, var12.toString(), ""
               )
            );
         }
      }

      if (var3 > 0) {
         var1.add(
            new io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem(
               io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.C, var3, "\n<details><summary>" + var3 + " compatible</summary>\n\n" + var2, "\n</details>\n"
            )
         );
         if (!var4.isEmpty()) {
            ((io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem)var4.get(0)).c = "\n" + ((io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem)var4.get(0)).c;
         }
      }

      var1.addAll(var4);
      return var1;
   }

   private static String h(Map<String, Object> var0) {
      boolean var1 = Boolean.TRUE.equals(var0.get("truncated"));
      String var2 = var1 ? String.valueOf(var0.get("instance")) : io.karatelabs.ext.common.CanonicalJson.a(var0.get("instance"));
      if (var2.length() > 120) {
         var2 = b(var2, 120);
         var1 = true;
      }

      String var3 = a(var2);
      return var1 ? var3 + " … [truncated] sha256:" + String.valueOf(var0.get("sha256")).substring(0, 12) : var3;
   }

   private static String a(String var0) {
      int var1 = 0;
      int var2 = 0;

      for (int var3 = 0; var3 < var0.length(); var3++) {
         var2 = var0.charAt(var3) == '`' ? var2 + 1 : 0;
         var1 = Math.max(var1, var2);
      }

      String var5 = "`".repeat(var1 + 1);
      String var4 = !var0.startsWith("`") && !var0.endsWith("`") ? "" : " ";
      return var5 + var4 + var0 + var4 + var5;
   }

   private static String b(Object var0) {
      return var0 == null ? "" : String.valueOf(var0).replace("\r", " ").replace("\n", " ");
   }

   private static String c(Object var0) {
      return b(var0).replace("|", "\\|");
   }

   private static enum ChangelogCategory {
      C,
      W,
      R,
      V,
      B;
   }

   private record ChangelogDocument(String c, List<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection> d) {


      String a() {
         StringBuilder var1 = new StringBuilder(this.c);

         for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection var3 : this.d) {
            if (var3.a()) {
               var1.append(var3.c());

               for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem var5 : var3.d()) {
                  if (!var5.e) {
                     var1.append(var5.c).append(var5.d);
                  }
               }
            }
         }

         return var1.toString();
      }

      int b() {
         int var1 = this.c.length();

         for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection var3 : this.d) {
            var1 += var3.b();
         }

         return var1;
      }

      String a(int var1) {
         if (this.b() <= var1) {
            return this.a();
         } else {
            int[] var2 = new int[io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.values().length];

            for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection var4 : this.d) {
               for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem var6 : var4.d()) {
                  var2[var6.a.ordinal()] += var6.b;
               }
            }

            int var16 = var1 - io.karatelabs.ext.openapi.changes.ApiChangelog.a(var2, false).length();
            int var17 = this.c.length();

            for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection var21 : this.d) {
               var17 += var21.e() ? 0 : var21.c().length();
            }

            for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection var22 : this.d) {
               int var7 = var16 - var17 - (var22.e() ? var22.c().length() : 0);

               for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem var9 : var22.d()) {
                  if (var9.a() > var7) {
                     var9.c = io.karatelabs.ext.openapi.changes.ApiChangelog.a(var9.c, var7 - var9.d.length());
                  }
               }
            }

            int[] var20 = new int[var2.length];
            int var23 = this.b();

            for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory var10 : io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory.values()) {
               for (int var11 = this.d.size() - 1; var11 >= 0 && var23 > var16; var11--) {
                  var var12 = this.d.get(var11).d();

                  for (int var13 = var12.size() - 1; var13 >= 0 && var23 > var16; var13--) {
                     io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem var14 = (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem)var12.get(var13);
                     if (var14.a == var10 && !var14.e) {
                        int var15 = this.d.get(var11).b();
                        var14.e = true;
                        var23 += this.d.get(var11).b() - var15;
                        var20[var10.ordinal()] += var14.b;
                     }
                  }
               }
            }

            boolean var25 = false;

            for (int var33 : var20) {
               var25 |= var33 > 0;
            }

            String var28 = var25 ? this.a() + io.karatelabs.ext.openapi.changes.ApiChangelog.a(var20, false) : this.a();
            if (var28.length() <= var1) {
               return var28;
            } else {
               boolean var31 = this.d.stream().anyMatch(var0 -> !var0.e());
               return io.karatelabs.ext.openapi.changes.ApiChangelog.b(this.c + io.karatelabs.ext.openapi.changes.ApiChangelog.a(var20, var31), var1);
            }
         }
      }

      public String c() {
         return this.c;
      }

      public List<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogSection> d() {
         return this.d;
      }
   }

   private record ChangelogSection(String c, List<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> d, boolean e) {


      boolean a() {
         return !this.e || this.d.stream().anyMatch(var0 -> !var0.e);
      }

      int b() {
         if (!this.a()) {
            return 0;
         } else {
            int var1 = this.c.length();

            for (io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem var3 : this.d) {
               var1 += var3.e ? 0 : var3.a();
            }

            return var1;
         }
      }

      public String c() {
         return this.c;
      }

      public List<io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogItem> d() {
         return this.d;
      }

      public boolean e() {
         return this.e;
      }
   }

   private static final class ChangelogItem {
      final io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory a;
      final int b;
      String c;
      final String d;
      boolean e;

      ChangelogItem(io.karatelabs.ext.openapi.changes.ApiChangelog.ChangelogCategory var1, int var2, String var3, String var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
      }

      int a() {
         return this.c.length() + this.d.length();
      }
   }
}
