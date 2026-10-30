package io.karatelabs.ext.coverage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public record HttpAction(String id, String type, Set<String> tags, Instant timestamp, String featurePath, String scenarioName, int scenarioLine, String slug, String e, String f, String g, Map<String, String> h, int i, long j, Map<String, Object> k) implements Action {

   public HttpAction(String id, String type, Set<String> tags, Instant timestamp, String featurePath, String scenarioName, int scenarioLine, String slug, String e, String f, String g, Map<String, String> h, int i, long j, Map<String, Object> k) {
      if (type == null || type.isEmpty()) {
         type = "http";
      }

      this.id = id;
      this.type = type;
      this.tags = tags;
      this.timestamp = timestamp;
      this.featurePath = featurePath;
      this.scenarioName = scenarioName;
      this.scenarioLine = scenarioLine;
      this.slug = slug;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
      this.k = k;
   }

   public HttpAction(
      String var1,
      String var2,
      Set<String> var3,
      Instant var4,
      String var5,
      String var6,
      int var7,
      String var8,
      String var9,
      String var10,
      String var11,
      Map<String, String> var12,
      int var13,
      long var14
   ) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14, null);
   }

   public boolean wasServedByMock() {
      if (!(this.k != null && this.k.get("response") instanceof Map var1)) {
         return false;
      } else if (var1.get("headers") instanceof Map var8) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
            if (var4.getKey() != null && "Karate-Mock".equalsIgnoreCase(String.valueOf(var4.getKey()))) {
               Object var5 = var4.getValue();
               Object var6 = var5 instanceof List var7 ? (var7.isEmpty() ? null : var7.get(0)) : var5;
               return "true".equalsIgnoreCase(String.valueOf(var6));
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isSuccess() {
      return this.i >= 200 && this.i < 300;
   }

   public boolean isClientError() {
      return this.i >= 400 && this.i < 500;
   }

   public boolean isServerError() {
      return this.i >= 500;
   }

   @Override
   public String id() {
      return this.id;
   }

   @Override
   public String type() {
      return this.type;
   }

   @Override
   public Set<String> tags() {
      return this.tags;
   }

   @Override
   public Instant timestamp() {
      return this.timestamp;
   }

   @Override
   public String featurePath() {
      return this.featurePath;
   }

   @Override
   public String scenarioName() {
      return this.scenarioName;
   }

   @Override
   public int scenarioLine() {
      return this.scenarioLine;
   }

   @Override
   public String slug() {
      return this.slug;
   }

   public String e() {
      return this.e;
   }

   public String f() {
      return this.f;
   }

   public String g() {
      return this.g;
   }

   public Map<String, String> h() {
      return this.h;
   }

   public int i() {
      return this.i;
   }

   public long j() {
      return this.j;
   }

   public Map<String, Object> k() {
      return this.k;
   }
}
