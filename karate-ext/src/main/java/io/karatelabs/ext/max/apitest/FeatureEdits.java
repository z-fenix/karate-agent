package io.karatelabs.ext.max.apitest;

import io.karatelabs.ext.coverage.graph.TraceGraph;
import io.karatelabs.ext.max.jsapi.FeatureTags;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.SourceSpan;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Table;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public final class FeatureEdits {
   private FeatureEdits() {
   }

   public static EditResult replaceStep(FeatureSource var0, FeatureEdits.StepRef var1, String var2, String var3) {
      EditResult var4 = guard(var0, var3);
      if (var4 != null) {
         return var4;
      } else {
         Step var5 = step(var0, var1);
         if (var5 == null) {
            return noSuchStep(var0, var1);
         } else {
            var var6 = lines(var2);
            if (var6.isEmpty()) {
               return EditResult.refuse("BAD_REQUEST", 400, "replaceStep needs the new step's text", null);
            } else {
               String var7 = var0.indentAt(var5.getSpan().start());
               var var8 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(String.join("\n", reindent(var6, var7)));
               if (var8 != null && var8.size() == 1) {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var9 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
                  io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var10 = var9.a(var1.scenarioIndex()).get(var1.stepIndex());
                  boolean var11 = !((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(0)).h().isEmpty();
                  List var12 = var11 ? ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(0)).h() : var10.h();
                  var var13 = new ArrayList<>(var9.a(var1.scenarioIndex()));
                  var13.set(var1.stepIndex(), ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(0)).a(var12));
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var14 = var9.a(var1.scenarioIndex(), var13);
                  String var15 = var5.getSpan().text(var0.text());
                  boolean var16 = io.karatelabs.ext.max.apitest.StepClassifier.a(var10) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED
                     || !io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var10, var15)
                     || io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var8.get(0)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED;
                  io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var17 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(
                     var11 ? firstOwnedStart(var5) : var5.getSpan().start(), var5.getSpan().end(), String.join(var0.eol(), reindentButFirst(var6, var7))
                  );
                  return commit(
                     var0, "replaceStep", var1.scenarioIndex(), var17, var14, var15, var16, var1x -> stepSpan(var1x, var1.scenarioIndex(), var1.stepIndex())
                  );
               } else {
                  return notOneStep("replaceStep", var2);
               }
            }
         }
      }
   }

   public static EditResult insertStep(FeatureSource var0, Integer var1, int var2, String var3, String var4) {
      EditResult var5 = guard(var0, var4);
      if (var5 != null) {
         return var5;
      } else {
         var var6 = var0.stepsOf(var1);
         if (var6 == null) {
            return noSuchSection(var0, var1);
         } else if (var2 >= 0 && var2 <= var6.size()) {
            var var7 = lines(var3);
            if (var7.isEmpty()) {
               return EditResult.refuse("BAD_REQUEST", 400, "insertStep needs the new step's text", null);
            } else {
               Step var8 = var6.isEmpty() ? null : (Step)var6.get(Math.max(0, var2 - 1));
               String var9 = var8 == null ? var0.stepIndent() : var0.indentAt(var8.getSpan().start());
               var var10 = reindent(var7, var9);
               var var11 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(String.join("\n", var10));
               if (var11 != null && var11.size() == 1) {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var12 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
                  if (io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var11.get(0)) != io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT) {
                     EditResult var13 = insideCall(var12.a(var1), var2);
                     if (var13 != null) {
                        return var13;
                     }
                  }

                  var var17 = new ArrayList<>(var12.a(var1));
                  var17.add(var2, (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var11.get(0));
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var14 = var12.a(var1, var17);
                  io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var15;
                  if (var2 == 0 && !var6.isEmpty()) {
                     int var18 = var0.lineStart(firstOwnedStart((Step)var6.get(0)));
                     var15 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var18, var18, block(var10, var0.eol()));
                  } else {
                     int var16 = var8 == null ? sectionKeywordEnd(var0, var1) : var8.getSpan().end();
                     var15 = insertAfterLine(var0, var16, var10);
                  }

                  boolean var19 = io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var11.get(0))
                     == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED;
                  return commit(var0, "insertStep", var1, var15, var14, null, var19, var2x -> stepSpan(var2x, var1, var2));
               } else {
                  return notOneStep("insertStep", var3);
               }
            }
         } else {
            return EditResult.refuse("NOT_FOUND", 404, "insertStep: index " + var2 + " is outside 0.." + var6.size() + " for " + sectionLabel(var0, var1), null);
         }
      }
   }

   private static EditResult insideCall(List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var0, int var1) {
      if (var1 > 0
         && var1 < var0.size()
         && io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var1 - 1)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT) {
         int var2 = var1;

         while (
            var2 < var0.size()
               && io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var2)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT
         ) {
            var2++;
         }

         if (var2 != var0.size()
            && io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var2)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD) {
            int var3 = var1 - 1;

            while (
               var3 > 0
                  && io.karatelabs.ext.max.apitest.StepClassifier.a((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var3 - 1)) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT
            ) {
               var3--;
            }

            ArrayList var4 = new ArrayList();
            String var5 = "";

            for (int var6 = var3; var6 < var2; var6++) {
               io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var7 = (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var6);
               String var8 = var7.e() == null ? "" : var7.e().strip();
               if ("path".equals(var7.d())) {
                  var4.add(var8);
               } else if ("url".equals(var7.d())) {
                  var5 = var8;
               }
            }

            String var9 = ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var2)).e() == null
               ? ""
               : ((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var0.get(var2)).e().strip().toUpperCase();
            String var10 = (var9 + " " + (var4.isEmpty() ? var5 : String.join(", ", var4))).strip();
            return EditResult.refuse(
               "BAD_REQUEST",
               400,
               "insertStep: at "
                  + var1
                  + " lands inside the "
                  + var10
                  + " call (steps "
                  + var3
                  + ".."
                  + var2
                  + "), where only a request step goes; insert after its method step: at "
                  + (var2 + 1)
                  + ", or before the call: at "
                  + var3,
               null
            );
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static EditResult deleteStep(FeatureSource var0, FeatureEdits.StepRef var1, String var2) {
      EditResult var3 = guard(var0, var2);
      if (var3 != null) {
         return var3;
      } else {
         Step var4 = step(var0, var1);
         if (var4 == null) {
            return noSuchStep(var0, var1);
         } else {
            io.karatelabs.ext.max.apitest.FeatureSnapshot var5 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
            io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var6 = var5.a(var1.scenarioIndex()).get(var1.stepIndex());
            var var7 = new ArrayList<>(var5.a(var1.scenarioIndex()));
            var7.remove(var1.stepIndex());
            io.karatelabs.ext.max.apitest.FeatureSnapshot var8 = var5.a(var1.scenarioIndex(), var7);
            String var9 = var4.getSpan().text(var0.text());
            boolean var10 = io.karatelabs.ext.max.apitest.StepClassifier.a(var6) == io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED
               || !io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var6, var9);
            io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var11 = deleteLines(var0, var0.lineStart(firstOwnedStart(var4)), var4.getSpan().end());
            return commit(var0, "deleteStep", var1.scenarioIndex(), var11, var8, var9, var10, var0x -> null);
         }
      }
   }

   public static EditResult insertComment(FeatureSource var0, FeatureEdits.StepRef var1, String var2, String var3) {
      EditResult var4 = guard(var0, var3);
      if (var4 != null) {
         return var4;
      } else {
         Step var5 = step(var0, var1);
         if (var5 == null) {
            return noSuchStep(var0, var1);
         } else {
            var var6 = commentLines(var2);
            if (var6.isEmpty()) {
               return EditResult.refuse("BAD_REQUEST", 400, "insertComment needs the comment's text", null);
            } else {
               String var7 = var0.indentAt(var5.getSpan().start());
               io.karatelabs.ext.max.apitest.FeatureSnapshot var8 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
               io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var9 = var8.a(var1.scenarioIndex()).get(var1.stepIndex());
               var var10 = new ArrayList<>(var9.h());
               var10.addAll(var6);
               var var11 = new ArrayList<>(var8.a(var1.scenarioIndex()));
               var11.set(var1.stepIndex(), var9.a(var10));
               int var12 = var0.lineStart(var5.getSpan().start());
               io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var13 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var12, var12, block(reindent(var6, var7), var0.eol()));
               return commit(
                  var0,
                  "insertComment",
                  var1.scenarioIndex(),
                  var13,
                  var8.a(var1.scenarioIndex(), var11),
                  null,
                  false,
                  var1x -> stepSpan(var1x, var1.scenarioIndex(), var1.stepIndex())
               );
            }
         }
      }
   }

   public static EditResult replaceComment(FeatureSource var0, FeatureEdits.StepRef var1, int var2, String var3, String var4) {
      EditResult var5 = guard(var0, var4);
      if (var5 != null) {
         return var5;
      } else {
         Step var6 = step(var0, var1);
         if (var6 == null) {
            return noSuchStep(var0, var1);
         } else {
            var var7 = FeatureSource.ownedComments(var6);
            if (var2 >= 0 && var2 < var7.size()) {
               var var8 = commentLines(var3);
               if (var8.size() != 1) {
                  return EditResult.refuse("BAD_REQUEST", 400, "replaceComment takes one comment line", null);
               } else {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var9 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
                  io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var10 = var9.a(var1.scenarioIndex()).get(var1.stepIndex());
                  var var11 = new ArrayList<>(var10.h());
                  var11.set(var2, (String)var8.get(0));
                  var var12 = new ArrayList<>(var9.a(var1.scenarioIndex()));
                  var12.set(var1.stepIndex(), var10.a(var11));
                  SourceSpan var13 = (SourceSpan)var7.get(var2);
                  io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var14 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var13.start(), var13.end(), (String)var8.get(0));
                  return commit(
                     var0,
                     "replaceComment",
                     var1.scenarioIndex(),
                     var14,
                     var9.a(var1.scenarioIndex(), var12),
                     var13.text(var0.text()),
                     false,
                     var1x -> stepSpan(var1x, var1.scenarioIndex(), var1.stepIndex())
                  );
               }
            } else {
               return noSuchComment(var1, var2, var7.size());
            }
         }
      }
   }

   public static EditResult deleteComment(FeatureSource var0, FeatureEdits.StepRef var1, int var2, String var3) {
      EditResult var4 = guard(var0, var3);
      if (var4 != null) {
         return var4;
      } else {
         Step var5 = step(var0, var1);
         if (var5 == null) {
            return noSuchStep(var0, var1);
         } else {
            var var6 = FeatureSource.ownedComments(var5);
            if (var2 >= 0 && var2 < var6.size()) {
               io.karatelabs.ext.max.apitest.FeatureSnapshot var7 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
               io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var8 = var7.a(var1.scenarioIndex()).get(var1.stepIndex());
               var var9 = new ArrayList<>(var8.h());
               var9.remove(var2);
               var var10 = new ArrayList<>(var7.a(var1.scenarioIndex()));
               var10.set(var1.stepIndex(), var8.a(var9));
               SourceSpan var11 = (SourceSpan)var6.get(var2);
               io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var12 = deleteLines(var0, var0.lineStart(var11.start()), var11.end());
               return commit(
                  var0,
                  "deleteComment",
                  var1.scenarioIndex(),
                  var12,
                  var7.a(var1.scenarioIndex(), var10),
                  var11.text(var0.text()),
                  false,
                  var1x -> stepSpan(var1x, var1.scenarioIndex(), var1.stepIndex())
               );
            } else {
               return noSuchComment(var1, var2, var6.size());
            }
         }
      }
   }

   public static EditResult replaceExamplesRow(FeatureSource var0, FeatureEdits.RowRef var1, List<String> var2, String var3) {
      EditResult var4 = guard(var0, var3);
      if (var4 != null) {
         return var4;
      } else {
         Table var5 = table(var0, var1.scenarioIndex(), var1.block());
         if (var5 == null) {
            return noSuchBlock(var0, var1.scenarioIndex(), var1.block());
         } else if (FeatureSource.tableProblem(var5) != null) {
            return lockedTable("replaceExamplesRow", var5);
         } else if (var1.row() >= 0 && var1.row() < var5.getRows().size()) {
            if (var2 != null && var2.size() == var5.getRows().get(0).size()) {
               io.karatelabs.ext.max.apitest.FeatureSnapshot var6 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
               var var7 = new ArrayList<>(var6.e().get(var1.scenarioIndex()).g().get(var1.block()).c());
               var7.set(var1.row(), stripped(var2));
               io.karatelabs.ext.max.apitest.FeatureSnapshot var8 = var6.a(var1.scenarioIndex(), var6.e().get(var1.scenarioIndex()).a(var1.block(), var7));
               SourceSpan var9 = var5.getRowSpan(var1.row());
               io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var10 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var9.start(), var9.end(), rowText(var2));
               return commit(
                  var0,
                  "replaceExamplesRow",
                  var1.scenarioIndex(),
                  var10,
                  var8,
                  var9.text(var0.text()),
                  false,
                  var1x -> rowSpan(var1x, var1.scenarioIndex(), var1.block(), var1.row())
               );
            } else {
               return EditResult.refuse(
                  "BAD_REQUEST",
                  400,
                  "replaceExamplesRow: the row needs " + var5.getRows().get(0).size() + " cells, as the header " + var5.getRows().get(0),
                  null
               );
            }
         } else {
            return noSuchRow(var1, var5);
         }
      }
   }

   public static EditResult insertExamplesRow(FeatureSource var0, int var1, int var2, int var3, List<String> var4, String var5) {
      EditResult var6 = guard(var0, var5);
      if (var6 != null) {
         return var6;
      } else {
         Table var7 = table(var0, var1, var2);
         if (var7 == null) {
            return noSuchBlock(var0, var1, var2);
         } else if (FeatureSource.tableProblem(var7) != null) {
            return lockedTable("insertExamplesRow", var7);
         } else {
            int var8 = var7.getRows().size();
            if (var3 >= 1 && var3 <= var8) {
               if (var4 != null && var4.size() == var7.getRows().get(0).size()) {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var9 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
                  var var10 = new ArrayList<>(var9.e().get(var1).g().get(var2).c());
                  var10.add(var3, stripped(var4));
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var11 = var9.a(var1, var9.e().get(var1).a(var2, var10));
                  SourceSpan var12 = var7.getRowSpan(var3 - 1);
                  String var13 = var0.indentAt(var12.start());
                  io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var14 = insertAfterLine(var0, var12.end(), List.of(var13 + rowText(var4)));
                  return commit(var0, "insertExamplesRow", var1, var14, var11, null, false, var3x -> rowSpan(var3x, var1, var2, var3));
               } else {
                  return EditResult.refuse(
                     "BAD_REQUEST",
                     400,
                     "insertExamplesRow: the row needs " + var7.getRows().get(0).size() + " cells, as the header " + var7.getRows().get(0),
                     null
                  );
               }
            } else {
               return EditResult.refuse("NOT_FOUND", 404, "insertExamplesRow: row " + var3 + " is outside 1.." + var8 + " (row 0 is the header)", null);
            }
         }
      }
   }

   public static EditResult deleteExamplesRow(FeatureSource var0, FeatureEdits.RowRef var1, String var2) {
      EditResult var3 = guard(var0, var2);
      if (var3 != null) {
         return var3;
      } else {
         Table var4 = table(var0, var1.scenarioIndex(), var1.block());
         if (var4 == null) {
            return noSuchBlock(var0, var1.scenarioIndex(), var1.block());
         } else if (FeatureSource.tableProblem(var4) != null) {
            return lockedTable("deleteExamplesRow", var4);
         } else if (var1.row() >= 1 && var1.row() < var4.getRows().size()) {
            io.karatelabs.ext.max.apitest.FeatureSnapshot var5 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
            var var6 = new ArrayList<>(var5.e().get(var1.scenarioIndex()).g().get(var1.block()).c());
            var6.remove(var1.row());
            io.karatelabs.ext.max.apitest.FeatureSnapshot var7 = var5.a(var1.scenarioIndex(), var5.e().get(var1.scenarioIndex()).a(var1.block(), var6));
            SourceSpan var8 = var4.getRowSpan(var1.row());
            io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var9 = deleteLines(var0, var0.lineStart(var8.start()), var8.end());
            return commit(var0, "deleteExamplesRow", var1.scenarioIndex(), var9, var7, var8.text(var0.text()), false, var0x -> null);
         } else {
            return var1.row() == 0 ? EditResult.refuse("BAD_REQUEST", 400, "deleteExamplesRow: row 0 is the header — it stays", null) : noSuchRow(var1, var4);
         }
      }
   }

   public static EditResult createScenario(FeatureSource var0, String var1, List<String> var2, List<String> var3, String var4) {
      EditResult var5 = guard(var0, var4);
      if (var5 != null) {
         return var5;
      } else {
         String var6 = badTag(var2);
         if (var6 != null) {
            return EditResult.refuse("BAD_REQUEST", 400, "createScenario refuses " + var6, null);
         } else if (var3 != null && !var3.isEmpty()) {
            String var7 = var0.stepIndent();
            ArrayList var8 = new ArrayList();

            for (String var10 : var3) {
               var8.addAll(reindent(lines(var10), var7));
            }

            var var19 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(String.join("\n", var8));
            if (var19 != null && var19.size() == var3.size()) {
               String var20 = var0.sectionIndent();
               ArrayList var11 = new ArrayList();
               if (var2 != null && !var2.isEmpty()) {
                  var11.add(var20 + String.join(" ", var2));
               }

               var11.add(var20 + "Scenario:" + (var1 != null && !var1.isBlank() ? " " + var1.strip() : ""));
               var11.addAll(var8);
               io.karatelabs.ext.max.apitest.FeatureSnapshot var12 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
               io.karatelabs.ext.max.apitest.FeatureSnapshot var13 = var12.a(
                  new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(
                     false, var2 == null ? List.of() : List.copyOf(var2), var1 != null && !var1.isBlank() ? var1.strip() : null, null, var19, List.of()
                  )
               );
               String var14 = var0.text();
               String var15 = var0.eol();
               String var16 = var0.finalNewline() ? var15 + block(var11, var15) : var15 + var15 + String.join(var15, var11);
               io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var17 = new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var14.length(), var14.length(), var16);
               int var18 = var0.feature().getSections().size();
               return commit(var0, "createScenario", var18, var17, var13, null, false, var1x -> {
                  FeatureSection var2x = var1x.feature().getSections().get(var18);
                  return Cards.Span.of(var1x, var2x.isOutline() ? var2x.getScenarioOutline().getSpan() : var2x.getScenario().getSpan());
               });
            } else {
               return EditResult.refuse("BAD_REQUEST", 400, stepsShape("createScenario", var3.size(), var19), null);
            }
         } else {
            return EditResult.refuse("BAD_REQUEST", 400, "createScenario needs at least one step", null);
         }
      }
   }

   static String stepsShape(String var0, int var1, List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var2) {
      return var0
         + ": steps is an array of strings, one step each, and a `# Title` comment is the first line of the step it titles, never a string of its own: ['# Sign in\\n* url baseUrl', \"* path 'login'\", '* method post']. The "
         + var1
         + (var1 == 1 ? " string reads" : " strings read")
         + " as "
         + (var2 == null ? "no steps" : var2.size() + (var2.size() == 1 ? " step" : " steps"))
         + ".";
   }

   public static EditResult createFeature(String var0, List<String> var1, String var2, List<String> var3, List<String> var4) {
      String var5 = badTag(var1);
      if (var5 == null) {
         var5 = badTag(var3);
      }

      if (var5 != null) {
         return EditResult.refuse("BAD_REQUEST", 400, "createFeature refuses " + var5, null);
      } else if (var4 != null && !var4.isEmpty()) {
         ArrayList var6 = new ArrayList();

         for (String var8 : var4) {
            var6.addAll(reindent(lines(var8), "  "));
         }

         var var13 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(String.join("\n", var6));
         if (var13 != null && var13.size() == var4.size()) {
            ArrayList var14 = new ArrayList();
            if (var1 != null && !var1.isEmpty()) {
               var14.add(String.join(" ", var1));
            }

            var14.add("Feature:" + (var0 != null && !var0.isBlank() ? " " + var0.strip() : ""));
            var14.add("");
            if (var3 != null && !var3.isEmpty()) {
               var14.add(String.join(" ", var3));
            }

            var14.add("Scenario:" + (var2 != null && !var2.isBlank() ? " " + var2.strip() : ""));
            var14.addAll(var6);
            FeatureSource var9 = FeatureSource.of(block(var14, "\n"));
            if (!var9.editable()) {
               return EditResult.refuse("UNINTENDED_CHANGE", 409, "createFeature wrote nothing — " + var9.notEditable(), null);
            } else {
               io.karatelabs.ext.max.apitest.FeatureSnapshot var10 = new io.karatelabs.ext.max.apitest.FeatureSnapshot(
                  var1 == null ? List.of() : List.copyOf(var1),
                  var0 != null && !var0.isBlank() ? var0.strip() : null,
                  null,
                  null,
                  List.of(
                     new io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot(
                        false, var3 == null ? List.of() : List.copyOf(var3), var2 != null && !var2.isBlank() ? var2.strip() : null, null, var13, List.of()
                     )
                  ),
                  List.of()
               );
               String var11 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var10, io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var9));
               if (var11 != null) {
                  return EditResult.refuse("UNINTENDED_CHANGE", 409, "createFeature wrote nothing — " + var11, null);
               } else {
                  EditResult.Change var12 = new EditResult.Change(
                     "createFeature",
                     0,
                     var10.e().get(0).d(),
                     false,
                     List.of(0),
                     null,
                     var9.text(),
                     Cards.Span.of(var9, var9.feature().getSections().get(0).getScenario().getSpan()),
                     false
                  );
                  return new EditResult(null, var9.bytes(), var9.version(), null, var12);
               }
            }
         } else {
            return EditResult.refuse("BAD_REQUEST", 400, stepsShape("createFeature", var4.size(), var13), null);
         }
      } else {
         return EditResult.refuse("BAD_REQUEST", 400, "createFeature needs at least one step", null);
      }
   }

   public static String callonceStep(String var0, String var1) {
      return "* def " + var0 + " = callonce read('" + var1 + "')";
   }

   public static EditResult tag(FeatureSource var0, Integer var1, List<String> var2, List<String> var3, String var4) {
      EditResult var5 = guard(var0, var4);
      if (var5 != null) {
         return var5;
      } else {
         List var6 = var2 == null ? List.of() : var2;
         List var7 = var3 == null ? List.of() : var3;
         if (var6.isEmpty() && var7.isEmpty()) {
            return EditResult.refuse("BAD_REQUEST", 400, "tag: nothing to do — pass tags to add and/or remove", null);
         } else {
            Integer var8 = null;
            if (var1 != null) {
               var var9 = var0.feature().getSections();
               if (var1 < 0 || var1 >= var9.size()) {
                  return noSuchSection(var0, var1);
               }

               FeatureSection var10 = (FeatureSection)var9.get(var1);
               var8 = var10.isOutline() ? var10.getScenarioOutline().getLine() : var10.getScenario().getLine();
            }

            FeatureTags.Edit var19 = FeatureTags.apply(var0.text(), var8, var1 == null, var6, var7);
            if (var19.error() != null) {
               return EditResult.refuse(var19.error());
            } else {
               FeatureSource var20 = FeatureSource.of(var19.content());
               if (!var20.editable()) {
                  return EditResult.refuse("UNINTENDED_CHANGE", 409, "tag wrote nothing — " + var20.notEditable(), null);
               } else {
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var11 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var0);
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var12 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var20);
                  io.karatelabs.ext.max.apitest.FeatureSnapshot var13 = var1 == null
                     ? new io.karatelabs.ext.max.apitest.FeatureSnapshot(var12.a(), var11.b(), var11.c(), var11.d(), var11.e(), var11.f())
                     : var11.a(var1.intValue(), var11.e().get(var1).b(var12.e().get(var1).c()));
                  String var14 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var13, var12);
                  if (var14 != null) {
                     return EditResult.refuse("UNINTENDED_CHANGE", 409, "tag wrote nothing — " + var14, null);
                  } else if (var20.bom() == var0.bom() && var20.finalNewline() == var0.finalNewline()) {
                     String var15 = var7.isEmpty() ? "link" : (var6.isEmpty() ? "unlink" : "tag");
                     Cards.Span var16 = null;
                     if (var1 != null) {
                        FeatureSection var17 = var20.feature().getSections().get(var1);
                        SourceSpan var18 = var17.isOutline() ? var17.getScenarioOutline().getSpan() : var17.getScenario().getSpan();
                        var16 = Cards.Span.of(var20, new SourceSpan(var18.start(), var20.lineEnd(var18.start()), var18.line(), var18.line()));
                     }

                     EditResult.Change var21 = new EditResult.Change(
                        var15,
                        var1,
                        var19.scenario(),
                        false,
                        var1 == null ? allSections(var0) : List.of(var1),
                        String.join(" ", var1 == null ? var11.a() : var11.e().get(var1).c()),
                        String.join(" ", var19.tags()),
                        var16,
                        false
                     );
                     return new EditResult(null, var20.bytes(), var20.version(), var0.version(), var21);
                  } else {
                     return EditResult.refuse("UNINTENDED_CHANGE", 409, "tag wrote nothing — the BOM or final newline would change", null);
                  }
               }
            }
         }
      }
   }

   private static EditResult commit(
      FeatureSource var0, String var1, Integer var2, io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement var3, io.karatelabs.ext.max.apitest.FeatureSnapshot var4, String var5, boolean var6, io.karatelabs.ext.max.apitest.FeatureEdits.TargetLocator var7
   ) {
      String var8 = var0.text();
      String var9 = var8.substring(0, var3.a()) + var3.c() + var8.substring(var3.b());
      byte[] var10 = var9.getBytes(StandardCharsets.UTF_8);
      int var11 = var0.byteOffset(var3.a());
      int var12 = var0.bytes().length - var0.byteOffset(var3.b());
      if (var10.length >= var11 + var12
         && Arrays.equals(var0.bytes(), 0, var11, var10, 0, var11)
         && Arrays.equals(var0.bytes(), var0.bytes().length - var12, var0.bytes().length, var10, var10.length - var12, var10.length)) {
         FeatureSource var13 = FeatureSource.of(var10);
         if (!var13.editable()) {
            return EditResult.refuse("UNINTENDED_CHANGE", 409, var1 + " wrote nothing — " + var13.notEditable(), null);
         } else if (var13.bom() == var0.bom() && var13.finalNewline() == var0.finalNewline()) {
            String var14 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var4, io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var13));
            if (var14 != null) {
               return EditResult.refuse("UNINTENDED_CHANGE", 409, var1 + " wrote nothing — the reparse is not the intended change: " + var14, null);
            } else {
               Cards.Span var15 = var7.locate(var13);
               String var16 = var15 == null ? null : var13.text().substring(var15.charStart(), var15.charEnd());
               boolean var17 = var2 == null;
               String var18 = !var17 && var2 < var13.feature().getSections().size() ? sectionName(var13, var2) : null;
               EditResult.Change var19 = new EditResult.Change(var1, var2, var18, var17, var17 ? allSections(var0) : List.of(var2), var5, var16, var15, var6);
               return new EditResult(null, var10, var13.version(), var0.version(), var19);
            }
         } else {
            return EditResult.refuse("UNINTENDED_CHANGE", 409, var1 + " wrote nothing — the BOM or final newline would change", null);
         }
      } else {
         return EditResult.refuse("UNINTENDED_CHANGE", 409, var1 + " wrote nothing — bytes outside the edited span would change", null);
      }
   }

   private static EditResult guard(FeatureSource var0, String var1) {
      if (var1 == null || var1.isBlank()) {
         return EditResult.refuse("BAD_REQUEST", 400, "every edit carries ifMatch — the version the read answered", null);
      } else if (!var1.equals(var0.version())) {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("expectedVersion", var1);
         var2.put("currentVersion", var0.version());
         return EditResult.refuse("STALE", 409, "the feature changed since it was read — read it again and re-apply the edit", var2);
      } else {
         return !var0.editable() ? EditResult.refuse("NOT_EDITABLE", 409, "the feature is not editable: " + var0.notEditable(), null) : null;
      }
   }

   private static Step step(FeatureSource var0, FeatureEdits.StepRef var1) {
      var var2 = var0.stepsOf(var1.scenarioIndex());
      return var2 != null && var1.stepIndex() >= 0 && var1.stepIndex() < var2.size() ? (Step)var2.get(var1.stepIndex()) : null;
   }

   private static Table table(FeatureSource var0, int var1, int var2) {
      var var3 = var0.feature().getSections();
      if (var1 >= 0 && var1 < var3.size() && ((FeatureSection)var3.get(var1)).isOutline()) {
         var var4 = ((FeatureSection)var3.get(var1)).getScenarioOutline().getExamplesTables();
         return var2 >= 0 && var2 < var4.size() && ((ExamplesTable)var4.get(var2)).getTable() != null ? ((ExamplesTable)var4.get(var2)).getTable() : null;
      } else {
         return null;
      }
   }

   private static EditResult noSuchStep(FeatureSource var0, FeatureEdits.StepRef var1) {
      var var2 = var0.stepsOf(var1.scenarioIndex());
      return var2 == null
         ? noSuchSection(var0, var1.scenarioIndex())
         : EditResult.refuse(
            "NOT_FOUND", 404, "no step " + var1.stepIndex() + " in " + sectionLabel(var0, var1.scenarioIndex()) + " — it has " + var2.size() + " steps", null
         );
   }

   private static EditResult noSuchSection(FeatureSource var0, Integer var1) {
      return var1 == null
         ? EditResult.refuse("NOT_FOUND", 404, "the feature has no Background", null)
         : EditResult.refuse("NOT_FOUND", 404, "no scenario " + var1 + " — the feature has " + var0.feature().getSections().size(), null);
   }

   private static EditResult noSuchBlock(FeatureSource var0, int var1, int var2) {
      var var3 = var0.feature().getSections();
      if (var1 < 0 || var1 >= var3.size()) {
         return noSuchSection(var0, var1);
      } else {
         return !((FeatureSection)var3.get(var1)).isOutline()
            ? EditResult.refuse("NOT_FOUND", 404, sectionLabel(var0, var1) + " is not an outline — it has no Examples", null)
            : EditResult.refuse(
               "NOT_FOUND",
               404,
               "no Examples block "
                  + var2
                  + " in "
                  + sectionLabel(var0, var1)
                  + " — it has "
                  + ((FeatureSection)var3.get(var1)).getScenarioOutline().getExamplesTables().size(),
               null
            );
      }
   }

   private static EditResult lockedTable(String var0, Table var1) {
      return EditResult.refuse(
         "NOT_EDITABLE", 409, var0 + ": the table is not editable by row — " + FeatureSource.tableProblem(var1) + "; replace the whole step instead", null
      );
   }

   private static EditResult noSuchRow(FeatureEdits.RowRef var0, Table var1) {
      return EditResult.refuse(
         "NOT_FOUND",
         404,
         "no row " + var0.row() + " in Examples block " + var0.block() + " — it has rows 0.." + (var1.getRows().size() - 1) + " (0 is the header)",
         null
      );
   }

   private static EditResult noSuchComment(FeatureEdits.StepRef var0, int var1, int var2) {
      return EditResult.refuse(
         "NOT_FOUND", 404, "step " + var0.stepIndex() + " owns " + var2 + " comment" + (var2 == 1 ? "" : "s") + " — no comment " + var1, null
      );
   }

   private static EditResult notOneStep(String var0, String var1) {
      return EditResult.refuse(
         "BAD_REQUEST",
         400,
         var0
            + ": the text does not read as exactly one step (a comment on the line above it is fine; a blank line, a second step or a broken docstring is not): "
            + var1.strip().lines().findFirst().orElse(""),
         null
      );
   }

   private static String sectionLabel(FeatureSource var0, Integer var1) {
      if (var1 == null) {
         return "the Background";
      } else {
         String var2 = sectionName(var0, var1);
         return "scenario " + var1 + (var2 == null ? "" : " '" + var2 + "'");
      }
   }

   private static String sectionName(FeatureSource var0, int var1) {
      FeatureSection var2 = var0.feature().getSections().get(var1);
      return var2.isOutline() ? var2.getScenarioOutline().getName() : var2.getScenario().getName();
   }

   private static int sectionKeywordEnd(FeatureSource var0, Integer var1) {
      SourceSpan var2;
      if (var1 == null) {
         var2 = var0.feature().getBackground().getSpan();
      } else {
         FeatureSection var3 = var0.feature().getSections().get(var1);
         var2 = var3.isOutline() ? var3.getScenarioOutline().getSpan() : var3.getScenario().getSpan();
      }

      return var0.lineEnd(var2.start());
   }

   private static List<Integer> allSections(FeatureSource var0) {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < var0.feature().getSections().size(); var2++) {
         var1.add(var2);
      }

      return var1;
   }

   private static String badTag(List<String> var0) {
      for (String var2 : (Iterable<String>) (Iterable<?>) (var0 == null ? List.of() : var0)) {
         String var3 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var2);
         if (var3 != null) {
            return var3;
         }
      }

      return null;
   }

   private static Cards.Span stepSpan(FeatureSource var0, Integer var1, int var2) {
      var var3 = var0.stepsOf(var1);
      return var3 != null && var2 < var3.size() ? Cards.Span.of(var0, ((Step)var3.get(var2)).getSpan()) : null;
   }

   private static Cards.Span rowSpan(FeatureSource var0, int var1, int var2, int var3) {
      Table var4 = table(var0, var1, var2);
      return var4 != null && var3 < var4.getRows().size() ? Cards.Span.of(var0, var4.getRowSpan(var3)) : null;
   }

   private static int firstOwnedStart(Step var0) {
      var var1 = FeatureSource.ownedComments(var0);
      return var1.isEmpty() ? var0.getSpan().start() : ((SourceSpan)var1.get(0)).start();
   }

   private static io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement deleteLines(FeatureSource var0, int var1, int var2) {
      int var3 = var0.afterLineEnd(var2);
      int var4 = var1;
      if (var3 == var0.text().length() && !var0.finalNewline() && var1 > 0) {
         var4 = var0.text().charAt(var1 - 1) == '\n' && var1 >= 2 && var0.text().charAt(var1 - 2) == '\r' ? var1 - 2 : var1 - 1;
      }

      return new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var4, var3, "");
   }

   private static io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement insertAfterLine(FeatureSource var0, int var1, List<String> var2) {
      int var3 = var0.afterLineEnd(var1);
      String var4 = var0.eol();
      return var3 == var0.text().length() && !var0.finalNewline()
         ? new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var3, var3, var4 + String.join(var4, var2))
         : new io.karatelabs.ext.max.apitest.FeatureEdits.TextReplacement(var3, var3, block(var2, var4));
   }

   private static String block(List<String> var0, String var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var0) {
         var2.append(var4).append(var1);
      }

      return var2.toString();
   }

   private static List<String> lines(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         for (String var5 : var0.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            var1.add(var5);
         }

         while (!var1.isEmpty() && ((String)var1.get(0)).isBlank()) {
            var1.remove(0);
         }

         while (!var1.isEmpty() && ((String)var1.get(var1.size() - 1)).isBlank()) {
            var1.remove(var1.size() - 1);
         }

         return var1;
      }
   }

   private static List<String> commentLines(String var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : lines(var0)) {
         String var4 = var3.strip();
         var1.add(var4.startsWith("#") ? var4 : "# " + var4);
      }

      return var1;
   }

   private static List<String> reindent(List<String> var0, String var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : reindentButFirst(var0, var1)) {
         var2.add(var2.isEmpty() ? var1 + var4 : var4);
      }

      return var2;
   }

   private static List<String> reindentButFirst(List<String> var0, String var1) {
      ArrayList var2 = new ArrayList();
      int var3 = 0;

      while (var3 < var0.size() - 1 && ((String)var0.get(var3)).stripLeading().startsWith("#")) {
         var3++;
      }

      int var4 = Integer.MAX_VALUE;

      for (int var5 = var3 + 1; var5 < var0.size(); var5++) {
         String var6 = (String)var0.get(var5);
         if (!var6.isBlank()) {
            var4 = Math.min(var4, var6.length() - var6.stripLeading().length());
         }
      }

      boolean var8 = var4 >= var1.length();

      for (int var9 = 0; var9 < var0.size(); var9++) {
         String var7 = (String)var0.get(var9);
         if (var9 == 0) {
            var2.add(var7.stripLeading());
         } else if (var9 <= var3) {
            var2.add(var1 + var7.stripLeading());
         } else if (var8) {
            var2.add(var7);
         } else {
            var2.add(var7.isBlank() ? "" : var1 + "  " + var7.substring(var4));
         }
      }

      return var2;
   }

   private static String rowText(List<String> var0) {
      StringBuilder var1 = new StringBuilder("|");

      for (String var3 : var0) {
         var1.append(' ').append(var3 == null ? "" : var3.strip().replace("|", "\\|")).append(" |");
      }

      return var1.toString();
   }

   private static List<String> stripped(List<String> var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0) {
         var1.add(var3 == null ? "" : var3.strip());
      }

      return var1;
   }

   public record RowRef(int scenarioIndex, int block, int row) {

   }

   public record StepRef(Integer scenarioIndex, int stepIndex) {

   }

   private interface TargetLocator {
      Cards.Span locate(FeatureSource var1);
   }

   private record TextReplacement(int a, int b, String c) {

   }
}
