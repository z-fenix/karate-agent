package io.karatelabs.http;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cookies {
   private static final Logger logger = LoggerFactory.getLogger(Cookies.class);
   public static final String NAME = "name";
   public static final String VALUE = "value";
   public static final String WRAP = "wrap";
   public static final String DOMAIN = "domain";
   public static final String PATH = "path";
   public static final String MAX_AGE = "max-age";
   public static final String SECURE = "secure";
   public static final String HTTP_ONLY = "httponly";
   public static final String SAME_SITE = "samesite";

   private Cookies() {
   }

   public static Map<String, Object> toMap(Cookie cookie) {
      Map<String, Object> map = new HashMap<>();
      map.put("name", cookie.name());
      map.put("value", cookie.value());
      map.put("wrap", cookie.wrap());
      map.put("domain", cookie.domain());
      map.put("path", cookie.path());
      map.put("max-age", cookie.maxAge());
      map.put("secure", cookie.isSecure());
      map.put("httponly", cookie.isHttpOnly());
      if (cookie instanceof DefaultCookie dc && dc.sameSite() != null) {
         map.put("samesite", dc.sameSite().name());
      }

      return map;
   }

   public static Cookie fromMap(Map<String, Object> map) {
      String name = (String)map.get("name");
      String value = (String)map.get("value");
      DefaultCookie cookie = new DefaultCookie(name, value);
      Boolean wrap = (Boolean)map.get("wrap");
      if (wrap != null) {
         cookie.setWrap(wrap);
      }

      String domain = (String)map.get("domain");
      if (domain != null) {
         cookie.setDomain(domain);
      }

      String path = (String)map.get("path");
      if (path != null) {
         cookie.setPath(path);
      }

      Object maxAge = map.get("max-age");
      if (maxAge != null) {
         cookie.setMaxAge(Long.parseLong(maxAge + ""));
      }

      Boolean secure = (Boolean)map.get("secure");
      if (secure != null) {
         cookie.setSecure(secure);
      }

      Boolean httpOnly = (Boolean)map.get("httponly");
      if (httpOnly != null) {
         cookie.setHttpOnly(httpOnly);
      }

      String sameSite = (String)map.get("samesite");
      if (sameSite != null) {
         cookie.setSameSite(io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite.valueOf(sameSite));
      }

      return cookie;
   }

   public static Map<String, Map<String, Object>> normalize(Object mapOrList) {
      Map<String, Map<String, Object>> cookies = new HashMap<>();
      if (mapOrList instanceof Map<?, ?> rawMap) {
         Map<String, Object> map = (Map<String, Object>) rawMap;
         map.forEach((k, v) -> {
            if (v instanceof String) {
               Map<String, Object> cookie = new HashMap<>(2);
               cookie.put("name", k);
               cookie.put("value", v);
               cookies.put(k, cookie);
            } else if (v instanceof Map) {
               Map<String, Object> cookie = (Map<String, Object>) v;
               cookie.put("name", k);
               cookies.put(k, cookie);
            }
         });
      } else if (mapOrList instanceof List<?> rawList) {
         List<Map<String, Object>> list = (List<Map<String, Object>>)(Object) rawList;
         list.forEach(m -> cookies.put((String)m.get("name"), m));
      }

      return cookies;
   }
}
