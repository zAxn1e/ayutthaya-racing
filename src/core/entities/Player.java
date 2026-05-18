package core.entities;

import core.debug.PlayerDebugSnapshot;
import core.config.PlayerConfig;
import core.player.CollisionDebugInfo;
import core.player.CollisionMap;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

public class Player extends Entity {
    private final float speed;
    private final int tileSize;
    private final EnumMap<Direction, BufferedImage> idleFrames;
    private final EnumMap<Direction, BufferedImage[]> runningFrames;

    private Direction currentDirection = Direction.NONE;
    private Direction nextDirection = Direction.NONE;
    private Direction facingDirection = Direction.DOWN;
    private boolean currentDirectionHeld = false;
    private int animFrame = 0;
    private double animTimer = 0;
    private boolean moving = false;

    public Player(float x, float y) {
        this(x, y, PlayerConfig.BASE_TILE_SIZE, PlayerConfig.moveSpeedForTile(PlayerConfig.BASE_TILE_SIZE));
    }

    public Player(float x, float y, int tileSize, float speed) {
        super(x, y, tileSize - PlayerConfig.hitboxInsetForTile(tileSize), tileSize - PlayerConfig.hitboxInsetForTile(tileSize));
        this.tileSize = tileSize;
        this.speed = speed;
        this.idleFrames = loadIdleFrames();
        this.runningFrames = loadRunningFrames();
    }

    public void setDirection(Direction dir) {
        setDirection(dir, false);
    }

    public void setDirection(Direction dir, boolean isCurrentDirectionHeld) {
        currentDirectionHeld = isCurrentDirectionHeld;

        if (dir == Direction.NONE) {
            currentDirection = Direction.NONE;
            nextDirection = Direction.NONE;
            moving = false;
            return;
        }

        nextDirection = dir;
    }

    @Override
    public void update(double delta) {
        animTimer += delta;
        if (animTimer >= PlayerConfig.ANIMATION_FRAME_SECONDS) {
            animTimer = 0;
            animFrame = (animFrame + 1) % PlayerConfig.SPRITE_FRAME_COUNT;
        }
    }

    public void move(CollisionMap map, double delta) {
        move(map, delta, null);
    }

    public void move(CollisionMap map, double delta, PlayerDebugSnapshot debugSnapshot) {
        if (map == null) {
            return;
        }

        if (debugSnapshot != null) {
            debugSnapshot.beginFrame(x, y, width, height, tileSize, speed, delta, currentDirection, nextDirection,
                    facingDirection, moving, currentDirectionHeld);
        }

        if (nextDirection != Direction.NONE && canMoveInDirection(map, nextDirection, debugSnapshot, "probe-next-open")) {
            currentDirection = nextDirection;
            nextDirection = Direction.NONE;
        }

        if (currentDirection != Direction.NONE
                && !currentDirectionHeld
                && nextDirection != Direction.NONE
                && !canMoveInDirection(map, nextDirection, debugSnapshot, "probe-next-blocked")) {
            currentDirection = Direction.NONE;
        }

        if (currentDirection == Direction.NONE) {
            moving = false;
            if (debugSnapshot != null) {
                debugSnapshot.setMovementOutcome("idle", "no active direction", false, x, y, tileSize);
            }
            return;
        }

        float moveDistance = speed * (float) delta;
        int stepCount = Math.max(1, (int) Math.ceil(moveDistance / Math.max(1.0f, tileSize / 4.0f)));
        float stepDistance = moveDistance / stepCount;
        if (debugSnapshot != null) {
            debugSnapshot.setMovementPlan(moveDistance, stepCount, stepDistance);
        }

        boolean moved = false;
        String lastMovementMode = "blocked";
        String lastBlockedReason = "-";

        for (int i = 0; i < stepCount; i++) {
            if (nextDirection != Direction.NONE && canMoveInDirection(map, nextDirection, debugSnapshot, "probe-step-turn")) {
                currentDirection = nextDirection;
                nextDirection = Direction.NONE;
            }

            float dx = currentDirection.dx * stepDistance;
            float dy = currentDirection.dy * stepDistance;

            boolean stepMoved = false;
            if (dx != 0) {
                stepMoved = tryMoveTo(map, x + dx, y, debugSnapshot);
            } else if (dy != 0) {
                stepMoved = tryMoveTo(map, x, y + dy, debugSnapshot);
            }

            if (!stepMoved) {
                lastBlockedReason = "step blocked in " + currentDirection.name();
                currentDirection = Direction.NONE;
                break;
            }

            moved = true;
            facingDirection = currentDirection;
            lastMovementMode = "step-direct";
            lastBlockedReason = "-";
        }

        clampToMapBounds(map);
        moving = moved;
        if (debugSnapshot != null) {
            CollisionDebugInfo settleInfo = map.inspectOccupancy("settled", x, y, width, height, PlayerConfig.collisionMarginForTile(tileSize));
            debugSnapshot.addCheck(settleInfo);
            if (!settleInfo.canOccupy()) {
                lastBlockedReason = settleInfo.getBlockedReason();
            }
            if (moved && !"blocked".equals(lastMovementMode)) {
                lastMovementMode = settleInfo.getResolution();
            }
            debugSnapshot.setMovementOutcome(lastMovementMode, lastBlockedReason, moving, x, y, tileSize);
        }
    }

