package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.TypeModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

public class AvroTypeModel implements TypeModel {
   public static final String SOURCE = "avro";
   private static final int MAX_DEPTH = 6;
   private final Schema schema;

   public AvroTypeModel(Schema schema) {
      this.schema = schema;
   }

   public String kind() {
      return "avro";
   }

   public Map<String, Object> partitions(String refDate) {
      Map<String, Object> out = new LinkedHashMap<>();
      Schema record = unwrap(this.schema);
      if (record.getType() == Type.RECORD) {
         this.walk(record, "", out, new LinkedHashSet<>(), 0);
      }

      return out;
   }

   private void walk(Schema record, String prefix, Map<String, Object> out, Set<String> visiting, int depth) {
      if (depth <= 6 && visiting.add(record.getFullName())) {
         for (Field field : record.getFields()) {
            String path = prefix.isEmpty() ? field.name() : prefix + "." + field.name();
            Schema fs = unwrap(field.schema());
            switch (fs.getType()) {
               case ENUM:
                  out.put(path, enumPartition(fs));
                  break;
               case BOOLEAN:
                  out.put(path, booleanPartition());
                  break;
               case RECORD:
                  this.walk(fs, path, out, visiting, depth + 1);
            }
         }

         visiting.remove(record.getFullName());
      }
   }

   private static Schema unwrap(Schema s) {
      if (s.getType() == Type.UNION) {
         for (Schema branch : s.getTypes()) {
            if (branch.getType() != Type.NULL) {
               return branch;
            }
         }
      }

      return s;
   }

   private static Map<String, Object> enumPartition(Schema enumSchema) {
      List<String> classes = new ArrayList<>(enumSchema.getEnumSymbols());
      List<Object> samples = new ArrayList<>(classes);
      return partition("enum", classes, samples);
   }

   private static Map<String, Object> booleanPartition() {
      List<String> classes = new ArrayList<>(List.of("true", "false"));
      List<Object> samples = new ArrayList<>(List.of(true, false));
      return partition("boolean", classes, samples);
   }

   private static Map<String, Object> partition(String kind, List<String> classes, List<Object> samples) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("kind", kind);
      m.put("classes", classes);
      m.put("samples", samples);
      m.put("source", "avro");
      return m;
   }

   public Object sample() {
      return sampleOf(this.schema, new LinkedHashSet<>(), 0);
   }

   private static Object sampleOf(Schema s, Set<String> visiting, int depth) {
      if (s.getType() == Type.UNION) {
         for (Schema member : s.getTypes()) {
            if (member.getType() == Type.NULL) {
               return null;
            }
         }
         return sampleOf(s.getTypes().get(0), visiting, depth);
      }
      return switch (s.getType()) {
         case RECORD -> {
            Map<String, Object> out = new LinkedHashMap<>();
            if (depth <= MAX_DEPTH && visiting.add(s.getFullName())) {
               for (Field field : s.getFields()) {
                  out.put(field.name(), sampleOf(field.schema(), visiting, depth + 1));
               }
               visiting.remove(s.getFullName());
            }
            yield out;
         }
         case ENUM -> s.getEnumSymbols().isEmpty() ? null : s.getEnumSymbols().get(0);
         case BOOLEAN -> false;
         case INT, LONG -> 0;
         case FLOAT, DOUBLE -> 0.0;
         case STRING, BYTES, FIXED -> "";
         case ARRAY -> new ArrayList<>();
         case MAP -> new LinkedHashMap<>();
         case UNION, NULL -> null;
      };
   }

   public Map<String, Object> toMap() {
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("kind", this.kind());
      Schema record = unwrap(this.schema);
      out.put("name", record.getFullName());
      if (record.getType() == Type.RECORD) {
         List<Map<String, Object>> fields = new ArrayList<>();

         for (Field f : record.getFields()) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("name", f.name());
            fm.put("type", unwrap(f.schema()).getType().getName());
            fields.add(fm);
         }

         out.put("fields", fields);
      }

      return out;
   }
}
