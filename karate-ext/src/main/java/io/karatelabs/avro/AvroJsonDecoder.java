package io.karatelabs.avro;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.util.TokenBuffer;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.io.ParsingDecoder;
import org.apache.avro.io.parsing.JsonGrammarGenerator;
import org.apache.avro.io.parsing.Symbol;
import org.apache.avro.io.parsing.Parser.ActionHandler;
import org.apache.avro.io.parsing.Symbol.Alternative;
import org.apache.avro.io.parsing.Symbol.EnumLabelsAction;
import org.apache.avro.io.parsing.Symbol.FieldAdjustAction;
import org.apache.avro.io.parsing.Symbol.IntCheckAction;
import org.apache.avro.util.Utf8;

public class AvroJsonDecoder extends ParsingDecoder implements ActionHandler {
   private JsonParser in;
   private static JsonFactory jsonFactory = new JsonFactory();
   Stack<io.karatelabs.avro.AvroJsonDecoder.ReorderBuffer> reorderBuffers = new Stack<>();
   io.karatelabs.avro.AvroJsonDecoder.ReorderBuffer currentReorderBuffer;

   private AvroJsonDecoder(Symbol root, InputStream in) throws IOException {
      super(root);
      this.configure(in);
   }

   private AvroJsonDecoder(Symbol root, String in) throws IOException {
      super(root);
      this.configure(in);
   }

   public AvroJsonDecoder(Schema schema, InputStream in) throws IOException {
      this(getSymbol(schema), in);
   }

   public AvroJsonDecoder(Schema schema, String in) throws IOException {
      this(getSymbol(schema), in);
   }

   private static Symbol getSymbol(Schema schema) {
      Objects.requireNonNull(schema, "Schema cannot be null");
      return new JsonGrammarGenerator().generate(schema);
   }

   public io.karatelabs.avro.AvroJsonDecoder configure(InputStream in) throws IOException {
      Objects.requireNonNull(in, "InputStream cannot be null");
      this.parser.reset();
      this.reorderBuffers.clear();
      this.currentReorderBuffer = null;
      this.in = jsonFactory.createParser(tools.jackson.core.ObjectReadContext.empty(), in);
      this.in.nextToken();
      return this;
   }

   public io.karatelabs.avro.AvroJsonDecoder configure(String in) throws IOException {
      Objects.requireNonNull(in, "String to read from cannot be null");
      this.parser.reset();
      this.reorderBuffers.clear();
      this.currentReorderBuffer = null;
      this.in = new JsonFactory().createParser(tools.jackson.core.ObjectReadContext.empty(), in);
      this.in.nextToken();
      return this;
   }

   private void advance(Symbol symbol) throws IOException {
      this.parser.processTrailingImplicitActions();
      if (this.in.currentToken() == null && this.parser.depth() == 1) {
         throw new EOFException();
      } else {
         this.parser.advance(symbol);
      }
   }

   public void readNull() throws IOException {
      this.advance(Symbol.NULL);
      if (this.in.currentToken() == JsonToken.VALUE_NULL) {
         this.in.nextToken();
      } else {
         throw this.error("null");
      }
   }

   public boolean readBoolean() throws IOException {
      this.advance(Symbol.BOOLEAN);
      JsonToken t = this.in.currentToken();
      if (t != JsonToken.VALUE_TRUE && t != JsonToken.VALUE_FALSE) {
         throw this.error("boolean");
      } else {
         this.in.nextToken();
         return t == JsonToken.VALUE_TRUE;
      }
   }

   public int readInt() throws IOException {
      this.advance(Symbol.INT);
      if (this.in.currentToken().isNumeric()) {
         int result = this.in.getIntValue();
         this.in.nextToken();
         return result;
      } else {
         throw this.error("int");
      }
   }

   public long readLong() throws IOException {
      this.advance(Symbol.LONG);
      if (this.in.currentToken().isNumeric()) {
         long result = this.in.getLongValue();
         this.in.nextToken();
         return result;
      } else {
         throw this.error("long");
      }
   }

   public float readFloat() throws IOException {
      this.advance(Symbol.FLOAT);
      if (this.in.currentToken().isNumeric()) {
         float result = this.in.getFloatValue();
         this.in.nextToken();
         return result;
      } else {
         throw this.error("float");
      }
   }

   public double readDouble() throws IOException {
      this.advance(Symbol.DOUBLE);
      if (this.in.currentToken().isNumeric()) {
         double result = this.in.getDoubleValue();
         this.in.nextToken();
         return result;
      } else {
         throw this.error("double");
      }
   }

   public Utf8 readString(Utf8 old) throws IOException {
      return new Utf8(this.readString());
   }

   public String readString() throws IOException {
      this.advance(Symbol.STRING);
      if (this.parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
         this.parser.advance(Symbol.MAP_KEY_MARKER);
         if (this.in.currentToken() != JsonToken.PROPERTY_NAME) {
            throw this.error("map-key");
         }
      } else if (this.in.currentToken() != JsonToken.VALUE_STRING) {
         throw this.error("string");
      }

      String result = this.in.getText();
      this.in.nextToken();
      return result;
   }

