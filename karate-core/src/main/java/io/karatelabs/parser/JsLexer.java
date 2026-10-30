package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

public class JsLexer extends BaseLexer {
   private boolean regexAllowed = true;
   private final ArrayDeque<JsLexer.LexerState> stateStack = new ArrayDeque<>();
   private static final char VERTICAL_TILDE = 'ⸯ';
   static final Set<String> RESERVED_WORDS = Set.of(
      "break",
      "case",
      "catch",
      "class",
      "const",
      "continue",
      "debugger",
      "default",
      "delete",
      "do",
      "else",
      "enum",
      "export",
      "extends",
      "false",
      "finally",
      "for",
      "function",
      "if",
      "import",
      "in",
      "instanceof",
      "new",
      "null",
      "return",
      "super",
      "switch",
      "this",
      "throw",
      "true",
      "try",
      "typeof",
      "var",
      "void",
      "while",
      "with"
   );

   public JsLexer(Resource resource) {
      super(resource);
      this.stateStack.push(JsLexer.LexerState.INITIAL);
   }

   public static List<Token> getTokens(Resource resource) {
      return tokenize(new JsLexer(resource));
   }

   @Override
   public Token nextToken() {
      this.tokenStart = this.pos;
      this.tokenLine = this.line;
      this.tokenCol = this.col;
      TokenType type = this.scanToken();
      int length = this.pos - this.tokenStart;
      if (type.regexAllowed != null) {
         this.regexAllowed = type.regexAllowed;
      }

      return new Token(this.buffer, type, this.tokenStart, this.tokenLine, this.tokenCol, length);
   }

   protected JsLexer.LexerState currentState() {
      return this.stateStack.peek();
   }

   protected void pushState(JsLexer.LexerState state) {
      this.stateStack.push(state);
   }

   protected JsLexer.LexerState popState() {
      return this.stateStack.size() > 1 ? this.stateStack.pop() : this.stateStack.peek();
   }

