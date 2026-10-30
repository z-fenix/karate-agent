package io.karatelabs.ext.robot.recording;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

final class RecordingPatternAnalyzer {
   private static final Pattern a = Pattern.compile("[a-z][a-z0-9_-]*");
   private static final Pattern b = Pattern.compile("\\d{4}-\\d{2}-\\d{2}([T ].*)?");
   private static final Pattern c = Pattern.compile("(?=.*\\d)(?=.*[A-Za-z])[A-Za-z0-9_\\-.=]{24,}");
   private static final Pattern d = Pattern.compile("(?i)(id|.*Id|.*_id|uuid|.*Uuid)");
   private static final Pattern e = Pattern.compile("[A-Za-z][A-Za-z-]*");
   private static final Pattern f = Pattern.compile("(?i).*(label|text|message|title|description|caption)$");
   private static final Set<String> g = Set.of("page", "next", "cursor", "offset", "total");
   private static final Set<String> h = Set.of("page", "size", "limit", "offset", "cursor", "pageSize", "per_page", "page_size");
   private final List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> i;
   private final JavaScriptIdentifierSanitizer j;
   private final double k;
   private final double l;
   private final List<io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern> m = new ArrayList<>();
   private final List<io.karatelabs.ext.robot.recording.RobotRecording.PatternVariable> n = new ArrayList<>();
   private final List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]> o = new ArrayList<>();
   private final Set<String> p = new HashSet<>();

   RecordingPatternAnalyzer(List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1, JavaScriptIdentifierSanitizer var2, double var3, double var5) {
      this.i = var1;
      this.j = var2;
      this.k = var3;
      this.l = var5;
   }

   List<io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern> a() {
      return this.m;
   }

   List<io.karatelabs.ext.robot.recording.RobotRecording.PatternVariable> b() {
      return this.n;
   }

   List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]> c() {
      return this.o;
   }

   void d() {
      LinkedHashMap<String, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange>> var1 = new LinkedHashMap<>();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var3 : this.i) {
         String var4 = this.a(var3);
         var1.computeIfAbsent(var4, var0 -> new ArrayList<>()).add(var3);
         io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var5 = d(var3);
         if (var5 != null && !((List)var1.get(var4)).contains(var5)) {
            ((List)var1.get(var4)).add(var5);
         }
      }

      for (List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var7 : var1.values()) {
         var7.sort((var0, var1x) -> Integer.compare(var0.a, var1x.a));
         if (var7.size() >= 2 && var7.stream().anyMatch(var0 -> !var0.a())) {
            this.a(var7);
         }
      }

      this.e();
   }

   private String a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var2 = c(var1);
      if (var2 != null) {
         return "chain " + var2.c.b + " " + var2.d.b();
      } else {
         var var3 = var1.e();
         String var4 = var1.h.a() + "/" + String.join("/", var3);
         if (!var1.a() && var3.size() > 1 && e.matcher((CharSequence)var3.get(var3.size() - 1)).matches()) {
            String var5 = var1.h.a() + "/" + String.join("/", var3.subList(0, var3.size() - 1));

            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var7 : this.i) {
               if (var7 != var1 && c(var7) == null && (var7.h.a() + "/" + String.join("/", var7.e())).equals(var5) && b(var7)) {
                  return "path " + var5;
               }
            }
         }

         return "path " + var4;
      }
   }

   private static boolean b(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0) {
      var var1 = var0.e();
      if (var1.isEmpty()) {
         return false;
      } else {
         String var2 = (String)var1.get(var1.size() - 1);
         String[] var3 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var0.h.g()).split("/");
         if (!"{}".equals(var3[var3.length - 1]) && e.matcher(var2).matches()) {
            if (var0.d() instanceof Map var4) {
               for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
                  if (d.matcher(String.valueOf(var6.getKey())).matches()
                     && var6.getValue() != null
                     && !(var6.getValue() instanceof Map)
                     && !(var6.getValue() instanceof List)
                     && io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var6.getValue()).equals(var2)) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      }
   }

   private static io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup c(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var1 = null;

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var3 : var0.s) {
         if ("path".equals(var3.a.a()) && var3.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN && "leaf".equals(var3.d.a())) {
            var1 = var3;
         }
      }

      return var1;
   }

   private static io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange d(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var1 = c(var0);
      return var1 != null && !var1.c.a() ? var1.c : null;
   }

   private void a(List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1) {
      int var2 = 0;

      while (((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var2)).a()) {
         var2++;
      }

      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();

      for (int var4 = var2; var4 < var1.size(); var4++) {
         if (((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var4)).d() instanceof Map var6) {
            var6.keySet().forEach(var1x -> var3.add(String.valueOf(var1x)));
         }

         if (((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var4)).c() != null
            && ((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var4)).c().get("headers") instanceof Map var14
            && !io.karatelabs.ext.robot.recording.RecordedExchange.b(a(var14), "etag").isEmpty()) {
            this.a((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var4), "ETag", "version");
         }
      }

      for (String var13 : (Iterable<String>) (Iterable<?>) (var3)) {
         ArrayList<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var15 = new ArrayList();
         ArrayList<java.lang.Object> var16 = new ArrayList();

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var9 : var1) {
            if (var9.d() instanceof Map var10 && var10.containsKey(var13)) {
               var15.add(var9);
               var16.add(var10.get(var13));
            }
         }

         this.a(var1, var2, var13, var15, var16);
      }
   }

   private void a(List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1, int var2, String var3, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var4, List<Object> var5) {
      io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var6 = (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var4.get(0);
      if (!var5.stream().anyMatch(var0 -> var0 == null || var0 instanceof Map || var0 instanceof List)) {
         if (var5.stream().allMatch(var0 -> var0 instanceof Boolean)) {
            this.a(var6, var3, "boolean");
         } else if (!var3.equalsIgnoreCase("version") && !var3.equalsIgnoreCase("rev")) {
            if (var5.stream().allMatch(this::a)) {
               this.a(var6, var3, "timestamp");
            } else {
               var var7 = var5.stream().map(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter::b).toList();
               if (!d.matcher(var3).matches()
                  && !var7.stream()
                     .allMatch(var0 -> io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.d.matcher(var0).matches() || io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.e.matcher(var0).matches())) {
                  if (var5.stream().allMatch(var0 -> var0 instanceof Number)) {
                     this.a(var6, var3, "counter");
                  } else if (var7.stream().anyMatch(var0 -> io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.g.matcher(var0).matches() || c.matcher(var0).matches())) {
                     this.a(var6, var3, "token");
                  } else if (var7.stream().allMatch(var0 -> io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.f.matcher(var0).matches())) {
                     this.a(var6, var3, "id");
                  } else if (var7.stream().allMatch(var0 -> a.matcher(var0).matches())) {
                     if (f.matcher(var3).matches() && b(var3, var4, var7)) {
                        this.a(var6, var3, "localized");
                     } else if (!a(var4, var7)) {
                        this.a(var6, var3, "text");
                     } else {
                        StringBuilder var8 = new StringBuilder();

                        for (int var9 = 0; var9 < var4.size(); var9++) {
                           var8.append(var9 == 0 ? "" : ", ").append("'").append((String)var7.get(var9)).append("' on ").append(((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var4.get(var9)).b);
                        }

                        for (int var10 = 0; var10 < var4.size(); var10++) {
                           if (((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var4.get(var10)).a >= ((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var1.get(var2)).a) {
                              this.m
                                 .add(
                                    new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                                       this.f(),
                                       (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var4.get(var10),
                                       "state",
                                       "match response" + io.karatelabs.ext.robot.recording.PageEvidenceExtractor.b(var3) + " == '" + (String)var7.get(var10) + "'",
                                       List.of(),
                                       var3 + " was " + var8
                                    )
                                 );
                           }
                        }
                     }
                  } else if (!var7.stream().anyMatch(var0 -> var0.chars().anyMatch(var0x -> var0x > 127)) && !b(var3, var4, var7)) {
                     this.a(var6, var3, "text");
                  } else {
                     this.a(var6, var3, "localized");
                  }
               } else {
                  if (!this.a(var3, var4, var5)) {
                     this.a(var6, var3, "id");
                  }
               }
            }
         } else {
            this.a(var6, var3, "version");
         }
      }
   }

   private static boolean a(List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var0, List<String> var1) {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (!((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var0.get(var2)).a()) {
            if (var2 > 0 && ((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var0.get(var2 - 1)).a() && !((String)var1.get(var2 - 1)).equals(var1.get(var2))) {
               return true;
            }

            for (int var3 = var2 + 1; var3 < var0.size(); var3++) {
               if (!((io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var0.get(var3)).a()) {
                  if (!((String)var1.get(var3)).equals(var1.get(var2))) {
                     return true;
                  }
                  break;
               }
            }
         }
      }

      return false;
   }

   private boolean a(String var1, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var2, List<Object> var3) {
      boolean var4 = false;

      for (int var5 = 0; var5 < var2.size(); var5++) {
         io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var6 = (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange)var2.get(var5);
         String var7 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var3.get(var5));
         io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var8 = null;
         boolean var9 = false;

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var11 : var6.s) {
            if (var11.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN && a(var11, var7) && "leaf".equals(var11.d.a())) {
               var8 = var11;
            }
         }

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var15 : this.i) {
            for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var13 : var15.s) {
               if (var13.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN && var13.c == var6 && var13.d.d().equals(var7)) {
                  var9 = true;
               }
            }
         }

         if (var9) {
            var4 = true;
         } else if (var8 != null) {
            var4 = true;
            this.m
               .add(
                  new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                     this.f(),
                     var6,
                     "read-back",
                     "match response" + io.karatelabs.ext.robot.recording.PageEvidenceExtractor.b(var1) + " == " + var8.i,
                     List.of(),
                     var1 + " returns " + var8.i + " from " + var8.c.b
                  )
               );
         } else if (this.a(var7, var6)) {
            var4 = true;
            this.a(var6, var1, "not-same-entity");
         }
      }

      return var4;
   }

   private boolean a(String var1, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var2) {
      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 : this.i) {
         if (var4.a >= var2.a) {
            break;
         }

         for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var6 : var4.s) {
            if (var6.b == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN && a(var6, var1)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var0, String var1) {
      return var0.a.d().equals(var1) || var0.o && io.karatelabs.ext.robot.recording.PageEvidenceExtractor.d(var0.a.d()).equals(var1);
   }

   private static boolean b(String var0, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1, List<String> var2) {
      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var4 : var1) {
         if (var4.d() instanceof Map var5) {
            for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
               String var8 = String.valueOf(var7.getKey());
               if (!var8.equals(var0) && var7.getValue() instanceof String var9 && a.matcher(var9).matches()) {
                  ArrayList<java.lang.String> var18 = new ArrayList();
                  boolean var11 = true;

                  for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var13 : var1) {
                     if ((var13.d() instanceof Map var15 ? var15.get(var8) : null) instanceof String var19) {
                        var18.add(var19);
                     } else {
                        var11 = false;
                     }
                  }

                  if (var11 && b(var18).equals(b(var2)) && !b(var18).isEmpty()) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static List<Integer> b(List<String> var0) {
      ArrayList<java.lang.Integer> var1 = new ArrayList();

      for (int var2 = 1; var2 < var0.size(); var2++) {
         if (!((String)var0.get(var2)).equals(var0.get(var2 - 1))) {
            var1.add(var2);
         }
      }

      return var1;
   }

   private boolean a(Object var1) {
      if (var1 instanceof Number var5) {
         double var3 = var5.doubleValue();
         return var3 >= this.k - 8.64E7 && var3 <= this.l + 8.64E7 || var3 * 1000.0 >= this.k - 8.64E7 && var3 * 1000.0 <= this.l + 8.64E7;
      } else {
         return var1 instanceof String var2 && b.matcher(var2).matches();
      }
   }

   private void e() {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var3 : this.i) {
         if (var3.a()) {
            String var4 = e(var3);
            if (var4 != null) {
               io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var5 = var1.put(var4, var3);
               if (var5 != null) {
                  var var6 = this.i.stream().filter(var2 -> !var2.a() && var2.a > var5.a && var2.a < var3.a).toList();
                  if (!var6.isEmpty()) {
                     this.a(var5, var3, var6);
                  }
               }
            }
         }
      }
   }

   private void a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var2, List<io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange> var3) {
      boolean var4 = "full".equals(var1.a("response")) && "full".equals(var2.a("response"));
      String var5 = var4 ? b(var1.d()) : null;
      if (var5 != null && var5.equals(b(var2.d()))) {
         var var6 = a(var1.d(), var5);
         var var7 = a(var2.d(), var5);
         String var8 = var5.isEmpty() ? "response" : var5;
         if (!a(var1, var5) && !a(var2, var5)) {
            var var9 = b(var6, var7);
            if (var9.isEmpty()) {
               this.a(var2, var8, "index");
            } else {
               String var10 = "response" + (var5.isEmpty() ? "" : io.karatelabs.ext.robot.recording.PageEvidenceExtractor.b(var5));
               String var11 = io.karatelabs.ext.robot.recording.JavaScriptIdentifierSanitizer.d(var5.isEmpty() ? var2.f() : var5);
               io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var12 = null;

               label117:
               for (String var14 : (Iterable<String>) (Iterable<?>) (var9)) {
                  for (io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var16 : var3) {
                     for (Object var18 : var7) {
                        String var19 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(((Map)var18).get(var14));
                        if (!a(var6, var14, var19)) {
                           String var20 = null;

                           for (io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence var22 : var16.l) {
                              if ("leaf".equals(var22.a()) && var22.d().equals(var19)) {
                                 var20 = var22.b();
                                 break;
                              }
                           }

                           io.karatelabs.ext.robot.recording.RecordingAnalyzer.EvidenceGroup var29 = var16.s.stream().filter(var1x -> var1x.i != null && var1x.a.d().equals(var19)).findFirst().orElse(null);
                           if (var20 != null || var29 != null) {
                              String var30 = "match " + var10 + " contains deep { " + io.karatelabs.ext.robot.recording.PageEvidenceExtractor.b(var14).replaceFirst("^\\.", "") + ": '#(";
                              if (var20 != null) {
                                 String var23 = this.j.a(var11 + Character.toUpperCase(var14.charAt(0)) + var14.substring(1));
                                 String var24 = this.f();
                                 this.m
                                    .add(
                                       new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                                          var24,
                                          var16,
                                          "def",
                                          "* def " + var23 + " = " + var20,
                                          List.of(),
                                          var14 + " of the " + var11 + " the mutation returned"
                                       )
                                    );
                                 this.m
                                    .add(
                                       new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                                          this.f(),
                                          var2,
                                          "collection",
                                          var30 + var23 + ")' }",
                                          List.of(var24),
                                          "the element " + var14 + " '" + var19 + "' appears after " + var16.b + " and not before"
                                       )
                                    );
                              } else {
                                 this.m
                                    .add(
                                       new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                                          this.f(),
                                          var2,
                                          "collection",
                                          var30 + var29.i + ")' }",
                                          List.of(),
                                          "the element "
                                             + var14
                                             + " '"
                                             + var19
                                             + "' the mutation sent as "
                                             + var29.i
                                             + " appears after "
                                             + var16.b
                                             + " and not before"
                                       )
                                    );
                              }

                              this.o.add(new io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]{var16, var2});
                              var12 = var16;
                              break label117;
                           }
                        }
                     }
                  }
               }

               int var25 = var7.size() - var6.size();
               if (var25 != 0) {
                  io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var26 = var12 != null ? var12 : var3.stream().filter(var1x -> a(var1x, var2)).findFirst().orElse(null);
                  if (var26 == null) {
                     this.a(var2, var8, "not-same-entity");
                  } else {
                     String var27 = this.j.a(var11 + "Count");
                     String var28 = this.f();
                     this.m.add(new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(var28, var1, "def", "* def " + var27 + " = " + var10 + ".length", List.of(), "the size before " + var26.b));
                     this.m
                        .add(
                           new io.karatelabs.ext.robot.recording.RecordingPatternAnalyzer.DiscoveredPattern(
                              this.f(),
                              var2,
                              "collection",
                              "match " + var10 + ".length == " + var27 + (var25 > 0 ? " + " : " - ") + Math.abs(var25),
                              List.of(var28),
                              "the size moved from " + var6.size() + " to " + var7.size() + " across " + var26.b
                           )
                        );
                     this.o.add(new io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]{var26, var1});
                     this.o.add(new io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange[]{var26, var2});
                  }
               }
            }
         } else {
            this.a(var2, var8, "incomplete");
         }
      } else {
         if (!var4 || var5 != null || b(var2.d()) != null) {
            this.a(var2, var5 == null ? "response" : var5, "incomplete");
         }
      }
   }

   private static String e(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0) {
      var var1 = new LinkedHashMap<>(var0.c);
      var var2 = new LinkedHashMap<>(var0.b());
      if (var2.get("headers") instanceof Map var3) {
         LinkedHashMap var5 = new LinkedHashMap(var3);
         var5.keySet().removeIf(var0x -> String.valueOf(var0x).equalsIgnoreCase("cookie"));
         var2.put("headers", var5);
      }

      var1.put("request", var2);
      return io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.a(var1);
   }

   private static boolean a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1) {
      var var2 = var0.e();
      var var3 = var1.e();
      return var0.h.a().equals(var1.h.a()) && (var2.equals(var3) || var2.size() == var3.size() + 1 && var2.subList(0, var3.size()).equals(var3));
   }

   private static boolean a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var0, String var1) {
      boolean var2 = var0.b().get("params") instanceof Map var3 && var3.keySet().stream().anyMatch(var0x -> h.contains(String.valueOf(var0x)));
      return var2 || !var1.isEmpty() && ((Map)var0.d()).keySet().stream().anyMatch(var0x -> g.contains(String.valueOf(var0x).toLowerCase(Locale.ROOT)));
   }

   private static String b(Object var0) {
      if (var0 instanceof List) {
         return "";
      } else if (var0 instanceof Map var1) {
         String var2 = null;

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if (var4.getValue() instanceof List) {
               if (var2 != null) {
                  return null;
               }

               var2 = String.valueOf(var4.getKey());
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   private static List<?> a(Object var0, String var1) {
      return var1.isEmpty() ? (List)var0 : (List)((Map)var0).get(var1);
   }

   private static List<String> b(List<?> var0, List<?> var1) {
      ArrayList<Object> var2 = new ArrayList<>(var0);
      var2.addAll(var1);
      if (!var2.isEmpty() && var2.stream().allMatch(var0x -> var0x instanceof Map)) {
         LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();
         ((Map)var2.get(0)).keySet().forEach(var1x -> var3.add(String.valueOf(var1x)));
         var3.removeIf(
            var1x -> !var2.stream()
               .allMatch(
                  var1xx -> ((Map)var1xx).get(var1x) != null && !(((Map)var1xx).get(var1x) instanceof Map) && !(((Map)var1xx).get(var1x) instanceof List)
               )
         );
         ArrayList<java.lang.String> var4 = new ArrayList();
         if (var3.contains("id")) {
            var4.add("id");
         }

         var3.stream().filter(var1x -> !var4.contains(var1x) && d.matcher(var1x).matches()).forEach(var4::add);
         var3.stream().filter(var1x -> !var4.contains(var1x)).forEach(var4::add);
         return var4;
      } else {
         return List.of();
      }
   }

   private static boolean a(List<?> var0, String var1, String var2) {
      return var0.stream().anyMatch(var2x -> var2x instanceof Map var3 && var3.get(var1) != null && io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var3.get(var1)).equals(var2));
   }

   private void a(io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange var1, String var2, String var3) {
      if (this.p.add(var1.b + " " + var2)) {
         this.n.add(new io.karatelabs.ext.robot.recording.RobotRecording.PatternVariable(var1.b, var2, var3));
      }
   }

   private String f() {
      return "p" + (this.m.size() + 1);
   }

   private static Map<String, Object> a(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   record DiscoveredPattern(String a, io.karatelabs.ext.robot.recording.RecordingAnalyzer.AnalyzedExchange b, String c, String d, List<String> e, String f) {
   }
}
