package io.karatelabs.gherkin;

import io.karatelabs.common.Resource;
import io.karatelabs.parser.BaseLexer;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;

public class GherkinLexer extends BaseLexer {
   private GherkinLexer.GherkinState gState = GherkinLexer.GherkinState.GHERKIN;
   private boolean descLineStart;
   private boolean featureDesc;
   private int matchParenDepth = 0;

   public GherkinLexer(Resource resource) {
      super(resource);
   }

   @Override
   public Token nextToken() {
      this.tokenStart = this.pos;
      this.tokenLine = this.line;
      this.tokenCol = this.col;
      TokenType type = this.scanToken();
      int length = this.pos - this.tokenStart;
      return new Token(this.buffer, type, this.tokenStart, this.tokenLine, this.tokenCol, length);
   }

   @Override
   protected TokenType scanToken() {
      return this.isAtEnd() ? TokenType.EOF : this.scanGherkinToken();
   }

   private TokenType scanGherkinToken() {
      char c = this.peek();
      if (c != ' ' && c != '\t') {
         return switch (this.gState) {
            case GHERKIN -> this.scanGherkinInitial();
            case GS_DESC -> this.scanGherkinDesc();
            case GS_COMMENT -> this.scanGherkinComment();
            case GS_TAGS -> this.scanGherkinTags();
            case GS_TABLE_ROW -> this.scanGherkinTableRow();
            case GS_DOC_STRING -> this.scanGherkinDocString();
            case GS_STEP -> this.scanGherkinStep();
            case GS_STEP_MATCH -> this.scanGherkinStepMatch();
            case GS_RHS -> this.scanGherkinRhs();
            default -> throw new IllegalStateException("Unexpected state: " + this.gState);
         };
      } else {
         return this.scanGherkinWhitespace();
      }
   }

   private TokenType scanGherkinWhitespace() {
      while (!this.isAtEnd()) {
         char c = this.peek();
         if (c != ' ' && c != '\t') {
            if (c == '\r' || c == '\n') {
               return this.scanGherkinWhitespaceWithNewline();
            }
            break;
         }

         this.advance();
      }

      return TokenType.WS;
   }

   private TokenType scanGherkinWhitespaceWithNewline() {
      while (!this.isAtEnd()) {
         char c = this.peek();
         if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
            this.advance();
            continue;
         }
         break;
      }

      if (this.gState == GherkinLexer.GherkinState.GS_COMMENT
         || this.gState == GherkinLexer.GherkinState.GS_DESC
         || this.gState == GherkinLexer.GherkinState.GS_TAGS
         || this.gState == GherkinLexer.GherkinState.GS_TABLE_ROW
         || this.gState == GherkinLexer.GherkinState.GS_STEP
         || this.gState == GherkinLexer.GherkinState.GS_STEP_MATCH
         || this.gState == GherkinLexer.GherkinState.GS_RHS) {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
      }

