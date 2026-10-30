package io.karatelabs.ext.sourcedoc;

import java.util.Arrays;

final class SourceLineIndex {
   private final int[] lineStarts;

   private SourceLineIndex(int[] var1) {
      this.lineStarts = var1;
   }

   static SourceLineIndex create(String var0) {
      int var1 = 1;

      for (int var2 = 0; var2 < var0.length(); var2++) {
         if (var0.charAt(var2) == '\n') {
            var1++;
         }
      }

      int[] var5 = new int[var1];
      int var3 = 1;

      for (int var4 = 0; var4 < var0.length(); var4++) {
         if (var0.charAt(var4) == '\n') {
            var5[var3++] = var4 + 1;
         }
      }

      return new SourceLineIndex(var5);
   }

   int lineAtOffset(int var1) {
      int var2 = Arrays.binarySearch(this.lineStarts, var1);
      return (var2 >= 0 ? var2 : -var2 - 2) + 1;
   }

   int columnAtOffset(int var1) {
      int var2 = Arrays.binarySearch(this.lineStarts, var1);
      int var3 = var2 >= 0 ? var2 : -var2 - 2;
      return var1 - this.lineStarts[var3] + 1;
   }

   io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan spanBetween(int var1, int var2) {
      return new io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan(this.lineAtOffset(var1), this.columnAtOffset(var1), this.lineAtOffset(var2), this.columnAtOffset(var2), var1, var2);
   }
}
