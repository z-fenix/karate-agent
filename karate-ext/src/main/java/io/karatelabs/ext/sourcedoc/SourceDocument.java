package io.karatelabs.ext.sourcedoc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SourceDocument {
   private final String sourceName;
   private final String sourceText;
   private final Object rootValue;
   private final Map<String, io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry> pointerEntries;
   private final List<io.karatelabs.ext.sourcedoc.SourceComment> comments;

   SourceDocument(String var1, String var2, Object var3, Map<String, io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry> var4, List<io.karatelabs.ext.sourcedoc.SourceComment> var5) {
      this.sourceName = var1;
      this.sourceText = var2;
      this.rootValue = var3;
      this.pointerEntries = var4;
      this.comments = List.copyOf(var5);
   }

   public static SourceDocument parse(String var0, String var1) {
      return io.karatelabs.ext.sourcedoc.SourceDocumentParser.selectParser(var0, var1).parse(var0, var1);
   }

   public Object value() {
      return this.rootValue;
   }

   public String sourceText() {
      return this.sourceText;
   }

   public String sourceName() {
      return this.sourceName;
   }

   public List<io.karatelabs.ext.sourcedoc.SourceComment> comments() {
      return this.comments;
   }

   public io.karatelabs.ext.sourcedoc.SourceDocument.ResolvedLocation resolveLocation(String var1, io.karatelabs.ext.sourcedoc.SourceDocument.Anchor var2) {
      String var3 = var1 == null ? "" : var1;
      boolean var4 = true;

      while (true) {
         io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var5 = this.pointerEntries.get(var3);
         if (var5 != null) {
            io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan var6 = var2 == io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.KEY && var5.keySpan != null ? var5.keySpan : var5.valueSpan;
            if (var6 != null) {
               return new io.karatelabs.ext.sourcedoc.SourceDocument.ResolvedLocation(var3, var4, var2 == io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.KEY && var5.keySpan != null ? io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.KEY : io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.VALUE, var6, var5.aliasAnchorSpan);
            }
         }

         if (var3.isEmpty()) {
            return null;
         }

         int var7 = var3.lastIndexOf(47);
         var3 = var7 <= 0 ? "" : var3.substring(0, var7);
         var4 = false;
      }
   }

   public io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan spanAt(String var1, io.karatelabs.ext.sourcedoc.SourceDocument.Anchor var2) {
      io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var3 = this.pointerEntries.get(var1 == null ? "" : var1);
      if (var3 == null) {
         return null;
      } else {
         return var2 == io.karatelabs.ext.sourcedoc.SourceDocument.Anchor.KEY ? var3.keySpan : var3.valueSpan;
      }
   }

   public String scalarTextAt(String var1) {
      io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var2 = this.pointerEntries.get(var1 == null ? "" : var1);
      return var2 == null ? null : var2.scalarText;
   }

   public int pointerCount() {
      return this.pointerEntries.size();
   }

   public static String escapePointerSegment(String var0) {
      return var0.replace("~", "~0").replace("/", "~1");
   }

   public static enum Anchor {
      KEY("key"),
      VALUE("value");

      private final String wire;

      private Anchor(String var3) {
         this.wire = var3;
      }

      public String wire() {
         return this.wire;
      }

      public static io.karatelabs.ext.sourcedoc.SourceDocument.Anchor of(String var0) {
         for (io.karatelabs.ext.sourcedoc.SourceDocument.Anchor var4 : values()) {
            if (var4.wire.equals(var0)) {
               return var4;
            }
         }

         throw new SourceDocumentException("unknown anchor: '" + var0 + "' (known: key, value)");
      }
   }

   static final class PointerEntry {
      io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan keySpan;
      io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan valueSpan;
      String scalarText;
      io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan aliasAnchorSpan;
   }

   public record ResolvedLocation(String pointer, boolean exact, io.karatelabs.ext.sourcedoc.SourceDocument.Anchor anchor, io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan span, io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan aliasAnchor) {


      public Map<String, Object> toMap() {
         var var1 = new LinkedHashMap<>(this.span.toMap());
         var1.put("anchor", this.anchor.wire());
         var1.put("exact", this.exact);
         if (!this.exact) {
            var1.put("pointer", this.pointer);
         }

         if (this.aliasAnchor != null) {
            LinkedHashMap<java.lang.String, java.lang.Integer> var2 = new LinkedHashMap();
            var2.put("line", this.aliasAnchor.line());
            var2.put("col", this.aliasAnchor.column());
            var1.put("aliasAnchor", var2);
         }

         return var1;
      }

      public String pointer() {
         return this.pointer;
      }

      public boolean exact() {
         return this.exact;
      }

      public io.karatelabs.ext.sourcedoc.SourceDocument.Anchor anchor() {
         return this.anchor;
      }

      public io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan span() {
         return this.span;
      }

      public io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan aliasAnchor() {
         return this.aliasAnchor;
      }
   }

   public record SourceSpan(int line, int column, int endLine, int endColumn, int offset, int endOffset) {


      public Map<String, Object> toMap() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("line", this.line);
         var1.put("col", this.column);
         var1.put("endLine", this.endLine);
         var1.put("endCol", this.endColumn);
         var1.put("offset", this.offset);
         var1.put("endOffset", this.endOffset);
         return var1;
      }

      public int line() {
         return this.line;
      }

      public int column() {
         return this.column;
      }

      public int endLine() {
         return this.endLine;
      }

      public int endColumn() {
         return this.endColumn;
      }

      public int offset() {
         return this.offset;
      }

      public int endOffset() {
         return this.endOffset;
      }
   }
}
