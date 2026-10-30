package io.karatelabs.ext.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RuleTreeJsonMapper {
   private RuleTreeJsonMapper() {
   }

   static List<Map<String, Object>> a(List<RuleDiffLine> var0) {
      ArrayList var1 = new ArrayList(var0.size());

      for (RuleDiffLine var3 : var0) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("kind", var3.a());
         var4.put("text", var3.b());
         var1.add(var4);
      }

      return var1;
   }

   static List<Map<String, Object>> b(List<ResultTreeNode> var0) {
      ArrayList var1 = new ArrayList(var0.size());

      for (ResultTreeNode var3 : var0) {
         var1.add(a(var3));
      }

      return var1;
   }

   private static Map<String, Object> a(ResultTreeNode var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.a() != null) {
         var1.put("key", var0.a());
      }

      var1.put("line", var0.b());
      if (var0.c() > 0) {
         var1.put("heading", var0.c());
      }

      var1.put("container", var0.d());
      if (var0.e() != null) {
         var1.put("text", var0.e());
      }

      if (var0.h() != null) {
         var1.put("status", var0.h());
      }

      if (var0.i() != null) {
         var1.put("prevText", var0.i());
      }

      if (var0.j() != null) {
         var1.put("wordDiff", a(var0.j()));
      }

      if (!var0.k().isEmpty()) {
         var1.put("children", b(var0.k()));
      }

      return var1;
   }
}
