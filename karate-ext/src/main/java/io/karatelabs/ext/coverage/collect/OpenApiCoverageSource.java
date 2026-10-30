package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.openapi.OpenApiDocument;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class OpenApiCoverageSource extends ContractCoverageSource {
   private final OpenApiDocument a;
   private final List<String> b;

   public OpenApiCoverageSource(Path var1) {
      this(new OpenApiDocument(var1.toFile()));
   }

   public OpenApiCoverageSource(Path var1, String var2, List<String> var3) {
      this(new OpenApiDocument(var1.toFile()), var2, var3);
   }

   public OpenApiCoverageSource(Resource var1, String var2, List<String> var3) {
      this(new OpenApiDocument(var1), var2, var3);
   }

   public OpenApiCoverageSource(Path var1, String var2) {
      this(new OpenApiDocument(var1.toFile()), var2);
   }

   public OpenApiCoverageSource(Resource var1, String var2) {
      this(new OpenApiDocument(var1), var2);
   }

   public OpenApiCoverageSource(File var1) {
      this(new OpenApiDocument(var1));
   }

   public OpenApiCoverageSource(OpenApiDocument var1) {
      this(var1, null);
   }

   public OpenApiCoverageSource(OpenApiDocument var1, String var2) {
      this(var1, var2, null);
   }

   public OpenApiCoverageSource(OpenApiDocument var1, String var2, List<String> var3) {
      super(io.karatelabs.ext.openapi.OpenApiContractLoader.a(var1, var3), var2);
      this.a = var1;
      this.b = var1.paths.keySet().stream().toList();
   }

   public OpenApiDocument i() {
      return this.a;
   }

   public List<String> j() {
      return this.b;
   }

   public CoverageItem a(String var1) {
      return this.a().stream().filter(var1x -> var1.equals(var1x.id())).findFirst().orElse(null);
   }

   public CoverageItem a(String var1, String var2) {
      String var3 = var1.toUpperCase() + " " + var2;
      return this.a().stream().filter(var1x -> var3.equals(var1x.name())).findFirst().orElse(null);
   }
}