    private boolean tryMoveTo(CollisionMap map, float newX, float newY, PlayerDebugSnapshot debugSnapshot) {
        CollisionDebugInfo directInfo = map.inspectOccupancy("direct", newX, newY, width, height, PlayerConfig.collisionMarginForTile(tileSize));
        if (debugSnapshot != null) {
            debugSnapshot.addCheck(directInfo);
        }
        if (!directInfo.canOccupy()) {
            return tryMoveWithCornerSlide(map, newX, newY, debugSnapshot);
        }

        x = newX;
        y = newY;
        return true;
    }

    private boolean canMoveInDirection(CollisionMap map, Direction dir) {
        return canMoveInDirection(map, dir, null, "probe");
    }

    private boolean canMoveInDirection(CollisionMap map, Direction dir, PlayerDebugSnapshot debugSnapshot, String label) {
        if (dir == Direction.NONE) return false;
        float probeDistance = PlayerConfig.collisionProbeDistanceForTile(tileSize);
        float probeX = x + dir.dx * probeDistance;
        float probeY = y + dir.dy * probeDistance;
        if (debugSnapshot != null) {
            debugSnapshot.addCheck(map.inspectOccupancy(label + "-" + dir.name(), probeX, probeY, width, height, PlayerConfig.collisionMarginForTile(tileSize)));
        }
        return canMoveToPosition(map, probeX, probeY);
    }

    private boolean canMoveToPosition(CollisionMap map, float newX, float newY) {
        int margin = PlayerConfig.collisionMarginForTile(tileSize);
        return map.canOccupy(newX, newY, width, height, margin);
    }

    private void clampToMapBounds(CollisionMap map) {
        float minX = 0;
        float minY = 0;
        float maxX = map.getWidthInTiles() * tileSize - width - 1;
        float maxY = map.getHeightInTiles() * tileSize - height - 1;
        x = Math.max(minX, Math.min(maxX, x));
        y = Math.max(minY, Math.min(maxY, y));
    }

    private boolean tryMoveWithCornerSlide(CollisionMap map, float newX, float newY, PlayerDebugSnapshot debugSnapshot) {
        float dx = newX - x;
        float dy = newY - y;

        if (dx == 0 && dy == 0) {
            return false;
        }

        if (dx != 0 && dy == 0) {
            return tryHorizontalSlide(map, newX, debugSnapshot);
        }

        if (dy != 0 && dx == 0) {
            return tryVerticalSlide(map, newY, debugSnapshot);
        }

        return false;
    }

