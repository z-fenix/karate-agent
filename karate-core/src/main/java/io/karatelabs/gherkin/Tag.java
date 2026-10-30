package io.karatelabs.gherkin;

import io.karatelabs.common.StringUtils;
import java.util.Collections;
import java.util.List;

public class Tag {
   public static final String IGNORE = "ignore";
   public static final String ENV = "env";
   public static final String ENVNOT = "envnot";
   public static final String SETUP = "setup";
   public static final String FAIL = "fail";
   public static final String LOCK = "lock";
   private final int line;
   private final String text;
   private final String name;
   private final List<String> values;
   private final SourceSpan span;

   public Tag(int line, String text) {
      this(line, text, null);
   }

   public Tag(int line, String text, SourceSpan span) {
      this.line = line;
      this.span = span;
      this.text = text.substring(1);
      int pos = text.indexOf(61);
      if (pos != -1) {
         this.name = text.substring(1, pos);
         String temp = text.substring(pos + 1);
         if (temp.isEmpty()) {
            this.values = Collections.emptyList();
         } else {
            this.values = StringUtils.split(temp, ',', false);
         }
      } else {
         this.name = this.text;
         this.values = Collections.emptyList();
      }
   }

   public int getLine() {
      return this.line;
   }

   public SourceSpan getSpan() {
      return this.span;
   }

   public String getText() {
      return this.text;
   }

   public String getName() {
      return this.name;
   }

   public List<String> getValues() {
      return this.values;
   }

   @Override
   public String toString() {
      return "@" + this.text;
   }

   @Override
   public int hashCode() {
      return this.text.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         Tag other = (Tag)obj;
         return this.text.equals(other.text);
      }
   }
}
