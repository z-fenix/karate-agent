package io.karatelabs.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class UrlResource implements Resource {
   private static final Path SYSTEM_TEMP = Path.of(System.getProperty("java.io.tmpdir"));
   private final URL url;
   private final URI uri;
   private final Path root;
   private final byte[] bytes;
   private String text;
   private String[] lines;

   UrlResource(URL url, byte[] bytes) {
      this(url, bytes, null);
   }

   UrlResource(URL url, byte[] bytes, Path root) {
      this.url = url;
      this.uri = toUri(url);
      this.root = root != null ? root : SYSTEM_TEMP;
      this.bytes = bytes;
   }

   private static URI toUri(URL url) {
      try {
         return url.toURI();
      } catch (Exception var2) {
         return null;
      }
   }

   public URL getUrl() {
      return this.url;
   }

   @Override
   public String getText() {
      if (this.text == null) {
         this.text = FileUtils.toString(this.bytes);
      }

      return this.text;
   }

   @Override
   public String getLine(int index) {
      if (this.lines == null) {
         this.lines = this.getText().split("\\r?\\n");
      }

      return this.lines[index];
   }

   @Override
   public boolean isFile() {
      return false;
   }

   @Override
   public boolean isClassPath() {
      return false;
   }

   @Override
   public boolean exists() {
      return true;
   }

   @Override
   public Path getPath() {
      return null;
   }

   @Override
   public Path getRoot() {
      return this.root;
   }

   @Override
   public URI getUri() {
      return this.uri;
   }

   @Override
   public String getRelativePath() {
      if (this.uri != null && this.uri.getPath() != null) {
         String path = this.uri.getPath();
         return path.startsWith("/") ? path.substring(1) : path;
      } else {
         return "";
      }
   }

   @Override
   public Resource resolve(String path) {
      if (path.startsWith("classpath:")) {
         return Resource.path(path);
      } else {
         try {
            URI baseUri = this.url.toURI();
            URI resolvedUri = baseUri.resolve(path);
            URL resolvedUrl = resolvedUri.toURL();
            return Resource.from(resolvedUrl, this.root);
         } catch (Exception var5) {
            return new PathResource(this.root.resolve(path), this.root);
         }
      }
   }

   public PathResource materialize(String filename) {
      try {
         Path target = this.root.resolve(filename);
         if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
         }

         Files.write(target, this.bytes);
         return new PathResource(target, this.root);
      } catch (Exception var3) {
         throw new RuntimeException("Failed to materialize resource to: " + filename, var3);
      }
   }

   @Override
   public InputStream getStream() {
      return new ByteArrayInputStream(this.bytes);
   }

   @Override
   public String toString() {
      return this.url != null ? this.url.toString() : this.getPrefixedPath();
   }
}
