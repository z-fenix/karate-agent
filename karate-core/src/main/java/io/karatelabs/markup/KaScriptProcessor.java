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

class KaScriptProcessor extends AbstractElementModelProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaScriptProcessor.class);
   static final String SRC = "src";
   static final String SCOPE = "scope";
   static final String LOCAL = "local";
   static final String NOCACHE = "nocache";

   KaScriptProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, "script", false, "scope", true, 1000);
   }

   protected void doProcess(ITemplateContext ctx, IModel model, IElementModelStructureHandler sh) {
      int depth = ctx.getElementStack().size();
      IProcessableElementTag tag = (IProcessableElementTag)ctx.getElementStack().get(depth - 1);
      String prefix = this.getDialectPrefix();
      String scope = tag.getAttributeValue(prefix, "scope");
      String src = tag.getAttributeValue(null, "src");
      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      if (src == null) {
         int n = model.size();

         while (n-- != 0) {
            ITemplateEvent event = model.get(n);
            if (event instanceof IText) {
               String text = ((IText)event).getText();
               if (text != null && !text.isBlank()) {
                  if ("local".equals(scope)) {
                     kec.evalLocal(text);
                  } else {
                     kec.evalGlobal(text);
                     kec.maybeDispatchAction();
                  }
               }
            }
         }

         model.reset();
      }
   }
}
