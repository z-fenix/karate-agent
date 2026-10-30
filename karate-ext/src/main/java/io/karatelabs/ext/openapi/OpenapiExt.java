package io.karatelabs.ext.openapi;

import io.karatelabs.common.Json;
import io.karatelabs.core.Ext;
import io.karatelabs.core.HttpRunEvent;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class OpenapiExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(OpenapiExt.class);
   static final String EMBED_NAME = "openapi-match";
   static final String EMBED_MIME = "application/json";
   private String path;
   private List<String> excludes = new ArrayList<>();
   private Suite suite;
   private String specTitle;
   private String specVersion;
   private List<Map<String, Object>> operations = new ArrayList<>();
   private OpenApiOperationMatcher matcher;
   private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch", "head", "options", "trace");

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "path" -> this.path;
         case "excludes" -> this.excludes;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "path":
            this.setPath(var2 == null ? null : String.valueOf(var2));
            break;
         case "excludes":
            this.setExcludes(toStringList(var2));
            break;
         default:
            throw new IllegalArgumentException("unknown openapi config key: openapi." + var1 + " (known: path, excludes)");
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("path", "excludes");
   }

   public String getPath() {
      return this.path;
   }

   public void setPath(String var1) {
      this.path = var1;
      if (var1 != null && !var1.isBlank()) {
         this.parseSpec(var1);
      }
   }

   public List<String> getExcludes() {
      return this.excludes;
   }

   public void setExcludes(List<String> var1) {
      this.excludes = (List<String>)(var1 == null ? new ArrayList<>() : var1);
   }

   private static List<String> toStringList(Object var0) {
      if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return var2;
      } else {
         return new ArrayList<>();
      }
   }

   public List<Map<String, Object>> getOperations() {
      return Collections.unmodifiableList(this.operations);
   }

   @Override
   public void onBoot(Suite var1) {
      this.suite = var1;
      Supplier var2 = () -> this.path != null && !this.path.isBlank() ? List.of(this.resolvePath(this.path).toString()) : List.of();
      if (var1.getGlobal("spec") instanceof SpecBinding var3) {
         var3.addSource(var2);
      } else {
         var1.registerGlobal("spec", new SpecBinding(var2));
      }

      logger.info("openapi plugin booted (path={}, excludes={})", this.path, this.excludes);
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      if (var1.getType() == RunEventType.HTTP_EXIT && var1 instanceof HttpRunEvent var2) {
         this.recordMatch(var2);
      }

      return true;
   }

   private void recordMatch(HttpRunEvent var1) {
      if (this.matcher != null) {
         HttpRequest var2 = var1.request();
         HttpResponse var3 = var1.response();
         if (var2 != null) {
            try {
               var var4 = this.matcher.a(var2.getMethod(), var2.getUrlAndPath());
               if (var4 == null) {
                  return;
               }

               LinkedHashMap var5 = new LinkedHashMap();
               var5.put("opId", var4.get("opId"));
               var5.put("method", var4.get("method"));
               var5.put("path", var4.get("path"));
               var5.put("status", var3 != null ? var3.getStatus() : 0);
               var5.put("durationMs", var3 != null ? var3.getResponseTime() : 0L);
               String var6 = Json.stringifyStrict(var5);
               LogContext.get().embed(var6.getBytes(StandardCharsets.UTF_8), "application/json", "openapi-match");
            } catch (Exception var7) {
               logger.warn("openapi-match: failed to record match for {} {} — {}", new Object[]{var2.getMethod(), var2.getUrlAndPath(), var7.getMessage()});
            }
         }
      }
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("version", "1.0.0-SNAPSHOT");
      if (this.path != null) {
         var1.put("path", this.path);
      }

      if (this.specTitle != null) {
         var1.put("specTitle", this.specTitle);
      }

      if (this.specVersion != null) {
         var1.put("specVersion", this.specVersion);
      }

      var1.put("operationCount", this.operations.size());
      var1.put("operations", this.operations);
      if (!this.excludes.isEmpty()) {
         var1.put("excludes", this.excludes);
      }

      return var1;
   }

   private void parseSpec(String var1) {
      try {
         Path var2 = this.resolvePath(var1);
         String var3 = new String(Files.readAllBytes(var2), StandardCharsets.UTF_8);
         Map var4 = (Map)new Yaml().load(var3);
         if (var4 == null) {
            throw new RuntimeException("empty spec: " + var2);
         } else {
            Map var5 = (Map)var4.get("info");
            if (var5 != null) {
               this.specTitle = (String)var5.get("title");
               Object var6 = var5.get("version");
               this.specVersion = var6 == null ? null : var6.toString();
            }

            Map var8 = (Map)var4.get("paths");
            if (var8 == null) {
               logger.warn("openapi spec has no paths: {}", var2);
            } else {
               this.operations = this.enumerate(var8);
               this.matcher = new OpenApiOperationMatcher(this.operations, io.karatelabs.ext.openapi.ServerBasePathResolver.a(var4, null));
               logger.info("openapi spec parsed: {} ops from {}", this.operations.size(), var2);
            }
         }
      } catch (Exception var7) {
         throw new RuntimeException("openapi.path: failed to parse '" + var1 + "' — " + var7.getMessage(), var7);
      }
   }

   private Path resolvePath(String var1) {
      return MaxPaths.resolvePath(this.suite == null ? null : this.suite.getRoot(), var1);
   }

   private List<Map<String, Object>> enumerate(Map<String, Object> var1) {
      ArrayList var2 = new ArrayList();

      for (Entry var4 : var1.entrySet()) {
         String var5 = (String)var4.getKey();
         if (!this.isExcluded(var5)) {
            Map var6 = (Map)var4.getValue();
            if (var6 != null) {
               for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
                  String var9 = ((String)var8.getKey()).toLowerCase();
                  if (HTTP_METHODS.contains(var9)) {
                     Map var10 = (Map)var8.getValue();
                     if (var10 != null) {
                        LinkedHashMap var11 = new LinkedHashMap();
                        String var12 = (String)var10.get("operationId");
                        var11.put("opId", var12 != null ? var12 : var9.toUpperCase() + " " + var5);
                        var11.put("method", var9.toUpperCase());
                        var11.put("path", var5);
                        Object var13 = var10.get("tags");
                        if (var13 instanceof List) {
                           var11.put("tags", var13);
                        }

                        var2.add(var11);
                     }
                  }
               }
            }
         }
      }

      return var2;
   }

   private boolean isExcluded(String var1) {
      if (this.excludes.isEmpty()) {
         return false;
      } else {
         for (String var3 : this.excludes) {
            if (globMatch(var3, var1)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean globMatch(String var0, String var1) {
      if (!var0.endsWith("/**")) {
         if (var0.endsWith("/*")) {
            String var3 = var0.substring(0, var0.length() - 2);
            return !var1.startsWith(var3 + "/") ? false : !var1.substring(var3.length() + 1).contains("/");
         } else {
            return var0.equals(var1);
         }
      } else {
         String var2 = var0.substring(0, var0.length() - 3);
         return var1.equals(var2) || var1.startsWith(var2 + "/");
      }
   }
}
