package io.karatelabs.ext.postman;

import io.karatelabs.core.Faker;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.ObjectLike;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostmanVariableScope implements ObjectLike {
   private static final Faker a = new Faker();
   private final String b;
   private final PostmanVariableScope c;
   private final Map<String, Object> d = new HashMap<>();
   private PmRuntime e;
   private static final Pattern f = Pattern.compile("\\{\\{([^}]+)}}");

   PostmanVariableScope(String var1, PostmanVariableScope var2) {
      this.b = var1;
      this.c = var2;
   }

   void a(PmRuntime var1) {
      this.e = var1;
   }

   public boolean a(String var1) {
      return this.d.containsKey(var1);
   }

   @Override
   public void putMember(String var1, Object var2) {
      this.d.put(var1, var2);
   }

   @Override
   public void removeMember(String var1) {
      this.d.remove(var1);
   }

   public Object b(String var1) {
      if (this.d.containsKey(var1)) {
         return this.d.get(var1);
      } else if (this.c != null) {
         return this.c.b(var1);
      } else {
         Object var2 = this.d(var1);
         return var2 != null ? var2 : null;
      }
   }

   private Object d(String var1) {
      return switch (var1) {
         case "$guid", "$randomUUID" -> UUID.randomUUID().toString();
         case "$timestamp" -> e("timestamp");
         case "$isoTimestamp" -> e("isoTimestamp");
         case "$randomFirstName" -> e("firstName");
         case "$randomLastName" -> e("lastName");
         case "$randomFullName" -> e("fullName");
         case "$randomEmail" -> e("email");
         case "$randomUserName" -> e("userName");
         case "$randomPhoneNumber" -> e("phoneNumber");
         case "$randomCity" -> e("city");
         case "$randomCountry" -> e("country");
         case "$randomStreetAddress" -> e("streetAddress");
         case "$randomInt" -> e("randomInt");
         case "$randomBoolean" -> e("randomBoolean");
         case "$iteration" -> this.e != null ? this.e.iteration : -1;
         default -> null;
      };
   }

   private static Object e(String var0) {
      JavaCallable var1 = (JavaCallable)a.jsGet(var0);
      return var1.call(null);
   }

   String c(String var1) {
      Matcher var2 = f.matcher(var1);
      StringBuilder var3 = new StringBuilder();

      while (var2.find()) {
         String var4 = var2.group(1).trim();
         Object var5 = this.b(var4);
         String var6;
         if (var5 == null) {
            var6 = var2.group(0);
         } else {
            var6 = var5 + "";
         }

         var6 = Matcher.quoteReplacement(var6);
         var2.appendReplacement(var3, var6);
      }

      var2.appendTail(var3);
      return var3.toString();
   }

   @Override
   public Map<String, Object> toMap() {
      return this.d;
   }

   public void a() {
      this.d.clear();
   }

   @Override
   public Object getMember(String var1) {
      return switch (var1) {
         case "get" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.b(var2[0] + "");
         case "set" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> {
            this.putMember(var2[0] + "", var2[1]);
            return null;
         };
         case "has" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.a(var2[0] + "");
         case "unset" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> {
            this.removeMember(var2[0] + "");
            return null;
         };
         case "replaceIn" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.c(var2[0] + "");
         case "toObject" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.d;
         default -> throw new UnsupportedPostmanFeatureException(this.b + "." + var1);
      };
   }
}
