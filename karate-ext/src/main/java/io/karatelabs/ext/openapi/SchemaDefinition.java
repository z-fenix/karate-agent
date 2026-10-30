package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaDefinition {
   static final Logger logger = LoggerFactory.getLogger(SchemaDefinition.class);
   public final OpenApiDocument document;
   public final String reference;
   public final String jsonPointer;
   public final String schemaName;
   public final Map<String, Object> rawSchema;
   private OpenApiSchemaNode rootNode;
   private List<NamedValue> exampleValues;
   private List<Object> enumValues;

   public SchemaDefinition(OpenApiDocument var1, Map<String, Object> var2) {
      this(var1, null, null, null, var2);
   }

   public SchemaDefinition(OpenApiDocument var1, String var2, String var3, String var4, Map<String, Object> var5) {
      this.document = var1;
      this.reference = var2;
      this.jsonPointer = var3;
      this.schemaName = var4;
      this.rawSchema = var5;
   }

   public static Map<String, Object> normalizeSchemaDescriptor(OpenApiDocument var0, Map<String, Object> var1) {
      if (var1.containsKey("$ref")) {
         SchemaDefinition var2 = var0.resolveSchemaReference(null, var1);
         if (var2 != null) {
            var1 = var2.rawSchema;
         }
      }

      Map var4 = (Map)var1.get("schema");
      if (var4 != null) {
         HashMap<java.lang.String, java.lang.Object> var6 = new HashMap(var4);
         if (var1.containsKey("summary")) {
            var6.put("summary", var1.get("summary"));
         }

         if (var1.containsKey("description")) {
            var6.put("description", var1.get("description"));
         }

         if (var1.containsKey("value")) {
            var6.put("value", var1.get("value"));
         }

         if (var1.containsKey("example")) {
            var6.put("example", var1.get("example"));
         }

         if (var1.containsKey("examples")) {
            var6.put("examples", var1.get("examples"));
         }

         return var6;
      } else {
         String var3 = (String)var1.get("type");
         if (var3 == null) {
            if (!var1.containsKey("value")) {
               var1 = Collections.singletonMap("value", var1);
            }

            return var1;
         } else {
            HashMap<java.lang.String, java.lang.Object> var5 = new HashMap();
            var5.put("type", var3);
            if (var1.containsKey("format")) {
               var5.put("format", var1.get("format"));
            }

            if (var1.containsKey("minimum")) {
               var5.put("minimum", var1.get("minimum"));
            }

            if (var1.containsKey("pattern")) {
               var5.put("pattern", var1.get("pattern"));
            }

            if (var1.containsKey("collectionFormat")) {
               var5.put("collectionFormat", var1.get("collectionFormat"));
            }

            if (var1.containsKey("summary")) {
               var5.put("summary", var1.get("summary"));
            }

            if (var1.containsKey("description")) {
               var5.put("description", var1.get("description"));
            }

            if (var1.containsKey("value")) {
               var5.put("value", var1.get("value"));
            }

            if (var1.containsKey("example")) {
               var5.put("example", var1.get("example"));
            }

            if (var1.containsKey("examples")) {
               var5.put("examples", var1.get("examples"));
            }

            return var5;
         }
      }
   }

   public OpenApiSchemaNode getRootNode() {
      this.ensureSchemaTree();
      return this.rootNode;
   }

   private void ensureSchemaTree() {
      if (this.rootNode == null) {
         this.rootNode = new OpenApiSchemaNode(null, 0, this);
         this.populateSchemaTree(this.rootNode);
      }
   }

   private boolean applyCompositionKeyword(OpenApiSchemaNode var1, String var2) {
      if (!this.rawSchema.containsKey(var2)) {
         return false;
      } else {
         var1.setType(var2);
         Object var3 = this.rawSchema.get(var2);
         if (var3 instanceof List) {
            for (Map var6 : (Iterable<Map>) (Iterable<?>) ((List)var3)) {
               SchemaDefinition var7 = this.createChildDefinition(var1, var6);
               if (var7 != null) {
                  var7.populateSchemaTree(var1.addChild(var7));
               }
            }
         }

         return true;
      }
   }

   public String description() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return (String)var1.schemaDefinition.rawSchema.get("description");
   }

   public String summary() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return (String)var1.schemaDefinition.rawSchema.get("summary");
   }

   public String format() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return (String)var1.schemaDefinition.rawSchema.get("format");
   }

   public String pattern() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return (String)var1.schemaDefinition.rawSchema.get("pattern");
   }

   public Object defaultValue() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return var1.schemaDefinition.rawSchema.get("default");
   }

   public Object value() {
      OpenApiSchemaNode var1 = this.getRootNode();
      return var1.schemaDefinition.rawSchema.get("value");
   }

   private static boolean asBoolean(Object var0) {
      if (var0 instanceof Boolean) {
         return (Boolean)var0;
      } else {
         return var0 instanceof String var1 ? "true".equals(var1) : false;
      }
   }

   private static Integer asInteger(Object var0) {
      if (var0 instanceof Number var4) {
         return var4.intValue();
      } else if (var0 instanceof String var1) {
         try {
            return Integer.parseInt(var1);
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Number asNumber(Object var0) {
      if (var0 instanceof Number var4) {
         return var4;
      } else if (var0 instanceof String var1) {
         try {
            return Double.parseDouble(var1);
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean nullable() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("nullable");
      return asBoolean(var2);
   }

   public boolean readOnly() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("readOnly");
      return asBoolean(var2);
   }

   public boolean writeOnly() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("writeOnly");
      return asBoolean(var2);
   }

   public boolean deprecated() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("deprecated");
      return asBoolean(var2);
   }

   public Integer minItems() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("minItems");
      return asInteger(var2);
   }

   public Integer maxItems() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("maxItems");
      return asInteger(var2);
   }

   public Integer minLength() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("minLength");
      return asInteger(var2);
   }

   public Integer maxLength() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("maxLength");
      return asInteger(var2);
   }

   public Number minimum() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("minimum");
      return asNumber(var2);
   }

   public Number maximum() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("maximum");
      return asNumber(var2);
   }

   public boolean exclusiveMinimum() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("exclusiveMinimum");
      return asBoolean(var2);
   }

   public boolean exclusiveMaximum() {
      OpenApiSchemaNode var1 = this.getRootNode();
      Object var2 = var1.schemaDefinition.rawSchema.get("exclusiveMaximum");
      return asBoolean(var2);
   }

   public List<Object> enumValues() {
      if (this.enumValues == null) {
         OpenApiSchemaNode var1 = this.getRootNode();
         if (var1.schemaDefinition.rawSchema.containsKey("enum")) {
            Object var2 = var1.schemaDefinition.rawSchema.get("enum");
            if (var2 instanceof List) {
               this.enumValues = (List<Object>)var2;
            }
         } else {
            this.enumValues = Collections.emptyList();
         }
      }

      return this.enumValues;
   }

   public List<NamedValue> exampleValues() {
      if (this.exampleValues == null) {
         this.exampleValues = new ArrayList<>();
         OpenApiSchemaNode var1 = this.getRootNode();
         Object var2 = this.value();
         if (var2 != null) {
            this.exampleValues.add(new NamedValue(null, var2));
         }

         if (var1.schemaDefinition.rawSchema.containsKey("example")) {
            Object var3 = var1.schemaDefinition.rawSchema.get("example");
            this.exampleValues.add(new NamedValue(null, var3));
         }

         if (var1.schemaDefinition.rawSchema.containsKey("examples")) {
            Object var7 = var1.schemaDefinition.rawSchema.get("examples");
            if (var7 instanceof List) {
               for (Object var13 : (List)var7) {
                  this.exampleValues.add(new NamedValue(null, var13));
               }
            } else if (var7 instanceof Map var4) {
               for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
                  this.exampleValues.add(new NamedValue((String)var6.getKey(), var6.getValue()));
               }
            } else {
               this.exampleValues.add(new NamedValue(null, var7));
            }
         }

         for (Object var12 : this.enumValues()) {
            this.exampleValues.add(new NamedValue(var12 + "", var12));
         }
      }

      return this.exampleValues;
   }

   private void populateSchemaTree(OpenApiSchemaNode var1) {
      String var2 = primarySchemaType(this.rawSchema.get("type"));
      if (var2 == null) {
         if (this.applyCompositionKeyword(var1, "oneOf")) {
            return;
         }

         if (this.applyCompositionKeyword(var1, "anyOf")) {
            return;
         }

         if (this.applyCompositionKeyword(var1, "allOf")) {
            return;
         }

         if (!this.rawSchema.containsKey("properties") && !this.rawSchema.containsKey("additionalProperties")) {
            if (!this.rawSchema.containsKey("items")) {
               return;
            }

            var2 = "array";
         } else {
            var2 = "object";
         }
      }

      var1.setType(var2);
      switch (var2) {
         case "object":
            Map var5 = (Map)this.rawSchema.get("properties");
            if (var5 == null) {
               return;
            }

            for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
               Object var15 = var14.getValue();
               if (var15 instanceof Map var9) {
                  SchemaDefinition var10 = this.createChildDefinition(var1, var9);
                  if (var10 != null) {
                     OpenApiSchemaNode var11 = var1.addChild(var10);
                     var11.setName((String)var14.getKey());
                     var10.populateSchemaTree(var11);
                  }
               } else {
                  logger.warn("*** no value info: {} - {}", var15, var1);
               }
            }
            break;
         case "array":
            Object var6 = this.rawSchema.get("items");
            if (var6 instanceof List var7 && !var7.isEmpty() && var7.getFirst() instanceof Map) {
               var6 = var7.getFirst();
            }

            if (var6 instanceof Map var13) {
               SchemaDefinition var8 = this.createChildDefinition(var1, var13);
               if (var8 != null) {
                  var8.populateSchemaTree(var1.addChild(var8));
               }
            }
         case "string":
         case "integer":
         case "number":
         case "boolean":
         case "null":
            break;
         default:
            logger.warn("schema type missing in spec: {} - {}", this.document.resource, this.rawSchema);
      }
   }

   private static String primarySchemaType(Object var0) {
      if (var0 instanceof String var4) {
         return var4;
      } else if (var0 instanceof List var1 && !var1.isEmpty()) {
         for (Object var3 : var1) {
            if (!"null".equals(var3)) {
               return (String)var3;
            }
         }

         return "null";
      } else {
         return null;
      }
   }

   private SchemaDefinition createChildDefinition(OpenApiSchemaNode var1, Map<String, Object> var2) {
      return this.document == null ? new SchemaDefinition(null, var2) : this.document.resolveSchemaReference(var1, var2);
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append("[");
      if (this.schemaName != null) {
         var1.append(this.schemaName).append(", ");
      }

      if (this.reference != null) {
         var1.append(this.reference).append(", ");
      }

      var1.append(this.rawSchema).append("]");
      return var1.toString();
   }
}
