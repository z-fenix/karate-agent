package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Resource;
import io.karatelabs.markup.ResourceResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;

final class AppResourceResolver implements ResourceResolver {
   static final String a = "META-INF/karate/app/";
   private final Path b;
   private volatile Path c;

   AppResourceResolver(Path var1) {
      this.b = var1 == null ? null : var1.toAbsolutePath().normalize();
   }

   private Path a() {
      if (this.c == null && this.b != null && Files.isDirectory(this.b)) {
         try {
            this.c = this.b.toRealPath();
         } catch (IOException var2) {
            return null;
         }
      }

      return this.c;
   }

   @Override
   public Resource resolve(String var1, Resource var2) {
      String var3 = var1;
      if (var1 != null && var1.startsWith("this:")) {
         if (!(var2 instanceof io.karatelabs.ext.rules.twin.AppResourceResolver.InMemoryAppResource var4)) {
            return null;
         }

         String var5 = var4.a.contains("/") ? var4.a.substring(0, var4.a.lastIndexOf(47) + 1) : "";
         var3 = var5 + var1.substring("this:".length());
      }

      var3 = a(var3);
      return var3 == null ? null : this.b(var3);
   }

   static String a(String var0) {
      if (var0 != null && !var0.isEmpty() && var0.indexOf(58) < 0 && var0.indexOf(92) < 0 && var0.indexOf(0) < 0) {
         ArrayList<java.lang.String> var1 = new ArrayList();

         for (String var5 : var0.split("/")) {
            if (!var5.isEmpty() && !var5.equals(".")) {
               if (var5.equals("..")) {
                  if (var1.isEmpty()) {
                     return null;
                  }

                  var1.remove(var1.size() - 1);
               } else {
                  var1.add(var5);
               }
            }
         }

         return var1.isEmpty() ? null : String.join("/", var1);
      } else {
         return null;
      }
   }

   private Resource b(String var1) {
      Path var2 = this.a();
      if (var2 != null) {
         Path var3 = var2.resolve(var1);
         if (io.karatelabs.ext.rules.twin.PlayRecordStore.a(var2, var3) && Files.isRegularFile(var3, LinkOption.NOFOLLOW_LINKS)) {
            try {
               io.karatelabs.ext.rules.twin.AppResourceResolver.InMemoryAppResource var16;
               try (InputStream var15 = Files.newInputStream(var3, LinkOption.NOFOLLOW_LINKS)) {
                  var16 = new io.karatelabs.ext.rules.twin.AppResourceResolver.InMemoryAppResource(var1, var15.readAllBytes(), Files.getLastModifiedTime(var3, LinkOption.NOFOLLOW_LINKS).toMillis(), this);
               }

               return var16;
            } catch (IOException var13) {
               return null;
            }
         }
      }

      URL var14 = AppResourceResolver.class.getClassLoader().getResource("META-INF/karate/app/" + var1);
      if (var14 != null && !var14.getPath().endsWith("/")) {
         try {
            if ("file".equals(var14.getProtocol()) && !a(Path.of(var14.toURI()), ("META-INF/karate/app/" + var1).split("/").length)) {
               return null;
            } else {
               URLConnection var4 = var14.openConnection();

               io.karatelabs.ext.rules.twin.AppResourceResolver.InMemoryAppResource var6;
               try (InputStream var5 = var4.getInputStream()) {
                  var6 = new io.karatelabs.ext.rules.twin.AppResourceResolver.InMemoryAppResource(var1, var5.readAllBytes(), var4.getLastModified(), this);
               }

               return var6;
            }
         } catch (URISyntaxException | IOException var11) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean a(Path var0, int var1) {
      Path var2 = var0;

      for (int var3 = 0; var3 < var1 && var2 != null; var2 = var2.getParent()) {
         if (Files.isSymbolicLink(var2)) {
            return false;
         }

         var3++;
      }

      return Files.isRegularFile(var0, LinkOption.NOFOLLOW_LINKS);
   }

   static final class InMemoryAppResource implements Resource {
      final String a;
      private final byte[] b;
      private final long c;
      private final AppResourceResolver d;

      InMemoryAppResource(String var1, byte[] var2, long var3, AppResourceResolver var5) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var5;
      }

      @Override
      public boolean isFile() {
         return false;
      }

      @Override
      public boolean isClassPath() {
         return false;
      }

      @Override
      public boolean exists() {
         return true;
      }

      @Override
      public URI getUri() {
         return URI.create("app:/" + this.a);
      }

      @Override
      public Path getPath() {
         return null;
      }

      @Override
      public Path getRoot() {
         return Path.of(System.getProperty("java.io.tmpdir"));
      }

      @Override
      public Resource resolve(String var1) {
         return this.d.resolve("this:" + var1, this);
      }

      @Override
      public InputStream getStream() {
         return new ByteArrayInputStream(this.b);
      }

      @Override
      public String getRelativePath() {
         return this.a;
      }

      @Override
      public String getText() {
         return new String(this.b, StandardCharsets.UTF_8);
      }

      @Override
      public String getLine(int var1) {
         String[] var2 = this.getText().split("\\r?\\n", -1);
         return var1 >= 0 && var1 < var2.length ? var2[var1] : null;
      }

      @Override
      public long getLastModified() {
         return this.c;
      }
   }
}
