package io.karatelabs.markup;

import java.io.Reader;
import java.io.StringReader;
import org.thymeleaf.templateresource.ITemplateResource;

class HtmlStringTemplateResource implements ITemplateResource {
   private final ResourceResolver resolver;
   private final String text;

   HtmlStringTemplateResource(String text, ResourceResolver resolver) {
      this.text = text;
      this.resolver = resolver;
   }

   public String getDescription() {
      return "(inline)";
   }

   public String getBaseName() {
      return null;
   }

   public boolean exists() {
      return false;
   }

   public Reader reader() {
      return new StringReader(this.text);
   }

   public ITemplateResource relative(String relativeLocation) {
      return new HtmlTemplateResource(null, this.resolver.resolve(relativeLocation, null));
   }
}
