package io.karatelabs.match;

import io.karatelabs.js.Context;
import io.karatelabs.js.Engine;
import java.util.function.BiConsumer;

public class Match {
   public static Value evaluate(Object actual, Context context, BiConsumer<Context, Result> onResult) {
      return new Value(Value.parseIfJsonOrXmlString(actual), context, onResult);
   }

   public static Value that(Object actual) {
      return new Value(Value.parseIfJsonOrXmlString(actual), null, (context, result) -> {
         if (!result.pass) {
            throw new RuntimeException(result.message);
         }
      });
   }

   public static Result execute(Engine engine, Match.Type matchType, Object actual, Object expected) {
      return execute(engine, matchType, actual, expected, false);
   }

   public static Result execute(Engine engine, Match.Type matchType, Object actual, Object expected, boolean matchEachEmptyAllowed) {
      return execute(engine, null, matchType, actual, expected, matchEachEmptyAllowed);
   }

   public static Result execute(Engine engine, MarkerPolicy policy, Match.Type matchType, Object actual, Object expected, boolean matchEachEmptyAllowed) {
      Result var9;
      try (
         Value actualValue = new Value(actual);
         Value expectedValue = new Value(expected);
      ) {
         Operation op = new Operation(engine, policy, matchType, actualValue, expectedValue, matchEachEmptyAllowed);
         op.execute();
         var9 = op.getResult();
      }

      return var9;
   }

   public static Result execute(Engine engine, Match.Type matchType, Object actual, Object expected, long memoryThreshold) {
      Result var9;
      try (
         Value actualValue = new Value(actual, null, null, true, memoryThreshold);
         Value expectedValue = new Value(expected, null, null, true, memoryThreshold);
      ) {
         Operation op = new Operation(engine, matchType, actualValue, expectedValue);
         op.execute();
         var9 = op.getResult();
      }

      return var9;
   }

   public static enum Type {
      EQUALS,
      NOT_EQUALS,
      CONTAINS,
      NOT_CONTAINS,
      CONTAINS_ONLY,
      CONTAINS_ANY,
      CONTAINS_DEEP,
      CONTAINS_ONLY_DEEP,
      CONTAINS_ANY_DEEP,
      EACH_EQUALS,
      EACH_NOT_EQUALS,
      EACH_CONTAINS,
      EACH_NOT_CONTAINS,
      EACH_CONTAINS_ONLY,
      EACH_CONTAINS_ANY,
      EACH_CONTAINS_DEEP,
      WITHIN,
      NOT_WITHIN;
   }
}
