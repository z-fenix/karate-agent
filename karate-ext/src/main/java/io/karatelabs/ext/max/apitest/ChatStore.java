package io.karatelabs.ext.max.apitest;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class ChatStore {
   public static final String DIR_ENV = "KARATE_CHAT_DIR";
   static final String SUBDIR = "api-chat";
   private final Path dir;
   private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

   private ChatStore(Path var1) {
      this.dir = var1.toAbsolutePath().normalize();
   }

   public static ChatStore server() {
      String var0 = System.getenv("KARATE_CHAT_DIR");
      return new ChatStore(var0 != null && !var0.isBlank() ? Path.of(var0.trim()) : Path.of(System.getProperty("user.home"), ".karate"));
   }

   public static ChatStore at(Path var0) {
      return new ChatStore(var0);
   }

   public Path dir() {
      return this.dir;
   }

   public Path file(TestId var1) {
      return this.dir.resolve("api-chat").resolve(var1.projectKey()).resolve(var1.testKey() + ".json");
   }

   public ReentrantLock lock(TestId var1) {
      return LOCKS.computeIfAbsent(MaxPaths.lockKey(this.file(var1)), var0 -> new ReentrantLock());
   }

   public ChatStore.Doc read(TestId var1) {
      byte[] var2;
      try {
         var2 = Files.readAllBytes(this.file(var1));
      } catch (NoSuchFileException var7) {
         return ChatStore.Doc.ABSENT;
      } catch (IOException var8) {
         return new ChatStore.Doc(new byte[0], io.karatelabs.ext.common.Sha256.a(new byte[0]), null);
      }

      Map var3 = null;

      try {
         if (Json.parseStrict(new String(var2, StandardCharsets.UTF_8), true) instanceof Map var4) {
            var3 = var4;
         }
      } catch (RuntimeException var6) {
      }

      return new ChatStore.Doc(var2, io.karatelabs.ext.common.Sha256.a(var2), var3);
   }

   public void write(TestId var1, Map<String, Object> var2) throws IOException {
      Path var3 = this.file(var1);
      var var4 = this.dir.getFileSystem().supportedFileAttributeViews();
      UserPrincipal var5 = var4.contains("posix")
         ? null
         : this.dir.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
      FileAttribute var6 = io.karatelabs.ext.common.secrets.SecretStore.a(var4, true, var5);
      FileAttribute var7 = io.karatelabs.ext.common.secrets.SecretStore.a(var4, false, var5);
      createDir(this.dir, var6);
      createDir(this.dir.resolve("api-chat"), var6);
      createDir(var3.getParent(), var6);
      Path var8 = Files.createTempFile(var3.getParent(), "." + var3.getFileName(), ".tmp", var7);

      try {
         Files.writeString(var8, Json.stringifyStrict(var2), StandardCharsets.UTF_8);

         try {
            Files.move(var8, var3, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var13) {
            throw new IOException("could not replace " + var3 + " atomically: " + var13.getMessage(), var13);
         }
      } finally {
         Files.deleteIfExists(var8);
      }
   }

   public void delete(TestId var1) throws IOException {
      Files.deleteIfExists(this.file(var1));
   }

   public boolean deleteIfHash(TestId var1, String var2) throws IOException {
      ChatStore.Doc var3 = this.read(var1);
      if (var3.exists() && var3.hash.equals(var2)) {
         Files.deleteIfExists(this.file(var1));
         return true;
      } else {
         return false;
      }
   }

   public List<ChatStore.Doc> documents(Path var1) throws IOException {
      Path var2 = this.dir.resolve("api-chat").resolve(TestId.projectKey(var1));
      ArrayList<io.karatelabs.ext.max.apitest.ChatStore.Doc> var3 = new ArrayList();

      try {
         if (!Files.readAttributes(var2, BasicFileAttributes.class).isDirectory()) {
            return var3;
         }
      } catch (NoSuchFileException var16) {
         return var3;
      }

      try (DirectoryStream var4 = Files.newDirectoryStream(var2, "*.json")) {
         Iterator var5 = var4.iterator();

         while (true) {
            byte[] var7;
            while (true) {
               if (!var5.hasNext()) {
                  return var3;
               }

               Path var6 = (Path)var5.next();

               try {
                  var7 = Files.readAllBytes(var6);
                  break;
               } catch (IOException var13) {
                  var3.add(new ChatStore.Doc(new byte[0], io.karatelabs.ext.common.Sha256.a(new byte[0]), null));
               }
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var8 = null;

            try {
               if (Json.parseStrict(new String(var7, StandardCharsets.UTF_8), true) instanceof Map var9) {
                  var8 = new LinkedHashMap();

                  for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
                     var8.put(String.valueOf(var11.getKey()), var11.getValue());
                  }
               }
            } catch (RuntimeException var14) {
            }

            var3.add(new ChatStore.Doc(var7, io.karatelabs.ext.common.Sha256.a(var7), var8));
         }
      }
   }

   private static void createDir(Path var0, FileAttribute<?> var1) throws IOException {
      if (!Files.isDirectory(var0)) {
         if (var0.getParent() != null) {
            Files.createDirectories(var0.getParent());
         }

         try {
            Files.createDirectory(var0, var1);
         } catch (FileAlreadyExistsException var3) {
            if (!Files.isDirectory(var0)) {
               throw var3;
            }
         }
      }
   }

   public record Doc(byte[] bytes, String hash, Map<String, Object> parsed) {
      public static final ChatStore.Doc ABSENT = new ChatStore.Doc(null, null, null);


      public boolean exists() {
         return this.bytes != null;
      }

      public boolean unparseable() {
         return this.bytes != null && this.parsed == null;
      }
   }
}
