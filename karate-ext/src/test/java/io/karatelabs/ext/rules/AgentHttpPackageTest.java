package io.karatelabs.ext.rules;

import io.karatelabs.ext.agent.http.AgentHttpRequest;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentHttpPackageTest {
   @Test
   public void requestRetainsMethodUrlAndHeaders() {
      AgentHttpRequest request = new AgentHttpRequest();
      request.setUrl("/api");
      request.setMethod("POST");
      request.addHeader("Accept", "application/json");
      assertEquals("POST", request.method());
      assertEquals("/api", request.url());
      assertEquals("application/json", request.header("Accept"));
   }
}
