package io.karatelabs.js;

import io.karatelabs.parser.Node;

public class Event {
   public final EventType type;
   public final Context context;
   public final Node node;
   public final Object value;

   Event(EventType type, Context context, Node node) {
      this(type, context, node, null);
   }

   Event(EventType type, Context context, Node node, Object value) {
      this.type = type;
      this.context = context;
      this.node = node;
      this.value = value;
   }
}
