package io.karatelabs.ext.openapi.changes;

enum OpenApiChangeKind {
   OPERATION_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   OPERATION_REMOVED_BEFORE_SUNSET(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   OPERATION_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   OPERATION_PATH_CHANGED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   OPERATION_ID_CHANGED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   PATH_PARAM_RENAMED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   SERVERS_CHANGED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   SECURITY_TIGHTENED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   SECURITY_LOOSENED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PARAM_ADDED_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   PARAM_ADDED_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PARAM_REMOVED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   PARAM_BECAME_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   PARAM_BECAME_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PARAM_STYLE_CHANGED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   REQUEST_BODY_ADDED_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   REQUEST_BODY_ADDED_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   REQUEST_BODY_BECAME_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   REQUEST_BODY_BECAME_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   REQUEST_BODY_REMOVED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   REQUEST_MEDIA_TYPE_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   REQUEST_MEDIA_TYPE_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   RESPONSE_SUCCESS_STATUS_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   RESPONSE_STATUS_REMOVED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   RESPONSE_STATUS_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   RESPONSE_MEDIA_TYPE_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   RESPONSE_MEDIA_TYPE_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   RESPONSE_HEADER_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   RESPONSE_HEADER_REMOVED(null),
   RESPONSE_HEADER_BECAME_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   RESPONSE_HEADER_BECAME_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PROPERTY_ADDED_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PROPERTY_ADDED_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PROPERTY_REMOVED(null, null),
   PROPERTY_BECAME_REQUIRED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   PROPERTY_BECAME_OPTIONAL(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   TYPE_WIDENED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   TYPE_NARROWED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   TYPE_CHANGED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   NULLABLE_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   NULLABLE_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   ENUM_VALUE_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   ENUM_VALUE_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   CONSTRAINT_TIGHTENED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   CONSTRAINT_LOOSENED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   CONSTRAINT_CHANGED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   ADDITIONAL_PROPERTIES_CLOSED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   ADDITIONAL_PROPERTIES_OPENED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   DEFAULT_CHANGED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.WARNING),
   EXTENSIBLE_ENUM_ADDED(null, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   EXTENSIBLE_ENUM_REMOVED(null, io.karatelabs.ext.diff.DiffSeverity.WARNING),
   SCHEMA_ADDED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   SCHEMA_REMOVED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   COMPOSITION_BRANCH_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE, io.karatelabs.ext.diff.DiffSeverity.BREAKING),
   COMPOSITION_BRANCH_REMOVED(io.karatelabs.ext.diff.DiffSeverity.BREAKING, io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   DEPRECATION_ADDED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   DEPRECATION_REMOVED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   SUNSET_SET(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   SUNSET_CHANGED(null),
   SUNSET_REMOVED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE),
   SUCCESSOR_CHANGED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   SUCCESSOR_UNRESOLVED(io.karatelabs.ext.diff.DiffSeverity.WARNING),
   COMPONENT_CHANGED(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE);

   private final io.karatelabs.ext.diff.DiffSeverity request;
   private final io.karatelabs.ext.diff.DiffSeverity response;

   private OpenApiChangeKind(io.karatelabs.ext.diff.DiffSeverity var3) {
      this(var3, var3);
   }

   private OpenApiChangeKind(io.karatelabs.ext.diff.DiffSeverity var3, io.karatelabs.ext.diff.DiffSeverity var4) {
      this.request = var3;
      this.response = var4;
   }

   io.karatelabs.ext.diff.DiffSeverity level(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1) {
      return var1 == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE ? this.response : this.request;
   }

   boolean annotation() {
      return this == DEFAULT_CHANGED || this == DEPRECATION_ADDED || this == DEPRECATION_REMOVED;
   }

   static enum ChangeDirection {
      REQUEST,
      RESPONSE,
      NONE;

      String wire() {
         return this.name().toLowerCase();
      }
   }
}
