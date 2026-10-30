package io.karatelabs.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {
   static final Logger logger = LoggerFactory.getLogger(Node.class);
   private static final int INITIAL_CAPACITY = 4;
   private static final Node[] EMPTY_ARRAY = new Node[0];
   public final NodeType type;
   public final Token token;
   public boolean async;
   public boolean generator;
   private Node[] children;
   private short childCount;
   private String cachedText;
   public short slot = -1;
   public volatile Object meta;
   private Node parent;

   private void ensureCapacity(int minCapacity) {
      if (this.children == null) {
         this.children = new Node[Math.max(4, minCapacity)];
      } else if (minCapacity > this.children.length) {
         int newCapacity = Math.max(this.children.length * 2, minCapacity);
         this.children = Arrays.copyOf(this.children, newCapacity);
      }
   }

   public Node(NodeType type) {
      this.type = type;
      this.token = Token.EMPTY;
      if (type.expectedChildren > 0) {
         this.children = new Node[type.expectedChildren];
      }
   }

   public Node(Token token) {
      this.token = token;
      this.type = NodeType.TOKEN;
   }

   public Node getParent() {
      return this.parent;
   }

   public boolean isToken() {
      return this.type == NodeType.TOKEN;
   }

   public boolean isEof() {
      return this.type == NodeType.TOKEN && this.token.type == TokenType.EOF;
   }

   public Token getFirstToken() {
      if (this.isToken()) {
         return this.token;
      } else {
         return this.childCount == 0 ? Token.EMPTY : this.children[0].getFirstToken();
      }
   }

   public Token getLastToken() {
      if (this.isToken()) {
         return this.token;
      } else {
         return this.childCount == 0 ? Token.EMPTY : this.children[this.childCount - 1].getLastToken();
      }
   }

   public String toStringError(String message) {
      Token first = this.getFirstToken();
      if (first.getResource().isFile()) {
         return message + "\n    at " + first.getResource().getRelativePath() + ":" + first.getPositionDisplay();
      } else {
         return first.line == 0 ? message : message + "\n    at " + first.getPositionDisplay();
      }
   }

   @Override
   public String toString() {
      return this.isToken() ? this.token.getText() : "[" + this.type + "] " + this.getTextIncludingWhitespace();
   }

   public Node findFirstChild(NodeType type) {
      for (int i = 0; i < this.childCount; i++) {
         Node child = this.children[i];
         if (child.type == type) {
            return child;
         }

         Node temp = child.findFirstChild(type);
         if (temp != null) {
            return temp;
         }
      }

      return null;
   }

   public List<Node> findAll(NodeType type) {
      List<Node> results = new ArrayList<>();
      this.findAll(type, results);
      return results;
   }

   private void findAll(NodeType type, List<Node> results) {
      for (int i = 0; i < this.childCount; i++) {
         Node child = this.children[i];
         if (child.type == type) {
            results.add(child);
         }

         child.findAll(type, results);
      }
   }

   public Node findFirstChild(TokenType token) {
      for (int i = 0; i < this.childCount; i++) {
         Node child = this.children[i];
         if (child.token.type == token) {
            return child;
         }

         Node temp = child.findFirstChild(token);
         if (temp != null) {
            return temp;
         }
      }

      return null;
   }

   public Node findParent(NodeType type) {
      Node temp = this.parent;

      while (temp != null && temp.type != type) {
         temp = temp.parent;
      }

      return temp;
   }

   public List<Node> findImmediateChildren(NodeType type) {
      List<Node> results = new ArrayList<>();

      for (int i = 0; i < this.childCount; i++) {
         Node child = this.children[i];
         if (child.type == type) {
            results.add(child);
         }
      }

      return results;
   }

   public List<Node> findChildren(TokenType token) {
      List<Node> results = new ArrayList<>();
      this.findChildren(token, results);
      return results;
   }

   private void findChildren(TokenType token, List<Node> results) {
      for (int i = 0; i < this.childCount; i++) {
         Node child = this.children[i];
         if (!child.isToken()) {
            child.findChildren(token, results);
         } else if (child.token.type == token) {
            results.add(child);
         }
      }
   }

   public String getText() {
      if (this.cachedText != null) {
         return this.cachedText;
      } else if (this.isToken()) {
         this.cachedText = this.token.getText();
         return this.cachedText;
      } else if (this.childCount == 1) {
         this.cachedText = this.children[0].getText();
         return this.cachedText;
      } else {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.childCount; i++) {
            sb.append(this.children[i].getText());
         }

         this.cachedText = sb.toString();
         return this.cachedText;
      }
   }

   public String getTextIncludingWhitespace() {
      if (this.isToken()) {
         return this.token.getText();
      } else {
         int start = this.getFirstToken().pos;
         Token last = this.getLastToken();
         int end = last.pos + last.length;
         return last.getResource().getText().substring(start, end);
      }
   }

   public Node removeFirst() {
      if (this.childCount == 0) {
         throw new NoSuchElementException();
      } else {
         Node first = this.children[0];
         System.arraycopy(this.children, 1, this.children, 0, this.childCount - 1);
         this.children[--this.childCount] = null;
         return first;
      }
   }

   public void addFirst(Node child) {
      child.parent = this;
      this.ensureCapacity(this.childCount + 1);
      System.arraycopy(this.children, 0, this.children, 1, this.childCount);
      this.children[0] = child;
      this.childCount++;
   }

   public void add(Node child) {
      child.parent = this;
      this.ensureCapacity(this.childCount + 1);
      this.children[this.childCount++] = child;
   }

   public Node getFirst() {
      if (this.childCount == 0) {
         throw new NoSuchElementException();
      } else {
         return this.children[0];
      }
   }

   public Node getLast() {
      if (this.childCount == 0) {
         throw new NoSuchElementException();
      } else {
         return this.children[this.childCount - 1];
      }
   }

   public Node get(int index) {
      if (index >= 0 && index < this.childCount) {
         return this.children[index];
      } else {
         throw new IndexOutOfBoundsException(index);
      }
   }

   public int size() {
      return this.childCount;
   }

   public boolean isEmpty() {
      return this.childCount == 0;
   }
}
