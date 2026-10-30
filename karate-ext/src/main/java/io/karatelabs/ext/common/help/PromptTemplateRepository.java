package io.karatelabs.ext.common.help;

import java.util.Map;

public final class PromptTemplateRepository {
   private final String a;
   private final String b;

   private PromptTemplateRepository(String var1) {
      this.a = var1;
      this.b = "META-INF/karate/prompt/" + var1 + "/";
   }

   public static PromptTemplateRepository a(String var0) {
      return new PromptTemplateRepository(io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0));
   }

   public String a() {
      return this.b("core");
   }

   public String a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind var1) {
      return this.b(var1.file);
   }

   public String a(String var1, Map<String, Object> var2) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.a(this.b(io.karatelabs.ext.common.help.ClasspathTextLoader.b(var1)), var2);
   }

   public String b(String var1, Map<String, Object> var2) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.b(this.b(io.karatelabs.ext.common.help.ClasspathTextLoader.b(var1)), var2);
   }

   private String b(String var1) {
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a(this.b + var1 + ".md");
      if (var2 == null) {
         throw new IllegalStateException("prompt content missing: " + this.b + var1 + ".md");
      } else {
         return var2;
      }
   }

   public static enum PromptTemplateKind {
      AUTONOMOUS("loop"),
      LIVE("live");

      final String file;

      private PromptTemplateKind(String var3) {
         this.file = var3;
      }
   }
}
