package io.karatelabs.ext.robot.recording;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RecordingAnalyzer {
   private static final Set<String> a = Set.of("id", "uuid", "key", "code");
   private static final Set<String> b = Set.of("bearer", "basic", "digest", "token", "apikey", "api-key");
   private static final Set<String> c = Set.of("page-load", "polling", "duplicate");
   private static final Pattern d = Pattern.compile("\\d+(\\.\\d+)?");
   private static final Pattern e = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
   private final TrafficLog f;
   private final Map<String, Object> g;
   private final String h;
   private final Map<String, Object> i;
   private final Set<String> j;
   private final Map<String, Object> k;
   private final List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> l = new ArrayList<>();
   private final Map<Object, Map<String, Object>> m = new HashMap<>();
   private final Map<String, Object> n = new LinkedHashMap<>();
   private double o = Double.MAX_VALUE;
   private double p = 0.0;
   private final List<io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning> q = new ArrayList<>();
   private final LinkedHashSet<String> r = new LinkedHashSet<>();
   private String s;
   private final JavaScriptIdentifierSanitizer t = new JavaScriptIdentifierSanitizer();

   RecordingAnalyzer(TrafficLog var1, Map<String, Object> var2, String var3, Map<String, Object> var4, Collection<String> var5, Map<String, Object> var6) {
      this.f = var1;
      this.g = var2;
      this.h = var3;
      this.i = var4;
      this.j = (Set<String>)(var5 == null ? Set.of() : new HashSet<>(var5));
      this.k = var6 == null ? Map.of() : var6;

      for (Map var8 : var1.d()) {
         this.m.put(var8.get("seq"), var8);
         if (var8.get("value") != null) {
            this.n.putIfAbsent(String.valueOf(var8.get("value")), var8.get("seq"));
         }
      }
   }

   RobotRecording a() {
      if (this.g.get("error") != null) {
         return this.a(
            String.valueOf(this.g.get("code")), this.g.get("status") instanceof Number var7 ? var7.intValue() : 400, String.valueOf(this.g.get("error"))
         );
      } else {
         HashMap<java.lang.String, java.util.Map> var1 = new HashMap();
         HashSet<java.lang.String> var2 = new HashSet();

         for (Map var4 : this.d("kept")) {
            var1.put(String.valueOf(var4.get("id")), var4);
            var2.add(String.valueOf(var4.get("id")));
         }

         for (Map var12 : this.d("left")) {
            var1.put(String.valueOf(var12.get("id")), var12);
         }

         for (Map var13 : this.f.e()) {
            String var5 = String.valueOf(var13.get("id"));
            io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var6 = new io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange(this.l.size(), var13, var1.getOrDefault(var5, Map.of()), var2.contains(var5));
            this.l.add(var6);
            if (var6.j != null) {
               this.o = Math.min(this.o, var6.j);
               this.p = Math.max(this.p, var6.k == null ? var6.j : var6.k);
            }
         }

         if (var2.isEmpty()) {
            return this.a("EMPTY", 400, "no call was kept: nothing to convert");
         } else {
            int var11 = 0;

            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var17 : this.l) {
               if (var17.e) {
                  String var20 = d(var17);
                  if (var20 == null) {
                     var17.o = true;
                     var11++;
                  } else {
                     this.q.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning(var17.b, var20, a(var20, var17)));
                  }
               }
            }

            if (var11 == 0) {
               return this.a("UNCONVERTIBLE", 422, "no kept call can be a card: " + this.q.stream().map(io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning::b).toList());
            } else {
               this.b();

               for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var18 : this.l) {
                  if (var18.o) {
                     var18.r = this.c(var18);
                  }
               }

               var var16 = this.d();
               io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile var19 = this.a(var16);
               return this.a(var16, var19);
            }
         }
      }
   }

   private void b() {
      HashSet<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1 = new HashSet();
      boolean var2 = true;

      while (var2) {
         var2 = false;

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 : this.l) {
            if (var4.o && var1.add(var4)) {
               this.a(var4);
               var2 = true;
            }
         }
      }
   }

   private void a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      for (io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var3 : var1.m) {
         if (!var3.d().isEmpty()
            && (!"header".equals(var3.a()) || !io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var3.b(), var1.a()))
            && (!"cookie".equals(var3.a()) || !this.a(var1, var3))) {
            io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var4 = new io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup(var3);
            var1.s.add(var4);
            String var5 = var3.d();
            var var6 = this.b(var1, var3);
            boolean var7 = false;
            if (!var6.isEmpty()) {
               var var8 = var6.stream().filter(var1x -> var1x.a.k != null && var1.j != null && var1x.a.k <= var1.j).toList();
               if (var8.isEmpty()) {
                  var4.f = "concurrent";
                  var4.c = ((io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch)var6.get(0)).a;
                  var4.d = ((io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch)var6.get(0)).b;
               } else {
                  List var9 = var8.stream().filter(var0 -> var0.a.o || c.contains(var0.a.f) && !var0.a.g).toList();
                  io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch var10 = (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch)(var9.isEmpty() ? var8 : var9).get((var9.isEmpty() ? var8 : var9).size() - 1);
                  var4.c = var10.a;
                  var4.d = var10.b;
                  var4.o = var10.c;

                  for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch var12 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch>) (Iterable<?>) (var8)) {
                     if (var12 != var10) {
                        var4.m.add(var12.a);
                        var4.l.add(var12.b);
                     }
                  }

                  if (!var9.isEmpty()) {
                     var7 = true;
                     var4.g = var10.a.o ? var10.a.p : var10.a.f;
                  } else if (var10.a.g) {
                     var4.f = "left-by-choice";
                  } else if ("other-site".equals(var10.a.f)) {
                     var4.j = "other-site";
                  } else {
                     var4.c = null;
                     var4.d = null;
                  }
               }
            }

            if (var7 && !var4.a()) {
               if (var5.length() < 4) {
                  var4.f = "short";
                  var4.c = null;
                  var4.d = null;
               } else if ("boolean".equals(var3.f()) || "boolean".equals(var4.d.e())) {
                  var4.f = "boolean";
               } else if (this.n.containsKey(var5)) {
                  var4.f = "typed";
               }

               var7 = var4.f == null;
            }

            if (var7) {
               var4.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN;
               var4.h = a(var4.d, var4.c);
               var4.e = a(var3, var4.d, var4.o);
               if (!var4.c.o) {
                  var4.c.o = true;
                  var4.c.p = var4.c.f;
               }
            } else if (var4.a() || var4.j != null) {
               var4.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.NEEDS;
               var4.h = c(var1, var3);
               var4.n = var5.startsWith("‹secret:");
            } else if (this.n.containsKey(var5)) {
               var4.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.TYPED;
               var4.p = this.a(var1, var5, var3.f());
               var4.h = var4.p != null ? var4.p : d(var1, var3);
               var4.k = this.n.get(var5);
            } else if (this.b(var5)) {
               var4.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.NEEDS;
               var4.h = c(var1, var3);
            }
         }
      }

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var14 : var1.s) {
         if (var14.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.NEEDS && var14.c == null && "header".equals(var14.a.a()) && var14.a()) {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var16 : var1.s) {
               if ("cookie".equals(var16.a.a()) && var16.a.d().equals(var14.a.d())) {
                  var14.h = var16.a.b();
                  var14.j = "cookie-header";
               }
            }
         }
      }
   }

   private boolean a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var2) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var3 = null;
      io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var4 = null;

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var6 : this.l) {
         if (var6.a >= var1.a) {
            break;
         }

         for (io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var8 : var6.l) {
            if ("cookie".equals(var8.a()) && var8.c().equals(var2.b()) && var8.d().equals(var2.d())) {
               var3 = var6;
               var4 = var8;
            }
         }
      }

      boolean var9 = var3 != null && (var3.o || c.contains(var3.f) && !var3.g);
      if (!var9 && !io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var2.b(), var2.d())) {
         if (this.r.add(var2.b()) && this.s == null) {
            this.s = var1.b;
         }

         return true;
      } else if (var3 == null) {
         return false;
      } else {
         io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var10 = new io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup(var2);
         var1.s.add(var10);
         var10.c = var3;
         var10.d = var4;
         if (var9) {
            var10.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE;
            var10.g = var3.o ? var3.p : var3.f;
            if (!var3.o) {
               var3.o = true;
               var3.p = var3.f;
            }

            return true;
         } else {
            var10.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.NEEDS;
            var10.h = c(var1, var2);
            if (var3.g) {
               var10.f = "left-by-choice";
            } else if ("other-site".equals(var3.f)) {
               var10.j = "other-site";
            } else {
               var10.c = null;
               var10.d = null;
            }

            return true;
         }
      }
   }

   private List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch> b(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var2) {
      ArrayList<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch> var3 = new ArrayList();
      String var4 = var2.d();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var6 : this.l) {
         if (var6.a >= var1.a) {
            break;
         }

         for (io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var8 : var6.l) {
            boolean var9 = !var8.d().equals(var4) && io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var2, var8.d());
            if ((var8.d().equals(var4) || var9) && !var6.n.contains(var8.d())) {
               var3.add(new io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceMatch(var6, var8, var9));
               break;
            }
         }
      }

      return var3;
   }

   private static String a(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var0, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var1, boolean var2) {
      if (var0.e() || var2) {
         return "urlEncode";
      } else if ("number".equals(var1.e()) && "string".equals(var0.f())) {
         return "toString";
      } else {
         return !"number".equals(var1.e()) && "number".equals(var0.f()) ? "toNumber" : null;
      }
   }

   private String a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, String var2, String var3) {
      for (Entry var5 : this.k.entrySet()) {
         Object var6 = var5.getValue();
         if (!((String)var5.getKey()).startsWith("api") || this.b(var1)) {
            boolean var7 = "number".equals(var3) ? var6 instanceof Number : ("boolean".equals(var3) ? var6 instanceof Boolean : var6 instanceof String);
            if (var7 && io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var6).equals(var2)) {
               return (String)var5.getKey();
            }
         }
      }

      return null;
   }

   private boolean b(String var1) {
      if (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.d.matcher(var1).matches() || io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.e.matcher(var1).matches()) {
         return true;
      } else if (!d.matcher(var1).matches()) {
         return false;
      } else {
         double var2 = Double.parseDouble(var1);
         return var2 >= this.o && var2 <= this.p || var2 * 1000.0 >= this.o && var2 * 1000.0 <= this.p;
      }
   }

   private static String a(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var0, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      String var2 = var0.a();

      return switch (var2) {
         case "leaf" -> a.contains(var0.c().toLowerCase(Locale.ROOT))
            ? var1.f() + Character.toUpperCase(var0.c().charAt(0)) + var0.c().substring(1).toLowerCase(Locale.ROOT)
            : var0.c();
         case "location" -> var1.f() + "Location";
         default -> var0.c();
      };
   }

   private static String c(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var1) {
      String var2 = var1.d();
      if (var2.startsWith("‹secret:")) {
         return var2.substring("‹secret:".length(), var2.length() - 1);
      } else if ("header".equals(var1.a())) {
         String[] var3 = var1.g() == null ? new String[0] : var1.g().trim().split("[\\s;,=]+");
         if (var3.length > 1 && b.contains(var3[0].toLowerCase(Locale.ROOT)) && !var3[0].equals(var2)) {
            return var3[0].toLowerCase(Locale.ROOT).replace("-", "") + "Token";
         } else {
            return var1.b().startsWith("X-") ? var1.b().substring(2) : var1.b();
         }
      } else {
         return d(var0, var1);
      }
   }

   private static String d(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var1) {
      String var2 = var1.a();

      return switch (var2) {
         case "path" -> {
            int var4 = Integer.parseInt(var1.b());
            yield var4 == 0 ? "pathValue" : io.karatelabs.ext.robot.recording.JavaScriptIdentifierSanitizer.d(var0.e().get(var4 - 1)) + "Id";
         }
         case "body" -> var1.b().isEmpty() ? "body" : io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var1.b());
         default -> var1.b();
      };
   }

   private boolean b(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      String var2 = this.h == null ? this.c() : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(this.h).c();
      return var1.h.a().equals(var2);
   }

   private boolean c(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      boolean var2 = var1.s.stream().anyMatch(var1x -> var1x.a() && (var1x.a.d().startsWith("‹secret:") || this.n.containsKey(var1x.a.d())));
      if (!var2) {
         return false;
      } else {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 : this.l) {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var6 : var4.s) {
               if (var6.c == var1 && var6.a() && (var6.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN || var6.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private String c() {
      return String.valueOf(this.g.get("app"));
   }

   private List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage> d() {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();
      var1.add(this.c());

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var3 : this.l) {
         if (var3.o) {
            var1.add(var3.h.a());
         }
      }

      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage> var11 = new ArrayList();

      for (String var4 : (Iterable<String>) (Iterable<?>) (var1)) {
         if (var4.equals(this.c()) && this.h != null) {
            io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var5 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(this.h);
            ArrayList<java.lang.String> var6 = new ArrayList();

            for (String var10 : var5.g().split("/")) {
               if (!var10.isEmpty()) {
                  var6.add(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.d(var10));
               }
            }

            boolean var13 = var4.equals(var5.c())
               && this.l
                  .stream()
                  .filter(var1x -> var1x.o && var1x.h.a().equals(var4))
                  .allMatch(var1x -> var1x.e().size() >= var6.size() && var1x.e().subList(0, var6.size()).equals(var6));
            if (var13) {
               var11.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage(this.t.a("appUrl"), var4, this.h, var6));
               continue;
            }
         }

         var11.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage(this.t.a(a(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var4).e())), var4, var4, List.of()));
      }

      return var11;
   }

   static String a(String var0) {
      if (!var0.matches("[\\d.]+") && !var0.startsWith("[")) {
         String var1 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.c(var0);
         var var2 = new ArrayList<>(List.of(var0.split("\\.")));
         int var3 = var1.equals(var0) && var2.size() == 1 ? 0 : var1.split("\\.").length - 1;
         List var4 = var2.subList(0, Math.max(1, var2.size() - var3));
         if (var4.size() > 1 && ((String)var4.get(0)).equalsIgnoreCase("www")) {
            var4 = var4.subList(1, var4.size());
         }

         return String.join("-", var4) + "-Url";
      } else {
         return "ip" + var0.replaceAll("[^0-9a-fA-F]", "") + "Url";
      }
   }

   private io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile a(List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage> var1) {
      if (this.i != null && this.i.get("scheme") instanceof String var2) {
         boolean var15 = !var1.isEmpty() && "appUrl".equals(((io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage)var1.get(0)).a());
         boolean var4 = this.j.stream().anyMatch(var0 -> var0.endsWith("Url") && !var0.equals("appUrl"));
         ArrayList<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup> var5 = new ArrayList();
         HashSet<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var6 = new HashSet();

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var8 : this.l) {
            if (var8.o && (!"bearer".equals(var2) || !var8.r)) {
               for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var10 : var8.s) {
                  if (var10.a() && var10.b != io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.AUTH) {
                     var5.add(var10);
                     if (var10.c != null && var10.c.r && var10.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN) {
                        var6.add(var10.c);
                     }
                  }
               }
            }
         }

         boolean var16 = var15 && !var5.isEmpty();
         String var19 = null;
         switch (var2) {
            case "bearer":
               var16 &= var5.stream().allMatch(var0 -> a(var0, "authorization", "bearer"));
               var16 &= this.i.get("login") != null
                  && Boolean.TRUE.equals(this.i.get("loginSendsToAppUrl"))
                  && var6.size() == 1
                  && var5.stream().allMatch(var0 -> var0.c != null && var0.c.r);
               break;
            case "basic":
               var16 &= !var4 && var5.stream().allMatch(var0 -> a(var0, "authorization", "basic"));
               break;
            case "apikey":
               String var11 = io.karatelabs.ext.robot.recording.RecordedExchange.a(this.i.get("header"));
               String var12 = io.karatelabs.ext.robot.recording.RecordedExchange.a(this.i.get("query"));
               var16 &= !var4
                  && (
                     var11 != null
                        ? var5.stream().allMatch(var1x -> "header".equals(var1x.a.a()) && var1x.a.b().equalsIgnoreCase(var11))
                        : var12 != null && var5.stream().allMatch(var1x -> "query".equals(var1x.a.a()) && var1x.a.b().equals(var12))
                  );
               var19 = "auth.key";
               break;
            default:
               var16 = false;
         }

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var23 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var6)) {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var26 : this.l) {
               for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var14 : var26.s) {
                  if (var14.c == var23 && (var14.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN || var14.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE) && !var5.contains(var14)) {
                     var16 = false;
                  }
               }
            }
         }

         if (!var16) {
            return new io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile(var2, false);
         } else {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var24 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup>) (Iterable<?>) (var5)) {
               var24.b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.AUTH;
               var24.i = var19;
               var24.h = null;
               var24.f = null;
               var24.j = null;
            }

            if (!"apikey".equals(var2)) {
               var6.forEach(var0 -> var0.q = true);
            }

            return new io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile(var2, true);
         }
      } else {
         return null;
      }
   }

   private static boolean a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var0, String var1, String var2) {
      if ("header".equals(var0.a.a()) && var0.a.b().equalsIgnoreCase(var1) && var0.a.g() != null) {
         String[] var3 = var0.a.g().trim().split("\\s+");
         return var3.length == 2 && var3[0].equalsIgnoreCase(var2) && var3[1].equals(var0.a.d());
      } else {
         return false;
      }
   }

   private RobotRecording a(List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage> var1, io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile var2) {
      var var3 = this.l.stream().filter(var0 -> var0.o && !var0.q).toList();
      HashMap<java.lang.String, java.lang.String> var4 = new HashMap();

      for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage var6 : var1) {
         var4.put(var6.b(), var6.a());
      }

      LinkedHashMap<java.lang.String, java.lang.String> var24 = new LinkedHashMap();
      ArrayList<java.lang.String> var25 = new ArrayList();
      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.RecordedCookie> var7 = new ArrayList();
      HashSet<java.lang.String> var8 = new HashSet();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var10 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var12 : var10.s) {
            if (var12.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.NEEDS) {
               String var13 = var12.a() ? var12.a.d() : var12.h + "=" + var12.a.d();
               if (!var24.containsKey(var13)) {
                  String var14 = (String)var4.get(var10.h.a());
                  String var15 = var12.n && !this.b(var10)
                     ? var14.substring(0, var14.length() - "Url".length()) + "-" + var12.h.replaceFirst("^API_", "")
                     : var12.h;
                  var24.put(var13, this.t.a(var15));
                  var25.add((String)var24.get(var13));
               }

               var12.i = (String)var24.get(var13);
               if ("cookie".equals(var12.a.a()) && var8.add(var12.a.b())) {
                  var7.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordedCookie(var12.a.b(), var12.i));
                  if (var12.j == null) {
                     this.q
                        .add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning(var10.b, "sign-in", "the session cookie '" + var12.a.b() + "' was set before the recording: " + var12.i + " supplies it"));
                  }
               }

               if (var12.j != null) {
                  this.q
                     .add(
                        new io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning(
                           var10.b,
                           var12.j,
                           "other-site".equals(var12.j)
                              ? var12.c.h.e() + " produced the value " + var12.a.c() + " sends: " + var12.i + " supplies it"
                              : "the cookie '" + var12.h + "' echoed as " + var12.a.c() + " has no captured setter: " + var12.i + " supplies both"
                        )
                     );
               }
            }
         }
      }

      LinkedHashMap<String, io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable> var26 = new LinkedHashMap<>();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var29 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var33 : var29.s) {
            if (var33.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.TYPED) {
               io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable var36 = var26.computeIfAbsent(
                  var33.a.d() + " " + var33.a.f(),
                  var3x -> var33.p != null && this.t.b(var33.p)
                     ? new io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable(var33.p, a(var33.a.d(), var33.a.f()), var33.k, true)
                     : new io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable(this.t.a(var33.p != null ? d(var29, var33.a) : var33.h), a(var33.a.d(), var33.a.f()), var33.k, false)
               );
               var33.i = var36.a();
            }
         }
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint> var28 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.List> var30 = new LinkedHashMap();
      LinkedHashMap<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, List<io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture>> var32 = new LinkedHashMap<>();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var37 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var16 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var18 : var16.s) {
               if (var18.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN && var18.c == var37) {
                  String var19 = var37.b + " " + var18.d.b();
                  if (!var28.containsKey(var19)) {
                     String var20 = this.t.a(var18.h);
                     var30.put(var19, new ArrayList());
                     ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.NamedValue> var21 = new ArrayList();

                     for (int var22 = 0; var22 < var18.m.size(); var22++) {
                        var21.add(new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var18.m.get(var22).b, var18.l.get(var22).b()));
                     }

                     var28.put(var19, new io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint(var20, new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var37.b, var18.d.b()), (List<io.karatelabs.ext.robot.recording.RobotRecording.NamedValue>)var30.get(var19), var37.p, null, var21));
                     var32.computeIfAbsent(var37, var0 -> new ArrayList<>()).add(new io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture(var20, var18.d.b(), var18.d.e()));
                  }

                  var18.i = ((io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint)var28.get(var19)).a();
                  ((List)var30.get(var19)).add(new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var16.b, var18.a.c()));
               }
            }
         }
      }

      var var35 = new ArrayList<>(var28.values());
      LinkedHashMap<java.lang.String, io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint> var38 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.List> var40 = new LinkedHashMap();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var43 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var47 : var43.s) {
            if (var47.f != null) {
               String var50 = var47.a.d() + " " + (var47.c == null ? "" : var47.c.b) + " " + var47.f;
               if (!var38.containsKey(var50)) {
                  var40.put(var50, new ArrayList());
                  var38.put(
                     var50, new io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint(null, var47.c == null ? null : new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var47.c.b, var47.d.b()), (List<io.karatelabs.ext.robot.recording.RobotRecording.NamedValue>)var40.get(var50), null, var47.f, List.of())
                  );
               }

               ((List)var40.get(var50)).add(new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var43.b, var47.a.c()));
            }
         }
      }

      var35.addAll(var38.values());
      if (!this.r.isEmpty()) {
         this.q.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning(this.s, "cookies-unset", "cookies sent with no captured setter and no credential name, never sent: " + String.join(", ", this.r)));
      }

      RecordingPatternAnalyzer var42 = new RecordingPatternAnalyzer(var3, this.t, this.o, this.p);
      var42.d();
      Map<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, java.lang.Integer> var44 = a(var3, var42.c());
      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.PatternEvidence> var46 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern var51 : var42.a()) {
         var46.add(new io.karatelabs.ext.robot.recording.RobotRecording.PatternEvidence(var51.a(), var51.b().b, var51.c(), (Integer)var44.get(var51.b()), var51.d(), var51.e(), var51.f()));
      }

      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap> var49 = new ArrayList();
      TreeMap<Integer, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction>> var52 = new TreeMap<>();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var55 : (Iterable<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>) (Iterable<?>) (var3)) {
         var52.computeIfAbsent((Integer)var44.get(var55), var0 -> new ArrayList<>()).add(this.a(var55, var4, var32.getOrDefault(var55, List.of()), var49));
      }

      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup> var54 = new ArrayList();

      for (Entry var23 : (Iterable<Entry>) (Iterable<?>) (var52.entrySet())) {
         var54.add(new io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup((Integer)var23.getKey(), (List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction>)var23.getValue()));
      }

      var var57 = this.f.c();
      if (!Boolean.TRUE.equals(var57.get("complete"))) {
         var49.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap(null, "capture", a(var57)));
      }

      return new RobotRecording(null, this.c(), this.e(), var1, var25, new ArrayList<>(var26.values()), var7, var2, var54, var35, this.q, var49, var46, var42.b());
   }

   private static Map<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, Integer> a(List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var0, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]> var1) {
      HashMap<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var2 = new HashMap();
      var0.forEach(var1x -> var2.put(var1x, var1x));

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 : var0) {
         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var6 : var4.s) {
            if ((var6.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN || var6.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE) && var2.containsKey(var6.c)) {
               a(var2, var6.c, var4);
            }
         }
      }

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[] var10 : var1) {
         if (var2.containsKey(var10[0]) && var2.containsKey(var10[1])) {
            a(var2, var10[0], var10[1]);
         }
      }

      LinkedHashMap<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, java.lang.Integer> var9 = new LinkedHashMap();
      HashMap<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, java.lang.Integer> var11 = new HashMap();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var13 : var0) {
         io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var7 = a(var2, var13);
         var9.putIfAbsent(var7, var9.size());
         var11.put(var13, (Integer)var9.get(var7));
      }

      return var11;
   }

   private static io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange a(Map<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var0, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      while (var0.get(var1) != var1) {
         var1 = (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var0.get(var1);
      }

      return var1;
   }

   private static void a(Map<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var0, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var2) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var3 = a(var0, var1);
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 = a(var0, var2);
      if (var3 != var4) {
         var0.put(var3.a < var4.a ? var4 : var3, var3.a < var4.a ? var3 : var4);
      }
   }

   private io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, Map<String, String> var2, List<io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture> var3, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap> var4) {
      ArrayList<io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence> var5 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var7 : var1.s) {
         if (var7.b != io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.LITERAL || var7.f != null) {
            var5.add(
               new io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence(
                  var7.a.a(),
                  var7.a.b(),
                  var7.a.c(),
                  var7.a.d(),
                  var7.a.f(),
                  var7.b,
                  var7.i,
                  var7.d == null ? null : new io.karatelabs.ext.robot.recording.RobotRecording.NamedValue(var7.c.b, var7.d.b()),
                  var7.e,
                  var7.f
               )
            );
         }
      }

      Object var16 = var1.c.get("after");
      io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase var17 = var1.r ? io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase.SIGN_IN : (var16 instanceof Number var8 && var8.intValue() == 0 ? io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase.PAGE_OPENED : io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase.AFTER_ACTION);
      var var18 = var1.c();
      Object var9 = var18 == null ? null : var18.get("status");
      String var10 = var1.a("response");
      boolean var11 = var10 == null || "full".equals(var10) || "none".equals(var10);
      if (!var11) {
         String var12 = var1.b("response");
         var4.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap(var1.b, "response body", var12 == null ? var10 : var10 + " (" + var12 + ")"));
      }

      if (var1.c.get("headersPartial") instanceof List var19 && !var19.isEmpty()) {
         var4.add(new io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap(var1.b, "headers", "headersPartial"));
      }

      Object var20 = var1.d();
      LinkedHashMap var21 = null;
      if (var11 && var20 instanceof Map var14) {
         LinkedHashMap<java.lang.String, java.lang.String> var15 = new LinkedHashMap();
         var14.forEach((var1x, var2x) -> var15.put(String.valueOf(var1x), a(var2x)));
         var21 = var15;
      }

      io.karatelabs.ext.robot.recording.RobotRecording.ResponseExpectation var22 = new io.karatelabs.ext.robot.recording.RobotRecording.ResponseExpectation(var9, var21, var11 && var20 instanceof List, var11, var11 ? b(var1, var20) : null);
      return new io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction(
         var1.b, var1.c, var1.p, var16, this.m.get(var16), var17, var1.q, (String)var2.get(var1.h.a()), var1.e(), var5, var22, this.a(var1, var9), var3
      );
   }

   private static String a(Object var0) {
      if (var0 == null) {
         return "#null";
      } else if (var0 instanceof String) {
         return "#string";
      } else if (var0 instanceof Number) {
         return "#number";
      } else if (var0 instanceof Boolean) {
         return "#boolean";
      } else {
         return var0 instanceof List ? "#array" : "#object";
      }
   }

   private io.karatelabs.ext.robot.recording.RobotRecording.CaptureStats a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, Object var2) {
      Object var3 = var1.d.get("group");
      if (var3 != null && var1.d.get("consumedBy") != null) {
         ArrayList<java.lang.Double> var4 = new ArrayList();
         int var5 = 0;

         for (String var7 : List.of("kept", "left")) {
            for (Map var9 : this.d(var7)) {
               if (var3.equals(var9.get("group"))) {
                  io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var10 = this.l.stream().filter(var1x -> var1x.b.equals(String.valueOf(var9.get("id")))).findFirst().orElse(null);
                  if (var10 != null && var10.j != null) {
                     var4.add(var10.j);
                     if (var10.a <= var1.a) {
                        var5++;
                     }
                  }
               }
            }
         }

         var4.sort(null);
         ArrayList<java.lang.Double> var16 = new ArrayList();

         for (int var17 = 1; var17 < var4.size(); var17++) {
            var16.add((Double)var4.get(var17) - (Double)var4.get(var17 - 1));
         }

         var16.sort(null);
         double var18 = var16.isEmpty()
            ? 1000.0
            : (
               var16.size() % 2 == 1
                  ? (Double)var16.get(var16.size() / 2)
                  : ((Double)var16.get(var16.size() / 2 - 1) + (Double)var16.get(var16.size() / 2)) / 2.0
            );
         int var19 = (int)Math.min(5000.0, Math.max(250.0, Math.ceil(var18 / 100.0) * 100.0));
         int var20 = Math.min(30, Math.max(3, 2 * var5));
         String var11 = null;

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var13 : this.l) {
            var var14 = var13.s.iterator();

            while (true) {
               if (var14.hasNext()) {
                  io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var15 = (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup)var14.next();
                  if (var15.c != var1 || var15.b != io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN || !var15.d.b().startsWith("response.")) {
                     continue;
                  }

                  var11 = var15.d.b().substring("response.".length());
               }

               if (var11 != null) {
                  return new io.karatelabs.ext.robot.recording.RobotRecording.CaptureStats(var20, var19, var2, var11);
               }
               break;
            }
         }

         return new io.karatelabs.ext.robot.recording.RobotRecording.CaptureStats(var20, var19, var2, var11);
      } else {
         return null;
      }
   }

   private io.karatelabs.ext.robot.recording.RobotRecording.ApplicationEntryPage e() {
      Map<java.lang.String, java.util.Map> var1 = null;

      for (Map var3 : this.f.e()) {
         if ("Document".equals(var3.get("type")) && var3.get("request") instanceof Map var4) {
            if (var3.get("after") instanceof Number var11 && var11.intValue() == 0) {
               var1 = var3;
               break;
            }

            if (var1 == null) {
               var1 = var3;
            }
         }
      }

      io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var7 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var1 == null ? this.c() : io.karatelabs.ext.robot.recording.RecordedExchange.a(((Map)var1.get("request")).get("url")));
      String var8 = null;
      if (var1 != null && var1.get("response") instanceof Map var10 && var10.get("body") instanceof String var12) {
         Matcher var15 = e.matcher(var12);
         var8 = var15.find() ? var15.group(1).replaceAll("\\s+", " ").strip() : null;
         var8 = var8 != null && !var8.isEmpty() ? var8 : null;
      }

      return new io.karatelabs.ext.robot.recording.RobotRecording.ApplicationEntryPage(var7.e() + (var7.f() < 0 ? "" : ":" + var7.f()), var7.g(), var8);
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static List<String> b(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0, Object var1) {
      Object var2 = var0.b().get("body");
      boolean var3 = var2 instanceof Map var5 && var5.get("query") instanceof String
         || var2 instanceof List var4 && !var4.isEmpty() && var4.stream().allMatch(var0x -> var0x instanceof Map var1x && var1x.get("query") instanceof String);
      if (!var3) {
         return null;
      } else {
         ArrayList<String> var10 = new ArrayList<>();

         for (Object var12 : var1 instanceof List ? (List)var1 : var1 == null ? List.of(Map.of()) : List.of(var1)) {
            boolean var7 = var12 instanceof Map var8 && var8.containsKey("errors");
            Object var13 = var7 ? ((Map)var12).get("errors") : null;
            var10.add(!var7 ? "absent" : (var13 == null ? "null" : (var13 instanceof List var9 && var9.isEmpty() ? "empty" : "present")));
         }

         return var10;
      }
   }

   private static Object a(String var0, String var1) {
      if ("number".equals(var1)) {
         return c(var0);
      } else {
         return !"boolean".equals(var1) || !"true".equals(var0) && !"false".equals(var0) ? var0 : Boolean.valueOf(var0);
      }
   }

   private static String a(Map<String, Object> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var3 : List.of("inFlightAtStop", "failed", "headersPartial")) {
         if (var0.get(var3) instanceof Number var4 && var4.longValue() > 0L) {
            var1.add(var3 + " " + var4);
         }
      }

      for (String var8 : List.of("dropped", "lost", "uncaptured", "bodies")) {
         if (var0.get(var8) instanceof Map var10) {
            var10.forEach(
               (var2, var3x) -> {
                  if ((!"bodies".equals(var8) || "unavailable".equals(var2))
                     && (var3x instanceof Number var4x && var4x.longValue() > 0L || var3x instanceof List var5 && !var5.isEmpty())) {
                     var1.add(var8 + "." + var2 + " " + var3x);
                  }
               }
            );
         }
      }

      if (var0.get("ended") != null && !"stop".equals(var0.get("ended"))) {
         var1.add("ended " + var0.get("ended"));
      }

      if (var0.get("errors") instanceof List var7 && !var7.isEmpty()) {
         var1.add("errors " + var7);
      }

      return var1.isEmpty() ? "incomplete" : String.join(", ", var1);
   }

   private static String d(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0) {
      String var1 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var0.c.get("type"));
      if (!"WebSocket".equals(var1) && !"EventSource".equals(var1)) {
         String var2 = io.karatelabs.ext.robot.recording.PageEvidenceExtractor.c(var0.b());
         String var3 = var0.a("request");
         if (var2.contains("multipart/") || var2.contains("application/octet-stream") || "binary".equals(var3)) {
            return "upload";
         } else {
            return !"capped".equals(var3) && !"unavailable".equals(var3) ? null : "request-body";
         }
      } else {
         return "stream";
      }
   }

   private static String a(String var0, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      return switch (var0) {
         case "stream" -> "a live stream is not converted";
         case "upload" -> "a file upload is not converted";
         default -> "the request body was not captured (" + var1.a("request") + "): the call cannot be replayed";
      };
   }

   private static Object c(String var0) {
      try {
         return var0.contains(".") ? Double.parseDouble(var0) : Long.parseLong(var0);
      } catch (NumberFormatException var2) {
         return var0;
      }
   }

   private List<Map<String, Object>> d(String var1) {
      return this.g.get(var1) instanceof List var2 ? var2 : List.of();
   }

   private RobotRecording a(String var1, int var2, String var3) {
      return new RobotRecording(
         new io.karatelabs.ext.robot.recording.RobotRecording.RecordingMetadata(var1, var2, var3),
         this.c(),
         null,
         List.of(),
         List.of(),
         List.of(),
         List.of(),
         null,
         List.of(),
         List.of(),
         this.q,
         List.of(),
         List.of(),
         List.of()
      );
   }

   static final class AnalyzedExchange {
      final int a;
      final String b;
      final Map<String, Object> c;
      final Map<String, Object> d;
      final boolean e;
      final String f;
      final boolean g;
      final io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl h;
      final String i;
      final Double j;
      final Double k;
      final List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> l;
      final List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence> m;
      final Set<String> n = new HashSet<>();
      boolean o;
      String p;
      boolean q;
      boolean r;
      final List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup> s = new ArrayList<>();

      AnalyzedExchange(int var1, Map<String, Object> var2, Map<String, Object> var3, boolean var4) {
         this.a = var1;
         this.b = String.valueOf(var2.get("id"));
         this.c = var2;
         this.d = var3;
         this.e = var4;
         this.f = var4 ? null : io.karatelabs.ext.robot.recording.RecordedExchange.a(var3.get("reason"));
         this.g = var3.get("moved") != null;
         Map<java.lang.String, Object> var5 = var2.get("request") instanceof Map var6 ? var6 : Map.of();
         this.h = var2.containsKey("lost") ? null : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(io.karatelabs.ext.robot.recording.RecordedExchange.a(var5.get("url")));
         this.i = var5.get("method") == null ? "" : String.valueOf(var5.get("method")).toUpperCase(Locale.ROOT);
         this.j = var2.get("start") instanceof Number var11 ? var11.doubleValue() : null;
         Map<java.lang.String, Object> var12 = var2.get("response") instanceof Map var14 ? var14 : null;
         Object var15 = var12 == null ? null : var12.get("responseTime");
         boolean var16 = var2.get("hops") instanceof Number var9 && var9.intValue() > 1;
         this.k = this.j != null && var15 instanceof Number var17 && !var16 ? this.j + var17.doubleValue() : null;
         this.l = this.h == null ? List.of() : io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var2);
         this.m = this.h != null && this.h.b() ? io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var5, this.h) : List.of();

         for (io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var19 : this.m) {
            this.n.add(var19.d());
            if (var19.d().indexOf(37) >= 0) {
               this.n.add(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.d(var19.d()));
            }
         }
      }

      boolean a() {
         return io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b.contains(this.i);
      }

      Map<String, Object> b() {
         return this.c.get("request") instanceof Map var1 ? var1 : Map.of();
      }

      Map<String, Object> c() {
         return this.c.get("response") instanceof Map var1 ? var1 : null;
      }

      Object d() {
         var var1 = this.c();
         return var1 == null ? null : var1.get("body");
      }

      String a(String var1) {
         return this.c.get("bodyState") instanceof Map var2 ? io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get(var1)) : null;
      }

      String b(String var1) {
         return this.c.get("bodyState") instanceof Map var3 && var3.get("why") instanceof Map var2 ? io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get(var1)) : null;
      }

      List<String> e() {
         ArrayList<java.lang.String> var1 = new ArrayList();

         for (String var5 : this.h.g().split("/")) {
            if (!var5.isEmpty()) {
               var1.add(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.d(var5));
            }
         }

         return var1;
      }

      String f() {
         var var1 = this.e();
         String[] var2 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(this.h.g()).split("/");
         int var3 = var2.length - 1;

         for (int var4 = var1.size() - 1; var3 >= 0 && var4 >= 0; var3--) {
            if (!var2[var3].isEmpty()) {
               if (!"{}".equals(var2[var3])) {
                  return io.karatelabs.ext.robot.recording.JavaScriptIdentifierSanitizer.d((String)var1.get(var4));
               }

               var4--;
            }
         }

         return "value";
      }
   }

   private record EvidenceMatch(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange a, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence b, boolean c) {
   }

   static final class EvidenceGroup {
      final io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence a;
      io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind b = io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.LITERAL;
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange c;
      io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence d;
      String e;
      String f;
      String g;
      String h;
      String i;
      String j;
      Object k;
      final List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> l = new ArrayList<>();
      final List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> m = new ArrayList<>();
      boolean n;
      boolean o;
      String p;

      EvidenceGroup(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var1) {
         this.a = var1;
      }

      boolean a() {
         return io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.g.matcher(this.a.d()).matches();
      }
   }
}
