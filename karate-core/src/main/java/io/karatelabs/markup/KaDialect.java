package io.karatelabs.markup;

import java.util.HashSet;
import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

class KaDialect extends AbstractProcessorDialect {
   private final ResourceResolver resolver;
   private final String contextPath;
   private final boolean serverMode;

   KaDialect(MarkupConfig config) {
      super("karate", "ka", 2000);
      this.resolver = config.getResolver();
      this.contextPath = config.getContextPath();
      this.serverMode = config.isServerMode();
   }

   public Set<IProcessor> getProcessors(String dialectPrefix) {
      Set<IProcessor> ps = new HashSet<>();
      ps.add(new KaScriptSrcProcessor(dialectPrefix, this.resolver, this.contextPath, this.serverMode, "src"));
      ps.add(new KaScriptSrcProcessor(dialectPrefix, this.resolver, this.contextPath, this.serverMode, "href"));
      ps.add(new KaScriptProcessor(dialectPrefix));
      ps.add(new KaSetAttrProcessor(dialectPrefix));
      ps.add(new KaDispatchProcessor(dialectPrefix));
      ps.add(new KaDataMirrorProcessor(dialectPrefix));
      ps.add(new KaIslandProcessor(dialectPrefix));
      return ps;
   }
}
