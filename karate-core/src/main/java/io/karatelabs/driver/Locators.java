package io.karatelabs.driver;

import io.karatelabs.common.Resource;
import io.karatelabs.js.JsFunction;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Locators {
   private static final String DOCUMENT = "document";
   private static final Set<String> XPATH_PREFIXES = Set.of("/", "./", "../", "(//");
   private static final Pattern WILDCARD_PATTERN = Pattern.compile("^\\{(\\^)?([^:}]*)?(?::(\\d+))?}(.*)$");
   public static final String SCROLL_JS_FUNCTION = "function(e){ if (!e || e.nodeType !== 1) return; var d = window.getComputedStyle(e).display; while (d == 'none' && e.parentElement) { e = e.parentElement; d = window.getComputedStyle(e).display } if (e && e.nodeType === 1) e.scrollIntoView({block: 'center'}) }";
   private static final Pattern FUNCTION_DEFINITION_FALLBACK = Pattern.compile("^\\s*(async\\s*)?(function\\b|\\([^()]*\\)\\s*=>|[A-Za-z_$][\\w$]*\\s*=>)");
   public static final String ELEMENT_NOT_FOUND = "KARATE_ELEMENT_NOT_FOUND";

   public static String selector(String locator) {
      return selector(locator, "document");
   }

   public static String selector(String locator, String contextNode) {
      if (locator == null || locator.isEmpty()) {
         throw new DriverException("locator cannot be null or empty");
      } else if (locator.startsWith("(") && !locator.startsWith("(//")) {
         return locator;
      } else if (locator.startsWith("{")) {
         return expandWildcard(locator);
      } else if (isXpath(locator)) {
         return xpathSelector(locator, contextNode);
      } else {
         String escaped = escapeForJs(locator);
         return "document".equals(contextNode)
            ? "(window.__kjs && window.__kjs.qsDeep ? window.__kjs.qsDeep(\"" + escaped + "\") : document.querySelector(\"" + escaped + "\"))"
            : contextNode + ".querySelector(\"" + escaped + "\")";
      }
   }

   public static boolean isXpath(String locator) {
      for (String prefix : XPATH_PREFIXES) {
         if (locator.startsWith(prefix)) {
            return true;
         }
      }

      return false;
   }

   public static String expandWildcard(String locator) {
      Matcher m = WILDCARD_PATTERN.matcher(locator);
      if (!m.matches()) {
         throw new DriverException("bad wildcard locator: " + locator);
      } else {
         boolean contains = m.group(1) != null;
         String tagPart = m.group(2);
         String indexPart = m.group(3);
         String text = m.group(4);
         String tag = tagPart != null && !tagPart.isEmpty() ? tagPart : "*";
         int index = indexPart != null ? Integer.parseInt(indexPart) : 1;
         String escapedTag = escapeForJs(tag);
         String escapedText = escapeForJs(text);
         return "window.__kjs.resolve(\"" + escapedTag + "\", \"" + escapedText + "\", " + index + ", " + contains + ")";
      }
   }

   private static String xpathSelector(String xpath, String contextNode) {
      if (xpath.startsWith("/(")) {
         if ("document".equals(contextNode)) {
            xpath = xpath.substring(1);
         } else {
            xpath = "(." + xpath.substring(2);
         }
      } else if (!"document".equals(contextNode) && !xpath.startsWith(".")) {
         xpath = "." + xpath;
      }

      String escapedXpath = escapeForJs(xpath);
      return "document.evaluate(\"" + escapedXpath + "\", " + contextNode + ", null, 9, null).singleNodeValue";
   }

   public static String selectorAll(String locator) {
      return selectorAll(locator, "document");
   }

   public static String selectorAll(String locator, String contextNode) {
      if (locator == null || locator.isEmpty()) {
         throw new DriverException("locator cannot be null or empty");
      } else if (locator.startsWith("{")) {
         String js = expandWildcard(locator);
         return "(function(){ var e = " + js + "; return e ? [e] : [] })()";
      } else if (isXpath(locator)) {
         if (!"document".equals(contextNode) && !locator.startsWith(".")) {
            locator = "." + locator;
         }

         String escapedXpath = escapeForJs(locator);
         return "document.evaluate(\"" + escapedXpath + "\", " + contextNode + ", null, 5, null)";
      } else {
         String escaped = escapeForJs(locator);
         return "document".equals(contextNode)
            ? "(window.__kjs && window.__kjs.qsaDeep ? window.__kjs.qsaDeep(\"" + escaped + "\") : document.querySelectorAll(\"" + escaped + "\"))"
            : contextNode + ".querySelectorAll(\"" + escaped + "\")";
      }
   }

   public static String wrapInFunctionInvoke(String js) {
      return "(function(){ " + js + " })()";
   }

   public static boolean isFunctionDefinition(String js) {
      Node program;
      try {
         program = new JsParser(Resource.text(js)).leadingFunctionExpression().parse();
      } catch (RuntimeException var3) {
         return looksLikeFunctionDefinition(js);
      }

      List<Node> statements = program.findImmediateChildren(NodeType.STATEMENT);
      return statements.size() == 1 && isFunctionNode(onlyChild(statements.get(0)));
   }

   private static boolean looksLikeFunctionDefinition(String js) {
      Matcher m = FUNCTION_DEFINITION_FALLBACK.matcher(js);
      return !m.find() ? false : !m.group(2).startsWith("function") || js.stripTrailing().endsWith("}");
   }

   private static boolean isFunctionNode(Node node) {
      if (node == null) {
         return false;
      } else {
         return switch (node.type) {
            case FN_EXPR, FN_ARROW_EXPR -> true;
            case REF_EXPR -> !node.findImmediateChildren(NodeType.FN_ARROW_EXPR).isEmpty();
            case EXPR_LIST, EXPR, PAREN_EXPR -> isFunctionNode(onlyChild(node));
            default -> false;
         };
      }
   }

   private static Node onlyChild(Node node) {
      Node only = null;

      for (int i = 0; i < node.size(); i++) {
         Node child = node.get(i);
         if (!child.isToken()) {
            if (only != null) {
               return null;
            }

            only = child;
         }
      }

      return only;
   }

   public static String toFunction(Object expression) {
      if (expression == null) {
         return "_ => _";
      } else {
         if (expression instanceof JsFunction fn) {
            String source = fn.getSource();
            if (source != null) {
               return source;
            }
         }

         String expr = expression.toString();
         if (expr.isEmpty()) {
            return "_ => _";
         } else if (isFunctionDefinition(expr)) {
            return expr;
         } else {
            char first = expr.charAt(0);
            return first != '_' && first != '!' ? expr : "_ => " + expr;
         }
      }
   }

   public static String scriptSelector(String locator, Object expression) {
      return scriptSelector(locator, expression, "document");
   }

   public static String scriptSelector(String locator, Object expression, String contextNode) {
      String js = "var fun = " + toFunction(expression) + "; var e = " + selector(locator, contextNode) + "; return fun(e)";
      return wrapInFunctionInvoke(js);
   }

   public static String scriptAllSelector(String locator, Object expression) {
      return scriptAllSelector(locator, expression, "document");
   }

   public static String scriptAllSelector(String locator, Object expression, String contextNode) {
      if (locator.startsWith("{")) {
         String resolverJs = expandWildcard(locator);
         String js = "var res = []; var fun = " + toFunction(expression) + "; var e = " + resolverJs + "; ";
         js = js + "if (e) res.push(fun(e)); return res";
         return wrapInFunctionInvoke(js);
      } else {
         boolean isXpathLocator = isXpath(locator);
         if (isXpathLocator && !"document".equals(contextNode) && !locator.startsWith(".")) {
            locator = "." + locator;
         }

         String escaped = escapeForJs(locator);
         String selectorExpr;
         if (isXpathLocator) {
            selectorExpr = "document.evaluate(\"" + escaped + "\", " + contextNode + ", null, 5, null)";
         } else if ("document".equals(contextNode)) {
            selectorExpr = "(window.__kjs && window.__kjs.qsaDeep ? window.__kjs.qsaDeep(\""
               + escaped
               + "\") : document.querySelectorAll(\""
               + escaped
               + "\"))";
         } else {
            selectorExpr = contextNode + ".querySelectorAll(\"" + escaped + "\")";
         }

         String js = "var res = []; var fun = " + toFunction(expression) + "; var es = " + selectorExpr + "; ";
         if (isXpathLocator) {
            js = js + "var e = null; while(e = es.iterateNext()) res.push(fun(e)); return res";
         } else {
            js = js + "es.forEach(function(e){ res.push(fun(e)) }); return res";
         }

         return wrapInFunctionInvoke(js);
      }
   }

   public static String highlight(String locator, int millis) {
      String fn = highlightFn(millis);
      String js = "var e = " + selector(locator) + "; var fun = " + fn + "; fun(e)";
      return wrapInFunctionInvoke(js);
   }

   public static String highlightAll(String locator, int millis) {
      return scriptAllSelector(locator, highlightFn(millis));
   }

   private static String highlightFn(int millis) {
      return "function(e){ var old = e.getAttribute('style'); e.setAttribute('style', 'background: yellow; border: 2px solid red;'); setTimeout(function(){ e.setAttribute('style', old || '') }, "
         + millis
         + ") }";
   }

   public static String commitFieldEventsJs(String e) {
      return e
         + ".dispatchEvent(new Event('input', {bubbles: true}));"
         + e
         + ".dispatchEvent(new Event('change', {bubbles: true}));"
         + e
         + ".dispatchEvent(new FocusEvent('blur', {bubbles: false}));"
         + e
         + ".dispatchEvent(new FocusEvent('focusout', {bubbles: true}));";
   }

   public static String optionSelector(String locator, String text) {
      boolean textEquals = text.startsWith("{}");
      boolean textContains = text.startsWith("{^}");
      String condition;
      if (!textEquals && !textContains) {
         condition = "e.options[i].value === t || e.options[i].text === t";
      } else {
         text = text.substring(text.indexOf(125) + 1);
         condition = textContains ? "e.options[i].text.indexOf(t) !== -1" : "e.options[i].text === t";
      }

      String escapedText = escapeForJs(text);
      String js = "var e = "
         + selector(locator)
         + ";"
         + notFoundGuard("e", locator)
         + " var t = \""
         + escapedText
         + "\"; var matched = false; for (var i = 0; i < e.options.length; ++i) if ("
         + condition
         + ") { e.options[i].selected = true; matched = true; break } if (matched) { "
         + commitFieldEventsJs("e")
         + " } return matched";
      return wrapInFunctionInvoke(js);
   }

   private static String notFoundGuard(String var, String locator) {
      return " if (!" + var + ") throw new Error(\"KARATE_ELEMENT_NOT_FOUND: " + escapeForJs(locator) + "\");";
   }

   public static String getPositionJs(String locator) {
      String js = "var e = "
         + selector(locator)
         + ";"
         + notFoundGuard("e", locator)
         + " var r = e.getBoundingClientRect(); var dx = window.scrollX; var dy = window.scrollY; return { x: r.x + dx, y: r.y + dy, width: r.width, height: r.height }";
      return wrapInFunctionInvoke(js);
   }

   public static String focusJs(String locator) {
      String js = "var e = "
         + selector(locator)
         + ";"
         + notFoundGuard("e", locator)
         + " e.focus(); try { e.selectionStart = e.selectionEnd = e.value.length } catch(x) {}";
      return wrapInFunctionInvoke(js);
   }

   public static String clickJs(String locator) {
      String js = "var e = " + selector(locator) + ";" + notFoundGuard("e", locator) + " e.click()";
      return wrapInFunctionInvoke(js);
   }

   public static String scrollJs(String locator) {
      String js = "var e = "
         + selector(locator)
         + "; var fun = function(e){ if (!e || e.nodeType !== 1) return; var d = window.getComputedStyle(e).display; while (d == 'none' && e.parentElement) { e = e.parentElement; d = window.getComputedStyle(e).display } if (e && e.nodeType === 1) e.scrollIntoView({block: 'center'}) }; fun(e)";
      return wrapInFunctionInvoke(js);
   }

   public static String inputJs(String locator, String value) {
      String escapedValue = escapeForJs(value);
      String js = "var e = "
         + selector(locator)
         + ";"
         + notFoundGuard("e", locator)
         + " e.focus(); var proto = e.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype; var setter = Object.getOwnPropertyDescriptor(proto, 'value'); if (setter && setter.set) { setter.set.call(e, \""
         + escapedValue
         + "\"); } else { e.value = \""
         + escapedValue
         + "\"; } e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true }))";
      return wrapInFunctionInvoke(js);
   }

   public static String clearJs(String locator) {
      String js = "var e = "
         + selector(locator)
         + ";"
         + notFoundGuard("e", locator)
         + " e.focus(); var proto = e.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype; var setter = Object.getOwnPropertyDescriptor(proto, 'value'); if (setter && setter.set) { setter.set.call(e, ''); } else { e.value = ''; } e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true }))";
      return wrapInFunctionInvoke(js);
   }

   public static String textJs(String locator) {
      String js = "var e = " + selector(locator) + "; return e ? e.textContent : null";
      return wrapInFunctionInvoke(js);
   }

   public static String valueJs(String locator) {
      String js = "var e = " + selector(locator) + "; return e ? e.value : null";
      return wrapInFunctionInvoke(js);
   }

   public static String attributeJs(String locator, String name) {
      String js = "var e = " + selector(locator) + "; return e ? e.getAttribute(\"" + escapeForJs(name) + "\") : null";
      return wrapInFunctionInvoke(js);
   }

   public static String propertyJs(String locator, String name) {
      String js = "var e = " + selector(locator) + "; return e ? e[\"" + escapeForJs(name) + "\"] : null";
      return wrapInFunctionInvoke(js);
   }

   public static String closestJs(String locator, String cssSelector) {
      String escaped = escapeForJs(cssSelector);
      return wrapInFunctionInvoke("var e = " + selector(locator) + "; return e ? e.closest(\"" + escaped + "\") : null");
   }

   public static String matchesJs(String locator, String cssSelector) {
      String escaped = escapeForJs(cssSelector);
      return wrapInFunctionInvoke("var e = " + selector(locator) + "; return e ? e.matches(\"" + escaped + "\") : false");
   }

   public static String scopedSelectorJs(String baseJsLocator, String cssChildSelector) {
      String escaped = escapeForJs(cssChildSelector);
      return wrapInFunctionInvoke("var __b = " + baseJsLocator + "; return __b ? __b.querySelector(\"" + escaped + "\") : null");
   }

   public static String scopedCountJs(String baseJsLocator, String cssChildSelector) {
      String escaped = escapeForJs(cssChildSelector);
      return wrapInFunctionInvoke("var __b = " + baseJsLocator + "; return __b ? __b.querySelectorAll(\"" + escaped + "\").length : 0");
   }

   public static String scopedIndexedSelectorJs(String baseJsLocator, String cssChildSelector, int index) {
      String escaped = escapeForJs(cssChildSelector);
      return wrapInFunctionInvoke(
         "var __b = "
            + baseJsLocator
            + "; if (!__b) return null; var all = __b.querySelectorAll(\""
            + escaped
            + "\"); return all.length > "
            + index
            + " ? all["
            + index
            + "] : null"
      );
   }

   public static String enabledJs(String locator) {
      String js = "var e = " + selector(locator) + "; return e ? !e.disabled : false";
      return wrapInFunctionInvoke(js);
   }

   public static String existsJs(String locator) {
      String js = "return " + selector(locator) + " !== null";
      return wrapInFunctionInvoke(js);
   }

   public static String outerHtmlJs(String locator) {
      String js = "var e = " + selector(locator) + "; return e ? e.outerHTML : null";
      return wrapInFunctionInvoke(js);
   }

   public static String innerHtmlJs(String locator) {
      String js = "var e = " + selector(locator) + "; return e ? e.innerHTML : null";
      return wrapInFunctionInvoke(js);
   }

   public static String findAllJs(String locator) {
      if (!isXpath(locator) && !locator.startsWith("{")) {
         String escaped = escapeForJs(locator);
         String js = "var els = (window.__kjs && window.__kjs.qsaDeep ? window.__kjs.qsaDeep(\""
            + escaped
            + "\") : document.querySelectorAll(\""
            + escaped
            + "\")); var result = []; for (var i = 0; i < els.length; i++) {   var uniq = (window.__kjs && window.__kjs.uniqueCss) ? window.__kjs.uniqueCss(els[i]) : null;   result.push(uniq || (\""
            + escaped
            + ":nth-of-type(\" + (i + 1) + \")\")); } return result";
         return wrapInFunctionInvoke(js);
      } else if (locator.startsWith("{")) {
         String resolverJs = expandWildcard(locator);
         return wrapInFunctionInvoke("var e = " + resolverJs + "; return e ? ['" + escapeForJs(locator) + "'] : []");
      } else {
         return wrapInFunctionInvoke("return ['" + escapeForJs(locator) + "']");
      }
   }

   public static String countJs(String locator) {
      if (locator.startsWith("{")) {
         String resolverJs = expandWildcard(locator);
         return wrapInFunctionInvoke("return " + resolverJs + " ? 1 : 0");
      } else if (isXpath(locator)) {
         String escapedXpath = escapeForJs(locator);
         String js = "var iter = document.evaluate(\""
            + escapedXpath
            + "\", document, null, 5, null); var count = 0; while(iter.iterateNext()) count++; return count";
         return wrapInFunctionInvoke(js);
      } else {
         String escaped = escapeForJs(locator);
         return "(window.__kjs && window.__kjs.qsaDeep ? window.__kjs.qsaDeep(\"" + escaped + "\") : document.querySelectorAll(\"" + escaped + "\")).length";
      }
   }

   public static String escapeForJs(String text) {
      return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   public static String escapeXpathString(String text) {
      if (text == null) {
         return "''";
      } else if (!text.contains("'")) {
         return "'" + text + "'";
      } else {
         return !text.contains("\"") ? "\"" + text + "\"" : "concat('" + text.replace("'", "',\"'\",'") + "')";
      }
   }
}
