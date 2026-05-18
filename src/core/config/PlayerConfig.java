package core.config;

public final class PlayerConfig {
    public static final int BASE_TILE_SIZE = 32;
    private static final float TILE_SCALE_DENOMINATOR = 32.0f;

    private PlayerConfig() {
    }

    // Movement and hitbox
    public static final float BASE_MOVE_SPEED = 120.0f;
    public static final int CORNER_SLIDE_ATTEMPTS = 4;

    // Animation timing
    public static final double ANIMATION_FRAME_SECONDS = 0.08;

    // Thief sprite settings
    public static final String SPRITE_BASE_CLASSPATH = "/sprites/thief";
    public static final String SPRITE_BASE_FILE_PATH = ProjectPaths.spriteThiefDirectory().getPath();
    public static final String IDLE_ROTATIONS_DIR = "rotations";

    static {
        // Debug: print resolved sprite paths at JVM startup
        try {
            System.out.println("PlayerConfig: SPRITE_BASE_FILE_PATH=" + SPRITE_BASE_FILE_PATH);
            System.out.println("PlayerConfig: spriteThiefDirectory()=" + ProjectPaths.spriteThiefDirectory().getCanonicalPath());
        } catch (Exception ignored) {}
    }
    public static final String RUNNING_ANIMATIONS_DIR = "animations\\Running";
    public static final int SPRITE_SOURCE_FRAME_SIZE = 92;
    public static final int SPRITE_FRAME_COUNT = 6;

    public static float moveSpeedForTile(int tileSize) {
        return BASE_MOVE_SPEED * scale(tileSize);
    }

    public static int hitboxInsetForTile(int tileSize) {
        return Math.max(2, Math.round(6 * scale(tileSize)));
    }

    public static int collisionMarginForTile(int tileSize) {
        return Math.max(1, Math.round(3 * scale(tileSize)));
    }

    public static int wallCollisionInsetForTile(int tileSize) {
        return Math.max(1, Math.round(5 * scale(tileSize)));
    }

    public static float collisionProbeDistanceForTile(int tileSize) {
        return Math.max(2.0f, 4.0f * scale(tileSize));
    }

    public static float cornerSlideStepForTile(int tileSize) {
        return Math.max(0.75f, 1.5f * scale(tileSize));
    }

    public static int mazeCollisionToleranceForTile(int tileSize) {
        return Math.max(1, Math.round(2 * scale(tileSize)));
    }

    public static float mazeCollisionNudgeForTile(int tileSize) {
        return Math.max(1.0f, 2.0f * scale(tileSize));
    }

    public static int spriteRenderSizeForTile(int tileSize) {
        return Math.round(tileSize * 1.6f);
    }

    private static float scale(int tileSize) {
        return tileSize / TILE_SCALE_DENOMINATOR;
    }
}
