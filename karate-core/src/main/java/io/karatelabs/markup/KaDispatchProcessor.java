package io.karatelabs.markup;

import io.karatelabs.common.Json;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaDispatchProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaDispatchProcessor.class);
   private static final String DISPATCH = "dispatch";
   private static final String VALS = "vals";

   KaDispatchProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "dispatch", true, 1000, true);
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      String av = attributeValue.trim();
      String eventName = av;
      String triggerOn = null;
      int at = av.indexOf(64);
      if (at >= 0) {
         if (av.lastIndexOf(64) != at) {
            throw new RuntimeException("ka:dispatch supports a single `event @ trigger` delimiter; got `" + attributeValue + "`");
         }

         eventName = av.substring(0, at).trim();
         triggerOn = av.substring(at + 1).trim();
         if (triggerOn.isEmpty()) {
            throw new RuntimeException("ka:dispatch trigger must be non-empty after `@`; got `" + attributeValue + "`");
         }
      }

      if (eventName.isEmpty()) {
         throw new RuntimeException("ka:dispatch requires an event name; got `" + attributeValue + "`");
      } else {
         MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
         String detailJson = "{}";
         String vals = tag.getAttributeValue(this.getDialectPrefix(), "vals");
         if (vals != null && !vals.isEmpty()) {
            Object result = kec.evalLocalAsObject(vals);
            if (result instanceof Map) {
               detailJson = Json.of(result).toString();
            } else if (result != null) {
               logger.warn("ka:dispatch ignored ka:vals — did not evaluate to an object: {}", vals);
            }
         }

         String js = "window.dispatchEvent(new CustomEvent(\""
            + escapeJsString(eventName)
            + "\", {detail: "
            + detailJson
            + ", bubbles: true, composed: true}))";
         if (triggerOn != null) {
            structureHandler.setAttribute("hx-on:" + triggerOn, KaDataProcessor.escapeSingleQuoted(js), AttributeValueQuotes.SINGLE);
         } else {
            structureHandler.setAttribute("onclick", KaDataProcessor.escapeSingleQuoted(js), AttributeValueQuotes.SINGLE);
         }
      }
   }

   private static String escapeJsString(String s) {
      StringBuilder sb = new StringBuilder(s.length() + 4);
      int i = 0;

      for (int n = s.length(); i < n; i++) {
         char c = s.charAt(i);
         switch (c) {
            case '\n':
               sb.append("\\n");
               break;
            case '\r':
               sb.append("\\r");
               break;
            case '"':
               sb.append("\\\"");
               break;
            case '\\':
               sb.append("\\\\");
               break;
            default:
               sb.append(c);
         }
      }

      return sb.toString();
   }
}
