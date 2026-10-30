package io.karatelabs.markup;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.TemplateData;
import org.thymeleaf.inline.IInliner;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementTagStructureHandler;

class TraceWrappingHandler implements IElementTagStructureHandler {
   private final IElementTagStructureHandler delegate;
   private final ITemplateContext context;
   private final String payload;

   TraceWrappingHandler(IElementTagStructureHandler delegate, ITemplateContext context, String payload) {
      this.delegate = delegate;
      this.context = context;
      this.payload = payload;
   }

   private IModel wrap(IModel inner) {
      IModelFactory f = this.context.getModelFactory();
      IModel out = f.createModel();
      out.add(f.createOpenElementTag("ka-trace", "data-ka-trace", this.payload));
      out.addModel(inner);
      out.add(f.createCloseElementTag("ka-trace"));
      return out;
   }

   public void setBody(IModel model, boolean processable) {
      this.delegate.setBody(this.wrap(model), processable);
   }

   public void replaceWith(IModel model, boolean processable) {
      this.delegate.replaceWith(this.wrap(model), processable);
   }

   public void reset() {
      this.delegate.reset();
   }

   public void setLocalVariable(String name, Object value) {
      this.delegate.setLocalVariable(name, value);
   }

   public void removeLocalVariable(String name) {
      this.delegate.removeLocalVariable(name);
   }

   public void setAttribute(String name, String value) {
      this.delegate.setAttribute(name, value);
   }

   public void setAttribute(String name, String value, AttributeValueQuotes quotes) {
      this.delegate.setAttribute(name, value, quotes);
   }

   public void replaceAttribute(AttributeName old, String name, String value) {
      this.delegate.replaceAttribute(old, name, value);
   }

   public void replaceAttribute(AttributeName old, String name, String value, AttributeValueQuotes quotes) {
      this.delegate.replaceAttribute(old, name, value, quotes);
   }

   public void removeAttribute(String name) {
      this.delegate.removeAttribute(name);
   }

   public void removeAttribute(String prefix, String name) {
      this.delegate.removeAttribute(prefix, name);
   }

   public void removeAttribute(AttributeName name) {
      this.delegate.removeAttribute(name);
   }

   public void setSelectionTarget(Object target) {
      this.delegate.setSelectionTarget(target);
   }

   public void setInliner(IInliner inliner) {
      this.delegate.setInliner(inliner);
   }

   public void setTemplateData(TemplateData data) {
      this.delegate.setTemplateData(data);
   }

   public void setBody(CharSequence text, boolean processable) {
      this.delegate.setBody(text, processable);
   }

   public void insertBefore(IModel model) {
      this.delegate.insertBefore(model);
   }

   public void insertImmediatelyAfter(IModel model, boolean processable) {
      this.delegate.insertImmediatelyAfter(model, processable);
   }

   public void replaceWith(CharSequence text, boolean processable) {
      this.delegate.replaceWith(text, processable);
   }

   public void removeElement() {
      this.delegate.removeElement();
   }

   public void removeTags() {
      this.delegate.removeTags();
   }

   public void removeBody() {
      this.delegate.removeBody();
   }

   public void removeAllButFirstChild() {
      this.delegate.removeAllButFirstChild();
   }

   public void iterateElement(String iterVar, String iterStatusVar, Object iteratedObject) {
      this.delegate.iterateElement(iterVar, iterStatusVar, iteratedObject);
   }
}
