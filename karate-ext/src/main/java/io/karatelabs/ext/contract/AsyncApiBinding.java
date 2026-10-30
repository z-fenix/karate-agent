package io.karatelabs.ext.contract;

public record AsyncApiBinding(String channelId, String replyChannelId, String replyAddressLocation) {

   public AsyncApiBinding(String channelId, String replyChannelId, String replyAddressLocation) {
      this.channelId = channelId;
      this.replyChannelId = replyChannelId;
      this.replyAddressLocation = replyAddressLocation;
   }

   public static AsyncApiBinding of(String var0) {
      return new AsyncApiBinding(var0, null, null);
   }
}
