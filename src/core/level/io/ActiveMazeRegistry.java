package core.level.io;

import core.config.ProjectPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ActiveMazeRegistry {
    public static final String DEFAULT_ACTIVE_MAZE_CONFIG = ProjectPaths.mazeFilePath("active_maze_path.txt");
    public static final String LEGACY_ACTIVE_MAZE_POINTER = ProjectPaths.mazeFilePath("active_maze.txt");
    public static final String DEFAULT_RUNTIME_MAZE = ProjectPaths.mazeFilePath("maze_v2_layout_1-edited");

    private ActiveMazeRegistry() {
    }

    public static String readActiveMazePath(String configPath) throws IOException {
        File file = new File(configPath);
        if (!file.exists()) {
            return null;
        }

        String raw = Files.readString(file.toPath()).trim();
        if (raw.isEmpty()) {
            return null;
        }

        File stored = new File(raw);
        if (stored.isAbsolute()) {
            return stored.getPath();
        }

        File parent = file.getParentFile();
        return parent == null ? stored.getPath() : new File(parent, raw).getPath();
    }

    public static String resolveRuntimeMazePath(String configPath, String legacyPointerPath, String defaultMazePath)
            throws IOException {
        String configuredPath = readActiveMazePath(configPath);
        if (configuredPath != null && new File(configuredPath).exists()) {
            return configuredPath;
        }

        String fromLegacyPointer = readLegacyMazePointerPath(legacyPointerPath);
        if (fromLegacyPointer != null && new File(fromLegacyPointer).exists()) {
            try {
                writeActiveMazePath(configPath, new File(fromLegacyPointer));
            } catch (IOException ignored) {
                // migration failure - continue using legacy pointer without interrupting runtime
            }
            return fromLegacyPointer;
        }

        return defaultMazePath;
    }

    public static void writeActiveMazePath(String configPath, File mazeFile) throws IOException {
        File configFile = new File(configPath);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String value = mazeFile.getPath();
        if (parent != null) {
            try {
                Path parentPath = parent.getCanonicalFile().toPath();
                Path mazePath = mazeFile.getCanonicalFile().toPath();
                value = parentPath.relativize(mazePath).toString();
            } catch (IllegalArgumentException ignored) {
                value = mazeFile.getCanonicalFile().getPath();
            }
        }

        try (FileWriter writer = new FileWriter(configFile, false)) {
            writer.write(value);
            writer.write(System.lineSeparator());
        }
    }

    private static String readLegacyMazePointerPath(String pointerPath) throws IOException {
        File pointerFile = new File(pointerPath);
        if (!pointerFile.exists()) {
            return null;
        }

        List<String> lines = Files.readAllLines(pointerFile.toPath());
        String candidate = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith(";")) {
                candidate = trimmed;
                break;
            }
        }
        if (candidate == null || candidate.isEmpty() || looksLikeMazeRow(candidate)) {
            return null;
        }

        File path = new File(candidate);
        if (path.isAbsolute()) {
            return path.getPath();
        }
        File parent = pointerFile.getParentFile();
        return parent == null ? path.getPath() : new File(parent, candidate).getPath();
    }

    private static boolean looksLikeMazeRow(String line) {
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '#' || ch == '.' || ch == 'P' || ch == 'G' || ch == 'o' || ch == 'O') {
                return true;
            }
        }
        return false;
    }
}
