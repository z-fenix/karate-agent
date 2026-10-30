package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class ObservationReconciler {
   public static final String a = "OBSERVE_IMPURE";
   private static final Map<String, Object> b = Map.of("absent", true);
   private static final String c = "!threw";
   private static final String d = "readBack";
   private static final Pattern e = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
   private static final Pattern f = Pattern.compile("\\d+");
   private static final Pattern g = Pattern.compile("[0-9a-fA-F]{4}");
   private static final Comparator<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference> h = Comparator.<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference, String>comparing(io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference::a, io.karatelabs.ext.rules.twin.CanonicalJson::a)
      .thenComparingInt(io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference::b)
      .thenComparingInt(io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference::c);
   private static final io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue i = new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue("\u0000absent", b);

   private ObservationReconciler() {
   }

   public static Map<String, Object> a(String var0, Map<String, Map<String, Object>> var1) {
      TwinModel var2;
      try {
         var2 = TwinModelLoader.a(var0);
      } catch (RuntimeException var9) {
         return null;
      }

      io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel var3 = var2.i();
      if (var3 != null && var1 != null && !var1.isEmpty()) {
         var var4 = a(var1);
         if (var4.isEmpty()) {
            return null;
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();

            for (Entry var7 : var3.g().entrySet()) {
               var5.put(c((String)var7.getKey()), (String)var7.getValue());
            }

            LinkedHashSet<java.lang.String> var10 = new LinkedHashSet();

            for (String var8 : var3.f()) {
               var10.add(c(var8));
            }

            io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeResult var12 = a(var2, var4, var5);
            if (var12 == null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
               var14.put("code", "OBSERVE_IMPURE");
               return var14;
            } else {
               var var13 = a(var4, var3.e());
               return var13.isEmpty() ? null : a(var13, var12, var5, var10);
            }
         }
      } else {
         return null;
      }
   }

   static Set<String> b(String var0, Map<String, Map<String, Object>> var1) {
      TwinModel var2;
      try {
         var2 = TwinModelLoader.a(var0);
      } catch (RuntimeException var10) {
         return null;
      }

      io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel var3 = var2.i();
      if (var3 != null && var1 != null && !var1.isEmpty()) {
         var var4 = a(var1);
         if (var4.isEmpty()) {
            return null;
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();

            for (Entry var7 : var3.g().entrySet()) {
               var5.put(c((String)var7.getKey()), (String)var7.getValue());
            }

            io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeResult var11 = a(var2, var4, var5);
            if (var11 == null) {
               return null;
            } else {
               TreeSet<java.lang.String> var12 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

               for (List var9 : var11.a()) {
                  var12.addAll(var9);
               }

               return var12;
            }
         }
      } else {
         return null;
      }
   }

   static boolean a(Set<String> var0, String var1) {
      return a(var1, var0, true);
   }

   private static List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> a(Map<String, Map<String, Object>> var0) {
      ArrayList<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> var1 = new ArrayList();

      for (Entry var3 : var0.entrySet()) {
         if (((Map)var3.getValue()).get("log") instanceof List var4) {
            for (int var12 = 0; var12 < var4.size(); var12++) {
               if (var4.get(var12) instanceof Map var6 && var6.get("observed") instanceof Map var7 && var7.get("response") instanceof Map var8) {
                  int var15 = var6.get("at") instanceof Number var10 ? var10.intValue() : 0;
                  var1.add(
                     new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry((String)var3.getKey(), var15, String.valueOf(var6.get("kind")), var6.get("command") instanceof String var16 ? var16 : null, var8)
                  );
               }
            }
         }
      }

      return var1;
   }

   private static io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeResult a(TwinModel var0, List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> var1, Map<String, String> var2) {
      ArrayList var3 = null;
      ArrayList var4 = null;
      TreeSet<java.lang.String> var5 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (int var6 = 0; var6 < 3; var6++) {
         ArrayList var7 = new ArrayList(var1.size());
         ArrayList var8 = new ArrayList(var1.size());

         for (int var9 = 0; var9 < var1.size(); var9++) {
            var7.add(null);
            var8.add(null);
         }

         for (int var21 = 0; var21 < var1.size(); var21++) {
            int var10 = var6 == 2 ? var1.size() - 1 - var21 : var21;
            io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry var11 = (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry)var1.get(var10);
            JavaCallable var12 = a(var0, var11);
            io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder var13 = new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder(var2);
            String var14 = "!threw";
            if (var12 != null) {
               try {
                  Object var15 = var12.call(null, new io.karatelabs.ext.rules.twin.ObservationReconciler.RecordingMap(var11.e(), "", var13));
                  Object var16 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var15);
                  var14 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var16);
                  if (var6 == 0 && var16 instanceof Map var17) {
                     for (Object var19 : var17.keySet()) {
                        var5.add(String.valueOf(var19));
                     }
                  }
               } catch (RuntimeException var20) {
                  var14 = "!threw";
               }
            }

            var7.set(var10, var13.a());
            var8.set(var10, var14);
         }

         if (var3 == null) {
            var3 = var7;
            var4 = var8;
         } else if (!var3.equals(var7) || !var4.equals(var8)) {
            return null;
         }
      }

      return new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeResult(var3, new ArrayList<>(var5));
   }

   private static JavaCallable a(TwinModel var0, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry var1) {
      if ("readBack".equals(var1.c())) {
         return var0.i().b();
      } else {
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2 = var1.d() == null ? null : var0.f().get(var1.d());
         return var2 == null ? null : var2.l();
      }
   }

   private static List<io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken> b(String var0) {
      if (var0 == null) {
         return null;
      } else {
         ArrayList<io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken> var1 = new ArrayList();
         int var2 = 0;

         while (var2 < var0.length()) {
            if (var0.charAt(var2) == '[') {
               int var5;
               if (var2 + 1 < var0.length() && var0.charAt(var2 + 1) == '"') {
                  StringBuilder var6 = new StringBuilder();
                  var5 = a(var0, var2 + 2, var6);
                  if (var5 < 0) {
                     return null;
                  }

                  var1.add(new io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken(var6.toString(), -1));
               } else {
                  var5 = var0.indexOf(93, var2);
                  if (var5 < 0 || !f.matcher(var0.substring(var2 + 1, var5)).matches()) {
                     return null;
                  }

                  var1.add(new io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken(null, Integer.parseInt(var0.substring(var2 + 1, var5))));
               }

               var2 = var5 + 1;
            } else {
               if (!var1.isEmpty()) {
                  if (var0.charAt(var2) != '.') {
                     return null;
                  }

                  var2++;
               }

               int var3 = var2;

               while (var2 < var0.length() && var0.charAt(var2) != '.' && var0.charAt(var2) != '[') {
                  var2++;
               }

               String var4 = var0.substring(var3, var2);
               if (!e.matcher(var4).matches()) {
                  return null;
               }

               var1.add(new io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken(var4, -1));
            }
         }

         return var1.isEmpty() ? null : var1;
      }
   }

   static boolean a(String var0) {
      var var1 = b(var0);
      if (var1 != null && !((io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken)var1.getFirst()).a()) {
         String var2 = ((io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken)var1.getFirst()).b();
         return "body".equals(var2) || "status".equals(var2);
      } else {
         return false;
      }
   }

   private static int a(String var0, int var1, StringBuilder var2) {
      while (var1 < var0.length()) {
         char var3 = var0.charAt(var1++);
         if (var3 == '"') {
            return var1 < var0.length() && var0.charAt(var1) == 93 ? var1 : -1;
         }

         if (var3 != '\\') {
            var2.append(var3);
         } else {
            if (var1 >= var0.length()) {
               return -1;
            }

            char var4 = var0.charAt(var1++);
            switch (var4) {
               case '"':
               case '/':
               case '\\':
                  var2.append(var4);
                  break;
               case 'b':
                  var2.append('\b');
                  break;
               case 'f':
                  var2.append('\f');
                  break;
               case 'n':
                  var2.append('\n');
                  break;
               case 'r':
                  var2.append('\r');
                  break;
               case 't':
                  var2.append('\t');
                  break;
               case 'u':
                  if (var1 + 4 <= var0.length() && g.matcher(var0.substring(var1, var1 + 4)).matches()) {
                     var2.append((char)Integer.parseInt(var0.substring(var1, var1 + 4), 16));
                     var1 += 4;
                     break;
                  }

                  return -1;
               default:
                  return -1;
            }
         }
      }

      return -1;
   }

   private static String c(String var0) {
      var var1 = b(var0);
      if (var1 == null) {
         return var0;
      } else {
         String var2 = "";

         for (io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken var4 : (Iterable<io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken>) (Iterable<?>) (var1)) {
            var2 = var4.a() ? var2 + "[" + var4.c() + "]" : a(var2, var4.b());
         }

         return var2;
      }
   }

   static String a(String var0, String var1) {
      if (e.matcher(var1).matches()) {
         return var0.isEmpty() ? var1 : var0 + "." + var1;
      } else {
         return var0 + "[" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var1) + "]";
      }
   }

   private static String a(String var0, int var1, Object var2, Map<String, String> var3) {
      String var4 = (String)var3.get(var0);
      return var4 != null && var2 instanceof Map var5 && var5.containsKey(var4)
         ? var0 + "[" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var5.get(var4)) + "]"
         : var0 + "[" + var1 + "]";
   }

   private static String b(String var0, String var1) {
      return var0.isEmpty() ? var1 : var0 + "." + var1;
   }

   private static Set<String> b(Map<?, ?> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (Object var3 : var0.keySet()) {
         var1.add(String.valueOf(var3));
      }

      return var1;
   }

   private static List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack> a(List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> var0, String var1) {
      var var2 = b(var1);
      String var3 = c(var1);
      LinkedHashMap<java.lang.String, java.util.ArrayList> var4 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.util.ArrayList> var5 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry var7 : var0) {
         if ("readBack".equals(var7.c())) {
            var4.computeIfAbsent(var7.a(), var0x -> new ArrayList<>()).add(var7);
         }

         if (var2 != null) {
            Object var8 = a(var7.e(), var2);
            if (var8 != null) {
               var5.computeIfAbsent(var7.a() + "\u0000" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var8), var0x -> new ArrayList<>()).add(var7);
            }
         }
      }

      ArrayList<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack> var9 = new ArrayList();

      for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         if (((List)var12.getValue()).size() > 1) {
            var9.add(new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack("readBack", (String)var12.getKey(), (List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry>)var12.getValue()));
         }
      }

      for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
         if (((List)var13.getValue()).size() > 1) {
            var9.add(new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack("entity:" + var3, ((io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry)((List)var13.getValue()).get(0)).a(), (List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry>)var13.getValue()));
         }
      }

      return var9;
   }

   private static Object a(Object var0, List<io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken> var1) {
      Object var2 = var0;

      for (io.karatelabs.ext.rules.twin.ObservationReconciler.PathToken var4 : var1) {
         if (var4.a()) {
            if (!(var2 instanceof List var6 && var4.c() < var6.size())) {
               return null;
            }

            var2 = var6.get(var4.c());
         } else {
            if (!(var2 instanceof Map var5 && var5.containsKey(var4.b()))) {
               return null;
            }

            var2 = var5.get(var4.b());
         }
      }

      return var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2) ? var2 : null;
   }

   private static Map<String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue> a(Map<String, Object> var0, Map<String, String> var1) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue> var2 = new LinkedHashMap();
      a(var0.get("body"), "body", var2, var1);
      return var2;
   }

   private static void a(Object var0, String var1, Map<String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue> var2, Map<String, String> var3) {
      if (var0 instanceof Map var4) {
         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            a(var7.getValue(), a(var1, String.valueOf(var7.getKey())), var2, var3);
         }
      } else if (var0 instanceof List var5 && var3.get(var1) != null) {
         for (int var8 = 0; var8 < var5.size(); var8++) {
            a(var5.get(var8), a(var1, var8, var5.get(var8), var3), var2, var3);
         }
      } else {
         var2.put(var1, new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue(io.karatelabs.ext.rules.twin.CanonicalJson.c(var0), io.karatelabs.ext.rules.twin.CanonicalJson.d(var0)));
      }
   }

   private static Map<String, Object> a(List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack> var0, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeResult var1, Map<String, String> var2, Set<String> var3) {
      TreeSet var4 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (List var6 : var1.a()) {
         var4.addAll(var6);
      }

      TreeMap<String, Map<String, List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference>>> var17 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      TreeSet var18 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      LinkedHashSet<java.lang.String> var7 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationTrack var9 : var0) {
         ArrayList<java.util.Map<java.lang.String,io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue>> var10 = new ArrayList(var9.c().size());
         TreeSet var11 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

         for (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry var13 : var9.c()) {
            var var14 = a(var13.e(), var2);
            var10.add(var14);
            var11.addAll(var14.keySet());
         }

         var18.addAll(var11);

         for (String var29 : (Iterable<String>) (Iterable<?>) (var11)) {
            for (Map var15 : (Iterable<Map>) (Iterable<?>) (var10)) {
               if (!var15.containsKey(var29)) {
                  var7.add(var29);
               }
            }

            for (int var33 = 1; var33 < var10.size(); var33++) {
               io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue var36 = a((Map<String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue>)var10.get(var33 - 1), var29);
               io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue var16 = a((Map<String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue>)var10.get(var33), var29);
               if (!var36.a().equals(var16.a())) {
                  ((java.util.List)(((java.util.Map)(var17.computeIfAbsent(var29, var0x -> new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a))))
                     .computeIfAbsent(var9.a(), var0x -> new ArrayList<>())))
                     .add(new io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference(var9.b(), var9.c().get(var33 - 1).b(), var9.c().get(var33).b(), var36, var16));
               }
            }
         }
      }

      int var19 = 0;

      for (String var22 : (Iterable<String>) (Iterable<?>) (var18)) {
         var19 += a(var22, var4, var7.contains(var22)) ? 1 : 0;
      }

      ArrayList var21 = new ArrayList();
      ArrayList<Map<String, Object>> var23 = new ArrayList<>();
      LinkedHashSet<java.lang.String> var24 = new LinkedHashSet();

      for (Entry var30 : (Iterable<Entry>) (Iterable<?>) (var17.entrySet())) {
         boolean var34 = var3.contains(var30.getKey());
         if (var34 || !a((String)var30.getKey(), var4, var7.contains(var30.getKey()))) {
            for (Entry var38 : (Iterable<Entry>) (Iterable<?>) (((Map)var30.getValue()).entrySet())) {
               (var34 ? var23 : var21).add(a((String)var30.getKey(), (String)var38.getKey(), (List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference>)var38.getValue()));
               if (var34) {
                  var24.add((String)var30.getKey());
               }
            }
         }
      }

      for (String var31 : var3) {
         if (!var24.contains(var31)) {
            LinkedHashMap<java.lang.String, java.lang.Object> var35 = new LinkedHashMap();
            var35.put("path", var31);
            var35.put("matched", 0);
            var23.add(var35);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var28 = new LinkedHashMap();
      var28.put("fields", var18.size());
      var28.put("read", var19);
      var28.put("unread", var18.size() - var19);
      var28.put("volatile", var23);
      var28.put("observed", var1.b());
      var28.put("findings", var21);
      return var28;
   }

   private static Map<String, Object> a(String var0, String var1, List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference> var2) {
      TreeSet<java.lang.String> var3 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList(var2.size());

      for (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationDifference var6 : var2.stream().sorted(h).toList()) {
         var3.add(var6.a());
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("sequenceId", var6.a());
         var7.put("between", List.of(var6.b(), var6.c()));
         var7.put("from", var6.d().b());
         var7.put("to", var6.e().b());
         var4.add(var7);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("kind", "unobservedVariation");
      var8.put("field", var0);
      var8.put("group", var1);
      var8.put("sequences", new ArrayList(var3));
      var8.put("changes", var4);
      return var8;
   }

   private static io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue a(Map<String, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue> var0, String var1) {
      io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue var2 = (io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue)var0.get(var1);
      return var2 == null ? i : var2;
   }

   private static boolean a(String var0, Set<String> var1, boolean var2) {
      for (String var4 : var1) {
         if (var4.equals(var0) || c(var4, var0)) {
            return true;
         }

         if (var4.endsWith(".**") || var4.equals("**")) {
            String var5 = a(var4, 2);
            if (var5.isEmpty() || var5.equals(var0) || c(var0, var5)) {
               return true;
            }
         }

         if (var2 && (var4.endsWith(".*") || var4.equals("*")) && d(var0).equals(a(var4, 1))) {
            return true;
         }
      }

      return false;
   }

   private static String a(String var0, int var1) {
      int var2 = var0.length() - var1 - (var0.length() == var1 ? 0 : 1);
      return var0.substring(0, Math.max(var2, 0));
   }

   private static String d(String var0) {
      int var1 = var0.lastIndexOf(46);
      int var2 = var0.lastIndexOf(91);
      int var3 = Math.max(var1, var2);
      return var3 < 0 ? "" : var0.substring(0, var3);
   }

   private static boolean c(String var0, String var1) {
      return var1.isEmpty()
         ? !var0.isEmpty()
         : var0.length() > var1.length() && var0.startsWith(var1) && (var0.charAt(var1.length()) == '.' || var0.charAt(var1.length()) == '[');
   }

   private record ObservedLogEntry(String a, int b, String c, String d, Map<String, Object> e) {

      private ObservedLogEntry(String a, int b, String c, String d, Map<String, Object> e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   private record ObservationDifference(String a, int b, int c, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue d, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue e) {

      private ObservationDifference(String a, int b, int c, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue d, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationValue e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   private record ObservationTrack(String a, String b, List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> c) {

      private ObservationTrack(String a, String b, List<io.karatelabs.ext.rules.twin.ObservationReconciler.ObservedLogEntry> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record ObservationValue(String a, Object b) {

      private ObservationValue(String a, Object b) {
         this.a = a;
         this.b = b;
      }
   }

   private record ObservationProbeResult(List<List<String>> a, List<String> b) {

      private ObservationProbeResult(List<List<String>> a, List<String> b) {
         this.a = a;
         this.b = b;
      }
   }

   static final class ObservationProbeRecorder {
      private final Map<String, String> a;
      private final Set<String> b = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      ObservationProbeRecorder(Map<String, String> var1) {
         this.a = var1;
      }

      void a(String var1) {
         this.b.add(var1);
      }

      List<String> a() {
         return new ArrayList<>(this.b);
      }

      Object a(Object var1, String var2) {
         if (var1 instanceof Map var4) {
            return new io.karatelabs.ext.rules.twin.ObservationReconciler.RecordingMap(var4, var2, this);
         } else {
            return var1 instanceof List var3 ? new io.karatelabs.ext.rules.twin.ObservationReconciler.RecordingList(var3, var2, this) : var1;
         }
      }
   }

   static final class RecordingList extends AbstractList<Object> {
      private final List<?> a;
      private final String b;
      private final io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder c;

      RecordingList(List<?> var1, String var2, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
      }

      @Override
      public Object get(int var1) {
         Object var2 = this.a.get(var1);
         String var3 = io.karatelabs.ext.rules.twin.ObservationReconciler.a(this.b, var1, var2, this.c.a);
         this.c.a(var3);
         return this.c.a(var2, var3);
      }

      @Override
      public int size() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "*"));
         return this.a.size();
      }

      @Override
      public Iterator<Object> iterator() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "**"));
         return (Iterator<Object>)this.a.iterator();
      }
   }

   static final class RecordingMap extends AbstractMap<String, Object> {
      private final Map<?, ?> a;
      private final String b;
      private final io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder c;

      RecordingMap(Map<?, ?> var1, String var2, io.karatelabs.ext.rules.twin.ObservationReconciler.ObservationProbeRecorder var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
      }

      @Override
      public boolean containsKey(Object var1) {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.a(this.b, String.valueOf(var1)));
         return this.a.containsKey(var1);
      }

      @Override
      public Object get(Object var1) {
         String var2 = String.valueOf(var1);
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.a(this.b, var2));
         return this.c.a(this.a.get(var1), io.karatelabs.ext.rules.twin.ObservationReconciler.a(this.b, var2));
      }

      @Override
      public int size() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "*"));
         return this.a.size();
      }

      @Override
      public Set<String> keySet() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "*"));
         return io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.a);
      }

      @Override
      public Collection<Object> values() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "**"));
         return new ArrayList<>((Collection<? extends Object>)this.a.values());
      }

      @Override
      public Set<Entry<String, Object>> entrySet() {
         this.c.a(io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.b, "**"));
         LinkedHashSet<java.util.Map.Entry<java.lang.String,java.lang.Object>> var1 = new LinkedHashSet();

         for (Entry var3 : this.a.entrySet()) {
            var1.add(new SimpleImmutableEntry<>(String.valueOf(var3.getKey()), var3.getValue()));
         }

         return var1;
      }
   }

   private record PathToken(String b, int c) {

      private PathToken(String b, int c) {
         this.b = b;
         this.c = c;
      }

      boolean a() {
         return this.b == null;
      }

      public String b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }
   }
}
