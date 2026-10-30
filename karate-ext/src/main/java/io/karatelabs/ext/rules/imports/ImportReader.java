package io.karatelabs.ext.rules.imports;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface ImportReader {
   String FORMULA = "formula";
   String MERGED = "merged";
   String DATE = "date";
   String TIME = "time";
   String POINTS = "points";
   String FRACTION = "fraction";

   Set<String> extensions();

   ImportReader.Sheet read(Path var1, String var2) throws IOException;

   public record Cell(String text, String ref, String unit, Set<String> flags, int pad) {

      public Cell(String text, String ref, String unit, Set<String> flags, int pad) {
         this.text = text;
         this.ref = ref;
         this.unit = unit;
         this.flags = flags;
         this.pad = pad;
      }

      public static ImportReader.Cell text(String var0, String var1) {
         return new ImportReader.Cell(var0, var1, null, Set.of(), 0);
      }

      public boolean has(String var1) {
         return this.flags.contains(var1);
      }
   }

   public static final class Refused extends RuntimeException {
      private final String code;

      public Refused(String var1, String var2) {
         super(var2);
         this.code = var1;
      }

      public String code() {
         return this.code;
      }
   }

   public record Row(int number, List<ImportReader.Cell> cells) {

      public Row(int number, List<ImportReader.Cell> cells) {
         this.number = number;
         this.cells = cells;
      }
   }

   public record Sheet(String name, List<ImportReader.Row> rows) {

      public Sheet(String name, List<ImportReader.Row> rows) {
         this.name = name;
         this.rows = rows;
      }
   }
}
