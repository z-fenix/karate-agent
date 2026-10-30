package io.karatelabs.markup;

import io.karatelabs.common.Json;
import java.util.LinkedHashMap;
import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.inline.NoOpInliner;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaIslandProcessor extends AbstractElementModelProcessor {
   private static final String ISLAND = "island";
   private static final int PRECEDENCE = 1000;

   KaIslandProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "island", true, 1000);
   }

   protected void doProcess(ITemplateContext ctx, IModel model, IElementModelStructureHandler sh) {
      if (model.size() != 0) {
         IProcessableElementTag openTag = (IProcessableElementTag)model.get(0);
         String attrValue = openTag.getAttributeValue(this.getDialectPrefix(), "island");
         if (attrValue != null && !attrValue.isEmpty()) {
            String expr = attrValue.trim();
            String customId = null;
            int lastColon = expr.lastIndexOf(58);
            if (lastColon > 0) {
               String idCandidate = expr.substring(lastColon + 1).trim();
               if (!idCandidate.isEmpty() && idCandidate.matches("[A-Za-z0-9_-]+")) {
                  customId = idCandidate;
                  expr = expr.substring(0, lastColon).trim();
               }
            }

            String scriptId = customId != null ? customId : expr + "-data";
            MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
            Object value = kec.evalLocal(expr);
            String jsonContent = value != null ? Json.stringifyStrict(value) : "null";
            IModelFactory f = ctx.getModelFactory();
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("type", "application/json");
            attrs.put("id", scriptId);
            IOpenElementTag scriptOpen = f.createOpenElementTag("script", attrs, AttributeValueQuotes.DOUBLE, false);
            IText body = f.createText(jsonContent);
            ICloseElementTag scriptClose = f.createCloseElementTag("script");
            model.reset();
            model.add(scriptOpen);
            model.add(body);
            model.add(scriptClose);
            sh.setInliner(NoOpInliner.INSTANCE);
         }
      }
   }
}
