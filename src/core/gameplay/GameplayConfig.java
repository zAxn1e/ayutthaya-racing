package core.gameplay;

/**
 * Central tuning knobs for the spawn director, pickup system, and enemy system.
 * All timing values are in seconds (game-time, paused time excluded).
 */
public final class GameplayConfig {
    private GameplayConfig() {}

    // ── Stage progression (elapsed game-time thresholds in seconds) ──
    /** Stage 1 starts immediately — only FAT police */
    public static final double STAGE_1_TIME = 0.0;
    /** Stage 2 unlock — CAR police added */
    public static final double STAGE_2_TIME = 30.0;
    /** Stage 3 unlock — CHICKEN police added */
    public static final double STAGE_3_TIME = 70.0;

    // ── Enemy spawning ──
    /** Max active slots for FAT enemies */
    public static final int MAX_FAT_ENEMIES = 2;
    /** Max active slots for CAR enemies */
    public static final int MAX_CAR_ENEMIES = 2;
    /** Max active slots for CHICKEN enemies */
    public static final int MAX_CHICKEN_ENEMIES = 1;
    /** Delay between checking for new enemy spawns (seconds) */
    public static final double ENEMY_SPAWN_INTERVAL = 4.0;
    /** Respawn cooldown after a police is killed (seconds) */
    public static final double ENEMY_RESPAWN_DELAY = 8.0;
    /** Retry delay when no valid respawn tile is found */
    public static final double ENEMY_RESPAWN_RETRY_DELAY = 0.75;
    /** Minimum tile distance from player for enemy spawn placement */
    public static final int ENEMY_MIN_SPAWN_DISTANCE_TILES = 8;
    /** Max placement attempts per spawn try */
    public static final int ENEMY_SPAWN_ATTEMPTS = 200;

    // ── Enemy movement ──
    /** Turn slowdown factor: speed * this value during a turn recovery */
    public static final float TURN_SLOWDOWN_FACTOR = 0.35f;
    /** Seconds to recover from turn slowdown back to base speed */
    public static final double TURN_RECOVERY_SECONDS = 0.45;
    /** Probability (0..1) that an enemy biases toward the player at an intersection */
    public static final double ENEMY_CHASE_BIAS = 0.70;

    // ── Point pickups ──
    /** Max simultaneous point pickups on the map */
    public static final int MAX_POINT_PICKUPS = 5;
    /** Interval between point pickup spawn checks */
    public static final double POINT_SPAWN_INTERVAL = 2.0;
    /** Score awarded per point pickup */
    public static final int POINT_SCORE_VALUE = 1;

    // ── Kill pickups ──
    /** Max simultaneous kill pickups on the map (across all types) */
    public static final int MAX_KILL_PICKUPS = 2;
    /** Cooldown between kill pickup spawns */
    public static final double KILL_SPAWN_COOLDOWN = 12.0;
    /** Number of placement attempts for pickups */
    public static final int PICKUP_SPAWN_ATTEMPTS = 300;

    // ── Enemy collision with player ──
    /** Score penalty when an enemy touches the player */
    public static final int ENEMY_TOUCH_PENALTY = 30;
    /** Invincibility seconds after being hit by enemy */
    public static final double PLAYER_INVINCIBILITY_SECONDS = 2.0;

    public static int maxEnemiesForType(EnemyType type) {
        return switch (type) {
            case FAT -> MAX_FAT_ENEMIES;
            case CAR -> MAX_CAR_ENEMIES;
            case CHICKEN -> MAX_CHICKEN_ENEMIES;
        };
    }
}
