package io.karatelabs.ext.postman.editor;

import io.karatelabs.js.JsonParser;
import io.karatelabs.js.JsonSpan;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class JsonSourceEditor {
   private final io.karatelabs.ext.postman.editor.PostmanCollectionDocument.JsonFormatting a;
   private final String b;
   private String c;
   private JsonSpan.Doc d;
   private Object e;
   private final List<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> f = new ArrayList<>();

   public JsonSourceEditor(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var1) {
      this.a = var1.e();
      this.b = var1.b();
      this.c = this.b;
      this.d = var1.d();
      this.e = var1.h();
   }

   public String a() {
      return this.c;
   }

   public JsonSpan.Doc b() {
      return this.d;
   }

   public Object c() {
      return this.e;
   }

   public List<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> d() {
      return List.copyOf(this.f);
   }

   public boolean e() {
      return !this.f.isEmpty();
   }

   public void a(io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation var1) {
      var var2 = b(var1.b());
      String var3 = var1.a();
      switch (var3) {
         case "replace":
            io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget var7 = this.a(var2, false);
            if (var7.c() == null) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "replace: nothing at " + var1.b());
            }

            this.a(var7.c().start(), var7.c().end(), this.a(var1.c(), this.a(var7.c().start()), this.a(var7.a())));
            break;
         case "add":
            if (var2.isEmpty()) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "add: the root cannot be replaced");
            }

            io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget var6 = this.a(var2, true);
            if (var6.a().kind() == JsonSpan.Kind.OBJECT) {
               if (var6.c() != null) {
                  this.a(var6.c().start(), var6.c().end(), this.a(var1.c(), this.a(var6.c().start()), this.a(var6.a())));
               } else {
                  this.a(var6.a(), var6.d(), var1.c());
               }
            } else {
               this.a(var6.a(), var6.b(), var1.c());
            }
            break;
         case "remove":
            io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget var5 = this.a(var2, false);
            if (var5.c() == null) {
               throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "remove: nothing at " + var1.b());
            }

            this.a(var5);
            break;
         default:
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "op is add, replace or remove, not " + var1.a());
      }

      this.d = JsonParser.parseSpans(this.c);
      this.e = a(this.e, var1, var2);
   }

   public void a(List<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> var1) {
      var var2 = new ArrayList<>(var1);
      var2.sort((var0, var1x) -> Integer.compare(var1x.a(), var0.a()));

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var4 : (Iterable<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit>) (Iterable<?>) (var2)) {
         if (var4.a() < 0 || var4.b() < var4.a() || var4.b() > this.c.length()) {
            throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "the undo token does not fit this version of the file");
         }

         this.a(var4.a(), var4.b(), var4.c());
      }

      try {
         this.d = JsonParser.parseSpans(this.c);
      } catch (RuntimeException var5) {
         throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("UNINTENDED_CHANGE", "the restored text is not JSON: " + var5.getMessage());
      }

      this.e = this.d.root().toValue();
   }

   private void a(JsonSpan var1, String var2, Object var3) {
      String var4 = c(var2) + (this.a.e() ? ": " : ":");
      var var5 = var1.members();
      if (var5.isEmpty()) {
         String var10 = this.a(var1.start());
         String var11 = this.a.f()
            ? var4 + this.a(var3, var10, true)
            : this.a.b() + var10 + this.a.a() + var4 + this.a(var3, var10 + this.a.a(), false) + this.a.b() + var10;
         this.a(var1.start() + 1, var1.end() - 1, var11);
      } else {
         JsonSpan.Member var6 = (JsonSpan.Member)var5.get(var5.size() - 1);
         String var7 = this.a(var6.keyStart());
         boolean var8 = this.a(var1);
         String var9 = var8 ? this.g() + var4 + this.a(var3, var7, true) : "," + this.a.b() + var7 + var4 + this.a(var3, var7, false);
         this.a(var6.value().end(), var6.value().end(), var9);
      }
   }

   private void a(JsonSpan var1, int var2, Object var3) {
      var var4 = var1.elements();
      if (var4.isEmpty()) {
         String var9 = this.a(var1.start());
         String var11 = this.a.f() ? this.a(var3, var9, true) : this.a.b() + var9 + this.a.a() + this.a(var3, var9 + this.a.a(), false) + this.a.b() + var9;
         this.a(var1.start() + 1, var1.end() - 1, var11);
      } else {
         boolean var5 = this.a(var1);
         if (var2 >= var4.size()) {
            JsonSpan var10 = (JsonSpan)var4.get(var4.size() - 1);
            String var12 = this.a(var10.start());
            String var13 = var5 ? this.g() + this.a(var3, var12, true) : "," + this.a.b() + var12 + this.a(var3, var12, false);
            this.a(var10.end(), var10.end(), var13);
         } else {
            JsonSpan var6 = (JsonSpan)var4.get(var2);
            String var7 = this.a(var6.start());
            String var8 = var5 ? this.a(var3, var7, true) + this.g() : this.a(var3, var7, false) + "," + this.a.b() + var7;
            this.a(var6.start(), var6.start(), var8);
         }
      }
   }

   private void a(io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget var1) {
      JsonSpan var2 = var1.a();
      if (var2.kind() == JsonSpan.Kind.OBJECT) {
         var var3 = var2.members();
         int var4 = var1.b();
         if (var3.size() == 1) {
            this.a(var2.start() + 1, var2.end() - 1, "");
         } else if (var4 < var3.size() - 1) {
            this.a(((JsonSpan.Member)var3.get(var4)).keyStart(), ((JsonSpan.Member)var3.get(var4 + 1)).keyStart(), "");
         } else {
            this.a(((JsonSpan.Member)var3.get(var4 - 1)).value().end(), ((JsonSpan.Member)var3.get(var4)).value().end(), "");
         }
      } else {
         var var5 = var2.elements();
         int var6 = var1.b();
         if (var5.size() == 1) {
            this.a(var2.start() + 1, var2.end() - 1, "");
         } else if (var6 < var5.size() - 1) {
            this.a(((JsonSpan)var5.get(var6)).start(), ((JsonSpan)var5.get(var6 + 1)).start(), "");
         } else {
            this.a(((JsonSpan)var5.get(var6 - 1)).end(), ((JsonSpan)var5.get(var6)).end(), "");
         }
      }
   }

   private String g() {
      return this.a.e() && !this.a.f() ? ", " : ",";
   }

   private boolean a(JsonSpan var1) {
      return this.a.f() || this.c.substring(var1.start(), var1.end()).indexOf(10) < 0;
   }

   private String a(int var1) {
      int var2 = this.c.lastIndexOf(10, var1 - 1) + 1;
      int var3 = var2;

      while (var3 < var1 && (this.c.charAt(var3) == ' ' || this.c.charAt(var3) == '\t')) {
         var3++;
      }

      return this.c.substring(var2, var3);
   }

   private void a(int var1, int var2, String var3) {
      int var4 = var3.length() - (var2 - var1);
      int var5 = var1;
      int var6 = var2;
      ArrayList<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> var7 = new ArrayList();

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var9 : this.f) {
         if (var9.b() >= var1 && var9.a() <= var2) {
            var7.add(var9);
            var5 = Math.min(var5, var9.a());
            var6 = Math.max(var6, var9.b());
         }
      }

      StringBuilder var13 = new StringBuilder();
      int var14 = var5;

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var11 : (Iterable<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit>) (Iterable<?>) (var7)) {
         var13.append(this.c, var14, var11.a()).append(var11.c());
         var14 = var11.b();
      }

      var13.append(this.c, var14, var6);
      ArrayList<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> var15 = new ArrayList();

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var12 : this.f) {
         if (var12.b() < var1) {
            var15.add(var12);
         } else if (var12.a() > var2) {
            var15.add(new io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit(var12.a() + var4, var12.b() + var4, var12.c()));
         }
      }

      var15.add(new io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit(var5, var6 + var4, var13.toString()));
      var15.sort((var0, var1x) -> Integer.compare(var0.a(), var1x.a()));
      this.f.clear();
      this.f.addAll(var15);
      this.c = this.c.substring(0, var1) + var3 + this.c.substring(var2);
   }

   public static String a(String var0, List<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit> var1) {
      var var2 = new ArrayList<>(var1);
      var2.sort((var0x, var1x) -> Integer.compare(var1x.a(), var0x.a()));
      StringBuilder var3 = new StringBuilder(var0);

      for (io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit var5 : (Iterable<io.karatelabs.ext.postman.editor.JsonSourceEditor.TextEdit>) (Iterable<?>) (var2)) {
         var3.replace(var5.a(), var5.b(), var5.c());
      }

      return var3.toString();
   }

   public boolean f() {
      return a(this.c, this.f).equals(this.b);
   }

   public JsonSpan a(String var1) {
      var var2 = b(var1);
      return var2.isEmpty() ? this.d.root() : this.a(var2, true).c();
   }

   io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget a(List<String> var1, boolean var2) {
      if (var1.isEmpty()) {
         throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "the root is not addressable by this op");
      } else {
         JsonSpan var3 = this.d.root();

         for (int var4 = 0; var4 < var1.size(); var4++) {
            String var5 = (String)var1.get(var4);
            boolean var6 = var4 == var1.size() - 1;
            String var7 = b(var1.subList(0, var4 + 1));
            if (var3.kind() != JsonSpan.Kind.OBJECT) {
               if (var3.kind() != JsonSpan.Kind.ARRAY) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", b(var1.subList(0, var4)) + " is a scalar, not a container");
               }

               int var10 = var3.elements().size();
               int var11 = "-".equals(var5) ? var10 : b(var5, var7);
               if (var11 == var10 && var6 && var2) {
                  return new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget(var3, var10, null, var5);
               }

               if (var11 >= var10) {
                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "nothing at " + var7 + " — the array has " + var10 + " elements");
               }

               if (var6) {
                  return new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget(var3, var11, var3.elements().get(var11), var5);
               }

               var3 = var3.elements().get(var11);
            } else {
               int var8 = -1;

               for (int var9 = 0; var9 < var3.members().size(); var9++) {
                  if (var3.members().get(var9).key().equals(var5)) {
                     if (var8 >= 0) {
                        throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_EDITABLE", "the key at " + var7 + " is duplicated in the file");
                     }

                     var8 = var9;
                  }
               }

               if (var8 < 0) {
                  if (var6 && var2) {
                     return new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget(var3, -1, null, var5);
                  }

                  throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "nothing at " + var7);
               }

               if (var6) {
                  return new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPointerTarget(var3, var8, var3.members().get(var8).value(), var5);
               }

               var3 = var3.members().get(var8).value();
            }
         }

         throw new IllegalStateException();
      }
   }

   private static int b(String var0, String var1) {
      if (!var0.matches("0|[1-9][0-9]*")) {
         throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("NOT_FOUND", "nothing at " + var1 + " — an array index is a number");
      } else {
         return Integer.parseInt(var0);
      }
   }

   public static List<String> b(String var0) {
      if (var0 == null) {
         throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "a JSON pointer is required");
      } else if (var0.isEmpty()) {
         return List.of();
      } else if (var0.charAt(0) != '/') {
         throw new io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonEditException("BAD_REQUEST", "a JSON pointer starts with /, got " + var0);
      } else {
         ArrayList<java.lang.String> var1 = new ArrayList();

         for (String var5 : var0.substring(1).split("/", -1)) {
            var1.add(var5.replace("~1", "/").replace("~0", "~"));
         }

         return var1;
      }
   }

   public static String b(List<String> var0) {
      StringBuilder var1 = new StringBuilder();

      for (String var3 : var0) {
         var1.append('/').append(var3.replace("~", "~0").replace("/", "~1"));
      }

      return var1.toString();
   }

   public static String a(String var0, String var1) {
      return var0 + "/" + var1.replace("~", "~0").replace("/", "~1");
   }

   static Object a(Object var0, io.karatelabs.ext.postman.editor.JsonSourceEditor.JsonPatchOperation var1, List<String> var2) {
      Object var3 = var0;

      for (int var4 = 0; var4 < var2.size() - 1; var4++) {
         var3 = a(var3, (String)var2.get(var4));
      }

      String var11 = (String)var2.get(var2.size() - 1);
      if (var3 instanceof Map var5) {
         String var8 = var1.a();
         byte var9 = -1;
         switch (var8.hashCode()) {
            case -934610812:
               if (var8.equals("remove")) {
                  var9 = 0;
               }
            default:
               switch (var9) {
                  case 0:
                     var5.remove(var11);
                     break;
                  default:
                     var5.put(var11, a(var1.c()));
               }
         }
      } else if (var3 instanceof List var6) {
         int var12 = "-".equals(var11) ? var6.size() : Integer.parseInt(var11);
         String var13 = var1.a();
         switch (var13) {
            case "remove":
               var6.remove(var12);
               break;
            case "replace":
               var6.set(var12, a(var1.c()));
               break;
            default:
               var6.add(var12, a(var1.c()));
         }
      }

      return var0;
   }

   private static Object a(Object var0, String var1) {
      return var0 instanceof Map var2 ? var2.get(var1) : ((List)var0).get(Integer.parseInt(var1));
   }

   static Object a(Object var0) {
      if (var0 instanceof Map var5) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

         for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            var6.put(String.valueOf(var8.getKey()), a(var8.getValue()));
         }

         return var6;
      } else if (!(var0 instanceof List var1)) {
         return var0;
      } else {
         ArrayList<java.lang.Object> var2 = new ArrayList();

         for (Object var4 : var1) {
            var2.add(a(var4));
         }

         return var2;
      }
   }

   public static boolean a(Object var0, Object var1) {
      if (var0 instanceof Map var2 && var1 instanceof Map var9) {
         if (var2.size() != var9.size()) {
            return false;
         } else {
            for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
               if (!var9.containsKey(var5.getKey()) || !a(var5.getValue(), var9.get(var5.getKey()))) {
                  return false;
               }
            }

            return true;
         }
      } else if (var0 instanceof List var6 && var1 instanceof List var8) {
         if (var6.size() != var8.size()) {
            return false;
         } else {
            for (int var4 = 0; var4 < var6.size(); var4++) {
               if (!a(var6.get(var4), var8.get(var4))) {
                  return false;
               }
            }

            return true;
         }
      } else if (var0 instanceof Number var7 && var1 instanceof Number var3) {
         return new BigDecimal(var7.toString()).compareTo(new BigDecimal(var3.toString())) == 0;
      } else {
         return var0 == null ? var1 == null : var0.equals(var1);
      }
   }

   public static List<String> b(Object var0, Object var1) {
      TreeSet var2 = new TreeSet();
      a(var0, var1, "", var2);
      return new ArrayList<>(var2);
   }

   private static void a(Object var0, Object var1, String var2, TreeSet<String> var3) {
      if (var0 instanceof Map var4 && var1 instanceof Map var5) {
         for (Object var13 : var4.keySet()) {
            String var10 = String.valueOf(var13);
            if (!var5.containsKey(var10)) {
               var3.add(a(var2, var10));
            } else {
               a(var4.get(var10), var5.get(var10), a(var2, var10), var3);
            }
         }

         for (Object var14 : var5.keySet()) {
            if (!var4.containsKey(var14)) {
               var3.add(a(var2, String.valueOf(var14)));
            }
         }
      } else if (var0 instanceof List var6 && var1 instanceof List var7) {
         int var8 = Math.max(var6.size(), var7.size());

         for (int var9 = 0; var9 < var8; var9++) {
            if (var9 < var6.size() && var9 < var7.size()) {
               a(var6.get(var9), var7.get(var9), var2 + "/" + var9, var3);
            } else {
               var3.add(var2 + "/" + var9);
            }
         }
      } else if (!a(var0, var1)) {
         var3.add(var2);
      }
   }

   String a(Object var1, String var2, boolean var3) {
      StringBuilder var4 = new StringBuilder();
      a(var1, var2, var3 ? "" : this.a.b(), this.a.a(), this.a.e() ? ": " : ":", var3 ? this.g() : ",", var4);
      return var4.toString();
   }

   public static String b(Object var0) {
      StringBuilder var1 = new StringBuilder();
      a(var0, "", "", "", ":", ",", var1);
      return var1.toString();
   }

   private static void a(Object var0, String var1, String var2, String var3, String var4, String var5, StringBuilder var6) {
      if (var0 == null) {
         var6.append("null");
      } else if (!(var0 instanceof Boolean) && !(var0 instanceof Number)) {
         if (var0 instanceof Map var7) {
            if (var7.isEmpty()) {
               var6.append("{}");
               return;
            }

            String var13 = var1 + var3;
            var6.append('{');
            boolean var10 = true;

            for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
               var6.append(var10 ? "" : var5).append(var2).append(var2.isEmpty() ? "" : var13);
               var6.append(c(String.valueOf(var12.getKey()))).append(var4);
               a(var12.getValue(), var13, var2, var3, var4, var5, var6);
               var10 = false;
            }

            var6.append(var2).append(var2.isEmpty() ? "" : var1).append('}');
         } else if (var0 instanceof List var8) {
            if (var8.isEmpty()) {
               var6.append("[]");
               return;
            }

            String var14 = var1 + var3;
            var6.append('[');
            boolean var15 = true;

            for (Object var17 : var8) {
               var6.append(var15 ? "" : var5).append(var2).append(var2.isEmpty() ? "" : var14);
               a(var17, var14, var2, var3, var4, var5, var6);
               var15 = false;
            }

            var6.append(var2).append(var2.isEmpty() ? "" : var1).append(']');
         } else {
            var6.append(c(var0.toString()));
         }
      } else {
         var6.append(var0 instanceof Number var9 ? a(var9) : var0.toString());
      }
   }

   static String a(Number var0) {
      if (!(var0 instanceof Double) && !(var0 instanceof Float)) {
         return var0.toString();
      } else {
         double var1 = var0.doubleValue();
         if (!Double.isNaN(var1) && !Double.isInfinite(var1)) {
            if (var1 == Math.rint(var1) && Math.abs(var1) < 1.0E21) {
               return new BigDecimal(var1).toBigInteger().toString();
            } else {
               return Math.abs(var1) >= 1.0E-6
                  ? new BigDecimal(Double.toString(var1)).toPlainString()
                  : Double.toString(var1).replace(".0E", "E").replace("E", "e");
            }
         } else {
            return "null";
         }
      }
   }

   static String c(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 2).append('"');

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         switch (var3) {
            case '\b':
               var1.append("\\b");
               break;
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\f':
               var1.append("\\f");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '"':
               var1.append("\\\"");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            default:
               if (var3 < ' ') {
                  var1.append(String.format("\\u%04x", Integer.valueOf(var3)));
               } else if (Character.isHighSurrogate(var3) && var2 + 1 < var0.length() && Character.isLowSurrogate(var0.charAt(var2 + 1))) {
                  var1.append(var3).append(var0.charAt(++var2));
               } else if (Character.isSurrogate(var3)) {
                  var1.append(String.format("\\u%04x", Integer.valueOf(var3)));
               } else {
                  var1.append(var3);
               }
         }
      }

      return var1.append('"').toString();
   }

   record JsonPointerTarget(JsonSpan a, int b, JsonSpan c, String d) {

   }

   public record JsonPatchOperation(String a, String b, Object c) {

   }

   public static final class JsonEditException extends RuntimeException {
      final String a;

      JsonEditException(String var1, String var2) {
         super(var2);
         this.a = var1;
      }

      public String a() {
         return this.a;
      }
   }

   public record TextEdit(int a, int b, String c) {

   }
}
