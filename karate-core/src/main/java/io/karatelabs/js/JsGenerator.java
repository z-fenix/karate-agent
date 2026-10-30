package io.karatelabs.js;

class JsGenerator extends JsObject {
   final GeneratorActivation activation;

   JsGenerator(Engine engine, JsFunctionNode function, CoreContext functionContext, Object[] args) {
      super(null, JsGeneratorPrototype.INSTANCE);
      this.activation = new GeneratorActivation(engine, function, functionContext, args);
   }

   static JsObject result(Object value, boolean done) {
      JsObject r = new JsObject();
      r.putMember("value", value);
      r.putMember("done", done);
      return r;
   }

   Object next(CoreContext ctx, Object value) {
      return this.activation.isDone() ? result(Terms.UNDEFINED, true) : this.drive(ctx, GeneratorActivation.ResumeKind.NEXT, value);
   }

   Object returnValue(CoreContext ctx, Object value) {
      if (!this.activation.isDone() && this.activation.isStarted()) {
         return this.drive(ctx, GeneratorActivation.ResumeKind.RETURN, value);
      } else if (this.activation.isRunning()) {
         throw JsErrorException.typeError("Generator is already running");
      } else {
         this.activation.retire();
         return result(value, true);
      }
   }

   Object throwValue(CoreContext ctx, Object value) {
      if (!this.activation.isDone() && this.activation.isStarted()) {
         return this.drive(ctx, GeneratorActivation.ResumeKind.THROW, value);
      } else if (this.activation.isRunning()) {
         throw JsErrorException.typeError("Generator is already running");
      } else {
         this.activation.retire();
         ctx.stopAndThrow(value);
         return Terms.UNDEFINED;
      }
   }

   private Object drive(CoreContext ctx, GeneratorActivation.ResumeKind kind, Object value) {
      GeneratorActivation.StepOutcome outcome = this.activation.step(kind, value);

      return switch (outcome.kind) {
         case YIELDED -> result(outcome.value, false);
         case RETURNED -> result(outcome.value, true);
         case THREW -> {
            ctx.stopAndThrow(outcome.value);
            yield Terms.UNDEFINED;
         }
         case HOST_CANCELLED -> throw new EngineInterruptedException();
      };
   }
}
