package io.karatelabs.markup;

import io.karatelabs.common.ResourceNotFoundException;
import io.karatelabs.common.StringUtils;
import io.karatelabs.js.Engine;
import io.karatelabs.js.FlowControlSignal;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.StandardEngineContextFactory;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.exceptions.TemplateOutputException;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.util.FastStringWriter;

public class Markup {
   private static final Logger logger = LoggerFactory.getLogger(Markup.class);
   private static final Map<String, Object> IS_STRING = Collections.singletonMap("isString", true);
   private final StandardEngineContextFactory standardFactory = new StandardEngineContextFactory();
   private final TemplateEngine wrapped = new TemplateEngine();
   private HtmlTemplateResolver templateResolver;
   private static final int CONTEXT_LINES = 2;

   Markup(MarkupConfig config, IDialect... dialects) {
      this.wrapped.setEngineContextFactory((ec, data, attrs, context) -> {
         IEngineContext engineContext = this.standardFactory.createEngineContext(ec, data, attrs, context);
         return new MarkupTemplateContext(engineContext, config);
      });
      this.wrapped.setDialect(new MarkupStandardDialect(config));

      for (IDialect dialect : dialects) {
         this.wrapped.addDialect(dialect);
      }
   }

   public String processPath(String path, Map<String, Object> vars) {
      return this.process(true, path, vars);
   }

   public String processString(String input, Map<String, Object> vars) {
      return this.process(false, input, vars);
   }

   private String process(boolean isPath, String content, Map<String, Object> vars) {
      return this.process(isPath, content, new Markup.MarkupEngineContext(vars));
   }

   private String process(boolean isPath, String content, IContext context) {
      Writer stringWriter = new FastStringWriter(100);
      this.process(isPath, content, context, stringWriter);
      return stringWriter.toString();
   }

   private void process(boolean isPath, String content, IContext context, Writer writer) {
      try {
         if (this.templateResolver != null) {
            this.templateResolver.resetStringTemplateState();
         }

         TemplateSpec templateSpec = new TemplateSpec(content, isPath ? Collections.emptyMap() : IS_STRING);
         TemplateManager templateManager = this.wrapped.getConfiguration().getTemplateManager();
         templateManager.parseAndProcess(templateSpec, context, writer);

         try {
            writer.flush();
         } catch (IOException var8) {
            throw new TemplateOutputException("error flushing output writer", content, -1, -1, var8);
         }
      } catch (ResourceNotFoundException var9) {
         throw var9;
      } catch (Exception var10) {
         if (hasFlowControlSignal(var10)) {
            throw new RuntimeException(var10);
         } else {
            String formatted = this.logTemplateError(isPath, content, var10);
            throw new RuntimeException(formatted, var10);
         }
      }
   }

   private static boolean hasFlowControlSignal(Throwable e) {
      for (Throwable t = e; t != null; t = t.getCause()) {
         if (t instanceof FlowControlSignal) {
            return true;
         }
      }

      return false;
   }

   private String logTemplateError(boolean isPath, String template, Exception e) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n========== TEMPLATE ERROR ==========\n");
      Throwable current = e;
      String templateName = null;
      int line = -1;

      int col;
      for (col = -1; current != null; current = current.getCause()) {
         if (current instanceof TemplateProcessingException tpe) {
            if (tpe.getTemplateName() != null) {
               templateName = tpe.getTemplateName();
            }

            if (tpe.getLine() != null) {
               line = tpe.getLine();
            }

            if (tpe.getCol() != null) {
               col = tpe.getCol();
            }
         }
      }

      if (isPath) {
         sb.append("Resource: ").append(template).append("\n");
      } else if (templateName != null && !templateName.equals(template)) {
         sb.append("File: ").append(templateName).append("\n");
      }

      if (line > 0) {
         sb.append("Line: ").append(line);
         if (col > 0) {
            sb.append(", Col: ").append(col);
         }

         sb.append("\n");
         this.appendContextLines(sb, template, line);
      } else if (template.length() > 200) {
         sb.append("Template: ").append(template.substring(0, 200)).append("...\n");
      } else {
         sb.append("Template: ").append(template).append("\n");
      }

      Throwable rootCause = e;

      while (rootCause.getCause() != null) {
         rootCause = rootCause.getCause();
      }

      String displayMessage = rootCause.getMessage();

      for (Throwable scan = e; scan != null; scan = scan.getCause()) {
         if (scan instanceof MarkupHintException) {
            displayMessage = scan.getMessage();
            break;
         }
      }

      sb.append("Error: ").append(displayMessage).append("\n");
      sb.append("====================================\n");
      String formatted = sb.toString();
      logger.error(formatted);
      if (logger.isTraceEnabled()) {
         logger.trace("Full stack trace: {}", StringUtils.throwableToString(e));
      }

      return formatted;
   }

   private void appendContextLines(StringBuilder sb, String template, int errorLine) {
      String[] lines = template.split("\n");
      int start = Math.max(0, errorLine - 2 - 1);
      int end = Math.min(lines.length, errorLine + 2);
      int lineNumWidth = String.valueOf(end).length();

      for (int i = start; i < end; i++) {
         int lineNum = i + 1;
         String marker = lineNum == errorLine ? ">" : " ";
         sb.append(String.format("%s %" + lineNumWidth + "d | %s%n", marker, lineNum, lines[i]));
      }
   }

   public static Markup init(Engine engine, String root) {
      return init(engine, new RootResourceResolver(root));
   }

   public static Markup init(Engine engine, ResourceResolver resolver) {
      MarkupConfig config = new MarkupConfig();
      config.setResolver(resolver);
      config.setEngineSupplier(() -> engine);
      return init(config);
   }

   public static Markup init(MarkupConfig config, IDialect... additionalDialects) {
      IDialect[] dialects = new IDialect[1 + additionalDialects.length];
      dialects[0] = new KaDialect(config);
      System.arraycopy(additionalDialects, 0, dialects, 1, additionalDialects.length);
      Markup markup = new Markup(config, dialects);
      markup.templateResolver = new HtmlTemplateResolver(config);
      markup.wrapped.setTemplateResolver(markup.templateResolver);
      return markup;
   }

   static class MarkupEngineContext implements IContext {
      private final Map<String, Object> vars;

      public MarkupEngineContext(Map<String, Object> vars) {
         if (vars == null) {
            vars = Collections.emptyMap();
         }

         this.vars = vars;
      }

      public Locale getLocale() {
         return Locale.US;
      }

      public boolean containsVariable(String name) {
         return this.vars.containsKey(name);
      }

      public Set<String> getVariableNames() {
         return this.vars.keySet();
      }

      public Object getVariable(String name) {
         return this.vars.get(name);
      }
   }
}
