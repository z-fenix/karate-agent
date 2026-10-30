package io.karatelabs.ext.agent.llm;

import io.karatelabs.common.Json;
import java.util.Map;
import java.util.regex.Pattern;

public class LlmException extends RuntimeException {
   private static final Pattern a = Pattern.compile("virtual\\s*network|firewall", 2);
   private static final int b = 160;
   private final io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind c;
   private final int d;
   private final String e;
   private final LlmProvider f;

   public LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind var1, String var2) {
      this(var1, var2, 0, null);
   }

   public LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind var1, String var2, int var3) {
      this(var1, var2, var3, null);
   }

   public LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind var1, String var2, int var3, Throwable var4) {
      this(var1, var2, var3, var4, null, null);
   }

   public LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind var1, String var2, int var3, Throwable var4, String var5, LlmProvider var6) {
      super(var2, var4);
      this.c = var1;
      this.d = var3;
      this.e = var5;
      this.f = var6;
   }

   public io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind a() {
      return this.c;
   }

   public int b() {
      return this.d;
   }

   public String c() {
      return this.e;
   }

   public LlmProvider d() {
      return this.f;
   }

   public boolean e() {
      return this.c == io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.RATE_LIMIT || this.c == io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.SERVER_ERROR || this.c == io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.NETWORK;
   }

   public String f() {
      String var1 = this.h();
      if (this.d <= 0) {
         return var1;
      } else {
         return var1 != null && !var1.isEmpty() ? this.d + " · " + var1 : String.valueOf(this.d);
      }
   }

   public String g() {
      String var1 = this.f == null ? "the provider's API-key env var" : this.f.getEnvVar();

      return switch (this.d) {
         case 401 -> "The provider rejected the API key — check that " + var1 + " holds a current, valid key for this endpoint.";
         case 403 -> this.e != null && a.matcher(this.e).find()
            ? "The provider's network ACL blocked this machine — allowlist this machine's egress IP address in the resource firewall (for Azure OpenAI: the resource's Networking → firewall / selected-networks rules)."
            : "The key is not authorized for this resource — check that " + var1 + " holds a key with access to this deployment or model.";
         case 404 -> "No model or deployment answers at this endpoint — check the model name, and on Azure OpenAI the deployment name in the endpoint URL (that path segment, not the catalog model id, is what Azure resolves).";
         case 429 -> "The provider is rate-limiting or the account's quota is exhausted — retry later, raise the quota, or switch provider/model.";
         default -> null;
      };
   }

   private String h() {
      String var1 = a(this.e);
      String var2 = var1 != null ? var1 : this.e;
      if (var2 != null && !var2.isBlank()) {
         return b(var2.strip().replaceAll("\\s+", " "));
      } else {
         return this.getMessage() == null ? "" : b(this.getMessage().strip());
      }
   }

   private static String a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         Object var1;
         try {
            var1 = Json.parseStrict(var0);
         } catch (Exception var7) {
            return null;
         }

         if (!(var1 instanceof Map var2)) {
            return null;
         } else {
            Object var3 = var2.get("error");
            if (var3 instanceof Map var4 && var4.get("message") instanceof String var5 && !var5.isBlank()) {
               return var5;
            } else if (var3 instanceof String var8 && !var8.isBlank()) {
               return var8;
            } else {
               return var2.get("message") instanceof String var9 && !var9.isBlank() ? var9 : null;
            }
         }
      } else {
         return null;
      }
   }

   private static String b(String var0) {
      return var0.length() <= 160 ? var0 : var0.substring(0, 159) + "…";
   }

   public static LlmException a(int var0, String var1) {
      return a(var0, var1, null);
   }

   public static LlmException a(int var0, String var1, LlmProvider var2) {
      if (var0 == 402) {
         return new LlmException(
            io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.INVALID_REQUEST,
            "Insufficient credits / payment required (HTTP 402) — top up the API account or switch provider/model. Detail: " + var1,
            var0,
            null,
            var1,
            var2
         );
      } else {
         io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind var3;
         if (var0 == 401 || var0 == 403) {
            var3 = io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.AUTHENTICATION;
         } else if (var0 == 429) {
            var3 = io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.RATE_LIMIT;
         } else if (var0 >= 400 && var0 < 500) {
            var3 = io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.INVALID_REQUEST;
         } else if (var0 >= 500) {
            var3 = io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.SERVER_ERROR;
         } else {
            var3 = io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.UNKNOWN;
         }

         return new LlmException(var3, var1, var0, null, var1, var2);
      }
   }

   public static LlmException a(Throwable var0) {
      for (Throwable var1 = var0; var1 != null; var1 = var1.getCause() == var1 ? null : var1.getCause()) {
         if (var1 instanceof LlmException var2) {
            return var2;
         }
      }

      return null;
   }

   public static String a(LlmProvider var0) {
      return "No API key is set in this environment — export " + var0.getEnvVar() + " (or select a provider whose key is set) before running.";
   }

   public static enum LlmErrorKind {
      AUTHENTICATION,
      RATE_LIMIT,
      INVALID_REQUEST,
      SERVER_ERROR,
      NETWORK,
      TIMEOUT,
      CONTEXT_LENGTH,
      UNKNOWN;
   }
}
