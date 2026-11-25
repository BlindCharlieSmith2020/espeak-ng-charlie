/*
 * Copyright (C) 2026 Charlie Smith
 *
 * This file is part of eSpeak NG Charlie, a fork of eSpeak NG.
 * Licensed under the GNU General Public License, version 3 or later.
 * See the file COPYING in the project root.
 */

package nz.charlie.espeakng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

/** Writes an eSpeak {@code voices/!v} variant file from Design voice sliders. */
public final class DesignedVoice {
    public int pitchBase = 80;
    public int pitchRange = 118;
    public int speedPercent = 100;
    public int flutter = 0;
    public int roughness = 0;
    public int breathiness = 0; // 0-100, maps to breath / voicing
    public int formant1 = 100;
    public int formant2 = 100;
    public int formant3 = 100;
    public String name = "My voice";

    public static String safeFileName(String name) {
        if (name == null) return "designed-voice";
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
        cleaned = cleaned.replaceAll("^-+", "").replaceAll("-+$", "");
        if (cleaned.length() == 0) cleaned = "designed-voice";
        if (!cleaned.toLowerCase(Locale.US).startsWith("designed-")
                && !cleaned.toLowerCase(Locale.US).startsWith("designed_")) {
            cleaned = "designed-" + cleaned;
        }
        return cleaned;
    }

    /** Render the eSpeak variant file contents. Pure text, easy to unit-test. */
    public String toVoiceFile() {
        String fileName = safeFileName(name);
        int breath = Math.max(0, Math.min(100, breathiness));
        int voicing = Math.max(0, 100 - breath);
        StringBuilder sb = new StringBuilder();
        sb.append("// Designed with eSpeak NG Charlie Design voice\n");
        sb.append("language variant\n");
        sb.append("name ").append(name.trim().length() == 0 ? fileName : name.trim()).append('\n');
        sb.append("gender male\n\n");
        sb.append("pitch ").append(pitchBase).append(' ').append(pitchRange).append('\n');
        sb.append("speed ").append(speedPercent).append('\n');
        sb.append("flutter ").append(flutter).append('\n');
        sb.append("roughness ").append(roughness).append('\n');
        if (breath > 0) {
            sb.append("breath  ").append(breath).append(' ').append(breath).append(' ')
              .append(Math.max(0, breath - 15)).append(' ').append(Math.max(0, breath - 25))
              .append(" 15 10\n");
            sb.append("voicing ").append(voicing).append('\n');
        }
        sb.append('\n');
        sb.append("formant 0 100 100 100\n");
        sb.append("formant 1 ").append(formant1).append(" 100 100\n");
        sb.append("formant 2 ").append(formant2).append(" 100 100\n");
        sb.append("formant 3 ").append(formant3).append(" 100 100\n");
        sb.append("formant 4 100 100 100\n");
        sb.append("formant 5 100 100 100\n");
        sb.append("formant 6 100 100 100\n");
        sb.append("formant 7 100 100 100\n");
        sb.append("formant 8 100 100 100\n");
        return sb.toString();
    }

    public File writeTo(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }
        File dest = new File(directory, safeFileName(name));
        FileOutputStream out = new FileOutputStream(dest);
        try {
            byte[] data = toVoiceFile().getBytes(Charset.forName("UTF-8"));
            out.write(data);
        } finally {
            out.close();
        }
        return dest;
    }
}
