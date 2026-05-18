package core.level.io;

import core.level.Maze;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MazeTextSerializer {
    private MazeTextSerializer() {
    }

    public static List<String> toRows(Maze maze) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                row.append(maze.getTile(x, y).getSymbol());
            }
            rows.add(row.toString());
        }
        return rows;
    }

    public static String toText(Maze maze, MazeMetadata metadata) {
        StringBuilder text = new StringBuilder();
        LinkedHashMap<String, String> effectiveMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            effectiveMetadata.putAll(metadata.entries());
        }
        String spawnZoneRows = encodeSpawnZoneRows(maze);
        if (spawnZoneRows == null) {
            effectiveMetadata.remove("spawnZoneRows");
        } else {
            effectiveMetadata.put("spawnZoneRows", spawnZoneRows);
        }

        if (!effectiveMetadata.isEmpty()) {
            for (Map.Entry<String, String> entry : effectiveMetadata.entrySet()) {
                text.append("; ").append(entry.getKey()).append('=').append(entry.getValue()).append(System.lineSeparator());
            }
            text.append(System.lineSeparator());
        }

        for (String row : toRows(maze)) {
            text.append(row).append(System.lineSeparator());
        }
        return text.toString();
    }

    public static String encodeSpawnZoneRows(Maze maze) {
        if (maze == null || !maze.hasAnySpawnZone()) {
            return null;
        }
        StringBuilder encoded = new StringBuilder();
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            if (y > 0) {
                encoded.append('|');
            }
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                encoded.append(maze.isSpawnZone(x, y) ? '1' : '0');
            }
        }
        return encoded.toString();
    }
}
