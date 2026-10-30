package io.karatelabs.match;

import io.karatelabs.common.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validators {
   static final Map<String, Validators.Validator> VALIDATORS = new HashMap<>(11);

   static {
      VALIDATORS.put("array", v -> v.isList() ? Result.PASS : Result.fail("not an array or list"));
      VALIDATORS.put("boolean", v -> v.isBoolean() ? Result.PASS : Result.fail("not a boolean"));
      VALIDATORS.put("ignore", v -> Result.PASS);
      VALIDATORS.put("notnull", v -> v.isNull() ? Result.fail("null") : Result.PASS);
      VALIDATORS.put("null", v -> v.isNull() ? Result.PASS : Result.fail("not null"));
      VALIDATORS.put("number", v -> v.isNumber() ? Result.PASS : Result.fail("not a number"));
      VALIDATORS.put("object", v -> v.isMap() ? Result.PASS : Result.fail("not an object or map"));
      VALIDATORS.put("present", v -> v.isNotPresent() ? Result.fail("not present") : Result.PASS);
      VALIDATORS.put("notpresent", v -> v.isNotPresent() ? Result.PASS : Result.fail("present"));
      VALIDATORS.put("string", v -> v.isNotPresent() ? Result.fail("not present") : (v.isString() ? Result.PASS : Result.fail("not a string")));
      VALIDATORS.put("uuid", v -> {
         if (!v.isString()) {
            return Result.fail("not a string");
         } else {
            try {
               UUID.fromString(v.getValue());
               return Result.PASS;
            } catch (Exception var2) {
               return Result.fail("not a valid uuid");
            }
         }
      });
   }

   static class RegexValidator implements Validators.Validator {
      private final Pattern pattern;

      public RegexValidator(String regex) {
         regex = StringUtils.trimToEmpty(regex);
         this.pattern = Pattern.compile(regex);
      }

      public Result apply(Value v) {
         if (!v.isString()) {
            return Result.fail("not a string");
         } else {
            String strValue = v.getValue();
            Matcher matcher = this.pattern.matcher(strValue);
            return matcher.matches() ? Result.PASS : Result.fail("regex match failed");
         }
      }
   }

   interface Validator extends Function<Value, Result> {
   }
}
