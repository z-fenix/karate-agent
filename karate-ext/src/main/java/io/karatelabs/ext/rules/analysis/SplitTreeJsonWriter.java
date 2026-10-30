package io.karatelabs.ext.rules.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public final class SplitTreeJsonWriter implements SplitTreeListener, AutoCloseable {
   public static final String a = "split-tree/1";
   private static final String c = "A box is one inclusive index range [lo,hi] per declared axis, in the order of axes[]: the integer itself on an int axis, the position in members[] on an enum or boolean axis; its rows are the product of hi-lo+1. preorder[] carries three numbers per node of the binary split tree, in preorder: an internal node is [0, axis, cut] and its two children are the next two subtrees, the left [lo, cut-1] and the right [cut, hi] on that axis with every other axis unchanged; a leaf is [1, verdict, signature], verdict indexing verdicts[] and signature indexing signatures[] or -1. leafBoxes[] holds 2 x axes numbers per leaf, lo and hi per axis, in preorder leaf order, and leafLines[] the authored throw line of a refused leaf, -1 otherwise. A signature is ';'-separated cells 'L<line>#<index>=<cell>' in the kernel's own order, so comparing one with a certificate's sorts the cells of both by (line, index) first.";
   private final Path d;
   private final Writer e;
   private final String f;
   private final String g;
   private final String h;
   private final String i;
   private final Map<String, Integer> j = new HashMap<>();
   private final List<String> k = new ArrayList<>();
   private final StringBuilder l = new StringBuilder();
   private final StringBuilder m = new StringBuilder();
   private int n;
   private long o;
   private long p;
   private final long[] q = new long[io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.values().length];
   private boolean r;

   public SplitTreeJsonWriter(Path var1, String var2, String var3, String var4, String var5) {
      this.d = var1;
      this.f = var2;
      this.g = var3;
      this.h = var4;
      this.i = var5;

      try {
         if (var1.getParent() != null) {
            Files.createDirectories(var1.getParent());
         }

         this.e = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(var1), 65536), StandardCharsets.UTF_8), 65536);
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   @Override
   public void a(List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, long[] var2, long[] var3) {
      this.n = var1.size();
      this.b(
         "{\"schema\":"
            + c("split-tree/1")
            + ",\"book\":"
            + c(this.f)
            + ",\"calcSha256\":"
            + c(this.g)
            + ",\"schemaSha256\":"
            + c(this.h)
            + ",\"evaluator\":"
            + c(this.i)
            + ",\"semantics\":"
            + c(
               "A box is one inclusive index range [lo,hi] per declared axis, in the order of axes[]: the integer itself on an int axis, the position in members[] on an enum or boolean axis; its rows are the product of hi-lo+1. preorder[] carries three numbers per node of the binary split tree, in preorder: an internal node is [0, axis, cut] and its two children are the next two subtrees, the left [lo, cut-1] and the right [cut, hi] on that axis with every other axis unchanged; a leaf is [1, verdict, signature], verdict indexing verdicts[] and signature indexing signatures[] or -1. leafBoxes[] holds 2 x axes numbers per leaf, lo and hi per axis, in preorder leaf order, and leafLines[] the authored throw line of a refused leaf, -1 otherwise. A signature is ';'-separated cells 'L<line>#<index>=<cell>' in the kernel's own order, so comparing one with a certificate's sorts the cells of both by (line, index) first."
            )
            + ",\"verdicts\":["
      );

      for (io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome var7 : io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.values()) {
         this.b((var7.ordinal() == 0 ? "" : ",") + c(var7.name().toLowerCase()));
      }

      this.b("],\"axes\":[");

      for (int var8 = 0; var8 < this.n; var8++) {
         io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var9 = (io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension)var1.get(var8);
         this.b(
            (var8 == 0 ? "" : ",")
               + "{\"name\":"
               + c(var9.c())
               + ",\"kind\":"
               + c(var9.d().name())
               + ",\"lo\":"
               + var9.e()
               + ",\"hi\":"
               + var9.f()
               + ",\"members\":"
         );
         if (var9.g() == null) {
            this.b("null}");
         } else {
            this.b("[");

            for (int var10 = 0; var10 < var9.g().size(); var10++) {
               this.b((var10 == 0 ? "" : ",") + a(var9.g().get(var10)));
            }

            this.b("]}");
         }
      }

      this.b("],\"root\":");
      this.b(this.b(var2, var3));
      this.b(",\"preorder\":[");
   }

   @Override
   public void a(int var1, long var2) {
      this.a("0," + var1 + "," + var2);
   }

   @Override
   public void a(long[] var1, long[] var2, io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome var3, String var4, int var5) {
      int var6 = var4 == null ? -1 : this.j.computeIfAbsent(var4, var1x -> {
         this.k.add(var1x);
         return this.k.size() - 1;
      });
      this.a("1," + var3.ordinal() + "," + var6);
      this.p++;
      this.q[var3.ordinal()] += this.a(var1, var2);

      for (int var7 = 0; var7 < this.n; var7++) {
         this.l.append(this.l.length() == 0 ? "" : ",").append(var1[var7]).append(',').append(var2[var7]);
      }

      this.m.append(this.m.length() == 0 ? "" : ",").append(var5);
   }

   private long a(long[] var1, long[] var2) {
      long var3 = 1L;

      for (int var5 = 0; var5 < this.n; var5++) {
         var3 *= var2[var5] - var1[var5] + 1L;
      }

      return var3;
   }

   public void a() {
      this.b("],\"leafBoxes\":[" + this.l + "],\"leafLines\":[" + this.m + "],\"signatures\":[");

      for (int var1 = 0; var1 < this.k.size(); var1++) {
         this.b((var1 == 0 ? "" : ",") + c(this.k.get(var1)));
      }

      this.b("],\"counts\":{\"nodes\":" + this.o + ",\"internal\":" + (this.o - this.p) + ",\"leaves\":" + this.p);

      for (io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome var4 : io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.values()) {
         this.b(",\"" + var4.name().toLowerCase() + "Rows\":" + this.q[var4.ordinal()]);
      }

      this.b("}}\n");
      this.r = true;
   }

   @Override
   public void close() {
      try {
         this.e.close();
         if (!this.r) {
            Files.deleteIfExists(this.d);
         }
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }
   }

   private void a(String var1) {
      this.b(this.o++ == 0L ? var1 : "," + var1);
   }

   private String b(long[] var1, long[] var2) {
      StringBuilder var3 = new StringBuilder("{\"lo\":[");

      for (int var4 = 0; var4 < this.n; var4++) {
         var3.append(var4 == 0 ? "" : ",").append(var1[var4]);
      }

      var3.append("],\"hi\":[");

      for (int var5 = 0; var5 < this.n; var5++) {
         var3.append(var5 == 0 ? "" : ",").append(var2[var5]);
      }

      return var3.append("]}").toString();
   }

   private void b(String var1) {
      try {
         this.e.write(var1);
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   private static String a(Object var0) {
      return var0 instanceof String var1 ? c(var1) : String.valueOf(var0);
   }

   private static String c(String var0) {
      StringBuilder var1 = new StringBuilder("\"");

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         switch (var3) {
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
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
               } else {
                  var1.append(var3);
               }
         }
      }

      return var1.append('"').toString();
   }
}
