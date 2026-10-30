package io.karatelabs.match;

import io.karatelabs.js.Engine;

public class MatchContext {
   final Engine engine;
   final MarkerPolicy policy;
   final Operation root;
   final int depth;
   final boolean xml;
   final String path;
   final String name;
   final int index;

   MatchContext(Engine engine, MarkerPolicy policy, Operation root, boolean xml, int depth, String path, String name, int index) {
      this.engine = engine;
      this.policy = policy;
      this.root = root;
      this.xml = xml;
      this.depth = depth;
      this.path = path;
      this.name = name;
      this.index = index;
   }

   Engine engineFor(String marker) {
      return this.policy != null && !this.policy.trusted(marker) ? this.root.sandbox() : this.engine;
   }

   MatchContext descend(String name) {
      if (this.xml) {
         String childPath = this.path.endsWith("/@") ? this.path + name : (this.depth == 0 ? "" : this.path) + "/" + name;
         return new MatchContext(this.engine, this.policy, this.root, this.xml, this.depth + 1, childPath, name, -1);
      } else {
         boolean needsQuotes = name.indexOf(45) != -1 || name.indexOf(32) != -1 || name.indexOf(46) != -1;
         String childPath = needsQuotes ? this.path + "['" + name + "']" : this.path + "." + name;
         return new MatchContext(this.engine, this.policy, this.root, this.xml, this.depth + 1, childPath, name, -1);
      }
   }

   MatchContext descend(int index) {
      return this.xml
         ? new MatchContext(this.engine, this.policy, this.root, this.xml, this.depth + 1, this.path + "[" + (index + 1) + "]", this.name, index)
         : new MatchContext(this.engine, this.policy, this.root, this.xml, this.depth + 1, this.path + "[" + index + "]", this.name, index);
   }
}
