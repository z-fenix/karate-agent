package io.karatelabs.ext.robot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class BrowserDiagnosticsCollector {
   private static final int a = 50;
   private static final int b = 50;
   private static final int c = 100;
   private static final Set<String> d = Set.of("XHR", "Fetch", "Document");
   private static final int e = 400;
   private static final int f = 300;
   private static final int g = 600;
   private final List<Map<String, Object>> h = new ArrayList<>();
   private final LinkedList<Map<String, Object>> i = new LinkedList<>();
   private final LinkedList<Map<String, Object>> j = new LinkedList<>();
   private int k;
   private final Map<String, String[]> l = new ConcurrentHashMap<>();
   private final LinkedList<Map<String, Object>> m = new LinkedList<>();
   private final LinkedHashMap<String, Map<String, Object>> n = new LinkedHashMap<>();
   private final LinkedList<Map<String, Object>> o = new LinkedList<>();

   boolean a(String var1, Map<String, Object> var2) {
      switch (var1) {
         case "Network.requestWillBeSent":
            String var14 = (String)var2.get("type");
            String var17 = (String)var2.get("requestId");
            Map<java.lang.String, java.lang.String> var19 = (Map)var2.get("request");
            if (var19 == null) {
               return true;
            }

            this.a(var17, (String)var19.get("url"), (String)var19.get("method"), var14, var2.get("timestamp"));
            if (!d.contains(var14)) {
               return true;
            }

            this.l.put(var17, new String[]{(String)var19.get("url"), (String)var19.get("method")});
            break;
         case "Network.responseReceived":
            String var13 = (String)var2.get("requestId");
            Map<java.lang.String, java.lang.Object> var16 = (Map)var2.get("response");
            this.a(var13, var16, var2.get("timestamp"));
            String[] var18 = this.l.remove(var13);
            if (var16 == null) {
               return true;
            }

            Number var8 = (Number)var16.get("status");
            if (var8 == null || var8.intValue() < 400) {
               return true;
            }

            String var9 = var18 != null ? var18[0] : (String)var16.get("url");
            String var10 = var18 != null ? var18[1] : null;
            LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
            var11.put("url", var9);
            var11.put("method", var10);
            var11.put("status", var8.intValue());
            var11.put("statusText", var16.get("statusText"));
            var11.put("type", var2.get("type"));
            var11.put("time", System.currentTimeMillis());
            this.f(var11);
            break;
         case "Network.loadingFailed":
            String var12 = (String)var2.get("requestId");
            this.a(var12, var2.get("timestamp"), null, (String)var2.get("errorText"));
            String[] var15 = this.l.remove(var12);
            if (var15 == null) {
               return true;
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("url", var15[0]);
            var7.put("method", var15[1]);
            var7.put("error", var2.get("errorText"));
            var7.put("type", var2.get("type"));
            var7.put("time", System.currentTimeMillis());
            this.f(var7);
            break;
         case "Network.loadingFinished":
            this.a((String)var2.get("requestId"), var2.get("timestamp"), var2.get("encodedDataLength"), null);
            this.l.remove((String)var2.get("requestId"));
            break;
         case "Runtime.exceptionThrown":
            Map var5 = (Map)var2.get("exceptionDetails");
            if (var5 == null) {
               return true;
            }

            Map var6 = e(var5);
            this.g(var6);
            this.d(var6);
            break;
         case "Runtime.consoleAPICalled":
            this.d(c(var2));
            break;
         default:
            return false;
      }

      return true;
   }

   private static double a(Object var0) {
      return var0 instanceof Number var1 ? var1.doubleValue() * 1000.0 : -1.0;
   }

   private void a(String var1, String var2, String var3, String var4, Object var5) {
      if (var1 != null) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("url", var2);
         var6.put("method", var3);
         var6.put("type", var4);
         var6.put("startTs", a(var5));
         var6.put("startWallMs", System.currentTimeMillis());
         synchronized (this.n) {
            if (this.n.size() >= 600) {
               var var8 = this.n.keySet().iterator();
               if (var8.hasNext()) {
                  var8.next();
                  var8.remove();
               }
            }

            this.n.put(var1, var6);
         }
      }
   }

   private void a(String var1, Map<String, Object> var2, Object var3) {
      if (var1 != null && var2 != null) {
         synchronized (this.n) {
            var var5 = this.n.get(var1);
            if (var5 != null) {
               if (var2.get("status") instanceof Number var7) {
                  var5.put("status", var7.intValue());
               }

               var5.put("mime", var2.get("mimeType"));
               double var13 = var5.get("startTs") instanceof Number var9 ? var9.doubleValue() : -1.0;
               double var14 = a(var3);
               if (var13 >= 0.0 && var14 >= 0.0) {
                  var5.put("ttfbMs", Math.round(var14 - var13));
               }
            }
         }
      }
   }

   private void a(String var1, Object var2, Object var3, String var4) {
      if (var1 != null) {
         Map var5;
         synchronized (this.n) {
            var5 = this.n.remove(var1);
         }

         if (var5 != null) {
            double var14 = var5.get("startTs") instanceof Number var8 ? var8.doubleValue() : -1.0;
            double var15 = a(var2);
            if (var14 >= 0.0 && var15 >= 0.0) {
               var5.put("ms", Math.round(var15 - var14));
            }

            if (var3 instanceof Number var10) {
               var5.put("size", var10.longValue());
            }

            if (var4 != null) {
               var5.put("error", var4);
               var5.put("failed", true);
            }

            var5.remove("startTs");
            var5.remove("startWallMs");
            synchronized (this.m) {
               if (this.m.size() >= 400) {
                  this.m.removeFirst();
               }

               this.m.add(var5);
            }
         }
      }
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      String var2 = String.valueOf(var0.getOrDefault("type", "log"));
      var1.put("level", var2);
      StringBuilder var3 = new StringBuilder();
      Object var4 = var0.get("args");
      if (var4 instanceof List) {
         for (Object var7 : (List)var4) {
            if (var7 instanceof Map var8) {
               Object var9 = var8.containsKey("value") ? var8.get("value") : var8.get("description");
               if (var9 != null) {
                  if (var3.length() > 0) {
                     var3.append(' ');
                  }

                  var3.append(var9);
               }
            }
         }
      }

      var1.put("text", var3.toString());
      var var10 = b(var0.get("stackTrace"));
      if (var10 != null) {
         if (var10.get("url") != null) {
            var1.put("source", var10.get("url"));
         }

         if (var10.get("lineNumber") instanceof Number var11) {
            var1.put("line", var11.intValue());
         }
      }

      var1.put("time", System.currentTimeMillis());
      return var1;
   }

   private static Map<String, Object> b(Object var0) {
      return var0 instanceof Map var1 && var1.get("callFrames") instanceof List var2 && !var2.isEmpty() && var2.get(0) instanceof Map var3 ? var3 : null;
   }

   private void d(Map<String, Object> var1) {
      if (var1 != null) {
         synchronized (this.o) {
            if (this.o.size() >= 300) {
               this.o.removeFirst();
            }

            this.o.add(var1);
         }
      }
   }

   private static Map<String, Object> e(Map<String, Object> var0) {
      Map<java.lang.String, java.lang.String> var1 = (Map)var0.get("exception");
      boolean var2 = Boolean.TRUE.equals(var0.get("isPromiseRejection"));
      String var3 = var1 != null ? (String)var1.get("description") : null;
      String var4 = (String)var0.get("text");
      String var5 = var3 != null ? var3 : var4;
      String var6 = var5;
      if (var5 != null) {
         int var7 = var5.indexOf(10);
         if (var7 >= 0) {
            var6 = var5.substring(0, var7);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
      var12.put("level", "error");
      var12.put("type", var2 ? "rejection" : "exception");
      var12.put("text", var6);
      if (var5 != null) {
         var12.put("stack", var5);
      }

      Object var8 = var0.get("url");
      if (var8 != null) {
         var12.put("source", var8);
      }

      if (var0.get("lineNumber") instanceof Number var10) {
         var12.put("line", var10.intValue());
      }

      if (var0.get("columnNumber") instanceof Number var11) {
         var12.put("column", var11.intValue());
      }

      var12.put("time", System.currentTimeMillis());
      return var12;
   }

   private void f(Map<String, Object> var1) {
      synchronized (this.i) {
         if (this.i.size() >= 50) {
            this.i.removeFirst();
         }

         this.i.add(var1);
      }
   }

   private void g(Map<String, Object> var1) {
      synchronized (this.j) {
         if (this.j.size() >= 50) {
            this.j.removeFirst();
         }

         this.j.add(var1);
      }
   }

   void a(String var1, String var2, String var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("method", var1);
      var4.put("error", var2);
      if (var3 != null) {
         var4.put("locator", var3);
      }

      var4.put("time", System.currentTimeMillis());
      synchronized (this.h) {
         this.h.add(var4);
         if (this.h.size() > 100) {
            this.h.remove(0);
         }
      }
   }

   boolean a() {
      return this.l.isEmpty();
   }

   int b() {
      return this.l.size();
   }

   String[] c() {
      return this.l.values().stream().findFirst().orElse(null);
   }

   Map<String, Object> a(List<Map<String, Object>> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var var3 = new ArrayList<>(var1);
      synchronized (this.j) {
         var3.addAll(this.j);
      }

      var3.sort(Comparator.comparingLong(var0 -> var0.get("time") instanceof Number var2x ? var2x.longValue() : 0L));
      var2.put("console", var3);
      synchronized (this.h) {
         var2.put("bot", new ArrayList<>(this.h));
      }

      synchronized (this.i) {
         var2.put("network", new ArrayList<>(this.i));
      }

      synchronized (this.j) {
         this.k = this.j.size();
         return var2;
      }
   }

   int d() {
      synchronized (this.j) {
         return this.j.size();
      }
   }

   String a(int var1) {
      synchronized (this.j) {
         if (this.j.size() <= var1) {
            return null;
         } else {
            Object var3 = this.j.get(this.j.size() - 1).get("text");
            return var3 == null ? null : String.valueOf(var3);
         }
      }
   }

   int e() {
      synchronized (this.j) {
         return this.j.size() - this.k;
      }
   }

   Object a(Map<String, Object> var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      synchronized (this.m) {
         for (Map var5 : this.m) {
            var2.add(new LinkedHashMap(var5));
         }
      }

      long var15 = System.currentTimeMillis();
      int var16 = 0;
      synchronized (this.n) {
         for (Map var8 : this.n.values()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap(var8);
            Object var10 = var9.remove("startWallMs");
            var9.remove("startTs");
            var9.put("open", true);
            if (var10 instanceof Number var11) {
               var9.put("ageMs", var15 - var11.longValue());
            }

            var2.add(var9);
            var16++;
         }
      }

      Number var17 = var1.get("slowerThan") instanceof Number var18 ? var18 : null;
      Object var19 = var1.get("status");
      String var21 = var1.get("type") instanceof String var22 ? var22 : null;
      ArrayList<Map<String, Object>> var23 = new ArrayList<>();

      for (Map var27 : (Iterable<Map>) (Iterable<?>) (var2)) {
         if ((var21 == null || var21.equalsIgnoreCase(String.valueOf(var27.get("type"))))
            && (var19 == null || a(var27.get("status"), var19))
            && (var17 == null || h(var27) >= var17.longValue())) {
            var23.add(var27);
         }
      }

      int var26 = var1.get("limit") instanceof Number var28 ? var28.intValue() : 0;
      if (var26 > 0 && var23.size() > var26) {
         var23 = new ArrayList(var23.subList(var23.size() - var26, var23.size()));
      }

      if ("markdown".equals(var1.get("output"))) {
         return a(var23, var16);
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var29 = new LinkedHashMap();
         var29.put("requests", var23);
         var29.put("count", var23.size());
         var29.put("open", var16);
         String var30 = io.karatelabs.ext.robot.RobotGuidanceFormatter.b(var23);
         if (var30 != null) {
            var29.put("hint", var30);
         }

         return var29;
      }
   }

   private static long h(Map<String, Object> var0) {
      if (var0.get("ms") instanceof Number var3) {
         return var3.longValue();
      } else {
         return Boolean.TRUE.equals(var0.get("open")) && var0.get("ageMs") instanceof Number var1 ? var1.longValue() : 0L;
      }
   }

   private static boolean a(Object var0, Object var1) {
      if (var0 instanceof Number var2) {
         int var3 = var2.intValue();
         if (var1 instanceof Number var7) {
            return var3 == var7.intValue();
         } else {
            String var4 = String.valueOf(var1).trim();

            try {
               if (var4.startsWith(">=")) {
                  return var3 >= Integer.parseInt(var4.substring(2).trim());
               } else if (var4.startsWith("<=")) {
                  return var3 <= Integer.parseInt(var4.substring(2).trim());
               } else if (var4.startsWith(">")) {
                  return var3 > Integer.parseInt(var4.substring(1).trim());
               } else {
                  return var4.startsWith("<") ? var3 < Integer.parseInt(var4.substring(1).trim()) : var3 == Integer.parseInt(var4);
               }
            } catch (NumberFormatException var6) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static String a(List<Map<String, Object>> var0, int var1) {
      StringBuilder var2 = new StringBuilder("| ms | status | type | url |\n|---:|---:|---|---|\n");

      for (Map var4 : var0) {
         Object var5 = var4.get("ms");
         String var6 = var5 != null ? String.valueOf(var5) : (Boolean.TRUE.equals(var4.get("open")) ? "open " + var4.getOrDefault("ageMs", "?") : "?");
         var2.append("| ")
            .append(var6)
            .append(" | ")
            .append(var4.getOrDefault("status", var4.containsKey("error") ? "ERR" : "-"))
            .append(" | ")
            .append(var4.getOrDefault("type", "-"))
            .append(" | ")
            .append(String.valueOf(var4.get("url")))
            .append(" |\n");
      }

      var2.append("\n_").append(var0.size()).append(" request(s), ").append(var1).append(" still open._");
      return var2.toString();
   }

   Object b(Map<String, Object> var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      synchronized (this.o) {
         for (Map var5 : this.o) {
            var2.add(new LinkedHashMap(var5));
         }
      }

      String var9 = var1.get("level") instanceof String var10 ? var10 : null;
      if (var9 != null) {
         var2.removeIf(var1x -> !var9.equalsIgnoreCase(String.valueOf(var1x.get("level"))));
      }

      int var11 = var1.get("limit") instanceof Number var13 ? var13.intValue() : 0;
      if (var11 > 0 && var2.size() > var11) {
         var2 = new ArrayList(var2.subList(var2.size() - var11, var2.size()));
      }

      if (!"markdown".equals(var1.get("output"))) {
         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
         var15.put("messages", var2);
         var15.put("count", var2.size());
         return var15;
      } else {
         StringBuilder var14 = new StringBuilder("| level | text | source |\n|---|---|---|\n");

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (var2)) {
            var14.append("| ")
               .append(var7.getOrDefault("level", "log"))
               .append(" | ")
               .append(String.valueOf(var7.getOrDefault("text", "")).replace("|", "\\|").replace("\n", " "))
               .append(" | ")
               .append(var7.getOrDefault("source", ""))
               .append(" |\n");
         }

         return var14.toString();
      }
   }

   void f() {
      synchronized (this.m) {
         this.m.clear();
      }

      synchronized (this.n) {
         this.n.clear();
      }

      synchronized (this.o) {
         this.o.clear();
      }
   }
}
