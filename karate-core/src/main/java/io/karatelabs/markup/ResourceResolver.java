package io.karatelabs.markup;

import io.karatelabs.common.Resource;

@FunctionalInterface
public interface ResourceResolver {
   Resource resolve(String var1, Resource var2);
}
