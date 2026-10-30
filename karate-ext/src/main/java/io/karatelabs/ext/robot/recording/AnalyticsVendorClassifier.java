package io.karatelabs.ext.robot.recording;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class AnalyticsVendorClassifier {
   private static final Map<String, List<String>> a = new LinkedHashMap<>();

   private AnalyticsVendorClassifier() {
   }

   static String a(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for (Entry var2 : a.entrySet()) {
            for (String var4 : (Iterable<String>) (Iterable<?>) ((List)var2.getValue())) {
               if (var0.equals(var4) || var0.endsWith("." + var4)) {
                  return (String)var2.getKey();
               }
            }
         }

         return null;
      }
   }

   static {
      a.put("Google Analytics", List.of("google-analytics.com", "analytics.google.com"));
      a.put("Google Tag Manager", List.of("googletagmanager.com"));
      a.put("Google Ads", List.of("googlesyndication.com", "googleadservices.com", "doubleclick.net"));
      a.put("Meta", List.of("facebook.com", "facebook.net"));
      a.put("Hotjar", List.of("hotjar.com"));
      a.put("Segment", List.of("segment.io", "segment.com"));
      a.put("Mixpanel", List.of("mixpanel.com"));
      a.put("Amplitude", List.of("amplitude.com"));
      a.put("Heap", List.of("heap.io", "heapanalytics.com"));
      a.put("PostHog", List.of("posthog.com"));
      a.put("New Relic", List.of("newrelic.com", "nr-data.net"));
      a.put("Datadog", List.of("datadog.com", "datadoghq.com"));
      a.put("Sentry", List.of("sentry.io"));
      a.put("FullStory", List.of("fullstory.com"));
      a.put("LogRocket", List.of("logrocket.com"));
      a.put("Microsoft Clarity", List.of("clarity.ms"));
      a.put("The Trade Desk", List.of("adsrvr.org"));
      a.put("Carbon Ads", List.of("carbonads.com", "carbonads.net"));
      a.put("BuySellAds", List.of("buysellads.com", "buysellads.net"));
      a.put("Intercom", List.of("intercom.io"));
      a.put("HubSpot", List.of("hubspot.com", "hs-analytics.net", "hs-scripts.com"));
      a.put("Optimizely", List.of("optimizely.com"));
      a.put("LaunchDarkly", List.of("launchdarkly.com"));
      a.put("Split", List.of("split.io"));
      a.put("Ahrefs", List.of("ahrefs.com"));
      a.put("Semrush", List.of("semrush.com"));
      a.put("Moz", List.of("moz.com"));
   }
}
