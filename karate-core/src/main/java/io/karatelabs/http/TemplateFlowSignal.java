package io.karatelabs.http;

import io.karatelabs.js.FlowControlSignal;

public class TemplateFlowSignal extends RuntimeException implements FlowControlSignal {
   private final TemplateFlowSignal.Kind kind;

   public TemplateFlowSignal(TemplateFlowSignal.Kind kind) {
      super(kind.name().toLowerCase());
      this.kind = kind;
   }

   public TemplateFlowSignal.Kind getKind() {
      return this.kind;
   }

   @Override
   public synchronized Throwable fillInStackTrace() {
      return this;
   }

   public static enum Kind {
      REDIRECT,
      SWITCH;
   }
}
