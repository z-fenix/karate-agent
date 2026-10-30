package io.karatelabs.ext.agent.events;

import java.util.Map;

public record AgentRunContext(String chatId, String turnId, String previousRunId) {

   public AgentRunContext(String chatId, String turnId, String previousRunId) {
      this.chatId = chatId;
      this.turnId = turnId;
      this.previousRunId = previousRunId;
   }

   void addTo(Map<String, Object> var1) {
      var1.put("chatId", this.chatId);
      var1.put("turnId", this.turnId);
      if (this.previousRunId != null) {
         var1.put("prevRunId", this.previousRunId);
      }
   }
}
