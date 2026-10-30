package io.karatelabs.driver.cdp;

import io.karatelabs.driver.Dialog;
import io.karatelabs.driver.DriverException;

public class CdpDialog implements Dialog {
   private final CdpClient cdp;
   private final String message;
   private final String type;
   private final String defaultPrompt;
   private volatile boolean handled = false;

   CdpDialog(CdpClient cdp, String message, String type, String defaultPrompt) {
      this.cdp = cdp;
      this.message = message;
      this.type = type;
      this.defaultPrompt = defaultPrompt;
   }

   @Override
   public String getMessage() {
      return this.message;
   }

   @Override
   public String getType() {
      return this.type;
   }

   @Override
   public String getDefaultPrompt() {
      return this.defaultPrompt;
   }

   @Override
   public void accept() {
      this.accept(null);
   }

   @Override
   public void accept(String promptText) {
      if (!this.handled) {
         this.handled = true;
         CdpMessage message = this.cdp.method("Page.handleJavaScriptDialog").param("accept", true);
         if (promptText != null) {
            message.param("promptText", promptText);
         }

         CdpResponse response = message.send();
         if (response.isError()) {
            throw new DriverException("dialog accept failed: " + response.getError());
         }
      }
   }

   @Override
   public void dismiss() {
      if (!this.handled) {
         this.handled = true;
         CdpResponse response = this.cdp.method("Page.handleJavaScriptDialog").param("accept", false).send();
         if (response.isError()) {
            throw new DriverException("dialog dismiss failed: " + response.getError());
         }
      }
   }

   @Override
   public boolean isHandled() {
      return this.handled;
   }

   @Override
   public String toString() {
      return "Dialog{type='" + this.type + "', message='" + this.message + "'}";
   }
}
