package io.karatelabs.http;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Pair;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
   private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
   public static final byte[] ZERO_BYTES = new byte[0];
   public static final int MEGABYTE = 1048576;
   private static final int LENIENT_WHOLE_DOCUMENT = -8449;

   public static Charset parseContentTypeCharset(String mimeType) {
      Map<String, String> map = parseContentTypeParams(mimeType);
      if (map == null) {
         return null;
      } else {
         String cs = map.get("charset");
         return cs == null ? null : Charset.forName(cs);
      }
   }

   public static Map<String, String> parseContentTypeParams(String mimeType) {
      List<String> items = StringUtils.split(mimeType, ';', false);
      int count = items.size();
      if (count <= 1) {
         return null;
      } else {
         Map<String, String> map = new LinkedHashMap<>(count - 1);

         for (int i = 1; i < count; i++) {
            String item = items.get(i);
            int pos = item.indexOf(61);
            if (pos != -1) {
               String key = item.substring(0, pos).trim();
               String val = item.substring(pos + 1).trim();
               map.put(key, val);
            }
         }

         return map;
      }
   }

   public static Map<String, String> parseUriPattern(String pattern, String url) {
      int qpos = url.indexOf(63);
      if (qpos != -1) {
         url = url.substring(0, qpos);
      }

      List<String> leftList = StringUtils.split(pattern, '/', false);
      List<String> rightList = StringUtils.split(url, '/', false);
      int leftSize = leftList.size();
      int rightSize = rightList.size();
      if (rightSize != leftSize) {
         return null;
      } else {
         Map<String, String> map = new LinkedHashMap<>(leftSize);

         for (int i = 0; i < leftSize; i++) {
            String left = leftList.get(i);
            String right = rightList.get(i);
            if (!left.equals(right)) {
               if (!left.startsWith("{") || !left.endsWith("}")) {
                  return null;
               }

               left = left.substring(1, left.length() - 1);
               map.put(left, right);
            }
         }

         return map;
      }
   }

   public static String normaliseUriPath(String uri) {
      uri = uri.indexOf(63) == -1 ? uri : uri.substring(0, uri.indexOf(63));
      if (uri.endsWith("/")) {
         uri = uri.substring(0, uri.length() - 1);
      }

      if (!uri.startsWith("/")) {
         uri = "/" + uri;
      }

      return uri;
   }

   public static Pair<String> parseUriIntoUrlBaseAndPath(String rawUri) {
      int pos = rawUri.indexOf(47);
      if (pos == -1) {
         return Pair.of(null, "");
      } else {
         URI uri;
         try {
            uri = new URI(rawUri);
         } catch (Exception var5) {
            throw new RuntimeException(var5);
         }

         if (uri.getHost() == null) {
            return Pair.of(null, rawUri);
         } else {
            String path = uri.getRawPath();
            pos = rawUri.lastIndexOf(path);
            String urlBase = rawUri.substring(0, pos);
            return Pair.of(urlBase, rawUri.substring(pos));
         }
      }
   }

   public static Object fromBytes(byte[] bytes, boolean strict, ResourceType resourceType) {
      if (bytes == null) {
         return null;
      } else if (resourceType != null && resourceType.isBinary()) {
         return bytes;
      } else {
         String raw = FileUtils.toString(bytes);
         return fromString(raw, strict, resourceType);
      }
   }

   public static Object fromString(String raw, boolean strict, ResourceType resourceType) {
      String trimmed = raw.trim();
      if (trimmed.isEmpty()) {
         return raw;
      } else if (resourceType != null && resourceType.isBinary()) {
         return raw;
      } else {
         switch (trimmed.charAt(0)) {
            case '<':
               if (resourceType != null && !resourceType.isXml() && !resourceType.isText()) {
                  return raw;
               } else {
                  try {
                     return Xml.toXmlDoc(raw);
                  } catch (Exception var7) {
                     logger.trace("failed to parse xml: {}", var7.getMessage());
                     if (strict) {
                        throw var7;
                     } else {
                        return raw;
                     }
                  }
               }
            case '[':
            case '{':
               if (strict) {
                  return Json.parseStrict(raw);
               } else if (resourceType != null && !resourceType.isJson()) {
                  try {
                     return Json.parseStrict(raw);
                  } catch (Exception var5) {
                     logger.trace("not json despite the leading brace: {}", var5.getMessage());
                     return raw;
                  }
               } else {
                  try {
                     Object parsed = new JSONParser(-8449).parse(raw, JSONValue.defaultReader.DEFAULT_ORDERED);
                     return parsed != null ? parsed : raw;
                  } catch (Exception var6) {
                     logger.trace("failed to parse json: {}", var6.getMessage());
                     return raw;
                  }
               }
            default:
               return raw;
         }
      }
   }

   public static Object fromBytesDeclared(byte[] bytes, ResourceType resourceType) {
      if (bytes == null) {
         return null;
      } else if (resourceType == null) {
         return fromBytes(bytes, false, null);
      } else if (resourceType.isBinary()) {
         return bytes;
      } else {
         String raw = FileUtils.toString(bytes);
         if (raw.trim().isEmpty()) {
            return raw;
         } else if (resourceType.isJson()) {
            char first = raw.trim().charAt(0);
            if (first != '{' && first != '[') {
               return raw;
            } else {
               try {
                  Object parsed = new JSONParser(-8449).parse(raw, JSONValue.defaultReader.DEFAULT_ORDERED);
                  return parsed != null ? parsed : raw;
               } catch (Exception var5) {
                  logger.trace("declared json failed to parse, keeping raw string: {}", var5.getMessage());
                  return raw;
               }
            }
         } else if (resourceType.isXml()) {
            try {
               return Xml.toXmlDoc(raw);
            } catch (Exception var6) {
               logger.trace("declared xml failed to parse, keeping raw string: {}", var6.getMessage());
               return raw;
            }
         } else {
            return raw;
         }
      }
   }

   public static Pair<Object> parsePathAndParams(String uri) {
      QueryStringDecoder decoder = new QueryStringDecoder(uri);
      String path = decoder.path();
      Map<String, List<String>> params = decoder.parameters();
      return Pair.of(path, params);
   }

   public static String buildUri(String path, Map<String, List<String>> params) {
      if (params != null && !params.isEmpty()) {
         QueryStringEncoder encoder = new QueryStringEncoder(path);
         params.forEach((name, values) -> {
            if (values != null) {
               values.forEach(value -> encoder.addParam(name, value));
            }
         });
         return encoder.toString();
      } else {
         return path;
      }
   }

   public static String extractPath(String uri) {
      int qpos = uri.indexOf(63);
      return qpos == -1 ? uri : uri.substring(0, qpos);
   }

   public static String extractQueryString(String uri) {
      int qpos = uri.indexOf(63);
      return qpos == -1 ? null : uri.substring(qpos + 1);
   }

   public static Map<String, List<String>> parseQueryParams(String uri) {
      QueryStringDecoder decoder = new QueryStringDecoder(uri);
      return decoder.parameters();
   }

   public static enum Header {
      ACCEPT("Accept"),
      ALLOW("Allow"),
      CACHE_CONTROL("Cache-Control"),
      CONTENT_LENGTH("Content-Length"),
      CONTENT_TYPE("Content-Type"),
      COOKIE("Cookie"),
      LOCATION("Location"),
      SET_COOKIE("Set-Cookie"),
      TRANSFER_ENCODING("Transfer-Encoding");

      public final String key;

      private Header(String key) {
         this.key = key;
      }

      public static String[] keys() {
         return Arrays.stream(values()).map(v -> v.key).toArray(String[]::new);
      }
   }

   public static enum Method {
      GET("GET"),
      POST("POST"),
      PUT("PUT"),
      DELETE("DELETE"),
      HEAD("HEAD"),
      OPTIONS("OPTIONS"),
      TRACE("TRACE"),
      CONNECT("CONNECT"),
      PATCH("PATCH");

      public final String key;

      private Method(String key) {
         this.key = key;
      }
   }
}
