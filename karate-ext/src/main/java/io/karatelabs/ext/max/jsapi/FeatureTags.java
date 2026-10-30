package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Tag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class FeatureTags {
   private static final String BOM = "\ufeff";

   private FeatureTags() {
   }

   public static FeatureTags.Edit apply(String var0, Object var1, boolean var2, List<String> var3, List<String> var4) {
      for (String var6 : var3) {
         String var7 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var6);
         if (var7 != null) {
            return FeatureTags.Edit.refuse(JsApi.missing("File.tag refuses " + var7 + " — see the tag grammar (@<family>=<namespace>:<id>, or a bare @<name>)"));
         }
      }

      boolean var31 = var0.startsWith("\ufeff");
      String var32 = var31 ? var0.substring(1) : var0;

      Feature var33;
      try {
         var33 = Feature.read(Resource.text(var32));
      } catch (RuntimeException var30) {
         return FeatureTags.Edit.refuse(JsApi.missing("File.tag could not parse the feature: " + var30.getMessage()));
      }

      var var8 = var33.getSections();
      int var9 = -1;
      if (!var2) {
         ArrayList<java.lang.Integer> var10 = new ArrayList();
         if (var1 instanceof Number var11) {
            int var36 = var11.intValue();

            for (int var39 = 0; var39 < var8.size(); var39++) {
               if (keywordLine((FeatureSection)var8.get(var39)) == var36) {
                  var10.add(var39);
               }
            }

            if (var10.isEmpty()) {
               return FeatureTags.Edit.refuse(JsApi.notFound("File.tag: no scenario keyword on line " + var36 + " — the sections are " + describe(var8)));
            }
         } else {
            String var12 = String.valueOf(var1);

            for (int var13 = 0; var13 < var8.size(); var13++) {
               if (var12.equals(name((FeatureSection)var8.get(var13)))) {
                  var10.add(var13);
               }
            }

            if (var10.isEmpty()) {
               return FeatureTags.Edit.refuse(JsApi.notFound("File.tag: no scenario named '" + var12 + "' — the sections are " + describe(var8)));
            }

            if (var10.size() > 1) {
               var var38 = var10.stream().map(var1x -> String.valueOf(keywordLine((FeatureSection)var8.get(var1x)))).toList();
               return FeatureTags.Edit.refuse(
                  JsApi.missing(
                     "File.tag: '"
                        + var12
                        + "' names "
                        + var10.size()
                        + " scenarios, on lines "
                        + String.join(", ", var38)
                        + " — pass the line number instead of the name"
                  )
               );
            }
         }

         var9 = (Integer)var10.get(0);
      }

      List var34 = var2 ? var33.getTags() : tagsOf((FeatureSection)var8.get(var9));
      List var35 = texts(var34);
      List var37 = var2 ? List.of() : texts(var33.getTags());
      List var40 = effective(var37);

      for (String var15 : var4) {
         String var16 = commaHost(var35, var15);
         if (var16 != null) {
            return FeatureTags.Edit.refuse(
               JsApi.missing(
                  "File.tag: '"
                     + var15
                     + "' is one value inside '"
                     + var16
                     + "' — it does not edit inside a comma-joined tag. Remove '"
                     + var16
                     + "' then add the tokens you want."
               )
            );
         }
      }

      ArrayList<java.lang.String> var41 = new ArrayList();
      ArrayList<java.lang.String> var42 = new ArrayList();
      ArrayList<java.lang.String> var43 = new ArrayList();
      ArrayList<java.lang.String> var17 = new ArrayList();
      ArrayList<java.lang.String> var18 = new ArrayList();

      for (String var20 : var3) {
         if (var35.contains(var20) || commaHost(var35, var20) != null) {
            var41.add(var20);
         } else if (var40.contains(var20)) {
            var42.add(var20);
         } else {
            var17.add(var20);
         }
      }

      for (String var46 : var4) {
         if (var40.contains(var46)) {
            return FeatureTags.Edit.refuse(
               JsApi.missing(
                  "File.tag: '"
                     + var46
                     + "' is a FEATURE tag inherited by this scenario — remove it with {feature: true}, which drops it from every scenario in the file."
               )
            );
         }

         if (var35.contains(var46)) {
            var18.add(var46);
         } else {
            var43.add(var46);
         }
      }

      io.karatelabs.ext.max.jsapi.FeatureTags.EditableLines var45 = io.karatelabs.ext.max.jsapi.FeatureTags.EditableLines.a(var32);
      List var47 = ownedLines(var34);

      for (String var22 : (Iterable<String>) (Iterable<?>) (var18)) {
         for (int var24 : (Iterable<java.lang.Integer>) (Iterable<?>) (linesOf(var34, var22))) {
            var45.a.set(var24 - 1, removeToken(var45.a.get(var24 - 1), var22));
         }
      }

      if (!var17.isEmpty()) {
         String var48 = String.join(" ", var17);
         if (var47.isEmpty()) {
            int var51 = var2 ? var33.getLine() : keywordLine((FeatureSection)var8.get(var9));
            var45.a(var51 - 1, indent(var45.a.get(var51 - 1)) + var48);
         } else {
            int var52 = (Integer)var47.get(var47.size() - 1);
            String var55 = var45.a.get(var52 - 1);
            var45.a.set(var52 - 1, var55.isBlank() ? indent(var55) + var48 : var55.stripTrailing() + " " + var48);
         }
      }

      for (int var49 = var47.size() - 1; var49 >= 0; var49--) {
         int var53 = (Integer)var47.get(var49) - 1;
         if (var45.a.get(var53).isBlank()) {
            var45.a(var53);
         }
      }

      String var50 = var45.a();
      ArrayList var54 = new ArrayList(var35);
      var54.removeAll(var18);
      var54.addAll(var17);
      Object var56 = var2 ? var54 : dedupe(concat(var37, var54));

      Feature var57;
      try {
         var57 = Feature.read(Resource.text(var50));
      } catch (RuntimeException var29) {
         return FeatureTags.Edit.refuse(JsApi.missing("File.tag wrote nothing — the spliced feature does not parse: " + var29.getMessage()));
      }

      var var25 = var57.getSections();
      if (var25.size() != var8.size()) {
         return FeatureTags.Edit.refuse(
            JsApi.missing("File.tag wrote nothing — the splice changed the scenario count (" + var8.size() + " → " + var25.size() + ")")
         );
      } else {
         List var26 = var2 ? texts(var57.getTags()) : tagTexts((FeatureSection)var25.get(var9));
         List var27 = var2 ? var26 : dedupe(concat(texts(var57.getTags()), var26));
         if (!var27.equals(var56)) {
            return FeatureTags.Edit.refuse(JsApi.missing("File.tag wrote nothing — the spliced tags read " + var27 + ", not the intended " + var56));
         } else {
            for (int var28 = 0; var28 < var8.size(); var28++) {
               if (var28 != var9 && !tagTexts((FeatureSection)var8.get(var28)).equals(tagTexts((FeatureSection)var25.get(var28)))) {
                  return FeatureTags.Edit.refuse(
                     JsApi.missing("File.tag wrote nothing — the splice also changed the tags of '" + name((FeatureSection)var25.get(var28)) + "'")
                  );
               }
            }

            if (!var2 && !texts(var33.getTags()).equals(texts(var57.getTags()))) {
               return FeatureTags.Edit.refuse(JsApi.missing("File.tag wrote nothing — the splice also changed the feature's own tags"));
            } else {
               int var58 = var2 ? var57.getLine() : keywordLine((FeatureSection)var25.get(var9));
               return new FeatureTags.Edit(
                  null, var31 ? "\ufeff" + var50 : var50, var2 ? null : name((FeatureSection)var25.get(var9)), var58, var27, var17, var18, var41, var43, var42
               );
            }
         }
      }
   }

   private static int keywordLine(FeatureSection var0) {
      return var0.isOutline() ? var0.getScenarioOutline().getLine() : var0.getScenario().getLine();
   }

   private static String name(FeatureSection var0) {
      return var0.isOutline() ? var0.getScenarioOutline().getName() : var0.getScenario().getName();
   }

   private static List<Tag> tagsOf(FeatureSection var0) {
      List var1 = var0.isOutline() ? var0.getScenarioOutline().getTags() : var0.getScenario().getTags();
      return var1 == null ? List.of() : var1;
   }

   private static List<String> tagTexts(FeatureSection var0) {
      return texts(tagsOf(var0));
   }

   private static List<String> texts(List<Tag> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Tag var3 : (Iterable<Tag>) (Iterable<?>) (var0 == null ? List.of() : var0)) {
         var1.add("@" + var3.getText());
      }

      return var1;
   }

   private static List<Integer> ownedLines(List<Tag> var0) {
      return new ArrayList<>(new TreeSet<>((var0 == null ? List.<Tag>of() : var0).stream().map(Tag::getLine).toList()));
   }

   private static List<Integer> linesOf(List<Tag> var0, String var1) {
      TreeSet<java.lang.Integer> var2 = new TreeSet();

      for (Tag var4 : var0) {
         if (var1.equals("@" + var4.getText())) {
            var2.add(var4.getLine());
         }
      }

      return new ArrayList<>(var2);
   }

   private static List<String> effective(List<String> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var3 : var0) {
         var1.add(var3);
         int var4 = var3.indexOf(61);
         if (var4 >= 0 && var3.indexOf(44) >= 0) {
            String var5 = var3.substring(0, var4 + 1);

            for (String var9 : var3.substring(var4 + 1).split(",", -1)) {
               if (!var9.isBlank()) {
                  var1.add(var5 + var9.trim());
               }
            }
         }
      }

      return var1;
   }

   private static String commaHost(List<String> var0, String var1) {
      int var2 = var1.indexOf(61);
      if (var2 < 0) {
         return null;
      } else {
         String var3 = var1.substring(0, var2 + 1);
         String var4 = var1.substring(var2 + 1);

         for (String var6 : var0) {
            if (!var6.equals(var1) && var6.startsWith(var3)) {
               String var7 = var6.substring(var3.length());
               if (var7.indexOf(44) >= 0 && List.of(var7.split(",", -1)).contains(var4)) {
                  return var6;
               }
            }
         }

         return null;
      }
   }

   private static String removeToken(String var0, String var1) {
      int var2 = indent(var0).length();
      String var3 = var0;

      while (true) {
         int var4 = indexOfToken(var3, var1);
         if (var4 < 0) {
            return var3;
         }

         int var5 = var4 + var1.length();
         if (var4 > var2 && var3.charAt(var4 - 1) == ' ') {
            var4--;
         } else if (var5 < var3.length() && var3.charAt(var5) == ' ') {
            var5++;
         }

         var3 = var3.substring(0, var4) + var3.substring(var5);
      }
   }

   private static int indexOfToken(String var0, String var1) {
      int var2 = 0;

      while (true) {
         int var3 = var0.indexOf(var1, var2);
         if (var3 < 0) {
            return -1;
         }

         int var4 = var3 + var1.length();
         boolean var5 = var3 == 0 || Character.isWhitespace(var0.charAt(var3 - 1));
         boolean var6 = var4 == var0.length() || Character.isWhitespace(var0.charAt(var4));
         if (var5 && var6) {
            return var3;
         }

         var2 = var3 + 1;
      }
   }

   private static String indent(String var0) {
      int var1 = 0;

      while (var1 < var0.length() && (var0.charAt(var1) == ' ' || var0.charAt(var1) == '\t')) {
         var1++;
      }

      return var0.substring(0, var1);
   }

   private static String describe(List<FeatureSection> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (FeatureSection var3 : var0) {
         var1.add("'" + name(var3) + "' (line " + keywordLine(var3) + ")");
      }

      return var1.isEmpty() ? "(none)" : String.join(", ", var1);
   }

   private static List<String> concat(List<String> var0, List<String> var1) {
      var var2 = new ArrayList<>(var0);
      var2.addAll(var1);
      return var2;
   }

   private static List<String> dedupe(List<String> var0) {
      return new ArrayList<>(new LinkedHashSet<>(var0));
   }

   public record Edit(Map<String, Object> error, String content, String scenario, int line, List<String> tags, List<String> added, List<String> removed, List<String> unchanged, List<String> absent, List<String> inherited) {


      static FeatureTags.Edit refuse(Map<String, Object> var0) {
         return new FeatureTags.Edit(var0, null, null, 0, null, null, null, null, null, null);
      }
   }

   private static final class EditableLines {
      final List<String> a = new ArrayList<>();
      final List<String> b = new ArrayList<>();

      static io.karatelabs.ext.max.jsapi.FeatureTags.EditableLines a(String var0) {
         io.karatelabs.ext.max.jsapi.FeatureTags.EditableLines var1 = new io.karatelabs.ext.max.jsapi.FeatureTags.EditableLines();
         int var2 = 0;

         for (int var3 = 0; var3 < var0.length(); var3++) {
            if (var0.charAt(var3) == '\n') {
               boolean var4 = var3 > var2 && var0.charAt(var3 - 1) == '\r';
               var1.a.add(var0.substring(var2, var4 ? var3 - 1 : var3));
               var1.b.add(var4 ? "\r\n" : "\n");
               var2 = var3 + 1;
            }
         }

         if (var2 < var0.length()) {
            var1.a.add(var0.substring(var2));
            var1.b.add("");
         }

         return var1;
      }

      void a(int var1, String var2) {
         String var3 = this.b(var1);
         this.a.add(var1, var2);
         this.b.add(var1, var3);
      }

      void a(int var1) {
         this.a.remove(var1);
         this.b.remove(var1);
      }

      private String b(int var1) {
         if (var1 < this.b.size() && !this.b.get(var1).isEmpty()) {
            return this.b.get(var1);
         } else {
            for (String var3 : this.b) {
               if (!var3.isEmpty()) {
                  return var3;
               }
            }

            return "\n";
         }
      }

      String a() {
         StringBuilder var1 = new StringBuilder();

         for (int var2 = 0; var2 < this.a.size(); var2++) {
            var1.append(this.a.get(var2)).append(this.b.get(var2));
         }

         return var1.toString();
      }
   }
}
