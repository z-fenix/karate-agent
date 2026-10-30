package io.karatelabs.ext.requirements;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

public final class RequirementsYamlWriter {
   private RequirementsYamlWriter() {
   }

   public static String a(io.karatelabs.ext.requirements.model.RequirementsBundle var0) {
      DumperOptions var1 = new DumperOptions();
      var1.setDefaultFlowStyle(FlowStyle.BLOCK);
      var1.setPrettyFlow(true);
      var1.setIndent(2);
      var1.setSplitLines(false);
      var1.setDefaultScalarStyle(ScalarStyle.PLAIN);
      Yaml var2 = new Yaml(var1);
      return var2.dump(var0.a());
   }
}
