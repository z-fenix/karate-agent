package io.karatelabs.ext.robot.recording;

import io.karatelabs.common.Json;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PageEvidenceExtractor {
   private static final Pattern a = Pattern.compile("(?i)<meta\\b[^>]*>");
   private static final Pattern b = Pattern.compile("(?i)\\bname\\s*=\\s*[\"']([^\"']*)[\"']");
   private static final Pattern c = Pattern.compile("(?i)\\bcontent\\s*=\\s*[\"']([^\"']*)[\"']");
   private static final Pattern d = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
   private static final Pattern e = Pattern.compile("[\\s;,=]+");
   private static final Pattern f = Pattern.compile("(?i).*(sess|auth|jwt|xsrf|csrf).*|(?i).*(^|[^a-z])s?sid([^a-z]|$).*");

   private PageEvidenceExtractor() {
   }

   static List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence> a(Map<String, Object> var0, io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var1) {
      ArrayList<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence> var2 = new ArrayList();
      String[] var3 = var1.g().split("/");
      int var4 = 0;

      for (String var8 : var3) {
         if (!var8.isEmpty()) {
            String var9 = d(var8);
            var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("path", String.valueOf(var4), "path[" + var4 + "]", var9, !var9.equals(var8), "text", null));
            var4++;
         }
      }

      if (var0.get("params") instanceof Map var14) {
         var14.forEach((var1x, var2x) -> {
            for (Object var5 : var2x instanceof List ? (List)var2x : List.of(var2x == null ? "" : var2x)) {
               var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("query", String.valueOf(var1x), "query" + g(String.valueOf(var1x)), io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var5), false, "text", null));
            }
         });
      }

      if (var0.get("headers") instanceof Map var15) {
         var15.forEach((var1x, var2x) -> {
            String var3x = String.valueOf(var1x);

            for (Object var5 : var2x instanceof List ? (List)var2x : List.of(var2x)) {
               String var6 = String.valueOf(var5);
               if (var3x.equalsIgnoreCase("cookie")) {
                  for (String var17 : var6.split(";")) {
                     int var18 = var17.indexOf(61);
                     if (var18 > 0) {
                        String var12x = var17.substring(0, var18).trim();
                        var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("cookie", var12x, "cookie" + g(var12x), var17.substring(var18 + 1).trim(), false, "text", var6));
                     }
                  }
               } else {
                  String[] var7 = e.split(var6.trim());

                  for (String var11x : var7) {
                     if (!var11x.isEmpty()) {
                        var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("header", var3x, "header" + g(var3x), var11x, false, "text", var6));
                     }
                  }

                  if (var7.length > 1) {
                     var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("header", var3x, "header" + g(var3x), var6, false, "text", var6));
                  }
               }
            }
         });
      }

      Object var16 = var0.get("body");
      if (var16 instanceof String var19 && c(var0).contains("application/x-www-form-urlencoded")) {
         for (String var11 : var19.split("&")) {
            int var12 = var11.indexOf(61);
            if (var12 >= 0) {
               String var13 = e(var11.substring(0, var12));
               var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("form", var13, "form" + g(var13), e(var11.substring(var12 + 1)), false, "text", null));
            }
         }
      } else if (var16 instanceof String var20) {
         var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("body", "", "body", var20, false, "string", null));
      } else {
         a(var16, "body", "", (var1x, var2x, var3x, var4x) -> var2.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence("body", var2x, var1x, var3x, false, var4x, null)));
      }

      return var2;
   }

   static List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> a(Map<String, Object> var0) {
      ArrayList<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> var1 = new ArrayList();
      Map<java.lang.String, java.lang.Object> var2 = var0.get("response") instanceof Map var3 ? var3 : null;
      if (var2 == null) {
         return var1;
      } else {
         Object var10 = var2.get("body");
         a(var10, "response", "", (var1x, var2x, var3x, var4) -> var1.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence("leaf", var1x, a(var2x), var3x, var4)));
         if (var10 instanceof String var11 && var11.toLowerCase(Locale.ROOT).contains("<meta")) {
            Matcher var5 = a.matcher(var11);

            while (var5.find()) {
               Matcher var6 = b.matcher(var5.group());
               Matcher var7 = c.matcher(var5.group());
               if (var6.find() && var7.find()) {
                  String var8 = var6.group(1).toLowerCase(Locale.ROOT);
                  if ((var8.contains("csrf") || var8.contains("xsrf")) && !var7.group(1).isEmpty()) {
                     var1.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence("meta", "meta " + var6.group(1), var6.group(1), var7.group(1), "string"));
                  }
               }
            }
         }

         if (var2.get("headers") instanceof Map var12) {
            List var15 = io.karatelabs.ext.robot.recording.RecordedExchange.b(var12, "location");
            if (!var15.isEmpty()) {
               var1.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence("location", "responseHeaders['Location'][0]", "Location", (String)var15.get(0), "text"));
            }

            for (String var20 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.robot.recording.RecordedExchange.b(var12, "set-cookie"))) {
               a(var20, var1);
            }
         }

         Object var16 = var0.get("redirects");
         if (var16 instanceof List) {
            for (Object var19 : (List)var16) {
               if (var19 instanceof Map var21 && var21.get("setCookie") instanceof List var22) {
                  var22.forEach(var1x -> a(String.valueOf(var1x), var1));
               }
            }
         }

         return var1;
      }
   }

   static List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> b(Map<String, Object> var0) {
      return a(var0).stream().filter(var0x -> "cookie".equals(var0x.a())).toList();
   }

   private static void a(String var0, List<io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence> var1) {
      String var2 = var0.split(";", 2)[0];
      int var3 = var2.indexOf(61);
      if (var3 > 0) {
         String var4 = var2.substring(0, var3).trim();
         var1.add(new io.karatelabs.ext.robot.recording.PageEvidenceExtractor.ResponseValueEvidence("cookie", "responseCookies['" + var4 + "'].value", var4, var2.substring(var3 + 1).trim(), "text"));
      }
   }

   static void a(Object var0, String var1, String var2, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.EvidenceLeafConsumer var3) {
      if (var0 instanceof Map var4) {
         var4.forEach((var3x, var4x) -> {
            String var5x = String.valueOf(var3x);
            a(var4x, var1 + b(var5x), var2.isEmpty() ? var5x : var2 + "." + var5x, var3);
         });
      } else if (var0 instanceof List var5) {
         for (int var9 = 0; var9 < var5.size(); var9++) {
            a(var5.get(var9), var1 + "[" + var9 + "]", var2 + "[" + var9 + "]", var3);
         }
      } else if (var0 instanceof String var6) {
         var3.leaf(var1, var2, var6, "string");
         Object var10 = f(var6);
         if (var10 != null) {
            a(var10, "karate.fromString(" + var1 + ")", var2, var3);
         }
      } else if (var0 instanceof Number var7) {
         var3.leaf(var1, var2, io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var7), "number");
      } else if (var0 instanceof Boolean var8) {
         var3.leaf(var1, var2, var8.toString(), "boolean");
      }
   }

   private static Object f(String var0) {
      String var1 = var0.trim();
      if (var1.length() >= 2 && (var1.startsWith("{") && var1.endsWith("}") || var1.startsWith("[") && var1.endsWith("]"))) {
         try {
            Object var2 = Json.parseStrict(var1, true);
            return !(var2 instanceof Map) && !(var2 instanceof List) ? null : var2;
         } catch (RuntimeException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String a(String var0) {
      String var1 = var0.replaceAll("\\[\\d+]", "");
      int var2 = var1.lastIndexOf(46);
      return var2 < 0 ? var1 : var1.substring(var2 + 1);
   }

   static String b(String var0) {
      return d.matcher(var0).matches() ? "." + var0 : "[" + c(var0) + "]";
   }

   private static String g(String var0) {
      return b(var0);
   }

   static String c(String var0) {
      StringBuilder var1 = new StringBuilder("'");

      for (char var5 : var0.toCharArray()) {
         switch (var5) {
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '\'':
               var1.append("\\'");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            case '\u2028':
            case '\u2029':
               var1.append(String.format("\\u%04x", Integer.valueOf(var5)));
               break;
            default:
               var1.append(var5);
         }
      }

      return var1.append('\'').toString();
   }

   static boolean a(String var0, boolean var1) {
      String var2 = var0.toLowerCase(Locale.ROOT);
      return var2.startsWith("if-")
         ? var1
         : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.c.contains(var2)
            || var2.startsWith("sec-")
            || var2.startsWith(":")
            || var2.equals("accept-encoding")
            || var2.equals("accept-language")
            || var2.equals("cookie");
   }

   static boolean a(String var0, String var1) {
      return io.karatelabs.ext.agent.llm.SecretRedactor.isSensitiveName(var0) || f.matcher(var0).matches() || var1.startsWith("‹secret:");
   }

   static boolean a(io.karatelabs.ext.robot.recording.PageEvidenceExtractor.PageValueEvidence var0, String var1) {
      boolean var2 = "header".equals(var0.a()) || "body".equals(var0.a()) && "string".equals(var0.f());
      return var2 && var0.d().indexOf(37) >= 0 && !var0.d().equals(var1) && d(var0.d()).equals(var1);
   }

   static String c(Map<String, Object> var0) {
      if (var0.get("headers") instanceof Map var1) {
         for (Entry var3 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if ("content-type".equalsIgnoreCase(String.valueOf(var3.getKey()))) {
               Object var4 = var3.getValue();
               return String.valueOf(var4 instanceof List var5 ? (var5.isEmpty() ? "" : var5.get(0)) : var4).toLowerCase(Locale.ROOT);
            }
         }
      }

      return "";
   }

   static String d(String var0) {
      return e(var0.replace("+", "%2B"));
   }

   static String e(String var0) {
      try {
         return URLDecoder.decode(var0, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException var2) {
         return var0;
      }
   }

   interface EvidenceLeafConsumer {
      void leaf(String var1, String var2, String var3, String var4);
   }

   record PageValueEvidence(String a, String b, String c, String d, boolean e, String f, String g) {}

   record ResponseValueEvidence(String a, String b, String c, String d, String e) {}
}
