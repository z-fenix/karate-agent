package io.karatelabs.ext.sourcedoc;

public record SourceComment(String text, SourceComment.Kind kind, String scope, int line, int column) {
   public enum Kind {
      BLOCK,
      INLINE
   }
}