    private boolean tryHorizontalSlide(CollisionMap map, float newX, PlayerDebugSnapshot debugSnapshot) {
        for (int i = 1; i <= PlayerConfig.CORNER_SLIDE_ATTEMPTS; i++) {
            float offset = PlayerConfig.cornerSlideStepForTile(tileSize) * i;
            if (tryOccupy(map, newX, y - offset, debugSnapshot, "slide-up-" + i)
                    || tryOccupy(map, newX, y + offset, debugSnapshot, "slide-down-" + i)) {
                return true;
            }
        }
        return tryPartialAdvance(map, newX, y, debugSnapshot);
    }

    private boolean tryVerticalSlide(CollisionMap map, float newY, PlayerDebugSnapshot debugSnapshot) {
        for (int i = 1; i <= PlayerConfig.CORNER_SLIDE_ATTEMPTS; i++) {
            float offset = PlayerConfig.cornerSlideStepForTile(tileSize) * i;
            if (tryOccupy(map, x - offset, newY, debugSnapshot, "slide-left-" + i)
                    || tryOccupy(map, x + offset, newY, debugSnapshot, "slide-right-" + i)) {
                return true;
            }
        }
        return tryPartialAdvance(map, x, newY, debugSnapshot);
    }

    private boolean tryPartialAdvance(CollisionMap map, float targetX, float targetY, PlayerDebugSnapshot debugSnapshot) {
        float dx = targetX - x;
        float dy = targetY - y;
        for (int i = 3; i >= 1; i--) {
            float factor = i / 4.0f;
            if (tryOccupy(map, x + dx * factor, y + dy * factor, debugSnapshot, "partial-" + i + "/4")) {
                return true;
            }
        }
        return false;
    }

    private boolean tryOccupy(CollisionMap map, float candidateX, float candidateY, PlayerDebugSnapshot debugSnapshot,
                              String label) {
        CollisionDebugInfo info = map.inspectOccupancy(label, candidateX, candidateY, width, height, PlayerConfig.collisionMarginForTile(tileSize));
        if (debugSnapshot != null) {
            debugSnapshot.addCheck(info);
        }
        if (!info.canOccupy()) {
            return false;
        }
        x = candidateX;
        y = candidateY;
        return true;
    }

    @Override
    public void render(Graphics2D g) {
        int size = PlayerConfig.spriteRenderSizeForTile(tileSize);
        int drawX = Math.round(x + width / 2f - size / 2f);
        int drawY = Math.round(y + height - size);

        // Draw shadow
        g.setColor(new Color(0, 0, 0, 60));
        int shadowWidth = Math.max(18, (int) (width * 0.9f));
        int shadowHeight = Math.max(8, shadowWidth / 3);
        int shadowX = Math.round(x + width / 2f - shadowWidth / 2f);
        int shadowY = Math.round(y + height / 1.4f - shadowHeight / 2f);
        g.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

        BufferedImage frame = getCurrentFrame();
        if (frame != null) {
            g.drawImage(frame, drawX, drawY, size, size, null);
            return;
        }

        renderFallback(g, drawX, drawY, size);
    }

    private void renderFallback(Graphics2D g, int drawX, int drawY, int size) {
        int mouthAngle = moving ? 45 - (animFrame * 12) : 5;
        if (mouthAngle < 5) {
            mouthAngle = 5;
        }

        g.setColor(new Color(255, 220, 50));
        g.fillArc(drawX, drawY, size, size, mouthAngle, 360 - mouthAngle * 2);

        g.setColor(Color.BLACK);
        int eyeX = drawX + size / 2 + 2;
        int eyeY = drawY + size / 4;
        g.fillOval(eyeX, eyeY, 4, 4);
    }

    private BufferedImage getCurrentFrame() {
        Direction visualDirection = getVisualDirection();
        if (moving) {
            BufferedImage[] frames = runningFrames.get(visualDirection);
            if (frames != null && frames.length > 0) {
                return frames[animFrame % frames.length];
            }
        }

        return idleFrames.get(visualDirection);
    }

    private Direction getVisualDirection() {
        if (currentDirection != Direction.NONE) {
            return currentDirection;
        }

        if (nextDirection != Direction.NONE) {
            return nextDirection;
        }

        return facingDirection;
    }

