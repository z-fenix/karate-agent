package io.karatelabs.markup;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaWithProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaWithProcessor.class);
   private static final int PRECEDENCE = 600;
   private static final String ATTR_NAME = "with";

   KaWithProcessor(TemplateMode templateMode, String dialectPrefix) {
      super(templateMode, dialectPrefix, null, false, "with", true, 600, true);
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String av, IElementTagStructureHandler structureHandler
   ) {
      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      Object evalResult = kec.evalLocalAsObject(av);
      if (!(evalResult instanceof Map<?, ?> rawMap)) {
         logger.warn("value did not evaluate to json: {}", av);
      } else {
         Map<String, Object> map = (Map<String, Object>) rawMap;
         map.forEach(structureHandler::setLocalVariable);
      }
   }
}
