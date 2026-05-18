package editor;

import core.level.Maze;
import core.level.io.MazeFileData;
import core.level.io.MazeMetadata;
import core.level.io.MazeTextLoader;
import core.level.io.MazeTextSerializer;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;

public class MazeEditorDocument {
    private Maze maze;
    private MazeMetadata metadata;
    private File currentFile;
    private boolean dirty;
    private final Deque<EditorSnapshot> undoStack = new ArrayDeque<>();
    private final Deque<EditorSnapshot> redoStack = new ArrayDeque<>();

    public MazeEditorDocument(Maze maze, MazeMetadata metadata, File currentFile) {
        this.maze = maze;
        this.metadata = metadata == null ? MazeMetadata.empty() : metadata;
        this.currentFile = currentFile;
        this.dirty = false;
    }

    public static MazeEditorDocument createBlank(int width, int height, int tileSize) {
        Maze maze = new Maze(width, height, tileSize, null, null);
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        entries.put("name", "untitled");
        entries.put("tileSize", Integer.toString(tileSize));
        return new MazeEditorDocument(maze, new MazeMetadata(entries), null);
    }

    public static MazeEditorDocument load(File file, int fallbackTileSize) throws IOException {
        MazeFileData fileData = MazeTextLoader.loadFileData(file.getPath(), fallbackTileSize, null, null);
        MazeMetadata metadata = fileData.metadata().withEntry("tileSize",
                fileData.metadata().get("tileSize", Integer.toString(fileData.maze().getTileSize())));
        return new MazeEditorDocument(fileData.maze(), metadata, file);
    }

    public Maze getMaze() {
        return maze;
    }

    public MazeMetadata getMetadata() {
        return metadata;
    }

    public void setMetadataValue(String key, String value) {
        MazeMetadata updated = metadata.withEntry(key, value);
        if (!updated.equals(metadata)) {
            metadata = updated;
            if ("background".equals(key)) {
                rebuildMazeWithCurrentMetadata();
            }
            dirty = true;
        }
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getDisplayName() {
        if (currentFile != null) {
            return currentFile.getName();
        }
        return metadata.get("name", "untitled");
    }

    public void beginEdit() {
        undoStack.push(snapshot());
        if (undoStack.size() > 100) {
            while (undoStack.size() > 100) {
                undoStack.removeLast();
            }
        }
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        redoStack.push(snapshot());
        restore(undoStack.pop());
        dirty = true;
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }
        undoStack.push(snapshot());
        restore(redoStack.pop());
        dirty = true;
    }

    public boolean setTile(int x, int y, Maze.Tile tile) {
        if (!maze.isInBounds(x, y) || maze.getTile(x, y) == tile) {
            return false;
        }
        maze.setTile(x, y, tile);
        dirty = true;
        return true;
    }

    public boolean setSpawnZone(int x, int y, boolean inSpawnZone) {
        if (!maze.isInBounds(x, y) || maze.isSpawnZone(x, y) == inSpawnZone) {
            return false;
        }
        maze.setSpawnZone(x, y, inSpawnZone);
        dirty = true;
        return true;
    }

