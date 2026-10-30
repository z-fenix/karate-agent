package io.karatelabs.gherkin;

import io.karatelabs.common.StringUtils;
import io.karatelabs.js.Engine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Table {
   private static final Logger logger = LoggerFactory.getLogger(Table.class);
   private final List<List<String>> rows;
   private final Map<String, Table.Column> colMap;
   private final List<Table.Column> cols;
   private final List<Integer> lineNumbers;
   private final String dynamicExpression;
   private List<SourceSpan> rowSpans;

   public String getDynamicExpression() {
      return this.dynamicExpression;
   }

   public boolean isDynamic() {
      return this.dynamicExpression != null;
   }

   public Collection<String> getKeys() {
      return this.colMap.keySet();
   }

   public int getLineNumberForRow(int i) {
      return this.lineNumbers.get(i);
   }

   public SourceSpan getRowSpan(int i) {
      return this.rowSpans == null ? null : this.rowSpans.get(i);
   }

   void setRowSpans(List<SourceSpan> rowSpans) {
      this.rowSpans = rowSpans;
   }

   public Table replace(String token, String value) {
      int rowCount = this.rows.size();
      List<String> keys = this.rows.get(0);
      int colCount = keys.size();
      List<List<String>> list = new ArrayList<>(rowCount);
      list.add(keys);

      for (int i = 1; i < rowCount; i++) {
         List<String> row = this.rows.get(i);
         List<String> replaced = new ArrayList<>(colCount);
         list.add(replaced);

         for (int j = 0; j < colCount; j++) {
            String text = row.get(j).replace(token, value);
            replaced.add(text);
         }
      }

      return new Table(list, this.lineNumbers);
   }

   public String getValueAsString(String key, int row) {
      Table.Column col = this.colMap.get(key);
      return col == null ? null : this.rows.get(row).get(col.index);
   }

   public static Table fromKarateJson(List<Map<String, Object>> tableRows) {
      List<List<String>> rows = new ArrayList<>(tableRows.size());
      List<Integer> lines = new ArrayList<>(tableRows.size());

      for (Map<String, Object> row : tableRows) {
         rows.add((List<String>)row.get("row"));
         lines.add((Integer)row.get("line"));
      }

      return new Table(rows, lines);
   }

   public List<Map<String, Object>> toJson() {
      int count = this.rows.size();
      List<Map<String, Object>> list = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         Map<String, Object> map = new HashMap<>(2);
         list.add(map);
         map.put("row", this.rows.get(i));
         map.put("line", this.lineNumbers.get(i));
      }

      return list;
   }

   public Table(List<List<String>> rows, List<Integer> lineNumbers) {
      this.rows = rows;
      this.lineNumbers = lineNumbers;
      List<String> keys = rows.get(0);
      int colCount = keys.size();
      this.colMap = new LinkedHashMap<>(colCount);

      for (int i = 0; i < colCount; i++) {
         String key = keys.get(i);
         Table.ColumnType type;
         if (key.endsWith("!")) {
            key = key.substring(0, key.length() - 1);
            type = Table.ColumnType.EVALUATED;
         } else {
            type = Table.ColumnType.STRING;
         }

         this.colMap.put(key, new Table.Column(key, i, type));
      }

      if (colCount == 1 && rows.size() == 1) {
         this.dynamicExpression = keys.get(0);
      } else {
         this.dynamicExpression = null;
      }

      this.cols = new ArrayList<>(this.colMap.values());
   }

   public List<List<String>> getRows() {
      return this.rows;
   }

   public List<Map<String, String>> getRowsAsMaps() {
      int rowCount = this.rows.size();
      List<Map<String, String>> list = new ArrayList<>(rowCount - 1);

      for (int i = 1; i < rowCount; i++) {
         Map<String, String> map = new LinkedHashMap<>(this.cols.size());
         list.add(map);
         List<String> row = this.rows.get(i);

         for (Table.Column col : this.cols) {
            String value = col.index < row.size() ? row.get(col.index) : "";
            map.put(col.key, value);
         }
      }

      return list;
   }

   public List<Map<String, Object>> getRowsAsMapsConverted() {
      int rowCount = this.rows.size();
      List<Map<String, Object>> list = new ArrayList<>(rowCount - 1);

      for (int i = 1; i < rowCount; i++) {
         Map<String, Object> map = new LinkedHashMap<>(this.cols.size());
         list.add(map);
         List<String> row = this.rows.get(i);

         for (Table.Column col : this.cols) {
            String raw = col.index < row.size() ? row.get(col.index) : "";
            map.put(col.key, convert(raw, col));
         }
      }

      return list;
   }

   private static Object convert(String raw, Table.Column col) {
      try {
         switch (col.type) {
            case EVALUATED:
               if (StringUtils.isBlank(raw)) {
                  return null;
               }

               if (StringUtils.looksLikeJson(raw)) {
                  raw = "(" + raw + ")";
               }

               Engine engine = new Engine();
               engine.setLeadingFunctionExpression(true);
               return engine.eval(raw);
            default:
               return StringUtils.isBlank(raw) ? null : raw;
         }
      } catch (Exception var3) {
         if (logger.isTraceEnabled()) {
            logger.trace("type conversion failed for column: {}, type: {} - {}", new Object[]{col.key, col.type, var3.getMessage()});
         }

         return raw;
      }
   }

   public Map<String, Object> getExampleData(int exampleIndex) {
      List<String> row = this.rows.get(exampleIndex + 1);
      Map<String, Object> map = new LinkedHashMap<>(this.cols.size());

      for (Table.Column col : this.cols) {
         String raw = row.get(col.index);
         map.put(col.key, convert(raw, col));
      }

      return map;
   }

   public Map<String, String> getRawExampleData(int exampleIndex) {
      List<String> row = this.rows.get(exampleIndex + 1);
      Map<String, String> map = new LinkedHashMap<>(this.cols.size());

      for (Table.Column col : this.cols) {
         String raw = col.index < row.size() ? row.get(col.index) : "";
         map.put(col.key, raw);
      }

      return map;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('\n');

      for (List<String> row : this.rows) {
         sb.append('|').append('\t');

         for (String s : row) {
            sb.append(s).append('\t').append('|');
         }

         sb.append('\n');
      }

      return sb.toString();
   }

   static class Column {
      final String key;
      final int index;
      final Table.ColumnType type;

      Column(String key, int index, Table.ColumnType type) {
         this.key = key;
         this.index = index;
         this.type = type;
      }
   }

   static enum ColumnType {
      STRING,
      EVALUATED;
   }
}
