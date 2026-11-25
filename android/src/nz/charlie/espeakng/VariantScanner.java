/*
 * Copyright (C) 2026 Charlie Smith
 *
 * This file is part of eSpeak NG Charlie, a fork of eSpeak NG.
 * Licensed under the GNU General Public License, version 3 or later.
 * See the file COPYING in the project root.
 */

package nz.charlie.espeakng;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Discovers eSpeak voice variants at runtime by scanning the {@code voices/!v}
 * directory (and any extra directories). This is the fix for
 * <a href="https://github.com/espeak-ng/espeak-ng/issues/2376">issue #2376</a>:
 * the official Android UI hard-codes a short list and hides most variants.
 */
public final class VariantScanner {
    public static final String CAT_MALE = "male";
    public static final String CAT_FEMALE = "female";
    public static final String CAT_KLATT = "klatt";
    public static final String CAT_WHISPER = "whisper";
    public static final String CAT_CROAK = "croak";
    public static final String CAT_NAMED = "named";
    public static final String CAT_DESIGNED = "designed";
    public static final String CAT_OTHER = "other";

    private static final Pattern MALE = Pattern.compile("m\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FEMALE = Pattern.compile("f\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern KLATT = Pattern.compile("klatt\\d*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHISPER = Pattern.compile("whisper.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CROAK = Pattern.compile("croak.*", Pattern.CASE_INSENSITIVE);

    private VariantScanner() {}

    public static final class Item {
        public final String id;
        public final String displayName;
        public final String variantValue;

        public Item(String id, String displayName, String variantValue) {
            this.id = id;
            this.displayName = displayName;
            this.variantValue = variantValue;
        }
    }

    public static final class Category {
        public final String id;
        public final String title;
        public final List<Item> items = new ArrayList<Item>();

        public Category(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    /** Classify a variant file name into a category id. */
    public static String classify(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return CAT_OTHER;
        }
        String name = fileName;
        if (name.startsWith("designed-") || name.startsWith("designed_")) {
            return CAT_DESIGNED;
        }
        if (MALE.matcher(name).matches()) return CAT_MALE;
        if (FEMALE.matcher(name).matches()) return CAT_FEMALE;
        if (KLATT.matcher(name).matches()) return CAT_KLATT;
        if (WHISPER.matcher(name).matches()) return CAT_WHISPER;
        if (CROAK.matcher(name).matches()) return CAT_CROAK;
        return CAT_NAMED;
    }

    /** Human title for a raw variant file name, e.g. {@code m8} -> {@code Male 8}. */
    public static String displayName(String fileName) {
        if (fileName == null) return "";
        if (MALE.matcher(fileName).matches()) {
            return "Male " + fileName.substring(1);
        }
        if (FEMALE.matcher(fileName).matches()) {
            return "Female " + fileName.substring(1);
        }
        if (KLATT.matcher(fileName).matches()) {
            if (fileName.equalsIgnoreCase("klatt")) return "Klatt 1";
            return "Klatt " + fileName.substring(5);
        }
        // Insert spaces before capitals and digits in camel-case names.
        String spaced = fileName.replace('-', ' ').replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaced.length(); i++) {
            char c = spaced.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(spaced.charAt(i - 1))) {
                sb.append(' ');
            }
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> listFileNames(File... directories) {
        List<String> names = new ArrayList<String>();
        FileFilter filesOnly = new FileFilter() {
            @Override public boolean accept(File file) {
                return file.isFile() && !file.getName().startsWith(".");
            }
        };
        if (directories == null) return names;
        for (File dir : directories) {
            if (dir == null || !dir.isDirectory()) continue;
            File[] files = dir.listFiles(filesOnly);
            if (files == null) continue;
            for (File file : files) {
                String name = file.getName();
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Build the category list used by the settings UI. Always includes Male and
     * Female default entries (no variant file), plus Young and Old age groups,
     * then every file found on disk.
     */
    public static List<Category> buildCategories(List<String> fileNames) {
        Map<String, Category> map = new LinkedHashMap<String, Category>();
        add(map, CAT_MALE, "Male");
        add(map, CAT_FEMALE, "Female");
        add(map, CAT_KLATT, "Klatt");
        add(map, CAT_NAMED, "Named");
        add(map, CAT_DESIGNED, "Designed");
        add(map, CAT_WHISPER, "Whisper");
        add(map, CAT_CROAK, "Croak");
        add(map, CAT_OTHER, "Other");

        // Defaults that are gender/age, not files.
        map.get(CAT_MALE).items.add(new Item("male", "Default", "male"));
        map.get(CAT_FEMALE).items.add(new Item("female", "Default", "female"));

        Category young = new Category("young", "Young");
        young.items.add(new Item("male-young", "Male", "male-young"));
        young.items.add(new Item("female-young", "Female", "female-young"));
        Category old = new Category("old", "Old");
        old.items.add(new Item("male-old", "Male", "male-old"));
        old.items.add(new Item("female-old", "Female", "female-old"));

        if (fileNames != null) {
            for (String name : fileNames) {
                String cat = classify(name);
                Category category = map.get(cat);
                if (category == null) {
                    category = map.get(CAT_OTHER);
                }
                category.items.add(new Item(name, displayName(name), name));
            }
        }

        List<Category> result = new ArrayList<Category>();
        result.add(map.get(CAT_MALE));
        result.add(map.get(CAT_FEMALE));
        result.add(young);
        result.add(old);
        for (String id : new String[] { CAT_KLATT, CAT_NAMED, CAT_DESIGNED, CAT_WHISPER, CAT_CROAK, CAT_OTHER }) {
            Category category = map.get(id);
            if (!category.items.isEmpty()) {
                result.add(category);
            }
        }
        return result;
    }

    public static List<Category> scan(File... directories) {
        return buildCategories(listFileNames(directories));
    }

    private static void add(Map<String, Category> map, String id, String title) {
        map.put(id, new Category(id, title));
    }
}