    public boolean drawRectangle(Point start, Point end, Maze.Tile tile, boolean fill) {
        int minX = Math.min(start.x, end.x);
        int maxX = Math.max(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxY = Math.max(start.y, end.y);
        boolean changed = false;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean border = x == minX || x == maxX || y == minY || y == maxY;
                if (fill || border) {
                    changed |= setTile(x, y, tile);
                }
            }
        }
        return changed;
    }

    public boolean drawSpawnZoneRectangle(Point start, Point end, boolean inSpawnZone, boolean fill) {
        int minX = Math.min(start.x, end.x);
        int maxX = Math.max(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxY = Math.max(start.y, end.y);
        boolean changed = false;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean border = x == minX || x == maxX || y == minY || y == maxY;
                if (fill || border) {
                    changed |= setSpawnZone(x, y, inSpawnZone);
                }
            }
        }
        return changed;
    }

    public boolean floodFill(int startX, int startY, Maze.Tile replacement) {
        if (!maze.isInBounds(startX, startY)) {
            return false;
        }
        Maze.Tile target = maze.getTile(startX, startY);
        if (target == replacement) {
            return false;
        }

        boolean changed = false;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            if (!maze.isInBounds(point.x, point.y) || maze.getTile(point.x, point.y) != target) {
                continue;
            }
            maze.setTile(point.x, point.y, replacement);
            changed = true;

            queue.add(new Point(point.x + 1, point.y));
            queue.add(new Point(point.x - 1, point.y));
            queue.add(new Point(point.x, point.y + 1));
            queue.add(new Point(point.x, point.y - 1));
        }

        if (changed) {
            dirty = true;
        }
        return changed;
    }

    public boolean floodFillSpawnZone(int startX, int startY, boolean inSpawnZone) {
        if (!maze.isInBounds(startX, startY)) {
            return false;
        }
        boolean target = maze.isSpawnZone(startX, startY);
        if (target == inSpawnZone) {
            return false;
        }

        boolean changed = false;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            if (!maze.isInBounds(point.x, point.y) || maze.isSpawnZone(point.x, point.y) != target) {
                continue;
            }
            maze.setSpawnZone(point.x, point.y, inSpawnZone);
            changed = true;
            queue.add(new Point(point.x + 1, point.y));
            queue.add(new Point(point.x - 1, point.y));
            queue.add(new Point(point.x, point.y + 1));
            queue.add(new Point(point.x, point.y - 1));
        }

        if (changed) {
            dirty = true;
        }
        return changed;
    }

    public void replaceMaze(Maze newMaze) {
        maze = newMaze;
        metadata = metadata.withEntry("tileSize", Integer.toString(newMaze.getTileSize()));
        dirty = true;
    }

    public void save(File file) throws IOException {
        metadata = metadata.withEntry("tileSize", Integer.toString(maze.getTileSize()))
                .withEntry("width", Integer.toString(maze.getWidthInTiles()))
                .withEntry("height", Integer.toString(maze.getHeightInTiles()));
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(MazeTextSerializer.toText(maze, metadata));
        }
        currentFile = file;
        rebuildMazeWithCurrentMetadata();
        dirty = false;
    }

    private EditorSnapshot snapshot() {
        return new EditorSnapshot(
                MazeTextSerializer.toRows(maze),
                MazeTextSerializer.encodeSpawnZoneRows(maze),
                metadata,
                currentFile
        );
    }

    private void restore(EditorSnapshot snapshot) {
        int tileSize = Integer.parseInt(snapshot.metadata().get("tileSize", Integer.toString(maze.getTileSize())));
        maze = MazeTextLoader.loadFromRows(
                snapshot.rows(),
                tileSize,
                null,
                resolveBackgroundPathForPreview(snapshot.metadata().get("background", null), snapshot.currentFile()),
                snapshot.spawnZoneRows()
        );
        metadata = snapshot.metadata();
        currentFile = snapshot.currentFile();
    }

    private void rebuildMazeWithCurrentMetadata() {
        maze = MazeTextLoader.loadFromRows(
                MazeTextSerializer.toRows(maze),
                maze.getTileSize(),
                null,
                resolveBackgroundPathForPreview(metadata.get("background", null), currentFile)
        );
    }

    private String resolveBackgroundPathForPreview(String backgroundValue, File mazeFile) {
        if (backgroundValue == null || backgroundValue.isBlank()) {
            return null;
        }
        File backgroundFile = new File(backgroundValue);
        if (backgroundFile.isAbsolute()) {
            return backgroundFile.getPath();
        }
        if (mazeFile != null && mazeFile.getParentFile() != null) {
            return new File(mazeFile.getParentFile(), backgroundValue).getPath();
        }
        return backgroundFile.getPath();
    }

    private record EditorSnapshot(List<String> rows, String spawnZoneRows, MazeMetadata metadata, File currentFile) {
    }
}
