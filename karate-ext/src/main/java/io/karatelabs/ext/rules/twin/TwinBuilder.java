package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class TwinBuilder extends JsDispatch {
   private JavaCallable init;
   private final List<io.karatelabs.ext.rules.twin.TwinModel.StateModel> states = new ArrayList<>();
   private final List<io.karatelabs.ext.rules.twin.TwinModel.InvariantModel> invariants = new ArrayList<>();
   private final List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> commands = new ArrayList<>();
   private final Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> byId = new LinkedHashMap<>();
   private String clockField;
   private io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel readBack;
   private JavaCallable reset;
   private io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel resource;
   private final CalcFunctionHost gate = new CalcFunctionHost();

   TwinBuilder() {
      this.fn("calc", var1 -> this.gate.a(arg(var1, 0), arg(var1, 1)));
      this.fn("init", var1 -> this.init(arg(var1, 0)));
      this.fn("state", var1 -> this.state(text(arg(var1, 0)), arg(var1, 1)));
      this.fn("command", var1 -> this.command(text(arg(var1, 0)), arg(var1, 1)));
      this.fn("always", var1 -> this.always(text(arg(var1, 0)), arg(var1, 1)));
      this.fn("clock", var1 -> this.clock(arg(var1, 0)));
      this.fn("readBack", var1 -> this.readBack(arg(var1, 0)));
      this.fn("resource", var1 -> this.resource(arg(var1, 0)));
      this.fn("reset", var1 -> this.reset(arg(var1, 0)));
      this.fn("reject", var0 -> {
         throw new TwinRejectionException(text(arg(var0, 0)));
      });
   }

   TwinModel build(JavaCallable var1) {
      if (this.init == null) {
         throw TwinException.a(null, "init", "is required");
      } else if (this.reset != null && this.readBack == null) {
         throw TwinException.a(null, "reset", "requires readBack");
      } else {
         Map var2 = this.resource == null ? null : io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(this.init);
         TwinModel var3 = this.model(var1, List.copyOf(this.commands), Map.copyOf(this.byId), var2);
         io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var3);
         if (this.resource == null) {
            return var3;
         } else {
            ArrayList<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var4 = new ArrayList(this.commands.size());
            LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var5 = new LinkedHashMap();

            for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 : this.commands) {
               io.karatelabs.ext.rules.twin.TwinModel.CommandModel var8 = io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var7, this.resource);
               var4.add(var8);
               var5.put(var8.a(), var8);
            }

            return this.model(var1, List.copyOf(var4), Map.copyOf(var5), var2);
         }
      }
   }

   private TwinModel model(JavaCallable var1, List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var2, Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var3, Map<String, Object> var4) {
      return new TwinModel(
         this.init,
         List.copyOf(this.states),
         var2,
         List.copyOf(this.invariants),
         this.clockField,
         var3,
         var1,
         this.gate,
         this.readBack,
         this.reset,
         this.resource,
         var4
      );
   }

   private Object resource(Object var1) {
      if (this.resource != null) {
         throw TwinException.a(null, "resource", "duplicate resource — one aggregate per twin");
      } else {
         this.resource = io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var1);
         return this;
      }
   }

   private Object init(Object var1) {
      this.init = function(var1, null, "init");
      return this;
   }

   private Object state(String var1, Object var2) {
      if ("(unnamed)".equals(var1)) {
         throw TwinException.a(null, "state", "'(unnamed)' is reserved");
      } else {
         this.states.add(new io.karatelabs.ext.rules.twin.TwinModel.StateModel(var1, function(var2, null, "state", "predicate for '" + var1 + "'")));
         return this;
      }
   }

   private Object always(String var1, Object var2) {
      this.invariants.add(new io.karatelabs.ext.rules.twin.TwinModel.InvariantModel(var1, function(var2, null, "always", "predicate for '" + var1 + "'")));
      return this;
   }

   private Object clock(Object var1) {
      if (var1 instanceof String var2) {
         this.clockField = var2;
         return this;
      } else {
         throw TwinException.a(null, "clock", "must be a field name");
      }
   }

   private Object readBack(Object var1) {
      if (this.readBack != null) {
         throw TwinException.a(null, "readBack", "duplicate readBack");
      } else {
         Object var2 = member(var1, "request");
         Object var3 = member(var1, "observe");
         if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2) && var3 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var3)) {
            var var4 = names(null, "readBack.required", member(var1, "required"));
            var var5 = names(null, "readBack.oracle", member(var1, "oracle"));

            for (String var7 : (Iterable<String>) (Iterable<?>) (var5)) {
               if (!var4.contains(var7)) {
                  throw TwinException.a(null, "readBack.oracle", "'" + var7 + "' is not a read-back field — required declares " + var4);
               }
            }

            this.readBack = new io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel(
               function(var2, null, "readBack.request"),
               function(var3, null, "readBack.observe"),
               var4,
               var5,
               path(member(var1, "entity")),
               paths("readBack.volatile", member(var1, "volatile")),
               arrays(member(var1, "arrays"))
            );
            return this;
         } else {
            throw TwinException.a(null, "readBack", "requires both request and observe");
         }
      }
   }

   private static void grammar(String var0, String var1) {
      if (!io.karatelabs.ext.rules.twin.ObservationReconciler.a(var1)) {
         throw TwinException.a(
            null,
            var0,
            "'"
               + var1
               + "' is not a response path — rooted at 'body' or 'status', dotted or bracket-quoted (e.g. body.id, body[\"quote.id\"], body.items[0].sku)"
         );
      }
   }

   private static String path(Object var0) {
      if (var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         if (var0 instanceof String var1 && !var1.isEmpty()) {
            grammar("readBack.entity", var1);
            return var1;
         } else {
            throw TwinException.a(null, "readBack.entity", "must be a wire path such as 'body.id'");
         }
      } else {
         return null;
      }
   }

   private static List<String> paths(String var0, Object var1) {
      var var2 = names(null, var0, var1);

      for (String var4 : (Iterable<String>) (Iterable<?>) (var2)) {
         grammar(var0, var4);
      }

      return var2;
   }

   private static Map<String, String> arrays(Object var0) {
      if (var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         var var1 = object(var0);
         if (var1 == null) {
            throw TwinException.a(null, "readBack.arrays", "must be an object of identity keys keyed by array path");
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();

            for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
               if (((String)var4.getKey()).isEmpty() || !(var4.getValue() instanceof String var5) || var5.isEmpty()) {
                  throw TwinException.a(null, "readBack.arrays", "'" + (String)var4.getKey() + "' must name a non-empty identity key");
               }

               grammar("readBack.arrays", (String)var4.getKey());
               var2.put((String)var4.getKey(), var5);
            }

            return Collections.unmodifiableMap(var2);
         }
      } else {
         return Map.of();
      }
   }

   private Object reset(Object var1) {
      if (this.reset != null) {
         throw TwinException.a(null, "reset", "duplicate reset");
      } else {
         this.reset = function(var1, null, "reset");
         return this;
      }
   }

   private Object command(String var1, Object var2) {
      if (this.byId.containsKey(var1)) {
         throw TwinException.a(var1, "id", "duplicate command id");
      } else {
         Object var3 = member(var2, "apply");
         if (var3 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var3)) {
            Object var4 = member(var2, "args");
            List var5 = var4 instanceof JavaCallable var6 ? axes(var1, var6) : null;
            boolean var14 = var5 == null && var4 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var4);
            JavaCallable var7 = optionalFunction(member(var2, "request"), var1, "request");
            JavaCallable var8 = optionalFunction(member(var2, "observe"), var1, "observe");
            JavaCallable var9 = optionalFunction(member(var2, "capture"), var1, "capture");
            var var10 = names(var1, "captures", member(var2, "captures"));

            for (String var12 : (Iterable<String>) (Iterable<?>) (var10)) {
               if (io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var12) == null) {
                  throw TwinException.a(var1, "captures", "'" + var12 + "' is not a world path — a name, a dotted path, or [*] over an array (e.g. items[*].id)");
               }
            }

            if (var7 != null && var8 == null) {
               throw TwinException.a(var1, "observe", "is required when request is declared");
            } else if (!var10.isEmpty() && var9 == null) {
               throw TwinException.a(var1, "capture", "is required when captures names a field");
            } else if (var9 != null && var10.isEmpty()) {
               throw TwinException.a(var1, "captures", "is required when capture is declared");
            } else {
               JavaCallable var15 = optionalFunction(member(var2, "when"), var1, "when");
               boolean var16 = scopeGuard(var1, member(var2, "scopeGuard"), var15);
               io.karatelabs.ext.rules.twin.TwinModel.CommandModel var13 = new io.karatelabs.ext.rules.twin.TwinModel.CommandModel(
                  var1,
                  var15,
                  var16,
                  function(var3, var1, "apply"),
                  optionalFunction(member(var2, "then"), var1, "then"),
                  var5 == null ? this.argRows(var1, var4) : TwinModel.a(var5, null),
                  var5,
                  var14,
                  Boolean.TRUE.equals(member(var2, "idempotent")),
                  req(var1, member(var2, "req")),
                  var7,
                  var8,
                  var9,
                  var10,
                  refusals(var1, member(var2, "refusals")),
                  indefinite(var1, member(var2, "indefinite")),
                  io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var1, var2, var7),
                  example(var1, member(var2, "example"), var5)
               );
               this.commands.add(var13);
               this.byId.put(var1, var13);
               return this;
            }
         } else {
            throw TwinException.a(var1, "apply", "is required");
         }
      }
   }

   private static List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> axes(String var0, JavaCallable var1) {
      ArgBuilder var2 = new ArgBuilder(var0);
      var1.call(null, var2);
      return var2.axes();
   }

   private static Map<String, Object> example(String var0, Object var1, List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> var2) {
      if (var1 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
         return null;
      } else if (var2 == null) {
         throw TwinException.a(var0, "example", "needs the statement-form args it exemplifies");
      } else {
         try {
            io.karatelabs.ext.rules.twin.CanonicalJson.b(var1);
         } catch (SnapshotException var9) {
            throw TwinException.a(var0, "example", var9.getMessage());
         }

         if (io.karatelabs.ext.rules.twin.CanonicalJson.d(var1) instanceof Map var3) {
            LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();

            for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
               String var7 = String.valueOf(var6.getKey());
               io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var8 = var2.stream()
                  .filter(var1x -> var1x.a().equals(var7))
                  .findFirst()
                  .orElseThrow(() -> TwinException.a(var0, "example", "'" + var7 + "' is not a declared argument"));
               if (!TwinModel.a(var8, var6.getValue())) {
                  throw TwinException.a(var0, "example", io.karatelabs.ext.rules.twin.CanonicalJson.c(var6.getValue()) + " is not in the declared domain of '" + var7 + "'");
               }

               var10.put(var7, var6.getValue());
            }

            return Collections.unmodifiableMap(var10);
         } else {
            throw TwinException.a(var0, "example", "must be an object of argument values");
         }
      }
   }

   private List<Map<String, Object>> argRows(String var1, Object var2) {
      if (var2 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         return List.of(new LinkedHashMap<>());
      } else if (!(var2 instanceof List var3)) {
         throw TwinException.a(var1, "args", "must be a function or an array of argument objects");
      } else {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList(var3.size());

         for (int var5 = 0; var5 < var3.size(); var5++) {
            try {
               io.karatelabs.ext.rules.twin.CanonicalJson.b(var3.get(var5));
            } catch (SnapshotException var8) {
               throw TwinException.a(var1, "args", "row " + var5 + ": " + var8.getMessage());
            }

            if (!(io.karatelabs.ext.rules.twin.CanonicalJson.d(var3.get(var5)) instanceof Map var7)) {
               throw TwinException.a(var1, "args", "row " + var5 + " must be an object");
            }

            var4.add(var7);
         }

         return var4;
      }
   }

   private static List<String> names(String var0, String var1, Object var2) {
      if (var2 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         return List.of();
      } else if (!(var2 instanceof List var3)) {
         throw TwinException.a(var0, var1, "must be an array of field names");
      } else {
         ArrayList<java.lang.String> var4 = new ArrayList(var3.size());
         Iterator var5 = var3.iterator();

         while (var5.hasNext()) {
            if (!(var5.next() instanceof String var7) || var7.isEmpty()) {
               throw TwinException.a(var0, var1, "must be an array of field names");
            }

            var4.add(var7);
         }

         return List.copyOf(var4);
      }
   }

   private static boolean scopeGuard(String var0, Object var1, JavaCallable var2) {
      if (var1 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
         return false;
      } else if (var1 instanceof Boolean var3) {
         if (var3 && var2 == null) {
            throw TwinException.a(var0, "scopeGuard", "declares what 'when' rejects — declare when");
         } else {
            return var3;
         }
      } else {
         throw TwinException.a(var0, "scopeGuard", "must be true or false");
      }
   }

   private static String req(String var0, Object var1) {
      if (var1 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
         if (var1 instanceof String var2 && !var2.isEmpty()) {
            return var2;
         } else {
            throw TwinException.a(var0, "req", "must be an acceptance-criterion id");
         }
      } else {
         return null;
      }
   }

   private static Map<String, String> refusals(String var0, Object var1) {
      var var2 = object(var1);
      if (var2 == null) {
         if (var1 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
            throw TwinException.a(var0, "refusals", "must be an object of reasons keyed by kind");
         } else {
            return Map.of();
         }
      } else {
         LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (!(var5.getValue() instanceof String var6)) {
               throw TwinException.a(var0, "refusals", "'" + (String)var5.getKey() + "' must be a string");
            }

            var3.put((String)var5.getKey(), var6);
         }

         return Collections.unmodifiableMap(var3);
      }
   }

   private static List<String> indefinite(String var0, Object var1) {
      var var2 = object(var1);
      if (var2 == null) {
         if (var1 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
            throw TwinException.a(var0, "indefinite", "must be an object { transportFailure: [...] }");
         } else {
            return List.of();
         }
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (!"transportFailure".equals(var5.getKey())) {
               throw TwinException.a(var0, "indefinite", "'" + (String)var5.getKey() + "' is unknown — the only key is transportFailure");
            }

            if (!(var5.getValue() instanceof List var6) || var6.isEmpty()) {
               throw TwinException.a(var0, "indefinite.transportFailure", "must be a non-empty array of 'applied' or 'unchanged'");
            }

            for (Object var8 : var6) {
               if (!"applied".equals(var8) && !"unchanged".equals(var8)) {
                  throw TwinException.a(var0, "indefinite.transportFailure", "must be a non-empty array of 'applied' or 'unchanged'");
               }

               if (!var3.contains(var8)) {
                  var3.add((String)var8);
               }
            }
         }

         return List.copyOf(var3);
      }
   }

   static Map<String, Object> object(Object var0) {
      if (var0 instanceof JavaCallable) {
         return null;
      } else if (Terms.isPlainObject(var0)) {
         return ((ObjectLike)var0).toMap();
      } else if (var0 instanceof Map var1) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var1.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), var2x));
         return var2;
      } else {
         return null;
      }
   }

   private static JavaCallable optionalFunction(Object var0, String var1, String var2) {
      return var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0) ? function(var0, var1, var2) : null;
   }

   static JavaCallable function(Object var0, String var1, String var2) {
      return function(var0, var1, var2, null);
   }

   private static JavaCallable function(Object var0, String var1, String var2, String var3) {
      if (var0 instanceof JavaCallable var4) {
         return var4;
      } else {
         throw TwinException.a(var1, var2, (var3 == null ? "" : var3 + " ") + "must be a function");
      }
   }

   static Object member(Object var0, String var1) {
      if (var0 instanceof ObjectLike var3) {
         return var3.getMember(var1);
      } else {
         return var0 instanceof Map var2 ? var2.get(var1) : null;
      }
   }

   private static Object arg(Object[] var0, int var1) {
      return var0 != null && var0.length > var1 ? var0[var1] : null;
   }

   private static String text(Object var0) {
      return var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0) ? String.valueOf(var0) : null;
   }
}
