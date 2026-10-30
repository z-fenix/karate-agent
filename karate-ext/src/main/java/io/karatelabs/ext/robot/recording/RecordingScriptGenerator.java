package io.karatelabs.ext.robot.recording;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecordingScriptGenerator {
   private static final Pattern b = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
   private static final Pattern c = Pattern.compile("[^\\s;,=]+");
   private static final Pattern d = Pattern.compile("[\\\\^$.|?*+()\\[\\]{}]");
   private static final Pattern e = Pattern.compile("^responseCookies\\['([A-Za-z_$][A-Za-z0-9_$]*)']");
   private static final String f = "    ";
   private final RobotRecording g;
   private final List<String> h = new ArrayList<>();
   private final Map<String, Integer> i = new HashMap<>();
   private int j;
   static final int a = 64;
   private static final Pattern k = Pattern.compile("Made from a recording of \\S+ \\(run (\\S+)\\)");

   private RecordingScriptGenerator(RobotRecording var1) {
      this.g = var1;
   }

   public static io.karatelabs.ext.robot.recording.RecordingScriptGenerator.GeneratedScript a(RobotRecording var0, String var1) {
      RecordingScriptGenerator var2 = new RecordingScriptGenerator(var0);
      String var3 = var2.c(var1);

      for (String var7 : var3.split("\n")) {
         if (io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.g.matcher(var7).find()) {
            throw new IllegalStateException("a handle reached the feature: " + var7.strip());
         }
      }

      return new io.karatelabs.ext.robot.recording.RecordingScriptGenerator.GeneratedScript(var3, var2.b());
   }

   private String c(String var1) {
      this.h.add("Feature: " + this.a());
      boolean var2 = !var1.contains("/") && !var1.contains("\\") && !var1.endsWith(".jsonl");
      this.h.add("  Made from a recording of " + this.g.c().a() + this.g.c().b() + (var2 ? " (run " + var1 + ")" : " (" + var1 + ")"));
      this.h.add("");
      this.h.add("  Background:");

      for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage var4 : this.g.d()) {
         this.d("* def " + var4.a() + " = karate.get(" + b(var4.a()) + ", " + b(var4.c()) + ")");
      }

      for (String var10 : this.g.e()) {
         this.d("* def " + var10 + " = karate.get(" + b(var10) + ")");
         this.d("* if (!" + var10 + ") karate.fail(" + b("set " + var10 + ": ApiTest.config({name: '" + var10 + "', …})") + ")");
      }

      for (io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable var11 : this.g.f()) {
         if (!var11.d()) {
            this.d("* def " + var11.a() + " = " + a(var11.b()));
         }
      }

      if (!this.g.g().isEmpty()) {
         ArrayList<java.lang.String> var8 = new ArrayList();
         this.g.g().forEach(var1x -> var8.add(h(var1x.a()) + ": '#(" + var1x.b() + ")'"));
         this.d("* configure cookies = { " + String.join(", ", var8) + " }");
      }

      for (io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup var12 : this.g.i()) {
         this.h.add("");
         this.h.add("  Scenario: " + a(var12));
         this.j = 0;

         for (int var5 = 0; var5 < var12.b().size(); var5++) {
            if (var5 > 0) {
               this.h.add("");
            }

            this.b(var12.b().get(var5));
         }
      }

      return String.join("\n", this.h) + "\n";
   }

   private void d(String var1) {
      this.h.add("    " + var1);
      this.j++;
   }

   private static String a(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var0) {
      return io.karatelabs.ext.robot.recording.ActionCaptionBuilder.a(var0.e(), var0.f() == io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase.SIGN_IN);
   }

   private String a() {
      io.karatelabs.ext.robot.recording.RobotRecording.ApplicationEntryPage var1 = this.g.c();
      if (var1.c() != null) {
         return var1.c();
      } else {
         String[] var2 = var1.b().split("/");
         String var3 = var2.length == 0 ? "" : var2[var2.length - 1];
         if (var3.isEmpty()) {
            return var1.a();
         } else {
            String var4 = var3.replaceAll("[-_]+", " ").strip();
            return var4.isEmpty() ? var1.a() : Character.toUpperCase(var4.charAt(0)) + var4.substring(1);
         }
      }
   }

   public static String a(RobotRecording var0) {
      io.karatelabs.ext.robot.recording.RobotRecording.ApplicationEntryPage var1 = var0.c();
      String[] var2 = var1.b().split("/");
      String var3 = var2.length == 0 ? "" : var2[var2.length - 1].toLowerCase(Locale.ROOT);
      boolean var4 = var3.isEmpty() || Set.of("login", "signin", "sign-in", "index", "home", "default", "index.html").contains(var3);
      String var5 = a(var4 ? new RecordingScriptGenerator(var0).a() : var1.b());
      return var5 == null ? "recording" : var5;
   }

   public static String a(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
         var1 = (var1.length() > 64 ? var1.substring(0, 64) : var1).replaceAll("^-+|-+$", "");
         return var1.isEmpty() ? null : var1;
      }
   }

   public static byte[] a(Path var0) {
      try {
         return Files.isRegularFile(var0) ? Files.readAllBytes(var0) : null;
      } catch (UncheckedIOException | IOException var2) {
         return null;
      }
   }

   public static String b(Path var0) {
      return a(a(var0));
   }

   public static String a(byte[] var0) {
      List var1 = var0 == null ? List.of() : new String(var0, StandardCharsets.UTF_8).lines().toList();

      for (int var2 = 0; var2 + 1 < var1.size(); var2++) {
         if (((String)var1.get(var2)).startsWith("Feature:")) {
            Matcher var3 = k.matcher(((String)var1.get(var2 + 1)).strip());
            return var3.matches() ? var3.group(1) : null;
         }
      }

      return null;
   }

   private static String a(io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var3 : var0.b()) {
         String var4 = io.karatelabs.ext.robot.recording.ActionCaptionBuilder.b(var3.e(), var3.f() == io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase.SIGN_IN);
         if (var1.isEmpty() || !((String)var1.get(var1.size() - 1)).equalsIgnoreCase(var4)) {
            var1.add(var4);
         }
      }

      return io.karatelabs.ext.robot.recording.ActionCaptionBuilder.a(var1);
   }

   private void b(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1) {
      Map<java.lang.String, java.lang.Object> var2 = var1.b().get("request") instanceof Map var3 ? var3 : Map.of();
      this.h.add("    # " + a(var1));
      this.d("Given url " + var1.h());
      io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage var15 = this.g.d().stream().filter(var1x -> var1x.a().equals(var1.h())).findFirst().orElseThrow();
      ArrayList<String> var16 = new ArrayList<>();

      for (int var5 = var15.d().size(); var5 < var1.i().size(); var5++) {
         io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var6 = b(var1, "path", String.valueOf(var5), var1.i().get(var5));
         var16.add(var6 == null ? b(var1.i().get(var5)) : a(var6, false));
      }

      if (!var16.isEmpty()) {
         this.d("And path " + String.join(", ", var16));
      }

      if (var2.get("params") instanceof Map var17) {
         var17.forEach((var2x, var3x) -> {
            ArrayList<java.lang.String> var4 = new ArrayList();

            for (Object var7x : var3x instanceof List ? (List)var3x : var3x == null ? List.of("") : List.of(var3x)) {
               var4.add(this.a(var1, "query", String.valueOf(var2x), io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var7x)));
            }

            this.d("And param " + var2x + " = " + (var4.size() == 1 ? (String)var4.get(0) : "[" + String.join(", ", var4) + "]"));
         });
      }

      Object var18 = var2.get("body");
      String var20 = io.karatelabs.ext.robot.recording.PageEvidenceExtractor.c(var2);
      boolean var7 = var18 instanceof String && var20.contains("application/x-www-form-urlencoded");
      if (var2.get("headers") instanceof Map var8) {
         var8.forEach((var4, var5x) -> this.a(var1, String.valueOf(var4), var5x, var18, var7));
      }

      for (io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var24 : var1.j()) {
         if ("cookie".equals(var24.a()) && (var24.f() == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.CHAIN || var24.f() == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.TYPED)) {
            this.d("And cookie " + var24.b() + " = " + var24.g());
         }
      }

      if (var7) {
         for (String var12 : ((String)var18).split("&")) {
            int var13 = var12.indexOf(61);
            if (var13 >= 0) {
               String var14 = io.karatelabs.ext.robot.recording.PageEvidenceExtractor.e(var12.substring(0, var13));
               this.d("And form field " + var14 + " = " + this.a(var1, "form", var14, io.karatelabs.ext.robot.recording.PageEvidenceExtractor.e(var12.substring(var13 + 1))));
            }
         }
      } else if (var18 instanceof String var22 && !var22.isEmpty()) {
         this.d("And request " + this.a(var1, "body", "", var22));
      } else if (var18 instanceof Map || var18 instanceof List) {
         this.d("And request " + this.a(var1, var18, ""));
      }

      io.karatelabs.ext.robot.recording.RobotRecording.CaptureStats var23 = var1.l();
      if (var23 != null) {
         this.d("* configure retry = { count: " + var23.a() + ", interval: " + var23.b() + " }");
         this.d("And retry until responseStatus == " + var23.c() + (var23.d() == null ? "" : " && response." + var23.d() + " != null"));
      }

      this.d("When method " + String.valueOf(var2.get("method")).toLowerCase(Locale.ROOT));
      this.c(var1);
      this.i.put(var1.a(), this.j);
   }

   private void c(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1) {
      io.karatelabs.ext.robot.recording.RobotRecording.ResponseExpectation var2 = var1.k();
      if (var2.a() != null) {
         this.d("Then status " + io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var2.a()));
      }

      if (!var2.d()) {
         for (io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture var12 : var1.m()) {
            this.d("* def " + var12.a() + " = " + f(var12.b()));
         }
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();
         LinkedHashSet<java.lang.String> var4 = new LinkedHashSet();

         for (io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture var6 : var1.m()) {
            if (var6.b().startsWith("response.") || var6.b().startsWith("response[")) {
               var4.add(var6.b() + " == '#" + (!"number".equals(var6.c()) && !"boolean".equals(var6.c()) ? "string" : var6.c()) + "'");
            } else if (var6.b().startsWith("responseCookies")) {
               var4.add(e(var6.b()) + " == '#present'");
            } else if (var6.b().startsWith("responseHeaders")) {
               var4.add(var6.b() + " == '#string'");
            } else {
               var3.add("And match " + var6.a() + " == '#" + (!"number".equals(var6.c()) && !"boolean".equals(var6.c()) ? "string" : var6.c()) + "'");
            }
         }

         for (io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup var17 : this.g.i()) {
            for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var8 : var17.b()) {
               for (io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var10 : var8.j()) {
                  if (var10.f() == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE && var10.h() != null && var10.h().a().equals(var1.a())) {
                     var4.add(e(var10.h().b()) + " == '#present'");
                  }
               }
            }
         }

         var4.forEach(var1x -> this.d("And match " + var1x));
         if (var2.b() != null && !var2.b().isEmpty()) {
            ArrayList<java.lang.String> var14 = new ArrayList();
            var2.b().forEach((var1x, var2x) -> var14.add(h(var1x) + ": '" + var2x + "'"));
            this.d("And match response contains { " + String.join(", ", var14) + " }");
         } else if (var2.e() != null && var2.c()) {
            this.d("And match response == '#[" + var2.e().size() + "]'");
         } else if (var2.c()) {
            this.d("And match response == '#[]'");
         }

         if (var2.e() != null) {
            for (int var15 = 0; var15 < var2.e().size(); var15++) {
               String var18 = var2.c() ? "response[" + var15 + "]" : "response";
               String var21 = var2.e().get(var15);

               String var20 = switch (var21) {
                  case "present" -> "'#present'";
                  case "empty" -> "'#[0]'";
                  case "null" -> "'#null'";
                  default -> "'#notpresent'";
               };
               this.d("And match " + var18 + ".errors == " + var20);
            }
         }

         for (io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture var19 : var1.m()) {
            this.d("* def " + var19.a() + " = " + f(var19.b()));
         }

         var3.forEach(this::d);
      }
   }

   private static String e(String var0) {
      return f(var0.substring(0, var0.length() - ".value".length()));
   }

   private static String f(String var0) {
      if (var0.startsWith("responseCookies")) {
         return e.matcher(var0).replaceFirst("responseCookies.$1");
      } else if (!var0.startsWith("meta ")) {
         return var0;
      } else {
         String var1 = d.matcher(var0.substring("meta ".length())).replaceAll("\\\\$0");
         String var2 = "karate.extract(response, " + b("(?i)<meta[^>]*name\\s*=\\s*[\"']" + var1 + "[\"'][^>]*>") + ", 0)";
         return "karate.extract(" + var2 + ", " + b("(?i)content\\s*=\\s*[\"']([^\"']*)[\"']") + ", 1)";
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void a(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1, String var2, Object var3, Object var4, boolean var5) {
      String var6 = var2.toLowerCase(Locale.ROOT);
      Object var7 = ((Map)var1.b().get("request")).get("method");
      if (!io.karatelabs.ext.robot.recording.PageEvidenceExtractor.a(var2, io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b.contains(String.valueOf(var7).toUpperCase(Locale.ROOT)))) {
         ArrayList<java.lang.String> var8 = new ArrayList();

         for (Object var13 : var3 instanceof List ? (List)var3 : List.of(var3)) {
            String var11 = String.valueOf(var13);
            if ((!"accept".equals(var6) || !"*/*".equals(var11.strip()) && !"Document".equals(var1.b().get("type")))
               && (!"content-type".equals(var6) || !g(var11).equals(a(var4, var5)))) {
               String var12 = this.a(var1, var2, var11);
               if (var12 != null) {
                  var8.add(var12);
               }
            }
         }

         if (!var8.isEmpty()) {
            this.d("And header " + var2 + " = " + (var8.size() == 1 ? (String)var8.get(0) : "[" + String.join(", ", var8) + "]"));
         }
      }
   }

   private String a(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1, String var2, String var3) {
      io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var4 = b(var1, "header", var2, var3);
      if (var4 == null) {
         ArrayList<java.lang.String> var5 = new ArrayList();
         StringBuilder var6 = new StringBuilder();
         Matcher var7 = c.matcher(var3);
         int var8 = 0;

         while (var7.find()) {
            io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var9 = b(var1, "header", var2, var7.group());
            if (var9 != null) {
               if (var9.f() == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.AUTH && var9.g() == null) {
                  return null;
               }

               var6.append(var3, var8, var7.start());
               if (!var6.isEmpty()) {
                  var5.add(b(var6.toString()));
                  var6.setLength(0);
               }

               var5.add(a(var9, true));
               var8 = var7.end();
            }
         }

         var6.append(var3.substring(var8));
         if (!var6.isEmpty()) {
            var5.add(b(var6.toString()));
         }

         return String.join(" + ", var5);
      } else {
         return var4.f() == io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.AUTH && var4.g() == null ? null : a(var4, true);
      }
   }

   private String a(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1, String var2, String var3, String var4) {
      io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var5 = b(var1, var2, var3, var4);
      return var5 == null ? b(var4) : a(var5, true);
   }

   private static io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence b(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var0, String var1, String var2, String var3) {
      for (io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var5 : var0.j()) {
         if (var5.a().equals(var1) && var5.b().equals(var2) && var5.d().equals(var3) && var5.f() != io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.LITERAL && var5.f() != io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind.COOKIE) {
            return var5;
         }
      }

      return null;
   }

   private static String a(io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var0, boolean var1) {
      String var2 = var0.g();
      return var1 && "urlEncode".equals(var0.i()) ? "karate.urlEncode(" + var2 + ")" : var2;
   }

   private String a(io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction var1, Object var2, String var3) {
      if (var2 instanceof Map var9) {
         ArrayList<java.lang.String> var11 = new ArrayList();
         var9.forEach(
            (var4x, var5x) -> var11.add(h(String.valueOf(var4x)) + ": " + this.a(var1, var5x, var3.isEmpty() ? String.valueOf(var4x) : var3 + "." + var4x))
         );
         return var11.isEmpty() ? "{}" : "{ " + String.join(", ", var11) + " }";
      } else if (var2 instanceof List var8) {
         ArrayList<java.lang.String> var10 = new ArrayList();

         for (int var12 = 0; var12 < var8.size(); var12++) {
            var10.add(this.a(var1, var8.get(var12), var3 + "[" + var12 + "]"));
         }

         return "[" + String.join(", ", var10) + "]";
      } else {
         io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence var4 = var2 != null && !(var2 instanceof Map) && !(var2 instanceof List) ? b(var1, "body", var3, io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var2)) : null;
         if (var4 == null) {
            return a(var2);
         } else {
            String var6 = var4.i() == null ? "" : var4.i();

            String var5 = switch (var6) {
               case "toString" -> var4.g() + " + \"\"";
               case "toNumber" -> "Number(" + var4.g() + ")";
               case "urlEncode" -> "karate.urlEncode(" + var4.g() + ")";
               default -> var4.g();
            };
            return "'#(" + var5 + ")'";
         }
      }
   }

   private static String a(Object var0, boolean var1) {
      if (var1) {
         return "application/x-www-form-urlencoded";
      } else if (!(var0 instanceof Map) && !(var0 instanceof List)) {
         return var0 instanceof String var2 && !var2.isEmpty() ? "text/plain" : "";
      } else {
         return "application/json";
      }
   }

   private static String g(String var0) {
      return var0.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
   }

   private List<Map<String, Object>> b() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RobotRecording.PatternEvidence var3 : this.g.m()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("key", var3.a());
         var4.put("id", var3.b());
         var4.put("kind", var3.c());
         var4.put("scenario", var3.d());
         var4.put("at", this.i.get(var3.b()));
         var4.put("text", var3.e().startsWith("* ") ? var3.e() : "And " + var3.e());
         var4.put("requires", var3.f());
         var4.put("evidence", var3.g());
         var1.add(var4);
      }

      return var1;
   }

   private static String h(String var0) {
      return b.matcher(var0).matches() ? var0 : b(var0);
   }

   private static String a(Object var0) {
      if (var0 == null) {
         return "null";
      } else {
         return !(var0 instanceof Number) && !(var0 instanceof Boolean) ? b(String.valueOf(var0)) : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.b(var0);
      }
   }

   static String b(String var0) {
      return io.karatelabs.ext.robot.recording.PageEvidenceExtractor.c(var0);
   }

   public record GeneratedScript(String a, List<Map<String, Object>> b) {

   }
}
