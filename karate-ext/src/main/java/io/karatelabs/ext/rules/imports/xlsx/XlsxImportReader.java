package io.karatelabs.ext.rules.imports.xlsx;

import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.rules.imports.ImportReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.dhatim.fastexcel.reader.CellAddress;
import org.dhatim.fastexcel.reader.CellRangeAddress;
import org.dhatim.fastexcel.reader.ExcelReaderException;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;

public class XlsxImportReader implements ImportReader {
   private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
   private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
   private static final Pattern LITERALS = Pattern.compile("\"[^\"]*\"|\\\\.|_.|\\*.|\\[[^]]*]");
   private static final Pattern DATE_CODES = Pattern.compile("[ymdhsYMDHS]");
   private static final Pattern LEADING_ZEROS = Pattern.compile("0{2,}");

   @Override
   public Set<String> extensions() {
      return Set.of("xlsx");
   }

   @Override
   public ImportReader.Sheet read(Path var1, String var2) throws IOException {
      Main.requireProduct("rules", "rules");
      return parse(Files.readAllBytes(var1), var2);
   }

   public static ImportReader.Sheet sheet(byte[] var0, String var1) throws IOException {
      Main.requireLicensed("xlsx data");
      return parse(var0, var1);
   }

   private static ImportReader.Sheet parse(byte[] var0, String var1) throws IOException {
      ReadableWorkbook var2;
      try {
         var2 = new ReadableWorkbook(new ByteArrayInputStream(var0), new ReadingOptions(true, false));
      } catch (ExcelReaderException | IOException var18) {
         throw new ImportReader.Refused("IMPORT_SOURCE_INVALID", "not a readable .xlsx workbook: " + var18.getMessage());
      }

      try {
         ReadableWorkbook var3 = var2;

         ImportReader.Sheet var25;
         try {
            org.dhatim.fastexcel.reader.Sheet var4 = var1 == null ? var3.getFirstSheet() : var3.findSheet(var1).orElse(null);
            if (var4 == null) {
               throw new ImportReader.Refused(
                  "IMPORT_SHEET_MISSING",
                  "no sheet '" + var1 + "' (sheets: " + String.join(", ", var3.getSheets().map(org.dhatim.fastexcel.reader.Sheet::getName).toList()) + ")"
               );
            }

            var var5 = mergedRanges(var0, var4.getId());
            TreeMap<java.lang.Integer, org.dhatim.fastexcel.reader.Row> var6 = new TreeMap();

            for (org.dhatim.fastexcel.reader.Row var8 : var4.read()) {
               var6.put(var8.getRowNum(), var8);
            }

            int var21 = var6.isEmpty() ? 0 : (Integer)var6.lastKey();

            for (CellRangeAddress var9 : (Iterable<CellRangeAddress>) (Iterable<?>) (var5)) {
               var21 = Math.max(var21, var9.getLastRow() + 1);
            }

            ArrayList<io.karatelabs.ext.rules.imports.ImportReader.Row> var23 = new ArrayList();

            for (int var24 = 1; var24 <= var21; var24++) {
               org.dhatim.fastexcel.reader.Row var10 = (org.dhatim.fastexcel.reader.Row)var6.get(var24);
               int var11 = var24 - 1;
               int var12 = var10 == null ? 0 : var10.getCellCount();

               for (CellRangeAddress var14 : (Iterable<CellRangeAddress>) (Iterable<?>) (var5)) {
                  if (var14.containsRow(var11)) {
                     var12 = Math.max(var12, var14.getLastColumn() + 1);
                  }
               }

               ArrayList<io.karatelabs.ext.rules.imports.ImportReader.Cell> var26 = new ArrayList(var12);

               for (int var27 = 0; var27 < var12; var27++) {
                  int var15 = var27;
                  boolean var16 = var5.stream().anyMatch(var2x -> var2x.isInRange(var11, var15));
                  var26.add(cell(var10 != null && var27 < var10.getCellCount() ? var10.getCell(var27) : null, new CellAddress(var11, var15).toString(), var16));
               }

               var23.add(new ImportReader.Row(var24, var26));
            }

            var25 = new ImportReader.Sheet(var4.getName(), var23);
         } catch (Throwable var19) {
            if (var2 != null) {
               try {
                  var3.close();
               } catch (Throwable var17) {
                  var19.addSuppressed(var17);
               }
            }

            throw var19;
         }

         if (var2 != null) {
            var2.close();
         }

         return var25;
      } catch (XMLStreamException | ZipException | ExcelReaderException var20) {
         throw new ImportReader.Refused("IMPORT_SOURCE_INVALID", "not a readable .xlsx workbook: " + var20.getMessage());
      }
   }

   public static List<Map<String, Object>> records(ImportReader.Sheet var0) {
      ArrayList<java.lang.String> var1 = null;
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (ImportReader.Row var4 : var0.rows()) {
         if (!var4.cells().stream().allMatch(var0x -> var0x.text().isBlank())) {
            if (var1 == null) {
               var1 = new ArrayList();

               for (ImportReader.Cell var8 : var4.cells()) {
                  var1.add(var8.text().strip());
               }
            } else {
               LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();

               for (int var6 = 0; var6 < var1.size(); var6++) {
                  if (!((String)var1.get(var6)).isEmpty()) {
                     var5.put((String)var1.get(var6), var6 < var4.cells().size() ? text(var4.cells().get(var6)) : "");
                  }
               }

               var2.add(var5);
            }
         }
      }

      return var2;
   }

