package io.karatelabs.js;

public class JsFunctionWrapper extends JsFunction {
   private final JsFunction delegate;

   public JsFunctionWrapper(JsFunction delegate) {
      this.delegate = delegate;
   }

   @Override
   public Object call(Context ctx, Object[] args) {
      Object result = this.delegate.call(ctx, args);
      return Engine.toJava(result);
   }

   @Override
   public String getSource() {
      return this.delegate.getSource();
   }

   @Override
   public Object getMember(String name) {
      return this.delegate.getMember(name);
   }

   @Override
   public Object getMember(String name, Object receiver, CoreContext ctx) {
      return this.delegate.getMember(name, receiver, ctx);
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return this.delegate.resolveOwnIntrinsic(name);
   }

   @Override
   public void putMember(String name, Object value) {
      this.delegate.putMember(name, value);
   }
}
