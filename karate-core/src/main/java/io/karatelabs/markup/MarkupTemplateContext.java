package io.karatelabs.markup;

import io.karatelabs.js.Engine;
import io.karatelabs.js.EngineException;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.parser.ParserException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.IdentifierSequences;
import org.thymeleaf.engine.IterationStatusVar;
import org.thymeleaf.engine.TemplateData;
import org.thymeleaf.expression.IExpressionObjects;
import org.thymeleaf.inline.IInliner;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.templatemode.TemplateMode;

public class MarkupTemplateContext implements IEngineContext, MarkupScope {
   final IEngineContext wrapped;
   private final Engine engine;
   private final Map<String, Object> vars = new HashMap<>();
   private final ObjectLike underscoreView = new ObjectLike() {
      @Override
      public Object getMember(String name) {
         if (MarkupTemplateContext.this.vars.containsKey(name)) {
            return MarkupTemplateContext.this.vars.get(name);
         } else {
            return MarkupTemplateContext.this.wrapped.containsVariable(name) ? MarkupTemplateContext.this.getVariable(name) : null;
         }
      }

      @Override
      public void putMember(String name, Object value) {
         MarkupTemplateContext.this.vars.put(name, value);
      }

      @Override
      public void removeMember(String name) {
         MarkupTemplateContext.this.vars.remove(name);
      }

      @Override
      public Map<String, Object> toMap() {
         return new LinkedHashMap<>(MarkupTemplateContext.this.vars);
      }

      @Override
      public boolean isOwnProperty(String name) {
         return MarkupTemplateContext.this.vars.containsKey(name) || MarkupTemplateContext.this.wrapped.containsVariable(name);
      }
   };
   private static final String DISPATCH_JS = "(function() {  if (typeof request === 'undefined' || !request.post) return false;  var name = request.param('action');  if (!name) return false;  var fn = context.actions[name];  if (typeof fn !== 'function') return false;  try { fn(request.paramJson('form')); }  catch (e) { context.flash.error = (e && e.message) || (e + ''); }  return true;})()";
   private static final Pattern UNQUOTED_HYPHEN_COLON_KEY = Pattern.compile("(^|,)\\s*([a-zA-Z_$][\\w$]*(?:[-:][\\w$]+)+)\\s*:");
   private static final Pattern NOT_DEFINED_PATTERN = Pattern.compile("([A-Za-z_$][\\w$]*) is not defined");

   MarkupTemplateContext(IEngineContext wrapped, MarkupConfig config) {
      this.wrapped = wrapped;
      this.engine = config.getEngineSupplier().get();
      this.engine.put("_", this.underscoreView);
      MarkupContext markupContext;
      if (wrapped.getVariable("context") instanceof MarkupContext mc) {
         markupContext = mc;
      } else {
         markupContext = new SimpleMarkupContext(this, config.getResolver());
      }

      this.engine.put("context", markupContext);
      markupContext.setMarkupScope(this);
      if (markupContext instanceof ActionDispatchHost host) {
         host.setEagerDispatchHook(name -> this.maybeDispatchAction());
      }
   }

   @Override
   public void set(String name, Object value) {
      this.vars.put(name, value);
   }

   @Override
   public Object lookup(String name) {
      if (this.vars.containsKey(name)) {
         Object v = this.vars.get(name);
         if (v != null) {
            return v;
         }
      }

      if (this.wrapped.containsVariable(name)) {
         Object v = this.getVariable(name);
         if (v != null) {
            return v;
         }
      }

      return null;
   }

   void evalGlobal(String src) {
      this.getVariableNames().forEach(name -> this.engine.put(name, this.getVariable(name)));
      this.engine.eval(src);
   }

