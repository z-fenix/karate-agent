package io.karatelabs.ext.robot;

import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RobotAgentExtension implements AgentExtension {
   private final RobotFactory a;
   private io.karatelabs.ext.agent.runtime.AgentContext b;
   private SimpleObject c;
   private UnaryOperator<String> d;
   private AtomicBoolean e;
   private volatile Bot f;
   private Supplier<String> g = () -> null;
   private Function<String, String> h = var0 -> null;

   public RobotAgentExtension() {
      this(new RobotFactory());
   }

   RobotAgentExtension(RobotFactory var1) {
      this.a = var1;
   }

   public RobotAgentExtension a(Path var1) {
      this.a.setDefaultScreenshotsDir(var1);
      return this;
   }

   public RobotAgentExtension a(UnaryOperator<String> var1) {
      this.d = var1;
      return this;
   }

   public RobotAgentExtension a(AtomicBoolean var1) {
      this.e = var1;
      return this;
   }

   public RobotAgentExtension a(Supplier<String> var1) {
      this.g = var1 == null ? () -> null : var1;
      return this;
   }

   public RobotAgentExtension a(Function<String, String> var1) {
      this.h = var1 == null ? var0 -> null : var1;
      return this;
   }

   private Object a(Object... var1) {
      return io.karatelabs.ext.common.secrets.SecretStore.a(var1.length > 0 && var1[0] != null ? var1[0].toString() : null, this.h);
   }

   private void b() {
      String var1 = this.g.get();
      if (var1 != null) {
         throw new RuntimeException(var1);
      }
   }

   @Override
   public void init(io.karatelabs.ext.agent.runtime.AgentContext var1) {
      this.b = var1;
      this.c = new LazyBot(() -> this.b(new Object[0]));
   }

   @Override
   public Map<String, Object> bindings() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("Robot", this.c());
      if (this.c != null) {
         var1.put("bot", this.c);
      }

      var1.put("secret", (io.karatelabs.js.JavaInvokable)this::a);
      return var1;
   }

   @Override
   public List<String> terminalCallNames() {
      return List.of("Robot.success", "Robot.failure");
   }

   @Override
   public List<io.karatelabs.ext.agent.runtime.AgentHook> hooks() {
      return List.of(new StopLookingHook(), new LocatorFailureHook(), new SamePageStallHook(), new LookOutputCompactor(), new UnsavedTextOutputHook());
   }

   @Override
   public String promptFragment() {
      return io.karatelabs.ext.robot.RobotPromptProvider.a();
   }

   @Override
   public void close() {
      this.a.stopOwned();
   }

   public BotDriver a() {
      Bot var1 = this.f;
      return var1 != null && var1.isAlive() ? var1.getAgentDriver() : null;
   }

   private Object b(Object... var1) {
      if (this.e != null) {
         this.e.set(true);
      }

      Object var2 = ((JavaInvokable)this.a.jsGet("create")).invoke(var1);
      if (var2 instanceof Bot var3) {
         this.f = var3;
         if (this.b.toolExecutor() instanceof io.karatelabs.ext.agent.runtime.JavaScriptExecutor var4) {
            var3.getAgentDriver().setScreenshotConsumer(var4::a);
         }

         if (this.d != null) {
            var3.getAgentDriver().setUploadFileResolver(this.d);
         }

         if (this.e != null) {
            var3.getAgentDriver().setActionFlag(this.e);
         }

         var3.getAgentDriver().setScreenshotSavedListener((var1x, var2x) -> {
            this.b.screenshot(var1x, var2x);
            io.karatelabs.ext.common.EvidenceReporter.a("screenshots/" + var1x, var2x);
         });
         var3.getAgentDriver().setHealListener(this.b::healed);
      }

      return var2;
   }

   private SimpleObject c() {
      return new SimpleObject() {
         @Override
         public Object jsGet(String var1) {
            return switch (var1) {
               case "create" -> (io.karatelabs.js.JavaInvokable) var1x -> RobotAgentExtension.this.b(var1x);
               case "success" -> (io.karatelabs.js.JavaInvokable) var1x -> {
                  RobotAgentExtension.this.b();
                  RobotAgentExtension.this.b.submitOutcome(io.karatelabs.ext.agent.runtime.AgentOutcome.success(var1x.length > 0 ? var1x[0] : null));
                  return "Run completed successfully";
               };
               case "failure" -> (io.karatelabs.js.JavaInvokable) var1x -> {
                  RobotAgentExtension.this.b();
                  String var2 = var1x.length > 0 && var1x[0] != null ? var1x[0].toString() : "Unknown failure";
                  RobotAgentExtension.this.b.submitOutcome(io.karatelabs.ext.agent.runtime.AgentOutcome.failure(var2));
                  return "Run completed with failure";
               };
               case "step" -> (io.karatelabs.js.JavaInvokable) var1x -> {
                  String var2 = var1x.length > 0 && var1x[0] != null ? var1x[0].toString() : "Step";
                  String var3 = var1x.length > 1 && var1x[1] != null ? var1x[1].toString() : null;
                  String var4 = var1x.length > 2 && var1x[2] != null ? var1x[2].toString() : null;
                  RobotAgentExtension.this.b.step(var2, var3, var4);
                  io.karatelabs.ext.common.EvidenceReporter.a(var2, var3, var4);
                  return "Step recorded: " + var2;
               };
               case "help" -> (io.karatelabs.js.JavaInvokable) RobotFactory::helpStatic;
               default -> Terms.UNDEFINED;
            };
         }

         @Override
         public Collection<String> jsKeys() {
            return List.of("create", "success", "failure", "step", "help");
         }
      };
   }
}
