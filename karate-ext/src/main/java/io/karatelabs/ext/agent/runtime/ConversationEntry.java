package io.karatelabs.ext.agent.runtime;

public record ConversationEntry(String userText, String assistantText, boolean pinned) {

   public ConversationEntry(String userText, String assistantText, boolean pinned) {
      this.userText = userText;
      this.assistantText = assistantText;
      this.pinned = pinned;
   }

   public static ConversationEntry of(String var0, String var1) {
      return new ConversationEntry(var0, var1, false);
   }

   public static ConversationEntry pinned(String var0, String var1) {
      return new ConversationEntry(var0, var1, true);
   }
}
