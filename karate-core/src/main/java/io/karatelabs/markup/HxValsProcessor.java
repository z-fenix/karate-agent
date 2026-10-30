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

class HxValsProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(HxValsProcessor.class);

   HxValsProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "vals", true, 1000, true);
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      Object result = kec.evalLocalAsObject(attributeValue);
      if (!(result instanceof Map)) {
         logger.warn("ka:vals did not evaluate to json: {}", attributeValue);
      } else {
         String json = Json.of(result).toString();
         structureHandler.setAttribute("hx-vals", KaDataProcessor.escapeSingleQuoted(json), AttributeValueQuotes.SINGLE);
      }
   }
}
