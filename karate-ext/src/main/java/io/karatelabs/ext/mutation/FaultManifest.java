package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FaultManifest {
   public static final String TRANSPORT_DELAY_OPERATOR = "TRANSPORT_DELAY";
   public static final String TRANSPORT_RESET_OPERATOR = "TRANSPORT_RESET";
   private static final Set<String> RESPONSE_TAMPER_OPERATORS = Set.of(
      "FIELD_DROP",
      "FIELD_NULL",
      "TYPE_FLIP",
      "VALUE_OFF_BY_ONE",
      "CARDINALITY_EMPTY",
      "ENUM_SUBSTITUTION",
      "STATUS_SWAP",
      "CONTENT_TYPE_TAMPER",
      "MALFORMED_BODY"
   );
   private static final Set<String> BODY_FIELD_OPERATORS = Set.of("FIELD_DROP", "FIELD_NULL", "TYPE_FLIP", "VALUE_OFF_BY_ONE", "CARDINALITY_EMPTY", "ENUM_SUBSTITUTION");
   private final List<io.karatelabs.ext.mutation.FaultManifest.FaultSpec> faults;

   private FaultManifest(List<io.karatelabs.ext.mutation.FaultManifest.FaultSpec> var1) {
      this.faults = List.copyOf(var1);
   }

   public List<io.karatelabs.ext.mutation.FaultManifest.FaultSpec> faults() {
      return this.faults;
   }

   public io.karatelabs.ext.mutation.FaultManifest.FaultSpec faultById(String var1) {
      return this.faults.stream().filter(var1x -> var1x.c().equals(var1)).findFirst().orElse(null);
   }

   public List<String> faultIds() {
      return this.faults.stream().map(io.karatelabs.ext.mutation.FaultManifest.FaultSpec::c).toList();
   }

   public static io.karatelabs.ext.mutation.FaultManifest parse(String var0) {
      Object var1;
      try {
         var1 = Json.parseStrict(var0, true);
      } catch (RuntimeException var25) {
         throw new IllegalArgumentException("the fault manifest is not valid JSON: " + var25.getMessage());
      }

      if (!(var1 instanceof Map var2 && var2.get("faults") instanceof List var3)) {
         throw new IllegalArgumentException("the fault manifest must be an object with a `faults` array of fault rows");
      } else if (var3.isEmpty()) {
         throw new IllegalArgumentException("the fault manifest declares no faults — nothing to feed");
      } else {
         ArrayList<io.karatelabs.ext.mutation.FaultManifest.FaultSpec> var26 = new ArrayList();
         LinkedHashSet<java.lang.String> var5 = new LinkedHashSet();
         int var6 = 0;

         for (Object var8 : var3) {
            String var9 = "faults[" + var6++ + "]";
            if (!(var8 instanceof Map var10)) {
               throw new IllegalArgumentException(var9 + " is not an object");
            }

            String var11 = optionalTrimmedString(var10.get("operator"));
            if (var11 == null) {
               throw new IllegalArgumentException(var9 + ": `operator` is required — one of " + supportedOperatorsText());
            }

            boolean var12 = "TRANSPORT_DELAY".equals(var11) || "TRANSPORT_RESET".equals(var11);
            if (!var12 && !RESPONSE_TAMPER_OPERATORS.contains(var11)) {
               throw new IllegalArgumentException(var9 + ": unknown operator '" + var11 + "' — one of " + supportedOperatorsText());
            }

            String var13 = optionalTrimmedString(var10.get("operation"));
            int var14 = var13 == null ? -1 : var13.indexOf(32);
            if (var14 < 1 || var14 == var13.length() - 1) {
               throw new IllegalArgumentException(var9 + ": `operation` must be \"METHOD /path\" (path segments in {braces} match any value)");
            }

            String var15 = var13.substring(0, var14).toUpperCase();
            String var16 = var13.substring(var14 + 1).trim();
            String var17 = optionalTrimmedString(var10.get("field"));
            if (BODY_FIELD_OPERATORS.contains(var11) && var17 == null) {
               throw new IllegalArgumentException(var9 + " (" + var11 + "): `field` is required — the top-level response body field to tamper");
            }

            Object var18 = var10.get("value");
            if ("ENUM_SUBSTITUTION".equals(var11) && var18 == null) {
               throw new IllegalArgumentException(var9 + " (ENUM_SUBSTITUTION): `value` is required — the replacement to substitute");
            }

            int var19 = optionalInt(var10, "from", var9, -1);
            int var20 = optionalInt(var10, "to", var9, -1);
            if ("STATUS_SWAP".equals(var11) && var20 < 0) {
               throw new IllegalArgumentException(
                  var9 + " (STATUS_SWAP): `to` is required — the replacement status (`from` is optional: omitted = swap whatever is served)"
               );
            }

            for (int var24 : new int[]{var19, var20}) {
               if (var24 != -1 && (var24 < 100 || var24 > 599)) {
                  throw new IllegalArgumentException(var9 + ": status " + var24 + " is not a valid HTTP status (100–599)");
               }
            }

            int var27 = optionalInt(var10, "delayMs", var9, 0);
            if ("TRANSPORT_DELAY".equals(var11) && var27 <= 0) {
               throw new IllegalArgumentException(var9 + " (TRANSPORT_DELAY): a positive `delayMs` is required — the stall before the response is written");
            }

            if (var10.containsKey("delayMs") && var27 <= 0) {
               throw new IllegalArgumentException(var9 + ": a supplied `delayMs` must be a positive integer");
            }

            String var28 = optionalTrimmedString(var10.get("id"));
            if (var28 == null) {
               StringBuilder var29 = new StringBuilder(var11).append(':').append(var15).append(' ').append(var16);
               if (var17 != null) {
                  var29.append(':').append(var17);
               }

               if (var20 >= 0) {
                  var29.append(':').append(var19 >= 0 ? var19 + "->" + var20 : String.valueOf(var20));
               }

               if (var18 != null) {
                  var29.append(':').append(var18.getClass().getSimpleName()).append('(').append(Json.stringifyStrict(var18)).append(')');
               }

               if (var27 > 0) {
                  var29.append(':').append(var27).append("ms");
               }

               var28 = var29.toString();
            }

            if (!var5.add(var28)) {
               throw new IllegalArgumentException(var9 + ": duplicate fault id '" + var28 + "'");
            }

            var26.add(new io.karatelabs.ext.mutation.FaultManifest.FaultSpec(var28, var11, var15, var16, var17, var19, var20, var18, var27));
         }

         return new io.karatelabs.ext.mutation.FaultManifest(var26);
      }
   }

   private static String supportedOperatorsText() {
      var var0 = new ArrayList<>(RESPONSE_TAMPER_OPERATORS.stream().sorted().toList());
      var0.add("TRANSPORT_DELAY");
      var0.add("TRANSPORT_RESET");
      return String.join(" | ", var0);
   }

   private static String optionalTrimmedString(Object var0) {
      return var0 instanceof String var1 && !var1.isBlank() ? var1.trim() : null;
   }

   private static int optionalInt(Map<?, ?> var0, String var1, String var2, int var3) {
      if (!var0.containsKey(var1)) {
         return var3;
      } else {
         Object var4 = var0.get(var1);
         if (var4 instanceof Number var5
            && var5.doubleValue() == Math.floor(var5.doubleValue())
            && !(var5.doubleValue() > 2.147483647E9)
            && !(var5.doubleValue() < -2.1474836E9F)) {
            return var5.intValue();
         } else {
            throw new IllegalArgumentException(
               var2 + ": `" + var1 + "` must be an integer, got " + (var4 == null ? "null" : var4.getClass().getSimpleName() + " " + var4)
            );
         }
      }
   }

   public record FaultSpec(String c, String d, String e, String f, String g, int h, int i, Object j, int k) {


      public String operationText() {
         return this.e + " " + this.f;
      }

      public boolean matchesOperation(String var1, String var2) {
         if (!this.e.equalsIgnoreCase(var1 == null ? "" : var1)) {
            return false;
         } else {
            String[] var3 = io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(this.f).split("/", -1);
            String[] var4 = io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(var2).split("/", -1);
            if (var3.length != var4.length) {
               return false;
            } else {
               for (int var5 = 0; var5 < var3.length; var5++) {
                  boolean var6 = var3[var5].startsWith("{") && var3[var5].endsWith("}");
                  if (!var6 && !var3[var5].equals(var4[var5])) {
                     return false;
                  }
               }

               return true;
            }
         }
      }

      public Map<String, Object> toMap() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("id", this.c);
         var1.put("operator", this.d);
         var1.put("operation", this.operationText());
         if (this.g != null) {
            var1.put("field", this.g);
         }

         if (this.h >= 0) {
            var1.put("from", this.h);
         }

         if (this.i >= 0) {
            var1.put("to", this.i);
         }

         if (this.j != null) {
            var1.put("value", this.j);
         }

         if (this.k > 0) {
            var1.put("delayMs", this.k);
         }

         return var1;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public int h() {
         return this.h;
      }

      public int i() {
         return this.i;
      }

      public Object j() {
         return this.j;
      }

      public int k() {
         return this.k;
      }
   }
}
