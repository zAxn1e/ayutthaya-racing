package core.level;

import core.config.ProjectPaths;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Maze {
    public static final int DEFAULT_TILE_SIZE = 32;
    public static final int DEFAULT_WIDTH = 29;
    public static final int DEFAULT_HEIGHT = 22;
    private static final String DEFAULT_BACKGROUND_CLASSPATH = "/map/base.png";
    private static final String DEFAULT_BACKGROUND_FILE_PATH = ProjectPaths.mapFilePath("base.png");

    public enum Tile {
        EMPTY('.', true, "Empty", new Color(34, 40, 49)),
        WALL('#', false, "Wall", new Color(76, 95, 170)),
        PLAYER_SPAWN('P', true, "Player Spawn", new Color(241, 196, 15)),
        GHOST_SPAWN('G', true, "Ghost Spawn", new Color(231, 76, 60)),
        PELLET('o', true, "Pellet", new Color(245, 245, 245)),
        POWER_PELLET('O', true, "Power Pellet", new Color(46, 204, 113));

        private final char symbol;
        private final boolean walkable;
        private final String displayName;
        private final Color editorColor;

        Tile(char symbol, boolean walkable, String displayName, Color editorColor) {
            this.symbol = symbol;
            this.walkable = walkable;
            this.displayName = displayName;
            this.editorColor = editorColor;
        }

        public char getSymbol() {
            return symbol;
        }

        public boolean isWalkable() {
            return walkable;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Color getEditorColor() {
            return editorColor;
        }

        public static Tile fromSymbol(char symbol) {
            for (Tile tile : values()) {
                if (tile.symbol == symbol) {
                    return tile;
                }
            }
            return switch (symbol) {
                case 'X', '1' -> WALL;
                case '0', ' ', '-' -> EMPTY;
                default -> EMPTY;
            };
        }
    }

    private final Tile[][] tiles;
    private final boolean[][] spawnZoneLayer;
    private final int widthInTiles;
    private final int heightInTiles;
    private final int tileSize;
    private final BufferedImage backgroundImage;
    private final String backgroundClasspath;
    private final String backgroundFilePath;

    public Maze() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_TILE_SIZE, DEFAULT_BACKGROUND_CLASSPATH, DEFAULT_BACKGROUND_FILE_PATH);
    }

    public Maze(String backgroundClasspath, String backgroundFilePath) {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_TILE_SIZE, backgroundClasspath, backgroundFilePath);
    }

    public Maze(int widthInTiles, int heightInTiles, int tileSize) {
        this(widthInTiles, heightInTiles, tileSize, DEFAULT_BACKGROUND_CLASSPATH, DEFAULT_BACKGROUND_FILE_PATH);
    }

    public Maze(int widthInTiles, int heightInTiles, int tileSize, String backgroundClasspath, String backgroundFilePath) {
        this.backgroundClasspath = backgroundClasspath;
        this.backgroundFilePath = backgroundFilePath;
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.tileSize = tileSize;
        tiles = new Tile[widthInTiles][heightInTiles];
        spawnZoneLayer = new boolean[widthInTiles][heightInTiles];
        backgroundImage = loadBackgroundImage();
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                tiles[x][y] = Tile.EMPTY;
                spawnZoneLayer[x][y] = false;
            }
        }
    }

    public void setTile(int x, int y, Tile tile) {
        if (isInBounds(x, y)) {
            tiles[x][y] = tile;
        }
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) return Tile.WALL;
        return tiles[x][y];
    }

    public void setSpawnZone(int x, int y, boolean inSpawnZone) {
        if (isInBounds(x, y)) {
            spawnZoneLayer[x][y] = inSpawnZone;
        }
    }

    public boolean isSpawnZone(int x, int y) {
        if (!isInBounds(x, y)) {
            return false;
        }
        return spawnZoneLayer[x][y];
    }

    public void clearSpawnZoneLayer() {
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                spawnZoneLayer[x][y] = false;
            }
        }
    }

    public boolean hasAnySpawnZone() {
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                if (spawnZoneLayer[x][y]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isWalkable(int x, int y) {
        if (!isInBounds(x, y)) return false;
        Tile tile = tiles[x][y];
        return tile.isWalkable();
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < widthInTiles && y >= 0 && y < heightInTiles;
    }

    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getPixelWidth() {
        return widthInTiles * tileSize;
    }

    public int getPixelHeight() {
        return heightInTiles * tileSize;
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public String getBackgroundFilePath() {
        return backgroundFilePath;
    }

    public void render(Graphics2D g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getPixelWidth(), getPixelHeight(), null);
            return;
        }

        g.setColor(new Color(15, 18, 30));
        g.fillRect(0, 0, getPixelWidth(), getPixelHeight());

        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                int px = x * tileSize;
                int py = y * tileSize;

                if (tiles[x][y] == Tile.WALL) {
                    g.setColor(new Color(45, 55, 90));
                    g.fillRect(px, py, tileSize, tileSize);

                    g.setColor(new Color(70, 85, 130));
                    g.fillRect(px, py, tileSize, 3);
                    g.fillRect(px, py, 3, tileSize);

                    g.setColor(new Color(25, 30, 50));
                    g.fillRect(px, py + tileSize - 3, tileSize, 3);
                    g.fillRect(px + tileSize - 3, py, 3, tileSize);

                    g.setColor(new Color(80, 100, 150));
                    g.drawRect(px, py, tileSize - 1, tileSize - 1);
                }
            }
        }
    }

    private BufferedImage loadBackgroundImage() {
        if (backgroundClasspath != null && !backgroundClasspath.isBlank()) {
            try (InputStream in = Maze.class.getResourceAsStream(backgroundClasspath)) {
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        System.out.println("Maze.loadBackgroundImage: loaded from classpath " + backgroundClasspath + " => " + img.getWidth() + "x" + img.getHeight());
                    } else {
                        System.out.println("Maze.loadBackgroundImage: classpath resource returned null: " + backgroundClasspath);
                    }
                    return img;
                }
            } catch (IOException e) {
                System.err.println("Failed to load maze background from classpath: " + e.getMessage());
            }
        }

        if (backgroundFilePath == null || backgroundFilePath.isBlank()) {
            return null;
        }

        File backgroundFile = new File(backgroundFilePath);
        if (!backgroundFile.exists()) {
            return null;
        }

        try {
            return ImageIO.read(backgroundFile);
        } catch (IOException e) {
            System.err.println("Failed to load maze background file: " + e.getMessage());
            return null;
        }
    }
}
