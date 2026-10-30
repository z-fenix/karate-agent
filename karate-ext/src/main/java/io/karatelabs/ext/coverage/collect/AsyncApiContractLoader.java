package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.DataUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.ext.contract.Address;
import io.karatelabs.ext.contract.AsyncApiBinding;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Interaction;
import io.karatelabs.ext.contract.KafkaBinding;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.contract.Service;
import io.karatelabs.ext.contract.TypeModel;
import io.karatelabs.ext.contract.TypeModelResolver;
import io.karatelabs.ext.contract.TypeRef;
import io.karatelabs.ext.openapi.OpenApiDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class AsyncApiContractLoader implements ContractLoader {
   private static final Map<String, TypeModelResolver> a = a();

   private static Map<String, TypeModelResolver> a() {
      LinkedHashMap var0 = new LinkedHashMap();

      for (TypeModelResolver var2 : ServiceLoader.load(TypeModelResolver.class)) {
         if (var2.format() != null) {
            var0.putIfAbsent(var2.format().toLowerCase(), var2);
         }
      }

      return var0;
   }

   @Override
   public Protocol protocol() {
      return Protocol.ASYNCAPI;
   }

   @Override
   public Contract load(List<Path> var1, ContractLoader.LoaderOptions var2) {
      if (var1 != null && !var1.isEmpty()) {
         return a((Path)var1.getFirst());
      } else {
         throw new IllegalArgumentException("AsyncApiContractLoader needs at least one AsyncAPI artifact");
      }
   }

   public static Contract a(Path var0) {
      try {
         Path var1 = var0.toAbsolutePath().getParent();
         return a(Files.readString(var0, StandardCharsets.UTF_8), var1);
      } catch (IOException var2) {
         throw new RuntimeException("cannot read AsyncAPI doc: " + var0, var2);
      }
   }

   public static Contract a(String var0) {
      return a(var0, null);
   }

   public static Contract a(String var0, Path var1) {
      if (!(DataUtils.fromYaml(var0) instanceof Map var3)) {
         throw new IllegalArgumentException("AsyncAPI doc is not a YAML/JSON object");
      } else {
         String var4 = String.valueOf(var3.getOrDefault("asyncapi", ""));
         if (!var4.isBlank() && var3.containsKey("channels")) {
            Protocol var5 = a(var3);
            List var6 = var4.startsWith("2.") ? b(var3, var1) : a(var3, var1);
            LinkedHashMap var7 = new LinkedHashMap();

            for (Operation var9 : (Iterable<Operation>) (Iterable<?>) (var6)) {
               var7.putIfAbsent(var9.id(), var9);
            }

            LinkedHashSet var13 = new LinkedHashSet();

            for (Operation var10 : (Iterable<Operation>) (Iterable<?>) (var7.values())) {
               var13.add(var10.address().primary());
            }

            String var15 = d(c(var3.get("info")).get("title"));
            String var16 = d(c(var3.get("info")).get("version"));
            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("specType", "asyncapi");
            var11.put("asyncapiVersion", var4);
            var11.put("topics", new ArrayList(var13));
            var var12 = List.of(new Service(var15 == null ? "asyncapi" : var15, new ArrayList<>(var7.values())));
            return new Contract(var15 == null ? "asyncapi" : var15, var5, var15 == null ? "asyncapi" : var15, var16, var12, var11);
         } else {
            throw new IllegalArgumentException("not an AsyncAPI doc (no asyncapi: version / channels)");
         }
      }
   }

   private static List<Operation> a(Map<String, Object> var0, Path var1) {
      var var2 = c(var0.get("operations"));
      ArrayList var3 = new ArrayList();

      for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         var var6 = c(var5.getValue());
         String var7 = d(var6.get("action"));
         if (var7 != null) {
            String var8 = "send".equals(var7) ? "publish" : "subscribe";
            String var9 = b(c(var6.get("channel")).get("$ref"));
            var var10 = b(var0, var6.get("channel"));
            String var11 = a(var10, var9);
            ArrayList var12 = var6.get("messages") instanceof List var13 ? new ArrayList(var13) : new ArrayList<>(c(var10.get("messages")).values());
            String var15 = var6.containsKey("reply") ? b(c(c(var6.get("reply")).get("channel")).get("$ref")) : null;
            String var16 = var6.containsKey("reply") ? d(c(c(var6.get("reply")).get("address")).get("location")) : null;
            var3.add(a(var0, var11, var8, var12, var10, var9, var15, var16, var1));
         }
      }

      return var3;
   }

   private static List<Operation> b(Map<String, Object> var0, Path var1) {
      var var2 = c(var0.get("channels"));
      ArrayList var3 = new ArrayList();

      for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         String var6 = (String)var5.getKey();
         var var7 = c(var5.getValue());
         String var8 = a(var7, var6);

         for (String var10 : List.of("publish", "subscribe")) {
            if (var7.containsKey(var10)) {
               var var11 = c(var7.get(var10));
               String var12 = "publish".equals(var10) ? "subscribe" : "publish";
               ArrayList var13 = new ArrayList();
               if (var11.containsKey("message")) {
                  var13.add(var11.get("message"));
               }

               var3.add(a(var0, var8, var12, var13, var7, var6, null, null, var1));
            }
         }
      }

      return var3;
   }

   private static Operation a(
      Map<String, Object> var0, String var1, String var2, List<Object> var3, Map<String, Object> var4, String var5, String var6, String var7, Path var8
   ) {
      String var9 = var1 + "#" + var2;
      Address var10 = new Address(Address.Kind.TOPIC, var1, var2);
      LinkedHashSet var11 = new LinkedHashSet();

      for (Object var13 : var3) {
         var11.addAll(a(var0, var13));
      }

      String var19 = var11.isEmpty() ? null : String.join(",", var11);
      boolean var20 = "publish".equals(var2);
      Interaction var14 = var6 != null ? Interaction.REQUEST_RESPONSE : (var20 ? Interaction.PUBLISH : Interaction.SUBSCRIBE);
      ArrayList var15 = new ArrayList();
      if (var19 != null) {
         io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema var16 = a(var0, var3);
         var15.add(new Message(var19, a(var0, var19, var16, var8), null, Message.Role.VALUE, a(var16)));
      }

      Object var21 = var20 ? var15 : List.of();
      Object var17 = var20 ? List.of() : var15;
      LinkedHashMap var18 = new LinkedHashMap();
      var18.put("binding", KafkaBinding.of(var1, var20 ? KafkaBinding.Direction.PUBLISH : KafkaBinding.Direction.SUBSCRIBE, var19, null));
      var18.put("asyncBinding", new AsyncApiBinding(var5, var6, var7));
      var18.put("schema", var19);
      var18.put("key", null);
      return new Operation(var9, var10, var14, (List<Message>)var21, (List<Message>)var17, List.of(), null, List.of(var1), false, var18);
   }

   private static String a(Map<String, Object> var0, String var1) {
      Object var2 = c(c(var0.get("bindings")).get("kafka")).get("topic");
      if (var2 != null) {
         return String.valueOf(var2);
      } else {
         Object var3 = var0.get("address");
         return var3 != null ? String.valueOf(var3) : var1;
      }
   }

   private static List<String> a(Map<String, Object> var0, Object var1) {
      var var2 = b(var0, var1);
      Object var3 = var2.get("payload");
      ArrayList var4 = new ArrayList();
      if (var3 instanceof Map var5) {
         if (var5.containsKey("$ref")) {
            var4.add(b(var5.get("$ref")));
         } else {
            Object var8 = var5.get("oneOf");
            if (var8 instanceof List) {
               for (Object var9 : (List)var8) {
                  String var10 = b(c(var9).get("$ref"));
                  var4.add(var10 != null ? var10 : "inline");
               }
            } else {
               var8 = d(var2.get("name"));
               var4.add(var8 != null ? var8 : "inline");
            }
         }
      } else if (var2.get("name") != null) {
         var4.add(d(var2.get("name")));
      }

      var4.removeIf(Objects::isNull);
      return var4;
   }

   private static io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema a(Map<String, Object> var0, List<Object> var1) {
      if (var1.isEmpty()) {
         return null;
      } else {
         var var2 = b(var0, var1.getFirst());
         if (!(var2.get("payload") instanceof Map)) {
            return null;
         } else {
            var var3 = c(var2.get("payload"));
            String var4 = d(var2.get("schemaFormat"));
            Map var5;
            if (var3.containsKey("schemaFormat") && var3.get("schema") instanceof Map) {
               var4 = d(var3.get("schemaFormat"));
               var5 = c(var3.get("schema"));
            } else {
               var5 = var3;
            }

            if (var5.get("oneOf") instanceof List var6 && !var6.isEmpty()) {
               var5 = c(var6.getFirst());
            }

            String var8 = var4 != null && var4.toLowerCase().contains("avro") ? "avro" : "json-schema";
            return new io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema(var5, var4, var8, a(var5.get("$ref")));
         }
      }
   }

   private static String a(Object var0) {
      return var0 instanceof String var1 && !var1.isBlank() && !var1.startsWith("#/") && !var1.contains("://") ? var1 : null;
   }

   private static Map<String, Object> a(io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema var0) {
      if (var0 == null) {
         return Map.of();
      } else {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("format", var0.c);
         if (var0.b != null) {
            var1.put("schemaFormat", var0.b);
         }

         if (var0.d != null) {
            var1.put("schemaFile", var0.d);
         }

         return var1;
      }
   }

   private static TypeRef a(Map<String, Object> var0, String var1, io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema var2, Path var3) {
      if (var2 == null || var2.a.isEmpty()) {
         return TypeRef.nameOnly(var1);
      } else if ("avro".equals(var2.c)) {
         String var6 = a(var2, var3);
         TypeModelResolver var5 = a.get("avro");
         return var6 != null && var5 != null ? new TypeRef(var1, a(() -> var5.resolve(var6))) : TypeRef.nameOnly(var1);
      } else {
         var var4 = var2.a;
         return new TypeRef(var1, a(() -> {
            io.karatelabs.ext.openapi.SchemaDefinition var2x = new OpenApiDocument((Resource)null, var0).resolveSchemaReference(null, var4);
            return var2x == null ? null : var2x.getRootNode();
         }));
      }
   }

   private static String a(io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.AsyncApiPayloadSchema var0, Path var1) {
      if (var0.d != null) {
         if (var1 == null) {
            return null;
         } else {
            try {
               return Files.readString(var1.resolve(var0.d), StandardCharsets.UTF_8);
            } catch (RuntimeException | IOException var3) {
               return null;
            }
         }
      } else {
         return var0.a.isEmpty() ? null : Json.of(var0.a).toString();
      }
   }

   private static Supplier<TypeModel> a(final Supplier<TypeModel> var0) {
      return new Supplier<TypeModel>() {
         private TypeModel b;
         private boolean c;

         @Override
         public synchronized TypeModel get() {
            if (!this.c) {
               this.c = true;

               try {
                  this.b = (TypeModel)var0.get();
               } catch (RuntimeException var2) {
                  this.b = null;
               }
            }

            return this.b;
         }
      };
   }

   private static Protocol a(Map<String, Object> var0) {
      boolean var1 = false;

      for (Object var3 : c(var0.get("servers")).values()) {
         String var4 = d(c(var3).get("protocol"));
         if (var4 != null) {
            String var5 = var4.toLowerCase();
            if (var5.startsWith("kafka")) {
               return Protocol.KAFKA;
            }

            if (var5.equals("ws") || var5.equals("wss")) {
               var1 = true;
            }
         }
      }

      return var1 ? Protocol.WEBSOCKET : Protocol.ASYNCAPI;
   }

   private static Map<String, Object> b(Map<String, Object> var0, Object var1) {
      var var2 = c(var1);
      int var3 = 0;

      while (var2.get("$ref") instanceof String var4 && var3++ < 10) {
         var2 = b(var0, var4);
      }

      return var2;
   }

   private static Map<String, Object> b(Map<String, Object> var0, String var1) {
      if (var1 != null && var1.startsWith("#/")) {
         Object var2 = var0;

         for (String var6 : var1.substring(2).split("/")) {
            String var7 = var6.replace("~1", "/").replace("~0", "~");
            if (!(var2 instanceof Map)) {
               return Map.of();
            }

            var2 = ((Map)var2).get(var7);
         }

         return c(var2);
      } else {
         return Map.of();
      }
   }

   private static String b(Object var0) {
      if (var0 instanceof String var1 && !var1.isBlank()) {
         int var2 = var1.lastIndexOf(47);
         return var2 >= 0 ? var1.substring(var2 + 1) : var1;
      } else {
         return null;
      }
   }

   private static Map<String, Object> c(Object var0) {
      return var0 instanceof Map ? (Map)var0 : Map.of();
   }

   private static String d(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private record AsyncApiPayloadSchema(Map<String, Object> a, String b, String c, String d) {

   }
}
