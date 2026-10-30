package io.karatelabs.match;

import io.karatelabs.common.Json;
import io.karatelabs.js.Context;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsRegex;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.Node;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Expect implements SimpleObject {
   private final Object subject;
   private final boolean negated;
   private final BiConsumer<Context, Result> onResult;
   private final boolean throwOnFailure;
   private final Supplier<Context> contextSupplier;
   private final SimpleObject expectTo;
   private final SimpleObject expectToBe;
   private final SimpleObject expectToBeAt;
   private final SimpleObject expectToDeep;
   private final SimpleObject expectToHave;
   private final SimpleObject expectToHaveAll;
   private final SimpleObject expectToHaveAny;
   private final SimpleObject expectToHaveNested;

   public Expect(Object subject) {
      this(subject, false, null, true, null);
   }

   public Expect(Object subject, BiConsumer<Context, Result> onResult) {
      this(subject, false, onResult, true, null);
   }

   public Expect(Object subject, boolean negated, BiConsumer<Context, Result> onResult, boolean throwOnFailure) {
      this(subject, negated, onResult, throwOnFailure, null);
   }

   public Expect(Object subject, boolean negated, BiConsumer<Context, Result> onResult, boolean throwOnFailure, Supplier<Context> contextSupplier) {
      this.subject = subject;
      this.negated = negated;
      this.onResult = onResult;
      this.throwOnFailure = throwOnFailure;
      this.contextSupplier = contextSupplier;
      this.expectToBeAt = this.initExpectToBeAt();
      this.expectToBe = this.initExpectToBe();
      this.expectToDeep = this.initExpectToDeep();
      this.expectToHaveAll = this.initExpectToHaveAll();
      this.expectToHaveAny = this.initExpectToHaveAny();
      this.expectToHaveNested = this.initExpectToHaveNested();
      this.expectToHave = this.initExpectToHave();
      this.expectTo = this.initExpectTo();
   }

   public static String getSourceFromContext(Context context) {
      if (context == null) {
         return null;
      } else {
         try {
            Node node = context.getNode();
            if (node != null) {
               node = node.getParent();
            }

            return node != null ? node.getTextIncludingWhitespace() : null;
         } catch (Exception var2) {
            return null;
         }
      }
   }

   private void handleResult(Context context, Result result) {
      if (this.negated) {
         result = result.pass ? Result.fail("expected condition to be false") : Result.PASS;
      }

      Context effectiveContext = context;
      if (context == null && this.contextSupplier != null) {
         effectiveContext = this.contextSupplier.get();
      }

      if (this.onResult != null) {
         this.onResult.accept(effectiveContext, result);
      }

      if (!result.pass && this.throwOnFailure) {
         String message = result.message;
         String source = getSourceFromContext(effectiveContext);
         if (source != null) {
            message = source + "\n" + message;
         }

         throw new AssertionError(message);
      }
   }

   private static Result evaluate(Object actual, Match.Type type, Object expected) {
      Result var4;
      try (Value value = Match.evaluate(actual, null, null)) {
         var4 = value.is(type, expected);
      }

      return var4;
   }

   private JavaCallable match(Match.Type type) {
      return (context, args) -> {
         Result result = evaluate(this.subject, type, args[0]);
         this.handleResult(context, result);
         return null;
      };
   }

   private JavaCallable matchChainable(Match.Type type) {
      return (context, args) -> {
         Result result = evaluate(this.subject, type, args[0]);
         this.handleResult(context, result);
         return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
      };
   }

   private JavaCallable handle(Function<Object, String> handler) {
      return (context, args) -> {
         String message = handler.apply(args[0]);
         Result result = message == null ? Result.PASS : Result.fail(message);
         this.handleResult(context, result);
         return null;
      };
   }

   private JavaCallable handleChainable(Function<Object, String> handler) {
      return (context, args) -> {
         String message = handler.apply(args[0]);
         Result result = message == null ? Result.PASS : Result.fail(message);
         this.handleResult(context, result);
         return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
      };
   }

   private SimpleObject initExpectToBeAt() {
      return key -> {
         return switch (key) {
            case "least" -> this.handle(rhs -> {
               if (this.subject instanceof Number actual && rhs instanceof Number expected) {
                  return actual.doubleValue() >= expected.doubleValue() ? null : this.subject + " is not at least: " + rhs;
               } else {
                  return this.subject + " is not at least: " + rhs;
               }
            });
            case "most" -> this.handle(rhs -> {
               if (this.subject instanceof Number actual && rhs instanceof Number expected) {
                  return actual.doubleValue() <= expected.doubleValue() ? null : this.subject + " is not at most: " + rhs;
               } else {
                  return this.subject + " is not at most: " + rhs;
               }
            });
            default -> throw new RuntimeException("expect().to.be.at - no such api: " + key);
         };
      };
   }

   private SimpleObject initExpectToBe() {
      return key -> {
         return switch (key) {
            case "not" -> (new Expect(this.subject, !this.negated, this.onResult, this.throwOnFailure, this.contextSupplier)).expectToBe;
            case "that", "and", "which" -> this;
            case "a", "an" -> this.handleChainable(rhs -> {
               String expected = rhs + "";
               String actual;
               if (this.subject instanceof List) {
                  actual = "array";
               } else {
                  actual = Terms.typeOf(this.subject);
               }

               return expected.equals(actual) ? null : "actual: " + actual + ", expected: " + expected;
            });
            case "true", "false", "_true", "_false" -> {
               String k = key;
               if (key.charAt(0) == '_') {
                  k = key.substring(1);
               }

               Result result = Boolean.valueOf(k).equals(this.subject) ? Result.PASS : Result.fail("actual: " + this.subject + ", expected: " + k);
               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "null", "_null" -> {
               Result result = this.subject == null ? Result.PASS : Result.fail("actual: " + this.subject + ", expected: null");
               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "undefined", "_undefined" -> {
               boolean isUndefined = this.subject == null || this.subject == Terms.UNDEFINED;
               Result result = isUndefined ? Result.PASS : Result.fail("actual: " + this.subject + ", expected: undefined");
               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "ok" -> {
               Result result = Terms.isTruthy(this.subject) ? Result.PASS : Result.fail("actual: " + this.subject + ", expected: (truthy)");
               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "empty" -> {
               Result result;
               if (this.subject instanceof String s) {
                  result = s.isEmpty() ? Result.PASS : Result.fail("actual: " + this.subject + ", expected: ''");
               } else if (this.subject instanceof Map<?, ?> map) {
                  result = map.isEmpty() ? Result.PASS : Result.fail("actual: map with " + map.size() + " items, expected: empty");
               } else if (this.subject instanceof List<?> list) {
                  result = list.isEmpty() ? Result.PASS : Result.fail("actual: list with " + list.size() + " items, expected: empty");
               } else {
                  result = Result.fail("actual: " + this.subject + ", expected: (empty)");
               }

               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "oneOf" -> (JavaCallable) (context, args) -> {
               Result resultx = evaluate(args[0], Match.Type.CONTAINS, this.subject);
               this.handleResult(context, resultx);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "above" -> this.handle(rhs -> {
               if (this.subject instanceof Number actual && rhs instanceof Number expected) {
                  return actual.doubleValue() > expected.doubleValue() ? null : this.subject + " is not above: " + rhs;
               } else {
                  return this.subject + " is not above: " + rhs;
               }
            });
            case "below" -> this.handle(rhs -> {
               if (this.subject instanceof Number actual && rhs instanceof Number expected) {
                  return actual.doubleValue() < expected.doubleValue() ? null : this.subject + " is not below: " + rhs;
               } else {
                  return this.subject + " is not below: " + rhs;
               }
            });
            case "within" -> (JavaCallable) (context, args) -> {
               Number actual = (Number)this.subject;
               Number first = (Number)args[0];
               Number second = (Number)args[1];
               Result resultx;
               if (actual.doubleValue() >= first.doubleValue() && actual.doubleValue() <= second.doubleValue()) {
                  resultx = Result.PASS;
               } else {
                  resultx = Result.fail(actual + " not within " + first + ", " + second);
               }

               this.handleResult(context, resultx);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "closeTo", "approximately" -> (JavaCallable) (context, args) -> {
               Number actual = (Number)this.subject;
               Number expected = (Number)args[0];
               Number delta = (Number)args[1];
               Result resultx;
               if (Math.abs(actual.doubleValue() - expected.doubleValue()) <= delta.doubleValue()) {
                  resultx = Result.PASS;
               } else {
                  resultx = Result.fail(actual + " not close to " + expected + " ± " + delta);
               }

               this.handleResult(context, resultx);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "at" -> this.expectToBeAt;
            default -> throw new RuntimeException("expect().to.be - no such api: " + key);
         };
      };
   }

   private SimpleObject initExpectToDeep() {
      return key -> {
         int tmp1$ = -1;
         switch (key.hashCode()) {
            case 1942574248:
               if (key.equals("include")) {
                  tmp1$ = 0;
               }
            default:
               switch (tmp1$) {
                  case 0:
                     return this.match(Match.Type.CONTAINS_DEEP);
                  default:
                     throw new RuntimeException("expect().to.deep - no such api: " + key);
               }
         }
      };
   }

   private SimpleObject initExpectToHaveAll() {
      return key -> {
         int tmp1$ = -1;
         switch (key.hashCode()) {
            case 3288564:
               if (key.equals("keys")) {
                  tmp1$ = 0;
               }
            default:
               switch (tmp1$) {
                  case 0:
                     return (JavaCallable)(context, args) -> {
                        Result result;
                        if (this.subject instanceof Map<?, ?> map) {
                           result = evaluate(map.keySet(), Match.Type.CONTAINS, args[0]);
                        } else {
                           result = Result.fail("not an object");
                        }

                        this.handleResult(context, result);
                        return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
                     };
                  default:
                     throw new RuntimeException("expect().to.have.all - no such api: " + key);
               }
         }
      };
   }

   private SimpleObject initExpectToHaveAny() {
      return key -> {
         int tmp1$ = -1;
         switch (key.hashCode()) {
            case 3288564:
               if (key.equals("keys")) {
                  tmp1$ = 0;
               }
            default:
               switch (tmp1$) {
                  case 0:
                     return (JavaCallable)(context, args) -> {
                        Result result;
                        if (this.subject instanceof Map<?, ?> map) {
                           result = evaluate(map.keySet(), Match.Type.CONTAINS_ANY, args[0]);
                        } else {
                           result = Result.fail("not an object");
                        }

                        this.handleResult(context, result);
                        return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
                     };
                  default:
                     throw new RuntimeException("expect().to.have.any - no such api: " + key);
               }
         }
      };
   }

   private SimpleObject initExpectToHaveNested() {
      return key -> {
         if ("property".equals(key)) {
            return (JavaCallable)(context, args) -> {
               String path = args[0] + "";

               Json json;
               try {
                  json = Json.of(this.subject);
               } catch (Exception var7) {
                  Result fail = Result.fail("expected to have property: " + path);
                  this.handleResult(context, fail);
                  return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
               }

               Result result;
               if (args.length > 1) {
                  Object actual = json.get(path, null);
                  result = evaluate(actual, Match.Type.EQUALS, args[1]);
               } else {
                  result = json.pathExists(path) ? Result.PASS : Result.fail("expected to have property: " + path);
               }

               this.handleResult(context, result);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
         } else {
            throw new RuntimeException("expect().to.have.nested - no such api: " + key);
         }
      };
   }

   private SimpleObject initExpectToHave() {
      return key -> {
         return switch (key) {
            case "not" -> (new Expect(this.subject, !this.negated, this.onResult, this.throwOnFailure, this.contextSupplier)).expectToHave;
            case "that", "and", "which" -> this;
            case "length" -> this.handleChainable(rhs -> {
               if (rhs instanceof Number n) {
                  String message = "actual: " + this.subject + ", expected: (length=" + n + ")";
                  if (this.subject instanceof String s) {
                     return s.length() == n.intValue() ? null : message;
                  } else if (this.subject instanceof Map<?, ?> map) {
                     return n.equals(map.get("length")) ? null : message;
                  } else if (this.subject instanceof List<?> l) {
                     return l.size() == n.intValue() ? null : message;
                  } else {
                     return message;
                  }
               } else {
                  return "not a number: " + rhs;
               }
            });
            case "property" -> (JavaCallable) (context, args) -> {
               String k = args[0] + "";
               Object propertyValue = null;
               Result result;
               if (this.subject instanceof Map<?, ?> map) {
                  if (args.length > 1) {
                     if (map.containsKey(k)) {
                        Object v = map.get(k);
                        propertyValue = v;
                        result = evaluate(v, Match.Type.EQUALS, args[1]);
                     } else {
                        result = Result.fail("property " + k + " not found");
                     }
                  } else if (map.containsKey(k)) {
                     propertyValue = map.get(k);
                     result = Result.PASS;
                  } else {
                     result = Result.fail("does not contain property: " + k);
                  }
               } else {
                  result = Result.fail("not an object");
               }

               this.handleResult(context, result);
               return new Expect(propertyValue != null ? propertyValue : this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "keys" -> (JavaCallable) (context, args) -> {
               Result result;
               if (this.subject instanceof Map<?, ?> map) {
                  result = evaluate(map.keySet(), Match.Type.CONTAINS_ONLY, args[0]);
               } else {
                  result = Result.fail("not an object");
               }

               this.handleResult(context, result);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "all" -> this.expectToHaveAll;
            case "any" -> this.expectToHaveAny;
            case "nested" -> this.expectToHaveNested;
            default -> throw new RuntimeException("expect().to.have - no such api: " + key);
         };
      };
   }

   private SimpleObject initExpectTo() {
      return key -> {
         return switch (key) {
            case "that", "and", "which" -> this;
            case "equal", "eql" -> this.match(Match.Type.EQUALS);
            case "include", "contain" -> this.matchChainable(Match.Type.CONTAINS);
            case "deep" -> this.expectToDeep;
            case "be" -> this.expectToBe;
            case "exist" -> {
               Result result = this.subject != null && this.subject != Terms.UNDEFINED
                  ? Result.PASS
                  : Result.fail("actual: " + this.subject + ", expected: (exists)");
               this.handleResult(null, result);
               yield new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            }
            case "have" -> this.expectToHave;
            case "match" -> (JavaCallable) (context, args) -> {
               Result resultx;
               if (this.subject instanceof String s) {
                  if (args[0] instanceof JsRegex regex) {
                     resultx = regex.test(s) ? Result.PASS : Result.fail("actual: " + s + ", expected: " + regex);
                  } else {
                     resultx = Result.fail("not a regex: " + args[0]);
                  }
               } else {
                  resultx = Result.fail("not a string");
               }

               this.handleResult(context, resultx);
               return new Expect(this.subject, false, this.onResult, this.throwOnFailure, this.contextSupplier);
            };
            case "not" -> (new Expect(this.subject, !this.negated, this.onResult, this.throwOnFailure, this.contextSupplier)).expectTo;
            default -> throw new RuntimeException("expect().to - no such api: " + key);
         };
      };
   }

   @Override
   public Object jsGet(String key) {
      if ("to".equals(key)) {
         return this.expectTo;
      } else if ("not".equals(key)) {
         return new Expect(this.subject, true, this.onResult, this.throwOnFailure, this.contextSupplier);
      } else if ("that".equals(key) || "and".equals(key) || "which".equals(key)) {
         return this;
      } else if ("be".equals(key) || "is".equals(key)) {
         return this.expectToBe;
      } else if ("have".equals(key) || "has".equals(key)) {
         return this.expectToHave;
      } else if ("include".equals(key) || "contain".equals(key)) {
         return this.matchChainable(Match.Type.CONTAINS);
      } else if (!"equal".equals(key) && !"eql".equals(key)) {
         throw new RuntimeException("expect() - no such api: " + key);
      } else {
         return this.match(Match.Type.EQUALS);
      }
   }

   public Object getSubject() {
      return this.subject;
   }

   public boolean isNegated() {
      return this.negated;
   }

   public BiConsumer<Context, Result> getOnResult() {
      return this.onResult;
   }

   public boolean isThrowOnFailure() {
      return this.throwOnFailure;
   }

   public Supplier<Context> getContextSupplier() {
      return this.contextSupplier;
   }
}
