
package io.karatelabs.js;

import io.karatelabs.js.JavaInvokable;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.ParserException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class ContextRoot implements Context {
   static final JsBuiltinMethod PARSE_INT = new JsBuiltinMethod("parseInt", 2, (ctx, args) -> {
      if (args.length == 0) {
         return Double.NaN;
      } else {
         int radix = args.length > 1 && args[1] != Terms.UNDEFINED ? Terms.objectToNumber(args[1]).intValue() : 0;
         return Terms.parseInt(argToString(args[0]), radix);
      }
   });
   static final JsBuiltinMethod PARSE_FLOAT = new JsBuiltinMethod(
      "parseFloat", 1, (ctx, args) -> args.length == 0 ? Double.NaN : Terms.parseFloat(argToString(args[0]), false)
   );
   private final Engine engine;
   final BindingsStore bindings;
   final Object thisObject;
   private final long nanoOrigin = System.nanoTime();
   private final double timeOrigin = System.currentTimeMillis();
   Consumer<String> onConsoleLog;
   ContextListener listener;
   ExternalBridge bridge;
   RunInterceptor<?> interceptor;
   DebugPointFactory<?> pointFactory;
   short evalId;
   private Map<String, JsFunction> builtinConstructors;

   private static String argToString(Object arg) {
      return arg instanceof Number n ? Terms.numberToString(n) : arg + "";
   }

   ContextRoot(Engine engine) {
      this.engine = engine;
      this.bindings = engine.bindings;
      this.thisObject = new JsGlobalThis(this);
   }

   void setOnConsoleLog(Consumer<String> onConsoleLog) {
      this.onConsoleLog = onConsoleLog;
   }

   BindingSlot resolveOrInit(String key) {
      BindingSlot s = this.bindings.getSlot(key);
      if (s != null) {
         return s;
      } else {
         Object global = this.initGlobal(key);
         if (global != null) {
            this.bindings.putHidden(key, global);
            return this.bindings.getSlot(key);
         } else {
            return null;
         }
      }
   }

   Object get(String key) {
      BindingSlot s = this.resolveOrInit(key);
      if (s == null) {
         return Terms.UNDEFINED;
      } else {
         Object v = s.value;
         return v instanceof JsLazy lz ? lz.get() : v;
      }
   }

   boolean hasKey(String key) {
      return this.bindings.hasMember(key) ? true : canInitGlobal(key);
   }

   private static boolean canInitGlobal(String key) {
      return switch (key) {
         case "console", "parseInt", "parseFloat", "encodeURIComponent", "decodeURIComponent", "encodeURI", "decodeURI", "undefined", "Array", "Date", "Error", "Function", "Infinity", "Java", "JSON", "Map", "Math", "NaN", "Number", "BigInt", "Boolean", "Object", "Promise", "RegExp", "Set", "String", "TypeError", "ReferenceError", "RangeError", "SyntaxError", "URIError", "EvalError", "AggregateError", "WeakMap", "WeakSet", "TextEncoder", "TextDecoder", "Uint8Array", "globalThis", "setTimeout", "clearTimeout", "structuredClone", "performance", "isNaN", "isFinite", "eval", "Symbol", "Reflect" -> true;
         default -> false;
      };
   }

   @Override
   public Engine getEngine() {
      return this.engine;
   }

   @Override
   public Context getParent() {
      return null;
   }

   @Override
   public ContextScope getScope() {
      return ContextScope.ROOT;
   }

   @Override
   public int getDepth() {
      return -1;
   }

   @Override
   public Node getNode() {
      return null;
   }

   @Override
   public String getPath() {
      return "ROOT";
   }

   @Override
   public int getIteration() {
      return -1;
   }

   @Override
   public Object getThisObject() {
      return this.thisObject;
   }

   @Override
   public CallInfo getCallInfo() {
      return null;
   }

   @Override
   public Object[] getCallArgs() {
      return null;
   }

   @Override
   public Object getReturnValue() {
      return null;
   }

   @Override
   public Object getErrorThrown() {
      return null;
   }

   @Override
   public ExitType getExitType() {
      return null;
   }

   @Override
   public String toString() {
      return "ROOT";
   }

   JsFunction builtinConstructor(String name) {
      if (this.builtinConstructors == null) {
         this.builtinConstructors = new HashMap<>();
      }

      JsFunction c = this.builtinConstructors.get(name);
      if (c == null) {
         c = (JsFunction)(switch (name) {
            case "Array" -> new JsArrayConstructor();
            case "BigInt" -> new JsBigIntConstructor();
            case "Boolean" -> new JsBooleanConstructor();
            case "Date" -> new JsDateConstructor();
            case "Function" -> new JsFunctionConstructor();
            case "Map" -> new JsMapConstructor();
            case "Number" -> new JsNumberConstructor();
            case "Object" -> new JsObjectConstructor();
            case "Promise" -> new JsPromiseConstructor();
            case "RegExp" -> new JsRegexConstructor();
            case "Set" -> new JsSetConstructor();
            case "String" -> new JsStringConstructor();
            case "WeakMap" -> new JsWeakMapConstructor();
            case "WeakSet" -> new JsWeakSetConstructor();
            case "Error" -> new JsErrorConstructor(JsErrorPrototype.ERROR, 1);
            case "TypeError" -> new JsErrorConstructor(JsErrorPrototype.TYPE_ERROR, 1);
            case "RangeError" -> new JsErrorConstructor(JsErrorPrototype.RANGE_ERROR, 1);
            case "SyntaxError" -> new JsErrorConstructor(JsErrorPrototype.SYNTAX_ERROR, 1);
            case "ReferenceError" -> new JsErrorConstructor(JsErrorPrototype.REFERENCE_ERROR, 1);
            case "URIError" -> new JsErrorConstructor(JsErrorPrototype.URI_ERROR, 1);
            case "EvalError" -> new JsErrorConstructor(JsErrorPrototype.EVAL_ERROR, 1);
            case "AggregateError" -> new JsErrorConstructor(JsErrorPrototype.AGGREGATE_ERROR, 2);
            default -> null;
         });
         if (c != null) {
            this.builtinConstructors.put(name, c);
         }
      }

      return c;
   }

   private Object initGlobal(String key) {
      return switch (key) {
         case "console" -> new JsConsole(this);
         case "parseInt" -> PARSE_INT;
         case "parseFloat" -> PARSE_FLOAT;
         case "isNaN" -> (JsInvokable) args -> {
            if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
               Number n = Terms.objectToNumber(args[0]);
               return n == null || Double.isNaN(n.doubleValue());
            } else {
               return true;
            }
         };
         case "isFinite" -> (JsInvokable) args -> {
            if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
               Number n = Terms.objectToNumber(args[0]);
               if (n == null) {
                  return false;
               } else {
                  double d = n.doubleValue();
                  return !Double.isNaN(d) && !Double.isInfinite(d);
               }
            } else {
               return false;
            }
         };
         case "encodeURIComponent" -> (JsInvokable) args -> {
            String encoded = URLEncoder.encode(Terms.toStringCoerce(args[0], null), StandardCharsets.UTF_8);
            return encoded.replace("+", "%20")
               .replace("%21", "!")
               .replace("%27", "'")
               .replace("%28", "(")
               .replace("%29", ")")
               .replace("%7E", "~")
               .replace("%2A", "*");
         };
         case "decodeURIComponent" -> (JsInvokable) args -> URLDecoder.decode(Terms.toStringCoerce(args[0], null), StandardCharsets.UTF_8);
         case "encodeURI" -> (JsInvokable) args -> {
            String encoded = URLEncoder.encode(Terms.toStringCoerce(args[0], null), StandardCharsets.UTF_8);
            return encoded.replace("+", "%20")
               .replace("%21", "!")
               .replace("%23", "#")
               .replace("%24", "$")
               .replace("%26", "&")
               .replace("%27", "'")
               .replace("%28", "(")
               .replace("%29", ")")
               .replace("%2A", "*")
               .replace("%2B", "+")
               .replace("%2C", ",")
               .replace("%2F", "/")
               .replace("%3A", ":")
               .replace("%3B", ";")
               .replace("%3D", "=")
               .replace("%3F", "?")
               .replace("%40", "@")
               .replace("%7E", "~");
         };
         case "decodeURI" -> (JsInvokable) args -> URLDecoder.decode(Terms.toStringCoerce(args[0], null), StandardCharsets.UTF_8);
         case "undefined" -> Terms.UNDEFINED;
         case "eval" -> (JsInvokable) args -> {
            if (args.length == 0) {
               return Terms.UNDEFINED;
            } else {
               Object src = args[0];
               if (src instanceof String s) {
                  try {
                     return this.engine.evalRaw(s);
                  } catch (ParserException var5) {
                     throw JsErrorException.syntaxError(var5.getMessage());
                  }
               } else {
                  return src;
               }
            }
         };
         case "Array", "Date", "Function", "Error", "Map", "Number", "BigInt", "Boolean", "Object", "Promise", "RegExp", "Set", "String", "TypeError", "ReferenceError", "RangeError", "SyntaxError", "URIError", "EvalError", "AggregateError", "WeakMap", "WeakSet" -> this.builtinConstructor(
            key
         );
         case "globalThis" -> this.thisObject;
         case "setTimeout" -> new JsBuiltinMethod("setTimeout", 2, AsyncSupport::setTimeout);
         case "clearTimeout" -> new JsBuiltinMethod("clearTimeout", 1, AsyncSupport::clearTimeout);
         case "Infinity" -> Double.POSITIVE_INFINITY;
         case "Java" -> new JsJava(this.bridge);
         case "JSON" -> new JsJson();
         case "Math" -> new JsMath();
         case "NaN" -> Double.NaN;
         case "performance" -> new JsPerformance(this.nanoOrigin, this.timeOrigin);
         case "structuredClone" -> new JsBuiltinMethod("structuredClone", 1, JsStructuredClone::call);
         case "TextDecoder" -> new JsTextDecoder();
         case "TextEncoder" -> new JsTextEncoder();
         case "Uint8Array" -> new JsUint8Array(0);
         case "Reflect" -> new JsReflect();
         case "Symbol" -> {
            JsObject sym = new ContextRoot.SymbolStandIn();

            for (String name : JsSymbol.WELL_KNOWN) {
               sym.putMember(name, new JsSymbol(JsSymbol.keyOf(name)));
            }

            yield sym;
         }
         default -> null;
      };
   }

   private static final class SymbolStandIn extends JsObject implements JsCallable {
      @Override
      public Object call(Context context, Object[] args) {
         Object description = args.length == 0 ? Terms.UNDEFINED : args[0];
         return JsSymbol.mint(description == Terms.UNDEFINED ? "" : Terms.toStringCoerce(description, context instanceof CoreContext cc ? cc : null));
      }
   }
}
