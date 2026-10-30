package io.karatelabs.ext.requirements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AcceptanceCriterion(String a, String b) {
   private static final Pattern c = Pattern.compile("^([\\w.]+):\\s*(.*)$");

   public AcceptanceCriterion(String a, String b) {
      this.a = a;
      this.b = b;
   }

   public static List<AcceptanceCriterion> a(List<String> var0) {
      ArrayList<io.karatelabs.ext.requirements.AcceptanceCriterion> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         int var2 = 0;

         for (String var4 : var0) {
            var2++;
            if (var4 != null && !var4.isBlank()) {
               String var5 = var4.strip();
               Matcher var6 = c.matcher(var5);
               if (var6.matches()) {
                  var1.add(new AcceptanceCriterion(var6.group(1), var6.group(2).strip()));
               } else {
                  var1.add(new AcceptanceCriterion(String.valueOf(var2), var5));
               }
            }
         }

         return var1;
      }
   }
}
