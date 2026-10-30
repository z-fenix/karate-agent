package io.karatelabs.ext.agent.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentEventLogger implements AgentEventListener {
   private static final Logger a = LoggerFactory.getLogger("io.karatelabs.ext.harness.run");
   private static final int b = 200;

   @Override
   public void onEvent(AgentEvent var1) {
      switch (var1) {
         case io.karatelabs.ext.agent.events.AgentEvent.IterationStartEvent var4:
            a.info("[turn {}]", var4.g());
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ToolRequestEvent var5:
            a.info("  js> {}", a(var5.h()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ToolResultEvent var6:
            a.info("  => {}", a(var6.i()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.HintEvent var7:
            a.info("  hint[{}] {}", var7.h(), a(var7.i()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.StepEvent var8:
            a.info("  step: {}", var8.h());
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.AssertionEvent var9:
            a.info("  assert: {}", var9.h().get("pass"));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ScreenshotEvent var10:
            a.info("  screenshot: {}", var10.h());
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.HealedEvent var11:
            a.info("  healed: '{}' → '{}' ({})", new Object[]{var11.h(), var11.i(), var11.j()});
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ContextTrimEvent var12:
            a.debug("  (context trimmed)");
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ArtifactEvent var13:
            a.debug("  artifact[{}] {}", var13.h(), var13.j());
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ConsoleEvent var14:
            a.debug("  console: {}", a(var14.h()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.LlmResponseEvent var15:
            a.debug("  llm: {} in / {} out tokens ({}ms)", new Object[]{var15.j(), var15.k(), var15.n()});
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.CompleteEvent var16:
            a.info(
               "[done] status={} iterations={} tokens={}+{} {}",
               new Object[]{var16.g(), var16.i(), var16.j(), var16.k(), var16.n() != null ? "error=" + var16.n() : ""}
            );
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent var17:
            a.warn("[error]{} {}", var17.i() ? " (retryable)" : "", var17.h());
            if (var17.j() != null) {
               a.warn("[hint] {}", var17.j());
            }
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.LlmRequestEvent var18:
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent var19:
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.LlmResponseStartEvent var20:
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.TextChunkEvent var21:
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.PausedEvent var22:
            a.info("[paused]");
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ResumedEvent var23:
            a.info("[resumed] {}", a(var23.h()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.HumanActionEvent var24:
            a.info("  human: {} {}", var24.g().get("action"), a(String.valueOf(var24.g().getOrDefault("locator", ""))));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.FocusEvent var25:
            a.debug("  focus[{}] {}", var25.g(), a(var25.h()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.AnnotationEvent var26:
            a.info("  note: {}", a(var26.g()));
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.BrowserResetEvent var27:
            a.info("  [browser reset]");
            break;
         case io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent var28:
            a.info("  [control → {}]{}", var28.g(), var28.h() != null ? " " + a(var28.h()) : "");
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   private static String a(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = var0.replace('\n', ' ');
         return var1.length() <= 200 ? var1 : var1.substring(0, 200) + "…";
      }
   }
}
