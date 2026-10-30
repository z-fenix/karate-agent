package io.karatelabs.core;

import io.karatelabs.common.Resource;
import java.util.Set;

public interface FileReaderProvider {
   Set<String> extensions();

   Object read(Resource var1, String var2);

   static FileReaderProvider forExtension(String extension) {
      return FileReaderProviders.forExtension(extension);
   }

   static String[] splitFragment(String path) {
      return FileReaderProviders.splitFragment(path);
   }
}
