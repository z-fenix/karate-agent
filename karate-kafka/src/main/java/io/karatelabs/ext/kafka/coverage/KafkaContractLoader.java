package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.Address;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Interaction;
import io.karatelabs.ext.contract.KafkaBinding;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.contract.SchemaRegistry;
import io.karatelabs.ext.contract.Service;
import io.karatelabs.ext.contract.TypeRef;
import io.karatelabs.ext.contract.Address.Kind;
import io.karatelabs.ext.contract.ContractLoader.LoaderOptions;
import io.karatelabs.ext.contract.KafkaBinding.Direction;
import io.karatelabs.ext.contract.Message.Role;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KafkaContractLoader implements ContractLoader {
   public static final int LATEST = -1;

   public Protocol protocol() {
      return Protocol.KAFKA;
   }

   public Contract load(List<Path> artifacts, LoaderOptions opts) {
      Object cfg = opts == null ? null : opts.config().get("kafka");
      if (cfg instanceof Map<?, ?> m && m.get("schemaRegistry") != null) {
         SchemaRegistry registry = ConfluentSchemaRegistry.of(String.valueOf(m.get("schemaRegistry")));
         return m.get("topics") instanceof List<?> list && !list.isEmpty() ? fromConfig(list, registry) : fromRegistry(registry);
      } else {
         List<?> entries = cfg instanceof List<?> l ? l : (cfg == null ? List.of() : List.of(cfg));
         return fromConfig(entries);
      }
   }

   public static Contract fromConfig(List<?> entries) {
      return fromConfig(entries, null);
   }

   public static Contract fromConfig(List<?> entries, SchemaRegistry registry) {
      List<Operation> operations = new ArrayList<>();
      LinkedHashSet<String> topics = new LinkedHashSet<>();

      for (Object raw : entries) {
         if (!(raw instanceof Map<?, ?> entry)) {
            throw new IllegalArgumentException("cov.kafka entries must be objects {topic, schema?, key?, direction?}, got: " + raw);
         }

         String topic = str(entry.get("topic"));
         if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("cov.kafka entry needs a 'topic': " + entry);
         }

         topics.add(topic);
         String schema = str(entry.get("schema"));
         String key = str(entry.get("key"));
         TypeRef valueRef;
         if (registry != null) {
            String subject = topic + "-value";
            valueRef = new TypeRef(schema != null ? schema : subject, () -> registry.resolve(subject, -1));
         } else {
            valueRef = schema == null ? null : TypeRef.nameOnly(schema);
         }

         TypeRef keyRef = key == null ? null : TypeRef.nameOnly(key);

         for (Direction dir : directions(str(entry.get("direction")))) {
            operations.add(operation(topic, schema, valueRef, key, keyRef, dir));
         }
      }

      return contract(operations, topics);
   }

   public static Contract fromRegistry(SchemaRegistry registry) {
      LinkedHashMap<String, String> topicToValueSubject = new LinkedHashMap<>();

      for (String subject : registry.subjects()) {
         if (subject.endsWith("-value")) {
            topicToValueSubject.put(subject.substring(0, subject.length() - "-value".length()), subject);
         } else if (subject.endsWith("-key")) {
            topicToValueSubject.putIfAbsent(subject.substring(0, subject.length() - "-key".length()), null);
         } else {
            topicToValueSubject.putIfAbsent(subject, subject);
         }
      }

      List<Operation> operations = new ArrayList<>();

      for (Entry<String, String> e : topicToValueSubject.entrySet()) {
         String valueSubject = e.getValue();
         TypeRef valueRef = valueSubject == null ? null : new TypeRef(valueSubject, () -> registry.resolve(valueSubject, -1));

         for (Direction dir : List.of(Direction.PUBLISH, Direction.SUBSCRIBE)) {
            operations.add(operation(e.getKey(), valueSubject, valueRef, null, null, dir));
         }
      }

      return contract(operations, topicToValueSubject.keySet());
   }

   private static Contract contract(List<Operation> operations, Collection<String> topics) {
      Map<String, Object> ext = new LinkedHashMap<>();
      ext.put("specType", "kafka");
      ext.put("topics", new ArrayList<>(topics));
      List<Service> services = List.of(new Service("kafka", operations));
      return new Contract("kafka", Protocol.KAFKA, "kafka", null, services, ext);
   }

   private static Operation operation(String topic, String schemaName, TypeRef valueRef, String keyName, TypeRef keyRef, Direction dir) {
      String direction = dir == Direction.PUBLISH ? "publish" : "subscribe";
      String id = topic + "#" + direction;
      Address address = new Address(Kind.TOPIC, topic, direction);
      Interaction interaction = dir == Direction.PUBLISH ? Interaction.PUBLISH : Interaction.SUBSCRIBE;
      List<Message> value = new ArrayList<>();
      if (valueRef != null) {
         value.add(new Message(schemaName, valueRef, null, Role.VALUE));
      }

      if (keyRef != null) {
         value.add(new Message(keyName, keyRef, null, Role.KEY));
      }

      List<Message> inputs = dir == Direction.PUBLISH ? value : List.of();
      List<Message> outputs = dir == Direction.PUBLISH ? List.of() : value;
      Map<String, Object> ext = new LinkedHashMap<>();
      ext.put("binding", KafkaBinding.of(topic, dir, schemaName, keyName));
      ext.put("schema", schemaName);
      ext.put("key", keyName);
      return new Operation(id, address, interaction, inputs, outputs, List.of(), null, List.of(topic), false, ext);
   }

   private static List<Direction> directions(String direction) {
      if (direction != null && !direction.isBlank() && !"both".equalsIgnoreCase(direction)) {
         String var1 = direction.toLowerCase();

         return switch (var1) {
            case "publish", "produce", "send" -> List.of(Direction.PUBLISH);
            case "subscribe", "consume", "receive" -> List.of(Direction.SUBSCRIBE);
            default -> throw new IllegalArgumentException("cov.kafka direction must be publish|subscribe|both, got: " + direction);
         };
      } else {
         return List.of(Direction.PUBLISH, Direction.SUBSCRIBE);
      }
   }

   private static String str(Object o) {
      return o == null ? null : String.valueOf(o);
   }
}
