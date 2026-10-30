package io.karatelabs.ext.grpc;

import com.squareup.wire.schema.Field.Label;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

final class GrpcDiff {
   private GrpcDiff() {
   }

   static List<io.karatelabs.ext.grpc.GrpcDiff.Change> diff(ProtoFileElement oldFile, ProtoFileElement newFile) {
      List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes = new ArrayList<>();
      diffServices(oldFile, newFile, changes);
      diffMessages(oldFile, newFile, changes);
      diffEnums(oldFile, newFile, changes);
      return changes;
   }

   static Map<String, Object> report(List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      List<Map<String, Object>> breaking = new ArrayList<>();
      List<Map<String, Object>> warnings = new ArrayList<>();
      List<Map<String, Object>> compatible = new ArrayList<>();

      for (io.karatelabs.ext.grpc.GrpcDiff.Change c : changes) {
         switch (c.level()) {
            case BREAKING:
               breaking.add(c.toMap());
               break;
            case WARNING:
               warnings.add(c.toMap());
               break;
            case COMPATIBLE:
               compatible.add(c.toMap());
         }
      }

      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("breaking", breaking.size());
      summary.put("warnings", warnings.size());
      summary.put("compatible", compatible.size());
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("ok", breaking.isEmpty());
      out.put("summary", summary);
      out.put("breaking", breaking);
      out.put("warnings", warnings);
      out.put("compatible", compatible);
      return out;
   }

   private static void diffServices(ProtoFileElement oldFile, ProtoFileElement newFile, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<String, ServiceElement> oldSvcs = byName(oldFile.getServices(), ServiceElement::getName);
      Map<String, ServiceElement> newSvcs = byName(newFile.getServices(), ServiceElement::getName);

      for (String name : oldSvcs.keySet()) {
         if (!newSvcs.containsKey(name)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("SERVICE_REMOVED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, name, "service removed"));
         }
      }

      for (String namex : newSvcs.keySet()) {
         if (!oldSvcs.containsKey(namex)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("SERVICE_ADDED", io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE, namex, "service added"));
         }
      }

      for (String namexx : oldSvcs.keySet()) {
         if (newSvcs.containsKey(namexx)) {
            diffMethods(namexx, oldSvcs.get(namexx), newSvcs.get(namexx), changes);
         }
      }
   }

   private static void diffMethods(String service, ServiceElement oldSvc, ServiceElement newSvc, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<String, RpcElement> oldRpcs = byName(oldSvc.getRpcs(), RpcElement::getName);
      Map<String, RpcElement> newRpcs = byName(newSvc.getRpcs(), RpcElement::getName);

      for (String m : oldRpcs.keySet()) {
         if (!newRpcs.containsKey(m)) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change("METHOD_REMOVED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, service + "/" + m, "rpc removed")
            );
         }
      }

