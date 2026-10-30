package io.karatelabs.core;

import java.util.Map;

public interface RunEvent {
   RunEventType getType();

   long getTimeStamp();

   Map<String, Object> toJson();
}
