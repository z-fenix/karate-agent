package io.karatelabs.ext.pair.jsapi;

import io.karatelabs.core.ReportAssets;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.MockStarter;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.common.run.RunnerEnvironmentPolicy;
import io.karatelabs.ext.mock.MockInstance;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import io.karatelabs.ext.pair.EndpointAliasSet;
import io.karatelabs.ext.pair.DataProfile;
import io.karatelabs.ext.pair.NondeterminismDetector;
import io.karatelabs.ext.pair.PairEvidenceRepository;
import io.karatelabs.ext.pair.PairException;
import io.karatelabs.ext.pair.ContractReportArtifacts;
import io.karatelabs.ext.pair.PairMarkdownReportRenderer;
import io.karatelabs.ext.pair.PairRunner;
import io.karatelabs.ext.pair.PairReportAssembler;
import io.karatelabs.ext.pair.ContractSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Contract extends JsDispatch {
   private static final Set<String> MOCK_OPTS = Set.of(
      "openapi",
      "feature",
      "js",
      "file",
      "rules",
      "handlers",
      "resources",
      "crud",
      "validate",
      "port",
      "arg",
      "pathPrefix",
      "javaBridge",
      "requestExpressions",
      "ssl"
   );
   private Path root;
   private Path classpathRoot;
   private Path workDir;
   private Path reportDir;
   private RunnerEnvironmentPolicy console = io.karatelabs.ext.common.run.RunnerEnvironmentPolicy.a;
   private MockStarter mocks = (var0, var1, var2, var3, var4) -> JsApi.missing(
      "no mock starter is bound here, so only a mock already standing can be paired: pass its URL (mock:'http://…')"
   );
   private Map<String, ReportAssets> nav;

   public Contract() {
      this.fn("pair", var1 -> this.pair(JsApi.arg(var1, 0)));
      this.fn("read", var1 -> this.read(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("report", var1 -> this.report(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public Contract withNav(Map<String, ReportAssets> var1) {
      this.nav = var1;
      return this;
   }

   public Contract withRoot(Path var1) {
      return this.withRoot(var1, var1);
   }

   public Contract withRoot(Path var1, Path var2) {
      this.root = var1;
      this.classpathRoot = var2;
      return this;
   }

   public Contract withConsole(RunnerEnvironmentPolicy var1) {
      this.console = var1;
      return this;
   }

   public Contract withMocks(MockStarter var1) {
      this.mocks = var1;
      return this;
   }

   public Contract withWorkDir(Path var1) {
      this.workDir = var1;
      return this;
   }

   public Contract withReportDir(Path var1) {
      this.reportDir = var1;
      return this;
   }

   private Map<String, Object> graphStanding(Instant var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("contributes", false);
      Path var3 = this.reportDir == null ? null : this.reportDir.resolve("ext/coverage/coverage-data.json");
      String var4;
      if (var3 != null && Files.isRegularFile(var3)) {
         try {
            var4 = Files.getLastModifiedTime(var3).toInstant().isBefore(var1) ? "olderThanThisPair" : "current";
         } catch (IOException var8) {
            var4 = "unreadable";
         }
      } else {
         var4 = "none";
      }

      var2.put("projectGraph", var4);

      String var5 = switch (var4) {
         case "none" -> "there is no project coverage graph"
            + (var3 == null ? "" : " at " + var3)
            + " yet, so nothing outside this pair knows these operations were called. ";
         case "olderThanThisPair" -> "the project coverage graph predates this pair, so it does not reflect this run. ";
         default -> "";
      };
      var2.put(
         "note",
         var5
            + "A pair does not feed the project coverage graph, on purpose: it runs the suite TWICE and holds `@destructive` scenarios out of both legs, so counting its traffic would double-count evidence and disagree with the suite's own run. The claim's denominator above is read from the pair's OWN graph. For Coverage.status / the RTM / `Openapi.grade`'s coverage and proven-ness dimensions, run the suite the ordinary way — Runner.run('<suite>'), whose settle folds it into the project graph."
      );
      return var2;
   }

   private Object pair(Object var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("contract.pair", "openapi");
      if (var1 instanceof Map var2) {
         Map var3 = var2;
         var var4 = paths(var2.get("suite"));
         if (var4.isEmpty()) {
            return JsApi.missing(
               "Contract.pair needs `suite` — the functional suite BOTH legs answer to (a feature, a folder of them, or a list). The suite is the shared artifact; there is no pair without it."
            );
         } else {
            String var5 = str(var2.get("spec"));
            String var6 = str(var2.get("provider"));
            if (var6 == null) {
               return JsApi.missing(
                  "Contract.pair needs `provider` — the real provider's base URL. Without a provider leg there is NO PAIR: a mock-only run is a regression check on the mock, which is a different finding with a different owner (run it with Runner.suite)."
               );
            } else {
               Path var7 = this.root != null ? this.root : Path.of(".");
               ContractSchema var8;
               if (var5 == null) {
                  var8 = io.karatelabs.ext.pair.ContractSchema.EMPTY;
               } else {
                  try {
                     var8 = io.karatelabs.ext.pair.ContractSchema.fromCatalog(io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromResource(MaxPaths.resolveResource(this.root, this.classpathRoot, var5)));
                  } catch (RuntimeException var35) {
                     return JsApi.missing("Contract.pair could not parse spec '" + var5 + "': " + var35.getMessage());
                  }
               }

               DataProfile var9;
               NondeterminismDetector var10;
               Map var11;
               EndpointAliasSet var12;
               try {
                  var9 = io.karatelabs.ext.pair.DataProfile.of(var3.get("profile"));
                  var10 = io.karatelabs.ext.pair.NondeterminismDetector.fromConfig(var3.get("ignore"));
                  var11 = bars(var3.get("bars"));
                  var12 = io.karatelabs.ext.pair.EndpointAliasSet.parse(var3.get("aliases"));
               } catch (PairException var34) {
                  return JsApi.missing("Contract.pair: " + var34.getMessage());
               }

               if (!var10.rejectedRules().isEmpty()) {
                  ArrayList var38 = new ArrayList();

                  for (Map var41 : var10.rejectedRules()) {
                     var38.add(String.valueOf(var41.get("refused")));
                  }

                  return JsApi.missing(
                     "Contract.pair: "
                        + var10.rejectedRules().size()
                        + " of your `ignore` rules could not be read, so NOTHING was run — a pair minted while a declared waiver silently did nothing is a divergence set you would read wrong and an artifact you would commit. "
                        + String.join(" | ", var38)
                  );
               } else if (var9 == io.karatelabs.ext.pair.DataProfile.FIXTURE && str(var2.get("fixture")) == null) {
                  return JsApi.missing(
                     "Contract.pair: the `fixture` data profile needs `fixture` — the path to the declared fixture BOTH legs were pre-loaded from. Its digest becomes a freshness binding, so a pair cannot claim that profile without naming what it loaded. (Loading the provider side is your step, not the harness's — it cannot reset somebody else's provider.)"
                  );
               } else if (var9 != io.karatelabs.ext.pair.DataProfile.FIXTURE && str(var2.get("fixture")) != null) {
                  return JsApi.missing(
                     "Contract.pair: `fixture` only means something under the `fixture` data profile — this pair declared `"
                        + var9.wire()
                        + "`. Either drop it, or declare {profile:'fixture'} and say that both legs were pre-loaded from it."
                  );
               } else {
                  MockInstance var13 = null;

                  try {
                     Object var14 = var3.get("mock");
                     String var40 = str(var14);
                     if (var40 == null || !var40.toLowerCase().startsWith("http")) {
                        if (var14 instanceof Map var18) {
                           ArrayList var19 = new ArrayList();

                           for (Object var21 : var18.keySet()) {
                              if (!MOCK_OPTS.contains(String.valueOf(var21))) {
                                 var19.add(String.valueOf(var21));
                              }
                           }

                           if (!var19.isEmpty()) {
                              return JsApi.missing(
                                 "Contract.pair: `mock` names "
                                    + var19
                                    + ", which Http.mock does not understand — and an option it does not understand is one it would silently ignore, standing up a mock other than the one you asked for while the evidence recorded your value. Known keys: "
                                    + MOCK_OPTS
                                    + ". For the mock your project ships, pass the feature path itself (mock:'mock/api.feature') or {feature:'mock/api.feature'}."
                              );
                           }
                        }

                        Object var16;
                        Object var17;
                        if (var14 == null) {
                           if (var5 == null) {
                              return JsApi.missing(
                                 "Contract.pair without `spec` needs `mock` — there is no contract document to stand a default mock up from. Pass the URL of a mock already standing (http://…), or the path to the mock feature your project ships (mock:'mock/api.feature')."
                              );
                           }

                           var16 = var5;
                           var17 = null;
                        } else if (var14 instanceof Map var44
                           && (var44.containsKey("openapi") || var44.containsKey("feature") || var44.containsKey("js") || var44.containsKey("file"))) {
                           var16 = var14;
                           var17 = var14;
                        } else if (var14 instanceof Map) {
                           if (var5 == null) {
                              return JsApi.missing(
                                 "Contract.pair: `mock` is an overlay ({handlers:…} / {rules:…}), but this pair has no `spec` to overlay it on. Name the mock outright — a feature path, a URL, or {feature:…} — or pass `spec`."
                              );
                           }

                           var16 = var5;
                           var17 = var14;
                        } else {
                           if (!MaxPaths.isFeature(var40)) {
                              return JsApi.missing(
                                 "Contract.pair: `mock` = '"
                                    + var40
                                    + "' is not something this harness can stand up, so it is REFUSED rather than ignored — a pair whose mock leg silently ran something other than what you named would be evidence about the wrong system. It takes the URL of a mock already standing (http://…), the path to a karate mock feature ('mock/api.feature'), or Http.mock options ({handlers:{…}} / {rules:{…}} / {js:'…'}) overlaying the spec."
                                    + (var5 == null ? "" : " Omit it entirely and the pair stands one up from `spec`.")
                              );
                           }

                           var16 = var40;
                           var17 = null;
                        }

                        Object var45 = this.mocks.mock(this.root, this.classpathRoot, null, var16, var17);
                        if (!(var45 instanceof MockInstance var49)) {
                           if (var45 instanceof Map var53 && var53.get("error") != null) {
                              var53.put(
                                 "error",
                                 "Contract.pair: the mock leg could not stand up — "
                                    + var53.get("error")
                                    + " — a leading '/' anchors the project root; the mock your project ships is usually beside the spec (mock/*.feature)."
                                    + (
                                       var5 == null
                                          ? ""
                                          : " Omit `mock` entirely and the pair stands one up from `spec` — a different subject (a synthesized mock, not the one you ship), and the evidence records which."
                                    )
                              );
                           }

                           return var45;
                        }

                        var13 = var49;
                        var40 = var49.getUrl();
                     }

                     String var42 = this.console.env(str(var3.get("env")));
                     io.karatelabs.ext.pair.PairRunner.PairRunOptions var43 = new io.karatelabs.ext.pair.PairRunner.PairRunOptions(
                        var7,
                        var4,
                        var5,
                        var8,
                        var6,
                        var40,
                        var3.get("mock"),
                        str(var3.get("fixture")),
                        var9,
                        var10,
                        tags(var3.get("tags")),
                        var42,
                        props(var3.get("props")),
                        str(var3.get("targetProp")) != null ? str(var3.get("targetProp")) : "contract.target",
                        !Boolean.FALSE.equals(var3.get("confirm")),
                        intOrNull(var3.get("maxAgeDays")),
                        str(var3.get("providerEnv")),
                        str(var3.get("providerBuild")),
                        null,
                        this.workDir != null ? this.workDir : var7.resolve("target/pairs"),
                        var11,
                        var12,
                        this.console.kickoff(var42)
                     );
                     var var46 = io.karatelabs.ext.pair.PairRunner.run(var43);
                     boolean var50 = Boolean.TRUE.equals(var3.get("draft"));
                     if (var50) {
                        var46.put("draft", true);
                     }

                     Instant var52 = Instant.parse(String.valueOf(var46.get("mintedAt")));
                     String var56 = String.valueOf(var46.get("pairId"));
                     Path var22 = str(var3.get("out")) != null
                        ? resolve(var7, str(var3.get("out")))
                        : (var50 ? var7.resolve("target/pairs").resolve(io.karatelabs.ext.pair.PairEvidenceRepository.fileName(var52, var56)) : io.karatelabs.ext.pair.PairEvidenceRepository.evidencePath(var7, var52, var56));
                     if (var13 != null && !var13.getWarnings().isEmpty()) {
                        LinkedHashMap var23 = new LinkedHashMap();
                        var23.put("mock", var13.getWarnings());
                        var23.put(
                           "note",
                           "the mock leg loaded with warnings — read them before reading anything into the divergence set below. An overlay key matching no operation was NOT applied, so any difference it was meant to remove is still here, and it is a difference from your declaration rather than from your mock."
                        );
                        var46.put("warnings", var23);
                     }

                     io.karatelabs.ext.pair.PairEvidenceRepository.writeEvidence(var22, var46);
                     var var57 = new LinkedHashMap<>(var46);
                     var57.put("evidenceFile", io.karatelabs.ext.pair.ContractReportArtifacts.relativePath(var7, var22));
                     var57.put("coverageGraph", this.graphStanding(var52));
                     if (var50) {
                        var57.put(
                           "draft",
                           "a DRAFT — written under target/ (build output), so it is not project evidence and nothing should commit it. Contract.read() with no argument looks only in contract/pairs and will not find it. Drop `draft` when this is the run you mean to keep."
                        );
                     } else if (!(var46.get("claim") instanceof Map var24 && var24.get("rungs") != null)) {
                        boolean var59 = var22.toAbsolutePath().startsWith(var7.resolve("contract/pairs").toAbsolutePath());
                        var57.put(
                           "probeNote",
                           "this pair measured NOTHING and holds no rung."
                              + (
                                 var59
                                    ? " It was still written into contract/pairs as committable project evidence, where a bare Contract.read() will select it."
                                    : ""
                              )
                              + " If this was a wiring probe (dead provider, mis-pointed target), delete "
                              + io.karatelabs.ext.pair.ContractReportArtifacts.relativePath(var7, var22)
                              + " and re-mint with {draft:true}, which lands under target/ instead. If the environment is genuinely down, keep it: measured-nothing is the honest record of that."
                        );
                     }

                     String var58 = var7.relativize(var22.toAbsolutePath()).toString();
                     Object var60 = full(var3) ? var57 : summary(var57);
                     boolean var26 = !(var46.get("claim") instanceof Map var27 && var27.get("rungs") != null);
                     String var61 = var50
                        ? "this DRAFT is readable back at the path above and is otherwise inert — re-run without `draft` to mint the pair your project keeps."
                        : (
                           var26
                              ? "this pair measured NOTHING (see `probeNote`) — do NOT re-grade off it, and do not commit it unless the environment being down is the fact you mean to record. If this was a wiring probe, delete the file above and re-mint with {draft:true} while you explore."
                              : "the pair is written and COMMITTABLE — `git blame` is its audit record, and it is what an auditor reads later without re-running. `Openapi.grade` reads it from here: the `contract` dimension scores the rung normalised (rung ÷ 4), and the maturity level `Proven` is awarded at rung 4 and nowhere else — so re-grade the spec to see this run move the number. It is immutable: read it back with Contract.read(file[, {asOf}]) to derive freshness at an explicit instant, which is what keeps a time-sensitive verdict re-derivable. Still exploring? Pass {draft:true} and it lands in build output instead — an exploratory pair that looks exactly like the real one is how a half-finished pair gets committed."
                        );
                     return JsApi.applyOutputConvention(JsApi.next((Map<String, Object>)var60, "Contract.read('" + var58 + "')", var61), () -> io.karatelabs.ext.pair.PairMarkdownReportRenderer.render(var57), var3);
                  } catch (PairException var36) {
                     return JsApi.missing("Contract.pair: " + var36.getMessage());
                  } finally {
                     if (var13 != null) {
                        var13.stop();
                     }
                  }
               }
            }
         }
      } else {
         return JsApi.missing(
            "Contract.pair({suite, spec, provider, …}) needs an options object, e.g. Contract.pair({suite:'checks/contract', spec:'openapi.yaml', provider:'https://staging.example.com'})"
         );
      }
   }

   private Object read(Object var1, Object var2) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("contract.read", "openapi");
      Object var3 = io.karatelabs.ext.pair.ContractReportArtifacts.readPairReport(this.root, var1, var2, "Contract.read");
      if (!(var3 instanceof Map var4)) {
         return var3;
      } else if (var4.get("error") != null) {
         return var4;
      } else {
         Object var5 = optsOf(var1, var2);
         return JsApi.applyOutputConvention(full(var5) ? var4 : summary(var4), () -> io.karatelabs.ext.pair.PairMarkdownReportRenderer.render(var4), var5);
      }
   }

   private static Object optsOf(Object var0, Object var1) {
      return var1 == null && var0 instanceof Map ? var0 : var1;
   }

   public Object report(Object var1, Object var2) {
      return io.karatelabs.ext.pair.ContractReportArtifacts.publishReport(this.root, this.reportDir, this.nav, var1, var2);
   }

   public static void siblingLinks(Map<String, Object> var0, Path var1) {
      io.karatelabs.ext.common.ReportAssetsInstaller.a(var0, var1);
   }

   private static boolean full(Object var0) {
      return var0 instanceof Map var1 && Boolean.TRUE.equals(var1.get("full"));
   }

   private static Map<String, Object> summary(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (String var3 : List.of(
         "headline",
         "actions",
         "evidenceFile",
         "selected",
         "draft",
         "probeNote",
         "pairId",
         "mintedAt",
         "scope",
         "dataProfile",
         "warnings",
         "rehearsal",
         "rehearsalCheck",
         "preconditionProbe",
         "claimSentence",
         "rung",
         "freshness",
         "claim",
         "legs",
         "excluded",
         "confirmation",
         "outcomes",
         "notMeasured",
         "coverageGraph"
      )) {
         if (var0.get(var3) != null) {
            var1.put(var3, var0.get(var3));
         }
      }

      if (var0.get("divergences") instanceof List var12) {
         ArrayList var4 = new ArrayList();

         for (Object var6 : var12) {
            if (var6 instanceof Map var7) {
               LinkedHashMap var8 = new LinkedHashMap();

               for (String var10 : List.of("slug", "name", "outcome", "cause", "confirmation")) {
                  if (var7.get(var10) != null) {
                     var8.put(var10, var7.get(var10));
                  }
               }

               if (var7.get("differences") instanceof List var16 && !var16.isEmpty()) {
                  var8.put("differences", var16.size());
               }

               var4.add(var8);
            }
         }

         var1.put("divergences", var4);
      }

      if (var0.get("ignored") instanceof Map var13 && !var13.isEmpty()) {
         var1.put("ignored", var0.get("ignored"));
      }

      if (var0.get("refresh") != null) {
         var1.put("refresh", var0.get("refresh"));
      }

      var1.put(
         "full",
         "this is the SUMMARY, and it holds everything bounded by the operation or rule count: the claim, the whole ladder with the bars behind every rung, the waiver register, and the notMeasured rows with their causes AND the values that decided them (a precondition mismatch is diagnosed off those, so they travel with it). What is NOT here is what grows with the suite — the compared values on each DIVERGENCE, where the findings list keeps a count instead, plus every quiet agreement (`scenarios`) and the bindings. Pass {full:true} for those, or open the file named in `evidenceFile`."
      );
      return var1;
   }

   private static Map<String, Map<String, Object>> bars(Object var0) {
      if (var0 == null) {
         return Map.of();
      } else if (!(var0 instanceof Map var1)) {
         throw new PairException(
            "`bars` takes an object of bar-id -> {reason, owner}, e.g. {bars: {dataProfile: {reason: 'both legs load one fixture by policy', owner: '@api-guild'}}}"
         );
      } else {
         LinkedHashMap var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            String var5 = (String)var4.getKey();
            if (!io.karatelabs.ext.pair.PairReportAssembler.PROVENANCE_FIELDS.contains(var5)) {
               throw new PairException(
                  "`bars` cannot stand down `"
                     + var5
                     + "`. Relaxable bars are "
                     + io.karatelabs.ext.pair.PairReportAssembler.PROVENANCE_FIELDS
                     + ". `adherence` and `rehearsal` are deliberately not among them: a leg that never addressed its target measured something other than the pair, and a provider that is itself a karate mock agrees with the mock by construction — neither is an exception to the claim, both are the absence of one. (An unknown id is refused rather than ignored, so a typo cannot read as a waiver that silently did nothing.)"
               );
            }

            if (!(var4.getValue() instanceof Map var6) || str(var6.get("reason")) == null || str(var6.get("owner")) == null) {
               throw new PairException(
                  "the `"
                     + var5
                     + "` bar needs reason, owner — standing a rung bar down is a WAIVER, and an anonymous exception is not an approval record. It is reported beside the rung it moved, so a reader can see the claim rests on an exception rather than on evidence."
               );
            }

            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("reason", str(var6.get("reason")));
            var8.put("owner", str(var6.get("owner")));
            var2.put(var5, var8);
         }

         return var2;
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Contract — one suite, two targets (the paired run)\n\nA contract test is **one functional suite run against the mock AND against the real\nprovider**. The two runs of one *minted* pair expose a **measured divergence set** — say\ndivergence set, never \"a fidelity number\": a pass/fail delta is only a lower bound.\n\nWhat you write is an ordinary suite. Nothing in it is contract-testing-shaped except a tag\nand the target being a variable:\n\n```gherkin\nBackground:\n  * url karate.properties['contract.target']   # the harness hands each leg its target\n\n@contract\nScenario: create an order, then read it back\n  ...\n```\n\n**Already reading a property of your own?** Most suites are — `baseUrl` is the common one.\nDo NOT rewrite the suite: pass `targetProp:'baseUrl'` and the harness hands each leg its\ntarget through that property instead.\n\n**The target being a variable is not a style note — it is the thing that makes a pair a\npair.** A suite that keeps its own base URL (or falls back to auto-starting its own mock)\ndoes not fail: it runs BOTH legs against the same place and agrees with itself. So the\nharness checks, off the exchanges each leg actually recorded, that the leg addressed the\ntarget it was handed — a leg that never did yields **no pair**, naming what it talked to\ninstead, and a single scenario that did not is `notmeasured`. If your config already reads\na property of its own, name it with `targetProp` rather than rewriting the suite. And if the\naddress a leg used IS the target under another name — a container reaching the host as\n`host.docker.internal`, a gateway in front of the provider — declare the equivalence with\n`aliases` rather than renaming anything: a signed waiver on the record, not a bar stood down.\n\n## `Contract.pair(opts)` — mint a pair and run it\n\n| Option | Means |\n|---|---|\n| `suite` | the suite BOTH legs answer to — a feature, a folder, or a list |\n| `spec` | the contract document — **optional**. With it, an exchange maps to the spec's operation and the spec's declared response headers are compared, the evidence binds the document, freshness watches it, and `Openapi.grade` reads the pair. Without it the pair still runs and the divergence set still stands — but operations are named by method and path only, no response headers are compared, and no document is bound (the claim block and sentence say so) |\n| `provider` | the real provider's base URL — **no provider leg, no pair** |\n| `mock` | **the mock whose substitutability you are claiming** — the URL of one already standing, the path to the karate mock feature your project ships (`'mock/api.feature'`), or `Http.mock` options (`{handlers:{…}}` / `{rules:{…}}`) overlaying the spec. Omit it and the pair stands one up from `spec` — a *synthesized* mock, which is a different subject from the one your CI runs. **Required when `spec` is omitted** — there is no document to synthesize one from. A value this harness cannot stand up is **refused**, never ignored |\n| `profile` | `seeded` (default) · `fixture` · `ambient` — how the two legs came to be in the same state |\n| `fixture` | required by (and only by) `profile:'fixture'` — the fixture BOTH legs were pre-loaded from; its digest becomes a freshness binding |\n| `ignore` | `'nondeterminism'` (the bundled starter profile) or your own `{id, key\\|scope, marker, reason, owner}` rules |\n| `tags` | tag expressions; `~@destructive` is added to BOTH legs whatever you pass |\n| `env`, `props`, `targetProp` | the karate env, extra system properties, and the property each leg's target is handed through (default `contract.target`) |\n| `aliases` | **two spellings that are one address**, declared and signed: `[{hosts:['host.docker.internal','localhost'], reason:'…', owner:'…'}]`. Only the four loopback spellings are normalised automatically; a container's `host.docker.internal`, a gateway or tunnel in front of the provider, a DNS alias for an IP literal are things this harness cannot know, and guessing at infrastructure is worse than asking. A member is a bare HOST — matching that host whatever port is being compared, though **the two ports must still agree** (declaring two hosts equal says nothing about their ports, so `localhost:41835` is not `host.docker.internal:8080`) — or a full origin (`https://gw.example.com:443`), matching that scheme/host/port exactly. This does **not** stand the adherence bar down — that bar cannot be stood down; it says which addresses ARE the target, travels in the artifact with its reason and owner, and a reader can disagree with it |\n| `confirm` | default **true** — a verdict-layer divergence is re-run on the failing leg before it is published |\n| `providerEnv`, `providerBuild`, `maxAgeDays` | what the evidence binds to, and how long it lives |\n| `out` | where the evidence file lands (default `contract/pairs/<date>-<pairId>.json`) |\n| `draft` | **while you are still setting this up.** The pair runs and is readable back, but lands under `target/` — so it is not project evidence, nothing else reads it, and nothing commits it. An exploratory pair that looks exactly like the real one is how a half-finished one ends up in git |\n\n## What comes back, and how to read it\n\n- **`outcomes`** — six classes, and the last one is the common one:\n  `agreed` · `staleMock` (mock passes, provider fails) · `mockBug` (provider passes, mock\n  fails) · `notassertedDivergence` (both passed and the **responses differ** — a finding\n  owned by your SUITE, whose assertions are too loose to see it) · `agreedFailure` (both red\n  — agreement about behaviour, not a fidelity finding) · **`notmeasured`**.\n- **`notmeasured` is decided BEFORE anything is compared** — an environment failure, a leg\n  that skipped, a failed precondition probe, a read over ambient state, a divergence that\n  did not reproduce. Classifying any of those as divergence manufactures findings out of\n  infrastructure.\n- **`claim` is always two numbers.** The measured proportion (divergence-free ÷ admissible)\n  NEVER travels without the coverage of the claim (exercised ÷ declared) beside it. A suite\n  touching three operations of forty must not read 100%. With no coverage graph there is no\n  percentage at all — the divergence set still ships as a list.\n- **`ignored` is a waiver register.** Nothing is ignored by default. A rule carries a reason\n  and an owner, re-classifies rather than suppresses (both values are kept), and is marked\n  **stale** when it covered nothing.\n- **`coverageGraph` says what the pair did NOT do.** A pair runs the suite twice into its own\n  run dirs and holds `@destructive` out of both legs, so it does not feed the PROJECT\n  coverage graph — counting it would double-count evidence and disagree with the suite's own\n  run. The claim's denominator is read from the pair's own graph; `Coverage.status`, the RTM\n  and `Openapi.grade`'s coverage/proven-ness dimensions still want an ordinary\n  `Runner.run(...)`, which folds into it. The block says so, and says whether the project\n  graph is missing or older than this pair.\n\n## The graded ladder — what the pair entitles you to SAY\n\nEvery operation the pair exercised gets a rung, and the artifact gets the **floor** of them.\n\n| Rung | Name | What earns it |\n|---|---|---|\n| 1 | `shape-conformant` | the contract declares this operation |\n| 2 | `verified-against-mock` | a suite exercised it and the MOCK leg was green |\n| 3 | `verified-against-provider` | the PROVIDER leg exercised it, addressed the target it was handed, and was green |\n| 4 | `proven-substitutable` | the two legs were compared and did not differ, at BOTH layers |\n\n**A rung carries the COUNT for every bar it rests on**, never just its name — adherence,\nprobe read-backs, the divergence tally. A bar whose only evidence is that nothing refused it\nis not evidence: `0 of 0` reads NOT CHECKED here as everywhere.\n\n**Rung 4 asks more than \"no differences\".** It also needs: the `seeded` profile (the other\ntwo bought their state rather than building it) · a precondition probe that actually\n**observed** the two legs starting equivalent · a provider that is **not** one of our mocks\n(a rehearsal cannot be waived at any price) · and the mock leg served by **the mock your\nproject ships**, because one synthesized from the same document your assertions were written\nagainst agrees with them by construction.\n\n**The aggregate is the floor of its parts, plus a cap**: while any declared operation is\nunexercised the artifact cannot hold rung 4, however clean the operations it did look at.\n\n**What rung 4 does NOT establish, permanently:** layer 2 compares status, body and only the\nheaders the contract declares, so the rung is silent by construction on transport-adjacent\ndifferences — header canonicalisation, charset, chunking, compression. And it is\n**release-time**, not a live badge: a decayed pair holds NO rung rather than a smaller one.\n\n**`Openapi.grade` reads the rung.** The `contract` dimension's score IS the rung normalised\n(rung ÷ 4), and the maturity ladder's top level `Proven` is awarded only at rung 4.\n\n### Standing a bar down is a WAIVER\n\n`bars: {dataProfile|mockIdentity|probeVerified: {reason, owner}}` — relaxing a bar is an\nexception to the claim, so it owes a reason and an owner, it is reported beside the rung it\nmoved, and the rung says it rests on an exception rather than on evidence. Tightening needs\nnothing. `adherence` and `rehearsal` are **not** relaxable: a leg that never reached its\ntarget measured something other than the pair, and a stand-in agreeing with itself is not an\nexception to the claim but the absence of one.\n\n## `Contract.read([file][, {asOf, full}])` — freshness, derived at read time\n\n**Returns a summary**: the claim sentence, the standing rung, the freshness verdict, both\nhalves of the proportion, the counts by outcome class, one line per finding saying what\ndiverged and how — and, verbatim, everything bounded by the operation or rule count: the\nwhole per-operation ladder with the bars behind each rung, the waiver register with its\nreasons and owners, and the `notMeasured` rows with their causes. The file is optional and leads, so the everyday call is\n`Contract.read()` — with no file it reads the pair the project minted **most recently**, by\nthe pair's own `mintedAt` and not by its file name (several are minted on any day you are\nfixing the wiring, and their names differ only by a random id). When it picked one of\nseveral, `selected` says so and names another — a default that picks silently is how one\npair's verdict gets quoted while a later, worse one sits unread beside it.\n\nPass **`{full:true}`** for what grows with the suite and is therefore NOT in the summary:\nthe two compared values on every difference, every quiet agreement (`scenarios`), and the\nbindings. That is the whole line — unbounded goes behind `full`, bounded travels in every\nshape — so nothing the summary names is something you then have to fetch. `evidenceFile`\nnames the committed file either way.\n\nThe stored pair is **immutable**. `Contract.read` compares today's bindings against the\npair's and reports:\n\n- **`stale`** — a binding changed (it names which), so the evidence is about a different\n  subject than the project holds today;\n- **`expired`** — nothing visibly changed; the evidence is simply older than `maxAgeDays`\n  (14 with the provider build known, **7 without** — the less we can bind, the faster it\n  expires);\n- **`fresh`** — every binding matches, at THIS `asOf`.\n\nThe bindings watched are the **spec**, the **suite**, the **mock's own declaration** (its\noverlay config, never its URL) and any **rulebook** it names, the **data profile**, and a\ndeclared **fixture**. A binding this reader could not recompute is named in `unchecked`\nrather than left to read as checked-and-fine.\n\n## `Contract.report([file][, {asOf, out}])` — the same reading, as a page\n\nWrites `ext/contract/pages/contract.html` into the report dir (`out` to put it elsewhere) —\nthe rung and its bars, the two numbers, the divergence set with the compared values, the\nwaiver register and the freshness verdict, for a reader who is not at a prompt. It renders\n**what `Contract.read` returns and re-derives nothing**, so the page and the verb cannot\ndisagree about what the pair claims; publish the report dir and it travels beside the\nCoverage and Traceability pages. Freshness is derived at READ time, so the page is a\nsnapshot at the `asOf` it was rendered with (it says so on the page) — re-render it after\nevery mint rather than treating it as live.\n\nA decayed pair carries **`refresh.call`** — the `Contract.pair(…)` call pre-bound to its own\nscope, waivers included, so re-minting produces the same claim — or says plainly that no\ncall exists and why (an `ambient` profile's ceiling; no recorded provider). Refreshing a\nDRAFT carries `draft:true` (an exploratory pair must not be promoted into project evidence by\na paste), and a `caution` rides along when the recorded provider is a loopback address in the\nOS's hand-out port range — the shape of a stand-in stood up in a console session, whose port\nis gone by the time anybody reads the pair.\n\n## The four things the discipline actually asks of you\n\n1. **Tag destructive operations `@destructive`** — they are held out of BOTH legs, and the\n   exclusion is disclosed in the denominator rather than quietly taken.\n2. **Declare a data profile** — and know that choosing `ambient` is choosing a ceiling\n   below *proven substitutable*.\n3. **Write setup through the API, in the `Background`** — that is the precondition probe,\n   and a read-back the API cannot do is outside the contract's scope by construction.\n4. **Give every ignore rule a reason and an owner** — it is a waiver.\n\n## Fences worth knowing before you read a result\n\n- **Timing is recorded, never compared** — a mock is faster by construction.\n- **Side effects are compared only through the API** — a database probe cannot belong to a\n  suite whose whole point is a swappable target.\n- **Callbacks, webhooks and async delivery are out of scope.**\n- **A leg that cannot run yields NO PAIR**, not an empty divergence set — but read the\n  boundary exactly, because two different things look alike from a distance. *Refused\n  outright*: a leg that ran and never addressed the target it was handed, and two targets\n  that are one target — wiring faults, decided before anything executes. *Minted, and\n  every scenario `notmeasured`*: a target that was addressed and refused us (connection\n  refused, auth, a rate limit). The second is a pair whose honest content is \"we could not\n  measure\", and its `claim` says so — **`measured:false`, no percentage** — precisely so an\n  empty divergence set can never be read as fidelity. If you ever see `measured:true` over\n  `0 of 0`, that is a bug, not a clean bill of health.\n- **A `provider` that is itself a karate mock makes this a REHEARSAL**, and the payload says\n  so (`rehearsal`, and in the claim sentence). Two instances of one mock sit at two\n  addresses and agree with each other by construction — no URL check can see that, so the\n  wire is asked instead (the `Karate-Mock` header, the same signal `mockOnly` rides).\n  Rehearsing the mechanism against a stand-in is fine; quoting its numbers as a provider\n  claim is not.\n- **Execution order is pinned and scenario parallelism is off** — two legs that ordered a\n  stateful suite differently are not comparable.\n"
      );
   }

   private static Path resolve(Path var0, String var1) {
      Path var2 = Path.of(var1);
      return (var2.isAbsolute() ? var2 : var0.resolve(var2)).normalize();
   }

   private static List<String> paths(Object var0) {
      ArrayList var1 = new ArrayList();
      if (var0 instanceof List) {
         for (Object var4 : (List)var0) {
            if (var4 != null && !String.valueOf(var4).isBlank()) {
               var1.add(String.valueOf(var4));
            }
         }
      } else if (var0 != null && !String.valueOf(var0).isBlank()) {
         var1.add(String.valueOf(var0));
      }

      return var1;
   }

   private static List<String> tags(Object var0) {
      return paths(var0);
   }

   private static Map<String, String> props(Object var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (var4.getKey() != null && var4.getValue() != null) {
               var1.put(String.valueOf(var4.getKey()), String.valueOf(var4.getValue()));
            }
         }
      }

      return var1;
   }

   private static String str(Object var0) {
      String var1 = var0 == null ? null : String.valueOf(var0);
      return var1 != null && !var1.isBlank() ? var1 : null;
   }

   private static Integer intOrNull(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : null;
   }
}
