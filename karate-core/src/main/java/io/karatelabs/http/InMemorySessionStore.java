package io.karatelabs.http;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {
   private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
   private long lastCleanup = System.currentTimeMillis();
   private static final long CLEANUP_INTERVAL_MS = 60000L;

   @Override
   public Session create(int expirySeconds) {
      this.cleanupExpiredIfNeeded();
      long now = Instant.now().getEpochSecond();
      long expires = now + expirySeconds;
      String id = UUID.randomUUID().toString();
      Session session = new Session(id, new HashMap<>(), now, now, expires);
      this.sessions.put(id, session);
      return session;
   }

   @Override
   public Session get(String id) {
      if (id == null) {
         return null;
      } else {
         this.cleanupExpiredIfNeeded();
         Session session = this.sessions.get(id);
         if (session == null) {
            return null;
         } else if (this.isExpired(session)) {
            this.sessions.remove(id);
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
         this.sessions.put(session.getId(), session);
      }
   }

   @Override
   public void delete(String id) {
      if (id != null) {
         this.sessions.remove(id);
      }
   }

   public int size() {
      return this.sessions.size();
   }

   public void clear() {
      this.sessions.clear();
   }

   private boolean isExpired(Session session) {
      return session.getExpires() > 0L && Instant.now().getEpochSecond() > session.getExpires();
   }

   private void cleanupExpiredIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - this.lastCleanup >= 60000L) {
         this.lastCleanup = now;
         Iterator<Entry<String, Session>> iterator = this.sessions.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<String, Session> entry = iterator.next();
            if (this.isExpired(entry.getValue())) {
               iterator.remove();
            }
         }
      }
   }
}
