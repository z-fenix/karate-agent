package io.karatelabs.cli;

import io.karatelabs.common.FileUtils;
import io.karatelabs.core.KaratePom;
import io.karatelabs.output.Console;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
   name = "clean",
   mixinStandardHelpOptions = true,
   description = {"Clean output directories"}
)
public class CleanCommand implements Callable<Integer> {
   @Option(
      names = {"-o", "--output"},
      description = {"Output directory to clean (default: from karate-pom.json or target/karate-reports)"}
   )
   String outputDir;
   @Option(
      names = {"-w", "--workdir"},
      description = {"Working directory for relative path resolution (default: current directory)"}
   )
   String workingDir;
   @Option(
      names = {"-p", "--pom"},
      description = {"Path to project file (default: karate-pom.json)"}
   )
   String pomFile;
   @Option(
      names = {"--no-pom"},
      description = {"Ignore karate-pom.json even if present"}
   )
   boolean noPom;
   private KaratePom pom;

   public Integer call() {
      if (!this.noPom) {
         this.loadPom();
      }

      String effectiveOutputDir = this.resolveOutputDir();
      Path output = Path.of(effectiveOutputDir);
      int cleanedCount = 0;
      String outputDirName = output.getFileName().toString();
      if (Files.exists(output)) {
         try {
            this.deleteDirectory(output.toFile());
            Console.println(Console.info("Cleaned: " + outputDirName));
            cleanedCount++;
         } catch (Exception var16) {
            Console.println(Console.fail("Failed to clean: " + outputDirName + " - " + var16.getMessage()));
            return 1;
         }
      }

      Path parent = output.getParent();
      if (parent == null) {
         parent = Path.of(".");
      }

      String backupPrefix = outputDirName + "_";

      try {
         File parentDir = parent.toFile();
         File[] backupDirs = parentDir.listFiles((dir, name) -> name.startsWith(backupPrefix) && new File(dir, name).isDirectory());
         if (backupDirs != null) {
            for (File backupDir : backupDirs) {
               try {
                  this.deleteDirectory(backupDir);
                  Console.println(Console.info("Cleaned: " + backupDir.getName()));
                  cleanedCount++;
               } catch (Exception var15) {
                  Console.println(Console.warn("Failed to clean: " + backupDir.getName()));
               }
            }
         }
      } catch (Exception var17) {
      }

      Path tempDir = parent.resolve("karate-temp");
      if (Files.exists(tempDir) && Files.isDirectory(tempDir)) {
         try {
            this.deleteDirectory(tempDir.toFile());
            Console.println(Console.info("Cleaned: karate-temp"));
            cleanedCount++;
         } catch (Exception var14) {
            Console.println(Console.warn("Failed to clean: karate-temp"));
         }
      }

      if (cleanedCount == 0) {
         Console.println(Console.info("Nothing to clean"));
      }

      return 0;
   }

   private void loadPom() {
      String file = this.pomFile != null ? this.pomFile : "karate-pom.json";
      Path pomPath;
      if (this.workingDir != null) {
         pomPath = Path.of(this.workingDir).resolve(file);
      } else {
         pomPath = Path.of(file);
      }

      if (Files.exists(pomPath)) {
         try {
            this.pom = KaratePom.load(pomPath);
         } catch (Exception var4) {
         }
      }
   }

   private String resolveWorkingDir() {
      if (this.workingDir != null) {
         return this.workingDir;
      } else {
         return this.pom != null && this.pom.getWorkingDir() != null ? this.pom.getWorkingDir() : null;
      }
   }

   private String resolveOutputDir() {
      if (this.outputDir != null) {
         return this.outputDir;
      } else {
         if (this.pom != null) {
            String pomOutputDir = this.pom.getOutput().getDir();
            if (pomOutputDir != null && !pomOutputDir.isEmpty()) {
               return pomOutputDir;
            }
         }

         return FileUtils.getBuildDir() + "/karate-reports";
      }
   }

   private void deleteDirectory(File dir) {
      File[] files = dir.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               this.deleteDirectory(file);
            } else {
               file.delete();
            }
         }
      }

      dir.delete();
   }

   public String getOutputDir() {
      return this.outputDir;
   }

   public String getWorkingDir() {
      return this.workingDir;
   }
}