   void maybeDispatchAction() {
      if (this.wrapped.getVariable("context") instanceof ActionDispatchHost host) {
         if (!host.isActionDispatched()) {
            if (host.getActions() != null && !host.getActions().isEmpty()) {
               Object dispatched = this.engine
                  .eval(
                     "(function() {  if (typeof request === 'undefined' || !request.post) return false;  var name = request.param('action');  if (!name) return false;  var fn = context.actions[name];  if (typeof fn !== 'function') return false;  try { fn(request.paramJson('form')); }  catch (e) { context.flash.error = (e && e.message) || (e + ''); }  return true;})()"
                  );
               if (Boolean.TRUE.equals(dispatched)) {
                  host.markActionDispatched();
               }
            }
         }
      }
   }

   public Object evalLocalAsObject(String src) {
      String temp;
      if (src.startsWith("${")) {
         temp = "`" + src + "`";
      } else {
         temp = "({" + src + "})";
      }

      try {
         return this.evalLocal(temp);
      } catch (ParserException var5) {
         String hint = buildAttrKeyHint(src);
         if (hint != null) {
            throw new MarkupHintException(hint, var5);
         } else {
            throw var5;
         }
      }
   }

   private static String buildAttrKeyHint(String src) {
      Matcher m = UNQUOTED_HYPHEN_COLON_KEY.matcher(src);
      LinkedHashSet<String> badKeys = new LinkedHashSet<>();

      while (m.find()) {
         badKeys.add(m.group(2));
      }

      if (badKeys.isEmpty()) {
         return null;
      } else {
         String corrected = src;

         for (String key : badKeys) {
            corrected = corrected.replaceAll("(^|,)(\\s*)" + Pattern.quote(key) + "(\\s*):", "$1$2'" + Matcher.quoteReplacement(key) + "'$3:");
         }

         StringBuilder hint = new StringBuilder();
         hint.append("SyntaxError parsing object-literal expression — attribute key");
         hint.append(badKeys.size() > 1 ? "s " : " ");
         boolean first = true;

         for (String k : badKeys) {
            if (!first) {
               hint.append(", ");
            }

            hint.append("`").append(k).append("`");
            first = false;
         }

         hint.append(badKeys.size() > 1 ? " contain" : " contains");
         hint.append(" hyphens or colons and must be quoted. Try: ").append(corrected);
         return hint.toString();
      }
   }

   public Object evalLocal(String src) {
      Map<String, Object> localVars = new HashMap<>();

      for (String name : this.getVariableNames()) {
         localVars.put(name, this.getVariable(name));
      }

      localVars.put("_", this.underscoreView);

      try {
         return this.engine.evalWith(src, localVars);
      } catch (EngineException var5) {
         String missing = extractMissingName(var5);
         if (missing == null) {
            throw var5;
         } else {
            throw new MarkupHintException(this.buildMissingNameHint(missing, var5), var5);
         }
      }
   }

   private String buildMissingNameHint(String missing, Throwable cause) {
      String base = "ReferenceError: '" + missing + "' is not defined";
      return this.vars.containsKey(missing)
         ? base
            + " — did you mean `_."
            + missing
            + "`? ka:scope blocks namespace template state via the `_` map; bare names must come from a th:with or a parent template binding."
         : base
            + " — if `"
            + missing
            + "` is a fragment parameter, declare it via th:with at the call site (e.g. <div th:insert=\"~{file::frag}\" th:with=\""
            + missing
            + ": value\">). If it's optional, read it inside the fragment via context.get('"
            + missing
            + "') or context.get('"
            + missing
            + "', defaultValue) — returns null (or the default) when the name is unbound.";
   }

   private static String extractMissingName(EngineException e) {
      for (Throwable current = e; current != null; current = current.getCause()) {
         if (current instanceof EngineException ee && "ReferenceError".equals(ee.getJsErrorName())) {
            String jsMsg = ee.getJsMessage();
            if (jsMsg != null) {
               Matcher m = NOT_DEFINED_PATTERN.matcher(jsMsg);
               if (m.find()) {
                  return m.group(1);
               }
            }
         }
      }

      return null;
   }

