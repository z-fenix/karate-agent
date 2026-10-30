package io.karatelabs.ext.max.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PowerGenerator {
   private static final String DEFAULT_SKILLS = "src/main/resources/META-INF/karate/skills/index.json";
   private static final String DEFAULT_HELP = "src/main/resources/META-INF/karate/help/pages/manifest.json";
   private static final String DEFAULT_OUT = "../etc/power";
   private static final String BUNDLE = "karate";
   private static final String REPO_URL = "https://github.com/karatelabs/karate-agent-examples";
   private static final String REPO_BRANCH = "main";
   private static final String PATH_IN_REPO = "powers/karate";
   private static final String ICON = "karate-icon-512.png";
   private static final String LICENSE = "MIT";
   private static final String SAMPLE_REPORT = "https://karatelabs.github.io/karate-agent-examples/traceability-demo/";
   private static final String PRODUCT_PAGE = "https://karatelabs.io/agent";
   private static final String EULA = "https://karatelabs.io/eula";
   private static final String PRIVACY = "https://karatelabs.io/privacy-policy";
   private static final String NAME = "karate";
   private static final String DISPLAY_NAME = "Karate — deterministic test evidence";
   private static final String AUTHOR = "Karate Labs";
   private static final String DESCRIPTION = "Run API, browser and business-rule checks against what you build, and get back deterministic evidence: coverage, requirement traceability, and one shareable report.";
   private static final String MCP_URL = "http://localhost:4444/api/mcp";
   private static final String HOST = "http://localhost:4444";
   private static final String IMAGE = "public.ecr.aws/karatelabs/karate-agent:latest";
   private static final String WORKFLOWS = "karate-workflows.md";
   private static final String EVIDENCE = "karate-evidence-loop.md";

   private PowerGenerator() {
   }

   public static void main(String[] var0) throws IOException {
      Path var1 = Path.of(var0.length > 0 ? var0[0] : "src/main/resources/META-INF/karate/skills/index.json");
      Path var2 = Path.of(var0.length > 1 ? var0[1] : "src/main/resources/META-INF/karate/help/pages/manifest.json");
      Path var3 = Path.of(var0.length > 2 ? var0[2] : "../etc/power");
      var var4 = generate(Files.readString(var1), Files.readString(var2));

      for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         Path var7 = var3.resolve((String)var6.getKey());
         Files.createDirectories(var7.getParent());
         Files.writeString(var7, (CharSequence)var6.getValue());
         System.out.println("wrote " + var7 + " (" + ((String)var6.getValue()).length() + " bytes)");
      }
   }

   public static Map<String, String> generate(String var0, String var1) {
      var var2 = parseObjects(var0, "skills");
      var var3 = parseObjects(var1, "pages");
      LinkedHashMap<java.lang.String, java.lang.String> var4 = new LinkedHashMap();
      var4.put("karate/POWER.md", powerMd(var2));
      var4.put("karate/mcp.json", mcpJson());
      var4.put("karate/steering/karate-workflows.md", workflowsMd(var2));
      var4.put("karate/steering/karate-evidence-loop.md", evidenceLoopMd(var3));
      var4.put("registry-entry.json", registryEntry());
      return var4;
   }

   private static String powerMd(List<Map<String, String>> var0) {
      StringBuilder var1 = new StringBuilder();
      var1.append("---\n");
      var1.append("name: \"").append("karate").append("\"\n");
      var1.append("displayName: \"").append("Karate — deterministic test evidence").append("\"\n");
      var1.append("description: \"")
         .append(
            "Run API, browser and business-rule checks against what you build, and get back deterministic evidence: coverage, requirement traceability, and one shareable report."
         )
         .append("\"\n");
      var1.append("keywords: ").append(keywords()).append('\n');
      var1.append("author: \"").append("Karate Labs").append("\"\n");
      var1.append("repository: \"").append("https://github.com/karatelabs/karate-agent-examples").append("\"\n");
      var1.append("license: \"").append("MIT").append("\"\n");
      var1.append("---\n\n");
      var1.append("# Karate\n\n");
      var1.append(
         "Karate runs the checks — API, browser, and executable business rules — and turns the result into evidence you can hand to someone: what was actually exercised, which requirement each check ties back to, and one HTML report that stands on its own.\n\n"
      );
      var1.append(
         "It is a server you point at this workspace. You drive it through **one tool**, `karate_eval`, whose single `code` argument is a line of JavaScript and which returns JSON — every capability is a namespace behind that one tool.\n\n"
      );
      var1.append("**Discover, then compose — do not memorise an API.** The server describes itself at runtime and that is the intended way to use it:\n\n");
      var1.append("| Call | What you get |\n|---|---|\n");
      var1.append("| `help()` | the menu of namespaces, one line each |\n");
      var1.append("| `Noun.help()` | one namespace in full (`Coverage.help()`, `Requirement.help()`, …) |\n");
      var1.append("| `Skill.flows()` | the task-framed workflows — match the user's task to one first |\n");
      var1.append("| `Skill.help('<name>')` | that workflow, end to end |\n");
      var1.append("| `Help.page('<id>')` | the product help for a concept |\n");
      var1.append("| `Work.current()` | which project the server is pointed at |\n\n");
      var1.append(
         "Every call returns `{command, payload}`. An error is **data** (`{error, …}`), never an exception — read it and adjust. The server keeps one persistent JavaScript session, so a `var` you set survives into your next call.\n\n"
      );
      var1.append("## Onboarding\n\n");
      var1.append("Run these once, in order, before using anything else in this power.\n\n");
      var1.append("**1. Is the server up?**\n\n");
      var1.append("```bash\ncurl -s ").append("http://localhost:4444").append("/api/health\n```\n\n");
      var1.append("A healthy server answers `{\"status\":\"ok\",\"server\":\"karate\", …}`. If it answers, skip to step 4.\n\n");
      var1.append("**2. Start it, mounting this workspace.**\n\n");
      var1.append("```bash\ndocker run -d --name karate-agent -p 4444:4444 -v \"$PWD\":/work \\\n  ")
         .append("public.ecr.aws/karatelabs/karate-agent:latest")
         .append("\n```\n\n");
      var1.append(
         "The mounted folder is where everything is read and written — requirements, checks, runs, reports. Mounting the workspace root is what lets Karate see the code you are building.\n\n"
      );
      var1.append("**3. If the port never answers, check for a licensing failure — do not retry the boot.**\n\n");
      var1.append("```bash\ndocker logs karate-agent | grep 'license check'\n```\n\n");
      var1.append(
         "Karate is commercial software and validates a runtime license at startup; without a valid one the server exits rather than coming up degraded. A `license check - …` line means the license is missing, malformed or expired.\n\n"
      );
      var1.append(
         "**Stop here and tell the user.** This is not something you can work around, retrying will not change it, and **there is no trial or self-serve download** — a license is issued by Karate Labs. Give the user both of these, in this order:\n\n"
      );
      var1.append(
            "1. **See the deliverable first — nothing to install, no license needed.** The public sample publishes its real CI output; the **Coverage** and **Traceability** tabs are exactly what this produces for their own project:\n\n   <"
         )
         .append("https://karatelabs.github.io/karate-agent-examples/traceability-demo/")
         .append(">\n\n");
      var1.append("2. **To get a license, contact Karate Labs** at <")
         .append("https://karatelabs.io/agent")
         .append("> — the contact form is at the end of that page.\n\n");
      var1.append("Once they have the license file:\n\n");
      var1.append("```bash\nmkdir -p .karate && cp /path/to/karate.lic .karate/karate.lic\ndocker restart karate-agent\n```\n\n");
      var1.append("**4. Confirm the tool and the project.** Call `karate_eval` with:\n\n");
      var1.append("```js\nWork.current()\n```\n\n");
      var1.append(
         "This returns `{name, root, …}` — the project the server is pointed at. Compare its folder name to this workspace; on a mismatch call `Work.list()` then `Work.use('<name>')` before you do anything else.\n\n"
      );
      var1.append(
         "**If `karate_eval` is not among your tools, say so before you do anything else.** The connection to this server is opened once, when your session starts — so a server that was not yet listening at that moment leaves the tool absent for the rest of the session, and nothing you do inside the conversation reopens it. Tell the user both halves: the server is up (step 1 proves it), the connection to it is not, and reconnecting the `karate` server from their MCP-servers panel brings the tool back.\n\n"
      );
      var1.append("Meanwhile you can keep working over HTTP — `curl -s ")
         .append("http://localhost:4444")
         .append(
            "/api/eval -d '<code>'` runs the same JavaScript and returns the same JSON. **Name the fallback each time you use it.** Falling back silently keeps the answers flowing and hides the broken connection behind them, and the same workspace is driven from surfaces where no shell is available — so a connection nobody noticed was down is a failure deferred, not avoided.\n\n"
         );
      var1.append("**5. Get oriented.** The server sends its own cold-start guide when the MCP connection opens. If you do not have it, pull it: `curl -s ")
         .append("http://localhost:4444")
         .append("/api/prompts`. Then `Skill.flows()` for the workflow menu.\n\n");
      var1.append(
         "**6. Optional — a verification hook.** If the user wants the checks run automatically rather than on request, write `.kiro/hooks/karate-verify.kiro.hook`:\n\n"
      );
      var1.append("```json\n").append(hookJson()).append("\n```\n\n");
      var1.append("Trigger it from the hooks panel. (Task-lifecycle triggers are available in the CLI form; `userTriggered` works everywhere.)\n\n");
      var1.append("## Workflows\n\n");
      var1.append(
         "Match the user's task to a row, then read the steering file named on the right. If nothing fits, call `Skill.flows()` on the live server — it is the same catalog, filtered to what this particular server actually has installed.\n\n"
      );
      var1.append("| When the user wants to | Read the steering file |\n|---|---|\n");
      var1.append("| pick a workflow, author or improve tests, migrate an existing suite, scaffold from a spec | `")
         .append("karate-workflows.md")
         .append("` |\n");
      var1.append("| know whether the work is actually verified — coverage, gaps, traceability, the report | `")
         .append("karate-evidence-loop.md")
         .append("` |\n\n");
      var1.append("The second one is the point of this power. Anything can run a test; the reason to reach for Karate is the evidence it leaves behind. ")
         .append(var0.size())
         .append(" workflows ship with the server — `Skill.list()` on the live server is the authoritative roster.\n\n");
      var1.append("## Licensing, privacy and support\n\n");
      var1.append(
         "**The MCP server is yours.** This power integrates exactly one MCP server: a Karate Agent instance that **you** run, on your own machine or inside your own network. There is no Karate Labs-hosted endpoint — the `url` in `mcp.json` is your own port.\n\n"
      );
      var1.append(
         "- **Your code stays with you.** Source, tests, requirements and run results are read and written in the folder you mounted. No source, usage data or telemetry is sent to Karate Labs by using this power.\n"
      );
      var1.append(
         "- **Licensing is checked offline.** The runtime license is a signed file validated locally; it makes no network call, so the agent runs in a fully air-gapped network.\n"
      );
      var1.append(
         "- **AI features are bring-your-own-model.** You choose the provider and supply the key; requests go from your own instance straight to that provider. Karate Labs is not in the path, and the agent needs no AI key at all when your own client is driving it.\n\n"
      );
      var1.append("**Licensing.** This power's own content — `POWER.md` and its steering files — is ")
         .append("MIT")
         .append(". The Karate Agent it drives is commercial software governed by the Karate Labs [EULA](")
         .append("https://karatelabs.io/eula")
         .append("); a runtime license is issued by Karate Labs.\n\n");
      var1.append("**Privacy.** [Privacy Policy](").append("https://karatelabs.io/privacy-policy").append(").\n\n");
      var1.append("**Support.** <")
         .append("https://karatelabs.io/agent")
         .append("> — the contact form at the end of that page reaches Karate Labs for licensing, evaluation and support.\n");
      return var1.toString();
   }

   private static String registryEntry() {
      String var0 = "https://github.com/karatelabs/karate-agent-examples".replace("https://github.com/", "https://raw.githubusercontent.com/")
         + "/main/powers/karate-icon-512.png";
      return "{\n  \"name\": \"karate\",\n  \"displayName\": \"Karate — deterministic test evidence\",\n  \"author\": \"Karate Labs\",\n  \"description\": \"Run API, browser and business-rule checks against what you build, and get back deterministic evidence: coverage, requirement traceability, and one shareable report.\",\n  \"iconUrl\": \""
         + var0
         + "\",\n  \"repositoryUrl\": \"https://github.com/karatelabs/karate-agent-examples\",\n  \"repositoryCloneUrl\": \"https://github.com/karatelabs/karate-agent-examples.git\",\n  \"pathInRepo\": \"powers/karate\",\n  \"repositoryBranch\": \"main\",\n  \"license\": \"MIT\"\n}\n";
   }

   private static String mcpJson() {
      return "{\n  \"mcpServers\": {\n    \"karate\": {\n      \"url\": \"http://localhost:4444/api/mcp\",\n      \"disabled\": false\n    }\n  }\n}\n";
   }

   private static String hookJson() {
      return "{\n  \"enabled\": true,\n  \"name\": \"Verify with Karate\",\n  \"description\": \"Run the Karate checks and report what the evidence shows.\",\n  \"version\": \"1\",\n  \"when\": { \"type\": \"userTriggered\" },\n  \"then\": {\n    \"type\": \"askAgent\",\n    \"prompt\": \"Use the karate power. Run this project's checks, then report coverage gaps and requirement traceability. Report a criterion with no passing check as unverified — do not describe it as done.\"\n  }\n}";
   }

   private static String workflowsMd(List<Map<String, String>> var0) {
      StringBuilder var1 = new StringBuilder();
      var1.append("# Karate workflows\n\n");
      var1.append(
         "Karate ships its own instructions and serves them on demand. Do not improvise a multi-step task from first principles — **pull the workflow that matches and follow it**. Each is a complete recipe, kept in step with the running engine.\n\n"
      );
      var1.append(
         "```js\nSkill.flows()                       // the task-framed menu\nSkill.help('<name>')                // one workflow, end to end\nSkill.help('<name>', '<topic>')     // a deep dive within it\nSkill.search('coverage gaps')       // when you do not know the name\n```\n\n"
      );
      var1.append(
         "`Skill.list()` on the live server is authoritative — a workflow gated on an add-on appears only when that add-on is installed. The tables below are what the current release carries.\n\n"
      );
      var1.append("## By task\n\n");
      var1.append("| The user wants to | Pull |\n|---|---|\n");

      for (Map var3 : var0) {
         String var4 = (String)var3.get("flow");
         if (var4 != null && !var4.isBlank()) {
            var1.append("| ").append(oneLine(var4)).append(" | `Skill.help('").append(var3.getOrDefault("name", "")).append("')` |\n");
         }
      }

      var1.append("\n## Reference\n\n");
      var1.append("Not a task — look these up while doing one.\n\n");
      var1.append("| Workflow | Covers |\n|---|---|\n");

      for (Map var9 : var0) {
         String var11 = (String)var9.get("flow");
         if (var11 == null || var11.isBlank()) {
            var1.append("| `Skill.help('").append(var9.getOrDefault("name", "")).append("')` | ").append(oneLine((String)var9.getOrDefault("hint", ""))).append(" |\n");
         }
      }

      var1.append("\n## Deep dives\n\n");
      var1.append("Some workflows carry topic files — pass the topic as the second argument.\n\n");

      for (Map var10 : var0) {
         String var12 = (String)var10.get("topics");
         if (var12 != null && !var12.isBlank()) {
            var1.append("- `").append(var10.getOrDefault("name", "")).append("` → ");
            String[] var5 = var12.split(",");

            for (int var6 = 0; var6 < var5.length; var6++) {
               var1.append(var6 == 0 ? "" : ", ").append('`').append(var5[var6].trim()).append('`');
            }

            var1.append('\n');
         }
      }

      return var1.toString();
   }

   private static String evidenceLoopMd(List<Map<String, String>> var0) {
      StringBuilder var1 = new StringBuilder();
      var1.append("# Is the work actually verified?\n\n");
      var1.append(
         "This is the question Karate exists to answer, and the one worth reaching for it over. A green test run says a test passed. It does not say the thing you were asked to build is covered. Karate closes that distance: every check declares the requirement or acceptance criterion it exercises, and the engine derives — deterministically, with no model in the loop — which criteria have a passing check behind them and which do not.\n\n"
      );
      var1.append(
         "**Report what the evidence shows, not what you intended.** A criterion with no passing check is unverified. Say so plainly, then offer to write the missing check. That is the whole value of having this power installed; softening it removes the point.\n\n"
      );
      var1.append("## The loop\n\n");
      var1.append(
         "1. **Know what has to be true.** If this workspace carries a spec, its acceptance criteria are the list. Otherwise the requirements live in the project — `Requirement.list()`.\n"
      );
      var1.append(
         "2. **Author checks that declare what they verify.** A check states the criterion it exercises; that declaration is what the traceability is built from, and it is easy to get subtly wrong — follow `Skill.help('requirements')` rather than guessing the tag form.\n"
      );
      var1.append("3. **Run them.** `Skill.help('launch')` for a whole suite in one command, `Skill.help('coverage')` for what a run records.\n");
      var1.append("4. **Read the verdict, not the exit code:**\n\n");
      var1.append(
         "```js\nRequirement.matrix()        // every requirement × what covers it\nRequirement.gaps()          // no passing check, untested criteria, hollow links\nRequirement.readiness()     // the confidence-to-ship verdict + its blockers\nCoverage.gaps()             // what the run did not touch\n```\n\n"
      );
      var1.append("Each takes `{output:'both'}` to add a rendered scorecard alongside the data — use it when you are reporting to a human.\n\n");
      var1.append(
         "**A verdict is never cacheable.** It is derived from the last run, and it changes the moment anything runs — including a run someone started in a terminal you cannot see. When the question is asked again, ask the engine again; never repeat an earlier answer from memory, however recent. `runEvidence` on the reply says how much run evidence is behind it: `tests: 0` means nothing has run yet, so *every* status reads NOTCOVERED because nothing was checked — which is not the same finding as a run that checked and missed.\n\n"
      );
      var1.append(
         "5. **Leave the artifact.** The run produces one HTML report with the coverage and the traceability matrix in it — `Skill.help('coverage')` covers publishing it. It is the thing that outlives the conversation.\n\n"
      );
      var1.append(
         "Before anything has been run, `Runner.suite(path, {dryRun:true})` answers *\"which requirements have a check at all?\"* without executing a step — useful the moment a spec grows a new criterion.\n\n"
      );
      var1.append("## \"Which completed tasks aren't actually verified?\"\n\n");
      var1.append(
         "When the workspace carries a spec whose tasks are ticked off as done — a task list, an implementation plan, a checklist of work items — those ticks are **claims**, and Karate reads them alongside the criteria they say they satisfy. `Requirement.readiness()` returns `claimedUnverified`: every criterion some completed task claims and no passing check backs, each named with the task that claimed it. That is the whole answer to *\"is the work really done?\"*, already joined — do not reconstruct it by reading the task list and matching ids up yourself. A criterion in that list is work someone marked complete that nothing verifies: report it that way, name the task, and offer to write the check.\n\n"
      );
      var1.append("## Concepts\n\n");
      var1.append(
         "`Help.page('<id>')` returns a whole help page; `Help.search('<term>')` finds the right one when you do not know the id. (A dotted slug a user pastes from a report — `rule.execute`, `model.coverage.oracleonly` — is a *help-id*, not a page: resolve those with `Help.get('<id>')`, and `Help.topics()` lists them all.) The pages:\n\n"
      );
      var1.append("| `Help.page(...)` | |\n|---|---|\n");

      for (Map var3 : var0) {
         var1.append("| `").append(var3.getOrDefault("id", "")).append("` | ").append(oneLine((String)var3.getOrDefault("title", ""))).append(" |\n");
      }

      return var1.toString();
   }

   private static String keywords() {
      var var0 = List.of(
         "karate",
         "test automation",
         "api testing",
         "ui testing",
         "browser testing",
         "gherkin",
         "openapi",
         "traceability",
         "acceptance criteria",
         "coverage gaps",
         "mock server"
      );
      StringBuilder var1 = new StringBuilder("[");

      for (int var2 = 0; var2 < var0.size(); var2++) {
         var1.append(var2 == 0 ? "" : ", ").append('"').append((String)var0.get(var2)).append('"');
      }

      return var1.append(']').toString();
   }

   static List<Map<String, String>> parseObjects(String var0, String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.String>> var2 = new ArrayList();
      int var3 = var0.indexOf("\"" + var1 + "\"");
      if (var3 < 0) {
         return var2;
      } else {
         var3 = var0.indexOf(91, var3);
         if (var3 < 0) {
            return var2;
         } else {
            int var4 = matchingBracket(var0, var3);
            if (var4 < 0) {
               throw new IllegalArgumentException("unterminated '" + var1 + "' array — malformed JSON");
            } else {
               for (String var6 : splitObjects(var0.substring(var3, var4))) {
                  var2.add(parseFields(var6));
               }

               return var2;
            }
         }
      }
   }

   private static List<String> splitObjects(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      boolean var2 = false;
      int var3 = 0;
      int var4 = -1;

      for (int var5 = 0; var5 < var0.length(); var5++) {
         char var6 = var0.charAt(var5);
         if (var2) {
            if (var6 == '\\') {
               var5++;
            } else if (var6 == '"') {
               var2 = false;
            }
         } else if (var6 == '"') {
            var2 = true;
         } else if (var6 == '{') {
            if (var3++ == 0) {
               var4 = var5;
            }
         } else if (var6 == '}') {
            if (--var3 == 0) {
               var1.add(var0.substring(var4, var5 + 1));
            }
         }
      }

      return var1;
   }

   private static Map<String, String> parseFields(String var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();
      Matcher var2 = Pattern.compile("\"([A-Za-z]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(var0);

      while (var2.find()) {
         var1.put(var2.group(1), unescape(var2.group(2)));
      }

      Matcher var3 = Pattern.compile("\"([A-Za-z]+)\"\\s*:\\s*\\[([^]]*)]").matcher(var0);

      while (var3.find()) {
         ArrayList<java.lang.String> var4 = new ArrayList();
         Matcher var5 = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(var3.group(2));

         while (var5.find()) {
            var4.add(unescape(var5.group(1)));
         }

         var1.put(var3.group(1), String.join(",", var4));
      }

      return var1;
   }

   private static int matchingBracket(String var0, int var1) {
      int var2 = 0;
      boolean var3 = false;

      for (int var4 = var1; var4 < var0.length(); var4++) {
         char var5 = var0.charAt(var4);
         if (var3) {
            if (var5 == '\\') {
               var4++;
            } else if (var5 == '"') {
               var3 = false;
            }
         } else {
            switch (var5) {
               case '"':
                  var3 = true;
                  break;
               case '[':
                  var2++;
                  break;
               case ']':
                  if (--var2 == 0) {
                     return var4 + 1;
                  }
            }
         }
      }

      return -1;
   }

   private static String unescape(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '\\' && var2 + 1 < var0.length()) {
            char var4 = var0.charAt(++var2);
            switch (var4) {
               case 'b':
               case 'f':
                  var1.append(' ');
                  break;
               case 'c':
               case 'd':
               case 'e':
               case 'g':
               case 'h':
               case 'i':
               case 'j':
               case 'k':
               case 'l':
               case 'm':
               case 'o':
               case 'p':
               case 'q':
               case 's':
               default:
                  var1.append(var4);
                  break;
               case 'n':
               case 'r':
               case 't':
                  var1.append(' ');
                  break;
               case 'u':
                  if (var2 + 4 < var0.length()) {
                     var1.append((char)Integer.parseInt(var0.substring(var2 + 1, var2 + 5), 16));
                     var2 += 4;
                  }
            }
         } else {
            var1.append(var3);
         }
      }

      return var1.toString();
   }

   private static String oneLine(String var0) {
      return var0.replace("|", "\\|").replaceAll("\\s+", " ").trim();
   }
}
