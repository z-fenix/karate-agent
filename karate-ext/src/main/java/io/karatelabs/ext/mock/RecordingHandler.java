package io.karatelabs.ext.mock;

import io.karatelabs.core.MockHandler;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RecordingHandler implements CallLog, Function<HttpRequest, HttpResponse> {
   private volatile io.karatelabs.ext.mock.RecordingHandler.MockHandlerBinding served;
   private final List<Map<String, Object>> calls = Collections.synchronizedList(new ArrayList<>());

   public RecordingHandler(Function<HttpRequest, HttpResponse> var1) {
      this.swap(var1);
   }

   public void swap(Function<HttpRequest, HttpResponse> var1) {
      this.served = new io.karatelabs.ext.mock.RecordingHandler.MockHandlerBinding(var1, !(var1 instanceof MockHandler));
   }

   public HttpResponse apply(HttpRequest var1) {
      long var2 = System.currentTimeMillis();
      io.karatelabs.ext.mock.RecordingHandler.MockHandlerBinding var4 = this.served;
      HttpResponse var5 = var4.a().apply(var1);
      long var6 = System.currentTimeMillis();
      if (var4.b() && var5 != null) {
         var5.setHeader("Karate-Mock", "true");
      }

      this.calls.add(record(var1, var5, var2, var6));
      return var5;
   }

   @Override
   public List<Map<String, Object>> getCalls() {
      synchronized (this.calls) {
         return new ArrayList<>(this.calls);
      }
   }

   @Override
   public void clear() {
      this.calls.clear();
   }

   @Override
   public int clearAndCount() {
      synchronized (this.calls) {
         int var2 = this.calls.size();
         this.calls.clear();
         return var2;
      }
   }

   private static Map<String, Object> record(HttpRequest var0, HttpResponse var1, long var2, long var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("method", var0.getMethod());
      var6.put("path", var0.getPath());
      var6.put("url", var0.getUrlAndPath());
      var6.put("headers", var0.getHeaders());
      var6.put("body", safeBody(var0));
      var6.put("status", var1 == null ? 0 : var1.getStatus());
      var6.put("responseTime", var4 - var2);
      var6.put("t", var2);
      return var6;
   }

   private static Object safeBody(HttpRequest var0) {
      try {
         return var0.getBodyConverted();
      } catch (RuntimeException var2) {
         return var0.getBodyString();
      }
   }

   private record MockHandlerBinding(Function<HttpRequest, HttpResponse> a, boolean b) {

   }
}
