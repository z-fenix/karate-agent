package io.karatelabs.http;

public class WsFrame {
   private final WsFrame.Type type;
   private final String text;
   private final byte[] bytes;
   private final int closeCode;
   private final String closeReason;

   private WsFrame(WsFrame.Type type, String text, byte[] bytes, int closeCode, String closeReason) {
      this.type = type;
      this.text = text;
      this.bytes = bytes;
      this.closeCode = closeCode;
      this.closeReason = closeReason;
   }

   public static WsFrame text(String content) {
      return new WsFrame(WsFrame.Type.TEXT, content, null, 0, null);
   }

   public static WsFrame binary(byte[] content) {
      return new WsFrame(WsFrame.Type.BINARY, null, content != null ? (byte[])content.clone() : null, 0, null);
   }

   public static WsFrame ping() {
      return new WsFrame(WsFrame.Type.PING, null, null, 0, null);
   }

   public static WsFrame pong() {
      return new WsFrame(WsFrame.Type.PONG, null, null, 0, null);
   }

   public static WsFrame close() {
      return new WsFrame(WsFrame.Type.CLOSE, null, null, 1000, null);
   }

   public static WsFrame close(int code, String reason) {
      return new WsFrame(WsFrame.Type.CLOSE, null, null, code, reason);
   }

   public WsFrame.Type getType() {
      return this.type;
   }

   public boolean isText() {
      return this.type == WsFrame.Type.TEXT;
   }

   public boolean isBinary() {
      return this.type == WsFrame.Type.BINARY;
   }

   public boolean isPing() {
      return this.type == WsFrame.Type.PING;
   }

   public boolean isPong() {
      return this.type == WsFrame.Type.PONG;
   }

   public boolean isClose() {
      return this.type == WsFrame.Type.CLOSE;
   }

   public String getText() {
      return this.text;
   }

   public byte[] getBytes() {
      return this.bytes != null ? (byte[])this.bytes.clone() : null;
   }

   public int getCloseCode() {
      return this.closeCode;
   }

   public String getCloseReason() {
      return this.closeReason;
   }

   @Override
   public String toString() {
      return switch (this.type) {
         case TEXT -> "WsFrame[TEXT: " + truncate(this.text, 100) + "]";
         case BINARY -> "WsFrame[BINARY: " + (this.bytes != null ? this.bytes.length : 0) + " bytes]";
         case PING -> "WsFrame[PING]";
         case PONG -> "WsFrame[PONG]";
         case CLOSE -> "WsFrame[CLOSE: " + this.closeCode + (this.closeReason != null ? " " + this.closeReason : "") + "]";
      };
   }

   private static String truncate(String s, int maxLen) {
      if (s == null) {
         return "null";
      } else {
         return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
      }
   }

   public static enum Type {
      TEXT,
      BINARY,
      PING,
      PONG,
      CLOSE;
   }
}
