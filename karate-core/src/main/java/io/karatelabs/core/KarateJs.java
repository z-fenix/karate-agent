package io.karatelabs.core;

import io.karatelabs.common.DataUtils;
import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.driver.Driver;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.http.DefaultHttpClientFactory;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpClientFactory;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpRequestBuilder;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.markup.Markup;
import io.karatelabs.match.Expect;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import io.karatelabs.match.Value;
import io.karatelabs.process.ProcessBuilder;
import io.karatelabs.process.ProcessHandle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minidev.json.JSONValue;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class KarateJs extends KarateJsBase implements PerfContext {
   private static final Faker FAKER = new Faker();
   private final JavaCallable read;
   private static final ThreadLocal<KarateJs.ServerOwner> STARTING_FOR = new ThreadLocal<>();

   public KarateJs(Resource root) {
      this(root, new DefaultHttpClientFactory());
   }

   public KarateJs(Resource root, HttpClientFactory factory) {
      this(root, factory.create());
   }

   public KarateJs(Resource root, HttpClient client) {
      super(root, client);
      this.engine.putRootBinding("karate", this);
      this.engine.putRootBinding("read", this.read = this.initRead());
   }

   public void setPrevRequest(HttpRequest request) {
      this.prevRequest = request;
   }

   public void setPrevResponse(HttpResponse response) {
      this.prevResponse = response;
   }

   @Override
   public void capturePerfEvent(String name, long startTime, long endTime) {
      if (this.context != null) {
         this.context.getRuntime().captureCustomPerfEvent(name, startTime, endTime);
      }
   }

   public Markup markup() {
      if (this._markup == null) {
         if (this.resourceResolver != null) {
            this._markup = Markup.init(this.engine, this.resourceResolver);
         } else {
            this._markup = Markup.init(this.engine, this.root.getPrefixedPath());
         }
      }

      return this._markup;
   }

   public String doc(Map<String, Object> options) {
      String read = (String)options.get("read");
      if (read == null) {
         throw new RuntimeException("doc() requires 'read' key with template path");
      } else {
         String html = this.markup().processPath(read, null);
         if (this.onDoc != null) {
            this.onDoc.accept(html);
         }

         return html;
      }
   }

   private JavaInvokable doc() {
      return args -> {
         if (this.onDoc == null) {
            logger.warn("doc() called, but no destination set");
            return null;
         } else if (args.length == 0) {
            throw new RuntimeException("doc() needs at least one argument");
         } else {
            String read;
            if (args[0] instanceof Map<?, ?> map) {
               read = (String)map.get("read");
            } else if (args[0] == null) {
               read = null;
            } else {
               read = args[0] + "";
            }

            if (read == null) {
               throw new RuntimeException("doc() read arg should not be null");
            } else {
               Map<String, Object> vars;
               if (args.length > 1) {
                  vars = (Map<String, Object>)args[1];
               } else {
                  vars = null;
               }

               String html = this.markup().processPath(read, vars);
               this.onDoc.accept(html);
               return null;
            }
         }
      };
   }

   private JavaCallable initRead() {
      return (context, args) -> {
         if (args.length == 0) {
            throw new RuntimeException("read() needs at least one argument");
         } else {
            String rawPath = args[0] + "";
            if (rawPath.startsWith("@")) {
               return new FeatureCall(null, rawPath);
            } else {
               String path = rawPath;
               String tagSelector = null;
               String fragment = null;
               int tagPos = rawPath.indexOf(".feature@");
               String[] split = FileReaderProvider.splitFragment(rawPath);
               if (split == null || tagPos != -1 && tagPos <= split[0].length()) {
                  if (tagPos != -1) {
                     path = rawPath.substring(0, tagPos + 8);
                     tagSelector = "@" + rawPath.substring(tagPos + 9);
                  }
               } else {
                  path = split[0];
                  fragment = split[1];
               }

               Resource resource;
               if (path.startsWith("this:")) {
                  path = path.substring(5);
                  resource = this.getCurrentResource().resolve(path);
               } else {
                  resource = this.root.resolve(path);
               }

               FileReaderProvider provider = FileReaderProvider.forExtension(resource.getExtension());
               if (provider != null) {
                  return provider.read(resource, fragment);
               } else {
                  String s0$ = resource.getExtension();

                  return switch (s0$) {
                     case "json" -> JSONValue.parseKeepingOrder(this.authorText(resource.getText()));
                     case "js" -> this.engine.eval(resource);
                     case "feature" -> {
                        Feature feature = Feature.read(resource);
                        yield tagSelector != null ? new FeatureCall(feature, tagSelector) : feature;
                     }
                     case "xml" -> {
                        Document doc = Xml.toXmlDoc(this.authorText(resource.getText()));
                        this.processXmlEmbeddedExpressions(doc);
                        yield doc;
                     }
                     case "csv" -> DataUtils.fromCsv(this.authorText(resource.getText()));
                     case "yml", "yaml" -> DataUtils.fromYaml(this.authorText(resource.getText()));
                     case "pdf", "png", "jpg", "jpeg", "gif", "ico", "mp4", "bin", "zip", "gz", "tar", "xlsx", "xls", "docx", "doc", "pptx", "ppt" -> FileUtils.toBytes(
                        resource.getStream()
                     );
                     default -> {
                        byte[] bytes = FileUtils.toBytes(resource.getStream());
                        yield hasBinaryMagic(bytes) ? bytes : this.authorText(FileUtils.toString(bytes));
                     }
                  };
               }
            }
         }
      };
   }

   private static boolean hasBinaryMagic(byte[] bytes) {
      if (bytes.length >= 4 && bytes[0] == 80 && bytes[1] == 75) {
         byte b2 = bytes[2];
         byte b3 = bytes[3];
         return b2 == 3 && b3 == 4 || b2 == 5 && b3 == 6 || b2 == 7 && b3 == 8;
      } else {
         return bytes.length >= 2 && bytes[0] == 31 && bytes[1] == -117
            ? true
            : bytes.length >= 8
               && bytes[0] == -48
               && bytes[1] == -49
               && bytes[2] == 17
               && bytes[3] == -32
               && bytes[4] == -95
               && bytes[5] == -79
               && bytes[6] == 26
               && bytes[7] == -31;
      }
   }

   private void processXmlEmbeddedExpressions(Node node) {
      if (node != null) {
         if (node.getNodeType() == 9) {
            node = node.getFirstChild();
         }

         if (node != null) {
            NamedNodeMap attribs = node.getAttributes();
            if (attribs != null) {
               for (int i = 0; i < attribs.getLength(); i++) {
                  Attr attr = (Attr)attribs.item(i);
                  String value = attr.getValue();
                  if (value != null && value.contains("#(")) {
                     attr.setValue(this.processEmbeddedString(value));
                  }
               }
            }

            List<Node> elementsToRemove = new ArrayList<>();
            List<Node[]> nodesToReplace = new ArrayList<>();
            NodeList children = node.getChildNodes();

            for (int ix = 0; ix < children.getLength(); ix++) {
               Node child = children.item(ix);
               if (child.getNodeType() == 3) {
                  String text = child.getTextContent();
                  if (text != null && text.contains("#(")) {
                     String trimmed = text.trim();
                     if (trimmed.startsWith("##(") && trimmed.endsWith(")")) {
                        String expr = trimmed.substring(3, trimmed.length() - 1);
                        Object result = this.engine.eval(expr);
                        if (result == null) {
                           elementsToRemove.add(child.getParentNode());
                        } else if (result instanceof Node) {
                           nodesToReplace.add(new Node[]{child, (Node)result});
                        } else {
                           child.setTextContent(result.toString());
                        }
                     } else if (trimmed.startsWith("#(") && trimmed.endsWith(")") && !trimmed.substring(2).contains("#(")) {
                        String expr = trimmed.substring(2, trimmed.length() - 1);

                        try {
                           Object result = this.engine.eval(expr);
                           if (result instanceof Node) {
                              nodesToReplace.add(new Node[]{child, (Node)result});
                           } else {
                              child.setTextContent(KarateJsUtils.valueToString(result));
                           }
                        } catch (Exception var14) {
                        }
                     } else {
                        child.setTextContent(this.processEmbeddedString(text));
                     }
                  }
               } else if (child.getNodeType() == 1) {
                  this.processXmlEmbeddedExpressions(child);
               } else if (child.getNodeType() == 4) {
                  String text = child.getTextContent();
                  if (text != null && text.contains("#(")) {
                     child.setTextContent(this.processEmbeddedString(text));
                  }
               }
            }

            for (Node[] pair : nodesToReplace) {
               Node textNode = pair[0];
               Node xmlNode = pair[1];
               Node parent = textNode.getParentNode();
               if (parent != null) {
                  Document ownerDoc = parent.getOwnerDocument();
                  Node toImport = xmlNode;
                  if (xmlNode.getNodeType() == 9) {
                     toImport = ((Document)xmlNode).getDocumentElement();
                  }

                  Node imported = ownerDoc.importNode(toImport, true);
                  parent.replaceChild(imported, textNode);
               }
            }

            for (Node toRemove : elementsToRemove) {
               Node parent = toRemove.getParentNode();
               if (parent != null) {
                  parent.removeChild(toRemove);
               }
            }
         }
      }
   }

   private String processEmbeddedString(String str) {
      StringBuilder result = new StringBuilder();
      int i = 0;

      while (i < str.length()) {
         int hashPos = str.indexOf(35, i);
         if (hashPos == -1 || hashPos >= str.length() - 1) {
            result.append(str.substring(i));
            break;
         }

         result.append(str, i, hashPos);
         char next = str.charAt(hashPos + 1);
         if (next == '(') {
            int closePos = KarateJsUtils.findMatchingParen(str, hashPos + 1);
            if (closePos > 0) {
               String expr = str.substring(hashPos + 2, closePos);

               try {
                  Object value = this.engine.eval(expr);
                  result.append(KarateJsUtils.valueToString(value));
               } catch (Exception var10) {
                  result.append(str, hashPos, closePos + 1);
               }

               i = closePos + 1;
            } else {
               result.append('#');
               i = hashPos + 1;
            }
         } else if (next == '#' && hashPos + 2 < str.length() && str.charAt(hashPos + 2) == '(') {
            int closePos = KarateJsUtils.findMatchingParen(str, hashPos + 2);
            if (closePos > 0) {
               String expr = str.substring(hashPos + 3, closePos);

               try {
                  Object value = this.engine.eval(expr);
                  result.append(KarateJsUtils.valueToString(value));
               } catch (Exception var9) {
                  result.append(str, hashPos, closePos + 1);
               }

               i = closePos + 1;
            } else {
               result.append("##");
               i = hashPos + 2;
            }
         } else {
            result.append('#');
            i = hashPos + 1;
         }
      }

      return result.toString();
   }

   private JavaInvokable http() {
      return args -> {
         if (args.length > 0) {
            this.http.url(args[0] + "");
         }

         return this.http;
      };
   }

   private JavaInvokable readAsString() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("read() needs at least one argument");
         } else {
            Resource resource = this.root.resolve(args[0] + "");
            return this.authorText(resource.getText());
         }
      };
   }

   private String authorText(String text) {
      ScenarioRuntime rt = this.getRuntime();
      if (rt != null) {
         rt.addAuthorText(text);
      }

      return text;
   }

   private ScenarioRuntime liveRuntime() {
      ScenarioRuntime rt = this.mockHandler == null ? ScenarioRuntime.currentOrNull() : null;
      return rt != null ? rt : this.getRuntime();
   }

   private JavaInvokable sysenv() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("sysenv() needs the environment-variable name");
         } else {
            Object fallback = args.length > 1 ? args[1] : null;
            Object first = args[0];
            if (first == null) {
               return fallback;
            } else {
               String value = this.getEnvVar(first.toString());
               return value != null && !value.isEmpty() ? value : fallback;
            }
         }
      };
   }

   private JavaInvokable sysprop() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("sysprop() needs the property name");
         } else {
            Object fallback = args.length > 1 ? args[1] : null;
            Object first = args[0];
            if (first == null) {
               return fallback;
            } else {
               String value = this.getProperty(first.toString());
               return value != null && !value.isEmpty() ? value : fallback;
            }
         }
      };
   }

   private JavaInvokable readAsBytes() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("readAsBytes() needs at least one argument");
         } else {
            String path = args[0] + "";
            Resource resource = this.getCurrentResource().resolve(path);

            try {
               byte[] t$;
               try (InputStream is = resource.getStream()) {
                  t$ = is.readAllBytes();
               }

               return t$;
            } catch (IOException var9) {
               throw new RuntimeException("Failed to read bytes from: " + path, var9);
            }
         }
      };
   }

   private JavaInvokable get() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("get() needs at least one argument");
         } else {
            String expr = args[0] + "";
            Object result;
            if (!expr.startsWith("$") && isSimpleIdentifier(expr)) {
               result = this.engine.get(expr);
            } else {
               ScenarioRuntime rt = this.liveRuntime();

               try {
                  result = rt == null ? this.engine.eval(expr) : rt.getExecutor().evalKarateExpression(expr);
               } catch (Exception var6) {
                  result = null;
               }
            }

            if ("#notpresent".equals(result)) {
               result = null;
            }

            return result == null && args.length > 1 ? args[1] : result;
         }
      };
   }

   private static boolean isSimpleIdentifier(String s) {
      if (s != null && !s.isEmpty()) {
         char first = s.charAt(0);
         if (!Character.isLetter(first) && first != '_' && first != '$') {
            return false;
         } else {
            for (int i = 1; i < s.length(); i++) {
               char c = s.charAt(i);
               if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private JavaInvokable set() {
      return args -> {
         if (args.length == 1) {
            if (!(args[0] instanceof Map<?, ?> bulk)) {
               throw new RuntimeException("set() with a single argument expects a Map / JSON object");
            } else {
               for (Entry<?, ?> e : bulk.entrySet()) {
                  this.engine.put(e.getKey() + "", e.getValue());
               }

               return null;
            }
         } else if (args.length < 2) {
            throw new RuntimeException("set() needs at least two arguments: name and value");
         } else {
            String name = args[0] + "";
            if (args.length == 2) {
               this.engine.put(name, args[1]);
            } else {
               String path = args[1] + "";
               Object value = args[2];
               Object target = this.engine.get(name);
               if (!path.startsWith("/") && !(target instanceof Node)) {
                  if (target == null) {
                     target = new LinkedHashMap();
                     this.engine.put(name, target);
                  }

                  Json.of(target).set(path, value);
               } else {
                  Document doc;
                  if (target instanceof Document targetDoc) {
                     doc = targetDoc;
                  } else if (target instanceof Node) {
                     doc = ((Node)target).getOwnerDocument();
                  } else if (target == null) {
                     doc = Xml.newDocument();
                     this.engine.put(name, doc);
                  } else {
                     if (!(target instanceof String) || !StringUtils.isXml((String)target)) {
                        throw new RuntimeException("cannot set xpath on non-XML variable: " + name);
                     }

                     doc = Xml.toXmlDoc((String)target);
                     this.engine.put(name, doc);
                  }

                  if (value instanceof Node) {
                     Xml.setByPath(doc, path, (Node)value);
                  } else {
                     Xml.setByPath(doc, path, value == null ? "" : value.toString());
                  }
               }
            }

            return null;
         }
      };
   }

   private JavaInvokable karateMatch() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("karate.match() needs at least one argument");
         } else if (args.length >= 2) {
            Object actual = args[0];
            Object expected = args[1];
            ScenarioRuntime rt = this.liveRuntime();

            Map t$;
            try (Value value = Match.evaluate(actual, null, null)) {
               t$ = rt == null
                  ? value.is(this.engine, Match.Type.EQUALS, expected).toMap()
                  : value.is(rt.getEngine(), rt.markerPolicy(), Match.Type.EQUALS, expected).toMap();
            }

            return t$;
         } else {
            ScenarioRuntime rt = this.liveRuntime();
            if (rt == null) {
               throw new RuntimeException("karate.match(String) is not available in this context");
            } else {
               String expression = args[0].toString();
               Result result = rt.getExecutor().evalMatchString(expression, null);
               return result.toMap();
            }
         }
      };
   }

   private JavaInvokable call() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt == null) {
            throw new RuntimeException("karate.call() is not available in this context");
         } else if (args.length == 0) {
            throw new RuntimeException("karate.call() requires at least one argument (feature path)");
         } else {
            boolean sharedScope = false;
            String path;
            Object arg;
            if (args[0] instanceof Boolean) {
               sharedScope = (Boolean)args[0];
               if (args.length < 2) {
                  throw new RuntimeException("karate.call() with sharedScope requires a feature path");
               }

               path = args[1].toString();
               arg = args.length > 2 ? args[2] : null;
            } else {
               path = args[0].toString();
               arg = args.length > 1 ? args[1] : null;
            }

            Object result = rt.executeJsCall(path, arg);
            if (sharedScope && result instanceof Map<?, ?> resultMap) {
               for (Entry<?, ?> entry : resultMap.entrySet()) {
                  this.engine.put((String) entry.getKey(), entry.getValue());
               }
            }

            return result;
         }
      };
   }

   private JavaInvokable eval() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("eval() needs one argument");
         } else {
            return this.engine.eval(args[0].toString());
         }
      };
   }

   private JavaCallable expect() {
      return (context, args) -> {
         if (args.length == 0) {
            throw new RuntimeException("expect() needs at least one argument");
         } else {
            return new Expect(args[0], this.onMatch);
         }
      };
   }

   private JavaInvokable remove() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("remove() needs two arguments: variable name and path");
         } else {
            String varName = args[0].toString();
            String path = args[1].toString();
            Object var = this.engine.get(varName);
            if (var instanceof Node && path != null && path.startsWith("/")) {
               Document doc = var instanceof Document ? (Document)var : ((Node)var).getOwnerDocument();
               Xml.removeByPath(doc, path);
            } else if ((var instanceof Map || var instanceof List) && path != null) {
               Json.of(var).remove(path);
            }

            return null;
         }
      };
   }

   private JavaInvokable setXml() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("setXml() needs at least two arguments: name and xml");
         } else {
            String name = args[0].toString();
            if (args.length == 2) {
               String xml = args[1].toString();
               this.engine.put(name, Xml.toXmlDoc(xml));
            } else {
               String path = args[1].toString();
               String xml = args[2].toString();
               if (this.engine.get(name) instanceof Node doc) {
                  if (doc.getNodeType() != 9) {
                     doc = doc.getOwnerDocument();
                  }

                  Xml.setByPath((Document)doc, path, Xml.toXmlDoc(xml));
               }
            }

            return null;
         }
      };
   }

   private JavaInvokable exec() {
      return args -> {
         this.checkProcessEnabled("exec");
         if (args.length == 0) {
            throw new RuntimeException("exec() needs at least one argument");
         } else {
            ProcessBuilder builder = ProcessBuilder.create();
            Object arg = args[0];
            if (arg instanceof String) {
               builder.line((String)arg);
            } else if (arg instanceof List) {
               builder.args((List<String>)arg);
            } else {
               if (!(arg instanceof Map)) {
                  throw new RuntimeException("exec() argument must be string, array, or object");
               }

               builder = ProcessBuilder.fromMap((Map<String, Object>)arg);
            }

            ProcessHandle handle = ProcessHandle.start(builder.build());
            handle.waitSync();
            return handle.getStdOut();
         }
      };
   }

   private JavaInvokable fork() {
      return args -> {
         this.checkProcessEnabled("fork");
         if (args.length == 0) {
            throw new RuntimeException("fork() needs at least one argument");
         } else {
            ProcessBuilder builder = ProcessBuilder.create();
            Consumer<String> listener = null;
            Consumer<String> errorListener = null;
            boolean autoStart = true;
            Object arg = args[0];
            if (arg instanceof String) {
               builder.line((String)arg);
            } else if (arg instanceof List) {
               builder.args((List<String>)arg);
            } else {
               if (!(arg instanceof Map<?, ?> rawOptions)) {
                  throw new RuntimeException("fork() argument must be string, array, or object");
               }

               Map<String, Object> options = (Map<String, Object>) rawOptions;
               builder = ProcessBuilder.fromMap(options);
               if (options.get("listener") instanceof JavaCallable jsListener) {
                  listener = line -> {
                     try {
                        jsListener.call(null, line);
                     } catch (Exception var3x) {
                        logger.warn("process listener error: {}", var3x.getMessage());
                     }
                  };
               }

               if (options.get("errorListener") instanceof JavaCallable jsErrorListener) {
                  errorListener = line -> {
                     try {
                        jsErrorListener.call(null, line);
                     } catch (Exception var3x) {
                        logger.warn("process errorListener error: {}", var3x.getMessage());
                     }
                  };
               }

               Object startObj = options.get("start");
               if (startObj instanceof Boolean) {
                  autoStart = (Boolean)startObj;
               }
            }

            if (listener != null) {
               builder.listener(listener);
            }

            if (errorListener != null) {
               builder.errorListener(errorListener);
            }

            ProcessHandle handle = ProcessHandle.create(builder.build());
            ScenarioRuntime rt = this.getRuntime();
            if (rt != null) {
               handle.setSignalConsumer(rt::setListenResult);
            }

            if (autoStart) {
               handle.start();
            }

            return handle;
         }
      };
   }

   private JavaInvokable start() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("start() needs at least one argument: feature path or config map");
         } else {
            Object arg = args[0];
            MockServer.Builder builder;
            if (arg instanceof String path) {
               builder = MockServer.feature(this.root.resolve(path));
            } else {
               if (!(arg instanceof Map<?, ?> config)) {
                  throw new RuntimeException("start() argument must be a string path or config map");
               }

               String mockPath = (String)config.get("mock");
               if (mockPath == null) {
                  throw new RuntimeException("start() config requires 'mock' key with feature path");
               }

               builder = MockServer.feature(this.root.resolve(mockPath));
               if (config.containsKey("port")) {
                  builder.port(((Number)config.get("port")).intValue());
               }

               if (config.containsKey("ssl")) {
                  builder.ssl(Boolean.TRUE.equals(config.get("ssl")));
               }

               if (config.containsKey("cert")) {
                  builder.certPath((String)config.get("cert"));
               }

               if (config.containsKey("key")) {
                  builder.keyPath((String)config.get("key"));
               }

               if (config.containsKey("arg")) {
                  builder.arg((Map<String, Object>)config.get("arg"));
               }

               if (config.containsKey("pathPrefix")) {
                  builder.pathPrefix((String)config.get("pathPrefix"));
               }

               if (config.containsKey("maxInitialLineLength")) {
                  builder.maxInitialLineLength(((Number)config.get("maxInitialLineLength")).intValue());
               }

               if (config.containsKey("maxHeaderSize")) {
                  builder.maxHeaderSize(((Number)config.get("maxHeaderSize")).intValue());
               }

               if (config.containsKey("maxContentLength")) {
                  builder.maxContentLength(((Number)config.get("maxContentLength")).intValue());
               }
            }

            KarateJs.ServerOwner owner = startingFor(this.liveRuntime());
            KarateJs.ServerOwner outer = STARTING_FOR.get();
            STARTING_FOR.set(owner);

            MockServer server;
            try {
               server = builder.start();
            } finally {
               if (outer == null) {
                  STARTING_FOR.remove();
               } else {
                  STARTING_FOR.set(outer);
               }
            }

            if (owner != null) {
               owner.own(server);
            }

            return server;
         }
      };
   }

   private static KarateJs.ServerOwner startingFor(ScenarioRuntime rt) {
      Suite suite = rt != null && rt.getFeatureRuntime() != null ? rt.getFeatureRuntime().getSuite() : null;
      if (suite == null) {
         return STARTING_FOR.get();
      } else if (suite.getCallSingleLock().isHeldByCurrentThread()) {
         return new KarateJs.ServerOwner(suite, null);
      } else {
         ScenarioRuntime top = rt;

         while (true) {
            FeatureRuntime fr = top.getFeatureRuntime();
            if (!fr.getCallOnceLock().isHeldByCurrentThread() && !Thread.holdsLock(fr.getSetupOnceCache())) {
               if (top.getSetupCaller() != null) {
                  top = top.getSetupCaller();
                  continue;
               }

               if (!fr.isTopLevel() && fr.getCallerScenario() != null) {
                  top = fr.getCallerScenario();
                  continue;
               }

               return new KarateJs.ServerOwner(suite, top);
            }

            return new KarateJs.ServerOwner(suite, null);
         }
      }
   }

   private JavaInvokable proceed() {
      return args -> {
         if (this.mockHandler == null) {
            throw new RuntimeException("proceed() can only be called within a mock scenario");
         } else {
            HttpRequest currentRequest = this.mockHandler.getCurrentRequest();
            String targetUrl;
            if (args.length > 0 && args[0] != null) {
               targetUrl = args[0].toString();
            } else {
               String host = currentRequest.getHeader("Host");
               if (host == null) {
                  throw new RuntimeException("proceed() needs a target URL or Host header in request");
               }

               targetUrl = "http://" + host;
            }

            HttpRequestBuilder builder = new HttpRequestBuilder(this.client);
            builder.url(targetUrl);
            builder.path(currentRequest.getPath());
            builder.method(currentRequest.getMethod());
            if (currentRequest.getHeaders() != null) {
               currentRequest.getHeaders().forEach((name, values) -> {
                  String lowerName = name.toLowerCase();
                  if (!lowerName.equals("content-length") && !lowerName.equals("host") && !lowerName.equals("transfer-encoding")) {
                     builder.header(name, (List<String>)values);
                  }
               });
            }

            Object body = currentRequest.getBodyConverted();
            if (body != null) {
               builder.body(body);
            }

            return builder.invoke();
         }
      };
   }

   private Object getRequest() {
      if (this.mockHandler == null) {
         logger.warn("karate.request is only available in mock context");
         return null;
      } else {
         return this.engine.get("request");
      }
   }

   private Object getResponse() {
      return this.mockHandler != null ? this.engine.get("response") : this.prevResponse;
   }

   private JavaInvokable readAsStream() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("readAsStream() needs at least one argument");
         } else {
            String path = args[0] + "";
            Resource resource = this.getCurrentResource().resolve(path);

            try {
               return resource.getStream();
            } catch (Exception var5) {
               throw new RuntimeException("Failed to open stream for: " + path, var5);
            }
         }
      };
   }

   private JavaInvokable render() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("render() needs at least one argument");
         } else {
            Map<String, Object> vars = null;
            String readPath;
            if (args[0] instanceof Map<?, ?> map) {
               readPath = (String)map.get("read");
            } else {
               if (args[0] == null) {
                  throw new RuntimeException("render() read arg should not be null");
               }

               readPath = args[0] + "";
            }

            if (args.length > 1 && args[1] instanceof Map) {
               vars = (Map<String, Object>)args[1];
            }

            return this.markup().processPath(readPath, vars);
         }
      };
   }

   private JavaInvokable toAbsolutePath() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("toAbsolutePath() needs a path argument");
         } else {
            String path = args[0] + "";
            Resource resource = this.getCurrentResource().resolve(path);
            return resource.isFile() && resource.getPath() != null ? resource.getPath().toAbsolutePath().toString() : resource.getPrefixedPath();
         }
      };
   }

   private JavaInvokable write() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("write() needs value and path arguments");
         } else {
            Object value = args[0];
            String path = args[1] + "";
            String outputDir = this.getOutputDir();
            File file = new File(outputDir, path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
               parent.mkdirs();
            }

            byte[] bytes = KarateJsUtils.convertToBytes(value);

            try {
               Files.write(file.toPath(), bytes);
               logger.debug("wrote {} bytes to: {}", bytes.length, file.getAbsolutePath());
               return file;
            } catch (Exception var9) {
               throw new RuntimeException("Failed to write file: " + file.getAbsolutePath(), var9);
            }
         }
      };
   }

   private Driver getDriverLazy() {
      ScenarioRuntime rt = this.getRuntime();
      if (rt == null) {
         throw new RuntimeException("karate.driver can only be read within a scenario");
      } else {
         return rt.getDriver();
      }
   }

   private JavaInvokable channel() {
      return args -> {
         if (args.length == 0) {
            throw new RuntimeException("channel() needs a type argument, e.g. karate.channel('kafka')");
         } else {
            String type = args[0].toString();
            ScenarioRuntime rt = this.getRuntime();
            if (rt == null) {
               throw new RuntimeException("channel() can only be called within a scenario");
            } else {
               FeatureRuntime fr = rt.getFeatureRuntime();
               Suite suite = fr != null ? fr.getSuite() : null;
               ChannelFactory factory = suite != null ? suite.getChannelFactory(type) : null;

               try {
                  if (factory == null) {
                     String cap = Character.toUpperCase(type.charAt(0)) + type.substring(1);
                     String factoryClass = "io.karatelabs.ext." + type + "." + cap + "ChannelFactory";
                     Class<?> clazz = Class.forName(factoryClass);
                     factory = (ChannelFactory)clazz.getDeclaredConstructor().newInstance();
                  }

                  Channel channel = factory.create(rt, new HashMap<>());
                  rt.registerChannel(channel);
                  return channel.init(rt);
               } catch (RuntimeException var10) {
                  throw var10;
               } catch (ClassNotFoundException var11) {
                  throw new RuntimeException(
                     "cannot find ["
                        + type
                        + "]: no channel factory is registered for it and io.karatelabs.ext."
                        + type
                        + "."
                        + Character.toUpperCase(type.charAt(0))
                        + type.substring(1)
                        + "ChannelFactory is not on the classpath. Add the 'karate-"
                        + type
                        + "' dependency (maven / gradle), or run an engine jar that bundles the protocol leaves, or put the leaf jar on the classpath."
                  );
               } catch (Exception var12) {
                  throw new RuntimeException(var12.getMessage(), var12);
               }
            }
         }
      };
   }

   @Override
   public Object jsGet(String key) {
      return switch (key) {
         case "append" -> KarateJsUtils.append();
         case "appendTo" -> KarateJsUtils.appendTo();
         case "distinct" -> KarateJsUtils.distinct();
         case "extract" -> KarateJsUtils.extract();
         case "extractAll" -> KarateJsUtils.extractAll();
         case "filter" -> KarateJsUtils.filter();
         case "filterKeys" -> KarateJsUtils.filterKeys();
         case "forEach" -> KarateJsUtils.forEach();
         case "fromJson" -> KarateJsUtils.fromJson();
         case "fromString" -> KarateJsUtils.fromString();
         case "jsonPath" -> KarateJsUtils.jsonPath();
         case "keysOf" -> KarateJsUtils.keysOf();
         case "lowerCase" -> KarateJsUtils.lowerCase();
         case "map" -> KarateJsUtils.map();
         case "mapWithKey" -> KarateJsUtils.mapWithKey();
         case "merge" -> KarateJsUtils.merge();
         case "os" -> KarateJsUtils.getOsInfo();
         case "pause" -> KarateJsUtils.pause();
         case "pretty" -> KarateJsUtils.pretty();
         case "prettyXml" -> KarateJsUtils.prettyXml();
         case "range" -> KarateJsUtils.range();
         case "repeat" -> KarateJsUtils.repeat();
         case "sizeOf" -> KarateJsUtils.sizeOf();
         case "sort" -> KarateJsUtils.sort();
         case "toBean" -> KarateJsUtils.toBean();
         case "toBytes" -> KarateJsUtils.toBytes();
         case "toCsv" -> KarateJsUtils.toCsv();
         case "toJson" -> KarateJsUtils.toJson();
         case "toString" -> KarateJsUtils.toStringValue();
         case "typeOf" -> KarateJsUtils.typeOf();
         case "urlDecode" -> KarateJsUtils.urlDecode();
         case "urlEncode" -> KarateJsUtils.urlEncode();
         case "uuid" -> KarateJsUtils.uuid();
         case "valuesOf" -> KarateJsUtils.valuesOf();
         case "abort" -> this.abort();
         case "call" -> this.call();
         case "callonce" -> this.callonce();
         case "channel" -> this.channel();
         case "callSingle" -> this.callSingle();
         case "config" -> this.getConfig();
         case "configure" -> this.configure();
         case "doc" -> this.doc();
         case "driver" -> this.getDriverLazy();
         case "embed" -> this.embed();
         case "env" -> this.env;
         case "eval" -> this.eval();
         case "exec" -> this.exec();
         case "expect" -> this.expect();
         case "fail" -> KarateJsUtils.fail();
         case "faker" -> FAKER;
         case "feature" -> this.getFeatureData();
         case "fork" -> this.fork();
         case "get" -> this.get();
         case "http" -> this.http();
         case "info" -> this.getInfo();
         case "log" -> this.log();
         case "logger" -> this.getLogger();
         case "match" -> this.karateMatch();
         case "prevRequest" -> this.getPrevRequest();
         case "proceed" -> this.proceed();
         case "properties" -> this.getProperties();
         case "read" -> this.read;
         case "readAsBytes" -> this.readAsBytes();
         case "readAsStream" -> this.readAsStream();
         case "readAsString" -> this.readAsString();
         case "remove" -> this.remove();
         case "render" -> this.render();
         case "request" -> this.getRequest();
         case "response" -> this.getResponse();
         case "scenario" -> this.getScenarioData();
         case "scenarioOutline" -> this.getScenarioOutlineData();
         case "suite" -> this.getSuiteData();
         case "set" -> this.set();
         case "setup" -> this.setup();
         case "setupOnce" -> this.setupOnce();
         case "setXml" -> this.setXml();
         case "signal" -> this.signal();
         case "start" -> this.start();
         case "stop" -> KarateJsUtils.stop();
         case "sysenv" -> this.sysenv();
         case "sysprop" -> this.sysprop();
         case "tags" -> this.getTags();
         case "tagValues" -> this.getTagValues();
         case "toAbsolutePath" -> this.toAbsolutePath();
         case "toJava" -> KarateJsUtils.toJava();
         case "waitForHttp" -> KarateJsUtils.waitForHttp();
         case "waitForPort" -> KarateJsUtils.waitForPort();
         case "write" -> this.write();
         case "xmlPath" -> KarateJsUtils.xmlPath();
         default -> null;
      };
   }

   record ServerOwner(Suite suite, ScenarioRuntime scenario) {
      void own(MockServer server) {
         if (this.scenario != null) {
            this.scenario.ownServer(server);
         }

         this.suite.ownServer(server);
      }
   }
}
