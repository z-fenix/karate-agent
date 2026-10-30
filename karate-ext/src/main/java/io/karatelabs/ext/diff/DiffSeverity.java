package io.karatelabs.ext.diff;

public enum DiffSeverity {
   BREAKING("breaking"),
   WARNING("warnings"),
   COMPATIBLE("compatible");

   private final String bucket;

   private DiffSeverity(String var3) {
      this.bucket = var3;
   }

   public String bucket() {
      return this.bucket;
   }
}
