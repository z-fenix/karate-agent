package io.karatelabs.markup;

import io.karatelabs.common.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaScriptSrcProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaScriptSrcProcessor.class);
   private final String hostContextPath;
   private final ResourceResolver resolver;
   private final boolean serverMode;
   private final String urlAttr;

   KaScriptSrcProcessor(String dialectPrefix, ResourceResolver resolver, String hostContextPath, boolean serverMode, String urlAttr) {
      super(TemplateMode.HTML, dialectPrefix, null, false, urlAttr, false, 1000, false);
      this.resolver = resolver;
      this.hostContextPath = hostContextPath;
      this.serverMode = serverMode;
      this.urlAttr = urlAttr;
   }

   protected void doProcess(ITemplateContext ctx, IProcessableElementTag tag, AttributeName an, String urlValue, IElementTagStructureHandler sh) {
      String scope = tag.getAttributeValue(this.getDialectPrefix(), "scope");
      String noCache = tag.getAttributeValue(this.getDialectPrefix(), "nocache");
      if (!this.serverMode && scope == null) {
         if (noCache != null) {
            sh.removeAttribute(this.getDialectPrefix(), "nocache");
         }
      } else if (scope == null && noCache == null) {
         if (this.hostContextPath != null) {
            urlValue = this.hostContextPath + urlValue;
            sh.setAttribute(this.urlAttr, urlValue);
         }
      } else {
         Resource srcResource = this.resolver.resolve(urlValue, null);
         if (scope == null) {
            if (this.hostContextPath != null) {
               urlValue = this.hostContextPath + urlValue;
            }

            if (noCache != null) {
               try {
                  urlValue = urlValue + "?ts=" + srcResource.getLastModified();
               } catch (Exception var11) {
                  logger.warn("nocache failed: {}", var11.getMessage());
               }

               sh.removeAttribute(this.getDialectPrefix(), "nocache");
            }

            sh.setAttribute(this.urlAttr, urlValue);
         } else {
            String js = srcResource.getText();
            MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
            if ("local".equals(scope)) {
               kec.evalLocal(js);
            } else {
               kec.evalGlobal(js);
            }

            sh.removeElement();
         }
      }
   }
}
