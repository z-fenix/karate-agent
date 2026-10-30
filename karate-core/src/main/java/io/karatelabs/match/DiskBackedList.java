package io.karatelabs.match;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Map.Entry;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskBackedList implements LargeValueStore {
   private static final Logger logger = LoggerFactory.getLogger(DiskBackedList.class);
   private final File tempFile;
   private final int size;
   private final List<Long> lineOffsets;
   private RandomAccessFile raf;
   private boolean closed = false;

   private DiskBackedList(File tempFile, int size, List<Long> lineOffsets) {
      this.tempFile = tempFile;
      this.size = size;
      this.lineOffsets = lineOffsets;
   }

   public static DiskBackedList create(Iterable<?> source) throws IOException {
      File tempFile = File.createTempFile("karate-match-", ".jsonl");
      tempFile.deleteOnExit();
      List<Long> offsets = new ArrayList<>();
      int count = 0;

      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
         long offset = 0L;

         for (Object item : source) {
            offsets.add(offset);
            String json = serializeItem(item);
            byte[] bytes = (json + "\n").getBytes(StandardCharsets.UTF_8);
            bos.write(bytes);
            offset += bytes.length;
            count++;
         }
      }

      logger.debug("created disk-backed list with {} items in {}", count, tempFile);
      return new DiskBackedList(tempFile, count, offsets);
   }

   public static long estimateSize(Object obj) {
      if (obj == null) {
         return 8L;
      } else if (obj instanceof String s) {
         return 40L + s.length() * 2L;
      } else if (obj instanceof Number) {
         return 24L;
      } else if (obj instanceof Boolean) {
         return 16L;
      } else if (obj instanceof byte[] bytes) {
         return 16 + bytes.length;
      } else if (obj instanceof List<?> list) {
         long size = 40L;

         for (Object item : list) {
            size += 8L + estimateSize(item);
         }

         return size;
      } else if (!(obj instanceof Map<?, ?> map)) {
         return 100L;
      } else {
         long size = 48L;

         for (Entry<?, ?> entry : map.entrySet()) {
            size += 32L + estimateSize(entry.getKey()) + estimateSize(entry.getValue());
         }

         return size;
      }
   }

   public static long estimateCollectionSize(List<?> list) {
      int SAMPLE_SIZE = 5;
      int size = list.size();
      if (size == 0) {
         return 40L;
      } else if (size <= 5) {
         long total = 40L;

         for (Object item : list) {
            total += 8L + estimateSize(item);
         }

         return total;
      } else {
         long sampleTotal = 0L;
         Random random = new Random();

         for (int i = 0; i < 5; i++) {
            int idx = random.nextInt(size);
            sampleTotal += 8L + estimateSize(list.get(idx));
         }

         long avgItemSize = sampleTotal / 5L;
         return 40L + avgItemSize * size;
      }
   }

   private static String serializeItem(Object item) {
      return item == null ? "null" : JSONValue.toJSONString(item);
   }

   private static Object deserializeItem(String json) {
      return json != null && !json.isEmpty() && !"null".equals(json) ? JSONValue.parse(json) : null;
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public Object get(int index) {
      if (this.closed) {
         throw new IllegalStateException("store has been closed");
      } else if (index >= 0 && index < this.size) {
         try {
            if (this.raf == null) {
               this.raf = new RandomAccessFile(this.tempFile, "r");
            }

            long offset = this.lineOffsets.get(index);
            this.raf.seek(offset);
            String line = this.raf.readLine();
            return deserializeItem(line);
         } catch (IOException var5) {
            throw new RuntimeException("failed to read item at index " + index, var5);
         }
      } else {
         throw new IndexOutOfBoundsException("index: " + index + ", size: " + this.size);
      }
   }

   @Override
   public Iterator<Object> iterator() {
      if (this.closed) {
         throw new IllegalStateException("store has been closed");
      } else {
         return new DiskBackedList.DiskBackedIterator();
      }
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         if (this.raf != null) {
            try {
               this.raf.close();
            } catch (IOException var2) {
               logger.warn("failed to close RandomAccessFile: {}", var2.getMessage());
            }

            this.raf = null;
         }

         if (this.tempFile.exists() && !this.tempFile.delete()) {
            logger.warn("failed to delete temp file: {}", this.tempFile);
         }
      }
   }

   private class DiskBackedIterator implements Iterator<Object> {
      private final BufferedReader reader;
      private int currentIndex = 0;
      private String nextLine = null;
      private boolean hasNextCalled = false;

      DiskBackedIterator() {
         try {
            this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(DiskBackedList.this.tempFile), StandardCharsets.UTF_8));
         } catch (FileNotFoundException var3) {
            throw new RuntimeException("temp file not found", var3);
         }
      }

      @Override
      public boolean hasNext() {
         if (this.hasNextCalled) {
            return this.nextLine != null;
         } else {
            this.hasNextCalled = true;
            if (this.currentIndex >= DiskBackedList.this.size) {
               this.closeReader();
               return false;
            } else {
               try {
                  this.nextLine = this.reader.readLine();
                  if (this.nextLine == null) {
                     this.closeReader();
                     return false;
                  } else {
                     return true;
                  }
               } catch (IOException var2) {
                  this.closeReader();
                  throw new RuntimeException("failed to read next line", var2);
               }
            }
         }
      }

      @Override
      public Object next() {
         if (!this.hasNextCalled) {
            this.hasNext();
         }

         if (this.nextLine == null) {
            throw new NoSuchElementException();
         } else {
            this.hasNextCalled = false;
            this.currentIndex++;
            return DiskBackedList.deserializeItem(this.nextLine);
         }
      }

      private void closeReader() {
         try {
            this.reader.close();
         } catch (IOException var2) {
            DiskBackedList.logger.warn("failed to close reader: {}", var2.getMessage());
         }
      }
   }
}
