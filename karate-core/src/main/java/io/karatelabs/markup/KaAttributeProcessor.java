package io.karatelabs.markup;

import io.karatelabs.js.Terms;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.StandardConditionalFixedValueTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.ArrayUtils;
import org.thymeleaf.util.EscapedAttributeUtils;

abstract class KaAttributeProcessor extends AbstractAttributeTagProcessor {
   private static final Logger logger = LoggerFactory.getLogger(KaAttributeProcessor.class);
   private final KaAttributeProcessor.ModificationType modificationType;

   KaAttributeProcessor(
      TemplateMode templateMode, String dialectPrefix, String attrName, int precedence, KaAttributeProcessor.ModificationType modificationType
   ) {
      super(templateMode, dialectPrefix, null, false, attrName, true, precedence, true);
      this.modificationType = modificationType;
   }

   protected final void doProcess(
      ITemplateContext ctx, IProcessableElementTag tag, AttributeName attributeName, String av, IElementTagStructureHandler structureHandler
   ) {
      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      Object evalResult = kec.evalLocalAsObject(av);
      if (!(evalResult instanceof Map<?, ?> rawMap)) {
         logger.warn("value did not evaluate to json: {}", av);
      } else {
         Map<String, Object> map = (Map<String, Object>) rawMap;
         map.forEach(
            (k, v) -> {
               if (this.getTemplateMode() != TemplateMode.HTML
                  || this.modificationType != KaAttributeProcessor.ModificationType.SUBSTITUTION
                  || !ArrayUtils.contains(StandardConditionalFixedValueTagProcessor.ATTR_NAMES, k)) {
                  String newAttributeValue = EscapedAttributeUtils.escapeAttribute(this.getTemplateMode(), v == null ? null : v.toString());
                  if (newAttributeValue != null && !newAttributeValue.isEmpty()) {
                     if (this.modificationType != KaAttributeProcessor.ModificationType.SUBSTITUTION
                        && tag.hasAttribute(k)
                        && !tag.getAttributeValue(k).isEmpty()) {
                        String currentValue = tag.getAttributeValue(k);
                        if (this.modificationType == KaAttributeProcessor.ModificationType.APPEND) {
                           structureHandler.setAttribute(k, currentValue + newAttributeValue);
                        } else if (this.modificationType == KaAttributeProcessor.ModificationType.APPEND_WITH_SPACE) {
                           structureHandler.setAttribute(k, currentValue + " " + newAttributeValue);
                        } else if (this.modificationType == KaAttributeProcessor.ModificationType.PREPEND) {
                           structureHandler.setAttribute(k, newAttributeValue + currentValue);
                        } else {
                           structureHandler.setAttribute(k, newAttributeValue + " " + currentValue);
                        }
                     } else {
                        structureHandler.setAttribute(k, newAttributeValue);
                     }
                  } else if (this.modificationType == KaAttributeProcessor.ModificationType.SUBSTITUTION) {
                     structureHandler.removeAttribute(k);
                  }
               } else if (Terms.isTruthy(v)) {
                  structureHandler.setAttribute(k, k);
               } else {
                  structureHandler.removeAttribute(k);
               }
            }
         );
      }
   }

   static class KarateAttrTagProcessor extends KaAttributeProcessor {
      public KarateAttrTagProcessor(TemplateMode templateMode, String dialectPrefix) {
         super(templateMode, dialectPrefix, "attr", 700, KaAttributeProcessor.ModificationType.SUBSTITUTION);
      }
   }

   static class KarateAttrappendTagProcessor extends KaAttributeProcessor {
      public KarateAttrappendTagProcessor(TemplateMode templateMode, String dialectPrefix) {
         super(templateMode, dialectPrefix, "attrappend", 900, KaAttributeProcessor.ModificationType.APPEND);
      }
   }

   static class KarateAttrprependTagProcessor extends KaAttributeProcessor {
      public KarateAttrprependTagProcessor(TemplateMode templateMode, String dialectPrefix) {
         super(templateMode, dialectPrefix, "attrprepend", 900, KaAttributeProcessor.ModificationType.PREPEND);
      }
   }

   static enum ModificationType {
      SUBSTITUTION,
      APPEND,
      PREPEND,
      APPEND_WITH_SPACE,
      PREPEND_WITH_SPACE;
   }
}
