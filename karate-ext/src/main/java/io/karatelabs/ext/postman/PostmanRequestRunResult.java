package io.karatelabs.ext.postman;

import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostmanRequestRunResult {
   private HttpRequest a;
   private HttpResponse b;
   private boolean c = true;
   private int d = 0;
   private int e = 0;
   private final StringBuilder f = new StringBuilder();
   private Exception g;
   private final List<PostmanTestResult> h = new ArrayList<>();
   private final List<String> i = new ArrayList<>();
   private Map<String, Object> j;
   private boolean k;

   public HttpRequest a() {
      return this.a;
   }

   public void a(HttpRequest var1) {
      this.a = var1;
   }

   public HttpResponse b() {
      return this.b;
   }

   public void a(HttpResponse var1) {
      this.b = var1;
   }

   public boolean c() {
      return this.c;
   }

   public void a(boolean var1) {
      this.c = var1;
   }

   public void d() {
      this.c = false;
   }

   public String e() {
      return this.f.toString();
   }

   public void a(String var1) {
      this.f.append(var1);
   }

   public Exception f() {
      return this.g;
   }

   public void a(Exception var1) {
      this.g = var1;
      this.c = false;
   }

   public int g() {
      return this.d;
   }

   public int h() {
      return this.e;
   }

   public void i() {
      this.d++;
   }

   public void j() {
      this.e++;
   }

   public List<PostmanTestResult> k() {
      return this.h;
   }

   public List<String> l() {
      return this.i;
   }

   public Map<String, Object> m() {
      return this.j;
   }

   public void a(Map<String, Object> var1) {
      this.j = var1;
   }

   public boolean n() {
      return this.k;
   }

   public void b(boolean var1) {
      this.k = var1;
   }

   public String o() {
      if (!this.k && this.b == null) {
         return "error";
      } else if (this.g == null && !this.h.stream().anyMatch(var0 -> !var0.b() && !var0.d())) {
         return this.p() == null ? "passed" : "notchecked";
      } else {
         return "failed";
      }
   }

   public String p() {
      if (this.k) {
         return "skipped";
      } else {
         var var1 = new ArrayList<>(this.i);
         this.h.stream().filter(io.karatelabs.ext.postman.PostmanTestResult::d).forEach(var1x -> var1.add(var1x.c()));
         if (!var1.isEmpty()) {
            return String.join(
               ", ", var1.stream().map(var0 -> var0.startsWith("not supported: ") ? var0.substring(15) : var0).map(var0 -> "not supported: " + var0).toList()
            );
         } else {
            return this.h.isEmpty() ? "no tests (0 of 0)" : null;
         }
      }
   }
}
