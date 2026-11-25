package nz.charlie.espeakng;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VariantScannerTest {
    @Test
    public void classifyKnownPatterns() {
        assertEquals(VariantScanner.CAT_MALE, VariantScanner.classify("m8"));
        assertEquals(VariantScanner.CAT_MALE, VariantScanner.classify("m1"));
        assertEquals(VariantScanner.CAT_FEMALE, VariantScanner.classify("f5"));
        assertEquals(VariantScanner.CAT_KLATT, VariantScanner.classify("klatt"));
        assertEquals(VariantScanner.CAT_KLATT, VariantScanner.classify("klatt6"));
        assertEquals(VariantScanner.CAT_WHISPER, VariantScanner.classify("whisper"));
        assertEquals(VariantScanner.CAT_WHISPER, VariantScanner.classify("whisperf"));
        assertEquals(VariantScanner.CAT_CROAK, VariantScanner.classify("croak"));
        assertEquals(VariantScanner.CAT_DESIGNED, VariantScanner.classify("designed-MyVoice"));
        assertEquals(VariantScanner.CAT_NAMED, VariantScanner.classify("robosoft"));
        assertEquals(VariantScanner.CAT_NAMED, VariantScanner.classify("Storm"));
    }

    @Test
    public void displayNames() {
        assertEquals("Male 8", VariantScanner.displayName("m8"));
        assertEquals("Female 5", VariantScanner.displayName("f5"));
        assertEquals("Klatt 1", VariantScanner.displayName("klatt"));
        assertEquals("Klatt 6", VariantScanner.displayName("klatt6"));
        assertEquals("Robosoft", VariantScanner.displayName("robosoft"));
    }

    @Test
    public void buildCategoriesIncludesDefaultsAndDiscoveredFiles() {
        List<String> names = Arrays.asList("m8", "klatt3", "robosoft", "designed-Charlie", "croak");
        List<VariantScanner.Category> cats = VariantScanner.buildCategories(names);
        List<String> ids = new ArrayList<String>();
        for (VariantScanner.Category c : cats) ids.add(c.id);
        assertTrue(ids.contains(VariantScanner.CAT_MALE));
        assertTrue(ids.contains(VariantScanner.CAT_FEMALE));
        assertTrue(ids.contains("young"));
        assertTrue(ids.contains("old"));
        assertTrue(ids.contains(VariantScanner.CAT_KLATT));
        assertTrue(ids.contains(VariantScanner.CAT_NAMED));
        assertTrue(ids.contains(VariantScanner.CAT_DESIGNED));
        assertTrue(ids.contains(VariantScanner.CAT_CROAK));

        // Male has Default plus m8
        assertEquals("male", cats.get(0).items.get(0).variantValue);
        boolean foundM8 = false;
        for (VariantScanner.Item item : cats.get(0).items) {
            if ("m8".equals(item.variantValue)) foundM8 = true;
        }
        assertTrue(foundM8);
    }

    @Test
    public void listFileNamesReadsDirectoryAndSkipsDotfiles() throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "espeak-variant-scan-" + System.nanoTime());
        assertTrue(dir.mkdirs());
        try {
            touch(new File(dir, "m8"));
            touch(new File(dir, "robosoft"));
            touch(new File(dir, ".hidden"));
            List<String> names = VariantScanner.listFileNames(dir);
            assertEquals(Arrays.asList("m8", "robosoft"), names);
        } finally {
            for (File f : dir.listFiles()) f.delete();
            dir.delete();
        }
    }

    private static void touch(File file) throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        out.write('x');
        out.close();
    }
}
