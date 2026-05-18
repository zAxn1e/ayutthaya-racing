package core.player;

import core.config.PlayerConfig;
import core.level.Maze;
import core.level.mapv2.MapV2CoordinateMapper;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class MapV2CollisionMap implements CollisionMap {
    private final Rectangle[] walls;
    private final int tileSize;
    private final int widthInTiles;
    private final int heightInTiles;
    private final int widthPx;
    private final int heightPx;

    public MapV2CollisionMap(List<Rectangle> sourceWalls) {
        this(Maze.DEFAULT_WIDTH, Maze.DEFAULT_HEIGHT, Maze.DEFAULT_TILE_SIZE, sourceWalls);
    }

    public MapV2CollisionMap(int widthInTiles, int heightInTiles, int tileSize, List<Rectangle> sourceWalls) {
        this.tileSize = tileSize;
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.widthPx = widthInTiles * tileSize;
        this.heightPx = heightInTiles * tileSize;
        MapV2CoordinateMapper mapper = new MapV2CoordinateMapper(widthPx, heightPx);
        this.walls = sourceWalls.stream()
                .map(mapper::scaleRect)
                .map(this::deflateWall)
                .toArray(Rectangle[]::new);
    }

    @Override
    public int getTileSize() {
        return tileSize;
    }

    @Override
    public int getWidthInTiles() {
        return widthInTiles;
    }

    @Override
    public int getHeightInTiles() {
        return heightInTiles;
    }

    @Override
    public boolean isWalkable(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= getWidthInTiles() || tileY >= getHeightInTiles()) {
            return false;
        }
        int playerBodySize = tileSize - PlayerConfig.hitboxInsetForTile(tileSize);
        float x = tileX * getTileSize() + (getTileSize() - playerBodySize) / 2f;
        float y = tileY * getTileSize() + (getTileSize() - playerBodySize) / 2f;
        return canOccupy(x, y, playerBodySize, playerBodySize, PlayerConfig.collisionMarginForTile(tileSize));
    }

    @Override
    public boolean canOccupy(float x, float y, float width, float height, int margin) {
        float left = x + margin;
        float top = y + margin;
        float right = x + width - margin;
        float bottom = y + height - margin;

        if (left < 0 || top < 0 || right >= widthPx || bottom >= heightPx) {
            return false;
        }

        Rectangle2D.Float hitbox = new Rectangle2D.Float(left, top, Math.max(1f, right - left), Math.max(1f, bottom - top));
        for (Rectangle wall : walls) {
            if (hitbox.intersects(wall)) {
                return false;
            }
        }
        return true;
    }

    private Rectangle deflateWall(Rectangle wall) {
        int inset = Math.max(0, PlayerConfig.wallCollisionInsetForTile(tileSize));
        int width = Math.max(0, wall.width - inset * 2);
        int height = Math.max(0, wall.height - inset * 2);
        int x = wall.x + Math.min(inset, Math.max(0, wall.width - 1) / 2);
        int y = wall.y + Math.min(inset, Math.max(0, wall.height - 1) / 2);
        if (width == 0 || height == 0) {
            return new Rectangle(x, y, width, height);
        }
        if (x + width > widthPx) {
            x = widthPx - width;
        }
        if (y + height > heightPx) {
            y = heightPx - height;
        }
        return new Rectangle(Math.max(0, x), Math.max(0, y), width, height);
    }
}
