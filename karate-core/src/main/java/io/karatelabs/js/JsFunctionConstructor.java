package io.karatelabs.js;

import io.karatelabs.parser.ParserException;

class JsFunctionConstructor extends JsFunction {
   JsFunctionConstructor() {
      this.name = "Function";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("prototype", JsFunctionPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      StringBuilder src = new StringBuilder("(function anonymous(");
      if (args.length == 0) {
         src.append(") {\n})");
      } else {
         for (int i = 0; i < args.length - 1; i++) {
            if (i > 0) {
               src.append(',');
            }

            src.append(argToString(args[i]));
         }

         src.append("\n) {\n");
         src.append(argToString(args[args.length - 1]));
         src.append("\n})");
      }

      Engine engine = context.getEngine();
      if (engine == null) {
         throw JsErrorException.typeError("Function constructor unavailable: no engine");
      } else {
         try {
            return engine.evalRaw(src.toString());
         } catch (ParserException var6) {
            throw JsErrorException.syntaxError(var6.getMessage());
         }
      }
   }

   private static String argToString(Object arg) {
      return arg != null && arg != Terms.UNDEFINED ? arg.toString() : "";
   }
}
