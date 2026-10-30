package io.karatelabs.ext.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface MapConvertible {
   Map<String, Object> toMap();

   static List<Map<String, Object>> a(Collection<? extends MapConvertible> var0) {
      return var0.stream().map(MapConvertible::toMap).collect(Collectors.toList());
   }
}
