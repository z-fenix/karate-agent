package io.karatelabs.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;

public enum ResourceType {
   JS("text/javascript", vals("javascript"), vals("js")),
   JSON("application/json", vals("json"), vals("json")),
   CSS("text/css", vals("css"), vals("css")),
   ICO("image/x-icon", vals("x-icon"), vals("ico")),
   PNG("image/png", vals("png"), vals("png")),
   GIF("image/gif", vals("gif"), vals("gif")),
   JPG("image/jpeg", vals("jpeg", "jpg"), vals("jpg", "jpeg")),
   SVG("image/svg+xml", vals("svg"), vals("svg")),
   MP4("video/mp4", vals("mp4"), vals("mp4")),
   PDF("application/pdf", vals("pdf"), vals("pdf")),
   HTML("text/html", vals("html"), vals("html", "htm")),
   XML("application/xml", vals("xml"), vals("xml")),
   TEXT("text/plain", vals("plain"), vals("txt")),
   WOFF2("application/font-woff2", vals("woff2"), vals("woff2")),
   MULTIPART("multipart/form-data", vals("multipart"), vals()),
   URLENCODED("application/x-www-form-urlencoded", vals("urlencoded"), vals()),
   BINARY("application/octet-stream", vals("octet"), vals()),
   CSV("text/csv", vals("csv"), vals("csv")),
   YAML("application/yaml", vals("yaml", "yml"), vals("yaml", "yml")),
   TOML("application/toml", vals("toml"), vals("toml")),
   MARKDOWN("text/markdown", vals("markdown"), vals("md", "markdown"));

   public final String contentType;
   public final String[] contentLike;
   public final String[] extensions;
   private static final Map<String, ResourceType> EXTENSION_MAP = new HashMap<>();

   private static String[] vals(String... values) {
      return values;
   }

   private ResourceType(String contentType, String[] contentLike, String[] extensions) {
      this.contentType = contentType;
      this.contentLike = contentLike;
      this.extensions = extensions;
   }

   public static ResourceType fromFileExtension(String path) {
      if (path == null) {
         return null;
      } else {
         int pos = path.lastIndexOf(46);
         if (pos != -1 && pos != path.length() - 1) {
            String extension = path.substring(pos + 1).trim().toLowerCase();
            return EXTENSION_MAP.get(extension);
         } else {
            return null;
         }
      }
   }

   public String getExtension() {
      return this.extensions.length == 0 ? null : this.extensions[0];
   }

   public boolean isVideo() {
      return this == MP4;
   }

   public boolean isImage() {
      return switch (this) {
         case ICO, PNG, GIF, JPG, BINARY -> true;
         default -> false;
      };
   }

   public boolean isUrlEncodedOrMultipart() {
      return switch (this) {
         case MULTIPART, URLENCODED -> true;
         default -> false;
      };
   }

   public boolean isHtml() {
      return this == HTML;
   }

   public boolean isJson() {
      return this == JSON;
   }

   public boolean isXml() {
      return this == XML;
   }

   public boolean isText() {
      return this == TEXT;
   }

   public boolean isBinary() {
      return switch (this) {
         case ICO, PNG, GIF, JPG, MP4, PDF, BINARY -> true;
         default -> false;
      };
   }

   public boolean isScript() {
      return this == JS;
   }

   public static ResourceType fromContentType(String ct) {
      if (ct == null) {
         return null;
      } else {
         ct = ct.toLowerCase();

         for (ResourceType rt : values()) {
            if (ct.equals(rt.contentType)) {
               return rt;
            }

            for (String like : rt.contentLike) {
               if (ct.contains(like)) {
                  return rt;
               }
            }
         }

         return null;
      }
   }

   public static ResourceType fromObject(Object o) {
      return fromObject(o, null);
   }

   public static ResourceType fromObject(Object o, ResourceType defaultType) {
      if (o instanceof List || o instanceof Map) {
         return JSON;
      } else if (o instanceof String) {
         return TEXT;
      } else if (o instanceof Node) {
         return XML;
      } else {
         return o instanceof byte[] ? BINARY : defaultType;
      }
   }

   static {
      for (ResourceType rt : values()) {
         for (String ext : rt.extensions) {
            EXTENSION_MAP.put(ext, rt);
         }
      }
   }
}
