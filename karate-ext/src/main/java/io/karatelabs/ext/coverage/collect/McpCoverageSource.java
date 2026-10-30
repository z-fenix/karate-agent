package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import io.karatelabs.ext.coverage.CoverageItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class McpCoverageSource implements io.karatelabs.ext.coverage.CoverageSource {
   private final Path a;
   private List<CoverageItem> b;

   public McpCoverageSource(Path var1) {
      this.a = var1;
   }

   @Override
   public List<CoverageItem> a() {
      if (this.b != null) {
         return this.b;
      } else {
         ArrayList var1 = new ArrayList();

         for (Object var3 : this.h()) {
            if (var3 instanceof Map var4 && var4.get("name") instanceof String var5 && !var5.isBlank()) {
               LinkedHashMap var10 = new LinkedHashMap();
               var10.put("kind", "mcp-tool");
               if (var4.get("description") instanceof String var7) {
                  var10.put("description", var7);
               }

               String var11 = var4.get("title") instanceof String var12 ? var12 : var5;
               var1.add(new CoverageItem(var5, var11, Set.of(), null, var10));
            }
         }

         this.b = var1;
         return var1;
      }
   }

   private List<?> h() {
      if (this.a != null && Files.isRegularFile(this.a)) {
         Object var1;
         try {
            var1 = Json.parseLenient(Files.readString(this.a));
         } catch (Exception var3) {
            throw new RuntimeException("cov.mcp: cannot read tool catalog " + this.a + " — " + var3.getMessage(), var3);
         }

         if (var1 instanceof Map var2) {
            var1 = var2.get("tools");
         }

         return var1 instanceof List var4 ? var4 : List.of();
      } else {
         return List.of();
      }
   }

   @Override
   public String b() {
      return "mcp";
   }

   @Override
   public Map<String, Object> c() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("title", "MCP Tools");
      if (this.a != null) {
         var1.put("catalog", this.a.toString());
      }

      return var1;
   }
}
