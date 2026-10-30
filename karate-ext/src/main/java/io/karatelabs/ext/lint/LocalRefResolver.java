package io.karatelabs.ext.lint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class LocalRefResolver {
   static final int MAX_RESOLVED_NODES = 2000000;
   private final Object rootDocument;
   private final Map<String, Object> resolvedReferenceCache = new HashMap<>();
   private final Deque<String> activeReferences = new ArrayDeque<>();
   private final IdentityHashMap<Object, Boolean> activeContainers = new IdentityHashMap<>();
   private int visitedNodeCount;
   private int cycleFallbackCount;

   private LocalRefResolver(Object var1) {
      this.rootDocument = var1;
   }

   static Object resolve(Object var0) {
      return new LocalRefResolver(var0).resolveNode(var0);
   }

   private Object resolveNode(Object var1) {
      if (++this.visitedNodeCount > 2000000) {
         throw new LintException(
            "resolving $refs expanded past 2000000 nodes — the document has a ref fan-out too large to lint against a resolved view (declare `resolved: false`)"
         );
      } else if (var1 instanceof Map var7) {
         if (var7.get("$ref") instanceof String var9 && var9.startsWith("#")) {
            return this.resolveRefObject(var7, var9);
         } else {
            this.enterContainer(var1);
            LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();

            for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
               var10.put(String.valueOf(var6.getKey()), this.resolveNode(var6.getValue()));
            }

            this.activeContainers.remove(var1);
            return Collections.unmodifiableMap(var10);
         }
      } else if (!(var1 instanceof List var2)) {
         return var1;
      } else {
         this.enterContainer(var1);
         ArrayList<java.lang.Object> var3 = new ArrayList(var2.size());

         for (Object var5 : var2) {
            var3.add(this.resolveNode(var5));
         }

         this.activeContainers.remove(var1);
         return Collections.unmodifiableList(var3);
      }
   }

   private void enterContainer(Object var1) {
      if (this.activeContainers.put(var1, Boolean.TRUE) != null) {
         throw new LintException(
            "the document contains a recursive YAML anchor (a node that contains itself), which cannot be resolved into a view — remove the anchor cycle"
         );
      }
   }

   private Object resolveRefObject(Map<?, ?> var1, String var2) {
      if (this.activeReferences.contains(var2)) {
         this.cycleFallbackCount++;
         return this.copyUnresolvedMap(var1);
      } else {
         Object var3 = this.resolvePointer(var2);
         if (var3 == null) {
            return this.copyUnresolvedMap(var1);
         } else {
            Object var4 = this.resolvedReferenceCache.get(var2);
            if (var4 == null) {
               int var5 = this.cycleFallbackCount;
               this.activeReferences.push(var2);
               IdentityHashMap var6 = new IdentityHashMap<>(this.activeContainers);
               this.activeContainers.clear();

               try {
                  var4 = this.resolveNode(var3);
               } finally {
                  this.activeReferences.pop();
                  this.activeContainers.clear();
                  this.activeContainers.putAll(var6);
               }

               if (this.cycleFallbackCount == var5) {
                  this.resolvedReferenceCache.put(var2, var4);
               }
            }

            if (var1.size() == 1) {
               return var4;
            } else {
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               if (var4 instanceof Map var12) {
                  for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var12.entrySet())) {
                     var11.put(String.valueOf(var8.getKey()), var8.getValue());
                  }
               }

               for (Entry var14 : var1.entrySet()) {
                  if (!"$ref".equals(var14.getKey())) {
                     var11.put(String.valueOf(var14.getKey()), this.resolveNode(var14.getValue()));
                  }
               }

               return Collections.unmodifiableMap(var11);
            }
         }
      }
   }

   private Object resolvePointer(String var1) {
      String var2 = var1.substring(1);
      if (var2.isEmpty()) {
         return this.rootDocument;
      } else if (!var2.startsWith("/")) {
         return null;
      } else {
         Object var3 = this.rootDocument;

         for (String var7 : var2.substring(1).split("/", -1)) {
            String var8 = var7.replace("~1", "/").replace("~0", "~");
            if (var3 instanceof Map var10) {
               var3 = var10.get(var8);
            } else {
               if (!(var3 instanceof List var9)) {
                  return null;
               }

               try {
                  int var11 = Integer.parseInt(var8);
                  var3 = var11 >= 0 && var11 < var9.size() ? var9.get(var11) : null;
               } catch (NumberFormatException var12) {
                  return null;
               }
            }

            if (var3 == null) {
               return null;
            }
         }

         return var3;
      }
   }

   private Object copyUnresolvedMap(Map<?, ?> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : var1.entrySet()) {
         var2.put(
            String.valueOf(var4.getKey()), !(var4.getValue() instanceof Map) && !(var4.getValue() instanceof List) ? var4.getValue() : this.resolveNode(var4.getValue())
         );
      }

      return Collections.unmodifiableMap(var2);
   }
}