    private EnumMap<Direction, BufferedImage> loadIdleFrames() {
        EnumMap<Direction, BufferedImage> frames = new EnumMap<>(Direction.class);
        frames.put(Direction.UP, loadSpriteImage(PlayerConfig.IDLE_ROTATIONS_DIR + "\\north.png"));
        frames.put(Direction.RIGHT, loadSpriteImage(PlayerConfig.IDLE_ROTATIONS_DIR + "\\east.png"));
        frames.put(Direction.DOWN, loadSpriteImage(PlayerConfig.IDLE_ROTATIONS_DIR + "\\south.png"));
        frames.put(Direction.LEFT, loadSpriteImage(PlayerConfig.IDLE_ROTATIONS_DIR + "\\west.png"));
        return frames;
    }

    private EnumMap<Direction, BufferedImage[]> loadRunningFrames() {
        EnumMap<Direction, BufferedImage[]> frames = new EnumMap<>(Direction.class);
        frames.put(Direction.UP, loadAnimationFrames("north"));
        frames.put(Direction.RIGHT, loadAnimationFrames("east"));
        frames.put(Direction.DOWN, loadAnimationFrames("south"));
        frames.put(Direction.LEFT, loadAnimationFrames("west"));
        return frames;
    }

    private BufferedImage[] loadAnimationFrames(String directionName) {
        BufferedImage[] frames = new BufferedImage[PlayerConfig.SPRITE_FRAME_COUNT];
        boolean foundAny = false;

        for (int i = 0; i < PlayerConfig.SPRITE_FRAME_COUNT; i++) {
            String relativePath = PlayerConfig.RUNNING_ANIMATIONS_DIR + "\\" + directionName + "\\frame_%03d.png".formatted(i);
            frames[i] = loadSpriteImage(relativePath);
            foundAny = foundAny || frames[i] != null;
        }

        if (!foundAny) {
            return null;
        }

        for (int i = 1; i < frames.length; i++) {
            if (frames[i] == null) {
                frames[i] = frames[i - 1];
            }
        }
        if (frames[0] == null) {
            for (BufferedImage frame : frames) {
                if (frame != null) {
                    frames[0] = frame;
                    break;
                }
            }
        }

        return frames;
    }

    private BufferedImage loadSpriteImage(String relativePath) {
        String normalizedPath = relativePath.replace('\\', '/');
        String classpath = PlayerConfig.SPRITE_BASE_CLASSPATH + "/" + normalizedPath;

        try (InputStream in = Player.class.getResourceAsStream(classpath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                return validateFrameSize(image, classpath);
            }
        } catch (IOException e) {
            System.err.println("Failed to load player sprite from classpath " + classpath + ": " + e.getMessage());
        }

        File spriteFile = new File(PlayerConfig.SPRITE_BASE_FILE_PATH, relativePath);
        if (!spriteFile.exists()) {
            System.err.println("Player sprite not found: " + spriteFile.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(spriteFile);
            return validateFrameSize(image, spriteFile.getPath());
        } catch (IOException e) {
            System.err.println("Failed to load player sprite file " + spriteFile.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    private BufferedImage validateFrameSize(BufferedImage image, String source) {
        if (image == null) {
            return null;
        }

        if (image.getWidth() != PlayerConfig.SPRITE_SOURCE_FRAME_SIZE ||
                image.getHeight() != PlayerConfig.SPRITE_SOURCE_FRAME_SIZE) {
            System.err.println("Unexpected player sprite size at " + source + ": " +
                    image.getWidth() + "x" + image.getHeight());
        }

        return image;
    }

    public int getGridX() { return (int)((x + width/2) / tileSize); }
    public int getGridY() { return (int)((y + height/2) / tileSize); }

    public void reset(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        currentDirection = Direction.NONE;
        nextDirection = Direction.NONE;
        moving = false;
        facingDirection = Direction.DOWN;
    }

    public Direction getCurrentDirection() { return currentDirection; }
    public Direction getNextDirection() { return nextDirection; }
    public Direction getFacingDirection() { return facingDirection; }
    public boolean isMoving() { return moving; }
    public float getSpeed() { return speed; }
    public int getTileSize() { return tileSize; }
}
