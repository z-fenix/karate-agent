package io.karatelabs.ext.agent.llm;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LlmRegistry {
   private static final Logger g = LoggerFactory.getLogger(io.karatelabs.ext.agent.llm.LlmRegistry.class);
   public static final String a = "karate-agent.json";
   public static final String b = "google/gemma-4-26b-a4b-it";
   public static final String c = "qwen/qwen3.6-35b-a3b";
   public static final List<String> d = List.of("google/gemma-4-26b-a4b-it", "qwen/qwen3.6-35b-a3b");
   public static final String e = "KARATE_LLM_MODEL";
   public static final List<String> f = List.of("gpt-4o", "gpt-4o-mini");
   private final List<io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration> h;

   private LlmRegistry(List<io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration> var1) {
      this.h = var1;
   }

   public List<io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration> a() {
      return this.h;
   }

   public static io.karatelabs.ext.agent.llm.LlmRegistry a(Path var0, Function<String, String> var1) {
      Path var2 = a(var0);
      if (var2 != null) {
         try {
            Object var3 = Json.parseStrict(Files.readString(var2));
            if (var3 instanceof Map) {
               g.info("BYO-LLM registry: loaded {}", var2);
               return a((Map<?, ?>)var3, var1);
            }

            g.warn("BYO-LLM registry: {} is not a JSON object — falling back to env auto-detect", var2);
         } catch (Exception var4) {
            g.warn("BYO-LLM registry: failed to read {} ({}) — falling back to env auto-detect", var2, var4.toString());
         }
      }

      return a(var1);
   }

   private static Path a(Path var0) {
      if (var0 == null) {
         return null;
      } else {
         Path var1 = var0.toAbsolutePath();

         for (int var2 = 0; var2 < 3 && var1 != null; var1 = var1.getParent()) {
            Path var3 = var1.resolve("karate-agent.json");
            if (Files.isRegularFile(var3)) {
               return var3;
            }

            var2++;
         }

         return null;
      }
   }

   private static io.karatelabs.ext.agent.llm.LlmRegistry a(Map<?, ?> var0, Function<String, String> var1) {
      ArrayList<io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration> var2 = new ArrayList();
      Object var3 = var0.get("providers");
      if (var3 instanceof List && !((List)var3).isEmpty()) {
         boolean var4 = ((List)var3).stream().anyMatch(var0x -> var0x instanceof Map && Boolean.TRUE.equals(((Map)var0x).get("default")));
         boolean var5 = true;

         for (Object var7 : (List)var3) {
            if (var7 instanceof Map var8) {
               String var9 = a(var8.get("name"));
               if (var9 != null) {
                  var9 = var9.toLowerCase(Locale.ROOT);
                  LlmProvider var10 = b(var9);
                  if (var10 == null) {
                     g.warn(
                        "BYO-LLM registry: provider '{}' is not a built-in (anthropic/openai/openrouter/ollama) — skipped (custom url/protocol is not yet supported)",
                        var9
                     );
                  } else {
                     Object var11 = var8.get("models");
                     ArrayList<java.lang.String> var12 = null;
                     boolean var13 = false;
                     if (var11 instanceof List) {
                        var12 = new ArrayList();

                        for (Object var15 : (List)var11) {
                           String var16 = a(var15);
                           if (var16 != null) {
                              var12.add(var16);
                           }
                        }
                     } else if (var11 == null || "auto".equalsIgnoreCase(a(var11))) {
                        var13 = true;
                     }

                     boolean var18 = Boolean.TRUE.equals(var8.get("default")) || !var4 && var5;
                     var2.add(new io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration(var9, var10, var12, var13, var18));
                     var5 = false;
                  }
               }
            }
         }

         return var2.isEmpty() ? a(var1) : new io.karatelabs.ext.agent.llm.LlmRegistry(var2);
      } else {
         return a(var1);
      }
   }

   static io.karatelabs.ext.agent.llm.LlmRegistry a(Function<String, String> var0) {
      ArrayList<io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration> var1 = new ArrayList();
      boolean var2 = b(var0, io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC);
      boolean var3 = b(var0, io.karatelabs.ext.agent.llm.LlmProvider.OPENAI);
      boolean var4 = b(var0, io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER);
      if (var2) {
         var1.add(new io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration("anthropic", io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC, List.of("sonnet", "haiku", "opus"), false, true));
      }

      if (var3) {
         var1.add(new io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration("openai", io.karatelabs.ext.agent.llm.LlmProvider.OPENAI, b(var0), false, !var2));
      }

      if (var4) {
         var1.add(new io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration("openrouter", io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER, d, false, !var2 && !var3));
      }

      if (a(var0, io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA)) {
         var1.add(new io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration("ollama", io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA, null, true, false));
      }

      return new io.karatelabs.ext.agent.llm.LlmRegistry(var1);
   }

   public static List<String> b(Function<String, String> var0) {
      String var1 = (String)var0.apply("KARATE_LLM_MODEL");
      if (var1 != null && !var1.isBlank()) {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (String var6 : var1.split(",")) {
            String var7 = var6.trim();
            if (!var7.isEmpty()) {
               var2.add(var7);
            }
         }

         if (!var2.isEmpty()) {
            return var2;
         }
      }

      String var8 = io.karatelabs.ext.agent.llm.LlmProvider.azureDeployment(io.karatelabs.ext.agent.llm.LlmProvider.OPENAI.configuredEndpoint(var0));
      return var8 != null ? List.of(var8) : f;
   }

   public static String a(LlmProvider var0, Function<String, String> var1) {
      return switch (var0) {
         case ANTHROPIC -> "sonnet";
         case OPENAI -> (String)b(var1).get(0);
         case OPENROUTER -> "google/gemma-4-26b-a4b-it";
         case OLLAMA -> null;
      };
   }

   private static boolean b(Function<String, String> var0, LlmProvider var1) {
      String var2 = (String)var0.apply(var1.getEnvVar());
      return var2 != null && !var2.isBlank();
   }

   static boolean a(Function<String, String> var0, LlmProvider var1) {
      String var2 = (String)var0.apply(var1.getBaseUrlEnvVar());
      return var2 != null && !var2.isBlank();
   }

   private static LlmProvider b(String var0) {
      return switch (var0) {
         case "anthropic" -> io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC;
         case "openai" -> io.karatelabs.ext.agent.llm.LlmProvider.OPENAI;
         case "openrouter" -> io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER;
         case "ollama" -> io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA;
         default -> null;
      };
   }

   private static String a(Object var0) {
      return var0 == null ? null : var0.toString();
   }

   public static List<String> a(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 == null || var0.isBlank()) {
         var0 = "http://localhost:11434";
      }

      String var2 = var0.trim();
      if (!var2.startsWith("http://") && !var2.startsWith("https://")) {
         var2 = "http://" + var2;
      }

      while (var2.endsWith("/")) {
         var2 = var2.substring(0, var2.length() - 1);
      }

      if (var2.endsWith("/v1")) {
         var2 = var2.substring(0, var2.length() - "/v1".length());
      }

      try {
         HttpClient var3 = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1000L)).build();
         HttpRequest var4 = HttpRequest.newBuilder(URI.create(var2 + "/api/tags")).timeout(Duration.ofMillis(1500L)).GET().build();
         HttpResponse var5 = var3.send(var4, BodyHandlers.ofString());
         if (var5.statusCode() / 100 != 2) {
            return var1;
         }

         Object var6 = Json.parseStrict((String)var5.body());
         if (var6 instanceof Map) {
            Object var7 = ((Map)var6).get("models");
            if (var7 instanceof List) {
               for (Object var9 : (List)var7) {
                  if (var9 instanceof Map) {
                     String var10 = a(((Map)var9).get("name"));
                     if (var10 != null && !var10.isBlank()) {
                        var1.add(var10);
                     }
                  }
               }
            }
         }
      } catch (InterruptedException | IOException var11) {
         g.debug("Ollama /api/tags not reachable at {} ({})", var2, var11.toString());
      }

      return var1;
   }

   public static final class LlmProviderRegistration {
      public final String a;
      public final LlmProvider b;
      public final List<String> c;
      public final boolean d;
      public final boolean e;

      LlmProviderRegistration(String var1, LlmProvider var2, List<String> var3, boolean var4, boolean var5) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
      }
   }
}
