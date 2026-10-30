package io.karatelabs.parser;

public enum TokenType {
   WS_LF,
   WS,
   EOF,
   BACKTICK,
   L_CURLY,
   R_CURLY,
   L_BRACKET,
   R_BRACKET,
   L_PAREN,
   R_PAREN,
   COMMA,
   COLON,
   SEMI,
   DOT_DOT_DOT,
   QUES_DOT,
   DOT,
   NULL(true),
   TRUE(true),
   FALSE(true),
   FUNCTION(true),
   RETURN(true),
   TRY(true),
   CATCH(true),
   FINALLY(true),
   THROW(true),
   NEW(true),
   VAR(true),
   LET(true),
   CONST(true),
   IF(true),
   ELSE(true),
   TYPEOF(true),
   INSTANCEOF(true),
   DELETE(true),
   FOR(true),
   IN(true),
   OF(true),
   DO(true),
   WHILE(true),
   SWITCH(true),
   CASE(true),
   DEFAULT(true),
   BREAK(true),
   CONTINUE(true),
   THIS(true),
   VOID(true),
   CLASS(true),
   EXTENDS(true),
   SUPER(true),
   EQ_EQ_EQ,
   EQ_EQ,
   EQ,
   EQ_GT,
   LT_LT_EQ,
   LT_LT,
   LT_EQ,
   LT,
   GT_GT_GT_EQ,
   GT_GT_GT,
   GT_GT_EQ,
   GT_GT,
   GT_EQ,
   GT,
   NOT_EQ_EQ,
   NOT_EQ,
   NOT,
   PIPE_PIPE_EQ,
   PIPE_PIPE,
   PIPE_EQ,
   PIPE,
   AMP_AMP_EQ,
   AMP_AMP,
   AMP_EQ,
   AMP,
   CARET_EQ,
   CARET,
   QUES_QUES_EQ,
   QUES_QUES,
   QUES,
   PLUS_PLUS,
   PLUS_EQ,
   PLUS,
   MINUS_MINUS,
   MINUS_EQ,
   MINUS,
   STAR_STAR_EQ,
   STAR_STAR,
   STAR_EQ,
   STAR,
   SLASH_EQ,
   SLASH,
   PERCENT_EQ,
   PERCENT,
   TILDE,
   L_COMMENT,
   B_COMMENT,
   S_STRING,
   D_STRING,
   NUMBER,
   BIGINT,
   IDENT,
   PRIVATE_NAME,
   REGEX,
   DOLLAR_L_CURLY,
   T_STRING,
   G_PREFIX,
   G_COMMENT,
   G_DESC,
   G_FEATURE,
   G_SCENARIO,
   G_SCENARIO_OUTLINE,
   G_EXAMPLES,
   G_BACKGROUND,
   G_TAG,
   G_TRIPLE_QUOTE,
   G_PIPE,
   G_TABLE_CELL,
   G_KEYWORD,
   G_EXPR;

   public final boolean primary = !isCommentOrWhitespace(this);
   public final boolean keyword;
   public final Boolean regexAllowed;

   private TokenType() {
      this(false);
   }

   private TokenType(boolean keyword) {
      this.keyword = keyword;
      this.regexAllowed = isRegexAllowed(this);
   }

   private static boolean isCommentOrWhitespace(TokenType type) {
      return switch (type) {
         case WS_LF, WS, L_COMMENT, B_COMMENT, G_COMMENT -> true;
         default -> false;
      };
   }

   private static Boolean isRegexAllowed(TokenType type) {
      return switch (type) {
         case L_CURLY, L_BRACKET, L_PAREN, COMMA, COLON, SEMI, RETURN, THROW, IF, ELSE, TYPEOF, INSTANCEOF, DELETE, IN, DO, CASE, DEFAULT, VOID, EQ_EQ_EQ, EQ_EQ, EQ, EQ_GT, LT_EQ, LT, GT_EQ, GT, NOT_EQ_EQ, NOT_EQ, NOT, PIPE_PIPE_EQ, PIPE_PIPE, PIPE_EQ, PIPE, AMP_AMP_EQ, AMP_AMP, AMP_EQ, AMP, CARET_EQ, CARET, QUES_QUES, QUES, PLUS_EQ, PLUS, MINUS_EQ, MINUS, STAR_STAR_EQ, STAR_STAR, STAR_EQ, STAR, SLASH_EQ, PERCENT_EQ, PERCENT, TILDE -> true;
         case R_CURLY, R_BRACKET, R_PAREN, NULL, TRUE, FALSE, S_STRING, D_STRING, NUMBER, BIGINT, IDENT, PRIVATE_NAME -> false;
         default -> null;
      };
   }

   public boolean oneOf(TokenType... types) {
      for (TokenType type : types) {
         if (this == type) {
            return true;
         }
      }

      return false;
   }
}
