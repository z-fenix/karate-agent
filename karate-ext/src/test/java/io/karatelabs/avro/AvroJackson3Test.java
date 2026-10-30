package io.karatelabs.avro;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.junit.Test;
import static org.junit.Assert.*;

public class AvroJackson3Test {
    private final Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Sample\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"count\",\"type\":\"int\"}]}");

    @Test
    public void roundTripsCompactAndPrettyRecords() throws Exception {
        GenericData.Record record = new GenericData.Record(schema);
        record.put("name", "中文\nquoted\"");
        record.put("count", 42);
        for (boolean pretty : new boolean[]{false, true}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AvroJsonEncoder encoder = new AvroJsonEncoder(schema, out, pretty);
            GenericDatumWriter<GenericData.Record> writer = new GenericDatumWriter<>(schema);
            writer.write(record, encoder);
            writer.write(record, encoder);
            encoder.flush();
            String json = out.toString(StandardCharsets.UTF_8);
            assertTrue(json.contains(System.lineSeparator()));
            AvroJsonDecoder decoder = new AvroJsonDecoder(schema, json);
            GenericDatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
            assertEquals(record, reader.read(null, decoder));
            assertEquals(record, reader.read(null, decoder));
        }
    }

    @Test
    public void readsFieldsOutOfOrderUsingTokenBuffer() throws Exception {
        AvroJsonDecoder decoder = new AvroJsonDecoder(schema, "{\"count\":7,\"name\":\"buffered\"}");
        GenericData.Record record = new GenericDatumReader<GenericData.Record>(schema).read(null, decoder);
        assertEquals("buffered", record.get("name").toString());
        assertEquals(7, record.get("count"));
    }
}
