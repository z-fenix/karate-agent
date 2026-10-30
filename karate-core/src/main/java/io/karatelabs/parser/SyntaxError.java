package io.karatelabs.parser;

public class SyntaxError {
   public final Token token;
   public final String message;
   public final NodeType expected;

   public SyntaxError(Token token, String message, NodeType expected) {
      this.token = token;
      this.message = message;
      this.expected = expected;
   }

   public SyntaxError(Token token, String message) {
      this(token, message, null);
   }

   public int getLine() {
      return this.token.line + 1;
   }

   public int getColumn() {
      return this.token.col + 1;
   }

   public long getOffset() {
      return this.token.pos;
   }

   @Override
   public String toString() {
      return "[" + this.getLine() + ":" + this.getColumn() + "] " + this.message;
   }
}
