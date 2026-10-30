package io.karatelabs.js;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JsonSpan(JsonSpan.Kind kind, int start, int end, List<JsonSpan.Member> members, List<JsonSpan> elements, Object value) {
   public String text(String source) {
      return source.substring(this.start, this.end);
   }

   public Object toValue() {
      switch (this.kind) {
         case OBJECT:
            Map<String, Object> map = new LinkedHashMap<>();

            for (JsonSpan.Member m : this.members) {
               map.put(m.key, m.value.toValue());
            }

            return map;
         case ARRAY:
            List<Object> list = new ArrayList<>(this.elements.size());

            for (JsonSpan e : this.elements) {
               list.add(e.toValue());
            }

            return list;
         default:
            return this.value;
      }
   }

   public record Doc(boolean bom, JsonSpan root) {
   }

   public static enum Kind {
      OBJECT,
      ARRAY,
      STRING,
      NUMBER,
      TRUE,
      FALSE,
      NULL;
   }

   public record Member(String key, int keyStart, int keyEnd, JsonSpan value) {
   }
}
