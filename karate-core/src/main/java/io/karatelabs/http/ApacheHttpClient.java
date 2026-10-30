package io.karatelabs.http;

import io.karatelabs.common.Resource;
import io.karatelabs.core.KarateConfig;
import io.karatelabs.output.HttpLogger;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHttpClient implements HttpClient, HttpRequestInterceptor {
   private static final Logger LOGGER = LoggerFactory.getLogger(ApacheHttpClient.class);
   private HttpRequest request;
   private volatile CloseableHttpClient httpClient;
   private BasicCookieStore cookieStore;
   private volatile ClassicHttpRequest currentRequest;
   private int readTimeout = 30000;
   private int connectTimeout = 30000;
   private boolean followRedirects = true;
   private boolean strictResponseParsing;
   private String proxyUri;
   private String proxyUsername;
   private String proxyPassword;
   private List<String> nonProxyHosts;
   private boolean ssl = false;
   private String sslAlgorithm = "TLS";
   private String sslKeyStore;
   private String sslKeyStorePassword;
   private String sslKeyStoreType;
   private String sslTrustStore;
   private String sslTrustStorePassword;
   private String sslTrustStoreType;
   private boolean sslTrustAll = true;
   private boolean httpRetryEnabled = false;
   private int retryCount = 3;
   private int retryInterval = 1000;
   private Charset charset = StandardCharsets.UTF_8;
   private InetAddress localAddress;
   private String ntlmUsername;
   private String ntlmPassword;
   private String ntlmDomain;
   private String ntlmWorkstation;
   private final HttpLogger logger = new HttpLogger();
   private volatile ApacheHttpClient.ClientKey currentKey;
   private long startTime;
   private volatile boolean released;

   @Override
   public void apply(KarateConfig config) {
      if (config != null) {
         this.strictResponseParsing = config.isStrictResponseParsing();
         ApacheHttpClient.ClientKey key = ApacheHttpClient.ClientKey.of(config);
         if (key.equals(this.currentKey)) {
            LOGGER.trace("http client config unchanged, keeping the built client");
         } else {
            this.ssl = key.ssl();
            this.sslAlgorithm = key.sslAlgorithm();
            this.sslKeyStore = key.sslKeyStore();
            this.sslKeyStorePassword = key.sslKeyStorePassword();
            this.sslKeyStoreType = key.sslKeyStoreType();
            this.sslTrustStore = key.sslTrustStore();
            this.sslTrustStorePassword = key.sslTrustStorePassword();
            this.sslTrustStoreType = key.sslTrustStoreType();
            this.sslTrustAll = key.sslTrustAll();
            this.proxyUri = key.proxyUri();
            this.proxyUsername = key.proxyUsername();
            this.proxyPassword = key.proxyPassword();
            this.nonProxyHosts = key.nonProxyHosts();
            this.readTimeout = key.readTimeout();
            this.connectTimeout = key.connectTimeout();
            this.followRedirects = key.followRedirects();
            this.httpRetryEnabled = key.httpRetryEnabled();
            this.retryCount = key.retryCount();
            this.retryInterval = key.retryInterval();
            if (key.charset() != null) {
               this.charset = key.charset();
            }

            boolean unresolved = false;
            if (key.localAddress() != null) {
               try {
                  this.localAddress = InetAddress.getByName(key.localAddress());
               } catch (Exception var5) {
                  unresolved = true;
                  LOGGER.warn("invalid local address: {}", key.localAddress());
               }
            } else {
               this.localAddress = null;
            }

            this.ntlmUsername = key.ntlmUsername();
            this.ntlmPassword = key.ntlmPassword();
            this.ntlmDomain = key.ntlmDomain();
            this.ntlmWorkstation = key.ntlmWorkstation();
            this.currentKey = unresolved ? null : key;
            this.closeQuietly();
            LOGGER.debug("http client config applied");
         }
      }
   }

   private void initHttpClient() {
      PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();
      HttpClientBuilder clientBuilder = HttpClientBuilder.create();
      clientBuilder.useSystemProperties();
      clientBuilder.disableContentCompression();
      if (this.httpRetryEnabled) {
         clientBuilder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(this.retryCount, TimeValue.ofMilliseconds(this.retryInterval)));
      } else {
         clientBuilder.disableAutomaticRetries();
      }

      if (this.followRedirects) {
         clientBuilder.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE);
      } else {
         clientBuilder.disableRedirectHandling();
      }

      this.cookieStore = new BasicCookieStore();
      clientBuilder.setDefaultCookieStore(this.cookieStore);
      if (this.ssl) {
         KeyStore trustStore = getKeyStore(this.sslTrustStore, this.sslTrustStorePassword, this.sslTrustStoreType);
         KeyStore keyStore = getKeyStore(this.sslKeyStore, this.sslKeyStorePassword, this.sslKeyStoreType);

         try {
            SSLContextBuilder builder = SSLContexts.custom().setProtocol(this.sslAlgorithm);
            if (trustStore == null && this.sslTrustAll) {
               builder = builder.loadTrustMaterial(new TrustAllStrategy());
            } else if (this.sslTrustAll) {
               builder = builder.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy());
            } else {
               builder = builder.loadTrustMaterial(trustStore, null);
            }

            if (keyStore != null) {
               char[] keyPassword = this.sslKeyStorePassword == null ? null : this.sslKeyStorePassword.toCharArray();
               builder = builder.loadKeyMaterial(keyStore, keyPassword);
            }

            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory socketFactory;
            if (keyStore != null) {
               socketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            } else {
               socketFactory = new ApacheHttpClient.LenientSslConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            }

            connectionManagerBuilder.setSSLSocketFactory(socketFactory);
         } catch (Exception var10) {
            LOGGER.error("ssl context init failed: {}", var10.getMessage());
            throw new RuntimeException(var10);
         }
      } else {
         try {
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory socketFactory = new ApacheHttpClient.LenientSslConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            connectionManagerBuilder.setSSLSocketFactory(socketFactory);
         } catch (Exception var8) {
            LOGGER.error("ssl context init failed: {}", var8.getMessage());
            throw new RuntimeException(var8);
         }
      }

      connectionManagerBuilder.setDefaultConnectionConfig(
            ConnectionConfig.custom()
               .setSocketTimeout(this.readTimeout, TimeUnit.MILLISECONDS)
               .setConnectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS)
               .build()
         )
         .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(this.readTimeout, TimeUnit.MILLISECONDS).build());
      HttpClientConnectionManager shared = this.sharedConnectionManager();
      if (shared != null) {
         clientBuilder.setConnectionManager(shared).setConnectionManagerShared(true);
      } else {
         clientBuilder.setConnectionManager(connectionManagerBuilder.build());
      }

      Builder configBuilder = RequestConfig.custom()
         .setCookieSpec("strict")
         .setResponseTimeout(Timeout.ofMilliseconds(this.readTimeout))
         .setConnectTimeout(Timeout.ofMilliseconds(this.connectTimeout));
      if (this.ntlmUsername != null) {
         BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
         NTCredentials ntCredentials = new NTCredentials(
            this.ntlmUsername, this.ntlmPassword != null ? this.ntlmPassword.toCharArray() : null, this.ntlmWorkstation, this.ntlmDomain
         );
         credsProvider.setCredentials(new AuthScope(null, -1), ntCredentials);
         clientBuilder.setDefaultCredentialsProvider(credsProvider);
      }

      if (this.proxyUri != null) {
         try {
            final URI proxy = new URI(this.proxyUri);
            HttpHost proxyHost = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
            if (this.proxyUsername != null && this.proxyPassword != null) {
               BasicCredentialsProvider proxyCredsProvider = new BasicCredentialsProvider();
               proxyCredsProvider.setCredentials(
                  new AuthScope(proxy.getHost(), proxy.getPort()), new UsernamePasswordCredentials(this.proxyUsername, this.proxyPassword.toCharArray())
               );
               clientBuilder.setDefaultCredentialsProvider(proxyCredsProvider);
            }

            if (this.nonProxyHosts != null && !this.nonProxyHosts.isEmpty()) {
               ProxySelector proxySelector = new ProxySelector() {
                  @Override
                  public List<Proxy> select(URI uri) {
                     String host = uri.getHost();

                     for (String pattern : ApacheHttpClient.this.nonProxyHosts) {
                        if (ApacheHttpClient.matchNonProxyHost(pattern, host)) {
                           return Collections.singletonList(Proxy.NO_PROXY);
                        }
                     }

                     return Collections.singletonList(new Proxy(Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                  }

                  @Override
                  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                     ApacheHttpClient.LOGGER.info("connect failed to uri: {}", uri, ioe);
                  }
               };
               clientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE, proxySelector));
            } else {
               clientBuilder.setProxy(proxyHost);
            }
         } catch (Exception var9) {
            LOGGER.error("proxy config failed: {}", var9.getMessage());
            throw new RuntimeException(var9);
         }
      }

      if (this.localAddress != null) {
         final InetAddress addr = this.localAddress;
         clientBuilder.setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
            protected InetAddress determineLocalAddress(HttpHost firstHop, HttpContext context) {
               return addr;
            }
         });
      }

      clientBuilder.setDefaultRequestConfig(configBuilder.build()).addRequestInterceptorLast(this);
      this.httpClient = clientBuilder.build();
      if (LOGGER.isTraceEnabled()) {
         LOGGER.trace("http client created");
      }
   }

   static boolean matchNonProxyHost(String pattern, String host) {
      if (pattern == null || host == null) {
         return false;
      } else if (pattern.startsWith("*")) {
         return host.endsWith(pattern.substring(1));
      } else {
         return pattern.endsWith("*") ? host.startsWith(pattern.substring(0, pattern.length() - 1)) : host.equalsIgnoreCase(pattern);
      }
   }

   @Override
   public HttpResponse invoke(HttpRequest request) {
      this.request = request;

      HttpResponse var25;
      try {
         ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(request.getMethod()).setUri(request.getUrlAndPath());
         if (request.getBody() != null) {
            EntityBuilder entityBuilder = EntityBuilder.create().setBinary(request.getBody());
            String contentTypeHeader = request.getHeader(HttpUtils.Header.CONTENT_TYPE.key);
            if (contentTypeHeader != null) {
               try {
                  ContentType parsed = ContentType.parse(contentTypeHeader);
                  if (parsed.getCharset() == null) {
                     entityBuilder.setContentType(ContentType.create(parsed.getMimeType(), this.charset));
                  } else {
                     entityBuilder.setContentType(parsed);
                  }
               } catch (Exception var18) {
                  LOGGER.debug("could not parse content-type: {}", contentTypeHeader);
               }
            }

            List<String> transferEncoding = request.getHeaderValues(HttpUtils.Header.TRANSFER_ENCODING.key);
            if (transferEncoding != null) {
               for (String te : transferEncoding) {
                  if (te != null) {
                     if (te.contains("chunked")) {
                        entityBuilder.chunked();
                     }

                     if (te.contains("gzip")) {
                        entityBuilder.setContentEncoding("gzip");
                     }
                  }
               }

               request.removeHeader(HttpUtils.Header.TRANSFER_ENCODING.key);
            }

            requestBuilder.setEntity(entityBuilder.build());
         }

         if (request.getHeaders() != null) {
            request.getHeaders().forEach((k, vals) -> vals.forEach(v -> requestBuilder.addHeader(k, v)));
         }

         boolean orphanRebuild = this.httpClient == null && this.released;
         if (this.httpClient == null) {
            if (this.released) {
               LOGGER.debug("http request after the client was released — this request owns its own client and will close it");
            }

            this.initHttpClient();
         }

         this.currentRequest = requestBuilder.build();

         HttpResponse finalResponse;
         try {
            finalResponse = (HttpResponse)this.httpClient.execute(this.currentRequest, response -> buildResponse(response, this.startTime));
         } finally {
            if (orphanRebuild) {
               this.closeQuietly();
            }
         }

         this.currentRequest = null;
         this.mergeCookiesFromStore(finalResponse);
         finalResponse.setRequest(request);
         finalResponse.setStrictParsing(this.strictResponseParsing);
         this.logger.logResponse(finalResponse);
         var25 = finalResponse;
      } catch (Exception var20) {
         throw new RuntimeException(var20);
      } finally {
         this.currentRequest = null;
      }

      return var25;
   }

   @Override
   public void abort() {
      ClassicHttpRequest req = this.currentRequest;
      if (req instanceof HttpUriRequestBase) {
         ((HttpUriRequestBase)req).abort();
         LOGGER.warn("http request aborted");
      }
   }

   public void process(org.apache.hc.core5.http.HttpRequest hr, EntityDetails entity, HttpContext context) {
      this.request.setHeaders(toHeaders(hr));
      this.logger.logRequest(this.request);
      this.startTime = System.currentTimeMillis();
   }

   static HttpResponse buildResponse(org.apache.hc.core5.http.HttpResponse httpResponse, long startTime) {
      long endTime = System.currentTimeMillis();
      int statusCode = httpResponse.getCode();
      Map<String, List<String>> headers = toHeaders(httpResponse);
      HttpResponse response = new HttpResponse();
      response.setStartTime(startTime);
      response.setResponseTime(endTime - startTime);
      response.setStatus(statusCode);
      response.setStatusText(httpResponse.getReasonPhrase());
      response.setHeaders(headers);
      if (httpResponse instanceof ClassicHttpResponse classicHttpResponse) {
         HttpEntity entity = classicHttpResponse.getEntity();
         if (entity != null) {
            try {
               Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
               String encoding = contentEncoding != null ? contentEncoding.getValue() : null;
               byte[] bytes;
               if ("br".equalsIgnoreCase(encoding)) {
                  try (InputStream is = entity.getContent()) {
                     BrotliInputStream brotliIs = new BrotliInputStream(is);

                     try {
                        bytes = brotliIs.readAllBytes();
                     } catch (Throwable var25) {
                        try {
                           brotliIs.close();
                        } catch (Throwable var22) {
                           var25.addSuppressed(var22);
                        }

                        throw var25;
                     }

                     brotliIs.close();
                  }
               } else if ("gzip".equalsIgnoreCase(encoding)) {
                  try (
                     InputStream is = entity.getContent();
                     GZIPInputStream gzipIs = new GZIPInputStream(is);
                  ) {
                     bytes = gzipIs.readAllBytes();
                  }
               } else if ("deflate".equalsIgnoreCase(encoding)) {
                  try (
                     InputStream is = entity.getContent();
                     InflaterInputStream inflaterIs = new InflaterInputStream(is);
                  ) {
                     bytes = inflaterIs.readAllBytes();
                  }
               } else {
                  bytes = EntityUtils.toByteArray(entity);
               }

               response.setBody(bytes, null);
               response.setContentLength(bytes.length);
               boolean decoded = "br".equalsIgnoreCase(encoding) || "gzip".equalsIgnoreCase(encoding) || "deflate".equalsIgnoreCase(encoding);
               if (decoded) {
                  response.removeHeader("Content-Encoding");
                  if (response.getHeader(HttpUtils.Header.CONTENT_LENGTH.key) != null) {
                     response.removeHeader(HttpUtils.Header.CONTENT_LENGTH.key);
                     response.setHeader(HttpUtils.Header.CONTENT_LENGTH.key, String.valueOf(bytes.length));
                  }
               }
            } catch (Exception var29) {
               LOGGER.warn("error extracting response body: {}", var29.getMessage());
            }
         }
      }

      return response;
   }

   private void mergeCookiesFromStore(HttpResponse response) {
      if (this.cookieStore != null) {
         List<Cookie> storedCookies = this.cookieStore.getCookies();
         Map<String, List<String>> headers = response.getHeaders();
         if (headers == null) {
            headers = new LinkedHashMap<>();
            response.setHeaders(headers);
         }

         List<String> responseCookies = headers.get(HttpUtils.Header.SET_COOKIE.key);
         if (responseCookies == null) {
            responseCookies = new ArrayList<>();
         }

         Set<String> alreadyMerged = new HashSet<>();

         for (String cookieValue : responseCookies) {
            try {
               io.netty.handler.codec.http.cookie.Cookie c = ClientCookieDecoder.LAX.decode(cookieValue);
               if (c != null) {
                  alreadyMerged.add(c.name());
               }
            } catch (Exception var13) {
               LOGGER.debug("could not decode cookie: {}", cookieValue);
            }
         }

         List<String> mergedCookies = new ArrayList<>(responseCookies);

         for (Cookie c : storedCookies) {
            String name = c.getName();
            if (!alreadyMerged.contains(name)) {
               Map<String, Object> map = new LinkedHashMap<>();
               map.put("name", name);
               map.put("value", c.getValue());
               if (c.getDomain() != null) {
                  map.put("domain", c.getDomain());
               }

               if (c.getPath() != null) {
                  map.put("path", c.getPath());
               }

               if (c.getExpiryInstant() != null) {
                  map.put("max-age", c.getExpiryInstant().toEpochMilli());
               }

               map.put("secure", c.isSecure());
               io.netty.handler.codec.http.cookie.Cookie nettyCookie = Cookies.fromMap(map);
               String cookieValue = ServerCookieEncoder.LAX.encode(nettyCookie);
               mergedCookies.add(cookieValue);
               alreadyMerged.add(name);
            }
         }

         if (!mergedCookies.isEmpty()) {
            headers.put(HttpUtils.Header.SET_COOKIE.key, mergedCookies);
         }

         this.cookieStore.clear();
      }
   }

   private static Map<String, List<String>> toHeaders(HttpMessage msg) {
      Header[] headers = msg.getHeaders();
      Map<String, List<String>> map = new LinkedHashMap<>(headers.length);

      for (Header outer : headers) {
         String name = outer.getName();
         Header[] inner = msg.getHeaders(name);
         List<String> list = new ArrayList<>(inner.length);

         for (Header h : inner) {
            list.add(h.getValue());
         }

         map.put(name, list);
      }

      return map;
   }

   private static KeyStore getKeyStore(String keyStorePath, String password, String type) {
      if (keyStorePath == null) {
         return null;
      } else {
         char[] passwordChars = password == null ? null : password.toCharArray();
         if (type == null) {
            type = KeyStore.getDefaultType();
         }

         try {
            KeyStore keyStore = KeyStore.getInstance(type);
            Resource resource = Resource.path(keyStorePath);
            byte[] bytes = resource.getStream().readAllBytes();

            try (InputStream is = new ByteArrayInputStream(bytes)) {
               keyStore.load(is, passwordChars);
            }

            LOGGER.debug("key store key count for {}: {}", keyStorePath, keyStore.size());
            return keyStore;
         } catch (Exception var12) {
            LOGGER.error("key store init failed: {}", var12.getMessage());
            throw new RuntimeException(var12);
         }
      }
   }

   protected HttpClientConnectionManager sharedConnectionManager() {
      return null;
   }

   @Override
   public void close() throws IOException {
      this.released = true;
      this.closeQuietly();
   }

   private void closeQuietly() {
      if (this.httpClient != null) {
         try {
            this.httpClient.close();
            LOGGER.debug("http client closed");
         } catch (Exception var5) {
            LOGGER.warn("error closing http client: {}", var5.getMessage());
         } finally {
            this.httpClient = null;
         }
      }
   }

   private record ClientKey(
      boolean ssl,
      String sslAlgorithm,
      String sslKeyStore,
      String sslKeyStorePassword,
      String sslKeyStoreType,
      String sslTrustStore,
      String sslTrustStorePassword,
      String sslTrustStoreType,
      boolean sslTrustAll,
      String proxyUri,
      String proxyUsername,
      String proxyPassword,
      List<String> nonProxyHosts,
      int readTimeout,
      int connectTimeout,
      boolean followRedirects,
      boolean httpRetryEnabled,
      int retryCount,
      int retryInterval,
      Charset charset,
      String localAddress,
      String ntlmUsername,
      String ntlmPassword,
      String ntlmDomain,
      String ntlmWorkstation
   ) {
      static ApacheHttpClient.ClientKey of(KarateConfig config) {
         List<String> nonProxy = config.getNonProxyHosts();
         return new ApacheHttpClient.ClientKey(
            config.isSslEnabled(),
            config.getSslAlgorithm(),
            config.getSslKeyStore(),
            config.getSslKeyStorePassword(),
            config.getSslKeyStoreType(),
            config.getSslTrustStore(),
            config.getSslTrustStorePassword(),
            config.getSslTrustStoreType(),
            config.isSslTrustAll(),
            config.getProxyUri(),
            config.getProxyUsername(),
            config.getProxyPassword(),
            nonProxy == null ? null : new ArrayList<>(nonProxy),
            config.getReadTimeout(),
            config.getConnectTimeout(),
            config.isFollowRedirects(),
            config.isHttpRetryEnabled(),
            config.isHttpRetryEnabled() ? config.getRetryCount() : 0,
            config.isHttpRetryEnabled() ? config.getRetryInterval() : 0,
            config.getCharset(),
            config.getLocalAddress(),
            config.getNtlmUsername(),
            config.getNtlmPassword(),
            config.getNtlmDomain(),
            config.getNtlmWorkstation()
         );
      }
   }

   static class LenientSslConnectionSocketFactory extends SSLConnectionSocketFactory {
      LenientSslConnectionSocketFactory(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
         super(sslContext, hostnameVerifier);
      }

      public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException {
         return super.createLayeredSocket(socket, target, port, context);
      }
   }
}
