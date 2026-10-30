package io.karatelabs.ext.coverage;

import java.util.List;

public record Warning(String type, String endpoint, Integer status, String message) {
   public static final String TYPE_UNDOCUMENTED_RESPONSE = "undocumented_response";
   public static final String TYPE_DEPRECATED_TESTED = "deprecated_tested";
   public static final String TYPE_UNMATCHED_CALL = "unmatched_call";
   public static final String TYPE_AMBIGUOUS_MATCH = "ambiguous_match";
   public static final String TYPE_OVERLAPPING_TEMPLATES = "overlapping_templates";

   public Warning(String type, String endpoint, Integer status, String message) {
      this.type = type;
      this.endpoint = endpoint;
      this.status = status;
      this.message = message;
   }

   public static Warning undocumentedResponse(String var0, int var1) {
      return new Warning("undocumented_response", var0, var1, "Undocumented response: " + var0 + " returned " + var1 + " (not in spec)");
   }

   public static Warning deprecatedTested(String var0) {
      return new Warning("deprecated_tested", var0, null, "Deprecated endpoint tested: " + var0);
   }

   public static Warning ambiguousMatch(String var0, List<String> var1) {
      return new Warning(
         "ambiguous_match",
         var0,
         null,
         "Ambiguous match: "
            + var0
            + " could bind to any of "
            + var1
            + " — the credited operation may be wrong (add a more specific path or disambiguate the spec)"
      );
   }

   public static Warning overlappingTemplates(String var0, String var1) {
      return new Warning(
         "overlapping_templates",
         var0 + " ~ " + var1,
         null,
         "Overlapping path templates: " + var0 + " and " + var1 + " can both match some concrete path — observed traffic may bind ambiguously"
      );
   }
}
