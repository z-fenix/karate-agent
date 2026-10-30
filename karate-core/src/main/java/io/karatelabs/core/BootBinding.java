package io.karatelabs.core;

import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootBinding {
   private static final Logger logger = LoggerFactory.getLogger(BootBinding.class);
   private final Suite suite;
   private final Path root;
   private final String env;
   private final Consumer<Ext> registrar;
   private final Set<Ext> exts = new LinkedHashSet<>();
   private String classpathDir;

   public BootBinding(Suite suite, Path root, String env, Consumer<Ext> registrar) {
      this.suite = suite;
      this.root = root;
      this.env = env;
      this.registrar = registrar == null ? e -> {} : registrar;
   }

   public String getEnv() {
      return this.env;
   }

   public String sysenv(String name) {
      return this.sysenv(name, null);
   }

   public String sysenv(String name, String defaultValue) {
      if (name == null) {
         return defaultValue;
      } else {
         String value = this.suite != null ? this.suite.getEnvVar(name) : System.getenv(name);
         return value != null && !value.isEmpty() ? value : defaultValue;
      }
   }

   public String sysprop(String name) {
      return this.sysprop(name, null);
   }

   public String sysprop(String name, String defaultValue) {
      if (name == null) {
         return defaultValue;
      } else {
         String value;
         if (this.suite != null) {
            Map<String, String> props = this.suite.getSystemProperties();
            value = props == null ? null : props.get(name);
         } else {
            value = System.getProperty(name);
         }

         return value != null && !value.isEmpty() ? value : defaultValue;
      }
   }

   public void classpath(String dir) {
      if (dir == null) {
         throw new IllegalArgumentException(
            "boot.classpath: dir is null — pass a project-relative directory such as 'src/test/resources', or '' for the project root"
         );
      } else if (!dir.startsWith("file:") && !dir.startsWith("classpath:") && !dir.startsWith("this:") && (dir.startsWith("/") || !Path.of(dir).isAbsolute())) {
         this.classpathDir = Resource.stripLeadingSlashes(dir);
      } else {
         throw new IllegalArgumentException(
            "boot.classpath('"
               + dir
               + "'): expected a directory RELATIVE to the project root (e.g. 'src/test/resources'), not an absolute or prefixed reference"
         );
      }
   }

   public String getClasspathDir() {
      return this.classpathDir;
   }

   public String read(String path) {
      if (path == null) {
         throw new IllegalArgumentException("boot.read: path is null");
      } else {
         Path classpathRoot = this.classpathDir != null && this.root != null ? this.root.resolve(this.classpathDir).normalize() : this.root;

         try {
            Resource r = Resource.path(path, this.root, classpathRoot);
            if (r.exists()) {
               return r.getText();
            } else {
               throw new RuntimeException(
                  "boot.read: file not found: "
                     + path
                     + " (resolved to "
                     + r
                     + "; root is "
                     + this.root
                     + " — a leading '/' anchors the project root, 'file:' is a host path)"
               );
            }
         } catch (ResourceNotFoundException var4) {
            throw new RuntimeException(
               "boot.read: file not found: "
                  + path
                  + " (root is "
                  + this.root
                  + " — a leading '/' anchors the project root, 'file:' is a host path; declare boot.classpath(dir) to map 'classpath:' refs)",
               var4
            );
         }
      }
   }

   public void log(Object msg) {
      logger.info("[boot] {}", msg == null ? "null" : msg.toString());
   }

   public Ext ext(String name) {
      if (name != null && !name.isBlank()) {
         for (Ext existing : this.exts) {
            if (name.equals(extShortName(existing))) {
               return existing;
            }
         }

         String className = extClassName(name);

         Ext ext;

         try {
            Class<?> cls = Class.forName(className);
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Ext booted)) {
               throw new RuntimeException("boot.ext('" + name + "'): " + className + " does not implement io.karatelabs.core.Ext");
            }

            ext = booted;
         } catch (ClassNotFoundException var6) {
            throw new RuntimeException("boot.ext('" + name + "'): not on classpath. Expected " + className + " (name-convention resolution).", var6);
         } catch (Exception var7) {
            throw new RuntimeException("boot.ext('" + name + "'): failed to construct " + className + " — " + var7.getMessage(), var7);
         }

         ext.onBoot(this.suite);
         this.exts.add(ext);
         this.registrar.accept(ext);
         logger.info("ext booted: {} ({})", name, ext.getClass().getName());
         return ext;
      } else {
         throw new IllegalArgumentException("boot.ext: name is null or empty");
      }
   }

   public boolean has(String name) {
      if (name != null && !name.isBlank()) {
         for (Ext existing : this.exts) {
            if (name.equals(extShortName(existing))) {
               return true;
            }
         }

         return extAvailable(name);
      } else {
         return false;
      }
   }

   static String extClassName(String name) {
      return "io.karatelabs.ext." + name + "." + capitalize(name) + "Ext";
   }

   static boolean extAvailable(String name) {
      if (name != null && !name.isBlank()) {
         try {
            Class.forName(extClassName(name));
            return true;
         } catch (ClassNotFoundException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public Set<Ext> getExts() {
      return this.exts;
   }

   public Map<String, Object> getGlobals() {
      return this.suite == null ? Collections.emptyMap() : this.suite.getGlobals();
   }

   public List<Map<String, Object>> manifests() {
      List<Map<String, Object>> out = new ArrayList<>();

      for (Ext e : this.exts) {
         Map<String, Object> entry = new LinkedHashMap<>();
         entry.put("name", extShortName(e));
         entry.put("class", e.getClass().getName());
         Map<String, Object> manifest = e.getManifest();
         if (manifest != null) {
            entry.putAll(manifest);
         }

         out.add(entry);
      }

      return out;
   }

   private static String extShortName(Ext e) {
      String pkg = e.getClass().getPackage().getName();
      int lastDot = pkg.lastIndexOf(46);
      return lastDot < 0 ? pkg : pkg.substring(lastDot + 1);
   }

   private static String capitalize(String s) {
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }
}
