package io.karatelabs.ext.rules;

import java.util.Map;
import java.util.List;
import io.karatelabs.ext.openapi.SchemaMatchConverter;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchemaValidationPackageTest {
   @Test
   public void validatesValuesAgainstIntegerSchema() {
      assertEquals("request", io.karatelabs.ext.jsonschema.validation.ValidationDirection.REQUEST.wire());
      var schema = io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(Map.of("type", "integer"));
      assertTrue(schema.validate(3).valid);
      assertFalse(schema.validate("not-an-integer").valid);
   }

   @Test
   public void validatesNestedConstraintsAndFormats() {
      var schema = io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(Map.of(
         "type", "object", "required", List.of("email"),
         "properties", Map.of("email", Map.of("type", "string", "format", "email"))
      )).withFormatAssertion(true);
      assertTrue(schema.validate(Map.of("email", "a@example.com")).valid);
      assertFalse(schema.validate(Map.of()).valid);
      assertFalse(schema.validate(Map.of("email", "not-an-email")).valid);
   }

   @Test
   public void convertsRequiredAndOptionalSchemaPropertiesToMarkers() {
      var schema = Map.<String, Object>of("type", "object", "required", List.of("name"),
         "properties", Map.of("name", Map.of("type", "string"), "age", Map.of("type", "integer")));
      assertEquals(Map.of("name", "#string", "age", "##number"), SchemaMatchConverter.toMatchTemplateFromMap(schema));
   }
}
