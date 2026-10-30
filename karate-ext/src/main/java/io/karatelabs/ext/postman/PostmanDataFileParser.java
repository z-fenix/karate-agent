package io.karatelabs.ext.postman;

import io.karatelabs.common.DataUtils;
import io.karatelabs.js.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PostmanDataFileParser {
   private PostmanDataFileParser() {
   }

   public static List<Map<String, Object>> a(Path var0, String var1) {
      String var2 = var0.getFileName().toString().toLowerCase();
      if (!var2.endsWith(".csv") && !var2.endsWith(".json")) {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data is a .csv or .json file, not " + var1);
      } else if (!Files.isRegularFile(var0)) {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("no data file '" + var1 + "'");
      } else {
         String var3;
         try {
            var3 = Files.readString(var0, StandardCharsets.UTF_8);
         } catch (IOException var5) {
            throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' could not be read: " + var5.getMessage());
         }

         return a(var3, var2, var1);
      }
   }

   public static List<Map<String, Object>> a(String var0, String var1, String var2) {
      if (!var1.toLowerCase().endsWith(".csv") && !var1.toLowerCase().endsWith(".json")) {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data is a .csv or .json file, not " + var2);
      } else {
         List<java.util.Map<java.lang.String,java.lang.Object>> var3 = var1.toLowerCase().endsWith(".csv") ? a(var0, var2) : b(var0, var2);
         if (var3.isEmpty()) {
            throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var2 + "' has no rows");
         } else {
            return var3;
         }
      }
   }

   private static List<Map<String, Object>> a(String var0, String var1) {
      try {
         var var2 = DataUtils.fromCsv(var0);
         if (!var2.isEmpty() && var2.get(0).keySet().stream().anyMatch(var0x -> var0x == null || var0x.isBlank())) {
            throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' has a blank column name in its header row");
         } else {
            return var2;
         }
      } catch (io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException var3) {
         throw var3;
      } catch (RuntimeException var4) {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' is not CSV: " + var4.getMessage());
      }
   }

   private static List<Map<String, Object>> b(String var0, String var1) {
      Object var2;
      try {
         var2 = JsonParser.parseSpans(var0).root().toValue();
      } catch (RuntimeException var8) {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' is not JSON: " + var8.getMessage());
      }

      if (var2 instanceof List var3) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();

         for (Object var6 : var3) {
            if (!(var6 instanceof Map var7)) {
               throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' is a JSON array of objects, one per iteration; row " + var4.size() + " is not an object");
            }

            var4.add(new LinkedHashMap(var7));
         }

         return var4;
      } else {
         throw new io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException("data file '" + var1 + "' is a JSON array of objects, one per iteration");
      }
   }

   public static final class PostmanDataFileException extends RuntimeException {
      public PostmanDataFileException(String var1) {
         super(var1);
      }
   }
}
