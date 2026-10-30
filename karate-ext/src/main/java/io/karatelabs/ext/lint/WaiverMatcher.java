package io.karatelabs.ext.lint;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class WaiverMatcher {
   private final List<io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage> waivedItems = new ArrayList<>();
   private final List<Map<String, Object>> rejectedWaiverReports = new ArrayList<>();

   public static WaiverMatcher fromObject(Object var0) {
      WaiverMatcher var1 = new WaiverMatcher();
      if (var0 == null) {
         return var1;
      } else if (!(var0 instanceof Collection)) {
         if (var0 instanceof Map var5) {
            var1.waivedItems.add(new io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage(io.karatelabs.ext.lint.Waiver.parse(var5)));
            return var1;
         } else {
            throw new LintException("`waivers` must be a waiver object or a list of them, got " + var0.getClass().getSimpleName());
         }
      } else {
         for (Object var4 : (Collection)var0) {
            var1.waivedItems.add(new io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage(io.karatelabs.ext.lint.Waiver.parse(var4)));
         }

         return var1;
      }
   }

   public static WaiverMatcher fromList(List<Waiver> var0) {
      WaiverMatcher var1 = new WaiverMatcher();
      var0.forEach(var1x -> var1.waivedItems.add(new io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage(var1x)));
      return var1;
   }

   public WaiverMatcher addWaivers(List<Waiver> var1) {
      var1.forEach(var1x -> this.waivedItems.add(new io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage(var1x)));
      return this;
   }

   public WaiverMatcher addRejectedWaivers(List<Map<String, Object>> var1) {
      this.rejectedWaiverReports.addAll(var1);
      return this;
   }

   boolean isEmpty() {
      return this.waivedItems.isEmpty();
   }

   io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch findMatch(LintRule var1, String var2) {
      return this.findMatch(var1, List.of(var2));
   }

   io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch findMatch(LintRule var1, List<String> var2) {
      ArrayList<io.karatelabs.ext.lint.Waiver> var3 = new ArrayList();

      for (io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage var5 : this.waivedItems) {
         if (!var5.expired && !var2.stream().noneMatch(var2x -> var5.waiver.matches(var1.b(), var2x))) {
            if (!var1.q().b()) {
               var5.rejectionReasons.add("rule '" + var1.b() + "' is pinned un-waivable");
            } else {
               var var6 = var5.waiver.missingRequiredFields(var1.q());
               if (!var6.isEmpty()) {
                  var5.rejectionReasons.add("missing " + String.join(" + ", var6) + " (rule '" + var1.b() + "' requires " + String.join(" + ", var1.q().c()) + ")");
               } else {
                  var5.appliedCount++;
                  var3.add(var5.waiver);
               }
            }
         }
      }

      if (var3.isEmpty()) {
         return null;
      } else {
         Waiver var7 = (Waiver)var3.get(0);

         for (Waiver var10 : (Iterable<Waiver>) (Iterable<?>) (var3)) {
            if (var10.f() == io.karatelabs.ext.lint.Waiver.WaiverSource.INLINE) {
               var7 = var10;
               break;
            }
         }

         var var9 = new ArrayList<>(var3);
         var9.remove(var7);
         return new io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch(var7, var9);
      }
   }

   void markStaleWaivers(List<LintRule> var1) {
      this.validateWaiverRules(var1, var0 -> "no rule '" + var0 + "' ran, so this waiver can never apply");
   }

   void validateWaiverRules(List<LintRule> var1, UnaryOperator<String> var2) {
      for (io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage var4 : this.waivedItems) {
         for (LintRule var6 : var1) {
            if ("*".equals(var4.waiver.b()) || var4.waiver.b().equals(var6.b())) {
               if (!var6.q().b()) {
                  var4.rejectionReasons.add("rule '" + var6.b() + "' is pinned un-waivable");
               } else {
                  var var7 = var4.waiver.missingRequiredFields(var6.q());
                  if (!var7.isEmpty()) {
                     var4.rejectionReasons.add("missing " + String.join(" + ", var7) + " (rule '" + var6.b() + "' requires " + String.join(" + ", var6.q().c()) + ")");
                  }
               }
            }
         }

         if (!"*".equals(var4.waiver.b()) && var1.stream().noneMatch(var1x -> var1x.b().equals(var4.waiver.b()))) {
            var4.rejectionReasons.add(var2.apply(var4.waiver.b()));
         }
      }
   }

   int count() {
      return this.waivedItems.size();
   }

   Waiver waiverAt(int var1) {
      return this.waivedItems.get(var1).waiver;
   }

   int appliedCountAt(int var1) {
      return this.waivedItems.get(var1).appliedCount;
   }

   Set<String> rejectionDetailsAt(int var1) {
      return this.waivedItems.get(var1).rejectionReasons;
   }

   void markExpired(int var1) {
      this.waivedItems.get(var1).expired = true;
   }

   void reject(int var1, String var2) {
      this.waivedItems.get(var1).rejectionReasons.add(var2);
      this.waivedItems.get(var1).expired = true;
   }

   List<Map<String, Object>> report() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage var3 : this.waivedItems) {
         var var4 = new LinkedHashMap<>(var3.waiver.toMap());
         var4.put("applied", var3.appliedCount);
         if (!var3.rejectionReasons.isEmpty()) {
            var4.put("rejected", String.join("; ", var3.rejectionReasons));
         } else if (var3.appliedCount == 0) {
            var4.put("note", "covered no finding — the rule may have been fixed, or the scope may be wrong");
         }

         var1.add(var4);
      }

      var1.addAll(this.rejectedWaiverReports);
      return var1;
   }

   int rejectedCount() {
      int var1 = this.rejectedWaiverReports.size();

      for (io.karatelabs.ext.lint.WaiverMatcher.WaiverUsage var3 : this.waivedItems) {
         if (!var3.rejectionReasons.isEmpty()) {
            var1++;
         }
      }

      return var1;
   }

   final class WaiverMatch {
      private final Waiver winner;
      private final List<Waiver> additionalMatches;

      WaiverMatch(Waiver var1, List<Waiver> var2) {
         this.winner = var1;
         this.additionalMatches = var2;
      }

      Map<String, Object> toMap() {
         var var1 = new LinkedHashMap<>(this.winner.toMap());
         if (!this.additionalMatches.isEmpty()) {
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
            this.additionalMatches.forEach(var1x -> var2.add(var1x.toMap()));
            var1.put("alsoWaivedBy", var2);
         }

         return var1;
      }

      public Waiver winner() {
         return this.winner;
      }

      public List<Waiver> additionalMatches() {
         return this.additionalMatches;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch other)) return false;
         return Objects.equals(this.winner, other.winner) && Objects.equals(this.additionalMatches, other.additionalMatches);
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.winner, this.additionalMatches);
      }

      @Override
      public String toString() {
         return "w$a[winner=" + this.winner + ", also=" + this.additionalMatches + "]";
      }
   }

   private static final class WaiverUsage {
      final Waiver waiver;
      int appliedCount;
      boolean expired;
      final Set<String> rejectionReasons = new LinkedHashSet<>();

      WaiverUsage(Waiver var1) {
         this.waiver = var1;
      }
   }
}
