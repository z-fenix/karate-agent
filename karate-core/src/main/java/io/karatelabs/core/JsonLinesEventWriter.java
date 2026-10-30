package io.karatelabs.core;

import io.karatelabs.common.Json;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLinesEventWriter implements RunListener, Closeable {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   public static final String DEFAULT_FILENAME = "karate-events.jsonl";
   public static final String SCHEMA_VERSION = "1";
   private final Path jsonlPath;
   private final String env;
   private final int threadCount;
   private final UnaryOperator<String> mask;
   private BufferedWriter writer;
   private volatile boolean closed = false;

   public JsonLinesEventWriter(Path outputDir, String env, int threadCount) {
      this(outputDir, env, threadCount, null);
   }

   public JsonLinesEventWriter(Path outputDir, String env, int threadCount, UnaryOperator<String> mask) {
      this.jsonlPath = outputDir.resolve("karate-json").resolve("karate-events.jsonl");
      this.env = env;
      this.threadCount = threadCount;
      this.mask = mask;
   }

   public void init() throws IOException {
      Files.createDirectories(this.jsonlPath.getParent());
      this.writer = Files.newBufferedWriter(this.jsonlPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
      logger.debug("JSONL event stream started: {}", this.jsonlPath);
   }

   @Override
   public boolean onEvent(RunEvent event) {
      if (!this.closed && this.writer != null) {
         RunEventType type = event.getType();
         if (type != RunEventType.STEP_ENTER
            && type != RunEventType.STEP_EXIT
            && type != RunEventType.HTTP_ENTER
            && type != RunEventType.HTTP_EXIT
            && type != RunEventType.MATCH_EXIT) {
            try {
               Map<String, Object> envelope = new LinkedHashMap<>();
               envelope.put("type", event.getType().name());
               envelope.put("timeStamp", event.getTimeStamp());
               envelope.put("threadId", this.getThreadId(event));
               Map<String, Object> data = event.toJson();
               if (event.getType() == RunEventType.SUITE_ENTER) {
                  data.put("schemaVersion", "1");
                  data.put("version", Globals.KARATE_VERSION);
                  if (this.env != null && !this.env.isEmpty()) {
                     data.put("env", this.env);
                  }

                  data.put("threads", this.threadCount);
               }

               envelope.put("data", data);
               this.writeLine(Json.stringifyStrict(envelope));
            } catch (Exception var5) {
               logger.warn("Failed to write event to JSONL: {}", var5.getMessage());
            }

            return true;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private String getThreadId(RunEvent event) {
      RunEventType type = event.getType();
      if (type != RunEventType.SUITE_ENTER && type != RunEventType.SUITE_EXIT && type != RunEventType.PROGRESS) {
         Thread thread = Thread.currentThread();
         String name = thread.getName();
         return name != null && !name.isEmpty() && !"main".equals(name) ? name : "thread-" + thread.threadId();
      } else {
         return null;
      }
   }

   private synchronized void writeLine(String json) throws IOException {
      if (this.writer != null && !this.closed) {
         this.writer.write(this.mask == null ? json : this.mask.apply(json));
         this.writer.newLine();
         this.writer.flush();
      }
   }

   @Override
   public void close() throws IOException {
      this.closed = true;
      if (this.writer != null) {
         this.writer.close();
         this.writer = null;
         logger.info("JSONL event stream written to: {}", this.jsonlPath);
      }
   }

   public Path getJsonlPath() {
      return this.jsonlPath;
   }
}
