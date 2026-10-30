package io.karatelabs.ext.scaffold;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.TypeModel;
import io.karatelabs.ext.contract.TypeRef;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScaffoldContext {
   private final String outDir;
   private final List<ScaffoldContext.Artifact> artifacts;
   private final String filesHint;
   private final String writtenHint;
   private final Map<String, Object> vars;
   private final boolean overwrite;

   private ScaffoldContext(String var1, List<ScaffoldContext.Artifact> var2, String var3, String var4, Map<String, Object> var5, boolean var6) {
      this.outDir = var1;
      this.overwrite = var6;
      this.artifacts = var2 == null ? List.of() : List.copyOf(var2);
      this.filesHint = var3;
      this.writtenHint = var4;
      this.vars = var5 == null ? Map.of() : Map.copyOf(var5);
   }

   public String outDir() {
      return this.outDir;
   }

   public boolean overwrite() {
      return this.overwrite;
   }

   public List<ScaffoldContext.Artifact> artifacts() {
      return this.artifacts;
   }

   public String filesHint() {
      return this.filesHint;
   }

   public String writtenHint() {
      return this.writtenHint;
   }

   public Object var(String var1) {
      return this.vars.get(var1);
   }

   public String var(String var1, String var2) {
      Object var3 = this.vars.get(var1);
      return var3 == null ? var2 : String.valueOf(var3);
   }

   public String sample(Message var1) {
      return var1 == null ? null : this.sample(var1.type());
   }

   public String sample(TypeRef var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            TypeModel var2 = var1.model();
            Object var3 = var2 == null ? null : var2.sample();
            return var3 == null ? null : Json.of(var3).toString();
         } catch (RuntimeException var4) {
            return null;
         }
      }
   }

   public static String slug(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
         return var1.isEmpty() ? "api" : var1;
      } else {
         return "api";
      }
   }

   public static ScaffoldContext.Builder builder(Path var0) {
      return new ScaffoldContext.Builder(var0);
   }

   public record Artifact(String sourcePath, String targetName) {

   }

   public static final class Builder {
      private final Path root;
      private String outDir;
      private final List<ScaffoldContext.Artifact> artifacts = new ArrayList<>();
      private String filesHint;
      private String writtenHint;
      private final Map<String, Object> vars = new LinkedHashMap<>();
      private boolean overwrite;

      private Builder(Path var1) {
         this.root = var1;
      }

      public ScaffoldContext.Builder outDir(String var1) {
         this.outDir = var1 != null && !var1.isBlank() ? var1 : null;
         return this;
      }

      public ScaffoldContext.Builder copyArtifact(String var1, String var2) {
         this.artifacts.add(new ScaffoldContext.Artifact(var1, var2));
         return this;
      }

      public ScaffoldContext.Builder filesHint(String var1) {
         this.filesHint = var1;
         return this;
      }

      public ScaffoldContext.Builder writtenHint(String var1) {
         this.writtenHint = var1;
         return this;
      }

      public ScaffoldContext.Builder overwrite(Object var1) {
         this.overwrite = Boolean.TRUE.equals(var1);
         return this;
      }

      public ScaffoldContext.Builder var(String var1, Object var2) {
         this.vars.put(var1, var2);
         return this;
      }

      public ScaffoldContext build() {
         String var1 = this.outDir == null ? null : MaxPaths.resolveRef(this.root, this.outDir);
         return new ScaffoldContext(var1, this.artifacts, this.filesHint, this.writtenHint, this.vars, this.overwrite);
      }
   }
}
