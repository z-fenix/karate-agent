package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.FileReaderProvider;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.run.MaxWorkspace;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.parser.JsLexer;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class File extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(File.class);
   public static final int MAX_WRITE_BYTES = 26214400;
   private final Path root;
   private final Map<String, Long> readMtimes = new ConcurrentHashMap<>();
   private static final long READ_RECORD_INVALID = -1L;
   private Engine engine;
   private boolean noProject;
   private String aiNote;
   private File.Watcher watcher;
   private volatile ConsoleState console = new ConsoleState(null);
   private static final Pattern LOOP_EXIT_TEXT = Pattern.compile("\\bRobot\\s*\\.\\s*(success|failure)\\s*\\(");
   private TriageGate triage;
   static final List<String> GENERATED_DIRS = List.of("target", "build", "dist", "node_modules", "runs", ".git", ".idea");
   private static final int LIST_CAP = 200;

   public File(Path var1) {
      this.root = var1.toAbsolutePath().normalize();
      this.fn("read", var1x -> this.read(JsApi.str(var1x, 0)));
      this.fn("write", var1x -> this.write(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("writeBytes", var1x -> this.writeBytes(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("list", var1x -> this.list(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("ls", var1x -> this.ls(JsApi.str(var1x, 0)));
      this.fn("exists", var1x -> this.exists(JsApi.str(var1x, 0)));
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
      this.fn("move", var1x -> this.move(JsApi.str(var1x, 0), JsApi.str(var1x, 1)));
      this.fn("copy", var1x -> this.copy(JsApi.str(var1x, 0), JsApi.str(var1x, 1)));
      this.fn("mkdir", var1x -> this.mkdir(JsApi.str(var1x, 0)));
      this.fn("tag", var1x -> this.tag(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("call", var1x -> this.call(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public static File at(Path var0) {
      return new File(var0);
   }

   public File withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   public File withEngine(Engine var1) {
      this.engine = var1;
      return this;
   }

   public File withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public File withAiNote(String var1) {
      this.aiNote = var1;
      return this;
   }

   public File withWatcher(File.Watcher var1) {
      this.watcher = var1;
      return this;
   }

   private void changing(Path var1) {
      if (this.watcher != null) {
         this.watcher.changing(var1);
      }
   }

   private void changed(Path var1) {
      if (this.watcher != null) {
         this.watcher.changed(var1);
      }
   }

   static boolean callsLoopExit(String var0) {
      if (!var0.contains("Robot")) {
         return false;
      } else {
         List var1;
         try {
            var1 = JsLexer.getTokens(Resource.text(var0));
         } catch (RuntimeException var5) {
            return LOOP_EXIT_TEXT.matcher(var0).find();
         }

         for (int var2 = 0; var2 + 3 < var1.size(); var2++) {
            TokenType var3 = ((Token)var1.get(var2 + 1)).type;
            String var4 = ((Token)var1.get(var2 + 2)).getText();
            if (((Token)var1.get(var2)).type == TokenType.IDENT
               && "Robot".equals(((Token)var1.get(var2)).getText())
               && (var3 == TokenType.DOT || var3 == TokenType.QUES_DOT)
               && ("success".equals(var4) || "failure".equals(var4))
               && ((Token)var1.get(var2 + 3)).type == TokenType.L_PAREN) {
               return true;
            }
         }

         return false;
      }
   }

   private static Map<String, Object> loopExitRefused(String var0, String var1) {
      return JsApi.missing(
         var0 + "('" + var1 + "') refused: a check holds no Robot.success/failure — call Robot.success from the loop after the green Check.run"
      );
   }

   private boolean landsLoopExit(Path var1, Path var2) throws IOException {
      if (this.aiNote == null) {
         return false;
      } else {
         try (var var3 = Files.walk(var1)) {
            for (Path var5 : (Iterable<Path>) (var3.filter(var0 -> Files.isRegularFile(var0))::iterator)) {
               Path var6 = var2.resolve(var1.relativize(var5).toString());
               if (this.marksAi(var6) && callsLoopExit(Files.readString(var5))) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean marksAi(Path var1) {
      return this.aiNote != null && var1.toString().endsWith(".js") && var1.startsWith(MaxWorkspace.checksDir(this.root));
   }

   private void markLanded(Path var1) throws IOException {
      if (this.aiNote != null) {
         try (var var2 = Files.walk(var1)) {
            for (Path var4 : (Iterable<Path>) (var2.filter(var0 -> Files.isRegularFile(var0))::iterator)) {
               if (this.marksAi(var4)) {
                  String var5 = Files.readString(var4);
                  String var6 = Check.withAiHeader(var5, this.aiNote);
                  if (!var6.equals(var5)) {
                     Files.writeString(var4, var6);
                  }
               }
            }
         }
      }
   }

   private Map<String, Object> writeGuard(String var1) {
      return this.noProject ? JsApi.noProjectSelected(var1) : null;
   }

   public File withTriage(TriageGate var1) {
      this.triage = var1;
      return this;
   }

   private Object gated(Path var1, Supplier<Object> var2) {
      return this.triage == null ? var2.get() : this.triage.write(var1, var2);
   }

   private Object gated(List<Path> var1, boolean var2, Supplier<Object> var3) {
      if (this.triage == null) {
         return var3.get();
      } else {
         return var2 ? this.triage.tree(var1, var3) : this.triage.write(var1, var3);
      }
   }

   Object read(String var1) {
      if (var1 != null && !var1.isBlank()) {
         Path var2;
         try {
            var2 = this.resolve(var1);
         } catch (SecurityException var5) {
            return JsApi.missing("File.read denied: " + var5.getMessage());
         }

         if (!Files.isRegularFile(var2)) {
            return JsApi.notFound("no file '" + var1 + "' — use File.list() to see what is there");
         } else {
            try {
               String var3 = Files.readString(var2);
               this.readMtimes.put(key(var2), Files.getLastModifiedTime(var2).toMillis());
               return var3;
            } catch (Exception var4) {
               return JsApi.notFound("File.read('" + var1 + "') failed: " + var4.getMessage());
            }
         }
      } else {
         return JsApi.missing("File.read(path) requires a path String relative to the project dir");
      }
   }

   public Object readParsed(String var1) {
      String[] var2 = var1 == null ? null : FileReaderProvider.splitFragment(var1);
      String var3 = var2 == null ? var1 : var2[0];
      FileReaderProvider var4 = var3 != null && var3.lastIndexOf(46) >= 0 ? FileReaderProvider.forExtension(var3.substring(var3.lastIndexOf(46) + 1)) : null;
      if (var4 != null) {
         Path var11;
         try {
            var11 = this.resolve(var3);
         } catch (SecurityException var9) {
            return JsApi.missing("read denied: " + var9.getMessage());
         }

         if (!Files.isRegularFile(var11)) {
            return JsApi.notFound("no file '" + var3 + "' — use File.list() to see what is there");
         } else {
            try {
               return var4.read(Resource.from(var11, this.root), var2 == null ? null : var2[1]);
            } catch (RuntimeException var8) {
               return JsApi.error(var8);
            }
         }
      } else {
         Object var5 = this.read(var1);
         if (var5 instanceof String var6) {
            if (var1 != null && var1.toLowerCase().endsWith(".json")) {
               try {
                  return Json.of(var6).value();
               } catch (RuntimeException var10) {
                  return var6;
               }
            } else {
               return var6;
            }
         } else {
            return var5;
         }
      }
   }

   Object write(String var1, Object var2) {
      var var3 = this.writeGuard("File.write");
      if (var3 != null) {
         return var3;
      } else if (var1 == null || var1.isBlank()) {
         return JsApi.missing("File.write(path, content) requires a path String relative to the project dir");
      } else if (var2 == null) {
         return JsApi.missing("File.write(path, content) requires the content (a String) as the 2nd arg");
      } else {
         String var4;
         if (var2 instanceof List var5) {
            StringBuilder var6 = new StringBuilder();

            for (int var7 = 0; var7 < var5.size(); var7++) {
               if (var7 > 0) {
                  var6.append('\n');
               }

               Object var8 = var5.get(var7);
               var6.append(var8 == null ? "" : String.valueOf(var8));
            }

            var4 = var6.toString();
         } else {
            var4 = String.valueOf(var2);
         }

         Path var10;
         try {
            var10 = this.resolve(var1);
         } catch (SecurityException var9) {
            return JsApi.missing("File.write denied: " + var9.getMessage());
         }

         String var11 = var4;
         return this.gated(var10, () -> this.writeText(var1, var10, var11));
      }
   }

   private Object writeText(String var1, Path var2, String var3) {
      if (var1.endsWith(".md") || !var3.contains("‹secret:") && !var3.contains("‹masked›")) {
         boolean var4 = false;
         if (var1.endsWith(".md") && var3.contains("\\`")) {
            var3 = var3.replace("\\`", "`");
            var4 = true;
         }

         if (this.marksAi(var2)) {
            if (callsLoopExit(var3)) {
               return loopExitRefused("File.write", var1);
            }

            var3 = Check.withAiHeader(var3, this.aiNote);
         }

         if (Files.exists(var2)) {
            Long var5 = this.readMtimes.get(key(var2));
            if (var5 != null) {
               try {
                  if (Files.getLastModifiedTime(var2).toMillis() != var5) {
                     return JsApi.missing(
                        "'" + var1 + "' changed on disk since you read it — re-read with File.read('" + var1 + "'), merge your edit, then write again"
                     );
                  }
               } catch (Exception var7) {
               }
            }
         }

         this.changing(var2);

         try {
            if (var2.getParent() != null) {
               Files.createDirectories(var2.getParent());
            }

            Files.writeString(var2, var3);
            this.readMtimes.put(key(var2), Files.getLastModifiedTime(var2).toMillis());
            logger.debug("File.write: {} ({} chars)", var1, var3.length());
            this.changed(var2);
         } catch (Exception var8) {
            return JsApi.missing("File.write('" + var1 + "') failed: " + var8.getMessage());
         }

         String var9 = "written: " + var1;
         if (var4) {
            var9 = var9 + " (note: escaped backticks were auto-corrected — use raw backticks in .md content)";
         }

         return var9;
      } else {
         return JsApi.missing(
            "File.write('"
               + var1
               + "') refused: ‹secret:NAME› and ‹masked› are mask labels, not values. Write a stored secret as secret('NAME'); take a ‹masked› value from the variable that holds it. Then write again."
         );
      }
   }

   public Object writeBytes(String var1, Object var2) {
      var var3 = this.writeGuard("File.writeBytes");
      if (var3 != null) {
         return var3;
      } else if (var1 != null && !var1.isBlank()) {
         byte[] var4;
         if (var2 instanceof byte[] var6) {
            var4 = var6;
         } else {
            if (!(var2 instanceof String var5)) {
               return JsApi.missing("File.writeBytes(path, bytes) requires a byte[] or a base64 String as the 2nd arg");
            }

            try {
               var4 = Base64.getDecoder().decode(var5);
            } catch (IllegalArgumentException var9) {
               return JsApi.missing("File.writeBytes(path, bytes): the String 2nd arg must be valid base64");
            }
         }

         if (var4.length > 26214400) {
            return JsApi.missing("File.writeBytes('" + var1 + "') rejected: " + var4.length + " bytes exceeds the 26214400-byte limit");
         } else {
            Path var10;
            try {
               var10 = this.resolve(var1);
            } catch (SecurityException var8) {
               return JsApi.missing("File.writeBytes denied: " + var8.getMessage());
            }

            byte[] var7 = var4;
            return this.gated(var10, () -> this.writeBinary(var1, var10, var7));
         }
      } else {
         return JsApi.missing("File.writeBytes(path, bytes) requires a path String relative to the project dir");
      }
   }

   private Object writeBinary(String var1, Path var2, byte[] var3) {
      if (Long.valueOf(-1L).equals(this.readMtimes.get(key(var2)))) {
         return JsApi.missing(
            "'"
               + var1
               + "' was re-written by File.tag or Check.link since you read it — re-read with File.read('"
               + var1
               + "'), merge your edit, then write again"
         );
      } else {
         if (this.marksAi(var2)) {
            if (callsLoopExit(new String(var3, StandardCharsets.UTF_8))) {
               return loopExitRefused("File.writeBytes", var1);
            }

            var3 = Check.withAiHeader(new String(var3, StandardCharsets.UTF_8), this.aiNote).getBytes(StandardCharsets.UTF_8);
         }

         this.changing(var2);

         try {
            if (var2.getParent() != null) {
               Files.createDirectories(var2.getParent());
            }

            Files.write(var2, var3);
            this.readMtimes.put(key(var2), Files.getLastModifiedTime(var2).toMillis());
            logger.debug("File.writeBytes: {} ({} bytes)", var1, var3.length);
            this.changed(var2);
         } catch (Exception var5) {
            return JsApi.missing("File.writeBytes('" + var1 + "') failed: " + var5.getMessage());
         }

         return "written: " + var1 + " (" + var3.length + " bytes)";
      }
   }

   Object list(String var1, Object var2) {
      boolean var3 = var2 instanceof Map var4 && Boolean.TRUE.equals(var4.get("full"));
      int var18 = var2 instanceof Map var6 && var6.get("limit") instanceof Number var5 ? Math.max(1, var5.intValue()) : 200;
      String var19 = var1 != null && !var1.isBlank() ? var1 : "**/*";
      if (!Files.isDirectory(this.root)) {
         return List.of();
      } else {
         var19 = MaxPaths.stripLeadingSlashes(var19);
         if (var19.indexOf(42) < 0 && var19.indexOf(63) < 0) {
            Path var21;
            try {
               var21 = this.resolve(var19);
            } catch (SecurityException var17) {
               return JsApi.missing("File.list denied: " + var17.getMessage());
            }

            if (Files.isDirectory(var21)) {
               String var23 = this.root.relativize(var21).toString().replace('\\', '/');
               var19 = var23.isEmpty() ? "**" : var23 + "/**";
            }
         }

         Pattern var22 = globToRegex(var19);
         String var24 = var19;
         List var8 = var3
            ? List.of()
            : GENERATED_DIRS.stream().filter(var1x -> !var24.equals(var1x) && !var24.startsWith(var1x + "/") && !var24.contains("/" + var1x + "/")).toList();
         ArrayList var9 = new ArrayList();

         try {
            this.walkPruned(var8, var2x -> {
               if (var22.matcher(var2x).matches()) {
                  var9.add(var2x);
               }
            });
         } catch (Exception var16) {
            return JsApi.missing("File.list('" + var19 + "') failed: " + var16.getMessage());
         }

         var9.sort(null);
         if (var9.isEmpty() && !var8.isEmpty()) {
            ArrayList var10 = new ArrayList();

            try {
               this.walkPruned(List.of(), var2x -> {
                  if (var10.size() < 3 && var22.matcher(var2x).matches()) {
                     var10.add(var2x);
                  }
               });
            } catch (Exception var15) {
            }

            if (!var10.isEmpty()) {
               return JsApi.notFound(
                  "File.list('"
                     + var19
                     + "') matched nothing under the project, but "
                     + var10.size()
                     + "+ file(s) match inside a generated tree that is not walked by default (e.g. "
                     + (String)var10.get(0)
                     + "). Name it in the glob or pass {full:true}."
               );
            }
         }

         if (var9.isEmpty() && var1 != null && !var1.isBlank()) {
            io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a("nothing matches '" + var1 + "' — paths resolve from the project root; call File.list() to see it");
         }

         if (!var3 && var9.size() > var18) {
            LinkedHashMap var25 = new LinkedHashMap();
            var25.put("total", var9.size());
            var25.put("shown", var18);
            var25.put("truncated", true);
            LinkedHashMap<String, Integer> var11 = new LinkedHashMap<>();

            for (String var13 : (Iterable<String>) (Iterable<?>) (var9)) {
               int var14 = var13.lastIndexOf(47);
               var11.merge(var14 < 0 ? "." : var13.substring(0, var14), 1, Integer::sum);
            }

            var25.put("dirs", var11);
            var25.put("files", List.copyOf(var9.subList(0, var18)));
            var25.put(
               "note",
               "matched "
                  + var9.size()
                  + " files — showing "
                  + var18
                  + " (and a count per directory). Narrow the glob, use File.ls(dir) for one folder level, or pass {limit:n} / {full:true}."
                  + (var8.isEmpty() ? "" : " Skipped (generated): " + String.join(", ", var8) + ".")
            );
            return var25;
         } else {
            return var9;
         }
      }
   }

   private void walkPruned(List<String> var1, Consumer<String> var2) throws IOException {
      walkPruned(this.root, var1, var2);
   }

   static void walkPruned(final Path var0, final List<String> var1, final Consumer<String> var2) throws IOException {
      Files.walkFileTree(var0, new SimpleFileVisitor<Path>() {
         @Override
         public FileVisitResult preVisitDirectory(Path var1x, BasicFileAttributes var2x) {
            return !var1x.equals(var0) && var1.contains(var1x.getFileName().toString()) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFile(Path var1x, BasicFileAttributes var2x) {
            if (var2x.isRegularFile()) {
               var2.accept(var0.relativize(var1x).toString().replace('\\', '/'));
            }

            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFileFailed(Path var1x, IOException var2x) {
            return FileVisitResult.CONTINUE;
         }
      });
   }

   Object ls(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1 : "";

      Path var3;
      try {
         var3 = this.resolve(var2);
      } catch (SecurityException var8) {
         return JsApi.missing("File.ls denied: " + var8.getMessage());
      }

      if (!Files.isDirectory(var3)) {
         return JsApi.notFound("no directory '" + var2 + "' — File.ls lists one folder level");
      } else {
         try {
            List var5;
            try (var var4 = Files.list(var3)) {
               var5 = var4.<Map<String, Object>>map(var0 -> {
                     boolean var1x = Files.isDirectory(var0);
                     long var2x = 0L;
                     long var4x = 0L;

                     try {
                        var4x = Files.getLastModifiedTime(var0).toMillis();
                        if (!var1x) {
                           var2x = Files.size(var0);
                        }
                     } catch (Exception var7) {
                     }

                     return Map.of("name", var0.getFileName().toString(), "dir", var1x, "size", var2x, "modified", var4x);
                  })
                  .sorted(
                     Comparator.<Map<String, Object>, Boolean>comparing(var0 -> (Boolean)var0.get("dir"))
                        .reversed()
                        .thenComparing(var0 -> ((String)var0.get("name")).toLowerCase())
                  )
                  .toList();
            }

            return var5;
         } catch (Exception var10) {
            return JsApi.missing("File.ls('" + var2 + "') failed: " + var10.getMessage());
         }
      }
   }

   Object exists(String var1) {
      if (var1 != null && !var1.isBlank()) {
         try {
            return Files.exists(this.resolve(var1));
         } catch (SecurityException var3) {
            return false;
         }
      } else {
         return JsApi.missing("File.exists(path) requires a path String relative to the project dir");
      }
   }

   Object delete(String var1) {
      var var2 = this.writeGuard("File.delete");
      if (var2 != null) {
         return var2;
      } else if (var1 != null && !var1.isBlank()) {
         Path var3;
         try {
            var3 = this.resolve(var1);
         } catch (SecurityException var5) {
            return JsApi.missing("File.delete denied: " + var5.getMessage());
         }

         if (!Files.exists(var3)) {
            return JsApi.notFound("nothing to delete at '" + var1 + "' (already gone)");
         } else {
            return var3.equals(this.root)
               ? JsApi.missing("File.delete('" + var1 + "') refuses the project root itself")
               : this.gated(List.of(var3), Files.isDirectory(var3), () -> this.deleteAt(var1, var3));
         }
      } else {
         return JsApi.missing("File.delete(path) requires a path String relative to the project dir");
      }
   }

   private Object deleteAt(String var1, Path var2) {
      this.changing(var2);

      try {
         if (!Files.isDirectory(var2)) {
            Files.delete(var2);
            this.readMtimes.remove(key(var2));
            this.changed(var2);
            return "deleted: " + var1;
         } else {
            List<Path> var3;
            try (var var4 = Files.walk(var2)) {
               var3 = var4.sorted(Comparator.reverseOrder()).toList();
            }

            long var11 = var3.stream().filter(var0 -> Files.isRegularFile(var0)).count();

            for (Path var7 : (Iterable<Path>) (Iterable<?>) (var3)) {
               Files.delete(var7);
            }

            this.forgetTracked(var2);
            this.changed(var2);
            return "deleted: " + var1 + "/ (" + var11 + " file" + (var11 == 1L ? "" : "s") + ")";
         }
      } catch (Exception var10) {
         return JsApi.missing("File.delete('" + var1 + "') failed: " + var10.getMessage());
      }
   }

   Object move(String var1, String var2) {
      var var3 = this.writeGuard("File.move");
      if (var3 != null) {
         return var3;
      } else if (var1 != null && !var1.isBlank() && var2 != null && !var2.isBlank()) {
         Path var4;
         Path var5;
         try {
            var4 = this.resolve(var1);
            var5 = this.resolve(var2);
         } catch (SecurityException var7) {
            return JsApi.missing("File.move denied: " + var7.getMessage());
         }

         if (var4.equals(this.root)) {
            return JsApi.missing("File.move('" + var1 + "') refuses the project root itself");
         } else if (!Files.exists(var4)) {
            return JsApi.notFound("nothing to move at '" + var1 + "'");
         } else if (var5.equals(var4)) {
            return JsApi.missing("File.move: '" + var1 + "' and '" + var2 + "' are the same path");
         } else {
            return Files.exists(var5)
               ? JsApi.missing("File.move refuses to overwrite '" + var2 + "' — delete it first or pick another name")
               : this.gated(List.of(var4, var5), Files.isDirectory(var4), () -> this.moveTo(var1, var2, var4, var5));
         }
      } else {
         return JsApi.missing("File.move(from, to) requires both a from and a to path relative to the project dir");
      }
   }

   private Object moveTo(String var1, String var2, Path var3, Path var4) {
      try {
         if (this.landsLoopExit(var3, var4)) {
            return loopExitRefused("File.move", var2);
         }
      } catch (UncheckedIOException | IOException var8) {
         return JsApi.missing("File.move('" + var1 + "') could not be read: " + var8.getMessage());
      }

      this.changing(var3);
      this.changing(var4);

      try {
         if (var4.getParent() != null) {
            Files.createDirectories(var4.getParent());
         }

         try {
            Files.move(var3, var4, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var6) {
            Files.move(var3, var4);
         }

         this.forgetTracked(var3);
         this.markLanded(var4);
         this.changed(var3);
         this.changed(var4);
         logger.debug("File.move: {} -> {}", var1, var2);
      } catch (Exception var7) {
         return JsApi.missing("File.move('" + var1 + "' -> '" + var2 + "') failed: " + var7.getMessage());
      }

      return "moved: " + var1 + " -> " + var2;
   }

   Object copy(String var1, String var2) {
      var var3 = this.writeGuard("File.copy");
      if (var3 != null) {
         return var3;
      } else if (var1 != null && !var1.isBlank() && var2 != null && !var2.isBlank()) {
         Path var4;
         Path var5;
         try {
            var4 = this.resolve(var1);
            var5 = this.resolve(var2);
         } catch (SecurityException var7) {
            return JsApi.missing("File.copy denied: " + var7.getMessage());
         }

         if (var4.equals(this.root)) {
            return JsApi.missing("File.copy('" + var1 + "') refuses the project root itself");
         } else if (!Files.exists(var4)) {
            return JsApi.notFound("nothing to copy at '" + var1 + "'");
         } else if (var5.equals(var4)) {
            return JsApi.missing("File.copy: '" + var1 + "' and '" + var2 + "' are the same path");
         } else {
            return Files.exists(var5)
               ? JsApi.missing("File.copy refuses to overwrite '" + var2 + "' — delete it first or pick another name")
               : this.gated(List.of(var5), Files.isDirectory(var4), () -> this.copyTo(var1, var2, var4, var5));
         }
      } else {
         return JsApi.missing("File.copy(from, to) requires both a from and a to path relative to the project dir");
      }
   }

   private Object copyTo(String var1, String var2, Path var3, Path var4) {
      try {
         if (this.landsLoopExit(var3, var4)) {
            return loopExitRefused("File.copy", var2);
         }
      } catch (UncheckedIOException | IOException var13) {
         return JsApi.missing("File.copy('" + var1 + "') could not be read: " + var13.getMessage());
      }

      this.changing(var4);

      try {
         if (var4.getParent() != null) {
            Files.createDirectories(var4.getParent());
         }

         if (Files.isDirectory(var3)) {
            long[] var5 = new long[]{0L};

            try (var var6 = Files.walk(var3)) {
               for (Path var8 : (Iterable<Path>) (var6.sorted()::iterator)) {
                  Path var9 = var4.resolve(var3.relativize(var8));
                  if (Files.isDirectory(var8)) {
                     Files.createDirectories(var9);
                  } else {
                     Files.copy(var8, var9, StandardCopyOption.COPY_ATTRIBUTES);
                     var5[0]++;
                  }
               }
            }

            this.markLanded(var4);
            this.changed(var4);
            logger.debug("File.copy (dir): {} -> {} ({} files)", new Object[]{var1, var2, var5[0]});
            return "copied: " + var1 + " -> " + var2 + "/ (" + var5[0] + " file" + (var5[0] == 1L ? "" : "s") + ")";
         }

         Files.copy(var3, var4, StandardCopyOption.COPY_ATTRIBUTES);
         this.markLanded(var4);
         this.changed(var4);
         logger.debug("File.copy: {} -> {}", var1, var2);
      } catch (Exception var12) {
         return JsApi.missing("File.copy('" + var1 + "' -> '" + var2 + "') failed: " + var12.getMessage());
      }

      return "copied: " + var1 + " -> " + var2;
   }

   Object tag(String var1, Object var2, Object var3) {
      var var4 = this.writeGuard("File.tag");
      if (var4 != null) {
         return var4;
      } else if (var1 != null && !var1.isBlank()) {
         Object var5 = var2;
         Object var6 = var3;
         if (var3 == null && var2 instanceof Map) {
            var6 = var2;
            var5 = null;
         }

         if (var6 instanceof Map var7) {
            boolean var8 = Boolean.TRUE.equals(var7.get("feature"));
            if (var8 && var5 != null) {
               return JsApi.missing(
                  "File.tag: {feature: true} targets the feature's own tag lines — drop the scenario argument, or drop 'feature' to tag one scenario"
               );
            } else if (!var8 && var5 == null) {
               return JsApi.missing("File.tag needs a scenario: its exact name, or its keyword line number (or {feature: true} for the feature's own tags)");
            } else {
               var var9 = tokens(var7.get("add"));
               var var10 = tokens(var7.get("remove"));
               if (var9 != null && var10 != null) {
                  if (var9.isEmpty() && var10.isEmpty()) {
                     return JsApi.missing("File.tag: nothing to do — pass add and/or remove");
                  } else {
                     for (String var12 : (Iterable<String>) (Iterable<?>) (var9)) {
                        if (var10.contains(var12)) {
                           return JsApi.missing("File.tag: '" + var12 + "' is in both add and remove — pick one");
                        }
                     }

                     Path var17;
                     try {
                        var17 = this.resolve(var1);
                     } catch (SecurityException var16) {
                        return JsApi.missing("File.tag denied: " + var16.getMessage());
                     }

                     if (!Files.isRegularFile(var17)) {
                        return JsApi.notFound("no file '" + var1 + "' — File.tag edits a .feature file's tag lines");
                     } else if (!var1.endsWith(".feature")) {
                        return JsApi.missing("File.tag edits Gherkin tag lines — '" + var1 + "' is not a .feature file");
                     } else {
                        String var18;
                        try {
                           var18 = Files.readString(var17);
                        } catch (Exception var15) {
                           return JsApi.notFound("File.tag('" + var1 + "') could not read the file: " + var15.getMessage());
                        }

                        FeatureTags.Edit var13 = FeatureTags.apply(var18, var5, var8, var9, var10);
                        if (var13.error() != null) {
                           return var13.error();
                        } else {
                           if (!var13.content().equals(var18)) {
                              var var14 = this.rewrite("File.tag", var17, var13.content());
                              if (var14 != null) {
                                 return var14;
                              }

                              logger.debug("File.tag: {} +{} -{}", new Object[]{var1, var13.added(), var13.removed()});
                           }

                           LinkedHashMap var19 = new LinkedHashMap();
                           var19.put("path", var1);
                           if (var8) {
                              var19.put("feature", true);
                           } else {
                              var19.put("scenario", var13.scenario());
                           }

                           var19.put("line", var13.line());
                           var19.put("tags", var13.tags());
                           var19.put("added", var13.added());
                           var19.put("removed", var13.removed());
                           if (!var13.unchanged().isEmpty()) {
                              var19.put("unchanged", var13.unchanged());
                           }

                           if (!var13.absent().isEmpty()) {
                              var19.put("absent", var13.absent());
                           }

                           if (!var13.inherited().isEmpty()) {
                              var19.put("inherited", var13.inherited());
                           }

                           return JsApi.next(var19, "Runner.suite('" + var1 + "', {dryRun: true})");
                        }
                     }
                  }
               } else {
                  return JsApi.missing("File.tag: add/remove take a tag token String or an array of them, e.g. {add: ['@req=ORD-001/1', '@req=ORD-001/2']}");
               }
            }
         } else {
            return JsApi.missing(
               "File.tag(path, scenario, {add|remove}) requires an options map, e.g. File.tag('src/orders.feature', 'place an order', {add: '@req=ORD-001/1'})"
            );
         }
      } else {
         return JsApi.missing("File.tag(path, scenario, {add|remove}) requires a .feature path relative to the project dir");
      }
   }

   Map<String, Object> rewrite(String var1, Path var2, String var3) {
      var var4 = this.writeGuard(var1);
      if (var4 != null) {
         return var4;
      } else {
         Path var5;
         try {
            var5 = MaxPaths.within(this.root, var2);
         } catch (SecurityException var8) {
            return JsApi.missing(var1 + " denied: " + var8.getMessage());
         }

         return this.gated(var5, () -> {
            if (this.marksAi(var5) && callsLoopExit(var3)) {
               return loopExitRefused(var1, this.root.relativize(var5).toString().replace('\\', '/'));
            } else {
               this.changing(var5);

               try {
                  io.karatelabs.ext.common.AtomicFileWriter.a(var5, this.marksAi(var5) ? Check.withAiHeader(var3, this.aiNote) : var3);
                  this.readMtimes.put(key(var5), -1L);
                  this.changed(var5);
                  return null;
               } catch (Exception var5x) {
                  return JsApi.missing(var1 + "('" + this.root.relativize(var5).toString().replace('\\', '/') + "') failed: " + var5x.getMessage());
               }
            }
         }) instanceof Map var7 ? var7 : null;
      }
   }

   private static List<String> tokens(Object var0) {
      if (var0 == null) {
         return List.of();
      } else {
         List var1 = var0 instanceof List var2 ? List.copyOf(var2) : List.of(var0);
         LinkedHashSet var7 = new LinkedHashSet();
         Iterator var3 = var1.iterator();

         while (var3.hasNext()) {
            if (!(var3.next() instanceof String var5) || var5.isBlank()) {
               return null;
            }

            String var6 = var5.trim();
            var7.add(var6.startsWith("@") ? var6 : "@" + var6);
         }

         return new ArrayList<>(var7);
      }
   }

   Object mkdir(String var1) {
      var var2 = this.writeGuard("File.mkdir");
      if (var2 != null) {
         return var2;
      } else if (var1 != null && !var1.isBlank()) {
         Path var3;
         try {
            var3 = this.resolve(var1);
         } catch (SecurityException var5) {
            return JsApi.missing("File.mkdir denied: " + var5.getMessage());
         }

         if (Files.isRegularFile(var3)) {
            return JsApi.missing("File.mkdir('" + var1 + "') refuses — a file already exists there");
         } else {
            return Files.isDirectory(var3) ? "exists: " + var1 + "/" : this.gated(var3, () -> {
               try {
                  Files.createDirectories(var3);
                  logger.debug("File.mkdir: {}", var1);
               } catch (Exception var3x) {
                  return JsApi.missing("File.mkdir('" + var1 + "') failed: " + var3x.getMessage());
               }

               return "created: " + var1 + "/";
            });
         }
      } else {
         return JsApi.missing("File.mkdir(path) requires a path String relative to the project dir");
      }
   }

   Object call(String var1, Object var2) {
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("File.call(path, arg?) requires a project path to a .js file, e.g. File.call('/start-mock.js')");
      } else if (this.engine == null) {
         return JsApi.missing("File.call is not available here — it runs a script in the console engine (this File is a render-time/file-only binding)");
      } else {
         Path var3;
         try {
            var3 = this.resolve(var1);
         } catch (SecurityException var7) {
            return JsApi.missing("File.call denied: " + var7.getMessage());
         }

         if (!Files.isRegularFile(var3)) {
            return JsApi.notFound("no file '" + var1 + "' to call — use File.list('**/*.js') to see what is there");
         } else {
            String var4;
            try {
               var4 = Files.readString(var3);
            } catch (Exception var6) {
               return JsApi.notFound("File.call('" + var1 + "') could not read the file: " + var6.getMessage());
            }

            Supplier var5 = () -> {
               ProjectBoot.ensure(this.engine, this.root);
               ProjectConfig.ensure(this.engine, this.root, this.console);
               logger.debug("File.call: {} ({} chars)", var1, var4.length());
               Object var4x = this.engine.eval(new JsParser(Resource.text(var4)).leadingFunctionExpression().parse());
               return var4x instanceof JavaCallable var5x ? var5x.call(null, var2 == null ? new Object[0] : new Object[]{var2}) : var4x;
            };
            return this.triage == null ? var5.get() : this.triage.project(var5);
         }
      }
   }

   Path resolve(String var1) {
      return MaxPaths.resolveSandboxed(this.root, var1);
   }

   private static String key(Path var0) {
      return var0.toString();
   }

   private void forgetTracked(Path var1) {
      String var2 = key(var1);
      this.readMtimes.keySet().removeIf(var1x -> var1x.equals(var2) || var1x.startsWith(var2 + java.io.File.separator));
   }

   private static Pattern globToRegex(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '*') {
            if (var2 + 1 >= var0.length() || var0.charAt(var2 + 1) != '*') {
               var1.append("[^/]*");
            } else if (var2 + 2 < var0.length() && var0.charAt(var2 + 2) == '/') {
               var1.append("(.+/)?");
               var2 += 2;
            } else {
               var1.append(".*");
               var2++;
            }
         } else if (var3 == '?') {
            var1.append("[^/]");
         } else if ("\\.[]{}()+-^$|".indexOf(var3) >= 0) {
            var1.append('\\').append(var3);
         } else {
            var1.append(var3);
         }
      }

      return Pattern.compile(var1.toString());
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# File — sandboxed project-dir file access\n\nRead/write/list files under the serve project directory. Use it to author and iterate on\nreusable `checks/*.js` macros (then run them with `Check.run`), and as a file channel when\nshell/bash access is limited. All paths are relative to the project dir; a path that escapes\nit is denied (no host-filesystem access here).\n\n- `File.read(path)` → the file's text. Records the file's mtime so a later `write` can detect\n  a conflicting external change.\n- `File.write(path, content)` → `\"written: <path>\"`. Creates parent dirs. Refuses to overwrite\n  a file that changed on disk since you last `read` it (re-read, merge, retry). In `.md` files,\n  escaped backticks (`\\``) are auto-corrected to raw backticks. **For a multi-line file\n  (a `.feature`/`.js`), pass an ARRAY of lines** — `File.write('checks/x.feature', ['Feature: x',\n  '', '  Scenario: y', '    * print 1'])` — joined with newlines. This avoids a multi-line\n  string literal: a backtick template literal breaks on any backtick/`${` in the content, and a\n  quoted string can't span lines. (Zero-escaping HTTP alternative: `POST /api/write?path=<path>`\n  with the file body as the raw request body — e.g. `curl --data-binary @x.feature ...`.)\n- `File.writeBytes(path, bytes)` → `\"written: <path> (N bytes)\"`. The binary sibling — for a\n  non-text artifact like an upload-fixture PDF/PNG. Takes a `byte[]` or a base64 String; capped\n  at 25 MB. Drop a fixture under `checks/` then point an upload at it: `bot.act(loc,'upload','checks/x.pdf')`.\n- `File.list(glob?, opts?)` → matching relative paths (`**` = any dirs, `*` = a path segment,\n  `?` = one char; default `**/*`). E.g. `File.list('checks/*.js')`. A bare dir lists everything\n  under it. Walks recursively — for ONE folder level use `File.ls`. Generated trees (`target` ·\n  `build` · `dist` · `node_modules` · `runs` · `.git` · `.idea`) are not walked unless your glob\n  names one; past 200 matches the answer is an index —\n  `{total, shown, truncated, dirs:{<dir>:<count>}, files:[…], note}`. Opts: `{limit:n}`,\n  `{full:true}` (uncapped and unpruned).\n- `File.ls(dir?)` → one directory level: `[{name, dir, size, modified}]`, folders first then\n  files. `dir` defaults to the project root.\n- `File.exists(path)` → boolean.\n- `File.delete(path)` → `\"deleted: <path>\"` for a file (e.g. a throwaway check), or\n  `\"deleted: <path>/ (N files)\"` for a directory (removed recursively). Sandboxed to the\n  project; refuses only the project root, and reports if nothing was there.\n- `File.move(from, to)` → `\"moved: <from> -> <to>\"`. The one rename/move verb — keeps bytes\n  intact (binary-safe) and moves whole subtrees. Creates parent dirs; refuses to overwrite an\n  existing target (delete it first) and refuses the project root.\n- `File.copy(from, to)` → `\"copied: <from> -> <to>\"`. The non-destructive sibling of move —\n  binary-safe (e.g. duplicate an upload-fixture PDF under a new name, original kept), copies whole\n  subtrees. Creates parent dirs; refuses to overwrite an existing target and refuses the root.\n- `File.mkdir(path)` → `\"created: <path>/\"`. Create a real directory (and any missing parents) —\n  e.g. an empty folder to drop files into. Idempotent on an existing directory.\n- `File.tag(path, scenario, {add, remove})` → add/remove Gherkin tags on ONE scenario of a\n  `.feature` file — `File.tag('src/orders.feature', 'place an order', {add: '@req=ORD-001/1'})`.\n  `scenario` is the exact name or its keyword line number; `{feature: true}` (no `scenario`)\n  tags the feature itself, for every scenario in the file. Returns\n  `{line, tags, added, removed, unchanged?, absent?, inherited?}`; re-read before your next\n  `File.write` of that file.\n- `File.call(path, arg?)` → **run a project `.js` file here** — the console equivalent of a\n  feature's `karate.call('x.js')` / `callSingle('/x.js')` (there is no feature runtime in the\n  console, so `karate` is not bound). Karate's call semantics: the file is evaluated, and if\n  its value is a **function** it is invoked with `arg`. So the scaffold's own mock starter runs\n  as-is: `var started = File.call('/start-mock.js')` → `started.url`. The project's\n  `karate-boot.js` exts and `karate-config.js` vars are applied first (as for a check), and the\n  script's top-level vars/functions stay in the engine afterwards. A script that throws surfaces\n  its error — read it and fix the script. (To load config itself, use `Runner.config()`.)\n\nA path may be written **root-anchored** (`/start-mock.js`) or relative (`start-mock.js`) — both\nresolve from the project dir, the same rule the generated project files use. Only an escape\n*outside* the project (`../`) is denied.\n\nTypical check-authoring loop:\n```js\nFile.write('checks/login.js', \"// @description: log in\\nbot.go('/login')\\nbot.act('#user','input','admin')\")\nCheck.run('login.js')          // run it for real\n// read the error, edit, re-run:\nFile.read('checks/login.js')\n```"
      );
   }

   public interface Watcher {
      void changing(Path var1);

      void changed(Path var1);
   }
}
