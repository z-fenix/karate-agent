package io.karatelabs.ext.requirements;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequirementsMarkdownParser {
   private static final Pattern a = Pattern.compile("^(#{1,6})\\s+(.*)$");
   private static final Pattern b = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");
   private static final Pattern c = Pattern.compile("^-\\s*tags:\\s*(.*)$", 2);
   private static final Pattern d = Pattern.compile("^(?:[-*]|\\d+[.)])\\s+(.*)$");
   private static final Pattern e = Pattern.compile("^[-*]\\s+.*$");
   private static final Pattern f = Pattern.compile("^(acceptance(\\s+criteria)?|constraints)$");
   private static final Pattern g = Pattern.compile("@([A-Za-z0-9_.-]+)=(\\S+)");
   private final List<io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft> h = new ArrayList<>();
   private final Deque<int[]> i = new ArrayDeque<>();
   private io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft j;
   private io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind k = io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.BODY;

   private RequirementsMarkdownParser() {
   }

   public static io.karatelabs.ext.requirements.model.RequirementsBundle a(String var0) {
      return new RequirementsMarkdownParser().e(var0 == null ? "" : var0);
   }

   private io.karatelabs.ext.requirements.model.RequirementsBundle e(String var1) {
      int var2 = 0;

      for (String var6 : var1.split("\n", -1)) {
         var2++;
         String var7 = m(var6);
         Matcher var8 = a.matcher(var7);
         if (var8.matches()) {
            this.a(var8.group(1).length(), var8.group(2).trim(), var2);
         } else if (this.j != null) {
            String var9 = var7.trim();
            if (!var9.isEmpty()) {
               io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind var10 = l(j(var9));
               if (var10 != null) {
                  this.k = var10;
               } else {
                  Matcher var11 = c.matcher(var9);
                  if (var11.matches()) {
                     this.b(this.j, var11.group(1));
                  } else if (var9.startsWith("@") && var9.contains("=")) {
                     this.a(this.j, var9);
                  } else {
                     Matcher var12 = d.matcher(var9);
                     if (var12.matches()) {
                        String var13 = var12.group(1).trim();
                        if (this.k == io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.ACCEPTANCE) {
                           this.j.p.add(var13);
                        } else if (this.k == io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.CONSTRAINTS) {
                           this.j.q.add(i(var13));
                        } else {
                           this.j.o.add(e.matcher(var9).matches() ? var13 : var9);
                        }
                     } else if (this.k == io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.BODY) {
                        this.j.o.add(var9);
                     }
                  }
               }
            }
         }
      }

      this.a();
      return io.karatelabs.ext.requirements.model.RequirementsBundle.a(this.b());
   }

   private void a(int var1, String var2, int var3) {
      io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind var4 = this.j == null ? null : l(k(var2));
      if (var4 != null) {
         this.k = var4;
      } else {
         this.a();
         if (var1 <= 1) {
            this.j = null;
            this.i.clear();
         } else {
            String[] var5 = f(var2);
            io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft var6 = new io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft();
            var6.a = var5[0];
            var6.c = var5[1];
            var6.i = var2;
            var6.j = var3;
            var6.k = var1;

            while (!this.i.isEmpty() && ((int[])this.i.peek())[0] >= var1) {
               this.i.pop();
            }

            if (!this.i.isEmpty()) {
               var6.d = this.h.get(this.i.peek()[1]).a;
            }

            this.j = var6;
            this.k = io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.BODY;
         }
      }
   }

   private void a() {
      if (this.j != null) {
         int var1 = this.h.size();
         this.h.add(this.j);
         this.i.push(new int[]{this.j.k, var1});
         this.j = null;
      }
   }

   private List<io.karatelabs.ext.requirements.model.RequirementNode> b() {
      ArrayList<io.karatelabs.ext.requirements.model.RequirementNode> var1 = new ArrayList(this.h.size());

      for (io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft var3 : this.h) {
         var1.add(var3.a());
      }

      return var1;
   }

   private static String[] f(String var0) {
      Matcher var1 = b.matcher(var0);
      if (var1.matches()) {
         return new String[]{var1.group(1).trim(), var1.group(2).trim()};
      } else {
         int var2 = var0.indexOf(58);
         if (var2 > 0) {
            String var3 = var0.substring(0, var2).trim();
            if (!var3.isEmpty() && !var3.contains(" ")) {
               return new String[]{var3, var0.substring(var2 + 1).trim()};
            }
         }

         return new String[]{h(var0), var0};
      }
   }

   public static boolean b(String var0) {
      if (var0 == null) {
         return false;
      } else {
         for (String var4 : var0.split("\r?\n")) {
            String var5 = var4.trim();
            if (g.matcher(var5).find() && var5.startsWith("@")) {
               return true;
            }

            String var6 = g(var5);
            if (var6 != null && f.matcher(var6).matches()) {
               return true;
            }

            Matcher var7 = a.matcher(var4);
            if (var7.matches() && var7.group(1).length() >= 2 && c(var7.group(2).trim())) {
               return true;
            }
         }

         return false;
      }
   }

   private static String g(String var0) {
      Matcher var1 = a.matcher(var0);
      if (var1.matches()) {
         return var1.group(2).trim().replaceAll(":$", "").toLowerCase();
      } else {
         return var0.startsWith("**") && var0.endsWith("**") ? var0.substring(2, var0.length() - 2).trim().replaceAll(":$", "").toLowerCase() : null;
      }
   }

   public static boolean c(String var0) {
      if (b.matcher(var0).matches()) {
         return true;
      } else {
         int var1 = var0.indexOf(58);
         String var2 = var1 > 0 ? var0.substring(0, var1).trim() : "";
         return !var2.isEmpty() && !var2.contains(" ");
      }
   }

   private static String h(String var0) {
      String var1 = var0.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
      return var1.isEmpty() ? "req" : var1;
   }

   public static String d(String var0) {
      return h(var0 == null ? "" : var0);
   }

   private void a(io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft var1, String var2) {
      Matcher var3 = g.matcher(var2);
      int var4 = 0;

      StringBuilder var5;
      for (var5 = new StringBuilder(); var3.find(); var4 = var3.end()) {
         String var6 = var2.substring(var4, var3.start()).trim();
         if (!var6.isEmpty()) {
            a(var5, var6);
         }

         this.a(var1, var3.group(1), var3.group(2));
      }

      String var7 = var2.substring(var4).trim();
      if (!var7.isEmpty()) {
         a(var5, var7);
      }

      if (var5.length() > 0) {
         var1.h = var1.h == null ? var5.toString() : var1.h + "; " + var5;
      }
   }

   private static void a(StringBuilder var0, String var1) {
      if (var0.length() > 0) {
         var0.append(' ');
      }

      var0.append(var1);
   }

   private void b(io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft var1, String var2) {
      for (String var6 : var2.split(",")) {
         String var7 = var6.trim();
         int var8 = var7.indexOf(61);
         if (var8 > 0) {
            this.a(var1, var7.substring(0, var8).trim(), var7.substring(var8 + 1).trim());
         }
      }
   }

   private void a(io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementDraft var1, String var2, String var3) {
      switch (var2) {
         case "type":
            var1.b = var3;
            break;
         case "status":
            var1.e = var3;
            break;
         case "priority":
            var1.f = var3;
            break;
         case "criticality":
            var1.g = var3;
            break;
         case "parent":
            var1.d = var3;
            break;
         case "ref":
            for (String var14 : var3.split(",")) {
               String var15 = var14.trim();
               if (!var15.isEmpty()) {
                  var1.m.add(var15);
               }
            }
            break;
         case "real":
            for (String var9 : var3.split(",")) {
               String var10 = var9.trim();
               if (!var10.isEmpty()) {
                  var1.n.add(var10);
               }
            }
            break;
         default:
            var1.l.put(var2, var3);
      }
   }

   private static io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec i(String var0) {
      String var1 = null;
      String var2 = null;
      Matcher var3 = g.matcher(var0);

      while (var3.find()) {
         if ("kind".equals(var3.group(1))) {
            var1 = var3.group(2);
         } else if ("check".equals(var3.group(1))) {
            var2 = var3.group(2);
         }
      }

      String var4 = g.matcher(var0).replaceAll("").trim();
      return new io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec(var4, var1, var2);
   }

   private static String j(String var0) {
      return var0.startsWith("**") && var0.endsWith("**") ? k(var0.substring(2, var0.length() - 2)) : null;
   }

   private static String k(String var0) {
      String var1 = var0.trim();
      if (var1.endsWith(":")) {
         var1 = var1.substring(0, var1.length() - 1).trim();
      }

      return var1.toLowerCase();
   }

   private static io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind l(String var0) {
      if (var0 != null && f.matcher(var0).matches()) {
         return "constraints".equals(var0) ? io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.CONSTRAINTS : io.karatelabs.ext.requirements.RequirementsMarkdownParser.RequirementSectionKind.ACCEPTANCE;
      } else {
         return null;
      }
   }

   private static String m(String var0) {
      int var1 = var0.length();

      while (var1 > 0 && (var0.charAt(var1 - 1) == '\r' || var0.charAt(var1 - 1) == ' ' || var0.charAt(var1 - 1) == '\t')) {
         var1--;
      }

      return var0.substring(0, var1);
   }

   private static final class RequirementDraft {
      String a;
      String b;
      String c;
      String d;
      String e;
      String f;
      String g;
      String h;
      String i;
      int j;
      int k;
      final Map<String, String> l = new LinkedHashMap<>();
      final List<String> m = new ArrayList<>();
      final List<String> n = new ArrayList<>();
      final List<String> o = new ArrayList<>();
      final List<String> p = new ArrayList<>();
      final List<io.karatelabs.ext.requirements.model.RequirementNode.ConstraintSpec> q = new ArrayList<>();

      io.karatelabs.ext.requirements.model.RequirementNode a() {
         String var1 = String.join(" ", this.o).trim();
         return new io.karatelabs.ext.requirements.model.RequirementNode(
            this.a,
            this.b,
            this.c,
            this.d,
            this.e,
            this.f,
            this.g,
            this.l,
            this.m,
            this.n,
            var1.isEmpty() ? null : var1,
            this.p,
            this.q,
            null,
            this.h,
            this.i,
            this.j
         );
      }
   }

   private static enum RequirementSectionKind {
      BODY,
      ACCEPTANCE,
      CONSTRAINTS;
   }
}
