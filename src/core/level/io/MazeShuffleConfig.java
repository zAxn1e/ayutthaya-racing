package core.level.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record MazeShuffleConfig(boolean enabled, double intervalSeconds, List<String> mazePaths) {
    private static final double DEFAULT_INTERVAL_SECONDS = 45.0;
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_INTERVAL_SECONDS = "intervalSeconds";
    private static final String KEY_MAZES = "mazes";
    private static final String KEY_MAZE = "maze";

    public static MazeShuffleConfig loadFromFile(String configPath, String fallbackMazePath) {
        File configFile = new File(configPath);
        File configParent = configFile.getParentFile();
        List<String> mazeEntries = new ArrayList<>();
        boolean enabled = false;
        double intervalSeconds = DEFAULT_INTERVAL_SECONDS;

        if (configFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                for (String raw : lines) {
                    String line = stripComments(raw).trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    int splitAt = line.indexOf('=');
                    if (splitAt <= 0) {
                        continue;
                    }
                    String key = line.substring(0, splitAt).trim();
                    String value = line.substring(splitAt + 1).trim();
                    if (key.equalsIgnoreCase(KEY_ENABLED)) {
                        enabled = value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes");
                    } else if (key.equalsIgnoreCase(KEY_INTERVAL_SECONDS)) {
                        try {
                            intervalSeconds = Math.max(5.0, Double.parseDouble(value));
                        } catch (NumberFormatException ignored) {
                            intervalSeconds = DEFAULT_INTERVAL_SECONDS;
                        }
                    } else if (key.equalsIgnoreCase(KEY_MAZES)) {
                        String[] split = value.split(",");
                        for (String part : split) {
                            if (!part.isBlank()) {
                                mazeEntries.add(part.trim());
                            }
                        }
                    } else if (key.equalsIgnoreCase(KEY_MAZE)) {
                        if (!value.isBlank()) {
                            mazeEntries.add(value.trim());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to read maze shuffle config: " + e.getMessage());
            }
        }

        Set<String> dedup = new LinkedHashSet<>();
        for (String entry : mazeEntries) {
            String resolved = resolveMazePath(configParent, entry);
            if (resolved != null) {
                dedup.add(resolved);
            }
        }

        if (fallbackMazePath != null && !fallbackMazePath.isBlank()) {
            dedup.add(new File(fallbackMazePath).getPath());
        }

        List<String> resolvedMazes = new ArrayList<>(dedup);
        boolean usable = enabled && resolvedMazes.size() >= 2;
        return new MazeShuffleConfig(usable, intervalSeconds, resolvedMazes);
    }

    private static String resolveMazePath(File configParent, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        File path = new File(rawValue);
        if (!path.isAbsolute() && configParent != null) {
            path = new File(configParent, rawValue);
        }
        if (!path.exists()) {
            return null;
        }
        try {
            return path.getCanonicalPath();
        } catch (IOException ignored) {
            return path.getAbsolutePath();
        }
    }

    private static String stripComments(String line) {
        if (line == null) {
            return "";
        }
        int semicolon = line.indexOf(';');
        int hash = line.indexOf('#');
        int cut = -1;
        if (semicolon >= 0) {
            cut = semicolon;
        }
        if (hash >= 0) {
            cut = cut < 0 ? hash : Math.min(cut, hash);
        }
        return cut >= 0 ? line.substring(0, cut) : line;
    }
}
