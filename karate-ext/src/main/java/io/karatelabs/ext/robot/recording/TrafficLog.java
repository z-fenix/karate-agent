package io.karatelabs.ext.robot.recording;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TrafficLog(List<Map<String, Object>> d, List<Map<String, Object>> e, Map<String, Object> f) {


   public static Object a(Path var0, String var1) {
      if (!Files.isRegularFile(var0)) {
         return a("no traffic log for '" + var1 + "': its traffic was not captured", "NOT_FOUND", 404);
      } else {
         List var2;
         try {
            var2 = Files.readAllLines(var0, StandardCharsets.UTF_8);
         } catch (IOException var14) {
            return a("could not read the traffic log for '" + var1 + "': " + var14.getMessage(), "NOT_FOUND", 404);
         }

         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();
         Map var5 = null;

         for (String var7 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (!var7.isBlank()) {
               Object var8;
               try {
                  var8 = Json.parseStrict(var7, true);
               } catch (RuntimeException var13) {
                  return a("the traffic log for '" + var1 + "' is not finalized: a line is not JSON", "UNFINALIZED", 422);
               }

               if (var8 instanceof Map var9) {
                  String var11 = String.valueOf(var9.get("row"));
                  switch (var11) {
                     case "action":
                        var3.add(var9);
                        break;
                     case "exchange":
                        var4.add(var9);
                        break;
                     case "capture":
                        var5 = var9;
                  }
               }
            }
         }

         if (var5 == null) {
            return a("the traffic log for '" + var1 + "' is not finalized: it has no capture summary", "UNFINALIZED", 422);
         } else {
            return Boolean.TRUE.equals(var5.get("pruned"))
               ? a("the traffic log for '" + var1 + "' was pruned: only its capture summary is kept", "PRUNED", 410)
               : new TrafficLog(var3, var4, var5);
         }
      }
   }

   private static Map<String, Object> a(String var0, String var1, int var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("error", var0);
      var3.put("code", var1);
      var3.put("status", var2);
      return var3;
   }

   public String a() {
      String var1 = null;

      for (Map var3 : this.e) {
         if ("Document".equals(var3.get("type")) && var3.get("request") instanceof Map var4) {
            String var8 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(String.valueOf(var4.get("url"))).c();
            if (var8 != null) {
               if (var3.get("after") instanceof Number var6 && var6.intValue() == 0) {
                  return var8;
               }

               if (var1 == null) {
                  var1 = var8;
               }
            }
         }
      }

      return var1;
   }

   public List<Map<String, Object>> b() {
      ArrayList var1 = new ArrayList();

      for (Map var3 : this.d) {
         LinkedHashMap var4 = new LinkedHashMap(var3);
         var4.remove("row");
         var4.remove("t");
         var1.add(var4);
      }

      return var1;
   }

   public Map<String, Object> c() {
      var var1 = new LinkedHashMap<>(this.f);
      var1.remove("row");
      return var1;
   }

   public List<Map<String, Object>> d() {
      return this.d;
   }

   public List<Map<String, Object>> e() {
      return this.e;
   }

   public Map<String, Object> f() {
      return this.f;
   }
}
