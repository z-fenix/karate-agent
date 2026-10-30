package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaDataMirrorProcessor extends AbstractAttributeTagProcessor {
   private static final String DATA_MIRROR = "data-mirror";
   private static final int PRECEDENCE = 1000;

   KaDataMirrorProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "data-mirror", true, 1000, true);
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      structureHandler.setAttribute("type", "hidden", AttributeValueQuotes.DOUBLE);
      structureHandler.setAttribute("name", attributeValue, AttributeValueQuotes.DOUBLE);
      structureHandler.setAttribute(":value", "JSON.stringify(" + attributeValue + ")", AttributeValueQuotes.DOUBLE);
   }
}
