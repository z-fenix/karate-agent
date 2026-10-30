package io.karatelabs.core;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

final class FileReaderProviders {
   private static final Map<ClassLoader, SoftReference<List<FileReaderProvider>>> CACHE = new WeakHashMap<>();

   private FileReaderProviders() {
   }

   static FileReaderProvider forExtension(String extension) {
      if (extension != null && !extension.isEmpty()) {
         String ext = extension.toLowerCase(Locale.ROOT);
         FileReaderProvider found = null;

         for (FileReaderProvider p : providers()) {
            if (p.extensions().contains(ext)) {
               if (found != null) {
                  throw new IllegalStateException(
                     "read(): extension '"
                        + ext
                        + "' is claimed by both "
                        + found.getClass().getName()
                        + " and "
                        + p.getClass().getName()
                        + " - remove one from the classpath"
                  );
               }

               found = p;
            }
         }

         return found;
      } else {
         return null;
      }
   }

   static String[] splitFragment(String path) {
      for (int i = path.indexOf(35, 1); i != -1; i = path.indexOf(35, i + 1)) {
         if (path.indexOf(47, i) == -1 && path.indexOf(92, i) == -1) {
            String file = path.substring(0, i);
            int dot = file.lastIndexOf(46);
            if (dot > Math.max(file.lastIndexOf(47), file.lastIndexOf(92)) && forExtension(file.substring(dot + 1)) != null) {
               return new String[]{file, path.substring(i + 1)};
            }
         }
      }

      return null;
   }

   private static List<FileReaderProvider> providers() {
      ClassLoader context = Thread.currentThread().getContextClassLoader();
      ClassLoader loader = context != null ? context : FileReaderProvider.class.getClassLoader();
      synchronized (CACHE) {
         SoftReference<List<FileReaderProvider>> ref = CACHE.get(loader);
         List<FileReaderProvider> cached = ref == null ? null : ref.get();
         if (cached != null) {
            return cached;
         }
      }

      List<FileReaderProvider> found = discover(loader);
      synchronized (CACHE) {
         CACHE.put(loader, new SoftReference<>(found));
         return found;
      }
   }

   private static List<FileReaderProvider> discover(ClassLoader loader) {
      Map<String, FileReaderProvider> byName = new LinkedHashMap<>();
      ClassLoader defining = FileReaderProvider.class.getClassLoader();

      for (ClassLoader cl : loader == defining ? List.of(loader) : Arrays.asList(loader, defining)) {
         for (FileReaderProvider p : ServiceLoader.load(FileReaderProvider.class, cl)) {
            byName.putIfAbsent(p.getClass().getName(), p);
         }
      }

      return List.copyOf(byName.values());
   }
}
