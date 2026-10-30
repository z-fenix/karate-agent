package io.karatelabs.driver.cdp;

import io.karatelabs.driver.Keys;

public class CdpKeys implements Keys {
   private final CdpClient cdp;
   private int modifiers = 0;
   private static final int ALT_FLAG = 1;
   private static final int CTRL_FLAG = 2;
   private static final int META_FLAG = 4;
   private static final int SHIFT_FLAG = 8;

   public CdpKeys(CdpClient cdp) {
      this.cdp = cdp;
   }

   @Override
   public Keys type(String text) {
      for (char c : text.toCharArray()) {
         String s = String.valueOf(c);
         if (this.isSpecialKey(s)) {
            this.pressSpecialKey(s);
         } else {
            this.typeChar(c);
         }
      }

      return this;
   }

   @Override
   public Keys press(String key) {
      if (key.contains("+")) {
         return this.pressPlusNotation(key);
      } else {
         if (this.isSpecialKey(key)) {
            this.pressSpecialKey(key);
         } else {
            for (char c : key.toCharArray()) {
               this.typeChar(c);
            }
         }

         return this;
      }
   }

   private Keys pressPlusNotation(String combo) {
      String[] parts = combo.split("\\+");
      if (parts.length < 2) {
         return this.press(parts[0]);
      } else {
         String[] modifiers = new String[parts.length - 1];

         for (int i = 0; i < parts.length - 1; i++) {
            modifiers[i] = this.resolveKeyName(parts[i]);
         }

         String finalKey = this.resolveKeyName(parts[parts.length - 1]);
         return this.combo(modifiers, finalKey);
      }
   }

   private String resolveKeyName(String name) {
      return switch (name) {
         case "Control", "Ctrl" -> "\ue009";
         case "Shift" -> "\ue008";
         case "Alt" -> "\ue00a";
         case "Meta", "Command", "Cmd" -> "\ue03d";
         case "ArrowLeft", "Left" -> "\ue012";
         case "ArrowRight", "Right" -> "\ue014";
         case "ArrowUp", "Up" -> "\ue013";
         case "ArrowDown", "Down" -> "\ue015";
         case "Enter" -> "\ue007";
         case "Tab" -> "\ue004";
         case "Backspace" -> "\ue003";
         case "Delete" -> "\ue017";
         case "Escape", "Esc" -> "\ue00c";
         case "Home" -> "\ue011";
         case "End" -> "\ue010";
         case "PageUp" -> "\ue00e";
         case "PageDown" -> "\ue00f";
         case "Insert" -> "\ue016";
         case "Space" -> "\ue00d";
         case "F1" -> "\ue031";
         case "F2" -> "\ue032";
         case "F3" -> "\ue033";
         case "F4" -> "\ue034";
         case "F5" -> "\ue035";
         case "F6" -> "\ue036";
         case "F7" -> "\ue037";
         case "F8" -> "\ue038";
         case "F9" -> "\ue039";
         case "F10" -> "\ue03a";
         case "F11" -> "\ue03b";
         case "F12" -> "\ue03c";
         default -> name;
      };
   }

   @Override
   public Keys down(String key) {
      int flag = this.getModifierFlag(key);
      if (flag > 0) {
         this.modifiers |= flag;
      }

      CdpKeys.KeyInfo info = this.isSpecialKey(key) ? this.getKeyInfo(key) : this.getKeyInfoForChar(key.charAt(0));
      this.dispatchRawKeyDown(info);
      return this;
   }

   @Override
   public Keys up(String key) {
      int flag = this.getModifierFlag(key);
      if (flag > 0) {
         this.modifiers &= ~flag;
      }

      CdpKeys.KeyInfo info = this.isSpecialKey(key) ? this.getKeyInfo(key) : this.getKeyInfoForChar(key.charAt(0));
      this.dispatchKeyUp(info);
      return this;
   }

