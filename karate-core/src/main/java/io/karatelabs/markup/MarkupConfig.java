package io.karatelabs.markup;

import io.karatelabs.js.Engine;
import java.util.function.Supplier;

public class MarkupConfig {
   private Supplier<Engine> engineSupplier;
   private ResourceResolver resolver;
   private String contextPath;
   private boolean devMode;
   private boolean devTrace;
   private boolean serverMode = true;

   public void setResolver(ResourceResolver resolver) {
      this.resolver = resolver;
   }

   public ResourceResolver getResolver() {
      return this.resolver;
   }

   public String getContextPath() {
      return this.contextPath;
   }

   public void setContextPath(String contextPath) {
      this.contextPath = contextPath;
   }

   public boolean isDevMode() {
      return this.devMode;
   }

   public void setDevMode(boolean devMode) {
      this.devMode = devMode;
   }

   public boolean isDevTrace() {
      return this.devTrace;
   }

   public void setDevTrace(boolean devTrace) {
      this.devTrace = devTrace;
   }

   public boolean isServerMode() {
      return this.serverMode;
   }

   public void setServerMode(boolean serverMode) {
      this.serverMode = serverMode;
   }

   public void setEngineSupplier(Supplier<Engine> engineSupplier) {
      this.engineSupplier = engineSupplier;
   }

   public Supplier<Engine> getEngineSupplier() {
      return this.engineSupplier;
   }
}
