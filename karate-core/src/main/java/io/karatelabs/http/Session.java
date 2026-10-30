package io.karatelabs.http;

import io.karatelabs.js.ObjectLike;
import java.util.HashMap;
import java.util.Map;

public class Session implements ObjectLike {
   private final String id;
   private final Map<String, Object> data;
   private final long created;
   private long updated;
   private long expires;
   public static final Session TEMPORARY = new Session(null, null, -1L, -1L, -1L);

   public static Session inMemory() {
      return new Session("-1", new HashMap<>(), -1L, -1L, -1L);
   }

   public Session(String id, Map<String, Object> data, long created, long updated, long expires) {
      this.id = id;
      this.data = data;
      this.created = created;
      this.updated = updated;
      this.expires = expires;
   }

   public boolean isTemporary() {
      return this.id == null;
   }

   public Session copy() {
      return new Session(this.id, new HashMap<>(this.data), this.created, this.updated, this.expires);
   }

   public void setUpdated(long updated) {
      this.updated = updated;
   }

   public void setExpires(long expires) {
      this.expires = expires;
   }

   public String getId() {
      return this.id;
   }

   public Map<String, Object> getData() {
      return this.data;
   }

   public long getCreated() {
      return this.created;
   }

   public long getUpdated() {
      return this.updated;
   }

   public long getExpires() {
      return this.expires;
   }

   @Override
   public Object getMember(String name) {
      return this.data.get(name);
   }

   @Override
   public void putMember(String name, Object value) {
      this.data.put(name, value);
   }

   @Override
   public void removeMember(String s) {
      this.data.remove(s);
   }

   @Override
   public Map<String, Object> toMap() {
      return this.data;
   }

   @Override
   public String toString() {
      return this.id;
   }
}
