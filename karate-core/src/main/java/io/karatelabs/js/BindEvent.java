package io.karatelabs.js;

import io.karatelabs.parser.Node;

public class BindEvent {
   public final BindType type;
   public final String name;
   public final Object value;
   public final Object oldValue;
   public final BindScope scope;
   public final Object target;
   public final Context context;
   public final Node node;

   BindEvent(BindType type, String name, Object value, Object oldValue, BindScope scope, Object target, Context context, Node node) {
      this.type = type;
      this.name = name;
      this.value = value;
      this.oldValue = oldValue;
      this.scope = scope;
      this.target = target;
      this.context = context;
      this.node = node;
   }

   static BindEvent declare(String name, Object value, BindScope scope, Context context, Node node) {
      return new BindEvent(BindType.DECLARE, name, value, null, scope, null, context, node);
   }

   static BindEvent assign(String name, Object value, Object oldValue, Context context, Node node) {
      return new BindEvent(BindType.ASSIGN, name, value, oldValue, null, null, context, node);
   }

   static BindEvent delete(String name, Object oldValue, Context context, Node node) {
      return new BindEvent(BindType.DELETE, name, null, oldValue, null, null, context, node);
   }

   static BindEvent propertySet(String name, Object value, Object oldValue, Object target, Context context, Node node) {
      return new BindEvent(BindType.PROPERTY_SET, name, value, oldValue, null, target, context, node);
   }

   static BindEvent propertyDelete(String name, Object oldValue, Object target, Context context, Node node) {
      return new BindEvent(BindType.PROPERTY_DELETE, name, null, oldValue, null, target, context, node);
   }
}
