package io.karatelabs.http;

import org.slf4j.LoggerFactory;

public class DefaultHttpClientFactory implements HttpClientFactory {
   @Override
   public HttpClient create() {
      return new ApacheHttpClient();
   }

   @Override
   public void release(HttpClient client) {
      if (client != null) {
         try {
            client.close();
         } catch (Exception var3) {
            LoggerFactory.getLogger(DefaultHttpClientFactory.class).warn("error closing http client: {}", var3.getMessage());
         }
      }
   }
}
