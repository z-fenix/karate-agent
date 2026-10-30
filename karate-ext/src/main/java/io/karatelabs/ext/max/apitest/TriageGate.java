package io.karatelabs.ext.max.apitest;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TriageGate {
   private static final Logger logger = LoggerFactory.getLogger(TriageGate.class);
   static final List<String> TRIAGE_FIELDS = List.of("state", "base", "candidate", "candidateBytes", "runId", "failing", "answer", "verify", "fixing");
   static final String LOST_MESSAGE = "The change the assistant was checking was lost: the console restarted or the test changed. Nothing was saved.";
   static final String INTERRUPTED_MESSAGE = "The console restarted while the suspected bug was being checked. Nothing was saved.";
   static final String QUESTION = "bug or test?";
   private static final ConcurrentHashMap<Path, ReentrantLock> ADMISSION = new ConcurrentHashMap<>();
   volatile Runnable afterAdmission;
   volatile Runnable afterSourceMoved;
   private final Path root;
   private final Path project;
   private final ChatStore store;
   private UnaryOperator<String> sanitizer = UnaryOperator.identity();
   private LiveSessions sessions;
   private CandidateRunner runner = CandidateRunner.NONE;
   private TriageGate.ReportTurn reportTurn = (var0, var1x) -> {};
   private BooleanSupplier modelConfigured = () -> false;
   private final Set<String> live = ConcurrentHashMap.newKeySet();
   private final Map<TestId, Map<String, Object>> unsaved = new ConcurrentHashMap<>();

   public TriageGate(Path var1, ChatStore var2) {
      this.root = var1.toAbsolutePath().normalize();
      this.project = TestId.of(this.root, this.root).project();
      this.store = var2;
   }

   public TriageGate withSanitizer(UnaryOperator<String> var1) {
      this.sanitizer = var1;
      return this;
   }

   public TriageGate withSessions(LiveSessions var1) {
      this.sessions = var1;
      return this;
   }

   public TriageGate withRunner(CandidateRunner var1) {
      this.runner = var1 == null ? CandidateRunner.NONE : var1;
      return this;
   }

   public TriageGate withReportTurn(TriageGate.ReportTurn var1) {
      this.reportTurn = var1 == null ? (var0, var1x) -> {} : var1;
      return this;
   }

   public TriageGate withModelConfigured(BooleanSupplier var1) {
      this.modelConfigured = var1;
      return this;
   }

   public ChatStore store() {
      return this.store;
   }

   public TestId id(Path var1) {
      return TestId.of(this.root, var1);
   }

   private <T> T locked(TestId var1, Supplier<T> var2) {
      ReentrantLock var3 = this.store.lock(var1);
      var3.lock();

      Object var4;
      try {
         var4 = var2.get();
      } finally {
         var3.unlock();
      }

      return (T)var4;
   }

   ReentrantLock admission() {
      return ADMISSION.computeIfAbsent(this.project, var0 -> new ReentrantLock());
   }

   private <T> T admitted(Supplier<T> var1) {
      ReentrantLock var2 = this.admission();
      var2.lock();

      Object var3;
      try {
         var3 = var1.get();
      } finally {
         var2.unlock();
      }

      return (T)var3;
   }

   private io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState resolve(TestId var1) {
      var var2 = this.unsaved.get(var1);
      ChatStore.Doc var3 = this.store.read(var1);
      if (var2 != null) {
         return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("open", null, var2, var3, true);
      } else if (!var3.exists()) {
         return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("none", null, null, var3, false);
      } else if (var3.unparseable()) {
         return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("lost", "the chat store document does not parse", null, var3, false);
      } else {
         var var4 = new LinkedHashMap<>(var3.parsed());
         String var5 = hashOf(var1.file());
         if (var4.get("landing") instanceof Map var6) {
            if (Objects.equals(var6.get("after"), var5)) {
               var4.put("land", var6);
               TRIAGE_FIELDS.forEach(var4::remove);
            }

            var4.remove("landing");

            try {
               this.persist(var1, var4);
            } catch (IOException var14) {
               return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState(var4.get("state") instanceof String var8 ? var8 : "none", null, var4, var3, false, ioError(var1, var14));
            }
         }

         String var15 = var4.get("state") instanceof String var16 ? var16 : "none";
         if ("none".equals(var15)) {
            return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("none", null, var4, var3, false);
         } else {
            String var17 = var4.get("runId") instanceof String var19 ? var19 : null;
            if ("running".equals(var15)) {
               return this.live.contains(var17)
                  ? new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("running", null, var4, var3, false)
                  : new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("lost", "the candidate run did not finish", var4, var3, false);
            } else {
               if ("verifying".equals(var15)) {
                  Map var20 = var4.get("verify") instanceof Map var23 ? var23 : Map.of();
                  if (this.live.contains(var20.get("runId") instanceof String var24 ? var24 : null)) {
                     return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("verifying", null, var4, var3, false);
                  }

                  LinkedHashMap var25 = new LinkedHashMap();
                  var25.put("runId", var20.get("runId"));
                  var25.put("outcome", "interrupted");
                  var4.put("state", "answered");
                  var4.put("verify", var25);

                  try {
                     this.persist(var1, var4);
                  } catch (IOException var13) {
                     return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("answered", null, var4, var3, false, ioError(var1, var13));
                  }

                  var15 = "answered";
               }

               String var21 = var4.get("candidate") instanceof String var26 ? var26 : null;
               byte[] var27 = bytesOf(var4.get("candidateBytes"));
               if (var21 != null && var27 != null && io.karatelabs.ext.common.Sha256.a(var27).equals(var21)) {
                  String var30 = var4.get("base") instanceof String var11 ? var11 : null;
                  return (var30 == null ? var5 != null : !var30.equals(var5))
                     ? new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState(
                        "lost",
                        var30 == null ? "the test appeared since the candidate was built" : "the test changed since the candidate was built",
                        var4,
                        var3,
                        false
                     )
                     : new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState(var15, null, var4, var3, false);
               } else {
                  return new io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState("lost", "the candidate bytes do not hash to the candidate", var4, var3, false);
               }
            }
         }
      }
   }

   private void persist(TestId var1, Map<String, Object> var2) throws IOException {
      if (var2.get("state") == null && var2.get("land") == null && var2.get("landing") == null) {
         this.store.delete(var1);
      } else {
         var2.put("v", 2);
         var2.put("path", var1.path());
         this.store.write(var1, var2);
      }
   }

   private static String hashOf(Path var0) {
      try {
         return Files.isRegularFile(var0) ? io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var0)) : null;
      } catch (IOException var2) {
         return null;
      }
   }

   private static byte[] bytesOf(Object var0) {
      try {
         return var0 instanceof String var1 ? Base64.getDecoder().decode(var1) : null;
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   private static String encode(byte[] var0) {
      return Base64.getEncoder().encodeToString(var0);
   }

   public Map<String, Object> check(Path var1) {
      TestId var2 = this.id(var1);
      return this.locked(var2, () -> refusal(var2, this.resolve(var2)));
   }

   private static Map<String, Object> refusal(TestId var0, io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var1) {
      if (var1.h != null) {
         return var1.h;
      } else {
         String var2 = var1.c;

         return switch (var2) {
            case "none" -> null;
            case "running", "verifying" -> refuse("BUSY", "a candidate run of " + var0.path() + " is in flight — wait for its result");
            default -> refuse(
               "TRIAGE_PENDING",
               var0.path()
                  + " has an open question, bug or test: the tester answers it with the test page's buttons or in words, and nothing changes the test until then"
            );
         };
      }
   }

   public Object write(Path var1, Supplier<?> var2) {
      TestId var3 = this.id(var1);
      return FeatureStore.locked(var3.file(), () -> this.locked(var3, () -> {
         var var3x = refusal(var3, this.resolve(var3));
         if (var3x != null) {
            return var3x;
         } else {
            Runnable var4 = this.afterAdmission;
            if (var4 != null) {
               var4.run();
            }

            return var2.get();
         }
      }));
   }

   public Object write(List<Path> var1, Supplier<?> var2) {
      var var3 = var1.stream().map(MaxPaths::lockKey).distinct().sorted(Comparator.comparing(Path::toString)).toList();
      return this.writeEach(var3, 0, var2);
   }

   private Object writeEach(List<Path> var1, int var2, Supplier<?> var3) {
      return var2 == var1.size() ? var3.get() : this.write((Path)var1.get(var2), () -> this.writeEach(var1, var2 + 1, var3));
   }

   public Object tree(List<Path> var1, Supplier<?> var2) {
      var var3 = var1.stream().map(var0 -> var0.toAbsolutePath().normalize()).toList();
      return this.transaction(var1x -> var3.stream().anyMatch(var1xx -> var1x.under(var1xx) || var1x.file().equals(var1xx)), var2);
   }

   public Object project(Supplier<?> var1) {
      Object var2 = this.transaction(var0 -> true, () -> null);
      return var2 != null ? var2 : var1.get();
   }

   private Object transaction(Predicate<TestId> var1, Supplier<?> var2) {
      return this.admitted(() -> {
         var var3 = this.any(var1);
         if (var3 != null) {
            return var3;
         } else {
            Runnable var4 = this.afterAdmission;
            if (var4 != null) {
               var4.run();
            }

            return var2.get();
         }
      });
   }

   public Map<String, Object> checkTree(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      return this.any(var1x -> var1x.under(var2) || var1x.file().equals(var2));
   }

   public Map<String, Object> checkProject() {
      return this.any(var0 -> true);
   }

   public Map<String, Object> checkout(List<Path> var1) {
      var var2 = var1.stream()
         .map(var1x -> (Path)(var1x.isAbsolute() ? var1x : this.root.resolve(var1x)))
         .map(var0 -> var0.toAbsolutePath().normalize())
         .toList();
      ArrayList var3 = new ArrayList();
      io.karatelabs.ext.max.apitest.TriageGate.TriageIndex var4 = this.index();

      for (TestId var6 : var4.a) {
         boolean var7 = var2.stream().anyMatch(var1x -> var6.file().equals(var1x) || var6.under(var1x));
         if (var7 && this.locked(var6, () -> this.resolve(var6).b())) {
            var3.add(var6.path());
         }
      }

      if (var4.b != null) {
         var3.add("?");
      }

      if (var3.isEmpty()) {
         return null;
      } else {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("reason", "triage-pending");
         var8.put("paths", var3);
         return var8;
      }
   }

   private Map<String, Object> any(Predicate<TestId> var1) {
      io.karatelabs.ext.max.apitest.TriageGate.TriageIndex var2 = this.index();
      if (var2.b != null) {
         return var2.b;
      } else {
         for (TestId var4 : var2.a) {
            if (var1.test(var4)) {
               var var5 = this.locked(var4, () -> refusal(var4, this.resolve(var4)));
               if (var5 != null) {
                  return var5;
               }
            }
         }

         return null;
      }
   }

   private io.karatelabs.ext.max.apitest.TriageGate.TriageIndex index() {
      var var1 = new ArrayList<>(this.unsaved.keySet());
      Map var2 = null;

      try {
         for (ChatStore.Doc var4 : this.store.documents(this.project)) {
            if (var4.parsed() != null && var4.parsed().get("path") instanceof String var5) {
               TestId var8 = new TestId(this.project, var5);
               if (!var1.contains(var8)) {
                  var1.add(var8);
               }
            } else {
               var2 = refuse("TRIAGE_PENDING", "a triage record in the chat store cannot be read — discard it from the test's page");
            }
         }
      } catch (IOException var7) {
         var2 = refuse("IO_ERROR", "could not list the chat store for the project: " + var7.getMessage());
      }

      return new io.karatelabs.ext.max.apitest.TriageGate.TriageIndex(var1, var2);
   }

   static Map<String, Object> refuse(String var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("error", var1);
      var2.put("code", var0);
      var2.put("status", "IO_ERROR".equals(var0) ? 500 : ("NOT_FOUND".equals(var0) ? 404 : 409));
      return var2;
   }

   private static Map<String, Object> ioError(TestId var0, IOException var1) {
      return refuse("IO_ERROR", "could not write the chat store document for " + var0.path() + ": " + var1.getMessage());
   }

   public Map<String, Object> beginCandidate(TestId var1, byte[] var2, String var3, String var4) {
      return this.admitted(() -> this.locked(var1, () -> {
         io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var5 = this.resolve(var1);
         boolean var6 = "answered".equals(var5.c) && "test".equals(var5.a()) && !var5.g;
         if (var5.b() && !var6) {
            return refusal(var1, var5);
         } else {
            var var7 = this.stale(var1, var3);
            if (var7 != null) {
               return var7;
            } else {
               LinkedHashMap var8 = var5.e == null ? new LinkedHashMap() : new LinkedHashMap<>(var5.e);
               TRIAGE_FIELDS.forEach(var8::remove);
               if (var6) {
                  var8.put("fixing", var5.b("answer"));
               }

               var8.put("state", "running");
               var8.put("base", var3);
               var8.put("candidate", io.karatelabs.ext.common.Sha256.a(var2));
               var8.put("candidateBytes", encode(var2));
               var8.put("runId", var4);

               try {
                  this.persist(var1, var8);
               } catch (IOException var10) {
                  return ioError(var1, var10);
               }

               this.live.add(var4);
               return null;
            }
         }
      }));
   }

   private Map<String, Object> stale(TestId var1, String var2) {
      String var3 = hashOf(var1.file());
      return (var2 == null ? var3 != null : !var2.equals(var3))
         ? refuse(
            "STALE", var1.path() + (var3 == null ? " is gone" : (var2 == null ? " appeared" : " changed")) + " since the candidate was built — read it again"
         )
         : null;
   }

   public Map<String, Object> candidateDone(TestId var1, String var2, CandidateRunner.Outcome var3) {
      return this.candidateDone(var1, var2, var3, true);
   }

   public Map<String, Object> candidateDone(TestId var1, String var2, CandidateRunner.Outcome var3, boolean var4) {
      Map var5;
      try {
         var5 = this.locked(var1, () -> {
            ChatStore.Doc var5x = this.store.read(var1);
            LinkedHashMap var6 = var5x.parsed() == null ? null : new LinkedHashMap<>(var5x.parsed());
            if (var6 != null && "running".equals(var6.get("state")) && var2.equals(var6.get("runId"))) {
               Object var7 = var6.get("fixing");
               if (!"green".equals(var3.status()) && !var4 && var7 instanceof Map) {
                  var6.remove("fixing");
                  var6.put("state", "answered");
                  var6.put("answer", var7);
                  var6.put("failing", this.sanitized(var3.failing() != null ? var3.failing() : Map.of("actual", String.valueOf(var3.error()))));

                  try {
                     this.persist(var1, var6);
                  } catch (IOException var9) {
                     return ioError(var1, var9);
                  }

                  return Map.of("triage", "answered");
               } else if (!"green".equals(var3.status()) && var4) {
                  if (knownBug(var6, var6.get("candidate"), var3.failing())) {
                     TRIAGE_FIELDS.forEach(var6::remove);

                     try {
                        this.persist(var1, var6);
                     } catch (IOException var10) {
                        return ioError(var1, var10);
                     }

                     return Map.of("triage", "bug");
                  } else {
                     var6.remove("fixing");
                     var6.put("state", "open");
                     var6.put("failing", this.sanitized(var3.failing() != null ? var3.failing() : Map.of("actual", String.valueOf(var3.error()))));

                     try {
                        this.persist(var1, var6);
                     } catch (IOException var11) {
                        this.unsaved.put(var1, var6);
                        return Map.of("triage", "unsaved");
                     }

                     return Map.of("triage", "open");
                  }
               } else {
                  if ("green".equals(var3.status())) {
                     retire(var6, var6.get("candidate"));
                  }

                  TRIAGE_FIELDS.forEach(var6::remove);

                  try {
                     this.persist(var1, var6);
                  } catch (IOException var12) {
                     return ioError(var1, var12);
                  }

                  return Map.of("triage", "none");
               }
            } else {
               return Map.of("triage", "none");
            }
         });
      } finally {
         this.live.remove(var2);
      }

      return var5;
   }

   private static boolean knownBug(Map<String, Object> var0, Object var1, Map<String, Object> var2) {
      return var0.get("land") instanceof Map var4
         && Boolean.TRUE.equals(var4.get("bug"))
         && var1 != null
         && var1.equals(var4.get("after"))
         && var4.get("step") instanceof Map var3
         && Failing.sameStep(var3, var2);
   }

   private static void retire(Map<String, Object> var0, Object var1) {
      if (var0.get("land") instanceof Map var2 && Boolean.TRUE.equals(var2.get("bug")) && var1 != null && var1.equals(var2.get("after"))) {
         LinkedHashMap var4 = new LinkedHashMap(var2);
         var4.remove("bug");
         var4.remove("step");
         var0.put("land", var4);
      }
   }

   private Map<String, Object> sanitized(Map<String, Object> var1) {
      return (Map<String, Object>)(Json.parseStrict(this.sanitizer.apply(StringUtils.formatJson(var1, false, false, false)), true) instanceof Map var3
         ? var3
         : new LinkedHashMap<>(var1));
   }

   public Map<String, Object> land(TestId var1, byte[] var2, String var3, String var4) {
      var var5 = this.locked(var1, () -> refusal(var1, this.resolve(var1)));
      return var5 != null ? var5 : this.landLocked(var1, var2, var3, var4, null, var0 -> !var0.b());
   }

   private Map<String, Object> landLocked(TestId var1, byte[] var2, String var3, String var4, Map<String, Object> var5, Predicate<io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState> var6) {
      if (!RepoLock.acquire(this.root)) {
         return refuse("BUSY", "another write holds the project — retry");
      } else {
         Map var7;
         try {
            var7 = FeatureStore.locked(var1.file(), () -> this.locked(var1, () -> {
               io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var7x = this.resolve(var1);
               if (!var6.test(var7x)) {
                  return var7x.b() ? refusal(var1, var7x) : refuse("STALE", "the triage of " + var1.path() + " moved — read it again");
               } else {
                  var var8 = this.stale(var1, var3);
                  if (var8 != null) {
                     return var8;
                  } else {
                     byte[] var9 = null;
                     if (var3 != null) {
                        try {
                           var9 = Files.readAllBytes(var1.file());
                        } catch (IOException var19) {
                           return refuse("IO_ERROR", "could not read " + var1.path() + ": " + var19.getMessage());
                        }
                     }

                     LinkedHashMap var10x = new LinkedHashMap();
                     var10x.put("before", var3);
                     var10x.put("beforeBytes", var9 == null ? null : encode(var9));
                     var10x.put("after", io.karatelabs.ext.common.Sha256.a(var2));
                     var10x.put("runId", var4);
                     var10x.put("weakening", weakening(var9, var2));
                     if (var5 != null) {
                        var10x.put("bug", true);
                        var10x.put("step", var5);
                     }

                     LinkedHashMap var11 = var7x.e == null ? new LinkedHashMap() : new LinkedHashMap<>(var7x.e);
                     var11.put("landing", var10x);

                     try {
                        this.persist(var1, var11);
                     } catch (IOException var18) {
                        return ioError(var1, var18);
                     }

                     var var12 = FeatureStore.write(var1.file(), var2, var3);
                     if (var12 != null) {
                        var11.remove("landing");

                        try {
                           this.persist(var1, var11);
                           return var12;
                        } catch (IOException var16) {
                           return ioError(var1, var16);
                        }
                     } else {
                        Runnable var13 = this.afterSourceMoved;
                        if (var13 != null) {
                           var13.run();
                        }

                        var11.put("land", var10x);
                        var11.remove("landing");
                        TRIAGE_FIELDS.forEach(var11::remove);
                        this.unsaved.remove(var1);
                        LinkedHashMap var14 = new LinkedHashMap();
                        var14.put("path", var1.path());
                        var14.put("before", var3);
                        var14.put("after", var10x.get("after"));
                        var14.put("runId", var4);
                        var14.put("weakening", var10x.get("weakening"));
                        var14.put("created", var3 == null);

                        try {
                           this.persist(var1, var11);
                        } catch (IOException var17) {
                           var14.putAll(ioError(var1, var17));
                           var14.put("landed", true);
                        }

                        return var14;
                     }
                  }
               }
            }));
         } finally {
            RepoLock.release(this.root);
         }

         return var7;
      }
   }

   private static List<Object> weakening(byte[] var0, byte[] var1) {
      ArrayList var2 = new ArrayList();
      if (var0 != null) {
         for (WeakeningDiff.Flag var4 : WeakeningDiff.diff(FeatureSource.of(var0), FeatureSource.of(var1))) {
            var2.add(var4.toJson());
         }
      }

      return var2;
   }

   public Map<String, Object> beginVerification(TestId var1, byte[] var2, String var3) {
      return this.locked(var1, () -> {
         io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4 = this.resolve(var1);
         if ("answered".equals(var4.c) && "bug".equals(var4.a())) {
            var var5 = new LinkedHashMap<>(var4.e);
            var5.put("state", "verifying");
            var5.put("candidate", io.karatelabs.ext.common.Sha256.a(var2));
            var5.put("candidateBytes", encode(var2));
            var5.put("verify", new LinkedHashMap<>(Map.of("runId", var3)));

            try {
               this.persist(var1, var5);
            } catch (IOException var7) {
               return ioError(var1, var7);
            }

            this.live.add(var3);
            return null;
         } else {
            return var4.b() ? refuse("STALE", var1.path() + " is not an answered suspected bug") : refuse("NOT_FOUND", "no triage is open on " + var1.path());
         }
      });
   }

   public Map<String, Object> verificationDone(TestId var1, String var2, CandidateRunner.Outcome var3) {
      Map var5;
      try {
         Map var4 = this.locked(var1, () -> {
            io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4x = this.resolve(var1);
            var var5x = var4x.b("verify");
            if ("verifying".equals(var4x.c) && var5x != null && var2.equals(var5x.get("runId"))) {
               var var6x = Failing.identified(bytesOf(var4x.e.get("candidateBytes")), var4x.b("failing"));
               if ("red".equals(var3.status()) && Failing.sameStep(var6x, var3.failing())) {
                  return null;
               } else {
                  var var7 = new LinkedHashMap<>(var4x.e);
                  LinkedHashMap var8 = new LinkedHashMap();
                  var8.put("runId", var2);
                  var8.put("outcome", var3.status());
                  if (var3.step() != null) {
                     var8.put("step", var3.step());
                  }

                  if (var3.failing() != null) {
                     for (String var10x : List.of("row", "section", "scenario")) {
                        if (var3.failing().get(var10x) != null) {
                           var8.put(var10x, var3.failing().get(var10x));
                        }
                     }
                  }

                  var7.put("state", "answered");
                  var7.put("verify", var8);

                  try {
                     this.persist(var1, var7);
                  } catch (IOException var11x) {
                     return ioError(var1, var11x);
                  }

                  LinkedHashMap var12 = new LinkedHashMap();
                  var12.put("path", var1.path());
                  var12.put("state", "answered");
                  var12.put("verify", var8);
                  return var12;
               }
            } else {
               return refuse("STALE", "no verification " + var2 + " is in flight for " + var1.path());
            }
         });
         if (var4 == null) {
            io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var11 = this.locked(var1, () -> this.resolve(var1));
            byte[] var6 = bytesOf(var11.e.get("candidateBytes"));
            return this.landLocked(
               var1,
               var6,
               var11.a("base"),
               var2,
               Failing.identity(Failing.identified(var6, var3.failing())),
               var1x -> "verifying".equals(var1x.c) && var1x.b("verify") != null && var2.equals(var1x.b("verify").get("runId"))
            );
         }

         var5 = var4;
      } finally {
         this.live.remove(var2);
      }

      return var5;
   }

   public Map<String, Object> pageRun(SuiteResult var1, String var2) {
      if (var1 == null) {
         return null;
      } else {
         FeatureResult var3 = null;
         Map var4 = null;

         for (FeatureResult var6 : var1.getFeatureResults()) {
            if (var6.getFeature() != null && var6.getFeature().getResource() != null && var6.getFeature().getResource().getPath() != null) {
               if (var6.getFailedCount() > 0) {
                  var3 = var3 == null ? var6 : var3;
               } else {
                  var var7 = this.pageRunGreen(this.id(var6.getFeature().getResource().getPath()), var6.getFeature().getSourceHash(), Failing.passed(var6));
                  var4 = var4 == null ? var7 : var4;
               }
            }
         }

         if (var3 == null) {
            return var4;
         } else {
            var var8 = Failing.of(var1);
            Map var9 = var8 == null
               ? null
               : this.pageRunRed(
                  this.id(var3.getFeature().getResource().getPath()), var3.getFeature().getResource().getBytes(), var3.getFeature().getSourceHash(), var8, var2
               );
            return var4 != null ? var4 : var9;
         }
      }
   }

   Map<String, Object> pageRunGreen(TestId var1, String var2, List<Map<String, Object>> var3) {
      return this.locked(
         var1,
         () -> {
            io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4 = this.resolve(var1);
            var var5 = var4.b("land");
            if (var5 != null
               && Boolean.TRUE.equals(var5.get("bug"))
               && Objects.equals(var5.get("after"), var2)
               && var5.get("step") instanceof Map var6
               && !var3.stream().noneMatch(var1xx -> Failing.sameScenario(identity(var6), (Map<String, Object>)var1xx))) {
               var var10 = new LinkedHashMap<>(var4.e);
               retire(var10, var2);

               try {
                  this.persist(var1, var10);
                  return null;
               } catch (IOException var9) {
                  return ioError(var1, var9);
               }
            } else {
               return null;
            }
         }
      );
   }

   private static Map<String, Object> identity(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   Map<String, Object> pageRunRed(TestId var1, byte[] var2, String var3, Map<String, Object> var4, String var5) {
      if (!this.modelConfigured.getAsBoolean()) {
         return null;
      } else {
         Map<String, Object> var6 = this.admitted(() -> this.locked(var1, () -> {
            io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var6x = this.resolve(var1);
            if (var6x.b()) {
               return null;
            } else if (var3 != null && var3.equals(hashOf(var1.file())) && var3.equals(io.karatelabs.ext.common.Sha256.a(var2))) {
               LinkedHashMap var7 = var6x.e == null ? new LinkedHashMap() : new LinkedHashMap<>(var6x.e);
               if (knownBug(var7, var3, var4)) {
                  return Map.of("state", "bug");
               } else {
                  var7.put("state", "open");
                  var7.put("base", var3);
                  var7.put("candidate", var3);
                  var7.put("candidateBytes", encode(var2));
                  var7.put("runId", var5);
                  var7.put("failing", this.sanitized(var4));

                  try {
                     this.persist(var1, var7);
                  } catch (IOException var9) {
                     return ioError(var1, var9);
                  }

                  return Map.of("state", "open");
               }
            } else {
               return Map.of("state", "changed");
            }
         }));
         if (var6 != null && "open".equals(var6.get("state"))) {
            try {
               this.reportTurn.admit(var1, var5);
            } catch (RuntimeException var8) {
               logger.warn("the report turn for {} could not start: {}", var1.path(), var8.getMessage());
            }
         }

         return var6;
      }
   }

   public Map<String, Object> triage(TestId var1) {
      return this.locked(var1, () -> {
         io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var2 = this.resolve(var1);
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("path", var1.path());
         var3.put("state", var2.c);
         ArrayList var4 = new ArrayList();
         if (var2.h != null) {
            var3.putAll(var2.h);
            var3.put("answers", var4);
            return var3;
         } else {
            String var5 = var2.c;
            switch (var5) {
               case "lost":
                  var3.put("reason", var2.d);
                  var3.put("message", "The change the assistant was checking was lost: the console restarted or the test changed. Nothing was saved.");
                  var3.put("doc", var2.f.hash());
                  var4.add("discard");
                  break;
               case "running":
                  var3.put("runId", var2.a("runId"));
                  break;
               case "verifying":
                  var3.put("candidate", var2.a("candidate"));
                  var3.put("base", var2.a("base"));
                  var3.put("verify", var2.b("verify"));
                  break;
               case "open":
                  var3.put("failing", var2.b("failing"));
                  var3.put("question", "bug or test?");
                  var3.put("candidate", var2.a("candidate"));
                  var3.put("base", var2.a("base"));
                  var3.put("runId", var2.a("runId"));
                  if (var2.g) {
                     var3.put("unsaved", true);
                     var4.add("discard");
                  } else {
                     var4.addAll(List.of("bug", "test", "discard"));
                  }
                  break;
               case "answered":
                  var3.put("failing", var2.b("failing"));
                  var3.put("candidate", var2.a("candidate"));
                  var3.put("base", var2.a("base"));
                  var3.put("runId", var2.a("runId"));
                  var var7 = var2.b("answer");
                  var3.put("answer", var7);
                  if (var7 != null && var7.get("chat") != null) {
                     var3.put("chat", var7.get("chat"));
                  }

                  var var8 = var2.b("verify");
                  if (var8 != null) {
                     var3.put("verify", var8);
                     if ("interrupted".equals(var8.get("outcome"))) {
                        var3.put("message", "The console restarted while the suspected bug was being checked. Nothing was saved.");
                     }
                  }

                  boolean var9 = var7 != null && var7.get("runId") instanceof String var10 && this.live.contains(var10);
                  if (var9) {
                     var3.put("busy", true);
                  } else if ("bug".equals(var2.a())) {
                     if (var8 != null) {
                        var4.add("recheck");
                     }

                     var4.addAll(List.of("test", "discard"));
                  } else {
                     var4.addAll(List.of("test", "discard"));
                  }
            }

            var3.put("answers", var4);
            return var3;
         }
      });
   }

   public Map<String, Object> answer(TestId var1, Map<String, Object> var2) {
      String var3 = var2.get("answer") instanceof String var4 ? var4 : null;
      if (var3 != null && List.of("bug", "test", "discard").contains(var3)) {
         if (var2.get("doc") instanceof String var9) {
            return !"discard".equals(var3) ? refuse("BAD_REQUEST", "ApiTest.answer: doc goes with answer: 'discard' only") : this.locked(var1, () -> {
               try {
                  if (!this.store.deleteIfHash(var1, var9)) {
                     return refuse("STALE", "the chat store document for " + var1.path() + " changed — read ApiTest.triage again");
                  }
               } catch (IOException var4x) {
                  return ioError(var1, var4x);
               }

               this.unsaved.remove(var1);
               return discarded(var1);
            });
         } else {
            var var8 = this.locked(var1, () -> this.admit(var1, this.resolve(var1), var3, var2));
            if (var8 != null) {
               return var8;
            } else if ("discard".equals(var3)) {
               return this.locked(var1, () -> {
                  io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4x = this.resolve(var1);
                  var var5 = this.admit(var1, var4x, var3, var2);
                  if (var5 != null) {
                     return var5;
                  } else if (var4x.g) {
                     try {
                        this.store.delete(var1);
                     } catch (IOException var8x) {
                        return ioError(var1, var8x);
                     }

                     this.unsaved.remove(var1);
                     return discarded(var1);
                  } else {
                     var var6 = new LinkedHashMap<>(var4x.e);
                     TRIAGE_FIELDS.forEach(var6::remove);

                     try {
                        this.persist(var1, var6);
                     } catch (IOException var9x) {
                        return ioError(var1, var9x);
                     }

                     return discarded(var1);
                  }
               });
            } else {
               CandidateRunner.Claim var11;
               try {
                  var11 = this.runner.claim(var1, CandidateRunner.Purpose.ANSWER);
               } catch (CandidateRunner.Refused var7) {
                  return refuse(var7.code, var7.getMessage());
               }

               return this.started(
                  var11,
                  () -> this.locked(
                     var1,
                     () -> {
                        io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var5 = this.resolve(var1);
                        var var6 = this.admit(var1, var5, var3, var2);
                        if (var6 != null) {
                           return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(var6, null);
                        } else {
                           LinkedHashMap var7x = new LinkedHashMap();
                           var7x.put("choice", var3);
                           if (var2.get("ticket") instanceof String var8x && !var8x.isBlank()) {
                              var7x.put("ticket", var8x.trim());
                           }

                           var7x.put("chat", var11.chat());
                           var7x.put("turn", var11.turn());
                           var7x.put("runId", var11.runId());
                           var var11x = new LinkedHashMap<>(var5.e);
                           var11x.put("state", "answered");
                           var11x.put("answer", var7x);
                           var11x.remove("verify");

                           try {
                              this.persist(var1, var11x);
                           } catch (IOException var10) {
                              return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(ioError(var1, var10), null);
                           }

                           this.live.add(var11.runId());
                           LinkedHashMap var12 = new LinkedHashMap();
                           var12.put("path", var1.path());
                           var12.put("state", "answered");
                           var12.put("answer", var7x);
                           var12.put("runId", var11.runId());
                           return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(
                              var12,
                              new CandidateRunner.Request(
                                 var1,
                                 bytesOf(var11x.get("candidateBytes")),
                                 var5.a("base"),
                                 var5.a("candidate"),
                                 CandidateRunner.Purpose.ANSWER,
                                 var7x,
                                 var2xxx -> this.live.remove(var11.runId()),
                                 var5.b("failing")
                              )
                           );
                        }
                     }
                  )
               );
            }
         }
      } else {
         return refuse("BAD_REQUEST", "ApiTest.answer: answer is bug, test or discard");
      }
   }

   private Map<String, Object> started(CandidateRunner.Claim var1, Supplier<io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult> var2) {
      io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult var3 = null;

      try {
         var3 = (io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult)var2.get();
      } finally {
         if (var3 == null || var3.b == null) {
            var1.release();
         }
      }

      if (var3.b != null) {
         try {
            var1.start(var3.b);
         } catch (RuntimeException var7) {
            this.live.remove(var1.runId());
            var1.release();
            throw var7;
         }
      }

      return var3.a;
   }

   private static Map<String, Object> discarded(TestId var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("path", var0.path());
      var1.put("state", "none");
      var1.put("discarded", true);
      return var1;
   }

   private Map<String, Object> admit(TestId var1, io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var2, String var3, Map<String, Object> var4) {
      if (var2.h != null) {
         return var2.h;
      } else {
         String var5 = var2.c;
         switch (var5) {
            case "none":
               return refuse("NOT_FOUND", "no triage is open on " + var1.path());
            case "lost":
               var var7 = refuse(
                  "LOST",
                  "The change the assistant was checking was lost: the console restarted or the test changed. Nothing was saved. Its one answer is ApiTest.answer(path, {answer: 'discard', doc})."
               );
               var7.put("doc", var2.f.hash());
               return var7;
            case "running":
            case "verifying":
               return refusal(var1, var2);
            default:
               if (var2.g) {
                  return "discard".equals(var3) ? null : refuse("IO_ERROR", "the triage of " + var1.path() + " could not be saved — discard it");
               } else if (!Objects.equals(var4.get("candidate"), var2.a("candidate")) || !Objects.equals(var4.get("base"), var2.a("base"))) {
                  return refuse("STALE", "the candidate/base pair is not the triage's — read ApiTest.triage again");
               } else if ("answered".equals(var2.c) && "bug".equals(var3)) {
                  return refuse("STALE", var1.path() + " is already answered bug — ApiTest.recheck, test or discard");
               } else {
                  return "answered".equals(var2.c)
                        && var2.b("answer") != null
                        && var2.b("answer").get("runId") instanceof String var8
                        && this.live.contains(var8)
                     ? refuse("BUSY", "the chat is still answering for " + var1.path())
                     : null;
               }
         }
      }
   }

   public Map<String, Object> recheck(TestId var1, String var2) {
      var var3 = this.locked(var1, () -> admitRecheck(var1, this.resolve(var1), var2));
      if (var3 != null) {
         return var3;
      } else {
         CandidateRunner.Claim var4;
         try {
            var4 = this.runner.claim(var1, CandidateRunner.Purpose.VERIFY);
         } catch (CandidateRunner.Refused var6) {
            return refuse(var6.code, var6.getMessage());
         }

         return this.started(
            var4,
            () -> this.locked(
               var1,
               () -> {
                  io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4x = this.resolve(var1);
                  var var5 = admitRecheck(var1, var4x, var2);
                  if (var5 != null) {
                     return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(var5, null);
                  } else {
                     var var6x = new LinkedHashMap<>(var4x.e);
                     var6x.put("state", "verifying");
                     var6x.put("verify", new LinkedHashMap<>(Map.of("runId", var4.runId())));

                     try {
                        this.persist(var1, var6x);
                     } catch (IOException var8) {
                        return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(ioError(var1, var8), null);
                     }

                     this.live.add(var4.runId());
                     LinkedHashMap var7 = new LinkedHashMap();
                     var7.put("path", var1.path());
                     var7.put("state", "verifying");
                     var7.put("runId", var4.runId());
                     return new io.karatelabs.ext.max.apitest.TriageGate.TriageStartResult(
                        var7,
                        new CandidateRunner.Request(
                           var1,
                           bytesOf(var6x.get("candidateBytes")),
                           var4x.a("base"),
                           var2,
                           CandidateRunner.Purpose.VERIFY,
                           var4x.b("answer"),
                           var3xxx -> this.verificationDone(var1, var4.runId(), var3xxx),
                           var4x.b("failing")
                        )
                     );
                  }
               }
            )
         );
      }
   }

   private static Map<String, Object> admitRecheck(TestId var0, io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var1, String var2) {
      if (var1.h != null) {
         return var1.h;
      } else if ("none".equals(var1.c)) {
         return refuse("NOT_FOUND", "no triage is open on " + var0.path());
      } else if ("running".equals(var1.c) || "verifying".equals(var1.c) || "lost".equals(var1.c)) {
         return refusal(var0, var1);
      } else if (!"answered".equals(var1.c) || !"bug".equals(var1.a()) || var1.b("verify") == null) {
         return refuse("STALE", var0.path() + " has no verification to check again");
      } else {
         return var2 != null && var2.equals(var1.a("candidate")) ? null : refuse("STALE", "candidate is not the triage's — read ApiTest.triage again");
      }
   }

   public Map<String, Object> change(TestId var1) {
      return this.locked(
         var1,
         () -> {
            io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var2 = this.resolve(var1);
            var var3 = var2.b("land");
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("path", var1.path());
            if (var3 == null) {
               var4.put("undoable", false);
               var4.put("reason", "the chat has made no change to " + var1.path());
               return var4;
            } else {
               var4.put("before", var3.get("before"));
               var4.put("after", var3.get("after"));
               var4.put("runId", var3.get("runId"));
               var4.put("weakening", var3.get("weakening") instanceof List var5 ? var5 : List.of());
               if (Boolean.TRUE.equals(var3.get("bug"))) {
                  var4.put("bug", true);
               }

               String var7 = var2.h != null
                  ? String.valueOf(var2.h.get("error"))
                  : (
                     var2.b()
                        ? "a triage is open"
                        : (!Objects.equals(var3.get("after"), hashOf(var1.file())) ? "the test changed since the chat's change" : null)
                  );
               var4.put("undoable", var7 == null);
               if (var7 != null) {
                  var4.put("reason", var7);
               }

               return var4;
            }
         }
      );
   }

   public void followTags(Path var1, String var2, String var3) {
      TestId var4 = this.id(var1);
      this.locked(var4, () -> {
         io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var4x = this.resolve(var4);
         var var5 = var4x.b("land");
         if (var5 != null && !var4x.b() && Objects.equals(var5.get("after"), var2)) {
            var var6 = new LinkedHashMap<>(var5);
            var6.put("after", var3);
            var var7 = new LinkedHashMap<>(var4x.e);
            var7.put("land", var6);

            try {
               this.persist(var4, var7);
            } catch (IOException var9) {
            }

            return null;
         } else {
            return null;
         }
      });
   }

   public Map<String, Object> undo(TestId var1, String var2) {
      var var3 = this.locked(var1, () -> refusal(var1, this.resolve(var1)));
      if (var3 != null) {
         return var3;
      } else if (!RepoLock.acquire(this.root)) {
         return refuse("BUSY", "another write holds the project — retry");
      } else {
         Map var4;
         try {
            var4 = FeatureStore.locked(
               var1.file(),
               () -> this.locked(
                  var1,
                  () -> {
                     io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var3x = this.resolve(var1);
                     if (var3x.b()) {
                        return refusal(var1, var3x);
                     } else {
                        var var4x = var3x.b("land");
                        if (var4x != null && var2 != null && var2.equals(var4x.get("after"))) {
                           if (!var2.equals(hashOf(var1.file()))) {
                              return refuse("STALE", var1.path() + " changed since the chat's change — nothing to undo");
                           } else {
                              byte[] var5 = bytesOf(var4x.get("beforeBytes"));
                              if (var4x.get("before") != null && var5 == null) {
                                 return refuse("IO_ERROR", "the chat store record of " + var1.path() + " holds no readable bytes to restore");
                              } else {
                                 Map var6 = var4x.get("before") == null ? FeatureStore.delete(var1.file(), var2) : FeatureStore.write(var1.file(), var5, var2);
                                 if (var6 != null) {
                                    return var6;
                                 } else {
                                    Runnable var7x = this.afterSourceMoved;
                                    if (var7x != null) {
                                       var7x.run();
                                    }

                                    var var8 = new LinkedHashMap<>(var3x.e);
                                    var8.remove("land");
                                    LinkedHashMap var9 = new LinkedHashMap();
                                    var9.put("path", var1.path());
                                    var9.put("restored", var4x.get("before"));
                                    var9.put("deleted", var4x.get("before") == null);

                                    try {
                                       this.persist(var1, var8);
                                    } catch (IOException var11) {
                                       var9.putAll(ioError(var1, var11));
                                    }

                                    if (this.sessions != null) {
                                       this.sessions.invalidate(var1.path(), "the test was undone");
                                    }

                                    return var9;
                                 }
                              }
                           }
                        } else {
                           return refuse(
                              "STALE",
                              var4x == null ? "the chat has made no change to " + var1.path() : "after is not the last change's — read ApiTest.change again"
                           );
                        }
                     }
                  }
               )
            );
         } finally {
            RepoLock.release(this.root);
         }

         return var4;
      }
   }

   public byte[] candidateBytes(TestId var1) {
      return this.locked(var1, () -> {
         io.karatelabs.ext.max.apitest.TriageGate.ResolvedTriageState var2 = this.resolve(var1);
         return var2.e == null ? null : bytesOf(var2.e.get("candidateBytes"));
      });
   }

   public interface ReportTurn {
      void admit(TestId var1, String var2);
   }

   private record TriageIndex(List<TestId> a, Map<String, Object> b) {

   }

   private record TriageStartResult(Map<String, Object> a, CandidateRunner.Request b) {

   }

   private record ResolvedTriageState(String c, String d, Map<String, Object> e, ChatStore.Doc f, boolean g, Map<String, Object> h) {

      ResolvedTriageState(String var1, String var2, Map<String, Object> var3, ChatStore.Doc var4, boolean var5) {
         this(var1, var2, var3, var4, var5, null);
      }


      String a(String var1) {
         return this.e != null && this.e.get(var1) instanceof String var2 ? var2 : null;
      }

      Map<String, Object> b(String var1) {
         return this.e != null && this.e.get(var1) instanceof Map var2 ? var2 : null;
      }

      String a() {
         var var1 = this.b("answer");
         return var1 == null ? null : String.valueOf(var1.get("choice"));
      }

      boolean b() {
         return this.h != null || !"none".equals(this.c);
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public Map<String, Object> e() {
         return this.e;
      }

      public ChatStore.Doc f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }

      public Map<String, Object> h() {
         return this.h;
      }
   }
}
