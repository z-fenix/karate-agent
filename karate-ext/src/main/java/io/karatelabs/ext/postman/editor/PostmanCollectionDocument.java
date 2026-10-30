package io.karatelabs.ext.postman.editor;

import io.karatelabs.ext.common.Sha256;
import io.karatelabs.js.JsonParser;
import io.karatelabs.js.JsonSpan;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class PostmanCollectionDocument {
   private final byte[] a;
   private final String b;
   private final String c;
   private final JsonSpan.Doc d;
   private final io.karatelabs.ext.postman.editor.PostmanCollectionDocument.JsonFormatting e;
   private final String f;
   private int[] g;

   private PostmanCollectionDocument(byte[] var1, String var2, JsonSpan.Doc var3, String var4) {
      this.a = var1;
      this.b = var2;
      this.c = io.karatelabs.ext.common.Sha256.a(var1);
      this.d = var3;
      this.f = var4;
      this.e = var3 == null ? null : a(var2, var3);
   }

   public static PostmanCollectionDocument a(byte[] var0) {
      String var2 = null;

      String var1;
      try {
         var1 = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(var0)).toString();
      } catch (CharacterCodingException var5) {
         var1 = new String(var0, StandardCharsets.UTF_8);
         var2 = "the file is not valid UTF-8";
      }

      try {
         return new PostmanCollectionDocument(var0, var1, JsonParser.parseSpans(var1), var2);
      } catch (RuntimeException var6) {
         String var4 = var6.getMessage() == null ? var6.getClass().getSimpleName() : var6.getMessage().lines().findFirst().orElse("");
         return new PostmanCollectionDocument(var0, var1, null, var2 != null ? var2 : "the file is not JSON: " + var4);
      }
   }

   public static PostmanCollectionDocument a(Path var0) throws IOException {
      return a(Files.readAllBytes(var0));
   }

   public static PostmanCollectionDocument a(String var0) {
      return a(var0.getBytes(StandardCharsets.UTF_8));
   }

   public byte[] a() {
      return this.a;
   }

   public String b() {
      return this.b;
   }

   public String c() {
      return this.c;
   }

   public JsonSpan.Doc d() {
      return this.d;
   }

   public io.karatelabs.ext.postman.editor.PostmanCollectionDocument.JsonFormatting e() {
      return this.e;
   }

   public boolean f() {
      return this.f == null;
   }

   public String g() {
      return this.f;
   }

   public Object h() {
      return this.d.root().toValue();
   }

   public String b(String var1) {
      if (this.d == null) {
         return this.f;
      } else if (this.d.root().kind() != JsonSpan.Kind.OBJECT) {
         return "the root is not an object";
      } else {
         Object var2 = this.h();
         if (((Map)var2).get("info") instanceof Map var4) {
            if (!(((Map)var2).get("item") instanceof List)) {
               return "no item array";
            } else {
               boolean var5 = var1 != null && var1.endsWith(".postman_collection.json");
               boolean var6 = var4.get("schema") instanceof String var7 && var7.contains("getpostman.com")
                  || var4.get("_postman_id") != null
                  || var4.get("_postman_schema") != null;
               return !var5 && !var6 ? "info names no Postman schema or id, and the file is not *.postman_collection.json" : null;
            }
         } else {
            return "no info object";
         }
      }
   }

   public int a(int var1) {
      if (this.g == null) {
         int[] var2 = new int[this.b.length() + 1];
         int var3 = 0;
         int var4 = 0;

         while (var4 < this.b.length()) {
            int var5 = this.b.codePointAt(var4);
            int var6 = Character.charCount(var5);
            int var7 = var5 < 128 ? 1 : (var5 < 2048 ? 2 : (var5 < 65536 ? 3 : 4));
            var2[var4] = var3;
            if (var6 == 2) {
               var2[var4 + 1] = var3;
            }

            var3 += var7;
            var4 += var6;
         }

         var2[this.b.length()] = var3;
         this.g = var2;
      }

      return this.g[var1];
   }

   private static io.karatelabs.ext.postman.editor.PostmanCollectionDocument.JsonFormatting a(String var0, JsonSpan.Doc var1) {
      String var2 = "";
      int var3 = 0;

      while (var3 < var0.length()) {
         int var4 = var0.indexOf(10, var3);
         var4 = var4 < 0 ? var0.length() : var4;
         int var5 = var3;

         while (var5 < var4 && (var0.charAt(var5) == ' ' || var0.charAt(var5) == '\t')) {
            var5++;
         }

         if (var5 > var3 && var5 < var4) {
            var2 = var0.substring(var3, var5);
            break;
         }

         var3 = var4 + 1;
      }

      int var9 = var0.indexOf(10);
      String var10 = var9 > 0 && var0.charAt(var9 - 1) == '\r' ? "\r\n" : "\n";
      JsonSpan var6 = var1.root();
      boolean var7 = var0.substring(var6.start(), var6.end()).indexOf(10) < 0;
      return new io.karatelabs.ext.postman.editor.PostmanCollectionDocument.JsonFormatting(var2, var10, var1.bom(), var0.endsWith("\n"), a(var0, var6), var7);
   }

   private static boolean a(String var0, JsonSpan var1) {
      if (var1.kind() == JsonSpan.Kind.OBJECT) {
         var var2 = var1.members().iterator();
         if (var2.hasNext()) {
            JsonSpan.Member var6 = (JsonSpan.Member)var2.next();
            int var4 = var6.keyEnd();

            while (var4 < var0.length() && var0.charAt(var4) != ':') {
               var4++;
            }

            return var4 + 1 < var0.length() && var0.charAt(var4 + 1) == ' ';
         }
      }

      var var5 = var1.elements().iterator();
      if (var5.hasNext()) {
         JsonSpan var3 = (JsonSpan)var5.next();
         return a(var0, var3);
      } else {
         return true;
      }
   }

   public record JsonFormatting(String a, String b, boolean c, boolean d, boolean e, boolean f) {
   }
}
