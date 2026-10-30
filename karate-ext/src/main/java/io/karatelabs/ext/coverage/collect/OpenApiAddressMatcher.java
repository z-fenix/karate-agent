package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Outcome;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class OpenApiAddressMatcher implements AddressMatcher {
   private final List<String> a;

   OpenApiAddressMatcher(Contract var1) {
      this.a = (var1 == null ? null : var1.ext().get("basePaths")) instanceof List var3 && !var3.isEmpty() ? var3 : io.karatelabs.ext.openapi.ServerBasePathResolver.a;
   }

   @Override
   public List<CoverageItem> buildItems(Contract var1) {
      ArrayList<io.karatelabs.ext.coverage.CoverageItem> var2 = new ArrayList();

      for (Operation var4 : var1.operations()) {
         var2.add(a(var4));
      }

      return var2;
   }

   private static CoverageItem a(Operation var0) {
      String var1 = var0.address().qualifier();
      String var2 = var0.address().primary();
      String var3 = var1 + " " + var2;
      var var4 = new LinkedHashSet<>(var0.tags());
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("method", var1);
      var5.put("path", var2);
      var5.put("operationId", var0.ext().get("operationId"));
      var5.put("summary", var0.summary());
      var5.put("description", var0.ext().get("description"));
      var5.put("deprecated", var0.deprecated());
      ArrayList<java.lang.String> var6 = new ArrayList();
      ArrayList<java.lang.String> var7 = new ArrayList();

      for (Message var9 : var0.inputs()) {
         if (var9.role() == Message.Role.PARAM) {
            String var10 = (String)var9.ext().get("in");
            boolean var11 = Boolean.TRUE.equals(var9.ext().get("pathLevel"));
            if ("query".equals(var10)) {
               if (!var11 || !var6.contains(var9.name())) {
                  var6.add(var9.name());
               }
            } else if ("path".equals(var10) && (!var11 || !var7.contains(var9.name()))) {
               var7.add(var9.name());
            }
         }
      }

      var5.put("queryParams", var6);
      var5.put("pathParams", var7);
      var5.put("responses", var0.outcomes().stream().map(Outcome::code).toList());
      return new CoverageItem(var0.id(), var3, var4, null, var5);
   }

   @Override
   public Map<String, Object> buildMetadata(Contract var1, List<CoverageItem> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("specType", var1.ext().get("specType"));
      var3.put("title", var1.title());
      var3.put("description", var1.ext().get("description"));
      var3.put("version", var1.version());
      var3.put("baseUrl", var1.ext().get("baseUrl"));
      LinkedHashSet var4 = new LinkedHashSet();

      for (CoverageItem var6 : var2) {
         var4.addAll(var6.tags());
      }

      var3.put("tags", new ArrayList(var4));
      return var3;
   }

   @Override
   public boolean accepts(Action var1) {
      return var1 instanceof io.karatelabs.ext.coverage.HttpAction;
   }

   @Override
   public CoverageItem match(Action var1, List<CoverageItem> var2) {
      if (!(var1 instanceof io.karatelabs.ext.coverage.HttpAction var3)) {
         return null;
      } else {
         ArrayList<java.lang.String> var4 = new ArrayList();

         for (CoverageItem var6 : var2) {
            if (var3.e().equalsIgnoreCase(var6.getMethod()) && var6.getPath() != null) {
               var4.add(var6.getPath());
            }
         }

         io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var9 = null;

         for (String var7 : io.karatelabs.ext.openapi.ServerBasePathResolver.a(var3.g(), this.a)) {
            var9 = io.karatelabs.ext.openapi.PathTemplateMatcher.b(var7, var4);
            if (var9 != null) {
               break;
            }
         }

         if (var9 == null) {
            return null;
         } else {
            String var11 = var9.a();

            for (CoverageItem var8 : var2) {
               if (var3.e().equalsIgnoreCase(var8.getMethod()) && var11.equals(var8.getPath())) {
                  return var8;
               }
            }

            return null;
         }
      }
   }

   @Override
   public List<Warning> warnings(Action var1, CoverageItem var2, List<CoverageItem> var3) {
      if (var1 instanceof io.karatelabs.ext.coverage.HttpAction var4) {
         ArrayList<io.karatelabs.ext.coverage.Warning> var5 = new ArrayList();
         String var6 = var4.e() + " " + var2.getPath();
         List var7 = var2.getMetadata("responses");
         if (var7 != null && !var7.contains(String.valueOf(var4.i()))) {
            var5.add(Warning.undocumentedResponse(var6, var4.i()));
         }

         if (var2.isDeprecated()) {
            var5.add(Warning.deprecatedTested(var6));
         }

         io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch var8 = this.a(var4, var3);
         if (var8.a() == MatchKind.AMBIGUOUS) {
            var5.add(Warning.ambiguousMatch(var4.e() + " " + var4.g(), var8.b()));
         }

         return var5;
      } else {
         return List.of();
      }
   }

   public List<CoverageItem> a(Action var1, List<CoverageItem> var2) {
      if (var1 instanceof io.karatelabs.ext.coverage.HttpAction var3 && this.match(var1, var2) != null) {
         var var4 = this.a(var3, var2).b();
         ArrayList<io.karatelabs.ext.coverage.CoverageItem> var5 = new ArrayList();

         for (CoverageItem var7 : var2) {
            if (var3.e().equalsIgnoreCase(var7.getMethod()) && var4.contains(var7.getPath())) {
               var5.add(var7);
            }
         }

         return var5;
      } else {
         return List.of();
      }
   }

   @Override
   public MatchKind matchConfidence(Action var1, CoverageItem var2, List<CoverageItem> var3) {
      return var1 instanceof io.karatelabs.ext.coverage.HttpAction var4 ? this.a(var4, var3).a() : MatchKind.EXACT;
   }

   private io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch a(io.karatelabs.ext.coverage.HttpAction var1, List<CoverageItem> var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();
      io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch var4 = null;

      for (String var6 : io.karatelabs.ext.openapi.ServerBasePathResolver.a(var1.g(), this.a)) {
         io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch var7 = a(var6, var1.e(), var2);
         if (var7 != null) {
            if (var4 == null) {
               var4 = var7;
            }

            for (String var9 : var7.b()) {
               if (!var3.contains(var9)) {
                  var3.add(var9);
               }
            }
         }
      }

      if (var4 == null) {
         return new io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch(MatchKind.EXACT, List.of());
      } else {
         return var3.size() >= 2 ? new io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch(MatchKind.AMBIGUOUS, var3) : var4;
      }
   }

   private static io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch a(String var0, String var1, List<CoverageItem> var2) {
      ArrayList<io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch> var3 = new ArrayList();
      ArrayList<java.lang.String> var4 = new ArrayList();
      int var5 = -1;

      for (CoverageItem var7 : var2) {
         if (var1.equalsIgnoreCase(var7.getMethod()) && var7.getPath() != null) {
            io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var8 = io.karatelabs.ext.openapi.PathTemplateMatcher.a(var0, var7.getPath());
            if (var8 != null) {
               var3.add(var8);
               var5 = Math.max(var5, var8.c());
            }
         }
      }

      io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var9 = null;

      for (io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var11 : (Iterable<io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch>) (Iterable<?>) (var3)) {
         if (var11.c() == var5) {
            var4.add(var11.a());
            var9 = var11;
         }
      }

      if (var9 == null) {
         return null;
      } else {
         return var4.size() >= 2 ? new io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch(MatchKind.AMBIGUOUS, var4) : new io.karatelabs.ext.coverage.collect.OpenApiAddressMatcher.AddressMatch(var9.b().isEmpty() ? MatchKind.EXACT : MatchKind.TEMPLATED, var4);
      }
   }

   @Override
   public List<Warning> loadWarnings(List<CoverageItem> var1) {
      LinkedHashMap<java.lang.String, java.util.ArrayList> var2 = new LinkedHashMap();

      for (CoverageItem var4 : var1) {
         if (var4.getMethod() != null && var4.getPath() != null) {
            var2.computeIfAbsent(var4.getMethod().toUpperCase(), var0 -> new ArrayList<>()).add(var4.getPath());
         }
      }

      ArrayList<io.karatelabs.ext.coverage.Warning> var9 = new ArrayList();

      for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         List var6 = (List)var5.getValue();

         for (int var7 = 0; var7 < var6.size(); var7++) {
            for (int var8 = var7 + 1; var8 < var6.size(); var8++) {
               if (io.karatelabs.ext.openapi.PathTemplateMatcher.b((String)var6.get(var7), (String)var6.get(var8))) {
                  var9.add(
                     Warning.overlappingTemplates((String)var5.getKey() + " " + (String)var6.get(var7), (String)var5.getKey() + " " + (String)var6.get(var8))
                  );
               }
            }
         }
      }

      return var9;
   }

   @Override
   public Map<String, Object> aggregate(List<Action> var1, CoverageItem var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("callCount", var1.size());
      long var4 = 0L;
      LinkedHashMap<java.lang.Integer, java.lang.Integer> var6 = new LinkedHashMap();
      HashSet var7 = new HashSet();
      LinkedHashSet var8 = new LinkedHashSet();

      for (Action var10 : var1) {
         if (var10 instanceof io.karatelabs.ext.coverage.HttpAction var11) {
            var4 += var11.j();
            var6.merge(var11.i(), 1, Integer::sum);

            for (String var13 : io.karatelabs.ext.openapi.ServerBasePathResolver.a(var11.g(), this.a)) {
               io.karatelabs.ext.openapi.PathTemplateMatcher.PathTemplateMatch var14 = io.karatelabs.ext.openapi.PathTemplateMatcher.a(var13, var2.getPath());
               if (var14 != null) {
                  var7.addAll(var14.b().values());
                  break;
               }
            }

            var8.addAll(var11.h().keySet());
         }
      }

      var3.put("avgDurationMs", var1.isEmpty() ? 0L : var4 / var1.size());
      if (!var6.isEmpty()) {
         var3.put("statusCodes", var6);
      }

      List var15 = var2.getMetadata("pathParams");
      if (var15 != null && !var15.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Integer> var16 = new LinkedHashMap();
         var16.put("_total", var7.size());
         var3.put("pathParamCardinality", var16);
      }

      var3.put("queryParamsUsed", new ArrayList(var8));
      return var3;
   }

   private record AddressMatch(MatchKind a, List<String> b) {
   }
}
