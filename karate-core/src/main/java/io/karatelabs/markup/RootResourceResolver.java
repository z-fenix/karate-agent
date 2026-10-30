package io.karatelabs.markup;

import io.karatelabs.common.Resource;

public class RootResourceResolver implements ResourceResolver {
   private final boolean classpath;
   final String root;

   public RootResourceResolver(String root) {
      if (root == null) {
         root = "";
      }

      this.classpath = root.startsWith("classpath:");
      root = Resource.removePrefix(root);
      String SLASH = "/";
      if (!root.isEmpty() && !root.endsWith(SLASH)) {
         root = root + SLASH;
      }

      this.root = root;
   }

   @Override
   public Resource resolve(String path, Resource caller) {
      if (path.startsWith("classpath:")) {
         return Resource.path(path);
      } else {
         String basePath = this.classpath ? "classpath:" + this.root : this.root;
         return path.startsWith("this:") && caller != null
            ? caller.resolve(path.substring("this:".length()))
            : Resource.path(basePath + (path.charAt(0) == '/' ? path.substring(1) : path));
      }
   }
}
