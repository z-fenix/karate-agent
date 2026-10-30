package io.karatelabs.ext.mutation;

import io.karatelabs.http.DefaultHttpClientFactory;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpClientFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ResponseMutatingHttpClientFactory implements HttpClientFactory {
   private final DefaultHttpClientFactory defaultClientFactory = new DefaultHttpClientFactory();
   private final ResponseMutationSpec mutationSpec;
   private final io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher routeMatcher;
   private final ConcurrentLinkedQueue<io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent> appliedEvents = new ConcurrentLinkedQueue<>();
   private final Set<ResponseMutatingHttpClient> clients = ConcurrentHashMap.newKeySet();

   public ResponseMutatingHttpClientFactory(ResponseMutationSpec var1, io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher var2) {
      this.mutationSpec = var1;
      this.routeMatcher = var2;
   }

   @Override
   public HttpClient create() {
      ResponseMutatingHttpClient var1 = new ResponseMutatingHttpClient(this.defaultClientFactory.create(), this.mutationSpec, this.routeMatcher, this.appliedEvents::add);
      this.clients.add(var1);
      return var1;
   }

   @Override
   public void release(HttpClient var1) {
      if (var1 instanceof ResponseMutatingHttpClient var2) {
         this.clients.remove(var2);
         this.defaultClientFactory.release(var2.delegate());
      } else {
         this.defaultClientFactory.release(var1);
      }
   }

   public void abortAll() {
      for (ResponseMutatingHttpClient var2 : this.clients) {
         try {
            var2.abort();
         } catch (RuntimeException var4) {
         }
      }
   }

   public List<io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent> appliedEvents() {
      return List.copyOf(this.appliedEvents);
   }
}
