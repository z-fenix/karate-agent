package io.karatelabs.ext.mock;

import java.util.List;
import java.util.Map;

public interface CallLog {
   List<Map<String, Object>> getCalls();

   void clear();

   default int clearAndCount() {
      int var1 = this.getCalls().size();
      this.clear();
      return var1;
   }
}
