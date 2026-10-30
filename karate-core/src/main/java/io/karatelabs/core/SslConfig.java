package io.karatelabs.core;

import java.util.Map;

public class SslConfig {
   private boolean trustAll;
   private String trustStore;
   private String trustStorePassword;
   private String trustStoreType;
   private String keyStore;
   private String keyStorePassword;
   private String keyStoreType;
   private String algorithm = "TLS";
   private String certPath;
   private String keyPath;
   private boolean clientAuth;
   private String trustStorePath;

   public static SslConfig trustAll() {
      SslConfig config = new SslConfig();
      config.setTrustAll(true);
      return config;
   }

   public static SslConfig fromMap(Map<String, Object> map) {
      SslConfig config = new SslConfig();
      if (map.containsKey("trustAll")) {
         config.setTrustAll(Boolean.TRUE.equals(map.get("trustAll")));
      }

      if (map.containsKey("keyStore")) {
         config.setKeyStore((String)map.get("keyStore"));
      }

      if (map.containsKey("keyStorePassword")) {
         config.setKeyStorePassword((String)map.get("keyStorePassword"));
      }

      if (map.containsKey("keyStoreType")) {
         config.setKeyStoreType((String)map.get("keyStoreType"));
      }

      if (map.containsKey("trustStore")) {
         config.setTrustStore((String)map.get("trustStore"));
      }

      if (map.containsKey("trustStorePassword")) {
         config.setTrustStorePassword((String)map.get("trustStorePassword"));
      }

      if (map.containsKey("trustStoreType")) {
         config.setTrustStoreType((String)map.get("trustStoreType"));
      }

      if (map.containsKey("algorithm")) {
         config.setAlgorithm((String)map.get("algorithm"));
      }

      if (map.containsKey("cert")) {
         config.setCertPath((String)map.get("cert"));
      }

      if (map.containsKey("key")) {
         config.setKeyPath((String)map.get("key"));
      }

      return config;
   }

   public boolean isTrustAll() {
      return this.trustAll;
   }

   public void setTrustAll(boolean trustAll) {
      this.trustAll = trustAll;
   }

   public String getTrustStore() {
      return this.trustStore;
   }

   public void setTrustStore(String trustStore) {
      this.trustStore = trustStore;
   }

   public String getTrustStorePassword() {
      return this.trustStorePassword;
   }

   public void setTrustStorePassword(String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
   }

   public String getTrustStoreType() {
      return this.trustStoreType;
   }

   public void setTrustStoreType(String trustStoreType) {
      this.trustStoreType = trustStoreType;
   }

   public String getKeyStore() {
      return this.keyStore;
   }

   public void setKeyStore(String keyStore) {
      this.keyStore = keyStore;
   }

   public String getKeyStorePassword() {
      return this.keyStorePassword;
   }

   public void setKeyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
   }

   public String getKeyStoreType() {
      return this.keyStoreType;
   }

   public void setKeyStoreType(String keyStoreType) {
      this.keyStoreType = keyStoreType;
   }

   public String getAlgorithm() {
      return this.algorithm;
   }

   public void setAlgorithm(String algorithm) {
      this.algorithm = algorithm;
   }

   public String getCertPath() {
      return this.certPath;
   }

   public void setCertPath(String certPath) {
      this.certPath = certPath;
   }

   public String getKeyPath() {
      return this.keyPath;
   }

   public void setKeyPath(String keyPath) {
      this.keyPath = keyPath;
   }

   public boolean isClientAuth() {
      return this.clientAuth;
   }

   public void setClientAuth(boolean clientAuth) {
      this.clientAuth = clientAuth;
   }

   public String getTrustStorePath() {
      return this.trustStorePath;
   }

   public void setTrustStorePath(String trustStorePath) {
      this.trustStorePath = trustStorePath;
   }
}
