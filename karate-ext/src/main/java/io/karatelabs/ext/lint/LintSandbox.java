package io.karatelabs.ext.lint;

import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public final class LintSandbox implements AutoCloseable {
   public static final String TIMEOUT_ENV = "KARATE_LINT_TIMEOUT_MS";
   public static final long DEFAULT_TIMEOUT_MILLIS = 5000L;
   private static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper ERROR_FACTORY = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new LintException("a lint rule exceeded its " + var1 + "ms budget (override KARATE_LINT_TIMEOUT_MS)");
      }

      @Override
      public RuntimeException a(Throwable var1) {
         if (var1 instanceof LintException var3) {
            return var3;
         } else {
            String var2 = var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getClass().getSimpleName() + ": " + var1.getMessage();
            return new LintException("a lint rule threw: " + var2, var1);
         }
      }

      @Override
      public RuntimeException a() {
         return new LintException("lint interrupted");
      }
   };
   private final Engine engine;
   private final io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope executor;
   private final Object boundDocument;
   private final Map<String, JavaCallable> predicateCache = new HashMap<>();
   private final Map<String, JavaCallable> filterCache = new HashMap<>();
   static final List<String> FILTER_BINDINGS = List.of("_v", "_prop", "_parent", "_pprop", "_root", "_path");

   private LintSandbox(Object var1, ReadOnlyCoverage var2, long var3, boolean var5) {
      if (var2 == null) {
         throw new LintException(
            "a lint sandbox needs the read-only Coverage facade — the capability statement grants it unconditionally, so it must be bound unconditionally"
         );
      } else {
         this.engine = new Engine();
         this.boundDocument = var5 ? io.karatelabs.ext.common.SandboxExecutor.b(var1) : var1;
         this.engine.put("$", this.boundDocument);
         this.engine.put("Coverage", var2);
         this.engine.put("fn", new LintFns(() -> this.boundDocument));
         this.executor = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("lint-sandbox", var3);
      }
   }

   public static LintSandbox create(Object var0, ReadOnlyCoverage var1) {
      return new LintSandbox(var0, var1, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_LINT_TIMEOUT_MS", 5000L), true);
   }

   static LintSandbox createForPrewrappedDocument(Object var0, ReadOnlyCoverage var1, Long var2) {
      return new LintSandbox(var0, var1, var2 != null ? var2 : io.karatelabs.ext.common.SandboxExecutor.a("KARATE_LINT_TIMEOUT_MS", 5000L), false);
   }

   static LintSandbox createWithTimeout(Object var0, ReadOnlyCoverage var1, long var2) {
      return new LintSandbox(var0, var1, var2, true);
   }

   Object boundDocument() {
      return this.boundDocument;
   }

   public Map<String, Object> capabilities() {
      return io.karatelabs.ext.lint.SandboxCapabilityStatement.statement();
   }

   public Object evaluate(String var1) {
      return this.executor.a(() -> this.engine.eval(var1), ERROR_FACTORY);
   }

   <T> T runBounded(Callable<T> var1) {
      return this.executor.a(var1, ERROR_FACTORY);
   }

   Object evaluateSelector(LintRule var1, String var2) {
      try {
         return this.engine.eval(var2);
      } catch (RuntimeException var4) {
         throw new LintException("rule '" + var1.b() + "': its `given` selector failed — " + var4.getMessage(), var4);
      }
   }

   Object callPredicate(LintRule var1, Object... var2) {
      return this.compilePredicate(var1).call(null, var2);
   }

   private JavaCallable compilePredicate(LintRule var1) {
      return this.predicateCache.computeIfAbsent(var1.b(), var2 -> {
         Object var3 = this.engine.eval("(" + var1.r() + ")");
         if (var3 instanceof JavaCallable var4) {
            return var4;
         } else {
            throw new LintException("rule '" + var2 + "': `then` did not compile to a function — got " + (var3 == null ? "null" : var3.getClass().getSimpleName()));
         }
      });
   }

   io.karatelabs.ext.lint.JsonPathPlusSelector.FilterCompiler filterCompiler() {
      return var1 -> this.filterCache.computeIfAbsent(var1, var1x -> {
         if (this.engine.eval("(function(" + String.join(",", FILTER_BINDINGS) + "){ return (" + var1x + ") })") instanceof JavaCallable var3) {
            return var3;
         } else {
            throw new LintException("a jsonpath-plus filter did not compile: " + var1x);
         }
      });
   }

   public Object evaluatePredicate(LintRule var1, Object... var2) {
      return this.executor.a(() -> this.compilePredicate(var1).call(null, var2), ERROR_FACTORY);
   }

   @Override
   public void close() {
      this.executor.close();
   }
}
