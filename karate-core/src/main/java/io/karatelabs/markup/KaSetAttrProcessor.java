package io.karatelabs.markup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaSetAttrProcessor extends AbstractElementModelProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaSetAttrProcessor.class);
   private static final String SET = "set";

   KaSetAttrProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "set", true, 1000);
   }

   protected void doProcess(ITemplateContext ctx, IModel model, IElementModelStructureHandler sh) {
      int depth = ctx.getElementStack().size();
      IProcessableElementTag tag = (IProcessableElementTag)ctx.getElementStack().get(depth - 1);
      String name = tag.getAttributeValue(this.getDialectPrefix(), "set");
      int n = model.size();
      StringBuilder sb = new StringBuilder();

      while (n-- != 0) {
         ITemplateEvent event = model.get(n);
         if (event instanceof IText) {
            sb.append(((IText)event).getText());
         }
      }

      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      kec.setLocal(name, sb.toString().trim());
      model.reset();
   }
}
