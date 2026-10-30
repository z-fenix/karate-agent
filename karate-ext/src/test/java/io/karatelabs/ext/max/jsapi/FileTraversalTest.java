package io.karatelabs.ext.max.jsapi;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class FileTraversalTest {
   @Rule public TemporaryFolder temp = new TemporaryFolder();

   @Test public void enumeratesNestedFilesAndPrunesExcludedDirectories() throws Exception {
      var root = temp.getRoot().toPath();
      Files.createDirectories(root.resolve("requirements/nested"));
      Files.createDirectories(root.resolve("target/generated"));
      Files.writeString(root.resolve("requirements/main.md"), "# requirement");
      Files.writeString(root.resolve("requirements/nested/other.md"), "# requirement");
      Files.writeString(root.resolve("target/generated/ignored.md"), "generated");
      List<String> files = new ArrayList<>();
      File.walkPruned(root, List.of("target"), files::add);
      assertEquals(2, files.size());
      assertTrue(files.contains("requirements/main.md"));
      assertTrue(files.contains("requirements/nested/other.md"));
   }
}
