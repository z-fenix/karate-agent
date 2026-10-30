package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.max.apitest.CandidateRunner;
import io.karatelabs.ext.max.apitest.Failing;
import io.karatelabs.ext.max.apitest.TestId;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OverlayCandidateRunner implements CandidateRunner {
   private static final Logger logger = LoggerFactory.getLogger(OverlayCandidateRunner.class);
   private final Runner runner;
   private final Harness slot;
   private final Path root;
   private volatile BiConsumer<CandidateRunner.Claim, CandidateRunner.Request> chatTurn;
   private volatile BiFunction<TestId, CandidateRunner.Claim, CandidateRunner.Claim> chatIds;
   volatile Runnable afterSettle;

   public OverlayCandidateRunner(Runner var1, Harness var2, Path var3) {
      this.runner = var1;
      this.slot = var2;
      this.root = var3.toAbsolutePath().normalize();
   }

   public OverlayCandidateRunner withChatTurn(BiConsumer<CandidateRunner.Claim, CandidateRunner.Request> var1) {
      this.chatTurn = var1;
      return this;
   }

   public OverlayCandidateRunner withChatIds(BiFunction<TestId, CandidateRunner.Claim, CandidateRunner.Claim> var1) {
      this.chatIds = var1;
      return this;
   }

   @Override
   public CandidateRunner.Claim claim(TestId var1, CandidateRunner.Purpose var2) throws CandidateRunner.Refused {
      final BiConsumer var3 = this.chatTurn;
      if (var2 == CandidateRunner.Purpose.ANSWER && var3 == null) {
         throw new CandidateRunner.Refused("UNAVAILABLE", "the side chat is not connected on this console");
      } else {
         final String var4 = UUID.randomUUID().toString().substring(0, 8);
         String var5 = this.slot.claimApiRun(var4, (var2 == CandidateRunner.Purpose.VERIFY ? "verify " : "answer ") + var1.path());
         if (var5 != null) {
            throw new CandidateRunner.Refused("BUSY", var5);
         } else {
            CandidateRunner.Claim var6 = new CandidateRunner.Claim() {
               @Override
               public String runId() {
                  return var4;
               }

               @Override
               public void release() {
                  OverlayCandidateRunner.this.slot.settleApiRun(var4, "stopped");
               }

               @Override
               public void start(CandidateRunner.Request var1) {
                  if (var1.purpose() == CandidateRunner.Purpose.ANSWER) {
                     var3.accept(this, var1);
                  } else {
                     Thread.ofVirtual().name("verify-" + var4).start(() -> OverlayCandidateRunner.this.verify(this, var1));
                  }
               }
            };
            BiFunction var7 = this.chatIds;
            return var2 == CandidateRunner.Purpose.ANSWER && var7 != null ? (CandidateRunner.Claim)var7.apply(var1, var6) : var6;
         }
      }
   }

   private void verify(CandidateRunner.Claim var1, CandidateRunner.Request var2) {
      String var3 = "completed";

      try {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("runId", var1.runId());
         int var5 = Failing.lineOf(var2.candidateBytes(), var2.failing());
         String var6 = var5 > 0 ? var2.test().path() + ":" + var5 : var2.test().path();
         Map var7 = this.runner.runCandidate(var6, var2.candidateBytes(), var4);
         var2.onDone().accept(this.outcome(var7, var2.test()));
      } catch (RuntimeException var12) {
         var3 = "failed";
         logger.warn("verification {} of {} failed: {}", new Object[]{var1.runId(), var2.test().path(), var12.getMessage()});
         var2.onDone().accept(CandidateRunner.Outcome.error(var12.getMessage()));
      } finally {
         this.slot.settleApiRun(var1.runId(), var3);
         Runnable var9 = this.afterSettle;
         if (var9 != null) {
            var9.run();
         }
      }
   }

   CandidateRunner.Outcome outcome(Map<String, Object> var1, TestId var2) {
      return outcome(var1, var2, this.root);
   }

   static CandidateRunner.Outcome outcome(Map<String, Object> var0, TestId var1, Path var2) {
      if (var0.get("code") != null && var0.get("error") != null) {
         return CandidateRunner.Outcome.error(String.valueOf(var0.get("error")));
      } else {
         Object var3 = var0.get("runId");
         int var4 = var0.get("failed") instanceof Number var5 ? var5.intValue() : 0;
         int var8 = var0.get("total") instanceof Number var9 ? var9.intValue() : 0;
         if (var8 == 0) {
            return CandidateRunner.Outcome.error(var0.get("error") != null ? String.valueOf(var0.get("error")) : "the run executed no scenario");
         } else if (var4 == 0) {
            return CandidateRunner.Outcome.green();
         } else {
            Map var10 = var3 == null ? null : Failing.of(var2.resolve("runs").resolve(String.valueOf(var3)), var1.path());
            return var10 == null ? CandidateRunner.Outcome.error(String.valueOf(var0.get("error"))) : CandidateRunner.Outcome.red(var10);
         }
      }
   }
}
