package io.karatelabs.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReportAssets {
   public static final String BASE = "META-INF/karate-ext/";
   private static final String STATIC_PREFIX = "static/";
   private final String name;
   private String js;
   private String css;
   private final List<ReportAssets.Page> pages = new ArrayList<>();
   private final List<String> assets = new ArrayList<>();
   private String ctaLabel;
   private String ctaHref;
   private ClassLoader classLoader;

   private ReportAssets(String name) {
      this.name = name;
   }

   public static ReportAssets named(String name) {
      if (name != null && !name.isBlank()) {
         return new ReportAssets(name);
      } else {
         throw new IllegalArgumentException("ReportAssets.named: name is null or empty");
      }
   }

   public ReportAssets js(String path) {
      this.js = path;
      return this;
   }

   public ReportAssets css(String path) {
      this.css = path;
      return this;
   }

   public ReportAssets page(String slot, String title, String href) {
      this.pages.add(new ReportAssets.Page(slot, title, href));
      return this;
   }

   public ReportAssets cta(String label, String href) {
      this.ctaLabel = label;
      this.ctaHref = href;
      return this;
   }

   public ReportAssets asset(String path) {
      this.assets.add(path);
      return this;
   }

   public String name() {
      return this.name;
   }

   public List<ReportAssets.Page> pages() {
      return Collections.unmodifiableList(this.pages);
   }

   public String jsHref() {
      return stripStatic(this.js);
   }

   public String cssHref() {
      return this.css == null ? null : stripStatic(this.css);
   }

   public String ctaLabel() {
      return this.ctaLabel;
   }

   public String ctaHref() {
      return this.ctaHref;
   }

   void validateAndBind(ClassLoader classLoader) {
      if (this.js != null && !this.js.isBlank()) {
         this.requireResource(classLoader, this.js, "js");
         if (this.css != null) {
            this.requireResource(classLoader, this.css, "css");
         }

         for (String a : this.assets) {
            this.requireResource(classLoader, a, "asset");
         }

         for (ReportAssets.Page p : this.pages) {
            if (p.href() != null) {
               this.requireResource(classLoader, p.href(), "page");
            }
         }

         this.classLoader = classLoader;
      } else {
         throw new RuntimeException("ext '" + this.name + "': ReportAssets.js(...) is required");
      }
   }

   public void copyTo(Path extDir) throws IOException {
      Files.createDirectories(extDir);
      this.copyResource(this.js, extDir.resolve(stripStatic(this.js)));
      if (this.css != null) {
         this.copyResource(this.css, extDir.resolve(stripStatic(this.css)));
      }

      for (String a : this.assets) {
         this.copyResource(a, extDir.resolve(stripStatic(a)));
      }

      for (ReportAssets.Page p : this.pages) {
         if (p.href() != null) {
            this.copyResource(p.href(), extDir.resolve(p.href()));
         }
      }
   }

   private void copyResource(String path, Path target) throws IOException {
      try (InputStream is = this.classLoader.getResourceAsStream("META-INF/karate-ext/" + path)) {
         if (is == null) {
            throw new IOException("ext '" + this.name + "': resource vanished after validation: " + path);
         }

         Path parent = target.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private void requireResource(ClassLoader cl, String path, String key) {
      try {
         try (InputStream is = cl.getResourceAsStream("META-INF/karate-ext/" + path)) {
            if (is == null) {
               throw new RuntimeException("ext '" + this.name + "': " + key + " points at missing resource: META-INF/karate-ext/" + path);
            }
         }
      } catch (IOException var9) {
         throw new RuntimeException("ext '" + this.name + "': failed reading META-INF/karate-ext/" + path + " — " + var9.getMessage(), var9);
      }
   }

   private static String stripStatic(String p) {
      return p.startsWith("static/") ? p.substring("static/".length()) : p;
   }

   public record Page(String slot, String title, String href) {
   }
}
