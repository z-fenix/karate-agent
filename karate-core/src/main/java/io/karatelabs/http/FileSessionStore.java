package io.karatelabs.http;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSessionStore implements SessionStore {
   private static final Logger logger = LoggerFactory.getLogger(FileSessionStore.class);
   private final Path directory;
   private long lastCleanup = System.currentTimeMillis();
   private static final long CLEANUP_INTERVAL_MS = 300000L;
   private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");

   public FileSessionStore(Path directory) {
      this.directory = directory;

      try {
         Files.createDirectories(directory);
      } catch (IOException var3) {
         throw new RuntimeException("Failed to create session directory: " + directory, var3);
      }
   }

   @Override
   public Session create(int expirySeconds) {
      this.cleanupExpiredIfNeeded();
      long now = Instant.now().getEpochSecond();
      long expires = now + expirySeconds;
      String id = UUID.randomUUID().toString();
      Session session = new Session(id, new HashMap<>(), now, now, expires);
      this.writeToDisk(session);
      return session;
   }

   @Override
   public Session get(String id) {
      if (id == null) {
         return null;
      } else {
         this.cleanupExpiredIfNeeded();
         Session session = this.readFromDisk(id);
         if (session == null) {
            return null;
         } else if (this.isExpired(session)) {
            this.delete(id);
            return null;
         } else {
            return session;
         }
      }
   }

   @Override
   public void save(Session session) {
      if (session != null && session.getId() != null) {
         session.setUpdated(Instant.now().getEpochSecond());
         this.writeToDisk(session);
      }
   }

   @Override
   public void delete(String id) {
      if (id != null) {
         Path file = this.sessionFile(id);
         if (file != null) {
            try {
               Files.deleteIfExists(file);
            } catch (IOException var4) {
               logger.warn("failed to delete session file {}: {}", file, var4.getMessage());
            }
         }
      }
   }

   private Path sessionFile(String id) {
      return SAFE_ID.matcher(id).matches() ? this.directory.resolve(id + ".json") : null;
   }

   private Session readFromDisk(String id) {
      Path file = this.sessionFile(id);
      if (file != null && Files.exists(file)) {
         try {
            String json = Files.readString(file);
            Map<String, Object> map = Json.of(json).value();
            long created = ((Number)map.getOrDefault("created", 0L)).longValue();
            long updated = ((Number)map.getOrDefault("updated", 0L)).longValue();
            long expires = ((Number)map.getOrDefault("expires", 0L)).longValue();
            Map<String, Object> data = (Map<String, Object>)map.getOrDefault("data", new HashMap());
            return new Session(id, data, created, updated, expires);
         } catch (Exception var12) {
            logger.warn("failed to read session {}: {}", id, var12.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private void writeToDisk(Session session) {
      Path file = this.sessionFile(session.getId());
      if (file == null) {
         logger.warn("refusing to write session with unsafe id");
      } else {
         Map<String, Object> map = new HashMap<>();
         map.put("id", session.getId());
         map.put("created", session.getCreated());
         map.put("updated", session.getUpdated());
         map.put("expires", session.getExpires());
         map.put("data", session.getData());

         try {
            String json = Json.of(map).toString();
            Files.writeString(file, json);
         } catch (IOException var5) {
            logger.warn("failed to write session {}: {}", session.getId(), var5.getMessage());
         }
      }
   }

   private boolean isExpired(Session session) {
      return session.getExpires() > 0L && Instant.now().getEpochSecond() > session.getExpires();
   }

   private void cleanupExpiredIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - this.lastCleanup >= 300000L) {
         this.lastCleanup = now;

         try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.directory, "*.json")) {
            for (Path file : stream) {
               String id = file.getFileName().toString().replace(".json", "");
               Session session = this.readFromDisk(id);
               if (session != null && this.isExpired(session)) {
                  Files.deleteIfExists(file);
               }
            }
         } catch (IOException var10) {
            logger.warn("session cleanup failed: {}", var10.getMessage());
         }
      }
   }
}
