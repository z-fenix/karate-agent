package io.karatelabs.markup;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import org.thymeleaf.engine.TemplateData;

class SimpleMarkupContext implements MarkupContext {
   final MarkupTemplateContext markup;
   final ResourceResolver resolver;
   private MarkupScope markupScope;

   SimpleMarkupContext(MarkupTemplateContext markup, ResourceResolver resolver) {
      this.markup = markup;
      this.resolver = resolver;
   }

   @Override
   public void setMarkupScope(MarkupScope scope) {
      this.markupScope = scope;
   }

   @Override
   public MarkupScope getMarkupScope() {
      return this.markupScope;
   }

   @Override
   public String getTemplateName() {
      String name = this.markup.wrapped.getTemplateData().getTemplate();
      return name.startsWith("/") ? name.substring(1) : name;
   }

   @Override
   public String getCallerTemplateName() {
      TemplateData td = this.markup.wrapped.getTemplateData();
      return td.getTemplateResource() instanceof HtmlTemplateResource ktr ? ktr.getCaller() : null;
   }

   @Override
   public String read(String path) {
      Resource resource = this.resolver.resolve(path, null);
      return resource.getText();
   }

   @Override
   public byte[] readBytes(String path) {
      Resource resource = this.resolver.resolve(path, null);
      return FileUtils.toBytes(resource.getStream());
   }

   @Override
   public String toJson(Object obj) {
      return Json.stringifyStrict(obj);
   }

   @Override
   public Object fromJson(String json) {
      return Json.of(json).value();
   }
}
