package io.karatelabs.ext.jsformat;

import io.karatelabs.common.Resource;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.parser.JsLexer;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.ParserException;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsFormatter {
   static final int DEFAULT_LINE_WIDTH = 100;
   static final int MAX_NESTING_DEPTH = 200;
   private static final int INDENT_WIDTH = 2;
   private static final Pattern PARSER_LOCATION_PATTERN = Pattern.compile("^(\\d+):(\\d+) ", 8);
   private static final String RECURSION_ERROR_MESSAGE = "too much recursion";
   private static final Pattern DECIMAL_LITERAL_PATTERN = Pattern.compile("(0|[1-9][0-9]*)?(\\.[0-9]*)?([eE][+-]?[0-9]+)?");

   private JsFormatter() {
   }

   public static String format(String var0) {
      return formatTree(parseProgram(stripBom(var0)));
   }

   public static String formatTree(Node var0) {
      try {
         return formatTreeWithOracleCheck(var0);
      } catch (JsFormatException var2) {
         throw var2;
      } catch (StackOverflowError var3) {
         throw nestingLimitException((Token)null);
      } catch (RuntimeException var4) {
         throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.UNSUPPORTED_NODE, "unexpected tree shape: " + var4, 0, 0, -1, null);
      }
   }

   private static String formatTreeWithOracleCheck(Node var0) {
      checkNestingDepth(var0);
      Token var1 = var0.getFirstToken();
      if (var1 != Token.EMPTY && var0.type == NodeType.PROGRAM) {
         io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var2 = io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream.scanSource(var0);
         String var3 = new JsAstFormatter(var0, var2).format();
         Node var4 = parseFormattedProgram(var3);
         io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream var5 = new io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream(var4);
         Token var6 = io.karatelabs.ext.jsformat.JsFormatOracle.compareTreeShape(var2, var5, var4);
         if (var6 != null) {
            throw io.karatelabs.ext.jsformat.JsFormatException.atToken(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.ORACLE_MISMATCH, "the formatted tree differs from the source tree at '" + var6.getText() + "'", var6, null);
         } else {
            Token var7 = io.karatelabs.ext.jsformat.JsFormatOracle.findFirstCommentPlacementMismatch(var2, var5);
            if (var7 != null) {
               throw io.karatelabs.ext.jsformat.JsFormatException.atToken(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.COMMENT_UNPLACEABLE, "a comment would not keep its place", var7, null);
            } else {
               return var3;
            }
         }
      } else {
         throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.PARSE_FAILED, "a PROGRAM node with its source text is required", 0, 0, -1, var0.type.name());
      }
   }

   public static io.karatelabs.ext.jsformat.JsFormatter.FormatResult formatWithReport(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList(2);
      String var2 = stripBom(var0);
      if (var2 != var0) {
         var1.add("BOM_REMOVED");
      }

      try {
         Node var3 = parseProgram(var2);
         String var4 = formatTree(var3);
         if (containsCarriageReturn(var3)) {
            var1.add("CRLF_CONVERTED");
         }

         return new io.karatelabs.ext.jsformat.JsFormatter.FormatResult(var4.equals(var0) ? "unchanged" : "changed", var4, null, var1);
      } catch (JsFormatException var5) {
         return new io.karatelabs.ext.jsformat.JsFormatter.FormatResult("refused", null, var5, var1);
      }
   }

   public static String toJsLiteral(Object var0, int var1) {
      StringBuilder var2 = new StringBuilder();
      appendJsLiteral(var2, var0, var1);
      return var2.toString();
   }

   public static String toJsLiteral(Object var0) {
      return toJsLiteral(var0, 0);
   }

   static Node parseProgram(String var0) {
      try {
         return parseLeadingFunctionExpression(var0);
      } catch (StackOverflowError var6) {
         throw nestingLimitException((Token)null);
      } catch (RuntimeException var7) {
         String var2 = var7.getMessage() == null ? "parse failed" : var7.getMessage();
         if (!(var7 instanceof ParserException)) {
            throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.PARSE_FAILED, "the parser failed: " + var7.getClass().getSimpleName(), 0, 0, -1, null);
         } else if (var2.contains("too much recursion")) {
            throw nestingLimitException((Token)null);
         } else {
            Matcher var3 = PARSER_LOCATION_PATTERN.matcher(var2);
            int var4 = 0;
            int var5 = 0;
            if (var3.find()) {
               var4 = Integer.parseInt(var3.group(1));
               var5 = Integer.parseInt(var3.group(2));
            }

            throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.PARSE_FAILED, firstLine(var2), var4, var5, -1, null);
         }
      }
   }

   private static Node parseFormattedProgram(String var0) {
      try {
         return parseLeadingFunctionExpression(var0);
      } catch (RuntimeException var2) {
         throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.ORACLE_MISMATCH, "the formatted text does not parse: " + firstLine(var2.getMessage()), 0, 0, -1, null);
      }
   }

   private static Node parseLeadingFunctionExpression(String var0) {
      return new JsParser(Resource.text(var0)).leadingFunctionExpression().parse();
   }

   private static JsFormatException nestingLimitException(Token var0) {
      return io.karatelabs.ext.jsformat.JsFormatException.atToken(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.NESTING_LIMIT, "nesting deeper than 200 levels", var0, null);
   }

   private static void checkNestingDepth(Node var0) {
      Node[] var1 = new Node[64];
      int[] var2 = new int[64];
      int var3 = 0;
      var1[0] = var0;
      var2[0] = 1;

      while (var3 >= 0) {
         Node var4 = var1[var3];
         int var5 = var2[var3--];
         if (var5 > 200) {
            while (!var4.isToken() && !var4.isEmpty()) {
               var4 = var4.get(0);
            }

            throw nestingLimitException(var4.isToken() ? var4.token : null);
         }

         for (int var6 = var4.size() - 1; var6 >= 0; var6--) {
            Node var7 = var4.get(var6);
            if (!var7.isToken()) {
               if (++var3 == var1.length) {
                  var1 = Arrays.copyOf(var1, var3 * 2);
                  var2 = Arrays.copyOf(var2, var3 * 2);
               }

               var1[var3] = var7;
               var2[var3] = var5 + 1;
            }
         }
      }
   }

   private static String stripBom(String var0) {
      return var0.startsWith("\ufeff") ? var0.substring(1) : var0;
   }

   private static String firstLine(String var0) {
      if (var0 == null) {
         return "";
      } else {
         int var1 = var0.indexOf(10);
         return var1 < 0 ? var0 : var0.substring(0, var1);
      }
   }

   private static boolean containsCarriageReturn(Node var0) {
      Token var1 = var0.getFirstToken();

      while (var1.getPrev() != null) {
         var1 = var1.getPrev();
      }

      while (var1 != null) {
         if (var1.type == TokenType.WS_LF && var1.getText().indexOf(13) >= 0) {
            return true;
         }

         var1 = var1.getNext();
      }

      return false;
   }

   static String quoteStringLiteral(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 2);
      var1.append('\'');

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         switch (var3) {
            case '\b':
               var1.append("\\b");
               break;
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\u000b':
               var1.append("\\v");
               break;
            case '\f':
               var1.append("\\f");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '\'':
               var1.append("\\'");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            case '\u2028':
            case '\u2029':
               var1.append("\\u").append(Integer.toHexString(var3));
               break;
            default:
               if (var3 >= ' ' && var3 != 127) {
                  if (Character.isHighSurrogate(var3) && var2 + 1 < var0.length() && Character.isLowSurrogate(var0.charAt(var2 + 1))) {
                     var1.append(var3).append(var0.charAt(++var2));
                  } else if (Character.isSurrogate(var3)) {
                     var1.append(String.format("\\u%04X", Integer.valueOf(var3)));
                  } else {
                     var1.append(var3);
                  }
               } else {
                  var1.append(String.format("\\x%02X", Integer.valueOf(var3)));
               }
         }
      }

      return var1.append('\'').toString();
   }

   static String canonicalizeStringLiteral(String var0, boolean var1) {
      String var2 = var0.substring(1, var0.length() - 1);
      String var3 = unescapeStringLiteral(var0);
      if (var3 != null && var3.equals(JsLexer.unescapeStringLiteral(var2))) {
         String var4 = quoteStringLiteral(var3);
         return var1 && (var4.length() != var0.length() || !var4.regionMatches(1, var0, 1, var2.length())) ? null : var4;
      } else {
         return var1 ? null : var0;
      }
   }

   static String unescapeStringLiteral(String var0) {
      int var1 = var0.length() - 1;
      if (var0.indexOf(92) < 0) {
         return var0.substring(1, var1);
      } else {
         StringBuilder var2 = new StringBuilder(var1);

         for (int var3 = 1; var3 < var1; var3++) {
            char var4 = var0.charAt(var3);
            if (var4 != '\\') {
               var2.append(var4);
            } else {
               if (++var3 >= var1) {
                  return null;
               }

               char var5 = var0.charAt(var3);
               switch (var5) {
                  case '\n':
                  case '\u2028':
                  case '\u2029':
                     break;
                  case '\r':
                     if (var3 + 1 < var1 && var0.charAt(var3 + 1) == '\n') {
                        var3++;
                     }
                     break;
                  case 'b':
                     var2.append('\b');
                     break;
                  case 'f':
                     var2.append('\f');
                     break;
                  case 'n':
                     var2.append('\n');
                     break;
                  case 'r':
                     var2.append('\r');
                     break;
                  case 't':
                     var2.append('\t');
                     break;
                  case 'u':
                     if (var3 + 1 < var1 && var0.charAt(var3 + 1) == '{') {
                        int var11 = var0.indexOf(125, var3 + 2);
                        if (var11 < 0 || var11 >= var1 || var11 == var3 + 2) {
                           return null;
                        }

                        int var12 = parseHexDigits(var0, var3 + 2, var11, var1);
                        if (var12 < 0 || var12 > 1114111) {
                           return null;
                        }

                        var2.appendCodePoint(var12);
                        var3 = var11;
                        break;
                     }

                     int var10 = parseHexDigits(var0, var3 + 1, var3 + 5, var1);
                     if (var10 < 0) {
                        return null;
                     }

                     var2.append((char)var10);
                     var3 += 4;
                     break;
                  case 'v':
                     var2.append('\u000b');
                     break;
                  case 'x':
                     int var9 = parseHexDigits(var0, var3 + 1, var3 + 3, var1);
                     if (var9 < 0) {
                        return null;
                     }

                     var2.append((char)var9);
                     var3 += 2;
                     break;
                  default:
                     if (var5 >= '0' && var5 <= '7') {
                        int var6 = var5 <= '3' ? 3 : 2;
                        int var7 = 0;

                        for (int var8 = 0; var8 < var6 && var3 < var1 && var0.charAt(var3) >= '0' && var0.charAt(var3) <= '7'; var8++) {
                           var7 = var7 * 8 + (var0.charAt(var3) - '0');
                           var3++;
                        }

                        var3--;
                        var2.append((char)var7);
                     } else {
                        var2.append(var5);
                     }
               }
            }
         }

         return var2.toString();
      }
   }

   private static int parseHexDigits(String var0, int var1, int var2, int var3) {
      if (var2 <= var3 && var1 < var2 && var2 - var1 <= 6) {
         int var4 = 0;

         for (int var5 = var1; var5 < var2; var5++) {
            int var6 = Character.digit(var0.charAt(var5), 16);
            if (var6 < 0) {
               return -1;
            }

            var4 = var4 * 16 + var6;
         }

         return var4;
      } else {
         return -1;
      }
   }

   static BigDecimal parseDecimalLiteral(String var0) {
      if (!var0.isEmpty() && DECIMAL_LITERAL_PATTERN.matcher(var0).matches() && var0.charAt(0) != 'e' && var0.charAt(0) != 'E' && !var0.equals(".")) {
         try {
            return new BigDecimal(var0);
         } catch (NumberFormatException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String canonicalizeNumberLiteral(String var0) {
      BigDecimal var1 = parseDecimalLiteral(var0);
      if (var1 == null) {
         return var0;
      } else {
         double var2 = Double.parseDouble(var0);
         if (Double.isInfinite(var2)) {
            return var0;
         } else {
            String var4 = formatNumberLiteral((double)var2);
            return new BigDecimal(var4).compareTo(var1) == 0 ? var4 : var0;
         }
      }
   }

   static String formatNumberLiteral(Number var0) {
      if (!(var0 instanceof Integer) && !(var0 instanceof Long) && !(var0 instanceof Short) && !(var0 instanceof Byte)) {
         double var1 = var0.doubleValue();
         if (Double.isNaN(var1)) {
            throw new IllegalArgumentException("NaN cannot be a JS literal");
         } else if (Double.isInfinite(var1)) {
            throw new IllegalArgumentException("Infinity cannot be a JS literal");
         } else if (var1 == Math.floor(var1) && Math.abs(var1) < 1.0E16) {
            return Long.toString((long)var1);
         } else {
            double var3 = Math.abs(var1);
            return var3 >= 1.0E-6 && var3 < 1.0E21 ? new BigDecimal(Double.toString(var1)).stripTrailingZeros().toPlainString() : Double.toString(var1);
         }
      } else {
         return var0.toString();
      }
   }

   private static void appendJsLiteral(StringBuilder var0, Object var1, int var2) {
      if (var1 == null) {
         var0.append("null");
      } else {
         if (var1 instanceof JsFunction || var1 instanceof JavaCallable) {
            throw new IllegalArgumentException("functions are not JS-literal data");
         }

         if (var1 instanceof Boolean var4) {
            var0.append(var4);
         } else if (var1 instanceof Number var5) {
            var0.append(formatNumberLiteral(var5));
         } else if (var1 instanceof CharSequence var6) {
            var0.append(quoteStringLiteral(var6.toString()));
         } else if (var1 instanceof Map var7) {
            if (var7.isEmpty()) {
               var0.append("{}");
               return;
            }

            var0.append('{');

            for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
               appendIndentedLine(var0, var2 + 2);
               var0.append(quoteStringLiteral(var9.getKey().toString())).append(": ");
               appendJsLiteral(var0, var9.getValue(), var2 + 2);
               var0.append(',');
            }

            appendIndentedLine(var0, var2);
            var0.append('}');
         } else {
            if (!(var1 instanceof List var3)) {
               throw new IllegalArgumentException("not a JS-literal value: " + var1.getClass().getName());
            }

            if (var3.isEmpty()) {
               var0.append("[]");
               return;
            }

            var0.append('[');

            for (Object var11 : var3) {
               appendIndentedLine(var0, var2 + 2);
               appendJsLiteral(var0, var11, var2 + 2);
               var0.append(',');
            }

            appendIndentedLine(var0, var2);
            var0.append(']');
         }
      }
   }

   private static void appendIndentedLine(StringBuilder var0, int var1) {
      var0.append('\n');
      var0.append(" ".repeat(var1));
   }

   public record FormatResult(String status, String text, JsFormatException error, List<String> notes) {


      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("status", this.status);
         if (this.text != null) {
            var1.put("text", this.text);
         }

         if (this.error != null) {
            var1.put("reason", this.error.toMap());
         }

         if (!this.notes.isEmpty()) {
            var1.put("notes", this.notes);
         }

         return var1;
      }

      public String status() {
         return this.status;
      }

      public String text() {
         return this.text;
      }

      public JsFormatException error() {
         return this.error;
      }

      public List<String> notes() {
         return this.notes;
      }
   }
}
