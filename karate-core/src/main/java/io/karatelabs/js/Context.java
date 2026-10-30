package io.karatelabs.js;

import io.karatelabs.parser.Node;
import java.util.Collections;
import java.util.Map;

public interface Context {
   Engine getEngine();

   Context getParent();

   ContextScope getScope();

   int getDepth();

   Node getNode();

   String getPath();

   int getIteration();

   Object getThisObject();

   CallInfo getCallInfo();

   Object[] getCallArgs();

   Object getReturnValue();

   Object getErrorThrown();

   ExitType getExitType();

   default Map<String, Object> getLocalVariables() {
      return Collections.emptyMap();
   }

   default Object evalWithLocals(String expression) {
      return this.getEngine().eval(expression);
   }
}
