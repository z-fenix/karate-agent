package io.karatelabs.driver;

public interface Dialog {
   String getMessage();

   String getType();

   String getDefaultPrompt();

   void accept();

   void accept(String var1);

   void dismiss();

   boolean isHandled();
}
