package io.karatelabs.ext.mock;

import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.Session;
import io.karatelabs.js.Engine;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class JsRequestHandler implements Function<HttpRequest, HttpResponse> {
   private static final Map<Session, ReentrantLock> LOCKS = Collections.synchronizedMap(new WeakHashMap<>());
   private final String source;
   private final Session session;

   public static ReentrantLock lockOf(Session var0) {
      return LOCKS.computeIfAbsent(var0, var0x -> new ReentrantLock());
   }

   public JsRequestHandler(String var1, Session var2) {
      this.source = var1;
      this.session = var2;
   }

   Session session() {
      return this.session;
   }

   public HttpResponse apply(HttpRequest var1) {
      ReentrantLock var2 = lockOf(this.session);
      var2.lock();

      HttpResponse var5;
      try {
         var1.processBody();
         HttpResponse var3 = new HttpResponse();
         Engine var4 = new Engine();
         var4.put("request", var1);
         var4.put("response", var3);
         var4.put("session", this.session);
         var4.eval(this.source);
         var5 = var3;
      } finally {
         var2.unlock();
      }

      return var5;
   }
}
