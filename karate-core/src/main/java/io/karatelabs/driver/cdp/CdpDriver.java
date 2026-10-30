package io.karatelabs.driver.cdp;

import io.karatelabs.driver.BaseElement;
import io.karatelabs.driver.Dialog;
import io.karatelabs.driver.DialogHandler;
import io.karatelabs.driver.Driver;
import io.karatelabs.driver.DriverApi;
import io.karatelabs.driver.DriverException;
import io.karatelabs.driver.DriverOptions;
import io.karatelabs.driver.Element;
import io.karatelabs.driver.Finder;
import io.karatelabs.driver.InterceptHandler;
import io.karatelabs.driver.InterceptRequest;
import io.karatelabs.driver.InterceptResponse;
import io.karatelabs.driver.Keys;
import io.karatelabs.driver.Locators;
import io.karatelabs.driver.Mouse;
import io.karatelabs.driver.PageLoadStrategy;
import io.karatelabs.js.Terms;
import io.karatelabs.output.LogContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;

public class CdpDriver implements Driver {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final String DRIVER_JS = loadResource("driver.js");
   private static final Set<CdpDriver> ACTIVE = ConcurrentHashMap.newKeySet();
   private final CdpClient cdp;
   private final CdpDriverOptions options;
   private final CdpLauncher launcher;
   private volatile boolean domContentEventFired;
   private final Set<String> framesStillLoading = ConcurrentHashMap.newKeySet();
   private volatile String mainFrameId;
   private volatile String pendingNavigationUrl;
   private volatile String expectedLoaderId;
   private volatile String supersededLoaderId;
   private volatile String domContentLoaderId;
   private volatile String committedLoaderId;
   private volatile String preNavCommittedLoaderId;
   private volatile String historyTargetUrl;
   private volatile CompletableFuture<Void> loadTick = new CompletableFuture<>();
   private volatile boolean terminated = false;
   private volatile String currentDialogText;
   private volatile CdpDialog currentDialog;
   private volatile DialogHandler dialogHandler;
   private volatile InterceptHandler interceptHandler;
   private CdpDriver.Frame currentFrame;
   private final Map<String, Integer> frameContexts = new ConcurrentHashMap<>();
   private final Map<String, CompletableFuture<Integer>> frameContextReady = new ConcurrentHashMap<>();
   private volatile CompletableFuture<Integer> mainContextReady = new CompletableFuture<>();
   private static final long CONTEXT_READY_POLL_MS = 2000L;
   private static final Duration FAILURE_SCREENSHOT_TIMEOUT = Duration.ofSeconds(5L);
   private static final int NETWORK_RESOURCE_BUFFER_BYTES = 8388608;
   private static final int NETWORK_TOTAL_BUFFER_BYTES = 134217728;
   private final Map<String, Map<String, Object>> oopifTargets = new ConcurrentHashMap<>();
   private final Map<String, String> oopifSessions = new ConcurrentHashMap<>();
   private volatile String pageSessionId;
   private String currentTargetId;
   private volatile String ownedBrowserContextId;
   private volatile String browserWebSocketUrl;
   private volatile String browserContextId;
   private final Set<String> knownTargetIds = ConcurrentHashMap.newKeySet();
   private final Queue<Map<String, Object>> openedTargets = new ConcurrentLinkedQueue<>();
   private final Map<String, CompletableFuture<Void>> pendingTargetRemovals = new ConcurrentHashMap<>();
   private final Map<String, CdpDriver.InitScript> initScripts = new LinkedHashMap<>();
   private final Object initScriptLock = new Object();
   private String runtimeInitId;
   private CdpKeys keysInstance;
   private static final long LOCAL_NAV_COMMIT_TIMEOUT_MS = 2000L;
   private static final int ELEMENT_ACTION_ATTEMPTS = 3;

   private static String loadResource(String name) {
      try {
         String var2;
         try (InputStream is = CdpDriver.class.getResourceAsStream("/io/karatelabs/driver/" + name)) {
            var2 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
         }

         return var2;
      } catch (Exception var6) {
         throw new RuntimeException("Failed to load " + name, var6);
      }
   }

   public void addCdpEventListener(CdpEventListener listener) {
      this.cdp.addExternalListener(listener);
   }

   public void removeCdpEventListener(CdpEventListener listener) {
      this.cdp.removeExternalListener(listener);
   }

   public void awaitEventsDispatched(Runnable atFence) {
      this.cdp.awaitEventsDispatched(atFence);
   }

   public void awaitEventsDispatched() {
      this.cdp.awaitEventsDispatched(null);
   }

   public String addScriptToEvaluateOnNewDocument(String source) {
      CdpResponse response = this.cdp.method("Page.addScriptToEvaluateOnNewDocument").param("source", source).send();
      return scriptIdentifier(response);
   }

   public void removeScriptToEvaluateOnNewDocument(String identifier) {
      this.cdp.method("Page.removeScriptToEvaluateOnNewDocument").param("identifier", identifier).send();
   }

   public void addBinding(String name) {
      this.cdp.method("Runtime.addBinding").param("name", name).send();
   }

   public void removeBinding(String name) {
      this.cdp.method("Runtime.removeBinding").param("name", name).send();
   }

   static String scriptIdentifier(CdpResponse response) {
      return response == null ? null : response.getResultAsString("identifier");
   }

   public void addInitScript(String name, String source, String... deps) {
      synchronized (this.initScriptLock) {
         if (!this.initScripts.containsKey(name)) {
            for (String dep : deps) {
               if (!this.initScripts.containsKey(dep)) {
                  throw new DriverException("init script '" + name + "' depends on unregistered '" + dep + "'");
               }
            }

            if (this.runtimeInitId == null) {
               this.runtimeInitId = this.addScriptToEvaluateOnNewDocument(DRIVER_JS);
            }

            String cdpId = this.addScriptToEvaluateOnNewDocument(source);
            this.initScripts.put(name, new CdpDriver.InitScript(source, List.of(deps), cdpId));

            try {
               this.ensureKjsRuntime();
               this.evalDirect(source);
            } catch (Exception var10) {
               logger.debug("init script '{}' inject into current document deferred: {}", name, var10.getMessage());
            }
         }
      }
   }

   public void removeInitScript(String name) {
      synchronized (this.initScriptLock) {
         CdpDriver.InitScript script = this.initScripts.remove(name);
         if (script != null && script.cdpId() != null) {
            this.removeScriptToEvaluateOnNewDocument(script.cdpId());
         }
      }
   }

   public Integer getFrameOwnerBackendNodeId(String frameId) {
      if (frameId == null) {
         return null;
      } else {
         this.cdp.method("DOM.enable").send();
         CdpResponse response = this.cdp.method("DOM.getFrameOwner").param("frameId", frameId).send();
         return response != null && !response.isError() ? response.getResultAsInt("backendNodeId") : null;
      }
   }

   public Map<String, Object> describeNode(int backendNodeId) {
      CdpResponse response = this.cdp.method("DOM.describeNode").param("backendNodeId", backendNodeId).send();
      return response != null && !response.isError() ? response.getResult("node") : null;
   }

   private CdpDriver(CdpLauncher launcher, CdpDriverOptions options) {
      this.launcher = launcher;
      this.options = options;
      this.cdp = CdpClient.connect(launcher.getWebSocketUrl(), options.getTimeoutDuration());
      this.currentTargetId = extractTargetIdFromUrl(launcher.getWebSocketUrl());
      ACTIVE.add(this);
      this.initialize();
   }

   private CdpDriver(String webSocketUrl, CdpDriverOptions options) {
      this.launcher = null;
      this.options = options;
      this.cdp = CdpClient.connect(webSocketUrl, options.getTimeoutDuration());
      this.currentTargetId = extractTargetIdFromUrl(webSocketUrl);
      ACTIVE.add(this);
      this.initialize();
   }

   private static String extractTargetIdFromUrl(String webSocketUrl) {
      if (webSocketUrl == null) {
         return null;
      } else {
         int pageIndex = webSocketUrl.indexOf("/devtools/page/");
         return pageIndex != -1 ? webSocketUrl.substring(pageIndex + "/devtools/page/".length()) : null;
      }
   }

   public static CdpDriver start(CdpDriverOptions options) {
      CdpLauncher launcher = CdpLauncher.start(options);
      return new CdpDriver(launcher, options);
   }

   public static CdpDriver start() {
      return start(CdpDriverOptions.builder().build());
   }

   public static CdpDriver startHeadless() {
      return start(CdpDriverOptions.builder().headless(true).build());
   }

   public static CdpDriver connect(String webSocketUrl) {
      return connect(webSocketUrl, CdpDriverOptions.builder().build());
   }

   public static CdpDriver connect(String webSocketUrl, CdpDriverOptions options) {
      return new CdpDriver(webSocketUrl, options);
   }

   public static CdpDriver connectNewContext(String browserWebSocketUrl, CdpDriverOptions options) {
      CdpClient browser = CdpClient.connect(browserWebSocketUrl, options.getTimeoutDuration());

      CdpDriver var6;
      try {
         String contextId = browser.browserMethod("Target.createBrowserContext").send().getResultAsString("browserContextId");
         if (contextId == null) {
            throw new DriverException("browser did not return a browserContextId: " + browserWebSocketUrl);
         }

         try {
            String targetId = browser.browserMethod("Target.createTarget")
               .param("url", "about:blank")
               .param("browserContextId", contextId)
               .send()
               .getResultAsString("targetId");
            if (targetId == null) {
               throw new DriverException("browser did not return a targetId for context: " + contextId);
            }

            CdpDriver driver = new CdpDriver(pageWsUrlFrom(browserWebSocketUrl, targetId), options);
            driver.ownedBrowserContextId = contextId;
            driver.browserWebSocketUrl = browserWebSocketUrl;
            logger.debug("created driver in browser context {} target {}", contextId, targetId);
            var6 = driver;
         } catch (RuntimeException var12) {
            try {
               browser.browserMethod("Target.disposeBrowserContext").param("browserContextId", contextId).send();
            } catch (Exception var11) {
               logger.debug("could not dispose orphan browser context: {}", contextId);
            }

            throw var12;
         }
      } finally {
         browser.close();
      }

      return var6;
   }

   private static String pageWsUrlFrom(String browserWebSocketUrl, String targetId) {
      int index = browserWebSocketUrl.indexOf("/devtools/");
      if (index == -1) {
         throw new DriverException("not a devtools websocket url: " + browserWebSocketUrl);
      } else {
         return browserWebSocketUrl.substring(0, index) + "/devtools/page/" + targetId;
      }
   }

   public static void closeAll() {
      ACTIVE.forEach(CdpDriver::quit);
   }

