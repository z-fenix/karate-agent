package io.karatelabs.ext.grpc.coverage;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.karatelabs.ext.contract.Partitions;
import io.karatelabs.ext.contract.TypeModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProtobufTypeModel implements TypeModel {
   public static final String SOURCE = "proto";
   private static final int MAX_DEPTH = 6;
   private final Descriptor descriptor;
   private final Map<String, Map<String, Object>> constraints;

   public ProtobufTypeModel(Descriptor descriptor) {
      this(descriptor, null);
   }

   public ProtobufTypeModel(Descriptor descriptor, Map<String, Map<String, Object>> constraints) {
      this.descriptor = descriptor;
      this.constraints = constraints == null ? Map.of() : constraints;
   }

   public String kind() {
      return "protobuf";
   }

   public Map<String, Object> partitions(String refDate) {
      Map<String, Object> out = new LinkedHashMap<>();
      this.walk(this.descriptor, "", out, new LinkedHashSet<>(), 0);
      return out;
   }

   private void walk(Descriptor message, String prefix, Map<String, Object> out, Set<String> visiting, int depth) {
      if (depth <= 6 && visiting.add(message.getFullName())) {
         for (FieldDescriptor fd : message.getFields()) {
            String path = prefix.isEmpty() ? fd.getName() : prefix + "." + fd.getName();
            if (!fd.isMapField() && !fd.isRepeated()) {
               switch (fd.getJavaType()) {
                  case ENUM:
                     out.put(path, enumPartition(fd));
                     break;
                  case BOOLEAN:
                     out.put(path, booleanPartition());
                     break;
                  case MESSAGE:
                     this.walk(fd.getMessageType(), path, out, visiting, depth + 1);
                     break;
                  default:
                     Map<String, Object> p = this.constrainedPrimitive(fd);
                     if (p != null) {
                        out.put(path, p);
                     }
               }
            } else {
               Map<String, Object> card = this.cardinalityAxis(fd);
               if (card != null) {
                  out.put(path, card);
               }
            }
         }

         visiting.remove(message.getFullName());
      }
   }

   private Map<String, Object> cardinalityAxis(FieldDescriptor fd) {
      Map<String, Object> c = this.constraints.get(fd.getContainingType().getName() + "." + fd.getName());
      if (c != null && c.get("itemBounds") instanceof List<?> b && !b.isEmpty()) {
         List<Double> bounds = new ArrayList<>();

         for (Object o : b) {
            bounds.add(((Number)o).doubleValue());
         }

         Map<String, Object> p = Partitions.bva("cardinality", bounds, true);
         if (p != null) {
            p.put("source", "proto");
         }

         return p;
      } else {
         return null;
      }
   }

   private Map<String, Object> constrainedPrimitive(FieldDescriptor fd) {
      Map<String, Object> c = this.constraints.get(fd.getContainingType().getName() + "." + fd.getName());
      if (c == null) {
         return null;
      } else {
         Map<String, Object> p = null;
         switch (fd.getJavaType()) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
               if (c.get("numericBounds") instanceof List<?> b && !b.isEmpty()) {
                  List<Double> bounds = new ArrayList<>();

                  for (Object o : b) {
                     bounds.add(((Number)o).doubleValue());
                  }

                  p = Partitions.bva("number", bounds, false);
               }
               break;
            case STRING:
               p = Partitions.string((Integer)c.get("minLen"), (Integer)c.get("maxLen"), null, (String)c.get("pattern"));
         }

         if (p != null) {
            p.put("source", "proto");
         }

         return p;
      }
   }

   private static Map<String, Object> enumPartition(FieldDescriptor fd) {
      List<String> classes = new ArrayList<>();
      List<Object> samples = new ArrayList<>();

      for (EnumValueDescriptor v : fd.getEnumType().getValues()) {
         if (v.getNumber() != 0) {
            classes.add(v.getName());
            samples.add(v.getName());
         }
      }

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
      m.put("source", "proto");
      return m;
   }

   public Object sample() {
      return this.sampleOf(this.descriptor, new LinkedHashSet<>(), 0);
   }

   private Object sampleOf(Descriptor message, Set<String> visiting, int depth) {
      Map<String, Object> out = new LinkedHashMap<>();
      if (depth <= 6 && visiting.add(message.getFullName())) {
         for (FieldDescriptor fd : message.getFields()) {
            if (fd.isMapField()) {
               out.put(fd.getName(), new LinkedHashMap());
            } else if (fd.isRepeated()) {
               out.put(fd.getName(), new ArrayList());
            } else {
               out.put(fd.getName(), this.scalarSample(fd, visiting, depth));
            }
         }

         visiting.remove(message.getFullName());
         return out;
      } else {
         return out;
      }
   }

   private static String firstMeaningfulEnum(FieldDescriptor fd) {
      for (EnumValueDescriptor v : fd.getEnumType().getValues()) {
         if (v.getNumber() != 0) {
            return v.getName();
         }
      }

      return ((EnumValueDescriptor)fd.getEnumType().getValues().get(0)).getName();
   }

   private Object scalarSample(FieldDescriptor fd, Set<String> visiting, int depth) {
      Map<String, Object> c = this.constraints.get(fd.getContainingType().getName() + "." + fd.getName());
      return switch (fd.getJavaType()) {
         case ENUM -> firstMeaningfulEnum(fd);
         case BOOLEAN -> false;
         case INT -> (int)Math.round(constrainedNumber(c));
         case LONG -> Math.round(constrainedNumber(c));
         case FLOAT, DOUBLE -> constrainedNumber(c);
         case STRING -> constrainedString(c);
         case BYTE_STRING -> "";
         case MESSAGE -> this.sampleOf(fd.getMessageType(), visiting, depth + 1);
      };
   }

   private static double constrainedNumber(Map<String, Object> c) {
      if (c != null && c.get("numericBounds") instanceof List<?> b && !b.isEmpty()) {
         double min = ((Number)b.get(0)).doubleValue();
         double max = ((Number)b.get(b.size() - 1)).doubleValue();
         return b.size() == 1 ? min : (min + max) / 2.0;
      } else {
         return 0.0;
      }
   }

   private static String constrainedString(Map<String, Object> c) {
      if (c != null && c.get("minLen") instanceof Integer minLen && minLen > 0) {
         int len = c.get("maxLen") instanceof Integer maxLen ? Math.min(minLen, maxLen) : minLen;
         return "x".repeat(len);
      } else {
         return "";
      }
   }

   public Map<String, Object> toMap() {
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("kind", this.kind());
      out.put("message", this.descriptor.getFullName());
      List<Map<String, Object>> fields = new ArrayList<>();

      for (FieldDescriptor fd : this.descriptor.getFields()) {
         Map<String, Object> f = new LinkedHashMap<>();
         f.put("name", fd.getName());
         f.put("type", fd.getJavaType().name().toLowerCase());
         if (fd.isRepeated() && !fd.isMapField()) {
            f.put("repeated", true);
         }

         fields.add(f);
      }

      out.put("fields", fields);
      return out;
   }
}
