package core.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollisionDebugInfo {
    public static class SamplePoint {
        private final String label;
        private final float x;
        private final float y;
        private final int tileX;
        private final int tileY;
        private final boolean walkable;

        public SamplePoint(String label, float x, float y, int tileX, int tileY, boolean walkable) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.tileX = tileX;
            this.tileY = tileY;
            this.walkable = walkable;
        }

        public String getLabel() {
            return label;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public int getTileX() {
            return tileX;
        }

        public int getTileY() {
            return tileY;
        }

        public boolean isWalkable() {
            return walkable;
        }
    }

    public static class TileHit {
        private final int tileX;
        private final int tileY;
        private final boolean walkable;

        public TileHit(int tileX, int tileY, boolean walkable) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.walkable = walkable;
        }

        public int getTileX() {
            return tileX;
        }

        public int getTileY() {
            return tileY;
        }

        public boolean isWalkable() {
            return walkable;
        }
    }

    private final String label;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final int margin;
    private final boolean canOccupy;
    private final boolean usedNudge;
    private final String resolution;
    private final String blockedReason;
    private final List<SamplePoint> samplePoints;
    private final List<TileHit> tiles;

    public CollisionDebugInfo(String label, float x, float y, float width, float height, int margin,
                              boolean canOccupy, boolean usedNudge, String resolution, String blockedReason,
                              List<SamplePoint> samplePoints, List<TileHit> tiles) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.margin = margin;
        this.canOccupy = canOccupy;
        this.usedNudge = usedNudge;
        this.resolution = resolution;
        this.blockedReason = blockedReason;
        this.samplePoints = Collections.unmodifiableList(new ArrayList<>(samplePoints));
        this.tiles = Collections.unmodifiableList(new ArrayList<>(tiles));
    }

    public String getLabel() {
        return label;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int getMargin() {
        return margin;
    }

    public boolean canOccupy() {
        return canOccupy;
    }

    public boolean usedNudge() {
        return usedNudge;
    }

    public String getResolution() {
        return resolution;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public List<SamplePoint> getSamplePoints() {
        return samplePoints;
    }

    public List<TileHit> getTiles() {
        return tiles;
    }
}
