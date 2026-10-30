package io.karatelabs.ext.agent.events;

import io.karatelabs.common.Json;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentEventFileWriter implements AgentEventListener {
   private static final Logger b = LoggerFactory.getLogger(AgentEventFileWriter.class);
   public static final Set<String> a = Set.of("text_chunk", "llm_request", "llm_response_start");
   private final Path c;
   private final Set<String> d;
   private final UnaryOperator<String> e;
   private BufferedWriter f;
   private boolean g;

   public AgentEventFileWriter(Path var1, String var2, String var3, String var4) {
      this(var1, var2, var3, var4, a);
   }

   public AgentEventFileWriter(Path var1, String var2, String var3, String var4, Set<String> var5) {
      this(var1, var2, var3, var4, var5, null);
   }

   public AgentEventFileWriter(Path var1, String var2, String var3, String var4, Set<String> var5, UnaryOperator<String> var6) {
      this(var1, var2, var3, var4, var5, var6, null);
   }

   public AgentEventFileWriter(Path var1, String var2, String var3, String var4, Set<String> var5, UnaryOperator<String> var6, AgentRunContext var7) {
      this.c = var1;
      this.d = var5;
      this.e = var6;

      try {
         if (var1.getParent() != null) {
            Files.createDirectories(var1.getParent());
         }

         this.f = Files.newBufferedWriter(
            var1, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
         );
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
         var8.put("type", "run_start");
         var8.put("ts", io.karatelabs.ext.agent.events.AgentEvent.f());
         var8.put("runId", var2);
         if (var7 != null) {
            var7.addTo(var8);
         }

         if (var3 != null) {
            var8.put("task", var3);
         }

         if (var4 != null) {
            var8.put("model", var4);
         }

         this.writeLine(Json.stringifyStrict(var8));
      } catch (IOException var9) {
         this.g = true;
         b.warn("transcript writer disabled — cannot open {}: {}", var1, var9.toString());
      }
   }

   public Path path() {
      return this.c;
   }

   @Override
   public void onEvent(AgentEvent var1) {
      if (!this.g && !this.d.contains(var1.a())) {
         this.writeLine(var1.e());
      }
   }

   @Override
   public void close() {
      if (this.f != null) {
         try {
            this.f.flush();
            this.f.close();
         } catch (IOException var5) {
            b.warn("transcript writer close failed for {}: {}", this.c, var5.toString());
         } finally {
            this.f = null;
         }
      }
   }

   private void writeLine(String var1) {
      if (this.f != null) {
         try {
            this.f.write(this.e == null ? var1 : this.e.apply(var1));
            this.f.write(10);
            this.f.flush();
         } catch (IOException var3) {
            this.g = true;
            b.warn("transcript write failed for {}: {}", this.c, var3.toString());
         }
      }
   }
}
