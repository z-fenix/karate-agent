package io.karatelabs.markup;

import io.karatelabs.common.Json;
import java.util.HashMap;
import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

class KaDataProcessor extends AbstractElementModelProcessor {
   private static final String DATA = "data";

   KaDataProcessor(String dialectPrefix) {
      super(TemplateMode.HTML, dialectPrefix, null, false, "data", true, 900);
   }

   protected void doProcess(ITemplateContext ctx, IModel model, IElementModelStructureHandler sh) {
      if (model.size() != 0) {
         IProcessableElementTag openTag = (IProcessableElementTag)model.get(0);
         String elementName = openTag.getElementCompleteName().toLowerCase();
         String attrValue = openTag.getAttributeValue(this.getDialectPrefix(), "data");
         if (attrValue != null && !attrValue.isEmpty()) {
            int colonIndex = attrValue.indexOf(58);
            if (colonIndex <= 0) {
               throw new RuntimeException("ka:data requires format 'varName:expression', got: " + attrValue);
            } else {
               String varName = attrValue.substring(0, colonIndex).trim();
               String expression = attrValue.substring(colonIndex + 1).trim();
               MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
               Object initialData = kec.evalLocal(expression);
               String jsonData = initialData != null ? Json.stringifyStrict(initialData) : "{}";
               IModelFactory modelFactory = ctx.getModelFactory();
               Map<String, String> newAttrs = new HashMap<>();

               for (IAttribute attr : openTag.getAllAttributes()) {
                  String name = attr.getAttributeCompleteName();
                  if (!name.equals(this.getDialectPrefix() + ":data")) {
                     String value = attr.getValue();
                     newAttrs.put(name, value == null ? null : value.replace("'", "&#39;"));
                  }
               }

               String xDataValue = "{ " + varName + ": " + jsonData + " }";
               newAttrs.put("x-data", escapeSingleQuoted(xDataValue));
               IOpenElementTag newOpenTag = modelFactory.createOpenElementTag(openTag.getElementCompleteName(), newAttrs, AttributeValueQuotes.SINGLE, false);
               model.replace(0, newOpenTag);
               if ("form".equals(elementName)) {
                  Map<String, String> hiddenAttrs = new HashMap<>();
                  hiddenAttrs.put("type", "hidden");
                  hiddenAttrs.put("name", varName);
                  hiddenAttrs.put("x-bind:value", "JSON.stringify(" + varName + ")");
                  IStandaloneElementTag hiddenInput = modelFactory.createStandaloneElementTag("input", hiddenAttrs, null, false, true);
                  model.insert(1, hiddenInput);
               }
            }
         }
      }
   }

   static String escapeSingleQuoted(String s) {
      return escape(s, '\'');
   }

   static void setEscaped(IProcessableElementTag tag, IElementTagStructureHandler sh, String name, String value) {
      IAttribute existing = tag.getAttribute(name);
      AttributeValueQuotes quotes = existing != null && existing.getValueQuotes() == AttributeValueQuotes.SINGLE
         ? AttributeValueQuotes.SINGLE
         : AttributeValueQuotes.DOUBLE;
      sh.setAttribute(name, escape(value, (char)(quotes == AttributeValueQuotes.SINGLE ? '\'' : '"')), quotes);
   }

   private static String escape(String s, char delimiter) {
      StringBuilder sb = null;
      int i = 0;

      for (int n = s.length(); i < n; i++) {
         char c = s.charAt(i);
         String rep = c == '&' ? "&amp;" : (c == '<' ? "&lt;" : (c != delimiter ? null : (c == '\'' ? "&#39;" : "&quot;")));
         if (rep != null && sb == null) {
            sb = new StringBuilder(n + 16).append(s, 0, i);
         }

         if (sb != null) {
            if (rep != null) {
               sb.append(rep);
            } else {
               sb.append(c);
            }
         }
      }

      return sb == null ? s : sb.toString();
   }
}
