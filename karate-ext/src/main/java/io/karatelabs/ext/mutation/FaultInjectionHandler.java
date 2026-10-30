package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class FaultInjectionHandler implements Function<HttpRequest, HttpResponse> {
   public static final String CONTROL_PATH = "/__karate/fault";
   public static final String FAULT_HEADER = "Karate-Mutant";
   private final FaultManifest manifest;
   private final AtomicReference<Function<HttpRequest, HttpResponse>> delegate = new AtomicReference<>();
   private final AtomicReference<io.karatelabs.ext.mutation.FaultManifest.FaultSpec> armedFault = new AtomicReference<>();
   private final List<Map<String, Object>> servedFaults = Collections.synchronizedList(new ArrayList<>());

   public FaultInjectionHandler(FaultManifest var1) {
      this.manifest = var1;
   }

   public static List<String> filterControlPaths(Iterable<String> var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0) {
         if ("/__karate/fault".equals(io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(var3))) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public Function<HttpRequest, HttpResponse> withDelegate(Function<HttpRequest, HttpResponse> var1) {
      this.delegate.set(var1);
      return this;
   }

   @Override
   public HttpResponse apply(HttpRequest request) {
      return this.handle(request);
   }

   public HttpResponse handle(HttpRequest var1) {
      if ("/__karate/fault".equals(io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(var1.getPath()))) {
         return this.handleControl(var1);
      } else {
         io.karatelabs.ext.mutation.FaultManifest.FaultSpec var2 = this.armedFault.get();
         HttpResponse var3 = this.delegate.get().apply(var1);
         if (var2 != null && var3 != null && var2.matchesOperation(var1.getMethod(), var1.getPath())) {
            if (applyFault(var2, var3)) {
               if (!"TRANSPORT_RESET".equals(var2.d())) {
                  var3.setHeader("Karate-Mutant", var2.c());
               }

               LinkedHashMap var4 = new LinkedHashMap();
               var4.put("fault", var2.c());
               var4.put("operator", var2.d());
               var4.put("method", var1.getMethod());
               var4.put("path", io.karatelabs.ext.mutation.ResponseMutationFactory.normalizePath(var1.getPath()));
               this.servedFaults.add(var4);
            }

            return var3;
         } else {
            return var3;
         }
      }
   }

   private static boolean applyFault(io.karatelabs.ext.mutation.FaultManifest.FaultSpec var0, HttpResponse var1) {
      String var2 = var0.d();
      switch (var2) {
         case "TRANSPORT_DELAY":
            var1.setDelay(var0.k());
            return true;
         case "TRANSPORT_RESET":
            if (var0.k() > 0) {
               var1.setDelay(var0.k());
            }

            var1.setResetConnection(true);
            return true;
         default:
            int var4 = var0.h() >= 0 ? var0.h() : var1.getStatus();
            ResponseMutationSpec var5 = new ResponseMutationSpec(var0.c(), var0.d(), var0.operationText(), var0.g() == null ? "" : "field:" + var0.g(), var4, var0.i(), var0.g(), "", "", var0.j(), -1);
            return io.karatelabs.ext.mutation.ResponseMutatingHttpClient.applyMutation(var1, var5);
      }
   }

   public Map<String, Object> status() {
      LinkedHashMap var1 = new LinkedHashMap();
      io.karatelabs.ext.mutation.FaultManifest.FaultSpec var2 = this.armedFault.get();
      var1.put("armed", var2 == null ? null : var2.c());
      var1.put("faults", this.manifest.faults().stream().map(io.karatelabs.ext.mutation.FaultManifest.FaultSpec::toMap).toList());
      synchronized (this.servedFaults) {
         var1.put("served", List.copyOf(this.servedFaults));
         return var1;
      }
   }

   public Map<String, Object> armFault(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (var1 != null && !var1.isBlank() && !"none".equals(var1)) {
         io.karatelabs.ext.mutation.FaultManifest.FaultSpec var3 = this.manifest.faultById(var1);
         if (var3 == null) {
            var2.put("error", "no such fault '" + var1 + "' in the frozen manifest");
            var2.put("known", this.manifest.faultIds());
            return var2;
         } else {
            this.armedFault.set(var3);
            var2.put("armed", var3.c());
            var2.put("fault", var3.toMap());
            return var2;
         }
      } else {
         this.armedFault.set(null);
         var2.put("armed", null);
         return var2;
      }
   }

   private HttpResponse handleControl(HttpRequest var1) {
      if ("GET".equalsIgnoreCase(var1.getMethod())) {
         return jsonResponse(200, this.status());
      } else if (!"POST".equalsIgnoreCase(var1.getMethod())) {
         return jsonResponse(405, Map.of("error", "GET reads {armed, faults, served}; POST {id} arms, POST {id: null} disarms"));
      } else {
         Object var2;
         try {
            var2 = var1.getBodyConverted();
         } catch (RuntimeException var6) {
            return jsonResponse(400, Map.of("error", "the control body is not JSON: " + var6.getMessage()));
         }

         if (var2 instanceof Map var3 && var3.containsKey("id")) {
            Object var4 = var3.get("id");
            if (var4 != null && !(var4 instanceof String)) {
               return jsonResponse(400, Map.of("error", "`id` must be a string fault id, or null to disarm"));
            } else {
               var var5 = this.armFault((String)var4);
               return jsonResponse(var5.containsKey("error") ? 400 : 200, var5);
            }
         } else {
            return jsonResponse(400, Map.of("error", "the control body must be {id: '<fault id>'} to arm or {id: null} to disarm — nothing else changes state"));
         }
      }
   }

   private static HttpResponse jsonResponse(int var0, Map<String, Object> var1) {
      HttpResponse var2 = new HttpResponse();
      var2.setStatus(var0);
      var2.setBody(Json.stringifyStrict(var1).getBytes(StandardCharsets.UTF_8), null);
      var2.setContentType("application/json");
      return var2;
   }
}
