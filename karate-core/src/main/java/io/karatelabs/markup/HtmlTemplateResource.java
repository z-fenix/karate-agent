package io.karatelabs.markup;

import io.karatelabs.common.Resource;
import java.io.InputStreamReader;
import java.io.Reader;
import org.thymeleaf.templateresource.ITemplateResource;

class HtmlTemplateResource implements ITemplateResource {
   private final String caller;
   private final Resource resource;

   HtmlTemplateResource(String caller, Resource resource) {
      this.caller = caller;
      this.resource = resource;
   }

   public String getDescription() {
      return this.resource.getPrefixedPath();
   }

   public String getBaseName() {
      return this.resource.getFileNameWithoutExtension();
   }

   public String getCaller() {
      return this.caller;
   }

   public boolean exists() {
      return true;
   }

   public Reader reader() {
      return new InputStreamReader(this.resource.getStream());
   }

   public ITemplateResource relative(String relativeLocation) {
      return new HtmlTemplateResource(relativeLocation, this.resource.resolve(relativeLocation));
   }
}
