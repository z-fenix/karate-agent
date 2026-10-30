package io.karatelabs.ext.jsonschema.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValidationResult {
   public static final ValidationResult VALID = new ValidationResult(Collections.emptyList());
   public final boolean valid;
   public final List<ValidationError> errors;

   ValidationResult(List<ValidationError> var1) {
      this.errors = var1 == null ? List.of() : List.copyOf(var1);
      this.valid = this.errors.isEmpty();
   }

   public ValidationError firstError() {
      return this.errors.isEmpty() ? null : this.errors.get(0);
   }

   public String message() {
      if (this.valid) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder();
         a(this.errors, var1, 0);
         return var1.toString();
      }
   }

   private static void a(List<ValidationError> var0, StringBuilder var1, int var2) {
      for (ValidationError var4 : var0) {
         if (var1.length() > 0) {
            var1.append('\n');
         }

         var1.append("  ".repeat(var2)).append(var4);
         if (!var4.causes.isEmpty()) {
            a(var4.causes, var1, var2 + 1);
         }
      }
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("valid", this.valid);
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList(this.errors.size());

      for (ValidationError var4 : this.errors) {
         var2.add(var4.toMap());
      }

      var1.put("errors", var2);
      return var1;
   }
}
