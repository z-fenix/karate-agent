package io.karatelabs.ext.agent.runtime;

import io.karatelabs.js.Engine;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptExecutor implements ToolExecutor {
   private static final Logger a = LoggerFactory.getLogger(JavaScriptExecutor.class);
   private final Engine b;
   private volatile byte[] c;
   private static final int d = 200;
   private static final int e = 8000;
   private static final String f = ". To read it whole, end the block with the value as its last expression, not console.log(...): a returned value is not cut";
   private final List<String> g = new CopyOnWriteArrayList<>();
   private final UnaryOperator<String> h;

   public JavaScriptExecutor() {
      this(new Engine());
   }

   public JavaScriptExecutor(UnaryOperator<String> var1) {
      this(new Engine(), var1);
   }

   public JavaScriptExecutor(Engine var1) {
      this(var1, null);
   }

   private JavaScriptExecutor(Engine var1, UnaryOperator<String> var2) {
      this.b = var1;
      this.h = var2 != null ? var2 : UnaryOperator.identity();
      this.b
         .setOnConsoleLog(
            var1x -> {
               if (this.g.size() < 200) {
                  this.g.add(var1x);
               } else if (this.g.size() == 200) {
                  this.g
                     .add(
                        "⚠ console output truncated at 200 lines. To read it whole, end the block with the value as its last expression, not console.log(...): a returned value is not cut"
                     );
               }
            }
         );
   }

   private List<io.karatelabs.ext.agent.runtime.AgentArtifact> c() {
      ArrayList<io.karatelabs.ext.agent.runtime.AgentArtifact> var1 = new ArrayList();
      String var2 = this.d();
      if (!var2.isEmpty()) {
         var1.add(io.karatelabs.ext.agent.runtime.AgentArtifact.b(var2, "Console output"));
      }

      byte[] var3 = this.c;
      this.c = null;
      if (var3 != null && var3.length > 0) {
         var1.add(io.karatelabs.ext.agent.runtime.AgentArtifact.a(var3, "image/png", null));
      }

      return var1;
   }

   private String d() {
      if (this.g.isEmpty()) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder();

         for (String var3 : this.g) {
            String var4 = this.h.apply(var3);
            int var5 = 8000 - var1.length() - (var1.length() > 0 ? 1 : 0);
            if (var4.length() > var5) {
               if (var5 > 0) {
                  if (var1.length() > 0) {
                     var1.append('\n');
                  }

                  var1.append(var4, 0, var5);
               }

               var1.append("\n⚠ console output truncated at ")
                  .append(8000)
                  .append(" chars: a line of ")
                  .append(var4.length())
                  .append(" chars was cut")
                  .append(". To read it whole, end the block with the value as its last expression, not console.log(...): a returned value is not cut");
               break;
            }

            if (var1.length() > 0) {
               var1.append('\n');
            }

            var1.append(var4);
         }

         this.g.clear();
         return var1.toString();
      }
   }

   public void a(byte[] var1) {
      this.c = var1;
   }

   @Override
   public ToolResult a(String var1) throws io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException {
      String var2 = var1.replaceAll("(?m)//[^\n]*", "");
      if (!var2.contains(".then(") && !var2.contains("await ") && !var2.contains("async ")) {
         this.c = null;
         this.g.clear();

         try {
            Object var3 = this.b.eval(var1);
            if (Terms.isPlainObject(var3)) {
               var3 = ((ObjectLike)var3).toMap();
            }

            return new ToolResult(var3, this.c());
         } catch (Exception var5) {
            this.c = null;
            ControlSignal var4 = io.karatelabs.ext.agent.runtime.ControlSignal.a((Throwable)var5);
            if (var4 != null) {
               throw var4;
            } else {
               throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException(var5.getMessage(), var5, this.d());
            }
         }
      } else {
         throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException("All APIs are synchronous. Remove .then(), async, and await — calls return their result directly.", null, "");
      }
   }

   @Override
   public void a(String var1, Object var2) {
      this.b.putRootBinding(var1, var2);
   }

   @Override
   public Map<String, Object> a() {
      return this.b.getBindings();
   }

   public Engine b() {
      return this.b;
   }
}
