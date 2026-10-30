package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.ExtMeta;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProjectionDiffBuilder {
   public static final String a = "projection.diff/1";
   static final String b = "karate-projection-diff";
   private static final Set<io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind> c = EnumSet.of(
      io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.OPERATION_UNMODELLED, io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.REQUEST_DEPARTS, io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.RESPONSE_DEPARTS, io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED, io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.LIFECYCLE_DIFFERS
   );

   private ProjectionDiffBuilder() {
   }

   public static io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind a(String var0, String var1) {
      return switch (var0) {
         case "OPERATION_REMOVED", "OPERATION_REMOVED_BEFORE_SUNSET" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.OPERATION_UNMODELLED;
         case "OPERATION_ADDED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.OPERATION_UNCLAIMED;
         case "OPERATION_ID_CHANGED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.OPERATION_ID_DIFFERS;
         case "OPERATION_PATH_CHANGED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.ROUTE_DIFFERS;
         case "PATH_PARAM_RENAMED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.PATH_PARAM_NAME_DIFFERS;
         case "SERVERS_CHANGED", "SECURITY_TIGHTENED", "SECURITY_LOOSENED", "RESPONSE_HEADER_ADDED", "RESPONSE_HEADER_REMOVED", "RESPONSE_HEADER_BECAME_OPTIONAL", "RESPONSE_HEADER_BECAME_REQUIRED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED;
         case "DEPRECATION_ADDED", "DEPRECATION_REMOVED", "SUNSET_SET", "SUNSET_CHANGED", "SUNSET_REMOVED", "SUCCESSOR_CHANGED", "SUCCESSOR_UNRESOLVED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.LIFECYCLE_DIFFERS;
         case "COMPONENT_CHANGED" -> io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.COMPONENT_DIFFERS;
         case "PARAM_ADDED_REQUIRED", "PARAM_ADDED_OPTIONAL", "PARAM_REMOVED", "PARAM_BECAME_REQUIRED", "PARAM_BECAME_OPTIONAL", "PARAM_STYLE_CHANGED", "REQUEST_BODY_ADDED_REQUIRED", "REQUEST_BODY_ADDED_OPTIONAL", "REQUEST_BODY_BECAME_REQUIRED", "REQUEST_BODY_BECAME_OPTIONAL", "REQUEST_BODY_REMOVED", "REQUEST_MEDIA_TYPE_REMOVED", "REQUEST_MEDIA_TYPE_ADDED", "RESPONSE_SUCCESS_STATUS_REMOVED", "RESPONSE_STATUS_REMOVED", "RESPONSE_STATUS_ADDED", "RESPONSE_MEDIA_TYPE_REMOVED", "RESPONSE_MEDIA_TYPE_ADDED", "PROPERTY_ADDED_REQUIRED", "PROPERTY_ADDED_OPTIONAL", "PROPERTY_REMOVED", "PROPERTY_BECAME_REQUIRED", "PROPERTY_BECAME_OPTIONAL", "TYPE_WIDENED", "TYPE_NARROWED", "TYPE_CHANGED", "NULLABLE_ADDED", "NULLABLE_REMOVED", "ENUM_VALUE_ADDED", "ENUM_VALUE_REMOVED", "CONSTRAINT_TIGHTENED", "CONSTRAINT_LOOSENED", "CONSTRAINT_CHANGED", "ADDITIONAL_PROPERTIES_CLOSED", "ADDITIONAL_PROPERTIES_OPENED", "DEFAULT_CHANGED", "EXTENSIBLE_ENUM_ADDED", "EXTENSIBLE_ENUM_REMOVED", "SCHEMA_ADDED", "SCHEMA_REMOVED", "COMPOSITION_BRANCH_ADDED", "COMPOSITION_BRANCH_REMOVED" -> "response"
               .equals(var1)
            ? io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.RESPONSE_DEPARTS
            : io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.REQUEST_DEPARTS;
         default -> throw new IllegalStateException("openapi.diff/1 kind " + var0 + " has no projection.diff/1 label");
      };
   }

   public static Map<String, Object> a(Map<String, Object> var0, io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var1, String var2, io.karatelabs.ext.lint.WaiverLedger var3, LocalDate var4) {
      io.karatelabs.ext.diff.DiffReport var5 = new io.karatelabs.ext.diff.DiffReport("projection.diff/1", "karate-projection-diff", ExtMeta.version("karate-max-meta.properties"));

      for (String var7 : List.of("breaking", "warnings", "compatible")) {
         for (Map var9 : (Iterable<Map>) (Iterable<?>) ((List)var0.get(var7))) {
            var5.a(a(var5, var9, var1.b(), var3));
         }
      }

      for (Map var14 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("notEvaluated"))) {
         boolean var15 = "SECURITY_SCHEME".equals(var14.get("reason"));
         if ((var15 || "servers".equals(var14.get("construct"))) && "baseline".equals(var14.get("side"))) {
            String var16 = (String)var14.get("pointer");
            String var10 = var15 ? "auth" : "servers";
            io.karatelabs.ext.diff.DiffFinding var11 = var5.a(io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED.name(), io.karatelabs.ext.diff.DiffSeverity.WARNING)
               .a(var16)
               .b(var10 + " is outside the projection fence")
               .a(var15 ? "securityScheme" : "servers", var16, null)
               .b(var16, var16)
               .a("fence", (Object)var10);
            var5.a(var3 == null ? var11 : var11.b(var3.findApplicableWaiver(io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED.name(), var16, var16)));
         } else {
            var5.a(
               new io.karatelabs.ext.diff.UnevaluatedDiff(
                  (String)var14.get("reason"), (String)var14.get("construct"), (String)var14.get("side"), (String)var14.get("pointer"), var14.get("detail")
               )
            );
         }
      }

      if (var3 != null) {
         var5.a(var4.toString()).a(var3.report());
      }

      Map<java.lang.String, java.lang.String> var13 = (Map)var0.get("baseline");
      return var5.a(
            new io.karatelabs.ext.diff.DiffReport.DiffArtifact((String)var13.get("source"), (String)var13.get("digest"), "file"),
            new io.karatelabs.ext.diff.DiffReport.DiffArtifact("Rule.project('" + var2 + "')", var1.c(), "projection")
         )
         .a(true)
         .a("notprojected", var1.e())
         .toMap();
   }

   private static io.karatelabs.ext.diff.DiffFinding a(io.karatelabs.ext.diff.DiffReport var0, Map<String, Object> var1, Map<String, Object> var2, io.karatelabs.ext.lint.WaiverLedger var3) {
      String var4 = (String)var1.get("kind");
      String var5 = (String)var1.get("direction");
      Map<java.lang.String, java.lang.String> var6 = (Map)var1.get("construct");
      String var7 = (String)var6.get("key");
      String var8 = (String)var1.get("pointer");
      String var9 = (String)var1.get("authored");
      io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind var10 = a(var4, var5);
      String var11 = (String)var6.get("facet");
      String var12 = null;
      if (var10 == io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.REQUEST_DEPARTS && "parameter".equals(var6.get("kind")) && !var7.startsWith("path")) {
         var10 = io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED;
         var12 = var7.startsWith("query:") ? "queryParameters" : "headers";
      } else if (var10 == io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.CONSTRUCT_FENCED) {
         var12 = var4.startsWith("SECURITY") ? "auth" : (var4.startsWith("SERVERS") ? "servers" : "headers");
      } else if ("RESPONSE_STATUS_ADDED".equals(var4) && a(var2, var8, var7)) {
         var10 = io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.ENGINE_RESPONSE;
      }

      if (c.contains(var10)) {
         var11 = var11 == null ? var4 : var4 + ":" + var11;
      }

      io.karatelabs.ext.diff.DiffFinding var13 = var0.a(var10.name(), var10.level).a((String)var1.get("path")).b(a(var1, var4));
      if (var1.get("subject") instanceof Map var14) {
         var13.a((String)var14.get("kind"), (String)var14.get("key"));
      }

      if (var3 != null) {
         var13.b(var3.findApplicableWaiver(var10.name(), var8, var9));
      }

      return var13.a((String)var6.get("kind"), var7, var11)
         .c(var5)
         .b(var8, var9)
         .a((Map<String, Object>)var1.get("baselineLocation"), (Map<String, Object>)var1.get("currentLocation"))
         .a("operation", var1.get("operation"))
         .a("moved", var1.get("moved"))
         .a("witness", var1.get("witness"))
         .a("fence", (Object)var12);
   }

   private static String a(Map<String, Object> var0, String var1) {
      String var2 = (String)var0.get("detail");
      if (var2 != null && !var2.isBlank()) {
         return var2;
      } else {
         return switch (var1) {
            case "PROPERTY_ADDED_OPTIONAL" -> "optional property added";
            case "PROPERTY_ADDED_REQUIRED" -> "required property added";
            default -> var1.toLowerCase(Locale.ROOT).replace('_', ' ');
         };
      }
   }

   private static boolean a(Map<String, Object> var0, String var1, String var2) {
      String var3 = "400".equals(var2) ? "args_invalid" : ("404".equals(var2) ? "not_found" : null);
      Object var4 = var0;

      for (String var8 : var1.substring(1).split("/")) {
         var4 = var4 instanceof Map var9 ? var9.get(var8.replace("~1", "/").replace("~0", "~")) : null;
      }

      return var3 != null
            && var4 instanceof Map var11
            && var11.get("content") instanceof Map var12
            && var12.get("application/json") instanceof Map var13
            && var13.get("schema") instanceof Map var14
            && var14.get("properties") instanceof Map var15
         ? var15.values().stream().anyMatch(var1x -> var1x instanceof Map var2x && List.of(var3).equals(var2x.get("enum")))
         : false;
   }

   public static io.karatelabs.ext.lint.WaiverLedger a(Object var0, String var1, LocalDate var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind var7 : io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.ProjectionFindingKind.values()) {
         var3.add(var7.name());
      }

      return io.karatelabs.ext.lint.WaiverLedger.create(var0, var1, "projection.diff/1", var3, var2);
   }

   public static enum ProjectionFindingKind {
      OPERATION_UNMODELLED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      OPERATION_UNCLAIMED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      OPERATION_ID_DIFFERS(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      ROUTE_DIFFERS(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      PATH_PARAM_NAME_DIFFERS(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
      REQUEST_DEPARTS(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      RESPONSE_DEPARTS(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      ENGINE_RESPONSE(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
      CONSTRUCT_FENCED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
      LIFECYCLE_DIFFERS(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
      COMPONENT_DIFFERS(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE);

      final io.karatelabs.ext.diff.DiffSeverity level;

      private ProjectionFindingKind(io.karatelabs.ext.diff.DiffSeverity var3) {
         this.level = var3;
      }
   }
}
