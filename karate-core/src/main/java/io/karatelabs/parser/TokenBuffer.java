package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.Arrays;
import java.util.List;

public class TokenBuffer {
   private static final int MIN_CAPACITY = 64;
   private static final int CHARS_PER_TOKEN = 4;
   public final Resource resource;
   private Token[] tokens;
   private int count;
   private int capacity;
   private List<Token>[] comments;
   private volatile String[] texts;
   boolean escapedIdentifiers;
   private volatile Object[] literals;

   public TokenBuffer(Resource resource) {
      this.resource = resource;
      int sourceLength = resource.getText().length();
      this.capacity = Math.max(64, sourceLength / 4);
      this.tokens = new Token[this.capacity];
      this.count = 0;
   }

   public int addToken(Token token) {
      if (this.count >= this.capacity) {
         this.grow();
      }

      int index = this.count++;
      this.tokens[index] = token;
      return index;
   }

   private void grow() {
      int newCapacity = this.capacity * 2;
      this.tokens = Arrays.copyOf(this.tokens, newCapacity);
      if (this.comments != null) {
         this.comments = Arrays.copyOf(this.comments, newCapacity);
      }

      if (this.texts != null) {
         this.texts = Arrays.copyOf(this.texts, newCapacity);
      }

      if (this.literals != null) {
         this.literals = Arrays.copyOf(this.literals, newCapacity);
      }

      this.capacity = newCapacity;
   }

   Object getLiteral(int index) {
      Object[] cache = this.literals;
      return cache != null && index >= 0 && index < cache.length ? cache[index] : null;
   }

   void putLiteral(int index, Object value) {
      if (index >= 0) {
         Object[] cache = this.literals;
         if (cache == null) {
            cache = this.literals = new Object[this.cacheSize()];
         }

         if (index < cache.length) {
            cache[index] = value;
         }
      }
   }

   private int cacheSize() {
      return Math.max(this.count, 1);
   }

   String getText(int index, int pos, int length) {
      if (index >= 0 && index < this.count) {
         String[] cache = this.texts;
         if (cache == null) {
            cache = this.texts = new String[this.cacheSize()];
         }

         if (index >= cache.length) {
            return this.extractText(index, pos, length);
         } else {
            String text = cache[index];
            if (text == null) {
               text = cache[index] = this.extractText(index, pos, length);
            }

            return text;
         }
      } else {
         return this.resource.getText().substring(pos, pos + length);
      }
   }

   private String extractText(int index, int pos, int length) {
      String text = this.resource.getText().substring(pos, pos + length);
      return this.escapedIdentifiers && this.tokens[index].type == TokenType.IDENT ? JsLexer.identifierName(text) : text;
   }

   public Token getToken(int index) {
      return index >= 0 && index < this.count ? this.tokens[index] : null;
   }

   public Token getPrev(int index) {
      return index > 0 ? this.tokens[index - 1] : null;
   }

   public Token getNext(int index) {
      return index >= 0 && index < this.count - 1 ? this.tokens[index + 1] : null;
   }

   public void setComments(int index, List<Token> tokenComments) {
      if (this.comments == null) {
         this.comments = new List[this.capacity];
      }

      this.comments[index] = tokenComments;
   }

   public List<Token> getComments(int index) {
      return this.comments != null && index >= 0 && index < this.count ? this.comments[index] : null;
   }

   public int size() {
      return this.count;
   }
}
