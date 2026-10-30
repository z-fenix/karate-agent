package io.karatelabs.ext.contract;

public interface TypeModelResolver {
   String format();

   TypeModel resolve(String var1);
}
