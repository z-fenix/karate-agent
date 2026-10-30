package io.karatelabs.avro;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.MinimalPrettyPrinter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Objects;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.io.ParsingEncoder;
import org.apache.avro.io.parsing.JsonGrammarGenerator;
import org.apache.avro.io.parsing.Parser;
import org.apache.avro.io.parsing.Symbol;
import org.apache.avro.io.parsing.Parser.ActionHandler;
import org.apache.avro.io.parsing.Symbol.Alternative;
import org.apache.avro.io.parsing.Symbol.EnumLabelsAction;
import org.apache.avro.io.parsing.Symbol.FieldAdjustAction;
import org.apache.avro.io.parsing.Symbol.IntCheckAction;
import org.apache.avro.util.Utf8;

public class AvroJsonEncoder extends ParsingEncoder implements ActionHandler {
   private static final String LINE_SEPARATOR = System.getProperty("line.separator");
   final Parser parser;
   private JsonGenerator out;
   private boolean includeNamespace = true;
   protected BitSet isEmpty = new BitSet();

   public AvroJsonEncoder(Schema sc, OutputStream out) throws IOException {
      this(sc, getJsonGenerator(out, false));
   }

   public AvroJsonEncoder(Schema sc, OutputStream out, boolean pretty) throws IOException {
      this(sc, getJsonGenerator(out, pretty));
   }

   public AvroJsonEncoder(Schema sc, JsonGenerator out) throws IOException {
      this.configure(out);
      this.parser = new Parser(new JsonGrammarGenerator().generate(sc), this);
   }

   public void flush() throws IOException {
      this.parser.processImplicitActions();
      if (this.out != null) {
         this.out.flush();
      }
   }

   private static JsonGenerator getJsonGenerator(OutputStream out, boolean pretty) throws IOException {
      Objects.requireNonNull(out, "OutputStream cannot be null");
      tools.jackson.core.PrettyPrinter printer;
      if (pretty) {
         printer = new DefaultPrettyPrinter() {
            @Override
            public void writeRootValueSeparator(JsonGenerator jg) {
               jg.writeRaw(LINE_SEPARATOR);
            }
         };
      } else {
         MinimalPrettyPrinter minimal = new MinimalPrettyPrinter();
         minimal.setRootValueSeparator(LINE_SEPARATOR);
         printer = minimal;
      }
      tools.jackson.core.ObjectWriteContext context = new tools.jackson.core.ObjectWriteContext.Base() {
         @Override
         public tools.jackson.core.PrettyPrinter getPrettyPrinter() {
            return printer;
         }
      };
      JsonGenerator g = new JsonFactory().createGenerator(context, out, JsonEncoding.UTF8);
      return g;
   }

   public boolean isIncludeNamespace() {
      return this.includeNamespace;
   }

   public void setIncludeNamespace(boolean includeNamespace) {
      this.includeNamespace = includeNamespace;
   }

   public io.karatelabs.avro.AvroJsonEncoder configure(OutputStream out) throws IOException {
      this.configure(getJsonGenerator(out, false));
      return this;
   }

   private io.karatelabs.avro.AvroJsonEncoder configure(JsonGenerator generator) throws IOException {
      Objects.requireNonNull(generator, "JsonGenerator cannot be null");
      if (null != this.parser) {
         this.flush();
      }

      this.out = generator;
      return this;
   }

   public void writeNull() throws IOException {
      this.parser.advance(Symbol.NULL);
      this.out.writeNull();
   }

   public void writeBoolean(boolean b) throws IOException {
      this.parser.advance(Symbol.BOOLEAN);
      this.out.writeBoolean(b);
   }

   public void writeInt(int n) throws IOException {
      this.parser.advance(Symbol.INT);
      this.out.writeNumber(n);
   }

   public void writeLong(long n) throws IOException {
      this.parser.advance(Symbol.LONG);
      this.out.writeNumber(n);
   }

   public void writeFloat(float f) throws IOException {
      this.parser.advance(Symbol.FLOAT);
      this.out.writeNumber(f);
   }

