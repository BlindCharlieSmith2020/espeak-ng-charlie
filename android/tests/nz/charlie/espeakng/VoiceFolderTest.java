package nz.charlie.espeakng;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class VoiceFolderTest {
    @Test
    public void rejectsUnsafeNames() {
        assertFalse(VoiceFolder.isSafeName("../secret"));
        assertFalse(VoiceFolder.isSafeName("a/b"));
        assertFalse(VoiceFolder.isSafeName("a\\b"));
        assertFalse(VoiceFolder.isSafeName(""));
        assertFalse(VoiceFolder.isSafeName(null));
        assertTrue(VoiceFolder.isSafeName("m8"));
        assertTrue(VoiceFolder.isSafeName("designed-MyVoice"));
        assertTrue(VoiceFolder.isSafeName("en_dict"));
    }

    @Test
    public void ensureStructureCreatesSubfoldersAndReadme() {
        File root = new File(System.getProperty("java.io.tmpdir"), "espeak-voices-" + System.nanoTime());
        try {
            assertTrue(VoiceFolder.ensureStructure(root));
            assertTrue(new File(root, "variants").isDirectory());
            assertTrue(new File(root, "languages").isDirectory());
            assertTrue(new File(root, "dictionaries").isDirectory());
            File readme = new File(root, "README.txt");
            assertTrue(readme.isFile());
            assertTrue(readme.length() > 0);
        } finally {
            deleteTree(root);
        }
    }

    @Test
    public void rescanCopiesVariantsLanguagesAndDictionaries() throws Exception {
        File pub = new File(System.getProperty("java.io.tmpdir"), "espeak-pub-" + System.nanoTime());
        File data = new File(System.getProperty("java.io.tmpdir"), "espeak-data-" + System.nanoTime());
        try {
            VoiceFolder.ensureStructure(pub);
            write(new File(VoiceFolder.getVariantsDir(pub), "m8"), "language variant\nname male8\n");
            write(new File(VoiceFolder.getLanguagesDir(pub), "xx-test"), "name Test\nlanguage xx\n");
            write(new File(VoiceFolder.getDictionariesDir(pub), "xx_dict"), "not-a-real-dict\n");
            write(new File(VoiceFolder.getDictionariesDir(pub), "notes.txt"), "skip me\n");
            VoiceFolder.Result result = VoiceFolder.rescan(pub, data);
            assertEquals(1, result.variants);
            assertEquals(1, result.languages);
            assertEquals(1, result.dictionaries);
            assertEquals(1, result.skipped); // notes.txt
            assertTrue(new File(VoiceFolder.getEngineVariantsDir(data), "m8").isFile());
            assertTrue(new File(VoiceFolder.getEngineLangDir(data), "xx-test").isFile());
            assertTrue(new File(data, "xx_dict").isFile());
            assertFalse(new File(data, "notes.txt").exists());
        } finally {
            deleteTree(pub);
            deleteTree(data);
        }
    }

    @Test
    public void rescanSkipsPathTraversalNames() throws Exception {
        // isSafeName is the guard; names with slashes never appear as getName()
        assertFalse(VoiceFolder.isSafeName(".."));
        assertFalse(VoiceFolder.isSafeName("foo..bar"));
    }

    private static void write(File file, String text) throws Exception {
        file.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(text.getBytes(Charset.forName("UTF-8")));
        } finally {
            out.close();
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteTree(child);
            }
        }
        file.delete();
    }
}