   public void skipString() throws IOException {
      this.advance(Symbol.STRING);
      if (this.parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
         this.parser.advance(Symbol.MAP_KEY_MARKER);
         if (this.in.currentToken() != JsonToken.PROPERTY_NAME) {
            throw this.error("map-key");
         }
      } else if (this.in.currentToken() != JsonToken.VALUE_STRING) {
         throw this.error("string");
      }

      this.in.nextToken();
   }

   public ByteBuffer readBytes(ByteBuffer old) throws IOException {
      this.advance(Symbol.BYTES);
      if (this.in.currentToken() == JsonToken.VALUE_STRING) {
         byte[] result = this.readByteArray();
         this.in.nextToken();
         return ByteBuffer.wrap(result);
      } else {
         throw this.error("bytes");
      }
   }

   private byte[] readByteArray() throws IOException {
      return this.in.getText().getBytes(StandardCharsets.ISO_8859_1);
   }

   public void skipBytes() throws IOException {
      this.advance(Symbol.BYTES);
      if (this.in.currentToken() == JsonToken.VALUE_STRING) {
         this.in.nextToken();
      } else {
         throw this.error("bytes");
      }
   }

   private void checkFixed(int size) throws IOException {
      this.advance(Symbol.FIXED);
      IntCheckAction top = (IntCheckAction)this.parser.popSymbol();
      if (size != top.size) {
         throw new AvroTypeException("Incorrect length for fixed binary: expected " + top.size + " but received " + size + " bytes.");
      }
   }

   public void readFixed(byte[] bytes, int start, int len) throws IOException {
      this.checkFixed(len);
      if (this.in.currentToken() == JsonToken.VALUE_STRING) {
         byte[] result = this.readByteArray();
         this.in.nextToken();
         if (result.length != len) {
            throw new AvroTypeException("Expected fixed length " + len + ", but got" + result.length);
         } else {
            System.arraycopy(result, 0, bytes, start, len);
         }
      } else {
         throw this.error("fixed");
      }
   }

   public void skipFixed(int length) throws IOException {
      this.checkFixed(length);
      this.doSkipFixed(length);
   }

   private void doSkipFixed(int length) throws IOException {
      if (this.in.currentToken() == JsonToken.VALUE_STRING) {
         byte[] result = this.readByteArray();
         this.in.nextToken();
         if (result.length != length) {
            throw new AvroTypeException("Expected fixed length " + length + ", but got" + result.length);
         }
      } else {
         throw this.error("fixed");
      }
   }

   protected void skipFixed() throws IOException {
      this.advance(Symbol.FIXED);
      IntCheckAction top = (IntCheckAction)this.parser.popSymbol();
      this.doSkipFixed(top.size);
   }

   public int readEnum() throws IOException {
      this.advance(Symbol.ENUM);
      EnumLabelsAction top = (EnumLabelsAction)this.parser.popSymbol();
      if (this.in.currentToken() == JsonToken.VALUE_STRING) {
         this.in.getText();
         int n = top.findLabel(this.in.getText());
         if (n >= 0) {
            this.in.nextToken();
            return n;
         } else {
            throw new AvroTypeException("Unknown symbol in enum " + this.in.getText());
         }
      } else {
         throw this.error("fixed");
      }
   }

   public long readArrayStart() throws IOException {
      this.advance(Symbol.ARRAY_START);
      if (this.in.currentToken() == JsonToken.START_ARRAY) {
         this.in.nextToken();
         return this.doArrayNext();
      } else {
         throw this.error("array-start");
      }
   }

   public long arrayNext() throws IOException {
      this.advance(Symbol.ITEM_END);
      return this.doArrayNext();
   }

   private long doArrayNext() throws IOException {
      if (this.in.currentToken() == JsonToken.END_ARRAY) {
         this.parser.advance(Symbol.ARRAY_END);
         this.in.nextToken();
         return 0L;
      } else {
         return 1L;
      }
   }

   public long skipArray() throws IOException {
      this.advance(Symbol.ARRAY_START);
      if (this.in.currentToken() == JsonToken.START_ARRAY) {
         this.in.skipChildren();
         this.in.nextToken();
         this.advance(Symbol.ARRAY_END);
         return 0L;
      } else {
         throw this.error("array-start");
      }
   }

   public long readMapStart() throws IOException {
      this.advance(Symbol.MAP_START);
      if (this.in.currentToken() == JsonToken.START_OBJECT) {
         this.in.nextToken();
         return this.doMapNext();
      } else {
         throw this.error("map-start");
      }
   }

   public long mapNext() throws IOException {
      this.advance(Symbol.ITEM_END);
      return this.doMapNext();
   }

   private long doMapNext() throws IOException {
      if (this.in.currentToken() == JsonToken.END_OBJECT) {
         this.in.nextToken();
         this.advance(Symbol.MAP_END);
         return 0L;
      } else {
         return 1L;
      }
   }

