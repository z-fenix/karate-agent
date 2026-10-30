package io.karatelabs.ext.agent.runtime;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.JsLexer;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLoop {
   private static final Logger LOGGER = LoggerFactory.getLogger(AgentLoop.class);
   private final io.karatelabs.ext.agent.llm.LlmClient llmClient;
   private final ToolExecutor toolExecutor;
   private final AgentRunListener listener;
   private final Executor callbackExecutor;
   private final int maxIterations;
   private final RetryPolicy retryPolicy;
   private final OutcomeValidator outcomeValidator;
   private final long toolTimeoutMillis;
   private final AgentRunState state;
   private final List<AgentExtension> extensions;
   private final List<AgentHook> hooks = new ArrayList<>();
   private final List<String> terminalCallNames = new ArrayList<>();
   private String finishInstruction;
   private AgentExtension finishInstructionExtension;
   private final List<ExitCondition> exitConditions = new ArrayList<>();
   private final io.karatelabs.ext.agent.runtime.AgentLoop.LoopContext context = new io.karatelabs.ext.agent.runtime.AgentLoop.LoopContext();
   private volatile ExecutorService toolExecutorService;
   private volatile boolean toolUninterruptible;
   private int stuckRecoveryAttempts;
   private AgentOutcome pendingOutcome;
   private boolean endingStatementCompleted;
   private boolean terminationStatementRequested;
   private static final int MAX_STUCK_RECOVERY_ATTEMPTS = 3;
   private final Set<String> initialBindingNames = new HashSet<>();
   private final Map<String, Object> bindingsSnapshot = new LinkedHashMap<>();
   private final AtomicBoolean running = new AtomicBoolean(false);
   private final AtomicBoolean stopped = new AtomicBoolean(false);
   private final AtomicReference<AgentOutcome> externalOutcome = new AtomicReference<>();
   private final AtomicBoolean paused = new AtomicBoolean(false);
   private volatile CountDownLatch resumeLatch;
   private volatile String resumeMessage;
   private String lastToolCode;
   private String lastToolResult;
   private int repeatedToolCalls;
   private int unproductiveIterations;
   private static final int MAX_UNPRODUCTIVE_ITERATIONS = 3;
   private final String taskSuffix;
   private final int historyWindow;
   public static final int MAX_HISTORY_ENTRIES = 10;
   public static final int MAX_HISTORY_CHARACTERS = 24000;
   private final List<io.karatelabs.ext.agent.llm.LlmMessage> conversationHistory;
   private volatile boolean lastResponseTruncated;
   private static final String NEXT_REPLY_INSTRUCTION = "Next reply: plan in a few // lines, not prose, and send one step per block (one file or one call).";
   private static final int MAX_OUTPUT_LENGTH = 4000;
   private static final Pattern MISSING_METHOD_PATTERN = Pattern.compile("TypeError: ((?:[A-Za-z_$][\\w$]*\\.)*[A-Za-z_$][\\w$]*)\\.([A-Za-z_$][\\w$]*) is not a function");
   private volatile CountDownLatch pauseLatch;

   private AgentLoop(io.karatelabs.ext.agent.runtime.AgentLoop.Builder var1) {
      this.llmClient = var1.llmClient;
      this.toolExecutor = (ToolExecutor)(var1.toolExecutor != null ? var1.toolExecutor : new JavaScriptExecutor());
      this.listener = var1.listener;
      this.callbackExecutor = var1.callbackExecutor != null ? var1.callbackExecutor : Runnable::run;
      this.maxIterations = var1.maxIterations;
      this.retryPolicy = var1.retryPolicy != null ? var1.retryPolicy : io.karatelabs.ext.agent.runtime.RetryPolicy.defaults();
      this.outcomeValidator = var1.outcomeValidator;
      this.taskSuffix = var1.taskSuffix;
      this.historyWindow = var1.historyWindow;
      this.conversationHistory = buildHistoryMessages(var1.conversationHistory);
      this.extensions = var1.extensions;
      this.state = new AgentRunState();
      this.state.setHistoryWindow(var1.historyWindow);
      ArrayList var2 = new ArrayList();

      try {
         this.toolTimeoutMillis = this.initializeExtensions(var1, var2);
      } catch (Throwable var5) {
         for (int var4 = var2.size() - 1; var4 >= 0; var4--) {
            this.closeExtension((AgentExtension)var2.get(var4));
         }

         throw var5;
      }
   }

   private long initializeExtensions(io.karatelabs.ext.agent.runtime.AgentLoop.Builder var1, List<AgentExtension> var2) {
      StringBuilder var3 = new StringBuilder();
      if (var1.systemPrompt != null) {
         var3.append(var1.systemPrompt);
      }

      for (AgentExtension var5 : this.extensions) {
         var5.init(this.context);
         var2.add(var5);
         String var6 = var5.promptFragment();
         if (var6 != null && !var6.isEmpty()) {
            if (var3.length() > 0) {
               var3.append("\n\n");
            }

            var3.append(var6);
         }
      }

      this.state.setSystemPrompt(var3.length() > 0 ? var3.toString() : null);

      for (AgentExtension var10 : this.extensions) {
         var10.bindings().forEach((var1x, var2x) -> {
            this.toolExecutor.a(var1x, var2x);
            this.bindingsSnapshot.put(var1x, var2x);
         });
         this.hooks.addAll(var10.hooks());
         this.exitConditions.addAll(var10.exitConditions());
         this.terminalCallNames.addAll(var10.terminalCallNames());
         if (this.finishInstruction == null) {
            this.finishInstruction = var10.finishInstruction();
            if (this.finishInstruction != null) {
               this.finishInstructionExtension = var10;
            }
         }
      }

      this.exitConditions.addAll(var1.exitConditions);
      if (var1.bindings != null) {
         var1.bindings.forEach((var1x, var2x) -> {
            this.toolExecutor.a(var1x, var2x);
            this.bindingsSnapshot.put(var1x, var2x);
         });
      }

      this.initialBindingNames.addAll(this.toolExecutor.a().keySet());
      if (var1.toolTimeoutMillis >= 0L) {
         return var1.toolTimeoutMillis;
      } else {
         long var9 = 0L;

         for (AgentExtension var7 : this.extensions) {
            var9 = Math.max(var9, var7.toolTimeoutMs());
         }

         return var9;
      }
   }

   public AgentRunState run(String var1) {
      if (this.running.getAndSet(true)) {
         throw new IllegalStateException("Harness loop is already running");
      } else {
         try {
            String var2 = var1;
            if (this.taskSuffix != null && !this.taskSuffix.isEmpty()) {
               var2 = var1 + "\n\n" + this.taskSuffix;
            }

            this.state.setTask(var1);
            this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user(var2));

            while (!this.stopped.get() && this.state.iterations() < this.maxIterations) {
               if (!this.awaitResumeOrIntervention()) {
                  if (this.stopped.get()) {
                     break;
                  }

                  this.state.incrementIterations();
                  this.dispatchEvent(() -> this.listener.onIterationStart(this.state.iterations()));

                  for (AgentHook var4 : this.hooks) {
                     var4.a(this.context);
                  }

                  AgentOutcome var33 = this.checkExitConditions();
                  if (var33 != null) {
                     if (this.pendingOutcome != null) {
                        this.finishPendingOutcome((Object)null);
                        break;
                     }

                     if (this.handleOutcome(var33)) {
                        break;
                     }
                  }

                  if (this.historyWindow >= 0) {
                     this.trimHistory();
                  }

                  List<io.karatelabs.ext.agent.llm.LlmMessage> var34 = this.state.messages();

                  for (AgentHook var6 : this.hooks) {
                     var34 = var6.a(this.context, (List<io.karatelabs.ext.agent.llm.LlmMessage>)var34);
                  }

                  if (!this.conversationHistory.isEmpty()) {
                     ArrayList var35 = new ArrayList(this.conversationHistory.size() + var34.size());
                     var35.addAll(this.conversationHistory);
                     var35.addAll((Collection)var34);
                     var34 = var35;
                  }

                  int var36 = this.state.iterations();
                  this.dispatchEvent(() -> this.listener.onLlmRequest(var36));

                  io.karatelabs.ext.agent.llm.LlmResponse var37;
                  try {
                     var37 = this.requestLlmWithRetry(var36, (List<io.karatelabs.ext.agent.llm.LlmMessage>)var34);
                     if (var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.REFUSAL) {
                        this.state.addUsage(var37);
                        io.karatelabs.ext.agent.llm.LlmResponse refusal = var37;
                        this.dispatchEvent(() -> this.listener.onLlmResponse(var36, refusal));
                        this.dispatchEvent(() -> this.listener.onHint(var36, "provider-refusal", "the provider refused the request (" + describeRefusal(refusal) + "); retrying once"));
                        var37 = this.requestLlmWithRetry(var36, (List<io.karatelabs.ext.agent.llm.LlmMessage>)var34);
                     }
                  } catch (Exception var31) {
                     this.handleRunException(var31);
                     if (this.pendingOutcome != null) {
                        this.finishPendingOutcome((Object)null);
                     }
                     break;
                  }

                  this.state.addUsage(var37);
                  this.state.b(var37.rawFinishReason());
                  this.lastResponseTruncated = var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS;
                  io.karatelabs.ext.agent.llm.LlmResponse var7 = var37;
                  this.dispatchEvent(() -> this.listener.onLlmResponse(var36, var7));
                  String var8 = var37.text();
                  if (this.isStopRequested()) {
                     break;
                  }

                  if (!this.awaitResumeOrIntervention()) {
                     if (this.isStopRequested()) {
                        break;
                     }

                     if (var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.REFUSAL) {
                        if (this.pendingOutcome != null) {
                           this.finishPendingOutcome((Object)null);
                           break;
                        }

                        if (++this.unproductiveIterations >= 3) {
                           this.stopForLackOfProgress("The provider refused the request (" + describeRefusal(var37) + ")");
                           break;
                        }
                     } else {
                        var var9 = this.extractCode(var8);
                        if (var9.isEmpty()) {
                           boolean var38 = var8 == null || var8.isBlank();
                           if (!var38) {
                              this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.assistant(var8));
                           }

                           if (this.pendingOutcome != null) {
                              this.run(var38 ? null : var8.trim());
                              break;
                           }

                           AgentOutcome var39 = !var38 && var37.finishReason() != io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS ? this.parseProseOutcome(var8.trim()) : null;
                           if (var39 != null) {
                              this.applyOutcome(var39);
                              break;
                           }

                           boolean var42 = var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS;
                           if (!var42 && ++this.unproductiveIterations >= 3) {
                              this.stopForLackOfProgress("The model returned " + (var38 ? "an empty response" : "no actionable code block"));
                              break;
                           }

                           String var43 = !this.terminalCallNames.isEmpty()
                              ? "If the task is complete (or you are stuck), make the block a single " + String.join(" or ", this.terminalCallNames) + " call."
                              : (
                                 this.finishInstruction != null
                                    ? this.finishInstruction
                                    : "If the task is complete (or you are stuck), make the block a single terminal call that finishes the task."
                              );
                           this.state
                              .addMessage(
                                 io.karatelabs.ext.agent.llm.LlmMessage.user(
                                    var42
                                       ? (
                                             var38
                                                ? "Your reply hit the output-token ceiling before it began, so NOTHING was executed. "
                                                : "Your reply hit the output-token ceiling and was CUT OFF before the block closed, so NOTHING was executed. "
                                          )
                                          + truncationHint(var37)
                                          + "\n"
                                          + var43
                                       : (
                                          var38
                                             ? "Your last response was empty. Write a ```js block with your next action, or finish the task."
                                             : "Your reply had NO ```js block, so NOTHING was executed. Do not plan in prose. Reply with ONLY one fenced block:\n```js\n// <one-line plan>\n<your next call>\n```\n"
                                                + var43
                                       )
                                 )
                              );
                        } else {
                           this.unproductiveIterations = 0;
                           this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.assistant(var8));
                           int var10 = var9.size() - 1;
                           if (var10 > 0) {
                              LOGGER.debug("Ignoring {} extra code blocks (executing first only)", var10);
                           }

                           String var11 = (String)var9.get(0);
                           ArrayList var13 = new ArrayList();
                           ToolResult var14 = null;
                           this.dispatchEvent(() -> this.listener.onToolRequest(var11));
                           String var41;
                           if (var11.equals(this.lastToolCode) && this.lastToolResult != null) {
                              this.repeatedToolCalls++;
                              var41 = "Same result as previous call (repeated "
                                 + this.repeatedToolCalls
                                 + " times). Nothing changed. Try a different action or finish the task.\n[0ms | iteration "
                                 + this.state.iterations()
                                 + " of "
                                 + this.maxIterations
                                 + "]";
                              if (this.repeatedToolCalls >= 3) {
                                 String var44 = "You have repeated the same call " + this.repeatedToolCalls + " times. You MUST try something different or finish the task.";
                                 var41 = var41 + "\n⚠ " + var44;
                                 int var17 = this.state.iterations();
                                 this.dispatchEvent(() -> this.listener.onHint(var17, "repeat-count", var44));
                              }
                           } else {
                              this.repeatedToolCalls = 0;
                              long var16 = System.currentTimeMillis();

                              try {
                                 var14 = this.executeTool(var11);
                                 var41 = var14.value() != null ? Json.stringifyStrict(var14.value()) : "null";
                                 var41 = var41 + this.renderArtifacts(var14.artifacts(), var13, this.state.iterations());
                                 String var18 = this.findMissingMemberInCode(var11);
                                 if (var18 != null) {
                                    var41 = var41 + "\n⚠ " + var18;
                                    int var19 = this.state.iterations();
                                    this.dispatchEvent(() -> this.listener.onHint(var19, "wrong-member", var18));
                                 }

                                 LOGGER.debug("Tool result: {}", var41);
                              } catch (ControlSignal var29) {
                                 if (this.handleControlSignal(var29)) {
                                    break;
                                 }
                                 continue;
                              } catch (io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException var30) {
                                 var41 = this.formatToolError(var30);
                                 LOGGER.warn("Tool execution error: {}", var30.getMessage());
                              }

                              long var50 = System.currentTimeMillis() - var16;
                              int var20 = this.state.iterations();
                              var41 = var41 + "\n[" + var50 + "ms | iteration " + var20 + " of " + this.maxIterations + "]";
                              if (var7.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS) {
                                 String var21 = "your reply hit the output-token ceiling and was CUT OFF: a parse error here is that cut, and a block that ran may have written only part, so read back what landed. "
                                    + truncationHint(var7);
                                 var41 = var41 + "\n⚠ " + var21;
                                 this.dispatchEvent(() -> this.listener.onHint(var20, "response-truncated", var21));
                              }

                              if (this.pendingOutcome == null && var20 >= this.maxIterations * 4 / 5) {
                                 int var58 = this.maxIterations - var20;
                                 String var22 = this.terminalCallNames.isEmpty() ? "the terminal call that finishes the task" : String.join(" or ", this.terminalCallNames);
                                 String var23;
                                 if (var58 <= 0) {
                                    var23 = "Iteration budget exhausted — the run ends with this result.";
                                 } else if (this.terminalCallNames.isEmpty() && this.finishInstruction != null) {
                                    var23 = (var58 == 1 ? "FINAL iteration. " : "Only " + var58 + " iterations left. ")
                                       + (this.finishInstructionExtension != null ? this.finishInstructionExtension.finishInstruction(var58) : this.finishInstruction);
                                 } else if (var58 == 1) {
                                    var23 = "FINAL iteration — your NEXT block MUST be the terminal call: "
                                       + var22
                                       + ". Report what you already confirmed; do NOT observe again.";
                                 } else {
                                    var23 = "Only "
                                       + var58
                                       + " iterations left — your NEXT block must END with "
                                       + var22
                                       + ". A last screenshot/verification goes in that SAME block, terminal call last.";
                                 }

                                 var41 = var41 + "\n⚠ " + var23;
                                 this.dispatchEvent(() -> this.listener.onHint(var20, "iteration-budget", var23));
                              }

                              if (var10 > 0) {
                                 String var59 = firstCodeLine((String)var9.get(1));
                                 String var60 = var10
                                    + " extra code block(s) were DISCARDED — they did NOT run"
                                    + (var59 != null ? " (the block starting `" + var59 + "` never executed; the page was NOT acted on)" : "")
                                    + ". Only your FIRST ```js block executes per response. Write ONE block, read the result, then decide your next action.";
                                 var41 = var41 + "\n⚠ " + var60;
                                 this.dispatchEvent(() -> this.listener.onHint(var20, "extra-blocks", var60));
                              }

                              this.lastToolCode = var11;
                              this.lastToolResult = var41;
                           }

                           for (AgentHook var47 : this.hooks) {
                              var41 = var47.a(this.context, var11, var14, var41);
                           }

                           for (AgentHook var48 : this.hooks) {
                              Optional var51 = var48.afterTool(this.context, var11, var14, var41);
                              if (var51.isPresent() && !((String)var51.get()).isEmpty()) {
                                 var41 = var41 + "\n⚠ " + (String)var51.get();
                              }
                           }

                           String var49 = var41;
                           this.dispatchEvent(() -> this.listener.onToolResult(var11, var49));
                           if (var13.isEmpty()) {
                              this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user(var41));
                           } else {
                              io.karatelabs.ext.agent.llm.LlmMessage.Builder var52 = io.karatelabs.ext.agent.llm.LlmMessage.userBuilder().text(var41);

                              for (io.karatelabs.ext.agent.runtime.AgentArtifact var57 : (Iterable<io.karatelabs.ext.agent.runtime.AgentArtifact>) (Iterable<?>) (var13)) {
                                 var52.image(var57.b(), var57.e() != null ? var57.e() : "image/png");
                              }

                              this.state.addMessage(var52.build());
                           }

                           if (this.toolUninterruptible) {
                              if (this.pendingOutcome != null) {
                                 this.finishPendingOutcome((Object)null);
                              } else {
                                 this.applyOutcome(io.karatelabs.ext.agent.runtime.AgentOutcome.terminated("tool execution timed out (" + this.toolTimeoutMillis + "ms) and could not be interrupted"));
                              }
                              break;
                           }

                           if (this.pendingOutcome != null) {
                              Object var54 = var14 != null ? var14.value() : null;
                              if (formatClosingStatement(var54) != null || this.terminationStatementRequested || this.state.iterations() >= this.maxIterations) {
                                 this.finishPendingOutcome(var54);
                                 break;
                              }

                              this.terminationStatementRequested = true;
                              this.state
                                 .addMessage(
                                    io.karatelabs.ext.agent.llm.LlmMessage.user(
                                       "That was a call. The run is over, its verdict cannot move, and nothing was recorded. Reply with exactly this shape — a string literal, no call:\n```js\n'<what you established, and what you did NOT>'\n```"
                                    )
                                 );
                           } else {
                              AgentOutcome var53 = this.externalOutcome.getAndSet(null);
                              if (var53 != null && this.handleOutcome(var53)) {
                                 break;
                              }

                              AgentOutcome var56 = this.checkExitConditionsAfterStep(new ToolStep(var11, var14, var41));
                              if (var56 != null && this.handleOutcome(var56)) {
                                 break;
                              }

                              if (var53 == null && var56 == null) {
                                 this.stuckRecoveryAttempts = 0;
                              }

                              if (this.stopped.get()) {
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (this.pendingOutcome != null) {
               this.finishPendingOutcome((Object)null);
            }

            if (this.state.status() == io.karatelabs.ext.agent.runtime.AgentRunState.Status.RUNNING) {
               this.state.setStatus(io.karatelabs.ext.agent.runtime.AgentRunState.Status.TERMINATED);
               this.state.setSuccess(false);
               if (this.state.iterations() >= this.maxIterations) {
                  this.state.setLastError("Max iterations (" + this.maxIterations + ") reached");
               } else if (this.state.lastError() == null) {
                  this.state.setLastError("stopped");
               }
            }

            this.dispatchEvent(() -> this.listener.onComplete(this.state));
         } finally {
            this.running.set(false);
            this.closeResources();
         }

         return this.state;
      }
   }

   private io.karatelabs.ext.agent.llm.LlmResponse requestLlmWithRetry(int var1, List<io.karatelabs.ext.agent.llm.LlmMessage> var2) {
      int var3 = 0;

      while (true) {
         StringBuilder var4 = new StringBuilder();
         AtomicBoolean var5 = new AtomicBoolean();

         try {
            return this.llmClient.a(var2, this.state.systemPrompt(), var4x -> {
               if (var5.compareAndSet(false, true)) {
                  this.dispatchEvent(() -> this.listener.onLlmResponseStart(var1));
               }

               var4.append(var4x);
               this.dispatchEvent(() -> this.listener.onTextChunk(var4x, var4.toString()));
            });
         } catch (Exception var11) {
            boolean var7 = var11 instanceof io.karatelabs.ext.agent.llm.LlmException var8 && var8.e();
            if (!var7 || var3 >= this.retryPolicy.maxRetries()) {
               throw var11;
            }

            long var12 = this.retryPolicy.backoffMillis(var3);
            int var10 = var3 + 1;
            this.dispatchEvent(
               () -> this.listener
                  .onHint(var1, "llm-retry", "retryable LLM error (attempt " + var10 + "/" + this.retryPolicy.maxRetries() + "), backing off " + var12 + "ms: " + var11.getMessage())
            );
            if (!this.waitForRetry(var12)) {
               throw var11;
            }

            var3++;
         }
      }
   }

   private boolean waitForRetry(long var1) {
      long var3 = Math.max(0L, var1);

      while (var3 > 0L) {
         if (this.stopped.get()) {
            return false;
         }

         long var5 = Math.min(100L, var3);

         try {
            Thread.sleep(var5);
         } catch (InterruptedException var8) {
            Thread.currentThread().interrupt();
            return false;
         }

         var3 -= var5;
      }

      return !this.stopped.get();
   }

   private void stopForLackOfProgress(String var1) {
      String var2 = var1 + " for " + this.unproductiveIterations + " turns in a row — stopping (no progress)";
      LOGGER.warn("Loop terminating early: {}", var2);
      this.state.setStatus(io.karatelabs.ext.agent.runtime.AgentRunState.Status.TERMINATED);
      this.state.setLastError(var2 + ". " + this.state.iterations() + " iterations used.");
   }

   private static String describeRefusal(io.karatelabs.ext.agent.llm.LlmResponse var0) {
      String var1 = var0.rawFinishReason() != null ? var0.rawFinishReason() : "refusal";
      String var2 = var0.refusal();
      return var2 != null && !var2.isBlank() ? var1 + ": " + var2.strip() : var1;
   }

   static String truncationHint(io.karatelabs.ext.agent.llm.LlmResponse var0) {
      String var1 = var0.text() == null ? "" : var0.text();
      int var2 = var1.indexOf("```");
      int var3 = (var2 < 0 ? var1 : var1.substring(0, var2)).strip().length();
      if (var3 > 1000) {
         return var3
            + " chars of prose before the block spent it. Next reply: plan in a few // lines, not prose, and send one step per block (one file or one call).";
      } else {
         String var4 = var0.reasoning();
         return var4 != null && !var4.isBlank()
            ? (var1.isBlank() ? "Reasoning spent it before the reply began. " : "Reasoning spent most of it. ")
               + "Next reply: send one step per block (one file or one call)."
            : "Next reply: plan in a few // lines, not prose, and send one step per block (one file or one call).";
      }
   }

   static List<io.karatelabs.ext.agent.llm.LlmMessage> buildHistoryMessages(List<ConversationEntry> var0) {
      ArrayList<io.karatelabs.ext.agent.llm.LlmMessage> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         int var2 = var0.size();
         long var3 = 0L;

         for (ConversationEntry var6 : var0) {
            var3 += conversationEntryLength(var6);
         }

         boolean[] var8 = new boolean[var0.size()];

         for (int var9 = 0; var9 < var0.size() && (var2 > 10 || var3 > 24000L); var9++) {
            if (!((ConversationEntry)var0.get(var9)).pinned()) {
               var8[var9] = true;
               var2--;
               var3 -= conversationEntryLength((ConversationEntry)var0.get(var9));
            }
         }

         for (int var10 = 0; var10 < var0.size(); var10++) {
            ConversationEntry var7 = (ConversationEntry)var0.get(var10);
            if (!var8[var10]) {
               if (var7.userText() != null && !var7.userText().isBlank()) {
                  var1.add(io.karatelabs.ext.agent.llm.LlmMessage.user(var7.userText()));
               }

               if (var7.assistantText() != null && !var7.assistantText().isBlank()) {
                  var1.add(io.karatelabs.ext.agent.llm.LlmMessage.assistant(var7.assistantText()));
               }
            }
         }

         return var1;
      }
   }

   private static int conversationEntryLength(ConversationEntry var0) {
      return (var0.userText() == null ? 0 : var0.userText().length()) + (var0.assistantText() == null ? 0 : var0.assistantText().length());
   }

   private AgentOutcome checkExitConditionsAfterStep(ToolStep var1) {
      for (ExitCondition var3 : this.exitConditions) {
         Optional var4 = var3.a(this.state, var1);
         if (var4.isPresent()) {
            return (AgentOutcome)var4.get();
         }
      }

      return null;
   }

   private AgentOutcome checkExitConditions() {
      for (ExitCondition var2 : this.exitConditions) {
         Optional var3 = var2.check(this.state);
         if (var3.isPresent()) {
            return (AgentOutcome)var3.get();
         }
      }

      return null;
   }

   private boolean handleOutcome(AgentOutcome var1) {
      String var2 = this.findOutcomeRefusal(var1);
      if (var2 != null && this.state.iterations() >= this.maxIterations) {
         this.applyOutcome(io.karatelabs.ext.agent.runtime.AgentOutcome.failure("refused on the last iteration: " + var2));
         return true;
      } else if (var2 != null) {
         this.stopped.set(false);
         this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user("[refused] " + var2));
         int var8 = this.state.iterations();
         this.dispatchEvent(() -> this.listener.onHint(var8, "refused-exit", var2));
         this.lastToolCode = null;
         this.lastToolResult = null;
         return false;
      } else {
         if (var1.kind() == io.karatelabs.ext.agent.runtime.AgentOutcome.Kind.STUCK && this.outcomeValidator != null && this.stuckRecoveryAttempts < 3) {
            Optional var3;
            try {
               var3 = this.outcomeValidator.a(this.context, var1);
            } catch (Exception var6) {
               LOGGER.warn("StuckHandler threw; ending the loop STUCK", var6);
               var3 = Optional.empty();
            }

            if (var3.isPresent() && !((String)var3.get()).isBlank()) {
               this.stuckRecoveryAttempts++;
               this.stopped.set(false);
               String var9 = (String)var3.get();
               this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user("[stuck-recovery] " + var9));
               int var5 = this.state.iterations();
               this.dispatchEvent(() -> this.listener.onHint(var5, "stuck-recovery", var9));
               this.lastToolCode = null;
               this.lastToolResult = null;
               return false;
            }
         }

         if (this.pendingOutcome == null && !this.endingStatementCompleted && this.state.iterations() < this.maxIterations) {
            String var7 = this.findClosingInstruction(var1);
            if (var7 != null && !var7.isBlank()) {
               this.pendingOutcome = var1;
               this.stopped.set(false);
               this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user("[closing] " + var7));
               int var4 = this.state.iterations();
               this.dispatchEvent(() -> this.listener.onHint(var4, "closing-turn", var7));
               this.lastToolCode = null;
               this.lastToolResult = null;
               return false;
            }
         }

         this.applyOutcome(var1);
         return true;
      }
   }

   private boolean isStopRequested() {
      if (!this.stopped.get()) {
         return false;
      } else {
         if (this.pendingOutcome != null) {
            this.finishPendingOutcome((Object)null);
         }

         return true;
      }
   }

   private String findOutcomeRefusal(AgentOutcome var1) {
      for (AgentExtension var3 : this.extensions) {
         try {
            String var4 = var3.refusal(var1);
            if (var4 != null && !var4.isBlank()) {
               return var4;
            }
         } catch (Exception var5) {
            LOGGER.warn("refusal threw; accepting the outcome", var5);
         }
      }

      return null;
   }

   private AgentOutcome parseProseOutcome(String var1) {
      for (AgentExtension var3 : this.extensions) {
         try {
            AgentOutcome var4 = var3.proseOutcome(var1);
            if (var4 != null) {
               return var4;
            }
         } catch (Exception var5) {
            LOGGER.warn("proseOutcome threw; correcting the turn's format", var5);
         }
      }

      return null;
   }

   private String findClosingInstruction(AgentOutcome var1) {
      for (AgentExtension var3 : this.extensions) {
         try {
            String var4 = var3.closingInstruction(var1);
            if (var4 != null && !var4.isBlank()) {
               return var4;
            }
         } catch (Exception var5) {
            LOGGER.warn("closingInstruction threw; ending without a closing turn", var5);
         }
      }

      return null;
   }

   private void finishPendingOutcome(Object var1) {
      this.endingStatementCompleted = true;
      AgentOutcome var2 = this.pendingOutcome;
      this.pendingOutcome = null;
      String var3 = formatClosingStatement(var1);
      if (var3 != null) {
         this.state.setStatement(var3);
      }

      this.applyOutcome(var2);
   }

   private static String formatClosingStatement(Object var0) {
      if (normalizeValue(var0) instanceof String var2) {
         String var3 = var2.trim();
         if (var3.isEmpty()) {
            return null;
         } else {
            return var3.length() <= 4000 ? var3 : var3.substring(0, 4000) + "… [truncated]";
         }
      } else {
         return null;
      }
   }

   private void applyOutcome(AgentOutcome var1) {
      this.state.setSuccess(var1.isSuccess());
      this.state.setStatus(var1.status());
      if (var1.isSuccess()) {
         this.state.setFinalResult(normalizeValue(var1.value()));
      } else {
         if (var1.value() != null) {
            this.state.setFinalResult(normalizeValue(var1.value()));
         }

         this.state.setLastError(var1.message() != null ? var1.message() : "agent ended " + var1.kind());
      }

      this.stopped.set(true);
   }

   private boolean handleControlSignal(ControlSignal var1) {
      int var2 = this.state.iterations();
      if (this.pendingOutcome != null) {
         this.finishPendingOutcome((Object)null);
         return true;
      } else {
         switch (var1.kind()) {
            case STOP:
               this.applyOutcome(var1.outcome());
               return true;
            case SUSPEND:
               this.state.setStatus(io.karatelabs.ext.agent.runtime.AgentRunState.Status.WAITING);
               this.state.setSuccess(false);
               this.state.setLastError(var1.message() != null ? var1.message() : "suspended for input");
               this.stopped.set(true);
               this.dispatchEvent(() -> this.listener.onHint(var2, "suspend", var1.message() != null ? var1.message() : "suspended for input"));
               return true;
            case HINT:
               this.applyControlHint(var1, "[hint] ", "control-hint", var2);
               return false;
            case REPLAN:
               this.applyControlHint(var1, "[replan] ", "replan", var2);
               return false;
            default:
               return false;
         }
      }
   }

   private void applyControlHint(ControlSignal var1, String var2, String var3, int var4) {
      this.externalOutcome.set(null);
      this.stopped.set(false);
      String var5 = var1.message() != null ? var1.message() : "";
      this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user(var2 + var5));
      this.dispatchEvent(() -> this.listener.onHint(var4, var3, var5));
      this.lastToolCode = null;
      this.lastToolResult = null;
   }

   private void trimHistory() {
      var var1 = this.state.messages();
      int var2 = this.historyWindow * 2;
      if (var1.size() > 1 + var2) {
         int var3 = var1.size() - var2;

         while (var2 > 0 && var3 > 1 && !isAssistantMessage((io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var3))) {
            var3--;
         }

         if (var3 > 1) {
            int var4 = 0;

            for (int var5 = 1; var5 < var3; var5++) {
               if (isAssistantMessage((io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var5))) {
                  var4++;
               }
            }

            StringBuilder var16 = new StringBuilder();
            var16.append("Task: ").append(this.state.task()).append("\nHistory:\n");
            int var6 = 1;

            for (int var7 = 1; var7 < var3; var7++) {
               io.karatelabs.ext.agent.llm.LlmMessage var8 = (io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var7);
               if (isAssistantMessage(var8)) {
                  io.karatelabs.ext.agent.llm.LlmMessage var9 = var7 + 1 < var3 && !isAssistantMessage((io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var7 + 1))
                     ? (io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var7 + 1)
                     : null;
                  String var10 = var8.text();
                  var var11 = this.extractCode(var10);
                  String var12 = var11.isEmpty() ? "(no code)" : ((String)var11.get(0)).trim();
                  if (var12.length() > 800) {
                     var12 = var12.substring(0, 797) + "...";
                  }

                  String var13 = "";
                  if (!var11.isEmpty()) {
                     int var14 = var10.indexOf("```");
                     if (var14 > 0) {
                        var13 = var10.substring(0, var14).trim();
                        if (var13.length() > 300) {
                           var13 = var13.substring(0, 297) + "...";
                        }
                     }
                  }

                  String var35 = this.summarizeToolResult(var9 == null ? null : var9.text());
                  var16.append(var6++).append(". ");
                  if (!var13.isEmpty()) {
                     var16.append(var13).append("\n");
                  }

                  var16.append(var12).append("\n→ ").append(var35).append("\n");
               }
            }

            for (int var17 = 1; var17 < var3; var17++) {
               io.karatelabs.ext.agent.llm.LlmMessage var19 = (io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var17);
               if ("user".equals(var19.role().getValue()) && var19.text() != null && var19.text().startsWith("[User intervention]")) {
                  String var21 = var19.text().substring("[User intervention] ".length()).trim();
                  if (var21.length() > 300) {
                     var21 = var21.substring(0, 297) + "...";
                  }

                  var16.append("⚠ USER INTERVENTION: ").append(var21).append("\n");
               }
            }

            var var18 = this.snapshotValues();
            var var20 = this.snapshotFunctions();
            if (!var18.isEmpty()) {
               var16.append("Your variables (persistent):");

               for (Entry var25 : (Iterable<Entry>) (Iterable<?>) (var18.entrySet())) {
                  var16.append(" ").append((String)var25.getKey()).append(" = ");
                  Object var28 = var25.getValue();
                  if (var28 instanceof String var32) {
                     if (var32.length() <= 80) {
                        var16.append("\"").append(var32).append("\"");
                     } else {
                        var16.append("(string, ").append(var32.length()).append(" chars)");
                     }
                  } else if (var28 instanceof Number || var28 instanceof Boolean) {
                     var16.append(var28);
                  } else if (var28 instanceof Collection var34) {
                     var16.append("(array, ").append(var34.size()).append(" items)");
                  } else if (var28 instanceof Map var36) {
                     var16.append("(object, ").append(var36.size()).append(" keys)");
                  } else {
                     String var15 = String.valueOf(var28);
                     var16.append(var15.length() <= 80 ? var15 : "(" + var28.getClass().getSimpleName() + ", " + var15.length() + " chars)");
                  }

                  var16.append(",");
               }

               var16.setLength(var16.length() - 1);
               var16.append("\n");
            }

            if (!var20.isEmpty()) {
               var16.append("Your functions (persistent — call directly, no need to redeclare):");
               boolean var23 = true;

               for (Entry var29 : (Iterable<Entry>) (Iterable<?>) (var20.entrySet())) {
                  if (!var23) {
                     var16.append(",");
                  }

                  var16.append(" ").append((String)var29.getKey()).append((String)var29.getValue());
                  var23 = false;
               }

               var16.append("\n");
            }

            if (var18.isEmpty() && var20.isEmpty() && var4 > 2) {
               var16.append(
                  "(no variables or functions saved — use `var name = value` for data, `function name(arg) {...}` for reusable extractors. Both persist across iterations.)\n"
               );
            }

            io.karatelabs.ext.agent.llm.LlmMessage var24 = io.karatelabs.ext.agent.llm.LlmMessage.user(var16.toString());
            ArrayList<io.karatelabs.ext.agent.llm.LlmMessage> var27 = new ArrayList();
            var27.add(var24);

            for (int var30 = var3; var30 < var1.size(); var30++) {
               var27.add((io.karatelabs.ext.agent.llm.LlmMessage)var1.get(var30));
            }

            int var31 = var1.size();
            var1.clear();
            var1.addAll(var27);
            String var33 = var16.toString();
            LOGGER.info(
               "[trim] compressed {} turns, {} → {} messages, summary {} chars, {} user vars",
               new Object[]{var4, var31, var1.size(), var33.length(), var18.size()}
            );
            this.dispatchEvent(() -> this.listener.onContextTrim(var33));
         }
      }
   }

   private static boolean isAssistantMessage(io.karatelabs.ext.agent.llm.LlmMessage var0) {
      return var0.role() == io.karatelabs.ext.agent.llm.LlmMessage.Role.ASSISTANT;
   }

   private Map<String, Object> snapshotValues() {
      var var1 = this.toolExecutor.a();
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         if (!this.initialBindingNames.contains(var4.getKey())) {
            Object var5 = var4.getValue();
            if (!(var5 instanceof JsFunction)
               && !(var5 instanceof JavaInvokable)
               && (var5 instanceof String || var5 instanceof Number || var5 instanceof Boolean || var5 instanceof Collection || var5 instanceof Map)) {
               var2.put((String)var4.getKey(), var5);
            }
         }
      }

      return var2;
   }

   private Map<String, String> snapshotFunctions() {
      var var1 = this.toolExecutor.a();
      LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();

      for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         if (!this.initialBindingNames.contains(var4.getKey()) && var4.getValue() instanceof JsFunction var6) {
            int var8 = var6.getMember("length") instanceof Number var9 ? var9.intValue() : 0;
            var2.put((String)var4.getKey(), "(" + var8 + " arg" + (var8 == 1 ? "" : "s") + ")");
         }
      }

      return var2;
   }

   private String summarizeToolResult(String var1) {
      if (var1 != null) {
         String var2 = var1.stripLeading();
         boolean var3 = var2.startsWith("Error:") || var2.startsWith("error:") || var2.startsWith("FAILED:");
         if (!var3) {
            for (AgentHook var5 : this.hooks) {
               Optional var6 = var5.a(var1);
               if (var6.isPresent()) {
                  return (String)var6.get();
               }
            }
         }
      }

      return truncateToolResult(var1);
   }

   static String truncateToolResult(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0;
         int var2 = var0.lastIndexOf("\n[");
         if (var2 > 0) {
            var1 = var0.substring(0, var2);
         }

         int var3 = var1.indexOf("\n⚠");
         if (var3 > 0) {
            var1 = var1.substring(0, var3);
         }

         var1 = var1.trim();
         if (!var1.startsWith("Error:") && !var1.startsWith("error:") && !var1.startsWith("FAILED:")) {
            return var1.length() > 200 ? var1.substring(0, 197) + "..." : var1;
         } else {
            return var1.length() > 1500 ? var1.substring(0, 1497) + "..." : var1;
         }
      } else {
         return "null";
      }
   }

   private static String firstCodeLine(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for (String var4 : var0.split("\n")) {
            String var5 = var4.trim();
            if (!var5.isEmpty() && !var5.startsWith("//")) {
               return var5.length() > 60 ? var5.substring(0, 57) + "..." : var5;
            }
         }

         return null;
      }
   }

   private static Object normalizeValue(Object var0) {
      if (var0 instanceof Map var5) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

         for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            var6.put(String.valueOf(var8.getKey()), normalizeValue(var8.getValue()));
         }

         return var6;
      } else if (!(var0 instanceof List var1)) {
         return var0;
      } else {
         ArrayList<java.lang.Object> var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            var2.add(normalizeValue(var4));
         }

         return var2;
      }
   }

   private void handleRunException(Exception var1) {
      String var2 = var1.getMessage();
      this.state.setStatus(io.karatelabs.ext.agent.runtime.AgentRunState.Status.FAILED);
      this.state.setSuccess(false);
      this.state.setLastError(var2);
      io.karatelabs.ext.agent.llm.LlmException var3 = var1 instanceof io.karatelabs.ext.agent.llm.LlmException var4
         ? var4
         : new io.karatelabs.ext.agent.llm.LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.UNKNOWN, var2, 0, var1);
      if (var3.b() > 0) {
         this.state.a(var3.f(), var3.g());
      }

      this.dispatchEvent(() -> this.listener.onError(var3));
   }

   private ToolResult executeTool(String var1) throws io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException {
      if (this.toolTimeoutMillis <= 0L) {
         return this.toolExecutor.a(var1);
      } else {
         Future var2 = this.toolExecutorService().submit(() -> this.toolExecutor.a(var1));

         try {
            return (ToolResult)var2.get(this.toolTimeoutMillis, TimeUnit.MILLISECONDS);
         } catch (TimeoutException var7) {
            var2.cancel(true);
            if (this.isToolExecutorResponsive()) {
               throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException(
                  "Tool execution exceeded the " + this.toolTimeoutMillis + "ms timeout and was aborted. Break the work into smaller steps, or finish the task.", null, ""
               );
            } else {
               this.toolUninterruptible = true;
               throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException("Tool execution timed out (" + this.toolTimeoutMillis + "ms) and could not be interrupted; ending the run.", null, "");
            }
         } catch (ExecutionException var8) {
            Throwable var4 = var8.getCause();
            ControlSignal var5 = io.karatelabs.ext.agent.runtime.ControlSignal.a(var4);
            if (var5 != null) {
               throw var5;
            } else if (var4 instanceof io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException var6) {
               throw var6;
            } else {
               throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException(var4 != null ? var4.getMessage() : var8.getMessage(), var4, "");
            }
         } catch (InterruptedException var9) {
            var2.cancel(true);
            Thread.currentThread().interrupt();
            throw new io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException("tool execution interrupted", var9, "");
         }
      }
   }

   private boolean isToolExecutorResponsive() {
      long var1 = Math.max(250L, this.toolTimeoutMillis);

      try {
         this.toolExecutorService().submit(() -> Boolean.TRUE).get(var1, TimeUnit.MILLISECONDS);
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   private ExecutorService toolExecutorService() {
      ExecutorService var1 = this.toolExecutorService;
      if (var1 == null) {
         synchronized (this) {
            var1 = this.toolExecutorService;
            if (var1 == null) {
               var1 = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("harness-tool").factory());
               this.toolExecutorService = var1;
            }
         }
      }

      return var1;
   }

   private String formatToolError(io.karatelabs.ext.agent.runtime.ToolExecutor.ToolExecutionException var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append("Error: ").append(var1.getMessage());
      String var3 = this.findMissingMethodHint(var1.getMessage());
      if (var3 != null) {
         var2.append("\n⚠ ").append(var3);
         this.dispatchEvent(() -> this.listener.onHint(this.state.iterations(), "wrong-member", var3));
      } else {
         String var4 = this.findUnboundBuiltinHint(var1.getMessage());
         if (var4 != null) {
            var2.append("\n⚠ ").append(var4);
            this.dispatchEvent(() -> this.listener.onHint(this.state.iterations(), "unbound-global", var4));
         }
      }

      if (var1.a() != null && !var1.a().isEmpty()) {
         var2.append("\n\nConsole output:\n").append(var1.a());
      }

      return var2.toString();
   }

   private String findMissingMethodHint(String var1) {
      if (var1 == null) {
         return null;
      } else {
         Matcher var2 = MISSING_METHOD_PATTERN.matcher(var1);
         if (!var2.find()) {
            return null;
         } else {
            String var3 = var2.group(1);
            String var4 = var2.group(2);
            Object var5 = this.resolveBindingPath(var3);
            return var5 != null && getMember(var5, var4) == null ? this.suggestMember(var3, var5, var4) : null;
         }
      }
   }

   private String findMissingMemberInCode(String var1) {
      ArrayList<io.karatelabs.parser.Token> var2 = new ArrayList();

      try {
         for (Token var4 : JsLexer.getTokens(Resource.text(var1))) {
            if (var4.type.primary) {
               var2.add(var4);
            }
         }
      } catch (RuntimeException var11) {
         return null;
      }

      for (int var12 = 0; var12 < var2.size(); var12++) {
         Token var13 = (Token)var2.get(var12);
         if (var13.type == TokenType.IDENT && Character.isUpperCase(var13.getText().charAt(0)) && (var12 <= 0 || !isMemberAccess((Token)var2.get(var12 - 1)))) {
            String var5 = var13.getText();
            Object var6 = this.bindingsSnapshot.get(var5);

            for (int var7 = var12 + 1;
               var7 + 1 < var2.size() && isMemberAccess((Token)var2.get(var7)) && ((Token)var2.get(var7 + 1)).type == TokenType.IDENT && var6 instanceof SimpleObject;
               var7 += 2
            ) {
               String var8 = ((Token)var2.get(var7 + 1)).getText();
               Object var9 = getMember(var6, var8);
               if (var9 == null) {
                  String var10 = this.suggestMember(var5, var6, var8);
                  if (var10 != null) {
                     return var10;
                  }
                  break;
               }

               var6 = var9;
               var5 = var5 + "." + var8;
            }
         }
      }

      return null;
   }

   private static boolean isMemberAccess(Token var0) {
      return var0.type == TokenType.DOT || var0.type == TokenType.QUES_DOT;
   }

   private String suggestMember(String var1, Object var2, String var3) {
      for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (this.bindingsSnapshot.containsKey(var1) ? this.bindingsSnapshot.entrySet() : Set.of())) {
         String var6 = (String)var5.getKey();
         if (!var6.equals(var1) && getMember(var5.getValue(), var3) instanceof JavaCallable) {
            var var7 = memberNames(var2);
            return "`"
               + var1
               + "."
               + var3
               + "` does not exist — did you mean `"
               + var6
               + "."
               + var3
               + "(…)`?"
               + (var7.isEmpty() ? "" : " (`" + var1 + "` has only: " + String.join(", ", var7) + ")");
         }
      }

      ArrayList<java.lang.String> var8 = new ArrayList();

      for (String var11 : memberNames(var2)) {
         Object var13 = getMember(var2, var11);
         if (var13 != null && !(var13 instanceof JavaCallable) && getMember(var13, var3) instanceof JavaCallable) {
            var8.add(var1 + "." + var11 + "." + var3);
         }
      }

      if (!var8.isEmpty()) {
         return "`" + var1 + "." + var3 + "` does not exist — it lives one level down: `" + String.join("(…)` / `", var8) + "(…)`.";
      } else {
         var var10 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var3, memberNames(var2));
         if (!var10.isEmpty()) {
            return "`" + var1 + "` has no `" + var3 + "` — did you mean `" + var1 + "." + String.join("(…)` / `" + var1 + ".", var10) + "(…)`?";
         } else {
            var var12 = memberNames(var2);
            return !var12.isEmpty() ? "`" + var1 + "` has no `" + var3 + "` — it has: " + String.join(", ", var12) + "." : null;
         }
      }
   }

   private Object resolveBindingPath(String var1) {
      String[] var2 = var1.split("\\.");
      Object var3 = this.bindingsSnapshot.get(var2[0]);

      for (int var4 = 1; var4 < var2.length && var3 != null; var4++) {
         var3 = getMember(var3, var2[var4]);
      }

      return var3;
   }

   private String findUnboundBuiltinHint(String var1) {
      String var2 = JsApi.notDefinedIdentifier(var1);
      String var10000;
      if (var2 == null) {
         var10000 = null;
      } else {
         switch (var2) {
            case "read":
               var10000 = "File.read(path)";
               break;
            case "karate":
               var10000 = "File.read(path)` / `File.write(path, text)` / `File.call(path, arg?)";
               break;
            case "call":
            case "callSingle":
            case "callonce":
               var10000 = "File.call(path, arg?)";
               break;
            default:
               var10000 = null;
         }
      }

      String var3 = var10000;
      if (var3 == null) {
         return null;
      } else {
         return !this.bindingsSnapshot.containsKey("File")
            ? "`" + var2 + "` is a feature-runtime builtin and is not bound here — this mode binds: " + String.join(", ", this.bindingsSnapshot.keySet()) + "."
            : "`" + var2 + "` is a feature-runtime builtin and is not bound here — use `" + var3 + "`.";
      }
   }

   private static Object getMember(Object var0, String var1) {
      try {
         if (var0 instanceof Map var5) {
            Object var6 = var5.get(var1);
            return var6 == Terms.UNDEFINED ? null : var6;
         }

         if (var0 instanceof ObjectLike var2) {
            Object var3 = var2.getMember(var1);
            return var3 == Terms.UNDEFINED ? null : var3;
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private static Collection<String> memberNames(Object var0) {
      try {
         if (var0 instanceof Map var4) {
            ArrayList<java.lang.String> var5 = new ArrayList();
            var4.keySet().forEach(var1x -> var5.add(String.valueOf(var1x)));
            return var5;
         }

         if (var0 instanceof SimpleObject var1) {
            var var2 = var1.jsKeys();
            return (Collection<String>)(var2 != null ? var2 : List.of());
         }
      } catch (Exception var3) {
      }

      return List.of();
   }

   private String renderArtifacts(List<io.karatelabs.ext.agent.runtime.AgentArtifact> var1, List<io.karatelabs.ext.agent.runtime.AgentArtifact> var2, int var3) {
      StringBuilder var4 = new StringBuilder();

      for (io.karatelabs.ext.agent.runtime.AgentArtifact var6 : var1) {
         this.dispatchEvent(() -> this.listener.onArtifact(var3, var6));
         String var7 = var6.d();
         switch (var7) {
            case "image":
               var2.add(var6);
               break;
            case "text":
            case "log":
               this.appendTextArtifact(var4, var6, var3);
               break;
            case "file":
               this.appendFileArtifact(var4, var6);
               break;
            default:
               if (var6.a() != null) {
                  this.appendTextArtifact(var4, var6, var3);
               } else if (var6.c() != null) {
                  this.appendFileArtifact(var4, var6);
               }
         }
      }

      return var4.toString();
   }

   private void appendTextArtifact(StringBuilder var1, io.karatelabs.ext.agent.runtime.AgentArtifact var2, int var3) {
      var1.append("\n\n");
      if (var2.f() != null && !var2.f().isEmpty()) {
         var1.append(var2.f()).append(":\n");
      }

      var1.append(var2.a());
      if ("log".equals(var2.d())) {
         String var4 = var2.a();
         this.dispatchEvent(() -> this.listener.onConsole(var3, var4));
      }
   }

   private void appendFileArtifact(StringBuilder var1, io.karatelabs.ext.agent.runtime.AgentArtifact var2) {
      var1.append("\n[artifact: ");
      var1.append(var2.f() != null && !var2.f().isEmpty() ? var2.f() : "file");
      if (var2.c() != null) {
         var1.append(" → ").append(var2.c());
      }

      var1.append("]");
   }

   List<String> extractCode(String var1) {
      return io.karatelabs.ext.agent.runtime.CodeExtractor.a(var1, io.karatelabs.ext.agent.runtime.CodeExtractor.ExtractionMode.LENIENT, this.terminalCallNames);
   }

   public void stop() {
      this.stopped.set(true);
      if (this.paused.compareAndSet(true, false)) {
         CountDownLatch var1 = this.resumeLatch;
         this.resumeLatch = null;
         if (var1 != null) {
            var1.countDown();
         }
      }

      this.llmClient.c();
   }

   public void pause() {
      if (this.paused.compareAndSet(false, true)) {
         this.resumeLatch = new CountDownLatch(1);
         this.pauseLatch = new CountDownLatch(1);
         LOGGER.info("[pause] pause requested, loop will block at next checkpoint");
      } else {
         LOGGER.info("[pause] already paused, ignoring duplicate pause request");
      }
   }

   public boolean awaitPaused(long var1) {
      CountDownLatch var3 = this.pauseLatch;
      if (var3 == null) {
         return this.paused.get();
      } else {
         try {
            return var3.await(var1, TimeUnit.MILLISECONDS);
         } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
            return false;
         }
      }
   }

   public <T extends AgentExtension> T extension(Class<T> var1) {
      for (AgentExtension var3 : this.extensions) {
         if (var1.isInstance(var3)) {
            return (T)var1.cast(var3);
         }
      }

      return null;
   }

   public void resume(String var1) {
      this.resumeMessage = var1;
      if (this.paused.compareAndSet(true, false)) {
         CountDownLatch var2 = this.resumeLatch;
         this.resumeLatch = null;
         if (var2 != null) {
            var2.countDown();
         }

         LOGGER.info("[pause] resume requested");
      } else {
         LOGGER.info("[pause] resume called but not paused, message stored");
      }
   }

   public boolean isPaused() {
      return this.paused.get();
   }

   private boolean awaitResumeOrIntervention() {
      if (!this.paused.get()) {
         String var5 = this.resumeMessage;
         if (var5 != null) {
            this.resumeMessage = null;
            if (!var5.isBlank()) {
               this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user("[User intervention] " + var5));
               return true;
            }
         }

         return false;
      } else {
         this.dispatchEvent(this.listener::onPaused);
         CountDownLatch var1 = this.pauseLatch;
         if (var1 != null) {
            var1.countDown();
         }

         LOGGER.info("[pause] loop blocked at checkpoint (iteration {}), waiting for resume...", this.state.iterations());
         CountDownLatch var2 = this.resumeLatch;
         if (var2 != null) {
            try {
               var2.await();
            } catch (InterruptedException var4) {
               Thread.currentThread().interrupt();
               this.stopped.set(true);
               return false;
            }
         }

         String var3 = this.resumeMessage;
         this.resumeMessage = null;
         this.dispatchEvent(() -> this.listener.onResumed(var3));
         if (var3 != null && !var3.isBlank()) {
            this.state.addMessage(io.karatelabs.ext.agent.llm.LlmMessage.user("[User intervention] " + var3));
            return true;
         } else {
            return false;
         }
      }
   }

   public AgentRunState state() {
      return this.state;
   }

   public boolean isRunning() {
      return this.running.get();
   }

   private void dispatchEvent(Runnable var1) {
      if (this.listener != null) {
         this.callbackExecutor.execute(var1);
      }
   }

   private void closeExtension(AgentExtension var1) {
      try {
         var1.close();
      } catch (Exception var3) {
         LOGGER.warn("Error closing toolkit {}", var1.getClass().getSimpleName(), var3);
      }
   }

   private void closeResources() {
      for (AgentExtension var2 : this.extensions) {
         this.closeExtension(var2);
      }

      ExecutorService var3 = this.toolExecutorService;
      if (var3 != null) {
         var3.shutdownNow();
      }
   }

   public static io.karatelabs.ext.agent.runtime.AgentLoop.Builder builder() {
      return new io.karatelabs.ext.agent.runtime.AgentLoop.Builder();
   }

   public static class Builder {
      private io.karatelabs.ext.agent.llm.LlmClient llmClient;
      private ToolExecutor toolExecutor;
      private AgentRunListener listener = new AgentRunListener() {};
      private Executor callbackExecutor;
      private String systemPrompt;
      private int maxIterations = 50;
      private int historyWindow = 1;
      private Map<String, Object> bindings;
      private final List<AgentExtension> extensions = new ArrayList<>();
      private final List<ExitCondition> exitConditions = new ArrayList<>();
      private RetryPolicy retryPolicy = io.karatelabs.ext.agent.runtime.RetryPolicy.defaults();
      private OutcomeValidator outcomeValidator;
      private long toolTimeoutMillis = -1L;
      private String taskSuffix;
      private List<ConversationEntry> conversationHistory;

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder llmClient(io.karatelabs.ext.agent.llm.LlmClient var1) {
         this.llmClient = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder addExitCondition(ExitCondition var1) {
         this.exitConditions.add(var1);
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder retryPolicy(RetryPolicy var1) {
         this.retryPolicy = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder outcomeValidator(OutcomeValidator var1) {
         this.outcomeValidator = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder toolTimeoutMillis(long var1) {
         this.toolTimeoutMillis = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder toolExecutor(ToolExecutor var1) {
         this.toolExecutor = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder listener(AgentRunListener var1) {
         this.listener = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder callbackExecutor(Executor var1) {
         this.callbackExecutor = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder systemPrompt(String var1) {
         this.systemPrompt = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder maxIterations(int var1) {
         this.maxIterations = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder historyWindow(int var1) {
         this.historyWindow = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder addExtension(AgentExtension var1) {
         this.extensions.add(var1);
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder binding(String var1, Object var2) {
         if (this.bindings == null) {
            this.bindings = new LinkedHashMap<>();
         }

         this.bindings.put(var1, var2);
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder taskSuffix(String var1) {
         this.taskSuffix = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.AgentLoop.Builder conversationHistory(List<ConversationEntry> var1) {
         this.conversationHistory = var1;
         return this;
      }

      public AgentLoop build() {
         if (this.llmClient == null) {
            throw new IllegalArgumentException("LlmClient is required");
         } else {
            return new AgentLoop(this);
         }
      }
   }

   private final class LoopContext implements AgentContext {
      @Override
      public AgentRunState state() {
         return AgentLoop.this.state;
      }

      @Override
      public int maxIterations() {
         return AgentLoop.this.maxIterations;
      }

      @Override
      public ToolExecutor toolExecutor() {
         return AgentLoop.this.toolExecutor;
      }

      @Override
      public void submitOutcome(AgentOutcome var1) {
         AgentLoop.this.externalOutcome.set(var1);
         AgentLoop.this.stopped.set(true);
      }

      @Override
      public io.karatelabs.ext.agent.llm.LlmClient llmClient() {
         return AgentLoop.this.llmClient;
      }

      @Override
      public boolean lastResponseTruncated() {
         return AgentLoop.this.lastResponseTruncated;
      }

      @Override
      public void hint(String var1, String var2) {
         AgentLoop.this.dispatchEvent(() -> AgentLoop.this.listener.onHint(AgentLoop.this.state.iterations(), var1, var2));
      }

      @Override
      public void step(String var1, String var2, String var3) {
         AgentLoop.this.dispatchEvent(() -> AgentLoop.this.listener.onStep(var1, var2, var3));
      }

      @Override
      public void assertion(Map<String, Object> var1) {
         AgentLoop.this.dispatchEvent(() -> AgentLoop.this.listener.onAssertion(var1));
      }

      @Override
      public void screenshot(String var1, String var2) {
         AgentLoop.this.dispatchEvent(() -> AgentLoop.this.listener.onScreenshot(AgentLoop.this.state.iterations(), var1, var2));
      }

      @Override
      public void healed(String var1, String var2, String var3, Double var4) {
         AgentLoop.this.dispatchEvent(() -> AgentLoop.this.listener.onHealed(AgentLoop.this.state.iterations(), var1, var2, var3, var4));
      }
   }
}