   private void initialize() {
      logger.debug("initializing CDP driver");
      CdpResponse frameResponse = this.cdp.method("Page.getFrameTree").send();
      this.mainFrameId = frameResponse.getResult("frameTree.frame.id");
      this.committedLoaderId = frameResponse.getResultAsString("frameTree.frame.loaderId");
      logger.debug("main frame ID: {}", this.mainFrameId);
      if (this.currentTargetId == null) {
         List<String> pages = this.getPages();
         if (!pages.isEmpty()) {
            this.currentTargetId = pages.get(0);
            logger.warn("currentTargetId fallback to getPages()[0]: {} - parallel execution may be unreliable", this.currentTargetId);
         }
      } else {
         logger.debug("current target ID (from URL): {}", this.currentTargetId);
      }

      this.browserContextId = this.lookupBrowserContextId(this.currentTargetId);
      logger.debug("browser context ID: {}", this.browserContextId == null ? "<default>" : this.browserContextId);
      this.setupEventHandlers();
      this.pageSessionId = this.cdp.getSessionId();
      this.cdp.method("Page.enable").send();
      this.cdp.method("Runtime.enable").send();
      this.cdp.method("Network.enable").param("maxTotalBufferSize", 134217728).param("maxResourceBufferSize", 8388608).send();
      this.cdp.method("Target.setAutoAttach").param("autoAttach", true).param("waitForDebuggerOnStart", false).param("flatten", true).send();
      this.cdp.method("Page.setLifecycleEventsEnabled").param("enabled", true).send();

      try {
         CdpResponse initialTargets = this.cdp.browserMethod("Target.getTargets").send();
         List<Map<String, Object>> targets = initialTargets.getResult("targetInfos");
         if (targets != null) {
            for (Map<String, Object> target : targets) {
               String targetId = (String)target.get("targetId");
               if (targetId != null) {
                  this.knownTargetIds.add(targetId);
               }
            }
         }
      } catch (Exception var7) {
         logger.debug("initial Target.getTargets seed failed: {}", var7.getMessage());
      }

      this.cdp.browserMethod("Target.setDiscoverTargets").param("discover", true).send();
      this.checkIfPageAlreadyLoaded();
      this.awaitMainContext(this.options.getTimeoutDuration().toMillis());
   }

   private void nudgeLoadWaiter() {
      CompletableFuture<Void> tick = this.loadTick;
      this.loadTick = new CompletableFuture<>();
      tick.complete(null);
   }

   private void completeMainContext(int contextId) {
      this.frameContexts.put(this.mainFrameId, contextId);
      CompletableFuture<Integer> f = this.mainContextReady;
      if (f.isDone()) {
         this.mainContextReady = CompletableFuture.completedFuture(contextId);
      } else {
         f.complete(contextId);
      }

      this.nudgeLoadWaiter();
   }

   private void invalidateMainContext() {
      if (this.mainFrameId != null) {
         this.frameContexts.remove(this.mainFrameId);
      }

      if (this.mainContextReady.isDone()) {
         this.mainContextReady = new CompletableFuture<>();
      }
   }

