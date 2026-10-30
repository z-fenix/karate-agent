package io.karatelabs.js;

public interface DebugPointFactory<T> {
   int JS_STATEMENT = 0;
   int JS_EXPRESSION = 1;
   int GHERKIN_STEP = 2;

   T create(int var1, int var2, String var3, Object var4, Object var5);
}
