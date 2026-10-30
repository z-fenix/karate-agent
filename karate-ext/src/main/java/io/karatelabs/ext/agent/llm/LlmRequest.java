package io.karatelabs.ext.agent.llm;

import java.util.List;

public record LlmRequest(String stage, String systemPrompt, List<LlmMessage> messages) {}
