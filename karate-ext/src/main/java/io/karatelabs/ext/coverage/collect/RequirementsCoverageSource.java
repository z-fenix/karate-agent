package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.coverage.CoverageItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class RequirementsCoverageSource implements io.karatelabs.ext.coverage.CoverageSource {
   private final List<Path> a;
   private final io.karatelabs.ext.requirements.RequirementsRepository b;
   private final Object c;
   private final Object d;
   private final Object e;
   private List<CoverageItem> f;

   public RequirementsCoverageSource(Path var1) {
      this(var1, null, null);
   }

   public RequirementsCoverageSource(Path var1, Object var2) {
      this(var1, var2, null);
   }

   public RequirementsCoverageSource(Path var1, Object var2, Object var3) {
      this(List.of(var1), var2, var3);
   }

   public RequirementsCoverageSource(List<Path> var1, Object var2, Object var3) {
      this(var1, var2, var3, null);
   }

   public RequirementsCoverageSource(List<Path> var1, Object var2, Object var3, Path var4) {
      this(var1, var2, var3, var4, null);
   }

   public RequirementsCoverageSource(List<Path> var1, Object var2, Object var3, Path var4, Object var5) {
      this.a = List.copyOf(var1);
      this.b = new io.karatelabs.ext.requirements.RequirementsRepository(this.a, var4);
      this.c = var2;
      this.d = var3;
      this.e = var5;
   }

   @Override
   public List<CoverageItem> a() {
      if (this.f != null) {
         return this.f;
      } else {
         ArrayList<io.karatelabs.ext.coverage.CoverageItem> var1 = new ArrayList();
         var var2 = this.b.b();
         var var3 = this.b.c();
         var var4 = new LinkedHashMap<>(io.karatelabs.ext.requirements.ProofRequirements.a(this.e));

         for (io.karatelabs.ext.requirements.model.RequirementNode var6 : this.b.d()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("kind", "req");
            a(var7, "sourceFile", (String)var2.get(var6.b()));
            var7.put("anchor", io.karatelabs.ext.requirements.RequirementsMarkdownParser.d(a(var6)));
            if (var6.r() > 0) {
               var7.put("line", var6.r());
            }

            a(var7, "type", var6.c());
            a(var7, "reqStatus", var6.f());
            a(var7, "priority", var6.g());
            a(var7, "criticality", var6.h());
            if (var6.i() != null && "derived".equals(var6.i().get("source"))) {
               var7.put("derived", true);
            }

            var7.put("posture", io.karatelabs.ext.requirements.EvidencePolicy.a(var6.h(), this.c, b(var6)));
            a(var7, "body", var6.l());
            if (var6.j() != null && !var6.j().isEmpty()) {
               var7.put("ref", new ArrayList<>(var6.j()));
            }

            LinkedHashSet var8 = new LinkedHashSet();
            if (var6.k() != null) {
               var8.addAll(var6.k());
            }

            var8.addAll(io.karatelabs.ext.requirements.ProofResolver.a(this.d, b(var6)));
            if (!var8.isEmpty()) {
               var7.put("real", new ArrayList(var8));
            }

            var var9 = io.karatelabs.ext.requirements.AcceptanceCriterion.a(var6.m());
            if (var6.m() != null && !var6.m().isEmpty()) {
               var7.put("acceptance", new ArrayList<>(var6.m()));
            }

            if (!var9.isEmpty()) {
               ArrayList<java.lang.String> var10 = new ArrayList();

               for (io.karatelabs.ext.requirements.AcceptanceCriterion var12 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (var9)) {
                  var10.add(var6.b() + "/" + var12.a());
               }

               var7.put("decomposes", var10);
            }

            LinkedHashSet<java.lang.String> var17 = new LinkedHashSet();
            if (var6.i() != null) {
               var6.i().forEach((var1x, var2x) -> var17.add(var1x + "=" + var2x));
            }

            String var18 = var6.d() != null ? var6.d() : var6.b();
            var1.add(new CoverageItem(var6.b(), var18, var17, var6.e(), var7));

            for (io.karatelabs.ext.requirements.AcceptanceCriterion var13 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (var9)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
               var14.put("kind", "acceptance-criterion");
               var14.put("parentReq", var6.b());
               var14.put("criterion", var13.a());
               io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim var15 = (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim)var3.get(var6.b() + "/" + var13.a());
               if (var15 != null) {
                  var14.put("claimed", true);
                  a(var14, "claimedBy", var15.a());
                  a(var14, "claimedTask", var15.b());
               }

               List var16 = (List)var4.remove(var6.b() + "/" + var13.a());
               if (var16 != null) {
                  var14.put("proofs", var16);
               }

               var1.add(new CoverageItem(var6.b() + "/" + var13.a(), var13.b(), new LinkedHashSet<>(), null, var14));
            }
         }

         if (!var4.isEmpty()) {
            throw new IllegalArgumentException(
               "cov.requireProofs names no known acceptance criterion: " + var4.keySet() + " (ids are '<reqId>/<criterion>', e.g. ORD-001/2)"
            );
         } else {
            this.f = var1;
            return var1;
         }
      }
   }

   @Override
   public String b() {
      return "req";
   }

   @Override
   public Map<String, Object> c() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("title", "Requirements");
      if (!this.a.isEmpty()) {
         var1.put("home", this.a.get(0).toString());
      }

      return var1;
   }

   private static void a(Map<String, Object> var0, String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         var0.put(var1, var2);
      }
   }

   private static String a(io.karatelabs.ext.requirements.model.RequirementNode var0) {
      if (var0.q() != null && !var0.q().isBlank()) {
         return var0.q();
      } else {
         return var0.d() != null && !var0.d().isBlank() ? var0.b() + ": " + var0.d() : var0.b();
      }
   }

   private static Map<String, Object> b(io.karatelabs.ext.requirements.model.RequirementNode var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", var0.b());
      a(var1, "criticality", var0.h());
      a(var1, "type", var0.c());
      a(var1, "title", var0.d());
      a(var1, "parent", var0.e());
      var1.put("tags", var0.i() != null ? new LinkedHashMap<>(var0.i()) : new LinkedHashMap<>());
      return var1;
   }
}
