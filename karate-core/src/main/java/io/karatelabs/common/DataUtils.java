package io.karatelabs.common;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class DataUtils {
   private DataUtils() {
   }

   public static List<Map<String, Object>> fromCsv(String raw) {
      CsvReader<CsvRecord> reader = CsvReader.builder().ofCsvRecord(raw);
      List<String> header = new ArrayList<>();
      List<Map<String, Object>> rows = new ArrayList<>();

      try {
         boolean first = true;
         CloseableIterator var5 = reader.iterator();

         while (var5.hasNext()) {
            CsvRecord row = (CsvRecord)var5.next();
            if (first) {
               for (String field : row.getFields()) {
                  header.add(field.replace("\ufeff", ""));
               }

               first = false;
            } else {
               int count = header.size();
               Map<String, Object> map = new LinkedHashMap<>(count);

               for (int i = 0; i < count; i++) {
                  map.put(header.get(i), row.getField(i));
               }

               rows.add(map);
            }
         }

         return rows;
      } catch (Exception var10) {
         throw new RuntimeException(var10);
      }
   }

   public static String toCsv(List<Map<String, Object>> list) {
      StringWriter sw = new StringWriter();

      try {
         CsvWriter writer = CsvWriter.builder().build(sw);

         try {
            if (!list.isEmpty()) {
               writer.writeRecord(list.get(0).keySet());
            }

            for (Map<String, Object> map : list) {
               List<String> row = new ArrayList<>(map.size());

               for (Object value : map.values()) {
                  row.add(value == null ? null : value.toString());
               }

               writer.writeRecord(row);
            }
         } catch (Throwable var9) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException var10) {
         throw new RuntimeException(var10);
      }

      return sw.toString();
   }

   public static Object fromYaml(String raw) {
      LoaderOptions options = new LoaderOptions();
      options.setCodePointLimit(8388608);
      Yaml yaml = new Yaml(new SafeConstructor(options));
      return yaml.load(raw);
   }
}
