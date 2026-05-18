package core.level.io;

import core.level.Maze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MazeTextLoader {
    private MazeTextLoader() {
    }

    public static Maze loadFromFile(String filePath, int tileSize, String backgroundClasspath, String backgroundFilePath) throws IOException {
        return loadFileData(filePath, tileSize, backgroundClasspath, backgroundFilePath).maze();
    }

    public static MazeFileData loadFileData(String filePath, int tileSize, String backgroundClasspath, String backgroundFilePath) throws IOException {
        File file = new File(filePath);
        List<String> rows = new ArrayList<>();
        List<String> commentLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith(";")) {
                    commentLines.add(trimmed.substring(1).trim());
                    continue;
                }
                rows.add(line.replace(" ", "."));
            }
        }

        MazeMetadata metadata = MazeMetadata.fromCommentLines(commentLines);
        String metadataBackground = metadata.get("background", null);
        String spawnZoneRows = metadata.get("spawnZoneRows", null);
        String resolvedBackgroundPath = metadataBackground != null && !metadataBackground.isBlank()
                ? resolveBackgroundPath(file, metadataBackground)
                : backgroundFilePath;
        Maze maze = loadFromRows(rows, tileSize, backgroundClasspath, resolvedBackgroundPath, spawnZoneRows);
        return new MazeFileData(maze, metadata, file, resolvedBackgroundPath);
    }

    public static Maze loadFromRows(List<String> rows, int tileSize, String backgroundClasspath, String backgroundFilePath) {
        return loadFromRows(rows, tileSize, backgroundClasspath, backgroundFilePath, null);
    }

    public static Maze loadFromRows(List<String> rows, int tileSize, String backgroundClasspath,
                                    String backgroundFilePath, String encodedSpawnZoneRows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Maze text rows must not be empty.");
        }

        int width = rows.get(0).length();
        int height = rows.size();
        Maze maze = new Maze(width, height, tileSize, backgroundClasspath, backgroundFilePath);

        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            if (row.length() != width) {
                throw new IllegalArgumentException("Maze text rows must have equal width.");
            }
            for (int x = 0; x < width; x++) {
                char symbol = row.charAt(x);
                if (symbol == 'Z' || symbol == 'E' || symbol == 'K') {
                    maze.setTile(x, y, Maze.Tile.EMPTY);
                    maze.setSpawnZone(x, y, true);
                } else {
                    maze.setTile(x, y, Maze.Tile.fromSymbol(symbol));
                }
            }
        }

        applySpawnZoneRows(maze, encodedSpawnZoneRows);
        return maze;
    }

    private static void applySpawnZoneRows(Maze maze, String encodedSpawnZoneRows) {
        if (encodedSpawnZoneRows == null || encodedSpawnZoneRows.isBlank()) {
            return;
        }
        String[] rows = encodedSpawnZoneRows.split("\\|");
        maze.clearSpawnZoneLayer();
        for (int y = 0; y < Math.min(rows.length, maze.getHeightInTiles()); y++) {
            String row = rows[y];
            for (int x = 0; x < Math.min(row.length(), maze.getWidthInTiles()); x++) {
                maze.setSpawnZone(x, y, row.charAt(x) == '1');
            }
        }
    }

    private static String resolveBackgroundPath(File mazeFile, String backgroundPath) {
        if (backgroundPath == null || backgroundPath.isBlank()) {
            return null;
        }

        File backgroundFile = new File(backgroundPath);
        if (backgroundFile.isAbsolute()) {
            return backgroundFile.getPath();
        }

        File parent = mazeFile.getParentFile();
        if (parent == null) {
            return backgroundFile.getPath();
        }
        return new File(parent, backgroundPath).getPath();
    }
}
