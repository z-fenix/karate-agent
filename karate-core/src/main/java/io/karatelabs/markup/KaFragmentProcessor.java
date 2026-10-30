package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

final class KaFragmentProcessor extends AbstractElementTagProcessor {
   KaFragmentProcessor(TemplateMode templateMode, String dialectPrefix) {
      super(templateMode, dialectPrefix, null, false, "fragment", true, 1500);
   }

   protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
      AttributeName attributeName = this.getMatchingAttributeName().getMatchingAttributeName();
      String value = tag.getAttributeValue(attributeName);
      if (value != null && value.indexOf(40) >= 0) {
         throw new TemplateProcessingException(FragmentSupport.signatureMessage(value, null, null));
      } else {
         structureHandler.removeAttribute(attributeName);
      }
   }
}
