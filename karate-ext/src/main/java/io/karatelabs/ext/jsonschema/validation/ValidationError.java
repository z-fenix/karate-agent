package io.karatelabs.ext.jsonschema.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValidationError {
   public final String keyword;
   public final String instancePath;
   public final String schemaPath;
   public final Object expected;
   public final Object actual;
   public final String message;
   public final Integer branch;
   public final List<ValidationError> causes;
   public Integer line;
   public Integer column;

   ValidationError(String var1, String var2, String var3, Object var4, Object var5, String var6, Integer var7, List<ValidationError> var8) {
      this.keyword = var1;
      this.instancePath = var2;
      this.schemaPath = var3;
      this.expected = var4;
      this.actual = var5;
      this.message = var6;
      this.branch = var7;
      this.causes = var8 == null ? List.of() : var8;
   }

   static ValidationError of(String var0, String var1, String var2, Object var3, Object var4, String var5) {
      return new ValidationError(var0, var1, var2, var3, var4, var5, null, null);
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("keyword", this.keyword);
      var1.put("instancePath", this.instancePath);
      var1.put("schemaPath", this.schemaPath);
      var1.put("message", this.message);
      var1.put("expected", this.expected);
      var1.put("actual", this.actual);
      if (this.branch != null) {
         var1.put("branch", this.branch);
      }

      if (!this.causes.isEmpty()) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList(this.causes.size());

         for (ValidationError var4 : this.causes) {
            var2.add(var4.toMap());
         }

         var1.put("causes", var2);
      }

      if (this.line != null) {
         var1.put("line", this.line);
         var1.put("col", this.column);
      }

      return var1;
   }

   @Override
   public String toString() {
      return (this.instancePath.isEmpty() ? "$" : this.instancePath) + ": " + this.message;
   }
}
