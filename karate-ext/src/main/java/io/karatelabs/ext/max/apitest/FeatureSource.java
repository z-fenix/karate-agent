package io.karatelabs.ext.max.apitest;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.SourceSpan;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Table;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FeatureSource {
   static final int MAX_LINES = 32767;
   private final byte[] bytes;
   private final String text;
   private final String version;
   private final boolean bom;
   private final String eol;
   private final boolean finalNewline;
   private final Feature feature;
   private final String notEditable;
   private int[] byteAt;

   private FeatureSource(byte[] var1, String var2, Feature var3, String var4) {
      this.bytes = var1;
      this.text = var2;
      this.version = io.karatelabs.ext.common.Sha256.a(var1);
      this.feature = var3;
      this.notEditable = var4;
      this.bom = var2 != null && var2.startsWith("\ufeff");
      int var5 = var2 == null ? -1 : var2.indexOf(10);
      this.eol = var5 > 0 && var2.charAt(var5 - 1) == '\r' ? "\r\n" : "\n";
      this.finalNewline = var2 != null && var2.endsWith("\n");
   }

   public static FeatureSource of(byte[] var0) {
      String var1;
      try {
         var1 = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(var0)).toString();
      } catch (CharacterCodingException var4) {
         return new FeatureSource(var0, null, null, "the file is not valid UTF-8");
      }

      if (var1.lines().count() > 32767L) {
         return new FeatureSource(var0, var1, null, "the file has more than 32767 lines");
      } else if (var1.lines().anyMatch(var0x -> var0x.length() > 32767)) {
         return new FeatureSource(var0, var1, null, "a line has more than 32767 chars");
      } else {
         try {
            Feature var2 = Feature.read(Resource.text(var1));
            return new FeatureSource(var0, var1, var2, null);
         } catch (RuntimeException var5) {
            String var3 = var5.getMessage() == null ? var5.getClass().getSimpleName() : var5.getMessage().lines().findFirst().orElse("");
            return new FeatureSource(var0, var1, null, "the feature does not parse: " + var3);
         }
      }
   }

   public static FeatureSource of(String var0) {
      return of(var0.getBytes(StandardCharsets.UTF_8));
   }

   public byte[] bytes() {
      return this.bytes;
   }

   public String text() {
      return this.text;
   }

   public String version() {
      return this.version;
   }

   public Feature feature() {
      return this.feature;
   }

   public boolean editable() {
      return this.notEditable == null;
   }

   public String notEditable() {
      return this.notEditable;
   }

   public boolean bom() {
      return this.bom;
   }

   public String eol() {
      return this.eol;
   }

   public boolean finalNewline() {
      return this.finalNewline;
   }

   public int byteOffset(int var1) {
      if (this.byteAt == null) {
         int[] var2 = new int[this.text.length() + 1];
         int var3 = 0;
         int var4 = 0;

         while (var4 < this.text.length()) {
            int var5 = this.text.codePointAt(var4);
            int var6 = Character.charCount(var5);
            int var7 = var5 < 128 ? 1 : (var5 < 2048 ? 2 : (var5 < 65536 ? 3 : 4));
            var2[var4] = var3;
            if (var6 == 2) {
               var2[var4 + 1] = var3;
            }

            var3 += var7;
            var4 += var6;
         }

         var2[this.text.length()] = var3;
         this.byteAt = var2;
      }

      return this.byteAt[var1];
   }

   int lineStart(int var1) {
      int var2 = this.text.lastIndexOf(10, var1 - 1);
      return var2 < 0 ? 0 : var2 + 1;
   }

   int lineEnd(int var1) {
      int var2 = this.text.indexOf(10, var1);
      if (var2 < 0) {
         return this.text.length();
      } else {
         return var2 > 0 && this.text.charAt(var2 - 1) == 13 ? var2 - 1 : var2;
      }
   }

   int afterLineEnd(int var1) {
      int var2 = this.text.indexOf(10, var1);
      return var2 < 0 ? this.text.length() : var2 + 1;
   }

   String indentAt(int var1) {
      int var2 = this.lineStart(var1);
      int var3 = var2;

      while (var3 < this.text.length() && (this.text.charAt(var3) == ' ' || this.text.charAt(var3) == '\t')) {
         var3++;
      }

      return this.text.substring(var2, var3);
   }

   String stepIndent() {
      var var1 = this.allSteps().iterator();
      if (var1.hasNext()) {
         Step var2 = (Step)var1.next();
         return this.indentAt(var2.getSpan().start());
      } else {
         return "  ";
      }
   }

   String sectionIndent() {
      var var1 = this.feature.getSections().iterator();
      if (var1.hasNext()) {
         FeatureSection var2 = (FeatureSection)var1.next();
         SourceSpan var3 = var2.isOutline() ? var2.getScenarioOutline().getSpan() : var2.getScenario().getSpan();
         return this.indentAt(var3.start());
      } else {
         return "";
      }
   }

   List<Step> allSteps() {
      ArrayList<io.karatelabs.gherkin.Step> var1 = new ArrayList();
      if (this.feature.getBackground() != null && this.feature.getBackground().getSteps() != null) {
         var1.addAll(this.feature.getBackground().getSteps());
      }

      for (FeatureSection var3 : this.feature.getSections()) {
         var1.addAll(var3.isOutline() ? var3.getScenarioOutline().getSteps() : var3.getScenario().getSteps());
      }

      return var1;
   }

   List<Step> stepsOf(Integer var1) {
      if (var1 != null) {
         var var2 = this.feature.getSections();
         if (var1 >= 0 && var1 < var2.size()) {
            FeatureSection var3 = (FeatureSection)var2.get(var1);
            return var3.isOutline() ? var3.getScenarioOutline().getSteps() : var3.getScenario().getSteps();
         } else {
            return null;
         }
      } else {
         return this.feature.getBackground() != null && this.feature.getBackground().getSteps() != null ? this.feature.getBackground().getSteps() : null;
      }
   }

   static String tableProblem(Table var0) {
      var var1 = var0.getRows();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         SourceSpan var3 = var0.getRowSpan(var2);
         if (var3 != null && var3.line() != var3.endLine()) {
            return "row " + var2 + " spans lines " + var3.line() + "-" + var3.endLine();
         }

         if (((List)var1.get(var2)).size() != ((List)var1.get(0)).size()) {
            return "row " + var2 + " has " + ((List)var1.get(var2)).size() + " cells, the header " + ((List)var1.get(0)).size();
         }
      }

      return null;
   }

   static List<SourceSpan> ownedComments(Step var0) {
      var var1 = var0.getCommentSpans();
      int var2 = var0.getSpan().line() - 1;
      int var3 = var1.size();

      for (int var4 = var1.size() - 1; var4 >= 0 && ((SourceSpan)var1.get(var4)).line() == var2; var4--) {
         var3 = var4;
         var2--;
      }

      return var1.subList(var3, var1.size());
   }
}
