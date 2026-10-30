package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.AbstractStandardFragmentInsertionTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

class KaReplaceProcessor extends AbstractStandardFragmentInsertionTagProcessor {
   private static final int PRECEDENCE = 100;
   private static final String ATTR_NAME = "replace";
   private final MarkupConfig config;

   public KaReplaceProcessor(TemplateMode templateMode, String dialectPrefix, MarkupConfig config) {
      super(templateMode, dialectPrefix, "replace", 100, true);
      this.config = config;
   }

   protected void doProcess(
      ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      String fragmentExpr = attributeValue.startsWith("~{") ? attributeValue : "~{" + attributeValue + "}";
      IElementTagStructureHandler handler = structureHandler;
      if (FragmentTrace.enabled(this.config)) {
         String payload = FragmentTrace.buildPayload("replace", attributeValue, tag);
         handler = new TraceWrappingHandler(structureHandler, context, payload);
      }

      try {
         super.doProcess(context, tag, attributeName, fragmentExpr, handler);
      } catch (TemplateProcessingException var9) {
         FragmentSupport.translateSignatureError(var9);
         throw var9;
      }
   }
}