      return TokenType.WS_LF;
   }

   private TokenType scanGherkinInitial() {
      char c = this.peek();
      if (c == '\r' || c == '\n') {
         return this.scanGherkinWhitespaceWithNewline();
      } else if (c == '#') {
         this.gState = GherkinLexer.GherkinState.GS_COMMENT;
         this.advance();
         return this.scanGherkinComment();
      } else if (c == '@') {
         return this.scanGherkinTag();
      } else if (c == '|') {
         this.gState = GherkinLexer.GherkinState.GS_TABLE_ROW;
         return this.scanGherkinTableRow();
      } else if (c == '"' && this.peek(1) == '"' && this.peek(2) == '"') {
         this.advance();
         this.advance();
         this.advance();
         this.gState = GherkinLexer.GherkinState.GS_DOC_STRING;
         return TokenType.G_TRIPLE_QUOTE;
      } else if (!isIdentifierStart(c) && c != '*') {
         this.advance();
         return TokenType.WS;
      } else {
         return this.scanGherkinKeyword();
      }
   }

   private TokenType scanGherkinKeyword() {
      int start = this.pos;
      if (this.peek() == '*') {
         this.advance();
         this.gState = GherkinLexer.GherkinState.GS_STEP;
         return TokenType.G_PREFIX;
      } else {
         while (!this.isAtEnd() && isIdentifierPart(this.peek())) {
            this.advance();
         }

         String text = this.source.substring(start, this.pos);
         if (text.equals("Given") || text.equals("When") || text.equals("Then") || text.equals("And") || text.equals("But")) {
            this.gState = GherkinLexer.GherkinState.GS_STEP;
            return TokenType.G_PREFIX;
         } else if (text.equals("Feature") && this.match(':')) {
            this.gState = GherkinLexer.GherkinState.GS_DESC;
            this.featureDesc = true;
            return TokenType.G_FEATURE;
         } else {
            if (text.equals("Scenario")) {
               if (this.peek() == ' ' && this.source.startsWith("Outline:", this.pos + 1)) {
                  this.advance();

                  for (int i = 0; i < 8; i++) {
                     this.advance();
                  }

                  this.gState = GherkinLexer.GherkinState.GS_DESC;
                  this.featureDesc = false;
                  return TokenType.G_SCENARIO_OUTLINE;
               }

               if (this.match(':')) {
                  this.gState = GherkinLexer.GherkinState.GS_DESC;
                  this.featureDesc = false;
                  return TokenType.G_SCENARIO;
               }
            }

            if (text.equals("Background") && this.match(':')) {
               this.gState = GherkinLexer.GherkinState.GS_DESC;
               this.featureDesc = false;
               return TokenType.G_BACKGROUND;
            } else if (text.equals("Examples") && this.match(':')) {
               this.gState = GherkinLexer.GherkinState.GS_DESC;
               this.featureDesc = false;
               return TokenType.G_EXAMPLES;
            } else {
               return TokenType.WS;
            }
         }
      }
   }

   private TokenType scanGherkinTag() {
      this.advance();

      while (!this.isAtEnd() && !this.isWhitespace(this.peek())) {
         this.advance();
      }

      this.gState = GherkinLexer.GherkinState.GS_TAGS;
      return TokenType.G_TAG;
   }

   private TokenType scanGherkinDesc() {
      char c = this.peek();
      if (c != '\r' && c != '\n') {
         boolean comment = this.descLineStart && c == '#';
         this.descLineStart = false;

         while (!this.isAtEnd() && this.peek() != '\r' && this.peek() != '\n') {
            this.advance();
         }

         return comment ? TokenType.G_COMMENT : TokenType.G_DESC;
      } else {
         boolean transition = this.shouldTransitionToGherkin();

         while (!this.isAtEnd() && (this.peek() == ' ' || this.peek() == '\t' || this.peek() == '\r' || this.peek() == '\n')) {
            this.advance();
         }

         if (transition) {
            this.gState = GherkinLexer.GherkinState.GHERKIN;
         }

         this.descLineStart = true;
         return TokenType.WS_LF;
      }
   }

   private boolean shouldTransitionToGherkin() {
      int lookahead;
      for (lookahead = this.pos; lookahead < this.length; lookahead++) {
         char c = this.source.charAt(lookahead);
         if (c != '\r' && c != '\n' && c != ' ' && c != '\t') {
            break;
         }
      }

      if (lookahead >= this.length) {
         return true;
      } else {
         String rest = this.source.substring(lookahead);
         if (rest.startsWith("Scenario:")
            || rest.startsWith("Scenario Outline:")
            || rest.startsWith("Background:")
            || rest.startsWith("Examples:")
            || rest.startsWith("@")) {
            return true;
         } else {
            return this.featureDesc
               ? false
               : this.isStepKeyword(rest, "Given")
                  || this.isStepKeyword(rest, "When")
                  || this.isStepKeyword(rest, "Then")
                  || this.isStepKeyword(rest, "And")
                  || this.isStepKeyword(rest, "But")
                  || rest.startsWith("*")
                  || rest.startsWith("|")
                  || rest.startsWith("\"\"\"");
         }
      }
   }

   private boolean isStepKeyword(String rest, String keyword) {
      return rest.startsWith(keyword) && (rest.length() == keyword.length() || !isIdentifierPart(rest.charAt(keyword.length())));
   }

   private TokenType scanGherkinComment() {
      char c = this.peek();
      if (c != '\r' && c != '\n') {
         while (!this.isAtEnd() && this.peek() != '\r' && this.peek() != '\n') {
            this.advance();
         }

         return TokenType.G_COMMENT;
      } else {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return this.scanGherkinWhitespaceWithNewline();
      }
   }

   private TokenType scanGherkinTags() {
      char c = this.peek();
      if (c == '\r' || c == '\n') {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return this.scanGherkinWhitespaceWithNewline();
      } else if (c == '@') {
         return this.scanGherkinTag();
      } else {
         this.advance();
         return TokenType.WS;
      }
   }

   private TokenType scanGherkinTableRow() {
      char c = this.peek();
      if (c == '\r' || c == '\n') {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return this.scanGherkinWhitespaceWithNewline();
      } else if (c == '|') {
         this.advance();
         return TokenType.G_PIPE;
      } else {
         for (; !this.isAtEnd() && this.peek() != '|' && this.peek() != '\r' && this.peek() != '\n'; this.advance()) {
            if (this.peek() == '\\' && this.peek(1) == '|') {
               this.advance();
            }
         }

         return TokenType.G_TABLE_CELL;
      }
   }

   private TokenType scanGherkinDocString() {
      char c = this.peek();
      if (c == '"' && this.peek(1) == '"' && this.peek(2) == '"') {
         this.advance();
         this.advance();
         this.advance();
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return TokenType.G_TRIPLE_QUOTE;
      } else if (c != '\r' && c != '\n') {
         while (!this.isAtEnd()) {
            c = this.peek();
            if (c == '\r' || c == '\n' || c == '"' && this.peek(1) == '"' && this.peek(2) == '"') {
               break;
            }

            this.advance();
         }

         return TokenType.G_EXPR;
      } else {
         while (!this.isAtEnd() && (this.peek() == '\r' || this.peek() == '\n' || this.peek() == ' ' || this.peek() == '\t')) {
            this.advance();
         }

         return TokenType.WS_LF;
      }
   }

   private TokenType scanGherkinStep() {
      char c = this.peek();
      if (c == '\r' || c == '\n') {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return this.scanGherkinWhitespaceWithNewline();
      } else if (isIdentifierStart(c)) {
         return this.scanStepKeyword();
      } else if (c != '=') {
         this.gState = GherkinLexer.GherkinState.GS_RHS;
         return this.scanGherkinRhs();
      } else {
         this.advance();
         if (this.peek() != '\r' && this.peek() != '\n' && !this.isAtEnd()) {
            this.gState = GherkinLexer.GherkinState.GS_RHS;
         } else {
            this.gState = GherkinLexer.GherkinState.GHERKIN;
         }

         return TokenType.EQ;
      }
   }

   private TokenType scanStepKeyword() {
      int start = this.pos;

      while (!this.isAtEnd() && (isIdentifierPart(this.peek()) || this.peek() == ' ' || this.peek() == '.')) {
         if (this.peek() == '.') {
            this.advance();
         } else if (this.peek() == ' ') {
            String sofar = this.source.substring(start, this.pos);
            if (!this.isSpacedKeywordPrefix(sofar)) {
               break;
            }

            this.advance();
         } else {
            this.advance();
         }
      }

      String text = this.source.substring(start, this.pos);
      if (this.peek() == '[' || this.peek() == '(') {
         this.pos = start;
         this.col = this.tokenCol;
         this.gState = GherkinLexer.GherkinState.GS_RHS;
         return this.scanGherkinRhs();
      } else if (text.equals("match")) {
         this.gState = GherkinLexer.GherkinState.GS_STEP_MATCH;
         return TokenType.G_KEYWORD;
      } else if (this.isTypeKeyword(text) || this.isAssignKeyword(text)) {
         return TokenType.G_KEYWORD;
      } else if (this.isSpacedKeyword(text)) {
         this.gState = GherkinLexer.GherkinState.GS_RHS;
         return TokenType.G_KEYWORD;
      } else if (this.peek() != '\r' && this.peek() != '\n' && !this.isAtEnd()) {
         return TokenType.G_KEYWORD;
      } else {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return TokenType.G_KEYWORD;
      }
   }

   private boolean isTypeKeyword(String text) {
      return text.equals("def")
         || text.equals("json")
         || text.equals("xml")
         || text.equals("xmlstring")
         || text.equals("yaml")
         || text.equals("csv")
         || text.equals("string")
         || text.equals("bytes")
         || text.equals("copy");
   }

   private boolean isAssignKeyword(String text) {
      return text.equals("configure")
         || text.equals("header")
         || text.equals("param")
         || text.equals("cookie")
         || text.equals("form field")
         || text.equals("multipart file")
         || text.equals("multipart field");
   }

   private boolean isSpacedKeywordPrefix(String text) {
      return text.equals("form") || text.equals("multipart") || text.equals("soap") || text.equals("retry");
   }

   private boolean isSpacedKeyword(String text) {
      return text.equals("form fields")
         || text.equals("multipart fields")
         || text.equals("multipart files")
         || text.equals("soap action")
         || text.equals("retry until")
         || text.equals("multipart entity");
   }

   private TokenType scanGherkinStepMatch() {
      char c = this.peek();
      if (c == '\r' || c == '\n') {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         this.matchParenDepth = 0;
         return this.scanGherkinWhitespaceWithNewline();
      } else if (c != '(' && c != '[') {
         if ((c == ')' || c == ']') && this.matchParenDepth > 0) {
            this.matchParenDepth--;
            this.advance();
            return TokenType.G_EXPR;
         } else {
            if (this.matchParenDepth == 0) {
               if (c == '=' && this.peek(1) == '=') {
                  this.advance();
                  this.advance();
                  return this.finishMatchType();
               }

               if (c == '!' && this.peek(1) == '=') {
                  this.advance();
                  this.advance();
                  return this.finishMatchType();
               }
            }

            if (isIdentifierStart(c)) {
               int start = this.pos;

               while (!this.isAtEnd() && (isIdentifierPart(this.peek()) || this.peek() == '-')) {
                  this.advance();
               }

               String text = this.source.substring(start, this.pos);
               if (!text.equals("each") && !text.equals("header")) {
                  return this.matchParenDepth != 0 || !text.equals("contains") && !text.equals("within") ? TokenType.IDENT : this.finishMatchType();
               } else {
                  return TokenType.G_KEYWORD;
               }
            } else if (c == '!') {
               this.advance();
               int start = this.pos;

               while (!this.isAtEnd() && isIdentifierPart(this.peek())) {
                  this.advance();
               }

               String text = this.source.substring(start, this.pos);
               return this.matchParenDepth != 0 || !text.equals("contains") && !text.equals("within") ? TokenType.G_EXPR : this.finishMatchType();
            } else if (c == '"') {
               return this.scanQuotedExpr();
            } else if (c == '\'') {
               return this.scanSingleQuotedExpr();
            } else if (c == '$' || c == '@') {
               this.advance();

               while (!this.isAtEnd() && isIdentifierPart(this.peek())) {
                  this.advance();
               }

               return TokenType.IDENT;
            } else if (c == '?') {
               this.advance();
               if (this.match('(')) {
                  while (!this.isAtEnd() && this.peek() != ')') {
                     this.advance();
                  }

                  this.match(')');
               }

               return TokenType.IDENT;
            } else if (c == '.' && this.peek(1) == '.') {
               this.advance();
               this.advance();
               return TokenType.DOT;
            } else if (c == ':') {
               this.advance();
               return TokenType.DOT;
            } else if (c == '/') {
               this.advance();
               this.match('/');

               while (!this.isAtEnd() && isIdentifierPart(this.peek())) {
                  this.advance();
               }

               return TokenType.IDENT;
            } else if (!this.isWhitespace(c)) {
               this.advance();
               return TokenType.G_EXPR;
            } else {
               this.advance();
               return TokenType.WS;
            }
         }
      } else {
         this.matchParenDepth++;
         this.advance();
         return TokenType.G_EXPR;
      }
   }

   private TokenType finishMatchType() {
      while (!this.isAtEnd()) {
         char c = this.peek();
         if (c == ' ' || c == '\t') {
            int wsStart = this.pos;
            int wsCol = this.col;

            while (!this.isAtEnd() && (this.peek() == ' ' || this.peek() == '\t')) {
               this.advance();
            }

            if (this.checkAndConsumeModifier("only", 4) || this.checkAndConsumeModifier("any", 3) || this.checkAndConsumeModifier("deep", 4)) {
               continue;
            }

            this.pos = wsStart;
            this.col = wsCol;
         }
         break;
      }

      this.matchParenDepth = 0;
      if (this.peek() != '\r' && this.peek() != '\n' && !this.isAtEnd()) {
         this.gState = GherkinLexer.GherkinState.GS_RHS;
      } else {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
      }

      return TokenType.G_KEYWORD;
   }

   private boolean checkAndConsumeModifier(String modifier, int len) {
      if (this.pos + len > this.length) {
         return false;
      } else {
         for (int i = 0; i < len; i++) {
            if (this.source.charAt(this.pos + i) != modifier.charAt(i)) {
               return false;
            }
         }

         int afterPos = this.pos + len;
         if (afterPos < this.length && isIdentifierPart(this.source.charAt(afterPos))) {
            return false;
         } else {
            for (int ix = 0; ix < len; ix++) {
               this.advance();
            }

            return true;
         }
      }
   }

   private char charAtSafe(int index) {
      return index < this.length ? this.source.charAt(index) : '\u0000';
   }

   private TokenType scanQuotedExpr() {
      this.advance();

      while (!this.isAtEnd() && this.peek() != '"' && this.peek() != '\r' && this.peek() != '\n') {
         if (this.peek() == '\\') {
            this.advance();
            if (!this.isAtEnd()) {
               this.advance();
            }
         } else {
            this.advance();
         }
      }

      if (this.peek() == '"') {
         this.advance();
      }

      return TokenType.G_EXPR;
   }

   private TokenType scanSingleQuotedExpr() {
      this.advance();

      while (!this.isAtEnd() && this.peek() != '\'' && this.peek() != '\r' && this.peek() != '\n') {
         if (this.peek() == '\\') {
            this.advance();
            if (!this.isAtEnd()) {
               this.advance();
            }
         } else {
            this.advance();
         }
      }

      if (this.peek() == '\'') {
         this.advance();
      }

      return TokenType.G_EXPR;
   }

   private TokenType scanGherkinRhs() {
      char c = this.peek();
      if (c != '\r' && c != '\n') {
         while (!this.isAtEnd() && this.peek() != '\r' && this.peek() != '\n') {
            this.advance();
         }

         return TokenType.G_EXPR;
      } else {
         this.gState = GherkinLexer.GherkinState.GHERKIN;
         return this.scanGherkinWhitespaceWithNewline();
      }
   }

   private boolean isWhitespace(char c) {
      return c == ' ' || c == '\t' || c == '\r' || c == '\n';
   }

   private static enum GherkinState {
      GHERKIN,
      GS_DESC,
      GS_COMMENT,
      GS_TAGS,
      GS_TABLE_ROW,
      GS_DOC_STRING,
      GS_STEP,
      GS_STEP_MATCH,
      GS_RHS;
   }
}
