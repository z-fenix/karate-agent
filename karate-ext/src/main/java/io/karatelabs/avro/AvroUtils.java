package io.karatelabs.avro;

import io.karatelabs.common.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

public class AvroUtils {
   public static Schema toSchema(String path) {
      Resource resource = Resource.path(path);
      String text = resource.getText();
      Parser parser = new Parser();
      return parser.parse(text);
   }

   public static String toJson(Schema schema, byte[] bytes) {
      DatumReader<GenericRecord> datumReader = new GenericDatumReader(schema);
      ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
      BinaryDecoder binaryDecoder = new DecoderFactory().binaryDecoder(stream, null);

      try {
         GenericRecord gr = (GenericRecord)datumReader.read(null, binaryDecoder);
         return toJson(gr);
      } catch (Exception var6) {
         throw new RuntimeException(var6);
      }
   }

   public static String toJson(GenericRecord gr) {
      try {
         String var4;
         try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<GenericRecord> writer = new GenericDatumWriter(gr.getSchema());
            io.karatelabs.avro.AvroJsonEncoder encoder = new io.karatelabs.avro.AvroJsonEncoder(gr.getSchema(), outputStream);
            writer.write(gr, encoder);
            encoder.flush();
            var4 = outputStream.toString(StandardCharsets.UTF_8);
         }

         return var4;
      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }

   public static byte[] toBytes(GenericRecord gr) {
      try {
         byte[] var4;
         try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<GenericRecord> writer = new GenericDatumWriter(gr.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(gr, encoder);
            encoder.flush();
            var4 = outputStream.toByteArray();
         }

         return var4;
      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }

   public static GenericRecord fromJson(Schema schema, String json) {
      try {
         DatumReader reader = new GenericDatumReader(schema);
         io.karatelabs.avro.AvroJsonDecoder decoder = new io.karatelabs.avro.AvroJsonDecoder(schema, json);
         return (GenericRecord)reader.read(null, decoder);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }
}
