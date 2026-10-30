package io.karatelabs.output;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class HttpLogger {
   private final Logger logger;
   private int requestCount;

   public HttpLogger() {
      this(LogContext.HTTP_LOGGER);
   }

   public HttpLogger(Logger logger) {
      this.logger = logger;
   }

   private LogMask activeMask(String uri) {
      LogMask m = LogContext.get().getMask();
      if (m == null) {
         return null;
      } else {
         return m.enabledForUri(uri) ? m : null;
      }
   }

   private boolean isPretty() {
      return LogContext.get().isPretty();
   }

   public static void logHeaders(StringBuilder sb, int num, String prefix, Map<String, List<String>> headers) {
      logHeaders(sb, num, prefix, headers, null);
   }

   public static void logHeaders(StringBuilder sb, int num, String prefix, Map<String, List<String>> headers, LogMask mask) {
      logHeaders(sb, num, prefix, headers, mask, false);
   }

   private static void logHeaders(StringBuilder sb, int num, String prefix, Map<String, List<String>> headers, LogMask mask, boolean console) {
      if (headers != null && !headers.isEmpty()) {
         headers.forEach((k, v) -> {
            for (String value : v) {
               sb.append("\u001b[2m").append(num).append(prefix).append("\u001b[0m");
               sb.append("\u001b[36m").append(k).append("\u001b[0m");
               sb.append(": ");
               sb.append(mask == null ? value : (console ? mask.maskConsoleHeader(k, value) : mask.maskHeader(k, value)));
               sb.append('\n');
            }
         });
      }
   }

   public void logBody(StringBuilder sb, byte[] body, ResourceType rt) {
      this.logBody(sb, body, rt, null);
   }

   public void logBody(StringBuilder sb, byte[] body, ResourceType rt, LogMask mask) {
      if (body != null) {
         String text = FileUtils.toString(body);
         if (mask != null) {
            text = mask.maskBody(text);
         }

         if (rt == ResourceType.JSON && StringUtils.looksLikeJson(text)) {
            try {
               Object parsed = Json.parseLenient(text);
               String formatted = this.isPretty() ? StringUtils.formatJson(parsed, true, false, false) : Json.stringifyStrict(parsed);
               sb.append('\u0000').append("json").append('\u0001');
               sb.append(AnsiJson.colorize(formatted));
               sb.append('\u0002');
            } catch (Exception var8) {
               sb.append(text);
            }
         } else {
            sb.append(text);
         }

         sb.append('\n');
      }
   }

   public static String getStatusFailureMessage(int expected, HttpRequest request, HttpResponse response) {
      String url = request.getUrlAndPath();
      String rawResponse = response.getBodyString();
      long responseTime = response.getResponseTime();
      return "status code was: "
         + response.getStatus()
         + ", expected: "
         + expected
         + ", response time in milliseconds: "
         + responseTime
         + ", url: "
         + url
         + ", response: \n"
         + rawResponse;
   }

   public void logRequest(HttpRequest request) {
      this.requestCount++;
      boolean capture = LogContext.get().isCapturing(LogLevel.INFO);
      boolean trace = this.logger.isTraceEnabled();
      boolean debug = !trace && this.logger.isDebugEnabled();
      if (capture || trace || debug) {
         LogMask m = this.activeMask(request.getUrlAndPath());
         StringBuilder full = new StringBuilder();
         full.append("\u001b[1m").append("request:").append("\u001b[0m").append('\n');
         full.append("\u001b[2m").append(this.requestCount).append(" > ").append("\u001b[0m");
         full.append("\u001b[36m").append("\u001b[1m").append(request.getMethod()).append("\u001b[0m");
         full.append(' ').append(request.getUrlAndPath()).append('\n');
         int headersStart = full.length();
         logHeaders(full, this.requestCount, " > ", request.getHeaders(), m);
         int headersEnd = full.length();
         ResourceType rt = ResourceType.fromContentType(request.getContentType());
         if ((capture || trace) && rt != null && !rt.isBinary()) {
            byte[] body;
            if (rt == ResourceType.MULTIPART) {
               body = request.getBodyDisplay() == null ? null : request.getBodyDisplay().getBytes();
            } else {
               body = request.getBody();
            }

            this.logBody(full, body, rt, m);
         }

         if (capture) {
            LogContext.get().log(LogLevel.INFO, full.toString());
         }

         if (trace || debug) {
            this.logConsole(full, headersStart, headersEnd, request.getHeaders(), " > ", m, trace);
         }
      }

      if (!trace && !debug && this.logger.isInfoEnabled()) {
         StringBuilder line = new StringBuilder();
         line.append("\u001b[2m").append(this.requestCount).append(" > ").append("\u001b[0m");
         line.append("\u001b[36m").append("\u001b[1m").append(request.getMethod()).append("\u001b[0m");
         line.append(' ').append(request.getUrlAndPath());
         this.logger.info(forLog(line.toString()));
      }
   }

   public void logResponse(HttpResponse response) {
      HttpRequest request = response.getRequest();
      boolean capture = LogContext.get().isCapturing(LogLevel.INFO);
      boolean trace = this.logger.isTraceEnabled();
      boolean debug = !trace && this.logger.isDebugEnabled();
      if (capture || trace || debug) {
         LogMask m = this.activeMask(request.getUrlAndPath());
         StringBuilder full = new StringBuilder();
         full.append("\u001b[2m").append("response time in milliseconds: ").append(response.getResponseTime()).append("\u001b[0m").append('\n');
         full.append("\u001b[2m").append(this.requestCount).append(" < ").append("\u001b[0m");
         full.append(colorStatus(response.getStatus())).append(' ');
         full.append("\u001b[36m").append(request.getMethod()).append("\u001b[0m");
         full.append(' ').append(request.getUrlAndPath()).append('\n');
         int headersStart = full.length();
         logHeaders(full, this.requestCount, " < ", response.getHeaders(), m);
         int headersEnd = full.length();
         ResourceType rt = response.getResourceType();
         if ((capture || trace) && rt != null && !rt.isBinary()) {
            this.logBody(full, response.getBodyBytes(), rt, m);
         }

         if (capture) {
            LogContext.get().log(LogLevel.INFO, full.toString());
         }

         if (trace || debug) {
            this.logConsole(full, headersStart, headersEnd, response.getHeaders(), " < ", m, trace);
         }
      }

      if (!trace && !debug && this.logger.isInfoEnabled()) {
         StringBuilder line = new StringBuilder();
         line.append("\u001b[2m").append(this.requestCount).append(" < ").append("\u001b[0m");
         line.append(colorStatus(response.getStatus())).append(' ');
         line.append("\u001b[36m").append(request.getMethod()).append("\u001b[0m");
         line.append(' ').append(request.getUrlAndPath());
         line.append("\u001b[2m").append(" (").append(response.getResponseTime()).append(" ms)").append("\u001b[0m");
         this.logger.info(forLog(line.toString()));
      }
   }

   private void logConsole(StringBuilder full, int headersStart, int headersEnd, Map<String, List<String>> headers, String prefix, LogMask m, boolean trace) {
      String text;
      if (m != null && m.hasReportOnlyHeaders()) {
         StringBuilder sb = new StringBuilder(full.substring(0, headersStart));
         logHeaders(sb, this.requestCount, prefix, headers, m, true);
         if (trace) {
            sb.append(full, headersEnd, full.length());
         }

         text = sb.toString();
      } else {
         text = trace ? full.toString() : full.substring(0, headersEnd);
      }

      if (trace) {
         this.logger.trace(forLog(text));
      } else {
         this.logger.debug(forLog(text));
      }
   }

   private static String forLog(String text) {
      return Console.isColorsEnabled() ? Console.stripSentinels(text) : Console.stripAnsi(text);
   }

   private static String colorStatus(int status) {
      String statusStr = String.valueOf(status);
      if (status >= 200 && status < 300) {
         return "\u001b[32m" + statusStr + "\u001b[0m";
      } else if (status >= 300 && status < 400) {
         return "\u001b[33m" + statusStr + "\u001b[0m";
      } else {
         return status >= 400 ? "\u001b[31m\u001b[1m" + statusStr + "\u001b[0m" : statusStr;
      }
   }
}
