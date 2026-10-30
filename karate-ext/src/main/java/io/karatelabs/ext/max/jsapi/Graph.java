package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Graph extends JsDispatch {
   static final String DATA_FILE = "ext/coverage/coverage-data.json";
   private final Path dataFile;

   public Graph(Path var1) {
      this.dataFile = var1;
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("node", var1x -> this.node(JsApi.str(var1x, 0)));
      this.fn("neighbors", var1x -> this.neighbors(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("edges", var1x -> this.edges(JsApi.arg(var1x, 0)));
      this.fn("traverse", var1x -> this.traverse(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("path", var1x -> this.path(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Graph — direction-agnostic traversal over the traceability property graph\n\nThe raw property-graph view of `coverage-data.json`: typed nodes (`item`/`test`) and typed\nedges walkable both ways (`hit`/`parent`/`ref`/`real`/`decomposes`). The LLM composes\nqueries; `Coverage.*`/`Requirement.*` are thin views over the same graph.\n\n- `Graph.node(id)` — one node by id (e.g. `req:ORD-001`, `openapi:placeOrder`).\n- `Graph.neighbors(id[, {kind, dir}])` — adjacent nodes (filter by edge `kind`/direction).\n- `Graph.edges([{kind}])` — edges, optionally one kind.\n- `Graph.traverse(start[, {follow, dir, depth}])` — BFS walk (cycle-safe).\n- `Graph.path(a, b[, opts])` — shortest path between two nodes.\n\nRelated: `Coverage.*`/`Requirement.*` (views), `Report.generate()`."
      );
   }

   public static Graph atReportDir(Path var0) {
      return new Graph(var0.resolve("ext/coverage/coverage-data.json"));
   }

   Map<String, Object> node(String var1) {
      io.karatelabs.ext.coverage.graph.PropertyGraph var2 = this.graph();
      if (var2 == null) {
         return this.notFound();
      } else if (var1 == null) {
         return JsApi.missing("Graph.node(id) requires a node id, e.g. 'openapi:listProducts' or 'req:ORD-001'");
      } else {
         io.karatelabs.ext.coverage.graph.PropertyNode var3 = var2.a(var1);
         return var3 == null ? JsApi.suggest(JsApi.notFound("no such node: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var2.b())) : nodeRow(var3);
      }
   }

   Map<String, Object> neighbors(String var1, Object var2) {
      io.karatelabs.ext.coverage.graph.PropertyGraph var3 = this.graph();
      if (var3 == null) {
         return this.notFound();
      } else if (var1 == null) {
         return JsApi.missing("Graph.neighbors(id, {kind?, dir?}) requires a node id");
      } else {
         var var4 = kindsOf(var2, "kind");
         io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var5 = dirOf(var2);
         var var6 = var3.a(var1, var4, var5);
         if (var6.isEmpty() && var3.a(var1) == null) {
            return JsApi.suggest(JsApi.notFound("no such node: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var3.b()));
         } else {
            ArrayList<java.util.LinkedHashMap> var7 = new ArrayList();

            for (io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor var9 : (Iterable<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyNeighbor>) (Iterable<?>) (var6)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
               var10.put("id", var9.a());
               io.karatelabs.ext.coverage.graph.PropertyNode var11 = var3.a(var9.a());
               var10.put("type", var11 != null ? var11.b() : null);
               var10.put("kind", var9.b());
               var10.put("dir", var9.c().name().toLowerCase(Locale.ROOT));
               var7.add(var10);
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
            var12.put("id", var1);
            var12.put("neighbors", var7);
            return var12;
         }
      }
   }

   Object edges(Object var1) {
      io.karatelabs.ext.coverage.graph.PropertyGraph var2 = this.graph();
      if (var2 == null) {
         return this.notFound();
      } else {
         String var3 = strOpt(var1, "from");
         String var4 = strOpt(var1, "to");
         var var5 = kindsOf(var1, "kind");
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var6 = new ArrayList();

         for (io.karatelabs.ext.coverage.graph.PropertyEdge var8 : (Iterable<io.karatelabs.ext.coverage.graph.PropertyEdge>) (Iterable<?>) (var2.a(var3, var4, var5))) {
            var6.add(edgeRow(var8));
         }

         return var6;
      }
   }

   Map<String, Object> traverse(String var1, Object var2) {
      io.karatelabs.ext.coverage.graph.PropertyGraph var3 = this.graph();
      if (var3 == null) {
         return this.notFound();
      } else if (var1 == null) {
         return JsApi.missing("Graph.traverse(start, {follow?, dir?, depth?}) requires a start node id");
      } else if (var3.a(var1) == null && var3.a(var1, null, io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.BOTH).isEmpty()) {
         return JsApi.suggest(JsApi.notFound("no such node: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var3.b()));
      } else {
         var var4 = kindsOf(var2, "follow");
         io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var5 = dirOf(var2);
         int var6 = intOf(var2, "depth", 0);
         ArrayList<java.util.LinkedHashMap> var7 = new ArrayList();

         for (io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth var9 : (Iterable<io.karatelabs.ext.coverage.graph.PropertyGraph.PropertyDepth>) (Iterable<?>) (var3.a(var1, var4, var5, var6))) {
            LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
            var10.put("id", var9.a());
            io.karatelabs.ext.coverage.graph.PropertyNode var11 = var3.a(var9.a());
            var10.put("type", var11 != null ? var11.b() : null);
            var10.put("depth", var9.b());
            var7.add(var10);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("start", var1);
         var12.put("dir", var5.name().toLowerCase(Locale.ROOT));
         var12.put("nodes", var7);
         return var12;
      }
   }

   Map<String, Object> path(String var1, String var2, Object var3) {
      io.karatelabs.ext.coverage.graph.PropertyGraph var4 = this.graph();
      if (var4 == null) {
         return this.notFound();
      } else if (var1 != null && var2 != null) {
         var var5 = kindsOf(var3, "follow");
         io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection var6 = dirOf(var3);
         var var7 = var4.a(var1, var2, var5, var6);
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
         var8.put("from", var1);
         var8.put("to", var2);
         var8.put("found", !var7.isEmpty());
         var8.put("length", var7.isEmpty() ? 0 : var7.size() - 1);
         var8.put("path", var7);
         return var8;
      } else {
         return JsApi.missing("Graph.path(a, b, {dir?, follow?}) requires two node ids");
      }
   }

   private static Map<String, Object> nodeRow(io.karatelabs.ext.coverage.graph.PropertyNode var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", var0.a());
      var1.put("type", var0.b());
      var1.put("source", var0.c());
      var1.putAll(var0.d());
      return var1;
   }

   private static Map<String, Object> edgeRow(io.karatelabs.ext.coverage.graph.PropertyEdge var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("from", var0.a());
      var1.put("to", var0.b());
      var1.put("kind", var0.c());
      var1.putAll(var0.d());
      return var1;
   }

   private static Set<String> kindsOf(Object var0, String var1) {
      Object var2 = var0 instanceof Map var3 ? var3.get(var1) : null;
      if (var2 == null) {
         return null;
      } else if (var2 instanceof List var7) {
         LinkedHashSet<java.lang.String> var4 = new LinkedHashSet();

         for (Object var6 : var7) {
            if (var6 != null) {
               var4.add(String.valueOf(var6));
            }
         }

         return var4.isEmpty() ? null : var4;
      } else {
         return Set.of(String.valueOf(var2));
      }
   }

   private static io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection dirOf(Object var0) {
      Object var1 = var0 instanceof Map var2 ? var2.get("dir") : null;
      return io.karatelabs.ext.coverage.graph.PropertyGraph.TraversalDirection.parse(var1 == null ? null : String.valueOf(var1));
   }

   private static String strOpt(Object var0, String var1) {
      Object var2 = var0 instanceof Map var3 ? var3.get(var1) : null;
      return var2 == null ? null : String.valueOf(var2);
   }

   private static int intOf(Object var0, String var1, int var2) {
      return (var0 instanceof Map var4 ? var4.get(var1) : null) instanceof Number var5 ? var5.intValue() : var2;
   }

   private io.karatelabs.ext.coverage.graph.PropertyGraph graph() {
      var var1 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.dataFile).c();
      return var1 == null ? null : io.karatelabs.ext.coverage.graph.PropertyGraph.a(var1);
   }

   private Map<String, Object> notFound() {
      return JsApi.dataError(
         io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.dataFile),
         this.dataFile,
         "run your suite (e.g. Runner.suite(...)) — on the console its settle builds the graph — or Report.aggregate() to build it from runs/"
      );
   }
}
