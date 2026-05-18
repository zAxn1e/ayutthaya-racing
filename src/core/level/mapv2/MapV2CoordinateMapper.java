package core.level.mapv2;

import core.level.Maze;

import java.awt.Rectangle;

public final class MapV2CoordinateMapper {
    private final float scaleX;
    private final float scaleY;

    public MapV2CoordinateMapper() {
        this(Maze.DEFAULT_WIDTH * Maze.DEFAULT_TILE_SIZE, Maze.DEFAULT_HEIGHT * Maze.DEFAULT_TILE_SIZE);
    }

    public MapV2CoordinateMapper(Maze maze) {
        this(maze.getPixelWidth(), maze.getPixelHeight());
    }

    public MapV2CoordinateMapper(int targetWidthPx, int targetHeightPx) {
        this.scaleX = targetWidthPx / (float) MapV2Layouts.SOURCE_WIDTH;
        this.scaleY = targetHeightPx / (float) MapV2Layouts.SOURCE_HEIGHT;
    }

    public Rectangle scaleRect(Rectangle sourceRect) {
        int x = Math.round(sourceRect.x * scaleX);
        int y = Math.round(sourceRect.y * scaleY);
        int width = Math.max(1, Math.round(sourceRect.width * scaleX));
        int height = Math.max(1, Math.round(sourceRect.height * scaleY));
        return new Rectangle(x, y, width, height);
    }
}
