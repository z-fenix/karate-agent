package io.karatelabs.js;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JsStringPrototype extends Prototype {
   static final JsStringPrototype INSTANCE = new JsStringPrototype();

   private JsStringPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("indexOf", 1, this::indexOf);
      this.install("startsWith", 1, this::startsWith);
      this.install("getBytes", 0, this::getBytes);
      this.install("split", 2, this::split);
      this.install("charAt", 1, this::charAt);
      this.install("charCodeAt", 1, this::charCodeAt);
      this.install("codePointAt", 1, this::codePointAt);
      this.install("concat", 1, this::concat);
      this.install("endsWith", 1, this::endsWith);
      this.install("includes", 1, this::includes);
      this.install("lastIndexOf", 1, this::lastIndexOf);
      this.install("padEnd", 1, this::padEnd);
      this.install("padStart", 1, this::padStart);
      this.install("repeat", 1, this::repeat);
      this.install("slice", 2, this::slice);
      this.install("substring", 2, this::substring);
      this.install("substr", 2, this::substr);
      this.install("toLowerCase", 0, this::toLowerCase);
      this.install("toUpperCase", 0, this::toUpperCase);
      this.install("trim", 0, this::trim);
      this.install("trimStart", 0, this::trimStart);
      this.install("trimLeft", 0, this::trimStart);
      this.install("trimEnd", 0, this::trimEnd);
      this.install("trimRight", 0, this::trimEnd);
      this.install("replace", 2, this::replace);
      this.install("replaceAll", 2, this::replaceAll);
      this.install("match", 1, this::match);
      this.install("matchAll", 1, this::matchAll);
      this.install("search", 1, this::search);
      this.install("valueOf", 0, this::valueOf);
      this.install("toLocaleLowerCase", 0, this::toLowerCase);
      this.install("toLocaleUpperCase", 0, this::toUpperCase);
      this.install("at", 1, this::at);
      this.install("toString", 0, this::valueOf);
      this.install("normalize", 0, this::normalize);
      this.install("localeCompare", 1, this::localeCompare);
      this.install("@@iterator", 0, IterUtils.SYMBOL_ITERATOR_METHOD);
   }

   private static String thisString(Context context, String methodName) {
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, "String.prototype." + methodName);
      if (thisObj instanceof JsString js) {
         return js.text;
      } else {
         return thisObj instanceof String s ? s : Terms.toStringCoerce(thisObj, context instanceof CoreContext cc ? cc : null);
      }
   }

   private static String argString(Object[] args, int idx, Context context) {
      Object arg = idx < args.length ? args[idx] : Terms.UNDEFINED;
      if (arg instanceof String s) {
         return s;
      } else {
         return arg instanceof JsString js ? js.text : Terms.toStringCoerce(arg, context instanceof CoreContext cc ? cc : null);
      }
   }

   private static int argInt(Object[] args, int idx, int defaultValue) {
      if (idx >= args.length) {
         return defaultValue;
      } else {
         Object arg = args[idx];
         if (arg != null && arg != Terms.UNDEFINED) {
            Number n = Terms.objectToNumber(arg);
            double d = n.doubleValue();
            if (Double.isNaN(d)) {
               return 0;
            } else if (d > 2.147483647E9) {
               return Integer.MAX_VALUE;
            } else {
               return d < -2.1474836E9F ? Integer.MIN_VALUE : (int)d;
            }
         } else {
            return defaultValue;
         }
      }
   }

   private Object indexOf(Context context, Object[] args) {
      String s = thisString(context, "indexOf");
      String search = argString(args, 0, context);
      int from = argInt(args, 1, 0);
      return s.indexOf(search, from);
   }

   private Object startsWith(Context context, Object[] args) {
      String s = thisString(context, "startsWith");
      String search = argString(args, 0, context);
      int from = argInt(args, 1, 0);
      return s.startsWith(search, from);
   }

   private Object getBytes(Context context, Object[] args) {
      return thisString(context, "getBytes").getBytes(StandardCharsets.UTF_8);
   }

   private Object split(Context context, Object[] args) {
      String s = thisString(context, "split");
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         long limit = -1L;
         if (args.length > 1 && args[1] != null && args[1] != Terms.UNDEFINED) {
            limit = Terms.toUint32(args[1]);
            if (limit == 0L) {
               return new JsArray(new ArrayList<>());
            }
         }

         if (args[0] instanceof JsRegex regex) {
            return regexSplit(s, regex, limit);
         } else {
            String sep = argString(args, 0, context);
            if (sep.isEmpty()) {
               int capped = limit < 0L ? s.length() : (int)Math.min(limit, (long)s.length());
               List<Object> result = new ArrayList<>(capped);

               for (int i = 0; i < capped; i++) {
                  result.add(String.valueOf(s.charAt(i)));
               }

               return new JsArray(result);
            } else {
               String[] parts = s.split(Pattern.quote(sep), -1);
               int take = limit >= 0L && parts.length > limit ? (int)limit : parts.length;
               List<Object> result = new ArrayList<>(take);

               for (int i = 0; i < take; i++) {
                  result.add(parts[i]);
               }

               return new JsArray(result);
            }
         }
      } else {
         JsArray single = new JsArray(new ArrayList<>(1));
         single.list.add(s);
         return single;
      }
   }

   private static Object regexSplit(String s, JsRegex regex, long limit) {
      List<Object> result = new ArrayList<>();
      Matcher matcher = regex.javaPattern.matcher(s);
      matcher.useTransparentBounds(true);
      matcher.useAnchoringBounds(false);
      int size = s.length();
      if (size == 0) {
         if (!matchAt(matcher, s, 0)) {
            result.add(s);
         }

         return new JsArray(result);
      } else {
         boolean unicode = regex.flags.indexOf(117) >= 0;
         int p = 0;
         int q = 0;

         while (q < size) {
            if (!matchAt(matcher, s, q)) {
               q = advanceStringIndex(s, q, unicode);
            } else {
               int e = Math.min(matcher.end(), size);
               if (e == p) {
                  q = advanceStringIndex(s, q, unicode);
               } else {
                  result.add(s.substring(p, q));
                  if (result.size() == limit) {
                     return new JsArray(result);
                  }

                  p = e;

                  for (int i = 1; i <= matcher.groupCount(); i++) {
                     String group = matcher.group(i);
                     result.add(group != null ? group : Terms.UNDEFINED);
                     if (result.size() == limit) {
                        return new JsArray(result);
                     }
                  }

                  q = e;
               }
            }
         }

         result.add(s.substring(p, size));
         return new JsArray(result);
      }
   }

   private static boolean matchAt(Matcher matcher, String s, int q) {
      matcher.region(q, s.length());
      return matcher.lookingAt();
   }

   private Object charAt(Context context, Object[] args) {
      String s = thisString(context, "charAt");
      int index = argInt(args, 0, 0);
      return index >= 0 && index < s.length() ? String.valueOf(s.charAt(index)) : "";
   }

   private Object charCodeAt(Context context, Object[] args) {
      String s = thisString(context, "charCodeAt");
      int index = argInt(args, 0, 0);
      return index >= 0 && index < s.length() ? Integer.valueOf(s.charAt(index)) : Double.NaN;
   }

   private Object codePointAt(Context context, Object[] args) {
      String s = thisString(context, "codePointAt");
      int index = argInt(args, 0, 0);
      return index >= 0 && index < s.length() ? s.codePointAt(index) : Terms.UNDEFINED;
   }

   private Object concat(Context context, Object[] args) {
      String s = thisString(context, "concat");
      StringBuilder sb = new StringBuilder(s);

      for (int i = 0; i < args.length; i++) {
         sb.append(argString(args, i, context));
      }

      return sb.toString();
   }

   private Object endsWith(Context context, Object[] args) {
      String s = thisString(context, "endsWith");
      String search = argString(args, 0, context);
      if (args.length > 1 && args[1] != null && args[1] != Terms.UNDEFINED) {
         int endPosition = Math.max(0, Math.min(argInt(args, 1, s.length()), s.length()));
         return s.substring(0, endPosition).endsWith(search);
      } else {
         return s.endsWith(search);
      }
   }

   private Object includes(Context context, Object[] args) {
      String s = thisString(context, "includes");
      String search = argString(args, 0, context);
      int from = argInt(args, 1, 0);
      return s.indexOf(search, from) >= 0;
   }

   private Object lastIndexOf(Context context, Object[] args) {
      String s = thisString(context, "lastIndexOf");
      String search = argString(args, 0, context);
      if (args.length > 1 && args[1] != null && args[1] != Terms.UNDEFINED) {
         int from = argInt(args, 1, s.length());
         return s.lastIndexOf(search, from);
      } else {
         return s.lastIndexOf(search);
      }
   }

   private Object padEnd(Context context, Object[] args) {
      String s = thisString(context, "padEnd");
      int targetLength = argInt(args, 0, 0);
      String padString = args.length > 1 && args[1] != Terms.UNDEFINED ? argString(args, 1, context) : " ";
      if (padString.isEmpty()) {
         return s;
      } else if (s.length() >= targetLength) {
         return s;
      } else {
         int padLength = targetLength - s.length();
         StringBuilder sb = new StringBuilder(s);

         for (int i = 0; i < padLength; i++) {
            sb.append(padString.charAt(i % padString.length()));
         }

         return sb.toString();
      }
   }

   private Object padStart(Context context, Object[] args) {
      String s = thisString(context, "padStart");
      int targetLength = argInt(args, 0, 0);
      String padString = args.length > 1 && args[1] != Terms.UNDEFINED ? argString(args, 1, context) : " ";
      if (padString.isEmpty()) {
         return s;
      } else if (s.length() >= targetLength) {
         return s;
      } else {
         int padLength = targetLength - s.length();
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < padLength; i++) {
            sb.append(padString.charAt(i % padString.length()));
         }

         sb.append(s);
         return sb.toString();
      }
   }

   private Object repeat(Context context, Object[] args) {
      String s = thisString(context, "repeat");
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         double d = Terms.objectToNumber(args[0]).doubleValue();
         if (Double.isNaN(d)) {
            return "";
         } else if (!(d < 0.0) && !Double.isInfinite(d)) {
            int count = (int)d;
            return s.repeat(count);
         } else {
            throw JsErrorException.rangeError("Invalid count value");
         }
      } else {
         return "";
      }
   }

   private Object slice(Context context, Object[] args) {
      String s = thisString(context, "slice");
      int beginIndex = argInt(args, 0, 0);
      int endIndex = args.length > 1 && args[1] != Terms.UNDEFINED ? argInt(args, 1, s.length()) : s.length();
      if (beginIndex < 0) {
         beginIndex = Math.max(s.length() + beginIndex, 0);
      }

      if (endIndex < 0) {
         endIndex = Math.max(s.length() + endIndex, 0);
      }

      beginIndex = Math.min(beginIndex, s.length());
      endIndex = Math.min(endIndex, s.length());
      return beginIndex >= endIndex ? "" : s.substring(beginIndex, endIndex);
   }

   private Object substring(Context context, Object[] args) {
      String s = thisString(context, "substring");
      int beginIndex = argInt(args, 0, 0);
      int endIndex = args.length > 1 && args[1] != Terms.UNDEFINED ? argInt(args, 1, s.length()) : s.length();
      beginIndex = Math.min(Math.max(beginIndex, 0), s.length());
      endIndex = Math.min(Math.max(endIndex, 0), s.length());
      if (beginIndex > endIndex) {
         int temp = beginIndex;
         beginIndex = endIndex;
         endIndex = temp;
      }

      return s.substring(beginIndex, endIndex);
   }

   private Object substr(Context context, Object[] args) {
      String s = thisString(context, "substr");
      int len = s.length();
      int start = argInt(args, 0, 0);
      if (start < 0) {
         start = Math.max(len + start, 0);
      }

      if (start >= len) {
         return "";
      } else {
         int defaultLength = len - start;
         int length;
         if (args.length > 1 && args[1] != null && args[1] != Terms.UNDEFINED) {
            length = argInt(args, 1, defaultLength);
         } else {
            length = defaultLength;
         }

         if (length <= 0) {
            return "";
         } else {
            int end = Math.min(start + length, len);
            return s.substring(start, end);
         }
      }
   }

   private Object toLowerCase(Context context, Object[] args) {
      return thisString(context, "toLowerCase").toLowerCase();
   }

   private Object toUpperCase(Context context, Object[] args) {
      return thisString(context, "toUpperCase").toUpperCase();
   }

   private Object trim(Context context, Object[] args) {
      return trimSpec(thisString(context, "trim"), true, true);
   }

   private Object trimStart(Context context, Object[] args) {
      return trimSpec(thisString(context, "trimStart"), true, false);
   }

   private Object trimEnd(Context context, Object[] args) {
      return trimSpec(thisString(context, "trimEnd"), false, true);
   }

   private static String trimSpec(String s, boolean leading, boolean trailing) {
      int len = s.length();
      int start = 0;
      int end = len;
      if (leading) {
         while (start < end) {
            int cp = s.codePointAt(start);
            if (!isJsWhitespace(cp)) {
               break;
            }

            start += Character.charCount(cp);
         }
      }

      if (trailing) {
         while (end > start) {
            int cp = s.codePointBefore(end);
            if (!isJsWhitespace(cp)) {
               break;
            }

            end -= Character.charCount(cp);
         }
      }

      return start == 0 && end == len ? s : s.substring(start, end);
   }

   private static boolean isJsWhitespace(int c) {
      return c == 9
         || c == 11
         || c == 12
         || c == 32
         || c == 160
         || c == 65279
         || c == 10
         || c == 13
         || c == 8232
         || c == 8233
         || c >= 5760 && Character.getType(c) == 12;
   }

   private Object replace(Context context, Object[] args) {
      String s = thisString(context, "replace");
      Object search = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Object replacement = args.length > 1 ? args[1] : Terms.UNDEFINED;
      if (search instanceof JsRegex regex) {
         return replacement instanceof JsCallable fn ? regexReplace(s, regex, fn, context, regex.global) : regex.replace(s, argString(args, 1, context));
      } else {
         String searchStr = argString(args, 0, context);
         if (replacement instanceof JsCallable fn) {
            int idx = s.indexOf(searchStr);
            if (idx < 0) {
               return s;
            } else {
               Object r = fn.call(context, new Object[]{searchStr, idx, s});
               String coerced = Terms.toStringCoerce(r, context instanceof CoreContext cc ? cc : null);
               return s.substring(0, idx) + coerced + s.substring(idx + searchStr.length());
            }
         } else {
            return s.replace(searchStr, argString(args, 1, context));
         }
      }
   }

   private Object replaceAll(Context context, Object[] args) {
      String s = thisString(context, "replaceAll");
      Object search = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Object replacement = args.length > 1 ? args[1] : Terms.UNDEFINED;
      if (search instanceof JsRegex regex) {
         if (!regex.global) {
            throw JsErrorException.typeError("String.prototype.replaceAll called with a non-global RegExp argument");
         } else {
            return replacement instanceof JsCallable fn ? regexReplace(s, regex, fn, context, true) : regex.replace(s, argString(args, 1, context));
         }
      } else {
         String searchStr = argString(args, 0, context);
         if (!(replacement instanceof JsCallable fn)) {
            return s.replace(searchStr, argString(args, 1, context));
         } else {
            StringBuilder sb = new StringBuilder();
            int from = 0;

            while (true) {
               int idx = searchStr.isEmpty() ? from : s.indexOf(searchStr, from);
               if (idx < 0 || searchStr.isEmpty() && from > s.length()) {
                  sb.append(s, Math.min(from, s.length()), s.length());
                  return sb.toString();
               }

               sb.append(s, from, idx);
               Object r = fn.call(context, new Object[]{searchStr, idx, s});
               sb.append(Terms.toStringCoerce(r, context instanceof CoreContext cc ? cc : null));
               if (searchStr.isEmpty()) {
                  if (from < s.length()) {
                     sb.append(s.charAt(from));
                  }

                  from++;
               } else {
                  from = idx + searchStr.length();
               }
            }
         }
      }
   }

   private static String regexReplace(String s, JsRegex regex, JsCallable fn, Context context, boolean global) {
      if (global) {
         regex.resetLastIndex();
      }

      Matcher m = regex.javaPattern.matcher(s);
      StringBuilder sb = new StringBuilder();
      int last = 0;
      CoreContext cc = context instanceof CoreContext c ? c : null;
      boolean hasNamed = !regex.groupNames.isEmpty();

      while (m.find()) {
         sb.append(s, last, m.start());
         int groups = m.groupCount();
         Object[] callArgs = new Object[groups + 3 + (hasNamed ? 1 : 0)];
         callArgs[0] = m.group(0);

         for (int i = 1; i <= groups; i++) {
            String g = m.group(i);
            callArgs[i] = g != null ? g : Terms.UNDEFINED;
         }

         callArgs[groups + 1] = m.start();
         callArgs[groups + 2] = s;
         if (hasNamed) {
            callArgs[groups + 3] = regex.buildGroups(m);
         }

         Object r = fn.call(context, callArgs);
         sb.append(Terms.toStringCoerce(r, cc));
         last = m.end();
         if (!global) {
            break;
         }
      }

      sb.append(s, last, s.length());
      return sb.toString();
   }

   private Object match(Context context, Object[] args) {
      String s = thisString(context, "match");
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         JsRegex regex = args[0] instanceof JsRegex r ? r : new JsRegex(argString(args, 0, context));
         return regex.match(s);
      } else {
         return List.of("");
      }
   }

   private Object matchAll(Context context, Object[] args) {
      final String s = thisString(context, "matchAll");
      final JsRegex regex;
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         if (args[0] instanceof JsRegex r) {
            if (!r.global) {
               throw JsErrorException.typeError("String.prototype.matchAll called with a non-global RegExp argument");
            }

            regex = r;
         } else {
            regex = new JsRegex(argString(args, 0, context), "g");
         }
      } else {
         regex = new JsRegex("", "g");
      }

      final Matcher matcher = regex.javaPattern.matcher(s);
      JsIterator iter = new JsIterator() {
         boolean fetched;
         boolean done;
         JsArray pending;
         final boolean unicode = regex.flags.indexOf(117) >= 0;
         int nextIndex = regex.currentLastIndex();

         private void fetch() {
            if (!this.fetched && !this.done) {
               if (this.nextIndex >= 0 && this.nextIndex <= s.length() && matcher.find(this.nextIndex) && (!regex.sticky || matcher.start() == this.nextIndex)) {
                  this.nextIndex = matcher.end() == matcher.start() ? JsStringPrototype.advanceStringIndex(s, matcher.end(), this.unicode) : matcher.end();
                  List<Object> groups = new ArrayList<>();
                  groups.add(matcher.group(0));

                  for (int i = 1; i <= matcher.groupCount(); i++) {
                     String g = matcher.group(i);
                     groups.add(g != null ? g : Terms.UNDEFINED);
                  }

                  JsArray match = new JsArray(groups);
                  match.putMember("index", matcher.start());
                  match.putMember("input", s);
                  match.putMember("groups", regex.buildGroups(matcher));
                  this.pending = match;
                  this.fetched = true;
               } else {
                  this.done = true;
               }
            }
         }

         @Override
         public boolean hasNext() {
            this.fetch();
            return !this.done;
         }

         @Override
         public Object next() {
            this.fetch();
            if (this.done) {
               throw new NoSuchElementException();
            } else {
               this.fetched = false;
               JsArray v = this.pending;
               this.pending = null;
               return v;
            }
         }
      };
      return IterUtils.toIteratorObject(iter);
   }

   private static int advanceStringIndex(String s, int index, boolean unicode) {
      return unicode && index + 1 < s.length() && Character.isHighSurrogate(s.charAt(index)) && Character.isLowSurrogate(s.charAt(index + 1))
         ? index + 2
         : index + 1;
   }

   private Object search(Context context, Object[] args) {
      String s = thisString(context, "search");
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         JsRegex regex = args[0] instanceof JsRegex r ? r : new JsRegex(argString(args, 0, context));
         return regex.search(s);
      } else {
         return 0;
      }
   }

   private Object valueOf(Context context, Object[] args) {
      Object thisObj = context.getThisObject();
      if (thisObj instanceof JsString js) {
         return js.text;
      } else if (thisObj instanceof String s) {
         return s;
      } else {
         Terms.requireObjectCoercible(thisObj, "String.prototype.valueOf");
         throw JsErrorException.typeError("String.prototype.valueOf requires that 'this' be a String");
      }
   }

   private Object at(Context context, Object[] args) {
      String s = thisString(context, "at");
      if (args.length == 0) {
         return Terms.UNDEFINED;
      } else {
         int index = argInt(args, 0, 0);
         if (index < 0) {
            index += s.length();
         }

         return index >= 0 && index < s.length() ? String.valueOf(s.charAt(index)) : Terms.UNDEFINED;
      }
   }

   private Object normalize(Context context, Object[] args) {
      String s = thisString(context, "normalize");
      String form = args.length > 0 && args[0] != Terms.UNDEFINED ? argString(args, 0, context) : "NFC";

      return switch (form) {
         case "NFC" -> Normalizer.normalize(s, Form.NFC);
         case "NFD" -> Normalizer.normalize(s, Form.NFD);
         case "NFKC" -> Normalizer.normalize(s, Form.NFKC);
         case "NFKD" -> Normalizer.normalize(s, Form.NFKD);
         default -> throw JsErrorException.rangeError("The normalization form should be one of NFC, NFD, NFKC, NFKD");
      };
   }

   private Object localeCompare(Context context, Object[] args) {
      String s = thisString(context, "localeCompare");
      String other = argString(args, 0, context);
      return Integer.signum(s.compareToIgnoreCase(other));
   }
}
