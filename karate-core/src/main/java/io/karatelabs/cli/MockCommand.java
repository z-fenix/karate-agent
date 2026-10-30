package io.karatelabs.cli;

import io.karatelabs.core.MockServer;
import io.karatelabs.output.Console;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
   name = "mock",
   mixinStandardHelpOptions = true,
   description = {"Start a mock server from feature files"}
)
public class MockCommand implements Callable<Integer> {
   @Option(
      names = {"-m", "--mock"},
      description = {"Mock feature file (can be specified multiple times)"},
      required = true
   )
   List<String> mockFiles;
   @Option(
      names = {"-p", "--port"},
      description = {"Port to listen on (default: 0 = dynamic)"}
   )
   Integer port = 0;
   @Option(
      names = {"-s", "--ssl"},
      description = {"Enable HTTPS"}
   )
   boolean ssl;
   @Option(
      names = {"-c", "--cert"},
      description = {"SSL certificate file (PEM format)"}
   )
   String certPath;
   @Option(
      names = {"-k", "--key"},
      description = {"SSL private key file (PEM format)"}
   )
   String keyPath;
   @Option(
      names = {"--path-prefix"},
      description = {"URL path prefix to strip from incoming requests"}
   )
   String pathPrefix;
   @Option(
      names = {"-W", "--watch"},
      description = {"Enable hot-reload when feature files change"}
   )
   boolean watch;
   @Option(
      names = {"--max-initial-line-length"},
      description = {"Max request line length in bytes (default: 4096)"}
   )
   Integer maxInitialLineLength;
   @Option(
      names = {"--max-header-size"},
      description = {"Max total request header size in bytes (default: 8192)"}
   )
   Integer maxHeaderSize;
   @Option(
      names = {"--max-content-length"},
      description = {"Max request body size in bytes (default: 1048576)"}
   )
   Integer maxContentLength;

   public Integer call() {
      try {
         Console.println(Console.info("Starting mock server..."));
         MockServer.Builder builder = MockServer.feature(this.mockFiles.get(0));

         for (int i = 1; i < this.mockFiles.size(); i++) {
            builder.feature(this.mockFiles.get(i));
         }

         builder.port(this.port);
         if (this.ssl) {
            builder.ssl(true);
            if (this.certPath != null) {
               builder.certPath(this.certPath);
            }

            if (this.keyPath != null) {
               builder.keyPath(this.keyPath);
            }
         }

         if (this.pathPrefix != null) {
            builder.pathPrefix(this.pathPrefix);
         }

         if (this.watch) {
            builder.watch(true);
         }

         if (this.maxInitialLineLength != null) {
            builder.maxInitialLineLength(this.maxInitialLineLength);
         }

         if (this.maxHeaderSize != null) {
            builder.maxHeaderSize(this.maxHeaderSize);
         }

         if (this.maxContentLength != null) {
            builder.maxContentLength(this.maxContentLength);
         }

         MockServer server = builder.start();
         String protocol = this.ssl ? "https" : "http";
         Console.println();
         Console.println(Console.pass("Mock server started:"));
         Console.println("  URL: " + protocol + "://localhost:" + server.getPort());
         Console.println("  Features: " + String.join(", ", this.mockFiles));
         if (this.ssl) {
            Console.println("  SSL: enabled" + (this.certPath != null ? " (custom certificate)" : " (self-signed)"));
         }

         if (this.watch) {
            Console.println("  Watch: enabled (hot-reload on file change)");
         }

         Console.println();
         Console.println(Console.yellow("Press Ctrl+C to stop the server"));
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Console.println();
            Console.println(Console.warn("Shutting down mock server..."));
            server.stopAndWait();
            Console.println(Console.pass("Mock server stopped."));
         }));
         server.waitSync();
         return 0;
      } catch (Exception var4) {
         Console.println(Console.fail("Error starting mock server: " + var4.getMessage()));
         return 1;
      }
   }
}
