package io.karatelabs.js;

class JsSymbol extends JsObject {
   static final String[] WELL_KNOWN = new String[]{
      "iterator",
      "asyncIterator",
      "toPrimitive",
      "toStringTag",
      "hasInstance",
      "isConcatSpreadable",
      "species",
      "match",
      "matchAll",
      "replace",
      "search",
      "split",
      "unscopables"
   };
   final String wellKnownKey;
   private final String description;

   static String keyOf(String wellKnownName) {
      return "@@" + wellKnownName;
   }

   JsSymbol(String wellKnownKey) {
      this.wellKnownKey = wellKnownKey;
      this.description = wellKnownKey;
   }

   private JsSymbol(String description, boolean minted) {
      this.wellKnownKey = null;
      this.description = description;
   }

   static JsSymbol mint(String description) {
      return new JsSymbol(description, true);
   }

   static JsSymbol keyedBy(Object key) {
      return key instanceof JsSymbol s && s.wellKnownKey == null ? s : null;
   }

   @Override
   public String toString() {
      return this.wellKnownKey != null ? this.wellKnownKey : "Symbol(" + this.description + ")";
   }
}
