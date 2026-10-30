package io.karatelabs.core;

import io.karatelabs.common.StringUtils;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagSelector {
   private final List<String> tagTexts;
   private final Map<String, List<String>> tagValues;

   public TagSelector(Collection<Tag> tags) {
      if (tags != null && !tags.isEmpty()) {
         this.tagTexts = new ArrayList<>(tags.size());
         this.tagValues = new HashMap<>(tags.size());

         for (Tag tag : tags) {
            this.tagTexts.add(tag.getText());
            this.tagValues.computeIfAbsent(tag.getName(), k -> new ArrayList<>()).addAll(tag.getValues());
         }
      } else {
         this.tagTexts = Collections.emptyList();
         this.tagValues = Collections.emptyMap();
      }
   }

   public boolean evaluate(String tagSelector, String karateEnv) {
      if (containsIgnoreCase(this.tagTexts, "ignore")) {
         return false;
      } else if (this.tagValues.containsKey("setup")) {
         return false;
      } else {
         TagSelector.Values envValues = this.valuesFor("env");
         if (envValues.isPresent) {
            if (karateEnv == null) {
               return false;
            }

            if (!envValues.isAnyOf(karateEnv)) {
               return false;
            }
         }

         if (karateEnv != null && this.valuesFor("envnot").isAnyOf(karateEnv)) {
            return false;
         } else if (tagSelector == null) {
            return true;
         } else {
            Engine engine = new Engine();
            engine.put("anyOf", (JavaCallable)(ctx, args) -> this.anyOf(args));
            engine.put("allOf", (JavaCallable)(ctx, args) -> this.allOf(args));
            engine.put("not", (JavaCallable)(ctx, args) -> this.not(args));
            engine.put("valuesFor", (JavaCallable)(ctx, args) -> {
               String name = args.length > 0 ? args[0].toString() : "";
               return this.valuesFor(name);
            });
            Object result = engine.eval(tagSelector);
            return isTrue(result);
         }
      }
   }

   public boolean anyOf(Object... values) {
      for (String s : removeTagPrefixes(values)) {
         if (this.tagTexts.contains(s)) {
            return true;
         }
      }

      return false;
   }

   public boolean allOf(Object... values) {
      return this.tagTexts.containsAll(removeTagPrefixes(values));
   }

   public boolean not(Object... values) {
      return !this.anyOf(values);
   }

   public TagSelector.Values valuesFor(String name) {
      List<String> list = this.tagValues.get(removeTagPrefix(name));
      return new TagSelector.Values(list);
   }

   public boolean contains(String tagText) {
      return this.tagTexts.contains(removeTagPrefix(tagText));
   }

   public List<String> getTagTexts() {
      return this.tagTexts;
   }

   public Map<String, List<String>> getTagValues() {
      return this.tagValues;
   }

   private static String removeTagPrefix(String s) {
      return s != null && !s.isEmpty() && s.charAt(0) == '@' ? s.substring(1) : s;
   }

   private static Collection<String> removeTagPrefixes(Object... values) {
      List<String> list = new ArrayList<>(values.length);

      for (Object o : values) {
         list.add(removeTagPrefix(o.toString()));
      }

      return list;
   }

   private static boolean containsIgnoreCase(List<String> list, String value) {
      for (String s : list) {
         if (s.equalsIgnoreCase(value)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isTrue(Object value) {
      if (value == null) {
         return false;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue() != 0.0;
      } else {
         return value instanceof String ? !((String)value).isEmpty() : true;
      }
   }

   public static String fromKarateOptionsTags(List<String> tags) {
      return tags != null && !tags.isEmpty() ? fromKarateOptionsTags(tags.toArray(new String[0])) : null;
   }

   public static String fromKarateOptionsTags(String... tags) {
      if (tags != null && tags.length != 0) {
         for (String s : tags) {
            if (s.indexOf(40) != -1) {
               return s;
            }
         }

         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < tags.length; i++) {
            String and = tags[i].trim();
            if (and.startsWith("~")) {
               sb.append("not('").append(and.substring(1)).append("')");
            } else {
               sb.append("anyOf(");

               for (String tag : StringUtils.split(and, ',', false)) {
                  sb.append('\'').append(tag.trim()).append('\'').append(',');
               }

               sb.setLength(sb.length() - 1);
               sb.append(')');
            }

            if (i < tags.length - 1) {
               sb.append(" && ");
            }
         }

         return sb.toString();
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      return this.tagTexts.toString();
   }

   public static class Values implements SimpleObject {
      private final List<String> values;
      private final boolean isPresent;

      public Values(List<String> values) {
         this.values = values == null ? Collections.emptyList() : values;
         this.isPresent = !this.values.isEmpty();
      }

      @Override
      public Object jsGet(String name) {
         return switch (name) {
            case "isPresent" -> this.isPresent;
            case "isAnyOf" -> (JavaCallable) (ctx, args) -> this.isAnyOf(args);
            case "isAllOf" -> (JavaCallable) (ctx, args) -> this.isAllOf(args);
            case "isOnly" -> (JavaCallable) (ctx, args) -> this.isOnly(args);
            case "isEach" -> (JavaCallable) (ctx, args) -> args.length > 0 && args[0] instanceof JavaCallable fn ? this.isEach(fn) : false;
            default -> null;
         };
      }

      public boolean isPresent() {
         return this.isPresent;
      }

      public boolean isAnyOf(Object... args) {
         for (Object o : args) {
            if (this.values.contains(o.toString())) {
               return true;
            }
         }

         return false;
      }

      public boolean isAllOf(Object... args) {
         List<String> list = new ArrayList<>(args.length);

         for (Object o : args) {
            list.add(o.toString());
         }

         return this.values.containsAll(list);
      }

      public boolean isOnly(Object... args) {
         return this.isAllOf(args) && args.length == this.values.size();
      }

      public boolean isEach(JavaCallable fn) {
         for (String s : this.values) {
            Object result = fn.call(null, s);
            if (!TagSelector.isTrue(result)) {
               return false;
            }
         }

         return true;
      }
   }
}
