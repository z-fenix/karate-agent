package io.karatelabs.ext.grpc.coverage;

import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ProtovalidateConstraints {
   private static final String FIELD_OPTION = "buf.validate.field";
   private static final Set<String> NUMERIC_TYPES = Set.of(
      "int32", "int64", "uint32", "uint64", "sint32", "sint64", "fixed32", "fixed64", "sfixed32", "sfixed64", "float", "double"
   );

   private ProtovalidateConstraints() {
   }

   public static Map<String, Map<String, Object>> from(ProtoFileElement file) {
      Map<String, Map<String, Object>> out = new LinkedHashMap<>();
      if (file != null) {
         for (TypeElement t : file.getTypes()) {
            walk(t, out);
         }
      }

      return out;
   }

   private static void walk(TypeElement type, Map<String, Map<String, Object>> out) {
      if (type instanceof MessageElement msg) {
         for (FieldElement field : msg.getFields()) {
            Map<String, Object> fc = fieldConstraints(field);
            if (fc != null) {
               out.put(msg.getName() + "." + field.getName(), fc);
            }
         }

         for (TypeElement nested : msg.getNestedTypes()) {
            walk(nested, out);
         }
      }
   }

   private static Map<String, Object> fieldConstraints(FieldElement field) {
      List<Double> bounds = new ArrayList<>();
      List<Double> itemBounds = new ArrayList<>();
      Integer[] lens = new Integer[2];
      String[] pattern = new String[1];

      for (OptionElement opt : field.getOptions()) {
         if ("buf.validate.field".equals(opt.getName()) && opt.getValue() instanceof OptionElement typeRule) {
            String type = typeRule.getName();

            for (Entry<String, Object> rule : rules(typeRule.getValue()).entrySet()) {
               applyRule(type, rule.getKey(), rule.getValue(), bounds, itemBounds, lens, pattern);
            }
         }
      }

      if (bounds.isEmpty() && itemBounds.isEmpty() && lens[0] == null && lens[1] == null && pattern[0] == null) {
         return null;
      } else {
         Map<String, Object> fc = new LinkedHashMap<>();
         if (!bounds.isEmpty()) {
            fc.put("numericBounds", bounds);
         }

         if (!itemBounds.isEmpty()) {
            fc.put("itemBounds", itemBounds);
         }

         if (lens[0] != null) {
            fc.put("minLen", lens[0]);
         }

         if (lens[1] != null) {
            fc.put("maxLen", lens[1]);
         }

         if (pattern[0] != null) {
            fc.put("pattern", pattern[0]);
         }

         return fc;
      }
   }

   private static Map<String, Object> rules(Object typeRuleValue) {
      Map<String, Object> out = new LinkedHashMap<>();
      if (typeRuleValue instanceof OptionElement one) {
         out.put(one.getName(), one.getValue());
      } else if (typeRuleValue instanceof Map<?, ?> map) {
         for (Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
         }
      } else if (typeRuleValue instanceof List) {
         for (Object item : (List)typeRuleValue) {
            if (item instanceof OptionElement oe) {
               out.put(oe.getName(), oe.getValue());
            }
         }
      }

      return out;
   }

   private static void applyRule(String type, String rule, Object value, List<Double> bounds, List<Double> itemBounds, Integer[] lens, String[] pattern) {
      Object raw = value instanceof OptionElement oe ? oe.getValue() : value;
      if (NUMERIC_TYPES.contains(type)) {
         switch (rule) {
            case "gte":
            case "gt":
            case "lte":
            case "lt":
            case "const":
               Double d = toDouble(raw);
               if (d != null) {
                  bounds.add(d);
               }
         }
      } else if ("repeated".equals(type) || "map".equals(type)) {
         switch (rule) {
            case "min_items":
            case "max_items":
            case "min_pairs":
            case "max_pairs":
               Double d = toDouble(raw);
               if (d != null) {
                  itemBounds.add(d);
               }
         }
      } else if ("string".equals(type)) {
         switch (rule) {
            case "min_len":
            case "min_bytes":
               lens[0] = toInt(raw);
               break;
            case "max_len":
            case "max_bytes":
               lens[1] = toInt(raw);
               break;
            case "len":
            case "len_bytes":
               Integer n = toInt(raw);
               lens[0] = n;
               lens[1] = n;
               break;
            case "pattern":
               pattern[0] = toStr(raw);
         }
      }
   }

   private static Double toDouble(Object v) {
      try {
         return Double.parseDouble(unquote(String.valueOf(v)));
      } catch (RuntimeException var2) {
         return null;
      }
   }

   private static Integer toInt(Object v) {
      Double d = toDouble(v);
      return d == null ? null : (int)Math.round(d);
   }

   private static String toStr(Object v) {
      return v == null ? null : unquote(String.valueOf(v));
   }

   private static String unquote(String s) {
      return s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0) ? s.substring(1, s.length() - 1) : s;
   }
}
