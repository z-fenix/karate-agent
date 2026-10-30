package io.karatelabs.http;

import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.EncoderMode;
import io.netty.util.CharsetUtil;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPartBuilder {
   private static final Logger logger = LoggerFactory.getLogger(MultiPartBuilder.class);
   private final boolean multipart;
   private final String defaultCharset;
   private final HttpPostRequestEncoder encoder;
   private Map<String, Object> formFields;
   private final StringBuilder bodyForDisplay = new StringBuilder();
   private String contentTypeHeader;
   private byte[] builtBytes;

   public String getBoundary() {
      if (this.contentTypeHeader == null) {
         return null;
      } else {
         int pos = this.contentTypeHeader.lastIndexOf(61);
         return pos == -1 ? null : this.contentTypeHeader.substring(pos + 1);
      }
   }

   public Map<String, Object> getFormFields() {
      return this.formFields;
   }

   public String getContentTypeHeader() {
      return this.contentTypeHeader;
   }

   public boolean isMultipart() {
      return this.multipart;
   }

   public String getBodyForDisplay() {
      return this.bodyForDisplay.toString();
   }

   public MultiPartBuilder(boolean multipart, String defaultCharset) {
      this.multipart = multipart;
      this.defaultCharset = defaultCharset;
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf("POST"), "/");

      try {
         this.encoder = new HttpPostRequestEncoder(new DefaultHttpDataFactory(16384L), request, multipart, CharsetUtil.UTF_8, EncoderMode.HTML5);
      } catch (Exception var5) {
         throw new RuntimeException(var5);
      }
   }

   private void addFile(String name, File file, Map<String, Object> map) {
      String filename = (String)map.get("filename");
      if (filename == null) {
         filename = file.getName();
      }

      String contentType = (String)map.get("contentType");
      ResourceType resourceType;
      if (contentType == null) {
         resourceType = ResourceType.fromFileExtension(filename);
      } else {
         resourceType = ResourceType.fromContentType(contentType);
      }

      if (resourceType == null) {
         resourceType = ResourceType.BINARY;
      }

      if (contentType == null) {
         contentType = resourceType.contentType;
      }

      try {
         this.encoder.addBodyFileUpload(name, filename, file, contentType, !resourceType.isBinary());
      } catch (Exception var8) {
         throw new RuntimeException(var8);
      }
   }

   public MultiPartBuilder part(Map<String, Object> map) {
      String name = (String)map.get("name");
      Object value = map.get("value");
      if (!this.multipart) {
         List<String> list;
         if (value instanceof List<?> l) {
            list = (List<String>)(Object) l;
         } else if (value == null) {
            list = Collections.emptyList();
         } else {
            list = Collections.singletonList(value.toString());
         }

         if (this.formFields == null) {
            this.formFields = new HashMap<>();
         }

         for (String s : list) {
            this.formFields.put(name, s);

            try {
               this.encoder.addBodyAttribute(name, s);
            } catch (Exception var15) {
               throw new RuntimeException(var15);
            }
         }
      } else if (value instanceof File file) {
         this.addFile(name, file, map);
      } else if (value instanceof List) {
         for (Object o : (List)value) {
            Map<String, Object> temp = new HashMap<>();
            temp.put("name", name);
            temp.put("value", o);
            this.part(temp);
         }
      } else {
         String contentType = (String)map.get("contentType");
         ResourceType resourceType;
         if (contentType == null) {
            resourceType = ResourceType.fromObject(value);
         } else {
            resourceType = ResourceType.fromContentType(contentType);
         }

         if (resourceType == null) {
            resourceType = ResourceType.BINARY;
         }

         if (contentType == null) {
            contentType = resourceType.contentType;
         }

         Charset cs = null;
         if (!resourceType.isBinary()) {
            String charset = (String)map.get("charset");
            if (charset == null) {
               charset = this.defaultCharset;
            }

            if (charset != null) {
               cs = Charset.forName(charset);
            }
         }

         byte[] encoded = value == null ? HttpUtils.ZERO_BYTES : Json.toBytes(value);
         String filename = (String)map.get("filename");
         if (filename == null) {
            filename = "";
         }

         String transferEncoding = (String)map.get("transferEncoding");
         final Charset nullable = cs;
         MemoryFileUpload item = new MemoryFileUpload(name, filename, contentType, transferEncoding, cs, encoded.length) {
            public Charset getCharset() {
               return nullable;
            }
         };

         try {
            item.setContent(Unpooled.wrappedBuffer(encoded));
            this.encoder.addBodyHttpData(item);
            logger.debug("multipart: {}", item);
         } catch (Exception var14) {
            throw new RuntimeException(var14);
         }
      }

      return this;
   }

   public MultiPartBuilder part(String name, Object value) {
      Map<String, Object> map = new HashMap<>();
      map.put("name", name);
      map.put("value", value);
      return this.part(map);
   }

   public String toCurlCommand() {
      return this.toCurlCommand("sh");
   }

   public String toCurlCommand(String platform) {
      if (platform == null) {
         platform = "sh";
      }

      String lineContinuation = StringUtils.getLineContinuation(platform);
      StringBuilder sb = new StringBuilder();
      Iterator<InterfaceHttpData> parts = this.encoder.getBodyListAttributes().iterator();

      while (parts.hasNext()) {
         InterfaceHttpData part = parts.next();
         String name = part.getName();
         if (part instanceof FileUpload fileUpload) {
            String filename = fileUpload.getFilename();
            if (filename != null && !filename.isEmpty()) {
               sb.append("-F ");
               sb.append(StringUtils.shellEscapeForPlatform(name + "=@" + filename, platform));
               String contentType = fileUpload.getContentType();
               if (contentType != null && !contentType.isEmpty()) {
                  sb.append(";type=").append(contentType);
               }
            } else {
               String value = null;

               try {
                  value = fileUpload.getString();
               } catch (Exception var13) {
                  logger.error("failed to get file upload value: {}", var13.getMessage());
               }

               if (this.multipart) {
                  sb.append("-F ");
                  sb.append(StringUtils.shellEscapeForPlatform(name + "=" + (value != null ? value : ""), platform));
               } else {
                  sb.append("--data-urlencode ");
                  sb.append(StringUtils.shellEscapeForPlatform(name + "=" + (value != null ? value : ""), platform));
               }
            }
         } else if (part instanceof Attribute attr) {
            String value;
            try {
               value = attr.getValue();
            } catch (Exception var12) {
               value = null;
               logger.error("failed to get multipart value: {}", var12.getMessage());
            }

            if (this.multipart) {
               sb.append("-F ");
               sb.append(StringUtils.shellEscapeForPlatform(name + "=" + (value != null ? value : ""), platform));
            } else {
               sb.append("--data-urlencode ");
               sb.append(StringUtils.shellEscapeForPlatform(name + "=" + (value != null ? value : ""), platform));
            }
         }

         if (parts.hasNext()) {
            sb.append(lineContinuation);
         }
      }

      return sb.toString();
   }

   public byte[] build() {
      if (this.builtBytes != null) {
         return this.builtBytes;
      } else {
         for (InterfaceHttpData part : this.encoder.getBodyListAttributes()) {
            this.bodyForDisplay.append('\n').append(part.toString()).append('\n');
         }

         try {
            io.netty.handler.codec.http.HttpRequest request = this.encoder.finalizeRequest();
            this.contentTypeHeader = request.headers().get(HttpUtils.Header.CONTENT_TYPE.key);
            ByteBuf content;
            if (request instanceof FullHttpRequest fullRequest) {
               content = fullRequest.content();
            } else {
               content = Unpooled.buffer();

               HttpContent data;
               while ((data = this.encoder.readChunk(ByteBufAllocator.DEFAULT)) != null) {
                  content.writeBytes(data.content());
               }
            }

            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes);
            this.builtBytes = bytes;
            return bytes;
         } catch (Exception var5) {
            throw new RuntimeException(var5);
         }
      }
   }
}
