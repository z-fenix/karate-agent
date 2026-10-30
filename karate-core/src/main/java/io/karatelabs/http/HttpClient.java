package io.karatelabs.http;

import io.karatelabs.core.KarateConfig;
import java.io.Closeable;

public interface HttpClient extends Closeable {
   HttpResponse invoke(HttpRequest var1);

   void apply(KarateConfig var1);

   void abort();
}
