package io.karatelabs.ext.lint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaiverLedger {
   static final List<String> FILE_WAIVER_KEYS = List.of("rule", "scope", "reason", "owner", "expires", "successor", "sunset");
   private final WaiverMatcher waiverMatcher;
   private final List<Map<String, Object>> waiverExtras = new ArrayList<>();
   private final Map<String, LintRule> rulesById = new LinkedHashMap<>();
   private final IdentityHashMap<Waiver, Integer> waiverIndices = new IdentityHashMap<>();
   private final List<List<String>> rejectionDetails;
   private final String[] rejectionCodes;
   private final boolean[] expiredFlags;
   private final String projectSource;

   public static WaiverLedger create(Object var0, String var1, String var2, Collection<String> var3, LocalDate var4) {
      ArrayList var5 = new ArrayList();
      if (var0 instanceof Collection var6) {
         var5.addAll(var6);
      } else if (var0 instanceof Map) {
         var5.add(var0);
      } else if (var0 != null) {
         throw new LintException(
            "`waivers` must be a waiver object, a list of them, or a project-relative path to a YAML/JSON list, got " + var0.getClass().getSimpleName()
         );
      }

      ArrayList var14 = new ArrayList();
      ArrayList var7 = new ArrayList();

      for (Object var9 : var5) {
         var var10 = io.karatelabs.ext.lint.Waiver.validateKeysAndCopy(var9, FILE_WAIVER_KEYS);
         var14.add(new Waiver(stringValue(var10.get("rule")), stringValue(var10.get("scope")), stringValue(var10.get("reason")), stringValue(var10.get("owner")), io.karatelabs.ext.lint.Waiver.WaiverSource.FILE, stringValue(var10.get("expires"))));
         LinkedHashMap var11 = new LinkedHashMap();

         for (String var13 : List.of("successor", "sunset")) {
            if (var10.get(var13) != null) {
               var11.put(var13, var10.get(var13));
            }
         }

         var7.add(var11);
      }

      return new WaiverLedger(var14, var7, var1, var2, var3, var4);
   }

   private WaiverLedger(List<Waiver> var1, List<Map<String, Object>> var2, String var3, String var4, Collection<String> var5, LocalDate var6) {
      this.waiverMatcher = io.karatelabs.ext.lint.WaiverMatcher.fromList(var1);
      this.waiverExtras.addAll(var2);
      this.projectSource = var3;

      for (String var8 : var5) {
         this.rulesById.put(var8, new LintRule(var8, var8, null, null, null, null, null, null, null, null, null, null, false, null, null, null, var8));
      }

      int var16 = this.waiverMatcher.count();
      this.rejectionDetails = new ArrayList<>();
      this.rejectionCodes = new String[var16];
      this.expiredFlags = new boolean[var16];
      this.waiverMatcher.validateWaiverRules(List.copyOf(this.rulesById.values()), var1x -> "no kind '" + var1x + "' in " + var4);

      for (int var17 = 0; var17 < var16; var17++) {
         Waiver var9 = this.waiverMatcher.waiverAt(var17);
         this.waiverIndices.put(var9, var17);
         ArrayList var10 = new ArrayList();
         this.rejectionDetails.add(var10);
         if (!"*".equals(var9.b()) && !this.rulesById.containsKey(var9.b())) {
            this.rejectWaiver(var17, "UNKNOWN_KIND", "no kind '" + var9.b() + "' in " + var4);
         }

         var var11 = var9.missingRequiredFields(io.karatelabs.ext.lint.LintRule.WaiverPolicy.DEFAULT_WAIVER_POLICY);
         if (!var11.isEmpty()) {
            this.rejectWaiver(var17, "INCOMPLETE", "missing " + String.join(" + ", var11));
         }

         if (var9.g() != null) {
            LocalDate var12;
            try {
               var12 = LocalDate.parse(var9.g().trim());
            } catch (RuntimeException var15) {
               String var14 = "expires '" + var9.g() + "' is not an ISO date";
               this.waiverMatcher.reject(var17, var14);
               this.rejectWaiver(var17, "BAD_EXPIRES", var14);
               continue;
            }

            if (!var6.isBefore(var12)) {
               this.expiredFlags[var17] = true;
               this.waiverMatcher.markExpired(var17);
            }
         }
      }
   }

   private void rejectWaiver(int var1, String var2, String var3) {
      this.rejectionCodes[var1] = this.rejectionCodes[var1] == null ? var2 : this.rejectionCodes[var1];
      this.rejectionDetails.get(var1).add(var3);
   }

   public boolean isEmpty() {
      return this.waiverMatcher.count() == 0;
   }

   public Map<String, Object> findApplicableWaiver(String var1, String var2, String var3) {
      LintRule var4 = this.rulesById.get(var1);
      if (var4 == null) {
         return null;
      } else {
         ArrayList var5 = new ArrayList();

         for (String var9 : new String[]{var2, var3}) {
            if (var9 != null && !var5.contains(var9)) {
               var5.add(var9);
            }
         }

         io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch var10 = this.waiverMatcher.findMatch(var4, var5);
         if (var10 == null) {
            return null;
         } else {
            var var11 = this.describeWaiver(this.waiverIndices.get(var10.winner()).intValue());
            if (!var10.additionalMatches().isEmpty()) {
               ArrayList var12 = new ArrayList();
               var10.additionalMatches().forEach(var2x -> var12.add(this.describeWaiver(this.waiverIndices.get(var2x).intValue())));
               var11.put("alsoWaivedBy", var12);
            }

            return var11;
         }
      }
   }

   public List<Map<String, Object>> report() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < this.waiverMatcher.count(); var2++) {
         var var3 = this.describeWaiver(var2);
         boolean var4 = !this.waiverMatcher.rejectionDetailsAt(var2).isEmpty() || !this.rejectionDetails.get(var2).isEmpty();
         var3.put("status", var4 ? "rejected" : (this.expiredFlags[var2] ? "expired" : (this.waiverMatcher.appliedCountAt(var2) == 0 ? "stale" : "applied")));
         var3.put("applied", this.waiverMatcher.appliedCountAt(var2));
         if (var4) {
            var3.put("code", this.rejectionCodes[var2] == null ? "INCOMPLETE" : this.rejectionCodes[var2]);
            var3.put("rejected", String.join("; ", (Iterable<? extends CharSequence>)(this.rejectionDetails.get(var2).isEmpty() ? this.waiverMatcher.rejectionDetailsAt(var2) : this.rejectionDetails.get(var2))));
         }

         var1.add(var3);
      }

      return var1;
   }

   private Map<String, Object> describeWaiver(int var1) {
      var var2 = this.waiverMatcher.waiverAt(var1).toMap();
      var2.put("source", this.projectSource == null ? "inline" : "project");
      if (this.projectSource != null) {
         var2.put("file", this.projectSource);
      }

      var2.putAll(this.waiverExtras.get(var1));
      return var2;
   }

   private static String stringValue(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }
}
