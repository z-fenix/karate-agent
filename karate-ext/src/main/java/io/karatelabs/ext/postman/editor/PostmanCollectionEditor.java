package io.karatelabs.ext.postman.editor;

import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.jsonschema.validation.CompiledSchema;
import io.karatelabs.ext.jsonschema.validation.ValidationResult;
import io.karatelabs.js.JsonParser;
import io.karatelabs.js.JsonSpan;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostmanCollectionEditor {
   public static final String a = "META-INF/karate/schemas/postman-collection-v2.1.0.json";
   static final Set<String> b = Set.of("noauth", "basic", "bearer", "apikey", "oauth2");
   static final Set<String> c = Set.of("query", "variable", "header", "urlencoded", "formdata", "collection");
   private static final Pattern d = Pattern.compile("‹secret:[^›]*›");
   private static final Pattern e = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*)://");
   private static final Pattern f = Pattern.compile("^((?:/item/\\d+)*/event/\\d+)(?:/|$)");

   private PostmanCollectionEditor() {
   }

   public static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var0, String var1, List<?> var2, String var3) {
      io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult var4 = a(var0, var1, var3);
      if (var4 != null) {
         return var4;
      } else if (var2 == null) {
         return a("BAD_REQUEST", 400, "edits is a list of edit entries — Postman.help('edit')");
      } else if (a(var2)) {
         return a(
            "NOT_EDITABLE",
            409,
            "an edit carries a masked value (‹masked› or ‹secret:…›) — it reads as masked because the file's value is a secret; the file keeps its value, and that field is not editable here"
         );
      } else {
         io.karatelabs.ext.postman.editor.JsonSourceEditor var5 = new io.karatelabs.ext.postman.editor.JsonSourceEditor(var0);
         io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier var6 = new io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier(var5);

         try {
            for (Object var8 : var2) {
               if (!(var8 instanceof Map var9)) {
                  return a("BAD_REQUEST", 400, "each edit is an object, got " + var8);
               }

               var6.a(var9);
            }
         } catch (io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException var10) {
            return a(var10.a(), a(var10.a()), "edit wrote nothing — " + var10.getMessage());
         } catch (RuntimeException var11) {
            return a("UNINTENDED_CHANGE", 409, "edit wrote nothing — the spliced text does not reparse: " + var11.getMessage());
         }

         Object var12 = var5.b().root().toValue();
         if (!io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var12, var5.c())) {
            return a("UNINTENDED_CHANGE", 409, "edit wrote nothing — the reparse is not the ops applied in memory");
         } else {
            return !var5.f()
               ? a("UNINTENDED_CHANGE", 409, "edit wrote nothing — bytes outside the edited spans would change")
               : a(var0, var5, var0.h(), var12, var6.b);
         }
      }
   }

   public static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var0, String var1, String var2, String var3) {
      io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult var4 = a(var0, var1, var3);
      if (var4 != null) {
         return var4;
      } else {
         Map var5;
         try {
            var5 = (Map)JsonParser.parse(new String(Base64.getUrlDecoder().decode(var2), StandardCharsets.UTF_8));
         } catch (RuntimeException var11) {
            return a("BAD_REQUEST", 400, "undo is the token an earlier edit answered");
         }

         if (!var0.c().equals(var5.get("v"))) {
            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("expectedVersion", var5.get("v"));
            var12.put("currentVersion", var0.c());
            return a("STALE", 409, "the collection changed since that edit — its undo no longer applies", var12);
         } else {
            ArrayList var6 = new ArrayList();

            for (Object var8 : (List)var5.get("s")) {
               List var9 = (List)var8;
               var6.add(new io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit(((Number)var9.get(0)).intValue(), ((Number)var9.get(1)).intValue(), (String)var9.get(2)));
            }

            io.karatelabs.ext.postman.editor.JsonSourceEditor var13 = new io.karatelabs.ext.postman.editor.JsonSourceEditor(var0);

            try {
               var13.a(var6);
            } catch (io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException var10) {
               return a(var10.a(), a(var10.a()), "undo wrote nothing — " + var10.getMessage());
            }

            byte[] var14 = var13.a().getBytes(StandardCharsets.UTF_8);
            return !io.karatelabs.ext.common.Sha256.a(var14).equals(var5.get("r"))
               ? a("UNINTENDED_CHANGE", 409, "undo wrote nothing — the restored bytes are not the version before the edit")
               : a(var0, var13, var0.h(), var13.c(), List.of());
         }
      }
   }

   private static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(
      io.karatelabs.ext.postman.editor.PostmanCollectionDocument var0, io.karatelabs.ext.postman.editor.JsonSourceEditor var1, Object var2, Object var3, List<String> var4
   ) {
      var var5 = io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var2, var3);
      ValidationResult var6 = a().validate(var2);
      ValidationResult var7 = a().validate(var3);
      List var8 = var7.valid ? List.of() : var7.message().lines().limit(5L).toList();
      if (var6.valid && !var7.valid) {
         return a(
            "INVALID_COLLECTION", 422, "edit wrote nothing — the saved collection would no longer be valid against the v2.1 schema: " + (String)var8.get(0)
         );
      } else {
         byte[] var9 = var1.a().getBytes(StandardCharsets.UTF_8);
         String var10 = io.karatelabs.ext.common.Sha256.a(var9);
         String var11 = var1.e() ? a(var10, var0.c(), var1.d()) : null;
         ArrayList var12 = new ArrayList();

         for (String var14 : (Iterable<String>) (Iterable<?>) (var5)) {
            LinkedHashMap var15 = new LinkedHashMap();
            var15.put("path", var14);
            Object[] var16 = a(var2, var14);
            Object[] var17 = a(var3, var14);
            if (var16 != null) {
               var15.put("before", var16[0]);
            }

            if (var17 != null) {
               var15.put("after", var17[0]);
            }

            Matcher var18 = f.matcher(var14);
            if (var18.find()) {
               Object[] var19 = a(var17 != null ? var3 : var2, var18.group(1));
               if (var19 != null && var19[0] instanceof Map var20 && var20.get("listen") != null) {
                  var15.put("listen", String.valueOf(var20.get("listen")));
               }
            }

            var12.add(var15);
         }

         return new io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult(null, var9, var10, var0.c(), var5, var12, List.copyOf(var4), var7.valid, var8, var11);
      }
   }

   private static Object[] a(Object var0, String var1) {
      Object var2 = var0;

      for (String var4 : io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var1)) {
         if (!(var2 instanceof Map var6) || !var6.containsKey(var4)) {
            if (!(var2 instanceof List var5) || !var4.matches("\\d{1,9}") || Integer.parseInt(var4) >= var5.size()) {
               return null;
            }

            var2 = var5.get(Integer.parseInt(var4));
         } else {
            var2 = var6.get(var4);
         }
      }

      return new Object[]{var2};
   }

   private static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var0, String var1, String var2) {
      if (var2 == null || var2.isBlank()) {
         return a("BAD_REQUEST", 400, "every edit carries ifMatch — the version the read answered");
      } else if (!var2.equals(var0.c())) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("expectedVersion", var2);
         var4.put("currentVersion", var0.c());
         return a("STALE", 409, "the collection changed since it was read — read it again and redo the edit", var4);
      } else if (!var0.f()) {
         return a("NOT_EDITABLE", 409, "the collection is not editable: " + var0.g());
      } else {
         String var3 = var0.b(var1);
         return var3 != null ? a("NOT_COLLECTION", 400, "not a Postman collection: " + var3) : null;
      }
   }

   public static boolean a(Object var0) {
      if (!(var0 instanceof String var1)) {
         if (var0 instanceof Map var3) {
            return var3.values().stream().anyMatch(io.karatelabs.ext.postman.editor.PostmanCollectionEditor::a);
         } else {
            return var0 instanceof List var2 ? var2.stream().anyMatch(io.karatelabs.ext.postman.editor.PostmanCollectionEditor::a) : false;
         }
      } else {
         return var1.contains("‹masked›") || d.matcher(var1).find();
      }
   }

   public static Object b(Object var0) {
      if (var0 instanceof Map var1) {
         if (Boolean.TRUE.equals(var1.get("editable")) && ((Map<?, ?>)var1).entrySet().stream().anyMatch(var0x -> !"editable".equals(var0x.getKey()) && a(var0x.getValue()))
            )
          {
            var1.put("editable", false);
            var1.put("reason", "holds a secret");
         }

         var1.values().forEach(io.karatelabs.ext.postman.editor.PostmanCollectionEditor::b);
      } else if (var0 instanceof List var2) {
         var2.forEach(io.karatelabs.ext.postman.editor.PostmanCollectionEditor::b);
      }

      return var0;
   }

   public static String c(Object var0) {
      if (var0 instanceof Map var1 && !io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier.a((Object)var1)) {
         return "oauth2".equals(var1.get("type"))
            ? "the editor models oauth2 client credentials only"
            : "the editor does not model " + var1.get("type") + " auth";
      } else {
         return null;
      }
   }

   public static CompiledSchema a() {
      return io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanCollectionSchema.a;
   }

   private static String a(String var0, String var1, List<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> var2) {
      ArrayList var3 = new ArrayList();

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var5 : var2) {
         var3.add(List.of(var5.a(), var5.b(), var5.c()));
      }

      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("v", var0);
      var6.put("r", var1);
      var6.put("s", var3);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var6).getBytes(StandardCharsets.UTF_8));
   }

   static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(String var0, int var1, String var2) {
      return a(var0, var1, var2, null);
   }

   static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult a(String var0, int var1, String var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("error", var2);
      var4.put("code", var0);
      var4.put("status", var1);
      if (var3 != null) {
         var4.put("detail", var3);
      }

      return new io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult(var4, null, null, null, null, null, null, false, null, null);
   }

   private static int a(String var0) {
      return switch (var0) {
         case "NOT_FOUND" -> 404;
         case "BAD_REQUEST" -> 400;
         default -> 409;
      };
   }

   static final class CollectionEditApplier {
      final io.karatelabs.ext.postman.editor.JsonSourceEditor a;
      final List<String> b = new ArrayList<>();

      CollectionEditApplier(io.karatelabs.ext.postman.editor.JsonSourceEditor var1) {
         this.a = var1;
      }

      void a(Map<?, ?> var1) {
         if (var1.get("ops") == null) {
            if (var1.get("item") instanceof String var6) {
               JsonSpan var9 = this.e(var6);
               if (var9 != null && var9.kind() == JsonSpan.Kind.OBJECT) {
                  boolean var10 = var6.isEmpty();
                  boolean var5 = false;
                  if (var1.containsKey("name")) {
                     this.d(var10 ? "/info/name" : io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var6, "name"), var1.get("name"));
                     var5 = true;
                  }

                  if (var1.containsKey("description")) {
                     this.a(var10 ? "/info/description" : this.a(var6, var9), var1.get("description"));
                     var5 = true;
                  }

                  if (var1.containsKey("method")) {
                     this.b(var6, a(var1.get("method"), "method"));
                     var5 = true;
                  }

                  if (var1.containsKey("url")) {
                     this.c(var6, a(var1.get("url"), "url"));
                     var5 = true;
                  }

                  if (var1.containsKey("rows")) {
                     this.a(var6, var1);
                     var5 = true;
                  }

                  if (var1.containsKey("body")) {
                     this.c(var6, b(var1.get("body"), "body"));
                     var5 = true;
                  }

                  if (var1.containsKey("auth")) {
                     this.b(var6, var1.get("auth"));
                     var5 = true;
                  }

                  if (var1.containsKey("script")) {
                     this.d(var6, b(var1.get("script"), "script"));
                     var5 = true;
                  }

                  if (!var5) {
                     throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException(
                        "BAD_REQUEST", "an edit entry carries name, description, method, url, rows, body, auth, script or ops — Postman.help('edit')"
                     );
                  }
               } else {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no item at " + (var6.isEmpty() ? "''" : var6) + " — Postman.read lists them");
               }
            } else {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException(
                  "BAD_REQUEST", "an edit names its item by JSON pointer (item: '' is the collection) — Postman.help('edit')"
               );
            }
         } else {
            Object var3 = var1.get("ops");
            if (!(var3 instanceof List)) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "ops is a list of {op, path, value?}");
            } else {
               for (Object var4 : (List)var3) {
                  this.b(var4);
               }
            }
         }
      }

      private void b(Object var1) {
         if (var1 instanceof Map var2 && var2.get("op") instanceof String var3 && var2.get("path") instanceof String var4) {
            this.a(var4);
            this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation(var3, var4, var2.get("value")));
         } else {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "an op is {op: add|replace|remove, path, value?}, got " + var1);
         }
      }

      private void a(String var1) {
         var var2 = io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var1);
         JsonSpan var3 = this.a.b().root();

         for (int var4 = 0; var4 < var2.size() && var3 != null; var4++) {
            String var5 = (String)var2.get(var4);
            if (var3.kind() == JsonSpan.Kind.OBJECT) {
               JsonSpan var6 = null;

               for (JsonSpan.Member var8 : var3.members()) {
                  if (var8.key().equals(var5)) {
                     var6 = var8.value();
                  }
               }

               String var10 = io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var2.subList(0, var4 + 1));
               if ("protocolProfileBehavior".equals(var5)) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var10 + " is protocolProfileBehavior, which is kept as it is");
               }

               if ("response".equals(var5) && a(var3, "request")) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var10 + " is a saved response example, which is kept as it is");
               }

               if ("script".equals(var5) && var6 != null && var6.kind() == JsonSpan.Kind.STRING) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var10 + " is a script given by reference, which is kept as it is");
               }

               if ("auth".equals(var5) && var6 != null && var6.kind() == JsonSpan.Kind.OBJECT && !a(var6.toValue())) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException(
                     "NOT_EDITABLE", var10 + " is a " + ((Map)var6.toValue()).get("type") + " auth, which the editor does not model and keeps as it is"
                  );
               }

               var3 = var6;
            } else if (var3.kind() == JsonSpan.Kind.ARRAY) {
               int var9 = var5.matches("0|[1-9][0-9]*") ? Integer.parseInt(var5) : -1;
               var3 = var9 >= 0 && var9 < var3.elements().size() ? var3.elements().get(var9) : null;
            } else {
               var3 = null;
            }
         }
      }

      private String a(String var1, JsonSpan var2) {
         return a(var2, "request") && !a(var2, "description") && this.d(var1).kind() == JsonSpan.Kind.OBJECT
            ? var1 + "/request/description"
            : io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "description");
      }

      private void a(String var1, Object var2) {
         JsonSpan var3 = this.e(var1);
         if (var3 != null && var3.kind() == JsonSpan.Kind.OBJECT && a(var3, "content")) {
            this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "content"), var2);
         } else {
            this.d(var1, var2);
         }
      }

      private void b(String var1, String var2) {
         JsonSpan var3 = this.d(var1);
         if (var3.kind() == JsonSpan.Kind.STRING) {
            this.a(var1, var3, var1x -> var1x.put("method", var2));
         } else {
            this.d(var1 + "/request/method", var2);
         }
      }

      private void c(String var1, String var2) {
         JsonSpan var3 = this.d(var1);
         if (var3.kind() == JsonSpan.Kind.STRING) {
            this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "request"), var2);
         } else {
            String var4 = var1 + "/request/url";
            JsonSpan var5 = this.e(var4);
            if (var5 == null || var5.kind() == JsonSpan.Kind.STRING) {
               this.d(var4, var2);
            } else if (var5.kind() != JsonSpan.Kind.OBJECT) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var4 + " is neither a string nor an object");
            } else {
               io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl var6 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a(var2);
               Map var7 = (Map)var5.toValue();
               if (!var2.equals(var7.get("raw"))) {
                  this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, "raw"), var2);
               }

               this.a(var4, "protocol", var6.c(), var7);
               this.a(var4, "port", var6.f(), var7);
               this.a(var4, "hash", var6.j(), var7);
               this.a(var4, "host", var6.e(), ".", var7);
               this.a(var4, "path", var6.h(), "/", var7);
               this.a(var4, var6.i(), var7);
               this.b(var4, var6.h(), var7);
            }
         }
      }

      private void a(String var1, String var2, String var3, Map<?, ?> var4) {
         if (var3 == null) {
            if (var4.containsKey(var2)) {
               this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, var2), null));
            }
         } else if (!var3.equals(var4.get(var2))) {
            this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, var2), var3);
         }
      }

      private void a(String var1, String var2, List<String> var3, String var4, Map<?, ?> var5) {
         Object var6 = var5.get(var2);
         if (var6 instanceof String var7) {
            String var8 = String.join(var4, var3);
            if (!var8.equals(var7)) {
               this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, var2), var8);
            }
         } else if ((var6 instanceof List || !var3.isEmpty()) && !var3.equals(var6)) {
            this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, var2), var3);
         }
      }

      private void a(String var1, List<String[]> var2, Map<?, ?> var3) {
         String var4 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "query");
         if (var3.get("query") instanceof List var5) {
            ArrayList var12 = new ArrayList();

            for (int var13 = 0; var13 < var5.size(); var13++) {
               if (!(var5.get(var13) instanceof Map var15 && Boolean.TRUE.equals(var15.get("disabled")))) {
                  var12.add(var13);
               }
            }

            int var14 = Math.min(var12.size(), var2.size());

            for (int var16 = 0; var16 < var14; var16++) {
               Map var19 = (Map)var5.get((Integer)var12.get(var16));
               String var10 = var4 + "/" + var12.get(var16);
               if (!((String[])var2.get(var16))[0].equals(var19.get("key"))) {
                  this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var10, "key"), ((String[])var2.get(var16))[0]);
               }

               if (!Objects.equals(((String[])var2.get(var16))[1], var19.get("value"))) {
                  this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var10, "value"), ((String[])var2.get(var16))[1]);
               }
            }

            for (int var17 = var12.size() - 1; var17 >= var14; var17--) {
               this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var4 + "/" + var12.get(var17), null));
            }

            for (int var18 = var14; var18 < var2.size(); var18++) {
               this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var4 + "/-", a((String[])var2.get(var18))));
            }
         } else {
            if (!var2.isEmpty()) {
               ArrayList var11 = new ArrayList();

               for (String[] var8 : var2) {
                  var11.add(a(var8));
               }

               this.d(var4, var11);
            }
         }
      }

      private static Map<String, Object> a(String[] var0) {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("key", var0[0]);
         var1.put("value", var0[1]);
         return var1;
      }

      private void b(String var1, List<String> var2, Map<?, ?> var3) {
         ArrayList var4 = new ArrayList();

         for (String var6 : var2) {
            if (var6.startsWith(":") && var6.length() > 1) {
               var4.add(var6.substring(1));
            }
         }

         String var13 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "variable");
         if (var3.get("variable") instanceof List var14) {
            ArrayList var16 = new ArrayList();

            for (Object var20 : var14) {
               var16.add(var20 instanceof Map var11 && var11.get("key") instanceof String var10 ? var10 : null);
            }

            for (int var18 = var14.size() - 1; var18 >= 0; var18--) {
               if (!var4.contains(var16.get(var18))) {
                  this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var13 + "/" + var18, null));
               }
            }

            for (String var21 : (Iterable<String>) (Iterable<?>) (var4)) {
               if (!var16.contains(var21)) {
                  this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var13 + "/-", b(var21)));
               }
            }
         } else {
            if (!var4.isEmpty()) {
               ArrayList var15 = new ArrayList();

               for (String var9 : (Iterable<String>) (Iterable<?>) (var4)) {
                  var15.add(b(var9));
               }

               this.d(var13, var15);
            }
         }
      }

      private static Map<String, Object> b(String var0) {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("key", var0);
         var1.put("value", "");
         return var1;
      }

      private void a(String var1, Map<?, ?> var2) {
         String var3 = a(var2.get("rows"), "rows");
         if (!io.karatelabs.ext.postman.editor.PostmanCollectionEditor.c.contains(var3)) {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "rows is query, variable, header, urlencoded, formdata or collection, not " + var3);
         } else {
            boolean var4 = Boolean.TRUE.equals(var2.get("remove"));
            Object var5 = var2.get("row");
            boolean var6 = "end".equals(var5);
            if (var6 || var5 instanceof Number var7 && var7.intValue() >= 0) {
               if (var6 && var4) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "remove names a row by index");
               } else {
                  Map var11 = var4 ? Map.of() : d(b(var2.get("set"), "set"));
                  if (var6 && !(var11.get("key") instanceof String)) {
                     throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "a new row needs set.key");
                  } else {
                     if (var6 && !var11.containsKey("value") && !var11.containsKey("src")) {
                        var11.put("value", "");
                     }

                     int var8 = var6 ? -1 : ((Number)var5).intValue();
                     switch (var3) {
                        case "collection":
                           this.a(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "variable"), var8, var11, var4, null);
                           break;
                        case "header":
                           this.a(var1, var8, var11, var4);
                           break;
                        case "query":
                        case "variable":
                           this.b(var1, var3, var8, var11, var4);
                           break;
                        default:
                           this.c(var1, var3, var8, var11, var4);
                     }
                  }
               }
            } else {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "row is the row's index, or 'end' to add one");
            }
         }
      }

      private void a(String var1, int var2, Map<String, Object> var3, boolean var4) {
         JsonSpan var5 = this.d(var1);
         if (var5.kind() == JsonSpan.Kind.STRING) {
            if (var2 >= 0) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "the request is a URL string and has no header rows");
            } else {
               this.a(var1, var5, var1x -> ((List)var1x.get("header")).add(var3));
            }
         } else {
            String var6 = var1 + "/request/header";
            JsonSpan var7 = this.e(var6);
            if (var7 != null && var7.kind() == JsonSpan.Kind.STRING) {
               this.a(var6, (String)var7.value(), var2, var3, var4);
            } else {
               this.a(var6, var2, var3, var4, null);
            }
         }
      }

      private void a(String var1, String var2, int var3, Map<String, Object> var4, boolean var5) {
         String var6 = var2.contains("\r\n") ? "\r\n" : "\n";
         boolean var7 = var2.endsWith("\n");
         var var8 = new ArrayList<>(Arrays.asList((var7 ? var2.substring(0, var2.length() - var6.length()) : var2).split("\r?\n", -1)));
         if (var3 >= var8.size()) {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no header row " + var3 + " — there are " + var8.size());
         } else {
            boolean var9 = var4.keySet().stream().allMatch(var0 -> var0.equals("key") || var0.equals("value"));
            if (var9) {
               if (var5) {
                  var8.remove(var3);
               } else if (var3 < 0) {
                  var8.add(var4.get("key") + ": " + var4.get("value"));
               } else {
                  String[] var15 = ((String)var8.get(var3)).split(":", 2);
                  String var16 = var4.containsKey("key") ? String.valueOf(var4.get("key")) : var15[0].strip();
                  String var17 = var4.containsKey("value") ? String.valueOf(var4.get("value")) : (var15.length > 1 ? var15[1].strip() : "");
                  var8.set(var3, var16 + ": " + var17);
               }

               this.d(var1, String.join(var6, var8) + (var7 && !var8.isEmpty() ? var6 : ""));
            } else {
               ArrayList var10 = new ArrayList();

               for (String var12 : (Iterable<String>) (Iterable<?>) (var8)) {
                  String[] var13 = var12.split(":", 2);
                  LinkedHashMap var14 = new LinkedHashMap();
                  var14.put("key", var13[0].strip());
                  var14.put("value", var13.length > 1 ? var13[1].strip() : "");
                  var10.add(var14);
               }

               if (var3 < 0) {
                  var10.add(var4);
               } else {
                  ((Map)var10.get(var3)).putAll(var4);
               }

               this.d(var1, var10);
               this.b.add(var1);
            }
         }
      }

      private void b(String var1, String var2, int var3, Map<String, Object> var4, boolean var5) {
         JsonSpan var6 = this.d(var1);
         if (var6.kind() == JsonSpan.Kind.STRING) {
            this.a(var1, var6, var5x -> {
               var var6x = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a((String)var5x.get("url")).b();
               this.a(var6x, var2, var3, var4, var5);
               var5x.put("url", var6x);
            });
         } else {
            String var7 = var1 + "/request/url";
            JsonSpan var8 = this.e(var7);
            if (var8 == null) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "the request has no url");
            } else if (var8.kind() != JsonSpan.Kind.STRING) {
               this.a(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var7, var2), var3, var4, var5, "query".equals(var2) ? var7 : null);
            } else {
               boolean var9 = "query".equals(var2) && var4.keySet().stream().allMatch(var0 -> var0.equals("key") || var0.equals("value"));
               if (var9) {
                  io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl var13 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a((String)var8.value());
                  var var11 = new ArrayList<>(var13.i());
                  if (var3 >= var11.size()) {
                     throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no query row " + var3 + " — the URL has " + var11.size());
                  } else {
                     if (var5) {
                        var11.remove(var3);
                     } else if (var3 < 0) {
                        var11.add(new String[]{(String)var4.get("key"), var4.get("value") == null ? null : String.valueOf(var4.get("value"))});
                     } else {
                        String[] var12 = (String[])var11.get(var3);
                        var11.set(
                           var3,
                           new String[]{
                              var4.containsKey("key") ? String.valueOf(var4.get("key")) : var12[0],
                              var4.containsKey("value") ? String.valueOf(var4.get("value")) : var12[1]
                           }
                        );
                     }

                     this.d(var7, var13.a(var11).a());
                  }
               } else {
                  var var10 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a((String)var8.value()).b();
                  this.a(var10, var2, var3, var4, var5);
                  this.d(var7, var10);
                  this.b.add(var7);
               }
            }
         }
      }

      private void a(Map<String, Object> var1, String var2, int var3, Map<String, Object> var4, boolean var5) {
         List var6 = (List)var1.computeIfAbsent(var2, var0 -> new ArrayList());
         if (var3 >= var6.size()) {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no " + var2 + " row " + var3 + " — there are " + var6.size());
         } else {
            if (var5) {
               var6.remove(var3);
            } else if (var3 < 0) {
               var6.add(var4);
            } else {
               ((Map)var6.get(var3)).putAll(var4);
            }

            if ("query".equals(var2)) {
               var1.put("raw", io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a((String)var1.get("raw")).a(a(var6)).a());
            }
         }
      }

      private void c(String var1, String var2, int var3, Map<String, Object> var4, boolean var5) {
         JsonSpan var6 = this.d(var1);
         if (var6.kind() == JsonSpan.Kind.STRING) {
            if (var3 >= 0) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "the request is a URL string and has no body rows");
            } else {
               this.a(var1, var6, var2x -> var2x.put("body", b(var2, var4)));
            }
         } else {
            String var7 = var1 + "/request/body";
            if (this.e(var7) == null) {
               if (var3 >= 0) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "the request has no body");
               } else {
                  this.d(var7, b(var2, var4));
               }
            } else {
               this.a(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var7, var2), var3, var4, var5, null);
            }
         }
      }

      private static Map<String, Object> b(String var0, Map<String, Object> var1) {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("mode", var0);
         var2.put(var0, new ArrayList<>(List.of(var1)));
         return var2;
      }

      private void a(String var1, int var2, Map<String, Object> var3, boolean var4, String var5) {
         JsonSpan var6 = this.e(var1);
         if (var6 == null) {
            if (var2 >= 0) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no rows at " + var1);
            }

            this.d(var1, new ArrayList<>(List.of(var3)));
         } else {
            if (var6.kind() != JsonSpan.Kind.ARRAY) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var1 + " is not an array");
            }

            int var7 = var6.elements().size();
            if (var2 >= var7) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no row " + var2 + " at " + var1 + " — there are " + var7);
            }

            if (var4) {
               this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var1 + "/" + var2, null));
            } else if (var2 < 0) {
               this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var1 + "/-", var3));
            } else {
               String var8 = var1 + "/" + var2;

               for (Entry var10 : var3.entrySet()) {
                  String var11 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var8, (String)var10.getKey());
                  if (var10.getValue() == null) {
                     if (this.e(var11) != null) {
                        this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var11, null));
                     }
                  } else {
                     this.d(var11, var10.getValue());
                  }
               }
            }
         }

         if (var5 != null) {
            this.c(var5);
         }
      }

      private void c(String var1) {
         JsonSpan var2 = this.e(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "raw"));
         if (var2 != null && var2.kind() == JsonSpan.Kind.STRING) {
            Object var3 = this.f(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "query"));
            String var4 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl.a((String)var2.value()).a(a(var3 instanceof List var5 ? var5 : List.of())).a();
            if (!var4.equals(var2.value())) {
               this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "raw"), var4);
            }
         }
      }

      private static List<String[]> a(List<?> var0) {
         ArrayList var1 = new ArrayList();

         for (Object var3 : var0) {
            if (var3 instanceof Map var4 && !Boolean.TRUE.equals(var4.get("disabled")) && var4.get("key") != null) {
               var1.add(new String[]{String.valueOf(var4.get("key")), var4.get("value") == null ? null : String.valueOf(var4.get("value"))});
            }
         }

         return var1;
      }

      private void c(String var1, Map<?, ?> var2) {
         JsonSpan var3 = this.d(var1);
         if (var3.kind() == JsonSpan.Kind.STRING) {
            this.a(var1, var3, var1x -> var1x.put("body", b(var2)));
         } else {
            String var4 = var1 + "/request/body";
            if (this.e(var4) == null) {
               this.d(var4, b(var2));
            } else {
               if (var2.containsKey("mode")) {
                  this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, "mode"), a(var2.get("mode"), "body.mode"));
               }

               if (var2.containsKey("raw")) {
                  JsonSpan var5 = this.e(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, "raw"));
                  String var6 = a(var2.get("raw"), "body.raw");
                  this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, "raw"), var5 != null && var5.value() instanceof String var7 ? a(var7, var6) : var6);
               }

               if (var2.containsKey("language")) {
                  this.a(var4, List.of("options", "raw", "language"), a(var2.get("language"), "body.language"));
               }

               if (var2.containsKey("file")) {
                  this.a(var4, List.of("file", "src"), a(var2.get("file"), "body.file"));
               }

               if (var2.containsKey("graphql")) {
                  var var10 = b(var2.get("graphql"), "body.graphql");
                  String var11 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, "graphql");
                  if (this.e(var11) == null) {
                     this.d(var11, io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var10));
                     return;
                  }

                  if (var10.containsKey("query")) {
                     this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var11, "query"), a(var10.get("query"), "body.graphql.query"));
                  }

                  if (var10.containsKey("variables")) {
                     JsonSpan var12 = this.e(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var11, "variables"));
                     Object var13 = var10.get("variables");
                     if (var12 != null && var12.kind() == JsonSpan.Kind.STRING && var13 instanceof Map) {
                        var13 = io.karatelabs.ext.postman.editor.JsonSourceEditor.b(var13);
                     } else if (var12 != null && var12.kind() == JsonSpan.Kind.OBJECT && var13 instanceof String var9) {
                        var13 = JsonParser.parse(var9);
                     }

                     this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var11, "variables"), var13);
                  }
               }
            }
         }
      }

      private static Map<String, Object> b(Map<?, ?> var0) {
         LinkedHashMap var1 = new LinkedHashMap();
         String var2 = var0.get("mode") instanceof String var3 ? var3 : (var0.containsKey("graphql") ? "graphql" : (var0.containsKey("file") ? "file" : "raw"));
         var1.put("mode", var2);
         if (var0.containsKey("raw")) {
            var1.put("raw", var0.get("raw"));
         }

         if (var0.containsKey("language")) {
            var1.put("options", Map.of("raw", Map.of("language", var0.get("language"))));
         }

         if (var0.containsKey("file")) {
            var1.put("file", Map.of("src", var0.get("file")));
         }

         if (var0.containsKey("graphql")) {
            var1.put("graphql", io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var0.get("graphql")));
         }

         return var1;
      }

      private void a(String var1, List<String> var2, Object var3) {
         String var4 = var1;

         for (int var5 = 0; var5 < var2.size(); var5++) {
            String var6 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var4, (String)var2.get(var5));
            if (this.e(var6) == null || var5 == var2.size() - 1) {
               Object var7 = var3;

               for (int var8 = var2.size() - 1; var8 > var5; var8--) {
                  LinkedHashMap var9 = new LinkedHashMap();
                  var9.put((String)var2.get(var8), var7);
                  var7 = var9;
               }

               this.d(var6, var7);
               return;
            }

            var4 = var6;
         }
      }

      private void b(String var1, Object var2) {
         JsonSpan var3 = this.a.a(var1);
         boolean var4 = a(var3, "request");
         String var5 = var4 ? var1 + "/request/auth" : io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "auth");
         if (!var4 || this.d(var1).kind() != JsonSpan.Kind.STRING) {
            JsonSpan var21 = this.e(var5);
            Map var7 = var21 != null && var21.kind() == JsonSpan.Kind.OBJECT ? (Map)var21.toValue() : null;
            if (var7 != null && !a((Object)var7)) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException(
                  "NOT_EDITABLE", var5 + " is a " + var7.get("type") + " auth, which the editor does not model and keeps as it is"
               );
            } else if ("inherit".equals(var2)) {
               if (var21 != null) {
                  this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var5, null));
               }
            } else {
               var var8 = b(var2, "auth");
               if (!a((Object)var8)) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", "auth type " + var8.get("type") + " is not one the editor models");
               } else {
                  String var9 = (String)var8.get("type");
                  if (var7 != null && var9.equals(var7.get("type"))) {
                     Object var10 = var7.get(var9);
                     String var11 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var5, var9);

                     for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
                        if (!"type".equals(var13.getKey())) {
                           String var14 = String.valueOf(var13.getKey());
                           if (!(var10 instanceof List var15)) {
                              if (!(var10 instanceof Map)) {
                                 this.d(var11, c(var8).get(var9));
                                 break;
                              }

                              this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var11, var14), var13.getValue());
                           } else {
                              int var16 = 0;
                              boolean var17 = false;
                              Iterator var18 = var15.iterator();

                              while (true) {
                                 if (var18.hasNext()) {
                                    if (!(var18.next() instanceof Map var20 && var14.equals(var20.get("key")))) {
                                       var16++;
                                       continue;
                                    }

                                    this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var11 + "/" + var16, "value"), var13.getValue());
                                    var17 = true;
                                 }

                                 if (!var17) {
                                    this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var11 + "/-", c(var14, var13.getValue())));
                                 }
                                 break;
                              }
                           }
                        }
                     }
                  } else {
                     this.d(var5, c(var8));
                  }
               }
            }
         } else if (!"inherit".equals(var2)) {
            var var6 = b(var2, "auth");
            if (!a((Object)var6)) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", "auth type " + var6.get("type") + " is not one the editor models");
            } else {
               this.a(var1, this.d(var1), var1x -> var1x.put("auth", c(var6)));
            }
         }
      }

      private static Map<String, Object> c(Map<?, ?> var0) {
         LinkedHashMap var1 = new LinkedHashMap();
         String var2 = (String)var0.get("type");
         var1.put("type", var2);
         ArrayList var3 = new ArrayList();

         for (Entry var5 : var0.entrySet()) {
            if (!"type".equals(var5.getKey())) {
               var3.add(c(String.valueOf(var5.getKey()), var5.getValue()));
            }
         }

         if (!var3.isEmpty()) {
            var1.put(var2, var3);
         }

         return var1;
      }

      private static Map<String, Object> c(String var0, Object var1) {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("key", var0);
         var2.put("value", var1);
         var2.put("type", var1 instanceof Boolean ? "boolean" : (var1 instanceof Number ? "number" : "string"));
         return var2;
      }

      static boolean a(Object var0) {
         if (!(var0 instanceof Map var1 && var1.get("type") instanceof String var2)) {
            return false;
         } else if (!io.karatelabs.ext.postman.editor.PostmanCollectionEditor.b.contains(var2)) {
            return false;
         } else if (!"oauth2".equals(var2)) {
            return true;
         } else {
            Object var10 = var1.get("oauth2");
            Object var4 = var1.get("grant_type");
            if (var10 instanceof List) {
               for (Object var8 : (List)var10) {
                  if (var8 instanceof Map var9 && "grant_type".equals(var9.get("key"))) {
                     var4 = var9.get("value");
                  }
               }
            } else if (var10 instanceof Map var6) {
               var4 = var6.get("grant_type");
            }

            return "client_credentials".equals(var4);
         }
      }

      private void d(String var1, Map<?, ?> var2) {
         String var3 = a(var2.get("listen"), "script.listen");
         Object var4 = var2.get("n");
         boolean var5 = "end".equals(var4);
         if (var5 || var4 instanceof Number var6 && var6.intValue() >= 0) {
            boolean var16 = Boolean.TRUE.equals(var2.get("remove"));
            String var7 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "event");
            JsonSpan var8 = this.e(var7);
            if (var5) {
               LinkedHashMap var17 = new LinkedHashMap();
               var17.put("listen", var3);
               LinkedHashMap var18 = new LinkedHashMap();
               var18.put("type", "text/javascript");
               var18.put("exec", c(var2.get("exec")));
               var17.put("script", var18);
               if (var8 == null) {
                  this.d(var7, new ArrayList<>(List.of(var17)));
               } else {
                  this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var7 + "/-", var17));
               }
            } else {
               int var9 = -1;
               int var10 = 0;
               if (var8 != null && var8.kind() == JsonSpan.Kind.ARRAY) {
                  for (int var11 = 0; var11 < var8.elements().size(); var11++) {
                     if (var8.elements().get(var11).toValue() instanceof Map var12 && var3.equals(var12.get("listen")) && var10++ == ((Number)var4).intValue()) {
                        var9 = var11;
                        break;
                     }
                  }
               }

               if (var9 < 0) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "no " + var3 + " event " + var4 + " — the item has " + var10);
               } else if (var16) {
                  this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("remove", var7 + "/" + var9, null));
               } else {
                  String var19 = var7 + "/" + var9 + "/script";
                  JsonSpan var20 = this.e(var19);
                  if (var20 != null && var20.kind() != JsonSpan.Kind.STRING) {
                     JsonSpan var21 = this.e(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var19, "exec"));
                     var var14 = c(var2.get("exec"));
                     if (var21 != null && var21.kind() == JsonSpan.Kind.STRING) {
                        String var15 = ((String)var21.value()).contains("\r\n") ? "\r\n" : "\n";
                        this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var19, "exec"), String.join(var15, var14));
                     } else {
                        this.d(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var19, "exec"), var14);
                     }
                  } else {
                     throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", var19 + " is a script given by reference, which is kept as it is");
                  }
               }
            }
         } else {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "script.n is the event's index among the " + var3 + " events, or 'end' to add one");
         }
      }

      private static List<String> c(Object var0) {
         if (var0 instanceof List var5) {
            ArrayList var2 = new ArrayList();

            for (Object var4 : var5) {
               var2.add(var4 == null ? "" : String.valueOf(var4));
            }

            return var2;
         } else if (var0 instanceof String var1) {
            return new ArrayList<>(Arrays.asList(var1.split("\r?\n", -1)));
         } else {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "script.exec is the script's lines, as a list or one string");
         }
      }

      private JsonSpan d(String var1) {
         JsonSpan var2 = this.e(io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "request"));
         if (var2 == null) {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", (var1.isEmpty() ? "the collection" : var1) + " has no request — it is a folder");
         } else {
            return var2;
         }
      }

      private void a(String var1, JsonSpan var2, io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier.RequestMapMutator var3) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("method", "GET");
         var4.put("header", new ArrayList());
         var4.put("url", var2.value());
         var3.apply(var4);
         String var5 = io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var1, "request");
         this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("replace", var5, var4));
         this.b.add(var5);
      }

      private void d(String var1, Object var2) {
         this.a.a(new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation("add", var1, var2));
      }

      static String a(String var0, String var1) {
         JsonSpan var2;
         JsonSpan var3;
         try {
            var2 = JsonParser.parseSpans(var0).root();
            var3 = JsonParser.parseSpans(var1).root();
         } catch (RuntimeException var8) {
            return var1;
         }

         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();
         if (a(var0, var2, var1, var3, var4, var5) && !var4.isEmpty()) {
            StringBuilder var6 = new StringBuilder(var0);

            for (int var7 = var4.size() - 1; var7 >= 0; var7--) {
               var6.replace(((int[])var4.get(var7))[0], ((int[])var4.get(var7))[1], (String)var5.get(var7));
            }

            return var6.toString();
         } else {
            return var1;
         }
      }

      private static boolean a(String var0, JsonSpan var1, String var2, JsonSpan var3, List<int[]> var4, List<String> var5) {
         if (var1.kind() == JsonSpan.Kind.OBJECT && var3.kind() == JsonSpan.Kind.OBJECT) {
            if (var1.members().size() != var3.members().size()) {
               return false;
            } else {
               for (int var9 = 0; var9 < var1.members().size(); var9++) {
                  if (!var1.members().get(var9).key().equals(var3.members().get(var9).key())
                     || !a(var0, var1.members().get(var9).value(), var2, var3.members().get(var9).value(), var4, var5)) {
                     return false;
                  }
               }

               return true;
            }
         } else if (var1.kind() == JsonSpan.Kind.ARRAY && var3.kind() == JsonSpan.Kind.ARRAY) {
            if (var1.elements().size() != var3.elements().size()) {
               return false;
            } else {
               for (int var8 = 0; var8 < var1.elements().size(); var8++) {
                  if (!a(var0, var1.elements().get(var8), var2, var3.elements().get(var8), var4, var5)) {
                     return false;
                  }
               }

               return true;
            }
         } else if (var1.kind() != JsonSpan.Kind.OBJECT
            && var1.kind() != JsonSpan.Kind.ARRAY
            && var3.kind() != JsonSpan.Kind.OBJECT
            && var3.kind() != JsonSpan.Kind.ARRAY) {
            String var6 = var1.text(var0);
            String var7 = var3.text(var2);
            if (!var6.equals(var7)) {
               var4.add(new int[]{var1.start(), var1.end()});
               var5.add(var7);
            }

            return true;
         } else {
            return false;
         }
      }

      private JsonSpan e(String var1) {
         try {
            return this.a.a(var1);
         } catch (io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException var3) {
            if ("NOT_FOUND".equals(var3.a())) {
               return null;
            } else {
               throw var3;
            }
         }
      }

      private Object f(String var1) {
         JsonSpan var2 = this.e(var1);
         return var2 == null ? null : var2.toValue();
      }

      private static boolean a(JsonSpan var0, String var1) {
         for (JsonSpan.Member var3 : var0.members()) {
            if (var3.key().equals(var1)) {
               return true;
            }
         }

         return false;
      }

      private static String a(Object var0, String var1) {
         if (var0 instanceof String var2) {
            return var2;
         } else {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", var1 + " is a string, got " + var0);
         }
      }

      private static Map<?, ?> b(Object var0, String var1) {
         if (var0 instanceof Map var2) {
            return var2;
         } else {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", var1 + " is an object, got " + var0);
         }
      }

      private static Map<String, Object> d(Map<?, ?> var0) {
         return (Map<String, Object>)io.karatelabs.ext.postman.editor.JsonSourceEditor.a(var0);
      }

      private interface RequestMapMutator {
         void apply(Map<String, Object> var1);
      }
   }

   private static final class PostmanCollectionSchema {
      static final CompiledSchema a = a();

      private static CompiledSchema a() {
         try {
            CompiledSchema var1;
            try (InputStream var0 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.class
                  .getClassLoader()
                  .getResourceAsStream("META-INF/karate/schemas/postman-collection-v2.1.0.json")) {
               if (var0 == null) {
                  throw new IllegalStateException("missing META-INF/karate/schemas/postman-collection-v2.1.0.json");
               }

               var1 = io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(JsonParser.parse(new String(var0.readAllBytes(), StandardCharsets.UTF_8)));
            }

            return var1;
         } catch (IOException var5) {
            throw new IllegalStateException(var5);
         }
      }
   }

   public record CollectionEditResult(Map<String, Object> c, byte[] d, String e, String f, List<String> g, List<Map<String, Object>> h, List<String> i, boolean j, List<String> k, String l) {


      public boolean a() {
         return this.c == null;
      }

      public Map<String, Object> b() {
         if (this.c != null) {
            return this.c;
         } else {
            LinkedHashMap var1 = new LinkedHashMap();
            var1.put("version", this.e);
            var1.put("basedOn", this.f);
            LinkedHashMap var2 = new LinkedHashMap();
            var2.put("paths", this.g);
            var2.put("diff", this.h);
            if (!this.i.isEmpty()) {
               var2.put("upgraded", this.i);
            }

            var1.put("change", var2);
            var1.put("valid", this.j);
            if (!this.j) {
               var1.put("invalid", this.k);
            }

            if (this.l != null) {
               var1.put("undo", this.l);
            }

            return var1;
         }
      }

      public Map<String, Object> c() {
         return this.c;
      }

      public byte[] d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public List<String> g() {
         return this.g;
      }

      public List<Map<String, Object>> h() {
         return this.h;
      }

      public List<String> i() {
         return this.i;
      }

      public boolean j() {
         return this.j;
      }

      public List<String> k() {
         return this.k;
      }

      public String l() {
         return this.l;
      }
   }

   record PostmanUrl(String c, String d, List<String> e, String f, boolean g, List<String> h, List<String[]> i, String j) {


      static io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl a(String var0) {
         String var1 = var0;
         String var2 = null;
         int var3 = var0.indexOf(35);
         if (var3 >= 0) {
            var2 = var0.substring(var3 + 1);
            var1 = var0.substring(0, var3);
         }

         ArrayList var4 = new ArrayList();
         int var5 = var1.indexOf(63);
         if (var5 >= 0) {
            String var6 = var1.substring(var5 + 1);
            var1 = var1.substring(0, var5);
            if (!var6.isEmpty()) {
               for (String var10 : var6.split("&", -1)) {
                  int var11 = var10.indexOf(61);
                  var4.add(var11 < 0 ? new String[]{var10, null} : new String[]{var10.substring(0, var11), var10.substring(var11 + 1)});
               }
            }
         }

         String var15 = null;
         Matcher var16 = io.karatelabs.ext.postman.editor.PostmanCollectionEditor.e.matcher(var1);
         if (var16.find()) {
            var15 = var16.group(1);
            var1 = var1.substring(var16.end());
         }

         int var17 = var1.indexOf(47);
         String var18 = var17 < 0 ? var1 : var1.substring(0, var17);
         List var19 = var17 < 0 ? List.of() : Arrays.asList(var1.substring(var17 + 1).split("/", -1));
         String var20 = null;
         int var12 = var18.lastIndexOf(64);
         if (var12 >= 0) {
            var20 = var18.substring(0, var12);
            var18 = var18.substring(var12 + 1);
         }

         String var13 = null;
         int var14 = a(var18, ':');
         if (var14 >= 0) {
            var13 = var18.substring(var14 + 1);
            var18 = var18.substring(0, var14);
         }

         return new io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl(var15, var20, b(var18, '.'), var13, var17 >= 0, var19, var4, var2);
      }

      io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl a(List<String[]> var1) {
         return new io.karatelabs.ext.postman.editor.PostmanCollectionEditor.PostmanUrl(this.c, this.d, this.e, this.f, this.g, this.h, var1, this.j);
      }

      String a() {
         StringBuilder var1 = new StringBuilder();
         if (this.c != null) {
            var1.append(this.c).append("://");
         }

         if (this.d != null) {
            var1.append(this.d).append('@');
         }

         var1.append(String.join(".", this.e));
         if (this.f != null) {
            var1.append(':').append(this.f);
         }

         if (this.g) {
            var1.append('/').append(String.join("/", this.h));
         }

         if (!this.i.isEmpty()) {
            var1.append('?');

            for (int var2 = 0; var2 < this.i.size(); var2++) {
               var1.append(var2 == 0 ? "" : "&").append(this.i.get(var2)[0]);
               if (this.i.get(var2)[1] != null) {
                  var1.append('=').append(this.i.get(var2)[1]);
               }
            }
         }

         if (this.j != null) {
            var1.append('#').append(this.j);
         }

         return var1.toString();
      }

      Map<String, Object> b() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("raw", this.a());
         if (this.c != null) {
            var1.put("protocol", this.c);
         }

         var1.put("host", new ArrayList<>(this.e));
         if (this.f != null) {
            var1.put("port", this.f);
         }

         var1.put("path", new ArrayList<>(this.h));
         if (!this.i.isEmpty()) {
            ArrayList var2 = new ArrayList();

            for (String[] var4 : this.i) {
               var2.add(io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier.a(var4));
            }

            var1.put("query", var2);
         }

         if (this.j != null) {
            var1.put("hash", this.j);
         }

         ArrayList var5 = new ArrayList();

         for (String var7 : this.h) {
            if (var7.startsWith(":") && var7.length() > 1) {
               var5.add(io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditApplier.b(var7.substring(1)));
            }
         }

         if (!var5.isEmpty()) {
            var1.put("variable", var5);
         }

         return var1;
      }

      private static int a(String var0, char var1) {
         int var2 = 0;

         for (int var3 = var0.length() - 1; var3 >= 0; var3--) {
            char var4 = var0.charAt(var3);
            if (var4 == '}') {
               var2++;
            } else if (var4 == '{') {
               var2--;
            } else if (var4 == var1 && var2 == 0) {
               return var3;
            }
         }

         return -1;
      }

      private static List<String> b(String var0, char var1) {
         ArrayList var2 = new ArrayList();
         int var3 = 0;
         int var4 = 0;

         for (int var5 = 0; var5 < var0.length(); var5++) {
            char var6 = var0.charAt(var5);
            if (var6 == '{') {
               var3++;
            } else if (var6 == '}') {
               var3--;
            } else if (var6 == var1 && var3 == 0) {
               var2.add(var0.substring(var4, var5));
               var4 = var5 + 1;
            }
         }

         var2.add(var0.substring(var4));
         return var2;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public List<String> e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }

      public List<String> h() {
         return this.h;
      }

      public List<String[]> i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }
   }
}
