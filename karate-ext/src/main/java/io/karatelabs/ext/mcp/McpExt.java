package io.karatelabs.ext.mcp;

import io.karatelabs.core.Ext;
import io.karatelabs.core.Suite;
import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(McpExt.class);
   private static final String URL = "url";
   private static final String HEADERS = "headers";
   private static final String REVISION = "revision";
   String url;
   Map<String, Object> headers;
   String revision;

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("mcp", "openapi");
      var1.registerChannelFactory("mcp", new McpChannelFactory(this));
      logger.info("mcp ext booted (channel factory registered)");
   }

   void seedDefaults(McpSession var1) {
      if (var1.url == null && this.url != null) {
         var1.url = this.url;
      }

      if (var1.headers == null && this.headers != null) {
         var1.headers = new LinkedHashMap<>(this.headers);
      }

      if (var1.revision == null && this.revision != null) {
         var1.revision = this.revision;
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("url", "headers", "revision");
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "url" -> this.url;
         case "headers" -> this.headers;
         case "revision" -> this.revision;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "url":
            this.url = var2 != null ? var2.toString() : null;
            break;
         case "headers":
            if (var2 instanceof Map) {
               this.headers = (Map<String, Object>)var2;
            }
            break;
         case "revision":
            this.revision = var2 != null ? var2.toString() : null;
            break;
         default:
            logger.warn("mcp ext: unknown boot config key: {} - {}", var1, var2);
      }
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", McpSession.VERSION);
      return var1;
   }
}
