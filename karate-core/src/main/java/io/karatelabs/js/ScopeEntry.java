package io.karatelabs.js;

import io.karatelabs.parser.Node;

final class ScopeEntry {
   final int level;
   final ContextScope scope;
   final Node node;

   ScopeEntry(int level, ContextScope scope, Node node) {
      this.level = level;
      this.scope = scope;
      this.node = node;
   }

   @Override
   public String toString() {
      return this.scope + "@L" + this.level;
   }
}
