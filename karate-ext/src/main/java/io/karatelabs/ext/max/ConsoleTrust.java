package io.karatelabs.ext.max;

import io.karatelabs.http.HttpRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntSupplier;

public final class ConsoleTrust {
   public static final String ORIGINS_ENV = "KARATE_CONSOLE_ORIGINS";
   public static final String HOSTS_ENV = "KARATE_CONSOLE_HOSTS";
   public static final String HOST_REFUSED = "HOST_REFUSED";
   public static final String CROSS_ORIGIN_REFUSED = "CROSS_ORIGIN_REFUSED";
   private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
   private final boolean loopbackBound;
   private final IntSupplier boundPort;
   private final List<io.karatelabs.ext.max.ConsoleTrust.Authority> origins;
   private final List<io.karatelabs.ext.max.ConsoleTrust.Authority> hosts;

   public ConsoleTrust(String var1, IntSupplier var2, String var3, String var4) {
      this.loopbackBound = var1 != null && LOOPBACK.contains(var1.trim().toLowerCase(Locale.ROOT));
      this.boundPort = var2;
      this.origins = parse(var3, true);
      this.hosts = parse(var4, false);
   }

   public boolean rebindable() {
      return !this.loopbackBound && this.origins.isEmpty();
   }

   public String check(HttpRequest var1, boolean var2) {
      io.karatelabs.ext.max.ConsoleTrust.Authority var3 = io.karatelabs.ext.max.ConsoleTrust.Authority.b(var1.getHeader("Host"));
      if (this.rebindable() || var3 != null && this.hostTrusted(var3)) {
         String var4 = var1.getHeader("Origin");
         if (var4 == null) {
            return !var2 && write(var1) && crossSite(var1) ? "CROSS_ORIGIN_REFUSED" : null;
         } else if (!var2 && !write(var1)) {
            return null;
         } else {
            boolean var10000;
            if (this.rebindable()) {
               var10000 = sameAuthority(var4, var1.getHeader("Host"));
            } else {
               io.karatelabs.ext.max.ConsoleTrust.Authority var7 = io.karatelabs.ext.max.ConsoleTrust.Authority.a(var4);
               var10000 = var7 instanceof io.karatelabs.ext.max.ConsoleTrust.Authority && this.originTrusted(var7, var3);
            }

            boolean var5 = var10000;
            return var5 ? null : "CROSS_ORIGIN_REFUSED";
         }
      } else {
         return "HOST_REFUSED";
      }
   }

   private boolean hostTrusted(io.karatelabs.ext.max.ConsoleTrust.Authority var1) {
      if (!var1.a()) {
         for (io.karatelabs.ext.max.ConsoleTrust.Authority var3 : this.origins) {
            if (var3.c.equals(var1.c) && var3.c(var3.b) == var1.c(var3.b)) {
               return true;
            }
         }

         return this.hosts.stream().anyMatch(var1x -> var1x.c.equals(var1.c) && var1x.d == var1.d);
      } else {
         return !this.loopbackBound || var1.c("http") == this.boundPort.getAsInt();
      }
   }

   private boolean originTrusted(io.karatelabs.ext.max.ConsoleTrust.Authority var1, io.karatelabs.ext.max.ConsoleTrust.Authority var2) {
      if (this.origins.contains(var1)) {
         return true;
      } else if ("http".equals(var1.b) && var1.a()) {
         int var3 = this.loopbackBound ? this.boundPort.getAsInt() : (var2.a() ? var2.c("http") : -1);
         return var1.c("http") == var3;
      } else {
         return false;
      }
   }

   private static boolean sameAuthority(String var0, String var1) {
      try {
         URI var2 = URI.create(var0.trim());
         return var1 != null && var2.getScheme() != null && var2.getRawAuthority() != null && var2.getRawAuthority().equalsIgnoreCase(var1.trim());
      } catch (IllegalArgumentException var3) {
         return false;
      }
   }

   private static boolean write(HttpRequest var0) {
      String var1 = var0.getMethod() == null ? "GET" : var0.getMethod().toUpperCase(Locale.ROOT);
      return !var1.equals("GET") && !var1.equals("HEAD") && !var1.equals("OPTIONS");
   }

   private static boolean crossSite(HttpRequest var0) {
      String var1 = var0.getHeader("Sec-Fetch-Site");
      return "cross-site".equalsIgnoreCase(var1) || "same-site".equalsIgnoreCase(var1);
   }

   private static List<io.karatelabs.ext.max.ConsoleTrust.Authority> parse(String var0, boolean var1) {
      ArrayList<io.karatelabs.ext.max.ConsoleTrust.Authority> var2 = new ArrayList();
      if (var0 != null && !var0.isBlank()) {
         for (String var6 : var0.split(",")) {
            if (!var6.isBlank()) {
               io.karatelabs.ext.max.ConsoleTrust.Authority var7 = var1 ? io.karatelabs.ext.max.ConsoleTrust.Authority.a(var6) : io.karatelabs.ext.max.ConsoleTrust.Authority.b(var6);
               if (var7 == null) {
                  throw new IllegalArgumentException(
                     (var1 ? "KARATE_CONSOLE_ORIGINS: not an origin (scheme://host[:port]): " : "KARATE_CONSOLE_HOSTS: not a host[:port]: ") + var6.trim()
                  );
               }

               var2.add(var7);
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   record Authority(String b, String c, int d) {


      static io.karatelabs.ext.max.ConsoleTrust.Authority a(String var0) {
         try {
            URI var1 = URI.create(var0.trim());
            String var2 = var1.getScheme();
            if (var2 != null
               && var1.getHost() != null
               && var1.getRawUserInfo() == null
               && (var1.getRawPath() == null || var1.getRawPath().isEmpty() || var1.getRawPath().equals("/"))
               && var1.getRawQuery() == null
               && var1.getRawFragment() == null) {
               var2 = var2.toLowerCase(Locale.ROOT);
               int var3 = var1.getPort() == d(var2) ? -1 : var1.getPort();
               return new io.karatelabs.ext.max.ConsoleTrust.Authority(var2, var1.getHost().toLowerCase(Locale.ROOT), var3);
            } else {
               return null;
            }
         } catch (IllegalArgumentException var4) {
            return null;
         }
      }

      static io.karatelabs.ext.max.ConsoleTrust.Authority b(String var0) {
         if (var0 == null || var0.isBlank()) {
            return null;
         } else if (!var0.contains("/") && !var0.contains("@")) {
            try {
               URI var1 = URI.create("http://" + var0.trim());
               return var1.getHost() != null
                     && var1.getRawPath() != null
                     && var1.getRawPath().isEmpty()
                     && var1.getRawQuery() == null
                     && var1.getRawFragment() == null
                  ? new io.karatelabs.ext.max.ConsoleTrust.Authority(null, var1.getHost().toLowerCase(Locale.ROOT), var1.getPort())
                  : null;
            } catch (IllegalArgumentException var2) {
               return null;
            }
         } else {
            return null;
         }
      }

      int c(String var1) {
         return this.d == -1 ? d(var1) : this.d;
      }

      boolean a() {
         return ConsoleTrust.LOOPBACK.contains(this.c);
      }

      private static int d(String var0) {
         String var1 = var0 == null ? "" : var0;

         return switch (var1) {
            case "http", "ws" -> 80;
            case "https", "wss" -> 443;
            default -> -1;
         };
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }
   }
}
