package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class InlineWaiverScanner {
   static final String NEXT_LINE_DIRECTIVE = "karate-lint-disable-next-line";
   static final String SAME_LINE_DIRECTIVE = "karate-lint-disable-line";
   private static final List<String> ALLOWED_META_KEYS = List.of("owner", "expires");
   private static final Pattern META_ENTRY_PATTERN = Pattern.compile("^\\s*[A-Za-z_][A-Za-z0-9_-]*\\s*:");

   private InlineWaiverScanner() {
   }

   public static io.karatelabs.ext.lint.InlineWaiverScanner.InlineWaiverScan scan(io.karatelabs.ext.sourcedoc.SourceDocument var0) {
      ArrayList<io.karatelabs.ext.lint.Waiver> var1 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var0 == null) {
         return new io.karatelabs.ext.lint.InlineWaiverScanner.InlineWaiverScan(var1, var2);
      } else {
         for (io.karatelabs.ext.sourcedoc.SourceComment var4 : var0.comments()) {
            String var5 = var4.text().trim();
            String var6 = matchesDirective(var5, "karate-lint-disable-next-line")
               ? "karate-lint-disable-next-line"
               : (matchesDirective(var5, "karate-lint-disable-line") ? "karate-lint-disable-line" : null);
            if (var6 != null) {
               try {
                  var1.add(parseWaiver(var4, var6, var5.substring(var6.length())));
               } catch (LintException var8) {
                  var2.add(rejectedWaiver(var4, var8.getMessage()));
               }
            }
         }

         return new io.karatelabs.ext.lint.InlineWaiverScanner.InlineWaiverScan(var1, var2);
      }
   }

   private static boolean matchesDirective(String var0, String var1) {
      return !var0.startsWith(var1) ? false : var0.length() == var1.length() || Character.isWhitespace(var0.charAt(var1.length()));
   }

   private static Waiver parseWaiver(io.karatelabs.ext.sourcedoc.SourceComment var0, String var1, String var2) {
      io.karatelabs.ext.sourcedoc.SourceComment.Kind var3 = "karate-lint-disable-next-line".equals(var1) ? io.karatelabs.ext.sourcedoc.SourceComment.Kind.BLOCK : io.karatelabs.ext.sourcedoc.SourceComment.Kind.INLINE;
      if (var0.kind() != var3) {
         throw new LintException(
            "'"
               + var1
               + "' must be written "
               + (
                  var3 == io.karatelabs.ext.sourcedoc.SourceComment.Kind.BLOCK
                     ? "on its own line, above the construct it waives — this one trails a value, so use 'karate-lint-disable-line'"
                     : "at the end of the line it waives — this one is on its own line, so use 'karate-lint-disable-next-line'"
               )
         );
      } else if (var0.scope() != null && !var0.scope().isEmpty()) {
         String var4 = var2.trim();
         if (var4.isEmpty()) {
            throw new LintException("it names no rule (expected: " + var1 + " <rule-id> — <reason> (owner: <who>))");
         } else {
            int var5 = firstWhitespaceIndex(var4);
            String var6 = var5 < 0 ? var4 : var4.substring(0, var5);
            String var7 = var5 < 0 ? "" : var4.substring(var5);
            LinkedHashMap<java.lang.String, java.lang.String> var8 = new LinkedHashMap();
            String var9 = parseReasonAndMetadata(var7, var8);
            return new Waiver(var6, var0.scope(), var9.isEmpty() ? null : var9, (String)var8.get("owner"), io.karatelabs.ext.lint.Waiver.WaiverSource.INLINE, (String)var8.get("expires"));
         }
      } else {
         throw new LintException("it binds to no construct — an inline waiver is scoped to the exact pointer of what it waives, and there is nothing here to scope it to");
      }
   }

   private static String parseReasonAndMetadata(String var0, Map<String, String> var1) {
      String var2 = var0.trim();
      if (!var2.endsWith(")")) {
         return stripReasonSeparator(var2);
      } else {
         int var3 = var2.lastIndexOf(40);
         if (var3 < 0) {
            return stripReasonSeparator(var2);
         } else {
            String[] var4 = var2.substring(var3 + 1, var2.length() - 1).split(",");
            boolean var5 = false;

            for (String var9 : var4) {
               if (META_ENTRY_PATTERN.matcher(var9).find()) {
                  var5 = true;
               }
            }

            if (!var5) {
               return stripReasonSeparator(var2);
            } else {
               for (String var15 : var4) {
                  int var10 = var15.indexOf(58);
                  if (var10 < 0) {
                     throw new LintException("the trailing '(…)' group takes " + String.join(" / ", ALLOWED_META_KEYS) + " as 'key: value' pairs, and '" + var15.trim() + "' is not one");
                  }

                  String var11 = var15.substring(0, var10).trim();
                  if (!ALLOWED_META_KEYS.contains(var11)) {
                     throw new LintException("unknown key '" + var11 + "' in the trailing '(…)' group (known: " + String.join(", ", ALLOWED_META_KEYS) + ")");
                  }

                  var1.put(var11, var15.substring(var10 + 1).trim());
               }

               return stripReasonSeparator(var2.substring(0, var3));
            }
         }
      }
   }

   private static String stripReasonSeparator(String var0) {
      String var1 = var0.trim();

      while (!var1.isEmpty() && (var1.charAt(0) == '-' || var1.charAt(0) == ':' || var1.charAt(0) == 8212 || var1.charAt(0) == 8211)) {
         var1 = var1.substring(1).trim();
      }

      return var1;
   }

   private static int firstWhitespaceIndex(String var0) {
      for (int var1 = 0; var1 < var0.length(); var1++) {
         if (Character.isWhitespace(var0.charAt(var1))) {
            return var1;
         }
      }

      return -1;
   }

   private static Map<String, Object> rejectedWaiver(io.karatelabs.ext.sourcedoc.SourceComment var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("source", io.karatelabs.ext.lint.Waiver.WaiverSource.INLINE.wire());
      var2.put("scope", var0.scope());
      var2.put("line", var0.line());
      var2.put("col", var0.column());
      var2.put("rejected", var1);
      return var2;
   }

   public record InlineWaiverScan(List<Waiver> a, List<Map<String, Object>> b) {

   }
}
