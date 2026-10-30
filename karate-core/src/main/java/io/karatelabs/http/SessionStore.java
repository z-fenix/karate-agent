package io.karatelabs.http;

public interface SessionStore {
   Session create(int var1);

   Session get(String var1);

   void save(Session var1);

   void delete(String var1);
}
