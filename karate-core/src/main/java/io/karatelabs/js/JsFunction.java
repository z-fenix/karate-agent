package io.karatelabs.js;

import java.util.List;

public abstract class JsFunction extends JsObject implements JavaCallable {
   String name;
   int length;
   private JsObject functionPrototype;
   private static final List<String> INTRINSIC_NAMES = List.of("prototype", "name", "length");

   protected JsFunction() {
      super(null, JsFunctionPrototype.INSTANCE);
   }

   @Override
   public boolean isExternal() {
      return false;
   }

   @Override
   public boolean isConstructable() {
      return true;
   }

   public String getSource() {
      return null;
   }

   @Override
   public String toString() {
      String source = this.getSource();
      if (source != null) {
         return source;
      } else {
         String fnName = this.name != null ? this.name : "";
         return "function " + fnName + "() { [native code] }";
      }
   }

   JsObject getFunctionPrototype() {
      if (this.functionPrototype == null) {
         this.functionPrototype = new JsObject();
         this.functionPrototype.putMember("constructor", this);
      }

      return this.functionPrototype;
   }

   @Override
   boolean isJsFunction() {
      return true;
   }

   @Override
   public byte getOwnAttrs(String name) {
      if (this.hasExplicitAttrs(name)) {
         return this.getAttrs(name);
      } else {
         return switch (name) {
            case "length", "name" -> 4;
            case "prototype" -> 1;
            case "constructor" -> 5;
            default -> super.getOwnAttrs(name);
         };
      }
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return switch (name) {
         case "prototype" -> this.getFunctionPrototype();
         case "name" -> this.name == null ? "" : this.name;
         case "length" -> this.length;
         default -> null;
      };
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }
}
