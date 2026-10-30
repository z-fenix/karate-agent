package io.karatelabs.ext.agent.runtime;

import io.karatelabs.common.Resource;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

public record AgentArtifact(String component1, String e, String f, Resource g, URI h) {
   public static final String a = "image";
   public static final String b = "text";
   public static final String c = "log";
   public static final String d = "file";

   public AgentArtifact(String component1, String e, String f, Resource g, URI h) {
      this.component1 = component1;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
   }

   public static AgentArtifact a(byte[] var0, String var1, String var2) {
      return new AgentArtifact("image", var1, var2, Resource.bytes(var0), null);
   }

   public static AgentArtifact a(String var0, String var1) {
      return new AgentArtifact("text", "text/plain", var1, Resource.text(var0), null);
   }

   public static AgentArtifact b(String var0, String var1) {
      return new AgentArtifact("log", "text/plain", var1, Resource.text(var0), null);
   }

   public static AgentArtifact a(Path var0, String var1, String var2) {
      Resource var3 = Resource.from(var0);
      return new AgentArtifact("file", var1, var2, var3, var3.getUri());
   }

   public static AgentArtifact a(String var0, String var1, String var2) {
      return new AgentArtifact("file", var1, var2, null, URI.create(var0));
   }

   public static AgentArtifact a(String var0, Resource var1, String var2, String var3) {
      return new AgentArtifact(var0, var2, var3, var1, var1 != null ? var1.getUri() : null);
   }

   public String a() {
      return this.g != null ? this.g.getText() : null;
   }

   public byte[] b() {
      if (this.g == null) {
         return null;
      } else {
         try {
            byte[] var2;
            try (InputStream var1 = this.g.getStream()) {
               var2 = var1.readAllBytes();
            }

            return var2;
         } catch (Exception var6) {
            throw new RuntimeException("cannot read artifact bytes: " + var6.getMessage(), var6);
         }
      }
   }

   public URI c() {
      return this.h;
   }

   public String d() {
      return this.component1;
   }

   public String e() {
      return this.e;
   }

   public String f() {
      return this.f;
   }

   public Resource g() {
      return this.g;
   }

   public URI h() {
      return this.h;
   }
}
