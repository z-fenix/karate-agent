package io.karatelabs.ext.rules;

import io.karatelabs.ext.sourcedoc.SourceDocument;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SourceDocPackageTest {
   @Test
   public void parsesJsonAndYamlDocuments() {
      assertEquals(Map.of("name", "value"), SourceDocument.parse("test.json", "{\"name\":\"value\"}").value());
      assertEquals(Map.of("name", "value"), SourceDocument.parse("test.yaml", "name: value\n").value());
   }

   @Test
   public void preservesSourceLocationsAndYamlComments() {
      SourceDocument json = SourceDocument.parse("test.json", "{\"name\":\"value\"}");
      SourceDocument.ResolvedLocation location = json.resolveLocation("/name", SourceDocument.Anchor.VALUE);
      assertNotNull(location);
      assertEquals(1, location.toMap().get("line"));
      assertEquals("value", location.toMap().get("anchor"));
      SourceDocument yaml = SourceDocument.parse("test.yaml", "# note\nname: value\n");
      assertEquals(1, yaml.comments().size());
      assertEquals(io.karatelabs.ext.sourcedoc.SourceComment.Kind.BLOCK, yaml.comments().get(0).kind());
   }
}
