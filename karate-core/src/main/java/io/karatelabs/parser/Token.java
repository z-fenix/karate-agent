package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.Collections;
import java.util.List;

public class Token {
   private static final TokenBuffer EMPTY_BUFFER = new TokenBuffer(Resource.text(""));
   public static final Token EMPTY = new Token(EMPTY_BUFFER, TokenType.EOF, 0, (short)0, (short)0, (short)0);
   final TokenBuffer buffer;
   final int index;
   public final int pos;
   public final short line;
   public final short col;
   public final TokenType type;
   public final short length;

   public Token(TokenBuffer buffer, TokenType type, int pos, short line, short col, short length) {
      this.buffer = buffer;
      this.type = type;
      this.pos = pos;
      this.line = line;
      this.col = col;
      this.length = length;
      this.index = buffer.addToken(this);
   }

   public Token(TokenBuffer buffer, TokenType type, int pos, int line, int col, int length) {
      this(buffer, type, pos, (short)line, (short)col, (short)length);
   }

   public Resource getResource() {
      return this.buffer.resource;
   }

   public String getText() {
      return this.buffer.getText(this.index, this.pos, this.length);
   }

   public Object getCachedLiteral() {
      return this.buffer.getLiteral(this.index);
   }

   public void cacheLiteral(Object value) {
      this.buffer.putLiteral(this.index, value);
   }

   public Token getNextPrimary() {
      Token temp = this;

      do {
         temp = this.buffer.getNext(temp.index);
      } while (temp != null && !temp.type.primary);

      return temp;
   }

   public Token getPrev() {
      return this.buffer.getPrev(this.index);
   }

   public Token getNext() {
      return this.buffer.getNext(this.index);
   }

   public String getLineText() {
      return this.buffer.resource.getLine(this.line);
   }

   public String getPositionDisplay() {
      return this.line + 1 + ":" + (this.col + 1);
   }

   public List<Token> getComments() {
      List<Token> c = this.buffer.getComments(this.index);
      return c == null ? Collections.emptyList() : c;
   }

   @Override
   public String toString() {
      return switch (this.type) {
         case WS -> "_";
         case WS_LF -> "_\\n_";
         case EOF -> "_EOF_";
         default -> this.getText();
      };
   }
}
