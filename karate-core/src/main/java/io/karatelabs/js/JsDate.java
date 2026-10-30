package io.karatelabs.js;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;

non-sealed class JsDate extends JsObject implements JsDateValue {
   static final DateTimeFormatter TO_STRING_PARSER = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.ENGLISH);
   static final DateTimeFormatter UTC_STRING_PARSER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
   static final double MS_PER_SECOND = 1000.0;
   static final double MS_PER_MINUTE = 60000.0;
   static final double MS_PER_HOUR = 3600000.0;
   static final double MS_PER_DAY = 8.64E7;
   static final double MAX_TIME = 8.64E15;
   private double timeValue;
   private Object javaSource;

   static String formatYear4(int year) {
      return (year < 0 ? "-" : "") + String.format(Locale.ROOT, "%04d", Math.abs(year));
   }

   static String formatYearIso(int year) {
      return year >= 0 && year <= 9999 ? String.format(Locale.ROOT, "%04d", year) : (year < 0 ? "-" : "+") + String.format(Locale.ROOT, "%06d", Math.abs(year));
   }

   static String formatGmtOffset(int offsetSeconds) {
      int totalMin = offsetSeconds / 60;
      char sign = (char)(totalMin >= 0 ? 43 : 45);
      int abs = Math.abs(totalMin);
      return String.format(Locale.ROOT, "GMT%c%02d%02d", sign, abs / 60, abs % 60);
   }

   JsDate() {
      super(null, JsDatePrototype.INSTANCE);
      this.timeValue = System.currentTimeMillis();
   }

   JsDate(double timeValue) {
      super(null, JsDatePrototype.INSTANCE);
      this.timeValue = timeClip(timeValue);
   }

   JsDate(long timestamp) {
      this((double)timestamp);
   }

   JsDate(Date date) {
      this(date == null ? Double.NaN : date.getTime());
      this.javaSource = date;
   }

   JsDate(int year, int month, int date) {
      this(makeUtcFromLocalParts(year, month, date, 0, 0, 0, 0));
   }

   JsDate(int year, int month, int date, int hours, int minutes, int seconds) {
      this(makeUtcFromLocalParts(year, month, date, hours, minutes, seconds, 0));
   }

   JsDate(int year, int month, int date, int hours, int minutes, int seconds, int ms) {
      this(makeUtcFromLocalParts(year, month, date, hours, minutes, seconds, ms));
   }

   JsDate(Instant instant) {
      this(instant == null ? Double.NaN : instant.toEpochMilli());
      this.javaSource = instant;
   }

   JsDate(LocalDateTime ldt) {
      this(ldt == null ? Double.NaN : ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      this.javaSource = ldt;
   }

   JsDate(LocalDate ld) {
      this(ld == null ? Double.NaN : ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
      this.javaSource = ld;
   }

   JsDate(ZonedDateTime zdt) {
      this(zdt == null ? Double.NaN : zdt.toInstant().toEpochMilli());
      this.javaSource = zdt;
   }

   JsDate(String text) {
      this(parseToTimeValue(text));
   }

   boolean isInvalid() {
      return Double.isNaN(this.timeValue);
   }

   double getTimeValue() {
      return this.timeValue;
   }

   long getTime() {
      return (long)this.timeValue;
   }

   void setTimeValue(double v) {
      this.timeValue = timeClip(v);
      this.javaSource = null;
   }

   @Override
   public Object getOriginalJavaValue() {
      return this.javaSource;
   }

   @Override
   public String toString() {
      if (this.isInvalid()) {
         return "Invalid Date";
      } else {
         ZonedDateTime z = ZonedDateTime.ofInstant(Instant.ofEpochMilli((long)this.timeValue), ZoneId.systemDefault());
         String dayName = z.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
         String monthName = z.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
         return String.format(
            Locale.ROOT,
            "%s %s %02d %s %02d:%02d:%02d %s",
            dayName,
            monthName,
            z.getDayOfMonth(),
            formatYear4(z.getYear()),
            z.getHour(),
            z.getMinute(),
            z.getSecond(),
            formatGmtOffset((int)(localTzaMs((long)this.timeValue) / 1000L))
         );
      }
   }

   @Override
   public Object getJavaValue() {
      if (this.isInvalid()) {
         return null;
      } else {
         return this.javaSource != null ? this.javaSource : new Date((long)this.timeValue);
      }
   }

   @Override
   public Object getJsValue() {
      return this.isInvalid() ? Double.NaN : (long)this.timeValue;
   }

   static double timeClip(double v) {
      return Double.isFinite(v) && !(Math.abs(v) > 8.64E15) ? (long)v : Double.NaN;
   }

   static double makeTime(double h, double m, double s, double milli) {
      return Double.isFinite(h) && Double.isFinite(m) && Double.isFinite(s) && Double.isFinite(milli)
         ? trunc(h) * 3600000.0 + trunc(m) * 60000.0 + trunc(s) * 1000.0 + trunc(milli)
         : Double.NaN;
   }

   static double makeDay(double year, double month, double date) {
      if (Double.isFinite(year) && Double.isFinite(month) && Double.isFinite(date)) {
         long y = (long)trunc(year);
         long m = (long)trunc(month);
         long dt = (long)trunc(date);
         long ym = y + Math.floorDiv(m, 12);
         long mn = Math.floorMod(m, 12) + 1;
         if (ym <= 999999999L && ym >= -999999999L) {
            try {
               LocalDate ld = LocalDate.of((int)ym, (int)mn, 1);
               return (double)ld.toEpochDay() + (dt - 1L);
            } catch (Exception var17) {
               return Double.NaN;
            }
         } else {
            return Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   static double makeDate(double day, double time) {
      if (Double.isFinite(day) && Double.isFinite(time)) {
         double tv = day * 8.64E7 + time;
         return !Double.isFinite(tv) ? Double.NaN : tv;
      } else {
         return Double.NaN;
      }
   }

   static double localToUtc(double localMs) {
      return !Double.isFinite(localMs) ? Double.NaN : localMs - localTzaMs((long)localMs);
   }

   static double utcToLocal(double utcMs) {
      return !Double.isFinite(utcMs) ? Double.NaN : utcMs + localTzaMs((long)utcMs);
   }

   static long localTzaMs(long ms) {
      int totalSeconds = ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochMilli(ms)).getTotalSeconds();
      int minutes = totalSeconds / 60;
      return minutes * 60000L;
   }

   private static double trunc(double v) {
      return v < 0.0 ? Math.ceil(v) : Math.floor(v);
   }

   private static double makeUtcFromLocalParts(int year, int month, int date, int hours, int minutes, int seconds, int ms) {
      double day = makeDay(year, month, date);
      double time = makeTime(hours, minutes, seconds, ms);
      double local = makeDate(day, time);
      return timeClip(localToUtc(local));
   }

   static double parseToTimeValue(String s) {
      if (s == null) {
         return Double.NaN;
      } else {
         String dateStr = s.trim();
         if (dateStr.isEmpty()) {
            return Double.NaN;
         } else {
            if (dateStr.length() > 7 && (dateStr.charAt(0) == '+' || dateStr.charAt(0) == '-')) {
               double v = parseExtendedYearForm(dateStr);
               if (!Double.isNaN(v)) {
                  return timeClip(v);
               }
            }

            try {
               return Instant.parse(dateStr).toEpochMilli();
            } catch (Exception var9) {
               if (!dateStr.contains("T") && !dateStr.contains(" ")) {
                  try {
                     if (dateStr.matches("[+-]?\\d{4,6}")) {
                        int y = Integer.parseInt(dateStr);
                        return LocalDate.of(y, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                     }

                     if (dateStr.matches("[+-]?\\d{4,6}-\\d{2}")) {
                        int dash = dateStr.lastIndexOf(45);
                        int y = Integer.parseInt(dateStr.substring(0, dash));
                        int m = Integer.parseInt(dateStr.substring(dash + 1));
                        return LocalDate.of(y, m, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                     }

                     LocalDate ld = LocalDate.parse(dateStr);
                     return ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                  } catch (Exception var8) {
                  }
               }

               try {
                  if (dateStr.contains("T")) {
                     String trimmed = dateStr.endsWith("Z") ? dateStr.substring(0, dateStr.length() - 1) : dateStr;
                     LocalDateTime ldt = LocalDateTime.parse(trimmed);
                     if (dateStr.endsWith("Z")) {
                        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
                     }

                     return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                  }
               } catch (Exception var7) {
               }

               try {
                  return ZonedDateTime.parse(dateStr, TO_STRING_PARSER).toInstant().toEpochMilli();
               } catch (Exception var6) {
                  try {
                     LocalDateTime ldt = LocalDateTime.parse(dateStr, UTC_STRING_PARSER);
                     return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
                  } catch (Exception var5) {
                     return Double.NaN;
                  }
               }
            }
         }
      }
   }

   private static double parseExtendedYearForm(String s) {
      char sign = s.charAt(0);
      int i = 1;
      if (s.length() >= 7 && isDigits(s, 1, 7)) {
         int year = Integer.parseInt(s.substring(1, 7));
         if (sign == '-') {
            if (year == 0) {
               return Double.NaN;
            }

            year = -year;
         }

         i = 7;
         int month = 1;
         int day = 1;
         int hour = 0;
         int minute = 0;
         int second = 0;
         int millis = 0;
         long offsetMs = 0L;
         if (i < s.length() && s.charAt(i) == '-') {
            if (i + 3 > s.length() || !isDigits(s, i + 1, i + 3)) {
               return Double.NaN;
            }

            month = Integer.parseInt(s.substring(i + 1, i + 3));
            i += 3;
            if (i < s.length() && s.charAt(i) == '-') {
               if (i + 3 > s.length() || !isDigits(s, i + 1, i + 3)) {
                  return Double.NaN;
               }

               day = Integer.parseInt(s.substring(i + 1, i + 3));
               i += 3;
            }
         }

         if (i < s.length() && s.charAt(i) == 'T') {
            if (i + 6 > s.length() || !isDigits(s, i + 1, i + 3) || s.charAt(i + 3) != ':' || !isDigits(s, i + 4, i + 6)) {
               return Double.NaN;
            }

            hour = Integer.parseInt(s.substring(i + 1, i + 3));
            minute = Integer.parseInt(s.substring(i + 4, i + 6));
            i += 6;
            if (i < s.length() && s.charAt(i) == ':') {
               if (i + 3 > s.length() || !isDigits(s, i + 1, i + 3)) {
                  return Double.NaN;
               }

               second = Integer.parseInt(s.substring(i + 1, i + 3));
               i += 3;
               if (i < s.length() && s.charAt(i) == '.') {
                  if (i + 4 > s.length() || !isDigits(s, i + 1, i + 4)) {
                     return Double.NaN;
                  }

                  millis = Integer.parseInt(s.substring(i + 1, i + 4));
                  i += 4;
               }
            }

            if (i < s.length()) {
               char z = s.charAt(i);
               if (z == 'Z') {
                  offsetMs = 0L;
                  i++;
               } else if (z == '+' || z == '-') {
                  if (i + 6 > s.length() || !isDigits(s, i + 1, i + 3) || s.charAt(i + 3) != ':' || !isDigits(s, i + 4, i + 6)) {
                     return Double.NaN;
                  }

                  int oh = Integer.parseInt(s.substring(i + 1, i + 3));
                  int om = Integer.parseInt(s.substring(i + 4, i + 6));
                  offsetMs = (oh * 60L + om) * 60000L;
                  if (z == '-') {
                     offsetMs = -offsetMs;
                  }

                  i += 6;
               }
            }
         }

         if (i != s.length()) {
            return Double.NaN;
         } else {
            double day0 = makeDay(year, month - 1, day);
            double time = makeTime(hour, minute, second, millis);
            return makeDate(day0, time) - offsetMs;
         }
      } else {
         return Double.NaN;
      }
   }

   private static boolean isDigits(String s, int from, int to) {
      if (to > s.length()) {
         return false;
      } else {
         for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
               return false;
            }
         }

         return true;
      }
   }

   static Date parse(String dateStr) {
      double v = parseToTimeValue(dateStr);
      return new Date(Double.isNaN(v) ? 0L : (long)v);
   }
}
