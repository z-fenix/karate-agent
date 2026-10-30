package io.karatelabs.ext.kafka.coverage;

import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.junit.Test;
import static org.junit.Assert.*;

public class AvroTypeModelTest {
   @Test
   public void kafkaDispatchExposesCallableCommands() {
      io.karatelabs.ext.kafka.KafkaChannel channel = new io.karatelabs.ext.kafka.KafkaChannel(Map.of());
      assertTrue(channel.jsGet("consumer") instanceof io.karatelabs.js.JavaInvokable);
      assertNull(channel.jsGet("unknown"));
   }

   @Test
   public void malformedKafkaConfigurationIsRejected() {
      assertThrows(IllegalArgumentException.class, () -> KafkaContractLoader.fromConfig(List.of("not-an-object")));
   }

   @Test
   public void samplesEveryPrimitiveAndNullableUnion() {
      Schema schema = new Schema.Parser().parse("""
         {"type":"record","name":"Entry","fields":[
           {"name":"nullable","type":["null","string"]},
           {"name":"integer","type":"int"},
           {"name":"longValue","type":"long"},
           {"name":"decimal","type":"double"},
           {"name":"text","type":"string"},
           {"name":"bytes","type":"bytes"},
           {"name":"flag","type":"boolean"},
           {"name":"items","type":{"type":"array","items":"string"}},
           {"name":"properties","type":{"type":"map","values":"string"}},
           {"name":"choice","type":{"type":"enum","name":"Choice","symbols":["FIRST","SECOND"]}}
         ]}
         """);
      Map<?, ?> sample = (Map<?, ?>)new AvroTypeModel(schema).sample();
      assertTrue(sample.containsKey("nullable"));
      assertNull(sample.get("nullable"));
      assertEquals(0, sample.get("integer"));
      assertEquals(0, sample.get("longValue"));
      assertEquals(0.0, sample.get("decimal"));
      assertEquals("", sample.get("text"));
      assertEquals("", sample.get("bytes"));
      assertEquals(false, sample.get("flag"));
      assertEquals(List.of(), sample.get("items"));
      assertEquals(Map.of(), sample.get("properties"));
      assertEquals("FIRST", sample.get("choice"));
   }

   @Test
   public void recursiveNullableFieldsTerminateWithNull() {
      Schema schema = new Schema.Parser().parse("""
         {"type":"record","name":"Node","fields":[{"name":"next","type":["null","Node"]}]}
         """);
      Map<?, ?> sample = (Map<?, ?>)new AvroTypeModel(schema).sample();
      assertTrue(sample.containsKey("next"));
      assertNull(sample.get("next"));
   }
}
