package core.level.mapv2;

import core.level.Maze;

import java.awt.Rectangle;
import java.util.List;

public final class MapV2MazeConverter {
    private static final float WALL_COVERAGE_THRESHOLD = 0.18f;

    private MapV2MazeConverter() {
    }

    public static Maze buildMazeFromWalls(List<Rectangle> sourceWalls) {
        return buildMazeFromWalls(new Maze(), sourceWalls);
    }

    public static Maze buildMazeFromWalls(Maze maze, List<Rectangle> sourceWalls) {
        if (sourceWalls == null || sourceWalls.isEmpty()) {
            fillBorderWalls(maze);
            return maze;
        }

        MapV2CoordinateMapper mapper = new MapV2CoordinateMapper(maze);
        Rectangle[] scaledWalls = sourceWalls.stream()
                .map(mapper::scaleRect)
                .toArray(Rectangle[]::new);

        for (int tileY = 0; tileY < maze.getHeightInTiles(); tileY++) {
            for (int tileX = 0; tileX < maze.getWidthInTiles(); tileX++) {
                Rectangle tileBounds = new Rectangle(
                        tileX * maze.getTileSize(),
                        tileY * maze.getTileSize(),
                        maze.getTileSize(),
                        maze.getTileSize()
                );
                if (isWallTile(tileBounds, scaledWalls)) {
                    maze.setTile(tileX, tileY, Maze.Tile.WALL);
                }
            }
        }

        fillBorderWalls(maze);
        return maze;
    }

    private static boolean isWallTile(Rectangle tileBounds, Rectangle[] walls) {
        int tileArea = tileBounds.width * tileBounds.height;
        for (Rectangle wall : walls) {
            Rectangle intersection = tileBounds.intersection(wall);
            if (intersection.isEmpty()) {
                continue;
            }

            int overlapArea = intersection.width * intersection.height;
            float overlapRatio = overlapArea / (float) tileArea;
            if (overlapRatio >= WALL_COVERAGE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private static void fillBorderWalls(Maze maze) {
        for (int y = 0; y < maze.getHeightInTiles(); y++) {
            for (int x = 0; x < maze.getWidthInTiles(); x++) {
                if (x == 0 || y == 0 || x == maze.getWidthInTiles() - 1 || y == maze.getHeightInTiles() - 1) {
                    maze.setTile(x, y, Maze.Tile.WALL);
                }
            }
        }
    }
}
