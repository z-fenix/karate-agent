package io.karatelabs.core;

import io.karatelabs.js.JavaCallable;
import java.util.Map;

public class MockConfig {
   private boolean corsEnabled;
   private boolean mockHeaderEnabled = true;
   private Map<String, Object> responseHeaders;
   private JavaCallable beforeScenario;
   private JavaCallable afterScenario;

   public boolean isCorsEnabled() {
      return this.corsEnabled;
   }

   public boolean isMockHeaderEnabled() {
      return this.mockHeaderEnabled;
   }

   public void setMockHeaderEnabled(boolean mockHeaderEnabled) {
      this.mockHeaderEnabled = mockHeaderEnabled;
   }

   public void setCorsEnabled(boolean corsEnabled) {
      this.corsEnabled = corsEnabled;
   }

   public Map<String, Object> getResponseHeaders() {
      return this.responseHeaders;
   }

   public void setResponseHeaders(Map<String, Object> responseHeaders) {
      this.responseHeaders = responseHeaders;
   }

   public JavaCallable getBeforeScenario() {
      return this.beforeScenario;
   }

   public void setBeforeScenario(JavaCallable beforeScenario) {
      this.beforeScenario = beforeScenario;
   }

   public JavaCallable getAfterScenario() {
      return this.afterScenario;
   }

   public void setAfterScenario(JavaCallable afterScenario) {
      this.afterScenario = afterScenario;
   }
}
