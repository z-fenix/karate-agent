package io.karatelabs.ext.common.secrets;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecretStore {
   public static final String a = "KARATE_SECRETS_DIR";
   static final String b = "store.json";
   static final String c = ".store.lock";
   public static final Pattern d = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");
   private static final Pattern e = Pattern.compile("secret\\(\\s*['\"]([A-Za-z_][A-Za-z0-9_]*)['\"]\\s*\\)");
   private static final Map<Path, ReentrantLock> f = new ConcurrentHashMap<>();
   private final Path g;
   private final Path h;

   public SecretStore(Path var1) {
      this(var1, null);
   }

   private SecretStore(Path var1, Path var2) {
      this.g = var1.toAbsolutePath().normalize();
      this.h = var2;
   }

   public static SecretStore a() {
      String var0 = System.getenv("KARATE_SECRETS_DIR");
      return new SecretStore(var0 != null && !var0.isBlank() ? Path.of(var0.trim()) : Path.of(System.getProperty("user.home"), ".karate", "secrets"));
   }

   public SecretStore a(Path var1) {
      return new SecretStore(this.g, var1 == null ? null : var1.toAbsolutePath().normalize());
   }

   public Path b() {
      return this.g;
   }

   public Path c() {
      return this.g.resolve("store.json");
   }

   public boolean b(Path var1) {
      return var1 != null && c(this.g).startsWith(c(var1));
   }

   static Path c(Path var0) {
      Path var1 = var0.toAbsolutePath().normalize();

      ArrayDeque var2;
      for (var2 = new ArrayDeque(); var1.getParent() != null && !Files.exists(var1); var1 = var1.getParent()) {
         var2.push(var1.getFileName());
      }

      Path var3;
      try {
         var3 = var1.toRealPath();
      } catch (IOException var6) {
         var3 = var1;
      }

      for (Path var5 : (Iterable<Path>) (Iterable<?>) (var2)) {
         var3 = var3.resolve(var5);
      }

      return var3;
   }

   public List<String> d() {
      return new ArrayList<>(this.f().keySet());
   }

   public Map<String, String> e() {
      return this.f();
   }

   public static String a(String var0, Function<String, String> var1) {
      String var2 = var0 == null ? null : (String)var1.apply(var0);
      if (var2 == null && var0 != null) {
         var2 = System.getenv(var0);
      }

      if (var2 == null) {
         throw new RuntimeException("secret('" + var0 + "') is not stored: Secret.set('" + var0 + "', value), or set the environment variable " + var0);
      } else {
         return var2;
      }
   }

   public void a(String var1, String var2) {
      this.a(var2x -> {
         var2x.put(var1, var2);
         return true;
      });
   }

   public boolean a(String var1) {
      return this.a(var1x -> var1x.remove(var1) != null);
   }

   public static List<String> a(List<Path> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (Path var3 : (Iterable<Path>) (Iterable<?>) (new LinkedHashSet(var0))) {
         if (var3 != null) {
            var var4 = new ArrayList<>(List.of(var3.resolve("karate-base.js"), var3.resolve("karate-config.js")));
            TreeSet var5 = new TreeSet();

            try (DirectoryStream var6 = Files.newDirectoryStream(var3, "karate-config-*.js")) {
               var6.forEach(var5::add);
            } catch (IOException var11) {
            }

            var4.addAll(var5);

            for (Path var7 : (Iterable<Path>) (Iterable<?>) (var4)) {
               if (Files.isRegularFile(var7)) {
                  try {
                     Matcher var8 = e.matcher(Files.readString(var7, StandardCharsets.UTF_8));

                     while (var8.find()) {
                        var1.add(var8.group(1));
                     }
                  } catch (IOException var12) {
                  }
               }
            }
         }
      }

      return new ArrayList<>(var1);
   }

   public Map<String, String> a(List<Path> var1, Function<String, String> var2) {
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();

      for (String var5 : a(var1)) {
         String var6 = (String)var2.apply(var5);
         if (var6 != null && !var6.isEmpty()) {
            var3.put(var5, var6);
         }
      }

      var3.putAll(this.e());
      return var3;
   }

   private Map<String, String> f() {
      Path var1 = this.c();
      TreeMap<java.lang.String, java.lang.String> var2 = new TreeMap();
      if (!Files.isRegularFile(var1)) {
         return var2;
      } else {
         try {
            if (Json.parseStrict(Files.readString(var1, StandardCharsets.UTF_8), true) instanceof Map var3) {
               var3.forEach((var1x, var2x) -> {
                  if (var1x != null && var2x != null) {
                     var2.put(String.valueOf(var1x), String.valueOf(var2x));
                  }
               });
            }

            return var2;
         } catch (IOException var5) {
            throw new UncheckedIOException(var5);
         }
      }
   }

   private boolean a(Predicate<Map<String, String>> var1) {
      if (this.h != null && this.b(this.h)) {
         throw new IllegalStateException("the secret store " + this.g + " is inside the project; point KARATE_SECRETS_DIR outside it");
      } else {
         try {
            var var2 = this.g.getFileSystem().supportedFileAttributeViews();
            UserPrincipal var3 = var2.contains("posix")
               ? null
               : this.g.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
            FileAttribute var4 = a(var2, false, var3);
            this.a(a(var2, true, var3));
            ReentrantLock var5 = f.computeIfAbsent(this.g.toRealPath(), var0 -> new ReentrantLock());
            var5.lock();

            boolean var23;
            try (
               FileChannel var6 = FileChannel.open(this.g.resolve(".store.lock"), Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE), var4);
               FileLock var7 = var6.lock();
            ) {
               var var8 = this.f();
               if (!var1.test(var8)) {
                  return false;
               }

               this.a(var8, var4);
               var23 = true;
            } finally {
               var5.unlock();
            }

            return var23;
         } catch (IOException var22) {
            throw new UncheckedIOException(var22);
         }
      }
   }

   public static FileAttribute<?> a(Set<String> var0, boolean var1, UserPrincipal var2) throws IOException {
      if (var0.contains("posix")) {
         return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(var1 ? "rwx------" : "rw-------"));
      } else if (var0.contains("acl") && var2 != null) {
         final var var3 = List.of(
            AclEntry.newBuilder()
               .setType(AclEntryType.ALLOW)
               .setPrincipal(var2)
               .setPermissions(EnumSet.allOf(AclEntryPermission.class))
               .setFlags(var1 ? EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT) : EnumSet.noneOf(AclEntryFlag.class))
               .build()
         );
         return new FileAttribute<List<AclEntry>>() {
            @Override
            public String name() {
               return "acl:acl";
            }

            @Override
            public List<AclEntry> value() {
               return var3;
            }
         };
      } else {
         throw new IOException(
            "the file system holding the secret store supports neither POSIX permissions nor ACLs, so a value there could not be kept owner-only; point KARATE_SECRETS_DIR at one that does"
         );
      }
   }

   private void a(FileAttribute<?> var1) throws IOException {
      if (Files.isDirectory(this.g)) {
         if (var1.value() instanceof Set) {
            Files.setPosixFilePermissions(this.g, PosixFilePermissions.fromString("rwx------"));
         } else {
            Files.getFileAttributeView(this.g, AclFileAttributeView.class).setAcl((List<AclEntry>)var1.value());
         }
      } else {
         if (this.g.getParent() != null) {
            Files.createDirectories(this.g.getParent());
         }

         try {
            Files.createDirectory(this.g, var1);
         } catch (FileAlreadyExistsException var3) {
            if (!Files.isDirectory(this.g)) {
               throw var3;
            }

            this.a(var1);
         }
      }
   }

   private void a(Map<String, String> var1, FileAttribute<?> var2) throws IOException {
      Path var3 = Files.createTempFile(this.g, ".store", ".tmp", var2);

      try {
         Files.writeString(var3, Json.stringifyStrict(new TreeMap(var1)), StandardCharsets.UTF_8);

         try {
            Files.move(var3, this.c(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var8) {
            Files.move(var3, this.c(), StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(var3);
      }
   }
}
