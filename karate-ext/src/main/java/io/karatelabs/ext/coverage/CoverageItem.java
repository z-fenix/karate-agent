package io.karatelabs.ext.coverage;

import java.util.Map;
import java.util.Set;

public record CoverageItem(String id, String name, Set<String> tags, String parentId, Map<String, Object> metadata) {


   public <T> T getMetadata(String var1) {
      return (T)(this.metadata != null ? this.metadata.get(var1) : null);
   }

   public String getMethod() {
      return this.getMetadata("method");
   }

   public String getPath() {
      return this.getMetadata("path");
   }

   public boolean isDeprecated() {
      Boolean var1 = this.getMetadata("deprecated");
      return Boolean.TRUE.equals(var1);
   }
}
