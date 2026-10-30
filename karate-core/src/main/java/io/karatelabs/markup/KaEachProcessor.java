package io.karatelabs.markup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaEachProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaEachProcessor.class);
   private static final int PRECEDENCE = 200;
   private static final String ATTR_NAME = "each";

   KaEachProcessor(TemplateMode templateMode, String dialectPrefix) {
      super(templateMode, dialectPrefix, null, false, "each", true, 200, true);
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String av, IElementTagStructureHandler structureHandler
   ) {
      int pos = av.indexOf(58);
      if (pos == -1) {
         throw new RuntimeException("th:each requires `name : expression` form (got `" + av + "`).");
      } else {
         String varPart = av.substring(0, pos).trim();
         av = av.substring(pos + 1).trim();
         String statusVarName = null;
         int commaPos = varPart.indexOf(44);
         String iterVarName;
         if (commaPos != -1) {
            iterVarName = varPart.substring(0, commaPos).trim();
            statusVarName = varPart.substring(commaPos + 1).trim();
         } else {
            iterVarName = varPart;
         }

         MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
         Object value = kec.evalLocal(av);
         if (value instanceof Map<?, ?> map) {
            List<Map<String, Object>> entries = new ArrayList<>();

            for (Entry<?, ?> entry : map.entrySet()) {
               Map<String, Object> entryMap = new LinkedHashMap<>();
               entryMap.put("key", entry.getKey());
               entryMap.put("value", entry.getValue());
               entries.add(entryMap);
            }

            value = entries;
         }

         structureHandler.iterateElement(iterVarName, statusVarName, value);
      }
   }
}
