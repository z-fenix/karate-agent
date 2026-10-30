package io.karatelabs.ext.robot.recording;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CaptureArtifactWriter {
   private static final Logger a = LoggerFactory.getLogger(CaptureArtifactWriter.class);
   private static final Pattern b = Pattern.compile("(?i)((?:bearer|basic|digest|token)\\s+)(\\S+)");
   private static final Set<String> c = Set.of("full", "capped", "binary", "skipped", "unavailable");
   private final List<Map<String, Object>> d;
   private final List<Map<String, Object>> e;
   private final long f;
   private final int g;
   private final int h;
   private final Map<String, List<String>> i;
   private final boolean j;
   private final io.karatelabs.ext.robot.recording.CaptureArtifactWriter.CaptureCompletionStats k;

   CaptureArtifactWriter(List<Map<String, Object>> var1, List<Map<String, Object>> var2, long var3, int var5, int var6, Map<String, List<String>> var7, boolean var8, io.karatelabs.ext.robot.recording.CaptureArtifactWriter.CaptureCompletionStats var9) {
      this.d = var1;
      this.e = var2;
      this.f = var3;
      this.g = var5;
      this.h = var6;
      this.i = var7;
      this.j = var8;
      this.k = var9;
   }

   static Map<String, Object> a(String var0, String var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("row", "exchange");
      var3.put("id", var0);
      var3.put("lost", var1);
      var3.put("starts", var2);
      return var3;
   }

   static Map<String, Object> a(io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("row", "action");
      var2.put("seq", var0.a());
      var2.put("t", var0.b());
      Map<java.lang.String, Object> var3 = var0.d() == null ? Map.of() : var0.d();
      var2.put("kind", var3.get("action"));

      for (String var5 : List.of("tag", "name", "role", "locator", "url", "value", "secretError")) {
         if (var3.get(var5) != null) {
            var2.put(var5, var3.get(var5));
         }
      }

      if (var1 != null) {
         var2.put("note", var1);
      }

      return var2;
   }

   Map<String, Object> a(Path var1, String var2, List<String> var3, io.karatelabs.ext.agent.llm.SecretRedactor var4, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var5) {
      var var6 = new ArrayList<>(this.d);
      var6.addAll(this.e);

      for (Map var8 : (Iterable<Map>) (Iterable<?>) (var6)) {
         a(var8, var4, var5);
      }

      ArrayList<java.lang.String> var19 = new ArrayList();
      ArrayList<Map<String, Object>> var20 = new ArrayList<>();
      int var9 = 0;

      for (Map var11 : (Iterable<Map>) (Iterable<?>) (var6)) {
         String var12 = a(var11, var4, var5);
         Map var13 = var11;
         if (var12 == null) {
            var13 = "exchange".equals(var11.get("row"))
               ? a(
                  String.valueOf(var11.get("id")),
                  "mask",
                  var11.get("hops") instanceof Number var15 ? var15.intValue() : (var11.get("starts") instanceof Number var14 ? var14.intValue() : 0)
               )
               : a(var11);
            if ("exchange".equals(var11.get("row"))) {
               var9++;
            }

            var12 = b(var13);
         }

         var19.add(var12);
         var20.add(var13);
      }

      Map<java.lang.String, java.lang.Object> var21 = this.a(var20, var2, var3, var9);
      var19.add(b(var21));

      try {
         Files.createDirectories(var1.getParent());
         Path var22 = var1.resolveSibling(var1.getFileName() + ".tmp");
         Files.write(var22, var19, StandardCharsets.UTF_8);

         try {
            Files.move(var22, var1, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var17) {
            Files.move(var22, var1, StandardCopyOption.REPLACE_EXISTING);
         }

         return var21;
      } catch (IOException var18) {
         throw new UncheckedIOException(var18);
      }
   }

   static void a(Path var0, int var1, long var2) throws IOException {
      ArrayList<java.nio.file.Path> var4 = new ArrayList();

      try (var var5 = Files.list(var0)) {
         for (Path var7 : (Iterable<Path>) (Iterable<?>) (var5.toList())) {
            Path var8 = var7.resolve("traffic.jsonl");
            if (Files.isRegularFile(var8)) {
               var4.add(var8);
            }
         }
      }

      var4.sort(Comparator.<Path, FileTime>comparing(var0x -> {
         try {
            return Files.getLastModifiedTime(var0x);
         } catch (IOException var2x) {
            return FileTime.fromMillis(0L);
         }
      }).reversed());
      int var18 = 0;
      long var19 = 0L;

      for (Path var9 : (Iterable<Path>) (Iterable<?>) (var4)) {
         var var10 = Files.readAllLines(var9, StandardCharsets.UTF_8);
         if (!var10.isEmpty() && Json.parseStrict((String)var10.get(var10.size() - 1), true) instanceof Map var11 && !var11.containsKey("pruned")) {
            long var21 = Files.size(var9);
            if (var18 < var1 && var19 + var21 <= var2) {
               var18++;
               var19 += var21;
            } else {
               LinkedHashMap<String, Object> var14 = new LinkedHashMap<>(var11);
               var14.put("pruned", true);
               Path var15 = var9.resolveSibling(var9.getFileName() + ".tmp");
               Files.write(var15, List.of(b(var14)), StandardCharsets.UTF_8);
               Files.move(var15, var9, StandardCopyOption.REPLACE_EXISTING);
            }
         }
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("row", "action");
      var1.put("seq", var0.get("seq"));
      var1.put("lost", "mask");
      return var1;
   }

   private static String a(Map<String, Object> var0, io.karatelabs.ext.agent.llm.SecretRedactor var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      try {
         var var3 = c(var0);
         b(var3, var1, var2);
         return var1.redactWithAliasRegistry(b(var3), var2);
      } catch (RuntimeException var4) {
         a.warn("traffic row {} dropped by the mask: {}", var0.get("id"), var4.toString());
         return null;
      }
   }

   private static String b(Map<String, Object> var0) {
      return StringUtils.formatJson(var0, false, false, false);
   }

   private static void b(Map<String, Object> var0, io.karatelabs.ext.agent.llm.SecretRedactor var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      for (String var4 : List.of("request", "response")) {
         if (var0.get(var4) instanceof Map var5 && var5.get("headers") instanceof Map var6) {
            for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
               if (var8.getValue() instanceof List var9) {
                  ArrayList<java.lang.String> var24 = new ArrayList();

                  for (Object var12 : var9) {
                     var24.add(a((String)var8.getKey(), String.valueOf(var12), var1, var2));
                  }

                  var8.setValue(var24);
               }
            }
         }
      }

      if (var0.get("redirects") instanceof List var13) {
         for (Object var16 : var13) {
            if (var16 instanceof Map var17 && var17.get("setCookie") instanceof List var20) {
               ArrayList<java.lang.String> var22 = new ArrayList();

               for (Object var25 : var20) {
                  var22.add(a("set-cookie", String.valueOf(var25), var1, var2));
               }

               var17.put("setCookie", var22);
            }
         }
      }
   }

   static String a(String var0, String var1, io.karatelabs.ext.agent.llm.SecretRedactor var2, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var3) {
      String var4 = var0.toLowerCase(Locale.ROOT);
      switch (var4) {
         case "authorization":
         case "proxy-authorization":
            if (var1.isBlank()) {
               return var1;
            }

            Matcher var12 = b.matcher(var1);
            return var12.matches() ? var12.group(1) + var2.aliasSecretValue(var12.group(2), var3) : var2.aliasSecretValue(var1, var3);
         case "cookie":
            StringBuilder var11 = new StringBuilder();

            for (String var10 : var1.split(";")) {
               if (var11.length() > 0) {
                  var11.append("; ");
               }

               var11.append(a(var10.trim(), var2, var3));
            }

            return var11.toString();
         case "set-cookie":
            int var6 = var1.indexOf(59);
            String var7 = var6 < 0 ? var1 : var1.substring(0, var6);
            return a(var7.trim(), var2, var3) + (var6 < 0 ? "" : var1.substring(var6));
         default:
            return var1;
      }
   }

   private static String a(String var0, io.karatelabs.ext.agent.llm.SecretRedactor var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      int var3 = var0.indexOf(61);
      return var3 >= 0 && var3 != var0.length() - 1 ? var0.substring(0, var3 + 1) + var1.aliasSecretValue(var0.substring(var3 + 1), var2) : var0;
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      return (Map<String, Object>)Json.copy(var0, true, false);
   }

   private Map<String, Object> a(List<Map<String, Object>> var1, String var2, List<String> var3, int var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("row", "capture");
      var5.put("started", this.f);
      var5.put("finished", this.k.a);
      var5.put("failed", this.k.b);
      var5.put("inFlightAtStop", this.k.c);
      LinkedHashMap<java.lang.String, java.lang.Integer> var6 = new LinkedHashMap();
      var6.put("starts", this.g);
      var6.put("other", this.h);
      var5.put("dropped", var6);
      LinkedHashMap<java.lang.String, java.lang.Integer> var7 = new LinkedHashMap();

      for (String var9 : List.of("full", "capped", "binary", "skipped", "unavailable")) {
         var7.put(var9, 0);
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var16 = new LinkedHashMap();
      var16.put("queue", 0);
      var16.put("overLimit", 0);
      var16.put("drain", 0);
      var16.put("mask", var4);
      int var17 = 0;

      for (Map var11 : var1) {
         if ("exchange".equals(var11.get("row"))) {
            if (var11.get("lost") instanceof String var21) {
               switch (var21) {
                  case "queue":
                     var16.merge("queue", 1, Integer::sum);
                     break;
                  case "over-limit":
                     var16.merge("overLimit", 1, Integer::sum);
                     break;
                  case "drain":
                     var16.merge("drain", 1, Integer::sum);
               }
            } else {
               if (var11.get("bodyState") instanceof Map var12) {
                  for (Object var14 : List.of(var12.get("request"), var12.get("response"))) {
                     if (var14 instanceof String var15 && c.contains(var15)) {
                        var7.merge(var15, 1, Integer::sum);
                     }
                  }
               }

               if (var11.get("headersPartial") instanceof List var20) {
                  var17 += var20.size();
               }
            }
         }
      }

      var5.put("bodies", var7);
      var5.put("lost", var16);
      var5.put("headersPartial", var17);
      var5.put("uncaptured", this.i);
      var5.put("ended", var2);
      if (!var3.isEmpty()) {
         var5.put("errors", List.copyOf(var3));
      }

      boolean var18 = var16.values().stream().mapToInt(Integer::intValue).sum() == 0;
      boolean var19 = this.i.values().stream().allMatch(List::isEmpty);
      var5.put("complete", "stop".equals(var2) && var3.isEmpty() && var18 && this.g == 0 && this.h == 0 && this.k.c == 0 && var17 == 0 && var19 && !this.k.d);
      if (this.j) {
         var5.put("attribution", "uncertain");
      }

      return var5;
   }

   record CaptureCompletionStats(int a, int b, int c, boolean d) {
   }
}
