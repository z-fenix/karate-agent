package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class HxGenericProcessor extends AbstractAttributeTagProcessor {
   private final String attributeName;

   HxGenericProcessor(String dialectPrefix, String attributeName) {
      super(TemplateMode.HTML, dialectPrefix, null, false, attributeName, true, 1000, true);
      this.attributeName = attributeName;
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attrName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      String value = this.resolveValue(ctx, attributeValue);
      KaDataProcessor.setEscaped(tag, structureHandler, "hx-" + this.attributeName, value);
   }

   private String resolveValue(ITemplateContext ctx, String value) {
      if (value.contains("${")) {
         MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
         Object result = kec.evalLocal("`" + value + "`");
         return result != null ? result.toString() : "";
      } else {
         return value;
      }
   }
}