   @Override
   protected TokenType scanToken() {
      if (this.isAtEnd()) {
         return TokenType.EOF;
      } else {
         JsLexer.LexerState state = this.currentState();
         if (state == JsLexer.LexerState.TEMPLATE) {
            return this.scanTemplateContent();
         } else {
            char c = this.source.charAt(this.pos);
            if (c != ' '
               && c != '\t'
               && c != '\r'
               && c != '\n'
               && c != 11
               && c != '\f'
               && c != 160
               && c != '\ufeff'
               && c != 8232
               && c != 8233
               && (c <= 127 || Character.getType(c) != 12)) {
               if (c == '/') {
                  return this.scanSlash();
               } else if (c == '"') {
                  return this.scanDoubleString();
               } else if (c == '\'') {
                  return this.scanSingleString();
               } else if (c == '`') {
                  this.advance();
                  this.pushState(JsLexer.LexerState.TEMPLATE);
                  return TokenType.BACKTICK;
               } else if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '_' && c != '$') {
                  if ((c < '0' || c > '9') && (c != '.' || this.pos + 1 >= this.length || !isDigit(this.source.charAt(this.pos + 1)))) {
                     if (c == '#') {
                        return this.pos == 0 && this.peek(1) == '!' ? this.scanLineComment() : this.scanPrivateName();
                     } else {
                        return (c <= 127 || !Character.isJavaIdentifierStart(c) || c == 11823)
                              && (c != '\\' || this.peek(1) != 'u')
                              && (!Character.isHighSurrogate(c) || !isIdentifierCodePoint(this.source.codePointAt(this.pos), true))
                           ? this.scanOperator()
                           : this.scanIdentifier();
                     }
                  } else {
                     return this.scanNumber();
                  }
               } else {
                  return this.scanIdentifier();
               }
            } else {
               return this.scanWhitespace();
            }
         }
      }
   }

   private TokenType scanWhitespace() {
      boolean hasNewline = false;

      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c != ' ' && c != '\t' && c != 11 && c != '\f' && c != 160 && c != '\ufeff' && (c <= 127 || Character.getType(c) != 12)) {
            if (c != '\n' && c != 8232 && c != 8233) {
               if (c != '\r') {
                  break;
               }

               this.pos++;
               this.col++;
               hasNewline = true;
            } else {
               this.pos++;
               this.line++;
               this.col = 0;
               hasNewline = true;
            }
         } else {
            this.pos++;
            this.col++;
         }
      }

      return hasNewline ? TokenType.WS_LF : TokenType.WS;
   }

   private TokenType scanSlash() {
      this.advance();
      if (this.match('/')) {
         return this.scanLineComment();
      } else if (this.match('*')) {
         return this.scanBlockComment();
      } else if (this.regexAllowed) {
         return this.scanRegex();
      } else {
         return this.match('=') ? TokenType.SLASH_EQ : TokenType.SLASH;
      }
   }

   private TokenType scanLineComment() {
      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c != '\n' && c != '\r') {
            this.pos++;
            this.col++;
            continue;
         }
         break;
      }

      return TokenType.L_COMMENT;
   }

   private TokenType scanBlockComment() {
      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c == '*' && this.pos + 1 < this.length && this.source.charAt(this.pos + 1) == '/') {
            this.pos += 2;
            this.col += 2;
            break;
         }

         if (c == '\n') {
            this.pos++;
            this.line++;
            this.col = 0;
         } else {
            this.pos++;
            this.col++;
         }
      }

      return TokenType.B_COMMENT;
   }

   private TokenType scanRegex() {
      boolean inCharClass = false;

      while (!this.isAtEnd()) {
         char c = this.peek();
         if (c == '\n' || c == '\r') {
            break;
         }

         if (c == '\\') {
            this.advance();
            if (!this.isAtEnd() && this.peek() != '\n' && this.peek() != '\r') {
               this.advance();
            }
         } else if (c == '[') {
            inCharClass = true;
            this.advance();
         } else if (c == ']' && inCharClass) {
            inCharClass = false;
            this.advance();
         } else {
            if (c == '/' && !inCharClass) {
               this.advance();

               while (!this.isAtEnd() && isIdentifierPart(this.peek())) {
                  this.advance();
               }
               break;
            }

            this.advance();
         }
      }

      return TokenType.REGEX;
   }

   private TokenType scanDoubleString() {
      this.pos++;
      this.col++;

      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c == '"') {
            this.pos++;
            this.col++;
            break;
         }

         if (c == '\\' && this.pos + 1 < this.length) {
            char next = this.source.charAt(this.pos + 1);
            if (next == '\n') {
               this.pos += 2;
               this.line++;
               this.col = 0;
            } else {
               this.pos += 2;
               this.col += 2;
            }
         } else if (c == '\n') {
            this.pos++;
            this.line++;
            this.col = 0;
         } else {
            this.pos++;
            this.col++;
         }
      }

      return TokenType.D_STRING;
   }

   private TokenType scanSingleString() {
      this.pos++;
      this.col++;

      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c == '\'') {
            this.pos++;
            this.col++;
            break;
         }

         if (c == '\\' && this.pos + 1 < this.length) {
            char next = this.source.charAt(this.pos + 1);
            if (next == '\n') {
               this.pos += 2;
               this.line++;
               this.col = 0;
            } else {
               this.pos += 2;
               this.col += 2;
            }
         } else if (c == '\n') {
            this.pos++;
            this.line++;
            this.col = 0;
         } else {
            this.pos++;
            this.col++;
         }
      }

      return TokenType.S_STRING;
   }

   private TokenType scanTemplateContent() {
      if (this.isAtEnd()) {
         return TokenType.EOF;
      } else {
         char c = this.peek();
         if (c == '`') {
            this.advance();
            this.popState();
            return TokenType.BACKTICK;
         } else if (c == '$' && this.peek(1) == '{') {
            this.advance();
            this.advance();
            this.pushState(JsLexer.LexerState.PLACEHOLDER);
            return TokenType.DOLLAR_L_CURLY;
         } else {
            while (!this.isAtEnd()) {
               c = this.peek();
               if (c != '`' && (c != '$' || this.peek(1) != '{')) {
                  if (c != '\\') {
                     this.advance();
                     continue;
                  } else {
                     this.advance();
                     if (!this.isAtEnd()) {
                        char esc = this.peek();
                        if (esc >= '1' && esc <= '9') {
                           throw new ParserException("invalid escape sequence in template literal: \\" + esc);
                        }

                        if (esc == '0' && this.peek(1) >= '0' && this.peek(1) <= '9') {
                           throw new ParserException("invalid escape sequence in template literal: \\0" + this.peek(1));
                        }

                        if (esc != 'x' || isHexDigit(this.peek(1)) && isHexDigit(this.peek(2))) {
                           if (esc == 'u') {
                              char next = this.peek(1);
                              if (next == '{') {
                                 int j = 2;
                                 if (!isHexDigit(this.peek(j))) {
                                    throw new ParserException("invalid unicode escape sequence in template literal");
                                 }

                                 for (int codepoint = 0; isHexDigit(this.peek(j)); j++) {
                                    char h = this.peek(j);
                                    int v = h >= '0' && h <= '9' ? h - '0' : (h >= 'a' && h <= 'f' ? h - 'a' + 10 : h - 'A' + 10);
                                    codepoint = codepoint << 4 | v;
                                    if (codepoint > 1114111) {
                                       throw new ParserException("unicode escape sequence out of range in template literal");
                                    }
                                 }

                                 if (this.peek(j) != '}') {
                                    throw new ParserException("invalid unicode escape sequence in template literal");
                                 }
                              } else if (!isHexDigit(this.peek(1)) || !isHexDigit(this.peek(2)) || !isHexDigit(this.peek(3)) || !isHexDigit(this.peek(4))) {
                                 throw new ParserException("invalid unicode escape sequence in template literal");
                              }
                           }

                           this.advance();
                           continue;
                        }

                        throw new ParserException("invalid hex escape sequence in template literal");
                     }
                  }
               }
               break;
            }

            return TokenType.T_STRING;
         }
      }
   }

   private TokenType scanNumber() {
      char c = this.peek();
      if (c != '0' || this.peek(1) != 'x' && this.peek(1) != 'X') {
         if (c != '0' || this.peek(1) != 'b' && this.peek(1) != 'B') {
            if (c != '0' || this.peek(1) != 'o' && this.peek(1) != 'O') {
               boolean isInteger = true;
               if (c == '.') {
                  isInteger = false;
                  this.advance();
               } else {
                  while (!this.isAtEnd() && isDigit(this.peek())) {
                     this.advance();
                  }

                  if (!this.isAtEnd() && this.peek() == '_') {
                     this.scanDigitsWithSeparators();
                  }

                  if (this.peek() == '.') {
                     isInteger = false;
                     this.advance();
                  }
               }

               while (!this.isAtEnd() && isDigit(this.peek())) {
                  this.advance();
               }

               if (!this.isAtEnd() && this.peek() == '_') {
                  this.scanDigitsWithSeparators();
                  isInteger = false;
               }

               char p = this.peek();
               if (p == 'e' || p == 'E') {
                  isInteger = false;
                  this.advance();
                  if (this.peek() == '+' || this.peek() == '-') {
                     this.advance();
                  }

                  int expDigitsStart = this.pos;

                  while (!this.isAtEnd() && isDigit(this.peek())) {
                     this.advance();
                  }

                  if (!this.isAtEnd() && this.peek() == '_') {
                     if (this.pos == expDigitsStart) {
                        throw new ParserException("invalid numeric separator");
                     }

                     this.scanDigitsWithSeparators();
                  } else if (this.pos == expDigitsStart) {
                     throw new ParserException("missing exponent digits");
                  }
               }

               if (isInteger && !this.isAtEnd() && this.peek() == 'n') {
                  this.advance();
                  return TokenType.BIGINT;
               } else {
                  return TokenType.NUMBER;
               }
            } else {
               this.advance();
               this.advance();
               int octStart = this.pos;

               while (!this.isAtEnd() && this.peek() >= '0' && this.peek() <= '7') {
                  this.advance();
               }

               if (this.pos == octStart) {
                  throw new ParserException("invalid octal literal");
               } else {
                  if (!this.isAtEnd() && this.peek() == '_') {
                     this.scanOctalDigitsWithSeparators();
                  }

                  if (!this.isAtEnd() && this.peek() == 'n') {
                     this.advance();
                     return TokenType.BIGINT;
                  } else {
                     return TokenType.NUMBER;
                  }
               }
            }
         } else {
            this.advance();
            this.advance();
            int binStart = this.pos;

            while (!this.isAtEnd() && (this.peek() == '0' || this.peek() == '1')) {
               this.advance();
            }

            if (this.pos == binStart) {
               throw new ParserException("invalid binary literal");
            } else {
               if (!this.isAtEnd() && this.peek() == '_') {
                  this.scanBinaryDigitsWithSeparators();
               }

               if (!this.isAtEnd() && this.peek() == 'n') {
                  this.advance();
                  return TokenType.BIGINT;
               } else {
                  return TokenType.NUMBER;
               }
            }
         }
      } else {
         this.advance();
         this.advance();
         int hexStart = this.pos;

         while (!this.isAtEnd() && isHexDigit(this.peek())) {
            this.advance();
         }

         if (!this.isAtEnd() && this.peek() == '_') {
            if (this.pos == hexStart) {
               throw new ParserException("invalid numeric separator");
            }

            this.scanHexDigitsWithSeparators();
         }

         if (!this.isAtEnd() && this.peek() == 'n') {
            this.advance();
            return TokenType.BIGINT;
         } else {
            return TokenType.NUMBER;
         }
      }
   }

   private void scanDigitsWithSeparators() {
      while (!this.isAtEnd() && this.peek() == '_') {
         this.advance();
         if (this.isAtEnd() || !isDigit(this.peek())) {
            throw new ParserException("invalid numeric separator");
         }

         while (!this.isAtEnd() && isDigit(this.peek())) {
            this.advance();
         }
      }
   }

   private void scanHexDigitsWithSeparators() {
      while (!this.isAtEnd() && this.peek() == '_') {
         this.advance();
         if (this.isAtEnd() || !isHexDigit(this.peek())) {
            throw new ParserException("invalid numeric separator");
         }

         while (!this.isAtEnd() && isHexDigit(this.peek())) {
            this.advance();
         }
      }
   }

   private void scanBinaryDigitsWithSeparators() {
      while (!this.isAtEnd() && this.peek() == '_') {
         this.advance();
         if (this.isAtEnd() || this.peek() != '0' && this.peek() != '1') {
            throw new ParserException("invalid numeric separator");
         }

         while (!this.isAtEnd() && (this.peek() == '0' || this.peek() == '1')) {
            this.advance();
         }
      }
   }

   private void scanOctalDigitsWithSeparators() {
      while (!this.isAtEnd() && this.peek() == '_') {
         this.advance();
         if (this.isAtEnd() || this.peek() < '0' || this.peek() > '7') {
            throw new ParserException("invalid numeric separator");
         }

         while (!this.isAtEnd() && this.peek() >= '0' && this.peek() <= '7') {
            this.advance();
         }
      }
   }

   private TokenType scanIdentifier() {
      boolean escaped = false;

      while (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_' && c != '$') {
            if (c <= 127 || c != 8204 && c != 8205 && (!Character.isUnicodeIdentifierPart(c) || Character.isIdentifierIgnorable(c) || c == 11823)) {
               if (c == '\\' && this.peek(1) == 'u') {
                  this.scanIdentifierEscape(this.pos == this.tokenStart);
                  escaped = this.buffer.escapedIdentifiers = true;
               } else {
                  if (!Character.isHighSurrogate(c) || !isIdentifierCodePoint(this.source.codePointAt(this.pos), this.pos == this.tokenStart)) {
                     break;
                  }

                  this.pos += 2;
                  this.col += 2;
               }
            } else {
               this.pos++;
               this.col++;
            }
         } else {
            this.pos++;
            this.col++;
         }
      }

      return escaped ? TokenType.IDENT : this.keywordOrIdent(this.tokenStart, this.pos - this.tokenStart);
   }

   private void scanIdentifierEscape(boolean start) {
      int cp = -1;
      if (this.peek(2) == '{') {
         int j = 3;

         long value;
         for (value = 0L; isHexDigit(this.peek(j)) && value <= 1114111L; j++) {
            value = value * 16L + Character.digit(this.peek(j), 16);
         }

         if (j > 3 && this.peek(j) == '}' && value <= 1114111L) {
            cp = (int)value;
            this.pos += j + 1;
            this.col += j + 1;
         }
      } else if (isHexDigit(this.peek(2)) && isHexDigit(this.peek(3)) && isHexDigit(this.peek(4)) && isHexDigit(this.peek(5))) {
         cp = Integer.parseInt(this.source.substring(this.pos + 2, this.pos + 6), 16);
         this.pos += 6;
         this.col += 6;
      }

      if (cp == -1 || cp != 36 && cp != 95 && !isIdentifierCodePoint(cp, start)) {
         throw new ParserException(String.format("invalid escape sequence in identifier at %d:%d", this.tokenLine + 1, this.tokenCol + 1));
      }
   }

   private static boolean isIdentifierCodePoint(int cp, boolean start) {
      if (cp == 11823) {
         return false;
      } else if (Character.getType(cp) == 0) {
         return true;
      } else {
         return start
            ? Character.isUnicodeIdentifierStart(cp)
            : cp == 8204 || cp == 8205 || Character.isUnicodeIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp);
      }
   }

   static String identifierName(String raw) {
      if (raw.indexOf(92) == -1) {
         return raw;
      } else {
         StringBuilder sb = new StringBuilder(raw.length());

         for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\') {
               sb.append(c);
            } else if (raw.charAt(i + 2) == '{') {
               int end = raw.indexOf(125, i);
               sb.appendCodePoint(Integer.parseInt(raw.substring(i + 3, end), 16));
               i = end;
            } else {
               sb.append((char)Integer.parseInt(raw.substring(i + 2, i + 6), 16));
               i += 5;
            }
         }

         return sb.toString();
      }
   }

   private TokenType scanPrivateName() {
      this.advance();
      if (this.pos < this.length) {
         char c = this.source.charAt(this.pos);
         if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$' || c > 127 && Character.isJavaIdentifierStart(c)) {
            this.scanIdentifier();
            return TokenType.PRIVATE_NAME;
         }
      }

      throw new ParserException(String.format("unexpected character '#' at %d:%d", this.tokenLine + 1, this.tokenCol + 1));
   }

   private TokenType keywordOrIdent(int start, int len) {
      if (len == 2) {
         char c0 = this.source.charAt(start);
         if (c0 == 'i') {
            if (this.source.charAt(start + 1) == 'f') {
               return TokenType.IF;
            }

            if (this.source.charAt(start + 1) == 'n') {
               return TokenType.IN;
            }
         } else {
            if (c0 == 'o' && this.source.charAt(start + 1) == 'f') {
               return TokenType.OF;
            }

            if (c0 == 'd' && this.source.charAt(start + 1) == 'o') {
               return TokenType.DO;
            }
         }
      } else if (len == 3) {
         char c0 = this.source.charAt(start);
         if (c0 == 'v' && this.source.charAt(start + 1) == 'a' && this.source.charAt(start + 2) == 'r') {
            return TokenType.VAR;
         }

         if (c0 == 'l' && this.source.charAt(start + 1) == 'e' && this.source.charAt(start + 2) == 't') {
            return TokenType.LET;
         }

         if (c0 == 'n' && this.source.charAt(start + 1) == 'e' && this.source.charAt(start + 2) == 'w') {
            return TokenType.NEW;
         }

         if (c0 == 't' && this.source.charAt(start + 1) == 'r' && this.source.charAt(start + 2) == 'y') {
            return TokenType.TRY;
         }

         if (c0 == 'f' && this.source.charAt(start + 1) == 'o' && this.source.charAt(start + 2) == 'r') {
            return TokenType.FOR;
         }
      } else if (len == 4) {
         char c0x = this.source.charAt(start);
         if (c0x == 'n' && this.matchKeyword(start, "null")) {
            return TokenType.NULL;
         }

         if (c0x == 't' && this.matchKeyword(start, "true")) {
            return TokenType.TRUE;
         }

         if (c0x == 'e' && this.matchKeyword(start, "else")) {
            return TokenType.ELSE;
         }

         if (c0x == 'c' && this.matchKeyword(start, "case")) {
            return TokenType.CASE;
         }

         if (c0x == 'v' && this.matchKeyword(start, "void")) {
            return TokenType.VOID;
         }
      } else if (len == 5) {
         char c0xx = this.source.charAt(start);
         if (c0xx == 'f' && this.matchKeyword(start, "false")) {
            return TokenType.FALSE;
         }

         if (c0xx == 'c') {
            if (this.matchKeyword(start, "const")) {
               return TokenType.CONST;
            }

            if (this.matchKeyword(start, "catch")) {
               return TokenType.CATCH;
            }

            if (this.matchKeyword(start, "class")) {
               return TokenType.CLASS;
            }
         }

         if (c0xx == 't' && this.matchKeyword(start, "throw")) {
            return TokenType.THROW;
         }

         if (c0xx == 'w' && this.matchKeyword(start, "while")) {
            return TokenType.WHILE;
         }

         if (c0xx == 'b' && this.matchKeyword(start, "break")) {
            return TokenType.BREAK;
         }

         if (c0xx == 's' && this.matchKeyword(start, "super")) {
            return TokenType.SUPER;
         }
      } else if (len == 6) {
         char c0xxx = this.source.charAt(start);
         if (c0xxx == 'r' && this.matchKeyword(start, "return")) {
            return TokenType.RETURN;
         }

         if (c0xxx == 't' && this.matchKeyword(start, "typeof")) {
            return TokenType.TYPEOF;
         }

         if (c0xxx == 'd' && this.matchKeyword(start, "delete")) {
            return TokenType.DELETE;
         }

         if (c0xxx == 's' && this.matchKeyword(start, "switch")) {
            return TokenType.SWITCH;
         }
      } else if (len == 7) {
         char c0xxxx = this.source.charAt(start);
         if (c0xxxx == 'f' && this.matchKeyword(start, "finally")) {
            return TokenType.FINALLY;
         }

         if (c0xxxx == 'd' && this.matchKeyword(start, "default")) {
            return TokenType.DEFAULT;
         }

         if (c0xxxx == 'e' && this.matchKeyword(start, "extends")) {
            return TokenType.EXTENDS;
         }
      } else if (len == 8) {
         char c0xxxxx = this.source.charAt(start);
         if (c0xxxxx == 'f' && this.matchKeyword(start, "function")) {
            return TokenType.FUNCTION;
         }

         if (c0xxxxx == 'c' && this.matchKeyword(start, "continue")) {
            return TokenType.CONTINUE;
         }
      } else if (len == 10 && this.source.charAt(start) == 'i' && this.matchKeyword(start, "instanceof")) {
         return TokenType.INSTANCEOF;
      }

      return TokenType.IDENT;
   }

   private boolean matchKeyword(int start, String keyword) {
      int len = keyword.length();

      for (int i = 0; i < len; i++) {
         if (this.source.charAt(start + i) != keyword.charAt(i)) {
            return false;
         }
      }

      return true;
   }

   private TokenType scanOperator() {
      char c = this.advance();
      switch (c) {
         case '!':
            if (this.match('=')) {
               return this.match('=') ? TokenType.NOT_EQ_EQ : TokenType.NOT_EQ;
            }

            return TokenType.NOT;
         case '"':
         case '#':
         case '$':
         case '\'':
         case '/':
         case '0':
         case '1':
         case '2':
         case '3':
         case '4':
         case '5':
         case '6':
         case '7':
         case '8':
         case '9':
         case '@':
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         case 'Z':
         case '_':
         case '`':
         case 'a':
         case 'b':
         case 'c':
         case 'd':
         case 'e':
         case 'f':
         case 'g':
         case 'h':
         case 'i':
         case 'j':
         case 'k':
         case 'l':
         case 'm':
         case 'n':
         case 'o':
         case 'p':
         case 'q':
         case 'r':
         case 's':
         case 't':
         case 'u':
         case 'v':
         case 'w':
         case 'x':
         case 'y':
         case 'z':
         default:
            if (c > 127) {
               int type = Character.getType(c);
               if (type == 16 || type == 15 || c == 11823 || type == 13 || type == 14) {
                  throw new ParserException(String.format("unexpected character '\\u%04X' at %d:%d", Integer.valueOf(c), this.tokenLine + 1, this.tokenCol + 1));
               }
            }

            return TokenType.IDENT;
         case '%':
            return this.match('=') ? TokenType.PERCENT_EQ : TokenType.PERCENT;
         case '&':
            if (this.match('&')) {
               return this.match('=') ? TokenType.AMP_AMP_EQ : TokenType.AMP_AMP;
            }

            return this.match('=') ? TokenType.AMP_EQ : TokenType.AMP;
         case '(':
            return TokenType.L_PAREN;
         case ')':
            return TokenType.R_PAREN;
         case '*':
            if (this.match('*')) {
               return this.match('=') ? TokenType.STAR_STAR_EQ : TokenType.STAR_STAR;
            }

            return this.match('=') ? TokenType.STAR_EQ : TokenType.STAR;
         case '+':
            if (this.match('+')) {
               return TokenType.PLUS_PLUS;
            }

            return this.match('=') ? TokenType.PLUS_EQ : TokenType.PLUS;
         case ',':
            return TokenType.COMMA;
         case '-':
            if (this.match('-')) {
               return TokenType.MINUS_MINUS;
            }

            return this.match('=') ? TokenType.MINUS_EQ : TokenType.MINUS;
         case '.':
            if (this.match('.') && this.match('.')) {
               return TokenType.DOT_DOT_DOT;
            }

            return TokenType.DOT;
         case ':':
            return TokenType.COLON;
         case ';':
            return TokenType.SEMI;
         case '<':
            if (this.match('<')) {
               return this.match('=') ? TokenType.LT_LT_EQ : TokenType.LT_LT;
            }

            return this.match('=') ? TokenType.LT_EQ : TokenType.LT;
         case '=':
            if (this.match('=')) {
               return this.match('=') ? TokenType.EQ_EQ_EQ : TokenType.EQ_EQ;
            } else {
               if (this.match('>')) {
                  return TokenType.EQ_GT;
               }

               return TokenType.EQ;
            }
         case '>':
            if (this.match('>')) {
               if (this.match('>')) {
                  return this.match('=') ? TokenType.GT_GT_GT_EQ : TokenType.GT_GT_GT;
               }

               return this.match('=') ? TokenType.GT_GT_EQ : TokenType.GT_GT;
            }

            return this.match('=') ? TokenType.GT_EQ : TokenType.GT;
         case '?':
            if (this.peek() == '.' && !isDigit(this.peek(1))) {
               this.match('.');
               return TokenType.QUES_DOT;
            } else {
               if (this.match('?')) {
                  return this.match('=') ? TokenType.QUES_QUES_EQ : TokenType.QUES_QUES;
               }

               return TokenType.QUES;
            }
         case '[':
            return TokenType.L_BRACKET;
         case '\\':
            throw new ParserException(String.format("unexpected character '\\' at %d:%d", this.tokenLine + 1, this.tokenCol + 1));
         case ']':
            return TokenType.R_BRACKET;
         case '^':
            return this.match('=') ? TokenType.CARET_EQ : TokenType.CARET;
         case '{':
            if (this.currentState() == JsLexer.LexerState.PLACEHOLDER) {
               this.pushState(JsLexer.LexerState.PLACEHOLDER);
            }

            return TokenType.L_CURLY;
         case '|':
            if (this.match('|')) {
               return this.match('=') ? TokenType.PIPE_PIPE_EQ : TokenType.PIPE_PIPE;
            }

            return this.match('=') ? TokenType.PIPE_EQ : TokenType.PIPE;
         case '}':
            if (this.currentState() == JsLexer.LexerState.PLACEHOLDER) {
               this.popState();
            }

            return TokenType.R_CURLY;
         case '~':
            return TokenType.TILDE;
      }
   }

   public static String unescapeStringLiteral(String s) {
      if (s.indexOf(92) == -1) {
         return s;
      } else {
         StringBuilder sb = new StringBuilder(s.length());

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
               char next = s.charAt(i + 1);
               switch (next) {
                  case '"':
                     sb.append('"');
                     i++;
                     break;
                  case '\'':
                     sb.append('\'');
                     i++;
                     break;
                  case '0':
                     sb.append('\u0000');
                     i++;
                     break;
                  case '\\':
                     sb.append('\\');
                     i++;
                     break;
                  case 'b':
                     sb.append('\b');
                     i++;
                     break;
                  case 'f':
                     sb.append('\f');
                     i++;
                     break;
                  case 'n':
                     sb.append('\n');
                     i++;
                     break;
                  case 'r':
                     sb.append('\r');
                     i++;
                     break;
                  case 't':
                     sb.append('\t');
                     i++;
                     break;
                  case 'u':
                     if (i + 5 < s.length()) {
                        try {
                           int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                           sb.append((char)code);
                           i += 5;
                        } catch (NumberFormatException var7) {
                           sb.append(c);
                        }
                     } else {
                        sb.append(c);
                     }
                     break;
                  case 'v':
                     sb.append('\u000b');
                     i++;
                     break;
                  case 'x':
                     if (i + 3 < s.length()) {
                        try {
                           int code = Integer.parseInt(s.substring(i + 2, i + 4), 16);
                           sb.append((char)code);
                           i += 3;
                        } catch (NumberFormatException var6) {
                           sb.append(c);
                        }
                     } else {
                        sb.append(c);
                     }
                     break;
                  default:
                     sb.append(c);
               }
            } else {
               sb.append(c);
            }
         }

         return sb.toString();
      }
   }

   protected static enum LexerState {
      INITIAL,
      TEMPLATE,
      PLACEHOLDER;
   }
}
