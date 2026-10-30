package io.karatelabs.ext.common.secrets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecretExtractor {
   public static final String a = "••••";
   private static final String b = "password|passwd|passphrase|pwd|pw|token|api[ _-]?key|client[ _-]?secret|secret";
   private static final String c = "\"'`";
   private static final String d = "((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)";
   private static final String e = "(?:username|user|login)\\s*[:=]?\\s*([^\\s,/:=]+)";
   private static final List<io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule> f = List.of(
      new io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule(
         Pattern.compile(
            "(?i)\\b(password|passwd|passphrase|pwd|pw|token|api[ _-]?key|client[ _-]?secret|secret)\\b(?:\\s+(?:for|of)\\s+\\S+)?\\s*(?:is|:|=)\\s*((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)"
         ),
         1,
         2,
         false
      ),
      new io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule(
         Pattern.compile(
            "(?i)\\buse\\s+((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)\\s+as\\s+(?:the\\s+|my\\s+|your\\s+|our\\s+)?(password|passwd|passphrase|pwd|pw|token|api[ _-]?key|client[ _-]?secret|secret)\\b"
         ),
         3,
         1,
         false
      ),
      new io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule(
         Pattern.compile(
            "(?i)\\b(?:username|user|login)\\s*[:=]?\\s*([^\\s,/:=]+)\\s*,?\\s*(?:and\\s+)?(password|passwd|passphrase|pwd|pw|token|api[ _-]?key|client[ _-]?secret|secret|pass)\\s*[:=]?\\s*((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)"
         ),
         2,
         3,
         false
      ),
      new io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule(
         Pattern.compile(
            "(?i)\\b(?:log\\s*in|credentials?|creds)(?:\\s+(?:with|as|are|is|using))?\\s*[:=]?\\s*([^\\s/]+)\\s*/\\s*((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)"
         ),
         0,
         2,
         false
      ),
      new io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule(
         Pattern.compile(
            "(?i)\\b(password|passwd|passphrase|pwd|pw|token|api[ _-]?key|client[ _-]?secret|secret)\\s+((?<q>[\"'`])(?:(?!\\k<q>)[^\\r\\n])*\\k<q>|[\"'`]?[^\\s\"'`,;]+)"
         ),
         1,
         2,
         true
      )
   );
   private static final Map<String, String> g = Map.of(
      "password",
      "API_PASSWORD",
      "passwd",
      "API_PASSWORD",
      "passphrase",
      "API_PASSWORD",
      "pwd",
      "API_PASSWORD",
      "pw",
      "API_PASSWORD",
      "pass",
      "API_PASSWORD",
      "token",
      "API_TOKEN",
      "apikey",
      "API_KEY",
      "clientsecret",
      "CLIENT_SECRET",
      "secret",
      "API_SECRET"
   );
   private static final Set<String> h = Set.of(
      "is",
      "are",
      "was",
      "be",
      "the",
      "a",
      "an",
      "in",
      "on",
      "of",
      "for",
      "to",
      "and",
      "or",
      "not",
      "no",
      "yes",
      "this",
      "that",
      "it",
      "its",
      "my",
      "your",
      "our",
      "his",
      "her",
      "their",
      "required",
      "needed",
      "wrong",
      "invalid",
      "incorrect",
      "missing",
      "empty",
      "blank",
      "optional",
      "correct",
      "valid",
      "set",
      "unset",
      "same",
      "different",
      "hashed",
      "encrypted",
      "stored",
      "hidden",
      "masked",
      "long",
      "short",
      "weak",
      "strong",
      "expired",
      "fine",
      "ok",
      "okay",
      "there",
      "here",
      "what",
      "which",
      "where",
      "how",
      "still",
      "also",
      "now",
      "too",
      "case",
      "sensitive",
      "unknown",
      "secret",
      "right",
      "bad",
      "good",
      "new",
      "old",
      "field",
      "value",
      "header",
      "param",
      "parameter",
      "query",
      "body",
      "used",
      "sent",
      "saved",
      "shown",
      "visible",
      "exposed",
      "leaked",
      "safe",
      "secure",
      "owner",
      "page",
      "pages",
      "flow",
      "form",
      "check",
      "reset"
   );

   private SecretExtractor() {
   }

   public static String a(String var0, String var1, Map<String, String> var2) {
      String var3 = var0;

      for (int var4 = 2; var2.containsKey(var3) && !var1.equals(var2.get(var3)); var4++) {
         var3 = var0 + "_" + var4;
      }

      return var3;
   }

   public static io.karatelabs.ext.common.secrets.SecretExtractor.SecretExtractionResult a(String var0, io.karatelabs.ext.common.secrets.SecretStore var1) {
      ArrayList<io.karatelabs.ext.common.secrets.SecretExtractor.SecretOccurrence> var2 = new ArrayList();

      for (io.karatelabs.ext.common.secrets.SecretExtractor.SecretPatternRule var4 : f) {
         Matcher var5 = var4.a.matcher(var0);

         while (var5.find()) {
            String var6 = var5.group(var4.c);
            boolean var7 = var6.length() >= 2 && "\"'`".indexOf(var6.charAt(0)) >= 0 && var6.charAt(var6.length() - 1) == var6.charAt(0);
            boolean var8 = var7 || "\"'`".indexOf(var6.charAt(0)) >= 0;
            String var9 = var8 ? var6.substring(1, var6.length() - (var7 ? 1 : 0)) : var6;
            String var10 = var7 ? var9 : var9.replaceAll("[.!?)]+$", "");
            int var11 = var5.start(var4.c) + (var8 ? 1 : 0);
            if (!var10.isEmpty()
               && !var10.startsWith("secret(")
               && !var10.startsWith("‹")
               && !var10.equals("••••")
               && (var7 || !h.contains(var10.toLowerCase(Locale.ROOT)) && (!var4.d || a(var10)))
               && (var4.b != 2 || var4.c != 3 || !h.contains(var5.group(1).toLowerCase(Locale.ROOT)))
               && !var2.stream().anyMatch(var2x -> var2x.a < var11 + var10.length() && var11 < var2x.b)) {
               String var12 = var4.b == 0 ? "password" : var5.group(var4.b).toLowerCase(Locale.ROOT).replaceAll("[ _-]", "");
               var2.add(new io.karatelabs.ext.common.secrets.SecretExtractor.SecretOccurrence(var11, var11 + var10.length(), g.get(var12)));
            }
         }
      }

      if (var2.isEmpty()) {
         return new io.karatelabs.ext.common.secrets.SecretExtractor.SecretExtractionResult(var0, var0);
      } else {
         var2.sort((var0x, var1x) -> var0x.a - var1x.a);
         var var13 = new LinkedHashMap<>(var1.e());
         StringBuilder var14 = new StringBuilder();
         StringBuilder var15 = new StringBuilder();
         int var16 = 0;

         for (io.karatelabs.ext.common.secrets.SecretExtractor.SecretOccurrence var18 : (Iterable<io.karatelabs.ext.common.secrets.SecretExtractor.SecretOccurrence>) (Iterable<?>) (var2)) {
            String var19 = var0.substring(var18.a, var18.b);
            String var20 = a(var18.c, var19, var13);
            var1.a(var20, var19);
            var13.put(var20, var19);
            var14.append(var0, var16, var18.a).append("secret('").append(var20).append("')");
            var15.append(var0, var16, var18.a).append("••••");
            var16 = var18.b;
         }

         var14.append(var0, var16, var0.length());
         var15.append(var0, var16, var0.length());
         return new io.karatelabs.ext.common.secrets.SecretExtractor.SecretExtractionResult(var14.toString(), var15.toString());
      }
   }

   private static boolean a(String var0) {
      boolean var1 = false;
      boolean var2 = false;

      for (char var6 : var0.toCharArray()) {
         var1 |= Character.isDigit(var6);
         var2 |= Character.isLetter(var6);
      }

      return var0.length() >= 16 || var1 && var2 || var0.length() >= 8 && var0.indexOf(45) >= 0;
   }

   private record SecretOccurrence(int a, int b, String c) {

   }

   private record SecretPatternRule(Pattern a, int b, int c, boolean d) {

   }

   public record SecretExtractionResult(String a, String b) {

   }
}
