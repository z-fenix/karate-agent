package io.karatelabs.ext.max.apitest;

import java.util.Map;
import java.util.function.Consumer;

public interface CandidateRunner {
   CandidateRunner NONE = (var0, var1) -> {
      throw new CandidateRunner.Refused("UNAVAILABLE", "the side chat cannot start a run on this console");
   };

   CandidateRunner.Claim claim(TestId var1, CandidateRunner.Purpose var2) throws CandidateRunner.Refused;

   public interface Claim {
      String runId();

      default String chat() {
         return null;
      }

      default Integer turn() {
         return null;
      }

      void release();

      void start(CandidateRunner.Request var1);
   }

   public record Outcome(String status, Map<String, Object> failing, String error) {


      public static CandidateRunner.Outcome green() {
         return new CandidateRunner.Outcome("green", null, null);
      }

      public static CandidateRunner.Outcome red(Map<String, Object> var0) {
         return new CandidateRunner.Outcome("red", var0, null);
      }

      public static CandidateRunner.Outcome error(String var0) {
         return new CandidateRunner.Outcome("error", null, var0);
      }

      public Integer step() {
         return this.failing != null && this.failing.get("step") instanceof Number var1 ? var1.intValue() : null;
      }
   }

   public static enum Purpose {
      ANSWER,
      VERIFY;
   }

   public static final class Refused extends Exception {
      public final String code;

      public Refused(String var1, String var2) {
         super(var2);
         this.code = var1;
      }
   }

   public record Request(TestId test, byte[] candidateBytes, String base, String candidate, CandidateRunner.Purpose purpose, Map<String, Object> answer, Consumer<CandidateRunner.Outcome> onDone, Map<String, Object> failing) {

      public Request(
         TestId var1, byte[] var2, String var3, String var4, CandidateRunner.Purpose var5, Map<String, Object> var6, Consumer<CandidateRunner.Outcome> var7
      ) {
         this(var1, var2, var3, var4, var5, var6, var7, null);
      }

   }
}
