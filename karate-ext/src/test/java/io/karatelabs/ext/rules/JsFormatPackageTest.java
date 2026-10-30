package io.karatelabs.ext.rules;

import io.karatelabs.ext.jsformat.JsFormatter;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsFormatPackageTest {
   @Test
   public void formattingPreservesSourceAndIsIdempotent() {
      String formatted = JsFormatter.format("var x=1;");
      assertTrue(formatted.contains("x"));
      assertEquals(formatted, JsFormatter.format(formatted));
   }
}
