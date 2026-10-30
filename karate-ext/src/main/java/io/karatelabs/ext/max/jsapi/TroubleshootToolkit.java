package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.ToolResult;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TroubleshootToolkit implements AgentExtension {
   public static final String SCHEMA = "karate-troubleshoot/v1";
   static final Set<String> VERDICTS = Set.of("sut-bug", "rules-wrong", "bot-misread", "inconclusive");
   private final Path diagnosisFile;
   private final String failedRunId;
   private final String troubleshootRunId;

   public TroubleshootToolkit(Path var1, String var2, String var3) {
      this.diagnosisFile = var1;
      this.failedRunId = var2;
      this.troubleshootRunId = var3;
   }

   public static String preamble() {
      return "You are a QA failure diagnostician. A test run FAILED; your job is to read its artifacts and write a diagnosis — you do NOT have a browser and must not try to drive one.\n\nRespond with exactly ONE ```js block, then STOP — the loop runs it and shows you the result before your next turn. Only the first block executes; plan in a leading // comment, not in prose. Every `var`/function you define persists across turns — save extracted findings as variables, because earlier conversation turns get trimmed but your engine state does not.";
   }

   @Override
   public String promptFragment() {
      return "## Diagnosing the failed run — your task\n\nRead the failed run's artifacts (listed in your task message) with `File.read`, work out WHY it failed, and write your diagnosis to `"
         + this.relPath()
         + "` with `File.write`. Writing that file IS your exit — the run ends the moment a valid diagnosis is on disk, so do not keep investigating after you write it, and do not write it before you have evidence.\n\n**The three-cause triage (settle on ONE):** a failed check has three possible causes, not two —\n- `sut-bug` — the application genuinely misbehaved (it contradicts its own rules/requirements/spec). Cite the observed vs expected behavior.\n- `rules-wrong` — the expectation is stale: the rulebook/assertion/locator encodes an old truth and the app legitimately changed. Name the drifted artifact and line. Two reads settle it: when one deck's rows run against one system and only some fail, the drift is in what a FAIL row reaches and a PASS row does not (a cause shared by every row would have failed them all); when a log gives the observed value beside the expected one, solve their difference for the ONE factor that reproduces the observed number — the answer is a value the table does not hold today, so compute it, never pick it off the list.\n- `bot-misread` — the observation channel failed: the bot misread the page (a mis-resolved locator, a look() taken mid-load, a wrong-element act) and the finding is manufactured. This is the cause a two-cause model turns into false bugs — check it explicitly.\nIf the evidence genuinely cannot distinguish, the honest verdict is `inconclusive` — say what evidence is missing; never manufacture certainty.\n\n**Work the evidence, cheapest first:** your task message already carries the run record, the failing scenario's steps and what each one logged — re-reading the event stream for those returns the same lines and costs you iterations. Open a file only for what the task does not carry (the transcript, one JSON event per line, for a loop run's own actions; screenshots for what the bot saw), and save what you extract into variables. To REPRODUCE a suspected failure deterministically you may re-run a `checks/*.js` check with `Check.run('<name>.js')` — but there is NO browser here, so a UI check will refuse to drive; treat its error as information, not something to fix.\n\n**`diagnosis.md` MUST start with EXACTLY this front-matter block** (copy it verbatim, choosing one verdict; add the `check:` line only when a specific `checks/*.js` is the drifted artifact):\n\n```\n"
         + this.headerTemplate()
         + "```\n\nAfter the closing `---`, write the diagnosis a teammate can act on: the failing step, the evidence for your verdict (quote the transcript/error lines), what you ruled out and why, and the concrete next action (the fix for a drift, the bug report for a SUT bug, the missing evidence for inconclusive). State only what the artifacts show — never invent steps you did not see.\n";
   }

   String headerTemplate() {
      return "---\nschema: karate-troubleshoot/v1\nfailedRunId: "
         + this.failedRunId
         + "\ntroubleshootRunId: "
         + this.troubleshootRunId
         + "\nverdict: sut-bug | rules-wrong | bot-misread | inconclusive\ncheck: checks/<name>.js\n---\n";
   }

   private String relPath() {
      return "runs/" + this.failedRunId + "/diagnosis.md";
   }

   @Override
   public List<io.karatelabs.ext.agent.runtime.ExitCondition> exitConditions() {
      return List.of(var1 -> {
         var var2 = this.readValidDiagnosis();
         return var2 == null ? Optional.empty() : Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.success(var2));
      });
   }

   @Override
   public List<AgentHook> hooks() {
      return List.of(
         new AgentHook() {
            @Override
            public Optional<String> afterTool(AgentContext var1, String var2, ToolResult var3, String var4) {
               if (var2 == null || !var2.contains("diagnosis.md") || !Files.isRegularFile(TroubleshootToolkit.this.diagnosisFile)) {
                  return Optional.empty();
               } else {
                  return TroubleshootToolkit.this.readValidDiagnosis() != null
                     ? Optional.empty()
                     : Optional.of(
                        "your "
                           + TroubleshootToolkit.this.relPath()
                           + " does NOT satisfy the diagnosis contract — it must START with the exact front-matter block from your instructions (schema/failedRunId/troubleshootRunId lines verbatim, ONE verdict from: "
                           + String.join(" | ", TroubleshootToolkit.VERDICTS)
                           + "). Rewrite the file with File.write, header first."
                     );
               }
            }
         }
      );
   }

   Map<String, Object> readValidDiagnosis() {
      if (!Files.isRegularFile(this.diagnosisFile)) {
         return null;
      } else {
         String var1;
         try {
            var1 = Files.readString(this.diagnosisFile);
         } catch (Exception var6) {
            return null;
         }

         var var2 = parseFrontMatter(var1);
         if (var2 != null
            && "karate-troubleshoot/v1".equals(var2.get("schema"))
            && this.failedRunId.equals(var2.get("failedRunId"))
            && this.troubleshootRunId.equals(var2.get("troubleshootRunId"))) {
            String var3 = (String)var2.get("verdict");
            if (var3 != null && VERDICTS.contains(var3)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
               var4.put("verdict", var3);
               String var5 = (String)var2.get("check");
               if (var5 != null && !var5.isBlank() && !var5.contains("<")) {
                  var4.put("check", var5);
               }

               var4.put("path", this.relPath());
               var4.put("diagnosis", var1);
               return var4;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   static Map<String, String> parseFrontMatter(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String[] var1 = var0.split("\n", -1);
         if (var1.length != 0 && var1[0].trim().equals("---")) {
            LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();

            for (int var3 = 1; var3 < var1.length; var3++) {
               String var4 = var1[var3].trim();
               if (var4.equals("---")) {
                  return var2;
               }

               int var5 = var4.indexOf(58);
               if (var5 > 0) {
                  String var6 = var4.substring(0, var5).trim();
                  String var7 = var4.substring(var5 + 1).trim();
                  if ("verdict".equals(var6)) {
                     var7 = var7.toLowerCase(Locale.ROOT);
                  }

                  var2.put(var6, var7);
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }
}
