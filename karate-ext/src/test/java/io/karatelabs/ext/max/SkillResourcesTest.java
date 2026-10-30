package io.karatelabs.ext.max;

import io.karatelabs.ext.max.jsapi.Skill;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkillResourcesTest {
   @Test
   public void catalogAndAllListedSkillResourcesAreAvailable() {
      Skill skill = new Skill();
      List<Map<String, Object>> catalog = skill.list();
      assertFalse("skill catalog must not be empty", catalog.isEmpty());
      assertTrue("rule-authoring must be listed", catalog.stream().anyMatch(item -> "rule-authoring".equals(item.get("name"))));
      for (Map<String, Object> item : catalog) {
         String name = (String) item.get("name");
         assertTrue(name, skill.help(name, null).get("markdown") instanceof String);
         Object topics = item.get("topics");
         if (topics instanceof List<?>) {
            for (Object topic : (List<?>) topics) {
               assertTrue(name + "/" + topic, skill.help(name, topic.toString()).get("markdown") instanceof String);
            }
         }
      }
   }
}
