package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class PlayRecordStore {
   public static final String a = "PLAY_NOTFOUND";
   public static final String b = "PLAY_TAMPERED";
   public static final String c = "PLAY_UNCONFIRMED";
   static final String d = "record.json";
   static final String e = "confirm.json";
   private static final Pattern f = Pattern.compile("^play-[0-9a-f]{16}$");
   private static final List<String> g = List.of("id", "rulebook", "identity", "manifest", "steps", "digest");
   private static final List<String> h = List.of("record", "recordDigest", "expect", "req", "at", "digest");

   private PlayRecordStore() {
   }

   public static Path a(Path var0) {
      return var0.resolve("target").resolve("draft").resolve("play");
   }

   public static io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord a(Path var0, String var1) {
      Path var2 = b(var0, var1);
      Path var3 = var2.getParent();
      if (!Files.isRegularFile(var2, LinkOption.NOFOLLOW_LINKS)) {
         throw a(var1, "no play record " + var1 + " under " + a(var0));
      } else {
         var var4 = b(var2);
         if (var4 != null
            && var4.keySet().containsAll(g)
            && var1.equals(var4.get("id"))
            && var4.get("manifest") instanceof Map
            && var4.get("digest") instanceof String var5
            && a(var4.get("steps"))) {
            String var7 = b(var4);
            if (!var5.equals(var7)) {
               throw b(var1, "record.json's content is not what its digest was taken over");
            } else {
               return new io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord(var1, var3, var4, var7);
            }
         } else {
            throw b(var1, "record.json is not the adapter's record shape");
         }
      }
   }

   public static io.karatelabs.ext.rules.twin.PlayRecordStore.PlayConfirmation a(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0) {
      Path var1 = var0.f().resolve("confirm.json");
      if (a(var0.f().getParent(), var1) && Files.isRegularFile(var1, LinkOption.NOFOLLOW_LINKS)) {
         var var2 = b(var1);
         if (var2 == null
            || var2.size() != h.size()
            || !var2.keySet().containsAll(h)
            || !var0.e().equals(var2.get("record"))
            || !(var2.get("expect") instanceof Map)
            || !(var2.get("at") instanceof Number)
            || !(var2.get("digest") instanceof String var3 && b(var2.get("req")) != null)) {
            throw c(var0.e(), "confirm.json is not a confirmation");
         } else if (!var3.equals(b(var2))) {
            throw c(var0.e(), "confirm.json's content is not what its digest was taken over");
         } else if (!var0.h().equals(var2.get("recordDigest"))) {
            throw c(var0.e(), "the record moved after it was confirmed — confirm it again");
         } else {
            return new io.karatelabs.ext.rules.twin.PlayRecordStore.PlayConfirmation(c(var2.get("expect")), b(var2.get("req")), var3);
         }
      } else {
         throw c(var0.e(), "no confirm.json beside the record — confirm it in the console's play view");
      }
   }

   public static Map<String, Object> a(Path var0, String var1, Map<String, Object> var2, List<String> var3, long var4) {
      io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var6 = a(var0, var1);
      io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var2, var6.d().size());
      if (var3 != null && b(var3) == null) {
         throw new io.karatelabs.ext.rules.RuleApiException(400, "SEQUENCE_INVALID", "_req: must be an array of criterion ids");
      } else {
         Path var7 = var6.f().resolve("confirm.json");
         if (!a(a(var0), var7)) {
            throw a(var1, "play record " + var1 + ": confirm.json is a link, not a file under " + a(var0));
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            var8.put("record", var1);
            var8.put("recordDigest", var6.h());
            var8.put("expect", io.karatelabs.ext.rules.twin.CanonicalJson.d(var2));
            var8.put("req", var3 == null ? List.of() : List.copyOf(var3));
            var8.put("at", var4);
            var8.put("digest", a(var8));

            try {
               Files.writeString(
                  var7,
                  io.karatelabs.ext.rules.twin.CanonicalJson.c(var8),
                  StandardCharsets.UTF_8,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.TRUNCATE_EXISTING,
                  StandardOpenOption.WRITE,
                  LinkOption.NOFOLLOW_LINKS
               );
               return var8;
            } catch (IOException var10) {
               throw new UncheckedIOException(var10);
            }
         }
      }
   }

   public static String a(Path var0, Map<String, Object> var1) {
      if (var1.get("record") instanceof String var3 && f.matcher(var3).matches()) {
         Path var4 = a(var0).resolve(var3).resolve("record.json");
         if (!a(a(var0), var4)) {
            return "differs";
         } else if (!Files.isRegularFile(var4, LinkOption.NOFOLLOW_LINKS)) {
            return "absent";
         } else {
            try {
               var var5 = b(var4);
               return var5 != null && a(var5).equals(var1.get("digest")) ? "present" : "differs";
            } catch (RuntimeException var6) {
               return "differs";
            }
         }
      } else {
         return "absent";
      }
   }

   static String a(Map<String, Object> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.remove("digest");
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
   }

   private static String b(Map<String, Object> var0) {
      try {
         return a(var0);
      } catch (SnapshotException var2) {
         return null;
      }
   }

   private static Path b(Path var0, String var1) {
      if (var1 != null && f.matcher(var1).matches()) {
         Path var2 = a(var0).resolve(var1).resolve("record.json");
         if (!a(a(var0), var2)) {
            throw a(var1, "play record " + var1 + " is a link, not a file under " + a(var0));
         } else {
            return var2;
         }
      } else {
         throw a(String.valueOf(var1), "'" + var1 + "' is not a play record id (play-<16 hex>)");
      }
   }

   static boolean a(Path var0, Path var1) {
      Path var2 = var0;

      for (Path var4 : var0.relativize(var1)) {
         var2 = var2.resolve(var4);
         if (Files.isSymbolicLink(var2)) {
            return false;
         }
      }

      return true;
   }

   private static Map<String, Object> b(Path var0) {
      String var1;
      try (InputStream var2 = Files.newInputStream(var0, LinkOption.NOFOLLOW_LINKS)) {
         var1 = new String(var2.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      }

      Object var9;
      try {
         var9 = Json.parseStrict(var1, true);
      } catch (RuntimeException var6) {
         return null;
      }

      return var9 instanceof Map var3 ? c(var3) : null;
   }

   private static boolean a(Object var0) {
      if (!(var0 instanceof List var1)) {
         return false;
      } else {
         Iterator var2 = var1.iterator();

         while (var2.hasNext()) {
            if (!(var2.next() instanceof Map var4)
               || !(var4.get("command") instanceof String)
               || !(var4.get("args") instanceof Map)
               || !(var4.get("disposition") instanceof String)
               || !(var4.get("exchange") instanceof Map var5)
               || !(var5.get("request") instanceof Map var6)
               || !(var6.get("method") instanceof String)
               || !(var6.get("path") instanceof String)
               || !(var5.get("response") instanceof Map)
               || !(var4.get("worldHashBefore") instanceof String)
               || !(var4.get("worldHashAfter") instanceof String)) {
               return false;
            }
         }

         return true;
      }
   }

   private static List<String> b(Object var0) {
      if (!(var0 instanceof List var1)) {
         return null;
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList(var1.size());
         Iterator var3 = var1.iterator();

         while (var3.hasNext()) {
            if (!(var3.next() instanceof String var5) || var5.isBlank()) {
               return null;
            }

            var2.add(var5);
         }

         return var2;
      }
   }

   private static io.karatelabs.ext.rules.RuleApiException a(String var0, String var1) {
      return new io.karatelabs.ext.rules.RuleApiException(404, "PLAY_NOTFOUND", var1).a(Map.of("record", var0));
   }

   private static io.karatelabs.ext.rules.RuleApiException b(String var0, String var1) {
      return new io.karatelabs.ext.rules.RuleApiException(409, "PLAY_TAMPERED", "play record " + var0 + ": " + var1).a(Map.of("record", var0));
   }

   private static io.karatelabs.ext.rules.RuleApiException c(String var0, String var1) {
      return new io.karatelabs.ext.rules.RuleApiException(409, "PLAY_UNCONFIRMED", "play record " + var0 + ": " + var1).a(Map.of("record", var0));
   }

   private static Map<String, Object> c(Object var0) {
      return (Map<String, Object>)var0;
   }

   public record PlayConfirmation(Map<String, Object> a, List<String> b, String c) {

      public PlayConfirmation(Map<String, Object> a, List<String> b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   public record PlayRecord(String e, Path f, Map<String, Object> g, String h) {

      public PlayRecord(String e, Path f, Map<String, Object> g, String h) {
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }

      public String a() {
         return String.valueOf(this.g.get("rulebook"));
      }

      public Object b() {
         return this.g.get("identity");
      }

      public Map<String, Object> c() {
         return (Map<String, Object>)this.g.get("manifest");
      }

      public List<Map<String, Object>> d() {
         return (List<Map<String, Object>>)this.g.get("steps");
      }

      public String e() {
         return this.e;
      }

      public Path f() {
         return this.f;
      }

      public Map<String, Object> g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }
}
