package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.ToolResult;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiReplyToolkit implements AgentExtension {
   static final Pattern TOOL_NAME = Pattern.compile("\\b(Api|ApiTest|Http|Postman|Requirement|File|Harness|Runner|Secret)\\.[a-z][A-Za-z]*\\b");
   static final Pattern RUN_LINK = Pattern.compile("/manual/runs/([A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*)");
   private static final int SEEN_MAX = 400000;
   static final Set<String> PRODUCERS = Set.of("Traffic.migrate", "Postman.migrate", "ApiTest.createFeature", "ApiTest.createLoginFeature");
   private AgentContext ctx;
   private volatile boolean replied;
   private int readAt = -1;
   private String readVerb;
   private int lastAt = -1;
   private boolean lastRead;
   private int producedAt = -1;
   private String producedBy;
   private int callsAt = -1;
   private int calls;
   private final List<String> refused = new ArrayList<>();
   private final StringBuilder seen = new StringBuilder();

   @Override
   public void init(AgentContext var1) {
      this.ctx = var1;
   }

   @Override
   public String promptFragment() {
      return "End the run with `Api.reply(text)`: `text` is all the tester reads. Nothing after it runs.";
   }

   @Override
   public List<String> terminalCallNames() {
      return List.of("Api.reply");
   }

   @Override
   public Map<String, Object> bindings() {
      return Map.of("Api", new SimpleObject() {
         @Override
         public Object jsGet(String var1) {
            return "reply".equals(var1) ? (io.karatelabs.js.JavaInvokable)var1x -> ApiReplyToolkit.this.reply(var1x.length > 0 ? var1x[0] : null) : Terms.UNDEFINED;
         }

         @Override
         public Collection<String> jsKeys() {
            return List.of("reply");
         }
      });
   }

   public void refuseAfterReply() {
      if (this.replied) {
         throw new IllegalStateException("the run already replied — nothing after Api.reply runs");
      }
   }

   public void noteCall(String var1, boolean var2) {
      this.refuseAfterReply();
      this.readVerb = var2 ? var1 : null;
      this.readAt = var2 ? this.block() : -1;
      this.lastRead = var2;
      this.lastAt = this.block();
      if (PRODUCERS.contains(var1) && this.producedAt != this.block()) {
         this.producedAt = this.block();
         this.producedBy = var1;
      }

      if (this.callsAt != this.block()) {
         this.callsAt = this.block();
         this.calls = 0;
         this.refused.clear();
      }

      this.calls++;
   }

   public Object called(String var1, boolean var2, Supplier<Object> var3) {
      this.noteCall(var1, var2);

      Object var4;
      try {
         var4 = var3.get();
      } catch (RuntimeException var6) {
         if (io.karatelabs.ext.agent.runtime.ControlSignal.a((Throwable)var6) == null) {
            this.refused.add(var1 + ": " + var6.getMessage());
         }

         throw var6;
      }

      if (var4 instanceof Map var5 && var5.get("error") != null) {
         this.refused.add(var1 + ": " + var5.get("error"));
      }

      if (var4 instanceof Map var7 && var7.get("runId") != null && this.seen.length() < 400000) {
         this.seen.append(var7.get("runId")).append('\n');
      }

      return var4;
   }

   private List<String> refusedNow() {
      return this.callsAt == this.block() ? this.refused : List.of();
   }

   private int block() {
      return this.ctx == null ? 0 : this.ctx.state().iterations();
   }

   @Override
   public List<AgentHook> hooks() {
      return List.of(new AgentHook() {
         @Override
         public Optional<String> afterTool(AgentContext var1, String var2, ToolResult var3, String var4) {
            if (var4 != null && ApiReplyToolkit.this.seen.length() < 400000) {
               ApiReplyToolkit.this.seen.append(var4, 0, Math.min(var4.length(), 400000 - ApiReplyToolkit.this.seen.length())).append('\n');
            }

            var var5 = ApiReplyToolkit.this.refusedNow();
            if (!var5.isEmpty() && ApiReplyToolkit.this.calls >= 2) {
               StringBuilder var6 = new StringBuilder("Refused in this block, in order — none of these happened:");

               for (String var8 : (Iterable<String>) (Iterable<?>) (var5)) {
                  var6.append("\n- ").append(var8.length() > 300 ? var8.substring(0, 300) + "…" : var8);
               }

               return Optional.of(var6.toString());
            } else {
               return Optional.empty();
            }
         }
      });
   }

   @Override
   public AgentOutcome proseOutcome(String var1) {
      if (!this.replied
         && this.ctx != null
         && !this.ctx.lastResponseTruncated()
         && this.lastRead
         && this.lastAt >= 0
         && this.lastAt < this.block()
         && var1 != null
         && !var1.isBlank()
         && !var1.contains("```")
         && this.unsendable(var1) == null) {
         this.replied = true;
         return io.karatelabs.ext.agent.runtime.AgentOutcome.failure(var1.trim());
      } else {
         return null;
      }
   }

   private String unsendable(String var1) {
      Matcher var2 = TOOL_NAME.matcher(var1);
      if (var2.find()) {
         return "Api.reply was not sent: it names " + var2.group() + ", which the tester never sees. Say what happened in plain words.";
      } else {
         Matcher var3 = RUN_LINK.matcher(var1);
         String var4 = this.ctx != null && this.ctx.state().task() != null ? this.ctx.state().task() : "";

         while (var3.find()) {
            String var5 = var3.group(1);
            Pattern var6 = Pattern.compile("(?<![A-Za-z0-9_.-])" + Pattern.quote(var5) + "(?![A-Za-z0-9_-])");
            boolean var7 = var3.end() < var1.length() && var1.charAt(var3.end()) == '/';
            if (var7 || !var6.matcher(this.seen).find() && !var6.matcher(var4).find()) {
               return "Api.reply was not sent: it links /manual/runs/"
                  + var5
                  + ", a run no result of this run named. Link only a runId a result gave you, or leave the link out.";
            }
         }

         return null;
      }
   }

   private Object reply(Object var1) {
      this.refuseAfterReply();
      if (var1 instanceof String var2) {
         if (var2.isBlank()) {
            throw new IllegalArgumentException("Api.reply(text) refuses a blank reply");
         } else {
            String var3 = this.unsendable(var2);
            if (var3 != null) {
               throw new IllegalArgumentException(var3);
            } else if (this.readAt == this.block()) {
               throw new IllegalStateException(
                  "Api.reply was not sent: it came right after " + this.readVerb + ", whose result you have not seen. Reply in a later block."
               );
            } else if (!this.refusedNow().isEmpty()) {
               throw new IllegalStateException(
                  "Api.reply was not sent: "
                     + this.refusedNow().get(0).split(":", 2)[0]
                     + " was refused in this block, so it did not happen. Read the refusal, then reply in a later block."
               );
            } else if (this.producedAt == this.block()) {
               throw new IllegalStateException(
                  "Api.reply was not sent: " + this.producedBy + " ran in this block and you have not seen its result. Reply in a later block."
               );
            } else if (this.ctx.lastResponseTruncated()) {
               throw new IllegalStateException("this response was cut off at the output-token ceiling, so the reply was not sent — send it again, shorter");
            } else {
               this.replied = true;
               AgentOutcome var4 = io.karatelabs.ext.agent.runtime.AgentOutcome.failure(var2.trim());
               this.ctx.submitOutcome(var4);
               throw io.karatelabs.ext.agent.runtime.ControlSignal.stop(var4);
            }
         }
      } else {
         throw new IllegalArgumentException("Api.reply(text) takes the reply as a string");
      }
   }
}