   public void writeDouble(double d) throws IOException {
      this.parser.advance(Symbol.DOUBLE);
      this.out.writeNumber(d);
   }

   public void writeString(Utf8 utf8) throws IOException {
      this.writeString(utf8.toString());
   }

   public void writeString(String str) throws IOException {
      this.parser.advance(Symbol.STRING);
      if (this.parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
         this.parser.advance(Symbol.MAP_KEY_MARKER);
         this.out.writeName(str);
      } else {
         this.out.writeString(str);
      }
   }

   public void writeBytes(ByteBuffer bytes) throws IOException {
      if (bytes.hasArray()) {
         this.writeBytes(bytes.array(), bytes.position(), bytes.remaining());
      } else {
         byte[] b = new byte[bytes.remaining()];
         bytes.duplicate().get(b);
         this.writeBytes(b);
      }
   }

   public void writeBytes(byte[] bytes, int start, int len) throws IOException {
      this.parser.advance(Symbol.BYTES);
      this.writeByteArray(bytes, start, len);
   }

   private void writeByteArray(byte[] bytes, int start, int len) throws IOException {
      this.out.writeString(new String(bytes, start, len, StandardCharsets.ISO_8859_1));
   }

   public void writeFixed(byte[] bytes, int start, int len) throws IOException {
      this.parser.advance(Symbol.FIXED);
      IntCheckAction top = (IntCheckAction)this.parser.popSymbol();
      if (len != top.size) {
         throw new AvroTypeException("Incorrect length for fixed binary: expected " + top.size + " but received " + len + " bytes.");
      } else {
         this.writeByteArray(bytes, start, len);
      }
   }

   public void writeEnum(int e) throws IOException {
      this.parser.advance(Symbol.ENUM);
      EnumLabelsAction top = (EnumLabelsAction)this.parser.popSymbol();
      if (e >= 0 && e < top.size) {
         this.out.writeString(top.getLabel(e));
      } else {
         throw new AvroTypeException("Enumeration out of range: max is " + top.size + " but received " + e);
      }
   }

   public void writeArrayStart() throws IOException {
      this.parser.advance(Symbol.ARRAY_START);
      this.out.writeStartArray();
      this.push();
      this.isEmpty.set(this.depth());
   }

   public void writeArrayEnd() throws IOException {
      if (!this.isEmpty.get(this.pos)) {
         this.parser.advance(Symbol.ITEM_END);
      }

      this.pop();
      this.parser.advance(Symbol.ARRAY_END);
      this.out.writeEndArray();
   }

   public void writeMapStart() throws IOException {
      this.push();
      this.isEmpty.set(this.depth());
      this.parser.advance(Symbol.MAP_START);
      this.out.writeStartObject();
   }

   public void writeMapEnd() throws IOException {
      if (!this.isEmpty.get(this.pos)) {
         this.parser.advance(Symbol.ITEM_END);
      }

      this.pop();
      this.parser.advance(Symbol.MAP_END);
      this.out.writeEndObject();
   }

   public void startItem() throws IOException {
      if (!this.isEmpty.get(this.pos)) {
         this.parser.advance(Symbol.ITEM_END);
      }

      super.startItem();
      this.isEmpty.clear(this.depth());
   }

   public void writeIndex(int unionIndex) throws IOException {
      this.parser.advance(Symbol.UNION);
      Alternative top = (Alternative)this.parser.popSymbol();
      Symbol symbol = top.getSymbol(unionIndex);
      this.parser.pushSymbol(symbol);
   }

   public Symbol doAction(Symbol input, Symbol top) throws IOException {
      if (top instanceof FieldAdjustAction fa) {
         this.out.writeName(fa.fname);
      } else if (top == Symbol.RECORD_START) {
         this.out.writeStartObject();
      } else if (top != Symbol.RECORD_END && top != Symbol.UNION_END) {
         if (top != Symbol.FIELD_END) {
            throw new AvroTypeException("Unknown action symbol " + top);
         }
      } else {
         this.out.writeEndObject();
      }

      return null;
   }
}
