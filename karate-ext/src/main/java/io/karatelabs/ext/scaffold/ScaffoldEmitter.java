package io.karatelabs.ext.scaffold;

import io.karatelabs.ext.contract.Contract;
import java.util.Map;

public interface ScaffoldEmitter {
   Map<String, String> emit(Contract var1, ScaffoldContext var2);
}