   void setLocal(String name, Object value) {
      this.vars.put(name, value);
   }

   public void increaseLevel() {
      if (!this.vars.isEmpty()) {
         this.wrapped.setVariables(this.vars);
         this.vars.clear();
      }

      this.wrapped.increaseLevel();
   }

   public void setVariable(String name, Object value) {
      this.wrapped.setVariable(name, value);
   }

   public void setVariables(Map<String, Object> variables) {
      this.wrapped.setVariables(variables);
   }

   public void removeVariable(String name) {
      this.wrapped.removeVariable(name);
   }

   public void setTemplateData(TemplateData template) {
      this.wrapped.setTemplateData(template);
   }

   public void decreaseLevel() {
      this.wrapped.decreaseLevel();
   }

   public boolean containsVariable(String name) {
      return this.wrapped.containsVariable(name);
   }

   public Set<String> getVariableNames() {
      return this.wrapped.getVariableNames();
   }

   public Object getVariable(String name) {
      Object value = this.wrapped.getVariable(name);
      if (value instanceof IterationStatusVar status) {
         Map<String, Object> statusMap = new LinkedHashMap<>();
         statusMap.put("index", status.getIndex());
         statusMap.put("count", status.getCount());
         statusMap.put("size", status.getSize());
         statusMap.put("current", status.getCurrent());
         statusMap.put("even", status.isEven());
         statusMap.put("odd", status.isOdd());
         statusMap.put("first", status.isFirst());
         statusMap.put("last", status.isLast());
         return statusMap;
      } else {
         return value;
      }
   }

   public boolean isVariableLocal(String name) {
      return this.wrapped.isVariableLocal(name);
   }

   public void setSelectionTarget(Object selectionTarget) {
      this.wrapped.setSelectionTarget(selectionTarget);
   }

   public void setInliner(IInliner inliner) {
      this.wrapped.setInliner(inliner);
   }

   public void setElementTag(IProcessableElementTag elementTag) {
      this.wrapped.setElementTag(elementTag);
   }

   public List<IProcessableElementTag> getElementStackAbove(int contextLevel) {
      return this.wrapped.getElementStackAbove(contextLevel);
   }

   public int level() {
      return this.wrapped.level();
   }

   public TemplateData getTemplateData() {
      return this.wrapped.getTemplateData();
   }

   public TemplateMode getTemplateMode() {
      return this.wrapped.getTemplateMode();
   }

   public List<TemplateData> getTemplateStack() {
      return this.wrapped.getTemplateStack();
   }

   public List<IProcessableElementTag> getElementStack() {
      return this.wrapped.getElementStack();
   }

   public Map<String, Object> getTemplateResolutionAttributes() {
      return this.wrapped.getTemplateResolutionAttributes();
   }

   public IModelFactory getModelFactory() {
      return this.wrapped.getModelFactory();
   }

   public boolean hasSelectionTarget() {
      return this.wrapped.hasSelectionTarget();
   }

   public Object getSelectionTarget() {
      return this.wrapped.getSelectionTarget();
   }

   public IInliner getInliner() {
      return this.wrapped.getInliner();
   }

   public String getMessage(Class<?> origin, String key, Object[] messageParameters, boolean useAbsent) {
      return this.wrapped.getMessage(origin, key, messageParameters, useAbsent);
   }

   public String buildLink(String base, Map<String, Object> parameters) {
      return this.wrapped.buildLink(base, parameters);
   }

   public IdentifierSequences getIdentifierSequences() {
      return this.wrapped.getIdentifierSequences();
   }

   public IEngineConfiguration getConfiguration() {
      return this.wrapped.getConfiguration();
   }

   public IExpressionObjects getExpressionObjects() {
      return this.wrapped.getExpressionObjects();
   }

   public Locale getLocale() {
      return this.wrapped.getLocale();
   }
}
