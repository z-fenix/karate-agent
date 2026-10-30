package io.karatelabs.ext.sourcedoc;

public interface SourceDocumentParser {
   SourceDocument parse(String var1, String var2);

   static SourceDocumentParser selectParser(String var0, String var1) {
      String var2 = var0 == null ? "" : var0.toLowerCase();
      if (var2.endsWith(".json")) {
         return new JsonSourceDocumentParser();
      } else if (!var2.endsWith(".yaml") && !var2.endsWith(".yml")) {
         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = var1.charAt(var3);
            if (!Character.isWhitespace(var4)) {
               return (SourceDocumentParser)(var4 != '{' && var4 != '[' ? new YamlSourceDocumentParser() : new JsonSourceDocumentParser());
            }
         }

         return new YamlSourceDocumentParser();
      } else {
         return new YamlSourceDocumentParser();
      }
   }
}
