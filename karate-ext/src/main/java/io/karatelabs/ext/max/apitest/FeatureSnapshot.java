package io.karatelabs.ext.max.apitest;

import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.SourceSpan;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

record FeatureSnapshot(List<String> a, String b, String c, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot d, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot> e, List<String> f) {


   static io.karatelabs.ext.max.apitest.FeatureSnapshot a(FeatureSource var0) {
      Feature var1 = var0.feature();
      String var2 = var0.text();
      HashSet var3 = new HashSet();
      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var4 = null;
      if (var1.getBackground() != null && var1.getBackground().getSteps() != null) {
         var4 = new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(false, List.of(), null, null, a(var1.getBackground().getSteps(), var2, var3), List.of());
      }

      ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot> var5 = new ArrayList();

      for (FeatureSection var7 : var1.getSections()) {
         if (var7.isOutline()) {
            ScenarioOutline var8 = var7.getScenarioOutline();
            ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot> var9 = new ArrayList();

            for (ExamplesTable var11 : var8.getExamplesTables()) {
               var9.add(
                  new io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot(
                     var11.getName(), a(var11.getTags()), var11.getTable() == null ? List.of() : b(var11.getTable().getRows())
                  )
               );
            }

            var5.add(
               new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(true, a(var8.getTags()), var8.getName(), var8.getDescription(), a(var8.getSteps(), var2, var3), var9)
            );
         } else {
            var5.add(
               new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(
                  false,
                  a(var7.getScenario().getTags()),
                  var7.getScenario().getName(),
                  var7.getScenario().getDescription(),
                  a(var7.getScenario().getSteps(), var2, var3),
                  List.of()
               )
            );
         }
      }

      ArrayList<java.lang.String> var12 = new ArrayList();

      for (SourceSpan var14 : var1.getCommentSpans()) {
         if (!var3.contains(var14)) {
            var12.add(var14.text(var2).strip());
         }
      }

      return new io.karatelabs.ext.max.apitest.FeatureSnapshot(a(var1.getTags()), var1.getName(), var1.getDescription(), var4, var5, var12);
   }

   private static List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> a(List<Step> var0, String var1, Set<SourceSpan> var2) {
      ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var3 = new ArrayList();

      for (Step var5 : var0) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (SourceSpan var8 : FeatureSource.ownedComments(var5)) {
            var2.add(var8);
            var6.add(var8.text(var1).strip());
         }

         var3.add(
            new io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot(
               var5.getPrefix(), var5.getKeyword(), var5.getText(), var5.getDocString(), var5.getTable() == null ? null : b(var5.getTable().getRows()), var6
            )
         );
      }

