package io.karatelabs.ext.contract;

import java.util.Map;

public interface TypeModel {
   Object sample();

   Map<String, Object> partitions(String var1);

   Map<String, Object> toMap();

   String kind();
}
