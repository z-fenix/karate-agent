package io.karatelabs.ext.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EvidenceSheetComparator {
   public static final String a = "match";
   public static final String b = "match (±x)";
   public static final String c = "mismatch";
   public static final String d = "not rated";
   public static final String e = "unknown case";
   public static final String f = "rated but expected refused";
   public static final List<String> g = List.of("unknown case", "rated but expected refused", "not rated", "match", "match (±x)", "mismatch");

   private EvidenceSheetComparator() {
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, Object> var1, Map<String, Object> var2, EvidenceMapping var3) {
      return a(var0, var1, var2, var3, null);
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, Object> var1, Map<String, Object> var2, EvidenceMapping var3, RatebookSelectionRule var4) {
      var3.a(var0);
      List var5 = (List)var1.get("rows");
      String var6 = String.valueOf(((Map)var1.get("sheet")).get("digest"));
      a(var0, var1, var3, var5, var6);
      if (!(var2.get("rows") instanceof List var7)) {
         throw new RuleApiException(400, "RESULTS_INVALID", "results.rows is a list of {case? | inputs, figure?, error?, edition?, worksheet?}");
      } else {
         LinkedHashMap<java.lang.String, java.util.Map> var25 = new LinkedHashMap();
         LinkedHashMap<String, List<String>> var9 = new LinkedHashMap<>();

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var5)) {
            String var12 = String.valueOf(var11.get("id"));
            var25.put(var12, var11);
            if (!var3.f().isEmpty()) {
               var9.computeIfAbsent(a(var11.get("input"), var3), var0x -> new ArrayList<>()).add(var12);
            }
         }

         LinkedHashMap<String, List<io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow>> var26 = new LinkedHashMap<>();
         ArrayList<java.util.LinkedHashMap> var27 = new ArrayList();

         for (int var28 = 0; var28 < var7.size(); var28++) {
            if (!(var7.get(var28) instanceof Map var13)) {
               throw new RuleApiException(400, "RESULTS_INVALID", "results.rows[" + var28 + "] is not an object");
            }

            String var15 = null;
            String var16 = null;
            if (var13.get("case") != null) {
               var16 = String.valueOf(var13.get("case"));
               if (!var25.containsKey(var16)) {
                  var15 = "not in the sheet";
               }
            } else if (var13.get("inputs") instanceof Map var17 && !var3.f().isEmpty()) {
               String var43 = a(var17, var3, var28);
               List var19 = var43 == null ? List.of() : var9.getOrDefault(var43, List.of());
               if (var19.size() == 1) {
                  var16 = (String)var19.getFirst();
               } else {
                  var15 = var19.isEmpty() ? "unjoinable" : "multiply joined";
               }
            } else {
               var15 = "unjoinable";
            }

            if (var15 != null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var40 = new LinkedHashMap();
               var40.put("case", var16);
               var40.put("row", var28);
               var40.put("class", "unknown case");
               var40.put("reason", var15);
               var27.add(var40);
            } else {
               var26.computeIfAbsent(var16, var0x -> new ArrayList<>()).add(new io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow(var28, var13, var16));
            }
         }

         ArrayList<java.util.Map> var29 = new ArrayList();
         ArrayList var30 = new ArrayList();
         LinkedHashMap<java.lang.String, java.lang.Integer> var31 = new LinkedHashMap();

         for (String var36 : g) {
            var31.put(var36, 0);
         }

         for (Map var37 : (Iterable<Map>) (Iterable<?>) (var5)) {
            String var41 = String.valueOf(var37.get("id"));
            LinkedHashMap<java.lang.String, java.lang.Object> var44 = new LinkedHashMap();
            var44.put("case", var41);
            if (var37.get("label") != null) {
               var44.put("label", var37.get("label"));
            }

            List var46 = var26.getOrDefault(var41, List.of());
            io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow var20 = null;
            if (var46.size() > 1) {
               String var21 = a(((io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow)var46.getFirst()).b());
               boolean var22 = false;

               for (io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow var24 : (Iterable<io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow>) (Iterable<?>) (var46)) {
                  var22 |= !var21.equals(a(var24.b()));
               }

               if (var22) {
                  a(var44, "unknown case", "conflicting duplicates");
               } else {
                  var20 = (io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow)var46.getFirst();
               }
            } else if (var46.size() == 1) {
               var20 = (io.karatelabs.ext.rules.EvidenceSheetComparator.JoinedResultRow)var46.getFirst();
            }

            if (var20 != null) {
               var44.put("row", var20.a());
               a(var37, var20.b(), var3, var44);
               if (var20.b().get("worksheet") != null) {
                  var44.put("worksheet", var20.b().get("worksheet"));
               }

               Object var51 = var37.get("expected") instanceof Map var52 ? var52.get("edition") : null;
               Object var53 = var20.b().get("edition");
               if (var53 != null && var51 != null) {
                  a(var41, String.valueOf(var51), String.valueOf(var53), var4, var30);
               }
            } else if (var46.isEmpty()) {
               a(var44, "not rated", "absent");
            }

            var31.merge((String)var44.get("class"), 1, Integer::sum);
            var29.add(var44);
         }

         for (Map var38 : (Iterable<Map>) (Iterable<?>) (var27)) {
            var31.merge("unknown case", 1, Integer::sum);
            var29.add(var38);
         }

         int var35 = (Integer)var31.get("match") + (Integer)var31.get("match (±x)");
         int var39 = var35 + (Integer)var31.get("mismatch");
         LinkedHashMap<java.lang.String, java.lang.Object> var42 = new LinkedHashMap();
         var42.put("book", var0.d());
         var42.put("sheet", Map.of("digest", var6));
         var42.put("mapping", var3.j());
         var42.put("judgedOn", var0.o() == null ? "output" : "figures");
         var42.put("of", var5.size());
         var42.put("rated", var39);
         var42.put("matched", var35);
         var42.put("checked", var39 > 0);
         var42.put("counts", var31);
         var42.put("cases", var29);
         LinkedHashMap<java.lang.String, java.lang.Object> var45 = new LinkedHashMap();
         if (var2.get("producer") instanceof Map var47 && "karate-mock".equals(var47.get("kind"))) {
            var45.put("mockOnly", true);
         }

         if (var3.c()) {
            var45.put("mappingUnblessed", true);
         }

         var var48 = var3.a(var0.e());
         if (!var48.isEmpty()) {
            var45.put("lint", var48);
         }

         if (!var30.isEmpty()) {
            var45.put("editions", var30);
            var45.put("editionsCompared", var4 == null ? "raw" : "rule");
         }

         var42.put("disclosures", var45);
         return var42;
      }
   }

   private static void a(String var0, String var1, String var2, RatebookSelectionRule var3, List<Map<String, Object>> var4) {
      ArrayList<java.lang.String> var5 = new ArrayList();
      boolean var6;
      if (var3 == null) {
         var6 = !var1.equals(var2);
      } else {
         io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution var7 = var3.a(var1);
         io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution var8 = var3.a(var2);
         var6 = !var7.c().equals(var8.c());

         for (io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution var10 : List.of(var7, var8)) {
            if (!var10.h()) {
               var5.add(var10.b());
            }
         }
      }

      if (var6) {
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("case", var0);
         var11.put("ours", var1);
         var11.put("theirs", var2);
         if (!var5.isEmpty()) {
            var11.put("unmapped", var5);
         }

         var4.add(var11);
      }
   }

   private static void a(Map<String, Object> var0, Map<String, Object> var1, EvidenceMapping var2, Map<String, Object> var3) {
      boolean var5 = var0.get("expected") instanceof Map var6 && var6.get("refusal") != null;
      Object var14 = var1.get("figure");
      boolean var7 = var1.get("error") != null;
      boolean var8 = var14 instanceof String var9 ? !var9.isBlank() : var14 != null;
      if (var5 && !var7 && var8) {
         var3.put("theirs", var14);
         a(var3, "rated but expected refused", null);
      } else if (var7) {
         var3.put("error", var1.get("error"));
         a(var3, "not rated", "error");
      } else if (!var8) {
         a(var3, "not rated", "no-figure");
      } else {
         var3.put("theirs", var14);
         if (var14 instanceof String var15) {
            String var10 = var2.d(var15);
            if (!io.karatelabs.ext.rules.EvidenceMapping.b.matcher(var10).matches()) {
               a(var3, "not rated", "unparseable");
            } else {
               BigDecimal var11 = new BigDecimal(var10);
               if (var11.scale() > var2.g()) {
                  a(var3, "not rated", "scale");
               } else {
                  BigDecimal var12 = a(var0, var2);
                  if (var12 == null) {
                     a(var3, "not rated", "oracle-error");
                  } else {
                     var3.put("ours", var12.toPlainString());
                     if (var12.compareTo(var11) == 0) {
                        a(var3, "match", null);
                     } else {
                        BigDecimal var13 = var2.h();
                        if (var13 != null && var12.subtract(var11).abs().compareTo(var13) <= 0) {
                           var3.put("difference", var11.subtract(var12).toPlainString());
                           a(var3, "match (±x)", "±" + var13.toPlainString());
                        } else {
                           var3.put("difference", var11.subtract(var12).toPlainString());
                           a(var3, "mismatch", null);
                        }
                     }
                  }
               }
            }
         } else {
            a(var3, "not rated", "unparseable");
         }
      }
   }

   static BigDecimal a(Map<String, Object> var0, EvidenceMapping var1) {
      if (!(var0.get("error") == null && var0.get("expected") instanceof Map var2)) {
         return null;
      } else if (var2.get(var1.d()) instanceof Number var6) {
         double var7 = var6.doubleValue();
         return !Double.isNaN(var7) && !Double.isInfinite(var7) ? BigDecimal.valueOf(var7).setScale(var1.g(), RoundingMode.HALF_UP) : null;
      } else {
         return null;
      }
   }

   private static void a(Map<String, Object> var0, String var1, String var2) {
      var0.put("class", var1);
      if (var2 != null) {
         var0.put("reason", var2);
      }
   }

   private static String a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("figure", var0.get("figure"));
      var1.put("error", var0.get("error"));
      return io.karatelabs.ext.rules.twin.CanonicalJson.c(var1);
   }

   private static void a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, Object> var1, EvidenceMapping var2, List<Map<String, Object>> var3, String var4) {
      Map<java.lang.String, java.lang.Object> var5 = var1.get("binding") instanceof Map var6 ? var6 : Map.of();
      var var15 = EvidenceSheetExporter.a(var0, var2.b());
      ArrayList<java.lang.String> var16 = new ArrayList();

      for (String var9 : List.of("rulebook", "schema", "mapping", "engine")) {
         Object var10 = var5.get(var9);
         if ((!"mapping".equals(var9) || var10 != null) && !io.karatelabs.ext.rules.twin.CanonicalJson.c(var10).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var15.get(var9)))) {
            var16.add(var9);
         }
      }

      LinkedHashMap<java.lang.String, java.util.Map> var17 = new LinkedHashMap();

      for (Map var21 : var0.f()) {
         var17.put(String.valueOf(var21.get("_id")), var21);
      }

      for (Map var22 : var3) {
         if ("scenario".equals(var22.get("provenance"))) {
            Map var11 = (Map)var17.get(String.valueOf(var22.get("id")));
            if (var11 != null) {
               String var12 = var22.get("expected") instanceof Map var13 && var13.get("refusal") != null ? String.valueOf(var13.get("refusal")) : null;
               String var23 = RulebookRepository.a(var11) ? "schema-reject" : null;
               if (!Objects.equals(var12, var23)) {
                  var16.add("row " + var22.get("id") + " refusal");
               }
            }
         }
      }

      if (!var16.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
         var20.put("moved", var16);
         var20.put("bound", var5);
         var20.put("now", var15);
         throw new RuleApiException(409, "SHEET_STALE", "sheet " + var4 + " is stale — moved: " + String.join(", ", var16) + "; export the sheet again").a(var20);
      }
   }

   private static String a(Object var0, EvidenceMapping var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var1.f()) {
         var2.append(io.karatelabs.ext.rules.EvidenceMapping.b(io.karatelabs.ext.rules.FlattenedCsvCodec.a(var0, var4))).append('\t');
      }

      return var2.toString();
   }

   private static String a(Map<String, Object> var0, EvidenceMapping var1, int var2) {
      StringBuilder var3 = new StringBuilder();

      for (String var5 : var1.f()) {
         String var6 = var1.a(var5) != null ? var1.a(var5) : var5;
         if (!var0.containsKey(var6)) {
            return null;
         }

         try {
            var3.append(var1.b(var5, var0.get(var6))).append('\t');
         } catch (RuleApiException var8) {
            throw new RuleApiException(var8.a, var8.b, "results.rows[" + var2 + "]: " + var8.getMessage());
         }
      }

      return var3.toString();
   }

   private record JoinedResultRow(int a, Map<String, Object> b, String c) {

      private JoinedResultRow(int a, Map<String, Object> b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
}
