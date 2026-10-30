package io.karatelabs.ext.agent.llm;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Base64.Encoder;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SecretRedactor implements UnaryOperator<String> {
   public static final String MASKED_TOKEN = "‹masked›";
   private static final Logger LOGGER = LoggerFactory.getLogger(SecretRedactor.class);
   static final int MIN_PLAUSIBLE_SECRET_LENGTH = 8;
   static final int LONG_SECRET_LENGTH = 16;
   private static final int MAX_LEARNED_SECRET_LENGTH = 16384;
   private static final int MAX_LEARNED_SECRETS = 256;
   private static final int MAX_CONTEXTUAL_SECRET_LENGTH = 128;
   private static final int URL_ESCAPE_BYTE_WIDTH = 3;
   private static final int CONTEXT_LOOKBEHIND_LENGTH = 400;
   private static final String SENSITIVE_HEADER_NAMES_REGEX = "authorization|proxy-authorization|cookie|set-cookie";
   private static final String SENSITIVE_FIELD_NAME_REGEX = "[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*";
   private static final String SENSITIVE_NAME_REGEX = "(?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*";
   private static final Pattern SENSITIVE_NAME_PATTERN = Pattern.compile("(?i)authorization|proxy-authorization|cookie|set-cookie|.*(?:token|password|passwd|secret|api[_-]?key).*");
   private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN = Pattern.compile(
      "(?:(\\\\*[\"'])((?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*)\\1|(?<![\\w\"'\\\\])((?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*))\\s*(:|=(?!=))\\s*"
   );
   private static final Pattern NUMBER_LITERAL_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?(?![\\w.])");
   private static final Pattern COLON_PREFIX_PATTERN = Pattern.compile("\\s*:");
   private static final Pattern OBJECT_KEY_PREFIX_PATTERN = Pattern.compile("([A-Za-z_$][\\w$]*)\\\\*[\"']?\\s*:\\s*$");
   private static final String ESCAPED_STRING_CONTENT_REGEX = "(?:\\\\[^rn]|[^\\r\\n\"\\\\])*";
   private static final Pattern SENSITIVE_LINE_VALUE_PATTERN = Pattern.compile(
      "(?im)(?:(^"
         + headerAssignmentPrefixRegex()
         + ")([^'\"\\s{\\\\][^\\r\\n]*)|((?<!\\\\)\\\\[nr]"
         + headerAssignmentPrefixRegex()
         + ")([^'\"\\s{\\\\](?:\\\\[^rn]|[^\\r\\n\"\\\\])*)|(\\\\[nr]"
         + headerAssignmentPrefixRegex()
         + ")([^'\"\\s{\\\\][^\\r\\n\"\\\\]*))"
   );
   private static final String PARAMETER_BOUNDARY_REGEX = "(?:(?<![\\s\\S])|(?<=[?&\"'\\n]|\\\\n))";
   private static final Pattern SECRET_QUERY_PARAMETER_PATTERN = Pattern.compile(
      "(?:(?<![\\s\\S])|(?<=[?&\"'\\n]|\\\\n))([A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*=)(secret\\(['\"][A-Z_][A-Z0-9_]*['\"]\\)(?=[&\\s\"'#\\\\]|$)|(?:secret\\(['\"][A-Z_][A-Z0-9_]*['\"]\\))?[^&\\s\"'#\\\\]+)"
   );
   private static final Pattern QUERY_PARAMETER_PATTERN = Pattern.compile(
      "(?:(?<![\\s\\S])|(?<=[?&\"'\\n]|\\\\n))([A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*=)([^&\\r\\n\"'#\\\\]+)"
   );
   private static final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile(
      "(?<=[{\\[]|, )(?:((?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*)=|key=((?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*),\\s*value=)((?:(?i:bearer|basic|digest|token)[ \\t]+)?[^,}\\]\\s]*)"
   );
   private static final String BLANK_LINE_BOUNDARY_REGEX = "(?:\\r?\\n|\\\\r?\\\\n){2}";
   private static final Pattern MULTILINE_SECRET_VALUE_PATTERN = Pattern.compile(
      "("
         + namedSecretBodyRegex("[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*")
         + ")(?:(?<=\\n)([^\\r\\n]+)|(?<=[^\\\\]\\\\n)((?:\\\\[^rn]|[^\\r\\n\"\\\\])*)|([^\\r\\n\"\\\\]+))"
   );
   private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*");
   private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+)([A-Za-z0-9\\-._~+/]{8,}=*)");
   private static final Pattern BASE64_CANDIDATE_PATTERN = Pattern.compile("(?<![A-Za-z0-9+/_-])[A-Za-z0-9+/_-]{6,}={0,2}(?![A-Za-z0-9+/_=-])");
   private static final Pattern SECRET_ENV_NAME_PATTERN = Pattern.compile("[A-Z_][A-Z0-9_]*");
   private static final Pattern AUTH_SCHEME_VALUE_PATTERN = Pattern.compile("(?i)(?:bearer|basic|digest|token)\\s+(\\S+)");
   private static final Pattern AUTH_SCHEME_PREFIX_PATTERN = Pattern.compile("(?i)(?:bearer|basic|digest|token)\\s*");
   private static final Pattern INTERNAL_TOKEN_PATTERN = Pattern.compile("‹[^‹›]*›");
   private static final Pattern KARATE_PLACEHOLDER_PATTERN = Pattern.compile(
      "(?s)#(?:ignore|null|notnull|present|notpresent|array|object|list|map|boolean|number|string|uuid|regex\\b.*|\\?.*|\\[.*|\\(.*\\))|\\{\\{[^{}]*}}|\\$\\{[^{}]*}|<[A-Za-z_][\\w.-]*>"
   );
   private static final Pattern PROTECTED_KARATE_PATTERN = Pattern.compile(
      INTERNAL_TOKEN_PATTERN.pattern()
         + "|#(?:ignore|null|notnull|present|notpresent|array|object|list|map|boolean|number|string|uuid)\\b|#(?:regex|\\?)[^'\"\\r\\n]*|#\\[[^'\"\\r\\n]*|#\\([^'\"\\r\\n]*\\)|\\{\\{[^{}]*}}|\\$\\{[^{}]*}|<[A-Za-z_][\\w.-]*>"
   );
   private static final Pattern UNICODE_ESCAPE_HEX_PATTERN = Pattern.compile("[0-9A-Fa-f]{4}");
   private static final Pattern KARATE_MARKER_PATTERN = Pattern.compile(
      "(?s)##?(?:\\(.*\\)|\\[.*|\\?.*|regex\\b.*|(?:array|boolean|ignore|notnull|null|number|object|present|notpresent|string|uuid)\\s*(?:\\?.*)?)"
   );
   private static final Pattern STEP_KEYWORD_PATTERN = Pattern.compile("(?:Given|When|Then|And|But|\\*)\\s");
   private static final Pattern ASSERTION_STEP_PATTERN = Pattern.compile(STEP_KEYWORD_PATTERN.pattern() + "+(?:match|assert)\\b");
   private static final Pattern SENSITIVE_ASSERTION_PATTERN = Pattern.compile(
      "\\b(?:match\\s+(?:each\\s+)?|assert\\s+)(?:header\\s+)?\\S*(?:(?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*)\\s*(?:==|!=|!?contains(?:\\s+(?:only|any|deep))?)\\s*"
   );
   private static final Pattern SENSITIVE_TABLE_HEADER_PATTERN = Pattern.compile(
      STEP_KEYWORD_PATTERN.pattern() + "+(?:table|set)\\s+\\S*(?:[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*)"
   );
   private static final Pattern EXAMPLES_HEADER_PATTERN = Pattern.compile("(?:Examples|Scenarios):");
   private static final String TRIPLE_QUOTE_DELIMITER = "\"\"\"";
   private static final Pattern PARENTHESIZED_KARATE_MARKER_PATTERN = Pattern.compile("(?s)##?\\(.*\\)");
   private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\{\\{[^{}]*}}|\\$\\{[^{}]*}|<[A-Za-z_][\\w.-]*>");
   private static final List<UnaryOperator<io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText>> READING_DECODERS = List.of(io.karatelabs.ext.agent.llm.SecretRedactor::decodeHtmlEntities, io.karatelabs.ext.agent.llm.SecretRedactor::decodeJsonEscapes, io.karatelabs.ext.agent.llm.SecretRedactor::decodeUrlEscapes);
   private static final int MAX_DECODE_DEPTH = 4;
   private static final int MAX_DECODED_READINGS = 32;
   private static final Pattern SECRET_REFERENCE_PATTERN = Pattern.compile("secret\\((['\"])[A-Z_][A-Z0-9_]*\\1\\)");
   private static final Pattern SENSITIVE_CONTEXT_PATTERN = secretBoundaryRegex(
      "(?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*"
   );
   private final Supplier<Map<String, String>> secretValues;
   private final Set<String> learnedSecrets = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, Boolean> var1) {
         return this.size() > 256;
      }
   });
   private long learnedSecretsVersion;
   private final AtomicLong decodeLimitHitCount = new AtomicLong();
   private volatile io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog secretCatalog = new io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog(Map.of(), List.of());

   public static boolean isSensitiveName(String var0) {
      return var0 != null && SENSITIVE_NAME_PATTERN.matcher(var0).matches();
   }

   static boolean isPlausibleSecretValue(String var0) {
      if (var0.length() >= 16) {
         return true;
      } else if (var0.length() < 8) {
         return false;
      } else {
         int var1 = 0;
         int var2 = 0;

         for (int var3 = 0; var3 < var0.length() && var1 < 2; var3++) {
            char var4 = var0.charAt(var3);
            int var5 = Character.isDigit(var4) ? 1 : (Character.isLowerCase(var4) ? 2 : (Character.isUpperCase(var4) ? 4 : 8));
            if ((var2 & var5) == 0) {
               var2 |= var5;
               var1++;
            }
         }

         return var1 >= 2;
      }
   }

   public SecretRedactor(Supplier<Map<String, String>> var1) {
      this.secretValues = var1 == null ? Map::of : var1;
   }

   @Override
   public String apply(String value) {
      return this.redactKnownAndGeneric(value);
   }

   public String redactKnownAndGeneric(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = this.redactWithAliasRegistry(var1, (io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry)null);
         var var3 = findKnownSecretSpans(var1, this.secretCatalog());
         if (var3.isEmpty()) {
            return var2;
         } else {
            String var4 = Long.toHexString(ThreadLocalRandom.current().nextLong());
            String var5 = this.redactWithProtectedKnownSecrets(var1, var3, var4, var2);
            if (var5 == null) {
               ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var6 = new ArrayList();

               for (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var8 : (Iterable<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan>) (Iterable<?>) (var3)) {
                  if (this.redactWithProtectedKnownSecrets(var1, List.of(var8), var4, var2) != null) {
                     var6.add(var8);
                  }
               }

               var5 = var6.isEmpty() ? null : this.redactWithProtectedKnownSecrets(var1, var6, var4, var2);
            }

            return var5 == null ? var2 : this.redactWithAliasRegistry(var5, (io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry)null);
         }
      } else {
         return var1;
      }
   }

   private String redactWithProtectedKnownSecrets(String var1, List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var2, String var3, String var4) {
      StringBuilder var5 = new StringBuilder(var1.length());
      int var6 = 0;

      for (int var7 = 0; var7 < var2.size(); var7++) {
         io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var8 = (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan)var2.get(var7);
         var5.append(var1, var6, var8.start).append('\'').append(callPlaceholder(var3, var7)).append('\'');
         var6 = var8.endExclusive;
      }

      String var12 = this.redactWithAliasRegistry(var5.append(var1, var6, var1.length()).toString(), (io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry)null);
      String var13 = var12;

      for (int var9 = 0; var9 < var2.size(); var9++) {
         String var10 = ((io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan)var2.get(var9)).replacement;
         String var11 = var10.substring("‹secret:".length(), var10.length() - 1);
         var13 = var13.replace(callPlaceholder(var3, var9), var10);
         var12 = var12.replace("'" + callPlaceholder(var3, var9) + "'", "secret('" + var11 + "')").replace(callPlaceholder(var3, var9), var10);
      }

      return var13.equals(var4) ? var12 : null;
   }

   private static String callPlaceholder(String var0, int var1) {
      return "‹call:" + var0 + ":" + var1 + "›";
   }

   private static List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> findKnownSecretSpans(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog var1) {
      ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var2 = new ArrayList();
      if (!var1.valuesByName.isEmpty() && var0.indexOf(39) >= 0) {
         for (int var3 = 0; var3 < var0.length(); var3++) {
            char var4 = var0.charAt(var3);
            if (var4 == '\\') {
               var3++;
            } else if (var4 == '"' || var4 == '\'' && (var3 == 0 || !isIdentifierCharacter(var0.charAt(var3 - 1)))) {
               int var5 = findClosingQuote(var0, var3, var4);
               if (var5 >= 0) {
                  String var6 = var4 == '\'' ? lookupKnownSecretAlias(var0.substring(var3 + 1, var5), var1) : null;
                  if (var6 != null) {
                     var2.add(new io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan(var3, var5 + 1, var6));
                  }

                  var3 = var5;
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private static int findClosingQuote(String var0, int var1, char var2) {
      for (int var3 = var1 + 1; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\') {
            var3++;
         } else {
            if (var4 == var2) {
               return var3;
            }

            if (var4 == '\n' || var4 == '\r') {
               return -1;
            }
         }
      }

      return -1;
   }

   private static boolean isIdentifierCharacter(char var0) {
      return isAsciiLetterOrDigit(var0) || var0 == '_' || var0 == '$';
   }

   private static String lookupKnownSecretAlias(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog var1) {
      String var2 = decodeOneEscapeLayer(var0);
      if (var2 != null && !var2.isEmpty()) {
         for (io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry var4 : var1.entries) {
            if (var4.value.equals(var2)) {
               return var4.alias;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public String redactWithAliasRegistry(String var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      if (var1 != null && !var1.isEmpty()) {
         long var3 = var2 == null ? this.learnedSecretsVersion() : var2.size();
         UnaryOperator<String> var5 = var2x -> this.redactWithAliasRegistry(var2x, var2);
         String var6 = this.replaceKnownSecretOccurrences(var1, this.createSecretEntries(true, var2), var5);
         if (var2 != null) {
            for (io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var8 : this.decodeReadings(var6)) {
               if (var8.sourceOffsets != null) {
                  this.observeDecodedReading(var8, var2);
               }
            }

            var6 = this.replaceKnownSecretOccurrences(var6, this.createSecretEntries(true, var2), var5);
         }

         var6 = this.redactMaskedText(var6, var2);
         long var10 = var2 == null ? this.learnedSecretsVersion() : var2.size();
         return var10 == var3 ? var6 : this.replaceKnownSecretOccurrences(var6, this.createSecretEntries(true, var2), var5);
      } else {
         return var1;
      }
   }

   private void observeDecodedReading(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      if (var1.urlDecoded) {
         replaceMatches(var1.text, QUERY_PARAMETER_PATTERN, var2x -> var2x.group(1) + this.redactScalarCandidate(var2x.group(2), 0, var2));
      } else {
         this.redactMaskedText(var1.text, var2);
      }
   }

   public io.karatelabs.ext.agent.llm.SecretRedactor.RedactionLocation findFirstLiteralSecret(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         io.karatelabs.ext.agent.llm.SecretRedactor.RedactionLocation var2 = locateFirstDifference(var1, replaceMatches(this.maskKnownSecrets(var1, this.createSecretEntries(true)), JWT_PATTERN, var0 -> "‹masked›"));
         if (var2 != null) {
            return var2;
         } else {
            for (io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var4 : List.of(io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.SOURCE, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MATCH)) {
               String var5 = selectRedactableSections(var1, var4);
               String var6 = this.redactByMode(var5, null, var4);
               var2 = locateFirstDifference(var5, var4 == io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MATCH ? redactSensitiveAssertions(var6) : redactDataTableCells(var6));
               if (var2 != null) {
                  return var2;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.RedactionLocation locateFirstDifference(String var0, String var1) {
      int var2 = 0;

      while (var2 < var0.length() && var2 < var1.length() && var0.charAt(var2) == var1.charAt(var2)) {
         var2++;
      }

      if (var2 == var0.length() && var2 == var1.length()) {
         return null;
      } else {
         int var3 = var1.lastIndexOf(10, var2 - 1) + 1;
         int var4 = var1.indexOf(10, var2);
         int var5 = 1;

         for (int var6 = 0; var6 < var3; var6++) {
            if (var1.charAt(var6) == '\n') {
               var5++;
            }
         }

         return new io.karatelabs.ext.agent.llm.SecretRedactor.RedactionLocation(var5, var1.substring(var3, var4 < 0 ? var1.length() : var4).strip());
      }
   }

   private static String selectRedactableSections(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var1) {
      StringBuilder var2 = new StringBuilder(var0.length());
      boolean var3 = false;
      boolean var4 = false;

      for (String var8 : var0.split("\n", -1)) {
         String var9 = var8.strip();
         String var10 = "";
         if (var3 || var9.startsWith("\"\"\"")) {
            var10 = var4 ? var8 : "";
            var3 = var3 != var9.startsWith("\"\"\"");
         } else if (STEP_KEYWORD_PATTERN.matcher(var9).lookingAt()) {
            var4 = var1 == io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MATCH == ASSERTION_STEP_PATTERN.matcher(var9).lookingAt();
            var10 = var4 ? var8 : "";
         } else if (var9.startsWith("|")) {
            var10 = var4 ? var8 : "";
         } else if (EXAMPLES_HEADER_PATTERN.matcher(var9).lookingAt()) {
            var4 = var1 == io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.SOURCE;
            var10 = var4 ? "Examples:" : "";
         } else if (!var9.isEmpty() && !var9.startsWith("#")) {
            var4 = false;
         }

         var2.append(var10).append('\n');
      }

      return var2.substring(0, var2.length() - 1);
   }

   private static boolean isProtectedValue(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var1) {
      if (var0.isEmpty() || AUTH_SCHEME_PREFIX_PATTERN.matcher(var0).matches() || INTERPOLATION_PATTERN.matcher(var0).matches()) {
         return true;
      } else {
         return PARENTHESIZED_KARATE_MARKER_PATTERN.matcher(var0).matches()
            ? findRedactableQuotedLiteral(var0.substring(var0.indexOf(40) + 1, var0.length() - 1), 0, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.SOURCE) == null
            : var1 == io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MATCH && KARATE_MARKER_PATTERN.matcher(var0).matches();
      }
   }

   private static String maskUnlessProtected(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var1) {
      return !isProtectedValue(var0, var1) && !SECRET_REFERENCE_PATTERN.matcher(var0).matches() ? "‹masked›" : var0;
   }

   private String maskValueInMode(String var1, int var2, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var3, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var4) {
      return var4.usesFeatureSyntax() ? maskUnlessProtected(var1, var4) : this.redactScalarCandidate(var1, var2, var3);
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral findRedactableQuotedLiteral(String var0, int var1, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;
      int var5 = var1;

      while (var5 < var0.length()) {
         char var6 = var0.charAt(var5);
         if (var6 == '\n' || var6 == '\r') {
            break;
         }

         if (var6 != '"' && var6 != '\'' && var6 != '`') {
            if (var6 != '(' && var6 != '[' && var6 != '{') {
               if (var6 == ')' || var6 == ']' || var6 == '}' || var6 == ',') {
                  if (var3.isEmpty()) {
                     break;
                  }

                  if (var6 != ',') {
                     var4 -= var3.charAt(var3.length() - 1) == 'c' ? 1 : 0;
                     var3.setLength(var3.length() - 1);
                  }
               }
            } else {
               boolean var8 = var6 == '(' && isFunctionCallParenthesis(var0, var5);
               var3.append(var8 ? 'c' : var6);
               var4 += var8 ? 1 : 0;
            }

            var5++;
         } else {
            io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var7 = var6 == '`' ? parseBacktickLiteral(var0, var5) : parseQuotedLiteral(var0, var5);
            if (var7 == null) {
               var5++;
            } else {
               if (var4 == 0 && !isProtectedValue(var7.content, var2)) {
                  return var7;
               }

               var5 = var7.endExclusive;
            }
         }
      }

      return null;
   }

   private static boolean isFunctionCallParenthesis(String var0, int var1) {
      int var2 = var1 - 1;

      while (var2 >= 0 && (var0.charAt(var2) == ' ' || var0.charAt(var2) == '\t')) {
         var2--;
      }

      char var3 = var2 < 0 ? 32 : var0.charAt(var2);
      return isAsciiLetterOrDigit(var3) || var3 == '_' || var3 == '$' || var3 == ')' || var3 == ']';
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral parseBacktickLiteral(String var0, int var1) {
      int var2 = var0.indexOf(96, var1 + 1);
      int var3 = var0.indexOf(10, var1 + 1);
      return var2 >= 0 && (var3 < 0 || var3 >= var2) ? new io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral("`", '`', var0.substring(var1 + 1, var2), "`", var2 + 1) : null;
   }

   private static int literalContentStart(io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var0) {
      return var0.endExclusive - var0.prefix.length() - var0.content.length() - var0.suffix.length();
   }

   private static String redactSensitiveAssertions(String var0) {
      Matcher var1 = SENSITIVE_ASSERTION_PATTERN.matcher(var0);
      StringBuilder var2 = new StringBuilder();
      int var3 = 0;

      while (var1.find()) {
         io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var4 = findRedactableQuotedLiteral(var0, var1.end(), io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MATCH);
         if (var4 != null) {
            var2.append(var0, var3, literalContentStart(var4)).append(var4.prefix).append("‹masked›").append(var4.suffix);
            var3 = var4.endExclusive;
         }
      }

      return var3 == 0 ? var0 : var2.append(var0, var3, var0.length()).toString();
   }

   private static String redactDataTableCells(String var0) {
      String[] var1 = var0.split("\n", -1);
      ArrayList<java.lang.Integer> var2 = null;
      boolean var3 = false;
      boolean var4 = false;

      for (int var5 = 0; var5 < var1.length; var5++) {
         String var6 = var1[var5].strip();
         if (!var6.startsWith("|")) {
            var2 = null;
            if (!var6.isEmpty()) {
               var3 = var6.equals("Examples:");
               var4 = SENSITIVE_TABLE_HEADER_PATTERN.matcher(var6).lookingAt();
            }
         } else {
            var var7 = tableCellRanges(var1[var5]);
            ArrayList<java.lang.String> var8 = new ArrayList();

            for (int[] var10 : (Iterable<int[]>) (Iterable<?>) (var7)) {
               var8.add(var1[var5].substring(var10[0], var10[1]).strip());
            }

            if (var2 == null) {
               var2 = new ArrayList();

               for (int var19 = 0; var19 < var8.size(); var19++) {
                  if (isSensitiveName((String)var8.get(var19))) {
                     var2.add(var19);
                  }
               }
            } else {
               boolean var18 = !var3 && !var8.isEmpty() && isSensitiveName((String)var8.get(0));
               StringBuilder var20 = null;
               int var11 = 0;

               for (int var12 = 0; var12 < var7.size(); var12++) {
                  if (var4 || var18 && var12 > 0 || var2.contains(var12)) {
                     String var13 = (String)var8.get(var12);
                     boolean var14 = var13.length() >= 2
                        && (var13.charAt(0) == '\'' || var13.charAt(0) == '"')
                        && var13.charAt(var13.length() - 1) == var13.charAt(0);
                     String var15 = var14 ? var13.substring(1, var13.length() - 1) : var13;
                     boolean var16 = !var15.isEmpty()
                        && (var3 || (var14 ? !isProtectedValue(var15, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.SOURCE) : NUMBER_LITERAL_PATTERN.matcher(var13).matches() || findRedactableQuotedLiteral(var13, 0, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.SOURCE) != null));
                     if (var16) {
                        if (var20 == null) {
                           var20 = new StringBuilder();
                        }

                        int var17 = var1[var5].indexOf(var13, ((int[])var7.get(var12))[0]) + (var14 ? 1 : 0);
                        var20.append(var1[var5], var11, var17).append("‹masked›");
                        var11 = var17 + var15.length();
                     }
                  }
               }

               if (var20 != null) {
                  var1[var5] = var20.append(var1[var5], var11, var1[var5].length()).toString();
               }
            }
         }
      }

      return String.join("\n", var1);
   }

   private static List<int[]> tableCellRanges(String var0) {
      ArrayList<int[]> var1 = new ArrayList();
      int var2 = var0.indexOf(124) + 1;

      for (int var3 = var2; var3 < var0.length(); var3++) {
         if (var0.charAt(var3) == '|') {
            var1.add(new int[]{var2, var3});
            var2 = var3 + 1;
         }
      }

      return var1;
   }

   private static boolean looksLikeCredentialToken(String var0) {
      if (JWT_PATTERN.matcher(var0).matches()) {
         return true;
      } else {
         int var1 = 0;

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            var1 |= Character.isDigit(var3) ? 1 : (Character.isLowerCase(var3) ? 2 : (Character.isUpperCase(var3) ? 4 : 8));
         }

         boolean var4 = (var1 & 1) != 0 && (var1 & 6) != 0;
         return var0.length() >= 8 && var4 || var0.length() >= 20 && Integer.bitCount(var1) >= 2;
      }
   }

   private String redactMaskedText(String var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      return this.redactByMode(var1, var2, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MASK);
   }

   private String redactByMode(String var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var3) {
      String var4 = this.redactSensitiveAssignments(var1, var2, var3);
      var4 = replaceMatches(
         var4,
         SENSITIVE_LINE_VALUE_PATTERN,
         var3x -> var3x.group(1) != null
            ? var3x.group(1) + this.maskValueInMode(var3x.group(2), 0, var2, var3)
            : (var3x.group(3) != null ? var3x.group(3) + this.maskValueInMode(var3x.group(4), 1, var2, var3) : var3x.group(5) + this.maskValueInMode(var3x.group(6), 0, var2, var3))
      );
      var4 = replaceMatches(var4, SECRET_QUERY_PARAMETER_PATTERN, var3x -> var3x.group(1) + this.maskValueInMode(var3x.group(2), 0, var2, var3));
      var4 = replaceMatches(var4, KEY_VALUE_PAIR_PATTERN, var3x -> this.redactLogKeyValue(var3x.group(1) != null ? var3x.group(1) : var3x.group(2), var3x.group(3), var3x.group(), var2, var3));
      var4 = replaceMatches(
         var4,
         MULTILINE_SECRET_VALUE_PATTERN,
         var3x -> var3x.group(1)
            + (
               var3x.group(2) != null
                  ? this.maskValueInMode(var3x.group(2), 0, var2, var3)
                  : (var3x.group(3) != null ? this.maskValueInMode(var3x.group(3), 1, var2, var3) : this.maskValueInMode(var3x.group(4), 0, var2, var3))
            )
      );
      var4 = replaceMatches(var4, BEARER_TOKEN_PATTERN, var3x -> var3.usesFeatureSyntax() && !looksLikeCredentialToken(var3x.group(2)) ? var3x.group() : var3x.group(1) + this.maskValueInMode(var3x.group(2), 0, var2, var3));
      return replaceMatches(var4, JWT_PATTERN, var3x -> this.maskValueInMode(var3x.group(), 0, var2, var3));
   }

   public String aliasSecretValue(String var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      String var3 = this.secretNameForValue(var1);
      return var3 != null ? secretAlias(var3) : var2.aliasFor(var1);
   }

   public long decodeLimitHitCount() {
      return this.decodeLimitHitCount.get();
   }

   public void observeTextForSecrets(String var1) {
      this.redactKnownAndGeneric(var1);
   }

   public String maskConfiguredSecrets(String var1) {
      return var1 != null && !var1.isEmpty() ? this.maskKnownSecrets(var1, this.createSecretEntries(false)) : var1;
   }

   private String maskKnownSecrets(String var1, List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> var2) {
      return var2.isEmpty() ? var1 : this.replaceKnownSecretOccurrences(var1, var2, var2x -> this.maskKnownSecrets(var2x, var2));
   }

   private List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> createSecretEntries(boolean var1) {
      return this.createSecretEntries(var1, (io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry)null);
   }

   private List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> createSecretEntries(boolean var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2) {
      var var3 = new ArrayList<>(this.secretCatalog().entries);
      if (var2 != null) {
         for (Entry var5 : var2.snapshot().entrySet()) {
            var3.add(new io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry((String)var5.getValue(), (String)var5.getKey(), isPlausibleSecretValue((String)var5.getKey()) ? null : SENSITIVE_CONTEXT_PATTERN));
         }
      } else if (var1) {
         synchronized (this.learnedSecrets) {
            for (String var6 : this.learnedSecrets) {
               var3.add(new io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry("‹masked›", var6, null));
            }
         }
      }

      return var3;
   }

   private io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog secretCatalog() {
      var var1 = this.secretValues.get();
      LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
      if (var1 != null) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if (var4.getValue() != null && !((String)var4.getValue()).isEmpty()) {
               var2.put((String)var4.getKey(), (String)var4.getValue());
            }
         }
      }

      io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog var8 = this.secretCatalog;
      if (var8.valuesByName.equals(var2)) {
         return var8;
      } else {
         ArrayList<Entry<String, String>> var10 = new ArrayList<>(var2.entrySet());
         var10.sort((var0, var1x) -> ((String)var1x.getValue()).length() - ((String)var0.getValue()).length());
         ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> var5 = new ArrayList();

         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var10)) {
            var5.add(new io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry(secretAlias((String)var7.getKey()), (String)var7.getValue(), isPlausibleSecretValue((String)var7.getValue()) ? null : secretNameRegex((String)var7.getKey())));
         }

         var8 = new io.karatelabs.ext.agent.llm.SecretRedactor.SecretCatalog(var2, List.copyOf(var5));
         this.secretCatalog = var8;
         return var8;
      }
   }

   private long learnedSecretsVersion() {
      synchronized (this.learnedSecrets) {
         return this.learnedSecretsVersion;
      }
   }

   private void rememberSecretCandidate(String var1) {
      if (var1.length() <= 16384
         && isPlausibleSecretValue(var1)
         && !var1.startsWith("#")
         && var1.indexOf(8249) < 0
         && var1.indexOf(8250) < 0
         && !SECRET_ENV_NAME_PATTERN.matcher(var1).matches()
         && !this.secretCatalog().valuesByName.containsKey(var1)) {
         synchronized (this.learnedSecrets) {
            if (this.learnedSecrets.add(var1)) {
               this.learnedSecretsVersion++;
            }
         }

         Matcher var5 = AUTH_SCHEME_VALUE_PATTERN.matcher(var1);
         if (var5.matches()) {
            this.rememberSecretCandidate(var5.group(1));
         }
      }
   }

   private String replaceKnownSecretOccurrences(String var1, List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> var2, UnaryOperator<String> var3) {
      ArrayList var4 = new ArrayList();

      for (io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var6 : this.decodeReadings(var1)) {
         for (io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry var8 : var2) {
            collectSecretMatches(var6, var8, var4);
         }
      }

      ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var14 = new ArrayList();
      Matcher var15 = PROTECTED_KARATE_PATTERN.matcher(var1);

      while (var15.find()) {
         var14.add(new io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan(var15.start(), var15.end(), null));
      }

      List var13 = mergeReplacementSpans(var4, var14);
      Matcher var16 = BASE64_CANDIDATE_PATTERN.matcher(var1);
      int var17 = 0;

      while (var16.find()) {
         while (var17 < var13.size() && ((io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan)var13.get(var17)).endExclusive <= var16.start()) {
            var17++;
         }

         if ((var17 >= var13.size() || ((io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan)var13.get(var17)).start >= var16.end()) && !isSpanCovered(var16.start(), var16.end(), var14)) {
            String var9 = redactBase64Candidate(var16.group(), var2, var3);
            if (!var9.equals(var16.group())) {
               var13.add(var17, new io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan(var16.start(), var16.end(), var9));
            }
         }
      }

      if (var13.isEmpty()) {
         return var1;
      } else {
         StringBuilder var18 = new StringBuilder(var1.length());
         int var10 = 0;

         for (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var12 : (Iterable<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan>) (Iterable<?>) (var13)) {
            var18.append(var1, var10, var12.start).append(var12.replacement);
            var10 = var12.endExclusive;
         }

         return var18.append(var1, var10, var1.length()).toString();
      }
   }

   private static void collectSecretMatches(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var0, io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry var1, List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var2) {
      String var3 = var0.text;
      int var4 = var1.value.length();

      for (int var5 = var3.indexOf(var1.value); var5 >= 0; var5 = var3.indexOf(var1.value, var5 + 1)) {
         if (var1.contextPattern == null || hasContextBefore(var1.contextPattern, var3, var5, var5 + var4)) {
            int var6 = var0.sourceOffset(var5);
            int var7 = var0.sourceOffset(var5 + var4);
            if (var7 > var6) {
               var2.add(new io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan(var6, var7, var1.alias));
            }
         }
      }
   }

   private static boolean hasContextBefore(Pattern var0, String var1, int var2, int var3) {
      if (var3 < var1.length() && isAsciiLetterOrDigit(var1.charAt(var3))) {
         return false;
      } else {
         Matcher var4 = var0.matcher(var1);
         var4.useTransparentBounds(true);
         var4.region(Math.max(0, var2 - 400), var2);
         return var4.find();
      }
   }

   private static boolean isAsciiLetterOrDigit(char var0) {
      return var0 >= '0' && var0 <= '9' || var0 >= 'A' && var0 <= 'Z' || var0 >= 'a' && var0 <= 'z';
   }

   private static List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> mergeReplacementSpans(List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var0, List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var1) {
      var0.sort((var0x, var1x) -> var0x.start != var1x.start ? var0x.start - var1x.start : var1x.endExclusive - var0x.endExclusive);
      ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var2 = new ArrayList();

      for (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var4 : var0) {
         if (!isSpanCovered(var4.start, var4.endExclusive, var1)) {
            io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var5 = var2.isEmpty() ? null : (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan)var2.get(var2.size() - 1);
            if (var5 == null || var4.start >= var5.endExclusive) {
               var2.add(var4);
            } else if (var4.endExclusive > var5.endExclusive) {
               var2.set(var2.size() - 1, new io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan(var5.start, var4.endExclusive, var5.replacement));
            }
         }
      }

      return var2;
   }

   private static boolean isSpanCovered(int var0, int var1, List<io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan> var2) {
      for (io.karatelabs.ext.agent.llm.SecretRedactor.ReplacementSpan var4 : var2) {
         if (var4.start <= var0 && var1 <= var4.endExclusive) {
            return true;
         }
      }

      return false;
   }

   private List<io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText> decodeReadings(String var1) {
      ArrayList<io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText> var2 = new ArrayList();
      HashSet<java.lang.String> var3 = new HashSet();
      var2.add(new io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText(var1, null, false));
      var3.add(var1);
      int var4 = 0;

      for (int var5 = 0; var5 < 4; var5++) {
         int var6 = var2.size();

         for (int var7 = var4; var7 < var6; var7++) {
            for (UnaryOperator var9 : READING_DECODERS) {
               io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var10 = (io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText)var9.apply((io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText)var2.get(var7));
               if (var10 != null && var3.add(var10.text)) {
                  if (var2.size() == 32) {
                     if (this.decodeLimitHitCount.getAndIncrement() == 0L) {
                        LOGGER.warn("a text of {} characters has more than {} decoded readings; it is read to that bound", var1.length(), 32);
                     }

                     return var2;
                  }

                  var2.add(var10);
               }
            }
         }

         var4 = var6;
      }

      return var2;
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText decodeHtmlEntities(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var0) {
      String var1 = var0.text;
      if (var1.indexOf(38) < 0) {
         return null;
      } else {
         io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder var2 = new io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder(var0, false);

         for (int var3 = 0; var3 < var1.length(); var3++) {
            if (var1.charAt(var3) == '&') {
               int var4 = var1.indexOf(59, var3 + 2);
               String var5 = var4 > 0 && var4 - var3 <= 10 ? decodeHtmlEntity(var1.substring(var3 + 1, var4)) : null;
               if (var5 != null) {
                  var2.appendDecoded(var3, var5);
                  var3 = var4;
                  continue;
               }
            }

            var2.appendMappedChar(var3);
         }

         return var2.build();
      }
   }

   private static String decodeHtmlEntity(String var0) {
      switch (var0) {
         case "amp":
            return "&";
         case "lt":
            return "<";
         case "gt":
            return ">";
         case "quot":
            return "\"";
         case "apos":
            return "'";
         default:
            if (var0.length() >= 2 && var0.charAt(0) == '#') {
               try {
                  boolean var3 = var0.charAt(1) == 'x' || var0.charAt(1) == 'X';
                  int var4 = Integer.parseInt(var0.substring(var3 ? 2 : 1), var3 ? 16 : 10);
                  return Character.isValidCodePoint(var4) ? Character.toString(var4) : null;
               } catch (NumberFormatException var5) {
                  return null;
               }
            } else {
               return null;
            }
      }
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText decodeJsonEscapes(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var0) {
      String var1 = var0.text;
      if (var1.indexOf(92) < 0) {
         return null;
      } else {
         io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder var2 = new io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder(var0, false);

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = var1.charAt(var3);
            if (var4 == '\\' && var3 + 1 < var1.length()) {
               char var5 = var1.charAt(var3 + 1);

               String var10000 = switch (var5) {
                  case '"', '\'', '/', '\\' -> String.valueOf(var5);
                  case 'b' -> "\b";
                  case 'f' -> "\f";
                  case 'n' -> "\n";
                  case 'r' -> "\r";
                  case 't' -> "\t";
                  case 'u' -> {
                     if (var3 + 6 <= var1.length() && UNICODE_ESCAPE_HEX_PATTERN.matcher(var1).region(var3 + 2, var3 + 6).matches()) {
                        yield String.valueOf((char)Integer.parseInt(var1.substring(var3 + 2, var3 + 6), 16));
                     }

                     yield null;
                  }
                  default -> null;
               };

               String var6 = var10000;
               if (var6 == null) {
                  var2.appendMappedChar(var3);
               } else {
                  var2.appendDecoded(var3, var6);
                  var3 += var5 == 'u' ? 5 : 1;
               }
            } else {
               var2.appendMappedChar(var3);
            }
         }

         return var2.build();
      }
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText decodeUrlEscapes(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var0) {
      String var1 = var0.text;
      if (var1.indexOf(37) < 0 && var1.indexOf(43) < 0) {
         return null;
      } else {
         io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder var2 = new io.karatelabs.ext.agent.llm.SecretRedactor.DecodedReadingBuilder(var0, true);
         byte[] var3 = new byte[4];

         for (int var4 = 0; var4 < var1.length(); var4++) {
            char var5 = var1.charAt(var4);
            if (var5 == '+') {
               var2.appendDecoded(var4, " ");
            } else {
               int var6 = var5 == '%' ? parseHexByte(var1, var4 + 1) : -1;
               int var7 = var6 < 0 ? 0 : (var6 < 128 ? 1 : (var6 < 192 ? 0 : (var6 < 224 ? 2 : (var6 < 240 ? 3 : (var6 < 248 ? 4 : 0)))));
               String var8 = null;
               if (var7 > 0 && var4 + 3 * var7 <= var1.length()) {
                  var3[0] = (byte)var6;

                  int var9;
                  for (var9 = 1; var9 < var7; var9++) {
                     int var10 = var1.charAt(var4 + 3 * var9) == '%' ? parseHexByte(var1, var4 + 3 * var9 + 1) : -1;
                     if (var10 < 128 || var10 >= 192) {
                        break;
                     }

                     var3[var9] = (byte)var10;
                  }

                  if (var9 == var7) {
                     try {
                        var8 = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(var3, 0, var7)).toString();
                     } catch (CharacterCodingException var11) {
                        var8 = null;
                     }
                  }
               }

               if (var8 == null) {
                  var2.appendMappedChar(var4);
               } else {
                  var2.appendDecoded(var4, var8);
                  var4 += 3 * var7 - 1;
               }
            }
         }

         return var2.build();
      }
   }

   private static int parseHexByte(String var0, int var1) {
      if (var1 + 2 > var0.length()) {
         return -1;
      } else {
         int var2 = Character.digit(var0.charAt(var1), 16);
         int var3 = Character.digit(var0.charAt(var1 + 1), 16);
         return var2 >= 0 && var3 >= 0 ? var2 << 4 | var3 : -1;
      }
   }

   private static String redactBase64Candidate(String var0, List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> var1, UnaryOperator<String> var2) {
      String var3 = decodeBase64Utf8(var0);
      if (var3 == null) {
         return var0;
      } else {
         for (io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry var5 : var1) {
            if (var3.equals(var5.value) || var3.length() <= 128 && containsContextualSecret(var3, var5.value)) {
               return var5.alias;
            }
         }

         String var7 = var2.apply(var3);
         if (var7.equals(var3)) {
            return var0;
         } else {
            boolean var8 = var0.indexOf(45) >= 0 || var0.indexOf(95) >= 0;
            Encoder var6 = var8 ? Base64.getUrlEncoder() : Base64.getEncoder();
            return (var0.endsWith("=") ? var6 : var6.withoutPadding()).encodeToString(var7.getBytes(StandardCharsets.UTF_8));
         }
      }
   }

   private static boolean containsContextualSecret(String var0, String var1) {
      if (var0.endsWith(":" + var1)) {
         return true;
      } else {
         Matcher var2 = AUTH_SCHEME_VALUE_PATTERN.matcher(var0);
         return var2.matches() && var2.group(1).equals(var1);
      }
   }

   private static String headerAssignmentPrefixRegex() {
      return "[ \\t]*(?:\\d+ [<>] )?(?:authorization|proxy-authorization|cookie|set-cookie)[ \\t]*:[ \\t]*";
   }

   private static String namedSecretBodyRegex(String var0) {
      return "(?i:name)=\\\\*[\"'](?:" + var0 + ")\\\\*[\"'](?:(?!(?:\\r?\\n|\\\\r?\\\\n){2})[\\s\\S]){0,200}?(?:\\r?\\n|\\\\r?\\\\n){2}";
   }

   private static Pattern secretNameRegex(String var0) {
      StringBuilder var1 = new StringBuilder("(?i:");

      for (char var5 : var0.toCharArray()) {
         var1.append(var5 != '_' && var5 != '-' ? (Character.isLetterOrDigit(var5) ? Pattern.quote(String.valueOf(var5)) : "") : "[_-]?");
      }

      return secretBoundaryRegex(
         "(?i:authorization|proxy-authorization|cookie|set-cookie)|[A-Za-z0-9_-]*(?i:token|password|passwd|secret|api[_-]?key)[A-Za-z0-9_-]*|"
            + var1.append(')')
      );
   }

   private static Pattern secretBoundaryRegex(String var0) {
      String var1 = "(?:(?:(\\\\*[\"'])(?:"
         + var0
         + ")\\1|(?<![\\w\"'\\\\])(?:"
         + var0
         + "))\\s*(?::|=(?!=))\\s*(?:\\\\*[\"'])?|\\\\*[\"']key\\\\*[\"']\\s*:\\s*\\\\*[\"'](?:"
         + var0
         + ")\\\\*[\"']\\s*,\\s*\\\\*[\"']value\\\\*[\"']\\s*:\\s*\\\\*[\"']|(?<=[{\\[]|, )(?:key=(?:"
         + var0
         + "),\\s*value=|(?:"
         + var0
         + ")=)|(?:(?<![\\s\\S])|(?<=[?&\"'\\n]|\\\\n))(?:"
         + var0
         + ")=|"
         + namedSecretBodyRegex(var0)
         + "|<(?:"
         + var0
         + ")(?:\\s[^<>]*)?>)(?:(?i:bearer|basic)[ \\t]+)?|(?<![A-Za-z0-9])(?i:bearer|basic)[ \\t]+";
      return Pattern.compile("(?:" + var1 + ")\\z");
   }

   private static String secretAlias(String var0) {
      int var1 = var0.indexOf(35);
      return "‹secret:" + (var1 < 0 ? var0 : var0.substring(0, var1)).replaceAll("[^A-Za-z0-9_:]", "_") + "›";
   }

   private static String decodeBase64Utf8(String var0) {
      String var1 = var0.replace('-', '+').replace('_', '/').replace("=", "");
      if (var1.length() % 4 == 1) {
         return null;
      } else {
         String var2;
         try {
            var2 = StandardCharsets.UTF_8
               .newDecoder()
               .decode(ByteBuffer.wrap(Base64.getDecoder().decode(var1 + "==".substring(0, (4 - var1.length() % 4) % 4))))
               .toString();
         } catch (CharacterCodingException | IllegalArgumentException var4) {
            return null;
         }

         return var2.indexOf(0) < 0 ? var2 : null;
      }
   }

   private String redactSensitiveAssignments(String var1, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var2, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var3) {
      Matcher var4 = SENSITIVE_ASSIGNMENT_PATTERN.matcher(var1);
      StringBuilder var5 = new StringBuilder();
      int var6 = 0;
      int var7 = 0;

      while (var7 < var1.length() && var4.find(var7)) {
         String var8 = var4.group(2) != null ? var4.group(2) : var4.group(3);
         var7 = var4.end();
         if (SENSITIVE_NAME_PATTERN.matcher(var8).matches()) {
            boolean var9 = isTokenCollectionName(var8);
            String var10 = var4.group(1) == null ? "\"" : var4.group(1).replace('\'', '"');
            boolean var11 = "=".equals(var4.group(4));
            int var12 = var4.end();
            io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var15 = parseQuotedLiteral(var1, var12);
            Matcher var16 = NUMBER_LITERAL_PATTERN.matcher(var1).region(var12, var1.length());
            String var13;
            int var14;
            if (var3.usesFeatureSyntax()) {
               if (var11 && var1.lastIndexOf(10, var12 - 1) >= var4.end(4) && var1.startsWith("\"\"\"", var12)) {
                  int[] var18 = findTripleQuotedBody(var1, var12, var3);
                  if (var18 == null) {
                     continue;
                  }

                  var13 = var1.substring(var12, var18[0]) + "‹masked›";
                  var14 = var18[1];
               } else {
                  io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var17;
                  if ((var17 = findRedactableQuotedLiteral(var1, var12, var3)) != null) {
                     var13 = var1.substring(var12, literalContentStart(var17)) + var17.prefix + "‹masked›" + var17.suffix;
                     var14 = var17.endExclusive;
                  } else {
                     if (!var16.lookingAt() || var9) {
                        continue;
                     }

                     var13 = "‹masked›";
                     var14 = var16.end();
                  }
               }
            } else if (var15 != null) {
               if (var11 && var15.quote == '\'') {
                  continue;
               }

               var13 = this.redactQuotedContent(var15, var8, var2, var3);
               var14 = var15.endExclusive;
            } else if (var16.lookingAt()) {
               if (var9 || var11) {
                  continue;
               }

               var13 = var10 + aliasOrMask(var16.group(), var2) + var10;
               var14 = var16.end();
            } else {
               if (var11 || var12 >= var1.length() || var1.charAt(var12) != '[' && var1.charAt(var12) != '{') {
                  continue;
               }

               StringBuilder var19 = new StringBuilder();
               var14 = this.redactNestedValue(var1, var12, var10, var19, var2);
               var13 = var19.toString();
            }

            var5.append(var1, var6, var12).append(var13);
            var6 = var14;
            var7 = var14;
         }
      }

      return var6 == 0 ? var1 : var5.append(var1, var6, var1.length()).toString();
   }

   private static int[] findTripleQuotedBody(String var0, int var1, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var2) {
      int var3 = var0.indexOf(10, var1);
      int var4 = var3 < 0 ? -1 : var0.indexOf("\"\"\"", var3);
      int var5 = var4 < 0 ? -1 : var0.lastIndexOf(10, var4);
      if (var5 < var3) {
         return null;
      } else if (isProtectedValue(var0.substring(var3 + 1, var5).strip(), var2)) {
         return null;
      } else {
         int var6 = var3 + 1;

         while (var6 < var5) {
            int var7 = Math.min(var5, var0.indexOf(10, var6) < 0 ? var5 : var0.indexOf(10, var6));
            String var8 = var0.substring(var6, var7);
            if (!var8.isBlank()) {
               return new int[]{var6 + var8.length() - var8.stripLeading().length(), var7};
            }

            var6 = var7 + 1;
         }

         return null;
      }
   }

   private static boolean isTokenCollectionName(String var0) {
      return !SENSITIVE_NAME_PATTERN.matcher(var0.toLowerCase(Locale.ROOT).replace("tokens", "")).matches();
   }

   private String redactLogKeyValue(String var1, String var2, String var3, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var4, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var5) {
      return !var2.isEmpty() && var2.charAt(0) != '{' && var2.charAt(0) != '[' && (!isTokenCollectionName(var1) || !NUMBER_LITERAL_PATTERN.matcher(var2).matches())
         ? var3.substring(0, var3.length() - var2.length()) + this.maskValueInMode(var2, 0, var4, var5)
         : var3;
   }

   private int redactNestedValue(String var1, int var2, String var3, StringBuilder var4, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var5) {
      int var6 = 0;
      int var7 = var2;

      while (var7 < var1.length()) {
         char var8 = var1.charAt(var7);
         if (var8 != '[' && var8 != '{') {
            if (var8 == ']' || var8 == '}') {
               var4.append(var8);
               var7++;
               if (--var6 == 0) {
                  break;
               }
               continue;
            }

            if (var8 == '"' || var8 == '\'' || var8 == '\\') {
               io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var12 = parseQuotedLiteral(var1, var7);
               if (var12 != null) {
                  boolean var13 = COLON_PREFIX_PATTERN.matcher(var1).region(var12.endExclusive, var1.length()).lookingAt();
                  Matcher var11 = OBJECT_KEY_PREFIX_PATTERN.matcher(var4).region(Math.max(0, var4.length() - 80), var4.length());
                  var4.append(var13 ? var1.substring(var7, var12.endExclusive) : this.redactQuotedContent(var12, var11.find() ? var11.group(1) : "", var5, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode.MASK));
                  var7 = var12.endExclusive;
                  continue;
               }
            } else if (Character.isDigit(var8) || var8 == '-') {
               Matcher var9 = NUMBER_LITERAL_PATTERN.matcher(var1).region(var7, var1.length());
               char var10 = var7 == 0 ? 32 : var1.charAt(var7 - 1);
               if (var9.lookingAt() && !Character.isLetterOrDigit(var10) && var10 != '_' && var10 != '.' && var10 != '$') {
                  var4.append(var3).append(aliasOrMask(var9.group(), var5)).append(var3);
                  var7 = var9.end();
                  continue;
               }
            }
         } else {
            var6++;
         }

         var4.append(var8);
         var7++;
      }

      return var7;
   }

   private static io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral parseQuotedLiteral(String var0, int var1) {
      int var2 = var1;

      while (var2 < var0.length() && var0.charAt(var2) == '\\') {
         var2++;
      }

      if (var2 < var0.length() && (var0.charAt(var2) == '"' || var0.charAt(var2) == '\'')) {
         char var3 = var0.charAt(var2);
         int var4 = var2 - var1;

         for (int var5 = ++var2; var2 < var0.length(); var2++) {
            int var6;
            for (var6 = 0; var2 < var0.length() && var0.charAt(var2) == '\\'; var2++) {
               var6++;
            }

            if (var2 >= var0.length()) {
               return null;
            }

            if (var0.charAt(var2) == var3) {
               int var7 = var6 < var4 ? -1 : (var6 - var4) % (2 * (var4 + 1));
               if (var7 == 0) {
                  return new io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral(var0.substring(var1, var5), var3, var0.substring(var5, var2 - var4), var0.substring(var2 - var4, var2 + 1), var2 + 1);
               }

               if (var7 != var4 + 1) {
                  return null;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private String redactQuotedContent(io.karatelabs.ext.agent.llm.SecretRedactor.QuotedLiteral var1, String var2, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var3, io.karatelabs.ext.agent.llm.SecretRedactor.RedactionMode var4) {
      String var5 = var1.content;
      if (var4.usesFeatureSyntax()) {
         return var1.prefix + (isProtectedValue(var5, var4) ? var5 : "‹masked›") + var1.suffix;
      } else {
         boolean var6 = var1.quote == '\'' && (var5.startsWith("#") || "secret".equalsIgnoreCase(var2) && SECRET_ENV_NAME_PATTERN.matcher(var5).matches());
         return var1.prefix + (var6 ? var5 : this.redactScalarCandidate(var5, quotedEscapeDecodeDepth(var1.prefix), var3)) + var1.suffix;
      }
   }

   private static int quotedEscapeDecodeDepth(String var0) {
      int var1 = var0.length() - 1;
      int var2 = 1;

      for (int var3 = var1 + 1; var3 > 1 && var3 % 2 == 0; var3 /= 2) {
         var2++;
      }

      return var2;
   }

   private String redactScalarCandidate(String var1, int var2, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var3) {
      if (!var1.isEmpty()
         && !SECRET_REFERENCE_PATTERN.matcher(var1).matches()
         && !INTERNAL_TOKEN_PATTERN.matcher(var1).matches()
         && !KARATE_PLACEHOLDER_PATTERN.matcher(var1).matches()
         && !AUTH_SCHEME_PREFIX_PATTERN.matcher(var1).matches()
         && !this.secretCatalog().valuesByName.containsKey(var1)) {
         String var4 = var1;

         for (int var5 = 0; var5 < var2 && var4 != null && var4.indexOf(92) >= 0; var5++) {
            var4 = decodeOneEscapeLayer(var4);
         }

         if (var3 != null) {
            if (var4 == null) {
               var4 = var1;
            }

            if (INTERNAL_TOKEN_PATTERN.matcher(var4).find()) {
               return var1;
            } else {
               Matcher var7 = AUTH_SCHEME_VALUE_PATTERN.matcher(var4);
               String var6 = this.aliasSecretValue(var7.matches() ? var7.group(1) : var4, var3);
               return var7.matches() ? var4.substring(0, var7.start(1)) + var6 : var6;
            }
         } else {
            if (var4 != null) {
               this.rememberSecretCandidate(var4);
            }

            if (!var1.equals(var4)) {
               this.rememberSecretCandidate(var1);
            }

            return "‹masked›";
         }
      } else {
         return var1;
      }
   }

   private static String aliasOrMask(String var0, io.karatelabs.ext.agent.llm.SecretRedactor.SecretAliasRegistry var1) {
      return var1 == null ? "‹masked›" : var1.aliasFor(var0);
   }

   private String secretNameForValue(String var1) {
      for (Entry var3 : this.secretCatalog().valuesByName.entrySet()) {
         if (((String)var3.getValue()).equals(var1)) {
            return (String)var3.getKey();
         }
      }

      return null;
   }

   private static String decodeOneEscapeLayer(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 != '\\') {
            var1.append(var3);
         } else {
            if (++var2 == var0.length()) {
               return null;
            }

            char var4 = var0.charAt(var2);
            switch (var4) {
               case '"':
               case '\'':
               case '/':
               case '\\':
                  var1.append(var4);
                  break;
               case 'b':
                  var1.append('\b');
                  break;
               case 'f':
                  var1.append('\f');
                  break;
               case 'n':
                  var1.append('\n');
                  break;
               case 'r':
                  var1.append('\r');
                  break;
               case 't':
                  var1.append('\t');
                  break;
               case 'u':
                  if (var2 + 4 >= var0.length()) {
                     return null;
                  }

                  try {
                     var1.append((char)Integer.parseInt(var0.substring(var2 + 1, var2 + 5), 16));
                  } catch (NumberFormatException var6) {
                     return null;
                  }

                  var2 += 4;
                  break;
               default:
                  return null;
            }
         }
      }

      return var1.toString();
   }

   private static String replaceMatches(String var0, Pattern var1, Function<Matcher, String> var2) {
      Matcher var3 = var1.matcher(var0);
      if (!var3.find()) {
         return var0;
      } else {
         StringBuilder var4 = new StringBuilder();

         do {
            var3.appendReplacement(var4, Matcher.quoteReplacement((String)var2.apply(var3)));
         } while (var3.find());

         var3.appendTail(var4);
         return var4.toString();
      }
   }

   public static final class SecretAliasRegistry {
      private final Map<String, String> aliasesByValue = new LinkedHashMap<>();

      public synchronized String aliasFor(String var1) {
         return this.aliasesByValue.computeIfAbsent(var1, var1x -> "‹masked:k" + (this.aliasesByValue.size() + 1) + "›");
      }

      public synchronized int size() {
         return this.aliasesByValue.size();
      }

      synchronized Map<String, String> snapshot() {
         return new LinkedHashMap<>(this.aliasesByValue);
      }
   }

   private record SecretEntry(String alias, String value, Pattern contextPattern) {

   }

   public record RedactionLocation(int lineNumber, String lineText) {

   }

   private static enum RedactionMode {
      MASK,
      SOURCE,
      MATCH;

      boolean usesFeatureSyntax() {
         return this != MASK;
      }
   }

   private static final class DecodedReadingBuilder {
      private final io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText sourceReading;
      private final boolean urlDecoded;
      private final StringBuilder text;
      private final int[] sourceOffsets;
      private boolean changed;

      DecodedReadingBuilder(io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText var1, boolean var2) {
         this.sourceReading = var1;
         this.urlDecoded = var1.urlDecoded || var2;
         this.text = new StringBuilder(var1.text.length());
         this.sourceOffsets = new int[var1.text.length() + 1];
      }

      void appendMappedChar(int var1) {
         this.sourceOffsets[this.text.length()] = this.sourceReading.sourceOffset(var1);
         this.text.append(this.sourceReading.text.charAt(var1));
      }

      void appendDecoded(int var1, CharSequence var2) {
         for (int var3 = 0; var3 < var2.length(); var3++) {
            this.sourceOffsets[this.text.length()] = this.sourceReading.sourceOffset(var1);
            this.text.append(var2.charAt(var3));
         }

         this.changed = true;
      }

      io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText build() {
         if (!this.changed) {
            return null;
         } else {
            this.sourceOffsets[this.text.length()] = this.sourceReading.sourceOffset(this.sourceReading.text.length());
            return new io.karatelabs.ext.agent.llm.SecretRedactor.DecodedText(this.text.toString(), this.sourceOffsets, this.urlDecoded);
         }
      }
   }

   private record ReplacementSpan(int start, int endExclusive, String replacement) {

   }

   private record SecretCatalog(Map<String, String> valuesByName, List<io.karatelabs.ext.agent.llm.SecretRedactor.SecretEntry> entries) {

   }

   private record QuotedLiteral(String prefix, char quote, String content, String suffix, int endExclusive) {

   }

   private record DecodedText(String text, int[] sourceOffsets, boolean urlDecoded) {


      int sourceOffset(int var1) {
         return this.sourceOffsets == null ? var1 : this.sourceOffsets[var1];
      }
   }
}
