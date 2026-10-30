package io.karatelabs.ext.rules.twin;

import java.util.Map;

public interface RulebookRuntime extends AutoCloseable {
   io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookInvocation a(String var1, Object var2);

   Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> a();

   @Override
   void close();

   public record RulebookInvocation(String a, String b, Object c, Object d) {

      public RulebookInvocation(String a, String b, Object c, Object d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   public record RulebookUsageStats(String a, int b, int c, long d) {

      public RulebookUsageStats(String a, int b, int c, long d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
