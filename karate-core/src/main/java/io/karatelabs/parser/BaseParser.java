package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseParser {
   static final Logger logger = LoggerFactory.getLogger(BaseParser.class);
   private static final int MAX_DEPTH = 128;
   protected final Resource resource;
   protected final List<Token> tokens;
   private final int size;
   private int position = 0;
   private int stackPointer = 0;
   private final int[] positionStack = new int[128];
   private final Node[] nodeStack = new Node[128];
   protected final boolean errorRecoveryEnabled;
   private final List<SyntaxError> errors = new ArrayList<>();
   private int lastRecoveryPosition = -1;
   private Token cachedPeek = Token.EMPTY;
   private int cachedPeekPos = -1;

   protected Node markerNode() {
      return this.nodeStack[this.stackPointer - 1];
   }

   protected boolean isCallerType(NodeType type) {
      return this.stackPointer >= 2 && this.nodeStack[this.stackPointer - 2].type == type;
   }

   protected BaseParser(Resource resource, List<Token> tokens, boolean errorRecovery) {
      this.resource = resource;
      this.errorRecoveryEnabled = errorRecovery;
      this.tokens = tokens;
      this.size = tokens.size();
      this.positionStack[0] = this.position;
      this.nodeStack[0] = new Node(NodeType.ROOT);
      this.stackPointer = 1;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int start = Math.max(0, this.position - 7);
      int end = Math.min(this.position + 7, this.size);

      for (int i = start; i < end; i++) {
         if (i == 0) {
            sb.append("| ");
         }

         if (i == this.position) {
            sb.append(">>");
         }

         sb.append(this.tokens.get(i));
         sb.append(' ');
      }

      if (this.position == this.size) {
         sb.append(">>");
      }

      sb.append("|");
      sb.append("\ncurrent node: ");
      if (this.stackPointer >= 3) {
         sb.append(this.nodeStack[this.stackPointer - 3].type).append(" >> ");
      }

      if (this.stackPointer >= 2) {
         sb.append(this.nodeStack[this.stackPointer - 2].type).append(" >> ");
      }

      sb.append('[').append(this.nodeStack[this.stackPointer - 1].type).append(']');
      return sb.toString();
   }

   protected void error(String message) {
      Token token = this.peekToken();
      if (this.errorRecoveryEnabled) {
         this.errors.add(new SyntaxError(token, message));
      } else {
         String hint = AsiHint.forFailure(this.tokens, this.position);
         if (hint == null) {
            hint = StrayQuoteHint.forFailure(this.tokens, this.position);
         }

         if (token.getResource().isFile()) {
            System.err.println("file://" + token.getResource().getUri().getPath() + ":" + token.getPositionDisplay() + " " + message);
         }

         throw new ParserException(message + "\n" + token.getPositionDisplay() + " " + token + "\nparser state: " + this + (hint == null ? "" : "\n" + hint));
      }
   }

   protected void error(NodeType... expected) {
      if (this.errorRecoveryEnabled) {
         this.errors.add(new SyntaxError(this.peekToken(), "expected: " + Arrays.<NodeType>asList(expected), expected[0]));
      } else {
         this.error("expected: " + Arrays.<NodeType>asList(expected));
      }
   }

   protected void error(TokenType... expected) {
      if (this.errorRecoveryEnabled) {
         this.errors.add(new SyntaxError(this.peekToken(), "expected: " + Arrays.<TokenType>asList(expected)));
      } else {
         this.error("expected: " + Arrays.<TokenType>asList(expected));
      }
   }

   public List<SyntaxError> getErrors() {
      return this.errors;
   }

   public boolean hasErrors() {
      return !this.errors.isEmpty();
   }

   protected boolean recoverTo(TokenType... recoveryTokens) {
      if (this.position == this.lastRecoveryPosition && this.peek() != TokenType.EOF) {
         this.consumeNext();
      }

      this.lastRecoveryPosition = this.position;
      Set<TokenType> recoverySet = Set.of(recoveryTokens);

      while (true) {
         TokenType current = this.peek();
         if (current == TokenType.EOF || recoverySet.contains(current)) {
            return current != TokenType.EOF;
         }

         this.consumeNext();
      }
   }

   protected boolean consumeSoft(TokenType token) {
      if (this.consumeIf(token)) {
         return true;
      } else {
         this.error(token);
         return this.errorRecoveryEnabled;
      }
   }

   protected boolean exitSoft() {
      return this.exit(true, false, BaseParser.Shift.NONE);
   }

   protected void enter(NodeType type) {
      if (this.stackPointer >= 128) {
         throw new ParserException("too much recursion");
      } else {
         this.positionStack[this.stackPointer] = this.position;
         this.nodeStack[this.stackPointer] = new Node(type);
         this.stackPointer++;
      }
   }

   protected boolean enter(NodeType type, TokenType token) {
      if (this.peek() != token) {
         return false;
      } else if (this.stackPointer >= 128) {
         throw new ParserException("too much recursion");
      } else {
         this.positionStack[this.stackPointer] = this.position;
         this.nodeStack[this.stackPointer] = new Node(type);
         this.stackPointer++;
         this.consumeNext();
         return true;
      }
   }

   protected boolean enter(NodeType type, TokenType[] tokens) {
      return this.enterIf(type, tokens);
   }

   protected boolean enterIf(NodeType type, TokenType[] tokens) {
      if (tokens != null && !this.peekAnyOf(tokens)) {
         return false;
      } else if (this.stackPointer >= 128) {
         throw new ParserException("too much recursion");
      } else {
         this.positionStack[this.stackPointer] = this.position;
         this.nodeStack[this.stackPointer] = new Node(type);
         this.stackPointer++;
         if (tokens != null) {
            this.consumeNext();
         }

         return true;
      }
   }

   protected boolean enter(NodeType type, EnumSet<TokenType> tokens) {
      return this.enterIf(type, tokens);
   }

   protected boolean enterIf(NodeType type, EnumSet<TokenType> tokens) {
      if (!this.peekAnyOf(tokens)) {
         return false;
      } else if (this.stackPointer >= 128) {
         throw new ParserException("too much recursion");
      } else {
         this.positionStack[this.stackPointer] = this.position;
         this.nodeStack[this.stackPointer] = new Node(type);
         this.stackPointer++;
         this.consumeNext();
         return true;
      }
   }

   protected boolean exit() {
      return this.exit(true, false, BaseParser.Shift.NONE);
   }

   protected boolean exit(boolean result, boolean mandatory) {
      return this.exit(result, mandatory, BaseParser.Shift.NONE);
   }

   protected void exit(BaseParser.Shift shift) {
      this.exit(true, false, shift);
   }

   private boolean exit(boolean result, boolean mandatory, BaseParser.Shift shift) {
      Node node = this.nodeStack[this.stackPointer - 1];
      if (mandatory && !result) {
         this.error(node.type);
      }

      if (result) {
         Node parent = this.nodeStack[this.stackPointer - 2];
         switch (shift) {
            case NONE:
               parent.add(node);
               break;
            case LEFT:
               Node prev = parent.removeFirst();
               node.addFirst(prev);
               parent.add(node);
               break;
            case RIGHT:
               Node prevSibling = parent.removeFirst();
               if (prevSibling.type == node.type) {
                  Node newNode = new Node(node.type);
                  parent.add(newNode);
                  newNode.add(prevSibling.get(0));
                  newNode.add(prevSibling.get(1));
                  Node newRhs = new Node(node.type);
                  newNode.add(newRhs);
                  newRhs.add(prevSibling.get(2));
                  newRhs.add(node.get(0));
                  newRhs.add(node.get(1));
               } else {
                  node.addFirst(prevSibling);
                  parent.add(node);
               }
         }
      } else {
         this.position = this.positionStack[this.stackPointer - 1];
      }

      this.stackPointer--;
      this.nodeStack[this.stackPointer] = null;
      return result;
   }

   protected TokenType peek() {
      return this.peekToken().type;
   }

   protected Token peekToken() {
      if (this.cachedPeekPos != this.position) {
         this.cachedPeekPos = this.position;
         this.cachedPeek = this.position == this.size ? Token.EMPTY : this.tokens.get(this.position);
      }

      return this.cachedPeek;
   }

   protected void consume(TokenType token) {
      if (!this.consumeIf(token)) {
         this.error(token);
      }
   }

   protected boolean anyOf(TokenType[] tokens) {
      for (TokenType token : tokens) {
         if (this.consumeIf(token)) {
            return true;
         }
      }

      return false;
   }

   protected boolean anyOf(EnumSet<TokenType> tokens) {
      if (tokens.contains(this.peek())) {
         this.consumeNext();
         return true;
      } else {
         return false;
      }
   }

   protected boolean consumeIf(TokenType token) {
      if (this.peekIf(token)) {
         this.consumeNext();
         return true;
      } else {
         return false;
      }
   }

   protected boolean peekIf(TokenType token) {
      return this.peek() == token;
   }

   protected boolean peekAnyOf(TokenType[] tokens) {
      TokenType current = this.peek();

      for (TokenType token : tokens) {
         if (current == token) {
            return true;
         }
      }

      return false;
   }

   protected boolean peekAnyOf(EnumSet<TokenType> tokens) {
      return tokens.contains(this.peek());
   }

   protected TokenType lastConsumed() {
      return this.nodeStack[this.stackPointer - 1].getLast().token.type;
   }

   protected String lastConsumedText() {
      return this.nodeStack[this.stackPointer - 1].getLast().token.getText();
   }

   protected boolean noLineTerminatorBefore() {
      Token next = this.peekToken();
      return next == Token.EMPTY || !lineTerminatorBetween(this.nodeStack[this.stackPointer - 1].getLastToken(), next);
   }

   protected static boolean lineTerminatorFollows(Token tok) {
      Token next = tok.getNextPrimary();
      return next != null && lineTerminatorBetween(tok, next);
   }

   static boolean lineTerminatorBetween(Token last, Token next) {
      String text = last.getResource().getText();
      int from = Math.max(last.pos, Math.min(last.pos + last.length, text.length()));
      int to = Math.min(next.pos, text.length());

      for (int i = from; i < to; i++) {
         if (isLineTerminator(text.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isLineTerminator(char c) {
      return c == '\n' || c == '\r' || c == 8232 || c == 8233;
   }

   protected int getPosition() {
      return this.position;
   }

   protected void consumeNext() {
      this.nodeStack[this.stackPointer - 1].add(new Node(this.next()));
   }

   protected Token next() {
      return this.position == this.size ? Token.EMPTY : this.tokens.get(this.position++);
   }

   protected static enum Shift {
      NONE,
      LEFT,
      RIGHT;
   }
}
