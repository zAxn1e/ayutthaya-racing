package core.debug;
import core.entities.Direction;
import core.player.CollisionDebugInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerDebugSnapshot {
    private float x;
    private float y;
    private float width;
    private float height;
    private float centerX;
    private float centerY;
    private int gridX;
    private int gridY;
    private float speed;
    private float moveDistance;
    private float stepDistance;
    private double deltaSeconds;
    private int stepCount;
    private Direction currentDirection = Direction.NONE;
    private Direction nextDirection = Direction.NONE;
    private Direction facingDirection = Direction.NONE;
    private boolean moving;
    private boolean currentDirectionHeld;
    private String lastMovementMode = "idle";
    private String lastBlockedReason = "-";
    private final List<CollisionDebugInfo> recentChecks = new ArrayList<>();

    public void beginFrame(float x, float y, float width, float height, int tileSize, float speed, double deltaSeconds,
                           Direction currentDirection, Direction nextDirection, Direction facingDirection,
                           boolean moving, boolean currentDirectionHeld) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.centerX = x + width / 2f;
        this.centerY = y + height / 2f;
        this.gridX = (int) (centerX / tileSize);
        this.gridY = (int) (centerY / tileSize);
        this.speed = speed;
        this.deltaSeconds = deltaSeconds;
        this.currentDirection = currentDirection;
        this.nextDirection = nextDirection;
        this.facingDirection = facingDirection;
        this.moving = moving;
        this.currentDirectionHeld = currentDirectionHeld;
        this.moveDistance = 0;
        this.stepCount = 0;
        this.stepDistance = 0;
        this.lastMovementMode = "idle";
        this.lastBlockedReason = "-";
        this.recentChecks.clear();
    }

    public void setMovementPlan(float moveDistance, int stepCount, float stepDistance) {
        this.moveDistance = moveDistance;
        this.stepCount = stepCount;
        this.stepDistance = stepDistance;
    }

    public void setMovementOutcome(String lastMovementMode, String lastBlockedReason, boolean moving,
                                   float x, float y, int tileSize) {
        this.lastMovementMode = lastMovementMode;
        this.lastBlockedReason = lastBlockedReason;
        this.moving = moving;
        this.x = x;
        this.y = y;
        this.centerX = x + width / 2f;
        this.centerY = y + height / 2f;
        this.gridX = (int) (centerX / tileSize);
        this.gridY = (int) (centerY / tileSize);
    }

    public void addCheck(CollisionDebugInfo info) {
        if (info == null) {
            return;
        }
        recentChecks.add(info);
        if (recentChecks.size() > 14) {
            recentChecks.remove(0);
        }
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

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public float getSpeed() {
        return speed;
    }

    public float getMoveDistance() {
        return moveDistance;
    }

    public float getStepDistance() {
        return stepDistance;
    }

    public double getDeltaSeconds() {
        return deltaSeconds;
    }

    public int getStepCount() {
        return stepCount;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public Direction getNextDirection() {
        return nextDirection;
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isCurrentDirectionHeld() {
        return currentDirectionHeld;
    }

    public String getLastMovementMode() {
        return lastMovementMode;
    }

    public String getLastBlockedReason() {
        return lastBlockedReason;
    }

    public List<CollisionDebugInfo> getRecentChecks() {
        return Collections.unmodifiableList(recentChecks);
    }
}
