package io.karatelabs.ext.postman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

public class CurlRequestOptions implements Callable<Void> {
   @Option(
      names = {"--url"},
      description = {"url"}
   )
   public List<String> a;
   @Option(
      names = {"-L", "--location"},
      description = {"location"}
   )
   public boolean b;
   @Option(
      names = {"-X", "--request"},
      description = {"method"}
   )
   public String c;
   @Option(
      names = {"-H", "--header"},
      description = {"header"}
   )
   public List<String> d;
   @Option(
      names = {"-d", "--data"},
      description = {"data"}
   )
   public List<String> e;
   @Option(
      names = {"--data-raw"},
      description = {"data-raw"}
   )
   public String f;
   @Option(
      names = {"--data-urlencode"},
      description = {"data-urlencode"}
   )
   public List<String> g;
   @Option(
      names = {"-F", "--form"},
      description = {"multipart form data"}
   )
   public List<String> h;
   @Option(
      names = {"-u", "--user"},
      description = {"user:password for authentication"}
   )
   public String i;
   @Option(
      names = {"-A", "--user-agent"},
      description = {"user agent string"}
   )
   public String j;
   @Option(
      names = {"-b", "--cookie"},
      description = {"cookie string"}
   )
   public String k;
   @Option(
      names = {"-e", "--referer"},
      description = {"referer URL"}
   )
   public String l;
   @Option(
      names = {"--compressed"},
      description = {"compressed"}
   )
   public boolean m;
   @Unmatched
   public List<String> n;
   private static final Pattern o = Pattern.compile("^.?http.?://");

   @Override
   public Void call() throws Exception {
      return this.a();
   }

   public Void a() throws Exception {
      return null;
   }

   public static CurlRequestOptions a(String[] var0) {
      var var1 = Arrays.asList(var0);
      ArrayList<java.lang.String> var2 = new ArrayList(var1.size());
      int var3 = var1.size();

      for (int var4 = 0; var4 < var3; var4++) {
         String var5 = (String)var1.get(var4);
         if ((var4 != 0 || !var5.trim().equals("curl")) && !var5.trim().equals("\\")) {
            if (o.matcher(var5).find()) {
               var2.add("--url=" + var5);
            } else {
               var2.add(var5);
            }
         }
      }

      var0 = var2.toArray(new String[0]);
      CurlRequestOptions var7 = (CurlRequestOptions)CommandLine.populateCommand(new CurlRequestOptions(), var0);
      if (var7.k != null && var7.n != null && !var7.n.isEmpty() && var7.k.startsWith("\"")) {
         var7.k = var7.k.substring(1);
         var7.k = var7.k + " " + String.join(" ", var7.n);
         var7.n = null;
      }

      return var7;
   }
}
