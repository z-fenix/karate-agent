package io.karatelabs.ext.jsonschema.validation;

public class CompiledSchema {
   private final SchemaRegistry registry;
   private final Object rootSchema;
   private final boolean assertFormats;
   private final ValidationDirection direction;

   CompiledSchema(SchemaRegistry var1, Object var2) {
      this(var1, var2, false, io.karatelabs.ext.jsonschema.validation.ValidationDirection.NONE);
   }

   private CompiledSchema(SchemaRegistry var1, Object var2, boolean var3, ValidationDirection var4) {
      this.registry = var1;
      this.rootSchema = var2;
      this.assertFormats = var3;
      this.direction = var4;
   }

   public static CompiledSchema compile(Object var0) {
      return compile(var0, null);
   }

   public static CompiledSchema compile(Object var0, SchemaDialect var1) {
      return new SchemaRegistry(io.karatelabs.ext.jsonschema.validation.SchemaDialectResolver.resolve(var0, null, var1)).compile(var0);
   }

   public CompiledSchema withFormatAssertion(boolean var1) {
      return var1 == this.assertFormats ? this : new CompiledSchema(this.registry, this.rootSchema, var1, this.direction);
   }

   public CompiledSchema withDirection(ValidationDirection var1) {
      ValidationDirection var2 = var1 == null ? io.karatelabs.ext.jsonschema.validation.ValidationDirection.NONE : var1;
      return var2 == this.direction ? this : new CompiledSchema(this.registry, this.rootSchema, this.assertFormats, var2);
   }

   public SchemaDialect dialect() {
      return this.registry.dialect;
   }

   public ValidationResult validate(Object var1) {
      return new SchemaValidator(this.registry, this.assertFormats, this.direction).validate(this.rootSchema, var1);
   }
}
