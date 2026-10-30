package io.karatelabs.ext.max.demo;

import io.karatelabs.common.Json;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class DemoApp {
   static final double GOLD_DISCOUNT = 0.1;
   static final double SILVER_DISCOUNT = 0.05;
   static final double RUSH_RATE = 0.15;
   static final double FREE_SHIP_THRESHOLD = 100.0;
   static final double SHIPPING_FEE = 9.99;
   static final double REVIEW_THRESHOLD = 1000.0;
   private volatile DemoApp.Config config = new DemoApp.Config();
   static final List<DemoApp.Product> CATALOG = List.of(
      new DemoApp.Product("WIDGET", "Widget", "Widgets", money(25.0)),
      new DemoApp.Product("GADGET", "Gadget", "Gadgets", money(80.0)),
      new DemoApp.Product("GIZMO", "Gizmo", "Gadgets", money(12.5)),
      new DemoApp.Product("CABLE", "Cable", "Accessories", money(5.0))
   );
   private static final Map<String, DemoApp.Product> BY_SKU = new LinkedHashMap<>();
   static final String DEMO_USER = "tester";
   static final String DEMO_PASSWORD = "demo";
   private static final String ORDERS_PREFIX = "/demo/api/orders/";
   private final Set<String> tokens = ConcurrentHashMap.newKeySet();
   private final AtomicInteger logins = new AtomicInteger();
   private final AtomicInteger creates = new AtomicInteger();
   private final AtomicInteger orderSeq = new AtomicInteger(1000);
   private final Map<Integer, Map<String, Object>> orders = new ConcurrentHashMap<>();
   private static final String PAGE = "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"utf-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title>Acme Orders — demo storefront</title>\n  <style>\n    body { font: 15px/1.5 system-ui, sans-serif; margin: 0; color: #1e293b; background: #f8fafc; }\n    header { background: #4f46e5; color: #fff; padding: 16px 24px; }\n    header h1 { margin: 0; font-size: 18px; }\n    header p { margin: 2px 0 0; font-size: 13px; opacity: .85; }\n    main { max-width: 760px; margin: 24px auto; padding: 0 16px; }\n    section { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 20px; margin-bottom: 18px; }\n    h2 { font-size: 14px; text-transform: uppercase; letter-spacing: .05em; color: #64748b; margin: 0 0 12px; }\n    table { width: 100%; border-collapse: collapse; }\n    th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }\n    td.num, th.num { text-align: right; }\n    input[type=number] { width: 56px; padding: 4px; }\n    label { margin-right: 16px; }\n    button { background: #4f46e5; color: #fff; border: 0; padding: 9px 18px; border-radius: 8px; font-size: 14px; cursor: pointer; }\n    #quote { display: none; }\n    #quote.show { display: block; }\n    .total { font-size: 20px; font-weight: 700; }\n    .row { display: flex; justify-content: space-between; padding: 3px 0; }\n    .muted { color: #64748b; }\n  </style>\n</head>\n<body>\n  <header>\n    <h1>Acme Orders</h1>\n    <p>Demo storefront — build a cart, get a quote.</p>\n  </header>\n  <main>\n    <section>\n      <h2>Catalog</h2>\n      <table>\n        <thead><tr><th>Product</th><th>Category</th><th class=\"num\">Unit price</th><th class=\"num\">Qty</th></tr></thead>\n        <tbody id=\"catalog\"></tbody>\n      </table>\n    </section>\n    <section>\n      <h2>Options</h2>\n      <label>Tier\n        <select id=\"tier\">\n          <option value=\"standard\">Standard</option>\n          <option value=\"silver\">Silver</option>\n          <option value=\"gold\">Gold</option>\n        </select>\n      </label>\n      <label><input type=\"checkbox\" id=\"rush\"> Rush delivery</label>\n      <div style=\"margin-top:14px\"><button id=\"get-quote\">Get quote</button></div>\n    </section>\n    <section id=\"quote\">\n      <h2>Quote</h2>\n      <div class=\"row\"><span>Subtotal</span><span id=\"subtotal\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Discount</span><span id=\"discount\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Rush surcharge</span><span id=\"rushSurcharge\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Shipping</span><span id=\"shipping\" class=\"num\"></span></div>\n      <div class=\"row total\"><span>Total</span><span id=\"total\" class=\"num\"></span></div>\n      <div class=\"row\"><span>Disposition</span><span id=\"outcome\"></span></div>\n    </section>\n    <section id=\"admin\">\n      <h2>Pricing config (admin) — simulate a drift from the rules</h2>\n      <p class=\"muted\" style=\"margin-top:0\">These knobs default to the spec, so the storefront agrees with the rulebook oracle.\n         Change one (e.g. drop the gold discount to <code>0.05</code>) and re-run the checks — they go <b>red</b> and the\n         rules-vs-live diff lights up. <b>Reset to spec</b> is the fix.</p>\n      <div style=\"display:flex; flex-wrap:wrap; gap:10px 18px; align-items:end\">\n        <label>Gold discount <input type=\"number\" step=\"0.01\" id=\"cfg-goldDiscount\" class=\"cfg\" data-knob=\"goldDiscount\"></label>\n        <label>Silver discount <input type=\"number\" step=\"0.01\" id=\"cfg-silverDiscount\" class=\"cfg\" data-knob=\"silverDiscount\"></label>\n        <label>Rush rate <input type=\"number\" step=\"0.01\" id=\"cfg-rushRate\" class=\"cfg\" data-knob=\"rushRate\"></label>\n        <label>Free-ship over <input type=\"number\" step=\"1\" id=\"cfg-freeShipThreshold\" class=\"cfg\" data-knob=\"freeShipThreshold\"></label>\n        <label>Shipping fee <input type=\"number\" step=\"0.01\" id=\"cfg-shippingFee\" class=\"cfg\" data-knob=\"shippingFee\"></label>\n        <label>Review over <input type=\"number\" step=\"1\" id=\"cfg-reviewThreshold\" class=\"cfg\" data-knob=\"reviewThreshold\"></label>\n      </div>\n      <div style=\"margin-top:14px; display:flex; gap:10px\">\n        <button id=\"save-config\">Apply drift</button>\n        <button id=\"reset-config\" style=\"background:#475569\">Reset to spec</button>\n        <span id=\"config-status\" class=\"muted\"></span>\n      </div>\n    </section>\n  </main>\n  <script>\n    var fmt = function (n) { return '$' + Number(n).toFixed(2); };\n    fetch('/demo/api/products').then(function (r) { return r.json(); }).then(function (data) {\n      var rows = data.products.map(function (p) {\n        return '<tr data-sku=\"' + p.sku + '\">'\n          + '<td>' + p.name + '</td><td>' + p.category + '</td>'\n          + '<td class=\"num\">' + fmt(p.unitPrice) + '</td>'\n          + '<td class=\"num\"><input type=\"number\" min=\"0\" value=\"0\" class=\"qty\" data-sku=\"' + p.sku + '\"></td></tr>';\n      });\n      document.getElementById('catalog').innerHTML = rows.join('');\n    });\n    var loadConfig = function () {\n      fetch('/demo/api/config').then(function (r) { return r.json(); }).then(function (cfg) {\n        document.querySelectorAll('.cfg').forEach(function (el) { el.value = cfg[el.getAttribute('data-knob')]; });\n      });\n    };\n    loadConfig();\n    document.getElementById('save-config').addEventListener('click', function () {\n      var patch = {};\n      document.querySelectorAll('.cfg').forEach(function (el) { patch[el.getAttribute('data-knob')] = parseFloat(el.value); });\n      fetch('/demo/api/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(patch) })\n        .then(function (r) { return r.json(); })\n        .then(function (c) { document.getElementById('config-status').textContent = 'applied — re-run a check to see the drift'; });\n    });\n    document.getElementById('reset-config').addEventListener('click', function () {\n      fetch('/demo/api/config/reset', { method: 'POST' }).then(function (r) { return r.json(); }).then(function () {\n        loadConfig();\n        document.getElementById('config-status').textContent = 'reset to spec';\n      });\n    });\n    document.getElementById('get-quote').addEventListener('click', function () {\n      var items = [];\n      document.querySelectorAll('.qty').forEach(function (el) {\n        var qty = parseInt(el.value, 10) || 0;\n        if (qty > 0) { items.push({ sku: el.getAttribute('data-sku'), qty: qty }); }\n      });\n      var payload = { tier: document.getElementById('tier').value, rush: document.getElementById('rush').checked, items: items };\n      fetch('/demo/api/quote', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })\n        .then(function (r) { return r.json(); })\n        .then(function (q) {\n          if (q.error) { alert(q.error); return; }\n          document.getElementById('subtotal').textContent = fmt(q.subtotal);\n          document.getElementById('discount').textContent = '-' + fmt(q.discount);\n          document.getElementById('rushSurcharge').textContent = fmt(q.rushSurcharge);\n          document.getElementById('shipping').textContent = fmt(q.shipping);\n          document.getElementById('total').textContent = fmt(q.total);\n          document.getElementById('outcome').textContent = q.outcome === 'needs_review' ? 'Needs review' : 'Auto-approved';\n          document.getElementById('quote').classList.add('show');\n        });\n    });\n  </script>\n</body>\n</html>\n";
   private static final String PAGE_QUOTE_RENAMED;

   public HttpResponse products(HttpRequest var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      CATALOG.forEach(var1x -> var2.add(var1x.toMap()));
      return HttpResponse.json(Map.of("products", var2));
   }

   public HttpResponse quote(HttpRequest var1) {
      Object var2;
      try {
         var2 = Json.of(var1.getBodyString()).value();
      } catch (RuntimeException var17) {
         return HttpResponse.json(400, Map.of("error", "invalid JSON body"));
      }

      if (!(var2 instanceof Map var3)) {
         return HttpResponse.json(400, Map.of("error", "expected a JSON object {tier, rush, items}"));
      } else {
         String var4 = var3.get("tier") instanceof String var5 && !var5.isBlank() ? var5.trim() : "standard";
         boolean var18 = Boolean.TRUE.equals(var3.get("rush")) || "true".equals(String.valueOf(var3.get("rush")));
         if (var3.get("items") instanceof List var7 && !var7.isEmpty()) {
            ArrayList<io.karatelabs.ext.max.demo.DemoApp.QuoteItem> var8 = new ArrayList();

            for (Object var10 : var7) {
               if (!(var10 instanceof Map var11)) {
                  return HttpResponse.json(400, Map.of("error", "each item is an object {sku, qty}"));
               }

               String var12 = String.valueOf(var11.get("sku"));
               DemoApp.Product var13 = BY_SKU.get(var12);
               if (var13 == null) {
                  return HttpResponse.json(400, Map.of("error", "unknown sku: " + var12));
               }

               int var14 = var11.get("qty") instanceof Number var15 ? var15.intValue() : 0;
               if (var14 <= 0) {
                  return HttpResponse.json(400, Map.of("error", "qty must be a positive integer for " + var12));
               }

               var8.add(new io.karatelabs.ext.max.demo.DemoApp.QuoteItem(var13, var14));
            }

            return HttpResponse.json(price(this.config, var4, var18, var8));
         } else {
            return HttpResponse.json(400, Map.of("error", "items must be a non-empty array of {sku, qty}"));
         }
      }
   }

   public HttpResponse getConfig(HttpRequest var1) {
      return HttpResponse.json(this.config.toMap());
   }

   public HttpResponse setConfig(HttpRequest var1) {
      Object var2;
      try {
         var2 = Json.of(var1.getBodyString()).value();
      } catch (RuntimeException var6) {
         return HttpResponse.json(400, Map.of("error", "invalid JSON body"));
      }

      if (var2 instanceof Map var3) {
         var var4 = this.config.patch(var3);
         var var5 = new LinkedHashMap<>(this.config.toMap());
         var5.put("changed", var4);
         return HttpResponse.json(var5);
      } else {
         return HttpResponse.json(400, Map.of("error", "expected a JSON object of knob → number"));
      }
   }

   public HttpResponse resetConfig(HttpRequest var1) {
      this.config = new DemoApp.Config();
      return HttpResponse.json(this.config.toMap());
   }

   public Set<String> tokens() {
      return Set.copyOf(this.tokens);
   }

   public HttpResponse login(HttpRequest var1) {
      Object var2;
      try {
         var2 = Json.of(var1.getBodyString()).value();
      } catch (RuntimeException var5) {
         return HttpResponse.json(400, Map.of("error", "invalid JSON body"));
      }

      if (var2 instanceof Map var3 && "tester".equals(var3.get("username")) && "demo".equals(var3.get("password"))) {
         String var4 = UUID.randomUUID().toString();
         this.tokens.add(var4);
         this.logins.incrementAndGet();
         return HttpResponse.json(Map.of("token", var4));
      } else {
         return HttpResponse.json(401, Map.of("error", "invalid username or password"));
      }
   }

   public HttpResponse stats(HttpRequest var1) {
      return HttpResponse.json(Map.of("logins", this.logins.get(), "creates", this.creates.get()));
   }

   public HttpResponse createOrder(HttpRequest var1) {
      HttpResponse var2 = this.requireToken(var1);
      if (var2 != null) {
         return var2;
      } else {
         Object var3;
         try {
            var3 = Json.of(var1.getBodyString()).value();
         } catch (RuntimeException var16) {
            return HttpResponse.json(400, Map.of("error", "invalid JSON body"));
         }

         if (var3 instanceof Map var4 && var4.get("items") instanceof List var5 && !var5.isEmpty()) {
            ArrayList<java.util.LinkedHashMap> var17 = new ArrayList();
            BigDecimal var7 = BigDecimal.ZERO;

            for (Object var9 : var5) {
               if (!(var9 instanceof Map var10)) {
                  return HttpResponse.json(400, Map.of("error", "each item is an object {sku, qty}"));
               }

               String var11 = String.valueOf(var10.get("sku"));
               DemoApp.Product var12 = BY_SKU.get(var11);
               if (var12 == null) {
                  return HttpResponse.json(400, Map.of("error", "unknown sku: " + var11));
               }

               int var13 = var10.get("qty") instanceof Number var14 ? var14.intValue() : 0;
               if (var13 <= 0) {
                  return HttpResponse.json(400, Map.of("error", "qty must be a positive integer for " + var11));
               }

               var7 = var7.add(var12.unitPrice().multiply(BigDecimal.valueOf((long)var13)));
               LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
               var20.put("sku", var11);
               var20.put("qty", var13);
               var17.add(var20);
            }

            int var18 = this.orderSeq.incrementAndGet();
            LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
            var19.put("id", var18);
            var19.put("status", "created");
            var19.put("items", var17);
            var19.put("total", money(var7).doubleValue());
            this.orders.put(var18, var19);
            this.creates.incrementAndGet();
            return HttpResponse.json(201, var19);
         } else {
            return HttpResponse.json(400, Map.of("error", "items must be a non-empty array of {sku, qty}"));
         }
      }
   }

   public HttpResponse getOrder(HttpRequest var1) {
      HttpResponse var2 = this.requireToken(var1);
      if (var2 != null) {
         return var2;
      } else {
         String[] var3 = var1.getPath().substring("/demo/api/orders/".length()).split("/");
         Map var4 = var3.length == 1 ? this.findOrder(var3[0]) : null;
         if (var4 == null) {
            return HttpResponse.json(404, Map.of("error", "no such order: " + String.join("/", var3)));
         } else {
            synchronized (var4) {
               return HttpResponse.json(new LinkedHashMap(var4));
            }
         }
      }
   }

   public HttpResponse orderAction(HttpRequest var1) {
      HttpResponse var2 = this.requireToken(var1);
      if (var2 != null) {
         return var2;
      } else {
         String[] var3 = var1.getPath().substring("/demo/api/orders/".length()).split("/");
         if (var3.length != 2) {
            return HttpResponse.json(404, Map.of("error", "no route: POST " + var1.getPath()));
         } else {
            var var4 = this.findOrder(var3[0]);
            if (var4 == null) {
               return HttpResponse.json(404, Map.of("error", "no such order: " + var3[0]));
            } else {
               String var5 = var3[1];
               List var6;
               String var7;
               switch (var5) {
                  case "pay":
                     var6 = List.of("created");
                     var7 = "paid";
                     break;
                  case "ship":
                     var6 = List.of("paid");
                     var7 = "shipped";
                     break;
                  case "cancel":
                     var6 = List.of("created", "paid");
                     var7 = "cancelled";
                     break;
                  default:
                     return HttpResponse.json(404, Map.of("error", "unknown action: " + var5));
               }

               synchronized (var4) {
                  Object var13 = var4.get("status");
                  if (!var6.contains(var13)) {
                     return HttpResponse.json(409, Map.of("error", "cannot " + var5 + " an order that is " + var13));
                  } else {
                     LinkedHashMap<java.lang.String, java.lang.String> var10 = new LinkedHashMap(var4);
                     var10.put("status", var7);
                     if (!"pay".equals(var5) || !this.config.payNotPersisted) {
                        var4.put("status", var7);
                     }

                     return HttpResponse.json(var10);
                  }
               }
            }
         }
      }
   }

   private Map<String, Object> findOrder(String var1) {
      try {
         return this.orders.get(Integer.parseInt(var1));
      } catch (NumberFormatException var3) {
         return null;
      }
   }

   private HttpResponse requireToken(HttpRequest var1) {
      String var2 = var1.getHeader("Authorization");
      if (var2 != null && var2.startsWith("Bearer ")) {
         return this.tokens.contains(var2.substring("Bearer ".length()).trim()) ? null : HttpResponse.json(401, Map.of("error", "invalid or expired token"));
      } else {
         return HttpResponse.json(401, Map.of("error", "missing Bearer token — POST /demo/api/login first"));
      }
   }

   public HttpResponse page(HttpRequest var1) {
      return HttpResponse.html(
         this.config.quoteButtonRenamed
            ? PAGE_QUOTE_RENAMED
            : "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"utf-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title>Acme Orders — demo storefront</title>\n  <style>\n    body { font: 15px/1.5 system-ui, sans-serif; margin: 0; color: #1e293b; background: #f8fafc; }\n    header { background: #4f46e5; color: #fff; padding: 16px 24px; }\n    header h1 { margin: 0; font-size: 18px; }\n    header p { margin: 2px 0 0; font-size: 13px; opacity: .85; }\n    main { max-width: 760px; margin: 24px auto; padding: 0 16px; }\n    section { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 20px; margin-bottom: 18px; }\n    h2 { font-size: 14px; text-transform: uppercase; letter-spacing: .05em; color: #64748b; margin: 0 0 12px; }\n    table { width: 100%; border-collapse: collapse; }\n    th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }\n    td.num, th.num { text-align: right; }\n    input[type=number] { width: 56px; padding: 4px; }\n    label { margin-right: 16px; }\n    button { background: #4f46e5; color: #fff; border: 0; padding: 9px 18px; border-radius: 8px; font-size: 14px; cursor: pointer; }\n    #quote { display: none; }\n    #quote.show { display: block; }\n    .total { font-size: 20px; font-weight: 700; }\n    .row { display: flex; justify-content: space-between; padding: 3px 0; }\n    .muted { color: #64748b; }\n  </style>\n</head>\n<body>\n  <header>\n    <h1>Acme Orders</h1>\n    <p>Demo storefront — build a cart, get a quote.</p>\n  </header>\n  <main>\n    <section>\n      <h2>Catalog</h2>\n      <table>\n        <thead><tr><th>Product</th><th>Category</th><th class=\"num\">Unit price</th><th class=\"num\">Qty</th></tr></thead>\n        <tbody id=\"catalog\"></tbody>\n      </table>\n    </section>\n    <section>\n      <h2>Options</h2>\n      <label>Tier\n        <select id=\"tier\">\n          <option value=\"standard\">Standard</option>\n          <option value=\"silver\">Silver</option>\n          <option value=\"gold\">Gold</option>\n        </select>\n      </label>\n      <label><input type=\"checkbox\" id=\"rush\"> Rush delivery</label>\n      <div style=\"margin-top:14px\"><button id=\"get-quote\">Get quote</button></div>\n    </section>\n    <section id=\"quote\">\n      <h2>Quote</h2>\n      <div class=\"row\"><span>Subtotal</span><span id=\"subtotal\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Discount</span><span id=\"discount\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Rush surcharge</span><span id=\"rushSurcharge\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Shipping</span><span id=\"shipping\" class=\"num\"></span></div>\n      <div class=\"row total\"><span>Total</span><span id=\"total\" class=\"num\"></span></div>\n      <div class=\"row\"><span>Disposition</span><span id=\"outcome\"></span></div>\n    </section>\n    <section id=\"admin\">\n      <h2>Pricing config (admin) — simulate a drift from the rules</h2>\n      <p class=\"muted\" style=\"margin-top:0\">These knobs default to the spec, so the storefront agrees with the rulebook oracle.\n         Change one (e.g. drop the gold discount to <code>0.05</code>) and re-run the checks — they go <b>red</b> and the\n         rules-vs-live diff lights up. <b>Reset to spec</b> is the fix.</p>\n      <div style=\"display:flex; flex-wrap:wrap; gap:10px 18px; align-items:end\">\n        <label>Gold discount <input type=\"number\" step=\"0.01\" id=\"cfg-goldDiscount\" class=\"cfg\" data-knob=\"goldDiscount\"></label>\n        <label>Silver discount <input type=\"number\" step=\"0.01\" id=\"cfg-silverDiscount\" class=\"cfg\" data-knob=\"silverDiscount\"></label>\n        <label>Rush rate <input type=\"number\" step=\"0.01\" id=\"cfg-rushRate\" class=\"cfg\" data-knob=\"rushRate\"></label>\n        <label>Free-ship over <input type=\"number\" step=\"1\" id=\"cfg-freeShipThreshold\" class=\"cfg\" data-knob=\"freeShipThreshold\"></label>\n        <label>Shipping fee <input type=\"number\" step=\"0.01\" id=\"cfg-shippingFee\" class=\"cfg\" data-knob=\"shippingFee\"></label>\n        <label>Review over <input type=\"number\" step=\"1\" id=\"cfg-reviewThreshold\" class=\"cfg\" data-knob=\"reviewThreshold\"></label>\n      </div>\n      <div style=\"margin-top:14px; display:flex; gap:10px\">\n        <button id=\"save-config\">Apply drift</button>\n        <button id=\"reset-config\" style=\"background:#475569\">Reset to spec</button>\n        <span id=\"config-status\" class=\"muted\"></span>\n      </div>\n    </section>\n  </main>\n  <script>\n    var fmt = function (n) { return '$' + Number(n).toFixed(2); };\n    fetch('/demo/api/products').then(function (r) { return r.json(); }).then(function (data) {\n      var rows = data.products.map(function (p) {\n        return '<tr data-sku=\"' + p.sku + '\">'\n          + '<td>' + p.name + '</td><td>' + p.category + '</td>'\n          + '<td class=\"num\">' + fmt(p.unitPrice) + '</td>'\n          + '<td class=\"num\"><input type=\"number\" min=\"0\" value=\"0\" class=\"qty\" data-sku=\"' + p.sku + '\"></td></tr>';\n      });\n      document.getElementById('catalog').innerHTML = rows.join('');\n    });\n    var loadConfig = function () {\n      fetch('/demo/api/config').then(function (r) { return r.json(); }).then(function (cfg) {\n        document.querySelectorAll('.cfg').forEach(function (el) { el.value = cfg[el.getAttribute('data-knob')]; });\n      });\n    };\n    loadConfig();\n    document.getElementById('save-config').addEventListener('click', function () {\n      var patch = {};\n      document.querySelectorAll('.cfg').forEach(function (el) { patch[el.getAttribute('data-knob')] = parseFloat(el.value); });\n      fetch('/demo/api/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(patch) })\n        .then(function (r) { return r.json(); })\n        .then(function (c) { document.getElementById('config-status').textContent = 'applied — re-run a check to see the drift'; });\n    });\n    document.getElementById('reset-config').addEventListener('click', function () {\n      fetch('/demo/api/config/reset', { method: 'POST' }).then(function (r) { return r.json(); }).then(function () {\n        loadConfig();\n        document.getElementById('config-status').textContent = 'reset to spec';\n      });\n    });\n    document.getElementById('get-quote').addEventListener('click', function () {\n      var items = [];\n      document.querySelectorAll('.qty').forEach(function (el) {\n        var qty = parseInt(el.value, 10) || 0;\n        if (qty > 0) { items.push({ sku: el.getAttribute('data-sku'), qty: qty }); }\n      });\n      var payload = { tier: document.getElementById('tier').value, rush: document.getElementById('rush').checked, items: items };\n      fetch('/demo/api/quote', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })\n        .then(function (r) { return r.json(); })\n        .then(function (q) {\n          if (q.error) { alert(q.error); return; }\n          document.getElementById('subtotal').textContent = fmt(q.subtotal);\n          document.getElementById('discount').textContent = '-' + fmt(q.discount);\n          document.getElementById('rushSurcharge').textContent = fmt(q.rushSurcharge);\n          document.getElementById('shipping').textContent = fmt(q.shipping);\n          document.getElementById('total').textContent = fmt(q.total);\n          document.getElementById('outcome').textContent = q.outcome === 'needs_review' ? 'Needs review' : 'Auto-approved';\n          document.getElementById('quote').classList.add('show');\n        });\n    });\n  </script>\n</body>\n</html>\n"
      );
   }

   public static Map<String, Object> price(DemoApp.Config var0, String var1, boolean var2, List<io.karatelabs.ext.max.demo.DemoApp.QuoteItem> var3) {
      BigDecimal var4 = BigDecimal.ZERO;
      ArrayList<java.util.LinkedHashMap> var5 = new ArrayList();

      for (io.karatelabs.ext.max.demo.DemoApp.QuoteItem var7 : var3) {
         BigDecimal var8 = var7.a().unitPrice().multiply(BigDecimal.valueOf((long)var7.b()));
         var4 = var4.add(var8);
         LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
         var9.put("sku", var7.a().sku());
         var9.put("qty", var7.b());
         var9.put("lineTotal", money(var8).doubleValue());
         var5.add(var9);
      }

      var4 = money(var4);
      BigDecimal var15 = BigDecimal.valueOf(tierRate(var0, var1));
      BigDecimal var16 = money(var4.multiply(var15));
      BigDecimal var17 = var4.subtract(var16);
      BigDecimal var18 = var2 ? money(var17.multiply(BigDecimal.valueOf(var0.rushRate))) : money(0.0);
      BigDecimal var10 = var17.doubleValue() >= var0.freeShipThreshold ? money(0.0) : money(var0.shippingFee);
      BigDecimal var11 = money(var17.add(var18).add(var10));
      String var12 = var11.doubleValue() > var0.reviewThreshold ? "needs_review" : "auto_approved";
      LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
      var13.put("tier", var1);
      var13.put("rush", var2);
      var13.put("lines", var5);
      var13.put("subtotal", var4.doubleValue());
      var13.put("discount", var16.doubleValue());
      var13.put("rushSurcharge", var18.doubleValue());
      var13.put("shipping", var10.doubleValue());
      var13.put("total", var11.doubleValue());
      var13.put("outcome", var12);
      return var13;
   }

   private static double tierRate(DemoApp.Config var0, String var1) {
      String var2 = var1 == null ? "" : var1.toLowerCase();

      return switch (var2) {
         case "gold" -> var0.goldDiscount;
         case "silver" -> var0.silverDiscount;
         default -> 0.0;
      };
   }

   private static BigDecimal money(double var0) {
      return money(BigDecimal.valueOf(var0));
   }

   private static BigDecimal money(BigDecimal var0) {
      return var0.setScale(2, RoundingMode.HALF_UP);
   }

   static {
      for (DemoApp.Product var1 : CATALOG) {
         BY_SKU.put(var1.sku(), var1);
      }

      PAGE_QUOTE_RENAMED = "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"utf-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title>Acme Orders — demo storefront</title>\n  <style>\n    body { font: 15px/1.5 system-ui, sans-serif; margin: 0; color: #1e293b; background: #f8fafc; }\n    header { background: #4f46e5; color: #fff; padding: 16px 24px; }\n    header h1 { margin: 0; font-size: 18px; }\n    header p { margin: 2px 0 0; font-size: 13px; opacity: .85; }\n    main { max-width: 760px; margin: 24px auto; padding: 0 16px; }\n    section { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 20px; margin-bottom: 18px; }\n    h2 { font-size: 14px; text-transform: uppercase; letter-spacing: .05em; color: #64748b; margin: 0 0 12px; }\n    table { width: 100%; border-collapse: collapse; }\n    th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }\n    td.num, th.num { text-align: right; }\n    input[type=number] { width: 56px; padding: 4px; }\n    label { margin-right: 16px; }\n    button { background: #4f46e5; color: #fff; border: 0; padding: 9px 18px; border-radius: 8px; font-size: 14px; cursor: pointer; }\n    #quote { display: none; }\n    #quote.show { display: block; }\n    .total { font-size: 20px; font-weight: 700; }\n    .row { display: flex; justify-content: space-between; padding: 3px 0; }\n    .muted { color: #64748b; }\n  </style>\n</head>\n<body>\n  <header>\n    <h1>Acme Orders</h1>\n    <p>Demo storefront — build a cart, get a quote.</p>\n  </header>\n  <main>\n    <section>\n      <h2>Catalog</h2>\n      <table>\n        <thead><tr><th>Product</th><th>Category</th><th class=\"num\">Unit price</th><th class=\"num\">Qty</th></tr></thead>\n        <tbody id=\"catalog\"></tbody>\n      </table>\n    </section>\n    <section>\n      <h2>Options</h2>\n      <label>Tier\n        <select id=\"tier\">\n          <option value=\"standard\">Standard</option>\n          <option value=\"silver\">Silver</option>\n          <option value=\"gold\">Gold</option>\n        </select>\n      </label>\n      <label><input type=\"checkbox\" id=\"rush\"> Rush delivery</label>\n      <div style=\"margin-top:14px\"><button id=\"get-quote\">Get quote</button></div>\n    </section>\n    <section id=\"quote\">\n      <h2>Quote</h2>\n      <div class=\"row\"><span>Subtotal</span><span id=\"subtotal\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Discount</span><span id=\"discount\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Rush surcharge</span><span id=\"rushSurcharge\" class=\"num\"></span></div>\n      <div class=\"row muted\"><span>Shipping</span><span id=\"shipping\" class=\"num\"></span></div>\n      <div class=\"row total\"><span>Total</span><span id=\"total\" class=\"num\"></span></div>\n      <div class=\"row\"><span>Disposition</span><span id=\"outcome\"></span></div>\n    </section>\n    <section id=\"admin\">\n      <h2>Pricing config (admin) — simulate a drift from the rules</h2>\n      <p class=\"muted\" style=\"margin-top:0\">These knobs default to the spec, so the storefront agrees with the rulebook oracle.\n         Change one (e.g. drop the gold discount to <code>0.05</code>) and re-run the checks — they go <b>red</b> and the\n         rules-vs-live diff lights up. <b>Reset to spec</b> is the fix.</p>\n      <div style=\"display:flex; flex-wrap:wrap; gap:10px 18px; align-items:end\">\n        <label>Gold discount <input type=\"number\" step=\"0.01\" id=\"cfg-goldDiscount\" class=\"cfg\" data-knob=\"goldDiscount\"></label>\n        <label>Silver discount <input type=\"number\" step=\"0.01\" id=\"cfg-silverDiscount\" class=\"cfg\" data-knob=\"silverDiscount\"></label>\n        <label>Rush rate <input type=\"number\" step=\"0.01\" id=\"cfg-rushRate\" class=\"cfg\" data-knob=\"rushRate\"></label>\n        <label>Free-ship over <input type=\"number\" step=\"1\" id=\"cfg-freeShipThreshold\" class=\"cfg\" data-knob=\"freeShipThreshold\"></label>\n        <label>Shipping fee <input type=\"number\" step=\"0.01\" id=\"cfg-shippingFee\" class=\"cfg\" data-knob=\"shippingFee\"></label>\n        <label>Review over <input type=\"number\" step=\"1\" id=\"cfg-reviewThreshold\" class=\"cfg\" data-knob=\"reviewThreshold\"></label>\n      </div>\n      <div style=\"margin-top:14px; display:flex; gap:10px\">\n        <button id=\"save-config\">Apply drift</button>\n        <button id=\"reset-config\" style=\"background:#475569\">Reset to spec</button>\n        <span id=\"config-status\" class=\"muted\"></span>\n      </div>\n    </section>\n  </main>\n  <script>\n    var fmt = function (n) { return '$' + Number(n).toFixed(2); };\n    fetch('/demo/api/products').then(function (r) { return r.json(); }).then(function (data) {\n      var rows = data.products.map(function (p) {\n        return '<tr data-sku=\"' + p.sku + '\">'\n          + '<td>' + p.name + '</td><td>' + p.category + '</td>'\n          + '<td class=\"num\">' + fmt(p.unitPrice) + '</td>'\n          + '<td class=\"num\"><input type=\"number\" min=\"0\" value=\"0\" class=\"qty\" data-sku=\"' + p.sku + '\"></td></tr>';\n      });\n      document.getElementById('catalog').innerHTML = rows.join('');\n    });\n    var loadConfig = function () {\n      fetch('/demo/api/config').then(function (r) { return r.json(); }).then(function (cfg) {\n        document.querySelectorAll('.cfg').forEach(function (el) { el.value = cfg[el.getAttribute('data-knob')]; });\n      });\n    };\n    loadConfig();\n    document.getElementById('save-config').addEventListener('click', function () {\n      var patch = {};\n      document.querySelectorAll('.cfg').forEach(function (el) { patch[el.getAttribute('data-knob')] = parseFloat(el.value); });\n      fetch('/demo/api/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(patch) })\n        .then(function (r) { return r.json(); })\n        .then(function (c) { document.getElementById('config-status').textContent = 'applied — re-run a check to see the drift'; });\n    });\n    document.getElementById('reset-config').addEventListener('click', function () {\n      fetch('/demo/api/config/reset', { method: 'POST' }).then(function (r) { return r.json(); }).then(function () {\n        loadConfig();\n        document.getElementById('config-status').textContent = 'reset to spec';\n      });\n    });\n    document.getElementById('get-quote').addEventListener('click', function () {\n      var items = [];\n      document.querySelectorAll('.qty').forEach(function (el) {\n        var qty = parseInt(el.value, 10) || 0;\n        if (qty > 0) { items.push({ sku: el.getAttribute('data-sku'), qty: qty }); }\n      });\n      var payload = { tier: document.getElementById('tier').value, rush: document.getElementById('rush').checked, items: items };\n      fetch('/demo/api/quote', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })\n        .then(function (r) { return r.json(); })\n        .then(function (q) {\n          if (q.error) { alert(q.error); return; }\n          document.getElementById('subtotal').textContent = fmt(q.subtotal);\n          document.getElementById('discount').textContent = '-' + fmt(q.discount);\n          document.getElementById('rushSurcharge').textContent = fmt(q.rushSurcharge);\n          document.getElementById('shipping').textContent = fmt(q.shipping);\n          document.getElementById('total').textContent = fmt(q.total);\n          document.getElementById('outcome').textContent = q.outcome === 'needs_review' ? 'Needs review' : 'Auto-approved';\n          document.getElementById('quote').classList.add('show');\n        });\n    });\n  </script>\n</body>\n</html>\n"
         .replace("<button id=\"get-quote\">Get quote</button>", "<button id=\"request-quote\">Request quote</button>")
         .replace("getElementById('get-quote')", "getElementById('request-quote')");
   }

   public static final class Config {
      public double goldDiscount = 0.1;
      public double silverDiscount = 0.05;
      public double rushRate = 0.15;
      public double freeShipThreshold = 100.0;
      public double shippingFee = 9.99;
      public double reviewThreshold = 1000.0;
      public boolean payNotPersisted = false;
      public boolean quoteButtonRenamed = false;

      Map<String, Object> toMap() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("goldDiscount", this.goldDiscount);
         var1.put("silverDiscount", this.silverDiscount);
         var1.put("rushRate", this.rushRate);
         var1.put("freeShipThreshold", this.freeShipThreshold);
         var1.put("shippingFee", this.shippingFee);
         var1.put("reviewThreshold", this.reviewThreshold);
         var1.put("payNotPersisted", this.payNotPersisted);
         var1.put("quoteButtonRenamed", this.quoteButtonRenamed);
         return var1;
      }

      List<String> patch(Map<?, ?> var1) {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Entry var4 : var1.entrySet()) {
            if (var4.getValue() instanceof Boolean var5) {
               String var11 = String.valueOf(var4.getKey());
               switch (var11) {
                  case "payNotPersisted":
                     this.payNotPersisted = var5;
                     var2.add("payNotPersisted");
                     break;
                  case "quoteButtonRenamed":
                     this.quoteButtonRenamed = var5;
                     var2.add("quoteButtonRenamed");
               }
            } else if (var4.getValue() instanceof Number var10) {
               double var13 = var10.doubleValue();
               String var8 = String.valueOf(var4.getKey());
               switch (var8) {
                  case "goldDiscount":
                     this.goldDiscount = var13;
                     var2.add("goldDiscount");
                     break;
                  case "silverDiscount":
                     this.silverDiscount = var13;
                     var2.add("silverDiscount");
                     break;
                  case "rushRate":
                     this.rushRate = var13;
                     var2.add("rushRate");
                     break;
                  case "freeShipThreshold":
                     this.freeShipThreshold = var13;
                     var2.add("freeShipThreshold");
                     break;
                  case "shippingFee":
                     this.shippingFee = var13;
                     var2.add("shippingFee");
                     break;
                  case "reviewThreshold":
                     this.reviewThreshold = var13;
                     var2.add("reviewThreshold");
               }
            }
         }

         return var2;
      }
   }

   public record Product(String sku, String name, String category, BigDecimal unitPrice) {


      Map<String, Object> toMap() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("sku", this.sku);
         var1.put("name", this.name);
         var1.put("category", this.category);
         var1.put("unitPrice", this.unitPrice.doubleValue());
         return var1;
      }
   }

   record QuoteItem(DemoApp.Product a, int b) {

   }
}