   @Override
   public Keys combo(String[] modifierKeys, String key) {
      for (String mod : modifierKeys) {
         this.down(mod);
      }

      this.press(key);

      for (int i = modifierKeys.length - 1; i >= 0; i--) {
         this.up(modifierKeys[i]);
      }

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

   private void typeChar(char c) {
      boolean shiftHeld = (this.modifiers & 8) != 0;
      char effectiveChar = shiftHeld ? this.applyShift(c) : c;
      CdpKeys.KeyInfo info = this.getKeyInfoForChar(effectiveChar);
      if (shiftHeld && c >= 'a' && c <= 'z') {
         info.code = "Key" + Character.toUpperCase(c);
      }

      this.dispatchRawKeyDown(info);
      if ((this.modifiers & -9) == 0) {
         this.cdp.method("Input.insertText").param("text", String.valueOf(effectiveChar)).send();
      }

      this.dispatchKeyUp(info);
   }

   private char applyShift(char c) {
      if (c >= 'a' && c <= 'z') {
         return Character.toUpperCase(c);
      } else {
         return switch (c) {
            case '\'' -> '"';
            default -> c;
            case ',' -> '<';
            case '-' -> '_';
            case '.' -> '>';
            case '/' -> '?';
            case '0' -> ')';
            case '1' -> '!';
            case '2' -> '@';
            case '3' -> '#';
            case '4' -> '$';
            case '5' -> '%';
            case '6' -> '^';
            case '7' -> '&';
            case '8' -> '*';
            case '9' -> '(';
            case ';' -> ':';
            case '=' -> '+';
            case '[' -> '{';
            case '\\' -> '|';
            case ']' -> '}';
            case '`' -> '~';
         };
      }
   }

   private void pressSpecialKey(String key) {
      CdpKeys.KeyInfo info = this.getKeyInfo(key);
      this.dispatchRawKeyDown(info);
      if (info.text != null && !info.text.isEmpty()) {
         this.dispatchChar(info);
      }

      this.dispatchKeyUp(info);
   }

   private void dispatchRawKeyDown(CdpKeys.KeyInfo info) {
      CdpMessage message = this.cdp.method("Input.dispatchKeyEvent").param("type", "rawKeyDown").param("modifiers", this.modifiers);
      if (info.key != null) {
         message.param("key", info.key);
      }

      if (info.code != null) {
         message.param("code", info.code);
      }

      if (info.text != null) {
         message.param("text", info.text);
      }

      if (info.windowsVirtualKeyCode > 0) {
         message.param("windowsVirtualKeyCode", info.windowsVirtualKeyCode);
      }

      if (info.nativeVirtualKeyCode > 0) {
         message.param("nativeVirtualKeyCode", info.nativeVirtualKeyCode);
      }

      if (info.location > 0) {
         message.param("location", info.location);
      }

      message.send();
   }

   private void dispatchKeyUp(CdpKeys.KeyInfo info) {
      CdpMessage message = this.cdp.method("Input.dispatchKeyEvent").param("type", "keyUp").param("modifiers", this.modifiers);
      if (info.key != null) {
         message.param("key", info.key);
      }

      if (info.code != null) {
         message.param("code", info.code);
      }

      if (info.windowsVirtualKeyCode > 0) {
         message.param("windowsVirtualKeyCode", info.windowsVirtualKeyCode);
      }

      if (info.nativeVirtualKeyCode > 0) {
         message.param("nativeVirtualKeyCode", info.nativeVirtualKeyCode);
      }

      if (info.location > 0) {
         message.param("location", info.location);
      }

      message.send();
   }

   private void dispatchChar(CdpKeys.KeyInfo info) {
      if (info.text != null && !info.text.isEmpty()) {
         CdpMessage message = this.cdp.method("Input.dispatchKeyEvent").param("type", "char").param("modifiers", this.modifiers).param("text", info.text);
         if (info.windowsVirtualKeyCode > 0) {
            message.param("windowsVirtualKeyCode", info.windowsVirtualKeyCode);
         }

         message.send();
      }
   }

   private boolean isSpecialKey(String key) {
      if (key != null && !key.isEmpty()) {
         char c = key.charAt(0);
         return c >= '\ue000' && c <= '\ue03d';
      } else {
         return false;
      }
   }

   private int getModifierFlag(String key) {
      if (key != null && !key.isEmpty()) {
         return switch (key) {
            case "\ue008" -> 8;
            case "\ue009" -> 2;
            case "\ue00a" -> 1;
            case "\ue03d" -> 4;
            default -> 0;
         };
      } else {
         return 0;
      }
   }

   private CdpKeys.KeyInfo getKeyInfoForChar(char c) {
      CdpKeys.KeyInfo info = new CdpKeys.KeyInfo();
      info.text = String.valueOf(c);
      info.key = info.text;
      if (c >= 'a' && c <= 'z') {
         info.code = "Key" + Character.toUpperCase(c);
         info.windowsVirtualKeyCode = Character.toUpperCase(c);
      } else if (c >= 'A' && c <= 'Z') {
         info.code = "Key" + c;
         info.windowsVirtualKeyCode = c;
      } else if (c >= '0' && c <= '9') {
         info.code = "Digit" + c;
         info.windowsVirtualKeyCode = c;
      } else if (c == ' ') {
         info.key = " ";
         info.code = "Space";
         info.windowsVirtualKeyCode = 32;
      } else {
         switch (c) {
            case '\'':
               info.code = "Quote";
               info.windowsVirtualKeyCode = 222;
               break;
            case ',':
               info.code = "Comma";
               info.windowsVirtualKeyCode = 188;
               break;
            case '-':
               info.code = "Minus";
               info.windowsVirtualKeyCode = 189;
               break;
            case '.':
               info.code = "Period";
               info.windowsVirtualKeyCode = 190;
               break;
            case '/':
               info.code = "Slash";
               info.windowsVirtualKeyCode = 191;
               break;
            case ';':
               info.code = "Semicolon";
               info.windowsVirtualKeyCode = 186;
               break;
            case '=':
               info.code = "Equal";
               info.windowsVirtualKeyCode = 187;
               break;
            case '[':
               info.code = "BracketLeft";
               info.windowsVirtualKeyCode = 219;
               break;
            case '\\':
               info.code = "Backslash";
               info.windowsVirtualKeyCode = 220;
               break;
            case ']':
               info.code = "BracketRight";
               info.windowsVirtualKeyCode = 221;
               break;
            case '`':
               info.code = "Backquote";
               info.windowsVirtualKeyCode = 192;
               break;
            default:
               info.windowsVirtualKeyCode = 0;
         }
      }

      return info;
   }

   private CdpKeys.KeyInfo getKeyInfo(String specialKey) {
      CdpKeys.KeyInfo info = new CdpKeys.KeyInfo();
      info.text = "";
      if (specialKey != null && !specialKey.isEmpty()) {
         char c = specialKey.charAt(0);
         switch (c) {
            case '\ue003':
               info.key = "Backspace";
               info.code = "Backspace";
               info.windowsVirtualKeyCode = 8;
               break;
            case '\ue004':
               info.key = "Tab";
               info.code = "Tab";
               info.windowsVirtualKeyCode = 9;
               break;
            case '\ue005':
            case '\ue00b':
            case '\ue018':
            case '\ue019':
            case '\ue02a':
            case '\ue02b':
            case '\ue02c':
            case '\ue02d':
            case '\ue02e':
            case '\ue02f':
            case '\ue030':
            default:
               info.key = "";
               info.code = "";
               break;
            case '\ue006':
            case '\ue007':
               info.key = "Enter";
               info.code = "Enter";
               info.windowsVirtualKeyCode = 13;
               info.text = "\r";
               break;
            case '\ue008':
               info.key = "Shift";
               info.code = "ShiftLeft";
               info.windowsVirtualKeyCode = 16;
               info.location = 1;
               break;
            case '\ue009':
               info.key = "Control";
               info.code = "ControlLeft";
               info.windowsVirtualKeyCode = 17;
               info.location = 1;
               break;
            case '\ue00a':
               info.key = "Alt";
               info.code = "AltLeft";
               info.windowsVirtualKeyCode = 18;
               info.location = 1;
               break;
            case '\ue00c':
               info.key = "Escape";
               info.code = "Escape";
               info.windowsVirtualKeyCode = 27;
               break;
            case '\ue00d':
               info.key = " ";
               info.code = "Space";
               info.windowsVirtualKeyCode = 32;
               info.text = " ";
               break;
            case '\ue00e':
               info.key = "PageUp";
               info.code = "PageUp";
               info.windowsVirtualKeyCode = 33;
               break;
            case '\ue00f':
               info.key = "PageDown";
               info.code = "PageDown";
               info.windowsVirtualKeyCode = 34;
               break;
            case '\ue010':
               info.key = "End";
               info.code = "End";
               info.windowsVirtualKeyCode = 35;
               break;
            case '\ue011':
               info.key = "Home";
               info.code = "Home";
               info.windowsVirtualKeyCode = 36;
               break;
            case '\ue012':
               info.key = "ArrowLeft";
               info.code = "ArrowLeft";
               info.windowsVirtualKeyCode = 37;
               break;
            case '\ue013':
               info.key = "ArrowUp";
               info.code = "ArrowUp";
               info.windowsVirtualKeyCode = 38;
               break;
            case '\ue014':
               info.key = "ArrowRight";
               info.code = "ArrowRight";
               info.windowsVirtualKeyCode = 39;
               break;
            case '\ue015':
               info.key = "ArrowDown";
               info.code = "ArrowDown";
               info.windowsVirtualKeyCode = 40;
               break;
            case '\ue016':
               info.key = "Insert";
               info.code = "Insert";
               info.windowsVirtualKeyCode = 45;
               break;
            case '\ue017':
               info.key = "Delete";
               info.code = "Delete";
               info.windowsVirtualKeyCode = 46;
               break;
            case '\ue01a':
               info.key = "0";
               info.code = "Numpad0";
               info.windowsVirtualKeyCode = 96;
               info.text = "0";
               info.location = 3;
               break;
            case '\ue01b':
               info.key = "1";
               info.code = "Numpad1";
               info.windowsVirtualKeyCode = 97;
               info.text = "1";
               info.location = 3;
               break;
            case '\ue01c':
               info.key = "2";
               info.code = "Numpad2";
               info.windowsVirtualKeyCode = 98;
               info.text = "2";
               info.location = 3;
               break;
            case '\ue01d':
               info.key = "3";
               info.code = "Numpad3";
               info.windowsVirtualKeyCode = 99;
               info.text = "3";
               info.location = 3;
               break;
            case '\ue01e':
               info.key = "4";
               info.code = "Numpad4";
               info.windowsVirtualKeyCode = 100;
               info.text = "4";
               info.location = 3;
               break;
            case '\ue01f':
               info.key = "5";
               info.code = "Numpad5";
               info.windowsVirtualKeyCode = 101;
               info.text = "5";
               info.location = 3;
               break;
            case '\ue020':
               info.key = "6";
               info.code = "Numpad6";
               info.windowsVirtualKeyCode = 102;
               info.text = "6";
               info.location = 3;
               break;
            case '\ue021':
               info.key = "7";
               info.code = "Numpad7";
               info.windowsVirtualKeyCode = 103;
               info.text = "7";
               info.location = 3;
               break;
            case '\ue022':
               info.key = "8";
               info.code = "Numpad8";
               info.windowsVirtualKeyCode = 104;
               info.text = "8";
               info.location = 3;
               break;
            case '\ue023':
               info.key = "9";
               info.code = "Numpad9";
               info.windowsVirtualKeyCode = 105;
               info.text = "9";
               info.location = 3;
               break;
            case '\ue024':
               info.key = "*";
               info.code = "NumpadMultiply";
               info.windowsVirtualKeyCode = 106;
               info.text = "*";
               info.location = 3;
               break;
            case '\ue025':
               info.key = "+";
               info.code = "NumpadAdd";
               info.windowsVirtualKeyCode = 107;
               info.text = "+";
               info.location = 3;
               break;
            case '\ue026':
               info.key = ",";
               info.code = "NumpadComma";
               info.windowsVirtualKeyCode = 108;
               info.text = ",";
               info.location = 3;
               break;
            case '\ue027':
               info.key = "-";
               info.code = "NumpadSubtract";
               info.windowsVirtualKeyCode = 109;
               info.text = "-";
               info.location = 3;
               break;
            case '\ue028':
               info.key = ".";
               info.code = "NumpadDecimal";
               info.windowsVirtualKeyCode = 110;
               info.text = ".";
               info.location = 3;
               break;
            case '\ue029':
               info.key = "/";
               info.code = "NumpadDivide";
               info.windowsVirtualKeyCode = 111;
               info.text = "/";
               info.location = 3;
               break;
            case '\ue031':
               info.key = "F1";
               info.code = "F1";
               info.windowsVirtualKeyCode = 112;
               break;
            case '\ue032':
               info.key = "F2";
               info.code = "F2";
               info.windowsVirtualKeyCode = 113;
               break;
            case '\ue033':
               info.key = "F3";
               info.code = "F3";
               info.windowsVirtualKeyCode = 114;
               break;
            case '\ue034':
               info.key = "F4";
               info.code = "F4";
               info.windowsVirtualKeyCode = 115;
               break;
            case '\ue035':
               info.key = "F5";
               info.code = "F5";
               info.windowsVirtualKeyCode = 116;
               break;
            case '\ue036':
               info.key = "F6";
               info.code = "F6";
               info.windowsVirtualKeyCode = 117;
               break;
            case '\ue037':
               info.key = "F7";
               info.code = "F7";
               info.windowsVirtualKeyCode = 118;
               break;
            case '\ue038':
               info.key = "F8";
               info.code = "F8";
               info.windowsVirtualKeyCode = 119;
               break;
            case '\ue039':
               info.key = "F9";
               info.code = "F9";
               info.windowsVirtualKeyCode = 120;
               break;
            case '\ue03a':
               info.key = "F10";
               info.code = "F10";
               info.windowsVirtualKeyCode = 121;
               break;
            case '\ue03b':
               info.key = "F11";
               info.code = "F11";
               info.windowsVirtualKeyCode = 122;
               break;
            case '\ue03c':
               info.key = "F12";
               info.code = "F12";
               info.windowsVirtualKeyCode = 123;
               break;
            case '\ue03d':
               info.key = "Meta";
               info.code = "MetaLeft";
               info.windowsVirtualKeyCode = 91;
               info.location = 1;
         }

         return info;
      } else {
         return info;
      }
   }

   private static class KeyInfo {
      String key;
      String code;
      String text;
      int windowsVirtualKeyCode;
      int nativeVirtualKeyCode;
      int location;
   }
}
