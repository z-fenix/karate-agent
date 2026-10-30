package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseLexer {
   protected final Resource resource;
   protected final TokenBuffer buffer;
   protected final String source;
   protected final int length;
   protected int pos;
   protected int line;
   protected int col;
   protected int tokenStart;
   protected int tokenLine;
   protected int tokenCol;

   protected BaseLexer(Resource resource) {
      this.resource = resource;
      this.buffer = new TokenBuffer(resource);
      this.source = resource.getText();
      this.length = this.source.length();
      this.pos = 0;
      this.line = resource.getLineOffset();
      this.col = 0;
   }

   public abstract Token nextToken();

   protected abstract TokenType scanToken();

   public static List<Token> tokenize(BaseLexer lexer) {
      List<Token> list = new ArrayList<>();
      List<Token> comments = null;

      Token token;
      do {
         token = lexer.nextToken();
         if (token.type.primary) {
            list.add(token);
            if (comments != null) {
               lexer.buffer.setComments(token.index, comments);
               comments = null;
            }
         } else if (token.type == TokenType.L_COMMENT || token.type == TokenType.G_COMMENT || token.type == TokenType.B_COMMENT) {
            if (comments == null) {
               comments = new ArrayList<>();
            }

            comments.add(token);
         }
      } while (token.type != TokenType.EOF);

      return list;
   }

   protected boolean isAtEnd() {
      return this.pos >= this.length;
   }

   protected char peek() {
      return this.pos >= this.length ? '\u0000' : this.source.charAt(this.pos);
   }

   protected char peek(int offset) {
      int index = this.pos + offset;
      return index >= 0 && index < this.length ? this.source.charAt(index) : '\u0000';
   }

   protected char advance() {
      char c = this.source.charAt(this.pos++);
      if (c == '\n') {
         this.line++;
         this.col = 0;
      } else {
         this.col++;
      }

      return c;
   }

   protected boolean match(char expected) {
      if (this.pos < this.length && this.source.charAt(this.pos) == expected) {
         this.advance();
         return true;
      } else {
         return false;
      }
   }

   protected static boolean isDigit(char c) {
      return c >= '0' && c <= '9';
   }

   protected static boolean isHexDigit(char c) {
      return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
   }

   protected static boolean isIdentifierStart(char c) {
      return Character.isJavaIdentifierStart(c);
   }

   protected static boolean isIdentifierPart(char c) {
      return Character.isJavaIdentifierPart(c);
   }
}
