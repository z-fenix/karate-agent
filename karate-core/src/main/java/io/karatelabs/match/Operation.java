package io.karatelabs.match;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Operation {
   static final String REGEX = "regex";
   static final int MAX_EACH_FAILURES = 5;
   final MatchContext context;
   final Match.Type type;
   final Value actual;
   final Value expected;
   final List<Operation> failures;
   final boolean matchEachEmptyAllowed;
   boolean pass = true;
   private String failReason;
   private Engine sandbox;
   private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("\\W_\\W|\\W_|_\\W");

   Operation(Match.Type type, Value actual, Value expected) {
      this(new Engine(), null, null, type, actual, expected, false);
   }

   Operation(Engine engine, Match.Type type, Value actual, Value expected) {
      this(engine, null, null, type, actual, expected, false);
   }

   Operation(Engine engine, MarkerPolicy policy, Match.Type type, Value actual, Value expected, boolean matchEachEmptyAllowed) {
      this(engine, policy, null, type, actual, expected, matchEachEmptyAllowed);
   }

   Operation(MatchContext context, Match.Type type, Value actual, Value expected, boolean matchEachEmptyAllowed) {
      this(null, null, context, type, actual, expected, matchEachEmptyAllowed);
   }

   private Operation(Engine engine, MarkerPolicy policy, MatchContext context, Match.Type type, Value actual, Value expected, boolean matchEachEmptyAllowed) {
      this.type = type;
      this.actual = actual;
      this.expected = expected;
      this.matchEachEmptyAllowed = matchEachEmptyAllowed;
      if (context == null) {
         if (engine == null) {
            engine = new Engine();
         }

         this.failures = new ArrayList<>();
         if (actual.isXml()) {
            this.context = new MatchContext(engine, policy, this, true, 0, "/", "", -1);
         } else {
            this.context = new MatchContext(engine, policy, this, false, 0, "$", "", -1);
         }
      } else {
         this.context = context;
         this.failures = context.root.failures;
      }
   }

   Engine sandbox() {
      if (this.sandbox == null) {
         this.sandbox = new Engine();
      }

      return this.sandbox;
   }

   private Object evalMarker(String marker, String expr, Object underscore) {
      Engine engine = this.context.engineFor(marker);
      engine.put("$", this.context.root.actual.getValue());
      engine.put("_", underscore);

      Object e;
      try {
         e = engine.eval(expr);
      } catch (RuntimeException var9) {
         throw engine == this.context.engine ? var9 : this.context.policy.untrustedFailure(var9);
      } finally {
         engine.remove("$");
         engine.remove("_");
      }

      return e;
   }

   private Match.Type fromMatchEach() {
      return switch (this.type) {
         case EACH_CONTAINS -> Match.Type.CONTAINS;
         case EACH_NOT_CONTAINS -> Match.Type.NOT_CONTAINS;
         case EACH_CONTAINS_ONLY -> Match.Type.CONTAINS_ONLY;
         case EACH_CONTAINS_ANY -> Match.Type.CONTAINS_ANY;
         case EACH_EQUALS -> Match.Type.EQUALS;
         case EACH_NOT_EQUALS -> Match.Type.NOT_EQUALS;
         case EACH_CONTAINS_DEEP -> Match.Type.CONTAINS_DEEP;
         default -> throw new RuntimeException("unexpected outer match type: " + this.type);
      };
   }

   private static Match.Type macroToMatchType(boolean each, String macro) {
      if (macro.startsWith("^^")) {
         return each ? Match.Type.EACH_CONTAINS_ONLY : Match.Type.CONTAINS_ONLY;
      } else if (macro.startsWith("^+")) {
         return each ? Match.Type.EACH_CONTAINS_DEEP : Match.Type.CONTAINS_DEEP;
      } else if (macro.startsWith("^*")) {
         return each ? Match.Type.EACH_CONTAINS_ANY : Match.Type.CONTAINS_ANY;
      } else if (macro.startsWith("^")) {
         return each ? Match.Type.EACH_CONTAINS : Match.Type.CONTAINS;
      } else if (macro.startsWith("!^")) {
         return each ? Match.Type.EACH_NOT_CONTAINS : Match.Type.NOT_CONTAINS;
      } else if (macro.startsWith("!<")) {
         return Match.Type.NOT_WITHIN;
      } else if (macro.startsWith("<")) {
         return Match.Type.WITHIN;
      } else if (macro.startsWith("!=")) {
         return each ? Match.Type.EACH_NOT_EQUALS : Match.Type.NOT_EQUALS;
      } else {
         return each ? Match.Type.EACH_EQUALS : Match.Type.EQUALS;
      }
   }

   private static int matchTypeToStartPos(Match.Type mt) {
      return switch (mt) {
         case EACH_CONTAINS, CONTAINS, WITHIN -> 1;
         case EACH_NOT_CONTAINS, EACH_CONTAINS_ONLY, EACH_CONTAINS_ANY, EACH_NOT_EQUALS, EACH_CONTAINS_DEEP, CONTAINS_ONLY, CONTAINS_DEEP, CONTAINS_ANY, NOT_CONTAINS, NOT_EQUALS, NOT_WITHIN -> 2;
         default -> 0;
      };
   }

   boolean execute() {
      switch (this.type) {
         case EACH_CONTAINS:
         case EACH_NOT_CONTAINS:
         case EACH_CONTAINS_ONLY:
         case EACH_CONTAINS_ANY:
         case EACH_EQUALS:
         case EACH_NOT_EQUALS:
         case EACH_CONTAINS_DEEP:
            if (!this.actual.isList()) {
               return this.fail("actual is not an array or list");
            } else {
               int count = this.actual.getListSize();
               if (count == 0 && !this.matchEachEmptyAllowed) {
                  return this.fail("match each failed, empty array / list");
               } else {
                  Match.Type nestedMatchType = this.fromMatchEach();
                  List<Integer> failedIndices = new ArrayList<>();
                  boolean stoppedEarly = false;

                  for (int i = 0; i < count; i++) {
                     Object o = this.actual.getListElement(i);
                     this.context.engine.put("_$", o);
                     Value element = new Value(o);

                     Operation mo;
                     try {
                        mo = new Operation(this.context.descend(i), nestedMatchType, element, this.expected, this.matchEachEmptyAllowed);
                        mo.execute();
                     } catch (Throwable var14) {
                        try {
                           element.close();
                        } catch (Throwable var13) {
                           var14.addSuppressed(var13);
                        }

                        throw var14;
                     }

                     element.close();
                     this.context.engine.remove("_$");
                     if (!mo.pass) {
                        failedIndices.add(i);
                        if (failedIndices.size() >= 5 && i < count - 1) {
                           stoppedEarly = true;
                           break;
                        }
                     }
                  }

                  if (!failedIndices.isEmpty()) {
                     String suffix = stoppedEarly ? " (stopped after 5 failures)" : "";
                     if (failedIndices.size() == 1) {
                        return this.fail("match each failed at index " + failedIndices.getFirst() + suffix);
                     }

                     return this.fail("match each failed at indices " + failedIndices + suffix);
                  }

                  return true;
               }
            }
         default:
            if (!this.actual.isNotPresent() || this.expected.isString() && this.expected.getAsString().startsWith("#")) {
               if (this.actual.type != this.expected.type) {
                  if (this.actual.isString() && this.expected.isXml() && isContainsType(this.type)) {
                     String expStr = Xml.toString(this.expected.getValue(), false);
                     Operation mox = new Operation(this.context, this.type, this.actual, new Value(expStr), this.matchEachEmptyAllowed);
                     mox.execute();
                     return mox.pass ? this.pass() : this.fail(mox.failReason);
                  }

                  switch (this.type) {
                     case CONTAINS_ONLY:
                     case CONTAINS_DEEP:
                     case CONTAINS_ANY:
                     case NOT_CONTAINS:
                     case CONTAINS:
                     case CONTAINS_ONLY_DEEP:
                     case CONTAINS_ANY_DEEP:
                        if (!this.expected.isList() && (!this.expected.isString() || !this.expected.isArrayObjectOrReference())) {
                           boolean var26;
                           try (Value wrapped = new Value(Collections.singletonList(this.expected.getValue()))) {
                              Operation mox = new Operation(this.context, this.type, this.actual, wrapped, this.matchEachEmptyAllowed);
                              mox.execute();
                              var26 = mox.pass ? this.pass() : this.fail(mox.failReason);
                           }

                           return var26;
                        }
                     case NOT_EQUALS:
                     default:
                        break;
                     case NOT_WITHIN:
                     case WITHIN:
                        if (!this.actual.isList() && (!this.actual.isString() || !this.actual.isArrayObjectOrReference())) {
                           boolean failedIndices;
                           try (Value wrapped = new Value(Collections.singletonList(this.actual.getValue()))) {
                              Operation mox = new Operation(this.context, this.type, wrapped, this.expected, this.matchEachEmptyAllowed);
                              mox.execute();
                              failedIndices = mox.pass ? this.pass() : this.fail(mox.failReason);
                           }

                           return failedIndices;
                        }
                  }

                  if (this.expected.isXml() && this.actual.isMap()) {
                     Operation mox = new Operation(
                        this.context, this.type, this.actual, new Value(Xml.toObject(this.expected.getValue(), true)), this.matchEachEmptyAllowed
                     );
                     mox.execute();
                     return mox.pass ? this.pass() : this.fail(mox.failReason);
                  }

                  if (!this.expected.isString()) {
                     return this.type == Match.Type.NOT_EQUALS ? this.pass() : this.fail("data types don't match");
                  }

                  String expStr = this.expected.getValue();
                  if (!expStr.startsWith("#")) {
                     return this.type == Match.Type.NOT_EQUALS ? this.pass() : this.fail("data types don't match");
                  }
               }

               if (this.expected.isString()) {
                  String expStr = this.expected.getValue();
                  if (expStr.startsWith("#")) {
                     return switch (this.type) {
                        case NOT_CONTAINS -> this.macroEqualsExpected(expStr) ? this.fail("actual contains expected") : this.pass();
                        case NOT_EQUALS -> this.macroEqualsExpected(expStr) ? this.fail("is equal") : this.pass();
                        default -> this.macroEqualsExpected(expStr) ? this.pass() : this.fail(null);
                     };
                  }
               }

               switch (this.type) {
                  case CONTAINS_ONLY:
                  case CONTAINS_DEEP:
                  case CONTAINS_ANY:
                  case CONTAINS:
                  case CONTAINS_ONLY_DEEP:
                  case CONTAINS_ANY_DEEP:
                     return this.actualContainsExpected() ? this.pass() : this.fail("actual does not contain expected");
                  case NOT_CONTAINS:
                     return this.actualContainsExpected() ? this.fail("actual contains expected") : this.pass();
                  case NOT_EQUALS:
                     return this.actualEqualsExpected() ? this.fail("is equal") : this.pass();
                  case NOT_WITHIN:
                     return this.actualWithinExpected() ? this.fail("actual is within expected") : this.pass();
                  case WITHIN:
                     return this.actualWithinExpected() ? this.pass() : this.fail("actual is not within expected");
                  case EQUALS:
                     return this.actualEqualsExpected() ? this.pass() : this.fail("not equal");
                  default:
                     throw new RuntimeException("unexpected match type: " + this.type);
               }
            } else {
               return this.fail("actual path does not exist");
            }
      }
   }

   private boolean macroEqualsExpected(String expStr) {
      boolean optional = expStr.startsWith("##");
      if (optional && this.actual.isNull()) {
         return true;
      } else {
         int minLength = optional ? 3 : 2;
         if (expStr.length() > minLength) {
            String macro = expStr.substring(minLength - 1);
            if (macro.startsWith("(") && macro.endsWith(")")) {
               macro = macro.substring(1, macro.length() - 1);
               Match.Type nestedType = macroToMatchType(false, macro);
               int startPos = matchTypeToStartPos(nestedType);
               macro = macro.substring(startPos);
               Object evalResult = this.evalMarker(expStr, macro, this.actual.getValue());
               boolean negated = nestedType == Match.Type.NOT_CONTAINS;
               Match.Type searchType = negated ? Match.Type.CONTAINS : nestedType;
               if (this.actual.isList() && evalResult instanceof Map && (searchType == Match.Type.CONTAINS || searchType == Match.Type.CONTAINS_ANY)) {
                  Value expectedValue = new Value(evalResult);
                  int failuresBeforeSearch = this.failures.size();

                  for (int i = 0; i < this.actual.getListSize(); i++) {
                     try (Value elem = new Value(this.actual.getListElement(i))) {
                        Operation mo = new Operation(this.context.descend(i), searchType, elem, expectedValue, this.matchEachEmptyAllowed);
                        if (mo.execute()) {
                           if (negated) {
                              return this.fail("an array element matches expected");
                           }

                           return true;
                        }
                     }
                  }

                  if (!negated) {
                     return this.fail("no array element matches expected");
                  }

                  while (this.failures.size() > failuresBeforeSearch) {
                     this.failures.removeLast();
                  }

                  return true;
               }

               boolean i;
               try (Value evalValue = new Value(evalResult)) {
                  Operation mo = new Operation(this.context, nestedType, this.actual, evalValue, this.matchEachEmptyAllowed);
                  i = mo.execute();
               }

               return i;
            }

            if (macro.startsWith("[")) {
               int closeBracketPos = macro.indexOf(93);
               if (closeBracketPos != -1) {
                  if (!this.actual.isList()) {
                     return this.fail("actual is not an array");
                  }

                  if (closeBracketPos > 1) {
                     String bracketContents = macro.substring(1, closeBracketPos);
                     List<Object> listAct = this.actual.getValue();
                     int listSize = listAct.size();
                     String sizeExpr;
                     if (this.containsPlaceholderUnderscore(bracketContents)) {
                        sizeExpr = bracketContents;
                     } else {
                        sizeExpr = bracketContents + " == _";
                     }

                     Object evalResultx = this.evalMarker(expStr, sizeExpr, listSize);
                     if (!Terms.isTruthy(evalResultx)) {
                        return this.fail("actual array length is " + listSize);
                     }
                  }

                  if (macro.length() > closeBracketPos + 1) {
                     macro = StringUtils.trimToNull(macro.substring(closeBracketPos + 1));
                     if (macro != null) {
                        if (macro.startsWith("(") && macro.endsWith(")")) {
                           macro = macro.substring(1, macro.length() - 1);
                        }

                        if (macro.startsWith("?")) {
                           macro = "#" + macro;
                        }

                        if (macro.startsWith("#")) {
                           Operation mo = new Operation(this.context, Match.Type.EACH_EQUALS, this.actual, new Value(macro), this.matchEachEmptyAllowed);
                           mo.execute();
                           return mo.pass ? this.pass() : this.fail("all array elements matched");
                        }

                        Match.Type nestedTypex = macroToMatchType(true, macro);
                        int startPosx = matchTypeToStartPos(nestedTypex);
                        macro = macro.substring(startPosx);
                        Object evalResultx = this.evalMarker(expStr, macro, this.actual.getValue());

                        boolean mo;
                        try (Value evalValue = new Value(evalResultx)) {
                           Operation mox = new Operation(this.context, nestedTypex, this.actual, evalValue, this.matchEachEmptyAllowed);
                           mo = mox.execute();
                        }

                        return mo;
                     }
                  }

                  return true;
               }
            } else {
               int questionPos = macro.indexOf(63);
               String validatorName = null;
               if (questionPos != -1 && !macro.startsWith("regex")) {
                  validatorName = macro.substring(0, questionPos);
                  if (macro.length() > questionPos + 1) {
                     macro = StringUtils.trimToEmpty(macro.substring(questionPos + 1));
                  } else {
                     macro = "";
                  }
               } else {
                  validatorName = macro;
                  macro = "";
               }

               validatorName = StringUtils.trimToNull(validatorName);
               if (validatorName != null) {
                  Validators.Validator validator = null;
                  if (validatorName.startsWith("regex")) {
                     String regex = validatorName.substring(5).trim();
                     validator = new Validators.RegexValidator(regex);
                  } else {
                     validator = Validators.VALIDATORS.get(validatorName);
                  }

                  if (validator != null) {
                     if (!optional || !this.actual.isNotPresent() && !this.actual.isNull()) {
                        if (!optional && this.actual.isNotPresent()) {
                           return this.expected.isNotPresent()
                              || "#ignore".contentEquals(this.expected.getAsString())
                              || this.fail("actual path does not exist");
                        }

                        Result mr = validator.apply(this.actual);
                        if (!mr.pass) {
                           return this.fail(mr.message);
                        }
                     }
                  } else if (!validatorName.startsWith("regex")) {
                     String actualValue = this.actual.getValue();
                     return this.type == Match.Type.CONTAINS
                        ? actualValue.contains(expStr) || this.fail("actual does not contain expected")
                        : actualValue.equals(expStr) || this.fail("not equal");
                  }
               }

               macro = StringUtils.trimToNull(macro);
               if (macro != null && questionPos != -1) {
                  Object evalResultx = this.evalMarker(expStr, macro, this.actual.getValue());
                  if (!Terms.isTruthy(evalResultx)) {
                     return this.fail("evaluated to 'false'");
                  }
               }
            }
         }

         return true;
      }
   }

   private boolean containsPlaceholderUnderscore(String bracketContents) {
      Matcher m1 = UNDERSCORE_PATTERN.matcher(bracketContents);
      return m1.find();
   }

   private boolean actualEqualsExpected() {
      switch (this.actual.type) {
         case NULL:
            return true;
         case BOOLEAN:
            boolean actBoolean = this.actual.<Boolean>getValue();
            boolean expBoolean = this.expected.<Boolean>getValue();
            return actBoolean == expBoolean;
         case NUMBER:
            BigDecimal actBigDecimal = toBigDecimal(this.actual.getValue());
            BigDecimal expBigDecimal = toBigDecimal(this.expected.getValue());
            return actBigDecimal.compareTo(expBigDecimal) == 0;
         case STRING:
            return this.actual.getValue().equals(this.expected.getValue());
         case BYTES:
            byte[] actBytes = this.actual.getValue();
            byte[] expBytes = this.expected.getValue();
            return Arrays.equals(actBytes, expBytes);
         case LIST:
            int actListCount = this.actual.getListSize();
            int expListCount = this.expected.getListSize();
            if (actListCount != expListCount) {
               return this.fail("actual array length is not equal to expected - " + actListCount + ":" + expListCount);
            } else {
               List<Integer> failedListIndices = new ArrayList<>();
               int i = 0;

               for (; i < actListCount; i++) {
                  try (
                     Value actListValue = new Value(this.actual.getListElement(i));
                     Value expListValue = new Value(this.expected.getListElement(i));
                  ) {
                     Operation mo = new Operation(this.context.descend(i), Match.Type.EQUALS, actListValue, expListValue, this.matchEachEmptyAllowed);
                     mo.execute();
                     if (!mo.pass) {
                        failedListIndices.add(i);
                     }
                  }
               }

               if (!failedListIndices.isEmpty()) {
                  return failedListIndices.size() == 1
                     ? this.fail("array match failed at index " + failedListIndices.getFirst())
                     : this.fail("array match failed at indices " + failedListIndices);
               } else {
                  return true;
               }
            }
         case MAP:
            Map<String, Object> actMap = this.actual.getValue();
            Map<String, Object> expMap = this.expected.getValue();
            return this.matchMapValues(actMap, expMap);
         case XML:
            Map<String, Object> actXml = (Map<String, Object>)Xml.toObject(this.actual.getValue(), true);
            Map<String, Object> expXml = (Map<String, Object>)Xml.toObject(this.expected.getValue(), true);
            return this.matchMapValues(actXml, expXml);
         case OTHER:
            return this.actual.getValue().equals(this.expected.getValue());
         default:
            throw new RuntimeException("unexpected type (match equals): " + this.actual.type);
      }
   }

   private boolean matchMapValues(Map<String, Object> actMap, Map<String, Object> expMap) {
      if (actMap.size() <= expMap.size()
         || this.type != Match.Type.EQUALS && this.type != Match.Type.CONTAINS_ONLY && this.type != Match.Type.CONTAINS_ONLY_DEEP) {
         Set<String> unMatchedKeysAct = new LinkedHashSet<>(actMap.keySet());
         Set<String> unMatchedKeysExp = new LinkedHashSet<>(expMap.keySet());
         List<String> missingKeys = new ArrayList<>();
         List<String> failedKeys = new ArrayList<>();

         for (Entry<String, Object> expEntry : expMap.entrySet()) {
            String key = expEntry.getKey();
            Object childExp = expEntry.getValue();
            if (!actMap.containsKey(key)) {
               if (childExp instanceof String childString
                  && (childString.startsWith("##") || childString.equals("#ignore") || childString.equals("#notpresent"))) {
                  if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
                     unMatchedKeysExp.remove(key);
                     if (!unMatchedKeysExp.isEmpty() || this.type != Match.Type.CONTAINS && this.type != Match.Type.CONTAINS_DEEP) {
                        continue;
                     }

                     return true;
                  }

                  return true;
               } else if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
                  missingKeys.add(key);
               }
            } else {
               Value childActValue = new Value(actMap.get(key));

               Operation mo;
               try (Value childExpValue = new Value(childExp)) {
                  Match.Type childMatchType;
                  if (this.type == Match.Type.CONTAINS_DEEP) {
                     childMatchType = childActValue.isMapOrListOrXml() ? Match.Type.CONTAINS_DEEP : Match.Type.EQUALS;
                  } else if (this.type == Match.Type.CONTAINS_ONLY_DEEP) {
                     childMatchType = childActValue.isMapOrListOrXml() ? Match.Type.CONTAINS_ONLY_DEEP : Match.Type.EQUALS;
                  } else {
                     childMatchType = Match.Type.EQUALS;
                  }

                  mo = new Operation(this.context.descend(key), childMatchType, childActValue, childExpValue, this.matchEachEmptyAllowed);
                  mo.execute();
               } catch (Throwable var19) {
                  try {
                     childActValue.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }

                  throw var19;
               }

               childActValue.close();
               if (!mo.pass) {
                  if (this.type == Match.Type.EQUALS) {
                     failedKeys.add(key);
                  }
               } else {
                  if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
                     unMatchedKeysExp.remove(key);
                     if (!unMatchedKeysExp.isEmpty() || this.type != Match.Type.CONTAINS && this.type != Match.Type.CONTAINS_DEEP) {
                        unMatchedKeysAct.remove(key);
                        continue;
                     }

                     return true;
                  }

                  return true;
               }
            }
         }

         if (!missingKeys.isEmpty()) {
            return missingKeys.size() == 1
               ? this.fail("actual does not contain key - '" + missingKeys.get(0) + "'")
               : this.fail("actual does not contain keys - " + missingKeys);
         } else if (!failedKeys.isEmpty()) {
            return failedKeys.size() == 1
               ? this.fail("match failed for name: '" + failedKeys.get(0) + "'")
               : this.fail("match failed for names: " + failedKeys);
         } else if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
            if (unMatchedKeysExp.isEmpty()) {
               if (this.type == Match.Type.CONTAINS || this.type == Match.Type.CONTAINS_DEEP) {
                  return true;
               }

               if (this.type == Match.Type.NOT_CONTAINS && !expMap.isEmpty()) {
                  return true;
               }
            }

            if (!unMatchedKeysExp.isEmpty()) {
               return this.fail("all key-values did not match, expected has un-matched keys - " + unMatchedKeysExp);
            } else {
               return !unMatchedKeysAct.isEmpty() ? this.fail("all key-values did not match, actual has un-matched keys - " + unMatchedKeysAct) : true;
            }
         } else {
            return unMatchedKeysExp.isEmpty() || this.fail("no key-values matched");
         }
      } else {
         int sizeDiff = actMap.size() - expMap.size();
         Map<String, Object> diffMap = new LinkedHashMap<>(actMap);

         for (String key : expMap.keySet()) {
            diffMap.remove(key);
         }

         return this.fail("actual has " + sizeDiff + " more key(s) than expected - " + Json.stringifyStrict(diffMap));
      }
   }

   private boolean actualContainsExpected() {
      switch (this.actual.type) {
         case STRING:
            String actString = this.actual.getValue();
            String expString = this.expected.getValue();
            return actString.contains(expString);
         case BYTES:
         default:
            throw new RuntimeException("unexpected type (match contains): " + this.actual.type);
         case LIST:
            int actListCount = this.actual.getListSize();
            int expListCount = this.expected.getListSize();
            boolean[] actVisitedList = new boolean[actListCount];
            if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP && expListCount > actListCount) {
               return this.fail("actual array length is less than expected - " + actListCount + ":" + expListCount);
            } else if ((this.type == Match.Type.CONTAINS_ONLY || this.type == Match.Type.CONTAINS_ONLY_DEEP) && expListCount != actListCount) {
               return this.fail("actual array length is not equal to expected - " + actListCount + ":" + expListCount);
            } else {
               List<String> notFoundItems = new ArrayList<>();

               for (int j = 0; j < expListCount; j++) {
                  boolean found = false;

                  try (Value expListValue = new Value(this.expected.getListElement(j))) {
                     int failuresBeforeSearch = this.failures.size();

                     for (int i = 0; i < actListCount; i++) {
                        try (Value actListValue = new Value(this.actual.getListElement(i))) {
                           Operation mo = new Operation(this.context.descend(i), switch (this.type) {
                              case CONTAINS_DEEP -> actListValue.isMapOrListOrXml() ? Match.Type.CONTAINS_DEEP : Match.Type.EQUALS;
                              case CONTAINS_ONLY_DEEP -> actListValue.isMapOrListOrXml() ? Match.Type.CONTAINS_ONLY_DEEP : Match.Type.EQUALS;
                              case CONTAINS_ANY_DEEP -> actListValue.isMapOrListOrXml() ? Match.Type.CONTAINS_ANY : Match.Type.EQUALS;
                              default -> Match.Type.EQUALS;
                           }, actListValue, expListValue, this.matchEachEmptyAllowed);
                           mo.execute();
                           if (mo.pass) {
                              if (this.type == Match.Type.CONTAINS_ANY || this.type == Match.Type.CONTAINS_ANY_DEEP) {
                                 return true;
                              }

                              if (this.type != Match.Type.CONTAINS_ONLY && this.type != Match.Type.CONTAINS_ONLY_DEEP) {
                                 found = true;
                                 break;
                              }

                              if (!actVisitedList[i]) {
                                 actVisitedList[i] = true;
                                 found = true;
                                 break;
                              }
                           }
                        }
                     }

                     if (found) {
                        while (this.failures.size() > failuresBeforeSearch) {
                           this.failures.removeLast();
                        }
                     }

                     if (!found && this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
                        notFoundItems.add(expListValue.getAsString());
                     }
                  }
               }

               if (!notFoundItems.isEmpty()) {
                  if (notFoundItems.size() == 1) {
                     return this.fail("actual array does not contain expected item - " + notFoundItems.get(0));
                  }

                  return this.fail("actual array does not contain expected items - " + notFoundItems);
               } else {
                  if (this.type != Match.Type.CONTAINS_ANY && this.type != Match.Type.CONTAINS_ANY_DEEP) {
                     return true;
                  }

                  return this.fail("actual array does not contain any of the expected items");
               }
            }
         case MAP:
            Map<String, Object> actMap = this.actual.getValue();
            Map<String, Object> expMap = this.expected.getValue();
            return this.matchMapValues(actMap, expMap);
         case XML:
            Map<String, Object> actXml = (Map<String, Object>)Xml.toObject(this.actual.getValue());
            Map<String, Object> expXml = (Map<String, Object>)Xml.toObject(this.expected.getValue());
            return this.matchMapValues(actXml, expXml);
      }
   }

   private boolean actualWithinExpected() {
      switch (this.actual.type) {
         case STRING:
            String actString = this.actual.getValue();
            String expString = this.expected.getValue();
            return expString.contains(actString);
         case BYTES:
         default:
            throw new RuntimeException("unexpected type (match within): " + this.actual.type);
         case LIST:
            int actListCount = this.actual.getListSize();
            int expListCount = this.expected.getListSize();
            if (actListCount > expListCount) {
               return this.fail("actual array length is greater than expected - " + actListCount + ":" + expListCount);
            } else {
               List<String> notFoundItems = new ArrayList<>();
               int i = 0;

               for (; i < actListCount; i++) {
                  boolean found = false;

                  try (Value actListValue = new Value(this.actual.getListElement(i))) {
                     int failuresBeforeSearch = this.failures.size();

                     for (int j = 0; j < expListCount; j++) {
                        try (Value expListValue = new Value(this.expected.getListElement(j))) {
                           Operation mox = new Operation(this.context.descend(i), Match.Type.EQUALS, actListValue, expListValue, this.matchEachEmptyAllowed);
                           mox.execute();
                           if (mox.pass) {
                              found = true;
                              break;
                           }
                        }
                     }

                     while (this.failures.size() > failuresBeforeSearch) {
                        this.failures.removeLast();
                     }

                     if (!found) {
                        notFoundItems.add(actListValue.getAsString());
                     }
                  }
               }

               if (!notFoundItems.isEmpty()) {
                  return notFoundItems.size() == 1
                     ? this.fail("expected does not contain actual item - " + notFoundItems.get(0))
                     : this.fail("expected does not contain actual items - " + notFoundItems);
               } else {
                  return true;
               }
            }
         case MAP:
            Map<String, Object> actMap = this.actual.getValue();
            Map<String, Object> expMap = this.expected.getValue();
            List<String> missingKeys = new ArrayList<>();
            List<String> failedKeys = new ArrayList<>();

            for (Entry<String, Object> actEntry : actMap.entrySet()) {
               String key = actEntry.getKey();
               if (!expMap.containsKey(key)) {
                  missingKeys.add(key);
               } else {
                  try (
                     Value actValue = new Value(actEntry.getValue());
                     Value expValue = new Value(expMap.get(key));
                  ) {
                     Operation mox = new Operation(this.context.descend(key), Match.Type.EQUALS, actValue, expValue, this.matchEachEmptyAllowed);
                     mox.execute();
                     if (!mox.pass) {
                        failedKeys.add(key);
                     }
                  }
               }
            }

            if (!missingKeys.isEmpty()) {
               if (missingKeys.size() == 1) {
                  return this.fail("expected does not contain key - '" + missingKeys.getFirst() + "'");
               }

               return this.fail("expected does not contain keys - " + missingKeys);
            } else {
               if (!failedKeys.isEmpty()) {
                  if (failedKeys.size() == 1) {
                     return this.fail("match failed for name: '" + failedKeys.getFirst() + "'");
                  }

                  return this.fail("match failed for names: " + failedKeys);
               }

               return true;
            }
         case XML:
            Map<String, Object> actXml = (Map<String, Object>)Xml.toObject(this.actual.getValue());
            Map<String, Object> expXml = (Map<String, Object>)Xml.toObject(this.expected.getValue());
            Operation mo = new Operation(this.context, Match.Type.WITHIN, new Value(actXml), new Value(expXml), this.matchEachEmptyAllowed);
            mo.execute();
            return mo.pass;
      }
   }

   private static BigDecimal toBigDecimal(Object o) {
      if (o instanceof BigDecimal bd) {
         return bd;
      } else if (o instanceof BigInteger bi) {
         return new BigDecimal(bi);
      } else if (o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte) {
         return BigDecimal.valueOf(((Number)o).longValue());
      } else if (o instanceof Number n) {
         return BigDecimal.valueOf(n.doubleValue());
      } else {
         throw new RuntimeException("expected number instead of: " + o);
      }
   }

   boolean pass() {
      this.pass = true;
      return true;
   }

   boolean fail(String reason) {
      this.pass = false;
      if (reason == null) {
         return false;
      } else {
         this.failReason = this.failReason == null ? reason : reason + " | " + this.failReason;
         this.context.root.failures.add(this);
         return false;
      }
   }

   String getFailureReasons() {
      return collectFailureReasons(this);
   }

   Result getResult() {
      if (this.pass) {
         return Result.PASS;
      } else {
         List<Result.Failure> structuredFailures = collectStructuredFailures(this);
         String message = collectFailureReasons(this);
         return Result.fail(message, structuredFailures);
      }
   }

   private static List<Result.Failure> collectStructuredFailures(Operation root) {
      List<Result.Failure> result = new ArrayList<>();
      List<Operation> failuresCopy = new ArrayList<>(root.failures);
      Collections.reverse(failuresCopy);
      Set<String> previousPaths = new HashSet<>();

      for (Operation mo : failuresCopy) {
         if (!previousPaths.contains(mo.context.path) && !mo.isXmlAttributeOrMap()) {
            previousPaths.add(mo.context.path);
            result.add(
               new Result.Failure(
                  mo.context.path, mo.failReason, mo.actual.type, mo.expected.type, mo.actual.getValue(), mo.expected.getValue(), mo.context.depth
               )
            );
         }
      }

      return result;
   }

   private boolean isXmlAttributeOrMap() {
      return this.context.xml && this.actual.isMap() && (this.context.name.equals("@") || this.actual.<Map>getValue().containsKey("_"));
   }

   private static String collectFailureReasons(Operation root) {
      StringBuilder sb = new StringBuilder();
      sb.append("match failed: ").append(root.type).append('\n');
      Collections.reverse(root.failures);
      Set<String> previousPaths = new HashSet<>();
      List<Operation> visible = new ArrayList<>();

      for (Operation mo : root.failures) {
         if (!previousPaths.contains(mo.context.path) && !mo.isXmlAttributeOrMap()) {
            previousPaths.add(mo.context.path);
            visible.add(mo);
         }
      }

      TreeSet<Integer> uniqueDepths = new TreeSet<>();

      for (Operation mox : visible) {
         uniqueDepths.add(mox.context.depth);
      }

      Map<Integer, Integer> depthRank = new HashMap<>();
      int rank = 0;

      for (int d : uniqueDepths) {
         depthRank.put(d, rank++);
      }

      Iterator<Operation> iterator = visible.iterator();

      while (iterator.hasNext()) {
         Operation mox = iterator.next();
         String prefix = StringUtils.repeat(' ', (depthRank.get(mox.context.depth) + 1) * 2);
         sb.append(prefix).append(mox.context.path).append(" | ").append(mox.failReason);
         sb.append(" (").append(mox.actual.type).append(':').append(mox.expected.type).append(")");
         sb.append('\n');
         if (mox.context.xml) {
            sb.append(prefix).append(mox.actual.getAsXmlString()).append('\n');
            sb.append(prefix).append(mox.expected.getAsXmlString()).append('\n');
         } else {
            Value expected = mox.expected.getSortedLike(mox.actual);
            sb.append(prefix).append(mox.actual.getWithinSingleQuotesIfString()).append('\n');
            sb.append(prefix).append(expected.getWithinSingleQuotesIfString()).append('\n');
         }

         if (iterator.hasNext()) {
            sb.append('\n');
         }
      }

      return sb.toString();
   }

   private static boolean isContainsType(Match.Type t) {
      return t == Match.Type.CONTAINS
         || t == Match.Type.NOT_CONTAINS
         || t == Match.Type.CONTAINS_ANY
         || t == Match.Type.CONTAINS_ONLY
         || t == Match.Type.CONTAINS_DEEP
         || t == Match.Type.CONTAINS_ONLY_DEEP
         || t == Match.Type.CONTAINS_ANY_DEEP;
   }
}
