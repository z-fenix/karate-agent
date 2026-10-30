package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class HxMethodProcessor extends AbstractAttributeTagProcessor {
   private final String method;
   private final String contextPath;

   HxMethodProcessor(String dialectPrefix, String method, String contextPath) {
      super(TemplateMode.HTML, dialectPrefix, null, false, method, true, 1000, true);
      this.method = method;
      this.contextPath = contextPath;
   }

   protected void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler
   ) {
      String url = this.resolveUrl(ctx, attributeValue);
      if (this.contextPath != null && !this.contextPath.isEmpty() && url.startsWith("/")) {
         url = this.contextPath + url;
      }

      KaDataProcessor.setEscaped(tag, structureHandler, "hx-" + this.method, url);
   }

   private String resolveUrl(ITemplateContext ctx, String value) {
      if ("this".equals(value)) {
         return this.getTemplatePath(ctx);
      } else if (value.contains("${")) {
         MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
         Object result = kec.evalLocal("`" + value + "`");
         return result != null ? result.toString() : "";
      } else {
         return value;
      }
   }

   private String getTemplatePath(ITemplateContext ctx) {
      String template = ctx.getTemplateData().getTemplate();
      if (template.endsWith(".html")) {
         template = template.substring(0, template.length() - 5);
      }

      if (!template.startsWith("/")) {
         template = "/" + template;
      }

      return template;
   }
}
