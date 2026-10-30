package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.JavaCallable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class TwinReplayEngine {
   public static final int DEFAULT_PROFILE_CEILING = 8;
   public static final String PASS = "PASS";
   public static final String FAIL = "FAIL";
   public static final String UNRESOLVED = "UNRESOLVED";
   public static final String NOT_CHECKED = "NOTCHECKED";
   public static final String REFUSED = "REFUSED";
   public static final String INVALID = "INVALID";
   public static final String AMBIGUOUS_REQUEST = "AMBIGUOUS_REQUEST";
   public static final String PROFILE_OVERCAP = "PROFILE_OVERCAP";
   public static final String REASONLESS_REFUSAL = "REASONLESS_REFUSAL";
   public static final String RESET_HOOK_FAILED = "RESET_HOOK_FAILED";
   public static final String RESET_REFUSED = "RESET_REFUSED";
   public static final String CLOCK_INADMISSIBLE = "CLOCK_INADMISSIBLE";
   static final String KIND_MISMATCH_KEY = "kindMismatch";
   static final String REASON_MISMATCH_KEY = "reasonMismatch";
   static final String APPLIED = "applied";
   static final String UNCHANGED = "unchanged";
   static final String TRANSPORT_FAILURE = "transportFailure";
   private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("[A-Z][A-Z0-9_-]*");

   private TwinReplayEngine() {
   }

   public static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult replay(TwinModel var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver var1, List<Map<String, Object>> var2, int var3, RulebookRuntime var4) {
      ArrayList var5 = new ArrayList();
      var var6 = List.of(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(initialState(var0), List.of(), false));
      if (var0.j() != null) {
         var var7 = stepMarker(-1, "reset");

         Map var8;
         try {
            var8 = observe(var0.j(), ((io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate)var6.get(0)).state(), null);
         } catch (io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException var20) {
            c(var5, var7, var6);
            return a(var5, var6, b("INVALID", "reset request: " + var20.getMessage()));
         }

         Map var9 = var1.observe(var8);
         var7.put("request", var8);
         var7.put("observed", var9);
         c(var5, var7, var6);
         if (isTransportFailure(var9)) {
            return a(var5, var6, b("INVALID", "reset failed in transport"));
         }

         if (!(var9.get("response") instanceof Map var10) || !(var10.get("status") instanceof Number var11)) {
            return a(var5, var6, b("INVALID", "reset observation malformed: no status"));
         }

         if (var11.intValue() < 200 || var11.intValue() > 299) {
            var var38 = b("INVALID", "reset refused: status " + var11.intValue());
            var38.put("code", "RESET_REFUSED");
            return a(var5, var6, var38);
         }
      }

      if (var0.i() != null) {
         var var23 = stepMarker(-1, "readBack");

         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult var27;
         try {
            var27 = a(var0, var6, var23, var1);
         } catch (io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException var19) {
            c(var5, var23, var6);
            return a(var5, var6, b("INVALID", "read-back request: " + var19.getMessage()));
         }

         c(var5, var23, var27 != null && !var27.candidates().isEmpty() ? var27.candidates() : var6);
         if (var27 == null || var27.candidates().isEmpty() || var27.transportFailed()) {
            return a(
               var5,
               var6,
               b("INVALID", var27 != null && var27.observeFailed() ? "start state not reproduced: the read-back's observe failed" : "start state not reproduced")
            );
         }

         var6 = var27.candidates();
      }

      for (int var24 = 0; var24 < var2.size(); var24++) {
         Map var28 = (Map)var2.get(var24);
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var30 = var0.f().get(String.valueOf(var28.get("command")));
         Map var33 = c(var28);
         var var35 = stepMarker(var24, "command");
         var35.put("command", var30.a());
         var35.put("args", var33);
         List var37 = a(var0, var6, var33);
         if (var37.isEmpty()) {
            var35.put("profileBefore", unresolvedCaptureUnion(var6));
            c(var5, var35, var6);
            var var39 = b("INVALID", TwinWalker.b(var0, ((io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate)var6.get(0)).state(), var33));
            var39.put("code", "CLOCK_INADMISSIBLE");
            var39.put("at", var24);
            var39.put("command", var30.a());
            return a(var5, var6, var39);
         }

         var6 = var37;
         var35.put("profileBefore", unresolvedCaptureUnion(var37));
         TreeMap var13 = new TreeMap<String, Object>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

         try {
            for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var15 : (Iterable<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate>) (Iterable<?>) (var6)) {
               Map var16 = observe(var30.k(), var15.state(), var33);
               var13.put(io.karatelabs.ext.rules.twin.CanonicalJson.c(var16), var16);
            }
         } catch (io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException var21) {
            c(var5, var35, var37);
            return a(var5, var37, b("INVALID", "request on " + var30.a() + ": " + var21.getMessage()));
         }

         if (var13.size() != 1) {
            var35.put("refused", Map.of("code", "AMBIGUOUS_REQUEST", "requests", new ArrayList(var13.keySet())));
            c(var5, var35, var37);
            return a(var5, var37, a("AMBIGUOUS_REQUEST", var24));
         }

         Map var40 = (Map)var13.firstEntry().getValue();
         Map var41 = var1.observe(var40);
         var35.put("request", var40);
         var35.put("observed", var41);
         if (isTransportFailure(var41) && var30.p().isEmpty()) {
            c(var5, var35, var37);
            return a(var5, var37, b("INVALID", "transport failure on " + var30.a() + ", which declares no indefinite class"));
         }

         io.karatelabs.ext.rules.twin.TwinReplayEngine.TransitionResult var42 = a(var0, var37, var30, var33, var41, var4);
         var35.put("outcome", var42.kind());
         var35.put("candidates", var42.candidates());
         var35.put("properties", var42.properties());
         if (var42.mismatch() != null) {
            c(var5, var35, var37);
            var var17 = b("FAIL", null);
            var17.put("mismatch", var42.mismatch());
            var17.put("at", var24);
            var17.put("evidence", var35.get("evidence"));
            return a(var5, var37, var17);
         }

         var6 = var42.nextStates();
         if (var6.size() > var3) {
            var35.put("refused", Map.of("code", "PROFILE_OVERCAP", "size", var6.size(), "ceiling", var3));
         }

         c(var5, var35, var6);
         if (var35.containsKey("refused")) {
            return a(var5, var6, a("PROFILE_OVERCAP", var24));
         }
      }

      if (var0.i() != null) {
         var var25 = stepMarker(var2.size(), "readBack");

         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult var29;
         try {
            var29 = a(var0, var6, var25, var1);
         } catch (io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException var18) {
            c(var5, var25, var6);
            return a(var5, var6, b("INVALID", "read-back request: " + var18.getMessage()));
         }

         if (var29 == null) {
            c(var5, var25, var6);
            return a(var5, var6, a("AMBIGUOUS_REQUEST", var2.size()));
         }

         if (var29.transportFailed()) {
            c(var5, var25, var6);
         } else {
            if (var29.observeFailed()) {
               c(var5, var25, var6);
               var var32 = b("NOTCHECKED", "the terminal read-back's observe failed");
               var32.put("at", var2.size());
               return a(var5, var6, var32);
            }

            if (var29.candidates().isEmpty()) {
               c(var5, var25, var6);
               Map var31;
               if (var29.allNotChecked()) {
                  var31 = b("NOTCHECKED", "the only candidates were kept by a model error and the read-back contradicts them");
               } else {
                  var31 = b("FAIL", null);
                  LinkedHashMap var34 = new LinkedHashMap();
                  var34.put("kind", "readBackExplainsNoCandidate");
                  if (var29.missingCapture() != null) {
                     var34.put("missing", var29.missingCapture());
                  }

                  if (!var29.differences().isEmpty()) {
                     var34.put("differences", var29.differences());
                  }

                  var31.put("mismatch", var34);
                  var31.put("evidence", var25.get("evidence"));
               }

               var31.put("at", var2.size());
               return a(var5, var6, var31);
            }

            var6 = var29.candidates();
            c(var5, var25, var6);
         }
      }

      Map var26;
      if (var6.size() != 1) {
         var26 = b("UNRESOLVED", null);
         var26.put("candidates", unresolvedCaptureUnion(var6));
      } else if (!((io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate)var6.get(0)).unresolvedCaptures().isEmpty()) {
         var26 = b("UNRESOLVED", null);
         var26.put("unresolvedCaptures", ((io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate)var6.get(0)).unresolvedCaptures());
      } else {
         var26 = b("PASS", null);
      }

      return a(var5, var6, var26);
   }

   public static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult resetHookFailure(String var0, String var1) {
      var var2 = b("INVALID", var0);
      var2.put("code", "RESET_HOOK_FAILED");
      var2.put("stderrTail", var1);
      return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult(var2, List.of(), List.of(), List.of());
   }

   public static List<String> unresolvedCapturesAt(TwinModel var0, List<Map<String, Object>> var1, int var2, RulebookRuntime var3) {
      var var4 = List.of(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(initialState(var0), List.of(), false));

      for (int var5 = 0; var5 <= var2 && var5 < var1.size(); var5++) {
         Map var6 = (Map)var1.get(var5);
         String var7 = String.valueOf(var6.get("kind"));
         Map var8 = objectField(var6, "observed");
         if (!"reset".equals(var7)) {
            if ("readBack".equals(var7)) {
               if (var6.containsKey("refused")) {
                  return unresolvedCaptureUnion(var4);
               }

               if (var8 != null && !isTransportFailure(var8)) {
                  Map var9;
                  try {
                     var9 = a(var0, objectField(var8, "response"));
                  } catch (RuntimeException var11) {
                     continue;
                  }

                  if (var9 != null) {
                     io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult var10 = a(var4, var9, var0.i().c());
                     if (var10.candidates().isEmpty()) {
                        return unresolvedCaptureUnion(var4);
                     }

                     var4 = var10.candidates();
                  }
               }
            } else {
               if (var8 == null) {
                  return unresolvedCaptureUnion(var4);
               }

               io.karatelabs.ext.rules.twin.TwinModel.CommandModel var12 = var0.f().get(String.valueOf(var6.get("command")));
               io.karatelabs.ext.rules.twin.TwinReplayEngine.TransitionResult var13 = a(var0, var4, var12, objectField(var6, "args"), var8, var3);
               if (var13.mismatch() != null) {
                  return unresolvedCaptureUnion(var4);
               }

               var4 = var13.nextStates();
            }
         }
      }

      return unresolvedCaptureUnion(var4);
   }

   public static List<Map<String, Object>> summarizeRefusals(Map<String, Map<String, Object>> var0) {
      Comparator var1 = Comparator.<io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature, String>comparing(io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature::command, io.karatelabs.ext.rules.twin.CanonicalJson::a)
         .thenComparingInt(io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature::status)
         .thenComparing(io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature::body, io.karatelabs.ext.rules.twin.CanonicalJson::a);
      TreeMap var2 = new TreeMap(var1);

      for (Map var4 : var0.values()) {
         Object var6 = var4.get("log");
         if (var6 instanceof List) {
            for (Object var7 : (List)var6) {
               if (var7 instanceof Map var8
                  && "refused".equals(var8.get("outcome"))
                  && var8.get("command") instanceof String var9
                  && var8.get("observed") instanceof Map var10
                  && var10.get("response") instanceof Map var11
                  && var11.get("status") instanceof Number var12) {
                  Object var23 = var11.get("body");
                  io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature var14 = new io.karatelabs.ext.rules.twin.TwinReplayEngine.RefusalSignature(var9, var12.intValue(), var11.containsKey("body") ? io.karatelabs.ext.rules.twin.CanonicalJson.c(var23) : "");
                  Object var15 = (Map)var2.get(var14);
                  if (var15 == null) {
                     var15 = new LinkedHashMap();
                     ((java.util.Map)var15).put("command", var9);
                     ((java.util.Map)var15).put("status", var14.status());
                     if (var11.containsKey("body")) {
                        ((java.util.Map)var15).put("body", io.karatelabs.ext.rules.twin.CanonicalJson.d(var23));
                     }

                     ((java.util.Map)var15).put("reasons", new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a));
                     var2.put(var14, var15);
                  }

                  a(var8.get("candidates"), a(((java.util.Map)var15).get("reasons")));
               }
            }
         }
      }

      ArrayList var16 = new ArrayList(var2.size());

      for (Map var18 : (Iterable<Map>) (Iterable<?>) (var2.values())) {
         var18.put("reasons", new ArrayList<>(a(var18.get("reasons"))));
         var16.add(var18);
      }

      return var16;
   }

   private static void a(Object var0, Set<String> var1) {
      if (var0 instanceof List) {
         for (Object var4 : (List)var0) {
            if (var4 instanceof Map var5
               && var5.get("model") instanceof Map var6
               && "refused".equals(var6.get("kind"))
               && var6.get("reason") instanceof String var7) {
               var1.add(var7);
            }
         }
      }
   }

   private static Set<String> a(Object var0) {
      return (Set<String>)var0;
   }

   private static io.karatelabs.ext.rules.twin.TwinReplayEngine.TransitionResult a(TwinModel var0, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3, Map<String, Object> var4, RulebookRuntime var5) {
      String var7 = null;
      String var8 = null;
      boolean var9 = false;
      String var6;
      if (isTransportFailure(var4)) {
         var6 = "transportFailure";
      } else if (var2.l() == null) {
         var6 = "unknown";
      } else {
         try {
            var var10 = asMap(var2.l().call(null, objectField(var4, "response")));
            var6 = String.valueOf(var10.get("kind"));
            var7 = var10.get("reason") == null ? null : String.valueOf(var10.get("reason"));
            var8 = var10.get("cause") == null ? null : String.valueOf(var10.get("cause"));
         } catch (RuntimeException var27) {
            var6 = "error";
            var9 = true;
         }
      }

      ArrayList var28 = new ArrayList();
      ArrayList var11 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var13 : var1) {
         io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var14 = CommandExecutor.a(var0, var13.state(), var2, var3, var5);
         LinkedHashMap var15 = new LinkedHashMap();
         var15.put("key", io.karatelabs.ext.rules.twin.CanonicalJson.c(var13.state()));
         LinkedHashMap var16 = new LinkedHashMap();
         var16.put("kind", var14.b());
         if (var14.c() != null) {
            var16.put("stage", var14.c());
         }

         String var17 = "refused".equals(var14.b()) ? a(var2, var14) : null;
         if ("refused".equals(var14.b())) {
            var16.put("reason", var17);
         }

         if (var14.f() != null) {
            var16.put("code", var14.f());
         }

         var15.put("model", var16);
         ArrayList var18 = new ArrayList();
         ArrayList var19 = new ArrayList();
         if (!"error".equals(var14.b()) && !var9) {
            if ("transportFailure".equals(var6)) {
               for (String var22 : var2.p()) {
                  if ("applied".equals(var22) && var14.a()) {
                     var var23 = asObjectMap(io.karatelabs.ext.rules.twin.CanonicalJson.d(var14.j()));

                     for (String var25 : io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var2.n())) {
                        var23.remove(var25);
                     }

                     a(var2, var13, var14, var23, false, var19, var18);
                  }

                  if ("unchanged".equals(var22)) {
                     var19.add(var13);
                     var18.add(a(var13));
                  }
               }

               var15.put("consistent", !var19.isEmpty());
            } else if ("applied".equals(var6)) {
               if (var14.a()) {
                  boolean var20 = true;
                  var var21 = asObjectMap(io.karatelabs.ext.rules.twin.CanonicalJson.d(var14.j()));
                  if (var2.m() != null) {
                     try {
                        var2.m().call(null, var21, objectField(var4, "response"));
                        io.karatelabs.ext.rules.twin.CanonicalJson.b(var21);
                     } catch (RuntimeException var26) {
                        var20 = false;
                        var15.put("consistent", "notChecked");
                        var15.put("captureError", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var26));
                        var19.add(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(var13.state(), var13.unresolvedCaptures(), true));
                        var18.add(a(var13));
                     }
                  }

                  if (var20) {
                     a(var2, var13, var14, var21, true, var19, var18);
                     var15.put("consistent", true);
                  }
               } else {
                  var15.put("consistent", false);
               }
            } else if (!"refused".equals(var6)) {
               var15.put("consistent", false);
            } else if ("refused".equals(var14.b()) && var17 != null && var17.equals(var7)) {
               var19.add(var13);
               var18.add(a(var13));
               var15.put("consistent", true);
            } else {
               var15.put("consistent", false);
               var15.put("why", "refused".equals(var14.b()) && var17 != null ? "reasonMismatch" : "kindMismatch");
            }
         } else {
            var15.put("consistent", "notChecked");
            var19.add(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(var13.state(), var13.unresolvedCaptures(), true));
            var18.add(a(var13));
         }

         var15.put("successors", var18);
         var28.add(var15);
         var11.addAll(var19);
      }

      var var29 = mergeCandidateStates((List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate>)var11);
      LinkedHashMap var30 = null;
      if (var29.isEmpty()) {
         List var31 = c(var28);
         var30 = new LinkedHashMap();
         if (var31 != null) {
            var30.put("kind", "refusalReasonMismatch");
            var30.put("observed", var7);
            var30.put("model", var31);
            var30.put("differences", List.of(a("refusal", var7, d(var31))));
         } else {
            var30.put("kind", "noCandidateExplainsObservation");
            var30.put("outcome", var6);
            if (var7 != null) {
               var30.put("reason", var7);
            }

            if (var8 != null) {
               var30.put("cause", var8);
            }

            List var32 = a(var6, var28);
            if (!var32.isEmpty()) {
               var30.put("differences", var32);
            }
         }
      }

      return new io.karatelabs.ext.rules.twin.TwinReplayEngine.TransitionResult(var6, var28, var29, var30, a(var0, var28, var29));
   }

   private static List<String> c(List<Map<String, Object>> var0) {
      ArrayList var1 = new ArrayList();

      for (Map var3 : var0) {
         if (!"reasonMismatch".equals(var3.get("why"))) {
            return null;
         }

         Object var4 = asObjectMap(var3.get("model")).get("reason");
         if (!var1.contains(String.valueOf(var4))) {
            var1.add(String.valueOf(var4));
         }
      }

      return var1.isEmpty() ? null : var1;
   }

   private static Map<String, Object> a(String var0, Object var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("field", var0);
      var3.put("observed", var1);
      var3.put("expected", var2);
      return var3;
   }

   private static List<Map<String, Object>> a(String var0, List<Map<String, Object>> var1) {
      if (!"applied".equals(var0) && !"refused".equals(var0) && !"unknown".equals(var0)) {
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();

         for (Map var4 : var1) {
            String var5 = String.valueOf(asObjectMap(var4.get("model")).get("kind"));
            if (var5.equals(var0)) {
               return List.of();
            }

            var2.add(var5);
         }

         return var2.isEmpty() ? List.of() : List.of(a("outcome", var0, d(var2)));
      }
   }

   private static List<Map<String, Object>> b(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var0, Map<String, Object> var1, List<String> var2) {
      var var3 = new ArrayList<>(var2);

      for (String var5 : var1.keySet()) {
         if (!var3.contains(var5)) {
            var3.add(var5);
         }
      }

      ArrayList var13 = new ArrayList();

      for (String var6 : (Iterable<String>) (Iterable<?>) (var3)) {
         if (var1.containsKey(var6)) {
            String var7 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get(var6));
            TreeMap var8 = new TreeMap<String, Object>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
            boolean var9 = false;

            for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var11 : var0) {
               Object var12 = asObjectMap(var11.state()).get(var6);
               if (var11.unresolvedCaptures().contains(var6) || io.karatelabs.ext.rules.twin.CanonicalJson.c(var12).equals(var7)) {
                  var9 = true;
                  break;
               }

               var8.put(io.karatelabs.ext.rules.twin.CanonicalJson.c(var12), io.karatelabs.ext.rules.twin.CanonicalJson.d(var12));
            }

            if (!var9 && !var8.isEmpty()) {
               var13.add(a(var6, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1.get(var6)), new ArrayList(var8.values())));
            }
         }
      }

      return var13;
   }

   private static List<String> d(List<String> var0) {
      TreeSet var1 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      var1.addAll(var0);
      return new ArrayList<>(var1);
   }

   private static void a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var1, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var2, Map<String, Object> var3, boolean var4, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var5, List<Map<String, Object>> var6) {
      ArrayList var7 = new ArrayList();
      var var8 = new ArrayList<>(var1.unresolvedCaptures());

      for (String var10 : io.karatelabs.ext.rules.twin.JsonPathTokenMatcher.a(var0.n())) {
         LinkedHashMap var11 = new LinkedHashMap();
         var11.put("field", var10);
         var11.put("value", var4 ? var3.get(var10) : "unknown");
         var7.add(var11);
         if (var4) {
            var8.remove(var10);
         } else if (!var8.contains(var10)) {
            var8.add(var10);
         }
      }

      var8.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      var5.add(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(var3, var8, var1.notChecked()));
      LinkedHashMap var13 = new LinkedHashMap();
      var13.put("key", io.karatelabs.ext.rules.twin.CanonicalJson.c(var3));
      var13.put("then", var2.n());
      ArrayList var14 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation var12 : var2.p()) {
         var14.add(Map.of("name", var12.a(), "status", var12.b()));
      }

      var13.put("invariants", var14);
      var13.put("captures", var7);
      var13.put("unknown", var8);
      var6.add(var13);
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("key", io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.state()));
      var1.put("unknown", var0.unresolvedCaptures());
      return var1;
   }

   private static String a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var1) {
      if ("guard".equals(var1.c())) {
         return var0.o().get("guard");
      } else {
         return "apply".equals(var1.c()) ? var1.d() : null;
      }
   }

   static List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> mergeCandidateStates(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var0) {
      ArrayList var1 = new ArrayList();
      LinkedHashMap var2 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var4 : var0) {
         String var5 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var4.state());
         Integer var6 = (Integer)var2.get(var5);
         if (var6 == null) {
            var2.put(var5, var1.size());
            var1.add(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(io.karatelabs.ext.rules.twin.CanonicalJson.d(var4.state()), new ArrayList<>(var4.unresolvedCaptures()), var4.notChecked()));
         } else {
            io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var7 = (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate)var1.get(var6);
            var var8 = new ArrayList<>(var7.unresolvedCaptures());

            for (String var10 : var4.unresolvedCaptures()) {
               if (!var8.contains(var10)) {
                  var8.add(var10);
               }
            }

            var8.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
            var1.set(var6, new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(var7.state(), var8, var7.notChecked() || var4.notChecked()));
         }
      }

      return var1;
   }

   private static Map<String, Object> a(TwinModel var0, List<Map<String, Object>> var1, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      boolean var4 = false;
      boolean var5 = true;
      LinkedHashMap var6 = new LinkedHashMap();

      for (Map var8 : var1) {
         for (Map var10 : asMapList(var8.get("successors"))) {
            Object var11 = var10.get("then");
            if (var11 != null && !"notEvaluated".equals(var11)) {
               var4 = true;
               if (!"held".equals(var11)) {
                  var5 = false;
               }
            }

            for (Map var13 : asMapList(var10.get("invariants"))) {
               String var14 = String.valueOf(var13.get("name"));
               boolean var15 = "failed".equals(var6.get(var14)) || !"held".equals(var13.get("status"));
               var6.put(var14, var15 ? "failed" : "held");
            }
         }
      }

      var3.put("then", var4 ? (var5 ? "held" : "failed") : "notEvaluated");
      var3.put("invariants", var6);
      LinkedHashMap var16 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var18 : var0.b()) {
         boolean var19 = false;

         for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var21 : var2) {
            if ("held".equals(CommandExecutor.a(var18.b(), var21.state()))) {
               var19 = true;
            }
         }

         var16.put(var18.a(), var19);
      }

      var3.put("states", var16);
      return var3;
   }

   private static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult a(TwinModel var0, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var1, Map<String, Object> var2, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver var3) {
      io.karatelabs.ext.rules.twin.TwinModel.ReadBackModel var4 = var0.i();
      TreeMap var5 = new TreeMap<String, Object>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var7 : var1) {
         var var8 = observe(var4.a(), var7.state(), null);
         var5.put(io.karatelabs.ext.rules.twin.CanonicalJson.c(var8), var8);
      }

      if (var5.size() != 1) {
         var2.put("refused", Map.of("code", "AMBIGUOUS_REQUEST", "requests", new ArrayList(var5.keySet())));
         return null;
      } else {
         Map var11 = (Map)var5.firstEntry().getValue();
         Map var12 = var3.observe(var11);
         var2.put("request", var11);
         var2.put("observed", var12);
         if (isTransportFailure(var12)) {
            return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult(List.of(), List.of(), false, null, true, false, List.of());
         } else {
            Map var13;
            try {
               var13 = a(var0, objectField(var12, "response"));
            } catch (RuntimeException var10) {
               var2.put("observeError", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var10));
               return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult(List.of(), List.of(), false, null, false, true, List.of());
            }

            if (var13 == null) {
               var2.put("observeError", "read-back observe returned no object");
               return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult(List.of(), List.of(), false, null, false, true, List.of());
            } else {
               io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult var9 = a(var1, var13, var4.c());
               var2.put("candidates", var9.candidateChecks());
               if (var9.missingCapture() != null) {
                  var2.put("missing", var9.missingCapture());
               }

               return var9;
            }
         }
      }
   }

   private static Map<String, Object> a(TwinModel var0, Map<String, Object> var1) {
      Object var2 = var0.i().b().call(null, var1);
      if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         Object var3 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var2);
         return var3 instanceof Map ? asObjectMap(var3) : null;
      } else {
         return null;
      }
   }

   static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult a(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var0, Map<String, Object> var1, List<String> var2) {
      for (String var4 : var2) {
         if (!var1.containsKey(var4) || io.karatelabs.ext.rules.twin.CanonicalJson.a(var1.get(var4))) {
            return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult(List.of(), List.of(), false, var4, false, false, List.of());
         }
      }

      ArrayList var14 = new ArrayList();
      ArrayList var15 = new ArrayList();
      var var5 = new ArrayList<>(var1.keySet());
      var5.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var7 : var0) {
         var var8 = asObjectMap(io.karatelabs.ext.rules.twin.CanonicalJson.d(var7.state()));
         var var9 = new ArrayList<>(var7.unresolvedCaptures());
         ArrayList var10 = new ArrayList();
         boolean var11 = true;

         for (String var13 : (Iterable<String>) (Iterable<?>) (var5)) {
            if (var9.contains(var13)) {
               var8.put(var13, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1.get(var13)));
               var9.remove(var13);
               var10.add(var13);
            } else if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var8.get(var13)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get(var13)))) {
               var11 = false;
            }
         }

         LinkedHashMap var17 = new LinkedHashMap();
         var17.put("key", io.karatelabs.ext.rules.twin.CanonicalJson.c(var7.state()));
         var17.put("consistent", var11);
         var17.put("resolved", var10);
         var17.put("notChecked", var7.notChecked());
         var15.add(var17);
         if (var11) {
            var14.add(new io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate(var8, var9, var7.notChecked()));
         }
      }

      boolean var16 = !var0.isEmpty() && var0.stream().allMatch(io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate::notChecked);
      return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReadBackResult(mergeCandidateStates((List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate>)var14), var15, var16, null, false, false, var14.isEmpty() ? b(var0, var1, var2) : List.of());
   }

   private static Map<String, Object> stepMarker(int var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("at", var0);
      var2.put("kind", var1);
      return var2;
   }

   private static void c(List<Map<String, Object>> var0, Map<String, Object> var1, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var2) {
      var1.put("profileAfter", unresolvedCaptureUnion(var2));
      ArrayList var3 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var5 : var2) {
         var3.add(var5.unresolvedCaptures());
      }

      var1.put("unknownAfter", var3);
      var1.put("evidence", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1)));
      var0.add(var1);
   }

   private static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult a(List<Map<String, Object>> var0, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var1, Map<String, Object> var2) {
      ArrayList var3 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var5 : var1) {
         var3.add(var5.unresolvedCaptures());
      }

      return new io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult(var2, var0, unresolvedCaptureUnion(var1), var3);
   }

   private static Map<String, Object> b(String var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("kind", var0);
      if (var1 != null) {
         var2.put("reason", var1);
      }

      return var2;
   }

   private static Map<String, Object> a(String var0, int var1) {
      var var2 = b("REFUSED", null);
      var2.put("code", var0);
      var2.put("at", var1);
      return var2;
   }

   static List<String> unresolvedCaptureUnion(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var0) {
      ArrayList var1 = new ArrayList(var0.size());

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var3 : var0) {
         var1.add(io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.state()));
      }

      return var1;
   }

   static Object initialState(TwinModel var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var0.a().call(null, var1);
      io.karatelabs.ext.rules.twin.CanonicalJson.b(var1);
      return var1;
   }

   private static Map<String, Object> observe(JavaCallable var0, Object var1, Map<String, Object> var2) {
      Object var3 = var2 == null
         ? var0.call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1))
         : var0.call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var1), io.karatelabs.ext.rules.twin.CanonicalJson.d(var2));
      if (io.karatelabs.ext.rules.twin.CanonicalJson.d(var3) instanceof Map var5) {
         for (Object var7 : var5.keySet()) {
            if (!"method".equals(var7) && !"path".equals(var7) && !"body".equals(var7)) {
               throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("request carries an unknown key '" + var7 + "'");
            }
         }

         if (!(var5.get("method") instanceof String var11 && HTTP_METHOD_PATTERN.matcher(var11).matches())) {
            throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("request.method must be an upper-case ASCII token");
         } else if (var5.get("path") instanceof String var13 && var13.startsWith("/") && !var13.startsWith("//")) {
            LinkedHashMap var14 = new LinkedHashMap();
            var14.put("method", var11);
            var14.put("path", var13);
            if (var5.containsKey("body") && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var5.get("body"))) {
               try {
                  io.karatelabs.ext.rules.twin.CanonicalJson.b(var5.get("body"));
               } catch (SnapshotException var10) {
                  throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("request.body: " + var10.getMessage());
               }

               var14.put("body", var5.get("body"));
            }

            return var14;
         } else {
            throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("request.path must be relative and start with '/'");
         }
      } else {
         throw new io.karatelabs.ext.rules.twin.TwinReplayEngine.RequestConstructionException("request must return {method, path, body?}");
      }
   }

   private static Map<String, Object> asMap(Object var0) {
      if (io.karatelabs.ext.rules.twin.CanonicalJson.d(var0) instanceof Map var2) {
         Object var3 = var2.get("kind");
         if ("applied".equals(var3) && var2.size() == 1) {
            return Map.of("kind", "applied");
         }

         if ("refused".equals(var3) && var2.size() == 2 && var2.get("reason") instanceof String var4) {
            return Map.of("kind", "refused", "reason", var4);
         }

         if ("refused".equals(var3) && !(var2.get("reason") instanceof String)) {
            return Map.of("kind", "unknown", "cause", "REASONLESS_REFUSAL");
         }
      }

      return Map.of("kind", "unknown");
   }

   private static boolean isTransportFailure(Map<String, Object> var0) {
      return Boolean.TRUE.equals(var0.get("transportFailure"));
   }

   private static List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> a(TwinModel var0, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> var1, Map<String, Object> var2) {
      ArrayList var3 = new ArrayList(var1.size());

      for (io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate var5 : var1) {
         if (TwinWalker.a(var0, var5.state(), var2)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      var var1 = objectField(var0, "args");
      return var1 == null ? Map.of() : var1;
   }

   private static Map<String, Object> objectField(Map<String, Object> var0, String var1) {
      return var0 == null ? null : (Map)var0.get(var1);
   }

   private static Map<String, Object> asObjectMap(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static List<Map<String, Object>> asMapList(Object var0) {
      return var0 == null ? Collections.emptyList() : (List)var0;
   }

   static final class RequestConstructionException extends RuntimeException {
      RequestConstructionException(String var1) {
         super(var1);
      }
   }

   record StateCandidate(Object state, List<String> unresolvedCaptures, boolean notChecked) {

      StateCandidate(Object state, List<String> unresolvedCaptures, boolean notChecked) {
         this.state = state;
         this.unresolvedCaptures = unresolvedCaptures;
         this.notChecked = notChecked;
      }
   }

   private record TransitionResult(String kind, List<Map<String, Object>> candidates, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> nextStates, Map<String, Object> mismatch, Map<String, Object> properties) {

      private TransitionResult(String kind, List<Map<String, Object>> candidates, List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> nextStates, Map<String, Object> mismatch, Map<String, Object> properties) {
         this.kind = kind;
         this.candidates = candidates;
         this.nextStates = nextStates;
         this.mismatch = mismatch;
         this.properties = properties;
      }
   }

   public record ReplayResult(Map<String, Object> a, List<Map<String, Object>> b, List<String> c, List<List<String>> d) {

      public ReplayResult(Map<String, Object> a, List<Map<String, Object>> b, List<String> c, List<List<String>> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private record ReadBackResult(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> candidates, List<Map<String, Object>> candidateChecks, boolean allNotChecked, String missingCapture, boolean transportFailed, boolean observeFailed, List<Map<String, Object>> differences) {

      private ReadBackResult(List<io.karatelabs.ext.rules.twin.TwinReplayEngine.StateCandidate> candidates, List<Map<String, Object>> candidateChecks, boolean allNotChecked, String missingCapture, boolean transportFailed, boolean observeFailed, List<Map<String, Object>> differences) {
         this.candidates = candidates;
         this.candidateChecks = candidateChecks;
         this.allNotChecked = allNotChecked;
         this.missingCapture = missingCapture;
         this.transportFailed = transportFailed;
         this.observeFailed = observeFailed;
         this.differences = differences;
      }
   }

   public interface ReplayObserver {
      Map<String, Object> observe(Map<String, Object> var1);
   }

   private record RefusalSignature(String command, int status, String body) {

      private RefusalSignature(String command, int status, String body) {
         this.command = command;
         this.status = status;
         this.body = body;
      }
   }
}
