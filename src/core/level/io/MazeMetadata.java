package core.level.io;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MazeMetadata(Map<String, String> entries) {
    public MazeMetadata {
        entries = new LinkedHashMap<>(entries);
    }

    public static MazeMetadata empty() {
        return new MazeMetadata(new LinkedHashMap<>());
    }

    public static MazeMetadata fromCommentLines(List<String> commentLines) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        for (String line : commentLines) {
            int splitAt = line.indexOf('=');
            if (splitAt <= 0) {
                continue;
            }
            String key = line.substring(0, splitAt).trim();
            String value = line.substring(splitAt + 1).trim();
            if (!key.isEmpty()) {
                entries.put(key, value);
            }
        }
        return new MazeMetadata(entries);
    }

    public MazeMetadata withEntry(String key, String value) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(entries);
        if (value == null || value.isBlank()) {
            copy.remove(key);
        } else {
            copy.put(key, value.trim());
        }
        return new MazeMetadata(copy);
    }

    public String get(String key, String fallback) {
        String value = entries.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
