package io.karatelabs.ext.mutation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public record ResponseMutationSpec(String component1, String component2, String component3, String component4, int component5, int component6, String component7, String k, String l, Object m, int n) {
   public static final String FIELD_DROP = "FIELD_DROP";
   public static final String FIELD_NULL = "FIELD_NULL";
   public static final String TYPE_FLIP = "TYPE_FLIP";
   public static final String VALUE_OFF_BY_ONE = "VALUE_OFF_BY_ONE";
   public static final String CARDINALITY_EMPTY = "CARDINALITY_EMPTY";
   public static final String ENUM_SUBSTITUTION = "ENUM_SUBSTITUTION";
   public static final String STATUS_SWAP = "STATUS_SWAP";
   public static final String DECLARED_STATUS = "DECLARED_STATUS";
   public static final String CONTENT_TYPE_TAMPER = "CONTENT_TYPE_TAMPER";
   public static final String MALFORMED_BODY = "MALFORMED_BODY";

   public ResponseMutationSpec(String component1, String component2, String component3, String component4, int component5, int component6, String component7, String k, String l, Object m, int n) {
      this.component1 = component1;
      this.component2 = component2;
      this.component3 = component3;
      this.component4 = component4;
      this.component5 = component5;
      this.component6 = component6;
      this.component7 = component7;
      this.k = k;
      this.l = l;
      this.m = m;
      this.n = n;
   }

   public String artifactKey() {
      try {
         byte[] var1 = MessageDigest.getInstance("SHA-256").digest(this.component1.getBytes(StandardCharsets.UTF_8));
         String var2 = this.component1.replaceAll("[^A-Za-z0-9._-]", "_");
         if (var2.length() > 200) {
            var2 = var2.substring(0, 200);
         }

         return var2 + "-" + HexFormat.of().formatHex(var1, 0, 16);
      } catch (NoSuchAlgorithmException var3) {
         throw new AssertionError("SHA-256 is a mandatory JVM algorithm", var3);
      }
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", this.component1);
      var1.put("mutatorName", this.component2);
      var1.put("operation", this.component3);
      var1.put("site", this.component4);
      var1.put("original", this.k);
      var1.put("mutated", this.l);
      return var1;
   }

   public String operationText() {
      return this.component3 + " " + this.component4;
   }

   public String d() {
      return this.component1;
   }

   public String e() {
      return this.component2;
   }

   public String f() {
      return this.component3;
   }

   public String g() {
      return this.component4;
   }

   public int h() {
      return this.component5;
   }

   public int i() {
      return this.component6;
   }

   public String j() {
      return this.component7;
   }

   public String k() {
      return this.k;
   }

   public String l() {
      return this.l;
   }

   public Object m() {
      return this.m;
   }

   public int n() {
      return this.n;
   }
}
