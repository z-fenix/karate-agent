package io.karatelabs.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

public class HtmlReportWriterTest {

   @Test
   public void featureFileNameKeepsSanitizedPathCollisionsApart() {
      String first = HtmlReportWriter.featureFileName(Map.of(
         "relativePath", "requirements/case/总控台与跨端协同/wave116-local-case.feature"));
      String second = HtmlReportWriter.featureFileName(Map.of(
         "relativePath", "requirements/case/工程初始化与关闭/wave116-local-case.feature"));

      assertNotEquals(first, second);
      assertTrue(first.startsWith("requirements.case.________.wave116-local-case-"));
      assertTrue(second.startsWith("requirements.case.________.wave116-local-case-"));
      assertTrue(first.matches(".+-[0-9a-f]{10}"));
      assertTrue(second.matches(".+-[0-9a-f]{10}"));
   }

   @Test
   public void featureFileNameIsStableForTheSamePath() {
      Map<String, Object> feature = Map.of(
         "relativePath", "requirements/case/账号与邀请/profile.feature");

      assertEquals(HtmlReportWriter.featureFileName(feature), HtmlReportWriter.featureFileName(feature));
   }
}
