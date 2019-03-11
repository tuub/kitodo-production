package org.kitodo.production.plugin.step.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class FileinfoExporterTest {

    private static final String COPY_FILENAME = "fileInfoExporter-dummy.txt";

    FileinfoExporter fileinfoExporter;
    FileInfo fileInfo;
    File destDir;

    @Before
    public void init() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File copyFile = new File(classLoader.getResource(COPY_FILENAME).getFile());
        destDir = Files.createTempDirectory("fileinfoExporter").toFile();
        fileInfo = new FileInfo(copyFile);
        fileinfoExporter = new FileinfoExporter(copyFile.getParentFile(), destDir);
    }

    @After
    public void cleanUp() {
        if (destDir != null) {
            try {
                FileUtils.deleteDirectory(destDir);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Test
    public void copyAndCalculate() throws IOException {

        fileinfoExporter.accept(fileInfo);

        File destFile = new File(destDir, COPY_FILENAME);
        assertTrue(destFile.exists());

        String content = new String(Files.readAllBytes(destFile.toPath()));
        assertEquals("abc\n", content);

        assertEquals("edeaaff3f1774ad2888673770c6d64097e391bc362d7d6fb34982ddf0efd18cb", StringUtils.lowerCase(fileInfo.checksum));
        assertEquals(FileinfoExporter.HashType.SHA256.getName(), fileInfo.checksumType);
        assertEquals(4L, fileInfo.size);
    }

}
