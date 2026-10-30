package io.karatelabs.markup;

import org.thymeleaf.model.IProcessableElementTag;

final class FragmentTrace {
   static final String WRAPPER_TAG = "ka-trace";
   static final String MARKER_ATTR = "data-ka-trace";

   private FragmentTrace() {
   }

   static boolean enabled(MarkupConfig config) {
      return config != null && config.isDevMode() && config.isDevTrace();
   }

   static String buildPayload(String kind, String fragmentExpr, IProcessableElementTag tag) {
      StringBuilder sb = new StringBuilder(kind).append('|').append(sanitize(fragmentExpr));
      String withExpr = tag.getAttributeValue("th", "with");
      if (withExpr != null && !withExpr.isEmpty()) {
         sb.append('|').append(sanitize(withExpr));
      }

      return sb.toString();
   }

   private static String sanitize(String s) {
      return s == null ? "" : s.replace("-->", "--&gt;").replace("|", "&#124;");
   }
}
