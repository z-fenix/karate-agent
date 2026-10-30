package io.karatelabs.driver.w3c;

import io.karatelabs.driver.Keys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class W3cKeys implements Keys {
   private final W3cSession session;

   W3cKeys(W3cSession session) {
      this.session = session;
   }

   @Override
   public Keys type(String text) {
      if (text != null && !text.isEmpty()) {
         String activeElementId = this.findActiveElement();
         this.session.sendKeys(activeElementId, text);
         return this;
      } else {
         return this;
      }
   }

   @Override
   public Keys press(String key) {
      if (!key.contains("+")) {
         String resolved = resolveKeyName(key);
         this.performKeyActions(List.of(keyDown(resolved), keyUp(resolved)));
         return this;
      } else {
         String[] parts = key.split("\\+");
         String[] modifiers = new String[parts.length - 1];

         for (int i = 0; i < modifiers.length; i++) {
            modifiers[i] = resolveKeyName(parts[i]);
         }

         String mainKey = resolveKeyName(parts[parts.length - 1]);
         return this.combo(modifiers, mainKey);
      }
   }

   private static String resolveKeyName(String name) {
      return switch (name) {
         case "Control" -> "\ue009";
         case "Shift" -> "\ue008";
         case "Alt" -> "\ue00a";
         case "Meta", "Command" -> "\ue03d";
         case "Enter" -> "\ue007";
         case "Tab" -> "\ue004";
         case "Backspace" -> "\ue003";
         case "Delete" -> "\ue017";
         case "Escape" -> "\ue00c";
         case "ArrowUp" -> "\ue013";
         case "ArrowDown" -> "\ue015";
         case "ArrowLeft" -> "\ue012";
         case "ArrowRight" -> "\ue014";
         case "Home" -> "\ue011";
         case "End" -> "\ue010";
         case "PageUp" -> "\ue00e";
         case "PageDown" -> "\ue00f";
         case "Insert" -> "\ue016";
         case "Space" -> "\ue00d";
         default -> name;
      };
   }

   @Override
   public Keys down(String key) {
      this.performKeyActions(List.of(keyDown(key)));
      return this;
   }

   @Override
   public Keys up(String key) {
      this.performKeyActions(List.of(keyUp(key)));
      return this;
   }

   @Override
   public Keys combo(String[] modifierKeys, String key) {
      List<Map<String, Object>> actions = new ArrayList<>();

      for (String mod : modifierKeys) {
         actions.add(keyDown(mod));
      }

      actions.add(keyDown(key));
      actions.add(keyUp(key));

      for (int i = modifierKeys.length - 1; i >= 0; i--) {
         actions.add(keyUp(modifierKeys[i]));
      }

      this.performKeyActions(actions);
      return this;
   }

   @Override
   public Keys ctrl(String key) {
      return this.combo(new String[]{"\ue009"}, key);
   }

   @Override
   public Keys alt(String key) {
      return this.combo(new String[]{"\ue00a"}, key);
   }

   @Override
   public Keys shift(String key) {
      return this.combo(new String[]{"\ue008"}, key);
   }

   @Override
   public Keys meta(String key) {
      return this.combo(new String[]{"\ue03d"}, key);
   }

   private void performKeyActions(List<Map<String, Object>> keyActions) {
      Map<String, Object> keyboard = new HashMap<>();
      keyboard.put("type", "key");
      keyboard.put("id", "keyboard");
      keyboard.put("actions", keyActions);
      this.session.performActions(List.of(keyboard));
   }

   private static Map<String, Object> keyDown(String value) {
      return Map.of("type", "keyDown", "value", value);
   }

   private static Map<String, Object> keyUp(String value) {
      return Map.of("type", "keyUp", "value", value);
   }

   private String findActiveElement() {
      Object result = this.session.executeScript("return document.activeElement");
      if (W3cSession.isElementReference(result)) {
         return W3cSession.elementIdFrom(result);
      } else {
         throw new RuntimeException("Could not find active element for keyboard input");
      }
   }
}