   private Integer awaitMainContext(long timeoutMs) {
      try {
         return this.mainContextReady.get(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (Exception var4) {
         return null;
      }
   }

   private void completeFrameContext(String frameId, int contextId) {
      CompletableFuture<Integer> f = this.frameContextReady.computeIfAbsent(frameId, k -> new CompletableFuture<>());
      if (f.isDone()) {
         this.frameContextReady.put(frameId, CompletableFuture.completedFuture(contextId));
      } else {
         f.complete(contextId);
      }
   }

   private void releaseFrameContextWaiters(String frameId) {
      CompletableFuture<Integer> f = this.frameContextReady.remove(frameId);
      if (f != null && !f.isDone()) {
         f.complete(null);
      }
   }

   private Integer awaitFrameContext(String frameId, long timeoutMs) {
      CompletableFuture<Integer> f = this.frameContextReady.computeIfAbsent(frameId, k -> new CompletableFuture<>());

      try {
         return f.get(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (Exception var6) {
         return null;
      }
   }

   private void checkIfPageAlreadyLoaded() {
      try {
         CdpResponse response = this.cdp.method("Runtime.evaluate").param("expression", "document.readyState").param("returnByValue", true).send();
         String readyState = response.getResultAsString("result.value");
         if (("complete".equals(readyState) || "interactive".equals(readyState)) && !this.domContentEventFired) {
            logger.debug("page already loaded (readyState={}), setting domContentEventFired", readyState);
            this.domContentEventFired = true;
         }
      } catch (Exception var3) {
         logger.warn("readyState check failed: {}", var3.getMessage());
      }
   }

   private void setupEventHandlers() {
      this.cdp.on("Page.lifecycleEvent", event -> {
         if (!this.isFromOtherSession(event)) {
            String name = event.get("name");
            String frameId = event.get("frameId");
            logger.trace("lifecycleEvent: name={}, frameId={}", name, frameId);
            if ("DOMContentLoaded".equals(name) && this.mainFrameId.equals(frameId)) {
               this.domContentLoaderId = event.get("loaderId");
               this.domContentEventFired = true;
               this.nudgeLoadWaiter();
               logger.trace("DOMContentLoaded on main frame (via lifecycleEvent)");
            }
         }
      });
      this.cdp.on("Page.domContentEventFired", event -> {
         if (!this.isFromOtherSession(event)) {
            this.domContentEventFired = true;
            this.nudgeLoadWaiter();
            logger.trace("domContentEventFired (fallback)");
         }
      });
      this.cdp.on("Page.frameStartedLoading", event -> {
         if (!this.isFromOtherSession(event)) {
            String frameId = event.get("frameId");
            if (frameId != null && frameId.equals(this.mainFrameId)) {
               this.domContentEventFired = false;
               this.domContentLoaderId = null;
               this.framesStillLoading.clear();
               logger.trace("frameStartedLoading: {} (main frame, reset state)", frameId);
            } else if (frameId != null) {
               this.framesStillLoading.add(frameId);
               logger.trace("frameStartedLoading: {} (child frame)", frameId);
            }
         }
      });
      this.cdp.on("Page.frameStoppedLoading", event -> {
         if (!this.isFromOtherSession(event)) {
            String frameId = event.get("frameId");
            if (frameId != null) {
               this.framesStillLoading.remove(frameId);
               this.nudgeLoadWaiter();
            }

            logger.trace("frameStoppedLoading: {}, remaining: {}", frameId, this.framesStillLoading);
         }
      });
      this.cdp.on("Page.frameNavigated", event -> {
         if (!this.isFromOtherSession(event)) {
            String frameId = event.get("frame.id");
            if (frameId != null && frameId.equals(this.mainFrameId)) {
               this.committedLoaderId = event.get("frame.loaderId");
               this.nudgeLoadWaiter();
               logger.trace("frameNavigated (main frame): loaderId={}, url={}", this.committedLoaderId, (String)event.get("frame.url"));
            }
         }
      });
      this.cdp.on("Page.frameDetached", event -> {
         if (!this.isFromOtherSession(event)) {
            String frameId = event.get("frameId");
            if (frameId != null) {
               this.framesStillLoading.remove(frameId);
               this.frameContexts.remove(frameId);
               this.releaseFrameContextWaiters(frameId);
               this.nudgeLoadWaiter();
            }

            logger.trace("frameDetached: {}, remaining: {}", frameId, this.framesStillLoading);
         }
      });
      this.cdp.on("Page.javascriptDialogOpening", event -> {
         String message = event.get("message");
         String type = event.get("type");
         String defaultPrompt = event.get("defaultPrompt");
         this.currentDialogText = message;
         CdpDialog dialog = new CdpDialog(this.cdp, message, type, defaultPrompt);
         this.currentDialog = dialog;
         logger.debug("dialog opening: type={}, message={}", type, message);
         if (this.dialogHandler != null) {
            try {
               this.dialogHandler.handle(dialog);
            } catch (Exception var10) {
               String msg = var10.getMessage();
               if (msg != null && msg.contains("No dialog is showing")) {
                  logger.debug("dialog handler: dialog already resolved (benign race): {}", msg);
               } else {
                  logger.error("dialog handler error: {}", msg);
               }
            }

            if (!dialog.isHandled()) {
               logger.warn("dialog handler did not resolve dialog, auto-dismissing");

               try {
                  dialog.dismiss();
               } catch (Exception var9) {
                  logger.trace("auto-dismiss failed (dialog likely already handled): {}", var9.getMessage());
               }
            }

            this.currentDialog = null;
            this.currentDialogText = null;
         }

         if (this.dialogHandler == null) {
            if ("beforeunload".equals(type)) {
               logger.debug("auto-accepting beforeunload (no handler), pendingNavigation={}", this.pendingNavigationUrl);

               try {
                  dialog.accept();
               } catch (Exception var8) {
                  logger.trace("beforeunload accept failed (likely already handled): {}", var8.getMessage());
               }

               this.currentDialog = null;
               this.currentDialogText = null;
            } else {
               this.cdp.cancelPendingEvaluations();
            }
         }
      });
      this.cdp.on("Runtime.executionContextCreated", event -> {
         String eventSession = event.getSessionId();
         String currentSession = this.cdp.getSessionId();
         if (eventSession != null && !eventSession.equals(currentSession)) {
            logger.trace("ignoring executionContextCreated from other session: {}", eventSession);
         } else {
            Map<String, Object> context = event.get("context");
            if (context != null) {
               Number contextId = (Number)context.get("id");
               Map<String, Object> auxData = (Map<String, Object>)context.get("auxData");
               if (auxData != null && contextId != null) {
                  String frameId = (String)auxData.get("frameId");
                  Boolean isDefault = (Boolean)auxData.get("isDefault");
                  if (frameId != null && Boolean.TRUE.equals(isDefault)) {
                     if (frameId.equals(this.mainFrameId)) {
                        this.completeMainContext(contextId.intValue());
                     } else {
                        this.frameContexts.put(frameId, contextId.intValue());
                        this.completeFrameContext(frameId, contextId.intValue());
                     }

                     logger.trace("execution context created: frameId={}, contextId={}", frameId, contextId);
                  }
               }
            }
         }
      });
      this.cdp.on("Runtime.executionContextsCleared", event -> {
         String eventSession = event.getSessionId();
         String currentSession = this.cdp.getSessionId();
         if (eventSession != null && !eventSession.equals(currentSession)) {
            logger.trace("ignoring executionContextsCleared from old session: {}", eventSession);
         } else {
            this.frameContexts.clear();
            this.invalidateMainContext();
            this.frameContextReady.replaceAll((id, f) -> f.isDone() ? new CompletableFuture<>() : f);
            logger.trace("execution contexts cleared");
         }
      });
      this.cdp.on("Target.targetCreated", event -> {
         Map<String, Object> info = event.get("targetInfo");
         if (info != null) {
            String type = (String)info.get("type");
            if ("page".equals(type)) {
               String targetId = (String)info.get("targetId");
               if (targetId != null) {
                  if (this.knownTargetIds.add(targetId)) {
                     if (!targetId.equals(this.currentTargetId)) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("targetId", targetId);
                        entry.put("url", info.get("url"));
                        entry.put("title", info.get("title"));
                        this.openedTargets.add(entry);
                        logger.debug("new page target opened: {} url={}", targetId, info.get("url"));
                     }
                  }
               }
            }
         }
      });
      this.cdp.on("Target.targetInfoChanged", event -> {
         Map<String, Object> info = event.get("targetInfo");
         if (info != null) {
            String targetId = (String)info.get("targetId");
            if (targetId != null) {
               for (Map<String, Object> entry : this.openedTargets) {
                  if (targetId.equals(entry.get("targetId"))) {
                     entry.put("url", info.get("url"));
                     entry.put("title", info.get("title"));
                  }
               }
            }
         }
      });
      this.cdp.on("Target.targetDestroyed", event -> {
         String targetId = event.get("targetId");
         if (targetId != null) {
            this.knownTargetIds.remove(targetId);
            this.openedTargets.removeIf(entry -> targetId.equals(entry.get("targetId")));
            CompletableFuture<Void> removal = this.pendingTargetRemovals.remove(targetId);
            if (removal != null) {
               removal.complete(null);
            }
         }
      });
      this.cdp.on("Target.attachedToTarget", event -> {
         String attachedSessionId = event.get("sessionId");
         Map<String, Object> targetInfo = event.get("targetInfo");
         if (targetInfo != null && "iframe".equals(targetInfo.get("type"))) {
            String targetId = (String)targetInfo.get("targetId");
            this.oopifTargets.put(targetId, targetInfo);
            this.oopifSessions.put(targetId, attachedSessionId);
            logger.debug("Attached to isolated iframe (OOPIF): {} session={}", targetInfo.get("url"), attachedSessionId);
            this.cdp.method("Runtime.enable").sessionId(attachedSessionId).sendWithoutWaiting();
         }
      });
      this.cdp.on("Target.detachedFromTarget", event -> {
         String detachedSessionId = event.get("sessionId");
         if (detachedSessionId != null) {
            String detachedFrameId = null;

            for (Entry<String, String> e : this.oopifSessions.entrySet()) {
               if (detachedSessionId.equals(e.getValue())) {
                  detachedFrameId = e.getKey();
                  break;
               }
            }

            if (detachedFrameId != null) {
               this.oopifSessions.remove(detachedFrameId);
               this.oopifTargets.remove(detachedFrameId);
               this.frameContexts.remove(detachedFrameId);
               this.releaseFrameContextWaiters(detachedFrameId);
               logger.debug("OOPIF detached: frameId={}, sessionId={}", detachedFrameId, detachedSessionId);
            }

            if (detachedSessionId.equals(this.cdp.getSessionId()) && this.pageSessionId != null) {
               this.cdp.setSessionId(this.pageSessionId);
               this.currentFrame = null;
               logger.debug("active OOPIF session detached, reverted to main page session");
            }
         }
      });
      this.cdp.on("Fetch.requestPaused", this::onRequestPaused);
   }

   private boolean isFromOtherSession(CdpEvent event) {
      String eventSession = event.getSessionId();
      if (eventSession == null) {
         return false;
      } else {
         String currentSession = this.cdp.getSessionId();
         return eventSession.equals(currentSession) ? false : !eventSession.equals(this.pageSessionId);
      }
   }

   private void onRequestPaused(CdpEvent event) {
      String requestId = event.get("requestId");
      Map<String, Object> request = event.get("request");
      String resourceType = event.get("resourceType");
      String url = request != null ? (String)request.get("url") : "";
      String method = request != null ? (String)request.get("method") : "GET";
      Map<String, Object> headers = request != null ? (Map)request.get("headers") : Map.of();
      String postData = request != null ? (String)request.get("postData") : null;
      logger.debug("request intercepted: {} {}", method, url);
      InterceptResponse response = null;
      if (this.interceptHandler != null) {
         InterceptRequest interceptRequest = new InterceptRequest(requestId, url, method, headers, postData, resourceType);

         try {
            response = this.interceptHandler.handle(interceptRequest);
         } catch (Exception var12) {
            logger.error("intercept handler error: {}", var12.getMessage());
         }
      }

      if (response != null) {
         this.fulfillRequest(requestId, response);
      } else {
         this.continueRequest(requestId);
      }
   }

   private void fulfillRequest(String requestId, InterceptResponse response) {
      CdpMessage message = this.cdp.method("Fetch.fulfillRequest").param("requestId", requestId).param("responseCode", response.getStatus());
      if (response.getHeaders() != null && !response.getHeaders().isEmpty()) {
         List<Map<String, String>> headersList = new ArrayList<>();

         for (Entry<String, Object> entry : response.getHeaders().entrySet()) {
            headersList.add(Map.of("name", entry.getKey(), "value", String.valueOf(entry.getValue())));
         }

         message.param("responseHeaders", headersList);
      }

      String bodyBase64 = response.getBodyBase64();
      if (bodyBase64 != null && !bodyBase64.isEmpty()) {
         message.param("body", bodyBase64);
      }

      message.send();
   }

   private void continueRequest(String requestId) {
      this.cdp.method("Fetch.continueRequest").param("requestId", requestId).send();
   }

   @Override
   public void setUrl(String url) {
      logger.debug("navigating to: {}", url);
      this.pendingNavigationUrl = url;
      this.domContentEventFired = false;
      this.framesStillLoading.clear();
      this.preNavCommittedLoaderId = this.committedLoaderId;
      int navAttempts = 3;
      CdpResponse navResponse = null;
      boolean aborted = false;

      for (int attempt = 0; attempt < navAttempts; attempt++) {
         try {
            navResponse = this.cdp.method("Page.navigate").param("url", url).send();
         } catch (RuntimeException var12) {
            boolean transientNavTimeout = var12.getMessage() != null && var12.getMessage().contains("CDP timeout for: Page.navigate");
            if (transientNavTimeout && attempt < navAttempts - 1) {
               logger.warn("Page.navigate timed out, retrying ({}/{}): {}", new Object[]{attempt + 1, navAttempts - 1, url});
               continue;
            }

            throw var12;
         }

         aborted = "net::ERR_ABORTED".equals(navResponse.getResultAsString("errorText"));
         if (!aborted) {
            break;
         }

         if (attempt < navAttempts - 1) {
            logger.warn("Page.navigate returned ERR_ABORTED, retrying ({}/{}): {}", new Object[]{attempt + 1, navAttempts - 1, url});
            sleep(150L);
         }
      }

      if (aborted) {
         logger.warn("navigation aborted by browser, current document retained: {}", url);
         this.pendingNavigationUrl = null;
      } else if (!url.startsWith("data:") && !url.startsWith("about:")) {
         this.expectedLoaderId = navResponse.getResultAsString("loaderId");

         try {
            this.waitForPageLoad(this.options.getPageLoadStrategy());
         } finally {
            this.pendingNavigationUrl = null;
            this.expectedLoaderId = null;
         }
      } else {
         String localLoaderId = navResponse.getResultAsString("loaderId");
         if (localLoaderId != null && !pollUntil(2000L, 10L, () -> localLoaderId.equals(this.committedLoaderId))) {
            logger.debug("local navigation did not report commit within {}ms: {}", 2000L, url);
         }

         this.pendingNavigationUrl = null;
      }
   }

   @Override
   public void waitForPageLoad(PageLoadStrategy strategy) {
      this.waitForPageLoad(strategy, this.options.getTimeoutDuration());
   }

   @Override
   public void waitForPageLoad(PageLoadStrategy strategy, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();
      long lastStaleCheck = 0L;

      while (true) {
         CompletableFuture<Void> tick = this.loadTick;
         if (this.isPageLoadComplete(strategy)) {
            if (this.verifyJsExecution()) {
               return;
            }

            logger.warn("page load complete but JS context not ready yet");
         } else if (this.domContentEventFired && !this.framesStillLoading.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastStaleCheck > 2000L) {
               lastStaleCheck = now;
               this.pruneStaleFrames();
            }
         }

         long remaining = deadline - System.currentTimeMillis();
         if (remaining <= 0L || Thread.currentThread().isInterrupted()) {
            break;
         }

         try {
            tick.get(Math.min(remaining, 500L), TimeUnit.MILLISECONDS);
         } catch (InterruptedException var11) {
            Thread.currentThread().interrupt();
            break;
         } catch (Exception var12) {
         }
      }

      String url = this.pendingNavigationUrl != null ? this.pendingNavigationUrl : "(unknown)";
      String readyStateInfo = this.getReadyStateForDiagnostic();
      String jsExecInfo = this.getJsExecStateForDiagnostic();
      String diagnostic = String.format(
         "page load timeout after %dms - url: %s, strategy: %s, domContentEventFired: %s, framesStillLoading: %s, mainFrameId: %s, expectedLoaderId: %s, supersededLoaderId: %s, committedLoaderId: %s, domContentLoaderId: %s, readyState: %s, jsExec: %s, cdpOpen: %s",
         timeout.toMillis(),
         url,
         strategy,
         this.domContentEventFired,
         this.framesStillLoading,
         this.mainFrameId,
         this.expectedLoaderId,
         this.supersededLoaderId,
         this.committedLoaderId,
         this.domContentLoaderId,
         readyStateInfo,
         jsExecInfo,
         this.cdp.isOpen()
      );
      logger.warn(diagnostic);
      throw new RuntimeException(diagnostic);
   }

   private boolean verifyJsExecution() {
      Integer contextId = this.awaitMainContext(1000L);
      CdpDriver.JsProbe probe = this.probeJsAlive(contextId);
      if (probe == CdpDriver.JsProbe.ERROR && contextId != null) {
         probe = this.probeJsAlive(null);
      }

      return probe == CdpDriver.JsProbe.ALIVE;
   }

   private CdpDriver.JsProbe probeJsAlive(Integer contextId) {
      try {
         CdpMessage probe = this.cdp.method("Runtime.evaluate").param("expression", "typeof document !== 'undefined'").param("returnByValue", true);
         if (contextId != null) {
            probe.param("contextId", contextId);
         }

         CdpResponse response = probe.send();
         if (response.isError()) {
            return CdpDriver.JsProbe.ERROR;
         } else {
            return Boolean.TRUE.equals(response.getResult("result.value")) ? CdpDriver.JsProbe.ALIVE : CdpDriver.JsProbe.DEAD;
         }
      } catch (Exception var4) {
         logger.debug("JS execution probe failed (contextId={}): {}", contextId, var4.getMessage());
         return CdpDriver.JsProbe.ERROR;
      }
   }

   private boolean isPageLoadComplete(PageLoadStrategy strategy) {
      boolean domReady = this.isDomReady();
      if (!domReady) {
         domReady = this.checkDocumentReadyState();
         if (domReady) {
            logger.debug("page-load wait completed via document.readyState fallback (DOMContentLoaded event lagged)");
            this.domContentLoaderId = this.committedLoaderId;
            this.domContentEventFired = true;
         }
      }
      return switch (strategy) {
         case DOMCONTENT -> domReady;
         case DOMCONTENT_AND_FRAMES -> domReady && this.framesStillLoading.isEmpty();
         case LOAD, NETWORKIDLE -> domReady && this.framesStillLoading.isEmpty();
      };
   }

   private void pruneStaleFrames() {
      if (!this.framesStillLoading.isEmpty()) {
         try {
            CdpResponse response = this.cdp.method("Page.getFrameTree").send();
            List<Map<String, Object>> childFrames = response.getResult("frameTree.childFrames");
            Set<String> activeFrameIds = new HashSet<>();
            if (childFrames != null) {
               for (Map<String, Object> frameData : childFrames) {
                  Map<String, Object> frame = (Map<String, Object>)frameData.get("frame");
                  if (frame != null) {
                     activeFrameIds.add((String)frame.get("id"));
                  }
               }
            }

            int before = this.framesStillLoading.size();
            this.framesStillLoading.removeIf(id -> !activeFrameIds.contains(id));
            int removed = before - this.framesStillLoading.size();
            if (removed > 0) {
               logger.warn("pruned {} stale frame(s) from framesStillLoading, remaining: {}", removed, this.framesStillLoading);
            }
         } catch (Exception var7) {
            logger.trace("stale frame check failed: {}", var7.getMessage());
         }
      }
   }

   private boolean isDomReady() {
      String expected = this.expectedLoaderId;
      if (expected != null) {
         if (expected.equals(this.domContentLoaderId)) {
            return true;
         } else {
            String committed = this.committedLoaderId;
            return committed != null && committed.equals(this.domContentLoaderId) && !committed.equals(this.preNavCommittedLoaderId);
         }
      } else {
         String superseded = this.supersededLoaderId;
         if (superseded == null) {
            return this.domContentEventFired;
         } else {
            String domLoader = this.domContentLoaderId;
            return domLoader != null && !superseded.equals(domLoader);
         }
      }
   }

   private boolean checkDocumentReadyState() {
      String requireUrl = null;
      String expected = this.expectedLoaderId;
      if (expected != null && !expected.equals(this.committedLoaderId)) {
         String committed = this.committedLoaderId;
         if (committed == null || committed.equals(this.preNavCommittedLoaderId)) {
            return false;
         }

         requireUrl = this.pendingNavigationUrl;
         if (requireUrl == null) {
            return false;
         }
      }

      String superseded = this.supersededLoaderId;
      if (superseded != null) {
         String committedx = this.committedLoaderId;
         if (committedx == null || superseded.equals(committedx)) {
            requireUrl = this.historyTargetUrl;
            if (requireUrl == null) {
               return false;
            }
         }
      }

      try {
         CdpResponse response = this.cdp
            .method("Runtime.evaluate")
            .param("expression", "({r: document.readyState, u: window.location.href})")
            .param("returnByValue", true)
            .send();
         if (response.isError()) {
            logger.warn("readyState check CDP error: {}", response.getErrorMessage());
            return false;
         } else {
            String readyState = response.getResultAsString("result.value.r");
            logger.trace("readyState check returned: {}", readyState);
            boolean ready = "complete".equals(readyState) || "interactive".equals(readyState);
            if (ready && requireUrl != null) {
               ready = urlsEquivalent(requireUrl, response.getResultAsString("result.value.u"));
            }

            return ready;
         }
      } catch (Exception var7) {
         String msg = var7.getMessage();
         if (msg != null && msg.contains("websocket not open")) {
            logger.trace("readyState check skipped: websocket not open");
         } else {
            logger.warn("readyState check exception: {}", msg);
         }

         return false;
      }
   }

   static boolean urlsEquivalent(String requested, String actual) {
      if (requested == null || actual == null) {
         return false;
      } else if (requested.equals(actual)) {
         return true;
      } else {
         String r = requested.endsWith("/") ? requested.substring(0, requested.length() - 1) : requested;
         String a = actual.endsWith("/") ? actual.substring(0, actual.length() - 1) : actual;
         return r.equals(a);
      }
   }

   private String getReadyStateForDiagnostic() {
      try {
         CdpResponse response = this.cdp.method("Runtime.evaluate").param("expression", "document.readyState").param("returnByValue", true).send();
         return response.isError() ? "error:" + response.getErrorMessage() : response.getResultAsString("result.value");
      } catch (Exception var2) {
         return "exception:" + var2.getMessage();
      }
   }

   private String getJsExecStateForDiagnostic() {
      try {
         CdpResponse response = this.cdp.method("Runtime.evaluate").param("expression", "typeof document").param("returnByValue", true).send();
         return response.isError() ? "error:" + response.getErrorMessage() : "ok:" + response.getResultAsString("result.value");
      } catch (Exception var2) {
         return "exception:" + var2.getMessage();
      }
   }

   @Override
   public String getUrl() {
      CdpResponse response = this.cdpEval("window.location.href");
      return response.getResultAsString("result.value");
   }

   @Override
   public String getTitle() {
      CdpResponse response = this.cdpEval("document.title");
      return response.getResultAsString("result.value");
   }

   private void ensureKjsRuntime() {
      try {
         Boolean exists = (Boolean)this.evalDirect("typeof window.__kjs !== 'undefined' && typeof window.__kjs.resolve === 'function'");
         if (!Boolean.TRUE.equals(exists)) {
            this.evalDirect(DRIVER_JS);
         }
      } catch (Exception var2) {
      }
   }

   @Override
   public Object script(String expression) {
      if (expression.contains("__kjs")) {
         this.ensureKjsRuntime();
      }

      logger.trace("script: {}", truncate(expression, 200));
      CdpResponse response = this.cdpEval(expression);
      return this.extractJsValue(response, expression);
   }

   private static String truncate(String s, int max) {
      return s.length() <= max ? s : s.substring(0, max) + "...";
   }

   private Object evalDirect(String expression) {
      CdpResponse response = this.cdpEval(expression);
      return this.extractJsValue(response, expression);
   }

   private CdpResponse cdpEval(String expression) {
      return this.cdpEval(expression, true);
   }

   private CdpResponse cdpEval(String expression, boolean returnByValue) {
      if (this.dialogHandler == null && this.currentDialog != null && !this.currentDialog.isHandled()) {
         throw new DialogOpenedException("dialog is blocking Runtime.evaluate");
      } else {
         int transientInterval = this.fastPollInterval();
         int maxRetries = this.fastPollAttempts();
         int attempt = 0;

         CdpResponse response;
         while (true) {
            if (attempt > maxRetries) {
               throw new RuntimeException("eval retry logic error for: " + expression);
            }

            CdpMessage message = this.cdp.method("Runtime.evaluate").param("expression", expression).param("returnByValue", returnByValue);
            Integer contextId = this.getFrameContext();
            boolean mainContextUnresolved = this.currentFrame == null && contextId == null;
            if (mainContextUnresolved && attempt < maxRetries) {
               logger.warn("main-frame context not ready, retry {}/{}: {}", new Object[]{attempt + 1, maxRetries, truncate(expression, 100)});
               sleep(transientInterval);
            } else {
               if (contextId != null) {
                  message.param("contextId", contextId);
               } else if (mainContextUnresolved) {
                  logger.warn(
                     "main-frame context never became ready after {} attempts, falling back to default context: {}", maxRetries, truncate(expression, 100)
                  );
               }

               try {
                  response = message.send();
               } catch (DialogOpenedException var11) {
                  logger.debug("script opened a dialog, returning null: {}", truncate(expression, 100));
                  return new CdpResponse(Map.of());
               }

               if (!isTransientContextError(response)) {
                  break;
               }

               if (attempt >= maxRetries) {
                  logger.warn("retry exhausted for transient context error: {}", truncate(expression, 100));
                  break;
               }

               logger.warn("transient context error, retry {}/{}: {}", new Object[]{attempt + 1, maxRetries, truncate(expression, 100)});
               sleep(transientInterval);
            }

            attempt++;
         }

         if (attempt > 0 && !response.isError()) {
            logger.warn("retry succeeded after {} attempt(s): eval {}", attempt, truncate(expression, 50));
         }

         return response;
      }
   }

   static boolean isTransientContextError(CdpResponse response) {
      if (!response.isError()) {
         return false;
      } else {
         Integer errorCode = response.getErrorCode();
         if (errorCode != null && errorCode == -32000) {
            String message = response.getErrorMessage();
            if (message != null && message.contains("Object reference chain is too long")) {
               logger.trace("non-transient -32000 error, will not retry: {}", message);
               return false;
            } else {
               logger.trace("transient context error detected: {}", response.getErrorMessage());
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private Integer getFrameContext() {
      if (this.currentFrame == null) {
         return this.awaitMainContext(2000L);
      } else {
         return this.oopifSessions.containsKey(this.currentFrame.id) ? null : this.frameContexts.get(this.currentFrame.id);
      }
   }

   private Object extractJsValue(CdpResponse response, String expression) {
      if (response.isError()) {
         String message = response.getErrorMessage();
         if (message != null && message.contains("Object reference chain is too long")) {
            logger.debug("script result not serializable, returning null: {}", truncate(expression, 100));
            return null;
         } else {
            logger.warn("JS error for expression: {}", truncate(expression, 100));
            throw new RuntimeException("JS error: " + response.getError());
         }
      } else {
         Object exceptionDetails = response.getResult("exceptionDetails");
         if (exceptionDetails != null) {
            String description = response.getResultAsString("exceptionDetails.exception.description");
            if (description != null && !description.isEmpty()) {
               logger.warn("JS exception for: {} -> {}", truncate(expression, 100), truncate(description, 200));
               throw new RuntimeException("JS exception: " + description);
            } else {
               String text = response.getResultAsString("exceptionDetails.text");
               logger.warn("JS exception for: {} -> {}", truncate(expression, 100), text);
               throw new RuntimeException("JS exception: " + text);
            }
         } else {
            Object value = response.getResult("result.value");
            logger.trace("script result: {}", value);
            return value;
         }
      }
   }

   @Override
   public byte[] screenshot() {
      return this.screenshot(false);
   }

   @Override
   public byte[] screenshot(boolean embed) {
      return this.screenshot(embed, null);
   }

   private byte[] screenshot(boolean embed, Duration timeout) {
      CdpMessage message = this.cdp.method("Page.captureScreenshot").param("format", "png");
      if (timeout != null) {
         message.timeout(timeout);
      }

      CdpResponse response = message.send();
      String base64 = response.getResultAsString("data");
      byte[] bytes = Base64.getDecoder().decode(base64);
      if (embed) {
         LogContext ctx = LogContext.get();
         ctx.embed(bytes, "image/png", "screenshot.png");
      }

      return bytes;
   }

   @Override
   public byte[] screenshotFullPage(boolean embed) {
      CdpResponse metrics = this.cdp.method("Page.getLayoutMetrics").send();
      Integer width = metrics.getResultAsInt("cssContentSize.width");
      Integer height = metrics.getResultAsInt("cssContentSize.height");
      if (width == null || height == null) {
         width = metrics.getResultAsInt("contentSize.width");
         height = metrics.getResultAsInt("contentSize.height");
      }

      if (width != null && height != null && width >= 1 && height >= 1) {
         Map<String, Object> clip = new LinkedHashMap<>();
         clip.put("x", 0);
         clip.put("y", 0);
         clip.put("width", width);
         clip.put("height", height);
         clip.put("scale", 1);
         CdpResponse response = this.cdp
            .method("Page.captureScreenshot")
            .param("format", "png")
            .param("clip", clip)
            .param("captureBeyondViewport", true)
            .send();
         byte[] bytes = Base64.getDecoder().decode(response.getResultAsString("data"));
         if (embed) {
            LogContext.get().embed(bytes, "image/png", "screenshot.png");
         }

         return bytes;
      } else {
         return this.screenshot(embed);
      }
   }

   @Override
   public byte[] failureScreenshot() {
      return this.screenshot(false, FAILURE_SCREENSHOT_TIMEOUT);
   }

   @Override
   public byte[] screenshot(String locator, boolean embed) {
      Map<String, Object> pos = this.position(locator);
      if (pos == null) {
         return this.screenshot(embed);
      } else {
         Map<String, Object> clip = new LinkedHashMap<>(pos);
         clip.put("scale", 1);
         CdpResponse response = this.cdp.method("Page.captureScreenshot").param("format", "png").param("clip", clip).send();
         String base64 = response.getResultAsString("data");
         byte[] bytes = Base64.getDecoder().decode(base64);
         if (embed) {
            LogContext.get().embed(bytes, "image/png", "screenshot.png");
         }

         return bytes;
      }
   }

   @Override
   public void onDialog(DialogHandler handler) {
      this.dialogHandler = handler;
   }

   @Override
   public String getDialogText() {
      return this.currentDialogText;
   }

   @Override
   public Dialog getDialog() {
      if (this.currentDialog != null && this.currentDialog.isHandled()) {
         this.currentDialog = null;
         this.currentDialogText = null;
      }

      return this.currentDialog;
   }

   @Override
   public void dialog(boolean accept) {
      this.dialog(accept, null);
   }

   @Override
   public void dialog(boolean accept, String input) {
      CdpMessage message = this.cdp.method("Page.handleJavaScriptDialog").param("accept", accept);
      if (accept && input != null) {
         message.param("promptText", input);
      }

      message.send();
      this.currentDialogText = null;
      this.currentDialog = null;
   }

   @Override
   public void switchFrame(int index) {
      List<Map<String, Object>> childFrames = this.waitForChildFrames(index);
      Map<String, Object> frameData = childFrames.get(index);
      Map<String, Object> frame = (Map<String, Object>)frameData.get("frame");
      String frameId = (String)frame.get("id");
      String url = (String)frame.get("url");
      String name = (String)frame.get("name");
      this.currentFrame = new CdpDriver.Frame(frameId, url, name);
      logger.debug("switched to frame by index {}: {}", index, this.currentFrame);
      this.ensureFrameContext(frameId);
   }

   private List<Map<String, Object>> waitForChildFrames(int minIndex) {
      List<Map<String, Object>>[] holder = new List[1];
      boolean found = this.retry("frame at index " + minIndex, () -> {
         CdpResponse response = this.cdp.method("Page.getFrameTree").send();
         List<Map<String, Object>> childFrames = response.getResult("frameTree.childFrames");
         if (childFrames != null && minIndex >= 0 && minIndex < childFrames.size()) {
            holder[0] = childFrames;
            return true;
         } else {
            return false;
         }
      });
      if (!found) {
         throw new DriverException("no frame at index: " + minIndex + " after " + this.options.getRetryCount() + " retries");
      } else {
         return holder[0];
      }
   }

   @Override
   public void switchFrame(String locator) {
      if (locator == null) {
         this.currentFrame = null;
         this.cdp.setSessionId(this.pageSessionId);
         logger.debug("switched to main frame");
      } else {
         if (this.pageSessionId != null && !this.pageSessionId.equals(this.cdp.getSessionId())) {
            this.cdp.setSessionId(this.pageSessionId);
            this.currentFrame = null;
         }

         this.retryIfNeeded(locator);
         String js = Locators.wrapInFunctionInvoke(
            "var e = "
               + Locators.selector(locator)
               + "; if (!e) return null; if (e.tagName !== 'IFRAME' && e.tagName !== 'FRAME') return { error: 'not a frame element' }; return {    name: e.name || '',   src: e.src || '' }"
         );
         Object result = this.script(js);
         if (result == null) {
            throw new DriverException("frame not found: " + locator);
         } else {
            Map<String, Object> frameInfo = (Map<String, Object>)result;
            if (frameInfo.containsKey("error")) {
               throw new DriverException("locator is not a frame: " + locator);
            } else {
               String targetName = (String)frameInfo.get("name");
               String targetSrc = (String)frameInfo.get("src");
               CdpResponse response = this.cdp.method("Page.getFrameTree").send();
               List<Map<String, Object>> childFrames = response.getResult("frameTree.childFrames");
               String frameId = null;
               String url = null;
               String name = null;
               if (childFrames != null) {
                  for (Map<String, Object> frameData : childFrames) {
                     Map<String, Object> frame = (Map<String, Object>)frameData.get("frame");
                     String fId = (String)frame.get("id");
                     String fUrl = (String)frame.get("url");
                     String fName = (String)frame.get("name");
                     if (!this.oopifSessions.containsKey(fId)) {
                        boolean matches = false;
                        if (targetName != null && !targetName.isEmpty() && targetName.equals(fName)) {
                           matches = targetSrc == null || targetSrc.isEmpty() || fUrl != null && fUrl.contains(targetSrc);
                        } else if (targetSrc != null && !targetSrc.isEmpty() && fUrl != null && fUrl.contains(targetSrc)) {
                           matches = true;
                        } else if ((targetName == null || targetName.isEmpty()) && (targetSrc == null || targetSrc.isEmpty())) {
                           matches = true;
                        }

                        if (matches) {
                           frameId = fId;
                           url = fUrl;
                           name = fName;
                           this.cdp.setSessionId(this.pageSessionId);
                           break;
                        }
                     }
                  }
               }

               if (frameId == null) {
                  String[] matched = new String[3];
                  this.retry(
                     "OOPIF matching locator " + locator,
                     () -> {
                        for (Entry<String, String> entry : this.oopifSessions.entrySet()) {
                           String potentialFrameId = entry.getKey();
                           String sessionId = entry.getValue();
                           this.cdp.setSessionId(sessionId);

                           try {
                              CdpResponse evalName = this.cdp.method("Runtime.evaluate").param("expression", "window.name").param("returnByValue", true).send();
                              CdpResponse evalUrl = this.cdp
                                 .method("Runtime.evaluate")
                                 .param("expression", "window.location.href")
                                 .param("returnByValue", true)
                                 .send();
                              String internalName = evalName.getResultAsString("result.value");
                              String internalUrl = evalUrl.getResultAsString("result.value");
                              boolean noCriteria = (targetName == null || targetName.isEmpty()) && (targetSrc == null || targetSrc.isEmpty());
                              boolean nameOk = targetName == null || targetName.isEmpty() || targetName.equals(internalName);
                              boolean srcOk = targetSrc == null || targetSrc.isEmpty() || internalUrl != null && internalUrl.contains(targetSrc);
                              boolean matchesx = noCriteria || nameOk && srcOk;
                              if (matchesx) {
                                 matched[0] = potentialFrameId;
                                 matched[1] = internalUrl;
                                 matched[2] = internalName;
                                 return true;
                              }
                           } catch (Exception var16x) {
                              logger.trace("Failed to query OOPIF internal state: {}", var16x.getMessage());
                           }

                           this.cdp.setSessionId(this.pageSessionId);
                        }

                        return false;
                     }
                  );
                  frameId = matched[0];
                  url = matched[1];
                  name = matched[2];
               }

               if (frameId != null) {
                  this.currentFrame = new CdpDriver.Frame(frameId, url, name);
                  logger.debug("switched to frame by locator {}: {}", locator, this.currentFrame);
                  this.ensureFrameContext(frameId);
               } else {
                  this.cdp.setSessionId(this.pageSessionId);
                  StringBuilder diag = new StringBuilder("could not find frame for locator: ").append(locator);
                  diag.append(" (targetName='").append(targetName).append("', targetSrc='").append(targetSrc).append("'");
                  if (!this.oopifTargets.isEmpty()) {
                     diag.append(", knownOopifs=[");
                     boolean first = true;

                     for (Map<String, Object> info : this.oopifTargets.values()) {
                        if (!first) {
                           diag.append(", ");
                        }

                        diag.append(info.get("url"));
                        first = false;
                     }

                     diag.append("]");
                  }

                  diag.append(")");
                  throw new DriverException(diag.toString());
               }
            }
         }
      }
   }

   @Override
   public Map<String, Object> getCurrentFrame() {
      return this.currentFrame == null
         ? null
         : Map.of(
            "id",
            this.currentFrame.id,
            "url",
            this.currentFrame.url != null ? this.currentFrame.url : "",
            "name",
            this.currentFrame.name != null ? this.currentFrame.name : ""
         );
   }

   private void ensureFrameContext(String frameId) {
      if (this.oopifSessions.containsKey(frameId)) {
         this.cdp.setSessionId(this.oopifSessions.get(frameId));
         String expectedUrl = this.currentFrame != null ? this.currentFrame.url : null;
         this.waitForOopifReady(frameId, expectedUrl);
      } else {
         if (!this.frameContexts.containsKey(frameId)) {
            Integer contextId = this.awaitFrameContext(frameId, 1000L);
            if (contextId != null) {
               logger.debug("frame context arrived via event: frameId={}", frameId);
            }
         }

         if (!this.frameContexts.containsKey(frameId)) {
            logger.warn("frame context not received via event, falling back to isolated world: {}", frameId);

            try {
               CdpResponse response = this.cdp.method("Page.createIsolatedWorld").param("frameId", frameId).send();
               Integer contextId = response.getResult("executionContextId");
               if (contextId != null) {
                  this.frameContexts.put(frameId, contextId);
                  this.completeFrameContext(frameId, contextId);
                  logger.debug("created isolated world for frame {}: contextId={}", frameId, contextId);
               }
            } catch (Exception var4) {
               logger.warn("failed to create isolated world for frame {}: {}", frameId, var4.getMessage());
            }
         }

         this.waitForFrameContextReady(frameId);
      }
   }

   private void waitForOopifReady(String frameId, String expectedUrl) {
      boolean checkUrl = expectedUrl != null && !expectedUrl.isEmpty();
      String script = checkUrl ? "({r: document.readyState, u: document.URL})" : "document.readyState";
      boolean ready = pollUntil(2000L, 50L, () -> {
         try {
            CdpResponse response = this.cdp.method("Runtime.evaluate").param("expression", script).param("returnByValue", true).send();
            if (response.isError()) {
               return false;
            }

            if (checkUrl) {
               Map<String, Object> value = response.getResult("result.value");
               if (value != null) {
                  Object r = value.get("r");
                  Object u = value.get("u");
                  if (r instanceof String && !"loading".equals(r) && u instanceof String && ((String)u).contains(expectedUrl)) {
                     logger.trace("OOPIF ready: frameId={}, readyState={}, url={}", new Object[]{frameId, r, u});
                     return true;
                  }
               }
            } else {
               Object value = response.getResult("result.value");
               if (value instanceof String && !"loading".equals(value)) {
                  logger.trace("OOPIF ready: frameId={}, readyState={}", frameId, value);
                  return true;
               }
            }
         } catch (Exception var9) {
            logger.trace("OOPIF readiness check failed (will retry): {}", var9.getMessage());
         }

         return false;
      });
      if (!ready) {
         logger.warn("timeout waiting for OOPIF readiness: frameId={}, expectedUrl={} (will retry on first use)", frameId, expectedUrl);
      }
   }

   private void waitForFrameContextReady(String frameId) {
      boolean ready = pollUntil(
         1000L,
         50L,
         () -> {
            Integer contextId = this.frameContexts.get(frameId);
            if (contextId == null) {
               return false;
            } else {
               try {
                  CdpResponse response = this.cdp
                     .method("Runtime.evaluate")
                     .param("expression", "typeof document !== 'undefined'")
                     .param("returnByValue", true)
                     .param("contextId", contextId)
                     .send();
                  if (!response.isError() && Boolean.TRUE.equals(response.getResult("result.value"))) {
                     logger.trace("frame context ready: frameId={}, contextId={}", frameId, contextId);
                     return true;
                  }
               } catch (Exception var4) {
                  logger.trace("frame context not ready yet: {}", var4.getMessage());
               }

               return false;
            }
         }
      );
      if (!ready) {
         logger.warn("timeout waiting for frame context: frameId={} (will retry on first use)", frameId);
      }
   }

   @Override
   public void quit() {
      if (!this.terminated) {
         this.terminated = true;
         ACTIVE.remove(this);
         logger.debug("quitting CDP driver");
         if (!this.mainContextReady.isDone()) {
            this.mainContextReady.completeExceptionally(new DriverException("driver quit"));
         }

         if (this.cdp != null) {
            try {
               this.cdp.close();
            } catch (Exception var2) {
               logger.warn("error closing CDP: {}", var2.getMessage());
            }
         }

         this.disposeOwnedBrowserContext();
         if (this.launcher != null) {
            this.launcher.closeAndWait();
         }
      }
   }

   private void disposeOwnedBrowserContext() {
      String contextId = this.ownedBrowserContextId;
      String browserUrl = this.browserWebSocketUrl;
      if (contextId != null && browserUrl != null) {
         this.ownedBrowserContextId = null;

         try {
            CdpClient browser = CdpClient.connect(browserUrl, Duration.ofSeconds(5L));

            try {
               browser.browserMethod("Target.disposeBrowserContext").param("browserContextId", contextId).send();
               logger.debug("disposed browser context: {}", contextId);
            } finally {
               browser.close();
            }
         } catch (Exception var8) {
            logger.debug("could not dispose browser context {}: {}", contextId, var8.getMessage());
         }
      }
   }

   @Override
   public void close() {
      if (!this.terminated && this.currentTargetId != null) {
         String targetToClose = this.currentTargetId;
         logger.debug("close() - target to close: {}", targetToClose);
         List<String> allPages = this.getPages();
         logger.debug("close() - all pages: {}", allPages);
         String nextTarget = null;

         for (String pageId : allPages) {
            if (!pageId.equals(targetToClose)) {
               nextTarget = pageId;
               break;
            }
         }

         logger.debug("close() - switching to: {}", nextTarget);
         if (nextTarget == null) {
            CdpResponse response = this.cdp.browserMethod("Target.createTarget").param("url", "about:blank").send();
            nextTarget = response.getResult("targetId");
            logger.debug("close() - created blank tab: {}", nextTarget);
            if (nextTarget == null) {
               logger.warn("failed to create blank tab, quitting driver");
               this.quit();
               return;
            }
         }

         this.activateTarget(nextTarget);
         logger.debug("close() - switched to: {}, now closing: {}", nextTarget, targetToClose);
         CompletableFuture<Void> removed = new CompletableFuture<>();
         this.pendingTargetRemovals.put(targetToClose, removed);

         try {
            this.cdp.browserMethod("Target.closeTarget").param("targetId", targetToClose).send();
            logger.debug("close() - closed: {}", targetToClose);
            this.waitForTargetRemoved(targetToClose, removed);
         } finally {
            this.pendingTargetRemovals.remove(targetToClose);
         }
      }
   }

   private void waitForTargetRemoved(String targetId, CompletableFuture<Void> removed) {
      try {
         removed.get(1000L, TimeUnit.MILLISECONDS);
         logger.trace("target removal confirmed by event: {}", targetId);
      } catch (Exception var4) {
         if (!this.getPages().contains(targetId)) {
            logger.trace("target removed from page list: {}", targetId);
         } else {
            logger.warn("timeout waiting for target to be removed: {}", targetId);
         }
      }
   }

   @Override
   public boolean isTerminated() {
      return this.terminated;
   }

   @Override
   public boolean isReady() {
      CompletableFuture<Integer> f = this.mainContextReady;
      return f.isDone() && !f.isCompletedExceptionally();
   }

   @Override
   public void waitUntilReady() {
      this.awaitMainContext(this.options.getTimeoutDuration().toMillis());
   }

   @Override
   public boolean isResponsive() {
      if (!this.terminated && this.cdp.isOpen()) {
         try {
            CdpResponse response = this.cdp
               .method("Runtime.evaluate")
               .param("expression", "new Promise(function(r){ setTimeout(function(){ r(true); }, 0); })")
               .param("awaitPromise", true)
               .param("returnByValue", true)
               .timeout(Duration.ofSeconds(3L))
               .send();
            return !response.isError() && Boolean.TRUE.equals(response.getResult("result.value"));
         } catch (Exception var2) {
            logger.warn("isResponsive check failed: {}", var2.getMessage());
            return false;
         }
      } else {
         return false;
      }
   }

   public CdpClient getCdpClient() {
      return this.cdp;
   }

   @Override
   public DriverOptions getOptions() {
      return this.options;
   }

   public CdpDriverOptions getCdpOptions() {
      return this.options;
   }

   @Override
   public Element click(String locator) {
      logger.debug("click: {}", locator);
      this.elementAction(locator, Locators.clickJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element focus(String locator) {
      logger.debug("focus: {}", locator);
      this.elementAction(locator, Locators.focusJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element clear(String locator) {
      logger.debug("clear: {}", locator);
      this.elementAction(locator, Locators.clearJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element input(String locator, String value) {
      logger.debug("input: {} <- {}", locator, value);
      this.focus(locator);
      Boolean isDateTimeInput = (Boolean)this.script(locator, "_ && ['time','date','datetime-local','month','week'].indexOf(_.type) >= 0");
      if (Boolean.TRUE.equals(isDateTimeInput)) {
         this.elementAction(locator, Locators.inputJs(locator, value));
      } else {
         this.clear(locator);
         this.keys().type(value);
      }

      return BaseElement.existing(this, locator);
   }

   @Override
   public Element inputFile(String locator, String... files) {
      List<String> resolved = DriverApi.resolveFilePaths(files);
      logger.debug("inputFile: {} <- {}", locator, resolved);
      this.retryIfNeeded(locator);
      String objectId = this.objectId(locator);
      if (objectId == null) {
         throw new DriverException("inputFile: element did not resolve to a DOM node: " + locator);
      } else {
         CdpResponse response = this.cdp.method("DOM.setFileInputFiles").param("files", resolved).param("objectId", objectId).send();
         if (response != null && response.isError()) {
            throw new DriverException("inputFile failed: " + response.getErrorMessage() + " | locator: " + locator + " | files: " + resolved);
         } else {
            return BaseElement.existing(this, locator);
         }
      }
   }

   @Override
   public Element value(String locator, String value) {
      logger.debug("value: {} <- {}", locator, value);
      this.elementAction(locator, Locators.inputJs(locator, value));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element select(String locator, String text) {
      logger.debug("select: {} <- {}", locator, text);
      Object matched = this.elementAction(locator, Locators.optionSelector(locator, text));
      if (!Boolean.TRUE.equals(matched)) {
         throw new DriverException("select failed, no option matching: '" + text + "' for: " + locator);
      } else {
         return BaseElement.existing(this, locator);
      }
   }

   @Override
   public Element select(String locator, int index) {
      logger.debug("select: {} <- index {}", locator, index);
      this.retryIfNeeded(locator);
      String js = Locators.wrapInFunctionInvoke(
         "var e = " + Locators.selector(locator) + "; e.options[" + index + "].selected = true; " + Locators.commitFieldEventsJs("e")
      );
      this.script(js);
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element scroll(String locator) {
      logger.debug("scroll: {}", locator);
      this.retryIfNeeded(locator);
      this.script(Locators.scrollJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element highlight(String locator) {
      logger.debug("highlight: {}", locator);
      this.retryIfNeeded(locator);
      this.script(Locators.highlight(locator, this.options.getHighlightDuration()));
      return BaseElement.existing(this, locator);
   }

   @Override
   public String text(String locator) {
      this.retryIfNeeded(locator);
      return (String)this.script(Locators.textJs(locator));
   }

   @Override
   public String html(String locator) {
      this.retryIfNeeded(locator);
      return (String)this.script(Locators.outerHtmlJs(locator));
   }

   @Override
   public String value(String locator) {
      this.retryIfNeeded(locator);
      return (String)this.script(Locators.valueJs(locator));
   }

   @Override
   public String attribute(String locator, String name) {
      this.retryIfNeeded(locator);
      return (String)this.script(Locators.attributeJs(locator, name));
   }

   @Override
   public Object property(String locator, String name) {
      this.retryIfNeeded(locator);
      return this.script(Locators.propertyJs(locator, name));
   }

   @Override
   public boolean enabled(String locator) {
      this.retryIfNeeded(locator);
      Object result = this.script(Locators.enabledJs(locator));
      return Boolean.TRUE.equals(result);
   }

   @Override
   public boolean exists(String locator) {
      Object result = this.script(Locators.existsJs(locator));
      return Boolean.TRUE.equals(result);
   }

   @Override
   public Map<String, Object> position(String locator) {
      return (Map<String, Object>)this.elementAction(locator, Locators.getPositionJs(locator));
   }

   @Override
   public Map<String, Object> position(String locator, boolean relative) {
      this.retryIfNeeded(locator);
      if (relative) {
         String js = Locators.wrapInFunctionInvoke(
            "var r = " + Locators.selector(locator) + ".getBoundingClientRect(); return { x: r.x, y: r.y, width: r.width, height: r.height }"
         );
         return (Map<String, Object>)this.script(js);
      } else {
         return this.position(locator);
      }
   }

   @Override
   public Element locate(String locator) {
      return BaseElement.of(this, locator);
   }

   @Override
   public List<Element> locateAll(String locator) {
      String countJs = Locators.countJs(locator);
      Object countResult = this.script(countJs);
      int count = countResult instanceof Number ? ((Number)countResult).intValue() : 0;
      List<Element> elements = new ArrayList<>();
      if (count == 0) {
         return elements;
      } else {
         for (int i = 0; i < count; i++) {
            String indexedLocator = this.createIndexedLocator(locator, i);
            elements.add(new BaseElement(this, indexedLocator, true));
         }

         return elements;
      }
   }

   private String createIndexedLocator(String locator, int index) {
      if (locator.startsWith("{")) {
         locator = Locators.expandWildcard(locator);
      }

      if (Locators.isXpath(locator)) {
         String escapedXpath = Locators.escapeForJs(locator);
         return "(document.evaluate(\"(" + escapedXpath + ")[" + (index + 1) + "]\", document, null, 9, null).singleNodeValue)";
      } else {
         String escapedCss = Locators.escapeForJs(locator);
         return "(document.querySelectorAll(\"" + escapedCss + "\")[" + index + "])";
      }
   }

   @Override
   public Element optional(String locator) {
      return BaseElement.optional(this, locator);
   }

   @Override
   public Object script(String locator, Object expression) {
      this.retryIfNeeded(locator);
      String js = Locators.scriptSelector(locator, expression);
      return this.script(js);
   }

   public String objectId(String locator) {
      String expression = Locators.wrapInFunctionInvoke("return " + Locators.selector(locator) + ";");
      if (expression.contains("__kjs")) {
         this.ensureKjsRuntime();
      }

      CdpResponse response = this.cdpEval(expression, false);
      return response != null && !response.isError() ? response.getResultAsString("result.objectId") : null;
   }

   @Override
   public List<Object> scriptAll(String locator, Object expression) {
      this.retryIfNeeded(locator);
      String js = Locators.scriptAllSelector(locator, expression);
      return (List<Object>)this.script(js);
   }

   private void retryIfNeeded(String locator) {
      boolean found = this.retry("element: " + locator, () -> this.exists(locator));
      if (!found) {
         throw new DriverException("element not found after " + this.options.getRetryCount() + " retries: " + locator + " | " + this.getDriverState());
      }
   }

   private Object elementAction(String locator, String js) {
      RuntimeException lastError = null;

      for (int i = 0; i < 3; i++) {
         this.retryIfNeeded(locator);

         try {
            return this.script(js);
         } catch (RuntimeException var6) {
            if (!isElementVanished(var6)) {
               throw var6;
            }

            lastError = var6;
            logger.warn("element vanished between existence check and action, re-resolving ({}/{}): {}", new Object[]{i + 1, 3, locator});
            sleep(this.options.getRetryInterval());
         }
      }

      throw new DriverException("element vanished during action after 3 re-resolve attempts: " + locator + " | " + this.getDriverState(), lastError);
   }

   private static boolean isElementVanished(RuntimeException e) {
      return e.getMessage() != null && e.getMessage().contains("KARATE_ELEMENT_NOT_FOUND");
   }

   @Override
   public Element waitFor(String locator) {
      return this.waitFor(locator, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitFor(String locator, Duration timeout) {
      Element found = pollFor(timeout.toMillis(), this.options.getRetryInterval(), () -> this.exists(locator) ? BaseElement.existing(this, locator) : null);
      if (found == null) {
         throw new DriverException("timeout waiting for element: " + locator);
      } else {
         return found;
      }
   }

   @Override
   public Element waitForAny(String locator1, String locator2) {
      return this.waitForAny(new String[]{locator1, locator2});
   }

   @Override
   public Element waitForAny(String[] locators) {
      return this.waitForAny(locators, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForAny(String[] locators, Duration timeout) {
      Element found = pollFor(timeout.toMillis(), this.options.getRetryInterval(), () -> {
         for (String locator : locators) {
            if (this.exists(locator)) {
               return BaseElement.existing(this, locator);
            }
         }

         return null;
      });
      if (found == null) {
         throw new DriverException("timeout waiting for any element: " + String.join(", ", locators));
      } else {
         return found;
      }
   }

   @Override
   public Element waitForText(String locator, String expected) {
      return this.waitForText(locator, expected, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForText(String locator, String expected, Duration timeout) {
      Element found = pollFor(timeout.toMillis(), this.options.getRetryInterval(), () -> {
         if (this.exists(locator)) {
            String text = this.text(locator);
            if (text != null && text.contains(expected)) {
               return BaseElement.existing(this, locator);
            }
         }

         return null;
      });
      if (found == null) {
         throw new DriverException("timeout waiting for text '" + expected + "' in element: " + locator);
      } else {
         return found;
      }
   }

   @Override
   public Element waitForEnabled(String locator) {
      return this.waitForEnabled(locator, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForEnabled(String locator, Duration timeout) {
      Element found = pollFor(
         timeout.toMillis(), this.options.getRetryInterval(), () -> this.exists(locator) && this.enabled(locator) ? BaseElement.existing(this, locator) : null
      );
      if (found == null) {
         throw new DriverException("timeout waiting for element to be enabled: " + locator);
      } else {
         return found;
      }
   }

   @Override
   public String waitForUrl(String expected) {
      return this.waitForUrl(expected, this.options.getTimeoutDuration());
   }

   @Override
   public String waitForUrl(String expected, Duration timeout) {
      String found = pollFor(timeout.toMillis(), this.options.getRetryInterval(), () -> {
         String url = this.getUrl();
         return url != null && url.contains(expected) ? url : null;
      });
      if (found == null) {
         throw new DriverException("timeout waiting for URL to contain: " + expected);
      } else {
         return found;
      }
   }

   @Override
   public Element waitUntil(String locator, String expression) {
      return this.waitUntil(locator, expression, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitUntil(String locator, String expression, Duration timeout) {
      Element found = pollFor(
         timeout.toMillis(),
         this.options.getRetryInterval(),
         () -> this.exists(locator) && Terms.isTruthy(this.script(locator, expression)) ? BaseElement.existing(this, locator) : null
      );
      if (found == null) {
         throw new DriverException("timeout waiting for condition '" + expression + "' on element: " + locator);
      } else {
         return found;
      }
   }

   @Override
   public boolean waitUntil(String expression) {
      return this.waitUntil(expression, this.options.getTimeoutDuration());
   }

   @Override
   public boolean waitUntil(String expression, Duration timeout) {
      boolean met = pollUntil(timeout.toMillis(), this.options.getRetryInterval(), () -> Terms.isTruthy(this.script(expression)));
      if (!met) {
         throw new DriverException("timeout waiting for condition: " + expression);
      } else {
         return true;
      }
   }

   @Override
   public Object waitUntil(Supplier<Object> condition) {
      return this.waitUntil(condition, this.options.getTimeoutDuration());
   }

   @Override
   public Object waitUntil(Supplier<Object> condition, Duration timeout) {
      Object result = pollFor(timeout.toMillis(), this.options.getRetryInterval(), () -> {
         Object r = condition.get();
         return Terms.isTruthy(r) ? r : null;
      });
      if (result == null) {
         throw new DriverException("timeout waiting for condition");
      } else {
         return result;
      }
   }

   @Override
   public List<Element> waitForResultCount(String locator, int count) {
      return this.waitForResultCount(locator, count, this.options.getTimeoutDuration());
   }

   @Override
   public List<Element> waitForResultCount(String locator, int count, Duration timeout) {
      boolean met = pollUntil(timeout.toMillis(), this.options.getRetryInterval(), () -> ((Number)this.script(Locators.countJs(locator))).intValue() == count);
      if (!met) {
         throw new DriverException("timeout waiting for " + count + " elements: " + locator);
      } else {
         return this.locateAll(locator);
      }
   }

   @Override
   public Map<String, Object> cookie(String name) {
      CdpResponse response = this.cdp.method("Network.getCookies").send();
      List<Map<String, Object>> cookies = response.getResult("cookies");
      if (cookies != null) {
         for (Map<String, Object> cookie : cookies) {
            if (name.equals(cookie.get("name"))) {
               return cookie;
            }
         }
      }

      return null;
   }

   @Override
   public void cookie(Map<String, Object> cookie) {
      logger.debug("set cookie: {}", cookie.get("name"));
      CdpMessage message = this.cdp.method("Network.setCookie");
      if (!cookie.containsKey("url") && !cookie.containsKey("domain")) {
         message.param("url", this.getUrl());
      }

      cookie.forEach(message::param);
      message.send();
   }

   @Override
   public void deleteCookie(String name) {
      logger.debug("delete cookie: {}", name);
      Map<String, Object> cookie = this.cookie(name);
      if (cookie != null) {
         String domain = (String)cookie.get("domain");
         this.cdp.method("Network.deleteCookies").param("name", name).param("domain", domain).send();
      }
   }

   @Override
   public void clearCookies() {
      logger.debug("clear all cookies");
      this.cdp.method("Network.clearBrowserCookies").send();
   }

   @Override
   public List<Map<String, Object>> getCookies() {
      CdpResponse response = this.cdp.method("Network.getCookies").send();
      List<Map<String, Object>> cookies = response.getResult("cookies");
      return (List<Map<String, Object>>)(cookies != null ? cookies : new ArrayList<>());
   }

   @Override
   public void maximize() {
      logger.debug("maximize window");
      CdpResponse response = this.cdp.method("Browser.getWindowForTarget").send();
      Integer windowId = response.getResult("windowId");
      if (windowId != null) {
         this.cdp.method("Browser.setWindowBounds").param("windowId", windowId).param("bounds", Map.of("windowState", "maximized")).send();
      }
   }

   @Override
   public void minimize() {
      logger.debug("minimize window");
      CdpResponse response = this.cdp.method("Browser.getWindowForTarget").send();
      Integer windowId = response.getResult("windowId");
      if (windowId != null) {
         this.cdp.method("Browser.setWindowBounds").param("windowId", windowId).param("bounds", Map.of("windowState", "minimized")).send();
      }
   }

   @Override
   public void fullscreen() {
      logger.debug("fullscreen window");
      CdpResponse response = this.cdp.method("Browser.getWindowForTarget").send();
      Integer windowId = response.getResult("windowId");
      if (windowId != null) {
         this.cdp.method("Browser.setWindowBounds").param("windowId", windowId).param("bounds", Map.of("windowState", "fullscreen")).send();
      }
   }

   @Override
   public Map<String, Object> getDimensions() {
      CdpResponse response = this.cdp.method("Browser.getWindowForTarget").send();
      Integer windowId = response.getResult("windowId");
      if (windowId != null) {
         CdpResponse boundsResponse = this.cdp.method("Browser.getWindowBounds").param("windowId", windowId).send();
         return boundsResponse.getResult("bounds");
      } else {
         return Map.of();
      }
   }

   @Override
   public void setDimensions(Map<String, Object> dimensions) {
      logger.debug("set dimensions: {}", dimensions);
      CdpResponse response = this.cdp.method("Browser.getWindowForTarget").send();
      Integer windowId = response.getResult("windowId");
      if (windowId != null) {
         if (dimensions.containsKey("width") || dimensions.containsKey("height") || dimensions.containsKey("x") || dimensions.containsKey("y")) {
            this.cdp.method("Browser.setWindowBounds").param("windowId", windowId).param("bounds", Map.of("windowState", "normal")).send();
         }

         this.cdp.method("Browser.setWindowBounds").param("windowId", windowId).param("bounds", dimensions).send();
      }
   }

   @Override
   public byte[] pdf(Map<String, Object> options) {
      logger.debug("generate PDF");
      CdpMessage message = this.cdp.method("Page.printToPDF");
      if (options != null) {
         options.forEach(message::param);
      }

      CdpResponse response = message.send();
      String base64 = response.getResultAsString("data");
      return Base64.getDecoder().decode(base64);
   }

   @Override
   public byte[] pdf() {
      return this.pdf(null);
   }

   @Override
   public void refresh() {
      logger.debug("refresh page");
      this.beginSupersedingNavigation();

      try {
         this.cdp.method("Page.reload").send();
         this.waitForPageLoad(this.options.getPageLoadStrategy());
      } finally {
         this.supersededLoaderId = null;
      }
   }

   @Override
   public void reload() {
      logger.debug("reload page (ignore cache)");
      this.beginSupersedingNavigation();

      try {
         this.cdp.method("Page.reload").param("ignoreCache", true).send();
         this.waitForPageLoad(this.options.getPageLoadStrategy());
      } finally {
         this.supersededLoaderId = null;
      }
   }

   private void beginSupersedingNavigation() {
      this.domContentEventFired = false;
      this.framesStillLoading.clear();
      this.supersededLoaderId = this.committedLoaderId;
   }

   @Override
   public void back() {
      logger.debug("navigate back");
      CdpResponse response = this.cdp.method("Page.getNavigationHistory").send();
      Integer currentIndex = response.getResult("currentIndex");
      List<Map<String, Object>> entries = response.getResult("entries");
      if (currentIndex != null && currentIndex > 0 && entries != null) {
         Map<String, Object> prevEntry = entries.get(currentIndex - 1);
         Integer entryId = (Integer)prevEntry.get("id");
         if (entryId != null) {
            this.beginSupersedingNavigation();
            this.historyTargetUrl = (String)prevEntry.get("url");

            try {
               this.cdp.method("Page.navigateToHistoryEntry").param("entryId", entryId).send();
               this.waitForPageLoad(this.options.getPageLoadStrategy());
            } finally {
               this.supersededLoaderId = null;
               this.historyTargetUrl = null;
            }
         }
      }
   }

   @Override
   public void forward() {
      logger.debug("navigate forward");
      CdpResponse response = this.cdp.method("Page.getNavigationHistory").send();
      Integer currentIndex = response.getResult("currentIndex");
      List<Map<String, Object>> entries = response.getResult("entries");
      if (currentIndex != null && entries != null && currentIndex < entries.size() - 1) {
         Map<String, Object> nextEntry = entries.get(currentIndex + 1);
         Integer entryId = (Integer)nextEntry.get("id");
         if (entryId != null) {
            this.beginSupersedingNavigation();
            this.historyTargetUrl = (String)nextEntry.get("url");

            try {
               this.cdp.method("Page.navigateToHistoryEntry").param("entryId", entryId).send();
               this.waitForPageLoad(this.options.getPageLoadStrategy());
            } finally {
               this.supersededLoaderId = null;
               this.historyTargetUrl = null;
            }
         }
      }
   }

   @Override
   public void activate() {
      logger.debug("activate window");
      this.cdp.method("Page.bringToFront").send();
   }

   @Override
   public Mouse mouse() {
      return new CdpMouse(this.cdp);
   }

   @Override
   public Mouse mouse(String locator) {
      Map<String, Object> pos = this.position(locator, true);
      double x = ((Number)pos.get("x")).doubleValue();
      double y = ((Number)pos.get("y")).doubleValue();
      double width = ((Number)pos.get("width")).doubleValue();
      double height = ((Number)pos.get("height")).doubleValue();
      return new CdpMouse(this.cdp, x + width / 2.0, y + height / 2.0);
   }

   @Override
   public Mouse mouse(Number x, Number y) {
      return new CdpMouse(this.cdp, x.doubleValue(), y.doubleValue());
   }

   @Override
   public Keys keys() {
      if (this.keysInstance == null) {
         this.keysInstance = new CdpKeys(this.cdp);
      }

      return this.keysInstance;
   }

   @Override
   public List<String> getPages() {
      List<String> pages = new ArrayList<>();

      for (Map<String, Object> target : this.pageTargets()) {
         pages.add((String)target.get("targetId"));
      }

      return pages;
   }

   private String lookupBrowserContextId(String targetId) {
      if (targetId == null) {
         return null;
      } else {
         try {
            CdpResponse response = this.cdp.browserMethod("Target.getTargets").send();
            List<Map<String, Object>> targets = response.getResult("targetInfos");
            if (targets != null) {
               for (Map<String, Object> target : targets) {
                  if (targetId.equals(target.get("targetId"))) {
                     return (String)target.get("browserContextId");
                  }
               }
            }
         } catch (Exception var6) {
            logger.debug("could not resolve browser context for target {}: {}", targetId, var6.getMessage());
         }

         return null;
      }
   }

   private List<Map<String, Object>> pageTargets() {
      CdpResponse response = this.cdp.browserMethod("Target.getTargets").send();
      List<Map<String, Object>> targets = response.getResult("targetInfos");
      List<Map<String, Object>> pages = new ArrayList<>();
      if (targets != null) {
         for (Map<String, Object> target : targets) {
            if ("page".equals(target.get("type")) && Objects.equals(this.browserContextId, target.get("browserContextId"))) {
               pages.add(target);
            }
         }
      }

      return pages;
   }

   public List<Map<String, Object>> getTargetInfos() {
      List<Map<String, Object>> result = new ArrayList<>();

      for (Map<String, Object> target : this.pageTargets()) {
         Map<String, Object> info = new LinkedHashMap<>();
         String targetId = (String)target.get("targetId");
         info.put("targetId", targetId);
         info.put("url", target.get("url"));
         info.put("title", target.get("title"));
         info.put("active", targetId != null && targetId.equals(this.currentTargetId));
         result.add(info);
      }

      return result;
   }

   @Override
   public void switchPage(String titleOrUrl) {
      logger.debug("switch page by title/url: {}", titleOrUrl);
      int interval = this.fastPollInterval();
      int maxAttempts = this.fastPollAttempts();
      int attempt = 0;

      while (true) {
         String targetId = this.findPageTarget(titleOrUrl);
         if (targetId != null) {
            this.activateTarget(targetId);
            return;
         }

         if (attempt >= maxAttempts) {
            throw new DriverException("no page found matching: " + titleOrUrl);
         }

         sleep(interval);
         attempt++;
      }
   }

   private String findPageTarget(String titleOrUrl) {
      for (Map<String, Object> target : this.pageTargets()) {
         String title = (String)target.get("title");
         String url = (String)target.get("url");
         if (title != null && title.contains(titleOrUrl) || url != null && url.contains(titleOrUrl)) {
            return (String)target.get("targetId");
         }
      }

      return null;
   }

   @Override
   public void switchPageById(String targetId) {
      logger.debug("switch page by targetId: {}", targetId);
      if (targetId == null) {
         throw new DriverException("targetId is null");
      } else {
         int interval = this.fastPollInterval();
         int maxAttempts = this.fastPollAttempts();

         for (int attempt = 0; !this.getPages().contains(targetId); attempt++) {
            if (attempt >= maxAttempts) {
               throw new DriverException("no page found with targetId: " + targetId);
            }

            sleep(interval);
         }

         this.activateTarget(targetId);
      }
   }

   @Override
   public List<Map<String, Object>> drainOpenedTargets() {
      if (this.openedTargets.isEmpty()) {
         return Collections.emptyList();
      } else {
         List<Map<String, Object>> drained = new ArrayList<>();

         Map<String, Object> entry;
         while ((entry = this.openedTargets.poll()) != null) {
            drained.add(entry);
         }

         return drained;
      }
   }

   @Override
   public void switchPage(int index) {
      logger.debug("switch page by index: {}", index);
      List<String> pages = this.getPages();
      if (index >= 0 && index < pages.size()) {
         this.activateTarget(pages.get(index));
      } else {
         throw new DriverException("no page at index: " + index);
      }
   }

   @Override
   public Finder rightOf(String locator) {
      return new Finder(this, locator, Finder.Position.RIGHT_OF);
   }

   @Override
   public Finder leftOf(String locator) {
      return new Finder(this, locator, Finder.Position.LEFT_OF);
   }

   @Override
   public Finder above(String locator) {
      return new Finder(this, locator, Finder.Position.ABOVE);
   }

   @Override
   public Finder below(String locator) {
      return new Finder(this, locator, Finder.Position.BELOW);
   }

   @Override
   public Finder near(String locator) {
      return new Finder(this, locator, Finder.Position.NEAR);
   }

   @Override
   public void intercept(List<String> patterns, InterceptHandler handler) {
      logger.debug("enable request interception for patterns: {}", patterns);
      this.interceptHandler = handler;
      List<Map<String, Object>> requestPatterns = new ArrayList<>();

      for (String pattern : patterns) {
         requestPatterns.add(Map.of("urlPattern", pattern));
      }

      this.cdp.method("Fetch.enable").param("patterns", requestPatterns).send();
   }

   @Override
   public void intercept(InterceptHandler handler) {
      this.intercept(List.of("*"), handler);
   }

   @Override
   public void stopIntercept() {
      logger.debug("disable request interception");
      this.interceptHandler = null;
      this.cdp.method("Fetch.disable").send();
   }

   private void activateTarget(String targetId) {
      this.currentTargetId = targetId;
      this.cdp.browserMethod("Target.activateTarget").param("targetId", targetId).send();
      CdpResponse attachResponse = this.cdp.browserMethod("Target.attachToTarget").param("targetId", targetId).param("flatten", true).send();
      String sessionId = attachResponse.getResultAsString("sessionId");
      if (sessionId != null) {
         this.cdp.setSessionId(sessionId);
         this.pageSessionId = sessionId;
      }

      CdpResponse frameResponse = this.cdp.method("Page.getFrameTree").send();
      this.mainFrameId = frameResponse.getResult("frameTree.frame.id");
      this.currentFrame = null;
      this.committedLoaderId = frameResponse.getResultAsString("frameTree.frame.loaderId");
      this.domContentLoaderId = null;
      this.invalidateMainContext();
      this.cdp.method("Page.enable").send();
      this.cdp.method("Runtime.enable").send();
      this.cdp.method("Page.setLifecycleEventsEnabled").param("enabled", true).send();
      this.oopifTargets.clear();
      this.oopifSessions.clear();
      this.frameContextReady.clear();
      this.cdp.method("Target.setAutoAttach").param("autoAttach", true).param("waitForDebuggerOnStart", false).param("flatten", true).send();
      this.awaitMainContext(this.options.getTimeoutDuration().toMillis());
   }

   private int fastPollInterval() {
      return Math.min(this.options.getRetryInterval(), 100);
   }

   private int fastPollAttempts() {
      int interval = this.fastPollInterval();
      return Math.max(
         this.options.getRetryCount(), (int)Math.ceil((double)this.options.getRetryCount() * this.options.getRetryInterval() / Math.max(1, interval))
      );
   }

   private boolean retry(String description, Supplier<Boolean> condition, int maxAttempts, int interval) {
      if (Boolean.TRUE.equals(condition.get())) {
         return true;
      } else {
         logger.warn("retry started: {} (max {} attempts, {}ms interval)", new Object[]{description, maxAttempts, interval});

         for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
               logger.warn("retry aborted (thread interrupted): {}", description);
               return false;
            }

            sleep(interval);
            if (Boolean.TRUE.equals(condition.get())) {
               logger.warn("retry succeeded after {} attempt(s): {}", attempt, description);
               return true;
            }

            logger.warn("retry attempt {}/{} failed for: {}", new Object[]{attempt, maxAttempts, description});
         }

         logger.warn("retry FAILED after {} attempts: {} | {}", new Object[]{maxAttempts, description, this.getDriverState()});
         return false;
      }
   }

   private boolean retry(String description, Supplier<Boolean> condition) {
      return this.retry(description, condition, this.options.getRetryCount(), this.options.getRetryInterval());
   }

   private String getDriverState() {
      StringBuilder sb = new StringBuilder();
      sb.append("url=");

      try {
         sb.append(this.getUrl());
      } catch (Exception var3) {
         sb.append("(error: ").append(var3.getMessage()).append(")");
      }

      sb.append(", domContentEventFired=").append(this.domContentEventFired);
      sb.append(", framesLoading=").append(this.framesStillLoading.size());
      sb.append(", mainFrameId=").append(this.mainFrameId);
      if (this.currentFrame != null) {
         sb.append(", currentFrame=").append(this.currentFrame.id);
      }

      sb.append(", frameContexts=").append(this.frameContexts.size());
      sb.append(", terminated=").append(this.terminated);
      return sb.toString();
   }

   private static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
      }
   }

   private static <T> T pollFor(long timeoutMs, long intervalMs, Supplier<T> attempt) {
      long deadline = System.currentTimeMillis() + timeoutMs;

      while (true) {
         T result = attempt.get();
         if (result != null) {
            return result;
         }

         if (System.currentTimeMillis() >= deadline || Thread.currentThread().isInterrupted()) {
            return null;
         }

         sleep(intervalMs);
      }
   }

   private static boolean pollUntil(long timeoutMs, long intervalMs, Supplier<Boolean> condition) {
      return pollFor(timeoutMs, intervalMs, () -> Boolean.TRUE.equals(condition.get()) ? Boolean.TRUE : null) != null;
   }

   private static class Frame {
      final String id;
      final String url;
      final String name;

      Frame(String id, String url, String name) {
         this.id = id;
         this.url = url;
         this.name = name;
      }

      @Override
      public String toString() {
         return "Frame{id='" + this.id + "', url='" + this.url + "', name='" + this.name + "'}";
      }
   }

   private record InitScript(String source, List<String> deps, String cdpId) {
   }

   private static enum JsProbe {
      ALIVE,
      DEAD,
      ERROR;
   }
}
