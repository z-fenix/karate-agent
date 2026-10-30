package io.karatelabs.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathSecurity {
   public static boolean isSafe(String path) {
      if (path == null) {
         return true;
      } else if (containsTraversal(path)) {
         return false;
      } else {
         try {
            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            if (!decoded.equals(path) && containsTraversal(decoded)) {
               return false;
            } else {
               String doubleDec = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
               return doubleDec.equals(decoded) || !containsTraversal(doubleDec);
            }
         } catch (IllegalArgumentException var3) {
            return false;
         }
      }
   }

   private static boolean containsTraversal(String path) {
      String normalized = path.replace('\\', '/');
      if (normalized.contains("../") || normalized.contains("/..")) {
         return true;
      } else {
         return normalized.equals("..") || normalized.endsWith("/..") ? true : normalized.startsWith("..");
      }
   }

   public static void validate(String path) throws PathSecurity.PathTraversalException {
      if (!isSafe(path)) {
         throw new PathSecurity.PathTraversalException(path);
      }
   }

   public static class PathTraversalException extends RuntimeException {
      private final String path;

      public PathTraversalException(String path) {
         super("Path traversal detected: " + path);
         this.path = path;
      }

      public String getPath() {
         return this.path;
      }
   }
}
