package io.karatelabs.ext.jsonschema.validation;

import java.util.HashSet;
import java.util.Set;

final class EvaluationTracker {
   private final Set<String> evaluatedProperties = new HashSet<>();
   private final Set<Integer> evaluatedItemIndexes = new HashSet<>();
   private int evaluatedPrefixLength;
   private boolean allItemsEvaluated;

   void markPropertyEvaluated(String var1) {
      this.evaluatedProperties.add(var1);
   }

   void markItemEvaluated(int var1) {
      this.evaluatedItemIndexes.add(var1);
   }

   void markPrefixEvaluated(int var1) {
      this.evaluatedPrefixLength = Math.max(this.evaluatedPrefixLength, var1);
   }

   void markAllItemsEvaluated() {
      this.allItemsEvaluated = true;
   }

   boolean isPropertyEvaluated(String var1) {
      return this.evaluatedProperties.contains(var1);
   }

   boolean isItemEvaluated(int var1) {
      return this.allItemsEvaluated || var1 < this.evaluatedPrefixLength || this.evaluatedItemIndexes.contains(var1);
   }

   void merge(EvaluationTracker var1) {
      this.evaluatedProperties.addAll(var1.evaluatedProperties);
      this.evaluatedItemIndexes.addAll(var1.evaluatedItemIndexes);
      this.evaluatedPrefixLength = Math.max(this.evaluatedPrefixLength, var1.evaluatedPrefixLength);
      this.allItemsEvaluated = this.allItemsEvaluated | var1.allItemsEvaluated;
   }
}
