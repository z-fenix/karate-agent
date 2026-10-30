package io.karatelabs.ext.agent.runtime;

import java.util.List;

public record ToolResult(Object value, List<AgentArtifact> artifacts) {

   public ToolResult(Object value, List<AgentArtifact> artifacts) {
      artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
      this.value = value;
      this.artifacts = artifacts;
   }

   public ToolResult(Object var1) {
      this(var1, List.of());
   }

   public static ToolResult withArtifacts(Object var0, AgentArtifact... var1) {
      return new ToolResult(var0, List.of(var1));
   }

   public boolean hasArtifacts() {
      return !this.artifacts.isEmpty();
   }

   public Object value() {
      return this.value;
   }

   public List<AgentArtifact> artifacts() {
      return this.artifacts;
   }
}
