package io.karatelabs.markup;

import java.util.HashSet;
import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

public class HxDialect extends AbstractProcessorDialect {
   private final String contextPath;

   public HxDialect() {
      this((String)null);
   }

   public HxDialect(MarkupConfig config) {
      this(config != null ? config.getContextPath() : null);
   }

   public HxDialect(String contextPath) {
      super("Htmx", "ka", 3000);
      this.contextPath = contextPath;
   }

   public String getContextPath() {
      return this.contextPath;
   }

   public Set<IProcessor> getProcessors(String dialectPrefix) {
      Set<IProcessor> processors = new HashSet<>();
      processors.add(new HxMethodProcessor(dialectPrefix, "get", this.contextPath));
      processors.add(new HxMethodProcessor(dialectPrefix, "post", this.contextPath));
      processors.add(new HxMethodProcessor(dialectPrefix, "put", this.contextPath));
      processors.add(new HxMethodProcessor(dialectPrefix, "patch", this.contextPath));
      processors.add(new HxMethodProcessor(dialectPrefix, "delete", this.contextPath));
      processors.add(new HxValsProcessor(dialectPrefix));
      String[] genericAttributes = new String[]{
         "target",
         "swap",
         "trigger",
         "push-url",
         "select",
         "confirm",
         "indicator",
         "boost",
         "headers",
         "include",
         "sync",
         "disabled-elt",
         "encoding",
         "ext",
         "history",
         "history-elt",
         "preserve",
         "prompt",
         "replace-url",
         "request",
         "validate"
      };

      for (String attr : genericAttributes) {
         processors.add(new HxGenericProcessor(dialectPrefix, attr));
      }

      processors.add(new KaDataProcessor(dialectPrefix));
      return processors;
   }
}
