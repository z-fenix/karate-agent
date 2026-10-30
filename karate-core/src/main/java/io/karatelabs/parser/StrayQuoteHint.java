package io.karatelabs.parser;

import java.util.List;

public class StrayQuoteHint {
   private static final int MAX_LOOKAHEAD = 64;
   private static final int MAX_TEXT = 40;

   private StrayQuoteHint() {
   }

   static String forFailure(List<Token> tokens, int position) {
      int start = Math.max(0, Math.min(position, tokens.size() - 1) - 1);
      int end = Math.min(tokens.size(), start + 64);

      for (int i = start; i < end; i++) {
         Token token = tokens.get(i);
         if (token.type == TokenType.SEMI && i > start) {
            return null;
         }

         if (isString(token)) {
            Token next = token.getNextPrimary();
            if (next != null && (next.type == TokenType.IDENT || isString(next)) && !BaseParser.lineTerminatorBetween(token, next)) {
               String text = token.getText();
               String prefix = "hint: line " + (token.line + 1) + " col " + (token.col + 1) + ": ";
               if (text.length() == 2) {
                  return prefix + text + " is an empty string followed directly by " + clip(next.getText()) + " — a doubled or stray quote?";
               }

               return prefix + clip(text) + " is a string followed directly by " + clip(next.getText()) + " — a stray quote, or a missing ',' or '+'?";
            }
         }
      }

      return null;
   }

   private static boolean isString(Token token) {
      return token.type == TokenType.S_STRING || token.type == TokenType.D_STRING;
   }

   private static String clip(String text) {
      return text.length() <= 40 ? text : text.substring(0, 40) + "...";
   }
}
