package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RuleBaselineStore {
   private static final Logger c = LoggerFactory.getLogger(RuleBaselineStore.class);
   static final String a = ".baseline";
   static final String b = "__last__";

   private RuleBaselineStore() {
   }

   static Path a(Path var0, String var1, String var2) {
      return var0.resolve(var1).resolve(".baseline").resolve(var2 + ".json");
   }

   static Path a(Path var0, String var1) {
      return a(var0, var1, "__last__");
   }

   static RuleBaselineSnapshot a(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return null;
      } else {
         try {
            if (Json.parseStrict(Files.readString(var0, StandardCharsets.UTF_8), true) instanceof Map var2) {
               var var3 = a(var2.get("tree"));
               Map var4 = var2.get("mtimes") instanceof Map var5 ? var5 : Map.of();
               long var11 = var2.get("ts") instanceof Number var7 ? var7.longValue() : 0L;
               String var12 = var2.get("scenario") instanceof String var13 ? var13 : null;
               return new RuleBaselineSnapshot(RuleTreeDiffer.c(var3), var2.get("output"), var11, var4, var12);
            } else {
               return null;
            }
         } catch (RuntimeException | IOException var10) {
            c.warn("ignoring unreadable baseline {}: {}", var0, var10.toString());
            return null;
         }
      }
   }

   static void a(Path var0, List<ResultTreeNode> var1, Object var2, long var3, Map<String, String> var5, String var6) {
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("ts", var3);
      if (var6 != null) {
         var7.put("scenario", var6);
      }

      var7.put("mtimes", var5);
      var7.put("output", var2);
      var7.put("tree", a(RuleTreeDiffer.b(var1)));

      try {
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, Json.stringifyStrict(var7), StandardCharsets.UTF_8);
      } catch (IOException var9) {
         throw new UncheckedIOException(var9);
      }
   }

   private static List<Map<String, Object>> a(List<ExecutionTraceSnapshotNode> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList(var0.size());

      for (ExecutionTraceSnapshotNode var3 : var0) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("key", var3.a());
         var4.put("line", var3.b());
         var4.put("heading", var3.c());
         var4.put("container", var3.d());
         var4.put("text", var3.e());
         if (!var3.f().isEmpty()) {
            var4.put("children", a(var3.f()));
         }

         var1.add(var4);
      }

      return var1;
   }

   private static List<ExecutionTraceSnapshotNode> a(Object var0) {
      if (var0 instanceof List var1) {
         ArrayList<io.karatelabs.ext.rules.ExecutionTraceSnapshotNode> var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            if (var4 instanceof Map var5) {
               var2.add(
                  new ExecutionTraceSnapshotNode(
                     var5.get("key") instanceof String var6 ? var6 : null,
                     var5.get("line") instanceof Number var8 ? var8.intValue() : 0,
                     var5.get("heading") instanceof Number var9 ? var9.intValue() : 0,
                     Boolean.TRUE.equals(var5.get("container")),
                     var5.get("text") instanceof String var10 ? var10 : null,
                     a(var5.get("children"))
                  )
               );
            }
         }

         return var2;
      } else {
         return List.of();
      }
   }
}