   public static String text(ImportReader.Cell var0) {
      return var0.pad() > var0.text().length() ? "0".repeat(var0.pad() - var0.text().length()) + var0.text() : var0.text();
   }

   static ImportReader.Cell cell(org.dhatim.fastexcel.reader.Cell var0, String var1, boolean var2) {
      LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();
      if (var2) {
         var3.add("merged");
      }

      if (var0 == null) {
         return new ImportReader.Cell("", var1, null, var3, 0);
      } else {
         if (var0.getFormula() != null) {
            var3.add("formula");
         }

         String var4 = var0.getDataFormatString() == null ? "" : LITERALS.matcher(var0.getDataFormatString()).replaceAll("");
         if (var0.getValue() instanceof BigDecimal var8) {
            if (!var4.equalsIgnoreCase("General") && DATE_CODES.matcher(var4).find()) {
               LocalDateTime var10 = var0.asDate();
               var3.add("date");
               if (var10.toLocalTime().toNanoOfDay() != 0L) {
                  var3.add("time");
                  return new ImportReader.Cell(var10.format(DAY_TIME), var1, null, var3, 0);
               } else {
                  return new ImportReader.Cell(var10.format(DAY), var1, null, var3, 0);
               }
            } else {
               String var9 = var4.split(";", -1)[0];
               int var7 = LEADING_ZEROS.matcher(var9).matches() ? var9.length() : 0;
               return new ImportReader.Cell(var8.stripTrailingZeros().toPlainString(), var1, var4.contains("%") ? "fraction" : null, var3, var7);
            }
         } else {
            String var5 = var0.getValue() == null ? "" : var0.getValue().toString();
            return new ImportReader.Cell(var5, var1, var5.strip().endsWith("%") ? "points" : null, var3, 0);
         }
      }
   }

   static List<CellRangeAddress> mergedRanges(byte[] var0, String var1) throws IOException, XMLStreamException {
      var var2 = parts(var0, var0x -> var0x.endsWith(".rels"));
      String var3 = target(var2, "_rels/.rels", "", var0x -> var0x.endsWith("/officeDocument"), "Type");
      String var4 = var3.substring(0, var3.lastIndexOf(47) + 1);
      String var5 = target(var2, var4 + "_rels/" + var3.substring(var4.length()) + ".rels", var4, var1::equals, "Id");
      ArrayList<org.dhatim.fastexcel.reader.CellRangeAddress> var6 = new ArrayList();
      XMLStreamReader var7 = xml(parts(var0, var5::equals), var5);

      try {
         while (var7.hasNext()) {
            if (var7.next() == 1 && var7.getLocalName().equals("mergeCell")) {
               var6.add(CellRangeAddress.valueOf(var7.getAttributeValue(null, "ref")));
            }
         }
      } finally {
         var7.close();
      }

      return var6;
   }

   private static Map<String, byte[]> parts(byte[] var0, Predicate<String> var1) throws IOException {
      HashMap<java.lang.String, byte[]> var2 = new HashMap();

      try (ZipInputStream var3 = new ZipInputStream(new ByteArrayInputStream(var0))) {
         for (ZipEntry var4 = var3.getNextEntry(); var4 != null; var4 = var3.getNextEntry()) {
            if (var1.test(var4.getName())) {
               var2.put(var4.getName(), var3.readAllBytes());
            }
         }
      }

      return var2;
   }

   private static String target(Map<String, byte[]> var0, String var1, String var2, Predicate<String> var3, String var4) throws XMLStreamException {
      XMLStreamReader var5 = xml(var0, var1);

      try {
         while (var5.hasNext()) {
            if (var5.next() == 1 && var5.getLocalName().equals("Relationship") && var3.test(String.valueOf(var5.getAttributeValue(null, var4)))) {
               String var6 = var5.getAttributeValue(null, "Target");
               return var6.startsWith("/") ? var6.substring(1) : var2 + var6;
            }
         }
      } finally {
         var5.close();
      }

      throw new ImportReader.Refused("IMPORT_SOURCE_INVALID", "not a readable .xlsx workbook: no relationship in " + var1);
   }

   private static XMLStreamReader xml(Map<String, byte[]> var0, String var1) throws XMLStreamException {
      byte[] var2 = (byte[])var0.get(var1);
      if (var2 == null) {
         throw new ImportReader.Refused("IMPORT_SOURCE_INVALID", "not a readable .xlsx workbook: no part " + var1);
      } else {
         XMLInputFactory var3 = XMLInputFactory.newDefaultFactory();
         var3.setProperty("javax.xml.stream.supportDTD", false);
         var3.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
         return var3.createXMLStreamReader(new ByteArrayInputStream(var2));
      }
   }
}
