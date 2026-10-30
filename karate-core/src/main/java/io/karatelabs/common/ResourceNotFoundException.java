package io.karatelabs.common;

public class ResourceNotFoundException extends RuntimeException {
   private final String path;

   public ResourceNotFoundException(String path) {
      super("cannot find resource: " + path);
      this.path = path;
   }

   public String getPath() {
      return this.path;
   }
}
