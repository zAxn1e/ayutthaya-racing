package core.player;

import core.config.PlayerConfig;
import core.level.Maze;

import java.util.ArrayList;
import java.util.List;

public class MazeCollisionMapAdapter implements CollisionMap {
    private final Maze maze;

    public MazeCollisionMapAdapter(Maze maze) {
        this.maze = maze;
    }

    @Override
    public int getTileSize() {
        return maze.getTileSize();
    }

    @Override
    public int getWidthInTiles() {
        return maze.getWidthInTiles();
    }

    @Override
    public int getHeightInTiles() {
        return maze.getHeightInTiles();
    }

    @Override
    public boolean isWalkable(int tileX, int tileY) {
        return maze.isWalkable(tileX, tileY);
    }

    @Override
    public boolean canOccupy(float x, float y, float width, float height, int margin) {
        return inspectOccupancy("occupy", x, y, width, height, margin).canOccupy();
    }

    @Override
    public CollisionDebugInfo inspectOccupancy(String label, float x, float y, float width, float height, int margin) {
        int tileSize = maze.getTileSize();
        CollisionDebugInfo directInfo = inspectAt(label, x, y, width, height, margin + PlayerConfig.mazeCollisionToleranceForTile(tileSize),
                false, "direct+tolerance");
        if (directInfo.canOccupy()) {
            return directInfo;
        }

        float nudge = PlayerConfig.mazeCollisionNudgeForTile(tileSize);
        CollisionDebugInfo leftInfo = inspectAt(label, x - nudge, y, width, height, margin, true, "nudge-left");
        if (leftInfo.canOccupy()) {
            return leftInfo;
        }

        CollisionDebugInfo rightInfo = inspectAt(label, x + nudge, y, width, height, margin, true, "nudge-right");
        if (rightInfo.canOccupy()) {
            return rightInfo;
        }

        CollisionDebugInfo upInfo = inspectAt(label, x, y - nudge, width, height, margin, true, "nudge-up");
        if (upInfo.canOccupy()) {
            return upInfo;
        }

        CollisionDebugInfo downInfo = inspectAt(label, x, y + nudge, width, height, margin, true, "nudge-down");
        if (downInfo.canOccupy()) {
            return downInfo;
        }

        return directInfo;
    }

    private CollisionDebugInfo inspectAt(String label, float x, float y, float width, float height, int margin,
                                         boolean usedNudge, String resolution) {
        float left = x + margin;
        float top = y + margin;
        float right = x + width - margin;
        float bottom = y + height - margin;

        if (left < 0 || top < 0 || right >= maze.getPixelWidth() || bottom >= maze.getPixelHeight()) {
            return new CollisionDebugInfo(label, x, y, width, height, margin, false, usedNudge, resolution,
                    "out-of-bounds", List.of(), List.of());
        }

        List<CollisionDebugInfo.SamplePoint> samples = new ArrayList<>();
        List<CollisionDebugInfo.TileHit> tiles = new ArrayList<>();

        boolean topLeft = addSample(samples, tiles, "TL", left, top);
        boolean topRight = addSample(samples, tiles, "TR", right, top);
        boolean bottomLeft = addSample(samples, tiles, "BL", left, bottom);
        boolean bottomRight = addSample(samples, tiles, "BR", right, bottom);
        boolean topMid = addSample(samples, tiles, "TM", (left + right) / 2f, top);
        boolean bottomMid = addSample(samples, tiles, "BM", (left + right) / 2f, bottom);
        boolean leftMid = addSample(samples, tiles, "LM", left, (top + bottom) / 2f);
        boolean rightMid = addSample(samples, tiles, "RM", right, (top + bottom) / 2f);
        boolean center = addSample(samples, tiles, "C", (left + right) / 2f, (top + bottom) / 2f);

        boolean canOccupy = topLeft && topRight && bottomLeft && bottomRight
                && topMid && bottomMid && leftMid && rightMid && center;
        String blockedReason = canOccupy ? "-" : "wall sample blocked";
        return new CollisionDebugInfo(label, x, y, width, height, margin, canOccupy, usedNudge, resolution,
                blockedReason, samples, tiles);
    }

    public Maze getMaze() {
        return maze;
    }

    private boolean addSample(List<CollisionDebugInfo.SamplePoint> samples, List<CollisionDebugInfo.TileHit> tiles,
                              String label, float px, float py) {
        int tileX = (int) (px / maze.getTileSize());
        int tileY = (int) (py / maze.getTileSize());
        boolean walkable = maze.isWalkable(tileX, tileY);
        samples.add(new CollisionDebugInfo.SamplePoint(label, px, py, tileX, tileY, walkable));
        addTile(tiles, tileX, tileY, walkable);
        return walkable;
    }

    private void addTile(List<CollisionDebugInfo.TileHit> tiles, int tileX, int tileY, boolean walkable) {
        for (CollisionDebugInfo.TileHit tile : tiles) {
            if (tile.getTileX() == tileX && tile.getTileY() == tileY) {
                return;
            }
        }
        tiles.add(new CollisionDebugInfo.TileHit(tileX, tileY, walkable));
    }
}
