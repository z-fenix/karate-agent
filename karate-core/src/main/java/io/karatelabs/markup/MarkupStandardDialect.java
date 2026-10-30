package io.karatelabs.markup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.IPostProcessorDialect;
import org.thymeleaf.postprocessor.IPostProcessor;
import org.thymeleaf.postprocessor.PostProcessor;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.IStandardVariableExpression;
import org.thymeleaf.standard.expression.IStandardVariableExpressionEvaluator;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;
import org.thymeleaf.standard.expression.StandardExpressionParser;
import org.thymeleaf.standard.processor.StandardAttrTagProcessor;
import org.thymeleaf.standard.processor.StandardAttrappendTagProcessor;
import org.thymeleaf.standard.processor.StandardAttrprependTagProcessor;
import org.thymeleaf.standard.processor.StandardEachTagProcessor;
import org.thymeleaf.standard.processor.StandardFragmentTagProcessor;
import org.thymeleaf.standard.processor.StandardInsertTagProcessor;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;
import org.thymeleaf.standard.processor.StandardWithTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

class MarkupStandardDialect extends StandardDialect implements IStandardVariableExpressionEvaluator, IStandardExpressionParser, IPostProcessorDialect {
   private static final Logger logger = LoggerFactory.getLogger(MarkupStandardDialect.class);
   private final StandardExpressionParser expressionParser = new StandardExpressionParser();
   private final MarkupConfig config;

   MarkupStandardDialect(MarkupConfig config) {
      this.config = config;
   }

   public IStandardVariableExpressionEvaluator getVariableExpressionEvaluator() {
      return this;
   }

   public IStandardExpressionParser getExpressionParser() {
      return this;
   }

   public Set<IProcessor> getProcessors(String dialectPrefix) {
      Set<IProcessor> processors = StandardDialect.createStandardProcessorsSet(dialectPrefix);
      Set<IProcessor> patched = new HashSet<>(processors.size());

      for (IProcessor p : processors) {
         if (p instanceof StandardEachTagProcessor) {
            p = new KaEachProcessor(p.getTemplateMode(), dialectPrefix);
         }

         if (p instanceof StandardWithTagProcessor) {
            p = new KaWithProcessor(p.getTemplateMode(), dialectPrefix);
         }

         if (p instanceof StandardAttrTagProcessor) {
            p = new KaAttributeProcessor.KarateAttrTagProcessor(p.getTemplateMode(), dialectPrefix);
         }

         if (p instanceof StandardAttrappendTagProcessor) {
            p = new KaAttributeProcessor.KarateAttrappendTagProcessor(p.getTemplateMode(), dialectPrefix);
         }

         if (p instanceof StandardAttrprependTagProcessor) {
            p = new KaAttributeProcessor.KarateAttrprependTagProcessor(p.getTemplateMode(), dialectPrefix);
         }

         if (p instanceof StandardInsertTagProcessor) {
            p = new KaInsertProcessor(p.getTemplateMode(), dialectPrefix, this.config);
         }

         if (p instanceof StandardReplaceTagProcessor) {
            p = new KaReplaceProcessor(p.getTemplateMode(), dialectPrefix, this.config);
         }

         if (p instanceof StandardFragmentTagProcessor) {
            p = new KaFragmentProcessor(p.getTemplateMode(), dialectPrefix);
         }

         patched.add(p);
      }

      return patched;
   }

   public Set<IPostProcessor> getPostProcessors() {
      return FragmentTrace.enabled(this.config)
         ? Collections.singleton(new PostProcessor(TemplateMode.HTML, FragmentTraceHandler.class, 1000))
         : Collections.emptySet();
   }

   public int getDialectPostProcessorPrecedence() {
      return 1000;
   }

   public Object evaluate(IExpressionContext ctx, IStandardVariableExpression ve, StandardExpressionExecutionContext ec) {
      MarkupExpression ke = new MarkupExpression(ve.getExpression());
      return ke.execute(ctx);
   }

   public IStandardExpression parseExpression(IExpressionContext context, String input) {
      return (IStandardExpression)(input.charAt(0) == '~' ? this.expressionParser.parseExpression(context, input) : new MarkupExpression(input));
   }
}
