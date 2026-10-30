package io.karatelabs.ext.coverage;

import java.time.Instant;
import java.util.Set;

public record GrpcAction(String id, Set<String> tags, Instant timestamp, String featurePath, String scenarioName, int scenarioLine, String slug, String service, String method, String status, long durationMs, Object request) implements Action {

   public GrpcAction(
      String var1, Set<String> var2, Instant var3, String var4, String var5, int var6, String var7, String var8, String var9, String var10, long var11
   ) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, null);
   }


   @Override
   public String type() {
      return "grpc";
   }

   @Override
   public Object dimensionInput() {
      return this.request;
   }

   public String itemId() {
      return this.service + "/" + this.method;
   }

   public boolean isSuccess() {
      return "OK".equals(this.status);
   }
}
