package io.karatelabs.ext.common;

public interface MockRuntime {
   String getUrl();

   Object getVariable(String var1);

   Object brain();

   boolean isStopped();

   void stop();
}
