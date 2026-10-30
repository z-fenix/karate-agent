package io.karatelabs.ext.contract;

public record GrpcBinding(String service, String method, String pkg, boolean clientStream, boolean serverStream) {}
