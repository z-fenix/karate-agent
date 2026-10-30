package io.karatelabs.ext.openapi;

import io.karatelabs.ext.contract.Address;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.HttpBinding;
import io.karatelabs.ext.contract.Interaction;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Outcome;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.contract.Service;
import io.karatelabs.ext.contract.TypeRef;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

public class OpenApiContractLoader implements ContractLoader {
   @Override
   public Protocol protocol() {
      return Protocol.OPENAPI;
   }

   @Override
   public Contract load(List<Path> var1, ContractLoader.LoaderOptions var2) {
      if (var1 != null && !var1.isEmpty()) {
         return a(new OpenApiDocument(((Path)var1.getFirst()).toFile()));
      } else {
         throw new IllegalArgumentException("OpenApiContractLoader needs at least one spec artifact");
      }
   }

   public static Contract a(OpenApiDocument var0) {
      return a(var0, null);
   }

   public static Contract a(OpenApiDocument var0, List<String> var1) {
      ArrayList<io.karatelabs.ext.contract.Operation> var2 = new ArrayList();

      for (Entry var4 : var0.paths.entrySet()) {
         OpenApiPathItem var5 = (OpenApiPathItem)var4.getValue();

         for (OpenApiOperation var7 : var5.e) {
            var2.add(a((String)var4.getKey(), var7, var0));
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("specType", var0.specType);
      var8.put("description", var0.description);
      var8.put("baseUrl", var0.defaultServerUrl());
      var8.put("basePaths", var1 == null ? var0.basePaths() : var1);
      var8.put("specPaths", new ArrayList<>(var0.paths.keySet()));
      var var9 = List.of(new Service(var0.title, var2));
      return new Contract(var0.title, Protocol.OPENAPI, var0.title, var0.version(), var9, var8);
   }

   public static String a(String var0, String var1, String var2) {
      return var0 != null && !var0.isBlank() ? var0 : var1.toLowerCase() + "_" + var2.replace("/", "_").replace("{", "").replace("}", "");
   }

   private static Operation a(String var0, OpenApiOperation var1, OpenApiDocument var2) {
      String var3 = a(var1.g, var1.b, var0);
      Address var4 = new Address(Address.Kind.PATH, var0, var1.b);
      ArrayList<io.karatelabs.ext.contract.Message> var5 = new ArrayList();
      if (var1.c != null) {
         for (OpenApiParameter var7 : var1.c.b) {
            var5.add(a(var7, false));
         }
      }

      for (OpenApiParameter var17 : var1.a.d) {
         var5.add(a(var17, true));
      }

      if (var1.c != null) {
         for (OpenApiMediaType var18 : var1.c.d) {
            var5.add(new Message("body", TypeRef.nameOnly(var18.c), var18.c, Message.Role.BODY));
         }
      }

      ArrayList<io.karatelabs.ext.contract.Message> var16 = new ArrayList();
      ArrayList<io.karatelabs.ext.contract.Outcome> var19 = new ArrayList();
      boolean var8 = false;
      LinkedHashSet<java.lang.String> var9 = new LinkedHashSet();

      for (OpenApiResponse var11 : var1.d) {
         for (OpenApiResponseContent var13 : var11.e) {
            var16.add(new Message(var11.b, TypeRef.nameOnly(var13.c), var13.c, Message.Role.BODY));
            var9.add(var13.c);
            if ("text/event-stream".equals(var13.c)) {
               var8 = true;
            }
         }

         var19.add(new Outcome(var11.b, a(var11.b), null, var11.d));
      }

      ArrayList<java.lang.String> var20 = new ArrayList();
      if (var1.c != null) {
         for (OpenApiMediaType var23 : var1.c.d) {
            var20.add(var23.c);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
      var22.put("binding", new HttpBinding(var1.b, var0, var2.defaultServerUrl(), var20, new ArrayList<>(var9)));
      var22.put("operationId", var1.g);
      var22.put("description", var1.f);
      Interaction var24 = var8 ? Interaction.SERVER_STREAM : Interaction.REQUEST_RESPONSE;
      return new Operation(var3, var4, var24, var5, var16, var19, var1.e, new ArrayList<>(var1.h), var1.i, var22);
   }

   private static Message a(OpenApiParameter var0, boolean var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("in", var0.e);
      var2.put("pathLevel", var1);
      return new Message(var0.d, TypeRef.nameOnly(var0.d), null, Message.Role.PARAM, var2);
   }

   private static Outcome.Kind a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         return switch (var0.charAt(0)) {
            case '2' -> Outcome.Kind.SUCCESS;
            default -> Outcome.Kind.INFO;
            case '4' -> Outcome.Kind.CLIENT_ERROR;
            case '5' -> Outcome.Kind.SERVER_ERROR;
         };
      } else {
         return Outcome.Kind.INFO;
      }
   }
}
