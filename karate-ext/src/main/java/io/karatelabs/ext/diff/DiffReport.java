package io.karatelabs.ext.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiffReport {
   public static final String a = "karate-diff/v1";
   private final String c;
   private final String d;
   private final String e;
   private io.karatelabs.ext.diff.DiffReport.DiffArtifact f;
   private io.karatelabs.ext.diff.DiffReport.DiffArtifact g;
   private Map<String, Object> h;
   private boolean i;
   private final List<DiffFinding> j = new ArrayList<>();
   private final List<UnevaluatedDiff> k = new ArrayList<>();
   private String l;
   private List<Map<String, Object>> m = List.of();
   private final Map<String, Object> n = new LinkedHashMap<>();
   public static final List<String> b = List.of("breaking", "warning", "compatible");

   public DiffReport(String var1, String var2, String var3) {
      this.c = var1;
      this.d = var2;
      this.e = var3;
   }

   public String taxonomy() {
      return this.c;
   }

   public DiffFinding a(String var1, DiffSeverity var2) {
      return new DiffFinding(this.c, var1, var2);
   }

   public io.karatelabs.ext.diff.DiffReport a(io.karatelabs.ext.diff.DiffReport.DiffArtifact var1, io.karatelabs.ext.diff.DiffReport.DiffArtifact var2) {
      this.f = var1;
      this.g = var2;
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(Map<String, Object> var1) {
      this.h = var1;
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(boolean var1) {
      this.i = var1;
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(DiffFinding var1) {
      this.j.add(var1);
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(UnevaluatedDiff var1) {
      this.k.add(var1);
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(String var1) {
      this.l = var1;
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(List<Map<String, Object>> var1) {
      this.m = var1;
      return this;
   }

   public io.karatelabs.ext.diff.DiffReport a(String var1, Object var2) {
      if (var2 != null) {
         this.n.put(var1, var2);
      }

      return this;
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<io.karatelabs.ext.diff.DiffSeverity, java.util.List> var1 = new LinkedHashMap();
      LinkedHashMap<io.karatelabs.ext.diff.DiffSeverity, java.lang.Integer> var2 = new LinkedHashMap();

      for (DiffSeverity var6 : io.karatelabs.ext.diff.DiffSeverity.values()) {
         var1.put(var6, new ArrayList());
         var2.put(var6, 0);
      }

      int var13 = 0;
      LinkedHashMap<java.lang.String, java.lang.Integer> var14 = new LinkedHashMap();

      for (String var18 : List.of("mapped", "partmapped", "notmapped", "notjoined")) {
         var14.put(var18, 0);
      }

      for (DiffFinding var19 : this.j) {
         var var7 = var19.toMap();
         ((List)var1.get(var19.severity())).add(var7);
         if (var19.b() != null) {
            var14.merge(String.valueOf(var19.b().get("state")), 1, Integer::sum);
         }

         if (Boolean.TRUE.equals(var7.get("waived"))) {
            var13++;
         } else {
            var2.merge(var19.severity(), 1, Integer::sum);
         }
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var17 = new ArrayList();

      for (UnevaluatedDiff var22 : this.k) {
         var17.add(var22.toMap());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();

      for (DiffSeverity var10 : io.karatelabs.ext.diff.DiffSeverity.values()) {
         var21.put(var10.bucket(), var2.get(var10));
      }

      var21.put("waived", var13);
      var21.put("notEvaluated", var17.size());
      if (this.h != null) {
         var21.put("join", var14);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var24 = new LinkedHashMap();
      var24.put("version", "karate-diff/v1");
      LinkedHashMap<java.lang.String, java.lang.String> var25 = new LinkedHashMap();
      var25.put("name", this.d);
      var25.put("version", this.e);
      var24.put("analyzer", var25);
      var24.put("taxonomy", this.c);
      var24.put("ok", (Integer)var2.get(io.karatelabs.ext.diff.DiffSeverity.BREAKING) == 0);
      var24.put("complete", this.i && var17.isEmpty());
      var24.put("summary", var21);
      var24.put("baseline", this.f == null ? null : this.f.a());
      var24.put("current", this.g == null ? null : this.g.a());
      if (this.h != null) {
         var24.put("graph", this.h);
      }

      for (DiffSeverity var12 : io.karatelabs.ext.diff.DiffSeverity.values()) {
         var24.put(var12.bucket(), var1.get(var12));
      }

      var24.put("notEvaluated", var17);
      if (this.l != null) {
         var24.put("asOf", this.l);
      }

      var24.put("waivers", new ArrayList<>(this.m));
      var24.putAll(this.n);
      return var24;
   }

   public static Map<String, Object> a(String var0, Map<String, Object> var1) {
      Map<java.lang.String, Object> var2 = (Map)var1.get("summary");
      boolean var3 = Boolean.TRUE.equals(var1.get("ok")) && Boolean.TRUE.equals(var1.get("complete"));
      if (!"breaking".equals(var0)) {
         var3 &= var2.get(io.karatelabs.ext.diff.DiffSeverity.WARNING.bucket()) instanceof Number var4 && var4.intValue() == 0;
      }

      if ("compatible".equals(var0)) {
         var3 &= var2.get(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE.bucket()) instanceof Number var6 && var6.intValue() == 0;
      }

      return a(var0, var3, var3 ? 0 : 1);
   }

   public static Map<String, Object> b(String var0) {
      return a(var0, false, 2);
   }

   private static Map<String, Object> a(String var0, boolean var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("failOn", var0);
      var3.put("pass", var1);
      var3.put("exitCode", var2);
      return var3;
   }

   public static String b(String var0, Map<String, Object> var1) {
      Map<java.lang.String, Object> var2 = (Map)var1.get("summary");
      StringBuilder var3 = new StringBuilder("# ").append(var0).append(" — ");
      boolean var4 = Boolean.TRUE.equals(var1.get("ok"));
      boolean var5 = Boolean.TRUE.equals(var1.get("complete"));
      var3.append(!var4 ? "BREAKING" : (var5 ? "OK" : "OK, INCOMPLETE")).append("\n\n");
      var3.append("`")
         .append(((Map)var1.get("baseline")).get("source"))
         .append("` → `")
         .append(((Map)var1.get("current")).get("source"))
         .append("` · ")
         .append(var2.get("breaking"))
         .append(" breaking · ")
         .append(var2.get("warnings"))
         .append(" warnings · ")
         .append(var2.get("compatible"))
         .append(" compatible · ")
         .append(var2.get("notEvaluated"))
         .append(" not evaluated");
      if (var2.get("waived") instanceof Integer var6 && var6 > 0) {
         var3.append(" · ").append(var6).append(" waived");
      }

      var3.append("\n");
      boolean var15 = false;

      for (DiffSeverity var10 : io.karatelabs.ext.diff.DiffSeverity.values()) {
         for (Map var12 : (Iterable<Map>) (Iterable<?>) ((List)var1.get(var10.bucket()))) {
            var15 |= var12.get("join") instanceof Map var13 && !"notjoined".equals(var13.get("state"));
         }
      }

      for (DiffSeverity var25 : io.karatelabs.ext.diff.DiffSeverity.values()) {
         List var27 = (List)var1.get(var25.bucket());
         if (!var27.isEmpty()) {
            var3.append("\n## ")
               .append(var25.bucket())
               .append(var15 ? "\n\n| kind | path | detail | join |\n|---|---|---|---|\n" : "\n\n| kind | path | detail |\n|---|---|---|\n");

            for (Map var29 : (Iterable<Map>) (Iterable<?>) (var27)) {
               var3.append("| ")
                  .append(var29.get("kind"))
                  .append(Boolean.TRUE.equals(var29.get("waived")) ? " (waived)" : "")
                  .append(" | ")
                  .append(b(var29.get("path")))
                  .append(" | ")
                  .append(b(var29.get("detail")))
                  .append(" |");
               if (var15) {
                  var3.append(" ").append(b(a(var29.get("join")))).append(" |");
               }

               var3.append("\n");
            }
         }
      }

      List var18 = (List)var1.get("waivers");
      if (var18 != null && !var18.isEmpty()) {
         var3.append("\n## waivers\n\n| rule | scope | status | owner | reason |\n|---|---|---|---|---|\n");

         for (Map var23 : (Iterable<Map>) (Iterable<?>) (var18)) {
            var3.append("| ")
               .append(var23.get("rule"))
               .append(" | ")
               .append(b(var23.get("scope")))
               .append(" | ")
               .append(var23.get("status"))
               .append(" | ")
               .append(b(var23.get("owner")))
               .append(" | ")
               .append(b(var23.get("reason")))
               .append(" |\n");
         }
      }

      List var21 = (List)var1.get("notEvaluated");
      if (!var21.isEmpty()) {
         var3.append("\n## notEvaluated\n\n| reason | construct | side | pointer |\n|---|---|---|---|\n");

         for (Map var26 : (Iterable<Map>) (Iterable<?>) (var21)) {
            var3.append("| ")
               .append(var26.get("reason"))
               .append(" | ")
               .append(var26.get("construct"))
               .append(" | ")
               .append(var26.get("side"))
               .append(" | ")
               .append(b(var26.get("pointer")))
               .append(" |\n");
         }
      }

      return var3.toString();
   }

   private static String a(Object var0) {
      if (var0 instanceof Map var1) {
         ArrayList<java.lang.String> var2 = new ArrayList();
         var2.add(String.valueOf(var1.get("state")));
         if (var1.get("requirements") instanceof List var3 && !var3.isEmpty()) {
            var2.add(String.join(", ", var3.stream().map(var0x -> String.valueOf(((Map)var0x).get("id"))).toList()));
         }

         if (var1.get("tests") instanceof List var5 && !var5.isEmpty()) {
            var2.add(String.join(", ", var5.stream().map(var0x -> {
               Map<java.lang.String, Object> var1x = (Map)var0x;
               return var1x.get("file") + ":" + var1x.get("line");
            }).toList()));
         }

         return String.join(" · ", var2);
      } else {
         return "";
      }
   }

   private static String b(Object var0) {
      return var0 == null ? "" : String.valueOf(var0).replace("|", "\\|").replace("\n", " ");
   }

   public record DiffArtifact(String b, String c, String d) {

      public static io.karatelabs.ext.diff.DiffReport.DiffArtifact a(String var0, String var1, String var2) {
         return new io.karatelabs.ext.diff.DiffReport.DiffArtifact(var0, "sha256:" + var1, var2);
      }

      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("source", this.b);
         var1.put("digest", this.c);
         var1.put("basis", this.d);
         return var1;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }
   }
}
