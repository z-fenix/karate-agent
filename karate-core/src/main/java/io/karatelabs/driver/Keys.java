package io.karatelabs.driver;

public interface Keys {
   String NULL = "\ue000";
   String CANCEL = "\ue001";
   String HELP = "\ue002";
   String BACKSPACE = "\ue003";
   String TAB = "\ue004";
   String CLEAR = "\ue005";
   String RETURN = "\ue006";
   String ENTER = "\ue007";
   String SHIFT = "\ue008";
   String CONTROL = "\ue009";
   String ALT = "\ue00a";
   String PAUSE = "\ue00b";
   String ESCAPE = "\ue00c";
   String SPACE = "\ue00d";
   String PAGE_UP = "\ue00e";
   String PAGE_DOWN = "\ue00f";
   String END = "\ue010";
   String HOME = "\ue011";
   String LEFT = "\ue012";
   String UP = "\ue013";
   String RIGHT = "\ue014";
   String DOWN = "\ue015";
   String INSERT = "\ue016";
   String DELETE = "\ue017";
   String SEMICOLON = "\ue018";
   String EQUALS = "\ue019";
   String NUMPAD0 = "\ue01a";
   String NUMPAD1 = "\ue01b";
   String NUMPAD2 = "\ue01c";
   String NUMPAD3 = "\ue01d";
   String NUMPAD4 = "\ue01e";
   String NUMPAD5 = "\ue01f";
   String NUMPAD6 = "\ue020";
   String NUMPAD7 = "\ue021";
   String NUMPAD8 = "\ue022";
   String NUMPAD9 = "\ue023";
   String MULTIPLY = "\ue024";
   String ADD = "\ue025";
   String SEPARATOR = "\ue026";
   String SUBTRACT = "\ue027";
   String DECIMAL = "\ue028";
   String DIVIDE = "\ue029";
   String F1 = "\ue031";
   String F2 = "\ue032";
   String F3 = "\ue033";
   String F4 = "\ue034";
   String F5 = "\ue035";
   String F6 = "\ue036";
   String F7 = "\ue037";
   String F8 = "\ue038";
   String F9 = "\ue039";
   String F10 = "\ue03a";
   String F11 = "\ue03b";
   String F12 = "\ue03c";
   String META = "\ue03d";
   String COMMAND = "\ue03d";

   Keys type(String var1);

   Keys press(String var1);

   Keys down(String var1);

   Keys up(String var1);

   Keys combo(String[] var1, String var2);

   Keys ctrl(String var1);

   Keys alt(String var1);

   Keys shift(String var1);

   Keys meta(String var1);
}
