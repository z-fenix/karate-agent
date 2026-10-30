package io.karatelabs.parser;

import java.util.List;

public class AsiHint {
   private static final int MAX_LOOKBACK = 256;

   private AsiHint() {
   }

   static String forFailure(List<Token> tokens, int position) {
      int i = Math.min(position, tokens.size() - 1);

      for (int limit = Math.max(0, i - 256); i >= limit; i--) {
         Token token = tokens.get(i);
         if (token.type == TokenType.SEMI) {
            return null;
         }

         String hint = forOpener(token);
         if (hint != null) {
            return hint;
         }
      }

      return null;
   }

   public static String forOpener(Token token) {
      if (token == null) {
         return null;
      } else {
         boolean bracket = token.type == TokenType.L_BRACKET;
         if (!bracket && token.type != TokenType.L_PAREN) {
            return null;
         } else {
            Token prev = prevPrimary(token);
            if (prev == null || !Boolean.FALSE.equals(prev.type.regexAllowed)) {
               return null;
            } else if (!BaseParser.lineTerminatorBetween(prev, token)) {
               return null;
            } else if (prev.type == TokenType.R_PAREN && closesControlHead(prev)) {
               return null;
            } else {
               int prevLine = prev.line + 1;
               return "hint: line "
                  + (token.line + 1)
                  + " starts with '"
                  + (bracket ? 91 : 40)
                  + "' — without a ';' ending line "
                  + prevLine
                  + " it continues that statement (as "
                  + (bracket ? "an index" : "a call")
                  + "); add ';' to the end of line "
                  + prevLine
                  + " if a new statement was intended";
            }
         }
      }
   }

   private static boolean closesControlHead(Token rParen) {
      int depth = 0;
      Token token = rParen;

      for (int i = 0; i < 256 && token != null; i++) {
         if (token.type == TokenType.R_PAREN) {
            depth++;
         } else if (token.type == TokenType.L_PAREN) {
            if (--depth == 0) {
               Token before = prevPrimary(token);
               return before != null && before.type.oneOf(TokenType.IF, TokenType.FOR, TokenType.WHILE, TokenType.SWITCH, TokenType.CATCH);
            }
         }

         token = token.getPrev();
      }

      return false;
   }

   private static Token prevPrimary(Token token) {
      Token temp = token.getPrev();

      while (temp != null && !temp.type.primary) {
         temp = temp.getPrev();
      }

      return temp;
   }
}
