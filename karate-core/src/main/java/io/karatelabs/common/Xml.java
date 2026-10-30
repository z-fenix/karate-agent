package io.karatelabs.common;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class Xml {
   public static String toString(Node node) {
      return toString(node, false);
   }

   public static String toString(Node node, boolean pretty) {
      Node source = resolvePrefixes(node);
      DOMSource domSource = new DOMSource(source);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      TransformerFactory tf = TransformerFactory.newInstance();

      try {
         Transformer transformer = tf.newTransformer();
         transformer.setOutputProperty("omit-xml-declaration", "yes");
         if (pretty) {
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         } else {
            transformer.setOutputProperty("indent", "no");
         }

         transformer.transform(domSource, result);
         return writer.toString();
      } catch (Exception var8) {
         throw new RuntimeException(var8);
      }
   }

   private static Node resolvePrefixes(Node node) {
      if (node != null && node.getNodeType() == 1) {
         Element target = (Element)node;
         Node parent = target.getParentNode();
         if (parent != null && parent.getNodeType() != 9) {
            Map<String, String> inherited = collectAncestorNamespaces(target);
            inherited.keySet().removeIf(k -> target.getAttributeNode(k) != null);
            if (inherited.isEmpty()) {
               return node;
            } else {
               Document newDoc = newDocument();
               Element imported = (Element)newDoc.importNode(target, true);
               newDoc.appendChild(imported);

               for (Entry<String, String> e : inherited.entrySet()) {
                  imported.setAttribute(e.getKey(), e.getValue());
               }

               return imported;
            }
         } else {
            return node;
         }
      } else {
         return node;
      }
   }

   public static Document toXmlDoc(String xml) {
      return toXmlDoc(xml, false);
   }

   public static Document toXmlDoc(String xml, boolean namespaceAware) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(namespaceAware);
      factory.setIgnoringElementContentWhitespace(false);

      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         Xml.DtdEntityResolver dtdEntityResolver = new Xml.DtdEntityResolver();
         builder.setEntityResolver(dtdEntityResolver);
         InputStream is = FileUtils.toInputStream(xml);
         Document doc = builder.parse(is);
         return dtdEntityResolver.dtdPresent ? toXmlDoc(toString(doc, false), namespaceAware) : doc;
      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }

   public static Document fromMap(Map<String, Object> map) {
      Entry<String, Object> first = map.entrySet().iterator().next();
      return fromObject(first.getKey(), first.getValue());
   }

   public static Document newDocument() {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

      DocumentBuilder builder;
      try {
         builder = factory.newDocumentBuilder();
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }

      return builder.newDocument();
   }

   public static Document fromObject(String name, Object o) {
      Document doc = newDocument();
      List<Element> list = fromObject(doc, name, o);
      Element root = list.getFirst();
      doc.appendChild(root);
      return doc;
   }

   public static List<Element> fromObject(Document doc, String name, Object o) {
      if (o instanceof Map<?, ?> rawMap) {
         Map<String, Object> map = (Map<String, Object>) rawMap;
         Map<String, Object> attribs = (Map<String, Object>)map.get("@");
         Object value = map.get("_");
         if (value == null && attribs == null) {
            Element element = createElement(doc, name, null, null);

            for (Entry<String, Object> entry : map.entrySet()) {
               String childName = entry.getKey();
               Object childValue = entry.getValue();

               for (Element e : fromObject(doc, childName, childValue)) {
                  element.appendChild(e);
               }
            }

            return Collections.singletonList(element);
         } else {
            List<Element> elements = fromObject(doc, name, value);
            addAttributes(elements.getFirst(), attribs);
            return elements;
         }
      } else if (!(o instanceof List<?> list)) {
         String value = o == null ? null : o.toString();
         Element element = createElement(doc, name, value, null);
         return Collections.singletonList(element);
      } else {
         List<Element> elements = new ArrayList<>(list.size());

         for (Object child : list) {
            List<Element> childNodes = fromObject(doc, name, child);
            elements.addAll(childNodes);
         }

         return elements;
      }
   }

   public static void addAttributes(Element element, Map<String, Object> map) {
      if (map != null) {
         for (Entry<String, Object> entry : map.entrySet()) {
            Object attrValue = entry.getValue();
            element.setAttribute(entry.getKey(), attrValue == null ? null : attrValue.toString());
         }
      }
   }

   public static Element createElement(Node node, String name, String value, Map<String, Object> attributes) {
      Document doc = node.getNodeType() == 9 ? (Document)node : node.getOwnerDocument();
      Element element = doc.createElement(name);
      element.setTextContent(value);
      addAttributes(element, attributes);
      return element;
   }

   public static Object toObject(Node node) {
      return toObject(node, false);
   }

   public static Object toObject(Node node, boolean removeNamespace) {
      if (node.getNodeType() != 9) {
         Object value = getElementAsObject(node, removeNamespace);
         if (node.hasAttributes()) {
            Map<String, Object> attribs = getAttributes(node);
            if (removeNamespace) {
               attribs.keySet().removeIf(key -> "xmlns".equals(key) || key.startsWith("xmlns:"));
            }

            if (!attribs.isEmpty()) {
               Map<String, Object> wrapper = new LinkedHashMap<>(2);
               wrapper.put("_", value);
               wrapper.put("@", attribs);
               return wrapper;
            } else {
               return value;
            }
         } else {
            return value;
         }
      } else {
         Map<String, Object> map = new LinkedHashMap<>(1);
         node = node.getFirstChild();
         if (node == null) {
            return map;
         } else {
            while (node.getNodeType() != 1) {
               node = node.getNextSibling();
            }

            String name = removeNamespace ? node.getNodeName().replaceFirst("(^.*:)", "") : node.getNodeName();
            map.put(name, toObject(node, removeNamespace));
            return map;
         }
      }
   }

   private static Object getElementAsObject(Node node, boolean removeNamespace) {
      int childElementCount = getChildElementCount(node);
      if (childElementCount == 0) {
         String textContent = node.getTextContent();
         return StringUtils.isBlank(textContent) ? null : textContent;
      } else {
         Map<String, Object> map = new LinkedHashMap<>(childElementCount);
         NodeList nodes = node.getChildNodes();
         int childCount = nodes.getLength();

         for (int i = 0; i < childCount; i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == 1) {
               String childName = removeNamespace ? child.getNodeName().replaceFirst("(^.*:)", "") : child.getNodeName();
               Object childValue = toObject(child, removeNamespace);
               if (map.containsKey(childName)) {
                  Object temp = map.get(childName);
                  if (temp instanceof List) {
                     List<Object> list = (List<Object>) temp;
                     list.add(childValue);
                  } else {
                     List<Object> list = new ArrayList<>(childCount);
                     map.put(childName, list);
                     list.add(temp);
                     list.add(childValue);
                  }
               } else {
                  map.put(childName, childValue);
               }
            }
         }

         return map;
      }
   }

   public static int getChildElementCount(Node node) {
      NodeList nodes = node.getChildNodes();
      int childCount = nodes.getLength();
      int childElementCount = 0;

      for (int i = 0; i < childCount; i++) {
         Node child = nodes.item(i);
         if (child.getNodeType() == 1) {
            childElementCount++;
         }
      }

      return childElementCount;
   }

   private static Map<String, Object> getAttributes(Node node) {
      NamedNodeMap attribs = node.getAttributes();
      int attribCount = attribs.getLength();
      Map<String, Object> map = new LinkedHashMap<>(attribCount);

      for (int j = 0; j < attribCount; j++) {
         Node attrib = attribs.item(j);
         map.put(attrib.getNodeName(), attrib.getNodeValue());
      }

      return map;
   }

   private static XPathExpression compile(String path) {
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();

      try {
         return xpath.compile(path);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public static NodeList getNodeListByPath(Node node, String path) {
      XPathExpression expr = compile(path);

      try {
         return (NodeList)expr.evaluate(node, XPathConstants.NODESET);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public static String getTextValueByPath(Node node, String path) {
      XPathExpression expr = compile(path);

      try {
         return expr.evaluate(node);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public static Node getNodeByPath(Node node, String path, boolean create) {
      String searchPath = create ? stripNameSpacePrefixes(path) : path;
      XPathExpression expr = compile(searchPath);

      Node result;
      try {
         result = (Node)expr.evaluate(node, XPathConstants.NODE);
      } catch (XPathExpressionException var7) {
         throw new RuntimeException(var7);
      }

      if (result == null && create) {
         Document doc = node.getNodeType() == 9 ? (Document)node : node.getOwnerDocument();
         return createNodeByPath(doc, path);
      } else {
         return result;
      }
   }

   public static String stripNameSpacePrefixes(String path) {
      if (path.indexOf(58) == -1) {
         return path;
      } else {
         StringBuilder sb = new StringBuilder();

         for (String s : path.split("/")) {
            if (!sb.isEmpty() || path.startsWith("/")) {
               sb.append('/');
            }

            int pos = s.lastIndexOf(58);
            if (pos == -1) {
               sb.append(s);
            } else {
               sb.append(s.substring(pos + 1));
            }
         }

         return sb.toString();
      }
   }

   public static Node createNodeByPath(Document doc, String path) {
      int pos = path.lastIndexOf(47);
      if (pos == 0) {
         Node root = doc.getDocumentElement();
         if (root == null) {
            root = createElement(doc, path.substring(1), null, null);
            doc.appendChild(root);
         }

         return root;
      } else {
         String left = path.substring(0, pos);
         Node parent = getNodeByPath(doc, left, true);
         String right = path.substring(pos + 1);
         if (right.startsWith("@")) {
            Element parentElement = (Element)parent;
            right = right.substring(1);
            parentElement.setAttribute(right, "");
            return parentElement.getAttributeNode(right);
         } else {
            int bracketPos = right.indexOf(91);
            if (bracketPos != -1) {
               right = right.substring(0, bracketPos);
            }

            Element element = createElement(parent, right, null, null);
            parent.appendChild(element);
            return element;
         }
      }
   }

   public static void setByPath(Node doc, String path, String value) {
      Node node = getNodeByPath(doc, path, true);
      if (node.getNodeType() == 2) {
         node.setNodeValue(value);
      } else if (node.hasChildNodes() && node.getFirstChild().getNodeType() == 3) {
         node.getFirstChild().setTextContent(value);
      } else if (node.getNodeType() == 1) {
         node.setTextContent(value);
      }
   }

   public static void setByPath(Document doc, String path, Node in) {
      if (in.getNodeType() == 9) {
         in = in.getFirstChild();
      }

      Node node = getNodeByPath(doc, path, true);
      if (node == null) {
         throw new RuntimeException("no results for xpath: " + path);
      } else {
         Node newNode = doc.importNode(in, true);
         if (node.hasChildNodes() && node.getFirstChild().getNodeType() == 3) {
            node.replaceChild(newNode, node.getFirstChild());
         } else {
            node.appendChild(newNode);
         }
      }
   }

   public static void removeByPath(Document doc, String path) {
      Node node = getNodeByPath(doc, path, false);
      if (node != null) {
         if (node.getNodeType() == 2) {
            Element parent = ((Attr)node).getOwnerElement();
            parent.removeAttribute(node.getNodeName());
         } else {
            Node parent = node.getParentNode();
            if (parent != null) {
               parent.removeChild(node);
            }
         }
      }
   }

   public static Document toNewDocument(Node in) {
      Document doc = newDocument();
      Node node = doc.importNode(in, true);
      if (in.getNodeType() == 1 && node.getNodeType() == 1) {
         Element target = (Element)node;
         Map<String, String> inherited = collectAncestorNamespaces(in);

         for (Entry<String, String> e : inherited.entrySet()) {
            if (target.getAttributeNode(e.getKey()) == null) {
               target.setAttribute(e.getKey(), e.getValue());
            }
         }
      }

      doc.appendChild(node);
      return doc;
   }

   private static Map<String, String> collectAncestorNamespaces(Node node) {
      Map<String, String> inherited = new LinkedHashMap<>();

      for (Node parent = node.getParentNode(); parent != null && parent.getNodeType() == 1; parent = parent.getParentNode()) {
         NamedNodeMap attribs = parent.getAttributes();
         if (attribs != null) {
            for (int i = 0; i < attribs.getLength(); i++) {
               Node attr = attribs.item(i);
               String name = attr.getNodeName();
               if (("xmlns".equals(name) || name.startsWith("xmlns:")) && !inherited.containsKey(name)) {
                  inherited.put(name, attr.getNodeValue());
               }
            }
         }
      }

      return inherited;
   }

   private static class DtdEntityResolver implements EntityResolver {
      protected boolean dtdPresent;

      @Override
      public InputSource resolveEntity(String publicId, String systemId) {
         this.dtdPresent = true;
         return new InputSource(new StringReader(""));
      }
   }
}
