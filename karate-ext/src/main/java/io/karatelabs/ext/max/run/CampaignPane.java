package io.karatelabs.ext.max.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.jsapi.Campaign;

class CampaignPane extends JsDispatch {
   CampaignPane(Campaign var1) {
      this.fn("list", var1x -> var1.list());
      this.fn("get", var1x -> var1.get(JsApi.str(var1x, 0)));
   }
}
