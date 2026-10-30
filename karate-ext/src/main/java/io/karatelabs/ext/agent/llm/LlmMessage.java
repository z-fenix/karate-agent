package io.karatelabs.ext.agent.llm;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.UnaryOperator;

public class LlmMessage {
   private final io.karatelabs.ext.agent.llm.LlmMessage.Role role;
   private final List<io.karatelabs.ext.agent.llm.LlmMessage.Part> parts;

   private LlmMessage(io.karatelabs.ext.agent.llm.LlmMessage.Role var1, List<io.karatelabs.ext.agent.llm.LlmMessage.Part> var2) {
      this.role = var1;
      this.parts = var2;
   }

   public io.karatelabs.ext.agent.llm.LlmMessage.Role role() {
      return this.role;
   }

   public List<io.karatelabs.ext.agent.llm.LlmMessage.Part> parts() {
      return this.parts;
   }

   public String text() {
      StringBuilder var1 = new StringBuilder();

      for (io.karatelabs.ext.agent.llm.LlmMessage.Part var3 : this.parts) {
         if (var3.isText()) {
            if (var1.length() > 0) {
               var1.append("\n");
            }

            var1.append(var3.text());
         }
      }

      return var1.toString();
   }

   public boolean hasImages() {
      return this.parts.stream().anyMatch(io.karatelabs.ext.agent.llm.LlmMessage.Part::isImage);
   }

   public boolean isTextOnly() {
      return this.parts.stream().allMatch(io.karatelabs.ext.agent.llm.LlmMessage.Part::isText);
   }

   public LlmMessage mapText(UnaryOperator<String> var1) {
      ArrayList var2 = new ArrayList(this.parts.size());

      for (io.karatelabs.ext.agent.llm.LlmMessage.Part var4 : this.parts) {
         var2.add(var4.isText() ? io.karatelabs.ext.agent.llm.LlmMessage.Part.text(var1.apply(var4.text())) : var4);
      }

      return new LlmMessage(this.role, var2);
   }

   public static LlmMessage system(String var0) {
      return new LlmMessage(io.karatelabs.ext.agent.llm.LlmMessage.Role.SYSTEM, List.of(io.karatelabs.ext.agent.llm.LlmMessage.Part.text(var0)));
   }

   public static LlmMessage user(String var0) {
      return new LlmMessage(io.karatelabs.ext.agent.llm.LlmMessage.Role.USER, List.of(io.karatelabs.ext.agent.llm.LlmMessage.Part.text(var0)));
   }

   public static LlmMessage assistant(String var0) {
      return new LlmMessage(io.karatelabs.ext.agent.llm.LlmMessage.Role.ASSISTANT, List.of(io.karatelabs.ext.agent.llm.LlmMessage.Part.text(var0)));
   }

   public static io.karatelabs.ext.agent.llm.LlmMessage.Builder userBuilder() {
      return new io.karatelabs.ext.agent.llm.LlmMessage.Builder(io.karatelabs.ext.agent.llm.LlmMessage.Role.USER);
   }

   public static io.karatelabs.ext.agent.llm.LlmMessage.Builder assistantBuilder() {
      return new io.karatelabs.ext.agent.llm.LlmMessage.Builder(io.karatelabs.ext.agent.llm.LlmMessage.Role.ASSISTANT);
   }

   public static class Builder {
      private final io.karatelabs.ext.agent.llm.LlmMessage.Role role;
      private final List<io.karatelabs.ext.agent.llm.LlmMessage.Part> parts = new ArrayList<>();

      private Builder(io.karatelabs.ext.agent.llm.LlmMessage.Role var1) {
         this.role = var1;
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Builder text(String var1) {
         this.parts.add(io.karatelabs.ext.agent.llm.LlmMessage.Part.text(var1));
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Builder image(byte[] var1, String var2) {
         this.parts.add(io.karatelabs.ext.agent.llm.LlmMessage.Part.image(var1, var2));
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Builder imageBase64(String var1, String var2) {
         this.parts.add(io.karatelabs.ext.agent.llm.LlmMessage.Part.imageBase64(var1, var2));
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Builder png(byte[] var1) {
         return this.image(var1, "image/png");
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Builder jpeg(byte[] var1) {
         return this.image(var1, "image/jpeg");
      }

      public LlmMessage build() {
         return new LlmMessage(this.role, new ArrayList<>(this.parts));
      }
   }

   public static class Part {
      private final io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind kind;
      private final String text;
      private final String base64;
      private final String mediaType;

      private Part(io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind var1, String var2, String var3, String var4) {
         this.kind = var1;
         this.text = var2;
         this.base64 = var3;
         this.mediaType = var4;
      }

      public static io.karatelabs.ext.agent.llm.LlmMessage.Part text(String var0) {
         return new io.karatelabs.ext.agent.llm.LlmMessage.Part(io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind.TEXT, var0, null, null);
      }

      public static io.karatelabs.ext.agent.llm.LlmMessage.Part image(byte[] var0, String var1) {
         String var2 = Base64.getEncoder().encodeToString(var0);
         return new io.karatelabs.ext.agent.llm.LlmMessage.Part(io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind.IMAGE, null, var2, var1);
      }

      public static io.karatelabs.ext.agent.llm.LlmMessage.Part imageBase64(String var0, String var1) {
         return new io.karatelabs.ext.agent.llm.LlmMessage.Part(io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind.IMAGE, null, var0, var1);
      }

      public io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind kind() {
         return this.kind;
      }

      public String text() {
         return this.text;
      }

      public String base64() {
         return this.base64;
      }

      public String mediaType() {
         return this.mediaType;
      }

      public boolean isText() {
         return this.kind == io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind.TEXT;
      }

      public boolean isImage() {
         return this.kind == io.karatelabs.ext.agent.llm.LlmMessage.Part.Kind.IMAGE;
      }

      public static enum Kind {
         TEXT,
         IMAGE;
      }
   }

   public static enum Role {
      SYSTEM("system"),
      USER("user"),
      ASSISTANT("assistant");

      private final String value;

      private Role(String var3) {
         this.value = var3;
      }

      public String getValue() {
         return this.value;
      }
   }
}
