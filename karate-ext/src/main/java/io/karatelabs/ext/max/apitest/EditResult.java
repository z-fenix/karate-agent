package io.karatelabs.ext.max.apitest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EditResult(Map<String, Object> error, byte[] bytes, String version, String basedOn, EditResult.Change change) {


   static EditResult refuse(String var0, int var1, String var2, Map<String, Object> var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("error", var2);
      var4.put("code", var0);
      var4.put("status", var1);
      if (var3 != null && !var3.isEmpty()) {
         var4.put("detail", var3);
      }

      return new EditResult(var4, null, null, null, null);
   }

   static EditResult refuse(Map<String, Object> var0) {
      return new EditResult(var0, null, null, null, null);
   }

   public boolean ok() {
      return this.error == null;
   }

   public Map<String, Object> toJson() {
      if (this.error != null) {
         return this.error;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("version", this.version);
         var1.put("basedOn", this.basedOn);
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var2.put("verb", this.change.verb());
         if (this.change.background()) {
            var2.put("background", true);
         } else if (this.change.scenarioIndex() != null) {
            var2.put("scenario", this.change.scenarioIndex());
            var2.put("scenarioName", this.change.scenarioName());
         }

         var2.put("touches", this.change.touches());
         if (this.change.before() != null) {
            var2.put("before", this.change.before());
         }

         if (this.change.after() != null) {
            var2.put("after", this.change.after());
         }

         if (this.change.span() != null) {
            var2.put("span", this.change.span().toJson());
         }

         if (this.change.meaningUnknown()) {
            var2.put("meaningUnknown", true);
         }

         var1.put("change", var2);
         return var1;
      }
   }

   public record Change(String verb, Integer scenarioIndex, String scenarioName, boolean background, List<Integer> touches, String before, String after, Cards.Span span, boolean meaningUnknown) {

   }
}
