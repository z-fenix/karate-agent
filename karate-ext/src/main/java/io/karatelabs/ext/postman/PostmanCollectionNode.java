package io.karatelabs.ext.postman;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostmanCollectionNode {
   static final Logger a = LoggerFactory.getLogger(PostmanCollectionNode.class);
   public final Json b;
   public final PostmanCollectionNode c;
   public final List<PostmanCollectionNode> d;

   public PostmanCollectionNode(PostmanCollectionNode var1, Json var2) {
      this.c = var1;
      this.b = var2;
      List var3 = var2.get("item", null);
      if (var3 == null) {
         this.d = Collections.emptyList();
      } else {
         this.d = new ArrayList<>(var3.size());

         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var3)) {
            PostmanCollectionNode var6 = new PostmanCollectionNode(this, Json.of(var5));
            this.d.add(var6);
         }
      }

      this.L();
   }

   private void L() {
      if (this.b()) {
         String var1 = this.h();
         if (var1 != null && !var1.isEmpty()) {
            Object var2 = this.b.get("request.url");
            if (var2 instanceof String) {
               int var3 = var1.indexOf(63);
               if (var3 != -1) {
                  String var4 = var1.substring(0, var3);
                  String var5 = var1.substring(var3 + 1);
                  this.e(var4);
                  if (!var5.isEmpty()) {
                     this.y(var5);
                  }
               }
            }
         }
      }
   }

   public boolean a() {
      return this.c == null;
   }

   public boolean b() {
      return this.b.get("request", null) instanceof Map;
   }

   public PostmanCollectionNode c() {
      PostmanCollectionNode var1 = this;

      while (var1.c != null) {
         var1 = var1.c;
      }

      return var1;
   }

   public PostmanCollectionNode a(String var1) {
      return a(this.c(), var1);
   }

   private static PostmanCollectionNode a(PostmanCollectionNode var0, String var1) {
      String var2 = var0.d();
      if (var2 != null && var2.equals(var1)) {
         return var0;
      } else {
         String var3 = var0.e();
         if (var3 != null && var3.equals(var1)) {
            return var0;
         } else {
            for (PostmanCollectionNode var5 : var0.d) {
               PostmanCollectionNode var6 = a(var5, var1);
               if (var6 != null) {
                  return var6;
               }
            }

            return null;
         }
      }
   }

   public String d() {
      String var1 = this.b.get("name", null);
      if (var1 == null) {
         var1 = this.b.get("info.name", null);
      }

      return var1;
   }

   public String e() {
      return this.b.pathExists("id") ? this.b.get("id") : this.b.get("_postman_id", null);
   }

   public void b(String var1) {
      this.b.set("name", var1);
   }

   private String M() {
      return this.a() ? "info.description" : (this.b() ? "request.description" : "description");
   }

   public String f() {
      String var1 = this.M();
      Object var2 = this.b.get(var1, null);
      if (var2 == null) {
         return null;
      } else if (var2 instanceof String) {
         return (String)var2;
      } else if (var2 instanceof Map) {
         Object var3 = ((Map)var2).get("content");
         return var3 != null ? var3.toString() : null;
      } else {
         return var2.toString();
      }
   }

   public void c(String var1) {
      String var2 = this.M();
      if (var1 != null && !var1.isEmpty()) {
         this.b.set(var2, var1);
      } else {
         this.b.remove(var2);
      }
   }

   public String g() {
      return this.b.get("request.method", null);
   }

   public void d(String var1) {
      this.b.set("request.method", var1);
   }

   public String h() {
      return this.b.get("request.url.raw", null);
   }

   public void e(String var1) {
      this.b.setAsString("request.url.raw", var1);
   }

   public String i() {
      return this.b.get("request.url", null);
   }

   public List<String> j() {
      Object var1 = this.b.get("request.url.host", null);
      if (var1 == null) {
         return null;
      } else if (var1 instanceof List) {
         return (List<String>)var1;
      } else if (var1 instanceof String var2) {
         if (var2.endsWith(".")) {
            var2 = var2.substring(0, var2.length() - 1);
         }

         return Arrays.asList(var2.split("\\."));
      } else {
         return null;
      }
   }

   public String k() {
      return this.b.get("request.url.port", null);
   }

   private void N() {
      if (this.b.pathExists("request.url") && this.b.get("request.url") instanceof String var2) {
         this.b.set("request.url", new HashMap());
         this.u(var2);
      }
   }

   public void f(String var1) {
      this.N();
      this.b.setAsString("request.url.port", var1);
   }

   public void a(List<String> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      this.N();
      if (var1 == null) {
         this.b.remove("request.url.host");
      } else {
         this.b.set("request.url.host", var1);
      }
   }

   public List<String> l() {
      Object var1 = this.b.get("request.url.path", null);
      if (var1 == null) {
         return null;
      } else if (var1 instanceof List) {
         return (List<String>)var1;
      } else if (var1 instanceof String var2) {
         if (var2.startsWith("/")) {
            var2 = var2.substring(1);
         }

         return (List<String>)(var2.isEmpty() ? new ArrayList<>() : Arrays.asList(var2.split("/")));
      } else {
         return null;
      }
   }

   public void b(List<String> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      this.N();
      if (var1 == null) {
         this.b.remove("request.url.path");
      } else {
         this.b.set("request.url.path", var1);
      }
   }

   public String m() {
      return this.b.get("request.url.protocol", null);
   }

   private void v(String var1) {
      this.N();
      this.b.setAsString("request.url.protocol", var1);
   }

   public List<Map<String, Object>> n() {
      return this.b.get("request.url.query", new ArrayList<>());
   }

   public void c(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      this.N();
      if (var1 == null) {
         this.b.remove("request.url.query");
      } else {
         this.b.set("request.url.query", var1);
      }
   }

   public List<Map<String, Object>> o() {
      return this.b.get("request.url.variable", new ArrayList<>());
   }

   public void d(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("request.url.variable");
      } else {
         this.b.set("request.url.variable", var1);
      }
   }

   public List<Map<String, Object>> p() {
      Object var1 = this.b.get("request.header", null);
      if (var1 == null) {
         return new ArrayList<>();
      } else if (var1 instanceof List) {
         return (List<Map<String, Object>>)var1;
      } else if (var1 instanceof String var2) {
         ArrayList var3 = new ArrayList();
         String[] var4 = var2.split("\r?\n");

         for (String var8 : var4) {
            if (!var8.trim().isEmpty()) {
               int var9 = var8.indexOf(58);
               if (var9 > 0) {
                  HashMap var10 = new HashMap();
                  var10.put("key", var8.substring(0, var9).trim());
                  var10.put("value", var8.substring(var9 + 1).trim());
                  var3.add(var10);
               }
            }
         }

         return var3;
      } else {
         return new ArrayList<>();
      }
   }

   public void e(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("request.header");
      } else {
         this.b.set("request.header", var1);
      }
   }

   public void a(String var1, String var2) {
      var var3 = this.p();
      HashMap var4 = new HashMap();
      var4.put("key", var1);
      var4.put("value", var2);
      var3.add(var4);
      this.e(var3);
   }

   public String g(String var1) {
      var var2 = this.w(var1);
      if (var2.isEmpty()) {
         return null;
      } else {
         var var3 = this.p();
         Map var4 = (Map)var3.get((Integer)var2.getLast());
         return (String)var4.get("value");
      }
   }

   private List<Integer> w(String var1) {
      var1 = var1.toLowerCase();
      ArrayList var2 = new ArrayList();
      var var3 = this.p();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Map var5 = (Map)var3.get(var4);
         Object var6 = var5.get("key");
         if (var6 != null && var6.toString().toLowerCase().equals(var1)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public boolean h(String var1) {
      return !this.w(var1).isEmpty();
   }

   public void i(String var1) {
      var var2 = this.p();
      ArrayList var3 = new ArrayList();
      var var4 = this.w(var1);

      for (int var5 = 0; var5 < var2.size(); var5++) {
         if (!var4.contains(var5)) {
            var3.add((Map)var2.get(var5));
         }
      }

      this.b.set("request.header", var3);
   }

   public void b(String var1, String var2) {
      var var3 = this.p();
      var var4 = this.w(var1);
      if (var4.isEmpty()) {
         this.a(var1, var2);
      } else {
         for (int var6 : (Iterable<java.lang.Integer>) (Iterable<?>) (var4)) {
            Map var7 = (Map)var3.get(var6);
            var7.put("value", var2);
         }

         this.e(var3);
      }
   }

   public String q() {
      return !this.b() ? this.v() : this.b.get("request.auth.type", null);
   }

   public void j(String var1) {
      if (!this.b()) {
         this.k(var1);
      } else if (var1 == null) {
         this.b.remove("request.auth.type");
      } else {
         this.b.set("request.auth.type", var1);
      }
   }

   public List<Map<String, Object>> r() {
      return (List<Map<String, Object>>)(!this.b() ? this.w() : this.b.get("request.auth.basic", new ArrayList<>()));
   }

   public void f(List<Map<String, Object>> var1) {
      if (!this.b()) {
         this.i(var1);
      } else {
         if (var1 != null && var1.isEmpty()) {
            var1 = null;
         }

         if (var1 == null) {
            this.b.remove("request.auth.basic");
         } else {
            this.b.set("request.auth.basic", var1);
         }
      }
   }

   public List<Map<String, Object>> s() {
      return (List<Map<String, Object>>)(!this.b() ? this.x() : this.b.get("request.auth.bearer", new ArrayList<>()));
   }

   public List<Map<String, Object>> t() {
      return !this.b() ? this.b.get("auth.apikey", new ArrayList<>()) : this.b.get("request.auth.apikey", new ArrayList<>());
   }

   public void g(List<Map<String, Object>> var1) {
      if (!this.b()) {
         this.j(var1);
      } else {
         if (var1 != null && var1.isEmpty()) {
            var1 = null;
         }

         if (var1 == null) {
            this.b.remove("request.auth.bearer");
         } else {
            this.b.set("request.auth.bearer", var1);
         }
      }
   }

   public List<Map<String, Object>> u() {
      return (List<Map<String, Object>>)(!this.b() ? this.y() : this.b.get("request.auth.oauth2", new ArrayList<>()));
   }

   public void h(List<Map<String, Object>> var1) {
      if (!this.b()) {
         this.k(var1);
      } else {
         if (var1 != null && var1.isEmpty()) {
            var1 = null;
         }

         if (var1 == null) {
            this.b.remove("request.auth.oauth2");
         } else {
            this.b.set("request.auth.oauth2", var1);
         }
      }
   }

   public String v() {
      return this.b.get("auth.type", null);
   }

   public void k(String var1) {
      if (var1 == null) {
         this.b.remove("auth.type");
      } else {
         this.b.set("auth.type", var1);
      }
   }

   public List<Map<String, Object>> w() {
      return this.b.get("auth.basic", new ArrayList<>());
   }

   public void i(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("auth.basic");
      } else {
         this.b.set("auth.basic", var1);
      }
   }

   public List<Map<String, Object>> x() {
      return this.b.get("auth.bearer", new ArrayList<>());
   }

   public void j(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("auth.bearer");
      } else {
         this.b.set("auth.bearer", var1);
      }
   }

   public List<Map<String, Object>> y() {
      return this.b.get("auth.oauth2", new ArrayList<>());
   }

   public void k(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("auth.oauth2");
      } else {
         this.b.set("auth.oauth2", var1);
      }
   }

   public String z() {
      return this.b.get("request.body.mode", null);
   }

   public void l(String var1) {
      this.b.set("request.body.mode", var1);
   }

   public String A() {
      return this.b.get("request.body.raw", null);
   }

   public void m(String var1) {
      if (var1 == null) {
         this.b.remove("request.body.raw");
      } else {
         this.b.setAsString("request.body.raw", var1);
      }
   }

   public List<Map<String, Object>> B() {
      return this.b.get("request.body.urlencoded", new ArrayList<>());
   }

   public void l(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("request.body.urlencoded");
      } else {
         this.b.set("request.body.urlencoded", var1);
      }
   }

   public List<Map<String, Object>> C() {
      return this.b.get("request.body.formdata", new ArrayList<>());
   }

   public void m(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("request.body.formdata");
      } else {
         this.b.set("request.body.formdata", var1);
      }
   }

   public String D() {
      return this.b.get("request.body.file.src", null);
   }

   public void n(String var1) {
      this.b.set("request.body.file.src", var1);
   }

   public String E() {
      return this.b.get("request.body.graphql.query", null);
   }

   public void o(String var1) {
      this.b.set("request.body.graphql.query", var1);
   }

   public String F() {
      Object var1 = this.b.get("request.body.graphql.variables", null);
      if (var1 == null) {
         return null;
      } else {
         return var1 instanceof Map ? Json.of(var1).toString() : var1.toString();
      }
   }

   public void p(String var1) {
      this.b.setAsString("request.body.graphql.variables", var1);
   }

   public List<Map<String, Object>> G() {
      return this.b.get("variable", new ArrayList<>());
   }

   public void n(List<Map<String, Object>> var1) {
      if (var1 != null && var1.isEmpty()) {
         var1 = null;
      }

      if (var1 == null) {
         this.b.remove("variable");
      } else {
         this.b.set("variable", var1);
      }
   }

   public String H() {
      var var1 = this.q("prerequest");
      String var2 = StringUtils.join(var1, "\n");
      return StringUtils.trimToNull(var2);
   }

   public String I() {
      var var1 = this.q("test");
      String var2 = StringUtils.join(var1, "\n");
      return StringUtils.trimToNull(var2);
   }

   public List<String> q(String var1) {
      ArrayList var2 = new ArrayList();
      List var3 = this.b.get("event", null);
      if (var3 == null) {
         return var2;
      } else {
         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var3)) {
            String var6 = (String)var5.get("listen");
            if (var1.equals(var6)) {
               Object var7 = var5.get("script");
               if (var7 != null && !(var7 instanceof String) && var7 instanceof Map var8) {
                  Object var9 = var8.get("exec");
                  if (var9 instanceof List) {
                     var2.addAll((List)var9);
                  } else if (var9 instanceof String) {
                     var2.addAll(Arrays.asList(((String)var9).split("\r?\n")));
                  }
               }
            }
         }

         return var2;
      }
   }

   public List<String> r(String var1) {
      ArrayList var2 = new ArrayList();
      List var3 = this.b.get("event", null);
      if (var3 == null) {
         return var2;
      } else {
         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var3)) {
            if (var1.equals(var5.get("listen")) && var5.get("script") instanceof Map var6) {
               Object var11 = var6.get("exec");
               String var8 = var11 instanceof List var10 ? StringUtils.join(var10, "\n") : (var11 instanceof String var9 ? var9 : null);
               var8 = StringUtils.trimToNull(var8);
               if (var8 != null) {
                  var2.add(var8);
               }
            }
         }

         return var2;
      }
   }

   public void s(String var1) {
      var var2 = x(var1);
      this.a("prerequest", var2);
   }

   public void t(String var1) {
      var var2 = x(var1);
      this.a("test", var2);
   }

   private static List<String> x(String var0) {
      return (List<String>)(var0 != null && !var0.trim().isEmpty() ? Arrays.asList(var0.split("\n", -1)) : new ArrayList<>());
   }

   private void a(String var1, List<String> var2) {
      Object var3 = (List)this.b.get("event", null);
      if (var3 == null) {
         var3 = new ArrayList();
         this.b.set("event", var3);
      }

      Map var4 = null;

      for (Map var6 : (Iterable<Map>) (Iterable<?>) (var3)) {
         if (var1.equals(var6.get("listen"))) {
            var4 = var6;
            break;
         }
      }

      if (var4 == null && !var2.isEmpty()) {
         var4 = new HashMap();
         var4.put("listen", var1);
         ((java.util.List)var3).add(var4);
      }

      if (var4 != null) {
         if (var2.isEmpty()) {
            var4.remove("script");
         } else {
            Object var7 = (Map)var4.get("script");
            if (var7 == null) {
               var7 = new HashMap();
               var4.put("script", var7);
            }

            ((java.util.Map)var7).put("exec", new ArrayList(var2));
         }
      }
   }

   public String J() {
      StringBuilder var1 = new StringBuilder();
      String var2 = this.m();
      if (var2 != null) {
         var1.append(var2).append("://");
      }

      var var3 = this.j();
      if (var3 != null && !var3.isEmpty()) {
         var1.append(StringUtils.join(var3, "."));
      }

      String var4 = this.k();
      if (var4 != null) {
         var1.append(":").append(var4);
      }

      var var5 = this.l();
      if (var5 != null && !var5.isEmpty()) {
         var1.append("/");
         var1.append(StringUtils.join(var5, "/"));
      }

      String var6 = var1.toString();
      if (var6.isEmpty()) {
         var6 = this.h();
         if (var6 == null) {
            var6 = this.i();
         }

         if (var6 != null) {
            int var7 = var6.indexOf(63);
            if (var7 != -1) {
               var6 = var6.substring(0, var7);
            }
         }
      }

      return var6;
   }

   public String K() {
      return this.a(new PostmanVariableScope("pm.variables", null));
   }

   public String a(PostmanVariableScope var1) {
      StringBuilder var2 = new StringBuilder();
      String var3 = this.m();
      if (var3 != null) {
         var3 = var1.c(var3);
         var2.append(var3).append("://");
      }

      var var4 = this.j();
      if (var4 != null && !var4.isEmpty()) {
         ArrayList var5 = new ArrayList();

         for (String var7 : (Iterable<String>) (Iterable<?>) (var4)) {
            var5.add(var1.c(var7));
         }

         var2.append(StringUtils.join(var5, "."));
      }

      String var16 = this.k();
      if (var16 != null) {
         var16 = var1.c(var16);
         var2.append(":").append(var16);
      }

      var var18 = this.l();
      if (var18 != null && !var18.isEmpty()) {
         var2.append("/");
         HashMap var19 = new HashMap();
         var var8 = this.o();
         if (var8 != null) {
            for (Map var10 : (Iterable<Map>) (Iterable<?>) (var8)) {
               String var11 = (String)var10.get("key");
               String var12 = (String)var10.get("value");
               if (var11 != null && var12 != null) {
                  var12 = var1.c(var12);
                  var19.put(var11, var12);
               }
            }
         }

         ArrayList var24 = new ArrayList();

         for (String var30 : (Iterable<String>) (Iterable<?>) (var18)) {
            if (var30.startsWith(":")) {
               String var35 = var30.substring(1);
               Object var13 = var1.b(var35);
               String var14;
               if (var13 != null) {
                  var14 = var13.toString();
               } else {
                  var14 = (String)var19.get(var35);
               }

               var24.add(var14 != null ? var14 : var30);
            } else {
               var24.add(var1.c(var30));
            }
         }

         var2.append(StringUtils.join(var24, "/"));
      }

      var var20 = this.n();
      if (var20 != null && !var20.isEmpty()) {
         ArrayList var21 = new ArrayList();

         for (Map var28 : (Iterable<Map>) (Iterable<?>) (var20)) {
            String var31 = (String)var28.get("key");
            String var36 = (String)var28.get("value");
            if (var31 != null) {
               var31 = var1.c(var31);
               if (var36 != null) {
                  var36 = var1.c(var36);
               }

               var21.add(var36 != null ? var31 + "=" + var36 : var31);
            }
         }

         if (!var21.isEmpty()) {
            var2.append("?");
            var2.append(StringUtils.join(var21, "&"));
         }
      }

      String var22 = var2.toString();
      if (var22.isEmpty()) {
         var22 = this.h();
         if (var22 == null) {
            var22 = this.i();
         }

         if (var22 != null) {
            var22 = var1.c(var22);
            Pattern var26 = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
            Matcher var29 = var26.matcher(var22);
            StringBuffer var33 = new StringBuffer();

            while (var29.find()) {
               String var37 = var29.group(1);
               Object var38 = var1.b(var37);
               String var39 = var38 != null ? var38.toString() : var29.group(0);
               var29.appendReplacement(var33, Matcher.quoteReplacement(var39));
            }

            var29.appendTail(var33);
            var22 = var33.toString();
         }
      }

      return var22;
   }

   public void u(String var1) {
      this.e(var1);
      if (var1 != null && !var1.trim().isEmpty()) {
         if (!var1.contains("{{") || !var1.contains("}}")) {
            URI var2;
            try {
               var2 = new URI(var1);
            } catch (URISyntaxException var16) {
               a.warn("Could not parse URL: {}", var1, var16);
               return;
            }

            String var3 = var2.getScheme();
            if (var3 != null) {
               this.v(var3);
            }

            String var4 = var2.getHost();
            if (var4 != null) {
               var var5 = Arrays.asList(var4.split("\\."));
               this.a(var5);
            }

            int var17 = var2.getPort();
            if (var17 != -1) {
               this.f(String.valueOf(var17));
            }

            String var6 = var2.getPath();
            if (var6 != null && !var6.isEmpty()) {
               if (var6.startsWith("/")) {
                  var6 = var6.substring(1);
               }

               ArrayList var7 = new ArrayList();
               ArrayList var8 = new ArrayList();
               if (!var6.isEmpty()) {
                  String[] var9 = var6.split("/");

                  for (String var13 : var9) {
                     var7.add(var13);
                     if (var13.startsWith(":")) {
                        String var14 = var13.substring(1);
                        HashMap var15 = new HashMap();
                        var15.put("key", var14);
                        var15.put("value", "");
                        var8.add(var15);
                     }
                  }
               }

               this.b(var7);
               if (!var8.isEmpty()) {
                  this.d(var8);
               }
            }

            String var18 = var2.getQuery();
            if (var18 != null && !var18.isEmpty()) {
               this.y(var18);
            } else {
               this.c((List<Map<String, Object>>)null);
            }
         }
      }
   }

   private void y(String var1) {
      String[] var2 = var1.split("&");
      ArrayList var3 = new ArrayList();

      for (String var7 : var2) {
         String[] var8 = var7.split("=", 2);
         HashMap var9 = new HashMap();
         var9.put("key", var8[0]);
         String var10;
         if (var8.length > 1) {
            var10 = var8[1];
         } else {
            var10 = "";
         }

         var9.put("value", var10);
         var3.add(var9);
      }

      var var11 = this.n();
      if (var11 != null) {
         var11.addAll(var3);
      }

      this.c(var3);
   }
}
