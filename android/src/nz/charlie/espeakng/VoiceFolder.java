/*
 * Copyright (C) 2026 Charlie Smith
 *
 * This file is part of eSpeak NG Charlie, a fork of eSpeak NG.
 * Licensed under the GNU General Public License, version 3 or later.
 * See the file COPYING in the project root.
 */

package nz.charlie.espeakng;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * The reachable voice folder at Documents/eSpeak NG Voices, with subfolders
 * for variants, languages and dictionaries.
 */
public final class VoiceFolder {
    public static final String FOLDER_NAME = "eSpeak NG Voices";
    public static final String VARIANTS = "variants";
    public static final String LANGUAGES = "languages";
    public static final String DICTIONARIES = "dictionaries";

    private static final String README = ""
            + "eSpeak NG Charlie voice folder\n"
            + "==============================\n\n"
            + "Drop files in the folders below, then open eSpeak NG Charlie and choose\n"
            + "Rescan voices (or use Rescan voices in TTS settings).\n\n"
            + "variants/\n"
            + "  eSpeak voice variant files, the same kind as espeak-ng-data/voices/!v\n"
            + "  (for example m8, klatt, robosoft, or a voice you saved from Design voice).\n\n"
            + "languages/\n"
            + "  eSpeak language / voice files (the kind in espeak-ng-data/lang).\n\n"
            + "dictionaries/\n"
            + "  Compiled eSpeak dictionary files whose names end in _dict.\n"
            + "  You can also use Import eSpeak dictionary in TTS settings.\n";

    private VoiceFolder() {}

    public static File getPublicRoot() {
        File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        return new File(documents, FOLDER_NAME);
    }

    public static File getVariantsDir(File root) {
        return new File(root, VARIANTS);
    }

    public static File getLanguagesDir(File root) {
        return new File(root, LANGUAGES);
    }

    public static File getDictionariesDir(File root) {
        return new File(root, DICTIONARIES);
    }

    public static File getEngineVariantsDir(File dataPath) {
        return new File(new File(dataPath, "voices"), "!v");
    }

    public static File getEngineLangDir(File dataPath) {
        File imported = new File(new File(dataPath, "lang"), "imported");
        return imported;
    }

    public static boolean ensureStructure(File root) {
        boolean ok = true;
        ok &= mkdir(root);
        ok &= mkdir(getVariantsDir(root));
        ok &= mkdir(getLanguagesDir(root));
        ok &= mkdir(getDictionariesDir(root));
        File readme = new File(root, "README.txt");
        if (!readme.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(readme);
                try {
                    out.write(README.getBytes(Charset.forName("UTF-8")));
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                ok = false;
            }
        }
        return ok;
    }

    public static final class Result {
        public int variants;
        public int languages;
        public int dictionaries;
        public int skipped;
        public final List<String> errors = new ArrayList<String>();

        public String summary() {
            return "Imported " + variants + " variants, " + languages
                    + " languages and " + dictionaries + " dictionaries."
                    + (skipped > 0 ? " Skipped " + skipped + "." : "")
                    + (errors.isEmpty() ? "" : " " + errors.get(0));
        }
    }

    /**
     * Copy new or updated files from the public folder into the engine data
     * directory. Path traversal is rejected. Existing engine files with the
     * same name are overwritten so an updated designed voice is picked up.
     */
    public static Result rescan(File publicRoot, File dataPath) {
        Result result = new Result();
        if (publicRoot == null || dataPath == null) {
            result.errors.add("Missing folder.");
            return result;
        }
        ensureStructure(publicRoot);
        File variantDest = getEngineVariantsDir(dataPath);
        File langDest = getEngineLangDir(dataPath);
        mkdir(variantDest);
        mkdir(langDest);
        mkdir(dataPath);

        result.variants = copyDir(getVariantsDir(publicRoot), variantDest, result);
        // Also accept variant files dropped in the folder root (not README, not subdirs).
        result.variants += copyMatching(publicRoot, variantDest, result, /*dicts*/ false, /*rootVariants*/ true);
        result.languages = copyDir(getLanguagesDir(publicRoot), langDest, result);
        result.dictionaries = copyMatching(getDictionariesDir(publicRoot), dataPath, result, true, false);
        return result;
    }

    public static Result rescan(Context context) {
        File dataPath = CheckVoiceData.getDataPath(context);
        return rescan(getPublicRoot(), dataPath);
    }

    private static boolean mkdir(File dir) {
        return dir.exists() || dir.mkdirs();
    }

    private static int copyDir(File source, File dest, Result result) {
        if (source == null || !source.isDirectory()) return 0;
        File[] files = source.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File file : files) {
            if (!file.isFile() || file.getName().startsWith(".")) continue;
            if (copySafe(file, new File(dest, file.getName()), dest, result)) {
                count++;
            }
        }
        return count;
    }

    private static int copyMatching(File source, File dest, Result result,
                                    boolean dictionariesOnly, boolean rootVariants) {
        if (source == null || !source.isDirectory()) return 0;
        File[] files = source.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File file : files) {
            if (!file.isFile() || file.getName().startsWith(".")) continue;
            String name = file.getName();
            if (name.equalsIgnoreCase("README.txt")) continue;
            if (dictionariesOnly) {
                if (!name.endsWith("_dict")) {
                    result.skipped++;
                    continue;
                }
            } else if (rootVariants) {
                if (name.endsWith("_dict") || name.endsWith(".txt") || name.endsWith(".md")) {
                    continue;
                }
            }
            if (copySafe(file, new File(dest, name), dest, result)) {
                count++;
            }
        }
        return count;
    }

    static boolean isSafeName(String name) {
        if (name == null || name.length() == 0) return false;
        if (name.contains("..") || name.contains("/") || name.contains("\\")) return false;
        if (name.contains("\0")) return false;
        return true;
    }

    private static boolean copySafe(File source, File dest, File destRoot, Result result) {
        if (!isSafeName(source.getName())) {
            result.skipped++;
            result.errors.add("Skipped unsafe name: " + source.getName());
            return false;
        }
        try {
            File destCanon = dest.getCanonicalFile();
            File rootCanon = destRoot.getCanonicalFile();
            String destPath = destCanon.getPath();
            String rootPath = rootCanon.getPath();
            if (!destPath.equals(rootPath) && !destPath.startsWith(rootPath + File.separator)) {
                result.skipped++;
                result.errors.add("Skipped path outside the data folder: " + source.getName());
                return false;
            }
            dest.getParentFile().mkdirs();
            byte[] buffer = new byte[8192];
            FileInputStream in = new FileInputStream(source);
            try {
                FileOutputStream out = new FileOutputStream(dest);
                try {
                    int n;
                    while ((n = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, n);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
            return true;
        } catch (IOException e) {
            result.skipped++;
            result.errors.add("Could not copy " + source.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
