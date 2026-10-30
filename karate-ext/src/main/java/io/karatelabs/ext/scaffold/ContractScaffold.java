package io.karatelabs.ext.scaffold;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.contract.Contract;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ContractScaffold {
   public Object scaffold(Contract var1, ScaffoldEmitter var2, ScaffoldContext var3) {
      var var4 = var2.emit(var1, var3);
      var var5 = new LinkedHashMap<>(var4);
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("files", var5);
      var6.put("count", var5.size());
      String var7 = var3.outDir();
      if (var7 == null) {
         return JsApi.hint(var6, var3.filesHint());
      } else {
         try {
            Path var8 = Path.of(var7);
            var var9 = plan(var8, var4, var3.artifacts());
            ArrayList<java.lang.String> var10 = new ArrayList();

            for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
               if (Files.isRegularFile((Path)var12.getKey()) && !Files.readString((Path)var12.getKey()).equals(var12.getValue())) {
                  var10.add(var8.relativize((Path)var12.getKey()).toString());
               }
            }

            if (!var10.isEmpty() && !var3.overwrite()) {
               return JsApi.missing(
                  "scaffold wrote nothing: " + var10 + " already exist in '" + var7 + "' — pass {overwrite:true} to replace them, or pick another out"
               );
            } else {
               ArrayList var17 = new ArrayList();
               ArrayList var18 = new ArrayList();

               for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
                  Path var15 = (Path)var14.getKey();
                  (var10.contains(var8.relativize(var15).toString()) ? var18 : var17).add(var15.toString());
                  if (var15.getParent() != null) {
                     Files.createDirectories(var15.getParent());
                  }

                  Files.writeString(var15, (CharSequence)var14.getValue());
               }

               var6.put("out", var7);
               var6.put("written", var17);
               if (!var18.isEmpty()) {
                  var6.put("overwritten", var18);
               }

               return JsApi.hint(var6, var3.writtenHint());
            }
         } catch (RuntimeException | IOException var16) {
            return JsApi.missing("scaffold could not write to '" + var7 + "': " + var16.getMessage());
         }
      }
   }

   private static Map<Path, String> plan(Path var0, Map<String, String> var1, List<ScaffoldContext.Artifact> var2) {
      LinkedHashMap<java.nio.file.Path, java.lang.String> var3 = new LinkedHashMap();

      for (ScaffoldContext.Artifact var5 : var2) {
         var3.put(var0.resolve(var5.targetName()), Resource.path(var5.sourcePath()).getText());
      }

      for (Entry var7 : var1.entrySet()) {
         var3.put(var0.resolve((String)var7.getKey()), (String)var7.getValue());
      }

      return var3;
   }
}