   public long skipMap() throws IOException {
      this.advance(Symbol.MAP_START);
      if (this.in.currentToken() == JsonToken.START_OBJECT) {
         this.in.skipChildren();
         this.in.nextToken();
         this.advance(Symbol.MAP_END);
         return 0L;
      } else {
         throw this.error("map-start");
      }
   }

   public int readIndex() throws IOException {
      this.advance(Symbol.UNION);
      Alternative a = (Alternative)this.parser.popSymbol();
      String label;
      if (this.in.currentToken() == JsonToken.VALUE_NULL) {
         label = "null";
      } else if (this.in.currentToken() == JsonToken.START_OBJECT && this.in.nextToken() == JsonToken.PROPERTY_NAME) {
         label = this.in.getText();
         this.in.nextToken();
         this.parser.pushSymbol(Symbol.UNION_END);
      } else {
         if (a.size() != 2 || !"null".equals(a.getLabel(0)) && !"null".equals(a.getLabel(1))) {
            throw this.error("start-union");
         }

         label = "null".equals(a.getLabel(0)) ? a.getLabel(1) : a.getLabel(0);
      }

      int n = a.findLabel(label);
      if (n < 0) {
         throw new AvroTypeException("Unknown union branch " + label);
      } else {
         this.parser.pushSymbol(a.getSymbol(n));
         return n;
      }
   }

   public Symbol doAction(Symbol input, Symbol top) throws IOException {
      if (!(top instanceof FieldAdjustAction fa)) {
         if (top == Symbol.FIELD_END) {
            if (this.currentReorderBuffer != null && this.currentReorderBuffer.origParser != null) {
               this.in = this.currentReorderBuffer.origParser;
               this.currentReorderBuffer.origParser = null;
            }
         } else if (top == Symbol.RECORD_START) {
            if (this.in.currentToken() != JsonToken.START_OBJECT) {
               throw this.error("record-start");
            }

            this.in.nextToken();
            this.reorderBuffers.push(this.currentReorderBuffer);
            this.currentReorderBuffer = null;
         } else {
            if (top != Symbol.RECORD_END && top != Symbol.UNION_END) {
               throw new AvroTypeException("Unknown action symbol " + top);
            }

            while (this.in.currentToken() != JsonToken.END_OBJECT) {
               this.in.nextToken();
            }

            if (top == Symbol.RECORD_END) {
               if (this.currentReorderBuffer != null && !this.currentReorderBuffer.savedFields.isEmpty()) {
                  throw this.error("Unknown fields: " + this.currentReorderBuffer.savedFields.keySet());
               }

               this.currentReorderBuffer = this.reorderBuffers.pop();
            }

            this.in.nextToken();
         }
      } else {
         TokenBuffer tokenBuffer;
         Object var14;
         label138: {
            String name = fa.fname;
            if (this.currentReorderBuffer != null) {
               tokenBuffer = this.currentReorderBuffer.savedFields.get(name);

               try {
                  if (tokenBuffer != null) {
                     this.currentReorderBuffer.savedFields.remove(name);
                     this.currentReorderBuffer.origParser = this.in;
                     this.in = tokenBuffer.asParser();
                     this.in.nextToken();
                     var14 = null;
                     break label138;
                  }
               } catch (Throwable var12) {
                  if (tokenBuffer != null) {
                     try {
                        tokenBuffer.close();
                     } catch (Throwable var10) {
                        var12.addSuppressed(var10);
                     }
                  }

                  throw var12;
               }

               if (tokenBuffer != null) {
                  tokenBuffer.close();
               }
            }

            if (this.in.currentToken() == JsonToken.PROPERTY_NAME) {
               do {
                  String fn = this.in.getText();
                  this.in.nextToken();
                  if (name.equals(fn) || fa.aliases.contains(fn)) {
                     return null;
                  }

                  if (this.currentReorderBuffer == null) {
                     this.currentReorderBuffer = new io.karatelabs.avro.AvroJsonDecoder.ReorderBuffer();
                  }

                  TokenBuffer tokenBufferx = TokenBuffer.forBuffering(this.in, this.in.objectReadContext());

                  try {
                     tokenBufferx.copyCurrentStructure(this.in);
                     this.currentReorderBuffer.savedFields.put(fn, tokenBufferx);
                  } catch (Throwable var11) {
                     try {
                        tokenBufferx.close();
                     } catch (Throwable var9) {
                        var11.addSuppressed(var9);
                     }

                     throw var11;
                  }

                  tokenBufferx.close();
                  this.in.nextToken();
               } while (this.in.currentToken() == JsonToken.PROPERTY_NAME);

               throw new AvroTypeException("Expected field name not found: " + fa.fname);
            }

            return null;
         }

         if (tokenBuffer != null) {
            tokenBuffer.close();
         }

         return (Symbol)var14;
      }

      return null;
   }

   private AvroTypeException error(String type) {
      return new AvroTypeException("Expected " + type + ". Got " + this.in.currentToken());
   }

   private static class ReorderBuffer {
      public Map<String, TokenBuffer> savedFields = new HashMap<>();
      public JsonParser origParser = null;
   }
}
