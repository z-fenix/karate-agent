package io.karatelabs.ext.max;

import io.karatelabs.http.*;
import io.karatelabs.js.Engine;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerRegressionTest {
   @Test
   public void requestSessionTracksInitializationAndClosure() {
      ServerConfig config = new ServerConfig().sessionStore(new InMemorySessionStore());
      ServerMarkupContext context = new ServerMarkupContext(new HttpRequest(), new HttpResponse(), config);
      Engine engine = ServerRequestCycle.init(config, context, null, null).getEngine();
      assertEquals(true, engine.eval("session == null"));
      engine.eval("context.init(); session.ui = { theme: 'dark' }");
      assertEquals("dark", engine.eval("session.ui.theme"));
      engine.eval("context.close()");
      assertEquals(true, engine.eval("session == null"));
   }

   @Test
   public void reportAssetsAreOnClasspath() {
      assertNotNull(getClass().getClassLoader().getResource("META-INF/karate-ext/static/coverage.css"));
      assertNotNull(getClass().getClassLoader().getResource("META-INF/karate-ext/pages/coverage.html"));
   }
}
