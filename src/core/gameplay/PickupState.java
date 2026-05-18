package core.gameplay;

import java.awt.Rectangle;

/**
 * Runtime state for a single pickup on the map.
 */
public class PickupState {
    private final PickupType type;
    private int tileX;
    private int tileY;
    private Rectangle bounds;
    private boolean active;

    public PickupState(PickupType type, int tileX, int tileY, int tileSize) {
        this.type = type;
        this.tileX = tileX;
        this.tileY = tileY;
        int size = Math.max(10, tileSize - 2);
        int px = tileX * tileSize + (tileSize - size) / 2;
        int py = tileY * tileSize + (tileSize - size) / 2;
        this.bounds = new Rectangle(px, py, size, size);
        this.active = true;
    }

    public PickupType getType()    { return type; }
    public int getTileX()          { return tileX; }
    public int getTileY()          { return tileY; }
    public Rectangle getBounds()   { return bounds; }
    public boolean isActive()      { return active; }
    public void deactivate()       { this.active = false; }

    public void relocateToTile(int tileX, int tileY, int tileSize) {
        this.tileX = tileX;
        this.tileY = tileY;
        int size = Math.max(10, tileSize - 2);
        int px = tileX * tileSize + (tileSize - size) / 2;
        int py = tileY * tileSize + (tileSize - size) / 2;
        this.bounds = new Rectangle(px, py, size, size);
    }
}
