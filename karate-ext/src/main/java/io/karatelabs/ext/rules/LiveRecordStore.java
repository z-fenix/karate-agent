package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class LiveRecordStore {
   public static final String a = "ADJUDICATION_EXISTS";
   private static final String f = ".baseline";
   private static final String g = "live.json";
   private static final String h = "adjudications.json";
   private static final int i = 3;
   private static final int j = 1;
   private static final String k = "runSha";
   private static final Set<String> l = Set.of("against", "twinSha", "inputSha", "rulebooks", "profileCeiling", "disposition", "log", "runSha");
   private static final String m = "hook";
   private static final String n = "sutRevision";
   private static final String o = "shrink";
   private static final String p = "shrinkSha";
   private static final Set<String> q = Set.of("steps", "attempted", "from", "to", "replays", "verified", "mismatch", "budgetExhausted", "runSha", "shrinkSha");
   private static final Set<String> r = Set.of("from", "to", "replays", "verified", "runSha", "shrinkSha");
   public static final String b = "OK";
   public static final String c = "DAMAGED";
   public static final String d = "FOREIGN_PARENT";
   public static final String e = "STALE_PARENT";
   private static final Set<String> s = Set.of("hash", "calls", "threw");
   private static final Set<String> t = Set.of("at", "kind", "profileAfter", "unknownAfter", "evidence");
   private static final Set<String> u = Set.of("command", "args", "profileBefore");
   private static final Set<String> v = Set.of("reset", "readBack", "command");
   private static final Set<String> w = Set.of("applied", "refused", "transportFailure", "unknown", "error");
   private static final Set<String> x = Set.of("key", "consistent", "resolved", "notChecked");
   private static final Set<String> y = Set.of("key", "model", "consistent", "successors", "captureError", "why");
   private static final Set<String> z = Set.of("kindMismatch", "reasonMismatch");
   private static final Set<String> A = Set.of("kind", "stage", "reason", "code");
   private static final Set<String> B = Set.of("key", "then", "invariants", "captures", "unknown");
   private static final Set<String> C = Set.of("PASS", "FAIL", "UNRESOLVED", "NOTCHECKED", "REFUSED", "INVALID");
   private static final Set<String> D = Set.of("kind", "code", "at", "reason", "mismatch", "candidates", "unresolvedCaptures", "evidence", "stderrTail");
   private static final Set<String> E = Set.of("method", "path", "body");
   private static final Set<String> F = Set.of("status", "body");
   private static final Set<String> G = Set.of("then", "invariants", "states");
   private static final Pattern H = Pattern.compile("[0-9a-f]{64}");
   private static final Map<Path, ReentrantLock> I = new ConcurrentHashMap<>();
   private final Path J;
   private final ReentrantLock K;

   public LiveRecordStore(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      this.J = var2.resolve(".baseline");
      this.K = I.computeIfAbsent(var2, var0 -> new ReentrantLock());
   }

   public io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot a() {
      this.K.lock();

      io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot var4;
      try {
         Path var1 = this.J.resolve("live.json");
         var var2 = a(var1);
         var var3 = a(var1, var2);
         var4 = new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot(var2 == null ? 0 : ((Number)var2.get("version")).intValue(), var3);
      } finally {
         this.K.unlock();
      }

      return var4;
   }

   public Map<String, Map<String, Object>> b() {
      return this.a().c();
   }

   public String a(Map<String, Map<String, Object>> var1) {
      this.K.lock();

      String var6;
      try {
         Path var2 = this.J.resolve("live.json");
         var var3 = a(var2, a(var2));
         if (var1 != null) {
            var3.putAll(var1);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("version", 3);
         var4.put("runs", var3);
         String var5 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var4);
         a(var2, var5);
         var6 = io.karatelabs.ext.rules.twin.CanonicalJson.a(var5);
      } finally {
         this.K.unlock();
      }

      return var6;
   }

   public static String b(Map<String, Object> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.remove("runSha");
      var1.remove("shrink");
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
   }

   public static Map<String, Object> a(Map<String, Object> var0, String var1) {
      var var2 = new LinkedHashMap<>(var0);
      var2.put("runSha", var1);
      var2.put("shrinkSha", c(var2));
      return var2;
   }

   private static String c(Map<?, ?> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.remove("shrinkSha");
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
   }

   public static String a(Map<String, Object> var0, boolean var1) {
      if (!var0.containsKey("shrink")) {
         return null;
      } else if (!b(var0.get("shrink"))) {
         return "DAMAGED";
      } else if (!((Map)var0.get("shrink")).get("runSha").equals(var0.get("runSha"))) {
         return "FOREIGN_PARENT";
      } else {
         return var1 ? "OK" : "STALE_PARENT";
      }
   }

   private static boolean b(Object var0) {
      if (var0 instanceof Map var1
         && q.containsAll(var1.keySet())
         && var1.keySet().containsAll(r)
         && ("CONFIRMED".equals(var1.get("verified")) || "UNSTABLE".equals(var1.get("verified")))
         && var1.get("runSha") instanceof String var2
         && H.matcher(var2).matches()
         && var1.get("shrinkSha") instanceof String var3
         && H.matcher(var3).matches()
         && a(var1.get("from"), 0, Integer.MAX_VALUE)
         && a(var1.get("to"), 0, Integer.MAX_VALUE)
         && a(var1.get("replays"), 0, Integer.MAX_VALUE)
         && (!var1.containsKey("mismatch") || var1.get("mismatch") instanceof Map)
         && (!var1.containsKey("budgetExhausted") || Boolean.TRUE.equals(var1.get("budgetExhausted")))) {
         String var6 = "CONFIRMED".equals(var1.get("verified")) ? "steps" : "attempted";
         return !var1.containsKey("CONFIRMED".equals(var1.get("verified")) ? "attempted" : "steps")
               && a(var1.get(var6), io.karatelabs.ext.rules.LiveRecordStore::d)
               && ((List)var1.get(var6)).size() == ((Number)var1.get("to")).intValue()
            ? var3.equals(c(var1))
            : false;
      } else {
         return false;
      }
   }

   private static boolean d(Map<?, ?> var0) {
      return Set.of("command", "args").containsAll(var0.keySet())
         && var0.get("command") instanceof String
         && (!var0.containsKey("args") || var0.get("args") instanceof Map);
   }

   public static boolean a(Object var0) {
      if (var0 instanceof Map var1
         && l.equals(a(var1.keySet()))
         && e(var1)
         && (var1.get("sutRevision") == null || var1.get("sutRevision") instanceof String)
         && ("mock".equals(var1.get("against")) || "live".equals(var1.get("against")))
         && var1.get("twinSha") instanceof String
         && var1.get("inputSha") instanceof String
         && var1.get("runSha") instanceof String
         && a(var1.get("profileCeiling"), 1, 8)
         && var1.get("disposition") instanceof Map var2
         && k(var2)
         && var1.get("log") instanceof List var3
         && var1.get("rulebooks") instanceof Map var4) {
         for (Object var6 : var3) {
            if (!c(var6)) {
               return false;
            }
         }

         for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            if (!(var13.getKey() instanceof String)
               || !(var13.getValue() instanceof Map var7)
               || !s.equals(var7.keySet())
               || !(var7.get("hash") instanceof String)
               || !a(var7.get("calls"), 0, Integer.MAX_VALUE)
               || !a(var7.get("threw"), 0, Integer.MAX_VALUE)) {
               return false;
            }
         }

         return b(var1).equals(var1.get("runSha"));
      } else {
         return false;
      }
   }

   private static Set<Object> a(Set<?> var0) {
      Set<Object> var1 = new LinkedHashSet<>(var0);
      var1.remove("hook");
      var1.remove("sutRevision");
      var1.remove("shrink");
      return var1;
   }

   private static boolean e(Map<?, ?> var0) {
      Object var1 = var0.get("hook");
      boolean var2 = var0.get("disposition") instanceof Map var3 && "RESET_HOOK_FAILED".equals(var3.get("code"));
      if (!var2 || var1 != null && var0.get("log") instanceof List var5 && var5.isEmpty()) {
         return var1 == null
            ? !var0.containsKey("hook")
            : var1 instanceof Map var6
               && var6.keySet().equals(Set.of("command", "exit"))
               && i(var6.get("command"))
               && !((List)var6.get("command")).isEmpty()
               && (var6.get("exit") == null || a(var6.get("exit"), Integer.MIN_VALUE, Integer.MAX_VALUE));
      } else {
         return false;
      }
   }

   private static boolean c(Object var0) {
      if (var0 instanceof Map var1
         && var1.keySet().containsAll(t)
         && a(var1.get("at"), -1, Integer.MAX_VALUE)
         && v.contains(var1.get("kind"))
         && i(var1.get("profileAfter"))
         && var1.get("unknownAfter") instanceof List var2
         && var1.get("evidence") instanceof String var3
         && H.matcher(var3).matches()
         && "command".equals(var1.get("kind")) == var1.keySet().containsAll(u)) {
         for (Object var5 : var2) {
            if (!i(var5)) {
               return false;
            }
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if (!(var6.getKey() instanceof String var7) || !a(String.valueOf(var1.get("kind")), var7, var6.getValue())) {
               return false;
            }

            if (!"evidence".equals(var7)) {
               var11.put(var7, var6.getValue());
            }
         }

         return var3.equals(io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var11)));
      } else {
         return false;
      }
   }

   private static boolean a(String var0, String var1, Object var2) {
      if (t.contains(var1)) {
         return true;
      } else {
         return switch (var0) {
            case "reset" -> {
               switch (var1) {
                  case "request":
                     yield e(var2);
                  case "observed":
                     yield f(var2);
                  default:
                     yield false;
               }
            }
            case "readBack" -> {
               switch (var1) {
                  case "request":
                     yield e(var2);
                  case "observed":
                     yield f(var2);
                  case "refused":
                     yield d(var2);
                  case "missing":
                  case "observeError":
                     yield var2 instanceof String;
                  case "candidates":
                     yield a(var2, io.karatelabs.ext.rules.LiveRecordStore::f);
                  default:
                     yield false;
               }
            }
            case "command" -> {
               switch (var1) {
                  case "command":
                     yield var2 instanceof String;
                  case "args":
                     yield var2 instanceof Map;
                  case "request":
                     yield e(var2);
                  case "observed":
                     yield f(var2);
                  case "properties":
                     yield g(var2);
                  case "profileBefore":
                     yield i(var2);
                  case "outcome":
                     yield w.contains(var2);
                  case "refused":
                     yield d(var2);
                  case "candidates":
                     yield a(var2, io.karatelabs.ext.rules.LiveRecordStore::g);
                  default:
                     yield false;
               }
            }
            default -> false;
         };
      }
   }

   private static boolean d(Object var0) {
      if (var0 instanceof Map var1) {
         String var2 = String.valueOf(var1.get("code"));

         return switch (var2) {
            case "AMBIGUOUS_REQUEST" -> var1.keySet().equals(Set.of("code", "requests")) && i(var1.get("requests"));
            case "PROFILE_OVERCAP" -> var1.keySet().equals(Set.of("code", "size", "ceiling"))
               && a(var1.get("size"), 0, Integer.MAX_VALUE)
               && a(var1.get("ceiling"), 1, 8);
            default -> false;
         };
      } else {
         return false;
      }
   }

   private static boolean e(Object var0) {
      return var0 instanceof Map var1
         && E.containsAll(var1.keySet())
         && var1.get("method") instanceof String
         && var1.get("path") instanceof String
         && (!var1.containsKey("body") || h(var1.get("body")));
   }

   private static boolean f(Object var0) {
      if (!(var0 instanceof Map var1 && e(var1.get("request")))) {
         return false;
      } else {
         return var1.keySet().equals(Set.of("request", "transportFailure"))
            ? Boolean.TRUE.equals(var1.get("transportFailure"))
            : var1.keySet().equals(Set.of("request", "response"))
               && var1.get("response") instanceof Map var2
               && F.containsAll(var2.keySet())
               && a(var2.get("status"), 0, Integer.MAX_VALUE)
               && (!var2.containsKey("body") || h(var2.get("body")));
      }
   }

   private static boolean g(Object var0) {
      if (var0 instanceof Map var1
         && G.equals(var1.keySet())
         && var1.get("then") instanceof String
         && var1.get("invariants") instanceof Map var2
         && var1.get("states") instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (!(var5.getKey() instanceof String) || !(var5.getValue() instanceof String)) {
               return false;
            }
         }

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            if (!(var9.getKey() instanceof String) || !(var9.getValue() instanceof Boolean)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean h(Object var0) {
      try {
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var0);
         return true;
      } catch (RuntimeException var2) {
         return false;
      }
   }

   private static boolean f(Map<?, ?> var0) {
      return x.equals(var0.keySet())
         && var0.get("key") instanceof String
         && var0.get("consistent") instanceof Boolean
         && i(var0.get("resolved"))
         && var0.get("notChecked") instanceof Boolean;
   }

   private static boolean g(Map<?, ?> var0) {
      return y.containsAll(var0.keySet())
         && var0.keySet().containsAll(Set.of("key", "model", "consistent", "successors"))
         && var0.get("key") instanceof String
         && b(var0, "captureError")
         && (!var0.containsKey("why") || z.contains(var0.get("why")))
         && (Boolean.TRUE.equals(var0.get("consistent")) || Boolean.FALSE.equals(var0.get("consistent")) || "notChecked".equals(var0.get("consistent")))
         && var0.get("model") instanceof Map var1
         && A.containsAll(var1.keySet())
         && var1.get("kind") instanceof String
         && b(var1, "stage")
         && b(var1, "code")
         && (!var1.containsKey("reason") || var1.get("reason") == null || var1.get("reason") instanceof String)
         && a(var0.get("successors"), io.karatelabs.ext.rules.LiveRecordStore::h);
   }

   private static boolean h(Map<?, ?> var0) {
      return B.containsAll(var0.keySet())
         && var0.get("key") instanceof String
         && i(var0.get("unknown"))
         && b(var0, "then")
         && (!var0.containsKey("invariants") || a(var0.get("invariants"), io.karatelabs.ext.rules.LiveRecordStore::i))
         && (!var0.containsKey("captures") || a(var0.get("captures"), io.karatelabs.ext.rules.LiveRecordStore::j));
   }

   private static boolean i(Map<?, ?> var0) {
      return var0.keySet().equals(Set.of("name", "status")) && var0.get("name") instanceof String && var0.get("status") instanceof String;
   }

   private static boolean j(Map<?, ?> var0) {
      return var0.keySet().equals(Set.of("field", "value")) && var0.get("field") instanceof String;
   }

   private static boolean k(Map<?, ?> var0) {
      return C.contains(var0.get("kind"))
         && D.containsAll(var0.keySet())
         && b(var0, "code")
         && b(var0, "reason")
         && b(var0, "stderrTail")
         && (!var0.containsKey("at") || a(var0.get("at"), 0, Integer.MAX_VALUE))
         && (!var0.containsKey("mismatch") || var0.get("mismatch") instanceof Map)
         && (!var0.containsKey("candidates") || i(var0.get("candidates")))
         && (!var0.containsKey("unresolvedCaptures") || i(var0.get("unresolvedCaptures")))
         && (!var0.containsKey("evidence") || var0.get("evidence") instanceof String var1 && H.matcher(var1).matches());
   }

   private static boolean a(Object var0, Predicate<Map<?, ?>> var1) {
      if (!(var0 instanceof List var2)) {
         return false;
      } else {
         Iterator var3 = var2.iterator();

         while (var3.hasNext()) {
            if (!(var3.next() instanceof Map var5) || !var1.test(var5)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean b(Map<?, ?> var0, String var1) {
      return !var0.containsKey(var1) || var0.get(var1) instanceof String;
   }

   private static boolean i(Object var0) {
      return var0 instanceof List var1 && var1.stream().allMatch(var0x -> var0x instanceof String);
   }

   private static boolean a(Object var0, int var1, int var2) {
      return var0 instanceof Number var3 && var3.doubleValue() == Math.rint(var3.doubleValue()) && var3.doubleValue() >= var1 && var3.doubleValue() <= var2;
   }

   public List<Map<String, Object>> c() {
      this.K.lock();

      List var2;
      try {
         Path var1 = this.J.resolve("adjudications.json");
         var2 = b(var1, a(var1));
      } finally {
         this.K.unlock();
      }

      return var2;
   }

   public Map<String, Object> a(String var1) {
      for (Map var3 : this.c()) {
         if (var3.get("evidence") != null && var3.get("evidence").equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public Map<String, Object> a(String var1, String var2, String var3) {
      this.K.lock();

      LinkedHashMap var8;
      try {
         Path var4 = this.J.resolve("adjudications.json");
         var var5 = b(var4, a(var4));

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (var5)) {
            if (var1.equals(var7.get("evidence"))) {
               throw new io.karatelabs.ext.rules.twin.TwinException(
                  409,
                  "ADJUDICATION_EXISTS",
                  "evidence '"
                     + var1
                     + "' is already adjudicated as '"
                     + var7.get("disposition")
                     + "' — reverse it by editing "
                     + var4
                     + ", the append-only file under review"
               );
            }
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("evidence", var1);
         var12.put("disposition", var2);
         if (var3 != null) {
            var12.put("note", var3);
         }

         var12.put("ts", System.currentTimeMillis());
         var5.add(var12);
         LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
         var13.put("version", 1);
         var13.put("adjudications", var5);
         a(var4, io.karatelabs.ext.rules.twin.CanonicalJson.c(var13));
         var8 = var12;
      } finally {
         this.K.unlock();
      }

      return var8;
   }

   public Map<String, Object> a(String var1, String var2, String var3, Supplier<RuntimeException> var4) {
      this.K.lock();

      Map var6;
      try {
         Path var5 = this.J.resolve("live.json");
         if (!l(a(var5, a(var5))).contains(var1)) {
            throw (RuntimeException)var4.get();
         }

         var6 = this.a(var1, var2, var3);
      } finally {
         this.K.unlock();
      }

      return var6;
   }

   private static Set<String> l(Map<String, Map<String, Object>> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (Map var3 : var0.values()) {
         Object var5 = var3.get("log");
         if (var5 instanceof List) {
            for (Object var6 : (List)var5) {
               if (var6 instanceof Map var7 && var7.get("evidence") instanceof String var8) {
                  var1.add(var8);
               }
            }
         }
      }

      return var1;
   }

   private static Map<String, Object> a(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return null;
      } else {
         Object var1;
         try {
            var1 = Json.parseStrict(Files.readString(var0, StandardCharsets.UTF_8), true);
         } catch (RuntimeException | IOException var4) {
            throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, var4.toString());
         }

         if (var1 instanceof Map var2 && a(var2.get("version"), 1, Integer.MAX_VALUE)) {
            return var2;
         } else {
            throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "not a versioned record document");
         }
      }
   }

   private static Map<String, Map<String, Object>> a(Path var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();
      if (var1 == null) {
         return var2;
      } else if (!a(var1.get("version"), 1, 3)) {
         throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "not a version 3 live record document");
      } else if (!a(var1.get("version"), 3, 3)) {
         return var2;
      } else if (!(var1.get("runs") instanceof Map var3)) {
         throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "no runs object");
      } else {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            if (!(var5.getKey() instanceof String var6) || !(var5.getValue() instanceof Map)) {
               throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "a run is not an object keyed by sequence id");
            }

            var2.put(var6, (Map)var5.getValue());
         }

         return var2;
      }
   }

   private static List<Map<String, Object>> b(Path var0, Map<String, Object> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var1 == null) {
         return var2;
      } else if (!a(var1.get("version"), 1, 1)) {
         throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "not a version 1 adjudications document");
      } else if (!(var1.get("adjudications") instanceof List var3)) {
         throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "no adjudications array");
      } else {
         Iterator var7 = var3.iterator();

         while (var7.hasNext()) {
            if (!(var7.next() instanceof Map var6) || !(var6.get("evidence") instanceof String)) {
               throw new io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException(var0, "an adjudication carries no evidence key");
            }

            var2.add(var6);
         }

         return var2;
      }
   }

   static void a(Path var0, String var1) {
      try {
         Files.createDirectories(var0.getParent());
         Path var2 = io.karatelabs.ext.common.TempFileCreator.a(var0.getParent(), var0.getFileName().toString(), ".tmp");

         try {
            Files.writeString(var2, var1, StandardCharsets.UTF_8);

            try {
               Files.move(var2, var0, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException var8) {
               Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
            }
         } finally {
            Files.deleteIfExists(var2);
         }
      } catch (IOException var10) {
         throw new UncheckedIOException(var10);
      }
   }

   public static class LiveRecordException extends RuntimeException {
      private final Path a;

      LiveRecordException(Path var1, String var2) {
         super("the live record at " + var1 + " cannot be read (" + var2 + ")");
         this.a = var1;
      }

      public Path a() {
         return this.a;
      }
   }

   public record LiveRecordSnapshot(int b, Map<String, Map<String, Object>> c) {

      public LiveRecordSnapshot(int b, Map<String, Map<String, Object>> c) {
         this.b = b;
         this.c = c;
      }

      public boolean a() {
         return this.b > 0 && this.b < 3;
      }

      public int b() {
         return this.b;
      }

      public Map<String, Map<String, Object>> c() {
         return this.c;
      }
   }
}
