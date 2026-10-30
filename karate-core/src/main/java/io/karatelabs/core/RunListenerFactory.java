package io.karatelabs.core;

@FunctionalInterface
public interface RunListenerFactory {
   RunListener create();
}
