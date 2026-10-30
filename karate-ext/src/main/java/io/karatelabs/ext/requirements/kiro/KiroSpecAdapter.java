package io.karatelabs.ext.requirements.kiro;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KiroSpecAdapter {
   public static final String a = "kiro-not-spec-workflow";
   public static final String b = "kiro";
   private static final String c = ".kiro/specs";
   private static final String d = "requirements.md";
   private static final Pattern e = Pattern.compile("^Requirement\\s+(\\d+)\\s*(?:[—–:-]\\s*(.*))?$", 2);
   private static final Pattern f = Pattern.compile("^(#{1,6})\\s+(.*)$");
   private static final Pattern g = Pattern.compile(".*\\bi\\s+want\\s+(.+?)\\s*(?:,\\s*so\\s+that\\b.*)?$", 2);
   private static final Pattern h = Pattern.compile("@key=(\\S+)");
   private static final List<String> i = List.of("requirements document", "introduction", "requirements", "glossary");

   private KiroSpecAdapter() {
   }

   public static boolean a(Path var0) {
      return var0 != null && "requirements.md".equalsIgnoreCase(String.valueOf(var0.getFileName())) && d(var0);
   }

   public static boolean b(Path var0) {
      if (var0 != null && !a(var0) && d(var0)) {
         Path var1 = var0.toAbsolutePath().normalize().getParent();
         return var1 != null && Files.isRegularFile(var1.resolve("requirements.md"));
      } else {
         return false;
      }
   }

   private static boolean d(Path var0) {
      String var1 = var0.toAbsolutePath().normalize().toString().replace('\\', '/');
      return var1.contains("/.kiro/specs/");
   }

   public static String c(Path var0) {
      Path var1 = var0 == null ? null : var0.toAbsolutePath().normalize().getParent();
      return var1 == null ? "spec" : var1.getFileName().toString();
   }

   public static io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroSpecParseResult a(String var0, String var1, String var2) {
      return a(var0, null, var1, var2);
   }

   public static io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroSpecParseResult a(String var0, String var1, String var2, String var3) {
      String var4 = var0 == null ? "" : var0;
      var var5 = a(var4);
      if (var5.isEmpty()) {
         return new io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroSpecParseResult(
            io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(var4).d(),
            Map.of(),
            List.of(
               new io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic(
                  "kiro-not-spec-workflow",
                  var3,
                  "under .kiro/specs/ but not a spec-workflow document — no `### Requirement <N>` headings, so there is nothing for a claimed-done task to join to (Kiro's vibe mode writes freestyle docs into the same folder); parsed with the plain markdown front-end instead"
               )
            )
         );
      } else {
         String var6 = var2 != null && !var2.isBlank() ? var2 : "spec";
         io.karatelabs.ext.requirements.model.RequirementsBundle var7 = io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(a(var5, var6, var4));
         var var8 = a(var7.d(), var5, var6, b(var4));
         ArrayList var9 = new ArrayList();
         Map var10 = a(var1, var8, var5, var6, var3, var9);
         return new io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroSpecParseResult(var8, var10, var9);
      }
   }

   private static Map<String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> a(
      String var0,
      List<io.karatelabs.ext.requirements.model.RequirementNode> var1,
      List<io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> var2,
      String var3,
      String var4,
      List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> var5
   ) {
      var var6 = io.karatelabs.ext.requirements.kiro.KiroTaskParser.a(var0);
      if (var6.isEmpty()) {
         return Map.of();
      } else {
         LinkedHashMap<java.lang.String, io.karatelabs.ext.requirements.model.RequirementNode> var7 = new LinkedHashMap();

         for (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection var9 : var2) {
            String var10 = a(var9, var3);
            var1.stream().filter(var1x -> var10.equals(var1x.b())).findFirst().ifPresent(var2x -> var7.put(var9.a(), var2x));
         }

         LinkedHashMap<java.lang.String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> var16 = new LinkedHashMap();
         ArrayList<java.lang.String> var17 = new ArrayList();

         for (io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask var11 : (Iterable<io.karatelabs.ext.requirements.kiro.KiroTaskParser.KiroTask>) (Iterable<?>) (var6)) {
            if (var11.c()) {
               for (String var13 : var11.d()) {
                  io.karatelabs.ext.requirements.model.RequirementNode var14 = (io.karatelabs.ext.requirements.model.RequirementNode)var7.get(io.karatelabs.ext.requirements.kiro.KiroTaskParser.b(var13));
                  String var15 = var14 == null ? null : a(var14, io.karatelabs.ext.requirements.kiro.KiroTaskParser.c(var13));
                  if (var15 == null) {
                     var17.add(var13);
                  } else {
                     var16.putIfAbsent(var14.b() + "/" + var15, new io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim(var11.a(), var11.b()));
                  }
               }
            }
         }

         if (!var17.isEmpty()) {
            var5.add(
               new io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic(
                  "kiro-task-ref-unresolved",
                  var4,
                  "tasks.md claims "
                     + var17
                     + " — no such requirement/criterion in this spec (a renumbered criterion, or a task written against an earlier draft); the reference is ignored, so those claims are NOT graded"
               )
            );
         }

         return var16;
      }
   }

   private static String a(io.karatelabs.ext.requirements.model.RequirementNode var0, String var1) {
      var var2 = io.karatelabs.ext.requirements.AcceptanceCriterion.a(var0.m());

      int var3;
      try {
         var3 = Integer.parseInt(var1 == null ? "" : var1.trim());
      } catch (NumberFormatException var5) {
         return null;
      }

      return var3 >= 1 && var3 <= var2.size() ? ((io.karatelabs.ext.requirements.AcceptanceCriterion)var2.get(var3 - 1)).a() : null;
   }

   private static List<io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> a(String var0) {
      ArrayList<io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> var1 = new ArrayList();
      String var2 = null;
      String var3 = null;
      String var4 = null;
      int var5 = 0;
      int var6 = 0;
      ArrayList<java.lang.String> var7 = new ArrayList();

      for (String var11 : var0.split("\n", -1)) {
         var6++;
         Matcher var12 = f.matcher(var11.stripTrailing());
         if (var12.matches()) {
            String var13 = var12.group(2).trim();
            Matcher var14 = e.matcher(var13);
            boolean var15 = i.contains(var13.toLowerCase());
            if (var14.matches() || var15) {
               if (var2 != null) {
                  var1.add(a(var2, var3, var5, var4, var7));
               }

               var2 = var14.matches() ? var14.group(1) : null;
               var3 = var14.matches() ? var13 : null;
               var5 = var14.matches() ? var6 : 0;
               var4 = var14.matches() ? e(var14.group(2)) : null;
               var7 = new ArrayList();
               continue;
            }
         }

         if (var2 != null) {
            var7.add(var11);
         }
      }

      if (var2 != null) {
         var1.add(a(var2, var3, var5, var4, var7));
      }

      return var1;
   }

   private static io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection a(String var0, String var1, int var2, String var3, List<String> var4) {
      String var5 = null;
      ArrayList<java.lang.String> var6 = new ArrayList(var4.size());

      for (String var8 : var4) {
         String var9 = var8.trim();
         if (var5 == null && var9.startsWith("@")) {
            Matcher var10 = h.matcher(var9);
            if (var10.find()) {
               var5 = var10.group(1);
               var8 = h.matcher(var8).replaceFirst("").stripTrailing();
               if (var8.trim().isEmpty()) {
                  continue;
               }
            }
         }

         var6.add(var8);
      }

      String var11 = var3 != null ? var3 : a(var6);
      return new io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection(var0, var1, var2, var11, var5, var6);
   }

   private static String a(List<String> var0) {
      for (String var2 : var0) {
         String var3 = var2.trim();
         if (var3.toLowerCase().startsWith("**user story:**")) {
            Matcher var4 = g.matcher(var3);
            if (var4.matches()) {
               return var4.group(1).replaceAll("[.,;]$", "").trim();
            }
         }
      }

      return null;
   }

   private static String a(List<io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> var0, String var1, String var2) {
      StringBuilder var3 = new StringBuilder("# ").append(d(var1)).append("\n\n");
      var3.append("## ").append(var1).append(": ").append(d(var1)).append("\n");
      var3.append("@type=feature\n");
      String var4 = c(var2);
      if (var4 != null) {
         var3.append(var4).append("\n");
      }

      for (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection var6 : var0) {
         var3.append("\n### ").append(a(var6, var1));
         if (var6.d() != null) {
            var3.append(": ").append(var6.d());
         }

         var3.append("\n");

         for (String var8 : var6.f()) {
            var3.append(var8).append("\n");
         }
      }

      return var3.toString();
   }

   private static io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.DocumentHeader b(String var0) {
      int var1 = 0;

      for (String var5 : var0.split("\n", -1)) {
         var1++;
         Matcher var6 = f.matcher(var5.stripTrailing());
         if (var6.matches() && var6.group(1).length() == 1) {
            return new io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.DocumentHeader(var6.group(2).trim(), var1);
         }
      }

      return null;
   }

   private static String c(String var0) {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = false;

      for (String var6 : var0.split("\n", -1)) {
         Matcher var7 = f.matcher(var6.stripTrailing());
         if (var7.matches()) {
            var2 = "introduction".equals(var7.group(2).trim().toLowerCase());
         } else {
            String var8 = var6.trim();
            if (var2 && !var8.isEmpty() && !"---".equals(var8)) {
               var1.append(var1.length() > 0 ? " " : "").append(var8);
            }
         }
      }

      return var1.length() == 0 ? null : var1.toString();
   }

   private static String a(io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection var0, String var1) {
      return var0.e() != null ? var0.e() : var1 + "." + var0.a();
   }

   private static List<io.karatelabs.ext.requirements.model.RequirementNode> a(
      List<io.karatelabs.ext.requirements.model.RequirementNode> var0, List<io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> var1, String var2, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.DocumentHeader var3
   ) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection> var4 = new LinkedHashMap();

      for (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection var6 : var1) {
         var4.put(a(var6, var2), var6);
      }

      ArrayList<io.karatelabs.ext.requirements.model.RequirementNode> var9 = new ArrayList(var0.size());

      for (io.karatelabs.ext.requirements.model.RequirementNode var7 : var0) {
         io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection var8 = (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroRequirementSection)var4.get(var7.b());
         if (var8 == null) {
            var9.add(
               new io.karatelabs.ext.requirements.model.RequirementNode(
                  var7.b(),
                  var7.c(),
                  var7.d(),
                  var7.e(),
                  var7.f(),
                  var7.g(),
                  var7.h(),
                  var7.i(),
                  var7.j(),
                  var7.k(),
                  var7.l(),
                  var7.m(),
                  var7.n(),
                  new io.karatelabs.ext.requirements.model.RequirementNode.SourceReference("kiro", var2, null),
                  var7.p(),
                  var3 == null ? null : var3.a(),
                  var3 == null ? 0 : var3.b()
               )
            );
         } else {
            var9.add(
               new io.karatelabs.ext.requirements.model.RequirementNode(
                  var7.b(),
                  var7.c(),
                  var7.d(),
                  var7.e(),
                  var7.f(),
                  var7.g(),
                  var7.h(),
                  var7.i(),
                  var7.j(),
                  var7.k(),
                  var7.l(),
                  var7.m(),
                  var7.n(),
                  new io.karatelabs.ext.requirements.model.RequirementNode.SourceReference("kiro", var8.a(), null),
                  var7.p(),
                  var8.b(),
                  var8.c()
               )
            );
         }
      }

      return var9;
   }

   private static String d(String var0) {
      String var1 = var0.replace('-', ' ').replace('_', ' ').trim();
      if (var1.isEmpty()) {
         return var0;
      } else {
         StringBuilder var2 = new StringBuilder(var1.length());

         for (String var6 : var1.split("\\s+")) {
            var2.append(var2.length() > 0 ? " " : "").append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }

         return var2.toString();
      }
   }

   private static String e(String var0) {
      return var0 != null && !var0.isBlank() ? var0.trim() : null;
   }

   private record KiroRequirementSection(String a, String b, int c, String d, String e, List<String> f) {

   }

   public record KiroTaskClaim(String a, String b) {

   }

   private record DocumentHeader(String a, int b) {

   }

   public record KiroSpecParseResult(List<io.karatelabs.ext.requirements.model.RequirementNode> b, Map<String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> c, List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> d) {


      public io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic a() {
         return this.d.isEmpty() ? null : this.d.get(0);
      }

      public List<io.karatelabs.ext.requirements.model.RequirementNode> b() {
         return this.b;
      }

      public Map<String, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim> c() {
         return this.c;
      }

      public List<io.karatelabs.ext.requirements.model.RequirementsBundle.RequirementsDiagnostic> d() {
         return this.d;
      }
   }
}
