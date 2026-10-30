package io.karatelabs.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {
   public static final Result PASS = new Result(true, null, List.of());
   public final String message;
   public final boolean pass;
   public final List<Result.Failure> failures;

   public static Result fail(String message) {
      return new Result(false, message, List.of());
   }

   public static Result fail(String message, List<Result.Failure> failures) {
      return new Result(false, message, failures);
   }

   Result(boolean pass, String message) {
      this(pass, message, List.of());
   }

   Result(boolean pass, String message, List<Result.Failure> failures) {
      this.pass = pass;
      this.message = message;
      this.failures = failures != null ? failures : List.of();
   }

   @Override
   public String toString() {
      return this.pass ? "[pass]" : this.message;
   }

   public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>(4);
      map.put("pass", this.pass);
      map.put("message", this.message);
      if (!this.failures.isEmpty()) {
         List<Map<String, Object>> failuresList = new ArrayList<>(this.failures.size());

         for (Result.Failure f : this.failures) {
            failuresList.add(f.toMap());
         }

         map.put("failures", failuresList);
      }

      return map;
   }

   public record Failure(String path, String reason, Value.Type actualType, Value.Type expectedType, Object actualValue, Object expectedValue, int depth) {
      public Map<String, Object> toMap() {
         Map<String, Object> map = new HashMap<>();
         map.put("path", this.path);
         map.put("reason", this.reason);
         map.put("actualType", this.actualType != null ? this.actualType.name() : null);
         map.put("expectedType", this.expectedType != null ? this.expectedType.name() : null);
         map.put("actualValue", this.actualValue);
         map.put("expectedValue", this.expectedValue);
         map.put("depth", this.depth);
         return map;
      }
   }
}
