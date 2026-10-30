package io.karatelabs.markup;

import org.thymeleaf.exceptions.TemplateProcessingException;

final class FragmentSupport {
   private FragmentSupport() {
   }

   static void translateSignatureError(TemplateProcessingException e) {
      String msg = e.getMessage();
      if (msg != null && msg.contains("Cannot resolve fragment. Signature")) {
         throw new TemplateProcessingException(signatureMessage(null, null, msg), e);
      }
   }

   static String signatureMessage(String offendingValue, String resourceDescription, String thymeleafMsg) {
      StringBuilder sb = new StringBuilder();
      sb.append("karate-markup does not support param lists in th:fragment signatures.\n");
      if (offendingValue != null) {
         sb.append("  Found    th:fragment=\"").append(offendingValue).append("\"");
         if (resourceDescription != null) {
            sb.append("   in ").append(resourceDescription);
         }

         sb.append("\n");
      }

      sb.append("  Change   th:fragment=\"name(p1, p2)\"   ➜   th:fragment=\"name\"\n");
      sb.append("  Pass values via th:with at the call site; the fragment reads them as\n");
      sb.append("  ordinary scope variables. Unset names resolve to null (no typeof guard\n");
      sb.append("  needed). See karate-skills markup/skill.md Gotcha #21.");
      if (thymeleafMsg != null) {
         sb.append("\n  Original Thymeleaf error: ").append(thymeleafMsg);
      }

      return sb.toString();
   }
}
