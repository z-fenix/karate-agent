package io.karatelabs.ext.openapi;

import io.karatelabs.ext.contract.TypeModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiSchemaNode implements io.karatelabs.ext.common.MapConvertible, TypeModel {
   static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaNode.class);
   public final int depth;
   public final OpenApiSchemaNode parent;
   public final SchemaDefinition schemaDefinition;
   private String name;
   private String type;
   public final List<OpenApiSchemaNode> children = new ArrayList<>();
   private Object sampleOverride;
   private boolean samplePresent;

   public boolean hasValue() {
      return this.samplePresent;
   }

   public void markValuePresent() {
      this.samplePresent = true;
   }

   public void markValueAbsent() {
      this.samplePresent = false;
   }

   public void markTreePresent() {
      this.samplePresent = true;

      for (OpenApiSchemaNode var2 : this.children) {
         var2.markTreePresent();
      }
   }

   public void markTreeAbsent() {
      this.samplePresent = false;

      for (OpenApiSchemaNode var2 : this.children) {
         var2.markTreeAbsent();
      }
   }

   public void markRequiredNodesPresent() {
      if (this.isRequired()) {
         this.samplePresent = true;
      }

      for (OpenApiSchemaNode var2 : this.children) {
         var2.markRequiredNodesPresent();
      }
   }

   public boolean hasPresentValueInTree() {
      for (OpenApiSchemaNode var2 : this.children) {
         if (var2.hasPresentValueInTree()) {
            return true;
         }
      }

      return this.samplePresent;
   }

   public boolean hasMixedChildPresence() {
      if (this.children.isEmpty()) {
         return false;
      } else {
         boolean var1 = false;
         boolean var2 = false;

         for (OpenApiSchemaNode var4 : this.children) {
            boolean var5 = var4.hasPresentValueInTree();
            if (var5) {
               var1 = true;
            } else {
               var2 = true;
            }

            if (var1 && var2) {
               return true;
            }
         }

         return false;
      }
   }

   OpenApiSchemaNode(OpenApiSchemaNode var1, int var2, SchemaDefinition var3) {
      this.parent = var1;
      this.depth = var2;
      this.schemaDefinition = var3;
      if (var1 != null) {
         var1.children.add(this);
      }
   }

   public boolean isScalarType() {
      if (this.type == null) {
         return false;
      } else {
         String var1 = this.type;
         switch (var1) {
            case "string":
            case "integer":
            case "number":
            case "boolean":
               return true;
            default:
               return false;
         }
      }
   }

   public boolean isOneOfMember() {
      return this.parent == null ? false : "oneOf".equals(this.parent.type);
   }

   public boolean isRequired() {
      if (this.parent == null) {
         return false;
      } else if ("object".equals(this.parent.type)) {
         if (this.parent.parent != null && "allOf".equals(this.parent.parent.type)) {
            return true;
         } else {
            List var1 = (List)this.parent.schemaDefinition.rawSchema.get("required");
            return var1 != null ? var1.contains(this.name) : false;
         }
      } else {
         return "allOf".equals(this.parent.type);
      }
   }

   List<String> referencePath() {
      OpenApiSchemaNode var1 = this;
      ArrayList<java.lang.String> var2 = new ArrayList();

      do {
         var2.add(var1.schemaDefinition.reference);
         var1 = var1.parent;
      } while (var1 != null);

      return var2.reversed();
   }

   boolean hasReferenceAncestor(String var1) {
      OpenApiSchemaNode var2 = this;

      while (!var1.equals(var2.schemaDefinition.reference)) {
         var2 = var2.parent;
         if (var2 == null) {
            return false;
         }
      }

      return true;
   }

   OpenApiSchemaNode addChild(SchemaDefinition var1) {
      return new OpenApiSchemaNode(this, this.depth + 1, var1);
   }

   public String name() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String type() {
      return this.type;
   }

   public void setType(String var1) {
      this.type = var1;
   }

   public void setSampleOverride(Object var1) {
      this.sampleOverride = var1;
   }

   public Object generateSample() {
      if (this.sampleOverride != null) {
         return this.sampleOverride;
      } else {
         var var1 = this.schemaDefinition.exampleValues();
         if (!var1.isEmpty()) {
            return ((NamedValue)var1.getFirst()).b;
         } else if (this.type == null) {
            return null;
         } else {
            String var2 = this.type;
            switch (var2) {
               case "oneOf":
               case "allOf":
               case "anyOf":
                  LinkedHashMap var4 = new LinkedHashMap();

                  for (OpenApiSchemaNode var10 : this.children) {
                     if (var10.hasPresentValueInTree()) {
                        Object var12 = var10.generateSample();
                        if (!(var12 instanceof Map)) {
                           if (this.children.size() != 1) {
                              logger.warn("{} children sample not object: {}", this.type, this.children);
                           }

                           return var12;
                        }

                        var4.putAll((Map)var12);
                     }
                  }

                  return var4;
               case "object":
                  LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();

                  for (OpenApiSchemaNode var11 : this.children) {
                     if (var11.hasPresentValueInTree()) {
                        var5.put(var11.name(), var11.generateSample());
                     }
                  }

                  return var5;
               case "array":
                  ArrayList<java.lang.Object> var6 = new ArrayList();
                  if (this.children.isEmpty()) {
                     logger.warn("array schema with no items: {}", this);
                     return var6;
                  }

                  OpenApiSchemaNode var7 = this.children.getFirst();
                  if (var7.hasPresentValueInTree()) {
                     var6.add(var7.generateSample());
                  }

                  if (this.children.size() > 1) {
                     logger.warn("array schema with more than one item: {}", this);
                  }

                  return var6;
               case "string":
                  return "";
               case "integer":
               case "number":
                  return 0;
               case "boolean":
                  return false;
               default:
                  return null;
            }
         }
      }
   }

   @Override
   public Object sample() {
      this.markTreePresent();
      return this.generateSample();
   }

   @Override
   public String kind() {
      return "json-schema";
   }

   public io.karatelabs.ext.jsonschema.validation.ValidationResult validate(Object var1) {
      return this.validateWithDirection(var1, io.karatelabs.ext.jsonschema.validation.ValidationDirection.NONE);
   }

   public io.karatelabs.ext.jsonschema.validation.ValidationResult validateWithDirection(Object var1, io.karatelabs.ext.jsonschema.validation.ValidationDirection var2) {
      OpenApiDocument var3 = this.schemaDefinition.document;
      return new io.karatelabs.ext.jsonschema.validation.SchemaRegistry(var3.schemaDialect()).withExternalResolver(var3::loadExternalReference).register("", var3.rawDocument).compile(this.schemaDefinition.rawSchema).withDirection(var2).validate(var1);
   }

   @Override
   public Map<String, Object> partitions(String var1) {
      return io.karatelabs.ext.openapi.SchemaPartitionGenerator.a(this, var1);
   }

   @Override
   public Map<String, Object> toMap() {
      var var1 = new HashMap<>(this.schemaDefinition.rawSchema);
      if (this.type == null) {
         return var1;
      } else {
         String var2 = this.type;
         switch (var2) {
            case "oneOf":
            case "allOf":
            case "anyOf":
               var1.put(this.type, this.childSchemaMaps());
               break;
            case "object":
               HashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var4 = new HashMap(this.children.size());
               var1.put("properties", var4);

               for (OpenApiSchemaNode var6 : this.children) {
                  String var7 = var6.name();
                  if (var7 == null) {
                     logger.warn("null name for child: {}", this);
                  } else {
                     var4.put(var6.name(), var6.toMap());
                  }
               }
               break;
            case "array":
               var var5 = this.childSchemaMaps();
               if (var5.size() == 1) {
                  var1.put("items", var5.getFirst());
               } else if (var5.isEmpty()) {
                  var1.remove("items");
               } else {
                  var1.put("items", var5);
               }
         }

         return var1;
      }
   }

   private List<Map<String, Object>> childSchemaMaps() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList(this.children.size());

      for (OpenApiSchemaNode var3 : this.children) {
         var1.add(var3.toMap());
      }

      return var1;
   }

   @Override
   public String toString() {
      return this.name != null ? this.name + ": " + this.schemaDefinition : this.schemaDefinition.toString();
   }
}
