package io.karatelabs.ext.sourcedoc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonSourceDocumentParser implements SourceDocumentParser {
   private static final int MAX_DEPTH = 200;

   @Override
   public SourceDocument parse(String var1, String var2) {
      return new io.karatelabs.ext.sourcedoc.JsonSourceDocumentParser.ParserState(var1, var2).parseDocument();
   }

   private static final class ParserState {
      private final String sourceName;
      private final String sourceText;
      private final SourceLineIndex lineIndex;
      private final Map<String, io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry> pointerEntries = new LinkedHashMap<>();
      private int offset;
      private int depth;

      ParserState(String var1, String var2) {
         this.sourceName = var1;
         this.sourceText = var2;
         this.lineIndex = io.karatelabs.ext.sourcedoc.SourceLineIndex.create(var2);
      }

      SourceDocument parseDocument() {
         this.skipWhitespace();
         if (this.offset >= this.sourceText.length()) {
            return new SourceDocument(this.sourceName, this.sourceText, null, this.pointerEntries, List.of());
         } else {
            Object var1 = this.parseValue("");
            this.skipWhitespace();
            if (this.offset < this.sourceText.length()) {
               throw this.syntaxError("trailing content after the document");
            } else {
               return new SourceDocument(this.sourceName, this.sourceText, var1, this.pointerEntries, List.of());
            }
         }
      }

      private Object parseValue(String var1) {
         this.skipWhitespace();
         char var2 = this.peekChar();
         int var3 = this.offset;
         if (++this.depth > 200) {
            throw this.syntaxError("nesting deeper than 200 levels; this is not an API description");
         } else {
            Object var4 = switch (var2) {
               case '"' -> this.parseString();
               case '[' -> this.parseArray(var1);
               case 'f' -> this.expectKeyword("false", Boolean.FALSE);
               case 'n' -> this.expectKeyword("null", null);
               case 't' -> this.expectKeyword("true", Boolean.TRUE);
               case '{' -> this.parseObject(var1);
               default -> this.parseNumber();
            };
            this.depth--;
            io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var5 = this.pointerEntryFor(var1);
            var5.valueSpan = this.lineIndex.spanBetween(var3, this.offset);
            if (var2 == '"') {
               var5.scalarText = (String)var4;
            } else if (var2 != '{' && var2 != '[') {
               var5.scalarText = this.sourceText.substring(var3, this.offset);
            }

            return var4;
         }
      }

      private Object parseObject(String var1) {
         this.expectChar('{');
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         this.skipWhitespace();
         if (this.peekChar() == '}') {
            this.offset++;
            return var2;
         } else {
            char var6;
            do {
               this.skipWhitespace();
               int var3 = this.offset;
               if (this.peekChar() != '"') {
                  throw this.syntaxError("an object key must be a quoted string");
               }

               String var4 = this.parseString();
               String var5 = var1 + "/" + io.karatelabs.ext.sourcedoc.SourceDocument.escapePointerSegment(var4);
               this.pointerEntryFor(var5).keySpan = this.lineIndex.spanBetween(var3, this.offset);
               this.skipWhitespace();
               this.expectChar(':');
               var2.put(var4, this.parseValue(var5));
               this.skipWhitespace();
               var6 = this.nextChar();
               if (var6 == '}') {
                  return var2;
               }
            } while (var6 == ',');

            throw this.syntaxError("expected ',' or '}' in an object");
         }
      }

      private Object parseArray(String var1) {
         this.expectChar('[');
         ArrayList<java.lang.Object> var2 = new ArrayList();
         this.skipWhitespace();
         if (this.peekChar() == ']') {
            this.offset++;
            return var2;
         } else {
            int var3 = 0;

            char var4;
            do {
               var2.add(this.parseValue(var1 + "/" + var3++));
               this.skipWhitespace();
               var4 = this.nextChar();
               if (var4 == ']') {
                  return var2;
               }
            } while (var4 == ',');

            throw this.syntaxError("expected ',' or ']' in an array");
         }
      }

      private String parseString() {
         this.expectChar('"');
         StringBuilder var1 = new StringBuilder();

         while (true) {
            char var2 = this.nextChar();
            if (var2 == '"') {
               return var1.toString();
            }

            if (var2 == '\\') {
               char var3 = this.nextChar();
               switch (var3) {
                  case '"':
                     var1.append('"');
                     break;
                  case '/':
                     var1.append('/');
                     break;
                  case '\\':
                     var1.append('\\');
                     break;
                  case 'b':
                     var1.append('\b');
                     break;
                  case 'f':
                     var1.append('\f');
                     break;
                  case 'n':
                     var1.append('\n');
                     break;
                  case 'r':
                     var1.append('\r');
                     break;
                  case 't':
                     var1.append('\t');
                     break;
                  case 'u':
                     if (this.offset + 4 > this.sourceText.length()) {
                        throw this.syntaxError("a truncated \\u escape");
                     }

                     String var4 = this.sourceText.substring(this.offset, this.offset + 4);

                     for (int var5 = 0; var5 < 4; var5++) {
                        if (Character.digit(var4.charAt(var5), 16) < 0) {
                           throw this.syntaxError("'\\u" + var4 + "' is not four hex digits");
                        }
                     }

                     var1.append((char)Integer.parseInt(var4, 16));
                     this.offset += 4;
                     break;
                  default:
                     throw this.syntaxError("unknown escape '\\" + var3 + "'");
               }
            } else {
               if (var2 < ' ') {
                  throw this.syntaxError("an unescaped control character in a string");
               }

               var1.append(var2);
            }
         }
      }

      private Object expectKeyword(String var1, Object var2) {
         if (!this.sourceText.startsWith(var1, this.offset)) {
            throw this.syntaxError("expected '" + var1 + "'");
         } else {
            this.offset = this.offset + var1.length();
            return var2;
         }
      }

      private Object parseNumber() {
         int var1 = this.offset;
         this.consumeIf('-');
         if (this.consumeIf('0')) {
            this.rejectLeadingZero();
         } else if (!this.consumeDigits()) {
            throw this.syntaxError("expected a number");
         }

         boolean var2 = false;
         if (this.consumeIf('.')) {
            var2 = true;
            if (!this.consumeDigits()) {
               throw this.syntaxError("a fraction needs at least one digit after the '.'");
            }
         }

         if (this.offset < this.sourceText.length() && (this.sourceText.charAt(this.offset) == 'e' || this.sourceText.charAt(this.offset) == 'E')) {
            this.offset++;
            var2 = true;
            if (!this.consumeIf('+')) {
               this.consumeIf('-');
            }

            if (!this.consumeDigits()) {
               throw this.syntaxError("an exponent needs at least one digit");
            }
         }

         String var3 = this.sourceText.substring(var1, this.offset);
         if (var2) {
            return Double.parseDouble(var3);
         } else {
            try {
               return Integer.valueOf(var3);
            } catch (NumberFormatException var7) {
               try {
                  return Long.valueOf(var3);
               } catch (NumberFormatException var6) {
                  return new BigInteger(var3);
               }
            }
         }
      }

      private void rejectLeadingZero() {
         if (this.offset < this.sourceText.length() && isDigit(this.sourceText.charAt(this.offset))) {
            throw this.syntaxError("a number may not have a leading zero");
         }
      }

      private boolean consumeDigits() {
         int var1 = this.offset;

         while (this.offset < this.sourceText.length() && isDigit(this.sourceText.charAt(this.offset))) {
            this.offset++;
         }

         return this.offset > var1;
      }

      private boolean consumeIf(char var1) {
         if (this.offset < this.sourceText.length() && this.sourceText.charAt(this.offset) == var1) {
            this.offset++;
            return true;
         } else {
            return false;
         }
      }

      private static boolean isDigit(char var0) {
         return var0 >= '0' && var0 <= '9';
      }

      private io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry pointerEntryFor(String var1) {
         return this.pointerEntries.computeIfAbsent(var1, var0 -> new io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry());
      }

      private void skipWhitespace() {
         while (this.offset < this.sourceText.length()) {
            char var1 = this.sourceText.charAt(this.offset);
            if (var1 != ' ' && var1 != '\t' && var1 != '\n' && var1 != '\r') {
               return;
            }

            this.offset++;
         }
      }

      private char peekChar() {
         if (this.offset >= this.sourceText.length()) {
            throw this.syntaxError("the document ended where a value was expected");
         } else {
            return this.sourceText.charAt(this.offset);
         }
      }

      private char nextChar() {
         char var1 = this.peekChar();
         this.offset++;
         return var1;
      }

      private void expectChar(char var1) {
         if (this.nextChar() != var1) {
            this.offset--;
            throw this.syntaxError("expected '" + var1 + "'");
         }
      }

      private SourceDocumentException syntaxError(String var1) {
         int var2 = Math.min(this.offset, Math.max(0, this.sourceText.length() - 1));
         return new SourceDocumentException(this.sourceName + ":" + this.lineIndex.lineAtOffset(var2) + ":" + this.lineIndex.columnAtOffset(var2) + " — " + var1);
      }
   }
}
