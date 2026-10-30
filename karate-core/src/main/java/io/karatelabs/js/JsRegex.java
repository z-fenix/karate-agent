package io.karatelabs.js;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class JsRegex extends JsObject {
   public final String pattern;
   public final String flags;
   public final Pattern javaPattern;
   public final boolean global;
   public final boolean sticky;
   public final List<String> groupNames;
   private int lastIndex = 0;
   private static final List<String> INTRINSIC_NAMES = List.of("lastIndex");

   void resetLastIndex() {
      this.lastIndex = 0;
   }

   int currentLastIndex() {
      return this.lastIndex;
   }

   JsRegex() {
      this("(?:)");
   }

   JsRegex(String literalText) {
      super(null, JsRegexPrototype.INSTANCE);
      if (literalText.startsWith("/")) {
         int lastSlashIndex = literalText.lastIndexOf(47);
         if (lastSlashIndex <= 0) {
            throw JsErrorException.syntaxError("Invalid RegExp literal: " + literalText);
         }

         this.pattern = literalText.substring(1, lastSlashIndex);
         this.flags = lastSlashIndex < literalText.length() - 1 ? literalText.substring(lastSlashIndex + 1) : "";
      } else {
         this.pattern = literalText;
         this.flags = "";
      }

      this.global = this.flags.contains("g");
      this.sticky = this.flags.contains("y");
      this.groupNames = extractGroupNames(this.pattern);
      int javaFlags = translateJsFlags(this.flags);

      try {
         String javaPattern = translateJsRegexToJava(this.pattern);
         this.javaPattern = Pattern.compile(javaPattern, javaFlags);
      } catch (PatternSyntaxException var4) {
         throw JsErrorException.syntaxError("Invalid regular expression: /" + this.pattern + "/ - " + var4.getMessage());
      }
   }

   JsRegex(String pattern, String flags) {
      super(null, JsRegexPrototype.INSTANCE);
      this.pattern = pattern;
      this.flags = flags != null ? flags : "";
      this.global = this.flags.contains("g");
      this.sticky = this.flags.contains("y");
      this.groupNames = extractGroupNames(this.pattern);
      int javaFlags = translateJsFlags(this.flags);

      try {
         String javaPattern = translateJsRegexToJava(this.pattern);
         this.javaPattern = Pattern.compile(javaPattern, javaFlags);
      } catch (PatternSyntaxException var5) {
         throw JsErrorException.syntaxError("Invalid regular expression: /" + pattern + "/ - " + var5.getMessage());
      }
   }

   private static List<String> extractGroupNames(String p) {
      List<String> names = new ArrayList<>();
      boolean inClass = false;
      int n = p.length();

      for (int i = 0; i < n; i++) {
         char c = p.charAt(i);
         if (c == '\\') {
            i++;
         } else if (inClass) {
            if (c == ']') {
               inClass = false;
            }
         } else if (c == '[') {
            inClass = true;
         } else if (c == '(' && i + 2 < n && p.charAt(i + 1) == '?' && p.charAt(i + 2) == '<') {
            char d = i + 3 < n ? p.charAt(i + 3) : 0;
            if (d != '=' && d != '!') {
               int close = p.indexOf(62, i + 3);
               if (close > i + 3) {
                  names.add(p.substring(i + 3, close));
                  i = close;
               }
            }
         }
      }

      return names;
   }

   Object buildGroups(Matcher matcher) {
      if (this.groupNames.isEmpty()) {
         return Terms.UNDEFINED;
      } else {
         JsObject groups = new JsObject();
         groups.setPrototype(null);

         for (String name : this.groupNames) {
            String g = matcher.group(name);
            groups.putMember(name, g != null ? g : Terms.UNDEFINED);
         }

         return groups;
      }
   }

   private static String translateJsRegexToJava(String jsPattern) {
      return jsPattern;
   }

   private static int translateJsFlags(String flags) {
      int javaFlags = 0;
      if (flags.contains("i")) {
         javaFlags |= 2;
      }

      if (flags.contains("m")) {
         javaFlags |= 8;
      }

      if (flags.contains("s")) {
         javaFlags |= 32;
      }

      return javaFlags;
   }

   private Matcher matchFrom(String str) {
      Matcher matcher = this.javaPattern.matcher(str);
      if (!this.global && !this.sticky) {
         return matcher.find() ? matcher : null;
      } else {
         int start = this.lastIndex;
         if (start >= 0 && start <= str.length() && matcher.find(start) && (!this.sticky || matcher.start() == start)) {
            this.lastIndex = matcher.end();
            return matcher;
         } else {
            this.lastIndex = 0;
            return null;
         }
      }
   }

   public boolean test(String str) {
      return this.matchFrom(str) != null;
   }

   public String replace(String str, String replacement) {
      Matcher matcher = this.javaPattern.matcher(str);
      StringBuilder result = new StringBuilder();
      int last = 0;

      while (matcher.find()) {
         result.append(str, last, matcher.start());
         appendSubstitution(result, replacement, matcher, str);
         last = matcher.end();
         this.lastIndex = last;
         if (!this.global) {
            break;
         }
      }

      result.append(str, last, str.length());
      return result.toString();
   }

   private static void appendSubstitution(StringBuilder out, String tmpl, Matcher matcher, String input) {
      int n = tmpl.length();

      for (int i = 0; i < n; i++) {
         char c = tmpl.charAt(i);
         if (c == '$' && i + 1 < n) {
            char next = tmpl.charAt(i + 1);
            switch (next) {
               case '$':
                  out.append('$');
                  i++;
                  break;
               case '&':
                  out.append(matcher.group());
                  i++;
                  break;
               case '\'':
                  out.append(input, matcher.end(), input.length());
                  i++;
                  break;
               case '<':
                  int close = tmpl.indexOf(62, i + 2);
                  if (close < 0) {
                     out.append(c);
                  } else {
                     String name = tmpl.substring(i + 2, close);

                     String g;
                     try {
                        g = matcher.group(name);
                     } catch (IllegalStateException | IllegalArgumentException var13) {
                        g = "";
                     }

                     if (g != null) {
                        out.append(g);
                     }

                     i = close;
                  }
                  break;
               case '`':
                  out.append(input, 0, matcher.start());
                  i++;
                  break;
               default:
                  if (next >= '0' && next <= '9') {
                     int total = matcher.groupCount();
                     int idx = next - '0';
                     int consumed = 1;
                     if (i + 2 < n) {
                        char nn = tmpl.charAt(i + 2);
                        if (nn >= '0' && nn <= '9') {
                           int two = idx * 10 + (nn - '0');
                           if (two >= 1 && two <= total) {
                              idx = two;
                              consumed = 2;
                           }
                        }
                     }

                     if (idx != 0 && idx <= total) {
                        String gx = matcher.group(idx);
                        if (gx != null) {
                           out.append(gx);
                        }

                        i += consumed;
                     } else {
                        out.append(c);
                     }
                  } else {
                     out.append(c);
                  }
            }
         } else {
            out.append(c);
         }
      }
   }

   public Object match(String str) {
      if (!this.global) {
         return this.exec(str);
      } else {
         this.resetLastIndex();
         List<Object> matches = new ArrayList<>();

         Matcher matcher;
         while ((matcher = this.matchFrom(str)) != null) {
            matches.add(matcher.group(0));
            if (matcher.end() == matcher.start()) {
               this.lastIndex = matcher.end() + 1;
               if (this.lastIndex > str.length()) {
                  this.resetLastIndex();
                  break;
               }
            }
         }

         return matches.isEmpty() ? null : new JsArray(matches);
      }
   }

   public int search(String str) {
      Matcher matcher = this.javaPattern.matcher(str);
      return matcher.find() ? matcher.start() : -1;
   }

   JsArray exec(String str) {
      Matcher matcher = this.matchFrom(str);
      if (matcher == null) {
         return null;
      } else {
         List<Object> matches = new ArrayList<>();
         matches.add(matcher.group(0));

         for (int i = 1; i <= matcher.groupCount(); i++) {
            String group = matcher.group(i);
            matches.add(group != null ? group : Terms.UNDEFINED);
         }

         JsArray result = new JsArray(matches);
         result.putMember("index", matcher.start());
         result.putMember("input", str);
         result.putMember("groups", this.buildGroups(matcher));
         return result;
      }
   }

   @Override
   public String toString() {
      return "/" + this.pattern + "/" + this.flags;
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return "lastIndex".equals(name) ? this.lastIndex : null;
   }

   @Override
   public void putMember(String name, Object value, CoreContext ctx, boolean strict) {
      if ("lastIndex".equals(name)) {
         this.putMember(name, value);
      } else {
         super.putMember(name, value, ctx, strict);
      }
   }

   @Override
   public void putMember(String name, Object value) {
      if ("lastIndex".equals(name)) {
         double d = Terms.objectToNumber(value).doubleValue();
         if (Double.isNaN(d)) {
            this.lastIndex = 0;
         } else if (d > 2.147483647E9) {
            this.lastIndex = Integer.MAX_VALUE;
         } else if (d < -2.1474836E9F) {
            this.lastIndex = Integer.MIN_VALUE;
         } else {
            this.lastIndex = (int)d;
         }
      } else {
         super.putMember(name, value);
      }
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }
}
