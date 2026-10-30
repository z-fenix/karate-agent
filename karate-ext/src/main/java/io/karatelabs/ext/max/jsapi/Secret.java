package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Secret extends JsDispatch {
   private final io.karatelabs.ext.common.secrets.SecretStore store;
   private final Path root;
   private final Function<String, String> getenv;
   private Supplier<List<Path>> configDirs;
   private boolean noProject;

   public Secret(io.karatelabs.ext.common.secrets.SecretStore var1, Path var2) {
      this(var1, var2, System::getenv);
   }

   Secret(io.karatelabs.ext.common.secrets.SecretStore var1, Path var2, Function<String, String> var3) {
      this.root = var2.toAbsolutePath().normalize();
      this.store = var1.a(this.root);
      this.getenv = var3;
      this.configDirs = () -> List.of(this.root);
      this.fn("list", var1x -> this.list());
      this.fn("set", var1x -> this.set(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public Secret withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public Secret withConfigDirs(Supplier<List<Path>> var1) {
      this.configDirs = var1;
      return this;
   }

   Object list() {
      List var1;
      try {
         var1 = this.store.d();
      } catch (UncheckedIOException var9) {
         return JsApi.error(new RuntimeException("Secret.list could not read the store: " + var9.getMessage(), var9));
      }

      List var2 = io.karatelabs.ext.common.secrets.SecretStore.a(this.configDirs.get());
      TreeSet var3 = new TreeSet(var1);
      var3.addAll(var2);
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();

      for (String var6 : (Iterable<String>) (Iterable<?>) (var3)) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("name", var6);
         var7.put("stored", var1.contains(var6));
         String var8 = this.getenv.apply(var6);
         var7.put("env", var8 != null && !var8.isEmpty());
         var7.put("referenced", var2.contains(var6));
         var4.add(var7);
      }

      return var4;
   }

   Object set(String var1, Object var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Secret.set");
      } else if (var1 != null && io.karatelabs.ext.common.secrets.SecretStore.d.matcher(var1).matches()) {
         if (var2 instanceof String var3 && !var3.isEmpty()) {
            try {
               this.store.a(var1, var3);
            } catch (IllegalStateException var5) {
               return JsApi.missing("Secret.set refused: " + var5.getMessage());
            } catch (UncheckedIOException var6) {
               return JsApi.error(new RuntimeException("Secret.set could not write the store: " + var6.getMessage(), var6));
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            var4.put("name", var1);
            var4.put("stored", true);
            return var4;
         } else {
            return JsApi.missing("Secret.set('" + var1 + "', value): value is a non-empty string");
         }
      } else {
         return JsApi.missing("Secret.set(name, value): name is an environment-variable name, e.g. 'API_TOKEN'");
      }
   }

   Object delete(String var1) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Secret.delete");
      } else if (var1 != null && io.karatelabs.ext.common.secrets.SecretStore.d.matcher(var1).matches()) {
         boolean var2;
         try {
            var2 = this.store.a(var1);
         } catch (IllegalStateException var4) {
            return JsApi.missing("Secret.delete refused: " + var4.getMessage());
         } catch (UncheckedIOException var5) {
            return JsApi.error(new RuntimeException("Secret.delete could not write the store: " + var5.getMessage(), var5));
         }

         if (!var2) {
            return JsApi.notFound("no stored secret '" + var1 + "': Secret.list() shows the names");
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
            var3.put("name", var1);
            var3.put("deleted", true);
            return var3;
         }
      } else {
         return JsApi.missing("Secret.delete(name): name is an environment-variable name, e.g. 'API_TOKEN'");
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Secret: credential values, kept off git and out of model payloads\n\nkarate-config.js references a credential as `secret('API_TOKEN')`; only the reference is committed. In the console it resolves to the stored value, else the environment variable of the same name; a headless run reads the environment variable `API_TOKEN`. Commit a reference with `ApiTest.config({name, secret: 'API_TOKEN', ifMatch})`.\n\n- `Secret.list()` → `[{name, stored, env, referenced}]`: each name the store holds or karate-config references; `env` means the process environment has it. Values are never returned.\n- `Secret.set(name, value)` → `{name, stored}`. The person enters a value; never write one you were not given.\n- `Secret.delete(name)` → `{name, deleted}`."
      );
   }
}