      return var3;
   }

   private static List<List<String>> b(List<List<String>> var0) {
      ArrayList<java.util.List<java.lang.String>> var1 = new ArrayList();

      for (List var3 : var0) {
         var1.add(List.copyOf(var3));
      }

      return var1;
   }

   static List<String> a(List<Tag> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Tag var3 : (Iterable<Tag>) (Iterable<?>) (var0 == null ? List.of() : var0)) {
         var1.add("@" + var3.getText());
      }

      return var1;
   }

   List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> a(Integer var1) {
      return var1 == null ? (this.d == null ? null : this.d.f()) : this.e.get(var1).f();
   }

   io.karatelabs.ext.max.apitest.FeatureSnapshot a(Integer var1, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var2) {
      if (var1 == null) {
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot(this.a, this.b, this.c, this.d.a(var2), this.e, this.f);
      } else {
         var var3 = new ArrayList<>(this.e);
         var3.set(var1, ((io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot)var3.get(var1)).a(var2));
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot(this.a, this.b, this.c, this.d, var3, this.f);
      }
   }

   io.karatelabs.ext.max.apitest.FeatureSnapshot a(int var1, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var2) {
      var var3 = new ArrayList<>(this.e);
      var3.set(var1, var2);
      return new io.karatelabs.ext.max.apitest.FeatureSnapshot(this.a, this.b, this.c, this.d, var3, this.f);
   }

   io.karatelabs.ext.max.apitest.FeatureSnapshot a(io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var1) {
      var var2 = new ArrayList<>(this.e);
      var2.add(var1);
      return new io.karatelabs.ext.max.apitest.FeatureSnapshot(this.a, this.b, this.c, this.d, var2, this.f);
   }

   static List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> a(String var0) {
      FeatureSource var1 = FeatureSource.of("Feature:\nScenario:\n" + var0 + "\n");
      if (var1.editable() && var1.feature().getSections().size() == 1) {
         io.karatelabs.ext.max.apitest.FeatureSnapshot var2 = a(var1);
         return !var2.f().isEmpty() ? null : var2.e().get(0).f();
      } else {
         return null;
      }
   }

   static boolean a(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0, String var1) {
      var var2 = a(var1);
      return var2 != null && var2.size() == 1 && ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var2.get(0)).a().equals(var0.a());
   }

   static String a(io.karatelabs.ext.max.apitest.FeatureSnapshot var0, io.karatelabs.ext.max.apitest.FeatureSnapshot var1) {
      if (var0.equals(var1)) {
         return null;
      } else if (!Objects.equals(var0.a(), var1.a())) {
         return "the feature's tags read " + var1.a() + ", not " + var0.a();
      } else if (!Objects.equals(var0.b(), var1.b())) {
         return "the feature's name changed";
      } else if (!Objects.equals(var0.c(), var1.c())) {
         return "the feature's description changed";
      } else if (!Objects.equals(var0.f(), var1.f())) {
         return "comments outside the edit changed: " + var1.f() + " instead of " + var0.f();
      } else if (!Objects.equals(var0.d(), var1.d())) {
         return var0.d() != null && var1.d() != null
            ? "Background: " + a(var0.d(), var1.d())
            : "the Background " + (var1.d() == null ? "disappeared" : "appeared");
      } else if (var0.e().size() != var1.e().size()) {
         return "the scenario count changed (" + var0.e().size() + " → " + var1.e().size() + ")";
      } else {
         for (int var2 = 0; var2 < var0.e().size(); var2++) {
            io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var3 = var0.e().get(var2);
            io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var4 = var1.e().get(var2);
            if (!var3.equals(var4)) {
               return "scenario " + var2 + " (" + var4.a() + "): " + a(var3, var4);
            }
         }

         return "the shapes differ";
      }
   }

   private static String a(io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var0, io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var1) {
      if (var0.b() != var1.b()) {
         return "it became " + (var1.b() ? "an outline" : "a scenario");
      } else if (!Objects.equals(var0.c(), var1.c())) {
         return "tags read " + var1.c() + ", not " + var0.c();
      } else if (!Objects.equals(var0.d(), var1.d())) {
         return "the name changed to '" + var1.d() + "'";
      } else if (!Objects.equals(var0.e(), var1.e())) {
         return "the description changed";
      } else if (var0.f().size() != var1.f().size()) {
         return "the step count changed (" + var0.f().size() + " → " + var1.f().size() + ")";
      } else {
         for (int var2 = 0; var2 < var0.f().size(); var2++) {
            io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var3 = var0.f().get(var2);
            io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var4 = var1.f().get(var2);
            if (!var3.equals(var4)) {
               if (!var3.h().equals(var4.h())) {
                  return "step " + var2 + " owns comments " + var4.h() + ", expected " + var3.h();
               }

               return "step " + var2 + " reads '" + var4.b() + "', expected '" + var3.b() + "'";
            }
         }

         return !Objects.equals(var0.g(), var1.g()) ? "the Examples changed: " + var1.g() + ", expected " + var0.g() : "it differs";
      }
   }

   record ExamplesSnapshot(String a, List<String> b, List<List<String>> c) {}

   static final class ScenarioSnapshot {
      private final boolean a;
      private final List<String> b;
      private final String c;
      private final String d;
      private final List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> e;
      private final List<io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot> f;

      ScenarioSnapshot(boolean var1, List<String> var2, String var3, String var4, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var5, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot> var6) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
         this.f = var6;
      }

      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot a(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var1) {
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(this.a, this.b, this.c, this.d, List.copyOf(var1), this.f);
      }

      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot b(List<String> var1) {
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(this.a, List.copyOf(var1), this.c, this.d, this.e, this.f);
      }

      io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot a(int var1, List<List<String>> var2) {
         var var3 = new ArrayList<>(this.f);
         io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot var4 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot)var3.get(var1);
         var3.set(var1, new io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot(var4.a(), var4.b(), List.copyOf(var2)));
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(this.a, this.b, this.c, this.d, this.e, var3);
      }

      String a() {
         return (this.a ? "Scenario Outline" : "Scenario") + (this.c == null ? "" : ": " + this.c);
      }

      public boolean b() {
         return this.a;
      }

      public List<String> c() {
         return this.b;
      }

      public String d() {
         return this.c;
      }

      public String e() {
         return this.d;
      }

      public List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> f() {
         return this.e;
      }

      public List<io.karatelabs.ext.max.apitest.FeatureSnapshot.ExamplesSnapshot> g() {
         return this.f;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot other)) return false;
         return this.a == other.a && Objects.equals(this.b, other.b) && Objects.equals(this.c, other.c)
            && Objects.equals(this.d, other.d) && Objects.equals(this.e, other.e) && Objects.equals(this.f, other.f);
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.a, this.b, this.c, this.d, this.e, this.f);
      }

      @Override
      public String toString() {
         return "b$b[outline=" + this.a + ", tags=" + this.b + ", name=" + this.c + ", description=" + this.d
            + ", steps=" + this.e + ", examples=" + this.f + "]";
      }
   }

   static final class StepSnapshot {
      private final String a;
      private final String b;
      private final String c;
      private final String d;
      private final List<List<String>> e;
      private final List<String> f;

      StepSnapshot(String var1, String var2, String var3, String var4, List<List<String>> var5, List<String> var6) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
         this.f = var6;
      }

      io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot a() {
         return this.f.isEmpty() ? this : new io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot(this.a, this.b, this.c, this.d, this.e, List.of());
      }

      io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot a(List<String> var1) {
         return new io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot(this.a, this.b, this.c, this.d, this.e, List.copyOf(var1));
      }

      String b() {
         StringBuilder var1 = new StringBuilder();
         if (this.b != null) {
            var1.append(this.b);
         }

         if (this.c != null) {
            var1.append(var1.isEmpty() ? "" : " ").append(this.c.strip());
         }

         if (this.d != null) {
            var1.append(" \"\"\"…\"\"\"");
         }

         if (this.e != null) {
            var1.append(" | … |");
         }

         return var1.toString();
      }

      public String c() {
         return this.a;
      }

      public String d() {
         return this.b;
      }

      public String e() {
         return this.c;
      }

      public String f() {
         return this.d;
      }

      public List<List<String>> g() {
         return this.e;
      }

      public List<String> h() {
         return this.f;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot other)) return false;
         return Objects.equals(this.a, other.a) && Objects.equals(this.b, other.b) && Objects.equals(this.c, other.c)
            && Objects.equals(this.d, other.d) && Objects.equals(this.e, other.e) && Objects.equals(this.f, other.f);
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.a, this.b, this.c, this.d, this.e, this.f);
      }

      @Override
      public String toString() {
         return "b$c[prefix=" + this.a + ", keyword=" + this.b + ", text=" + this.c + ", docString=" + this.d
            + ", table=" + this.e + ", comments=" + this.f + "]";
      }
   }
}
