package io.karatelabs.core;

import io.karatelabs.js.Args;
import io.karatelabs.js.SimpleObject;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Faker implements SimpleObject {
   private static final Random RANDOM = new Random();
   private static final String[] FIRST_NAMES = new String[]{
      "Ethan", "Sophia", "Jackson", "Emma", "Liam", "Olivia", "Noah", "Ava", "William", "Isabella", "James", "Mia"
   };
   private static final String[] LAST_NAMES = new String[]{
      "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez"
   };
   private static final String[] CITIES = new String[]{
      "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "San Francisco", "Seattle", "Boston", "Denver", "Austin"
   };
   private static final String[] COUNTRIES = new String[]{
      "United States", "Canada", "United Kingdom", "Germany", "France", "Japan", "Australia", "Spain", "Italy", "Netherlands"
   };
   private static final String[] EMAIL_DOMAINS = new String[]{"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "example.com"};
   private static final String[] STREET_NAMES = new String[]{
      "Main Street", "Oak Avenue", "Park Road", "Cedar Lane", "Elm Street", "Maple Drive", "Pine Street", "Washington Boulevard", "Lake View", "Hill Road"
   };
   private static final String[] WORDS = new String[]{
      "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore"
   };
   private static final String[] COMPANY_SUFFIXES = new String[]{"Inc", "LLC", "Corp", "Ltd", "Group", "Solutions", "Technologies", "Systems"};
   private static final String[] JOB_TITLES = new String[]{
      "Software Engineer", "Product Manager", "Data Analyst", "Designer", "Marketing Manager", "Sales Representative", "HR Manager", "Accountant"
   };

   @Override
   public Object jsGet(String key) {
      return switch (key) {
         case "timestamp" -> Args.invoke(() -> Instant.now().getEpochSecond());
         case "timestampMs" -> Args.invoke(() -> Instant.now().toEpochMilli());
         case "isoTimestamp" -> Args.invoke(() -> Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
         case "firstName" -> Args.invoke(() -> FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)]);
         case "lastName" -> Args.invoke(() -> LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)]);
         case "fullName" -> Args.invoke(() -> FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)]);
         case "email" -> Args.invoke(() -> {
            String first = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)].toLowerCase();
            String domain = EMAIL_DOMAINS[RANDOM.nextInt(EMAIL_DOMAINS.length)];
            return first + RANDOM.nextInt(100) + "@" + domain;
         });
         case "userName" -> Args.invoke(() -> {
            String first = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)].toLowerCase();
            String last = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)].toLowerCase();
            return first + "." + last;
         });
         case "phoneNumber" -> Args.invoke(() -> "+1-555-" + String.format("%03d", RANDOM.nextInt(1000)) + "-" + String.format("%04d", RANDOM.nextInt(10000)));
         case "city" -> Args.invoke(() -> CITIES[RANDOM.nextInt(CITIES.length)]);
         case "country" -> Args.invoke(() -> COUNTRIES[RANDOM.nextInt(COUNTRIES.length)]);
         case "streetAddress" -> Args.invoke(() -> RANDOM.nextInt(9999) + 1 + " " + STREET_NAMES[RANDOM.nextInt(STREET_NAMES.length)]);
         case "zipCode" -> Args.invoke(() -> String.format("%05d", RANDOM.nextInt(100000)));
         case "latitude" -> Args.invoke(() -> RANDOM.nextDouble() * 180.0 - 90.0);
         case "longitude" -> Args.invoke(() -> RANDOM.nextDouble() * 360.0 - 180.0);
         case "randomInt" -> Args.invoke(args -> {
            if (args.length == 0) {
               return RANDOM.nextInt(1001);
            } else if (args.length == 1) {
               return RANDOM.nextInt(((Number)args[0]).intValue() + 1);
            } else {
               int min = ((Number)args[0]).intValue();
               int max = ((Number)args[1]).intValue();
               return RANDOM.nextInt(max - min + 1) + min;
            }
         });
         case "randomFloat" -> Args.invoke(args -> {
            if (args.length == 0) {
               return RANDOM.nextDouble();
            } else if (args.length == 1) {
               return RANDOM.nextDouble() * ((Number)args[0]).doubleValue();
            } else {
               double min = ((Number)args[0]).doubleValue();
               double max = ((Number)args[1]).doubleValue();
               return RANDOM.nextDouble() * (max - min) + min;
            }
         });
         case "randomBoolean" -> Args.invoke(RANDOM::nextBoolean);
         case "word" -> Args.invoke(() -> WORDS[RANDOM.nextInt(WORDS.length)]);
         case "sentence" -> Args.invoke(() -> {
            int wordCount = 5 + RANDOM.nextInt(10);
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toUpperCase(WORDS[RANDOM.nextInt(WORDS.length)].charAt(0)));
            sb.append(WORDS[RANDOM.nextInt(WORDS.length)].substring(1));

            for (int i = 1; i < wordCount; i++) {
               sb.append(" ").append(WORDS[RANDOM.nextInt(WORDS.length)]);
            }

            sb.append(".");
            return sb.toString();
         });
         case "paragraph" -> Args.invoke(() -> {
            int sentenceCount = 3 + RANDOM.nextInt(4);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < sentenceCount; i++) {
               if (i > 0) {
                  sb.append(" ");
               }

               int wordCount = 5 + RANDOM.nextInt(10);
               sb.append(Character.toUpperCase(WORDS[RANDOM.nextInt(WORDS.length)].charAt(0)));
               sb.append(WORDS[RANDOM.nextInt(WORDS.length)].substring(1));

               for (int j = 1; j < wordCount; j++) {
                  sb.append(" ").append(WORDS[RANDOM.nextInt(WORDS.length)]);
               }

               sb.append(".");
            }

            return sb.toString();
         });
         case "alphanumeric" -> Args.invoke(args -> {
            int length = args.length > 0 ? ((Number)args[0]).intValue() : 10;
            return generateAlphanumeric(length);
         });
         case "hexColor" -> Args.invoke(() -> String.format("#%06x", RANDOM.nextInt(16777216)));
         case "companyName" -> Args.invoke(() -> {
            String name = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
            String suffix = COMPANY_SUFFIXES[RANDOM.nextInt(COMPANY_SUFFIXES.length)];
            return name + " " + suffix;
         });
         case "jobTitle" -> Args.invoke(() -> JOB_TITLES[RANDOM.nextInt(JOB_TITLES.length)]);
         case "creditCardNumber" -> Args.invoke(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("4");

            for (int i = 1; i < 16; i++) {
               sb.append(RANDOM.nextInt(10));
            }

            return sb.toString();
         });
         default -> null;
      };
   }

   private static String generateAlphanumeric(int length) {
      String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
      StringBuilder sb = new StringBuilder(length);

      for (int i = 0; i < length; i++) {
         sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
      }

      return sb.toString();
   }
}
