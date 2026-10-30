package io.karatelabs.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathResource implements Resource {
   private final Path path;
   private final Path root;
   private final Path classpathRoot;
   private final boolean classpath;
   private final String relativePath;
   private final boolean overlay;
   private byte[] bytes;
   private String text;
   private String[] lines;

   public PathResource(Path path) {
      this(path, FileUtils.WORKING_DIR.toPath(), false);
   }

   public PathResource(Path path, Path root) {
      this(path, root, false);
   }

   public PathResource(Path path, Path root, boolean classpath) {
      this(path, root, classpath, null);
   }

   public PathResource(Path path, Path root, boolean classpath, Path classpathRoot) {
      this(path, root, classpath, classpathRoot, null);
   }

   PathResource(Path path, Path root, boolean classpath, Path classpathRoot, byte[] overlayBytes) {
      this.path = path.toAbsolutePath().normalize();
      this.root = root != null ? root.toAbsolutePath().normalize() : FileUtils.WORKING_DIR.toPath();
      this.classpathRoot = classpathRoot != null ? classpathRoot.toAbsolutePath().normalize() : this.root;
      this.classpath = classpath;
      this.relativePath = this.computeRelativePath();
      this.overlay = overlayBytes != null;
      this.bytes = overlayBytes;
   }

   public boolean isOverlay() {
      return this.overlay;
   }

   private String computeRelativePath() {
      try {
         if (this.root.getRoot() != null && this.path.getRoot() != null && !this.root.getRoot().equals(this.path.getRoot())) {
            return this.path.toString().replace('\\', '/');
         } else if (this.path.startsWith(this.root)) {
            return this.root.relativize(this.path).toString().replace('\\', '/');
         } else {
            Path cwd = FileUtils.WORKING_DIR.toPath().toAbsolutePath().normalize();
            return this.path.startsWith(cwd) ? cwd.relativize(this.path).toString().replace('\\', '/') : this.path.toString().replace('\\', '/');
         }
      } catch (Exception var2) {
         return this.path.toString().replace('\\', '/');
      }
   }

   @Override
   public boolean isFile() {
      return this.path != null;
   }

   @Override
   public boolean isClassPath() {
      return this.classpath;
   }

   @Override
   public boolean exists() {
      return this.overlay || Files.exists(this.path);
   }

   @Override
   public URI getUri() {
      return this.path.toUri();
   }

   @Override
   public Path getPath() {
      return this.path;
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
   public Resource resolve(String childPath) {
      if (childPath.startsWith("classpath:")) {
         return Resource.classpathWithRootFallback(childPath, this.root, this.classpathRoot);
      } else if (childPath.startsWith("file:")) {
         return Resource.path(childPath);
      } else if (OsUtils.isWindows() && childPath.length() > 1 && childPath.charAt(1) == ':') {
         return Resource.path(childPath);
      } else if (childPath.startsWith("/")) {
         Path resolved = this.root.resolve(Resource.stripLeadingSlashes(childPath));
         return new PathResource(resolved, this.root, this.classpath, this.classpathRoot);
      } else {
         Path base;
         if (Files.isDirectory(this.path)) {
            base = this.path;
         } else if (Files.exists(this.path)) {
            base = this.path.getParent();
         } else {
            String pathStr = this.path.toString();
            boolean hasExtension = pathStr.matches(".*\\.[a-zA-Z0-9]+$");
            base = hasExtension ? this.path.getParent() : this.path;
         }

         Path resolved = base.resolve(childPath);
         return new PathResource(resolved, this.root, this.classpath, this.classpathRoot);
      }
   }

   @Override
   public Resource getParent() {
      Path parentPath = this.path.getParent();
      return parentPath != null ? new PathResource(parentPath, this.root, this.classpath, this.classpathRoot) : null;
   }

   @Override
   public InputStream getStream() {
      if (this.overlay) {
         return new ByteArrayInputStream(this.bytes);
      } else {
         try {
            return Files.newInputStream(this.path);
         } catch (Exception var2) {
            throw new RuntimeException("Failed to open stream for: " + this.path, var2);
         }
      }
   }

   @Override
   public String getRelativePath() {
      return this.relativePath;
   }

   @Override
   public String getText() {
      if (this.text == null) {
         try {
            if (!this.overlay) {
               this.bytes = Files.readAllBytes(this.path);
            }

            this.text = FileUtils.toString(this.bytes);
         } catch (Exception var2) {
            throw new RuntimeException("Failed to read text from: " + this.path, var2);
         }
      }

      return this.text;
   }

   @Override
   public byte[] getBytes() {
      this.getText();
      return (byte[])this.bytes.clone();
   }

   @Override
   public String getLine(int index) {
      if (this.lines == null) {
         this.lines = this.getText().split("\\r?\\n");
      }

      return this.lines[index];
   }

   @Override
   public long getLastModified() {
      try {
         return Files.getLastModifiedTime(this.path).toMillis();
      } catch (Exception var2) {
         return 0L;
      }
   }

   @Override
   public String toString() {
      return this.relativePath;
   }
}
