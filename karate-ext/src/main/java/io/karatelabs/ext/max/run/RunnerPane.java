package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.jsapi.Runner;

class RunnerPane extends JsDispatch {
   RunnerPane(Runner var1) {
      this.fn("target", var1x -> var1.target(JsApi.str(var1x, 0)));
      this.fn("app", var1x -> var1.app(JsApi.str(var1x, 0)));
      this.fn("envs", var1x -> var1.envs());
      this.fn("env", var1x -> var1.switchEnv(JsApi.str(var1x, 0)));
      this.fn("auth", var1x -> var1.auth(JsApi.str(var1x, 0)));
      this.fn("tests", var1x -> var1.tests(JsApi.arg(var1x, 0)).stream().filter(var0x -> manual(String.valueOf(var0x.get("path")))).toList());
   }

   static boolean manual(String var0) {
      return !var0.startsWith("mutation/");
   }
}
