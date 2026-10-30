package io.karatelabs.js;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.Token;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class JsError extends JsObject {
   private final Throwable javaCause;
   private static final int MAX_FRAMES = 10;
   private static final List<String> INTRINSIC_NAMES = List.of("stack");
   private String frames;
   private Object stack;
   private boolean stackResolved;

   JsError(JsErrorPrototype prototype) {
      super(null, prototype);
      this.javaCause = null;
   }

   JsError(JsErrorPrototype prototype, Throwable javaCause) {
      super(null, prototype);
      this.javaCause = javaCause;
   }

   Throwable getJavaCause() {
      return this.javaCause;
   }

   void captureStack(CoreContext context) {
      if (this.frames == null && !this.stackResolved) {
         StringBuilder sb = new StringBuilder();
         int count = 0;

         for (CoreContext c = context; c != null && count < 10; c = c.parent) {
            Node node = c.node;
            if (node != null) {
               Token token = node.getFirstToken();
               if (token != null && token != Token.EMPTY) {
                  String source = token.getResource().isFile() ? token.getResource().getRelativePath() : "<eval>";
                  sb.append("\n    at ").append(source).append(':').append(token.getPositionDisplay());
                  count++;
               }
            }
         }

         this.frames = sb.toString();
      }
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      if (!"stack".equals(name)) {
         return null;
      } else {
         if (!this.stackResolved) {
            this.stack = this.toString() + (this.frames == null ? "" : this.frames);
            this.stackResolved = true;
         }

         return this.stack;
      }
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   @Override
   public byte getOwnAttrs(String name) {
      return "stack".equals(name) && !this.hasExplicitAttrs(name) ? 5 : super.getOwnAttrs(name);
   }

   @Override
   public void putMember(String name, Object value, CoreContext ctx, boolean strict) {
      if ("stack".equals(name)) {
         this.putMember(name, value);
      } else {
         super.putMember(name, value, ctx, strict);
      }
   }

   @Override
   public void putMember(String name, Object value) {
      if ("stack".equals(name)) {
         this.stack = value;
         this.stackResolved = true;
      } else {
         super.putMember(name, value);
      }
   }

   @Override
   public String toString() {
      Object nameVal = this.getMember("name");
      String name = nameVal != null && nameVal != Terms.UNDEFINED ? nameVal.toString() : "Error";
      Object msgVal = this.getMember("message");
      String msg = msgVal != null && msgVal != Terms.UNDEFINED ? msgVal.toString() : "";
      return msg.isEmpty() ? name : name + ": " + msg;
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("name", this.getMember("name"));
      Object msg = this.getMember("message");
      if (msg != null && msg != Terms.UNDEFINED) {
         result.put("message", msg);
      }

      Map<String, Object> own = super.toMap();
      if (own != null && !own.isEmpty()) {
         for (Entry<String, Object> e : own.entrySet()) {
            if (!result.containsKey(e.getKey())) {
               result.put(e.getKey(), e.getValue());
            }
         }
      }

      return result;
   }
}
