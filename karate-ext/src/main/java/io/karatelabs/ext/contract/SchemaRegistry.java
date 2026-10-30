package io.karatelabs.ext.contract;

import java.util.List;

public interface SchemaRegistry {
   List<String> subjects();

   TypeModel resolve(String var1, int var2);
}
