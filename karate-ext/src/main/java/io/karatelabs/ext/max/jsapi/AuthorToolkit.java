package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public final class AuthorToolkit implements AgentExtension {
   static final String SKILL = "META-INF/karate/skills/record-to-check/skill.md";
   private final AuthorToolkit.Mode mode;
   private final String entry;
   private final Path checksDir;
   private volatile String greenDigest;
   private volatile boolean unasserted;
   private volatile String runDigest;
   private volatile boolean running;
   private static final String COLD_RUN = "run `Check.run('<name>.js', null, {reset:true})` — no arg, as it is replayed; `{reset:true}` wipes cookies/storage so it runs the true cold-start path the way it runs for real, NOT from the mid-flow state your live drive left behind.";
   private static final String DURABLE_MEMORY = "**Your durable memory is the FILE and your saved variables — not the chat.** Context older than the most recent turn is compacted (a trimmed `look()` keeps a COUNT, not its locators). On recovery, `File.read` the check you are editing and re-read the source in your task — work from those DURABLE sources, never reconstruct a locator from memory (that is how a verbatim locator gets mangled). Any `var`/function you define in the engine persists across trims (it is listed back to you), so use one as a scratchpad for verified locators/values if useful.\n\n";

   public AuthorToolkit() {
      this(AuthorToolkit.Mode.AUTHOR);
   }

   public AuthorToolkit(AuthorToolkit.Mode var1) {
      this(var1, null, null);
   }

   public AuthorToolkit(AuthorToolkit.Mode var1, String var2, Path var3) {
      this.mode = var1;
      this.checksDir = var3;
      this.entry = var1 != AuthorToolkit.Mode.REVISE && var2 != null ? Check.definitionId(var2) : null;
   }

   public Check.Verdict verdict() {
      return new Check.Verdict() {
         @Override
         public void starting(String var1, boolean var2) {
            AuthorToolkit.this.running = true;
            AuthorToolkit.this.runDigest = var2 && AuthorToolkit.this.isEntry(var1) ? AuthorToolkit.digest(AuthorToolkit.this.checksDir) : null;
         }

         @Override
         public void ran(String var1, boolean var2, boolean var3, int var4) {
            AuthorToolkit.this.running = false;
            String var5 = AuthorToolkit.this.runDigest;
            AuthorToolkit.this.runDigest = null;
            if (AuthorToolkit.this.isEntry(var1)) {
               String var6 = var3 && var5 != null && var5.equals(AuthorToolkit.digest(AuthorToolkit.this.checksDir)) ? var5 : null;
               AuthorToolkit.this.unasserted = var6 != null && var4 == 0 && AuthorToolkit.this.mode == AuthorToolkit.Mode.AUTHOR;
               AuthorToolkit.this.greenDigest = AuthorToolkit.this.unasserted ? null : var6;
            }
         }

         @Override
         public boolean inPlace() {
            return AuthorToolkit.this.mode != AuthorToolkit.Mode.AUTHOR;
         }
      };
   }

   private boolean isEntry(String var1) {
      return this.entry != null && this.entry.equals(Check.definitionId(var1));
   }

   public String exitRefusal() {
      return this.running
         ? "Robot.success/failure refused inside a check: a check holds no loop exit — end the check with a value and call Robot.success from the loop after the green Check.run"
         : null;
   }

   static String digest(Path var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            MessageDigest var1 = MessageDigest.getInstance("SHA-256");
            if (Files.isDirectory(var0)) {
               byte[] var2 = new byte[65536];

               try (var var3 = Files.walk(var0, FileVisitOption.FOLLOW_LINKS)) {
                  for (Path var5 : (Iterable<Path>) (Iterable<?>) (var3.filter(var0x -> Files.isRegularFile(var0x)).sorted().toList())) {
                     var1.update(var0.relativize(var5).toString().getBytes(StandardCharsets.UTF_8));
                     var1.update((byte)0);

                     int var7;
                     try (InputStream var6 = Files.newInputStream(var5)) {
                        while ((var7 = var6.read(var2)) > 0) {
                           var1.update(var2, 0, var7);
                        }
                     }

                     var1.update((byte)0);
                  }
               }
            }

            return HexFormat.of().formatHex(var1.digest());
         } catch (Exception var13) {
            return "unreadable:" + System.nanoTime();
         }
      }
   }

   @Override
   public String refusal(AgentOutcome var1) {
      if (this.entry != null && var1.kind() == io.karatelabs.ext.agent.runtime.AgentOutcome.Kind.SUCCESS && (this.greenDigest == null || !this.greenDigest.equals(digest(this.checksDir)))) {
         String var2 = this.entry.substring("check:checks/".length());
         if (this.unasserted) {
            return "Robot.success refused: Check.run('"
               + var2
               + "') is green but asserts nothing. Add one match on where the flow lands — match.contains(bot.eval('location.href'), '<path>', 'landed') or match.contains(bot.text('<locator from look()>'), '<text>', '<label>') — then Check.run('"
               + var2
               + "', null, {reset:true}); call Robot.success once it is green.";
         } else {
            return this.mode == AuthorToolkit.Mode.HEAL
               ? "Robot.success refused: no Check.run('"
                  + var2
                  + "') has been green with no arg since checks/ last changed, and a copy's green does not count. Fix the target in place: File.write('checks/"
                  + var2
                  + "', src). Then Check.run('"
                  + var2
                  + "', null, {reset:true}); call Robot.success once that run is green."
               : "Robot.success refused: the test is replayed with no arg, and no Check.run('"
                  + var2
                  + "') has been green that way since checks/ last changed. Default each input in the check (`__arg.user || 'x'`), then Check.run('"
                  + var2
                  + "', null, {reset:true}).";
         }
      } else {
         return null;
      }
   }

   @Override
   public List<AgentHook> hooks() {
      return List.of(new AuthoringStageHook());
   }

   @Override
   public String promptFragment() {
      return switch (this.mode) {
         case AUTHOR -> this.authorFragment();
         case HEAL -> this.healFragment();
         case REVISE -> this.reviseFragment();
      };
   }

   private String authorFragment() {
      StringBuilder var1 = new StringBuilder();
      var1.append("## Authoring a check from a recording — your task\n\n");
      var1.append(
         "You are running AUTONOMOUSLY as the check author: the recorded UI flow (its replayable steps + annotations) is in your task message, and a LIVE browser (`bot`) is already open on the SUT — it is your REPL. Do NOT `Robot.create()` a second browser. Build the check in STAGES, live-first, as the skill below lays out; Stage 1 is the deterministic start the recording does not show (its Step 3).\n\n"
      );
      var1.append(
         "- **Drive each stage's steps on the live `bot` FIRST** (`bot.act('<loc>', 'click')`, then `bot.look()`) and codify only what you saw work.\n"
      );
      var1.append(
         "- `bot.act` already waits for the page to settle; add `bot.wait(...)` only for an async signal you observed — never on a title you have not seen.\n\n"
      );
      var1.append(
         "**Exit:** once every stage is green, run `Check.run('<name>.js', null, {reset:true})` — no arg, as it is replayed; `{reset:true}` wipes cookies/storage so it runs the true cold-start path the way it runs for real, NOT from the mid-flow state your live drive left behind. ONLY once that full-flow run is green, call `Robot.success('<one-line summary>')`. Never `Robot.failure` while iterations remain and the error is recoverable.\n\n"
      );
      var1.append(
         "**Your durable memory is the FILE and your saved variables — not the chat.** Context older than the most recent turn is compacted (a trimmed `look()` keeps a COUNT, not its locators). On recovery, `File.read` the check you are editing and re-read the source in your task — work from those DURABLE sources, never reconstruct a locator from memory (that is how a verbatim locator gets mangled). Any `var`/function you define in the engine persists across trims (it is listed back to you), so use one as a scratchpad for verified locators/values if useful.\n\n"
      );
      var1.append("**If you cannot get it green (low on iterations), leave a RESUMABLE hand-off — do NOT just stop:**\n");
      var1.append(
         "- `File.write` the best partial `checks/<name>.js` — keep the steps that work, mark each unresolved spot with a `// TODO:` saying exactly what is blocked (the locator that drifted + the candidates you tried, the assertion you could not verify, the entity that needs pinning), and keep it valid JS.\n"
      );
      var1.append("- Then `Robot.failure('<one line>: blocked on X — partial check saved to checks/<name>.js, see the // TODO markers')`.\n\n");
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/skills/record-to-check/skill.md");
      if (var2 != null && !var2.isBlank()) {
         var1.append("The authoring know-how (the `record-to-check` skill) follows — apply it:\n\n");
         var1.append(var2);
      }

      return var1.toString();
   }

   private String healFragment() {
      StringBuilder var1 = new StringBuilder();
      var1.append("## Healing a drifted check — your task\n\n");
      var1.append(
         "You are running AUTONOMOUSLY as the check maintainer. The target is the check named in your task message: its source there gives the flow's intent, and its stale values are what drifted. Edit that file in place. A LIVE browser (`bot`) is already open on the SUT — it is your REPL; do NOT `Robot.create()` a second one.\n\n"
      );
      var1.append("- **First action: `Check.run('<name>.js')`.** Green → `Robot.success` now; never rewrite a passing check.\n");
      var1.append(
         "- **On red, diagnose where it stopped:** the bot is left on the failing step, and the error names the line, its source and the page state. `bot.look({full: true})` there; never navigate, reload or guess a URL (config vars like `demoUrl` are not in your scope).\n"
      );
      var1.append("- **Fix only the failing line** to the live reality (locator, field, value), then re-run `Check.run` — one failing point at a time.\n");
      var1.append(
         "- **Never loosen, weaken or delete an assertion.** Benign drift → repair it; a genuine SUT defect → `Robot.failure('<one line>: looks like a real defect — <the discrepancy>')`. Unsure → report it.\n\n"
      );
      var1.append(
         "**Exit:** once the failing point drives clean, run `Check.run('<name>.js', null, {reset:true})` — no arg, as it is replayed; `{reset:true}` wipes cookies/storage so it runs the true cold-start path the way it runs for real, NOT from the mid-flow state your live drive left behind. ONLY once that full-flow run is green, call `Robot.success('<what drifted + how you fixed it>')`.\n\n"
      );
      var1.append(
         "**Your durable memory is the FILE and your saved variables — not the chat.** Context older than the most recent turn is compacted (a trimmed `look()` keeps a COUNT, not its locators). On recovery, `File.read` the check you are editing and re-read the source in your task — work from those DURABLE sources, never reconstruct a locator from memory (that is how a verbatim locator gets mangled). Any `var`/function you define in the engine persists across trims (it is listed back to you), so use one as a scratchpad for verified locators/values if useful.\n\n"
      );
      var1.append(
         "**Low on iterations and still red:** `File.write` the best partial with a `// TODO:` at each blocked spot (the drifted locator + the candidates tried), keep it valid JS, then `Robot.failure('<one line>: blocked on X — partial saved, see the // TODO markers')`.\n\n"
      );
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/skills/record-to-check/skill.md");
      if (var2 != null && !var2.isBlank()) {
         var1.append("The authoring know-how (the `record-to-check` skill) follows; apply it when repairing the source:\n\n");
         var1.append(var2);
      }

      return var1.toString();
   }

   private String reviseFragment() {
      StringBuilder var1 = new StringBuilder();
      var1.append("## Changing a check as asked — your task\n\n");
      var1.append(
         "You are running AUTONOMOUSLY as the check MAINTAINER. A person asked for a change to an existing deterministic check; the request and the check's CURRENT SOURCE are in your task message. A LIVE browser (`bot`) is already open on the SUT — it is your REPL. Work the loop yourself, one step per turn.\n\n"
      );
      var1.append(
         "**STEP 1 — run it first, as it is.** Your FIRST action MUST be `Check.run('<name>.js')`. If it is RED before you change anything, STOP: call `Robot.failure('<one line>: it already fails before the change — <the error>; repair it first')`. Repairing is not this task, and a change built on a broken check cannot be verified.\n\n"
      );
      var1.append(
         "**STEP 2 — make exactly the change asked, and nothing else.** Keep every other step, locator, value, assertion and header line as it is; never loosen, weaken or delete an assertion the request did not ask to change. Prove a new or changed step on the live `bot` first — drive it, `bot.look()` where it lands — and only THEN write it: `File.read` the check, `File.write` it with the change. If the request is ambiguous, contradicts the check, or needs a screen or a value you cannot find on the live app, STOP with `Robot.failure('<one line>: why it cannot be done as asked')` — never guess what was meant.\n\n"
      );
      var1.append(
         "**STEP 3 — a requested expectation stays as asked.** When the request asks the check to verify something (a total, a label, a count), write that expectation exactly as asked. If the live app shows something else, the app disagrees with the person: keep the assertion as asked and say so — NEVER rewrite the expectation to what the app shows. That red is the finding.\n\n"
      );
      var1.append(
         "**STEP 4 — finish with one cold run.** run `Check.run('<name>.js', null, {reset:true})` — no arg, as it is replayed; `{reset:true}` wipes cookies/storage so it runs the true cold-start path the way it runs for real, NOT from the mid-flow state your live drive left behind. Then call `Robot.success('<what you changed> — <passes | fails at the requested check: expected X, the app shows Y>')`. Success means the change is written as asked and the full flow ran cold; it is not a verdict — the test's next replay gives that. If the cold run fails anywhere OTHER than a step you were asked to change, your change broke the flow: fix your change, not the old steps, and run again.\n\n"
      );
      var1.append(
         "**If you cannot finish (low on iterations), call `Robot.failure('<one line>: what blocks the change')` and leave no partial edit behind — the server puts the check back exactly as it was when the run does not complete.**\n\n"
      );
      var1.append(
         "**Your durable memory is the FILE and your saved variables — not the chat.** Context older than the most recent turn is compacted (a trimmed `look()` keeps a COUNT, not its locators). On recovery, `File.read` the check you are editing and re-read the source in your task — work from those DURABLE sources, never reconstruct a locator from memory (that is how a verbatim locator gets mangled). Any `var`/function you define in the engine persists across trims (it is listed back to you), so use one as a scratchpad for verified locators/values if useful.\n\n"
      );
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/skills/record-to-check/skill.md");
      if (var2 != null && !var2.isBlank()) {
         var1.append(
            "The authoring know-how (the `record-to-check` skill — the bot.* action map, waits, parameterization, config/boot ties) follows; apply it when changing the source:\n\n"
         );
         var1.append(var2);
      }

      return var1.toString();
   }

   public static enum Mode {
      AUTHOR,
      HEAL,
      REVISE;
   }
}
