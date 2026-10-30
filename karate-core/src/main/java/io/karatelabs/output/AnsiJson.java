package io.karatelabs.output;

public class AnsiJson {
   private AnsiJson() {
   }

   public static String colorize(String json) {
      if (Console.isColorsEnabled() && json != null && !json.isEmpty()) {
         StringBuilder sb = new StringBuilder(json.length() + 256);
         int len = json.length();
         int i = 0;

         while (i < len) {
            char c = json.charAt(i);
            if (c == '"') {
               int start = i++;

               while (i < len) {
                  char sc = json.charAt(i);
                  if (sc == '\\' && i + 1 < len) {
                     i += 2;
                  } else {
                     if (sc == '"') {
                        i++;
                        break;
                     }

                     i++;
                  }
               }

               String str = json.substring(start, i);
               int j = i;

               while (j < len && Character.isWhitespace(json.charAt(j))) {
                  j++;
               }

               if (j < len && json.charAt(j) == ':') {
                  sb.append("\u001b[36m").append(str).append("\u001b[0m");
               } else {
                  sb.append("\u001b[32m").append(str).append("\u001b[0m");
               }
            } else if (c != '{' && c != '}' && c != '[' && c != ']' && c != ':' && c != ',') {
               if (c == '-' || c >= '0' && c <= '9') {
                  int start = i;

                  while (true) {
                     if (i < len) {
                        char nc = json.charAt(i);
                        if (nc == '-' || nc == '+' || nc == '.' || nc == 'e' || nc == 'E' || nc >= '0' && nc <= '9') {
                           i++;
                           continue;
                        }
                     }

                     sb.append("\u001b[33m").append(json, start, i).append("\u001b[0m");
                     break;
                  }
               } else if (c == 't' && i + 4 <= len && json.substring(i, i + 4).equals("true")) {
                  sb.append("\u001b[35m").append("true").append("\u001b[0m");
                  i += 4;
               } else if (c == 'f' && i + 5 <= len && json.substring(i, i + 5).equals("false")) {
                  sb.append("\u001b[35m").append("false").append("\u001b[0m");
                  i += 5;
               } else if (c == 'n' && i + 4 <= len && json.substring(i, i + 4).equals("null")) {
                  sb.append("\u001b[35m").append("null").append("\u001b[0m");
                  i += 4;
               } else {
                  sb.append(c);
                  i++;
               }
            } else {
               sb.append("\u001b[2m").append(c).append("\u001b[0m");
               i++;
            }
         }

         return sb.toString();
      } else {
         return json;
      }
   }
}
