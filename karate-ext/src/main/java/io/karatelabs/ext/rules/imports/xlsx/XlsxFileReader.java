package io.karatelabs.ext.rules.imports.xlsx;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import io.karatelabs.core.FileReaderProvider;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.rules.RuleApiException;
import io.karatelabs.ext.rules.imports.ImportReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

public class XlsxFileReader implements FileReaderProvider {
   @Override
   public Set<String> extensions() {
      return Set.of("xlsx");
   }

   @Override
   public Object read(Resource var1, String var2) {
      String var3 = var1.getSimpleName();
      String var4 = "read('" + var3 + (var2 == null ? "" : "#" + var2) + "')";

      try {
         Main.requireLicensed("xlsx data");
      } catch (RuntimeException var6) {
         throw new RuleApiException(
            403,
            "UNLICENSED",
            var4
               + ": "
               + var6.getMessage().replaceFirst("^### ", "")
               + " — or save the sheet as CSV and read('"
               + var3.replaceFirst("(?i)\\.xlsx$", ".csv")
               + "')"
         );
      }

      try {
         return XlsxImportReader.records(XlsxImportReader.sheet(FileUtils.toBytes(var1.getStream()), var2 != null && !var2.isEmpty() ? var2 : null));
      } catch (ImportReader.Refused var7) {
         throw new RuleApiException(400, var7.code(), var4 + ": " + var7.getMessage());
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      }
   }
}
