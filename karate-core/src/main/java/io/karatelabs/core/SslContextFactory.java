package io.karatelabs.core;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import io.karatelabs.output.LogContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;

public class SslContextFactory {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;

   public static SSLContext createClientContext(SslConfig config) {
      try {
         if (config.isTrustAll()) {
            return createTrustAllContext(config.getAlgorithm());
         } else {
            TrustManager[] trustManagers = loadTrustManagers(config);
            KeyManager[] keyManagers = loadKeyManagers(config);
            SSLContext ctx = SSLContext.getInstance(config.getAlgorithm());
            ctx.init(keyManagers, trustManagers, new SecureRandom());
            return ctx;
         }
      } catch (Exception var4) {
         throw new RuntimeException("failed to create client SSL context: " + var4.getMessage(), var4);
      }
   }

   public static SSLContext createServerContext(SslConfig config) {
      try {
         return config.getCertPath() == null ? SslUtils.generateSelfSigned() : loadFromPem(config.getCertPath(), config.getKeyPath(), config.getAlgorithm());
      } catch (Exception var2) {
         throw new RuntimeException("failed to create server SSL context: " + var2.getMessage(), var2);
      }
   }

   private static SSLContext createTrustAllContext(String algorithm) throws Exception {
      TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
         @Override
         public void checkClientTrusted(X509Certificate[] chain, String authType) {
         }

         @Override
         public void checkServerTrusted(X509Certificate[] chain, String authType) {
         }

         @Override
         public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
         }
      }};
      SSLContext ctx = SSLContext.getInstance(algorithm != null ? algorithm : "TLS");
      ctx.init(null, trustManagers, new SecureRandom());
      return ctx;
   }

   private static TrustManager[] loadTrustManagers(SslConfig config) throws Exception {
      if (config.getTrustStore() == null) {
         return null;
      } else {
         String type = config.getTrustStoreType() != null ? config.getTrustStoreType() : "JKS";
         KeyStore trustStore = KeyStore.getInstance(type);
         byte[] data = loadResource(config.getTrustStore());
         char[] password = config.getTrustStorePassword() != null ? config.getTrustStorePassword().toCharArray() : null;

         try (InputStream is = new ByteArrayInputStream(data)) {
            trustStore.load(is, password);
         }

         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(trustStore);
         return tmf.getTrustManagers();
      }
   }

   private static KeyManager[] loadKeyManagers(SslConfig config) throws Exception {
      if (config.getKeyStore() == null) {
         return null;
      } else {
         String type = config.getKeyStoreType() != null ? config.getKeyStoreType() : "PKCS12";
         KeyStore keyStore = KeyStore.getInstance(type);
         byte[] data = loadResource(config.getKeyStore());
         char[] password = config.getKeyStorePassword() != null ? config.getKeyStorePassword().toCharArray() : null;

         try (InputStream is = new ByteArrayInputStream(data)) {
            keyStore.load(is, password);
         }

         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         kmf.init(keyStore, password);
         return kmf.getKeyManagers();
      }
   }

   private static SSLContext loadFromPem(String certPath, String keyPath, String algorithm) throws Exception {
      byte[] certData = loadResource(certPath);
      byte[] keyData = loadResource(keyPath);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      Certificate cert;
      try (InputStream is = new ByteArrayInputStream(certData)) {
         cert = cf.generateCertificate(is);
      }

      PrivateKey var13 = loadPrivateKey(keyData);
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      keyStore.setKeyEntry("server", var13, new char[0], new Certificate[]{cert});
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, new char[0]);
      SSLContext ctx = SSLContext.getInstance(algorithm != null ? algorithm : "TLS");
      ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
      return ctx;
   }

   private static PrivateKey loadPrivateKey(byte[] keyData) throws Exception {
      String keyString = new String(keyData);
      String privateKeyPEM = keyString.replace("-----BEGIN PRIVATE KEY-----", "")
         .replace("-----END PRIVATE KEY-----", "")
         .replace("-----BEGIN RSA PRIVATE KEY-----", "")
         .replace("-----END RSA PRIVATE KEY-----", "")
         .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

      try {
         return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
      } catch (Exception var8) {
         try {
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
         } catch (Exception var7) {
            throw new RuntimeException("failed to load private key: unsupported algorithm", var7);
         }
      }
   }

   private static byte[] loadResource(String path) {
      Resource resource = Resource.path(path);
      return FileUtils.toBytes(resource.getText());
   }
}
