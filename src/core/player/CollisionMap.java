package core.player;

import java.util.ArrayList;
import java.util.List;

public interface CollisionMap {
    int getTileSize();
    int getWidthInTiles();
    int getHeightInTiles();
    boolean isWalkable(int tileX, int tileY);

    default boolean canOccupy(float x, float y, float width, float height, int margin) {
        int tileSize = getTileSize();
        float left = x + margin;
        float top = y + margin;
        float right = x + width - margin;
        float bottom = y + height - margin;

        int topLeftX = (int) (left / tileSize);
        int topLeftY = (int) (top / tileSize);
        int topRightX = (int) (right / tileSize);
        int topRightY = (int) (top / tileSize);
        int bottomLeftX = (int) (left / tileSize);
        int bottomLeftY = (int) (bottom / tileSize);
        int bottomRightX = (int) (right / tileSize);
        int bottomRightY = (int) (bottom / tileSize);

        return isWalkable(topLeftX, topLeftY)
                && isWalkable(topRightX, topRightY)
                && isWalkable(bottomLeftX, bottomLeftY)
                && isWalkable(bottomRightX, bottomRightY);
    }

    default CollisionDebugInfo inspectOccupancy(String label, float x, float y, float width, float height, int margin) {
        List<CollisionDebugInfo.SamplePoint> samples = new ArrayList<>();
        List<CollisionDebugInfo.TileHit> tiles = new ArrayList<>();
        int tileSize = getTileSize();

        float left = x + margin;
        float top = y + margin;
        float right = x + width - margin;
        float bottom = y + height - margin;

        boolean topLeft = addSample(samples, tiles, "TL", left, top, tileSize);
        boolean topRight = addSample(samples, tiles, "TR", right, top, tileSize);
        boolean bottomLeft = addSample(samples, tiles, "BL", left, bottom, tileSize);
        boolean bottomRight = addSample(samples, tiles, "BR", right, bottom, tileSize);

        boolean canOccupy = topLeft && topRight && bottomLeft && bottomRight;
        String blockedReason = canOccupy ? "-" : "blocked corner";
        return new CollisionDebugInfo(label, x, y, width, height, margin, canOccupy, false,
                "base-check", blockedReason, samples, tiles);
    }

    private boolean addSample(List<CollisionDebugInfo.SamplePoint> samples, List<CollisionDebugInfo.TileHit> tiles,
                              String label, float px, float py, int tileSize) {
        int tileX = (int) (px / tileSize);
        int tileY = (int) (py / tileSize);
        boolean walkable = isWalkable(tileX, tileY);
        samples.add(new CollisionDebugInfo.SamplePoint(label, px, py, tileX, tileY, walkable));
        addTile(tiles, tileX, tileY, walkable);
        return walkable;
    }

    private static void addTile(List<CollisionDebugInfo.TileHit> tiles, int tileX, int tileY, boolean walkable) {
        for (CollisionDebugInfo.TileHit tile : tiles) {
            if (tile.getTileX() == tileX && tile.getTileY() == tileY) {
                return;
            }
        }
        tiles.add(new CollisionDebugInfo.TileHit(tileX, tileY, walkable));
    }
}
