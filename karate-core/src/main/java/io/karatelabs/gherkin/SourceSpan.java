package io.karatelabs.gherkin;

import io.karatelabs.parser.Token;

public record SourceSpan(int start, int end, int line, int endLine) {
   static SourceSpan of(Token first, Token last) {
      return new SourceSpan(first.pos, last.pos + last.length, first.line + 1, last.line + 1);
   }

   public String text(String source) {
      return source.substring(this.start, this.end);
   }
}