      for (String mx : newRpcs.keySet()) {
         if (!oldRpcs.containsKey(mx)) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change("METHOD_ADDED", io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE, service + "/" + mx, "rpc added")
            );
         }
      }

      for (String mxx : oldRpcs.keySet()) {
         RpcElement a = oldRpcs.get(mxx);
         RpcElement b = newRpcs.get(mxx);
         if (b != null) {
            String path = service + "/" + mxx;
            if (!a.getRequestType().equals(b.getRequestType())) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "METHOD_INPUT_CHANGED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, path, a.getRequestType() + " -> " + b.getRequestType()
                  )
               );
            }

            if (!a.getResponseType().equals(b.getResponseType())) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "METHOD_OUTPUT_CHANGED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, path, a.getResponseType() + " -> " + b.getResponseType()
                  )
               );
            }

            if (a.getRequestStreaming() != b.getRequestStreaming() || a.getResponseStreaming() != b.getResponseStreaming()) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "METHOD_STREAMING_CHANGED",
                     io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING,
                     path,
                     io.karatelabs.ext.grpc.ProtobufUtils.kindOf(a.getRequestStreaming(), a.getResponseStreaming())
                        + " -> "
                        + io.karatelabs.ext.grpc.ProtobufUtils.kindOf(b.getRequestStreaming(), b.getResponseStreaming())
                  )
               );
            }
         }
      }
   }

   private static void diffMessages(ProtoFileElement oldFile, ProtoFileElement newFile, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<String, MessageElement> oldMsgs = messages(oldFile);
      Map<String, MessageElement> newMsgs = messages(newFile);

      for (String name : oldMsgs.keySet()) {
         if (!newMsgs.containsKey(name)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("MESSAGE_REMOVED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, name, "message removed"));
         }
      }

      for (String namex : newMsgs.keySet()) {
         if (!oldMsgs.containsKey(namex)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("MESSAGE_ADDED", io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE, namex, "message added"));
         }
      }

      for (String namexx : oldMsgs.keySet()) {
         if (newMsgs.containsKey(namexx)) {
            diffFields(namexx, oldMsgs.get(namexx), newMsgs.get(namexx), changes);
         }
      }
   }

   private static void diffFields(String message, MessageElement oldMsg, MessageElement newMsg, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<Integer, FieldElement> oldByTag = byTag(oldMsg.getFields(), FieldElement::getTag);
      Map<Integer, FieldElement> newByTag = byTag(newMsg.getFields(), FieldElement::getTag);

      for (Integer tag : oldByTag.keySet()) {
         FieldElement a = oldByTag.get(tag);
         FieldElement b = newByTag.get(tag);
         if (b == null) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change(
                  "FIELD_REMOVED",
                  io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING,
                  message + "." + a.getName(),
                  "field " + tag + " removed (its number must be reserved)"
               )
            );
         } else {
            String path = message + "." + a.getName();
            if (!a.getName().equals(b.getName())) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "FIELD_RENAMED",
                     io.karatelabs.ext.grpc.GrpcDiff.Level.WARNING,
                     message + "." + tag,
                     a.getName() + " -> " + b.getName() + " (wire-compatible, breaks JSON/codegen)"
                  )
               );
            }

            if (!a.getType().equals(b.getType())) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "FIELD_TYPE_CHANGED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, path, a.getType() + " -> " + b.getType()
                  )
               );
            }

            if (cardinality(a) != cardinality(b)) {
               changes.add(
                  new io.karatelabs.ext.grpc.GrpcDiff.Change(
                     "FIELD_CARDINALITY_CHANGED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, path, cardinality(a) + " -> " + cardinality(b)
                  )
               );
            }
         }
      }

      for (Integer tagx : newByTag.keySet()) {
         if (!oldByTag.containsKey(tagx)) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change(
                  "FIELD_ADDED", io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE, message + "." + newByTag.get(tagx).getName(), "field " + tagx + " added"
               )
            );
         }
      }
   }

   private static void diffEnums(ProtoFileElement oldFile, ProtoFileElement newFile, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<String, EnumElement> oldEnums = enums(oldFile);
      Map<String, EnumElement> newEnums = enums(newFile);

      for (String name : oldEnums.keySet()) {
         if (!newEnums.containsKey(name)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("ENUM_REMOVED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, name, "enum removed"));
         }
      }

      for (String namex : newEnums.keySet()) {
         if (!oldEnums.containsKey(namex)) {
            changes.add(new io.karatelabs.ext.grpc.GrpcDiff.Change("ENUM_ADDED", io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE, namex, "enum added"));
         }
      }

      for (String namexx : oldEnums.keySet()) {
         if (newEnums.containsKey(namexx)) {
            diffEnumValues(namexx, oldEnums.get(namexx), newEnums.get(namexx), changes);
         }
      }
   }

   private static void diffEnumValues(String enumName, EnumElement oldEnum, EnumElement newEnum, List<io.karatelabs.ext.grpc.GrpcDiff.Change> changes) {
      Map<Integer, EnumConstantElement> oldByTag = byTag(oldEnum.getConstants(), EnumConstantElement::getTag);
      Map<Integer, EnumConstantElement> newByTag = byTag(newEnum.getConstants(), EnumConstantElement::getTag);

      for (Integer tag : oldByTag.keySet()) {
         EnumConstantElement a = oldByTag.get(tag);
         EnumConstantElement b = newByTag.get(tag);
         if (b == null) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change(
                  "ENUM_VALUE_REMOVED", io.karatelabs.ext.grpc.GrpcDiff.Level.BREAKING, enumName + "." + a.getName(), "value " + tag + " removed"
               )
            );
         } else if (!a.getName().equals(b.getName())) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change(
                  "ENUM_VALUE_RENAMED", io.karatelabs.ext.grpc.GrpcDiff.Level.WARNING, enumName + "." + tag, a.getName() + " -> " + b.getName()
               )
            );
         }
      }

      for (Integer tagx : newByTag.keySet()) {
         if (!oldByTag.containsKey(tagx)) {
            changes.add(
               new io.karatelabs.ext.grpc.GrpcDiff.Change(
                  "ENUM_VALUE_ADDED",
                  io.karatelabs.ext.grpc.GrpcDiff.Level.COMPATIBLE,
                  enumName + "." + newByTag.get(tagx).getName(),
                  "value " + tagx + " added"
               )
            );
         }
      }
   }

   private static String cardinality(FieldElement f) {
      return f.getLabel() == Label.REPEATED ? "repeated" : "singular";
   }

   private static Map<String, MessageElement> messages(ProtoFileElement file) {
      Map<String, MessageElement> out = new LinkedHashMap<>();

      for (TypeElement t : file.getTypes()) {
         if (t instanceof MessageElement m) {
            out.put(m.getName(), m);
         }
      }

      return out;
   }

   private static Map<String, EnumElement> enums(ProtoFileElement file) {
      Map<String, EnumElement> out = new LinkedHashMap<>();

      for (TypeElement t : file.getTypes()) {
         if (t instanceof EnumElement e) {
            out.put(e.getName(), e);
         }
      }

      return out;
   }

   private static <T> Map<String, T> byName(List<T> items, Function<T, String> name) {
      Map<String, T> out = new LinkedHashMap<>();

      for (T t : items) {
         out.put(name.apply(t), t);
      }

      return out;
   }

   private static <T> Map<Integer, T> byTag(List<T> items, ToIntFunction<T> tag) {
      Map<Integer, T> out = new LinkedHashMap<>();

      for (T t : items) {
         out.put(tag.applyAsInt(t), t);
      }

      return out;
   }

   record Change(String kind, io.karatelabs.ext.grpc.GrpcDiff.Level level, String path, String detail) {
      Map<String, Object> toMap() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("kind", this.kind);
         m.put("level", this.level.name());
         m.put("path", this.path);
         m.put("detail", this.detail);
         return m;
      }
   }

   static enum Level {
      BREAKING,
      WARNING,
      COMPATIBLE;
   }
}
