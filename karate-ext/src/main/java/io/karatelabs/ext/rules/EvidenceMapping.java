package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class EvidenceMapping {
   static final String a = "evidence/mappings";
   static final Pattern b = Pattern.compile("-?\\d+(\\.\\d+)?");
   private final String c;
   private final String d;
   private final boolean e;
   private final String f;
   private final String g;
   private final List<String> h;
   private final Map<String, Integer> i = new LinkedHashMap<>();
   private final Map<String, BigDecimal> j = new LinkedHashMap<>();
   private final Map<String, List<String>> k;
   private final Map<String, String> l = new LinkedHashMap<>();
   private final Map<String, String> m = new LinkedHashMap<>();
   private final BidirectionalValueMapping n;
   private final List<String> o = new ArrayList<>();

   private EvidenceMapping(String var1, String var2, Map<String, Object> var3) {
      this.c = var1;
      this.d = var2;
      this.e = Boolean.TRUE.equals(var3.get("ai"));
      var var4 = b(var3.get("figure"), "figure");
      this.f = c(var4.get("name"), "figure.name");
      this.g = c(var4.get("field"), "figure.field");
      ArrayList<java.lang.String> var5 = new ArrayList();
      if (var3.get("keys") != null) {
         Object var7 = var3.get("keys");
         if (!(var7 instanceof List)) {
            throw e("keys is a list of input paths");
         }

         for (Object var8 : (List)var7) {
            var5.add(c(var8, "keys[]"));
         }
      }

      this.h = List.copyOf(var5);

      for (Entry var17 : b(var3.get("scale"), "scale").entrySet()) {
         if (!(var17.getValue() instanceof Number var22)
            || var22.doubleValue() != Math.rint(var22.doubleValue())
            || var22.intValue() < 0
            || var22.intValue() > 6) {
            throw new RuleApiException(400, "MAPPING_SCALE", "scale." + (String)var17.getKey() + " is a whole number of decimal places 0..6, not " + var17.getValue());
         }

         this.i.put((String)var17.getKey(), var22.intValue());
      }

      if (!this.i.containsKey(this.f)) {
         throw new RuleApiException(400, "MAPPING_SCALE", "scale." + this.f + " (0..6 decimal places) is required for the figure");
      } else {
         for (Entry var18 : b(var3.get("tolerance"), "tolerance").entrySet()) {
            Integer var23 = this.i.get(var18.getKey());
            if (var23 == null) {
               throw new RuleApiException(400, "MAPPING_TOLERANCE", "tolerance." + (String)var18.getKey() + " names a figure with no scale");
            }

            BigDecimal var26 = var18.getValue() instanceof Number var10 ? new BigDecimal(Terms.numberToString(var10)) : null;
            if (var26 == null
               || var26.signum() < 0
               || var26.stripTrailingZeros().scale() > var23
               || var26.movePointRight(var23).compareTo(BigDecimal.valueOf(100L)) >= 0) {
               throw new RuleApiException(
                  400,
                  "MAPPING_TOLERANCE",
                  "tolerance."
                     + (String)var18.getKey()
                     + " is >= 0, has at most "
                     + var23
                     + " decimal places and is under 100 units of the last place, not "
                     + var18.getValue()
               );
            }

            this.j.put((String)var18.getKey(), var26);
         }

         this.k = io.karatelabs.ext.rules.BidirectionalValueMapping.a(b(var3.get("format"), "format"));

         for (Entry var19 : b(var3.get("fields"), "fields").entrySet()) {
            String var24 = c(var19.getValue(), "fields." + (String)var19.getKey());
            String var27 = this.m.put(var24, (String)var19.getKey());
            if (var27 != null) {
               throw new RuleApiException(
                  400, "MAPPING_FIELD_COLLISION", "fields: '" + var27 + "' and '" + (String)var19.getKey() + "' both name the carrier field '" + var24 + "'"
               );
            }

            this.l.put((String)var19.getKey(), var24);
         }

         this.n = new BidirectionalValueMapping(b(var3.get("values"), "values"), FlattenedCsvCodec::b);
         if (var3.get("steps") != null) {
            Object var20 = var3.get("steps");
            if (!(var20 instanceof List)) {
               throw e("steps is a list of worksheet step keys");
            }

            for (Object var25 : (List)var20) {
               this.o.add(c(var25, "steps[]"));
            }
         }
      }
   }

   public static EvidenceMapping a(Path var0, String var1) {
      if (!var1.matches("[A-Za-z0-9_-]+")) {
         throw e("name '" + var1 + "' — letters, digits, '_' or '-' only");
      } else {
         Path var2 = var0.resolve("evidence/mappings").resolve(var1 + ".json");
         if (!Files.isRegularFile(var2)) {
            throw new RuleApiException(404, "MAPPING_NOT_FOUND", "no mapping '" + var1 + "' at " + var0.getFileName() + "/evidence/mappings/" + var1 + ".json");
         } else {
            byte[] var3;
            try {
               var3 = Files.readAllBytes(var2);
            } catch (IOException var7) {
               throw new UncheckedIOException(var7);
            }

            Object var4;
            try {
               var4 = Json.parseStrict(new String(var3, StandardCharsets.UTF_8), true);
            } catch (RuntimeException var6) {
               throw new RuleApiException(400, "PARSE_FAILED", "mapping '" + var1 + "': " + var6.getMessage());
            }

            return new EvidenceMapping(var1, io.karatelabs.ext.common.Sha256.a(var3), b(var4, "mapping"));
         }
      }
   }

   public static EvidenceMapping a(Map<String, Object> var0) {
      return new EvidenceMapping(null, io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var0)), var0);
   }

   public static EvidenceMapping a(Path var0, Object var1) {
      if (var1 instanceof String var4) {
         return a(var0, var4);
      } else if (var1 instanceof Map var2) {
         return a(var2);
      } else {
         throw e("mapping is a name under evidence/mappings/ or an inline block");
      }
   }

   public void a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1) {
      if (var1.o() != null) {
         if (!var1.o().contains(this.f)) {
            throw new RuleApiException(400, "FIGURE_UNKNOWN", "figure.name '" + this.f + "' is none of the declared figures " + var1.o());
         }
      } else {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.a(List.of(this.f), var1.g());
      }
   }

   public List<String> a(Object var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : this.n.a()) {
         var var5 = a(var1, var4);
         if (var5 == null) {
            var2.add("values." + var4 + ": the schema declares no closed list at that path");
         } else {
            ArrayList<java.lang.String> var6 = new ArrayList();

            for (Object var8 : var5) {
               var6.add(b(var8));
            }

            var var9 = new ArrayList<>(var6);
            var9.removeAll(this.n.b(var4));
            var var10 = new ArrayList<>(this.n.b(var4));
            var10.removeAll(var6);
            if (!var9.isEmpty() || !var10.isEmpty()) {
               var2.add(
                  "values."
                     + var4
                     + ": domain differs from the schema enum"
                     + (var9.isEmpty() ? "" : " — unmapped " + var9)
                     + (var10.isEmpty() ? "" : " — not in the enum " + var10)
               );
            }
         }
      }

      return var2;
   }

   static List<?> a(Object var0, String var1) {
      Object var2 = var0;

      for (io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var4 : io.karatelabs.ext.rules.FlattenedCsvCodec.a(var1.replace("[]", "[0]"))) {
         if (var4.c()) {
            return null;
         }

         if (var4.a() == null) {
            if (!(var2 instanceof List var5) || var5.isEmpty() || Schema.enumMembers(var5) != null) {
               return null;
            }

            var2 = var5.getFirst();
         } else {
            if (!(var2 instanceof Map var6)) {
               return null;
            }

            var2 = var6.get(var4.a());
         }
      }

      return Schema.enumMembers(var2);
   }

   public String a() {
      return this.c;
   }

   public String b() {
      return this.d;
   }

   public boolean c() {
      return this.e;
   }

   public String d() {
      return this.f;
   }

   public String e() {
      return this.g;
   }

   public List<String> f() {
      return this.h;
   }

   public int g() {
      return this.i.get(this.f);
   }

   public BigDecimal h() {
      return this.j.get(this.f);
   }

   public List<String> i() {
      return List.copyOf(this.o);
   }

   public String a(String var1) {
      return this.l.get(var1);
   }

   public String b(String var1) {
      return this.m.get(var1);
   }

   public boolean c(String var1) {
      return this.n.a(var1);
   }

   public Object a(String var1, Object var2) {
      return this.n.a(var1, var2);
   }

   public String b(String var1, Object var2) {
      return this.n.b(var1, var2);
   }

   public String d(String var1) {
      return io.karatelabs.ext.rules.BidirectionalValueMapping.a(var1, this.k.getOrDefault(this.f, List.of()));
   }

   public Map<String, Object> j() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (this.c != null) {
         var1.put("name", this.c);
      }

      var1.put("sha", this.d);
      if (this.e) {
         var1.put("ai", true);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("name", this.f);
      var2.put("field", this.g);
      var2.put("scale", this.g());
      if (this.h() != null) {
         var2.put("tolerance", this.h().toPlainString());
      }

      var1.put("figure", var2);
      if (!this.h.isEmpty()) {
         var1.put("keys", this.h);
      }

      return var1;
   }

   static String b(Object var0) {
      return io.karatelabs.ext.rules.BidirectionalValueMapping.a(var0);
   }

   private static Map<String, Object> b(Object var0, String var1) {
      if (var0 == null) {
         return Map.of();
      } else if (!(var0 instanceof Map)) {
         throw e(var1 + " is an object");
      } else {
         return (Map<String, Object>)var0;
      }
   }

   private static String c(Object var0, String var1) {
      if (var0 instanceof String var2 && !var2.isEmpty()) {
         return var2;
      } else {
         throw e(var1 + " is a non-empty string");
      }
   }

   private static RuleApiException e(String var0) {
      return new RuleApiException(400, "MAPPING_INVALID", "mapping: " + var0);
   }
}
