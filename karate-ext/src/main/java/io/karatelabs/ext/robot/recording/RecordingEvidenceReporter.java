package io.karatelabs.ext.robot.recording;

import io.karatelabs.common.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class RecordingEvidenceReporter {
   public static final Map<String, String> a;
   private static final List<String> j = List.of("/api/", "/ui/", "/novnc", "/websockify");
   private static final Set<String> k = Set.of("Image", "Font", "Stylesheet", "Media", "Script", "Manifest", "TextTrack");
   private static final Pattern l = Pattern.compile(
      "(?i).*\\.(png|jpe?g|gif|svg|ico|webp|avif|bmp|woff2?|ttf|eot|otf|css|map|m?js|mp3|mp4|webm|wav|ogg|webmanifest)$"
   );
   static final Set<String> b = Set.of("GET", "HEAD");
   static final Set<String> c = Set.of(
      "user-agent",
      "referer",
      "origin",
      "host",
      "connection",
      "content-length",
      "cache-control",
      "pragma",
      "if-none-match",
      "if-modified-since",
      "if-match",
      "if-unmodified-since",
      "priority",
      "upgrade-insecure-requests",
      "dnt",
      "te",
      "keep-alive"
   );
   static final Pattern d = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
   static final Pattern e = Pattern.compile("(?i)(?=[^/]*\\d)[0-9a-f]{8,}");
   static final Pattern f = Pattern.compile("\\d+");
   private static final String m = "([a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*|\\[[0-9a-f:.]+])";
   private static final Pattern n = Pattern.compile("(?i)([a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*|\\[[0-9a-f:.]+])");
   private static final Pattern o = Pattern.compile(
      "(?i)https?://([a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*|\\[[0-9a-f:.]+])(:\\d{1,5})?/?"
   );
   static final Pattern g = Pattern.compile("‹(?:masked|secret):[^›]+›");
   private static final Set<String> p = Set.of(
      "co.uk",
      "org.uk",
      "ac.uk",
      "gov.uk",
      "com.au",
      "net.au",
      "org.au",
      "co.nz",
      "co.jp",
      "co.in",
      "co.za",
      "com.br",
      "com.cn",
      "com.mx",
      "com.sg",
      "com.tr",
      "co.kr"
   );
   static final int h = 4;
   private static final int q = 160;
   private static final Set<String> r = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
   public static final String i = "a move is {id, to: 'kept'|'left', by: 'tester'|'chat', reason?}";

   private RecordingEvidenceReporter() {
   }

   public static Map<String, Object> a(TrafficLog var0, String var1, Collection<String> var2, List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ExchangeMove> var3, Collection<String> var4) {
      String var5 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var1).c();
      HashSet<String> var6 = new HashSet<>();

      for (String var8 : var2) {
         if (!n.matcher(var8).matches() && !o.matcher(var8).matches()) {
            return a("a hosts entry is a bare host ('api.acme.com') or an origin ('https://api.acme.com:8443'), not '" + var8 + "'", "BAD_REQUEST", 400);
         }

         var6.add(var8.contains("://") ? io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var8).c() : var8.toLowerCase(Locale.ROOT));
      }

      HashSet<java.lang.String> var12 = new HashSet();

      for (String var9 : var4) {
         var12.add(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var9).c());
      }

      ArrayList<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var14 = new ArrayList();

      for (Map var10 : var0.e()) {
         var14.add(new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange(var14.size(), var10));
      }

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var19 : (Iterable<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange>) (Iterable<?>) (var14)) {
         a(var19, var5, var6, var12);
         if (!var19.a()) {
            var19.l = a(var19);
         }
      }

      a(var14);
      b(var14);

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var20 : (Iterable<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange>) (Iterable<?>) (var14)) {
         if (var20.d == null && var20.d() == null) {
            var20.d = "no-response";
            var20.e = io.karatelabs.ext.robot.recording.RecordedExchange.a(var20.b.get("error"));
         }
      }

      HashMap<java.lang.String, java.lang.Integer> var18 = new HashMap();

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var11 : (Iterable<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange>) (Iterable<?>) (var14)) {
         if (var11.d == null && !var11.h() && var18.merge(var11.l, 1, Integer::sum) > 1) {
            var11.i = true;
         }
      }

      Map<java.lang.String, java.lang.Object> var22 = a(var14, var3);
      if (var22 != null) {
         return var22;
      } else {
         c(var14);
         return a(var0, var5, var14);
      }
   }

   private static void a(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var0, String var1, Set<String> var2, Set<String> var3) {
      if (var0.a()) {
         var0.d = "lost";
         var0.e = io.karatelabs.ext.robot.recording.RecordedExchange.a(var0.b.get("lost"));
      } else {
         io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var4 = var0.c;
         if (!var4.b()) {
            var0.d = "not-http";
         } else {
            String var5 = var0.f();
            if (var3.contains(var4.c()) && j.stream().anyMatch(var4.g::startsWith)) {
               var0.d = "console";
            } else if (!"Preflight".equals(var5) && (!"OPTIONS".equals(var0.e()) || a(var0.c(), "access-control-request-method") == null)) {
               String var6 = var4.a();
               if (!a(var6).equals(a(var1)) && !var2.contains(var6) && !var2.contains(var4.e)) {
                  var0.d = "other-site";
                  String var12 = c(var4.e);
                  if (var12 != null && var12.equals(c(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var1).e))) {
                     var0.e = "same company domain; hosts: ['" + var4.e + "'] keeps it";
                  }

                  var0.f = io.karatelabs.ext.robot.recording.AnalyticsVendorClassifier.a(var4.e);
               } else {
                  boolean var7 = "Document".equals(var5) && "sub".equals(var0.b.get("frame"));
                  if (!var7
                     && !k.contains(var5)
                     && ("Document".equals(var5) || "WebSocket".equals(var5) || "EventSource".equals(var5) || !l.matcher(var4.g).matches() && !c(var0))) {
                     if ("Document".equals(var5) && "GET".equals(var0.e())) {
                        var0.d = "page-load";
                     } else if (!"WebSocket".equals(var5)
                        && !"EventSource".equals(var5)
                        && !f(var0.c().get("body"))
                        && !b(a(var0.c(), "accept"), "text/event-stream")) {
                        String var8 = a(var0.c(), "content-type");
                        Object var9 = var0.b.get("bodyState") instanceof Map var10 ? var10.get("request") : null;
                        if (b(var8, "multipart/") || b(var8, "application/octet-stream") || "binary".equals(var9)) {
                           var0.d = "upload";
                        }
                     } else {
                        var0.d = "stream";
                     }
                  } else {
                     var0.d = "static";
                  }
               }
            } else {
               var0.d = "preflight";
            }
         }
      }
   }

   static String a(String var0) {
      io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var1 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var0);
      return r.contains(var1.e) ? new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl(var1.d, "localhost", var1.f, "").c() : String.valueOf(var0);
   }

   private static void a(List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var0) {
      LinkedHashMap<java.lang.String, java.util.ArrayList> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var3 : var0) {
         if (var3.d == null && var3.h()) {
            var1.computeIfAbsent(var3.l, var0x -> new ArrayList<>()).add(var3);
         }
      }

      for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         List var4 = (List)var13.getValue();
         HashSet<java.lang.Object> var5 = new HashSet();
         boolean var6 = false;

         for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var8 : (Iterable<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange>) (Iterable<?>) (var4)) {
            var6 |= !var5.add(var8.g());
         }

         if (var4.size() >= 3 && var6) {
            String var14 = "g" + io.karatelabs.ext.common.Sha256.a((String)var13.getKey()).substring(0, 8);
            String var15 = null;

            for (int var9 = 0; var9 < var4.size(); var9++) {
               io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var10 = (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange)var4.get(var9);
               var10.d = "polling";
               var10.g = var14;
               String var11 = var10.i();
               var10.h = var9 == var4.size() - 1 || var15 != null && !var15.equals(var11);
               var15 = var11;
            }
         }
      }
   }

   private static void b(List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var0) {
      HashSet<java.lang.String> var1 = new HashSet();

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var3 : var0) {
         if (var3.d == null && var3.h() && !var1.add(var3.l + "|" + var3.g() + "|" + var3.i())) {
            var3.d = "duplicate";
         }
      }
   }

   private static Map<String, Object> a(List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var0, List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ExchangeMove> var1) {
      HashMap<java.lang.String, io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var2 = new HashMap();

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var4 : var0) {
         var2.put(var4.b(), var4);
      }

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ExchangeMove var8 : var1) {
         io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var5 = (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange)var2.get(var8.a());
         if (var5 == null) {
            return a("no exchange '" + var8.a() + "' in this recording", "NOT_FOUND", 404);
         }

         if (!"kept".equals(var8.b()) && !"left".equals(var8.b())) {
            return a(a("to", var8.b()), "BAD_REQUEST", 400);
         }

         if (!"tester".equals(var8.c()) && !"chat".equals(var8.c())) {
            return a(a("by", var8.c()), "BAD_REQUEST", 400);
         }

         if ("chat".equals(var8.c()) && (var8.d() == null || var8.d().isBlank())) {
            return a("the chat moves a row only with a reason: {id, to, by: 'chat', reason}", "BAD_REQUEST", 400);
         }

         if (var5.a() && "kept".equals(var8.b())) {
            return a("exchange '" + var8.a() + "' was lost by capture; there is nothing to keep", "BAD_REQUEST", 400);
         }

         LinkedHashMap<String, Object> var6 = new LinkedHashMap();
         var6.put("by", var8.c());
         if (var8.d() != null && !var8.d().isBlank()) {
            var6.put("reason", var8.d());
         }

         if ("kept".equals(var8.b())) {
            if (var5.d != null) {
               var6.put("from", var5.d);
            }

            var5.d = null;
            var5.e = null;
         } else if (var5.d == null) {
            var5.d = "moved";
         }

         var5.k = var6;
      }

      return null;
   }

   private static void c(List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var0) {
      var var1 = var0.stream().filter(var0x -> var0x.d == null).toList();

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var3 : var0) {
         if ("polling".equals(var3.d) && var3.h && var3.k == null && var3.d() != null) {
            HashSet<java.lang.String> var4 = new HashSet();
            a(var3.d().get("body"), var4);
            String var5 = a(var3.d(), "location");
            if (var5 != null) {
               var4.add(var5);
            }

            var4.removeIf(var0x -> var0x.length() < 4);
            if (!var4.isEmpty()) {
               for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var7 : (Iterable<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange>) (Iterable<?>) (var1)) {
                  if (var7.a > var3.a && a(var7, var4)) {
                     var3.d = null;
                     var3.j = var7.b();
                     break;
                  }
               }
            }
         }
      }
   }

   private static boolean a(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var0, Set<String> var1) {
      return io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var0.c(), var0.c).stream().anyMatch(var1x -> var1.contains(var1x.d()));
   }

   private static Map<String, Object> a(TrafficLog var0, String var1, List<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange> var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var5 = new LinkedHashMap();
      long var6 = 0L;
      long var8 = 0L;

      for (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var11 : var2) {
         if (var11.a()) {
            var8 += g(var11.b.get("starts"));
         } else {
            var6 += g(var11.b.get("hops"));
         }

         (var11.d == null ? var3 : var4).add(b(var11));
         if (var11.d != null) {
            var5.merge(var11.d, 1, Integer::sum);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var14 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.String> var15 = new LinkedHashMap();

      for (String var13 : a.keySet()) {
         if (var5.containsKey(var13)) {
            var14.put(var13, (Integer)var5.get(var13));
            var15.put(var13, a.get(var13));
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
      var16.put("exchanges", var2.size());
      var16.put("kept", var3.size());
      var16.put("left", var4.size());
      var16.put("started", var0.f().get("started"));
      var16.put("hops", var6);
      var16.put("lost", var8);
      var16.put("byReason", var14);
      LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
      var17.put("app", var1);
      var17.put("kept", var3);
      var17.put("left", var4);
      var17.put("actions", var0.b());
      var17.put("reasons", var15);
      var17.put("counts", var16);
      var17.put("capture", var0.c());
      return var17;
   }

   private static Map<String, Object> b(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", var0.b());
      if (var0.a()) {
         var1.put("starts", var0.b.get("starts"));
      } else {
         var1.put("method", var0.e());
         var1.put("host", var0.c.b() ? var0.c.e : null);
         var1.put("path", var0.c.b() ? b(var0.c.g) : var0.c.d + ":");
         a(var1, "query", c(var0.c().get("params")));
         var var2 = var0.d();
         var1.put("status", var2 == null ? null : var2.get("status"));
         var1.put("type", var0.f());
         var1.put("after", var0.g());
         if (g(var0.b.get("hops")) > 1L) {
            var1.put("hops", var0.b.get("hops"));
         }
      }

      a(var1, "reason", var0.d);
      a(var1, "detail", var0.e);
      a(var1, "label", var0.f);
      a(var1, "group", var0.g);
      if (var0.h) {
         var1.put("representative", true);
      }

      a(var1, "consumedBy", var0.j);
      if (var0.i) {
         var1.put("repeated", true);
      }

      a(var1, "moved", var0.k);
      return var1;
   }

   private static String c(Object var0) {
      if (var0 instanceof Map var1 && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder();
         var1.forEach((var1x, var2x) -> a(var2, var1x, var2x));
         return var2.length() <= 160 ? var2.toString() : var2.substring(0, 160) + "…";
      } else {
         return null;
      }
   }

   private static void a(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   private static String a(String var0, String var1) {
      return "a move is {id, to: 'kept'|'left', by: 'tester'|'chat', reason?}: " + var0 + (var1 == null ? " is missing" : " is '" + var1 + "'");
   }

   private static Map<String, Object> a(String var0, String var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("error", var0);
      var3.put("code", var1);
      var3.put("status", var2);
      return var3;
   }

   static String a(Map<String, Object> var0) {
      io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var1 = new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange(0, var0);
      return !var1.a() && var1.c.b() ? a(var1) : null;
   }

   static String a(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var0) {
      TreeMap<java.lang.String, java.lang.Object> var1 = new TreeMap();
      var1.put("origin", var0.c.c());
      var1.put("method", var0.e());
      var1.put("path", var0.c.g);
      TreeMap<java.lang.String, java.lang.Object> var2 = new TreeMap();
      if (var0.c().get("params") instanceof Map var3) {
         var3.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), var2x));
      }

      var1.put("query", var2);
      TreeMap<java.lang.String, java.lang.Object> var6 = new TreeMap();
      if (var0.c().get("headers") instanceof Map var7) {
         var7.forEach((var1x, var2x) -> {
            String var3x = String.valueOf(var1x).toLowerCase(Locale.ROOT);
            if (!c.contains(var3x) && !var3x.startsWith("accept") && !var3x.startsWith("sec-") && !var3x.startsWith(":")) {
               var6.put(var3x, var2x);
            }
         });
      }

      var1.put("headers", var6);
      var1.put("body", d(var0.c().get("body")));
      return a(var1);
   }

   private static Object d(Object var0) {
      if (var0 instanceof List var1 && !var1.isEmpty() && var1.stream().allMatch(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter::e)) {
         return var1.stream().map(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter::d).toList();
      } else if (e(var0)) {
         Map<java.lang.String, Object> var3 = (Map)var0;
         TreeMap<java.lang.String, java.lang.Object> var2 = new TreeMap();
         var2.put("operationName", var3.get("operationName"));
         var2.put("variables", var3.get("variables"));
         if (var3.get("operationName") == null) {
            var2.put("query", var3.get("query"));
         }

         return var2;
      } else {
         return var0;
      }
   }

   private static boolean e(Object var0) {
      return var0 instanceof Map var1 && var1.get("query") instanceof String;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static boolean f(Object var0) {
      for (Object var6 : var0 instanceof List ? (List)var0 : var0 == null ? List.of("") : List.of(var0)) {
         if (var6 instanceof Map var3 && var3.get("query") instanceof String var4 && var4.stripLeading().startsWith("subscription")) {
            return true;
         }
      }

      return false;
   }

   static String a(Object var0) {
      StringBuilder var1 = new StringBuilder();
      a(var0, var1);
      return var1.toString();
   }

   private static void a(Object var0, StringBuilder var1) {
      if (var0 instanceof Map var2) {
         TreeMap<java.lang.String, java.lang.Object> var5 = new TreeMap();
         var2.forEach((var1x, var2x) -> var5.put(String.valueOf(var1x), var2x));
         var1.append('{');
         var5.forEach((var1x, var2x) -> {
            var1.append(StringUtils.formatJsonString(var1x)).append(':');
            a(var2x, var1);
            var1.append(',');
         });
         var1.append('}');
      } else if (var0 instanceof List var3) {
         var1.append('[');

         for (Object var6 : var3) {
            a(var6, var1);
            var1.append(',');
         }

         var1.append(']');
      } else if (var0 instanceof String var4) {
         var1.append(StringUtils.formatJsonString(var4));
      } else {
         var1.append(b(var0));
      }
   }

   static String b(Object var0) {
      if (var0 instanceof Number var1) {
         try {
            return new BigDecimal(var1.toString()).stripTrailingZeros().toPlainString();
         } catch (NumberFormatException var3) {
            return var1.toString();
         }
      } else {
         return String.valueOf(var0);
      }
   }

   private static void a(Object var0, Set<String> var1) {
      if (var0 instanceof Map var2) {
         var2.values().forEach(var1x -> a(var1x, var1));
      } else if (var0 instanceof List var3) {
         var3.forEach(var1x -> a(var1x, var1));
      } else if (var0 instanceof String || var0 instanceof Number) {
         var1.add(b(var0));
      }
   }

   public static String b(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String[] var1 = var0.split("/", -1);

         for (int var2 = 0; var2 < var1.length; var2++) {
            String var3 = io.karatelabs.ext.robot.recording.PageEvidenceExtractor.d(var1[var2]);
            if (!var3.isEmpty() && (f.matcher(var3).matches() || d.matcher(var3).matches() || e.matcher(var3).matches() || g.matcher(var3).matches())) {
               var1[var2] = "{}";
            }
         }

         return String.join("/", var1);
      }
   }

   static String c(String var0) {
      if (var0 != null && !var0.isEmpty() && !var0.startsWith("[") && !var0.matches("[\\d.]+")) {
         String[] var1 = var0.split("\\.");
         if (var1.length <= 2) {
            return var0;
         } else {
            String var2 = var1[var1.length - 2] + "." + var1[var1.length - 1];
            return p.contains(var2) ? var1[var1.length - 3] + "." + var2 : var2;
         }
      } else {
         return var0;
      }
   }

   private static boolean c(io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ClassifiedExchange var0) {
      if (!((var0.b.get("mime") instanceof Map var2 ? var2.get("response") : null) instanceof String var4)) {
         return false;
      } else {
         String var5 = var4.toLowerCase(Locale.ROOT);
         return var5.startsWith("image/")
            || var5.startsWith("font/")
            || var5.startsWith("audio/")
            || var5.startsWith("video/")
            || var5.equals("text/css")
            || var5.contains("javascript");
      }
   }

   private static String a(Map<String, Object> var0, String var1) {
      if (var0 != null && var0.get("headers") instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (String.valueOf(var4.getKey()).equalsIgnoreCase(var1)) {
               Object var5 = var4.getValue();
               return var5 instanceof List var6 ? (var6.isEmpty() ? "" : String.valueOf(var6.get(0))) : String.valueOf(var5);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean b(String var0, String var1) {
      return var0 != null && var0.toLowerCase(Locale.ROOT).contains(var1);
   }

   private static long g(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : 0L;
   }

   static {
      LinkedHashMap<java.lang.String, java.lang.String> var0 = new LinkedHashMap();
      var0.put("lost", "lost by capture (not checked)");
      var0.put("not-http", "not a web call");
      var0.put("console", "the console itself");
      var0.put("preflight", "a browser permission check");
      var0.put("other-site", "another site");
      var0.put("static", "a page file (picture, style, script)");
      var0.put("page-load", "a page you opened");
      var0.put("stream", "a live stream (not supported)");
      var0.put("upload", "a file upload (not supported)");
      var0.put("polling", "repeated in the background");
      var0.put("duplicate", "the same call again");
      var0.put("no-response", "never answered");
      var0.put("moved", "left out by choice");
      a = Collections.unmodifiableMap(var0);
   }

   public record ExchangeMove(String a, String b, String c, String d) {

   }

   private static final class ClassifiedExchange {
      final int a;
      final Map<String, Object> b;
      final io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl c;
      String d;
      String e;
      String f;
      String g;
      boolean h;
      boolean i;
      String j;
      Map<String, Object> k;
      String l;

      ClassifiedExchange(int var1, Map<String, Object> var2) {
         this.a = var1;
         this.b = var2;
         this.c = this.a() ? null : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(io.karatelabs.ext.robot.recording.RecordedExchange.a(this.c().get("url")));
      }

      boolean a() {
         return this.b.containsKey("lost");
      }

      String b() {
         return String.valueOf(this.b.get("id"));
      }

      Map<String, Object> c() {
         return this.b.get("request") instanceof Map var1 ? var1 : Map.of();
      }

      Map<String, Object> d() {
         return this.b.get("response") instanceof Map var1 ? var1 : null;
      }

      String e() {
         Object var1 = this.c().get("method");
         return var1 == null ? "" : String.valueOf(var1).toUpperCase(Locale.ROOT);
      }

      String f() {
         return io.karatelabs.ext.robot.recording.RecordedExchange.a(this.b.get("type"));
      }

      Object g() {
         return this.b.get("after");
      }

      boolean h() {
         return io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b.contains(this.e());
      }

      String i() {
         var var1 = this.d();
         if (var1 == null) {
            return "none";
         } else {
            Object var2 = this.b.get("bodyState") instanceof Map var3 ? var3.get("response") : null;
            return var1.get("status") + "|" + var2 + "|" + io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.a(var1.get("body"));
         }
      }
   }

   public record ParsedUrl(String d, String e, int f, String g) {


      public static io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl a(String var0) {
         if (var0 == null) {
            return new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl("", "", -1, "");
         } else {
            int var1 = var0.indexOf(58);
            String var2 = var1 < 0 ? "" : var0.substring(0, var1).toLowerCase(Locale.ROOT);
            if (var1 >= 0 && var0.startsWith("://", var1)) {
               String var3 = var0.substring(var1 + 3);
               int var4 = var3.indexOf(47);
               String var5 = var4 < 0 ? var3 : var3.substring(0, var4);
               String var6 = var4 < 0 ? "/" : var3.substring(var4);
               int var7 = var6.indexOf(63);
               var6 = var7 < 0 ? var6 : var6.substring(0, var7);
               var7 = var6.indexOf(35);
               var6 = var7 < 0 ? var6 : var6.substring(0, var7);
               int var8 = var5.lastIndexOf(64);
               var5 = var8 < 0 ? var5 : var5.substring(var8 + 1);
               String var9 = var5;
               int var10 = -1;
               int var11 = var5.lastIndexOf(58);
               if (var11 > var5.lastIndexOf(93)) {
                  var9 = var5.substring(0, var11);

                  try {
                     var10 = Integer.parseInt(var5.substring(var11 + 1));
                  } catch (NumberFormatException var15) {
                     var10 = -1;
                  }
               }
               short var12 = switch (var2) {
                  case "http", "ws" -> 80;
                  case "https", "wss" -> 443;
                  default -> -1;
               };
               return new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl(var2, var9.toLowerCase(Locale.ROOT), var10 == var12 ? -1 : var10, var6);
            } else {
               return new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl(var2, "", -1, var1 < 0 ? var0 : var0.substring(var1 + 1));
            }
         }
      }

      String a() {
         String var1 = this.c();
         return var1 != null && this.d.startsWith("ws") ? "http" + var1.substring(2) : var1;
      }

      boolean b() {
         return Set.of("http", "https", "ws", "wss").contains(this.d) && !this.e.isEmpty();
      }

      public String c() {
         return this.e.isEmpty() ? null : this.d + "://" + this.e + (this.f < 0 ? "" : ":" + this.f);
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public int f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }
   }

   private static void a(StringBuilder var0, String var1, Object var2) {
      var0.append(StringUtils.formatJsonString(var1)).append(':');
      a(var2, var0);
      var0.append(',');
   }

   private static void a(Map var0, Object var1, Object var2) {
      var0.put(String.valueOf(var1), var2);
   }

   private static void b(Map var0, Object var1, Object var2) {
      String var3 = String.valueOf(var1).toLowerCase(Locale.ROOT);
      if (!c.contains(var3) && !var3.startsWith("accept") && !var3.startsWith("sec-") && !var3.startsWith(":")) {
         var0.put(var3, var2);
      }
   }

   private static void c(Map var0, Object var1, Object var2) {
      var0.put(String.valueOf(var1), var2);
   }

   private static void a(StringBuilder var0, Object var1, Object var2) {
      for (Object var4 : var2 instanceof List ? (List)var2 : var2 == null ? List.of("") : List.of(var2)) {
         var0.append(var0.isEmpty() ? "" : "&").append(var1).append('=').append(var4 == null ? "" : b(var4));
      }
   }
}
