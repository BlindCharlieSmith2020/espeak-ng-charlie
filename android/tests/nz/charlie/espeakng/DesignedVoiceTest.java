package nz.charlie.espeakng;

import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class DesignedVoiceTest {
    @Test
    public void safeFileNamePrefixesDesigned() {
        assertEquals("designed-MyVoice", DesignedVoice.safeFileName("MyVoice"));
        assertEquals("designed-Soft-Voice", DesignedVoice.safeFileName("Soft Voice!"));
        assertEquals("designed-voice", DesignedVoice.safeFileName("   "));
        assertTrue(DesignedVoice.safeFileName("designed-already").startsWith("designed-"));
    }

    @Test
    public void voiceFileContainsChosenParameters() {
        DesignedVoice voice = new DesignedVoice();
        voice.name = "Charlie Test";
        voice.pitchBase = 90;
        voice.pitchRange = 130;
        voice.speedPercent = 110;
        voice.flutter = 4;
        voice.roughness = 3;
        voice.breathiness = 20;
        voice.formant1 = 95;
        voice.formant2 = 108;
        voice.formant3 = 102;
        String text = voice.toVoiceFile();
        assertTrue(text.contains("language variant"));
        assertTrue(text.contains("name Charlie Test"));
        assertTrue(text.contains("pitch 90 130"));
        assertTrue(text.contains("speed 110"));
        assertTrue(text.contains("flutter 4"));
        assertTrue(text.contains("roughness 3"));
        assertTrue(text.contains("breath  "));
        assertTrue(text.contains("formant 1 95 100 100"));
        assertTrue(text.contains("formant 2 108 100 100"));
        assertTrue(text.contains("formant 3 102 100 100"));
        assertTrue(text.contains("Designed with eSpeak NG Charlie"));
    }

    @Test
    public void writeToCreatesFile() throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "espeak-designed-" + System.nanoTime());
        assertTrue(dir.mkdirs());
        try {
            DesignedVoice voice = new DesignedVoice();
            voice.name = "Unit";
            File saved = voice.writeTo(dir);
            assertTrue(saved.isFile());
            assertEquals("designed-Unit", saved.getName());
            String text = new String(Files.readAllBytes(saved.toPath()), Charset.forName("UTF-8"));
            assertTrue(text.contains("name Unit"));
        } finally {
            for (File f : dir.listFiles()) f.delete();
            dir.delete();
        }
    }
}
