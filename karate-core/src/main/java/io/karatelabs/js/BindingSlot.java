package io.karatelabs.js;

final class BindingSlot {
   final String name;
   Object value;
   BindScope scope;
   boolean initialized = true;
   int level;
   BindingSlot previous;
   short evalId;
   boolean hidden;
   byte attrs = 7;
   boolean attrsExplicit;
   boolean tombstoned;
   BindingSlot nextInLevel;
   boolean deferredPop;

   BindingSlot(String name) {
      this.name = name;
   }

   BindingSlot(String name, Object value) {
      this.name = name;
      this.value = value;
   }

   BindingSlot(String name, Object value, BindScope scope, boolean initialized) {
      this.name = name;
      this.value = value;
      this.scope = scope;
      this.initialized = initialized;
   }

   BindingSlot(String name, Object value, BindScope scope, boolean initialized, int level, BindingSlot previous) {
      this.name = name;
      this.value = value;
      this.scope = scope;
      this.initialized = initialized;
      this.level = level;
      this.previous = previous;
   }

   BindingSlot(BindingSlot other) {
      this.name = other.name;
      this.value = other.value;
      this.scope = other.scope;
      this.initialized = other.initialized;
      this.level = other.level;
      this.previous = other.previous;
      this.evalId = other.evalId;
      this.hidden = other.hidden;
      this.attrs = other.attrs;
      this.attrsExplicit = other.attrsExplicit;
      this.tombstoned = other.tombstoned;
   }

   @Override
   public String toString() {
      return this.name + "=" + this.value + (this.scope != null ? " (" + this.scope + ")" : "") + "@L" + this.level;
   }
}
