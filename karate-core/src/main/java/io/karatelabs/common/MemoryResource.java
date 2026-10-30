package io.karatelabs.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class MemoryResource implements Resource {
   private static final Path SYSTEM_TEMP = Path.of(System.getProperty("java.io.tmpdir"));
   private final Path root;
   private final Path classpathRoot;
   private final String relativePath;
   private final int lineOffset;
   private volatile byte[] bytes;
   private String text;
   private volatile String[] lines;

   MemoryResource(String text) {
      this(text, (Path)null);
   }

   MemoryResource(byte[] bytes) {
      this(bytes, (Path)null);
   }

   MemoryResource(String text, Path root) {
      this.root = root != null ? root : SYSTEM_TEMP;
      this.classpathRoot = this.root;
      this.text = text;
      this.relativePath = "";
      this.lineOffset = 0;
   }

   MemoryResource(byte[] bytes, Path root) {
      this.root = root != null ? root : SYSTEM_TEMP;
      this.classpathRoot = this.root;
      this.bytes = bytes;
      this.relativePath = "";
      this.lineOffset = 0;
   }

   MemoryResource(String text, String relativePath) {
      this(text, relativePath, 0, null, null);
   }

   MemoryResource(String text, String relativePath, int lineOffset, Path root, Path classpathRoot) {
      this.root = root != null ? root : SYSTEM_TEMP;
      this.classpathRoot = classpathRoot != null ? classpathRoot : this.root;
      this.text = text;
      this.relativePath = relativePath != null ? relativePath : "";
      this.lineOffset = lineOffset;
   }

   @Override
   public String getText() {
      if (this.text == null) {
         this.text = FileUtils.toString(this.bytes);
      }

      return this.text;
   }

   private byte[] bytes() {
      byte[] temp = this.bytes;
      if (temp == null) {
         temp = FileUtils.toBytes(this.text);
         this.bytes = temp;
      }

      return temp;
   }

   @Override
   public String getLine(int index) {
      String[] temp = this.lines;
      if (temp == null) {
         temp = this.getText().split("\\r?\\n");
         this.lines = temp;
      }

      int adjusted = index - this.lineOffset;
      return adjusted >= 0 && adjusted < temp.length ? temp[adjusted] : "";
   }

   @Override
   public int getLineOffset() {
      return this.lineOffset;
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
   public Path getClasspathRoot() {
      return this.classpathRoot;
   }

   @Override
   public URI getUri() {
      return null;
   }

   @Override
   public String getRelativePath() {
      return this.relativePath;
   }

   @Override
   public Resource resolve(String path) {
      if (path.startsWith("classpath:")) {
         return Resource.classpathWithRootFallback(path, this.root, this.classpathRoot);
      } else {
         return (Resource)(path.startsWith("file:")
            ? Resource.path(path)
            : new PathResource(this.root.resolve(Resource.stripLeadingSlashes(path)), this.root, false, this.classpathRoot));
      }
   }

   public PathResource materialize(String filename) {
      try {
         Path target = this.root.resolve(filename);
         if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
         }

         Files.write(target, this.bytes());
         return new PathResource(target, this.root);
      } catch (Exception var3) {
         throw new RuntimeException("Failed to materialize resource to: " + filename, var3);
      }
   }

   @Override
   public InputStream getStream() {
      return new ByteArrayInputStream(this.bytes());
   }

   @Override
   public String toString() {
      return this.getPrefixedPath();
   }
}
