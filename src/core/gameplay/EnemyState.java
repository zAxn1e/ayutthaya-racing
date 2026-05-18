package core.gameplay;

import core.entities.Direction;

import java.awt.Rectangle;

/**
 * Runtime state for a single police enemy.
 * Tracks position (float-precision), current direction, speed with
 * turn-slowdown recovery, alive/dead status, and respawn timer.
 */
public class EnemyState {
    private final EnemyType type;
    private float x, y;
    private float width, height;
    private final int tileSize;
    private final float baseSpeed;
    private float currentSpeed;
    private Direction direction;
    private boolean alive;
    private double respawnTimer;
    private double turnRecoveryTimer;

    /** The grid tile the enemy was on when it last made a direction decision. */
    private int lastDecisionTileX = -1;
    private int lastDecisionTileY = -1;

    public EnemyState(EnemyType type, int tileX, int tileY, int tileSize, float playerBaseSpeed) {
        this.type = type;
        this.tileSize = tileSize;
        this.width = tileSize - 2;
        this.height = tileSize - 2;
        this.x = tileX * tileSize + (tileSize - width) / 2f;
        this.y = tileY * tileSize + (tileSize - height) / 2f;
        this.baseSpeed = playerBaseSpeed * type.speedMultiplier;
        this.currentSpeed = baseSpeed;
        this.direction = Direction.NONE;
        this.alive = true;
        this.respawnTimer = 0;
        this.turnRecoveryTimer = 0;
    }

    // ── Position helpers ──

    public int getGridX() { return (int) ((x + width / 2) / tileSize); }
    public int getGridY() { return (int) ((y + height / 2) / tileSize); }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    public float getCenterX() { return x + width / 2f; }
    public float getCenterY() { return y + height / 2f; }

    // ── Decision tracking ──

    /**
     * Returns true if the enemy has entered a new grid tile since its last
     * direction decision (or has never decided yet).
     */
    public boolean hasEnteredNewTile() {
        int gx = getGridX();
        int gy = getGridY();
        return gx != lastDecisionTileX || gy != lastDecisionTileY;
    }

    /** Mark the current tile as the decision tile (call after choosing a direction). */
    public void markDecisionTile() {
        lastDecisionTileX = getGridX();
        lastDecisionTileY = getGridY();
    }

    // ── Movement ──

    /**
     * Apply a direction change. When the new direction differs from the old,
     * trigger turn slowdown.
     */
    public void changeDirection(Direction newDir) {
        if (newDir != direction && newDir != Direction.NONE) {
            turnRecoveryTimer = GameplayConfig.TURN_RECOVERY_SECONDS;
            currentSpeed = baseSpeed * GameplayConfig.TURN_SLOWDOWN_FACTOR;
        }
        this.direction = newDir;
    }

    /**
     * Advance position by current speed × delta, and recover speed from turn
     * slowdown if applicable.
     */
    public void advance(double delta) {
        if (direction == Direction.NONE) return;
        x += direction.dx * currentSpeed * (float) delta;
        y += direction.dy * currentSpeed * (float) delta;

        if (turnRecoveryTimer > 0) {
            turnRecoveryTimer -= delta;
            if (turnRecoveryTimer <= 0) {
                turnRecoveryTimer = 0;
                currentSpeed = baseSpeed;
            } else {
                float t = 1f - (float) (turnRecoveryTimer / GameplayConfig.TURN_RECOVERY_SECONDS);
                currentSpeed = baseSpeed * (GameplayConfig.TURN_SLOWDOWN_FACTOR + (1f - GameplayConfig.TURN_SLOWDOWN_FACTOR) * t);
            }
        }
    }

    /**
     * Snap the enemy center to the center of its current grid tile.
     * Useful after a direction change to prevent drift.
     */
    public void snapToGrid() {
        int gx = getGridX();
        int gy = getGridY();
        x = gx * tileSize + (tileSize - width) / 2f;
        y = gy * tileSize + (tileSize - height) / 2f;
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    // ── Life cycle ──

    public void kill() {
        alive = false;
        respawnTimer = GameplayConfig.ENEMY_RESPAWN_DELAY;
    }

    public void scheduleRespawnRetry(double delaySeconds) {
        alive = false;
        respawnTimer = Math.max(0.1, delaySeconds);
    }

    /** Tick respawn timer. Returns true when the enemy is ready to respawn. */
    public boolean tickRespawn(double delta) {
        if (alive) return false;
        respawnTimer -= delta;
        return respawnTimer <= 0;
    }

    public void respawn(int tileX, int tileY) {
        this.x = tileX * tileSize + (tileSize - width) / 2f;
        this.y = tileY * tileSize + (tileSize - height) / 2f;
        this.alive = true;
        this.direction = Direction.NONE;
        this.currentSpeed = baseSpeed;
        this.turnRecoveryTimer = 0;
        this.respawnTimer = 0;
        this.lastDecisionTileX = -1;
        this.lastDecisionTileY = -1;
    }

    // ── Getters ──

    public EnemyType getType()       { return type; }
    public float getX()              { return x; }
    public float getY()              { return y; }
    public float getWidth()          { return width; }
    public float getHeight()         { return height; }
    public int getTileSize()         { return tileSize; }
    public float getBaseSpeed()      { return baseSpeed; }
    public float getCurrentSpeed()   { return currentSpeed; }
    public Direction getDirection()   { return direction; }
    public boolean isAlive()         { return alive; }
    public double getRespawnTimer()  { return respawnTimer; }
}
