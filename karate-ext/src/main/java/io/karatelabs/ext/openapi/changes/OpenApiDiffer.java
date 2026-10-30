package io.karatelabs.ext.openapi.changes;

import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.jsapi.JsApi;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenApiDiffer {
   static final String a = "karate-openapi-diff";
   private static final List<String> b = List.of("get", "put", "post", "delete", "options", "head", "patch", "trace");
   private static final Pattern c = Pattern.compile("\\{([^}]*)}");
   private static final String d = "\u0000unread";
   private static final Set<String> e = Set.of(
      "$ref", "summary", "description", "servers", "parameters", "get", "put", "post", "delete", "options", "head", "patch", "trace"
   );
   private final io.karatelabs.ext.openapi.changes.DiffDocument f;
   private final io.karatelabs.ext.openapi.changes.DiffDocument g;
   private final io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions h;
   private final DiffSession i;
   private List<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> j = List.of();
   private final Map<String, Set<String>> k = new HashMap<>();
   private final Map<String, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> l = new HashMap<>();
   private io.karatelabs.ext.lint.WaiverLedger m;
   private final io.karatelabs.ext.openapi.changes.ReferenceGraph n = new io.karatelabs.ext.openapi.changes.ReferenceGraph();
   private final LocalDate o;

   private OpenApiDiffer(io.karatelabs.ext.openapi.changes.DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffDocument var2, io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions var3) {
      this.f = var1;
      this.g = var2;
      this.h = var3 == null ? new io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions(null, null, null) : var3;
      this.o = this.h.a() != null ? this.h.a() : LocalDate.now(ZoneOffset.UTC);
      this.i = new DiffSession(var1, var2);
   }

   public static Map<String, Object> a(String var0, byte[] var1, String var2, byte[] var3, io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions var4) {
      io.karatelabs.ext.openapi.changes.DiffDocument var5 = io.karatelabs.ext.openapi.changes.DiffDocument.a("baseline", var0, a(var1), var1);
      io.karatelabs.ext.openapi.changes.DiffDocument var6 = io.karatelabs.ext.openapi.changes.DiffDocument.a("current", var2, a(var3), var3);
      return new OpenApiDiffer(var5, var6, var4).a();
   }

   public static Map<String, Object> a(String var0, Resource var1, String var2, Resource var3, io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions var4) {
      return a(var0, a("baseline", var0, var1), var2, a("current", var2, var3), var4);
   }

   public static String a(String var0, String var1, String var2) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("openapi.diff", "openapi");

      Map var3;
      try {
         HashMap var4 = new HashMap();
         var4.put("asOf", var2);
         var3 = a(var0, a("baseline", var0), var1, a("current", var1), io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions.a(var4));
      } catch (io.karatelabs.ext.openapi.changes.OpenApiDiffException var5) {
         var3 = JsApi.error(var5);
      }

      return StringUtils.formatJson(var3, true, false, false);
   }

   public static void a(String[] var0) {
      if (var0.length < 2) {
         System.err.println("usage: OpenapiDiff <baseline> <current> [asOf YYYY-MM-DD]");
         System.exit(2);
      }

      System.out.println(a(var0[0], var0[1], var0.length > 2 ? var0[2] : null));
   }

   private static byte[] a(String var0, String var1) {
      try {
         return Files.readAllBytes(Path.of(var1));
      } catch (RuntimeException | IOException var3) {
         throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var0, var1, String.valueOf(var3.getMessage()));
      }
   }

   private static byte[] a(String var0, String var1, Resource var2) {
      try {
         byte[] var4;
         try (InputStream var3 = var2.getStream()) {
            var4 = var3.readAllBytes();
         }

         return var4;
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      } catch (RuntimeException var9) {
         throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var0, var1, String.valueOf(var9.getMessage()));
      }
   }

   private static String a(byte[] var0) {
      String var1 = new String(var0, StandardCharsets.UTF_8);
      return var1.startsWith("\ufeff") ? var1.substring(1) : var1;
   }

   private Map<String, Object> a() {
      this.m = this.c();
      var var1 = this.a(this.f);
      var var2 = this.a(this.g);
      this.j = var2;

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var4 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var2)) {
         this.l.putIfAbsent(var4.a(), var4);
      }

      this.b();
      LinkedHashMap var13 = new LinkedHashMap();

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var5 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var1)) {
         var13.putIfAbsent(var5.a(), var5);
      }

      HashMap var15 = new HashMap();
      HashSet var16 = new HashSet();
      HashSet var6 = new HashSet();

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var8 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var1)) {
         io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var9 = this.l.get(var8.a());
         if (var13.get(var8.a()) == var8 && var9 != null) {
            var15.put(var8, var9);
            var6.add(var9);
         }
      }

      Map var17 = a(var1, var15.keySet());
      Map var18 = a(var2, var6);

      for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var17.entrySet())) {
         List var11 = (List)var18.get(var10.getKey());
         if (((List)var10.getValue()).size() == 1 && var11 != null && var11.size() == 1) {
            io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var12 = (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)((List)var10.getValue()).get(0);
            var15.put(var12, (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var11.get(0));
            var6.add((io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var11.get(0));
            var16.add(var12);
         }
      }

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var22 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var1)) {
         this.a(var22, this.a(var22, (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var15.get(var22)));
      }

      HashMap var21 = new HashMap();
      var15.forEach((var1x, var2x) -> var21.put(var2x, var1x));

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var26 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var2)) {
         this.a(var26, this.a((io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var21.get(var26), var26));
      }

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var27 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var1)) {
         io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var29 = (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var15.get(var27);
         if (var29 != null) {
            this.a(var27, var29, var16.contains(var27));
         } else {
            this.b(var27);
         }
      }

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var28 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>) (Iterable<?>) (var2)) {
         if (!var6.contains(var28)) {
            this.c(var28);
         }
      }

      this.n.a(this.i);
      return this.a(!var1.isEmpty() || !var2.isEmpty());
   }

   private void a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2) {
      this.n.a(var1.d(), var2, var1.l(), var1.i());
      this.n.a(var1.d(), var2, var1.d().a(var1.m()));
      if (var1.j().get("parameters") != null) {
         this.n.a(var1.d(), var2, var1.n() + "/parameters", var1.j().get("parameters"));
      }
   }

   private static Map<String, List<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation>> a(List<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> var0, Set<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var4 : var0) {
         if (!var1.contains(var4) && var4.b() != null) {
            ((java.util.List)(var2.computeIfAbsent(var4.b(), var0x -> new ArrayList<>()))).add(var4);
         }
      }

      return var2;
   }

   private List<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> a(io.karatelabs.ext.openapi.changes.DiffDocument var1) {
      ArrayList var2 = new ArrayList();
      this.a(var1, "paths", var2);
      this.a(var1, "webhooks", var2);
      return var2;
   }

   private void a(io.karatelabs.ext.openapi.changes.DiffDocument var1, String var2, List<io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation> var3) {
      boolean var4 = var2.equals("webhooks");
      if (var1.d.get(var2) instanceof Map var5) {
         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            String var8 = String.valueOf(var7.getKey());
            if (var4 || var8.startsWith("/")) {
               String var9 = "/" + var2 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var8);
               String var10 = var4 ? "webhook " + var8 : c.matcher(var8).replaceAll("{}");
               io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var11 = var1.a(var7.getValue(), var9);
               if (this.a(var11, "pathItem", var1) && var11.b() instanceof Map var12) {
                  for (Object var14 : var12.keySet()) {
                     String var15 = String.valueOf(var14);
                     if (!e.contains(var15) && !var15.startsWith("x-")) {
                        this.i.a(var15, var11.c() + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var15), var1.a, "KEYWORD", "not in the openapi.diff/1 inventory");
                     }
                  }

                  ArrayList var21 = new ArrayList();
                  Matcher var22 = c.matcher(var8);

                  while (var22.find()) {
                     var21.add(var22.group(1));
                  }

                  for (String var16 : b) {
                     if (var12.get(var16) instanceof Map var17) {
                        var3.add(new io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation(var1, var16, var8, var10, var21, var17, var12, var9 + "/" + var16, var11.c() + "/" + var16, var9, var11.c(), var4));
                     }
                  }
               } else {
                  this.k.computeIfAbsent(var1.a, var0 -> new HashSet<>()).add(var10);
               }
            }
         }
      }
   }

   private boolean a(io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var1, String var2, io.karatelabs.ext.openapi.changes.DiffDocument var3) {
      if (var1.e() != null) {
         this.i.a(var2, var1.c(), var3.a, "EXTERNAL_REF", var1.e());
         return false;
      } else if (var1.f()) {
         this.i.a(var2, var1.c(), var3.a, "UNRESOLVED_REF", String.valueOf(((Map)var1.b()).get("$ref")));
         return false;
      } else {
         return true;
      }
   }

   private void b() {
      for (io.karatelabs.ext.openapi.changes.DiffDocument var2 : List.of(this.f, this.g)) {
         if (var2.d.containsKey("jsonSchemaDialect")) {
            this.i.a("jsonSchemaDialect", "/jsonSchemaDialect", var2.a, "KEYWORD", "schemas are read under the OAS dialect");
         }

         if (var2.d.get("components") instanceof Map var3 && var3.get("securitySchemes") instanceof Map var4) {
            for (Object var6 : var4.keySet()) {
               this.i
                  .a("securityScheme", "/components/securitySchemes/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(String.valueOf(var6)), var2.a, "SECURITY_SCHEME", null);
            }
         }
      }

      var var7 = b(this.f.d.get("servers"));
      var var8 = b(this.g.d.get("servers"));
      if (!io.karatelabs.ext.openapi.changes.ComparableSchema.a(var7, var8)) {
         io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var9 = this.i.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SERVERS_CHANGED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, null).a("servers", "servers").b("servers").c(c(var7) + " -> " + c(var8));
         if (this.f.d.containsKey("servers")) {
            var9.b("/servers", "/servers");
         }

         if (this.g.d.containsKey("servers")) {
            var9.c("/servers", "/servers");
         }

         if (var9.l == null && var9.m == null) {
            var9.b("/servers", "");
         }

         var9.a();
      }
   }

   private static List<Object> b(Object var0) {
      ArrayList var1 = new ArrayList();
      if (var0 instanceof List var2) {
         for (Object var4 : var2) {
            if (var4 instanceof Map var5) {
               TreeMap var6 = new TreeMap();
               var6.put("url", var5.get("url"));
               if (var5.get("variables") instanceof Map var7) {
                  TreeMap var13 = new TreeMap();

                  for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
                     if (var10.getValue() instanceof Map var11) {
                        TreeMap var14 = new TreeMap();
                        var14.put("enum", var11.get("enum"));
                        var14.put("default", var11.get("default"));
                        var13.put(String.valueOf(var10.getKey()), var14);
                     }
                  }

                  var6.put("variables", var13);
               }

               var1.add(var6);
            }
         }
      }

      if (var1.isEmpty()) {
         var1.add(new TreeMap<>(Map.of("url", "/")));
      }

      return var1;
   }

   private static String c(Object var0) {
      ArrayList var1 = new ArrayList();

      for (Object var3 : (List)var0) {
         var1.add(String.valueOf(((Map)var3).get("url")));
      }

      return String.join(", ", var1);
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.OperationContext a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2) {
      io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var3 = var1 != null ? var1 : var2;
      var var4 = a(var3);
      LinkedHashMap var5 = new LinkedHashMap();
      if (var1 != null) {
         var5.put("baseline", var1.c());
      }

      if (var2 != null) {
         var5.put("current", var2.c());
      }

      var4.put("graphIds", var5);
      return new io.karatelabs.ext.openapi.changes.DiffSession.OperationContext(var3.a(), var4);
   }

   private static Map<String, Object> a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("method", var0.e().toUpperCase(Locale.ROOT));
      var1.put("path", var0.o() ? null : var0.f());
      if (var0.o()) {
         var1.put("webhook", var0.f());
      }

      var1.put("operationId", var0.b());
      return var1;
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var3, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var4) {
      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var5 = this.i.a(var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var2).a("operation", var2.a()).b(var2.a());
      if (var3 != null) {
         var5.b(var3.k(), var3.l()).a(this.d(var3));
      }

      if (var4 != null) {
         var5.c(var4.k(), var4.l());
      }

      return var5;
   }

   private boolean a(String var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2) {
      return this.k.getOrDefault(var1, Set.of()).contains(var2.g());
   }

   private void b(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1) {
      if (!this.a("current", var1)) {
         io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2 = this.a(var1, (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)null);
         OpenApiChangeKind var3 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.OPERATION_REMOVED;
         Object var4 = var1.i().get("x-sunset");
         String var5 = "operation removed";
         if (var4 != null) {
            String var6 = var1.l() + "/x-sunset";
            LocalDate var7 = a(var4);
            if (this.h.a() == null) {
               this.i.a("x-sunset", var6, "baseline", "NOASOF", "removed before or after x-sunset " + var4 + "? pass asOf");
            } else if (var7 == null) {
               this.i.a("x-sunset", var6, "baseline", "KEYWORD", "x-sunset is not a date: " + var4);
            } else if (this.h.a().isBefore(var7)) {
               var3 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.OPERATION_REMOVED_BEFORE_SUNSET;
               var5 = "removed as of " + this.h.a() + ", before its x-sunset " + var7;
            }
         }

         this.a(var3, var2, var1, null).c(var5).a();
         this.a(var1, var2, var1, null);
      }
   }

   private void c(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1) {
      if (!this.a("baseline", var1)) {
         io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2 = this.a((io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)null, var1);
         this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.OPERATION_ADDED, var2, null, var1).c("operation added").a();
         this.a(var1, var2, null, var1);
      }
   }

   private void a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, boolean var3) {
      io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var4 = this.a(var1, var2);
      if (var3) {
         boolean var5 = !var1.e().equalsIgnoreCase(var2.e());
         this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.OPERATION_PATH_CHANGED, var4, var1, var2)
            .c(
               var5
                  ? var1.e().toUpperCase(Locale.ROOT) + " " + var1.f() + " -> " + var2.e().toUpperCase(Locale.ROOT) + " " + var2.f()
                  : var1.f() + " -> " + var2.f()
            )
            .b(a(var2))
            .a();
      }

      if (!Objects.equals(var1.b(), var2.b())) {
         io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var10 = this.i
            .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.OPERATION_ID_CHANGED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var4)
            .a("operation", var4.a())
            .b(var4.a())
            .c(var1.b() + " -> " + var2.b())
            .b(a(var2))
            .a(this.d(var1));
         if (var1.b() != null) {
            var10.b(var1.k() + "/operationId", var1.l() + "/operationId");
         }

         if (var2.b() != null) {
            var10.c(var2.k() + "/operationId", var2.l() + "/operationId");
         }

         var10.a();
      }

      if (!var3) {
         Map var11 = null;
         Map var6 = null;

         for (int var7 = 0; var7 < var1.h().size(); var7++) {
            if (!var1.h().get(var7).equals(var2.h().get(var7))) {
               if (var11 == null) {
                  var11 = this.a(var1, false);
                  var6 = this.a(var2, false);
               }

               io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var8 = (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry)var11.get("path#" + var7);
               io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var9 = (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry)var6.get("path#" + var7);
               this.i
                  .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PATH_PARAM_RENAMED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var4)
                  .a("parameter", "path#" + var7)
                  .b(var4.a() + " path#" + var7)
                  .b(var8 == null ? var1.m() : var8.d(), var8 == null ? var1.n() : var8.e())
                  .c(var9 == null ? var2.m() : var9.d(), var9 == null ? var2.n() : var9.e())
                  .c("{" + var1.h().get(var7) + "} -> {" + var2.h().get(var7) + "}")
                  .a();
            }
         }
      }

      this.a(var1, var2, var4);
      this.b(var1, var2, var4);
      this.a(var1, var2, var4, var3);
      this.c(var1, var2, var4);
      this.d(var1, var2, var4);

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var13 : List.of(var1, var2)) {
         String var14 = var13.d().a;
         if (var13.i().containsKey("callbacks")) {
            this.i.a("callbacks", var13.l() + "/callbacks", var14, "CALLBACKS", null);
         }

         if (var13.i().containsKey("servers")) {
            this.i.a("servers", var13.l() + "/servers", var14, "KEYWORD", "operation servers");
         }

         if (var13.j().containsKey("servers")) {
            this.i.a("servers", var13.n() + "/servers", var14, "KEYWORD", "path servers");
         }
      }
   }

   private Map<String, Object> d(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1) {
      boolean var2 = Boolean.TRUE.equals(var1.i().get("deprecated"));
      Object var3 = var1.i().get("x-sunset");
      Object var4 = var1.i().get("x-successor");
      if (!var2 && var3 == null && var4 == null) {
         return null;
      } else {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("deprecated", var2);
         var5.put("sunset", var3 == null ? null : String.valueOf(var3));
         Object var6 = var4;
         if (var4 != null) {
            io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution var7 = this.d(var4);
            if (var7.a() != null) {
               var6 = var7.a();
            }
         }

         var5.put("successor", var6);
         return var5;
      }
   }

   private void a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      boolean var4 = Boolean.TRUE.equals(var1.i().get("deprecated"));
      boolean var5 = Boolean.TRUE.equals(var2.i().get("deprecated"));
      if (var4 != var5) {
         this.a(var5 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_REMOVED, var3, var1, var2)
            .d(var1.l() + "/deprecated", var2.l() + "/deprecated")
            .a();
      }

      Object var6 = var1.i().get("x-sunset");
      Object var7 = var2.i().get("x-sunset");
      if (var6 == null && var7 != null) {
         this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SUNSET_SET, var3, var1, var2).d(null, var2.l() + "/x-sunset").c(String.valueOf(var7)).a();
      } else if (var6 != null && var7 == null) {
         this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SUNSET_REMOVED, var3, var1, var2).d(var1.l() + "/x-sunset", null).c(String.valueOf(var6)).a();
      } else if (var6 != null && !io.karatelabs.ext.openapi.changes.ComparableSchema.a(var6, var7)) {
         LocalDate var8 = a(var6);
         LocalDate var9 = a(var7);
         if (var8 == null || var9 == null) {
            this.i.a("x-sunset", var1.l() + "/x-sunset", "both", "KEYWORD", "x-sunset is not a date: " + var6 + " -> " + var7);
         } else if (!var8.equals(var9)) {
            this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SUNSET_CHANGED, var3, var1, var2)
               .a(var9.isAfter(var8) ? io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE : io.karatelabs.ext.diff.DiffSeverity.WARNING)
               .d(var1.l() + "/x-sunset", var2.l() + "/x-sunset")
               .c(var8 + " -> " + var9)
               .a();
         }
      }

      Object var10 = var1.i().get("x-successor");
      Object var11 = var2.i().get("x-successor");
      if ((var10 != null || var11 != null) && !io.karatelabs.ext.openapi.changes.ComparableSchema.a(var10, var11)) {
         this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SUCCESSOR_CHANGED, var3, var1, var2)
            .d(var1.l() + "/x-successor", var2.l() + "/x-successor")
            .c(e(var10) + " -> " + e(var11))
            .a();
      }

      this.a(var2, var3, var1, var2);
   }

   private void a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var3, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var4) {
      Object var5 = var1.i().get("x-successor");
      if (var5 != null) {
         io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution var6 = this.d(var5);
         if (var6.b() != null) {
            this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SUCCESSOR_UNRESOLVED, var2, var3, var4)
               .a("successor", var2.a() + " x-successor")
               .d(var3 == null ? null : var3.l() + "/x-successor", var4 == null ? null : var4.l() + "/x-successor")
               .c(e(var5) + ": " + var6.b())
               .a();
         }
      }
   }

   private io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution d(Object var1) {
      if (var1 instanceof Map var2 && var2.get("method") != null && var2.get("path") != null) {
         String var7 = String.valueOf(var2.get("method")).toUpperCase(Locale.ROOT) + " " + c.matcher(String.valueOf(var2.get("path"))).replaceAll("{}");
         io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var8 = this.l.get(var7);
         return var8 == null ? new io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution(null, "dangling — no such current operation") : new io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution(e(var8), null);
      } else if (var1 instanceof String var6) {
         ArrayList var3 = new ArrayList();

         for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var5 : this.j) {
            if (var6.equals(var5.b())) {
               var3.add(var5);
            }
         }

         return var3.size() == 1
            ? new io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution(e((io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation)var3.get(0)), null)
            : new io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution(
               null,
               var3.isEmpty()
                  ? "dangling — no current operation has this operationId"
                  : "ambiguous — " + var3.size() + " current operations have this operationId"
            );
      } else {
         return new io.karatelabs.ext.openapi.changes.OpenApiDiffer.SuccessorResolution(null, "malformed — expected {method, path} or an operationId");
      }
   }

   private static Map<String, Object> e(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("method", var0.e().toUpperCase(Locale.ROOT));
      var1.put("path", var0.f());
      if (var0.b() != null) {
         var1.put("operationId", var0.b());
      }

      return var1;
   }

   private static String e(Object var0) {
      return var0 instanceof Map var1 ? var1.get("method") + " " + var1.get("path") : String.valueOf(var0);
   }

   private void b(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      var var4 = g(var1);
      var var5 = g(var2);
      String var6 = f(var1);
      String var7 = f(var2);

      for (Map var9 : (Iterable<Map>) (Iterable<?>) (var4)) {
         if (var5.stream().noneMatch(var1x -> a((Map<String, Set<String>>)var1x, var9))) {
            this.i
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SECURITY_TIGHTENED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var3)
               .a("security", "security " + b(var9))
               .b(var3.a() + " security " + b(var9))
               .b(var1.k() + "/security", var6)
               .c(var2.k() + "/security", var7)
               .c("no current alternative is satisfied by " + c(var9))
               .a();
         }
      }

      for (Map var11 : (Iterable<Map>) (Iterable<?>) (var5)) {
         if (var4.stream().noneMatch(var1x -> a((Map<String, Set<String>>)var1x, var11))) {
            this.i
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SECURITY_LOOSENED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var3)
               .a("security", "security " + b(var11))
               .b(var3.a() + " security " + b(var11))
               .c(var2.k() + "/security", var7)
               .c(c(var11) + " now suffices")
               .a();
         }
      }
   }

   private static String f(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0) {
      if (var0.i().containsKey("security")) {
         return var0.l() + "/security";
      } else {
         return var0.d().d.containsKey("security") ? "/security" : var0.l();
      }
   }

   private static List<Map<String, Set<String>>> g(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0) {
      Object var1 = var0.i().containsKey("security") ? var0.i().get("security") : var0.d().d.get("security");
      ArrayList var2 = new ArrayList();
      if (var1 instanceof List) {
         for (Object var5 : (List)var1) {
            TreeMap var6 = new TreeMap();
            if (var5 instanceof Map var7) {
               for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
                  TreeSet var10 = new TreeSet();
                  Object var12 = var9.getValue();
                  if (var12 instanceof List) {
                     for (Object var13 : (List)var12) {
                        var10.add(String.valueOf(var13));
                     }
                  }

                  var6.put(String.valueOf(var9.getKey()), var10);
               }
            }

            var2.add(var6);
         }
      }

      if (var2.isEmpty()) {
         var2.add(new TreeMap());
      }

      return var2;
   }

   private static boolean a(Map<String, Set<String>> var0, Map<String, Set<String>> var1) {
      for (Entry var3 : var0.entrySet()) {
         Set var4 = (Set)var1.get(var3.getKey());
         if (var4 == null || !var4.containsAll((Collection<?>)var3.getValue())) {
            return false;
         }
      }

      return true;
   }

   private static String b(Map<String, Set<String>> var0) {
      return var0.isEmpty() ? "{}" : String.join("+", var0.keySet());
   }

   private static String c(Map<String, Set<String>> var0) {
      return var0.isEmpty() ? "anonymous access" : var0.toString();
   }

   private void a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3, boolean var4) {
      var var5 = this.a(var1, var4);
      var var6 = this.a(var2, var4);
      boolean var7 = var5.containsKey("\u0000unread") || var6.containsKey("\u0000unread");
      var5.remove("\u0000unread");
      var6.remove("\u0000unread");
      SchemaDiffWalker var8 = new SchemaDiffWalker(this.i, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var3);

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var10 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry>) (Iterable<?>) (var5.values())) {
         io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var11 = (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry)var6.get(var10.a());
         String var12 = var3.a() + " param " + var10.a();
         Map var13 = Boolean.TRUE.equals(var10.c().get("deprecated")) ? Map.of("deprecated", true) : null;
         if (var11 == null && var7) {
            this.i.a("parameter", var10.e(), "baseline", "UNRESOLVED_REF", "unpaired beside a parameter behind an external or dangling $ref");
         } else if (var11 == null) {
            this.i
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_REMOVED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var3)
               .a("parameter", var10.a())
               .b(var12)
               .b(var10.d(), var10.e())
               .a(var13)
               .c("parameter removed")
               .a();
         } else {
            boolean var14 = a(var10);
            boolean var15 = a(var11);
            if (var14 != var15) {
               this.a(
                     var15 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_BECAME_REQUIRED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_BECAME_OPTIONAL,
                     var3,
                     var10,
                     var11,
                     var12,
                     var13
                  )
                  .d(var10.e() + "/required", var11.e() + "/required")
                  .a();
            }

            String var16 = b(var10);
            String var17 = b(var11);
            boolean var18 = a(var10, var16);
            boolean var19 = a(var11, var17);
            if (!var16.equals(var17) || var18 != var19) {
               this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_STYLE_CHANGED, var3, var10, var11, var12, var13)
                  .c("style " + var16 + " explode " + var18 + " -> style " + var17 + " explode " + var19)
                  .a();
            }

            boolean var20 = Boolean.TRUE.equals(var10.c().get("deprecated"));
            boolean var21 = Boolean.TRUE.equals(var11.c().get("deprecated"));
            if (var20 != var21) {
               this.i
                  .a(var21 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_REMOVED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE, var3)
                  .a("parameter", var10.a())
                  .b(var12)
                  .b(var10.d(), var10.e())
                  .c(var11.d(), var11.e())
                  .d(var10.e() + "/deprecated", var11.e() + "/deprecated")
                  .a(var13)
                  .a();
            }

            boolean var22 = false;

            for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var24 : List.of(var10, var11)) {
               String var25 = var24 == var10 ? "baseline" : "current";

               for (String var27 : List.of("allowReserved", "allowEmptyValue")) {
                  if (var24.c().containsKey(var27)) {
                     this.i.a(var27, var24.e() + "/" + var27, var25, "KEYWORD", null);
                  }
               }

               if (var24.c().containsKey("content")) {
                  this.i.a("content", var24.e() + "/content", var25, "PARAM_CONTENT", null);
                  var22 = true;
               }
            }

            if (!var22 && !this.a(var10.c(), var11.c(), io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var3, "param " + var10.a(), var12, var10.d(), var11.d(), var10.e(), var11.e())) {
               var8.a(
                  io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.f, var10.c().get("schema"), var10.e() + "/schema"),
                  io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.g, var11.c().get("schema"), var11.e() + "/schema"),
                  new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath("param " + var10.a(), var12, var10.d() + "/schema", var11.d() + "/schema", false, 0)
               );
            }
         }
      }

      for (io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var29 : (Iterable<io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry>) (Iterable<?>) (var6.values())) {
         if (!var5.containsKey(var29.a()) && var7) {
            this.i.a("parameter", var29.e(), "current", "UNRESOLVED_REF", "unpaired beside a parameter behind an external or dangling $ref");
         } else if (!var5.containsKey(var29.a())) {
            this.i
               .a(a(var29) ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_ADDED_REQUIRED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PARAM_ADDED_OPTIONAL, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var3)
               .a("parameter", var29.a())
               .b(var3.a() + " param " + var29.a())
               .c(var29.d(), var29.e())
               .c("parameter added")
               .a();
         }
      }
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var2, io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var3, io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var4, String var5, Map<String, Object> var6) {
      return this.i.a(var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var2).a("parameter", var3.a()).b(var5).b(var3.d(), var3.e()).c(var4.d(), var4.e()).a(var6);
   }

   private Map<String, io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry> a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, boolean var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      List var4 = var1.i().get("parameters") instanceof List var5 ? var5 : List.of();
      List var13 = var1.j().get("parameters") instanceof List var14 ? var14 : List.of();
      int var15 = 0;

      for (int var16 = 0; var16 < var4.size() + var13.size(); var16++) {
         boolean var8 = var16 < var4.size();
         String var9 = (var8 ? var1.l() : var1.n()) + "/parameters/" + (var8 ? var16 : var16 - var4.size());
         io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var10 = var1.d().a(var8 ? var4.get(var16) : var13.get(var16 - var4.size()), var9);
         if (this.a(var10, "parameter", var1.d()) && var10.b() instanceof Map var11) {
            String var17 = a(var1, var11, var2);
            if (!var3.containsKey(var17)) {
               var3.put(var17, new io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry(var17, String.valueOf(var11.get("in")), var11, var1.k() + "/parameters/" + var15++, var10.c()));
            }
         } else {
            var3.put("\u0000unread", null);
            var15++;
         }
      }

      return var3;
   }

   private static String a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0, Map<String, Object> var1, boolean var2) {
      String var3 = String.valueOf(var1.get("in"));
      String var4 = String.valueOf(var1.get("name"));
      if (var3.equals("path")) {
         int var5 = var2 ? -1 : var0.h().indexOf(var4);
         return var5 >= 0 ? "path#" + var5 : "path:" + var4;
      } else {
         return var3.equals("header") ? "header:" + var4.toLowerCase(Locale.ROOT) : var3 + ":" + var4;
      }
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var0) {
      return var0.b().equals("path") || Boolean.TRUE.equals(var0.c().get("required"));
   }

   private static String b(io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var0) {
      if (var0.c().get("style") instanceof String var1) {
         return var1;
      } else {
         return !var0.b().equals("query") && !var0.b().equals("cookie") ? "simple" : "form";
      }
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.OpenApiDiffer.ParameterEntry var0, String var1) {
      return var0.c().get("explode") instanceof Boolean var2 ? var2 : var1.equals("form");
   }

   private void c(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      io.karatelabs.ext.openapi.changes.OpenApiDiffer.RequestBodySource var4 = this.h(var1);
      io.karatelabs.ext.openapi.changes.OpenApiDiffer.RequestBodySource var5 = this.h(var2);
      if ((var4 != null || var5 != null) && (var4 == null || var4.a() != null) && (var5 == null || var5.a() != null)) {
         String var6 = var3.a() + " request";
         String var7 = var1.k() + "/requestBody";
         String var8 = var2.k() + "/requestBody";
         String var9 = var4 == null ? null : var4.b();
         String var10 = var5 == null ? null : var5.b();
         if (var4 == null) {
            this.i
               .a(
                  Boolean.TRUE.equals(var5.a().get("required"))
                     ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_BODY_ADDED_REQUIRED
                     : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_BODY_ADDED_OPTIONAL,
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST,
                  var3
               )
               .a("requestBody", "requestBody")
               .b(var6)
               .c(var8, var10)
               .a();
         } else if (var5 == null) {
            this.i.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_BODY_REMOVED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST, var3).a("requestBody", "requestBody").b(var6).b(var7, var9).a();
         } else {
            boolean var11 = Boolean.TRUE.equals(var4.a().get("required"));
            boolean var12 = Boolean.TRUE.equals(var5.a().get("required"));
            if (var11 != var12) {
               this.i
                  .a(
                     var12 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_BODY_BECAME_REQUIRED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_BODY_BECAME_OPTIONAL,
                     io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST,
                     var3
                  )
                  .a("requestBody", "requestBody")
                  .b(var6)
                  .b(var7, var9)
                  .c(var8, var10)
                  .d(var9 + "/required", var10 + "/required")
                  .a();
            }

            this.a(
               var4.a().get("content"),
               var5.a().get("content"),
               var7,
               var8,
               var9,
               var10,
               io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST,
               var3,
               "request",
               var6,
               io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_MEDIA_TYPE_REMOVED,
               io.karatelabs.ext.openapi.changes.OpenApiChangeKind.REQUEST_MEDIA_TYPE_ADDED
            );
         }
      }
   }

   private io.karatelabs.ext.openapi.changes.OpenApiDiffer.RequestBodySource h(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1) {
      if (!var1.i().containsKey("requestBody")) {
         return null;
      } else {
         io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var2 = var1.d().a(var1.i().get("requestBody"), var1.l() + "/requestBody");
         return this.a(var2, "requestBody", var1.d()) && var2.b() instanceof Map var3 ? new io.karatelabs.ext.openapi.changes.OpenApiDiffer.RequestBodySource(var3, var2.c()) : new io.karatelabs.ext.openapi.changes.OpenApiDiffer.RequestBodySource(null, var2.c());
      }
   }

   private void a(Object var1, Object var2, String var3, String var4, String var5, String var6, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var7, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var8, String var9, String var10, OpenApiChangeKind var11, OpenApiChangeKind var12) {
      var var13 = this.a(var1, this.f, var5);
      var var14 = this.a(var2, this.g, var6);
      SchemaDiffWalker var15 = new SchemaDiffWalker(this.i, var7, var8);

      for (Entry var17 : (Iterable<Entry>) (Iterable<?>) (var13.entrySet())) {
         String var18 = (String)var17.getKey();
         String var19 = "/content/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var17.getValue());
         String var20 = (String)var14.get(var18);
         if (var20 == null) {
            this.i.a(var11, var7, var8).a("mediaType", var9 + " " + var18).b(var10 + " " + var18).b(var3 + var19, var5 + var19).a();
         } else {
            String var21 = "/content/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var20);
            Object var22 = ((Map)var1).get(var17.getValue());
            Object var23 = ((Map)var2).get(var20);
            if (!b((String)var17.getValue()).equals(b(var20))) {
               this.i.a("mediaType", var5 + var19, "both", "MEDIA_TYPE_PARAMS", (String)var17.getValue() + " -> " + var20);
            }

            if (var22 instanceof Map var24 && var24.containsKey("itemSchema")) {
               this.i.a("itemSchema", var5 + var19 + "/itemSchema", "baseline", "OAS_VERSION", null);
            }

            if (var23 instanceof Map var29 && var29.containsKey("itemSchema")) {
               this.i.a("itemSchema", var6 + var21 + "/itemSchema", "current", "OAS_VERSION", null);
            }

            if (!this.a(
               var22 instanceof Map var30 ? var30 : Map.of(),
               var23 instanceof Map var25 ? var25 : Map.of(),
               var7,
               var8,
               var9 + " " + var18,
               var10 + " " + var18,
               var3 + var19,
               var4 + var21,
               var5 + var19,
               var6 + var21
            )) {
               if (var22 instanceof Map var31 && var31.containsKey("encoding")) {
                  this.i.a("encoding", var5 + var19 + "/encoding", "baseline", "ENCODING", null);
               }

               if (var23 instanceof Map var32 && var32.containsKey("encoding")) {
                  this.i.a("encoding", var6 + var21 + "/encoding", "current", "ENCODING", null);
               }

               var15.a(
                  io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.f, var22 instanceof Map var33 ? var33.get("schema") : null, var5 + var19 + "/schema"),
                  io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.g, var23 instanceof Map var34 ? var34.get("schema") : null, var6 + var21 + "/schema"),
                  new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath(var9 + " " + var18, var10 + " " + var18, var3 + var19 + "/schema", var4 + var21 + "/schema", false, 0)
               );
            }
         }
      }

      for (Entry var27 : (Iterable<Entry>) (Iterable<?>) (var14.entrySet())) {
         if (!var13.containsKey(var27.getKey())) {
            String var28 = "/content/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var27.getValue());
            this.i
               .a(var12, var7, var8)
               .a("mediaType", var9 + " " + (String)var27.getKey())
               .b(var10 + " " + (String)var27.getKey())
               .c(var4 + var28, var6 + var28)
               .a();
         }
      }
   }

   private Map<String, String> a(Object var1, io.karatelabs.ext.openapi.changes.DiffDocument var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      if (var1 instanceof Map var5) {
         for (Object var7 : var5.keySet()) {
            String var8 = String.valueOf(var7);
            String var9 = (String)(var4.putIfAbsent(a(var8), var8));
            if (var9 != null) {
               this.i
                  .a(
                     "mediaType",
                     var3 + "/content/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var8),
                     var2.a,
                     "MEDIA_TYPE_PARAMS",
                     "a second key on " + a(var8) + " beside " + var9
                  );
            }
         }
      }

      return var4;
   }

   private static String a(String var0) {
      int var1 = var0.indexOf(59);
      return (var1 < 0 ? var0 : var0.substring(0, var1)).trim().toLowerCase(Locale.ROOT);
   }

   private static String b(String var0) {
      int var1 = var0.indexOf(59);
      return var1 < 0 ? "" : var0.substring(var1 + 1).replace(" ", "").toLowerCase(Locale.ROOT);
   }

   private boolean a(Map<?, ?> var1, Map<?, ?> var2, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var3, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var4, String var5, String var6, String var7, String var8, String var9, String var10) {
      boolean var11 = var1.containsKey("schema");
      boolean var12 = var2.containsKey("schema");
      if (var11 == var12) {
         return false;
      } else {
         io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var13 = this.i
            .a(var12 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SCHEMA_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.SCHEMA_REMOVED, var3, var4)
            .a("schema", var5)
            .b(var6);
         if (var11) {
            var13.b(var7 + "/schema", var9 + "/schema");
         } else {
            var13.c(var8 + "/schema", var10 + "/schema");
         }

         var13.a();
         return true;
      }
   }

   private void d(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var1, io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      var var4 = i(var1);
      var var5 = i(var2);

      for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         String var8 = (String)var7.getKey();
         String var9 = "/responses/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var7.getValue());
         String var10 = var3.a() + " response " + var8;
         String var11 = (String)var5.get(var8);
         io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var12 = this.f.a(((Map)var1.i().get("responses")).get(var7.getValue()), var1.l() + var9);
         if (var11 == null) {
            this.i
               .a(
                  var8.startsWith("2") ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_SUCCESS_STATUS_REMOVED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_STATUS_REMOVED,
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE,
                  var3
               )
               .a("response", var8)
               .b(var10)
               .b(var1.k() + var9, var12.c())
               .a();
         } else {
            String var13 = "/responses/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var11);
            io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var14 = this.g.a(((Map)var2.i().get("responses")).get(var11), var2.l() + var13);
            if (!(!this.a(var12, "response", this.f) | !this.a(var14, "response", this.g)) && var12.b() instanceof Map var15 && var14.b() instanceof Map var16) {
               this.a(
                  var15.get("content"),
                  var16.get("content"),
                  var1.k() + var9,
                  var2.k() + var13,
                  var12.c(),
                  var14.c(),
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE,
                  var3,
                  "response " + var8,
                  var10,
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_MEDIA_TYPE_REMOVED,
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_MEDIA_TYPE_ADDED
               );
               this.a(var15, var16, var1.k() + var9, var2.k() + var13, var12.c(), var14.c(), var3, var8, var10);
               if (var15.containsKey("links")) {
                  this.i.a("links", var12.c() + "/links", "baseline", "LINKS", null);
               }

               if (var16.containsKey("links")) {
                  this.i.a("links", var14.c() + "/links", "current", "LINKS", null);
               }
            }
         }
      }

      for (Entry var19 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
         if (!var4.containsKey(var19.getKey())) {
            String var20 = "/responses/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var19.getValue());
            io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var21 = this.g.a(((Map)var2.i().get("responses")).get(var19.getValue()), var2.l() + var20);
            this.i
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_STATUS_ADDED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var3)
               .a("response", (String)var19.getKey())
               .b(var3.a() + " response " + (String)var19.getKey())
               .c(var2.k() + var20, var21.c())
               .a();
         }
      }
   }

   private static Map<String, String> i(io.karatelabs.ext.openapi.changes.OpenApiDiffer.DiffOperation var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.i().get("responses") instanceof Map var2) {
         for (Object var4 : var2.keySet()) {
            String var5 = String.valueOf(var4);
            if (!var5.startsWith("x-")) {
               var1.putIfAbsent(var5.equalsIgnoreCase("default") ? "default" : var5.toUpperCase(Locale.ROOT), var5);
            }
         }
      }

      return var1;
   }

   private void a(Map<?, ?> var1, Map<?, ?> var2, String var3, String var4, String var5, String var6, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var7, String var8, String var9) {
      var var10 = f(var1.get("headers"));
      var var11 = f(var2.get("headers"));
      SchemaDiffWalker var12 = new SchemaDiffWalker(this.i, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7);

      for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
         String var15 = (String)var14.getKey();
         String var16 = "/headers/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var14.getValue());
         String var17 = var8 + " header " + var15;
         String var18 = var9 + " header " + var15;
         io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var19 = this.f.a(((Map)var1.get("headers")).get(var14.getValue()), var5 + var16);
         if (this.a(var19, "header", this.f) && var19.b() instanceof Map var20) {
            boolean var31 = Boolean.TRUE.equals(var20.get("required"));
            String var22 = (String)var11.get(var15);
            if (var22 == null) {
               this.i
                  .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_HEADER_REMOVED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7)
                  .a(var31 ? io.karatelabs.ext.diff.DiffSeverity.BREAKING : io.karatelabs.ext.diff.DiffSeverity.WARNING)
                  .a("header", var17)
                  .b(var18)
                  .b(var3 + var16, var19.c())
                  .c(var31 ? "required header removed" : "optional header removed")
                  .a();
            } else {
               String var23 = "/headers/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var22);
               io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var24 = this.g.a(((Map)var2.get("headers")).get(var22), var6 + var23);
               if (this.a(var24, "header", this.g) && var24.b() instanceof Map var25) {
                  boolean var32 = Boolean.TRUE.equals(var25.get("required"));
                  if (var31 && !var32) {
                     this.i
                        .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_HEADER_BECAME_OPTIONAL, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7)
                        .a("header", var17)
                        .b(var18)
                        .b(var3 + var16, var19.c())
                        .c(var4 + var23, var24.c())
                        .d(var19.c() + "/required", var24.c() + "/required")
                        .a();
                  } else if (!var31 && var32) {
                     this.i
                        .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_HEADER_BECAME_REQUIRED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7)
                        .a("header", var17)
                        .b(var18)
                        .b(var3 + var16, var19.c())
                        .c(var4 + var23, var24.c())
                        .d(var19.c() + "/required", var24.c() + "/required")
                        .a();
                  }

                  boolean var27 = false;
                  if (var20.containsKey("content")) {
                     this.i.a("content", var19.c() + "/content", "baseline", "PARAM_CONTENT", null);
                     var27 = true;
                  }

                  if (var25.containsKey("content")) {
                     this.i.a("content", var24.c() + "/content", "current", "PARAM_CONTENT", null);
                     var27 = true;
                  }

                  if (!var27 && !this.a(var20, var25, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7, "response " + var17, var18, var3 + var16, var4 + var23, var19.c(), var24.c())) {
                     var12.a(
                        io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.f, var20.get("schema"), var19.c() + "/schema"),
                        io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.g, var25.get("schema"), var24.c() + "/schema"),
                        new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath("response " + var17, var18, var3 + var16 + "/schema", var4 + var23 + "/schema", false, 0)
                     );
                  }
               }
            }
         }
      }

      for (Entry var29 : (Iterable<Entry>) (Iterable<?>) (var11.entrySet())) {
         if (!var10.containsKey(var29.getKey())) {
            String var30 = "/headers/" + io.karatelabs.ext.openapi.changes.DiffDocument.c((String)var29.getValue());
            this.i
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.RESPONSE_HEADER_ADDED, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE, var7)
               .a("header", var8 + " header " + (String)var29.getKey())
               .b(var9 + " header " + (String)var29.getKey())
               .c(var4 + var30, var6 + var30)
               .a();
         }
      }
   }

   private static Map<String, String> f(Object var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 instanceof Map var2) {
         for (Object var4 : var2.keySet()) {
            String var5 = String.valueOf(var4).toLowerCase(Locale.ROOT);
            if (!var5.equals("content-type")) {
               var1.putIfAbsent(var5, String.valueOf(var4));
            }
         }
      }

      return var1;
   }

   private Map<String, Object> a(boolean var1) {
      io.karatelabs.ext.diff.DiffReport var2 = new io.karatelabs.ext.diff.DiffReport("openapi.diff/1", "karate-openapi-diff", ExtMeta.version("karate-max-meta.properties"));
      CoverageJoinIndex var3 = new CoverageJoinIndex(this.h.b(), this.h.c(), this.h.e(), this.h.f(), this.h.g());

      for (io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var5 : this.i.d) {
         io.karatelabs.ext.diff.DiffFinding var6 = var5.a(var2, var3, this.m);
         var2.a(var6);
         this.n.a(var5, var6.identifier());
      }

      this.i.a().forEach(var2::a);
      if (this.m != null) {
         var2.a(this.o.toString()).a(this.m.report());
      }

      return var2.a(b(this.f), b(this.g)).a(var3.a()).a(var1).a("components", this.n.a(var3)).toMap();
   }

   private io.karatelabs.ext.lint.WaiverLedger c() {
      Object var1 = this.h.d();
      String var2 = null;
      if (var1 instanceof String var3) {
         var2 = var3;

         try {
            Path var4 = this.h.e() == null ? Path.of(var3) : this.h.e().resolve(var3);
            var1 = io.karatelabs.ext.sourcedoc.SourceDocument.parse(var3, Files.readString(var4)).value();
         } catch (RuntimeException | IOException var8) {
            throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var3, String.valueOf(var8.getMessage()));
         }

         if (var1 == null) {
            var1 = List.of();
         }
      }

      if (var1 == null) {
         return null;
      } else {
         ArrayList var10 = new ArrayList();

         for (OpenApiChangeKind var7 : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.values()) {
            var10.add(var7.name());
         }

         try {
            return io.karatelabs.ext.lint.WaiverLedger.create(var1, var2, "openapi.diff/1", var10, this.o);
         } catch (io.karatelabs.ext.lint.LintException var9) {
            throw io.karatelabs.ext.openapi.changes.OpenApiDiffException.a(var2 == null ? "inline" : var2, var9.getMessage());
         }
      }
   }

   private static io.karatelabs.ext.diff.DiffReport.DiffArtifact b(io.karatelabs.ext.openapi.changes.DiffDocument var0) {
      return new io.karatelabs.ext.diff.DiffReport.DiffArtifact(var0.b, var0.f, "file");
   }

   public static String a(Map<String, Object> var0) {
      return io.karatelabs.ext.diff.DiffReport.b("Openapi.diff", var0);
   }

   static LocalDate a(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = String.valueOf(var0).trim();

         try {
            return LocalDate.parse(var1);
         } catch (RuntimeException var5) {
            try {
               return OffsetDateTime.parse(var1).withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
            } catch (RuntimeException var4) {
               return null;
            }
         }
      }
   }

   private static String g(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private record RequestBodySource(Map<?, ?> a, String b) {

   }

   private record DiffOperation(io.karatelabs.ext.openapi.changes.DiffDocument d, String e, String f, String g, List<String> h, Map<String, Object> i, Map<String, Object> j, String k, String l, String m, String n, boolean o) {


      String a() {
         String var1 = this.e.toUpperCase(Locale.ROOT);
         return this.o ? "webhook " + this.f + " " + var1 : var1 + " " + this.g;
      }

      String b() {
         return this.i.get("operationId") instanceof String var1 && !var1.isEmpty() ? var1 : null;
      }

      String c() {
         String var1 = this.i.get("operationId") instanceof String var2 ? var2 : null;
         return !this.o || var1 != null && !var1.isBlank() ? io.karatelabs.ext.openapi.OpenApiContractLoader.a(var1, this.e, this.f) : this.e + "_webhook_" + this.f;
      }

      public io.karatelabs.ext.openapi.changes.DiffDocument d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public List<String> h() {
         return this.h;
      }

      public Map<String, Object> i() {
         return this.i;
      }

      public Map<String, Object> j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public String l() {
         return this.l;
      }

      public String m() {
         return this.m;
      }

      public String n() {
         return this.n;
      }

      public boolean o() {
         return this.o;
      }
   }

   public record OpenApiDiffOptions(LocalDate a, String b, String c, Object d, Path e, String f, String g) {

      public OpenApiDiffOptions(LocalDate var1, String var2, String var3) {
         this(var1, var2, var3, null, null, null, null);
      }


      public static io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions a(Map<?, ?> var0) {
         if (var0 == null) {
            return new io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions(null, null, null);
         } else {
            LocalDate var1 = null;
            if (var0.get("asOf") != null) {
               var1 = io.karatelabs.ext.openapi.changes.OpenApiDiffer.a(var0.get("asOf"));
               if (var1 == null) {
                  throw new IllegalArgumentException("`asOf` must be an ISO date, e.g. '2026-09-01'");
               }
            }

            return new io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions(
               var1,
               io.karatelabs.ext.openapi.changes.OpenApiDiffer.g(var0.get("reportDir")),
               io.karatelabs.ext.openapi.changes.OpenApiDiffer.g(var0.get("baselineReportDir")),
               var0.get("waivers"),
               null,
               null,
               null
            );
         }
      }

      public io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions a(Path var1, String var2, String var3, String var4) {
         return new io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions(this.a, var2, this.c, this.d, var1, var3, var4);
      }
   }

   private record ParameterEntry(String a, String b, Map<String, Object> c, String d, String e) {

   }

   private record SuccessorResolution(Map<String, Object> a, String b) {

   }
}
