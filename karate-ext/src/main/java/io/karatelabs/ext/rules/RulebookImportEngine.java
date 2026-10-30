package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.rules.imports.ImportReader;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class RulebookImportEngine {
   static final String a = "evidence/ratebooks";
   private static final Pattern b = Pattern.compile("[-+]?(\\d+(\\.\\d*)?|\\.\\d+)");
   private static final Pattern c = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
   private static final Pattern d = Pattern.compile("\\d{4}-\\d{2}-\\d{2}([T ]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?");
   private static final Pattern e = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
   private static final BigDecimal f = BigDecimal.valueOf(100L);

   private RulebookImportEngine() {
   }

   static io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData a(Path var0, ImportMapping var1, String var2) {
      Path var3 = a(var0, var2, "source");
      if (!Files.isRegularFile(var3)) {
         throw new RuleApiException(404, "IMPORT_SOURCE_NOT_FOUND", "no source file '" + var2 + "' under the rulebook");
      } else {
         String var4 = var3.getFileName().toString();
         String var5 = var4.contains(".") ? var4.substring(var4.lastIndexOf(46) + 1).toLowerCase(Locale.ROOT) : "";
         ImportReader.Sheet var6;
         if (var5.equals("csv")) {
            try {
               var6 = b(var2, Files.readString(var3, StandardCharsets.UTF_8));
            } catch (IOException var11) {
               throw new UncheckedIOException(var11);
            }
         } else {
            ImportReader var7 = a(var5, b(var3, var2, var1.l));
            if (var1.l == null) {
               throw io.karatelabs.ext.rules.ImportMapping.a("source.sheet names the sheet a ." + var5 + " source is read from");
            }

            try {
               var6 = var7.read(var3, var1.l);
            } catch (ImportReader.Refused var9) {
               throw new RuleApiException(400, var9.code(), var2 + ": " + var9.getMessage());
            } catch (IOException var10) {
               throw new UncheckedIOException(var10);
            }
         }

         return a(var1, var2, var6);
      }
   }

   static ImportReader a(String var0, String var1) {
      for (ImportReader var3 : ServiceLoader.load(ImportReader.class, RulebookImportEngine.class.getClassLoader())) {
         if (var3.extensions().contains(var0)) {
            return var3;
         }
      }

      throw new RuleApiException(
         400,
         "IMPORT_READER_MISSING",
         "no reader for ."
            + var0
            + " sources (CSV and .xlsx are read) — "
            + (var1 != null ? "import " + var1 + ", the same sheet as CSV" : "import the sheet saved as CSV")
      );
   }

   private static String b(Path var0, String var1, String var2) {
      int var3 = var1.lastIndexOf(46);
      if (var3 < 0) {
         return null;
      } else {
         String var4 = var1.substring(0, var3);
         String var5 = var0.getFileName().toString().replaceFirst("\\.[^.]*$", "");
         if (var2 != null && Files.isRegularFile(var0.resolveSibling(var5).resolve(var2 + ".csv"))) {
            return var4 + "/" + var2 + ".csv";
         } else {
            return Files.isRegularFile(var0.resolveSibling(var5 + ".csv")) ? var4 + ".csv" : null;
         }
      }
   }

   static ImportReader.Sheet b(String var0, String var1) {
      ArrayList<io.karatelabs.ext.rules.imports.ImportReader.Row> var2 = new ArrayList();
      ArrayList<java.lang.String> var3 = new ArrayList();
      StringBuilder var4 = new StringBuilder();
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;
      int var8 = 1;

      for (int var9 = var1.startsWith("\ufeff") ? 1 : 0; var9 < var1.length(); var9++) {
         char var10 = var1.charAt(var9);
         if (var5) {
            if (var10 == '"') {
               if (var9 + 1 < var1.length() && var1.charAt(var9 + 1) == '"') {
                  var4.append('"');
                  var9++;
               } else {
                  var5 = false;
                  var6 = true;
               }
            } else {
               var4.append(var10);
            }
         } else if (var10 == ',') {
            var3.add(var4.toString());
            var4.setLength(0);
            var6 = false;
            var7 = true;
         } else if (var10 != '\n' && var10 != '\r') {
            if (var6) {
               throw new RuleApiException(400, "IMPORT_CSV_INVALID", var0 + " row " + var8 + ": text after a closing quote");
            }

            if (var10 == '"') {
               if (var4.length() > 0) {
                  throw new RuleApiException(400, "IMPORT_CSV_INVALID", var0 + " row " + var8 + ": a quote inside an unquoted cell");
               }

               var5 = true;
               var7 = true;
            } else {
               var4.append(var10);
               var7 = true;
            }
         } else {
            if (var10 == '\r' && var9 + 1 < var1.length() && var1.charAt(var9 + 1) == '\n') {
               var9++;
            }

            var3.add(var4.toString());
            var2.add(a(var8++, var3));
            var3 = new ArrayList();
            var4.setLength(0);
            var6 = false;
            var7 = false;
         }
      }

      if (var5) {
         throw new RuleApiException(400, "IMPORT_CSV_INVALID", var0 + " row " + var8 + ": an unclosed quote");
      } else {
         if (var7 || var4.length() > 0) {
            var3.add(var4.toString());
            var2.add(a(var8, var3));
         }

         return new ImportReader.Sheet(null, var2);
      }
   }

   private static ImportReader.Row a(int var0, List<String> var1) {
      ArrayList<io.karatelabs.ext.rules.imports.ImportReader.Cell> var2 = new ArrayList();

      for (int var3 = 0; var3 < var1.size(); var3++) {
         String var4 = (String)var1.get(var3);
         var2.add(new ImportReader.Cell(var4, a(var3) + var0, var4.strip().endsWith("%") ? "points" : null, Set.of(), 0));
      }

      return new ImportReader.Row(var0, var2);
   }

   static String a(int var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = var0 + 1; var2 > 0; var2 = (var2 - 1) / 26) {
         var1.insert(0, (char)(65 + (var2 - 1) % 26));
      }

      return var1.toString();
   }

   static io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData a(ImportMapping var0, String var1, ImportReader.Sheet var2) {
      String var3 = var1 + (var2.name() == null ? "" : " sheet '" + var2.name() + "'");
      ImportReader.Row var4 = null;
      int var5 = var0.m;

      for (ImportReader.Row var7 : var2.rows()) {
         if (var7.number() == var0.m) {
            var4 = var7;
         }

         var5 = Math.max(var5, var7.number());
      }

      if (var4 == null) {
         throw new RuleApiException(400, "IMPORT_HEADER_MISSING", var3 + ": no header row " + var0.m);
      } else {
         LinkedHashMap<java.lang.Object, java.lang.Integer> var23 = new LinkedHashMap();

         for (int var24 = 0; var24 < var4.cells().size(); var24++) {
            String var8 = var4.cells().get(var24).text().strip();
            if (!var8.isEmpty()) {
               Integer var9 = var23.put(var8, var24);
               if (var9 != null) {
                  throw new RuleApiException(
                     400,
                     "IMPORT_DUPLICATE_HEADER",
                     var3 + " row " + var0.m + ": header '" + var8 + "' is both column " + a(var9.intValue()) + " and " + a(var24)
                  );
               }
            }
         }

         LinkedHashMap<java.lang.Object, java.lang.Integer> var25 = new LinkedHashMap();

         for (Entry var28 : var0.u.entrySet()) {
            Integer var10 = (Integer)var23.get(var28.getKey());
            if (var10 == null) {
               boolean var11 = var0.v.contains(var28.getValue());
               throw new RuleApiException(
                  400,
                  "IMPORT_COLUMN_MISSING",
                  var3
                     + " row "
                     + var0.m
                     + ": no header '"
                     + (String)var28.getKey()
                     + "' — the "
                     + (var11 ? "key " : "")
                     + "column '"
                     + (String)var28.getValue()
                     + "' reads from it"
               );
            }

            var25.put((String)var28.getValue(), var10);
         }

         Integer var27 = null;
         if (var0.s != null) {
            var27 = (Integer)var23.get(var0.s.a());
            if (var27 == null) {
               throw new RuleApiException(
                  400, "IMPORT_COLUMN_MISSING", var3 + " row " + var0.m + ": no header '" + var0.s.a() + "' — the jurisdiction column target.layers names"
               );
            }
         }

         LinkedHashMap<String, List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>> var29 = new LinkedHashMap<>();
         ArrayList<Map<String, Object>> var30 = var0.o.equals("ratebook") ? new ArrayList<>() : null;
         if (var30 == null) {
            if (var0.t != null) {
               var29.put(var0.t, new ArrayList());
            } else {
               var29.put(null, new ArrayList());
               if (var0.s != null) {
                  for (String var12 : var0.s.c()) {
                     var29.put(var12, new ArrayList());
                  }
               }
            }
         }

         HashMap<java.lang.String, java.lang.Integer> var32 = new HashMap();
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var33 = new ArrayList();
         int[] var13 = new int[]{0};
         TreeMap<java.lang.Integer, io.karatelabs.ext.rules.imports.ImportReader.Row> var14 = new TreeMap();

         for (ImportReader.Row var16 : var2.rows()) {
            if (var16.number() > var0.m) {
               var14.put(var16.number(), var16);
            }
         }

         for (int var34 = var0.m + 1; var34 <= var5; var34++) {
            ImportReader.Row var35 = (ImportReader.Row)var14.get(var34);
            if (var0.n != null && var34 > var0.n) {
               var33.add(a(var34, "footer"));
            } else if (var35 != null && !a(var35)) {
               String var17 = var3 + " row " + var34;
               LinkedHashMap<java.lang.String, java.lang.String> var18 = new LinkedHashMap();

               for (String var20 : var0.v) {
                  var18.put(var20, b(var0, var20, a(var35, (Integer)var25.get(var20)), var17, var13));
               }

               for (Entry var39 : var0.w.entrySet()) {
                  if (var25.containsKey(var39.getKey())) {
                     var18.put(
                        (String)var39.getKey(),
                        a(
                           var0,
                           (String)var39.getKey(),
                           (String)var39.getValue(),
                           a(var35, (Integer)var25.get(var39.getKey())),
                           var17,
                           var13,
                           var30 != null && b((String)var39.getKey())
                        )
                     );
                  }
               }

               if (var30 != null) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var38 = new LinkedHashMap();

                  for (String var46 : io.karatelabs.ext.rules.ImportMapping.g) {
                     if (var18.get(var46) != null) {
                        var38.put(var46, var18.get(var46));
                     }
                  }

                  String var43 = var38.get("code") + "/" + var38.get("edition") + "/" + var38.get("line");
                  Integer var47 = var32.put(var43, var34);
                  if (var47 != null) {
                     throw new RuleApiException(400, "IMPORT_DUPLICATE_KEY", var17 + ": record " + var43 + " (code/edition/line) is also row " + var47);
                  }

                  var30.add(var38);
               } else {
                  String var37 = var0.t;
                  if (var0.s != null) {
                     ImportReader.Cell var40 = a(var35, var27);
                     String var21 = var40 == null ? "" : var40.text().strip();
                     if (var21.equals(var0.s.b())) {
                        var37 = null;
                     } else {
                        if (!var0.s.c().contains(var21)) {
                           throw new RuleApiException(
                              400,
                              "IMPORT_JURISDICTION",
                              var17
                                 + " column '"
                                 + var0.s.a()
                                 + "': '"
                                 + var21
                                 + "' is neither the countrywide value '"
                                 + var0.s.b()
                                 + "' nor one of "
                                 + var0.s.c()
                           );
                        }

                        var37 = var21;
                     }
                  }

                  ArrayList<java.lang.String> var41 = new ArrayList();

                  for (String var22 : var0.v) {
                     var41.add((String)var18.get(var22));
                  }

                  Integer var45 = var32.put((var37 == null ? "" : var37) + "\u0000" + String.join("\u0000", var41), var34);
                  if (var45 != null) {
                     throw new RuleApiException(400, "IMPORT_DUPLICATE_KEY", var17 + ": key " + var41 + (var37 == null ? "" : " in " + var37) + " is also row " + var45);
                  }

                  ((List)var29.get(var37)).add(new io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow(var34, List.copyOf(var41), var18));
               }
            } else {
               var33.add(a(var34, "blank"));
            }
         }

         return new io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData(var0, var1, var29, var30, var0.m + 1, var5, var33, var13[0]);
      }
   }

   private static boolean b(String var0) {
      return io.karatelabs.ext.rules.ImportMapping.g.indexOf(var0) >= 6;
   }

   private static Map<String, Object> a(int var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("row", var0);
      var2.put("reason", var1);
      return var2;
   }

   private static boolean a(ImportReader.Row var0) {
      for (ImportReader.Cell var2 : var0.cells()) {
         if (var2 != null && !var2.text().isBlank()) {
            return false;
         }
      }

      return true;
   }

   private static ImportReader.Cell a(ImportReader.Row var0, int var1) {
      return var1 < var0.cells().size() ? var0.cells().get(var1) : null;
   }

   private static String a(ImportMapping var0, String var1, ImportReader.Cell var2, String var3, int[] var4) {
      if (var2 == null) {
         return "";
      } else {
         String var5 = var3 + " column '" + var1 + "' (" + var2.ref() + ")";
         if (var2.has("merged")) {
            throw new RuleApiException(400, "IMPORT_MERGED_CELL", var5 + ": a merged cell in a mapped column");
         } else {
            if (var2.has("formula")) {
               if (!var0.D) {
                  throw new RuleApiException(400, "IMPORT_FORMULA", var5 + ": a formula — declare formulas: 'cached' to take its cached value");
               }

               var4[0]++;
            }

            return var2.text().strip();
         }
      }
   }

   private static String b(ImportMapping var0, String var1, ImportReader.Cell var2, String var3, int[] var4) {
      String var5 = a(var0, var1, var2, var3, var4);
      String var6 = var3 + " column '" + var1 + "'" + (var2 == null ? "" : " (" + var2.ref() + ")");
      if (var5.isEmpty()) {
         throw new RuleApiException(400, "IMPORT_EMPTY_CELL", var6 + ": the key cell is empty");
      } else {
         if (var2.pad() > 0) {
            Integer var7 = var0.z.get(var1);
            if (var7 == null) {
               throw new RuleApiException(
                  400, "IMPORT_KEY_FORMAT", var6 + ": a number formatted with " + var2.pad() + " leading-zero digits — declare pad." + var1 + " to keep them"
               );
            }

            var5 = "0".repeat(Math.max(0, var7 - var5.length())) + var5;
         }

         var5 = a(var0, var1, var2, var5, var6);

         try {
            return var0.C.b(var1, var5);
         } catch (RuleApiException var8) {
            throw new RuleApiException(var8.a, var8.b, var6 + ": " + var8.getMessage());
         }
      }
   }

   private static String a(ImportMapping var0, String var1, String var2, ImportReader.Cell var3, String var4, int[] var5, boolean var6) {
      String var7 = a(var0, var1, var3, var4, var5);
      String var8 = var4 + " column '" + var1 + "'" + (var3 == null ? "" : " (" + var3.ref() + ")");
      if (var7.isEmpty()) {
         if (var6) {
            return null;
         } else {
            throw new RuleApiException(400, "IMPORT_EMPTY_CELL", var8 + ": the cell is empty");
         }
      } else {
         switch (var2) {
            case "string":
               var7 = a(var0, var1, var3, var7, var8);

               try {
                  return var0.C.b(var1, var7);
               } catch (RuleApiException var12) {
                  throw new RuleApiException(var12.a, var12.b, var8 + ": " + var12.getMessage());
               }
            case "boolean":
               if (!var7.equals("true") && !var7.equals("false")) {
                  throw new RuleApiException(400, "IMPORT_NOT_BOOLEAN", var8 + ": '" + var7 + "' is not true or false");
               }

               return var7;
            default:
               return a(var0, var1, var2, var3, var7, var8);
         }
      }
   }

   private static String a(ImportMapping var0, String var1, String var2, ImportReader.Cell var3, String var4, String var5) {
      String var6 = io.karatelabs.ext.rules.BidirectionalValueMapping.a(var4, var0.B.getOrDefault(var1, List.of()));
      String var7 = var3.unit();
      if (var6.endsWith("%")) {
         if (!"points".equals(var7)) {
            throw new RuleApiException(400, "IMPORT_UNIT_MISMATCH", var5 + ": '" + var4 + "' carries a percent sign the reader did not read as points");
         }

         var6 = var6.substring(0, var6.length() - 1).strip();
      }

      if (!b.matcher(var6).matches()) {
         throw new RuleApiException(400, "IMPORT_NOT_NUMERIC", var5 + ": '" + var4 + "' is not a number");
      } else {
         BigDecimal var8 = new BigDecimal(var6.startsWith("+") ? var6.substring(1) : var6);
         io.karatelabs.ext.rules.ImportMapping.UnitConversion var9 = var0.x.get(var1);
         BigDecimal var10;
         if (var9 == null) {
            if (var7 != null) {
               throw new RuleApiException(400, "IMPORT_UNIT_MISMATCH", var5 + ": '" + var4 + "' reads as " + var7 + " and units." + var1 + " declares no unit");
            }

            var10 = var8;
         } else {
            String var11 = var9.a();
            if (var11.equals("auto")) {
               if (var7 == null) {
                  throw new RuleApiException(
                     400,
                     "IMPORT_UNIT_MISMATCH",
                     var5
                        + ": '"
                        + var4
                        + "' carries no unit evidence for units."
                        + var1
                        + ".source 'auto' — a %-suffixed text is points, a percent-formatted number a fraction"
                  );
               }

               var11 = var7;
            } else if (var7 != null && !var7.equals(var11)) {
               throw new RuleApiException(400, "IMPORT_UNIT_MISMATCH", var5 + ": '" + var4 + "' reads as " + var7 + " and units." + var1 + ".source declares " + var11);
            }

            BigDecimal var12 = var11.equals("points") ? var8.divide(f) : var8;
            String var13 = var9.b();

            var10 = switch (var13) {
               case "fraction" -> var12;
               case "plus-one" -> BigDecimal.ONE.add(var12);
               default -> var8;
            };
         }

         int var15 = var0.y.get(var1);
         if (var10.stripTrailingZeros().scale() > var15) {
            throw new RuleApiException(
               400, "IMPORT_PLACES", var5 + ": " + var10.stripTrailingZeros().toPlainString() + " needs more than places." + var1 + " = " + var15 + " decimals"
            );
         } else {
            String var16 = var10.setScale(var15).toPlainString();
            if (var16.startsWith("-") && var10.signum() == 0) {
               var16 = var16.substring(1);
            }

            return var16;
         }
      }
   }

   private static String a(ImportMapping var0, String var1, ImportReader.Cell var2, String var3, String var4) {
      String var5 = var0.A.get(var1);
      if (var2 != null && var2.has("time") && !"datetime".equals(var5)) {
         throw new RuleApiException(400, "IMPORT_TIME", var4 + ": '" + var3 + "' carries a time — declare transforms." + var1 + ": 'datetime'");
      } else if (var5 == null) {
         return var3;
      } else {
         switch (var5) {
            case "date":
               if (c.matcher(var3).matches() && c(var3)) {
                  return var3;
               }

               throw new RuleApiException(400, "IMPORT_DATE", var4 + ": '" + var3 + "' is not an ISO date (YYYY-MM-DD)");
            case "mdy":
               Matcher var8 = e.matcher(var3);
               String var9 = var8.matches()
                  ? String.format("%s-%02d-%02d", var8.group(3), Integer.parseInt(var8.group(1)), Integer.parseInt(var8.group(2)))
                  : null;
               if (var9 != null && c(var9)) {
                  return var9;
               }

               throw new RuleApiException(400, "IMPORT_DATE", var4 + ": '" + var3 + "' is not a date MM/DD/YYYY");
            default:
               if (d.matcher(var3).matches() && c(var3.substring(0, 10))) {
                  return var3;
               } else {
                  throw new RuleApiException(400, "IMPORT_DATE", var4 + ": '" + var3 + "' is not an ISO date-time");
               }
         }
      }
   }

   private static boolean c(String var0) {
      try {
         LocalDate.parse(var0);
         return true;
      } catch (DateTimeParseException var2) {
         return false;
      }
   }

   static String a(ImportMapping var0, List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow> var1) {
      var var2 = a(var0);
      StringBuilder var3 = new StringBuilder();
      a(var3, var2);

      for (io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow var5 : var1) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (String var8 : (Iterable<String>) (Iterable<?>) (var2)) {
            var6.add(var5.c().get(var8));
         }

         a(var3, var6);
      }

      return var3.toString();
   }

   static List<String> a(ImportMapping var0) {
      var var1 = new ArrayList<>(var0.v);
      var1.addAll(var0.w.keySet());
      return var1;
   }

   private static void a(StringBuilder var0, List<String> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (var2 > 0) {
            var0.append(',');
         }

         String var3 = (String)var1.get(var2);
         boolean var4 = var3.contains(",") || var3.contains("\"") || var3.contains("\n") || var3.contains("\r");
         var0.append(var4 ? "\"" + var3.replace("\"", "\"\"") + "\"" : var3);
      }

      var0.append('\n');
   }

   static String a(ImportMapping var0, String var1, List<String> var2, Map<String, Object> var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      if (var0.k) {
         var4.put("ai", true);
      }

      if (var1 == null) {
         var4.put("keys", var0.v);
         var4.put("columns", var0.w);
         if (var2 != null) {
            var4.put("layers", "jurisdiction");
            var4.put("jurisdictions", var2);
         }
      }

      var var5 = a(var0, var0.p, var1);
      if (var3 != null && !var0.p.equals("baseline")) {
         var5.put("rows", var3);
      }

      var4.put("provenance", var5);
      return a(var4, Set.of());
   }

   static Map<String, Object> a(ImportMapping var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();

      for (String var5 : List.of("source", "role", "ref", "jurisdiction", "effective", "renewalEffective", "retrieved")) {
         Object var6 = var5.equals("role") && var1 != null ? var1 : (var5.equals("jurisdiction") && var2 != null ? var2 : var0.E.get(var5));
         if (var6 != null) {
            var3.put(var5, var6);
         }
      }

      for (Entry var8 : var0.E.entrySet()) {
         var3.putIfAbsent((String)var8.getKey(), var8.getValue());
      }

      if (var0.D) {
         var3.put("formulas", "cached");
      }

      return var3;
   }

   static String a(Map<String, Object> var0, Set<String> var1) {
      StringBuilder var2 = new StringBuilder("{\n");
      int var3 = 0;

      for (Entry var5 : var0.entrySet()) {
         var2.append("  ").append(a(var5.getKey())).append(": ");
         if (var1.contains(var5.getKey()) && var5.getValue() instanceof List var6 && !var6.isEmpty()) {
            var2.append("[\n");

            for (int var8 = 0; var8 < var6.size(); var8++) {
               var2.append("    ").append(a(var6.get(var8))).append(var8 < var6.size() - 1 ? ",\n" : "\n");
            }

            var2.append("  ]");
         } else {
            var2.append(a(var5.getValue()));
         }

         var3++;
         var2.append(var3 < var0.size() ? ",\n" : "\n");
      }

      return var2.append("}\n").toString();
   }

   static String a(Object var0) {
      if (var0 instanceof Map var8) {
         StringBuilder var10 = new StringBuilder("{");
         int var11 = 0;

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
            var10.append(var11++ > 0 ? ", " : "").append(a(String.valueOf(var5.getKey()))).append(": ").append(a(var5.getValue()));
         }

         return var10.append('}').toString();
      } else if (var0 instanceof List var7) {
         StringBuilder var9 = new StringBuilder("[");

         for (int var3 = 0; var3 < var7.size(); var3++) {
            var9.append(var3 > 0 ? ", " : "").append(a(var7.get(var3)));
         }

         return var9.append(']').toString();
      } else if (var0 instanceof String var6) {
         String var2 = Json.stringifyStrict(List.of(var6));
         return var2.substring(1, var2.length() - 1);
      } else {
         return var0 instanceof Number var1 ? Terms.numberToString(var1) : String.valueOf(var0);
      }
   }

   static String a(String var0) {
      return var0.equals("baseline") ? "tables" : "evidence/" + var0;
   }

   static Map<String, String> a(io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var0, io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var1) {
      ImportMapping var2 = var0.c();
      String var3 = a(var2.p) + "/" + var2.q.replace('.', '/');
      List var4 = var2.s != null ? var2.s.c() : null;
      LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();

      for (Entry var7 : var0.e().entrySet()) {
         String var8 = (String)var7.getKey();
         if (var8 == null || var2.s == null || !((List)var7.getValue()).isEmpty()) {
            String var9 = var3 + (var8 == null ? "" : "@" + var8) + ".csv";
            var5.put(var9, a(var2, (List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>)var7.getValue()));
            var5.put(var9 + ".meta.json", a(var2, var8, var4, var0.b()));
         }
      }

      return var5;
   }

   static List<Map<String, Object>> a(Path var0, String var1, Map<String, String> var2, Map<String, Object> var3) {
      String var4 = a(var1);
      var var5 = new TreeMap<>(a(var0, var4));

      for (Entry var7 : var2.entrySet()) {
         var5.put((String)var7.getKey(), ((String)var7.getValue()).getBytes(StandardCharsets.UTF_8));
      }

      RuleTableCatalog.a(var5, var1.equals("baseline") ? io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.a : io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.a(var1));
      if (var1.equals("baseline")) {
         for (String var13 : var2.keySet()) {
            Path var8 = var0.resolve(var13);
            if (Files.isRegularFile(var8)) {
               String var9 = a(var8);
               Object var10 = var3 == null ? null : var3.get(var13);
               if (!var9.equals(var10)) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
                  var11.put("path", var13);
                  var11.put("expectedMtime", var10);
                  var11.put("currentMtime", var9);
                  throw new RuleApiException(
                        409,
                        "STALE_MTIME",
                        "stale write at "
                           + var13
                           + ": an existing table file takes its tableShas sha as ifMatch['"
                           + var13
                           + "'], "
                           + (var10 == null ? "none given" : "expected " + var10)
                     )
                     .a(var11);
               }
            }
         }
      }

      return a(var0, var2);
   }

   static List<Map<String, Object>> a(Path var0, Map<String, String> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (Entry var4 : var1.entrySet()) {
         Path var5 = var0.resolve((String)var4.getKey());

         try {
            Files.createDirectories(var5.getParent());
            Files.writeString(var5, (CharSequence)var4.getValue(), StandardCharsets.UTF_8);
         } catch (IOException var7) {
            throw new UncheckedIOException(var7);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("path", var4.getKey());
         var6.put("sha256", io.karatelabs.ext.common.Sha256.a(((String)var4.getValue()).getBytes(StandardCharsets.UTF_8)));
         var2.add(var6);
      }

      return var2;
   }

   static String a(io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var0) {
      ImportMapping var1 = var0.c();
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      if (var1.k) {
         var2.put("ai", true);
      }

      var2.put("provenance", a(var1, null, (String)null));
      var2.put("records", var0.f());
      return a(var2, Set.of("records"));
   }

   static Map<String, byte[]> a(Path var0, String var1) {
      Path var2 = var0.resolve(var1);
      TreeMap<java.lang.String, byte[]> var3 = new TreeMap();
      if (!Files.isDirectory(var2)) {
         return var3;
      } else {
         try {
            try (var var4 = Files.walk(var2)) {
               for (Path var6 : (Iterable<Path>) (var4::iterator)) {
                  if (Files.isRegularFile(var6) && !var6.getFileName().toString().startsWith(".")) {
                     var3.put(var1 + "/" + var2.relativize(var6).toString().replace('\\', '/'), Files.readAllBytes(var6));
                  }
               }
            }

            return var3;
         } catch (IOException var9) {
            throw new UncheckedIOException(var9);
         }
      }
   }

   static String a(Path var0) {
      try {
         return io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var0));
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }
   }

   static Path a(Path var0, String var1, String var2) {
      if (var1 != null && !var1.isBlank()) {
         Path var3 = var0.resolve(var1).normalize();
         if (!var3.startsWith(var0.normalize())) {
            throw new RuleApiException(400, "INVALID_INPUT", var2 + " path '" + var1 + "' leaves the rulebook");
         } else {
            return var3;
         }
      } else {
         throw new RuleApiException(400, "INVALID_INPUT", var2 + " is a rulebook-relative file path");
      }
   }

   static Object c(String var0, String var1) {
      return switch (var0) {
         case "number", "integer" -> Terms.narrow(Double.parseDouble(var1));
         case "boolean" -> Boolean.valueOf(var1);
         default -> var1;
      };
   }

   record ImportedRulebookData(ImportMapping c, String d, Map<String, List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>> e, List<Map<String, Object>> f, int g, int h, List<Map<String, Object>> i, int j) {

      ImportedRulebookData(ImportMapping c, String d, Map<String, List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>> e, List<Map<String, Object>> f, int g, int h, List<Map<String, Object>> i, int j) {
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
      }

      int a() {
         if (this.f != null) {
            return this.f.size();
         } else {
            int var1 = 0;

            for (List var3 : this.e.values()) {
               var1 += var3.size();
            }

            return var1;
         }
      }

      Map<String, Object> b() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("from", this.g);
         var1.put("to", this.h);
         var1.put("counted", Math.max(0, this.h - this.g + 1));
         var1.put("imported", this.a());
         var1.put("skipped", this.i);
         return var1;
      }

      public ImportMapping c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public Map<String, List<io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookRow>> e() {
         return this.e;
      }

      public List<Map<String, Object>> f() {
         return this.f;
      }

      public int g() {
         return this.g;
      }

      public int h() {
         return this.h;
      }

      public List<Map<String, Object>> i() {
         return this.i;
      }

      public int j() {
         return this.j;
      }
   }

   record ImportedRulebookRow(int a, List<String> b, Map<String, String> c) {

      ImportedRulebookRow(int a, List<String> b, Map<String, String> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
}
