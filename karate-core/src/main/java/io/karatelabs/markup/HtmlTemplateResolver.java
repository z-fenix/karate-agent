package io.karatelabs.markup;

import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceNotFoundException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.AlwaysValidCacheEntryValidity;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;

class HtmlTemplateResolver implements ITemplateResolver {
   private static final Logger logger = LoggerFactory.getLogger(HtmlTemplateResolver.class);
   private final ResourceResolver resolver;
   private final boolean devMode;
   private boolean doneWithStringTemplate;
   private Resource prevCaller;

   HtmlTemplateResolver(MarkupConfig config) {
      this.resolver = config.getResolver();
      this.devMode = config.isDevMode();
   }

   void resetStringTemplateState() {
      this.doneWithStringTemplate = false;
   }

   public String getName() {
      return this.getClass().getName();
   }

   public Integer getOrder() {
      return 1;
   }

   public TemplateResolution resolveTemplate(IEngineConfiguration ec, String ownerTemplate, String content, Map<String, Object> attributes) {
      if (attributes != null && !attributes.isEmpty() && !this.doneWithStringTemplate) {
         this.doneWithStringTemplate = true;
         this.prevCaller = null;
         HtmlStringTemplateResource templateResource = new HtmlStringTemplateResource(content, this.resolver);
         return new TemplateResolution(templateResource, TemplateMode.HTML, NonCacheableCacheEntryValidity.INSTANCE);
      } else {
         if (!content.endsWith(".html")) {
            content = content + ".html";
         }

         Resource caller;
         if (ownerTemplate != null && !ownerTemplate.startsWith("this:") && this.prevCaller != null) {
            String ownerPath = ownerTemplate.endsWith(".html") ? ownerTemplate : ownerTemplate + ".html";
            caller = this.resolver.resolve(ownerPath, null);
         } else {
            caller = this.prevCaller;
         }

         Resource resource = this.resolver.resolve(content, caller);
         if (resource != null && resource.exists()) {
            this.prevCaller = resource;
            HtmlTemplateResource templateResource = new HtmlTemplateResource(ownerTemplate, resource);
            return new TemplateResolution(
               templateResource,
               TemplateMode.HTML,
               (ICacheEntryValidity)(this.devMode ? NonCacheableCacheEntryValidity.INSTANCE : AlwaysValidCacheEntryValidity.INSTANCE)
            );
         } else {
            throw new ResourceNotFoundException(content);
         }
      }
   }
}
