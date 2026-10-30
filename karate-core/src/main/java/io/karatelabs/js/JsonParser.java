package io.karatelabs.js;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
   private JsonParser() {
   }

   public static Object parse(String input) {
      if (input == null) {
         throw JsErrorException.syntaxError("Unexpected end of JSON input");
      } else {
         JsonParser.ParseState state = new JsonParser.ParseState(input);
         state.skipWs();
         Object value = state.parseValue();
         state.skipWs();
         if (state.pos != input.length()) {
            throw state.syntaxError("Unexpected token after JSON value");
         } else {
            return value;
         }
      }
   }

   public static JsonSpan.Doc parseSpans(String input) {
      if (input == null) {
         throw JsErrorException.syntaxError("Unexpected end of JSON input");
      } else {
         JsonParser.SpanState state = new JsonParser.SpanState(input);
         boolean bom = !input.isEmpty() && input.charAt(0) == '\ufeff';
         if (bom) {
            state.pos = 1;
         }

         state.skipWs();
         JsonSpan root = state.parseSpan();
         state.skipWs();
         if (state.pos != input.length()) {
            throw state.syntaxError("Unexpected token after JSON value");
         } else {
            return new JsonSpan.Doc(bom, root);
         }
      }
   }

   private static class ParseState {
      final String s;
      final int len;
      int pos;

      ParseState(String s) {
         this.s = s;
         this.len = s.length();
      }

      Object parseValue() {
         if (this.pos >= this.len) {
            throw this.syntaxError("Unexpected end of JSON input");
         } else {
            char c = this.s.charAt(this.pos);
            switch (c) {
               case '"':
                  return this.parseString();
               case '[':
                  return this.parseArray();
               case 'f':
               case 't':
                  return this.parseBoolean();
               case 'n':
                  return this.parseNull();
               case '{':
                  return this.parseObject();
               default:
                  if (c != '-' && (c < '0' || c > '9')) {
                     throw this.syntaxError("Unexpected token '" + c + "' in JSON");
                  } else {
                     return this.parseNumber();
                  }
            }
         }
      }

      private Map<String, Object> parseObject() {
         this.pos++;
         Map<String, Object> map = new LinkedHashMap<>();
         this.skipWs();
         if (this.pos < this.len && this.s.charAt(this.pos) == '}') {
            this.pos++;
            return map;
         } else {
            while (true) {
               this.skipWs();
               if (this.pos < this.len && this.s.charAt(this.pos) == '"') {
                  String key = this.parseString();
                  this.skipWs();
                  if (this.pos < this.len && this.s.charAt(this.pos) == ':') {
                     this.pos++;
                     this.skipWs();
                     Object value = this.parseValue();
                     map.put(key, value);
                     this.skipWs();
                     if (this.pos >= this.len) {
                        throw this.syntaxError("Unexpected end of JSON input in object");
                     }

                     char c = this.s.charAt(this.pos);
                     if (c != ',') {
                        if (c == '}') {
                           this.pos++;
                           return map;
                        }

                        throw this.syntaxError("Expected ',' or '}' in object");
                     }

                     this.pos++;
                     continue;
                  }

                  throw this.syntaxError("Expected ':' after object key");
               }

               throw this.syntaxError("Expected string key in object");
            }
         }
      }

      private List<Object> parseArray() {
         this.pos++;
         List<Object> list = new ArrayList<>();
         this.skipWs();
         if (this.pos < this.len && this.s.charAt(this.pos) == ']') {
            this.pos++;
            return list;
         } else {
            while (true) {
               this.skipWs();
               list.add(this.parseValue());
               this.skipWs();
               if (this.pos >= this.len) {
                  throw this.syntaxError("Unexpected end of JSON input in array");
               }

               char c = this.s.charAt(this.pos);
               if (c != ',') {
                  if (c == ']') {
                     this.pos++;
                     return list;
                  }

                  throw this.syntaxError("Expected ',' or ']' in array");
               }

               this.pos++;
            }
         }
      }

      String parseString() {
         this.pos++;
         StringBuilder sb = new StringBuilder();

         while (this.pos < this.len) {
            char c = this.s.charAt(this.pos);
            if (c == '"') {
               this.pos++;
               return sb.toString();
            }

            if (c == '\\') {
               this.pos++;
               if (this.pos >= this.len) {
                  throw this.syntaxError("Unexpected end of JSON input in string escape");
               }

               char esc = this.s.charAt(this.pos);
               this.pos++;
               switch (esc) {
                  case '"':
                     sb.append('"');
                     break;
                  case '/':
                     sb.append('/');
                     break;
                  case '\\':
                     sb.append('\\');
                     break;
                  case 'b':
                     sb.append('\b');
                     break;
                  case 'f':
                     sb.append('\f');
                     break;
                  case 'n':
                     sb.append('\n');
                     break;
                  case 'r':
                     sb.append('\r');
                     break;
                  case 't':
                     sb.append('\t');
                     break;
                  case 'u':
                     sb.append(this.parseHex4());
                     break;
                  default:
                     throw this.syntaxError("Invalid escape '\\" + esc + "' in JSON string");
               }
            } else {
               if (c < ' ') {
                  throw this.syntaxError("Invalid control character in JSON string");
               }

               sb.append(c);
               this.pos++;
            }
         }

         throw this.syntaxError("Unterminated JSON string");
      }

      private char parseHex4() {
         if (this.pos + 4 > this.len) {
            throw this.syntaxError("Invalid \\u escape in JSON string");
         } else {
            int v = 0;

            for (int i = 0; i < 4; i++) {
               char h = this.s.charAt(this.pos + i);
               int d;
               if (h >= '0' && h <= '9') {
                  d = h - '0';
               } else if (h >= 'a' && h <= 'f') {
                  d = 10 + (h - 'a');
               } else {
                  if (h < 'A' || h > 'F') {
                     throw this.syntaxError("Invalid hex digit '" + h + "' in \\u escape");
                  }

                  d = 10 + (h - 'A');
               }

               v = v << 4 | d;
            }

            this.pos += 4;
            return (char)v;
         }
      }

      Object parseNumber() {
         int start = this.pos;
         boolean isFloat = false;
         if (this.s.charAt(this.pos) == '-') {
            this.pos++;
            if (this.pos >= this.len) {
               throw this.syntaxError("Invalid number: bare '-'");
            }
         }

         char c = this.s.charAt(this.pos);
         if (c == '0') {
            this.pos++;
         } else {
            if (c < '1' || c > '9') {
               throw this.syntaxError("Invalid number");
            }

            this.pos++;

            while (this.pos < this.len && this.s.charAt(this.pos) >= '0' && this.s.charAt(this.pos) <= '9') {
               this.pos++;
            }
         }

         if (this.pos < this.len && this.s.charAt(this.pos) == '.') {
            isFloat = true;
            this.pos++;
            int fracStart = this.pos;

            while (this.pos < this.len && this.s.charAt(this.pos) >= '0' && this.s.charAt(this.pos) <= '9') {
               this.pos++;
            }

            if (this.pos == fracStart) {
               throw this.syntaxError("Invalid number: missing fraction digits");
            }
         }

         if (this.pos < this.len && (this.s.charAt(this.pos) == 'e' || this.s.charAt(this.pos) == 'E')) {
            isFloat = true;
            this.pos++;
            if (this.pos < this.len && (this.s.charAt(this.pos) == '+' || this.s.charAt(this.pos) == '-')) {
               this.pos++;
            }

            int expStart = this.pos;

            while (this.pos < this.len && this.s.charAt(this.pos) >= '0' && this.s.charAt(this.pos) <= '9') {
               this.pos++;
            }

            if (this.pos == expStart) {
               throw this.syntaxError("Invalid number: missing exponent digits");
            }
         }

         String lit = this.s.substring(start, this.pos);
         if (isFloat) {
            return Double.parseDouble(lit);
         } else {
            try {
               long v = Long.parseLong(lit);
               return v >= -2147483648L && v <= 2147483647L ? (int)v : v;
            } catch (NumberFormatException var7) {
               return new BigInteger(lit);
            }
         }
      }

      Boolean parseBoolean() {
         if (this.s.charAt(this.pos) == 't') {
            if (this.pos + 4 <= this.len && this.s.charAt(this.pos + 1) == 'r' && this.s.charAt(this.pos + 2) == 'u' && this.s.charAt(this.pos + 3) == 'e') {
               this.pos += 4;
               return Boolean.TRUE;
            } else {
               throw this.syntaxError("Invalid literal — expected 'true'");
            }
         } else if (this.pos + 5 <= this.len
            && this.s.charAt(this.pos + 1) == 'a'
            && this.s.charAt(this.pos + 2) == 'l'
            && this.s.charAt(this.pos + 3) == 's'
            && this.s.charAt(this.pos + 4) == 'e') {
            this.pos += 5;
            return Boolean.FALSE;
         } else {
            throw this.syntaxError("Invalid literal — expected 'false'");
         }
      }

      Object parseNull() {
         if (this.pos + 4 <= this.len && this.s.charAt(this.pos + 1) == 'u' && this.s.charAt(this.pos + 2) == 'l' && this.s.charAt(this.pos + 3) == 'l') {
            this.pos += 4;
            return null;
         } else {
            throw this.syntaxError("Invalid literal — expected 'null'");
         }
      }

      void skipWs() {
         while (this.pos < this.len) {
            char c = this.s.charAt(this.pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
               return;
            }

            this.pos++;
         }
      }

      JsErrorException syntaxError(String message) {
         return JsErrorException.syntaxError(message + " at position " + this.pos);
      }
   }

   private static final class SpanState extends JsonParser.ParseState {
      SpanState(String s) {
         super(s);
      }

      JsonSpan parseSpan() {
         if (this.pos >= this.len) {
            throw this.syntaxError("Unexpected end of JSON input");
         } else {
            int start = this.pos;
            char c = this.s.charAt(this.pos);
            switch (c) {
               case '"':
                  return this.scalar(JsonSpan.Kind.STRING, start, this.parseString());
               case '[':
                  return this.parseArraySpan();
               case 'f':
               case 't':
                  Boolean b = this.parseBoolean();
                  return this.scalar(b ? JsonSpan.Kind.TRUE : JsonSpan.Kind.FALSE, start, b);
               case 'n':
                  return this.scalar(JsonSpan.Kind.NULL, start, this.parseNull());
               case '{':
                  return this.parseObjectSpan();
               default:
                  if (c != '-' && (c < '0' || c > '9')) {
                     throw this.syntaxError("Unexpected token '" + c + "' in JSON");
                  } else {
                     return this.scalar(JsonSpan.Kind.NUMBER, start, this.parseNumber());
                  }
            }
         }
      }

      private JsonSpan scalar(JsonSpan.Kind kind, int start, Object value) {
         return new JsonSpan(kind, start, this.pos, List.of(), List.of(), value);
      }

      private JsonSpan parseObjectSpan() {
         int start = this.pos++;
         List<JsonSpan.Member> members = new ArrayList<>();
         this.skipWs();
         if (this.pos < this.len && this.s.charAt(this.pos) == '}') {
            this.pos++;
            return new JsonSpan(JsonSpan.Kind.OBJECT, start, this.pos, List.of(), List.of(), null);
         } else {
            while (true) {
               this.skipWs();
               if (this.pos < this.len && this.s.charAt(this.pos) == '"') {
                  int keyStart = this.pos;
                  String key = this.parseString();
                  int keyEnd = this.pos;
                  this.skipWs();
                  if (this.pos < this.len && this.s.charAt(this.pos) == ':') {
                     this.pos++;
                     this.skipWs();
                     members.add(new JsonSpan.Member(key, keyStart, keyEnd, this.parseSpan()));
                     this.skipWs();
                     if (this.pos >= this.len) {
                        throw this.syntaxError("Unexpected end of JSON input in object");
                     }

                     char c = this.s.charAt(this.pos);
                     if (c != ',') {
                        if (c == '}') {
                           this.pos++;
                           return new JsonSpan(JsonSpan.Kind.OBJECT, start, this.pos, List.copyOf(members), List.of(), null);
                        }

                        throw this.syntaxError("Expected ',' or '}' in object");
                     }

                     this.pos++;
                     continue;
                  }

                  throw this.syntaxError("Expected ':' after object key");
               }

               throw this.syntaxError("Expected string key in object");
            }
         }
      }

      private JsonSpan parseArraySpan() {
         int start = this.pos++;
         List<JsonSpan> elements = new ArrayList<>();
         this.skipWs();
         if (this.pos < this.len && this.s.charAt(this.pos) == ']') {
            this.pos++;
            return new JsonSpan(JsonSpan.Kind.ARRAY, start, this.pos, List.of(), List.of(), null);
         } else {
            while (true) {
               this.skipWs();
               elements.add(this.parseSpan());
               this.skipWs();
               if (this.pos >= this.len) {
                  throw this.syntaxError("Unexpected end of JSON input in array");
               }

               char c = this.s.charAt(this.pos);
               if (c != ',') {
                  if (c == ']') {
                     this.pos++;
                     return new JsonSpan(JsonSpan.Kind.ARRAY, start, this.pos, List.of(), List.copyOf(elements), null);
                  }

                  throw this.syntaxError("Expected ',' or ']' in array");
               }

               this.pos++;
            }
         }
      }
   }
}
