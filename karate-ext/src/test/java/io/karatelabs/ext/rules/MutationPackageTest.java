package io.karatelabs.ext.rules;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MutationPackageTest {
   @Test
   public void parsesFaultManifestAndRejectsEmptyFaults() {
      String manifest = "{\"faults\":[{\"id\":\"reset\",\"operator\":\"TRANSPORT_RESET\",\"operation\":\"GET /orders\"}]}";
      assertEquals(List.of("reset"), io.karatelabs.ext.mutation.FaultManifest.parse(manifest).faultIds());
      assertThrows(IllegalArgumentException.class, () -> io.karatelabs.ext.mutation.FaultManifest.parse("{\"faults\":[]}"));
   }
}
