package io.karatelabs.core;

import io.karatelabs.output.LogContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;

public class SslUtils {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final int VALIDITY_DAYS = 365;

   public static SSLContext generateSelfSigned() {
      try {
         Date notBefore = new Date();
         Date notAfter = new Date(notBefore.getTime() + 31536000000L);
         SelfSignedCertificate ssc = new SelfSignedCertificate("localhost", notBefore, notAfter);
         KeyStore keyStore = KeyStore.getInstance("PKCS12");
         keyStore.load(null, null);
         CertificateFactory cf = CertificateFactory.getInstance("X.509");

         Certificate cert;
         try (FileInputStream fis = new FileInputStream(ssc.certificate())) {
            cert = cf.generateCertificate(fis);
         }

         PrivateKey var12 = loadPrivateKeyFromFile(ssc.privateKey());
         keyStore.setKeyEntry("server", var12, new char[0], new Certificate[]{cert});
         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         kmf.init(keyStore, new char[0]);
         SSLContext ctx = SSLContext.getInstance("TLS");
         ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
         logger.info("generated self-signed certificate for localhost (valid {} days)", 365);
         return ctx;
      } catch (Exception var11) {
         throw new RuntimeException("failed to generate self-signed certificate: " + var11.getMessage(), var11);
      }
   }

   public static SslContext generateNettySslContext() {
      try {
         Date notBefore = new Date();
         Date notAfter = new Date(notBefore.getTime() + 31536000000L);
         SelfSignedCertificate ssc = new SelfSignedCertificate("localhost", notBefore, notAfter);
         return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
      } catch (Exception var3) {
         throw new RuntimeException("failed to generate Netty SSL context: " + var3.getMessage(), var3);
      }
   }

   public static SslContext createNettySslContext(File certFile, File keyFile) {
      try {
         return SslContextBuilder.forServer(certFile, keyFile).build();
      } catch (Exception var3) {
         throw new RuntimeException("failed to create SSL context from files: " + var3.getMessage(), var3);
      }
   }

   public static SslContext createNettySslContext(String certPath, String keyPath) {
      return createNettySslContext(new File(certPath), new File(keyPath));
   }

   private static PrivateKey loadPrivateKeyFromFile(File keyFile) throws Exception {
      byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
      String keyString = new String(keyBytes, StandardCharsets.UTF_8);
      String privateKeyPEM = keyString.replace("-----BEGIN PRIVATE KEY-----", "")
         .replace("-----END PRIVATE KEY-----", "")
         .replace("-----BEGIN RSA PRIVATE KEY-----", "")
         .replace("-----END RSA PRIVATE KEY-----", "")
         .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

      try {
         return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
      } catch (Exception var9) {
         try {
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
         } catch (Exception var8) {
            throw new RuntimeException("failed to load private key: unsupported algorithm", var8);
         }
      }
   }
}
