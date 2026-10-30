package io.karatelabs.ext.jsformat;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

final class JsFormatOracle {
   private static final byte TRAILING_COMMA_FLAG = 2;
   private static final byte b = 4;
   private static final boolean[] EXACT_TEXT_TOKEN_TYPES = new boolean[TokenType.values().length];

   private JsFormatOracle() {
   }

   static Token findFirstStructuralMismatch(Node var0, Node var1) {
      return compareTreeShape(io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream.scanSource(var0), new io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream(var1), var1);
   }

   static Token findFirstCommentMismatch(Node var0, Node var1) {
      io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var2 = io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream.scanSource(var0);
      io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream var3 = new io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream(var1);
      compareTreeShape(var2, var3, var1);
      return findFirstCommentPlacementMismatch(var2, var3);
   }

   static Token compareTreeShape(io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var0, io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream var1, Node var2) {
      Token var3 = compareNodeShape(var0, var0.rootNode, var1, var2, 1, false, false);
      if (var3 == null && var0.primaryTokenIndex != var0.primaryTokenCount) {
         return var0.currentPrimaryToken != null ? var0.currentPrimaryToken : Token.EMPTY;
      } else {
         return var3;
      }
   }

   private static Token compareNodeShape(io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var0, Node var1, io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream var2, Node var3, int var4, boolean var5, boolean var6) {
      if (var4 > 200) {
         throw io.karatelabs.ext.jsformat.JsFormatException.atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.NESTING_LIMIT, "nesting deeper than 200 levels", var1);
      } else if (var1.type == NodeType.ERROR) {
         throw io.karatelabs.ext.jsformat.JsFormatException.atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.PARSE_FAILED, "the tree holds a parse error", var1);
      } else if (var1.type == var3.type && var1.async == var3.async && var1.generator == var3.generator) {
         int var7 = var1.size();
         int var8 = var3.size();
         int var9 = trailingCommaIndex(var1, var7);
         int var10 = trailingCommaIndex(var3, var8);
         int var11 = 0;
         int var12 = 0;

         while (true) {
            Node var13 = null;

            while (var11 < var7 && consumeIgnorableNode(var0, var1, var13 = var1.get(var11), var11, var7, var5)) {
               var11++;
            }

            Node var14 = null;

            while (var12 < var8 && consumeIgnorableNode(var2, var3, var14 = var3.get(var12), var12, var8, var6)) {
               var12++;
            }

            if (var11 == var7 || var12 == var8) {
               if (var11 == var7 && var12 == var8) {
                  if (var1.type == NodeType.DO_WHILE_STMT) {
                     var0.currentTokenFlags = (byte)(var0.currentTokenFlags | 4);
                     var2.currentTokenFlags = (byte)(var2.currentTokenFlags | 4);
                  }

                  return null;
               } else {
                  return var11 < var7 ? var13.getFirstToken() : var1.getLastToken();
               }
            }

            var11++;
            var12++;
            if (var13.isToken()) {
               if (!var14.isToken()) {
                  return var13.token;
               }

               Token var18 = var13.token;
               Token var16 = var14.token;
               int var17 = var18.type == TokenType.COMMA && isIgnorableChild(var1, var11 - 1, var7, var5) ? 2 : 0;
               var0.recordPrimaryToken(var18, (byte)var17);
               var2.recordPrimaryToken(var16, (byte)var17);
               if (!tokensEquivalent(var0, var18, var2, var16)) {
                  return var18;
               }
            } else {
               if (var14.isToken()) {
                  return var13.getFirstToken();
               }

               Token var15 = compareNodeShape(var0, var13, var2, var14, var4 + 1, var11 - 1 == var9, var12 - 1 == var10);
               if (var15 != null) {
                  return var15;
               }
            }
         }
      } else {
         return var1.getFirstToken();
      }
   }

   private static boolean consumeIgnorableNode(io.karatelabs.ext.jsformat.JsFormatOracle.CommentAlignmentStream var0, Node var1, Node var2, int var3, int var4, boolean var5) {
      if (var2.isToken()) {
         Token var7 = var2.token;
         if (var7.type == TokenType.COMMA && isIgnorableChild(var1, var3, var4, var5) && (var1.type != NodeType.ARRAY_ELEM || var4 != 1)) {
            var0.recordPrimaryToken(var7, (byte)2);
            return true;
         } else {
            return false;
         }
      } else if (var2.type != NodeType.EOS) {
         if (var2.type == NodeType.STATEMENT
            && var2.size() == 1
            && var2.get(0).isToken()
            && var2.get(0).token.type == TokenType.SEMI
            && var0.primaryTokenIndex > 0
            && (var0.currentTokenFlags & 4) != 0) {
            var0.recordPrimaryToken(var2.get(0).token, (byte)2);
            return true;
         } else {
            return false;
         }
      } else {
         for (int var6 = 0; var6 < var2.size(); var6++) {
            var0.recordPrimaryToken(var2.get(var6).token, (byte)2);
         }

         return true;
      }
   }

   private static boolean isIgnorableChild(Node var0, int var1, int var2, boolean var3) {
      if (var1 != var2 - 1) {
         return false;
      } else {
         return switch (var0.type) {
            case ARRAY_ELEM, OBJECT_ELEM, FN_CALL_ARG, FN_DECL_ARG -> var3;
            default -> isBeforeClosingDelimiter(var0.get(var1).token);
         };
      }
   }

   private static int trailingCommaIndex(Node var0, int var1) {
      return switch (var0.type) {
         case LIT_ARRAY, LIT_OBJECT -> var1 - 2;
         case FN_DECL_ARGS -> var1 > 0 && var0.get(0).isToken() ? var1 - 2 : -1;
         case FN_CALL_ARGS -> var1 - 1;
         default -> -1;
      };
   }

   private static boolean isBeforeClosingDelimiter(Token var0) {
      TokenType var1 = var0.getNextPrimary().type;
      return var1 == TokenType.R_PAREN || var1 == TokenType.R_BRACKET || var1 == TokenType.R_CURLY;
   }

   private static boolean tokensEquivalent(io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var0, Token var1, io.karatelabs.ext.jsformat.JsFormatOracle.FormattedCommentStream var2, Token var3) {
      TokenType var4 = var1.type;
      if (var4 != var3.type) {
         var4 = normalizeStringTokenType(var1.type);
         if (var4 != normalizeStringTokenType(var3.type)) {
            return false;
         }
      }

      if (!EXACT_TEXT_TOKEN_TYPES[var4.ordinal()]) {
         return true;
      } else {
         short var5 = var1.length;
         if (var5 == var3.length && var0.sourceText.regionMatches(var1.pos, var2.sourceText, var3.pos, var5)) {
            return true;
         } else if (var4 == TokenType.S_STRING) {
            String var8 = io.karatelabs.ext.jsformat.JsFormatter.unescapeStringLiteral(var1.getText());
            return var8 != null && var8.equals(io.karatelabs.ext.jsformat.JsFormatter.unescapeStringLiteral(var3.getText()));
         } else if (var4 != TokenType.NUMBER) {
            return false;
         } else {
            BigDecimal var6 = io.karatelabs.ext.jsformat.JsFormatter.parseDecimalLiteral(var1.getText());
            BigDecimal var7 = io.karatelabs.ext.jsformat.JsFormatter.parseDecimalLiteral(var3.getText());
            return var6 != null && var7 != null && var6.compareTo(var7) == 0;
         }
      }
   }

   private static TokenType normalizeStringTokenType(TokenType var0) {
      return var0 == TokenType.D_STRING ? TokenType.S_STRING : var0;
   }

   static Token findFirstCommentPlacementMismatch(io.karatelabs.ext.jsformat.JsFormatOracle.CommentAlignmentStream var0, io.karatelabs.ext.jsformat.JsFormatOracle.CommentAlignmentStream var1) {
      if (var0.commentCount != var1.commentCount) {
         return var0.commentCount > var1.commentCount ? var0.commentTokens[var1.commentCount] : (var0.currentPrimaryToken != null ? var0.currentPrimaryToken : Token.EMPTY);
      } else {
         for (int var2 = 0; var2 < var0.commentCount; var2++) {
            Token var3 = var0.commentTokens[var2];
            Token var4 = var1.commentTokens[var2];
            if (var3.length != var4.length || var0.commentPlacements[var2] != var1.commentPlacements[var2] || !var0.sourceText.regionMatches(var3.pos, var1.sourceText, var4.pos, var3.length)) {
               return var3;
            }
         }

         return null;
      }
   }

   static boolean isCommentToken(TokenType var0) {
      return var0 == TokenType.L_COMMENT || var0 == TokenType.B_COMMENT;
   }

   static boolean areTokensAdjacent(String var0, Token var1, Token var2) {
      return var1 != null && !containsLineBreak(var0, var1.pos + var1.length, var2.pos);
   }

   static boolean areAdjacentBeforeNextToken(String var0, Token var1, Token var2) {
      return var2 != null && var2.type != TokenType.EOF && !containsLineBreak(var0, var1.pos + var1.length, var2.pos);
   }

   static boolean containsLineBreak(String var0, int var1, int var2) {
      for (int var3 = Math.max(0, var1); var3 < var2 && var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\n' || var4 == '\r' || var4 == 8232 || var4 == 8233) {
            return true;
         }
      }

      return false;
   }

   static {
      for (TokenType var3 : new TokenType[]{
         TokenType.IDENT,
         TokenType.PRIVATE_NAME,
         TokenType.S_STRING,
         TokenType.D_STRING,
         TokenType.NUMBER,
         TokenType.BIGINT,
         TokenType.REGEX,
         TokenType.T_STRING,
         TokenType.L_COMMENT,
         TokenType.B_COMMENT,
         TokenType.WS,
         TokenType.WS_LF
      }) {
         EXACT_TEXT_TOKEN_TYPES[var3.ordinal()] = true;
      }

      for (TokenType var7 : TokenType.values()) {
         if (var7.name().startsWith("G_")) {
            EXACT_TEXT_TOKEN_TYPES[var7.ordinal()] = true;
         }
      }
   }

   abstract static class CommentAlignmentStream {
      final String sourceText;
      Token[] commentTokens = new Token[16];
      long[] commentPlacements = new long[16];
      int commentCount;
      int placementWriteIndex;
      int primaryTokenIndex;
      int anchorPrimaryIndex;
      Token previousPrimaryToken;
      Token currentPrimaryToken;
      byte currentTokenFlags;

      CommentAlignmentStream(String var1) {
         this.sourceText = var1;
      }

      void completeCommentPlacementMetadata(int var1, Token var2) {
         while (this.placementWriteIndex < var1) {
            Token var3 = this.commentTokens[this.placementWriteIndex];
            this.commentPlacements[this.placementWriteIndex++] = (long)this.anchorPrimaryIndex << 2 | (io.karatelabs.ext.jsformat.JsFormatOracle.areTokensAdjacent(this.sourceText, this.previousPrimaryToken, var3) ? 2 : 0) | (io.karatelabs.ext.jsformat.JsFormatOracle.areAdjacentBeforeNextToken(this.sourceText, var3, var2) ? 1 : 0);
         }
      }

      abstract void recordPrimaryToken(Token var1, byte var2);

      void advancePrimaryCursor(Token var1, byte var2) {
         this.primaryTokenIndex++;
         this.currentPrimaryToken = var1;
         this.currentTokenFlags = var2;
         if ((var2 & 2) == 0) {
            this.anchorPrimaryIndex++;
            this.previousPrimaryToken = var1;
         }
      }
   }

   static final class FormattedCommentStream extends io.karatelabs.ext.jsformat.JsFormatOracle.CommentAlignmentStream {
      FormattedCommentStream(Node var1) {
         super(var1.getFirstToken().getResource().getText());
      }

      @Override
      void recordPrimaryToken(Token var1, byte var2) {
         var var3 = var1.getComments();
         int var4 = 0;

         for (int var5 = var3.size(); var4 < var5; var4++) {
            Token var6 = (Token)var3.get(var4);
            if (io.karatelabs.ext.jsformat.JsFormatOracle.isCommentToken(var6.type)) {
               if (this.commentCount == this.commentTokens.length) {
                  this.commentTokens = Arrays.copyOf(this.commentTokens, this.commentCount * 2);
                  this.commentPlacements = Arrays.copyOf(this.commentPlacements, this.commentCount * 2);
               }

               this.commentTokens[this.commentCount++] = var6;
            }
         }

         if ((var2 & 2) == 0) {
            this.completeCommentPlacementMetadata(this.commentCount, var1);
         }

         this.advancePrimaryCursor(var1, var2);
      }
   }

   static final class SourceCommentStream extends io.karatelabs.ext.jsformat.JsFormatOracle.CommentAlignmentStream {
      int[] commentPrimaryIndices = new int[16];
      Token lastPrimaryToken;
      int primaryTokenCount;
      final Node rootNode;

      private SourceCommentStream(String var1, Node var2) {
         super(var1);
         this.rootNode = var2;
      }

      static io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream scanSource(Node var0) {
         Token var1 = var0.getFirstToken();
         io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var2 = new io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream(var1.getResource().getText(), var0);

         while (var1.getPrev() != null) {
            var1 = var1.getPrev();
         }

         while (var1 != null) {
            var1 = var2.scanToken(var1);
         }

         return var2;
      }

      private Token scanToken(Token var1) {
         if (var1.type.primary) {
            this.primaryTokenCount++;
            if (var1.type != TokenType.EOF) {
               this.lastPrimaryToken = var1;
            }
         } else if (io.karatelabs.ext.jsformat.JsFormatOracle.isCommentToken(var1.type)) {
            if (this.commentCount == this.commentTokens.length) {
               this.commentTokens = Arrays.copyOf(this.commentTokens, this.commentCount * 2);
               this.commentPlacements = Arrays.copyOf(this.commentPlacements, this.commentCount * 2);
               this.commentPrimaryIndices = Arrays.copyOf(this.commentPrimaryIndices, this.commentCount * 2);
            }

            this.commentTokens[this.commentCount] = var1;
            this.commentPrimaryIndices[this.commentCount++] = this.primaryTokenCount;
         }

         return var1.getNext();
      }

      @Override
      void recordPrimaryToken(Token var1, byte var2) {
         if ((var2 & 2) == 0) {
            int var3 = this.placementWriteIndex;

            while (var3 < this.commentCount && this.commentPrimaryIndices[var3] <= this.primaryTokenIndex) {
               var3++;
            }

            this.completeCommentPlacementMetadata(var3, var1);
         }

         if (var1.type != TokenType.EOF) {
            this.lastPrimaryToken = var1;
         }

         this.advancePrimaryCursor(var1, var2);
      }
   }
}
