package io.karatelabs.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public interface Resource {
   String CLASSPATH_COLON = "classpath:";
   String FILE_COLON = "file:";
   String THIS_COLON = "this:";

   boolean isFile();

   default boolean isInMemory() {
      return !this.isFile();
   }

   default boolean isLocalFile() {
      if (!this.isFile()) {
         return false;
      } else {
         try {
            URI uri = this.getUri();
            return uri != null && "file".equals(uri.getScheme());
         } catch (Exception var2) {
            return false;
         }
      }
   }

   default boolean isJarResource() {
      if (!this.isFile()) {
         return false;
      } else {
         try {
            URI uri = this.getUri();
            return uri != null && "jar".equals(uri.getScheme());
         } catch (Exception var2) {
            return false;
         }
      }
   }

   boolean isClassPath();

   boolean exists();

   URI getUri();

   Path getPath();

   Path getRoot();

   default Path getClasspathRoot() {
      return this.getRoot();
   }

   default String getRelativePathFrom(Path root) {
      if (this.isFile() && root != null) {
         try {
            Path thisPath = this.getPath();
            if (thisPath == null) {
               return null;
            } else {
               Path normalizedRoot = root.toAbsolutePath().normalize();
               Path normalizedThis = thisPath.toAbsolutePath().normalize();
               if (normalizedRoot.getRoot() != null && normalizedThis.getRoot() != null && !normalizedRoot.getRoot().equals(normalizedThis.getRoot())) {
                  return normalizedThis.toString().replace('\\', '/');
               } else {
                  String relativePath = normalizedRoot.relativize(normalizedThis).toString();
                  return relativePath.replace('\\', '/');
               }
            }
         } catch (Exception var6) {
            Path thisPathx = this.getPath();
            return thisPathx != null ? thisPathx.toAbsolutePath().normalize().toString().replace('\\', '/') : null;
         }
      } else {
         return null;
      }
   }

   Resource resolve(String var1);

   default Resource getParent() {
      return null;
   }

   InputStream getStream();

   default byte[] getBytes() {
      try {
         byte[] var2;
         try (InputStream is = this.getStream()) {
            var2 = FileUtils.toBytes(is);
         }

         return var2;
      } catch (IOException var6) {
         throw new RuntimeException(var6);
      }
   }

   String getRelativePath();

   String getText();

   String getLine(int var1);

   default int getLineOffset() {
      return 0;
   }

   default long getLastModified() {
      if (this.isFile()) {
         try {
            Path path = this.getPath();
            if (path != null) {
               return Files.getLastModifiedTime(path).toMillis();
            }
         } catch (Exception var3) {
         }
      }

      try {
         URI uri = this.getUri();
         if (uri != null) {
            return uri.toURL().openConnection().getLastModified();
         }
      } catch (Exception var2) {
      }

      return 0L;
   }

   default String getPackageQualifiedName() {
      String path = this.getRelativePath();
      if (path != null && !path.isEmpty()) {
         if (path.endsWith(".feature")) {
            path = path.substring(0, path.length() - 8);
         }

         if (path.charAt(0) == '/') {
            path = path.substring(1);
         }

         return path.replace('/', '.').replaceAll("\\.[.]+", ".");
      } else {
         return "";
      }
   }

   default String getExtension() {
      URI uri = this.getUri();
      String path = uri != null ? uri.getPath() : null;
      if (path == null) {
         path = this.getRelativePath();
      }

      if (path != null && !path.isEmpty()) {
         int pos = path.lastIndexOf(46);
         return pos != -1 && pos != path.length() - 1 ? path.substring(pos + 1) : "";
      } else {
         return "";
      }
   }

   default String getSimpleName() {
      URI uri = this.getUri();
      String path = uri != null ? uri.getPath() : null;
      if (path == null) {
         path = this.getRelativePath();
      }

      if (path != null && !path.isEmpty()) {
         if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
         }

         int pos = path.lastIndexOf(47);
         return pos == -1 ? path : path.substring(pos + 1);
      } else {
         return "";
      }
   }

   default String getFileNameWithoutExtension() {
      String path = this.getRelativePath();
      int pos = path.lastIndexOf(46);
      return pos == -1 ? path : path.substring(0, pos);
   }

   default String getPrefixedPath() {
      return this.isClassPath() ? "classpath:" + this.getRelativePath() : this.getRelativePath();
   }

   default String getRelativePathTo(Resource other) {
      if (this.isFile() && other.isFile()) {
         try {
            Path thisPath = this.getPath();
            Path otherPath = other.getPath();
            if (thisPath != null && otherPath != null) {
               thisPath = thisPath.toAbsolutePath().normalize();
               otherPath = otherPath.toAbsolutePath().normalize();
               if (thisPath.getRoot() != null && otherPath.getRoot() != null && !thisPath.getRoot().equals(otherPath.getRoot())) {
                  return otherPath.toString().replace('\\', '/');
               } else {
                  String relativePath = thisPath.relativize(otherPath).toString();
                  return relativePath.replace('\\', '/');
               }
            } else {
               return null;
            }
         } catch (Exception var5) {
            Path otherPathx = other.getPath();
            return otherPathx != null ? otherPathx.toAbsolutePath().normalize().toString().replace('\\', '/') : null;
         }
      } else {
         return null;
      }
   }

   static String normalizePath(String path) {
      if (path != null && path.contains("//")) {
         while (path.contains("//")) {
            path = path.replace("//", "/");
         }

         return path;
      } else {
         return path;
      }
   }

   static String removePrefix(String text) {
      return !text.startsWith("classpath:") && !text.startsWith("file:") ? text : text.substring(text.indexOf(58) + 1);
   }

   static String getParentPath(String relativePath) {
      int pos = relativePath.lastIndexOf(47);
      return pos == -1 ? "" : relativePath.substring(0, pos + 1);
   }

   static Path urlToPath(URL url, Path root) throws Exception {
      URI uri = url.toURI();
      if (!"jar".equals(uri.getScheme())) {
         return Path.of(uri);
      } else {
         try {
            return Path.of(uri);
         } catch (FileSystemNotFoundException var7) {
            try {
               FileSystems.newFileSystem(uri, Collections.emptyMap());
               return Path.of(uri);
            } catch (FileSystemAlreadyExistsException var5) {
               return Path.of(uri);
            } catch (ProviderNotFoundException var6) {
               throw new ProviderNotFoundException("JAR provider not available");
            }
         } catch (ProviderNotFoundException var8) {
            throw var8;
         }
      }
   }

   static String stripLeadingSlashes(String s) {
      if (s == null) {
         return null;
      } else {
         int i = 0;

         while (i < s.length() && s.charAt(i) == '/') {
            i++;
         }

         return s.substring(i);
      }
   }

   static Resource path(String path) {
      return path(path, (ClassLoader)null);
   }

   static Resource optional(String path) {
      if (path != null && path.startsWith("classpath:") && classpathUrl(path) == null) {
         return null;
      } else {
         try {
            return path(path, (ClassLoader)null);
         } catch (ResourceNotFoundException var2) {
            return null;
         }
      }
   }

   private static URL classpathUrl(String path) {
      String relativePath = normalizePath(removePrefix(path));
      if (relativePath.startsWith("/")) {
         relativePath = relativePath.substring(1);
      }

      ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
      URL url = contextCL == null ? null : contextCL.getResource(relativePath);
      return url != null ? url : ClassLoader.getSystemResource(relativePath);
   }

   static Resource path(String path, Path root) {
      return path(path, root, root);
   }

   static Resource path(String path, Path root, Path classpathRoot) {
      if (path == null) {
         path = "";
      }

      Path cpRoot = classpathRoot != null ? classpathRoot : root;
      if (path.startsWith("classpath:")) {
         return classpathWithRootFallback(path, root, cpRoot);
      } else if (root == null) {
         return path(path, (ClassLoader)null);
      } else {
         String normalized = normalizePath(path);
         if (normalized.startsWith("file:")) {
            return new PathResource(Path.of(removePrefix(normalized)), root, false, cpRoot);
         } else {
            Path asGiven = Path.of(normalized);
            return asGiven.isAbsolute() && !normalized.startsWith("/")
               ? new PathResource(asGiven, root, false, cpRoot)
               : new PathResource(root.resolve(stripLeadingSlashes(normalized)), root, false, cpRoot);
         }
      }
   }

   static Resource classpathWithRootFallback(String path, Path root) {
      return classpathWithRootFallback(path, root, root);
   }

   static Resource classpathWithRootFallback(String path, Path root, Path classpathRoot) {
      Path fallbackDir = classpathRoot != null ? classpathRoot : root;

      try {
         Resource hit = path(path, (ClassLoader)null);
         Path hitPath = root != null && hit.isFile() ? hit.getPath() : null;
         return (Resource)(hitPath != null && hitPath.getFileSystem() == FileSystems.getDefault() ? new PathResource(hitPath, root, true, fallbackDir) : hit);
      } catch (ResourceNotFoundException var6) {
         if (fallbackDir != null) {
            Path candidate = fallbackDir.resolve(stripLeadingSlashes(normalizePath(removePrefix(path))));
            if (Files.exists(candidate)) {
               return new PathResource(candidate, root != null ? root : fallbackDir, false, fallbackDir);
            }
         }

         throw var6;
      }
   }

   static Resource path(String path, ClassLoader classLoader) {
      if (path == null) {
         path = "";
      }

      if (!path.startsWith("classpath:")) {
         if (path.startsWith("file:")) {
            String filePath = normalizePath(removePrefix(path));
            return new PathResource(Path.of(filePath));
         } else {
            return new PathResource(Path.of(normalizePath(path)));
         }
      } else {
         String relativePath = removePrefix(path);
         relativePath = normalizePath(relativePath);
         if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
         }

         URL url = null;
         if (classLoader != null) {
            url = classLoader.getResource(relativePath);
         }

         if (url == null) {
            ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
            if (contextCL != null) {
               url = contextCL.getResource(relativePath);
            }
         }

         if (url == null) {
            url = ClassLoader.getSystemResource(relativePath);
         }

         if (url == null) {
            throw new ResourceNotFoundException(path);
         } else {
            try {
               Path resourcePath = urlToPath(url, null);
               return new PathResource(resourcePath, FileUtils.WORKING_DIR.toPath(), true);
            } catch (ProviderNotFoundException var11) {
               try {
                  MemoryResource var7;
                  try (InputStream is = url.openStream()) {
                     String content = FileUtils.toString(is);
                     var7 = new MemoryResource(content);
                  }

                  return var7;
               } catch (Exception var10) {
                  throw new RuntimeException("Failed to create resource from classpath: " + path, var10);
               }
            } catch (Exception var12) {
               throw new RuntimeException("Failed to create resource from classpath: " + path, var12);
            }
         }
      }
   }

   static List<Resource> scanClasspath(String classpathDir, String extension) {
      return scanClasspath(classpathDir, extension, null, null);
   }

   static List<Resource> scanClasspath(String classpathDir, String extension, ClassLoader classLoader) {
      return scanClasspath(classpathDir, extension, classLoader, null);
   }

   static List<Resource> scanClasspath(String classpathDir, String extension, ClassLoader classLoader, Path root) {
      Path effectiveRoot = root != null ? root : FileUtils.WORKING_DIR.toPath();
      List<Resource> results = new ArrayList<>();
      String normalizedDir = normalizePath(classpathDir);
      if (normalizedDir.startsWith("/")) {
         normalizedDir = normalizedDir.substring(1);
      }

      if (!normalizedDir.isEmpty() && !normalizedDir.endsWith("/")) {
         normalizedDir = normalizedDir + "/";
      }

      try {
         Enumeration<URL> urls = null;
         if (classLoader != null) {
            urls = classLoader.getResources(normalizedDir);
         }

         if (urls == null || !urls.hasMoreElements()) {
            ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
            if (contextCL != null) {
               urls = contextCL.getResources(normalizedDir);
            }
         }

         if (urls == null || !urls.hasMoreElements()) {
            urls = ClassLoader.getSystemResources(normalizedDir);
         }

         while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            scanUrl(url, normalizedDir, extension, results, effectiveRoot);
         }
      } catch (Exception var9) {
      }

      return results;
   }

   private static void scanUrl(URL url, String baseDir, String extension, List<Resource> results, Path root) {
      try {
         Path dirPath = urlToPath(url, null);
         if (dirPath != null && Files.isDirectory(dirPath)) {
            String suffix = "." + extension;

            try (Stream<Path> stream = Files.walk(dirPath)) {
               stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(suffix)).forEach(p -> results.add(new PathResource(p, root, true)));
            }
         }
      } catch (ProviderNotFoundException var12) {
         scanJarManually(url, baseDir, extension, results);
      } catch (Exception var13) {
      }
   }

   private static void scanJarManually(URL url, String baseDir, String extension, List<Resource> results) {
      String urlStr = url.toString();
      if (urlStr.startsWith("jar:")) {
         try {
            String jarPath = urlStr.substring(4);
            int bangIndex = jarPath.indexOf("!/");
            if (bangIndex == -1) {
               return;
            }

            String jarFilePath = jarPath.substring(0, bangIndex);
            if (jarFilePath.startsWith("file:")) {
               jarFilePath = jarFilePath.substring(5);
            }

            String suffix = "." + extension;

            try (JarFile jarFile = new JarFile(jarFilePath)) {
               Enumeration<JarEntry> entries = jarFile.entries();

               while (entries.hasMoreElements()) {
                  JarEntry entry = entries.nextElement();
                  String entryName = entry.getName();
                  if (!entry.isDirectory() && entryName.startsWith(baseDir) && entryName.endsWith(suffix)) {
                     try (InputStream is = jarFile.getInputStream(entry)) {
                        String content = FileUtils.toString(is);
                        results.add(new MemoryResource(content, entryName));
                     }
                  }
               }
            }
         } catch (Exception var20) {
         }
      }
   }

   static Resource text(String text) {
      return new MemoryResource(text);
   }

   static Resource bytes(byte[] bytes) {
      return new MemoryResource(bytes);
   }

   static Resource text(String text, Path root) {
      return new MemoryResource(text, root);
   }

   static Resource embedded(String text, Resource parent, int lineOffset) {
      if (parent == null) {
         return new MemoryResource(text);
      } else {
         String relativePath = parent.getRelativePath();
         if (relativePath == null || relativePath.isEmpty()) {
            relativePath = parent.getPrefixedPath();
         }

         return new MemoryResource(text, relativePath, lineOffset, parent.getRoot(), parent.getClasspathRoot());
      }
   }

   static Resource from(Path path) {
      return new PathResource(path);
   }

   static Resource from(Path path, Path root) {
      return new PathResource(path, root);
   }

   static Resource from(Path path, Path root, Path classpathRoot) {
      return new PathResource(path, root, false, classpathRoot);
   }

   static Resource overlay(Path path, byte[] bytes, Path root, Path classpathRoot) {
      return new PathResource(path, root, false, classpathRoot, (byte[])bytes.clone());
   }

   static Resource from(URL url) {
      return from(url, null);
   }

   static Resource from(URL url, Path root) {
      String protocol = url.getProtocol();
      if (!"http".equals(protocol) && !"https".equals(protocol)) {
         try {
            Path path = urlToPath(url, root);
            return root != null ? new PathResource(path, root) : new PathResource(path);
         } catch (ProviderNotFoundException var11) {
            try {
               MemoryResource var6;
               try (InputStream is = url.openStream()) {
                  String content = FileUtils.toString(is);
                  var6 = root != null ? new MemoryResource(content, root) : new MemoryResource(content);
               }

               return var6;
            } catch (Exception var10) {
               throw new RuntimeException("Failed to create resource from URL: " + url, var10);
            }
         } catch (Exception var12) {
            throw new RuntimeException("Failed to create resource from URL: " + url, var12);
         }
      } else {
         try {
            UrlResource content;
            try (InputStream is = url.openStream()) {
               byte[] bytes = FileUtils.toBytes(is);
               content = root != null ? new UrlResource(url, bytes, root) : new UrlResource(url, bytes);
            }

            return content;
         } catch (Exception var14) {
            throw new RuntimeException("Failed to fetch content from URL: " + url, var14);
         }
      }
   }
}
