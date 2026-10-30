package io.karatelabs.ext.contract;

import java.util.List;

public record HttpBinding(String method, String pathTemplate, String baseUrl, List<String> consumes, List<String> produces) {}
