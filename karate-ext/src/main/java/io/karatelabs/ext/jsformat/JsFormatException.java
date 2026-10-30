package io.karatelabs.ext.jsformat;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.Token;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsFormatException extends RuntimeException {
   public final io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode code;
   public final int lineNumber;
   public final int columnNumber;
   public final int sourceOffset;
   public final String nodeType;

   public JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode var1, String var2, int var3, int var4, int var5, String var6) {
      super(var2);
      this.code = var1;
      this.lineNumber = var3;
      this.columnNumber = var4;
      this.sourceOffset = var5;
      this.nodeType = var6;
   }

   static JsFormatException atToken(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode var0, String var1, Token var2, String var3) {
      return var2 != null && var2 != Token.EMPTY
         ? new JsFormatException(var0, var1, var2.line + 1, var2.col + 1, var2.pos, var3 != null ? var3 : var2.type.name())
         : new JsFormatException(var0, var1, 0, 0, -1, var3);
   }

   static JsFormatException atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode var0, String var1, Node var2) {
      return atToken(var0, var1, var2.getFirstToken(), var2.isToken() ? var2.token.type.name() : var2.type.name());
   }

   public Map<String, Object> toMap() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("code", this.code.name());
      var1.put("message", this.getMessage());
      var1.put("line", this.lineNumber);
      var1.put("column", this.columnNumber);
      if (this.nodeType != null) {
         var1.put("nodeType", this.nodeType);
      }

      return var1;
   }

   public static enum FormatErrorCode {
      UNSUPPORTED_NODE,
      COMMENT_UNPLACEABLE,
      ORACLE_MISMATCH,
      PARSE_FAILED,
      NOT_CANONICAL,
      NESTING_LIMIT;
   }
}
