package io.karatelabs.ext.asyncapi;

import io.karatelabs.ext.coverage.KafkaAction;
import io.karatelabs.ext.openapi.JsonSchemaInferrer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ObservedAsyncApiBuilder {
   private ObservedAsyncApiBuilder() {
   }

   public static Map<String, Object> a(List<KafkaAction> var0, String var1, String var2, String var3) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.TopicObservation> var4 = new LinkedHashMap();

      for (KafkaAction var6 : var0) {
         if (var6.topic() != null) {
            io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.TopicObservation var7 = var4.computeIfAbsent(var6.topic(), var0x -> new io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.TopicObservation());
            String var8 = var6.direction() == null ? "publish" : var6.direction();
            var7.a.add(var8);
            if (var6.payload() != null) {
               var7.b.add(var6.payload());
            }

            if (var6.schema() != null && var7.c == null) {
               var7.c = var6.schema();
            }
         }
      }

      LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var20 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var21 = new LinkedHashMap();

      for (Entry var24 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         String var9 = (String)var24.getKey();
         io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.TopicObservation var10 = (io.karatelabs.ext.asyncapi.ObservedAsyncApiBuilder.TopicObservation)var24.getValue();
         String var11 = var10.c != null ? var10.c : b(var9);
         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("name", var11);
         if (!var10.b.isEmpty()) {
            var12.put("payload", io.karatelabs.ext.openapi.JsonSchemaInferrer.a(var10.b));
         }

         LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var13 = new LinkedHashMap();
         var13.put(var11, var12);
         LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
         var14.put("address", var9);
         var14.put("messages", var13);
         var20.put(var9, var14);

         for (String var16 : var10.a) {
            String var17 = "publish".equals(var16) ? "send" : "receive";
            String var18 = var16 + c(var9);
            LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
            var19.put("action", var17);
            var19.put("channel", a("#/channels/" + var9));
            var19.put("messages", List.of(a("#/channels/" + var9 + "/messages/" + var11)));
            var21.put(var18, var19);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
      var23.put("asyncapi", "3.0.0");
      LinkedHashMap<java.lang.String, java.lang.Object> var25 = new LinkedHashMap();
      var25.put("title", var1 != null && !var1.isBlank() ? var1 : "Observed Kafka API");
      var25.put("version", var2 != null && !var2.isBlank() ? var2 : "1.0.0");
      var23.put("info", var25);
      LinkedHashMap<java.lang.String, java.lang.Object> var26 = new LinkedHashMap();
      var26.put("host", var3 != null && !var3.isBlank() ? var3 : "localhost:9092");
      var26.put("protocol", "kafka");
      var23.put("servers", Map.of("broker", var26));
      var23.put("channels", var20);
      var23.put("operations", var21);
      return var23;
   }

   public static int a(Map<String, Object> var0) {
      return var0.get("channels") instanceof Map var1 ? var1.size() : 0;
   }

   public static int b(Map<String, Object> var0) {
      return var0.get("operations") instanceof Map var1 ? var1.size() : 0;
   }

   private static Map<String, Object> a(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("$ref", var0);
      return var1;
   }

   private static String b(String var0) {
      return c(var0) + "Message";
   }

   private static String c(String var0) {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = true;

      for (char var6 : var0.toCharArray()) {
         if (Character.isLetterOrDigit(var6)) {
            var1.append(var2 ? Character.toUpperCase(var6) : var6);
            var2 = false;
         } else {
            var2 = true;
         }
      }

      return var1.isEmpty() ? "Topic" : var1.toString();
   }

   private static final class TopicObservation {
      final LinkedHashSet<String> a = new LinkedHashSet<>();
      final List<Object> b = new ArrayList<>();
      String c;
   }
}
