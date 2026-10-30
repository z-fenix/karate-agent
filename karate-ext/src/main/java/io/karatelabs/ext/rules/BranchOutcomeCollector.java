package io.karatelabs.ext.rules;

import io.karatelabs.js.ContextListener;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class BranchOutcomeCollector implements ContextListener {
   private final Map<Integer, Set<Boolean>> a = new LinkedHashMap<>();

   @Override
   public void onEvent(Event var1) {
      if (var1.type == EventType.BRANCH && var1.value instanceof Boolean var2) {
         this.a.computeIfAbsent(var1.node.getFirstToken().pos, var0 -> new TreeSet<>()).add(var2);
      }
   }

   Map<String, Set<Boolean>> a(Map<Integer, String> var1) {
      LinkedHashMap<java.lang.String, java.util.Set<java.lang.Boolean>> var2 = new LinkedHashMap();

      for (Entry var4 : this.a.entrySet()) {
         String var5 = (String)var1.get(var4.getKey());
         if (var5 != null) {
            var2.put(var5, (Set)var4.getValue());
         }
      }

      return var2;
   }

   Map<String, Object> b(Map<Integer, String> var1) {
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var2 = new LinkedHashMap();
      this.a(var1).forEach((var1x, var2x) -> var2.put(var1x, var2x.stream().map(String::valueOf).toList()));
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("sites", var2);
      return var3;
   }
}
