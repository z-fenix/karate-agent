package io.karatelabs.ext.robot.recording;

import io.karatelabs.driver.cdp.CdpEventListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrafficRecorder implements CdpEventListener {
   private static final Logger c = LoggerFactory.getLogger(TrafficRecorder.class);
   public static final String a = "traffic.jsonl";
   static final double b = 50.0;
   private static final Set<String> d = Set.of(
      "Network.requestWillBeSentExtraInfo",
      "Network.responseReceived",
      "Network.responseReceivedExtraInfo",
      "Network.loadingFinished",
      "Network.loadingFailed",
      "Network.webSocketWillSendHandshakeRequest",
      "Network.webSocketHandshakeResponseReceived",
      "Network.webSocketClosed"
   );
   private final Path e;
   private final io.karatelabs.ext.robot.recording.TrafficRecorder.TrafficRecorderLimits f;
   private final io.karatelabs.ext.robot.recording.CdpPageApi g;
   private final io.karatelabs.ext.agent.llm.SecretRedactor h;
   private final io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry i = new io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry();
   private final Object j = new Object();
   private io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState k = io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.OPEN;
   private final Map<String, io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest> l = new LinkedHashMap<>();
   private long m;
   private int n;
   private int o;
   private int p;
   private final AtomicInteger q = new AtomicInteger();
   private volatile boolean r;
   private final ExecutorService s = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1x = new Thread(var0, "traffic-capture");
      var1x.setDaemon(true);
      return var1x;
   });
   private final Map<String, io.karatelabs.ext.robot.recording.RecordedExchange> t = new LinkedHashMap<>();
   private final ArrayDeque<Runnable> u = new ArrayDeque<>();
   private int v;
   private long w;
   private boolean x;
   private double y;
   private String z;
   private String A;
   private boolean B;
   private final List<Map<String, Object>> C = new ArrayList<>();
   private final Map<String, String[]> D = new LinkedHashMap<>();
   private volatile boolean E;
   private final CompletableFuture<Void> F = new CompletableFuture<>();
   private final TreeMap<Integer, io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent> G = new TreeMap<>();
   private final Map<Integer, String> H = new LinkedHashMap<>();
   private boolean I;
   private Map<String, Object> J;

   public TrafficRecorder(Path var1, io.karatelabs.ext.robot.recording.TrafficRecorder.TrafficRecorderLimits var2, io.karatelabs.ext.robot.recording.CdpPageApi var3, io.karatelabs.ext.agent.llm.SecretRedactor var4) {
      this.e = var1;
      this.f = var2;
      this.g = var3;
      this.h = var4;
   }

   public Path a() {
      return this.e.resolve("traffic.jsonl");
   }

   @Override
   public void onEvent(String var1, Map<String, Object> var2) {
      if (var2 != null && (var1.startsWith("Network.") || var1.startsWith("Target."))) {
         synchronized (this.j) {
            if (this.k != io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.SEALED) {
               switch (var1) {
                  case "Network.requestWillBeSent":
                     this.a(var1, var2, var2.get("redirectResponse") != null);
                     break;
                  case "Network.webSocketCreated":
                     this.a(var1, var2, false);
                     break;
                  default:
                     if (var1.startsWith("Target.")) {
                        if (!this.a(var1, var2)) {
                           this.p++;
                        }

                        return;
                     }

                     if (!d.contains(var1)) {
                        return;
                     }

                     io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest var6 = this.l.get(String.valueOf(var2.get("requestId")));
                     if (var6 == null) {
                        if (this.k == io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.OPEN && !this.a(var1, var2)) {
                           this.p++;
                        }

                        return;
                     }

                     if (var6.c || var6.d) {
                        return;
                     }

                     if (!this.a(var1, var2)) {
                        var6.e.add(var1);
                        this.p++;
                     }
               }
            }
         }
      }
   }

   private void a(String var1, Map<String, Object> var2, boolean var3) {
      String var4 = String.valueOf(var2.get("requestId"));
      io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest var5 = this.l.get(var4);
      if (var5 == null) {
         if (this.k != io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.OPEN) {
            return;
         }

         var5 = new io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest(this.m++);
         this.l.put(var4, var5);
         if (this.n >= this.f.c) {
            var5.c = true;
         } else {
            this.n++;
         }
      } else if (!var3 || var5.a(var2)) {
         return;
      }

      var5.b(var2);
      if (!var5.c && !var5.d) {
         if (!this.a(var1, var2)) {
            var5.d = true;
            this.o++;
         }
      }
   }

   private boolean a(String var1, Map<String, Object> var2) {
      if (this.q.incrementAndGet() > this.f.d) {
         this.q.decrementAndGet();
         return false;
      } else {
         try {
            this.s.execute(() -> {
               this.q.decrementAndGet();
               if (!this.r) {
                  try {
                     this.b(var1, var2);
                  } catch (RuntimeException var4x) {
                     c.warn("traffic event {} not applied", var1, var4x);
                  }
               }

               this.d();
            });
            return true;
         } catch (RejectedExecutionException var4) {
            this.q.decrementAndGet();
            return false;
         }
      }
   }

   public void b() {
      synchronized (this.j) {
         if (this.k == io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.OPEN) {
            this.k = io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.CLOSING;
         }
      }
   }

   public void a(String var1, String var2, List<Map<String, Object>> var3, List<String> var4) {
      this.s.execute(() -> {
         this.z = var1;
         this.A = var2;
         this.B = true;

         for (Map var6 : this.C) {
            this.a(var6);
         }

         this.C.clear();

         for (Map var10 : var3) {
            this.a(var10);
         }

         for (String var11 : var4) {
            String var7 = a(var11);
            if (var7 != null) {
               this.D.put("sw:" + var7, new String[]{"serviceWorkers", var7});
            }
         }
      });
   }

   private void a(Map<String, Object> var1) {
      if (var1 != null) {
         String var2 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("targetId"));
         String var3 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("type"));
         String var4 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("browserContextId"));
         if (var2 != null && !var2.equals(this.z)) {
            String var6 = var3 == null ? "" : var3;

            String var5 = switch (var6) {
               case "page" -> "tabs";
               case "iframe" -> "frames";
               case "service_worker" -> "serviceWorkers";
               default -> null;
            };
            if (var5 != null && (this.A == null || var4 == null || this.A.equals(var4))) {
               var6 = a(io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("url")));
               if (var6 != null) {
                  this.D.put(var2, new String[]{var5, var6});
               }
            }
         }
      }
   }

   static String a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            URI var1 = URI.create(var0);
            return var1.getScheme() != null && var1.getHost() != null
               ? var1.getScheme() + "://" + var1.getHost() + (var1.getPort() < 0 ? "" : ":" + var1.getPort())
               : null;
         } catch (IllegalArgumentException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   public void a(io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent var1) {
      synchronized (this.G) {
         if (!this.I) {
            this.G.put(var1.a, var1);
         }
      }
   }

   public void a(int var1, String var2) {
      synchronized (this.G) {
         if (!this.I) {
            this.H.merge(var1, var2, (var0, var1x) -> var0 + "\n" + var1x);
         }
      }
   }

   public io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult a(String var1, io.karatelabs.ext.common.secrets.SecretStore var2) {
      if (this.r) {
         return new io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult("‹masked›", "the recording was finalized before the action was admitted");
      } else if (var2 == null) {
         return new io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult(this.i.aliasFor(var1), "no secret store");
      } else {
         try {
            String var3 = io.karatelabs.ext.common.secrets.SecretExtractor.a("API_PASSWORD", var1, var2.e());
            var2.a(var3, var1);
            return this.r
               ? new io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult(
                  "‹masked›",
                  "the recording was finalized while the password was being stored (it is in the store as "
                     + var3
                     + "); the action is not part of the recording"
               )
               : new io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult("secret('" + var3 + "')", null);
         } catch (UncheckedIOException | IllegalStateException var4) {
            return new io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult(this.r ? "‹masked›" : this.i.aliasFor(var1), var4.getMessage());
         }
      }
   }

   private void b(String var1, Map<String, Object> var2) {
      if (var1.startsWith("Target.")) {
         Map var10 = (Map)var2.get("targetInfo");
         if (this.B) {
            this.a(var10);
         } else if (var10 != null) {
            this.C.add(var10);
         }
      } else {
         String var3 = String.valueOf(var2.get("requestId"));
         io.karatelabs.ext.robot.recording.RecordedExchange var4 = this.t.computeIfAbsent(var3, io.karatelabs.ext.robot.recording.RecordedExchange::new);
         switch (var1) {
            case "Network.requestWillBeSent":
               this.a(var4, var2);
               break;
            case "Network.requestWillBeSentExtraInfo":
               var4.d.add(var2);
               break;
            case "Network.responseReceived":
               io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var14 = var4.a();
               if (var14 == null || var14.d != null) {
                  return;
               }

               var14.d = (Map<String, Object>)var2.get("response");
               var14.e = Boolean.TRUE.equals(var2.get("hasExtraInfo"));
               var4.n = a(var2.get("timestamp"));
               break;
            case "Network.responseReceivedExtraInfo":
               var4.e.add(var2);
               break;
            case "Network.loadingFinished":
               if (var4.j) {
                  return;
               }

               var4.j = true;
               var4.m = var2.get("encodedDataLength") instanceof Number var13 ? var13.longValue() : null;
               if (var4.f() && !var4.k && var4.d() != null) {
                  this.a(var4);
               }
               break;
            case "Network.loadingFailed":
               var4.k = true;
               var4.l = io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get("errorText"));
               break;
            case "Network.webSocketCreated":
               var4.f = true;
               var4.g = "WebSocket";
               var4.h = b(var2);
               if (var4.c.isEmpty()) {
                  io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var12 = new io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState();
                  var12.a = new LinkedHashMap<>(Map.of("url", String.valueOf(var2.get("url")), "method", "GET"));
                  var4.c.add(var12);
               }
               break;
            case "Network.webSocketWillSendHandshakeRequest":
               io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var11 = var4.b();
               if (var11 != null && var2.get("request") instanceof Map var8) {
                  var var16 = new LinkedHashMap<>(var11.a);
                  var16.put("headers", var8.get("headers"));
                  var11.a = var16;
                  var11.b = a(var2.get("timestamp"));
                  var11.c = a(var2.get("wallTime"));
               }
               break;
            case "Network.webSocketHandshakeResponseReceived":
               io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var7 = var4.b();
               if (var7 != null && var7.d == null) {
                  var7.d = (Map<String, Object>)var2.get("response");
                  var4.n = a(var2.get("timestamp"));
               }
         }
      }
   }

   private void a(io.karatelabs.ext.robot.recording.RecordedExchange var1, Map<String, Object> var2) {
      Map var3 = (Map)var2.get("redirectResponse");
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var4 = var1.a();
      if (var3 != null && var4 != null) {
         if (var4.d != null || var1.a((Map<String, Object>)var2.get("request"), a(var2.get("timestamp")), var3)) {
            return;
         }

         var4.d = var3;
         var4.f = true;
         var4.e = Boolean.TRUE.equals(var2.get("redirectHasExtraInfo"));
      } else if (var4 != null) {
         return;
      }

      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var5 = new io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState();
      var5.a = (Map<String, Object>)var2.get("request");
      var5.b = a(var2.get("timestamp"));
      var5.c = a(var2.get("wallTime"));
      var1.c.add(var5);
      if (var1.c.size() == 1) {
         var1.g = io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get("type"));
         var1.h = b(var2);
         var1.i = io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get("frameId"));
         if ("Document".equals(var1.g)) {
            this.y = var5.b;
         }

         this.a(var1, var5);
      }
   }

   private static String b(Map<String, Object> var0) {
      return var0.get("initiator") instanceof Map var1 ? io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("type")) : null;
   }

   private static double a(Object var0) {
      return var0 instanceof Number var1 ? var1.doubleValue() : 0.0;
   }

   private void a(io.karatelabs.ext.robot.recording.RecordedExchange var1, io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var2) {
      Map var3 = var2.a == null ? Map.of() : var2.a;
      String var4 = io.karatelabs.ext.robot.recording.RecordedExchange.a(io.karatelabs.ext.robot.recording.RecordedExchange.b((Map<String, Object>)var3.get("headers"), (Map<String, Object>)null), "content-type");
      Object var5 = var3.get("postData");
      if (var5 != null) {
         var1.o = this.a(String.valueOf(var5), var4);
      } else if (Boolean.TRUE.equals(var3.get("hasPostData"))) {
         var1.q = true;
         this.a(() -> this.g.b(var1.b), (var3x, var4x) -> {
            var1.q = false;
            var1.o = var4x == null && var3x != null ? this.a(var3x, var4) : io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("not-recoverable", null, var4);
         });
      } else {
         var1.o = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a;
      }
   }

   private void a(io.karatelabs.ext.robot.recording.RecordedExchange var1) {
      var var2 = var1.d();
      String var3 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var2.get("mimeType"));
      var1.r = true;
      this.a(() -> this.g.a(var1.b), (var3x, var4) -> {
         var1.r = false;
         if (var4 instanceof io.karatelabs.ext.robot.recording.CdpPageApi.CdpPageException) {
            var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a(var1.n < this.y ? "navigation" : "evicted", var1.m, var3);
         } else if (var4 != null || var3x == null) {
            var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("fetch-failed", var1.m, var3);
         } else if (!var3x.b() && io.karatelabs.ext.robot.recording.RecordedExchange.c(var3)) {
            var1.p = this.a(var3x.a(), var3);
         } else {
            var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.a(var3x.a(), var3x.b(), var3);
         }
      });
   }

   private io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody a(String var1, String var2) {
      io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody var3 = io.karatelabs.ext.robot.recording.RecordedExchange.a(var1, var2, this.f.component1, this.x ? -1L : this.f.b - this.w);
      if ("full".equals(var3.a())) {
         this.w = this.w + var3.c();
      } else if ("capped".equals(var3.a()) && var3.c() <= this.f.component1) {
         this.x = true;
      }

      return var3;
   }

   private <T> void a(Supplier<CompletableFuture<T>> var1, io.karatelabs.ext.robot.recording.TrafficRecorder.AsyncCompletionHandler<T> var2) {
      this.u.add(() -> {
         CompletableFuture<T> var3;
         try {
            var3 = var1.get();
         } catch (RuntimeException var5) {
            var3 = CompletableFuture.failedFuture(var5);
         }

         var3.whenComplete((var2xx, var3x) -> {
            try {
               this.s.execute(() -> {
                  this.v--;
                  if (!this.r) {
                     var2.apply(var2xx, var3x instanceof CompletionException var4 ? var4.getCause() : var3x);
                  }

                  this.c();
                  this.d();
               });
            } catch (RejectedExecutionException var5x) {
            }
         });
      });
      this.c();
   }

   void a(Runnable var1) {
      this.s.execute(var1);
   }

   private void c() {
      while (this.v < this.f.e && !this.u.isEmpty()) {
         this.v++;
         this.u.poll().run();
      }
   }

   private void d() {
      if (this.E && !this.F.isDone() && this.e()) {
         this.F.complete(null);
      }
   }

   private boolean e() {
      if (this.q.get() <= 0 && this.v <= 0 && this.u.isEmpty()) {
         for (io.karatelabs.ext.robot.recording.RecordedExchange var2 : this.t.values()) {
            if (!var2.c.isEmpty() && var2.j()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public Map<String, Object> b(String var1) {
      return this.a(var1, List.of());
   }

   public synchronized Map<String, Object> a(String var1, List<String> var2) {
      if (this.J != null) {
         return this.J;
      } else {
         this.E = true;
         this.s.execute(this::d);

         try {
            this.F.get(this.f.f, TimeUnit.MILLISECONDS);
         } catch (TimeoutException var19) {
         } catch (InterruptedException var20) {
            Thread.currentThread().interrupt();
         } catch (ExecutionException var21) {
            c.warn("traffic drain failed: {}", var21.getCause().toString());
         }

         synchronized (this.j) {
            this.k = io.karatelabs.ext.robot.recording.TrafficRecorder.RecorderState.SEALED;
            this.r = true;
         }

         CaptureArtifactWriter var22;
         try {
            var22 = this.s.submit(this::f).get();
         } catch (InterruptedException var15) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted finalizing the traffic capture", var15);
         } catch (ExecutionException var16) {
            throw new IllegalStateException("traffic capture finalization failed: " + var16.getCause(), var16.getCause());
         } finally {
            this.s.shutdown();
         }

         this.J = var22.a(this.a(), var1, var2, this.h, this.i);

         try {
            io.karatelabs.ext.robot.recording.CaptureArtifactWriter.a(this.e.getParent(), this.f.g(), this.f.h());
         } catch (RuntimeException | IOException var14) {
            c.warn("traffic logs not pruned: {}", var14.toString());
         }

         return this.J;
      }
   }

   private CaptureArtifactWriter f() {
      ArrayList var1 = new ArrayList();
      long var2 = 0L;
      boolean var4 = false;
      Double var5 = null;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      boolean var9 = false;
      synchronized (this.j) {
         for (Entry var12 : this.l.entrySet()) {
            io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest var13 = (io.karatelabs.ext.robot.recording.TrafficRecorder.PendingRedirectRequest)var12.getValue();
            var2 += var13.b;
            io.karatelabs.ext.robot.recording.RecordedExchange var14 = this.t.get(var12.getKey());
            String var15 = var13.c ? "over-limit" : (var13.d ? "queue" : (var14 != null && var14.c.size() >= var13.b ? null : "drain"));
            if (var15 != null) {
               var1.add(io.karatelabs.ext.robot.recording.CaptureArtifactWriter.a((String)var12.getKey(), var15, var13.b));
            } else {
               var14.s = var13.e;
               this.b(var14);
               if (var14.g()) {
                  var7++;
               } else if (var14.j || var14.k) {
                  var6++;
               } else if (var14.i() && !var14.s.contains("Network.responseReceived")) {
                  var8++;
               }

               if (var14.f() && ("unavailable".equals(var14.o.a()) || "unavailable".equals(var14.p.a()))) {
                  var9 = true;
               }

               for (io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var17 : var14.c) {
                  if (var17.c > 0.0 && var17.b > 0.0) {
                     double var18 = var17.c * 1000.0 - var17.b * 1000.0;
                     if (var5 == null) {
                        var5 = var18;
                     } else if (Math.abs(var18 - var5) > 50.0) {
                        var4 = true;
                     }
                  }
               }

               var1.add(var14.a(this.z));
            }
         }
      }

      ArrayList var26 = new ArrayList();
      boolean var27 = false;
      Double var28 = null;
      ArrayList var29;
      synchronized (this.G) {
         this.I = true;
         var29 = new ArrayList<>(this.G.values());

         for (io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent var36 : (Iterable<io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent>) (Iterable<?>) (var29)) {
            var26.add(io.karatelabs.ext.robot.recording.CaptureArtifactWriter.a(var36, this.H.get(var36.a)));
            double var39 = var36.c - var36.b;
            if (var28 == null) {
               var28 = var39;
            } else if (Math.abs(var39 - var28) > 50.0) {
               var27 = true;
            }
         }
      }

      for (Map var34 : (Iterable<Map>) (Iterable<?>) (var1)) {
         if (var34.get("start") instanceof Double var37) {
            int var41 = 0;
            long var43 = Long.MIN_VALUE;
            double var20 = Double.NEGATIVE_INFINITY;

            for (io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent var23 : (Iterable<io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent>) (Iterable<?>) (var29)) {
               if (var23.c <= var37 && (var23.c > var43 || var23.c == var43 && var23.b >= var20)) {
                  var41 = var23.a;
                  var43 = var23.c;
                  var20 = var23.b;
               }
            }

            var34.put("after", var41);
         }
      }

      LinkedHashMap var32 = new LinkedHashMap();
      var32.put("tabs", new ArrayList());
      var32.put("frames", new ArrayList());
      var32.put("serviceWorkers", new ArrayList());

      for (String[] var38 : this.D.values()) {
         List var42 = (List)var32.get(var38[0]);
         if (!var42.contains(var38[1])) {
            var42.add(var38[1]);
         }
      }

      return new CaptureArtifactWriter(var26, var1, var2, this.o, this.p, var32, var4 || var27, new io.karatelabs.ext.robot.recording.CaptureArtifactWriter.CaptureCompletionStats(var6, var7, var8, var9));
   }

   private void b(io.karatelabs.ext.robot.recording.RecordedExchange var1) {
      if (var1.f) {
         var1.o = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a;
         var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a;
      } else {
         if (var1.o == null || var1.q) {
            var1.o = var1.q ? io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("drain", null, null) : io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a;
         }

         if (var1.r) {
            var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("drain", var1.m, c(var1));
         } else if (var1.p == null) {
            if (var1.d() != null && (!var1.k || !var1.h())) {
               if (!var1.f()) {
                  var1.p = new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("skipped", null, var1.m, c(var1), null);
               } else if (var1.k) {
                  var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("aborted", var1.m, c(var1));
               } else if (var1.s.contains("Network.loadingFinished")) {
                  var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("dropped", null, c(var1));
               } else {
                  var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a("drain", var1.m, c(var1));
               }
            } else {
               var1.p = io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a;
            }
         }
      }
   }

   private static String c(io.karatelabs.ext.robot.recording.RecordedExchange var0) {
      var var1 = var0.d();
      return var1 == null ? null : io.karatelabs.ext.robot.recording.RecordedExchange.a(var1.get("mimeType"));
   }

   io.karatelabs.ext.robot.recording.RecordedExchange c(String var1) {
      try {
         return this.s.isShutdown() ? this.t.get(var1) : this.s.<io.karatelabs.ext.robot.recording.RecordedExchange>submit(() -> this.t.get(var1)).get();
      } catch (ExecutionException | InterruptedException var3) {
         throw new IllegalStateException(var3);
      }
   }

   io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent a(int var1) {
      synchronized (this.G) {
         return this.G.get(var1);
      }
   }

   public record QueuedRecorderEvent(int a, double b, long c, Map<String, Object> d) {

   }

   public record SecretResolutionResult(String a, String b) {

   }

   private interface AsyncCompletionHandler<T> {
      void apply(T var1, Throwable var2);
   }

   public record TrafficRecorderLimits(long component1, long b, int c, int d, int e, long f, int g, long h) {
      public static final io.karatelabs.ext.robot.recording.TrafficRecorder.TrafficRecorderLimits a = new io.karatelabs.ext.robot.recording.TrafficRecorder.TrafficRecorderLimits(1048576L, 67108864L, 2000, 10000, 6, 3000L, 20, 268435456L);


      public long a() {
         return this.component1;
      }

      public long b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public int e() {
         return this.e;
      }

      public long f() {
         return this.f;
      }

      public int g() {
         return this.g;
      }

      public long h() {
         return this.h;
      }
   }

   private static final class PendingRedirectRequest {
      final long a;
      int b;
      boolean c;
      boolean d;
      final Set<String> e = new LinkedHashSet<>();
      Object f;
      Object g;
      double h;
      Map<String, Object> i;

      PendingRedirectRequest(long var1) {
         this.a = var1;
      }

      boolean a(Map<String, Object> var1) {
         return this.b > 1
            && this.h == io.karatelabs.ext.robot.recording.TrafficRecorder.a(var1.get("timestamp"))
            && var1.get("request") instanceof Map var2
            && Objects.equals(this.f, var2.get("url"))
            && Objects.equals(this.g, var2.get("method"))
            && io.karatelabs.ext.robot.recording.RecordedExchange.a(this.i, (Map<String, Object>)var1.get("redirectResponse"));
      }

      void b(Map<String, Object> var1) {
         this.b++;
         this.h = io.karatelabs.ext.robot.recording.TrafficRecorder.a(var1.get("timestamp"));
         this.f = var1.get("request") instanceof Map var2 ? var2.get("url") : null;
         this.g = var1.get("request") instanceof Map var4 ? var4.get("method") : null;
         this.i = (Map<String, Object>)var1.get("redirectResponse");
      }
   }

   private static enum RecorderState {
      OPEN,
      CLOSING,
      SEALED;
   }
}
