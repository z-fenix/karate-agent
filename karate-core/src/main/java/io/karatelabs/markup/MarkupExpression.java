package io.karatelabs.markup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;

class MarkupExpression implements IStandardExpression {
   private static final Logger logger = LoggerFactory.getLogger(MarkupExpression.class);
   private final String input;

   MarkupExpression(String input) {
      if (input.contains("${")) {
         this.input = "`" + input + "`";
      } else {
         this.input = input;
      }
   }

   public String getStringRepresentation() {
      return this.input;
   }

   public Object execute(IExpressionContext ctx) {
      MarkupTemplateContext kec = (MarkupTemplateContext)ctx;
      return kec.evalLocal(this.input);
   }

   public Object execute(IExpressionContext context, StandardExpressionExecutionContext expContext) {
      return this.execute(context);
   }
}
